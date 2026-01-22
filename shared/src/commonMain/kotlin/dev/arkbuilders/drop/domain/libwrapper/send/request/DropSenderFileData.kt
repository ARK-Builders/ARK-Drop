package dev.arkbuilders.drop.domain.libwrapper.send.request

interface DropSenderFileData {
    fun len(): ULong

    fun read(): UByte?

    fun readChunk(size: Int): ByteArray
}
