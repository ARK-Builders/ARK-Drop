package dev.arkbuilders.drop.data.repository

import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.data.helper.ResourcesHelper
import dev.arkbuilders.drop.domain.libwrapper.getDropApi
import dev.arkbuilders.drop.domain.model.DropFileInfo
import dev.arkbuilders.drop.domain.model.ReceiveSession
import dev.arkbuilders.drop.domain.model.TransferStatus
import dev.arkbuilders.drop.domain.repository.ReceiveSessionRepo
import dev.arkbuilders.drop.domain.repository.TransferSessionRepo
import dev.arkbuilders.drop.domain.usecase.ReceiveFilesUseCase
import dev.arkbuilders.drop.instrumentation.FirebaseReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ReceiveSessionRepoImpl(
    private val receiveFilesUseCase: ReceiveFilesUseCase,
    private val transferHistoryRepository: TransferSessionRepo,
    private val resourcesHelper: ResourcesHelper,
    private val firebaseReporter: FirebaseReporter,
) : ReceiveSessionRepo {
    // Keep references to active sessions here so file transfers continue even if the ViewModel dies
    private val activeSessions = mutableListOf<ReceiveSession>()
    private val activeSessionsMutex = Mutex()
    private val cancelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun receiveFiles(
        ticket: String,
        confirmation: UByte,
    ): ReceiveSession? =
        withContext(Dispatchers.IO) {
            firebaseReporter.log("ReceiveSessionRepo: receiveFiles ticket=$ticket confirmation=$confirmation")

            receiveFilesUseCase.invoke(ticket, confirmation).fold(
                onSuccess = { bubble ->
                    firebaseReporter.log("ReceiveSessionRepo: use case returned bubble, creating subscriber")

                    val subscriber =
                        getDropApi().createReceiveSubscriber().also { subscriber ->
                            bubble.subscribe(subscriber)
                        }

                    val session =
                        ReceiveSession(
                            bubble,
                            subscriber,
                        )
                    activeSessionsMutex.withLock {
                        activeSessions.add(session)
                        firebaseReporter.setCustomKey("active_receive_count", activeSessions.size.toString())
                    }

                    firebaseReporter.log("ReceiveSessionRepo: starting bubble")
                    bubble.start()
                    firebaseReporter.log("ReceiveSessionRepo: session created and started successfully")
                    return@withContext session
                },
                onFailure = { e ->
                    firebaseReporter.recordError("ReceiveSessionRepo: receiveFiles failed ticket=$ticket", e)
                    return@withContext null
                },
            )
        }

    override suspend fun saveReceivedFiles(session: ReceiveSession): List<String> =
        withContext(Dispatchers.IO) {
            val subscriber = session.subscriber
            val completeFiles = subscriber.getCompleteFiles()
            val savedFiles = mutableListOf<DropFileInfo>()

            firebaseReporter.log("ReceiveSessionRepo: saveReceivedFiles completeFiles=${completeFiles.size}")

            try {
                completeFiles.forEach { (fileInfo, data) ->
                    firebaseReporter.log("ReceiveSessionRepo: saving file name=${fileInfo.name} size=${fileInfo.size}")
                    val savedFile = resourcesHelper.saveFileToDownloads(fileInfo.name, data)
                    if (savedFile != null) {
                        savedFiles.add(DropFileInfo(savedFile, fileInfo.size.toLong()))
                        Logger.i("Saved file name: $savedFile")
                        firebaseReporter.log("ReceiveSessionRepo: file saved path=$savedFile")
                    } else {
                        Logger.e("Failed to save file: ${fileInfo.name}")
                        firebaseReporter.recordError("ReceiveSessionRepo: failed to save file name=${fileInfo.name}", null)
                    }
                }

                val progress = subscriber.progress.value
                val senderName = progress.senderName
                val senderAvatar = progress.senderAvatar

                if (savedFiles.isNotEmpty()) {
                    firebaseReporter.log("ReceiveSessionRepo: adding completed transfer to history files=${savedFiles.size} sender=$senderName")

                    transferHistoryRepository.addReceivedTransfer(
                        files = savedFiles,
                        peerName = senderName,
                        peerAvatar = senderAvatar,
                        status = TransferStatus.COMPLETED,
                    )
                    firebaseReporter.log("ReceiveSessionRepo: transfer history entry added successfully")
                } else {
                    firebaseReporter.log("ReceiveSessionRepo: no files saved, adding failed transfer to history")

                    transferHistoryRepository.addReceivedTransfer(
                        files = emptyList(),
                        peerName = senderName,
                        peerAvatar = senderAvatar,
                        status = TransferStatus.FAILED,
                    )
                }
            } catch (e: Exception) {
                Logger.e("Error saving received files ${e.message}")
                firebaseReporter.recordError("ReceiveSessionRepo: saveReceivedFiles exception", e)

                val progress = subscriber.progress.value
                val senderName = progress.senderName
                val senderAvatar = progress.senderAvatar

                transferHistoryRepository.addReceivedTransfer(
                    files = emptyList(),
                    peerName = senderName,
                    peerAvatar = senderAvatar,
                    status = TransferStatus.FAILED,
                )
            }

            firebaseReporter.log("ReceiveSessionRepo: saveReceivedFiles completed savedCount=${savedFiles.size}")
            return@withContext savedFiles.map { it.name }
        }

    override fun cancelReceive(session: ReceiveSession) {
        firebaseReporter.log("ReceiveSessionRepo: cancelReceive session=${session.hashCode()}")
        cancelScope.launch {
            try {
                activeSessionsMutex.withLock {
                    activeSessions.remove(session)
                    firebaseReporter.setCustomKey("active_receive_count", activeSessions.size.toString())
                }
                session.bubble.unsubscribe(session.subscriber)
                session.bubble.cancel()
                firebaseReporter.log("ReceiveSessionRepo: session cancelled successfully")
            } catch (e: Throwable) {
                Logger.e("Error cancelling receive ${e.message}")
                firebaseReporter.recordError("ReceiveSessionRepo: cancelReceive error", e)
            }
        }
    }
}
