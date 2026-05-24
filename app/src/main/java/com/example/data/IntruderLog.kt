package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_logs")
data class IntruderLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val photoPath: String?,
    val isSilent: Boolean = false,
    val attemptCount: Int = 1
)
