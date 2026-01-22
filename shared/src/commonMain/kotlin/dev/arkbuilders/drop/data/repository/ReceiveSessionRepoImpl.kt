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
            receiveFilesUseCase.invoke(ticket, confirmation).fold(
                onSuccess = { bubble ->
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
                    }

                    bubble.start()
                    return@withContext session
                },
                onFailure = {
                    return@withContext null
                },
            )
        }

    override suspend fun saveReceivedFiles(session: ReceiveSession): List<String> =
        withContext(Dispatchers.IO) {
            val subscriber = session.subscriber
            val completeFiles = subscriber.getCompleteFiles()
            val savedFiles = mutableListOf<DropFileInfo>()

            try {
                completeFiles.forEach { (fileInfo, data) ->
                    val savedFile = resourcesHelper.saveFileToDownloads(fileInfo.name, data)
                    if (savedFile != null) {
                        savedFiles.add(DropFileInfo(savedFile, fileInfo.size.toLong()))
                        Logger.i("Saved file name: $savedFile")
                    } else {
                        Logger.e("Failed to save file: ${fileInfo.name}")
                    }
                }

                if (savedFiles.isNotEmpty()) {
                    val progress = subscriber.progress.value
                    val senderName = progress.senderName
                    val senderAvatar = progress.senderAvatar

                    transferHistoryRepository.addReceivedTransfer(
                        files = savedFiles,
                        peerName = senderName,
                        peerAvatar = senderAvatar,
                        status = TransferStatus.COMPLETED,
                    )
                }
            } catch (e: Exception) {
                Logger.e("Error saving received files ${e.message}")

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

            return@withContext savedFiles.map { it.name }
        }

    override fun cancelReceive(session: ReceiveSession) {
        cancelScope.launch {
            try {
                activeSessionsMutex.withLock {
                    activeSessions.remove(session)
                }
                session.bubble.unsubscribe(session.subscriber)
                session.bubble.cancel()
            } catch (e: Throwable) {
                Logger.e("Error cancelling receive ${e.message}")
            }
        }
    }
}
