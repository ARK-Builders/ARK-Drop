package dev.arkbuilders.drop.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import dev.arkbuilders.drop.data.db.dao.TransferSessionDao
import dev.arkbuilders.drop.data.db.entity.RoomTransferSession
import dev.arkbuilders.drop.data.db.typeconverter.DropFileListConverter

@Database(entities = [RoomTransferSession::class], version = 1)
@ConstructedBy(DropDatabaseConstructor::class)
@TypeConverters(
    DropFileListConverter::class,
)
abstract class DropDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferSessionDao

    companion object {
        const val DB_NAME = "drop.db"
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object DropDatabaseConstructor : RoomDatabaseConstructor<DropDatabase> {
    override fun initialize(): DropDatabase
}
