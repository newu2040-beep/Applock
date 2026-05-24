package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureUnlockDao {
    @Query("SELECT * FROM locked_apps ORDER BY appName ASC")
    fun getLockedAppsFlow(): Flow<List<LockedApp>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getLockedAppsSync(): List<LockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedApp(app: LockedApp)

    @Delete
    suspend fun deleteLockedApp(app: LockedApp)

    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getIntruderLogsFlow(): Flow<List<IntruderLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderLog(log: IntruderLog)

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteIntruderLogById(id: Int)

    @Query("DELETE FROM intruder_logs")
    suspend fun clearAllIntruderLogs()
}
