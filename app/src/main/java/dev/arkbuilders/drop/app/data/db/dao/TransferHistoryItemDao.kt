package dev.arkbuilders.drop.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.arkbuilders.drop.app.data.db.entity.TransferHistoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryItemDao {
    @Query("SELECT * FROM TransferHistoryItemEntity ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransferHistoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransferHistoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransferHistoryItemEntity>)

    @Query("DELETE FROM TransferHistoryItemEntity WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM TransferHistoryItemEntity")
    suspend fun clear()
}