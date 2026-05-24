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

    // --- FALLBACK PIN & ELITE MODE SECURITY ---
    var securePinCode: String = "1234" // Default PIN
    var isPinLockEnabled: Boolean = true
    var isEliteModeEnabled: Boolean = false // Demands 3 layers of sequential biometric/PIN clearance
    var isSystemLockEnabled: Boolean = false // Integrates Android native Keyguard locks (Password, PIN, Pattern)
    
    // --- CUSTOM WRONG ATTEMPT AUDIO ALARMS ---
    var wrongAttemptSound: String = "AI Vocal Lockdown Prompt"
    // Audio options: "Siren Alarm Threat Loop", "AI Vocal Lockdown Prompt", "Short Sci-Fi Beep Error", "Retro Arcade Buzzer", "Dramatic Nuclear Alert", "High-Frequency Sonic Sweep"

    // --- STEALTH launcher mask ---
    var appDisguiseIcon: String = "Default Shield"
    var appDisguiseName: String = "SecureUnlock Pro"
    // "Default Shield", "Simple Calculator", "Weather Forecast", "Compass Tool", "Secure Notepad", "Local Files"

    // --- CLONED SECURE APPS LIST ---
    private val clonedAppsList = java.util.Collections.synchronizedSet(mutableSetOf<String>(
        "com.whatsapp.cloned|WhatsApp Dual",
        "com.facebook.katana.cloned|Facebook Dual"
    ))

    // Customizable burglar alarm properties
    var customAlarmText: String = "Thief! Thief! Unauthorized access attempt detected!"
    var enableVoiceAlarm: Boolean = true
    var wallpaperPreset: String = "Starry Cyber Mesh"
    var customGalleryWallpaperUri: String = "" // For gallery selected photos

    fun setLockingPackage(packageName: String?) {
        _currentlyLockingPackage.value = packageName
    }

    fun getClonedApps(): List<String> {
        return clonedAppsList.toList()
    }

    fun addClonedApp(packageName: String, appName: String) {
        clonedAppsList.add("$packageName|$appName")
    }

    fun removeClonedApp(packageName: String) {
        synchronized(clonedAppsList) {
            val toRemove = clonedAppsList.filter { it.startsWith("$packageName|") }
            clonedAppsList.removeAll(toRemove.toSet())
        }
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
