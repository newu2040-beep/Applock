package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_BREACH_ID = "security_breach_alerts"
    const val CHANNEL_SHIELD_ID = "shield_status_monitor"
    const val CHANNEL_INSIGHTS_ID = "security_insights"

    const val NOTIFICATION_ID_BREACH = 1001
    const val NOTIFICATION_ID_SHIELD = 1002
    const val NOTIFICATION_ID_INSIGHTS = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High priority channel for real-time security threats
            val breachChannel = NotificationChannel(
                CHANNEL_BREACH_ID,
                "Intruder & Security Breach Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends instant alerts when unauthorized biometric access attempts are intercepted."
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Low priority channel for passive shield state status icon
            val shieldChannel = NotificationChannel(
                CHANNEL_SHIELD_ID,
                "Shield Active Status Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows dynamic safety activity reporting that SecureUnlock is silently guarding apps."
                enableLights(false)
                setShowBadge(false)
            }

            // 3. Medium priority channel for safety reviews & weekly analytics reports
            val insightsChannel = NotificationChannel(
                CHANNEL_INSIGHTS_ID,
                "Weekly Security Insights & Analytics",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Delivers summarized safety ratings, threat indicators and backup reminders."
                enableLights(true)
                lightColor = Color.CYAN
                setShowBadge(true)
            }

            manager.createNotificationChannel(breachChannel)
            manager.createNotificationChannel(shieldChannel)
            manager.createNotificationChannel(insightsChannel)
        }
    }

    /**
     * Sends an instant notification when a lock overlay is breached/entered incorrectly.
     */
    fun sendSecurityIncidentNotification(context: Context, appName: String, failedAttempts: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_BREACH_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ PRIVACY BREACH ATTEMPTED!")
            .setContentText("Intruder failed to unlock $appName ($failedAttempts attempts). Snapped successfully.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Warning: Unauthorized access attempt intercepted on app: $appName!\n" +
                "The biometric face scanner recorded the intruder's snapshot.\n" +
                "Audit logs have been saved as secure offline tickets on device storage."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.RED)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BREACH + (1..1000).random(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Updates or fires a silent active state background service monitor notifications bar.
     */
    fun sendShieldStatusNotification(context: Context, isMonitoring: Boolean, lockedCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isMonitoring) "🛡️ SecureUnlock Security Active" else "⚠️ Shield Mode Disabled"
        val descText = if (isMonitoring) {
            "Guarding $lockedCount application packages in real-time."
        } else {
            "Tap to configure app lock pattern protection layout."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SHIELD_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(titleText)
            .setContentText(descText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(isMonitoring) // swipe-to-dismiss protection if running
            .setAutoCancel(!isMonitoring)
            .setContentIntent(pendingIntent)
            .setColor(Color.BLUE)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_SHIELD, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Dispatches weekly analytics or insights summary logs alert info updates.
     */
    fun sendWeeklyInsightsNotification(context: Context, totalBreaches: Int, highestRiskApp: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (totalBreaches > 0) {
            "Your weekly safety audit report is ready. Captured $totalBreaches incident tickets. Most target scans: $highestRiskApp."
        } else {
            "Outstanding safety shield! No intruder access violations reported this week. Keep protecting your device."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_INSIGHTS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("📊 Security Insights & Weekly Report")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Health Summary Plan:\n" +
                "$content\n\n" +
                "Tip: Keep system accessibility active to ensure direct overlay coverage for background applications."
            ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.CYAN)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_INSIGHTS, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
