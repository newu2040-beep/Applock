package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppLockRepository
import com.example.data.IntruderLog
import com.example.data.LockedApp
import com.example.service.LockSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppListItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isLocked: Boolean = false,
    val isGame: Boolean = false
)

class AppLockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppLockRepository
    
    // UI reactive configurations
    var selectedTab by mutableStateOf(0)
    var searchQuery = MutableStateFlow("")
    
    private val rawInstalledApps = MutableStateFlow<List<AppListItem>>(emptyList())
    
    // Settings state
    val isAmoled = MutableStateFlow(LockSessionManager.isAmoledMode)
    val isDarkTheme = MutableStateFlow(LockSessionManager.isDarkTheme)
    val activeThemeName = MutableStateFlow(LockSessionManager.activeThemeName)
    val autoRelockOnScreenOff = MutableStateFlow(LockSessionManager.autoRelockOnScreenOff)
    val autoRelockOnAppClose = MutableStateFlow(LockSessionManager.autoRelockOnAppClose)
    val isStealthMode = MutableStateFlow(LockSessionManager.isStealthMode)
    val isFakeCrashEnabled = MutableStateFlow(LockSessionManager.isFakeCrashEnabled)
    val silentCaptureEnabled = MutableStateFlow(LockSessionManager.silentCaptureEnabled)
    val isLockSystemAppsEnabled = MutableStateFlow(LockSessionManager.isLockSystemAppsEnabled)
    
    // Customizable alarm & background properties
    val customAlarmText = MutableStateFlow(LockSessionManager.customAlarmText)
    val enableVoiceAlarm = MutableStateFlow(LockSessionManager.enableVoiceAlarm)
    val wallpaperPreset = MutableStateFlow(LockSessionManager.wallpaperPreset)

    init {
        val dao = AppDatabase.getDatabase(application).dao
        repository = AppLockRepository(dao)

        // Load installed apps in background
        viewModelScope.launch {
            loadInstalledApps()
        }
    }

    val lockedApps: StateFlow<List<LockedApp>> = repository.lockedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intruderLogs: StateFlow<List<IntruderLog>> = repository.intruderLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined app list: reacts to search query and lock states in database
    val appListState: StateFlow<List<AppListItem>> = combine(
        rawInstalledApps,
        lockedApps,
        searchQuery,
        isLockSystemAppsEnabled
    ) { installed, locked, query, lockSystem ->
        val lockedSet = locked.map { it.packageName }.toSet()
        installed
            .map { app ->
                app.copy(isLocked = lockedSet.contains(app.packageName))
            }
            .filter { app ->
                // Filter out system apps if system app locking is disabled
                if (!lockSystem && app.isSystemApp) false else true
            }
            .filter { app ->
                query.isEmpty() ||
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<AppListItem> { it.isLocked }.thenBy { it.appName })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High fidelity statistics
    val securityScore: StateFlow<Int> = lockedApps.map { list ->
        val size = list.size
        when {
            size == 0 -> 25
            size < 3 -> 55
            size < 8 -> 85
            else -> 100
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    private suspend fun loadInstalledApps() {
        withContext(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val ourPackage = getApplication<Application>().packageName

            val list = mutableListOf<AppListItem>()
            for (app in packages) {
                // Do not allow locking our own app to prevent accidental lockouts
                if (app.packageName == ourPackage) continue

                // Check if application has a launcher intent
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val appName = pm.getApplicationLabel(app).toString()
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        app.category == ApplicationInfo.CATEGORY_GAME
                    } else {
                        (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                    }
                    list.add(AppListItem(app.packageName, appName, isSystem, isGame = isGame))
                }
            }

            // Fallback rich lists for Android emulators/sandboxes without complex installed packages
            if (list.size < 5) {
                val fallbacks = listOf(
                    AppListItem("com.whatsapp", "WhatsApp", isSystemApp = false),
                    AppListItem("com.instagram.android", "Instagram", isSystemApp = false),
                    AppListItem("com.google.android.apps.photos", "Google Photos", isSystemApp = true),
                    AppListItem("com.android.settings", "Settings", isSystemApp = true),
                    AppListItem("com.android.chrome", "Google Chrome", isSystemApp = true),
                    AppListItem("com.google.android.youtube", "YouTube", isSystemApp = true),
                    AppListItem("com.google.android.apps.messaging", "Gmail", isSystemApp = true),
                    AppListItem("com.google.android.contacts", "Contacts", isSystemApp = true),
                    AppListItem("com.google.android.dialer", "Phone Dialer", isSystemApp = true),
                    AppListItem("com.facebook.katana", "Facebook", isSystemApp = false),
                    AppListItem("com.twitter.android", "Twitter / X", isSystemApp = false),
                    AppListItem("com.spotify.music", "Spotify", isSystemApp = false),
                    AppListItem("com.android.vending", "Google Play Store", isSystemApp = true),
                    AppListItem("com.roblox.client", "Roblox", isSystemApp = false, isGame = true),
                    AppListItem("com.king.candycrushsaga", "Candy Crush Saga", isSystemApp = false, isGame = true),
                    AppListItem("com.kiloo.subwaysurf", "Subway Surfers", isSystemApp = false, isGame = true),
                    AppListItem("com.tencent.ig", "PUBG MOBILE", isSystemApp = false, isGame = true),
                    AppListItem("com.innersloth.spacemafia", "Among Us", isSystemApp = false, isGame = true)
                )
                for (fallback in fallbacks) {
                    if (fallback.packageName != ourPackage && list.none { it.packageName == fallback.packageName }) {
                        list.add(fallback)
                    }
                }
            }

            rawInstalledApps.value = list.sortedBy { it.appName }
        }
    }

    fun toggleAppLock(app: AppListItem) {
        viewModelScope.launch {
            if (app.isLocked) {
                // Find matching LockedApp entry
                val lockedEntry = lockedApps.value.firstOrNull { it.packageName == app.packageName }
                if (lockedEntry != null) {
                    repository.unlockApp(lockedEntry)
                }
            } else {
                repository.lockApp(app.packageName, app.appName)
            }
        }
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun deleteIntruderLog(log: IntruderLog) {
        viewModelScope.launch {
            repository.deleteIntruderLogById(log.id)
        }
    }

    fun clearAllIntruderLogs() {
        viewModelScope.launch {
            repository.clearAllIntruderLogs()
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        LockSessionManager.isAmoledMode = enabled
        isAmoled.value = enabled
    }

    fun setDarkTheme(enabled: Boolean) {
        LockSessionManager.isDarkTheme = enabled
        isDarkTheme.value = enabled
    }

    fun setTheme(themeName: String) {
        LockSessionManager.activeThemeName = themeName
        activeThemeName.value = themeName
    }

    fun setAutoRelockOnScreenOff(enabled: Boolean) {
        LockSessionManager.autoRelockOnScreenOff = enabled
        autoRelockOnScreenOff.value = enabled
    }

    fun setAutoRelockOnAppClose(enabled: Boolean) {
        LockSessionManager.autoRelockOnAppClose = enabled
        autoRelockOnAppClose.value = enabled
    }

    fun setStealthMode(enabled: Boolean) {
        LockSessionManager.isStealthMode = enabled
        isStealthMode.value = enabled
    }

    fun setFakeCrashEnabled(enabled: Boolean) {
        LockSessionManager.isFakeCrashEnabled = enabled
        isFakeCrashEnabled.value = enabled
    }

    fun setSilentCaptureEnabled(enabled: Boolean) {
        LockSessionManager.silentCaptureEnabled = enabled
        silentCaptureEnabled.value = enabled
    }

    fun setIsLockSystemAppsEnabled(enabled: Boolean) {
        LockSessionManager.isLockSystemAppsEnabled = enabled
        isLockSystemAppsEnabled.value = enabled
    }

    fun setCustomAlarmText(text: String) {
        LockSessionManager.customAlarmText = text
        customAlarmText.value = text
    }

    fun setEnableVoiceAlarm(enabled: Boolean) {
        LockSessionManager.enableVoiceAlarm = enabled
        enableVoiceAlarm.value = enabled
    }

    fun setWallpaperPreset(preset: String) {
        LockSessionManager.wallpaperPreset = preset
        wallpaperPreset.value = preset
    }

    // Helper to simulate capture for presentation / visual checks in emulator
    fun simulateIntruderLog() {
        viewModelScope.launch {
            val randomApp = listOf("WhatsApp", "Google Photos", "Settings", "Gmail", "Instagram").random()
            val timestamp = System.currentTimeMillis()
            val filename = "intruder_simulated_${timestamp}.jpg"
            val file = java.io.File(getApplication<Application>().filesDir, filename)
            
            // Use our high fidelity procedural photo generator
            com.example.util.IntruderImageUtils.generateProceduralIntruderSelfie(
                getApplication(),
                randomApp,
                file
            )

            val simulatedLog = IntruderLog(
                timestamp = timestamp,
                appName = randomApp,
                photoPath = file.absolutePath,
                isSilent = LockSessionManager.silentCaptureEnabled,
                attemptCount = (3..5).random()
            )
            repository.addIntruderLog(simulatedLog)
        }
    }
}
