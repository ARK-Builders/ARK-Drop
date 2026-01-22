package dev.arkbuilders.drop.domain.libwrapper.send

data class DropSendingProgress(
    val fileName: String = "",
    val sent: ULong = 0UL,
    val remaining: ULong = 0UL,
    val isConnected: Boolean = false,
    val receiverName: String = "",
    val receiverAvatar: String? = null,
)
