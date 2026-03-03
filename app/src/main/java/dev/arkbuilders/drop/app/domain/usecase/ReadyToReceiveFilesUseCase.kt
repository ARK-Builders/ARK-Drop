package dev.arkbuilders.drop.app.domain.usecase

import dev.arkbuilders.drop.ReadyToReceiveRequest
import dev.arkbuilders.drop.ReceiverConfig
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.readyToReceive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.text.ifEmpty

class ReadyToReceiveFilesUseCase(
    private val profileRepo: ProfileRepo,
) {
    suspend operator fun invoke() =
        withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("Starting file receive")

                val profile = profileRepo.getCurrentProfile()
                val receiverProfile =
                    ReceiverProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                val request =
                    ReadyToReceiveRequest(
                        profile = receiverProfile,
                        config =
                            ReceiverConfig(
                                chunkSize = 1024u * 512u,
                                parallelStreams = 4u,
                            ),
                    )

                val bubble = readyToReceive(request)

                Timber.d(
                    "Receive bubble created with ticket and confirmation: ${
                        bubble.getTicket()
                    } ${bubble.getConfirmation()}",
                )
                bubble
            }.onFailure {
                Timber.e("Error starting file receive ${it.message}")
            }
        }
}
