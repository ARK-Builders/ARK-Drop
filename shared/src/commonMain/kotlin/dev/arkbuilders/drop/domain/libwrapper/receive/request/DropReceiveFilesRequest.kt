package dev.arkbuilders.drop.domain.libwrapper.receive.request

data class DropReceiveFilesRequest(
    val ticket: String,
    val confirmation: UByte,
    val profile: DropReceiverProfile,
    val config: DropReceiverConfig,
)
