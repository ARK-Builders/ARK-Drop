package dev.arkbuilders.drop.domain.libwrapper.receive

data class DropReceivingProgress(
    val isConnected: Boolean = false,
    val senderName: String = "",
    val senderAvatar: String? = null,
    val files: List<ReceiveFileInfo> = emptyList(),
    val fileProgress: Map<String, FileProgressInfo> = emptyMap(),
)

data class ReceiveFileInfo(
    val id: String,
    val name: String,
    val size: ULong,
)

data class FileProgressInfo(
    val receivedBytes: Long = 0L,
    val isComplete: Boolean = false,
)
