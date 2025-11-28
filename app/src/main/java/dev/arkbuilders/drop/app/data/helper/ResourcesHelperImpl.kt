package dev.arkbuilders.drop.app.data.helper

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import timber.log.Timber
import java.net.URLConnection

class ResourcesHelperImpl(
    private val context: Context,
) : ResourcesHelper {
    override fun getFileName(uri: String): String? {
        return try {
            context
                .contentResolver
                .query(uri.toUri(), null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting filename for URI: $uri")
            null
        }
    }

    override fun validateUris(uris: List<String>): Pair<List<String>, Int> {
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

    override fun getFileSize(uri: String): Long {
        return try {
            context.contentResolver.query(uri.toUri(), null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                } else {
                    0L
                }
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    override fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String? {
        val resolver = context.contentResolver

        val uniqueName = getUniqueFileName(fileName)
        val mime = getMimeType(uniqueName)

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null

        resolver.openOutputStream(uri)?.use { it.write(data) }

        return uniqueName
    }

    private fun getUniqueFileName(originalName: String): String {
        val resolver = context.contentResolver

        val baseName = originalName.substringBeforeLast(".")
        val ext = originalName.substringAfterLast(".", "")
        var candidateName = originalName
        var attempt = 1

        while (true) {
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, candidateName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

            val testUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (testUri != null) {
                resolver.delete(testUri, null, null)
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
                val ts = System.currentTimeMillis()
                return if (ext.isNotEmpty()) {
                    "${baseName}_$ts.$ext"
                } else {
                    "${baseName}_$ts"
                }
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".", "").lowercase()

        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let {
            return it
        }

        URLConnection.guessContentTypeFromName(fileName)?.let {
            return it
        }

        return "application/octet-stream"
    }
}
