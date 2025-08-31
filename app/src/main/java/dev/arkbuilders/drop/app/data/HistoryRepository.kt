package dev.arkbuilders.drop.app.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TransferHistoryItem(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val type: TransferType,
    val timestamp: Long,
    val status: TransferStatus,
    val peerName: String = "Unknown",
    val peerAvatar: String? = null,
    val fileCount: Int = 1
)

@Serializable
enum class TransferType {
    SENT, RECEIVED
}

@Serializable
enum class TransferStatus {
    COMPLETED, FAILED, CANCELLED
}

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "drop_history"
        private const val KEY_HISTORY = "transfer_history"
        private const val MAX_HISTORY_ITEMS = 100
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _historyItems = MutableStateFlow(loadHistory())
    val historyItems: StateFlow<List<TransferHistoryItem>> = _historyItems.asStateFlow()

    private fun loadHistory(): List<TransferHistoryItem> {
        return try {
            val historyJson = prefs.getString(KEY_HISTORY, null)
            if (historyJson != null) {
                json.decodeFromString<List<TransferHistoryItem>>(historyJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(items: List<TransferHistoryItem>) {
        try {
            val historyJson = json.encodeToString(items)
            prefs.edit().putString(KEY_HISTORY, historyJson).apply()
            _historyItems.value = items
        } catch (e: Exception) {
            // Handle serialization error
        }
    }

    fun addSentTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int = 1,
        status: TransferStatus = TransferStatus.COMPLETED
    ) {
        val newItem = TransferHistoryItem(
            id = generateId(),
            fileName = if (fileCount > 1) "$fileName and ${fileCount - 1} more" else fileName,
            fileSize = fileSize,
            type = TransferType.SENT,
            timestamp = System.currentTimeMillis(),
            status = status,
            peerName = peerName,
            peerAvatar = peerAvatar,
            fileCount = fileCount
        )
        
        addHistoryItem(newItem)
    }

    fun addReceivedTransfer(
        fileName: String,
        fileSize: Long,
        peerName: String,
        peerAvatar: String?,
        fileCount: Int = 1,
        status: TransferStatus = TransferStatus.COMPLETED
    ) {
        val newItem = TransferHistoryItem(
            id = generateId(),
            fileName = if (fileCount > 1) "$fileName and ${fileCount - 1} more" else fileName,
            fileSize = fileSize,
            type = TransferType.RECEIVED,
            timestamp = System.currentTimeMillis(),
            status = status,
            peerName = peerName,
            peerAvatar = peerAvatar,
            fileCount = fileCount
        )
        
        addHistoryItem(newItem)
    }

    private fun addHistoryItem(item: TransferHistoryItem) {
        val currentItems = _historyItems.value.toMutableList()
        currentItems.add(0, item) // Add to beginning (most recent first)
        
        // Keep only the most recent items
        if (currentItems.size > MAX_HISTORY_ITEMS) {
            currentItems.removeAt(currentItems.size - 1)
        }
        
        saveHistory(currentItems)
    }

    fun deleteHistoryItem(itemId: String) {
        val currentItems = _historyItems.value.toMutableList()
        currentItems.removeAll { it.id == itemId }
        saveHistory(currentItems)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    private fun generateId(): String {
        return "${System.currentTimeMillis()}_${(0..999).random()}"
    }
}
