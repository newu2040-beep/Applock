package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object CamouflageHelper {
    fun applyCamouflage(context: Context, key: String) {
        val pm = context.packageManager
        val packageName = context.packageName

        // Map key options to manifest alias components
        val components = listOf(
            "com.example.LauncherDefault" to "SecureUnlock",
            "com.example.LauncherCalculator" to "Calculator",
            "com.example.LauncherCompass" to "Compass",
            "com.example.LauncherNotepad" to "NotePad",
            "com.example.LauncherFiles" to "FilesViewer"
        )

        components.forEach { (compName, compKey) ->
            val isTarget = compKey.equals(key, ignoreCase = true)
            val state = if (isTarget) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(packageName, compName),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                // Squelch background errors gracefully
            }
        }
    }
}
