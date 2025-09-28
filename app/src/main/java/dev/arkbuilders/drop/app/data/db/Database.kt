package dev.arkbuilders.drop.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.arkbuilders.drop.app.data.db.dao.TransferHistoryItemDao
import dev.arkbuilders.drop.app.data.db.entity.TransferHistoryItemEntity
import dev.arkbuilders.drop.app.data.db.typeconverters.OffsetDateTimeTypeConverter

@androidx.room.Database(
    entities = [
        TransferHistoryItemEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    OffsetDateTimeTypeConverter::class,
)
abstract class Database : RoomDatabase() {

    abstract fun transferHistoryDao(): TransferHistoryItemDao

    companion object {
        const val DB_NAME = "arkdrop.db"

        fun build(ctx: Context) =
            Room.databaseBuilder(ctx, Database::class.java, DB_NAME)
                .build()
    }
}