package dev.arkbuilders.drop.data.helper

import dev.arkbuilders.drop.domain.libwrapper.send.request.DropSenderFileData

actual class ResourcesHelper {
    actual fun getFileName(uri: String): String? {
        throw NotImplementedError()
    }

    actual fun validateUris(uris: List<String>): Pair<List<String>, Int> {
        throw NotImplementedError()
    }

    actual fun getFileSize(uri: String): Long {
        throw NotImplementedError()
    }

    actual fun saveFileToDownloads(
        fileName: String,
        data: ByteArray,
    ): String? {
        throw NotImplementedError()
    }

    actual fun generateQRCode(
        ticket: String,
        confirmation: UByte,
    ): ByteArray? {
        throw NotImplementedError()
    }

    actual fun mapToSenderFileData(uri: String): DropSenderFileData {
        throw NotImplementedError()
    }
}
