package dev.arkbuilders.drop.domain.libwrapper.receive.request

data class DropReceiverConfig(
    var chunkSize: ULong,
    var parallelStreams: ULong,
)
