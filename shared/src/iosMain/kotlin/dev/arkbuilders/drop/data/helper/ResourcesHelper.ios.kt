@file:OptIn(ExperimentalForeignApi::class)

package dev.arkbuilders.drop.data.helper

import dev.arkbuilders.drop.bridge.crashlytics_log
import dev.arkbuilders.drop.bridge.crashlytics_recordError
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
            val url = NSURL.fileURLWithPath(uri)
                ?: NSURL.URLWithString(uri)
                ?: return null
            val name = url.lastPathComponent
            crashlytics_log("ResourcesHelper: getFileName - uri: $uri -> name: $name")
            name
        } catch (e: Exception) {
            crashlytics_recordError("ResourcesHelper: getFileName failed for: $uri", e.message)
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
                    crashlytics_log("ResourcesHelper: validateUris - valid: $uri, size: $size bytes")
                } else {
                    crashlytics_log("ResourcesHelper: validateUris - skipped (size out of range): $uri, size: $size")
                    skippedCount++
                }
            } catch (_: Exception) {
                crashlytics_log("ResourcesHelper: validateUris - skipped (exception): $uri")
                skippedCount++
            }
        }

        crashlytics_log("ResourcesHelper: validateUris complete - valid: ${validFiles.size}, skipped: $skippedCount")
        return validFiles to skippedCount
    }

    actual fun getFileSize(uri: String): Long {
        return try {
            val url = NSURL.fileURLWithPath(uri)
                ?: NSURL.URLWithString(uri)
                ?: return 0L
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
        crashlytics_log("ResourcesHelper: saveFileToDownloads - originalName=$fileName uniqueName=$uniqueName dataSize=${data.size}")

        return try {
            val fileManager = NSFileManager.defaultManager
            val documentsPath = fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            if (documentsPath == null) {
                crashlytics_recordError("ResourcesHelper: saveFileToDownloads - failed to get documents directory", null)
                return null
            }

            val fileURL = documentsPath.URLByAppendingPathComponent(uniqueName)
            if (fileURL == null) {
                crashlytics_recordError("ResourcesHelper: saveFileToDownloads - failed to create file URL", null)
                return null
            }

            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }
            val success = nsData.writeToURL(fileURL, atomically = true)
            if (success) {
                crashlytics_log("ResourcesHelper: saveFileToDownloads - saved successfully path=$uniqueName")
            } else {
                crashlytics_recordError("ResourcesHelper: saveFileToDownloads - writeToURL returned false", null)
            }

            uniqueName
        } catch (e: Exception) {
            crashlytics_recordError("ResourcesHelper: saveFileToDownloads - exception", e.message)
            null
        }
    }

    actual fun generateQRCode(
        ticket: String,
        confirmation: UByte,
    ): ByteArray? {
        crashlytics_log("ResourcesHelper: generateQRCode - ticket: $ticket, confirmation: $confirmation")
        return try {
            if (ticket.isEmpty()) {
                crashlytics_recordError("ResourcesHelper: generateQRCode - empty ticket", null)
                throw IllegalArgumentException("Ticket cannot be empty")
            }

            val qrData = "drop://receive?ticket=$ticket&confirmation=$confirmation"
            crashlytics_log("ResourcesHelper: generateQRCode - data: $qrData")
            val data = qrData.encodeToNSData()

            val filter = CIFilter.filterWithName("CIQRCodeGenerator")
            filter?.setValue(data, forKey = "inputMessage")

            // Scale up the QR code for better quality
            val outputImage = filter?.outputImage
            if (outputImage == null) {
                crashlytics_recordError("ResourcesHelper: generateQRCode - CIFilter returned nil outputImage", null)
                return null
            }

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

            val result = pngData?.let {
                val length = it.length.toInt()
                val bytes = ByteArray(length)
                bytes.usePinned { pinned ->
                    it.getBytes(pinned.addressOf(0), length = length.toULong())
                }
                crashlytics_log("ResourcesHelper: generateQRCode - success, PNG size: $length bytes")
                bytes
            }

            if (result == null) {
                crashlytics_recordError("ResourcesHelper: generateQRCode - PNG conversion returned nil", null)
            }

            result
        } catch (e: Throwable) {
            crashlytics_recordError("ResourcesHelper: generateQRCode - exception", e.message)
            null
        }
    }

    actual fun mapToSenderFileData(uri: String): DropSenderFileData {
        crashlytics_log("ResourcesHelper: mapToSenderFileData - uri: $uri")
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
