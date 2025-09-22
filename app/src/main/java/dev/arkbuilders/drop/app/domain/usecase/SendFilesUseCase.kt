package dev.arkbuilders.drop.app.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.SendFilesBubble
import dev.arkbuilders.drop.SendFilesRequest
import dev.arkbuilders.drop.SenderConfig
import dev.arkbuilders.drop.SenderFile
import dev.arkbuilders.drop.SenderProfile
import dev.arkbuilders.drop.app.ProfileManager
import dev.arkbuilders.drop.app.data.SenderFileDataImpl
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.sendFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class SendFilesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileManager: ProfileManager,
    private val resourcesHelper: ResourcesHelper,
) {
    suspend operator fun invoke(
        fileUris: List<Uri>,
    ): Result<SendFilesBubble> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.d("Starting file send for ${fileUris.size} files")

            val profile = profileManager.getCurrentProfile()
            val senderProfile = SenderProfile(
                name = profile.name.ifEmpty { "Anonymous" },
                avatarB64 = profile.avatarB64.takeIf { it.isNotEmpty() }
            )

            val senderFiles = fileUris.mapNotNull { uri ->
                val fileName = resourcesHelper.getFileName(uri)
                if (fileName != null) {
                    val fileData = SenderFileDataImpl(context, uri)
                    SenderFile(
                        name = fileName,
                        data = fileData
                    )
                } else {
                    Timber.w("Could not get filename for URI: $uri")
                    null
                }
            }

            if (senderFiles.isEmpty()) {
                Timber.e("No valid files to send")
                error("No valid files to send")
            }

            val request = SendFilesRequest(
                profile = senderProfile,
                files = senderFiles,
                config = SenderConfig(
                    chunkSize = 1024u * 512u,
                    parallelStreams = 4u,
                ),
            )

            val bubble = sendFiles(request)

            Timber.d("Send bubble created with ticket and confirmation: ${bubble.getTicket()} ${bubble.getConfirmation()}")
            bubble
        }.onFailure {
            Timber.e("Error starting file send ${it.message}")
        }
    }
}