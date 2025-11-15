package dev.arkbuilders.drop.app.data.repository

import dev.arkbuilders.drop.app.data.ReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.model.ReceiveSession
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.repository.TransferHistoryItemRepository
import dev.arkbuilders.drop.app.domain.usecase.ReceiveFilesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class ReceiveSessionRepo(
    private val receiveFilesUseCase: ReceiveFilesUseCase,
    private val transferHistoryRepository: TransferHistoryItemRepository,
    private val resourcesHelper: ResourcesHelper,
) {
    // Keep references to active sessions here so file transfers continue even if the ViewModel dies
    private val activeSessions = mutableListOf<ReceiveSession>()
    private val activeSessionsMutex = Mutex()
    private val cancelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun receiveFiles(
        ticket: String,
        confirmation: UByte,
    ): ReceiveSession? =
        withContext(Dispatchers.IO) {
            receiveFilesUseCase.invoke(ticket, confirmation).fold(
                onSuccess = { bubble ->
                    val subscriber =
                        ReceiveFilesSubscriberImpl().also { subscriber ->
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

    suspend fun saveReceivedFiles(session: ReceiveSession): List<String> =
        withContext(Dispatchers.IO) {
            val subscriber = session.subscriber
            val completeFiles = subscriber.getCompleteFiles()
            val savedFiles = mutableListOf<String>()

            try {
                completeFiles.forEach { (fileInfo, data) ->
                    val savedFile = resourcesHelper.saveFileToDownloads(fileInfo.name, data)
                    if (savedFile != null) {
                        savedFiles.add(savedFile)
                        Timber.i("Saved file name: $savedFile")
                    } else {
                        Timber.e("Failed to save file: ${fileInfo.name}")
                    }
                }

                if (savedFiles.isNotEmpty()) {
                    val progress = subscriber.progress.value
                    val senderName = progress.senderName
                    val senderAvatar = progress.senderAvatar

                    val totalSize = completeFiles.sumOf { it.second.size.toLong() }
                    val firstFileName = savedFiles.firstOrNull() ?: "Unknown"

                    transferHistoryRepository.addReceivedTransfer(
                        fileName = firstFileName,
                        fileSize = totalSize,
                        peerName = senderName,
                        peerAvatar = senderAvatar,
                        fileCount = savedFiles.size,
                        status = TransferStatus.COMPLETED,
                    )
                }
            } catch (e: Exception) {
                Timber.e("Error saving received files ${e.message}")

                val progress = subscriber.progress.value
                val senderName = progress.senderName
                val senderAvatar = progress.senderAvatar

                transferHistoryRepository.addReceivedTransfer(
                    fileName = "Transfer failed",
                    fileSize = 0L,
                    peerName = senderName,
                    peerAvatar = senderAvatar,
                    fileCount = completeFiles.size,
                    status = TransferStatus.FAILED,
                )
            }

            savedFiles
        }

    fun cancelReceive(session: ReceiveSession) {
        cancelScope.launch {
            try {
                activeSessionsMutex.withLock {
                    activeSessions.remove(session)
                }
                session.bubble.unsubscribe(session.subscriber)
                session.bubble.cancel()
            } catch (e: Throwable) {
                Timber.e("Error cancelling receive ${e.message}")
            }
        }
    }
}
