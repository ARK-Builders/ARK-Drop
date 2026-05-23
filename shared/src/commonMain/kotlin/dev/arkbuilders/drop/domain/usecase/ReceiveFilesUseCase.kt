package dev.arkbuilders.drop.domain.usecase

import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.domain.libwrapper.getDropApi
import dev.arkbuilders.drop.domain.libwrapper.receive.DropReceiveFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiveFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiverConfig
import dev.arkbuilders.drop.domain.libwrapper.receive.request.DropReceiverProfile
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import dev.arkbuilders.drop.instrumentation.FirebaseReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReceiveFilesUseCase(
    private val profileRepo: ProfileRepo,
    private val firebaseReporter: FirebaseReporter,
) {
    suspend operator fun invoke(
        ticket: String,
        confirmation: UByte,
    ): Result<DropReceiveFilesBubble> =
        withContext(Dispatchers.IO) {
            runCatching {
                firebaseReporter.setCustomKey("receive_ticket", ticket)
                firebaseReporter.log("ReceiveFilesUseCase: invoked with ticket=$ticket confirmation=$confirmation")
                Logger.d("Starting file receive with ticket: $ticket")

                val profile = profileRepo.profile.first()
                firebaseReporter.log("ReceiveFilesUseCase: profile loaded name=${profile.name}")

                val receiverProfile =
                    DropReceiverProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                // Using UInt values, converted to ULong for the config
                val chunkSize = 1024u * 512u  // UInt
                val parallelStreams = 4u       // UInt

                val request =
                    DropReceiveFilesRequest(
                        ticket = ticket,
                        confirmation = confirmation,
                        profile = receiverProfile,
                        config =
                            DropReceiverConfig(
                                chunkSize = chunkSize.toULong(),
                                parallelStreams = parallelStreams.toULong(),
                            ),
                    )

                firebaseReporter.log("ReceiveFilesUseCase: request created chunkSize=$chunkSize parallelStreams=$parallelStreams")

                val bubble = getDropApi().receiveFiles(request)

                firebaseReporter.log("ReceiveFilesUseCase: bubble created successfully")
                Logger.d("Receive bubble created and started")
                bubble
            }.onFailure { e ->
                Logger.e("Error starting file receive ${e.message}")
                firebaseReporter.recordError("ReceiveFilesUseCase: failed ticket=$ticket", e)
            }
        }
}
