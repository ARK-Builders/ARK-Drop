package dev.arkbuilders.drop.domain.usecase

import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.domain.libwrapper.getDropApi
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiverConfig
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiverProfile
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReceiveFilesUseCase(
    private val profileRepo: ProfileRepo,
) {
    suspend operator fun invoke(
        ticket: String,
        confirmation: UByte,
    ): Result<DropReceiveFilesBubble> =
        withContext(Dispatchers.IO) {
            runCatching {
                Logger.d("Starting file receive with ticket: $ticket")

                val profile = profileRepo.profile.first()
                val receiverProfile =
                    DropReceiverProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                val request =
                    DropReceiveFilesRequest(
                        ticket = ticket,
                        confirmation = confirmation,
                        profile = receiverProfile,
                        config =
                            DropReceiverConfig(
                                chunkSize = 1024u * 512u,
                                parallelStreams = 4u,
                            ),
                    )

                val bubble = getDropApi().receiveFiles(request)

                Logger.d("Receive bubble created and started")
                bubble
            }.onFailure {
                Logger.e("Error starting file receive ${it.message}")
            }
        }
}
