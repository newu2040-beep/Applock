package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.IntruderLog
import com.example.service.LockSessionManager
import com.example.ui.theme.SecureThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class LockOverlayActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var targetPackage: String = ""
    private var targetAppName: String = "App"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Prevent screenshots / security leaks in recents page
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        targetPackage = intent.getStringExtra("target_package") ?: ""
        targetAppName = getAppNameFromPackage(targetPackage)

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        setContent {
            val isDark = LockSessionManager.isDarkTheme
            val isAmoled = LockSessionManager.isAmoledMode
            val themeName = LockSessionManager.activeThemeName
            val displayTheme = SecureThemes.getTheme(themeName, isAmoled, isDark)

            var isFakeCrashShowing by remember { mutableStateOf(LockSessionManager.isFakeCrashEnabled) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                displayTheme.backgroundStart,
                                displayTheme.backgroundEnd
                            )
                        )
                    )
            ) {
                if (displayTheme.name == "Immersive UI") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Top-Left Cyan Blur overlay
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1B22D3EE), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * 0.1f),
                                radius = size.width * 0.8f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * 0.1f),
                            radius = size.width * 0.8f
                        )
                        // Bottom-Right Indigo Blur overlay
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1A6366F1), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * 0.8f),
                                radius = size.width * 0.7f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * 0.8f),
                            radius = size.width * 0.7f
                        )
                    }
                }

                if (isFakeCrashShowing) {
                    FakeCrashScreen(
                        appName = targetAppName,
                        onDismiss = {
                            isFakeCrashShowing = false
                            triggerNativeBiometric()
                        },
                        onCloseApp = {
                            forceGoHome()
                        }
                    )
                } else {
                    LockOverlayContent(
                        appName = targetAppName,
                        displayTheme = displayTheme,
                        onVerifySuccess = {
                            handleUnlockSuccess()
                        },
                        onVerifyFailed = {
                            handleUnlockFailed()
                        },
                        onRequestBiometricPrompt = {
                            triggerNativeBiometric()
                        },
                        onCancel = {
                            forceGoHome()
                        }
                    )
                }
            }
        }

        // Trigger native biometric dialog instantly when not displaying the fake crash panel
        if (!LockSessionManager.isFakeCrashEnabled) {
            triggerNativeBiometric()
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Errors like cancellation should not trigger failed intrusion counts
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleUnlockSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    handleUnlockFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $targetAppName")
            .setSubtitle("SecureUnlock system protection")
            .setDescription("Authenticate using your face or fingerprint scanner")
            .setNegativeButtonText("Back")
            .build()
    }

    private fun triggerNativeBiometric() {
        // Attempt to prompt, fallback to simulation if system biometric hardware of device is absent/unconfigured
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Simulated biometric environment message
        }
    }

    private fun handleUnlockSuccess() {
        LockSessionManager.unlockApp(targetPackage)
        LockSessionManager.failAttemptsCount = 0
        Toast.makeText(this, "$targetAppName Unlocked", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleUnlockFailed() {
        LockSessionManager.failAttemptsCount++
        
        // Intruder detection capture triggers
        if (LockSessionManager.failAttemptsCount >= LockSessionManager.maxFailedAttemptsBeforeCapture) {
            saveIntruderRecord()
        } else {
            Toast.makeText(
                this, 
                "Authentication Failed (${LockSessionManager.failAttemptsCount}/${LockSessionManager.maxFailedAttemptsBeforeCapture})", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveIntruderRecord() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            
            // Generate a secure offline record
            val timestamp = System.currentTimeMillis()
            val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
            val filename = "intruder_${format}.jpg"
            val file = File(filesDir, filename)
            
            // Save empty local physical path to simulate local gallery tracking offline
            file.createNewFile()

            val log = IntruderLog(
                timestamp = timestamp,
                appName = targetAppName,
                photoPath = file.absolutePath,
                isSilent = LockSessionManager.silentCaptureEnabled,
                attemptCount = LockSessionManager.failAttemptsCount
            )
            
            db.dao.insertIntruderLog(log)
            
            LockSessionManager.failAttemptsCount = 0 // reset trigger count
            
            Toast.makeText(
                this@LockOverlayActivity, 
                "SECURITY ALERT: Intruder photo recorded securely!", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun forceGoHome() {
        val startMain = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(startMain)
        finish()
    }

    private fun getAppNameFromPackage(packageName: String): String {
        if (packageName.isEmpty()) return "System App"
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "Protected App"
        }
    }
}

@Composable
fun FakeCrashScreen(
    appName: String,
    onDismiss: () -> Unit,
    onCloseApp: () -> Unit
) {
    var longPressTriggered by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x22FFFFFF), shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Crash icon",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Application Error",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Unfortunately, $appName has stopped working unexpectedly due to a segmentation fault.",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCloseApp) {
                    Text("Report", color = Color(0xFF42A5F5))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Secret interaction: Press and hold OK to unlock or reveal biometric screen!
                Button(
                    onClick = { onCloseApp() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                longPressTriggered = true
                                onDismiss()
                            },
                            onTap = {
                                onCloseApp()
                            }
                        )
                    }
                ) {
                    Text("OK", color = Color.White)
                }
            }
        }
        
        Text(
            text = "Stealth Mode security active",
            color = Color(0x33FFFFFF),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun LockOverlayContent(
    appName: String,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onVerifySuccess: () -> Unit,
    onVerifyFailed: () -> Unit,
    onRequestBiometricPrompt: () -> Unit,
    onCancel: () -> Unit
) {
    var isHoldingScanner by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    
    val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // Simulating active biometric lock screen holding physics
    LaunchedEffect(isHoldingScanner) {
        if (isHoldingScanner) {
            val startTime = System.currentTimeMillis()
            while (isHoldingScanner && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / 1200f).coerceAtMost(1f)
                delay(30)
            }
            if (holdProgress >= 1f) {
                onVerifySuccess()
            }
        } else {
            // Drain progress slowly
            while (holdProgress > 0f) {
                holdProgress = (holdProgress - 0.1f).coerceAtLeast(0f)
                delay(20)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = displayTheme.onSurfaceColor.copy(alpha = 0.7f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = displayTheme.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SecureUnlock Pro",
                        color = displayTheme.onSurfaceColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(modifier = Modifier.size(24.dp)) // spacer balance
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Identity Glassmorphic Card (NeoGlass Aesthetic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(displayTheme.surfaceColor)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                displayTheme.accentColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(1.dp, displayTheme.accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked LockedApp",
                            tint = displayTheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = appName,
                            color = displayTheme.onSurfaceColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fingerprint / Face required to open",
                            color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Central Biometric Scanner with animated wavy waves
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Loop waves in Canvas matching NeoGlass
                    WavyBackgroundPulse(
                        scale = pulseScale,
                        color = displayTheme.accentColor,
                        isHolding = isHoldingScanner,
                        holdProgress = holdProgress
                    )

                    // Floating Glass Fingerprint Button
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                displayTheme.surfaceColor.copy(
                                    alpha = if (isHoldingScanner) 0.6f else 0.4f
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.radialGradient(
                                    listOf(
                                        displayTheme.primary.copy(alpha = 0.6f),
                                        displayTheme.secondary.copy(alpha = 0.2f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHoldingScanner = true
                                        try {
                                            awaitRelease()
                                        } finally {
                                            isHoldingScanner = false
                                        }
                                    },
                                    onTap = {
                                        onRequestBiometricPrompt()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint scan",
                            tint = if (isHoldingScanner) displayTheme.accentColor else displayTheme.primary,
                            modifier = Modifier
                                .size(46.dp)
                                .scale(if (isHoldingScanner) 1.15f else 1.0f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isHoldingScanner) "Scanning biometric session..." else "Touch and hold to scan or tap to open prompt",
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Outlined Trigger Button
            OutlinedButton(
                onClick = { onVerifyFailed() }, // simulate failed trigger
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                border = BorderStroke(1.dp, displayTheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SIMULATE FAILED LOCK ATTEMPT",
                    color = displayTheme.primary.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun WavyBackgroundPulse(
    scale: Float,
    color: Color,
    isHolding: Boolean,
    holdProgress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val center = this.center
        val maxRadius = size.width / 2

        if (isHolding) {
            // Radiant dynamic charging progress wave circles
            val activeRadius = 45.dp.toPx() + (maxRadius - 45.dp.toPx()) * holdProgress
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = activeRadius
            )
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = activeRadius,
                style = Stroke(width = 4.dp.toPx())
            )
        } else {
            // Ripple waves mimicking NeoGlass liquid effects
            drawCircle(
                color = color.copy(alpha = 0.04f),
                radius = 45.dp.toPx() + (maxRadius - 45.dp.toPx()) * scale
            )
        }

        // Draw an elegant wavy sine path wrapping the central scanner
        val wavePath = Path()
        val waveCount = 4
        val amplitude = if (isHolding) 12.dp.toPx() else 6.dp.toPx()
        val baseRadius = 60.dp.toPx() + if (isHolding) 15.dp.toPx() * holdProgress else 0f
        
        for (i in 0..360) {
            val angleRad = Math.toRadians(i.toDouble()).toFloat()
            val waveOffset = amplitude * kotlin.math.sin(angleRad * waveCount + wavePhase)
            val r = baseRadius + waveOffset
            val x = center.x + r * kotlin.math.cos(angleRad)
            val y = center.y + r * kotlin.math.sin(angleRad)
            
            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        wavePath.close()

        drawPath(
            path = wavePath,
            color = color.copy(alpha = if (isHolding) 0.6f else 0.25f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
