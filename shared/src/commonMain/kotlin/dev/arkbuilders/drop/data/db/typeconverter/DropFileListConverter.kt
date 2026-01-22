package dev.arkbuilders.drop.data.db.typeconverter

import androidx.room.TypeConverter
import dev.arkbuilders.drop.domain.model.DropFileInfo
import kotlinx.serialization.json.Json

object DropFileListConverter {
    @TypeConverter
    fun fromList(list: List<DropFileInfo>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun toList(data: String): List<DropFileInfo> {
        return Json.decodeFromString(data)
    }
}
