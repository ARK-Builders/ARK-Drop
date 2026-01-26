@file:OptIn(ExperimentalForeignApi::class)

package dev.arkbuilders.drop.data.helper

import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData
import dev.arkbuilders.drop.domain.libwrapper.send.SenderFileDataImpl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import kotlinx.cinterop.addressOf
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreImage.*
import platform.CoreGraphics.*
import kotlin.IllegalArgumentException

actual class ResourcesHelper {
    actual fun getFileName(uri: String): String? {
        return try {
            val url = NSURL.URLWithString(uri) ?: return null
            url.lastPathComponent
        } catch (e: Exception) {
            null
        }
    }

    actual fun validateUris(uris: List<String>): Pair<List<String>, Int> {
        val validFiles = mutableListOf<String>()
        var skippedCount = 0

        uris.forEach { uri ->
            try {
                val size = getFileSize(uri)
                if (size in 1..2_000_000_000L) { // 2GB limit
                    validFiles.add(uri)
                } else {
                    skippedCount++
                }
            } catch (_: Exception) {
                skippedCount++
            }
        }

        return validFiles to skippedCount
    }

    actual fun getFileSize(uri: String): Long {
        return try {
            val url = NSURL.URLWithString(uri) ?: return 0L
            val resourceValues = url.resourceValuesForKeys(listOf(NSURLFileSizeKey), null)
            resourceValues?.get(NSURLFileSizeKey)?.let {
                (it as? NSNumber)?.longValue ?: 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    actual fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String? {
        val uniqueName = getUniqueFileName(fileName)

        return try {
            val fileManager = NSFileManager.defaultManager
            val documentsPath = fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            ) ?: return null

            val fileURL = documentsPath.URLByAppendingPathComponent(uniqueName) ?: return null
            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }
            nsData.writeToURL(fileURL, atomically = true)

            uniqueName
        } catch (e: Exception) {
            null
        }
    }

    actual fun generateQRCode(
        ticket: String,
        confirmation: UByte,
    ): ByteArray? {
        return try {
            if (ticket.isEmpty()) {
                throw IllegalArgumentException("Ticket cannot be empty")
            }

            val qrData = "drop://receive?ticket=$ticket&confirmation=$confirmation"
            val data = qrData.encodeToNSData()

            val filter = CIFilter.filterWithName("CIQRCodeGenerator")
            filter?.setValue(data, forKey = "inputMessage")

            val outputImage = filter?.outputImage ?: return null

            // Scale up the QR code for better quality
            val extent = outputImage.extent
            val scale = 512.0 / extent.useContents { size.width }
            val transform = CGAffineTransformMakeScale(scale, scale)
            val scaledImage = outputImage.imageByApplyingTransform(transform)

            // Convert CIImage to UIImage then to PNG data
            val context = CIContext.context()
            val scaledExtent = scaledImage.extent
            val cgImage = context.createCGImage(scaledImage, fromRect = scaledExtent)
            val uiImage = UIImage.imageWithCGImage(cgImage)
            val pngData = UIImagePNGRepresentation(uiImage)

            pngData?.let {
                val length = it.length.toInt()
                val bytes = ByteArray(length)
                bytes.usePinned { pinned ->
                    it.getBytes(pinned.addressOf(0), length = length.toULong())
                }
                bytes
            }
        } catch (e: Throwable) {
            null
        }
    }

    actual fun mapToSenderFileData(uri: String): DropSenderFileData {
        return SenderFileDataImpl(uri)
    }

    private fun getUniqueFileName(originalName: String): String {
        val baseName = originalName.substringBeforeLast(".")
        val ext = originalName.substringAfterLast(".", "")
        var candidateName = originalName
        var attempt = 1

        while (true) {
            if (!doesFileExist(candidateName)) {
                return candidateName
            }

            candidateName =
                if (ext.isNotEmpty()) {
                    "$baseName($attempt).$ext"
                } else {
                    "$baseName($attempt)"
                }

            attempt++

            if (attempt > 1000) {
                val ts = NSDate().timeIntervalSince1970.toLong()
                return if (ext.isNotEmpty()) {
                    "${baseName}_$ts.$ext"
                } else {
                    "${baseName}_$ts"
                }
            }
        }
    }

    private fun doesFileExist(name: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            val documentsPath = fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null
            ) ?: return false

            val fileURL = documentsPath.URLByAppendingPathComponent(name)
            fileManager.fileExistsAtPath(fileURL?.path ?: "")
        } catch (e: Exception) {
            false
        }
    }
}

// Helper extension functions
private fun String.encodeToNSData(): NSData {
    val nsString = this as NSString
    return nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: NSData.data()
}
