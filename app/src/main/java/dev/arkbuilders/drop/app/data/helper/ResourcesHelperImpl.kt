package dev.arkbuilders.drop.app.data.helper

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import timber.log.Timber

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
                    } else null
                }
        } catch (e: Exception) {
            Timber.Forest.e(e, "Error getting filename for URI: $uri")
            null
        }
    }

    override fun validateUris(uris: List<String>): Pair<List<String>, Int> {
        val validFiles = mutableListOf<String>()
        var skippedCount = 0

        uris.forEach { uri ->
            try {
                val size = getFileSize(uri)
                if (size > 0 && size <= 2_000_000_000L) { // 2GB limit
                    validFiles.add(uri)
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
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
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

}