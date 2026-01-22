package dev.arkbuilders.drop.domain.libwrapper.send.request

data class DropSendFilesRequest(
    var profile: DropSenderProfile,
    var files: List<DropSenderFile>,
    var config: DropSenderConfig?,
)
