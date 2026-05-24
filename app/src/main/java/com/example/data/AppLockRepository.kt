package com.example.data

import kotlinx.coroutines.flow.Flow

class AppLockRepository(private val dao: SecureUnlockDao) {
    val lockedApps: Flow<List<LockedApp>> = dao.getLockedAppsFlow()
    val intruderLogs: Flow<List<IntruderLog>> = dao.getIntruderLogsFlow()

    suspend fun getLockedAppsSync(): List<LockedApp> {
        return dao.getLockedAppsSync()
    }

    suspend fun lockApp(packageName: String, appName: String) {
        dao.insertLockedApp(LockedApp(packageName, appName, isLocked = true))
    }

    suspend fun unlockApp(app: LockedApp) {
        dao.deleteLockedApp(app)
    }

    suspend fun addIntruderLog(log: IntruderLog) {
        dao.insertIntruderLog(log)
    }

    suspend fun deleteIntruderLogById(id: Int) {
        dao.deleteIntruderLogById(id)
    }

    suspend fun clearAllIntruderLogs() {
        dao.clearAllIntruderLogs()
    }
}
