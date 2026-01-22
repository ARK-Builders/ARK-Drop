package dev.arkbuilders.drop.data.helper

import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData

expect class ResourcesHelper {
    fun getFileName(uri: String): String?

    fun validateUris(uris: List<String>): Pair<List<String>, Int>

    fun getFileSize(uri: String): Long

    fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String?

    fun generateQRCode(
        ticket: String,
        confirmation: UByte,
    ): ByteArray?

    fun mapToSenderFileData(uri: String): DropSenderFileData
}
