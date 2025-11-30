package dev.arkbuilders.drop.app.domain.usecase

import dev.arkbuilders.drop.ReceiveFilesBubble
import dev.arkbuilders.drop.ReceiveFilesRequest
import dev.arkbuilders.drop.ReceiverConfig
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.receiveFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ReceiveFilesUseCase(
    private val profileRepo: ProfileRepo,
) {
    suspend operator fun invoke(
        ticket: String,
        confirmation: UByte,
    ): Result<ReceiveFilesBubble> =
        withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("Starting file receive with ticket: $ticket")

                val profile = profileRepo.getCurrentProfile()
                val receiverProfile =
                    ReceiverProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                val request =
                    ReceiveFilesRequest(
                        ticket = ticket,
                        confirmation = confirmation,
                        profile = receiverProfile,
                        config =
                            ReceiverConfig(
                                chunkSize = 1024u * 512u,
                                parallelStreams = 4u,
                            ),
                    )

                val bubble = receiveFiles(request)

                Timber.d("Receive bubble created and started")
                bubble
            }.onFailure {
                Timber.e("Error starting file receive ${it.message}")
            }
        }
}
