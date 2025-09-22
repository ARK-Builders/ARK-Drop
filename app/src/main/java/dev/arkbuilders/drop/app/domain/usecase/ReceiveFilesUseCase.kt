package dev.arkbuilders.drop.app.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.ReceiveFilesBubble
import dev.arkbuilders.drop.ReceiveFilesRequest
import dev.arkbuilders.drop.ReceiverConfig
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.receiveFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ReceiveFilesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileManager: ProfileManager,
    private val resourcesHelper: ResourcesHelper,
) {
    suspend operator fun invoke(
        ticket: String,
        confirmation: UByte
    ): Result<ReceiveFilesBubble> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.d("Starting file receive with ticket: $ticket")

            val profile = profileManager.getCurrentProfile()
            val receiverProfile = ReceiverProfile(
                name = profile.name.ifEmpty { "Anonymous" },
                avatarB64 = profile.avatarB64.takeIf { it.isNotEmpty() }
            )

            val request = ReceiveFilesRequest(
                ticket = ticket,
                confirmation = confirmation,
                profile = receiverProfile,
                config = ReceiverConfig(
                    chunkSize = 1024u * 512u,
                    parallelStreams = 4u,
                )
            )

            val bubble = receiveFiles(request)

            Timber.d("Receive bubble created and started")
            bubble
        }.onFailure {
            Timber.e("Error starting file receive ${it.message}")
        }
    }
}