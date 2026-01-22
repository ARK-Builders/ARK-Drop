package dev.arkbuilders.drop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.arkbuilders.drop.data.db.entity.RoomTransferSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferSessionDao {
    @Query("SELECT * FROM RoomTransferSession ORDER BY timestamp DESC")
    fun getAll(): Flow<List<RoomTransferSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RoomTransferSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RoomTransferSession>)

    @Query("DELETE FROM RoomTransferSession WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM RoomTransferSession")
    suspend fun clear()
}
