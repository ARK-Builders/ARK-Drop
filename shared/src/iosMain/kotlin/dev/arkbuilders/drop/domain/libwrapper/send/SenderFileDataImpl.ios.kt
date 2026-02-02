
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
            println("📁 SenderFileDataImpl: Initializing file: $uri")
            
            // Try as file path first, then as URL string
            val url = NSURL.fileURLWithPath(uri) 
                ?: NSURL.URLWithString(uri) 
                ?: run {
                    println("⚠️ SenderFileDataImpl: Failed to create URL from: $uri")
                    return
                }

            println("📁 SenderFileDataImpl: Created URL: ${url.absoluteString}")

            // Get file size
            val resourceValues = url.resourceValuesForKeys(listOf(NSURLFileSizeKey), null)
            resourceValues?.get(NSURLFileSizeKey)?.let {
                totalLength = ((it as? NSNumber)?.longValue ?: 0L).toULong()
                println("📁 SenderFileDataImpl: File size: $totalLength bytes")
            } ?: println("⚠️ SenderFileDataImpl: Could not get file size")

            // Open input stream
            inputStream = NSInputStream.inputStreamWithURL(url)
            inputStream?.open()
            
            val status = inputStream?.streamStatus
            println("📁 SenderFileDataImpl: Stream status: $status")
            
            if (inputStream?.streamError != null) {
                println("⚠️ SenderFileDataImpl: Stream error: ${inputStream?.streamError?.localizedDescription}")
                return
            }
            
            isInitialized = true
            println("✅ SenderFileDataImpl: Successfully initialized")
        } catch (e: Exception) {
            println("❌ SenderFileDataImpl: Failed to initialize file: $uri, error: $e")
        }
    }

    override fun len(): ULong {
        initialize()
        return totalLength
    }

    override fun read(): UByte? {
        initialize()
        if (!isInitialized) {
            println("⚠️ SenderFileDataImpl.read() - not initialized for $uri")
            return null
        }
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
            println("⚠️ SenderFileDataImpl.read() error for $uri: $e")
            null
        }
    }

    override fun readChunk(size: Int): ByteArray {
        initialize()
        if (!isInitialized) {
            println("⚠️ SenderFileDataImpl.readChunk() - not initialized for $uri")
            return ByteArray(0)
        }
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
            println("⚠️ SenderFileDataImpl.readChunk() error for $uri: $e")
            ByteArray(0)
        }
    }
}
