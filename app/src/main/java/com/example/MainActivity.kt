package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.service.LockSessionManager
import com.example.ui.AppDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppLockViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: AppLockViewModel by viewModels()

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                if (LockSessionManager.autoRelockOnScreenOff) {
                    LockSessionManager.clearAllSessions()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register screen off receiver dynamically for active session clearing
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }

        setContent {
            MyApplicationTheme {
                AppDashboardScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}
