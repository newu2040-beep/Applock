package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LockSessionManager {
    private val unlockedPackages = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    
    private val _currentlyLockingPackage = MutableStateFlow<String?>(null)
    val currentlyLockingPackage: StateFlow<String?> = _currentlyLockingPackage

    // Interactive preferences (backed by runtime switches contextually)
    var autoRelockOnScreenOff: Boolean = true
    var autoRelockOnAppClose: Boolean = true
    var isStealthMode: Boolean = false
    var isFakeCrashEnabled: Boolean = false
    var activeThemeName: String = "Immersive UI"
    var isDarkTheme: Boolean = true
    var isAmoledMode: Boolean = false
    var failAttemptsCount: Int = 0
    val maxFailedAttemptsBeforeCapture = 3
    var silentCaptureEnabled: Boolean = true
    var isLockSystemAppsEnabled: Boolean = true

    fun setLockingPackage(packageName: String?) {
        _currentlyLockingPackage.value = packageName
    }

    fun isAppUnlocked(packageName: String): Boolean {
        return unlockedPackages.contains(packageName)
    }

    fun unlockApp(packageName: String) {
        unlockedPackages.add(packageName)
    }

    fun relockApp(packageName: String) {
        unlockedPackages.remove(packageName)
    }

    fun clearAllSessions() {
        unlockedPackages.clear()
    }
}
