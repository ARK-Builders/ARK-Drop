package dev.arkbuilders.drop.app.domain.usecase

import android.content.Context
import android.net.Uri
import dev.arkbuilders.drop.SendFilesToBubble
import dev.arkbuilders.drop.SendFilesToRequest
import dev.arkbuilders.drop.SenderConfig
import dev.arkbuilders.drop.SenderFile
import dev.arkbuilders.drop.SenderProfile
import dev.arkbuilders.drop.app.data.SenderFileDataImpl
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import dev.arkbuilders.drop.app.domain.repository.ProfileRepo
import dev.arkbuilders.drop.sendFilesTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.text.ifEmpty

class SendFilesToUseCase(
    private val context: Context,
    private val profileRepo: ProfileRepo,
    private val resourcesHelper: ResourcesHelper,
) {
    suspend operator fun invoke(
        ticket: String,
        confirmation: UByte,
        fileUris: List<Uri>,
    ): Result<SendFilesToBubble> =
        withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("Starting file send for ${fileUris.size} files")

                val profile = profileRepo.getCurrentProfile()
                val senderProfile =
                    SenderProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                val senderFiles =
                    fileUris.mapNotNull { uri ->
                        val fileName = resourcesHelper.getFileName(uri.toString())
                        if (fileName != null) {
                            val fileData = SenderFileDataImpl(context, uri)
                            SenderFile(
                                name = fileName,
                                data = fileData,
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

                val request =
                    SendFilesToRequest(
                        ticket = ticket,
                        confirmation = confirmation,
                        profile = senderProfile,
                        files = senderFiles,
                        config =
                            SenderConfig(
                                chunkSize = 1024u * 512u,
                                parallelStreams = 4u,
                            ),
                    )

                val bubble = sendFilesTo(request)

                Timber.d("Send bubble created and started")
                bubble
            }.onFailure {
                Timber.e("Error starting file send ${it.message}")
            }
        }
}
