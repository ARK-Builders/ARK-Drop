package dev.arkbuilders.drop.app

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun saveReceivedChunks(chunks: List<FileChunk>) {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IllegalStateException("Download directory not available")
        
        val receiveDir = downloadDir.resolve(UUID.randomUUID().toString())
        
        if (!receiveDir.exists()) {
            receiveDir.mkdirs()
        }
        
        chunks.forEach { chunk ->
            val file = receiveDir.resolve(chunk.name)
            file.appendBytes(chunk.data.map { it.toByte() }.toByteArray())
        }
    }
}

data class FileChunk(
    val name: String,
    val data: List<UByte>
)