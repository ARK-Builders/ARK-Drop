package dev.arkbuilders.drop.domain.libwrapper.send

import android.content.Context
import android.net.Uri
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData
import timber.log.Timber
import java.io.InputStream

class SenderFileDataImpl(
    private val context: Context,
    private val uri: Uri,
) : DropSenderFileData {
    companion object {
        private const val TAG = "SenderFileDataImpl"
    }

    private var inputStream: InputStream? = null
    private var totalLength: ULong = 0UL
    private var isInitialized = false

    private fun initialize() {
        if (isInitialized) return

        try {
            // Get file size
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        totalLength = cursor.getLong(sizeIndex).toULong()
                    }
                }
            }

            // Open input stream
            inputStream = context.contentResolver.openInputStream(uri)
            isInitialized = true

            Timber.tag(TAG).d("Initialized SenderFileData for URI: $uri, size: $totalLength")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize SenderFileData")
        }
    }

    override fun len(): ULong {
        initialize()
        return totalLength
    }

    override fun read(): UByte? {
        initialize()
        return try {
            val byte = inputStream?.read()
            if (byte == -1) {
                inputStream?.close()
                null
            } else {
                byte?.toUByte()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading byte")
            null
        }
    }

    override fun readChunk(size: Int): ByteArray {
        initialize()
        return try {
            var size = size
            inputStream?.available()?.let {
                if (it == 0) {
                    inputStream?.close()
                    return ByteArray(0)
                }
                if (it < size) {
                    size = it
                }
            }
            val bytes = ByteArray(size)
            inputStream?.read(bytes) ?: 0
            bytes
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading chunk of size $size")
            ByteArray(0)
        }
    }
}
