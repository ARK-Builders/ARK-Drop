package dev.arkbuilders.drop.domain.usecase

import co.touchlab.kermit.Logger
import dev.arkbuilders.drop.data.helper.ResourcesHelper
import dev.arkbuilders.drop.domain.libwrapper.getDropApi
import dev.arkbuilders.drop.domain.libwrapper.send.DropSendFilesBubble
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSendFilesRequest
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderConfig
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFile
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderProfile
import dev.arkbuilders.drop.domain.repository.ProfileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SendFilesUseCase(
    private val profileRepo: ProfileRepo,
    private val resourcesHelper: ResourcesHelper,
) {
    suspend operator fun invoke(fileUris: List<String>): Result<DropSendFilesBubble> =
        withContext(Dispatchers.IO) {
            runCatching {
                Logger.d("Starting file send for ${fileUris.size} files")

                val profile = profileRepo.profile.first()
                val senderProfile =
                    DropSenderProfile(
                        name = profile.name.ifEmpty { "Anonymous" },
                        avatarB64 = profile.avatar.base64.takeIf { it.isNotEmpty() },
                    )

                val senderFiles =
                    fileUris.mapNotNull { uri ->
                        val fileName = resourcesHelper.getFileName(uri)
                        if (fileName != null) {
                            val fileData = resourcesHelper.mapToSenderFileData(uri)
                            DropSenderFile(
                                name = fileName,
                                data = fileData,
                            )
                        } else {
                            Logger.w("Could not get filename for URI: $uri")
                            null
                        }
                    }

                if (senderFiles.isEmpty()) {
                    Logger.e("No valid files to send")
                    error("No valid files to send")
                }

                // Using UInt values, converted to ULong for the config
                val chunkSize = 1024u * 512u  // UInt
                val parallelStreams = 4u       // UInt
                
                val request =
                    DropSendFilesRequest(
                        profile = senderProfile,
                        files = senderFiles,
                        config =
                            DropSenderConfig(
                                chunkSize = chunkSize.toULong(),
                                parallelStreams = parallelStreams.toULong(),
                            ),
                    )

                val bubble: DropSendFilesBubble = getDropApi().sendFiles(request)

                Logger.d(
                    "Send bubble created with ticket and confirmation: ${
                        bubble.getTicket()
                    } ${bubble.getConfirmation()}",
                )
                bubble
            }.onFailure {
                Logger.e("Error starting file send ${it.message}", it)
            }
        }
}
