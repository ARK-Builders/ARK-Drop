package dev.arkbuilders.drop.app.domain

interface ResourcesHelper {
    fun getFileName(uri: String): String?

    fun validateUris(uris: List<String>): Pair<List<String>, Int>

    fun getFileSize(uri: String): Long

    fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String?
}
