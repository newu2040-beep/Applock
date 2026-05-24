package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SecureUnlockBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LockSessionManager.clearAllSessions()
            Toast.makeText(context, "SecureUnlock background security initialized", Toast.LENGTH_SHORT).show()
        }
    }
}
