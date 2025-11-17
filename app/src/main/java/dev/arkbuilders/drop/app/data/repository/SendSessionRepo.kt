package dev.arkbuilders.drop.app.data.repository

import android.net.Uri
import dev.arkbuilders.drop.app.data.SendFilesSubscriberImpl
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.model.DropFileInfo
import dev.arkbuilders.drop.app.domain.model.SendSession
import dev.arkbuilders.drop.app.domain.model.TransferStatus
import dev.arkbuilders.drop.app.domain.repository.TransferSessionRepo
import dev.arkbuilders.drop.app.domain.usecase.SendFilesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class SendSessionRepo(
    private val sendUseCase: SendFilesUseCase,
    private val resourcesHelper: ResourcesHelper,
    private val transferSessionRepository: TransferSessionRepo,
) {
    // Keep references to active sessions here so file transfers continue even if the ViewModel dies
    private val activeSessions = mutableListOf<SendSession>()
    private val activeSessionsMutex = Mutex()
    private val cancelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun sendFiles(fileUris: List<Uri>): SendSession? =
        withContext(Dispatchers.IO) {
            cleanupFinishedSessions()

            sendUseCase.invoke(fileUris).fold(
                onSuccess = { bubble ->
                    val subscriber =
                        SendFilesSubscriberImpl().also { subscriber ->
                            bubble.subscribe(subscriber)
                        }

                    val session = SendSession(bubble, subscriber)
                    activeSessionsMutex.withLock {
                        activeSessions.add(session)
                    }
                    return@withContext session
                },
                onFailure = {
                    return@withContext null
                },
            )
        }

    suspend fun recordSendCompletion(
        fileUris: List<Uri>,
        session: SendSession,
    ) {
        try {
            cleanupFinishedSessions()
            val progress = session.subscriber.progress.value
            val receiverName = progress.receiverName
            val receiverAvatar = progress.receiverAvatar

            val filesInfo =
                fileUris.map {
                    DropFileInfo(
                        name = resourcesHelper.getFileName(it.toString()) ?: "",
                        size = resourcesHelper.getFileSize(it.toString()),
                    )
                }

            transferSessionRepository.addSentTransfer(
                files = filesInfo,
                peerName = receiverName,
                peerAvatar = receiverAvatar,
                status = TransferStatus.COMPLETED,
            )
        } catch (e: Exception) {
            Timber.e("Error recording send completion ${e.message}")
        }
    }

    fun cancelSend(session: SendSession) {
        cancelScope.launch {
            try {
                activeSessionsMutex.withLock {
                    activeSessions.remove(session)
                }
                session.bubble.unsubscribe(session.subscriber)
                session.bubble.cancel()
            } catch (e: Throwable) {
                Timber.e("Error cancelling send ${e.message}")
            }
        }
    }

    private suspend fun cleanupFinishedSessions() =
        activeSessionsMutex.withLock {
            activeSessions.removeAll { it.bubble.isFinished() }
        }
}
