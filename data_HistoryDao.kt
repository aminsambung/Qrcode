package com.example.qrpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Delete
    suspend fun delete(item: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}