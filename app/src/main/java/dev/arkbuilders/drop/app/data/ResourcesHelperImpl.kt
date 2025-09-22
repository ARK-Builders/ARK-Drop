package dev.arkbuilders.drop.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arkbuilders.drop.app.domain.ResourcesHelper
import timber.log.Timber
import javax.inject.Inject

class ResourcesHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ResourcesHelper {
    override fun getFileName(uri: Uri): String? {
        return try {
            context
                .contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    } else null
                }
        } catch (e: Exception) {
            Timber.e(e, "Error getting filename for URI: $uri")
            null
        }
    }
}