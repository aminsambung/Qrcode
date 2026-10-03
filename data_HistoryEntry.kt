package com.example.qrpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: String,          // "QR", "EAN_13", "UPC_A", dll
    val timestamp: Long = System.currentTimeMillis(),
    val isScanned: Boolean = true // true = scan, false = generate
)