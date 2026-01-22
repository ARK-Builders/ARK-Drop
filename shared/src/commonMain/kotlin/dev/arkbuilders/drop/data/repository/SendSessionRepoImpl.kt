package dev.arkbuilders.drop.data.repository

import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.data.helper.ResourcesHelper
import dev.arkbuilders.drop.domain.libwrapper.getDropApi
import dev.arkbuilders.drop.domain.model.DropFileInfo
import dev.arkbuilders.drop.domain.model.SendSession
import dev.arkbuilders.drop.domain.model.TransferStatus
import dev.arkbuilders.drop.domain.repository.SendSessionRepo
import dev.arkbuilders.drop.domain.repository.TransferSessionRepo
import dev.arkbuilders.drop.domain.usecase.SendFilesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SendSessionRepoImpl(
    private val sendUseCase: SendFilesUseCase,
    private val resourcesHelper: ResourcesHelper,
    private val transferSessionRepository: TransferSessionRepo,
) : SendSessionRepo {
    // Keep references to active sessions here so file transfers continue even if the ViewModel dies
    private val activeSessions = mutableListOf<SendSession>()
    private val activeSessionsMutex = Mutex()
    private val cancelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun sendFiles(fileUris: List<String>): SendSession? =
        withContext(Dispatchers.IO) {
            cleanupFinishedSessions()

            sendUseCase.invoke(fileUris).fold(
                onSuccess = { bubble ->
                    val subscriber =
                        getDropApi().createSendSubscriber().also { subscriber ->
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

    override suspend fun recordSendCompletion(
        fileUris: List<String>,
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
                        name = resourcesHelper.getFileName(it) ?: "",
                        size = resourcesHelper.getFileSize(it),
                    )
                }

            transferSessionRepository.addSentTransfer(
                files = filesInfo,
                peerName = receiverName,
                peerAvatar = receiverAvatar,
                status = TransferStatus.COMPLETED,
            )
        } catch (e: Exception) {
            Logger.e("Error recording send completion ${e.message}")
        }
    }

    override fun cancelSend(session: SendSession) {
        cancelScope.launch {
            try {
                activeSessionsMutex.withLock {
                    activeSessions.remove(session)
                }
                session.bubble.unsubscribe(session.subscriber)
                session.bubble.cancel()
            } catch (e: Throwable) {
                Logger.e("Error cancelling send ${e.message}")
            }
        }
    }

    private suspend fun cleanupFinishedSessions() =
        activeSessionsMutex.withLock {
            activeSessions.removeAll { it.bubble.isFinished() }
        }
}
