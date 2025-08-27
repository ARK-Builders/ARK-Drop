package dev.arkbuilders.drop.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import dev.arkbuilders.drop.SenderFileData
import java.io.InputStream

class SenderFileDataImpl(
    private val context: Context, private val uri: Uri
) : SenderFileData {

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

            Log.d(TAG, "Initialized SenderFileData for URI: $uri, size: $totalLength")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SenderFileData", e)
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
            Log.e(TAG, "Error reading byte", e)
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
            Log.e(TAG, "Error reading chunk of size $size", e)
            ByteArray(0)
        }
    }
}
