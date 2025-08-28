package dev.arkbuilders.drop.app.data

import android.util.Log
import dev.arkbuilders.drop.ReceiveFilesConnectingEvent
import dev.arkbuilders.drop.ReceiveFilesReceivingEvent
import dev.arkbuilders.drop.ReceiveFilesSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ReceivingProgress(
    val isConnected: Boolean = false,
    val senderName: String = "",
    val senderAvatar: String? = null,
    val files: List<ReceiveFileInfo> = emptyList(),
    val fileProgress: Map<String, FileProgressInfo> = emptyMap()
)

data class ReceiveFileInfo(
    val id: String, 
    val name: String, 
    val size: ULong
)

data class FileProgressInfo(
    val receivedBytes: Long = 0L,
    val isComplete: Boolean = false
)

class ReceiveFilesSubscriberImpl : ReceiveFilesSubscriber {

    companion object {
        private const val TAG = "ReceiveFilesSubscriber"
    }

    private val id = UUID.randomUUID().toString()

    // Thread-safe storage for received data using ByteArrayOutputStream for efficient appending
    private val receivedDataStreams = ConcurrentHashMap<String, ByteArrayOutputStream>()
    
    private val _progress = MutableStateFlow(ReceivingProgress())
    val progress: StateFlow<ReceivingProgress> = _progress.asStateFlow()

    override fun getId(): String = id

    override fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun notifyReceiving(event: ReceiveFilesReceivingEvent) {
        Log.d(TAG, "Receiving data for file: ${event.id}, data size: ${event.data.size}")

        // Get or create ByteArrayOutputStream for this file
        val stream = receivedDataStreams.getOrPut(event.id) { ByteArrayOutputStream() }
        
        // Efficiently append data to the stream
        synchronized(stream) {
            stream.write(event.data)
        }
        
        // Find the file info to get expected size
        val currentProgress = _progress.value
        val fileInfo = currentProgress.files.find { it.id == event.id }
        
        if (fileInfo != null) {
            val receivedBytes = stream.size().toLong()
            val isComplete = receivedBytes.toULong() >= fileInfo.size
            
            // Update progress with new file progress info
            val updatedFileProgress = currentProgress.fileProgress.toMutableMap()
            updatedFileProgress[event.id] = FileProgressInfo(
                receivedBytes = receivedBytes,
                isComplete = isComplete
            )
            
            // Emit new state
            _progress.value = currentProgress.copy(
                fileProgress = updatedFileProgress.toMap()
            )
            
            if (isComplete) {
                Log.d(TAG, "File ${fileInfo.name} completed: $receivedBytes bytes")
            }
        }
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
        // Clear all data streams
        receivedDataStreams.clear()
        _progress.value = ReceivingProgress()
    }

    /**
     * Get files that have been completely received
     */
    fun getCompleteFiles(): List<Pair<ReceiveFileInfo, ByteArray>> {
        val currentProgress = _progress.value
        return currentProgress.files.mapNotNull { fileInfo ->
            val progressInfo = currentProgress.fileProgress[fileInfo.id]
            if (progressInfo?.isComplete == true) {
                val stream = receivedDataStreams[fileInfo.id]
                if (stream != null) {
                    synchronized(stream) {
                        val data = stream.toByteArray()
                        Pair(fileInfo, data)
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
    
    /**
     * Get progress for a specific file (0.0 to 1.0)
     */
    fun getFileProgress(fileId: String): Float {
        val currentProgress = _progress.value
        val fileInfo = currentProgress.files.find { it.id == fileId }
        val progressInfo = currentProgress.fileProgress[fileId]
        
        return if (fileInfo != null && progressInfo != null && fileInfo.size > 0UL) {
            (progressInfo.receivedBytes.toFloat() / fileInfo.size.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Get received bytes for a specific file
     */
    fun getReceivedBytes(fileId: String): Long {
        return _progress.value.fileProgress[fileId]?.receivedBytes ?: 0L
    }
    
    /**
     * Check if all files are complete
     */
    public fun areAllFilesComplete(): Boolean {
        val currentProgress = _progress.value
        return currentProgress.files.isNotEmpty() && 
               currentProgress.files.all { file ->
                   currentProgress.fileProgress[file.id]?.isComplete == true
               }
    }
}
