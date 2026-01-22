package dev.arkbuilders.drop.data.helper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import dev.arkbuilders.drop.domain.libwrapper.send.SenderFileDataImpl
import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection
import kotlin.IllegalArgumentException

actual class ResourcesHelper(
    private val context: Context,
) {
    actual fun getFileName(uri: String): String? {
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

    actual fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String? {
        val uniqueName = getUniqueFileName(fileName)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveModern(uniqueName, data)
        } else {
            saveLegacy(uniqueName, data)
        }
    }

    actual fun mapToSenderFileData(uri: String): DropSenderFileData {
        return SenderFileDataImpl(context, uri.toUri())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveModern(
        fileName: String,
        data: ByteArray,
    ): String? {
        val resolver = context.contentResolver
        val mime = getMimeType(fileName)
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/"

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null

        try {
            resolver.openOutputStream(uri)?.use { it.write(data) }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return fileName
    }

    private fun saveLegacy(
        fileName: String,
        data: ByteArray,
    ): String {
        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()

        val file = File(downloads, fileName)

        FileOutputStream(file).use { it.write(data) }

        return fileName
    }

    private fun getUniqueFileName(originalName: String): String {
        val baseName = originalName.substringBeforeLast(".")
        val ext = originalName.substringAfterLast(".", "")
        var candidateName = originalName
        var attempt = 1

        while (true) {
            if (doesFileExistInDownloads(candidateName).not()) {
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

    private fun doesFileExistInDownloads(name: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            doesFileExistModern(name)
        } else {
            doesFileExistLegacy(name)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun doesFileExistModern(name: String): Boolean {
        val resolver = context.contentResolver

        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"

        val selectionArgs =
            arrayOf(
                name,
                Environment.DIRECTORY_DOWNLOADS + "/",
            )

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            return cursor.moveToFirst()
        }

        return false
    }

    private fun doesFileExistLegacy(name: String): Boolean {
        val downloads =
            Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val file = File(downloads, name)
        return file.exists()
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

    actual fun generateQRCode(
        ticket: String,
        confirmation: UByte,
    ): ByteArray? {
        val writer = QRCodeWriter()
        try {
            if (ticket.isEmpty()) {
                throw IllegalArgumentException("Ticket cannot be empty")
            }

            val qrData = "drop://receive?ticket=$ticket&confirmation=$confirmation"
            val bitMatrix: BitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] =
                        if (bitMatrix[x, y]) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                }
            }
            return ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
        } catch (e: Throwable) {
            Timber.e("Unexpected error during QR code generation: ${e.message}")
            return null
        }
    }
}
