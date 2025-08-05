package dev.arkbuilders.drop.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.*
import dev.arkbuilders.drop.app.data.HistoryRepository
import dev.arkbuilders.drop.app.data.ReceiveFilesSubscriberImpl
import dev.arkbuilders.drop.app.data.SendFilesSubscriberImpl
import dev.arkbuilders.drop.app.data.SenderFileDataImpl
import dev.arkbuilders.drop.app.data.TransferStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileManager: ProfileManager,
    private val historyRepository: HistoryRepository
) {
    companion object {
        private const val TAG = "TransferManager"
    }

    private var currentSendBubble: SendFilesBubble? = null
    private var currentReceiveBubble: ReceiveFilesBubble? = null
    private var sendSubscriber: SendFilesSubscriberImpl? = null
    private var receiveSubscriber: ReceiveFilesSubscriberImpl? = null

    val sendProgress: StateFlow<dev.arkbuilders.drop.app.data.SendingProgress>?
        get() = sendSubscriber?.progress

    val receiveProgress: StateFlow<dev.arkbuilders.drop.app.data.ReceivingProgress>?
        get() = receiveSubscriber?.progress

    suspend fun sendFiles(fileUris: List<Uri>): SendFilesBubble? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting file send for ${fileUris.size} files")

            val profile = profileManager.getCurrentProfile()
            val senderProfile = SenderProfile(
                name = profile.name.ifEmpty { "Anonymous" },
                avatarB64 = profile.avatarB64.takeIf { it.isNotEmpty() }
            )

            val senderFiles = fileUris.mapNotNull { uri ->
                val fileName = getFileName(uri)
                if (fileName != null) {
                    val fileData = SenderFileDataImpl(context, uri)
                    SenderFile(
                        name = fileName,
                        data = fileData
                    )
                } else {
                    Log.w(TAG, "Could not get filename for URI: $uri")
                    null
                }
            }

            if (senderFiles.isEmpty()) {
                Log.e(TAG, "No valid files to send")
                return@withContext null
            }

            val request = SendFilesRequest(
                profile = senderProfile,
                files = senderFiles
            )

            // Create and subscribe to bubble
            val bubble = sendFiles(request)
            currentSendBubble = bubble

            // Set up subscriber
            sendSubscriber = SendFilesSubscriberImpl().also { subscriber ->
                bubble.subscribe(subscriber)
            }

            Log.d(TAG, "Send bubble created with ticket: ${bubble.getTicket()}")
            bubble

        } catch (e: Exception) {
            Log.e(TAG, "Error starting file send", e)
            null
        }
    }

    suspend fun receiveFiles(ticket: String, confirmation: UByte): ReceiveFilesBubble? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting file receive with ticket: $ticket")

            val profile = profileManager.getCurrentProfile()
            val receiverProfile = ReceiverProfile(
                name = profile.name.ifEmpty { "Anonymous" },
                avatarB64 = profile.avatarB64.takeIf { it.isNotEmpty() }
            )

            val request = ReceiveFilesRequest(
                ticket = ticket,
                confirmation = confirmation,
                profile = receiverProfile
            )

            // Create and subscribe to bubble
            val bubble = receiveFiles(request)
            currentReceiveBubble = bubble

            // Set up subscriber
            receiveSubscriber = ReceiveFilesSubscriberImpl().also { subscriber ->
                bubble.subscribe(subscriber)
            }

            // Start receiving
            bubble.start()

            Log.d(TAG, "Receive bubble created and started")
            bubble

        } catch (e: Exception) {
            Log.e(TAG, "Error starting file receive", e)
            null
        }
    }

    suspend fun saveReceivedFiles(): List<File> = withContext(Dispatchers.IO) {
        val subscriber = receiveSubscriber ?: return@withContext emptyList()
        val completeFiles = subscriber.getCompleteFiles()
        val savedFiles = mutableListOf<File>()

        try {
            completeFiles.forEach { (fileInfo, data) ->
                val savedFile = saveFileToDownloads(fileInfo.name, data)
                if (savedFile != null) {
                    savedFiles.add(savedFile)
                    Log.d(TAG, "Saved file: ${savedFile.absolutePath}")
                } else {
                    Log.e(TAG, "Failed to save file: ${fileInfo.name}")
                }
            }

            // Add to history if files were saved successfully
            if (savedFiles.isNotEmpty()) {
                val progress = receiveSubscriber?.progress?.value
                val senderName = progress?.senderName ?: "Unknown"
                val senderAvatar = progress?.senderAvatar

                val totalSize = completeFiles.sumOf { it.second.size.toLong() }
                val firstFileName = savedFiles.firstOrNull()?.name ?: "Unknown"

                historyRepository.addReceivedTransfer(
                    fileName = firstFileName,
                    fileSize = totalSize,
                    peerName = senderName,
                    peerAvatar = senderAvatar,
                    fileCount = savedFiles.size,
                    status = TransferStatus.COMPLETED
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving received files", e)

            // Add failed transfer to history
            val progress = receiveSubscriber?.progress?.value
            val senderName = progress?.senderName ?: "Unknown"
            val senderAvatar = progress?.senderAvatar

            historyRepository.addReceivedTransfer(
                fileName = "Transfer failed",
                fileSize = 0L,
                peerName = senderName,
                peerAvatar = senderAvatar,
                fileCount = completeFiles.size,
                status = TransferStatus.FAILED
            )
        }

        savedFiles
    }

    fun recordSendCompletion(fileUris: List<Uri>) {
        try {
            val progress = sendSubscriber?.progress?.value
            val receiverName = progress?.receiverName ?: "Unknown"
            val receiverAvatar = progress?.receiverAvatar

            val totalSize = fileUris.sumOf { uri ->
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                        } else 0L
                    } ?: 0L
                } catch (_: Exception) {
                    0L
                }
            }

            val firstFileName = getFileName(fileUris.firstOrNull()) ?: "Unknown"

            historyRepository.addSentTransfer(
                fileName = firstFileName,
                fileSize = totalSize,
                peerName = receiverName,
                peerAvatar = receiverAvatar,
                fileCount = fileUris.size,
                status = TransferStatus.COMPLETED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error recording send completion", e)
        }
    }

    private suspend fun saveFileToDownloads(fileName: String, data: ByteArray): File? = withContext(Dispatchers.IO) {
        try {
            // Use MediaStore for Android 10+ (Scoped Storage)
            return@withContext saveFileUsingMediaStore(fileName, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: $fileName", e)
            return@withContext null
        }
    }

    private fun saveFileUsingMediaStore(fileName: String, data: ByteArray): File? {
        try {
            val resolver = context.contentResolver

            // Generate unique filename to avoid conflicts
            val uniqueFileName = generateUniqueFileName(fileName)

            // Create content values for the file
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(uniqueFileName))
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            // Insert the file into MediaStore
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                // Write the file data
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                }

                // Get the actual file path for return
                val actualFile = getFileFromMediaStoreUri(uri, uniqueFileName)
                Log.d(TAG, "File saved using MediaStore: $uniqueFileName")
                return actualFile
            } else {
                Log.e(TAG, "Failed to create MediaStore entry for: $uniqueFileName")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file using MediaStore: $fileName", e)
            return null
        }
    }

    private fun saveFileUsingLegacyStorage(fileName: String, data: ByteArray): File? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Generate unique filename to avoid conflicts
            val uniqueFileName = generateUniqueFileNameForDirectory(downloadsDir, fileName)
            val file = File(downloadsDir, uniqueFileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(data)
                outputStream.flush()
            }

            Log.d(TAG, "File saved using legacy storage: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file using legacy storage: $fileName", e)
            return null
        }
    }

    private fun generateUniqueFileName(originalFileName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return generateUniqueFileNameForDirectory(downloadsDir, originalFileName)
    }

    private fun generateUniqueFileNameForDirectory(directory: File, originalFileName: String): String {
        val nameWithoutExt = originalFileName.substringBeforeLast(".", originalFileName)
        val extension = originalFileName.substringAfterLast(".", "")

        var counter = 1
        var candidateFileName = originalFileName
        var candidateFile = File(directory, candidateFileName)

        // Keep incrementing counter until we find a unique filename
        while (candidateFile.exists() || isFileNameInMediaStore(candidateFileName)) {
            candidateFileName = if (extension.isNotEmpty()) {
                "${nameWithoutExt}($counter).$extension"
            } else {
                "${nameWithoutExt}($counter)"
            }
            candidateFile = File(directory, candidateFileName)
            counter++

            // Safety check to prevent infinite loop
            if (counter > 1000) {
                Log.w(TAG, "Too many duplicate files, using timestamp suffix")
                val timestamp = System.currentTimeMillis()
                candidateFileName = if (extension.isNotEmpty()) {
                    "${nameWithoutExt}_$timestamp.$extension"
                } else {
                    "${nameWithoutExt}_$timestamp"
                }
                break
            }
        }

        Log.d(TAG, "Generated unique filename: $candidateFileName (original: $originalFileName)")
        return candidateFileName
    }

    private fun isFileNameInMediaStore(fileName: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(fileName, "${Environment.DIRECTORY_DOWNLOADS}/")

            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking MediaStore for filename: $fileName", e)
            false
        }
    }

    private fun getFileFromMediaStoreUri(uri: Uri, fileName: String): File {
        // For MediaStore files, we create a reference file object
        // The actual file is managed by the system
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, fileName)
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            else -> "application/octet-stream"
        }
    }

    fun cancelSend() {
        try {
            currentSendBubble?.let { bubble ->
                sendSubscriber?.let { subscriber ->
                    bubble.unsubscribe(subscriber)
                }
                // Note: cancel() is async in the UDL, but we'll call it anyway
                // bubble.cancel() // Commented out as it's async and we can't await here
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling send", e)
        } finally {
            cleanup()
        }
    }

    fun cancelReceive() {
        try {
            currentReceiveBubble?.let { bubble ->
                receiveSubscriber?.let { subscriber ->
                    bubble.unsubscribe(subscriber)
                }
                bubble.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling receive", e)
        } finally {
            cleanup()
        }
    }

    fun getCurrentSendTicket(): String? = currentSendBubble?.getTicket()

    fun getCurrentSendConfirmation(): UByte? = currentSendBubble?.getConfirmation()

    fun isSendFinished(): Boolean = currentSendBubble?.isFinished() ?: true

    fun isReceiveFinished(): Boolean = currentReceiveBubble?.isFinished() ?: true

    fun isSendConnected(): Boolean = currentSendBubble?.isConnected() ?: false

    fun cleanup() {
        sendSubscriber?.reset()
        receiveSubscriber?.reset()
        sendSubscriber = null
        receiveSubscriber = null
        currentSendBubble = null
        currentReceiveBubble = null
    }

    private fun getFileName(uri: Uri?): String? {
        if (uri == null) return null
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting filename for URI: $uri", e)
            null
        }
    }
}
