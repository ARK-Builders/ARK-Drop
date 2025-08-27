package dev.arkbuilders.drop.app.data

import android.util.Log
import dev.arkbuilders.drop.ReceiveFilesConnectingEvent
import dev.arkbuilders.drop.ReceiveFilesReceivingEvent
import dev.arkbuilders.drop.ReceiveFilesSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ReceivingProgress(
    val isConnected: Boolean = false,
    val senderName: String = "",
    val senderAvatar: String? = null,
    val files: List<ReceiveFileInfo> = emptyList(),
    val receivedData: MutableMap<String, ByteArray> = mutableMapOf()
)

data class ReceiveFileInfo(
    val id: String, val name: String, val size: ULong
)

class ReceiveFilesSubscriberImpl : ReceiveFilesSubscriber {

    companion object {
        private const val TAG = "ReceiveFilesSubscriber"
    }

    private val id = UUID.randomUUID().toString()

    private val _progress = MutableStateFlow(ReceivingProgress())
    val progress: StateFlow<ReceivingProgress> = _progress.asStateFlow()

    override fun getId(): String = id

    override fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun notifyReceiving(event: ReceiveFilesReceivingEvent) {
        Log.d(TAG, "Receiving data for file: ${event.id}, data size: ${event.data.size}")

        val receivedData = _progress.value.receivedData[event.id]
        if (receivedData == null) {
            _progress.value.receivedData.put(event.id, event.data)
        } else {
            _progress.value.receivedData.put(event.id, receivedData + event.data)
        }

        _progress.value = _progress.value
    }

    override fun notifyConnecting(event: ReceiveFilesConnectingEvent) {
        Log.d(TAG, "Connected to sender: ${event.sender.name}, files: ${event.files.size}")

        val fileInfos = event.files.map { file ->
            ReceiveFileInfo(
                id = file.id, name = file.name, size = file.len
            )
        }

        _progress.value = _progress.value.copy(
            isConnected = true,
            senderName = event.sender.name,
            senderAvatar = event.sender.avatarB64,
            files = fileInfos
        )
    }

    fun reset() {
        _progress.value = ReceivingProgress()
    }

    fun getCompleteFiles(): List<Pair<ReceiveFileInfo, ByteArray>> {
        val currentProgress = _progress.value
        return currentProgress.files.mapNotNull { fileInfo ->
            val data = currentProgress.receivedData[fileInfo.id]
            if (data != null && data.size.toULong() == fileInfo.size) {
                Pair(fileInfo, data)
            } else {
                null
            }
        }
    }
}
