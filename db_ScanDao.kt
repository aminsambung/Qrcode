package com.example.qrpro.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(item: ScanHistory): Long

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): ScanHistory?

    @Delete
    suspend fun delete(item: ScanHistory)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}