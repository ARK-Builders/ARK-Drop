package dev.arkbuilders.drop.domain.libwrapper.send.request

data class DropSenderConfig(
    var chunkSize: ULong,
    var parallelStreams: ULong,
)
