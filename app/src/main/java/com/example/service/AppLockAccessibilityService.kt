package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.AppDatabase
import com.example.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var cachedLockedPackages = setOf<String>()
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Continuously cache locked apps from database to avoid database locking on window changes
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.dao.getLockedAppsFlow().collect { list ->
                cachedLockedPackages = list.map { it.packageName }.toSet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val currentAppName = event.className?.toString() ?: ""

        // Skip our own app or launcher UI to avoid infinite redirect loops
        val ourPackage = packageName == packageNameContext()
        val isLauncher = packageName.contains("launcher") || packageName.contains("home") || currentAppName.contains("Launcher")

        if (ourPackage) {
            lastForegroundPackage = packageName
            return
        }

        if (isLauncher) {
            // Relock closed apps if user navigated back to Home Screen
            if (LockSessionManager.autoRelockOnAppClose && lastForegroundPackage != null) {
                val prevPkg = lastForegroundPackage!!
                if (cachedLockedPackages.contains(prevPkg) && LockSessionManager.isAppUnlocked(prevPkg)) {
                    LockSessionManager.relockApp(prevPkg)
                }
            }
            lastForegroundPackage = null
            return
        }

        // Check if package is locked and NOT unlocked in the current session
        if (cachedLockedPackages.contains(packageName)) {
            if (!LockSessionManager.isAppUnlocked(packageName)) {
                LockSessionManager.setLockingPackage(packageName)
                val intent = Intent(this, LockOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("target_package", packageName)
                }
                startActivity(intent)
            } else {
                // If the user navigates from unlocked App A to unlocked App B, App A remains unlocked unless they go Home or close it
                if (lastForegroundPackage != null && lastForegroundPackage != packageName) {
                    val prevPkg = lastForegroundPackage!!
                    if (LockSessionManager.autoRelockOnAppClose && cachedLockedPackages.contains(prevPkg)) {
                        LockSessionManager.relockApp(prevPkg)
                    }
                }
            }
            lastForegroundPackage = packageName
        } else {
            // User navigated to an unprotected app: relock the previously open locked app
            if (LockSessionManager.autoRelockOnAppClose && lastForegroundPackage != null) {
                val prevPkg = lastForegroundPackage!!
                if (cachedLockedPackages.contains(prevPkg) && LockSessionManager.isAppUnlocked(prevPkg)) {
                    LockSessionManager.relockApp(prevPkg)
                }
            }
            lastForegroundPackage = packageName
        }
    }

    override fun onInterrupt() {
        // Accessibility interrupts handler
    }

    private fun packageNameContext(): String {
        return applicationContext.packageName
    }
}
