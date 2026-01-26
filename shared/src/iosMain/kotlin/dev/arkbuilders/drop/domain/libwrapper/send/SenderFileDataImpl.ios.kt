
@file:OptIn(ExperimentalForeignApi::class)
package dev.arkbuilders.drop.domain.libwrapper.send

import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import platform.Foundation.*

class SenderFileDataImpl(
    private val uri: String,
) : DropSenderFileData {
    companion object {
        private const val TAG = "SenderFileDataImpl"
    }

    private var inputStream: NSInputStream? = null
    private var totalLength: ULong = 0UL
    private var isInitialized = false

    private fun initialize() {
        if (isInitialized) return

        try {
            val url = NSURL.URLWithString(uri) ?: return

            // Get file size
            val resourceValues = url.resourceValuesForKeys(listOf(NSURLFileSizeKey), null)
            resourceValues?.get(NSURLFileSizeKey)?.let {
                totalLength = ((it as? NSNumber)?.longValue ?: 0L).toULong()
            }

            // Open input stream
            inputStream = NSInputStream.inputStreamWithURL(url)
            inputStream?.open()
            isInitialized = true
        } catch (e: Exception) {
            // Handle error
        }
    }

    override fun len(): ULong {
        initialize()
        return totalLength
    }

    override fun read(): UByte? {
        initialize()
        return try {
            val buffer = UByteArray(1)
            val bytesRead = buffer.usePinned { pinned ->
                inputStream?.read(pinned.addressOf(0).reinterpret(), maxLength = 1u)?.toLong() ?: 0L
            }
            if (bytesRead == 0L) {
                inputStream?.close()
                null
            } else {
                buffer[0]
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun readChunk(size: Int): ByteArray {
        initialize()
        return try {
            val buffer = UByteArray(size)
            val bytesRead = buffer.usePinned { pinned ->
                inputStream?.read(pinned.addressOf(0).reinterpret(), maxLength = size.toULong())?.toLong() ?: 0L
            }
            if (bytesRead == 0L) {
                inputStream?.close()
                ByteArray(0)
            } else {
                buffer.asByteArray().copyOf(bytesRead.toInt())
            }
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}
