package com.example.qrpro.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val format: String,
    val type: String,          // "SCAN" atau "GENERATE"
    val productName: String? = null,
    val productImage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)