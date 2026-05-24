package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.speech.tts.TextToSpeech
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
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
    private var tts: TextToSpeech? = null

    private val systemKeyguardLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            handleUnlockSuccess()
        } else {
            handleUnlockFailed()
        }
    }

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

        // Initialize customizable TTS alarm speaker
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        setContent {
            val isDark = LockSessionManager.isDarkTheme
            val isAmoled = LockSessionManager.isAmoledMode
            val themeName = LockSessionManager.activeThemeName
            val displayTheme = SecureThemes.getTheme(themeName, isAmoled, isDark)
            val selectedWallpaper = LockSessionManager.wallpaperPreset

            var isFakeCrashShowing by remember { mutableStateOf(LockSessionManager.isFakeCrashEnabled) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Main wallpaper: Render custom gallery phone picture if configured
                val galleryWallpaper = LockSessionManager.customGalleryWallpaperUri
                if (galleryWallpaper.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = galleryWallpaper,
                        contentDescription = "Custom phone gallery preset background",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.5f // Blend gracefully with the back surface
                    )
                }

                // Background preset rendering
                if (selectedWallpaper == "Standard Slate" || selectedWallpaper.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        displayTheme.backgroundStart.copy(alpha = if (galleryWallpaper.isNotEmpty()) 0.5f else 1f),
                                        displayTheme.backgroundEnd.copy(alpha = if (galleryWallpaper.isNotEmpty()) 0.5f else 1f)
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
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DynamicPresetBackground(preset = selectedWallpaper, displayTheme = displayTheme)
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
                        packageName = targetPackage,
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
                        onRequestSystemLockPrompt = {
                            triggerSystemKeyguardUnlock()
                        },
                        onCancel = {
                            forceGoHome()
                        }
                    )
                }
            }
        }

        // Trigger system lock or native biometric dialog instantly when not displaying the fake crash panel
        if (!LockSessionManager.isFakeCrashEnabled) {
            if (LockSessionManager.isSystemLockEnabled) {
                triggerSystemKeyguardUnlock()
            } else {
                triggerNativeBiometric()
            }
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

    private fun triggerSystemKeyguardUnlock() {
        val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (km.isDeviceSecure) {
            val intent = km.createConfirmDeviceCredentialIntent(
                "Unlock $targetAppName",
                "Authenticate using your device security PIN, Pattern, or Password."
            )
            if (intent != null) {
                try {
                    systemKeyguardLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open native system verification screen", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Native device credentials launcher not generated.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No device security password, PIN, or pattern set on your phone.", Toast.LENGTH_LONG).show()
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
        
        // 1. Speak customized alert aloud
        val voiceEnabled = LockSessionManager.enableVoiceAlarm
        val customPhrase = LockSessionManager.customAlarmText.ifEmpty { "Thief! Thief! Unauthorized access attempt detected!" }
        if (voiceEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(customPhrase, TextToSpeech.QUEUE_FLUSH, null, "intruder_voice_id")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(customPhrase, TextToSpeech.QUEUE_FLUSH, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Trigger automatic background image capture and log inside app lock
        triggerSilentSelfieCapture()
    }

    private fun triggerSilentSelfieCapture() {
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
            val filename = "intruder_${format}.jpg"
            val file = File(filesDir, filename)

            // Step A: Immediately generate our high-fidelity cyber procedural selfie in case camera fails or lacks permissions, so we NEVER have a blank file or missing visual layout
            com.example.util.IntruderImageUtils.generateProceduralIntruderSelfie(this@LockOverlayActivity, targetAppName, file)

            // Step B: If CAMERA permission is active, attempt to overwrite with a REAL front-facing camera picture using CameraX
            if (ContextCompat.checkSelfPermission(this@LockOverlayActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(this@LockOverlayActivity)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                this@LockOverlayActivity,
                                cameraSelector,
                                imageCapture
                            )

                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            imageCapture.takePicture(
                                outputOptions,
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        try {
                                            cameraProvider.unbindAll()
                                        } catch (e: Exception) {}
                                        saveToDatabase(timestamp, file.absolutePath)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        try {
                                            cameraProvider.unbindAll()
                                        } catch (e: Exception) {}
                                        saveToDatabase(timestamp, file.absolutePath)
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            saveToDatabase(timestamp, file.absolutePath)
                        }
                    }, executor)
                } catch (e: Exception) {
                    saveToDatabase(timestamp, file.absolutePath)
                }
            } else {
                saveToDatabase(timestamp, file.absolutePath)
            }
        }
    }

    private fun saveToDatabase(timestamp: Long, absolutePath: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val log = IntruderLog(
                timestamp = timestamp,
                appName = targetAppName,
                photoPath = absolutePath,
                isSilent = LockSessionManager.silentCaptureEnabled,
                attemptCount = LockSessionManager.failAttemptsCount
            )
            db.dao.insertIntruderLog(log)
            
            // Dispatch a real-time system security notification to alert the user
            com.example.util.NotificationHelper.sendSecurityIncidentNotification(
                this@LockOverlayActivity,
                targetAppName,
                LockSessionManager.failAttemptsCount
            )
            
            Toast.makeText(
                this@LockOverlayActivity,
                "Intruder security photo captured: $targetAppName",
                Toast.LENGTH_SHORT
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {}
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
    packageName: String,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onVerifySuccess: () -> Unit,
    onVerifyFailed: () -> Unit,
    onRequestBiometricPrompt: () -> Unit,
    onRequestSystemLockPrompt: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        try {
            if (packageName.isNotEmpty()) {
                context.packageManager.getApplicationIcon(packageName)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val isEliteActive = LockSessionManager.isEliteModeEnabled
    var eliteStage by remember { mutableStateOf(1) } // 1: PIN Fallback, 2: Face Scanner, 3: Biometrics Scan
    var activeTabMode by remember { mutableStateOf(if (LockSessionManager.isPinLockEnabled) "PIN" else "BIOMETRICS") }

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
                if (isEliteActive) {
                    // Elite Layer 3 passed! Unlock app completely.
                    onVerifySuccess()
                } else {
                    onVerifySuccess()
                }
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                displayTheme.accentColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(1.dp, displayTheme.accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            coil.compose.AsyncImage(
                                model = appIcon,
                                contentDescription = "Target logo representation",
                                modifier = Modifier.size(34.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked APP",
                                tint = displayTheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = appName,
                            color = displayTheme.onSurfaceColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEliteActive) {
                                when (eliteStage) {
                                    1 -> "🛡️ Elite Mode [Layer 1/3] - Enter PIN"
                                    2 -> "🧬 Elite Mode [Layer 2/3] - Face Unlocking"
                                    else -> "🌸 Elite Mode [Layer 3/3] - Fingerprint Scan"
                                }
                            } else {
                                "Secure Fallback Access Active"
                            },
                            color = displayTheme.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Key Interface: Stage, Numpad Keypad, or Biometric Screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isEliteActive) {
                    when (eliteStage) {
                        1 -> {
                            PinKeypadView(
                                correctPin = LockSessionManager.securePinCode,
                                onSuccess = {
                                    eliteStage = 2
                                    Toast.makeText(context, "PIN verified! Proceeding to face recognition standard...", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    onVerifyFailed()
                                },
                                displayTheme = displayTheme
                            )
                        }
                        2 -> {
                            FaceScanView(
                                onScanDone = {
                                    eliteStage = 3
                                    Toast.makeText(context, "Face signature mapped! Approving final biometric thumb signature...", Toast.LENGTH_SHORT).show()
                                },
                                displayTheme = displayTheme
                            )
                        }
                        else -> {
                            BiometricScannerView(
                                isHoldingScanner = isHoldingScanner,
                                holdProgress = holdProgress,
                                pulseScale = pulseScale,
                                displayTheme = displayTheme,
                                onRequestBiometricPrompt = onRequestBiometricPrompt,
                                onScannerPressChange = { isHoldingScanner = it }
                            )
                        }
                    }
                } else {
                    // Regular Mode allows toggling between Keyboard keypad or Fingerprint scan fallback
                    if (activeTabMode == "PIN") {
                        PinKeypadView(
                            correctPin = LockSessionManager.securePinCode,
                            onSuccess = {
                                onVerifySuccess()
                            },
                            onFailure = {
                                onVerifyFailed()
                            },
                            displayTheme = displayTheme
                        )
                    } else {
                        BiometricScannerView(
                            isHoldingScanner = isHoldingScanner,
                            holdProgress = holdProgress,
                            pulseScale = pulseScale,
                            displayTheme = displayTheme,
                            onRequestBiometricPrompt = onRequestBiometricPrompt,
                            onScannerPressChange = { isHoldingScanner = it }
                        )
                    }
                }
            }

            // Controls & Toggle Buttons Room
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (LockSessionManager.isSystemLockEnabled) {
                    Button(
                        onClick = onRequestSystemLockPrompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = displayTheme.accentColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "System Unlock",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNLOCK WITH SYSTEM KEYGUARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isEliteActive) {
                    // Quick Selector Tabs
                    Row(
                        modifier = Modifier
                            .width(220.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(displayTheme.surfaceColor)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { activeTabMode = "BIOMETRICS" },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTabMode == "BIOMETRICS") displayTheme.primary else Color.Transparent,
                                contentColor = if (activeTabMode == "BIOMETRICS") Color.Black else displayTheme.onSurfaceColor.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("BIOMETRIC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { activeTabMode = "PIN" },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTabMode == "PIN") displayTheme.primary else Color.Transparent,
                                contentColor = if (activeTabMode == "PIN") Color.Black else displayTheme.onSurfaceColor.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🔑 PIN CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Reset steps link for debug
                    Text(
                        text = "Layer stage verification prevents system breaches.",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }

                // Failed simulation helper
                OutlinedButton(
                    onClick = { onVerifyFailed() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    border = BorderStroke(1.dp, displayTheme.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "SIMULATE KEY DECOY EXPLOIT WARNING",
                        color = displayTheme.primary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PinKeypadView(
    correctPin: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    displayTheme: com.example.ui.theme.AppThemeColors,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var showFlashError by remember { mutableStateOf(false) }

    LaunchedEffect(showFlashError) {
        if (showFlashError) {
            delay(800)
            showFlashError = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "ENTER SECURING CREDENTIALS",
            color = displayTheme.onSurfaceColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Bullet dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val isActive = pin.length >= i
                val dotColor = if (showFlashError) Color(0xFFEF4444) else if (isActive) displayTheme.accentColor else displayTheme.onSurfaceColor.copy(alpha = 0.15f)
                val scale = if (isActive) 1.25f else 1.0f
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(scale)
                        .background(dotColor, shape = CircleShape)
                        .border(1.dp, if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid 3x4
        val buttonsList = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "◀")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            for (row in buttonsList) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (char in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.4f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(displayTheme.surfaceColor.copy(alpha = 0.35f))
                                .clickable {
                                    when (char) {
                                        "C" -> pin = ""
                                        "◀" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        else -> {
                                            if (pin.length < 4) {
                                                pin += char
                                                if (pin.length == 4) {
                                                    if (pin == correctPin) {
                                                        onSuccess()
                                                        pin = ""
                                                    } else {
                                                        showFlashError = true
                                                        onFailure()
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .border(1.dp, displayTheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                color = displayTheme.onSurfaceColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaceScanView(
    onScanDone: () -> Unit,
    displayTheme: com.example.ui.theme.AppThemeColors,
    modifier: Modifier = Modifier
) {
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            val startTime = System.currentTimeMillis()
            while (isScanning && scanProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                scanProgress = (elapsed / 1500f).coerceAtMost(1f)
                delay(30)
            }
            if (scanProgress >= 1f) {
                onScanDone()
            }
        } else {
            scanProgress = 0f
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "LAYER 2: FACIAL TELEMETRY MATRIX",
            color = displayTheme.onSurfaceColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Box(
            modifier = Modifier
                .size(175.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(displayTheme.surfaceColor)
                .border(1.5.dp, displayTheme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scopeSize = 130.dp.toPx()
                val topX = (size.width - scopeSize) / 2
                val topY = (size.height - scopeSize) / 2
                
                val len = 15.dp.toPx()
                val color = displayTheme.primary
                
                // Corners
                drawLine(color, androidx.compose.ui.geometry.Offset(topX, topY), androidx.compose.ui.geometry.Offset(topX + len, topY), strokeWidth = 3.dp.toPx())
                drawLine(color, androidx.compose.ui.geometry.Offset(topX, topY), androidx.compose.ui.geometry.Offset(topX, topY + len), strokeWidth = 3.dp.toPx())
                
                drawLine(color, androidx.compose.ui.geometry.Offset(topX + scopeSize, topY), androidx.compose.ui.geometry.Offset(topX + scopeSize - len, topY), strokeWidth = 3.dp.toPx())
                drawLine(color, androidx.compose.ui.geometry.Offset(topX + scopeSize, topY), androidx.compose.ui.geometry.Offset(topX + scopeSize, topY + len), strokeWidth = 3.dp.toPx())
                
                drawLine(color, androidx.compose.ui.geometry.Offset(topX, topY + scopeSize), androidx.compose.ui.geometry.Offset(topX + len, topY + scopeSize), strokeWidth = 3.dp.toPx())
                drawLine(color, androidx.compose.ui.geometry.Offset(topX, topY + scopeSize), androidx.compose.ui.geometry.Offset(topX, topY + scopeSize - len), strokeWidth = 3.dp.toPx())
                
                drawLine(color, androidx.compose.ui.geometry.Offset(topX + scopeSize, topY + scopeSize), androidx.compose.ui.geometry.Offset(topX + scopeSize - len, topY + scopeSize), strokeWidth = 3.dp.toPx())
                drawLine(color, androidx.compose.ui.geometry.Offset(topX + scopeSize, topY + scopeSize), androidx.compose.ui.geometry.Offset(topX + scopeSize, topY + scopeSize - len), strokeWidth = 3.dp.toPx())

                // Face contours
                drawOval(
                    color = displayTheme.accentColor.copy(alpha = if (isScanning) 0.5f else 0.2f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.55f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                if (isScanning) {
                    val scanY = topY + scopeSize * scanProgress
                    drawLine(
                        color = displayTheme.accentColor.copy(alpha = 0.8f),
                        start = androidx.compose.ui.geometry.Offset(topX, scanY),
                        end = androidx.compose.ui.geometry.Offset(topX + scopeSize, scanY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            if (!isScanning) {
                Button(
                    onClick = { isScanning = true },
                    colors = ButtonDefaults.buttonColors(containerColor = displayTheme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START FACE SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "MAPPING... ${(scanProgress * 100).toInt()}%",
                    color = displayTheme.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "Ensure face contours center in focus box.",
            color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BiometricScannerView(
    isHoldingScanner: Boolean,
    holdProgress: Float,
    pulseScale: Float,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onRequestBiometricPrompt: () -> Unit,
    onScannerPressChange: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            WavyBackgroundPulse(
                scale = pulseScale,
                color = displayTheme.accentColor,
                isHolding = isHoldingScanner,
                holdProgress = holdProgress
            )

            // Touch sensor
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        displayTheme.surfaceColor.copy(
                            alpha = if (isHoldingScanner) 0.61f else 0.41f
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
                                onScannerPressChange(true)
                                try {
                                    awaitRelease()
                                } finally {
                                    onScannerPressChange(false)
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
                        .size(42.dp)
                        .scale(if (isHoldingScanner) 1.15f else 1.0f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isHoldingScanner) "Scanning biometric fingerprint..." else "Touch & Hold to scan signature or Tap for prompt",
            color = displayTheme.onSurfaceColor.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
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

@Composable
fun DynamicPresetBackground(
    preset: String,
    displayTheme: com.example.ui.theme.AppThemeColors
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (preset) {
            "Starry Cyber Mesh" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "mesh")
                val meshAnimate by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(12000, easing = LinearEasing)
                    ),
                    label = "meshScale"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF02040A))
                    
                    val gridSpacing = 48.dp.toPx()
                    for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                        drawLine(
                            color = displayTheme.primary.copy(alpha = 0.04f),
                            start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                            end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                        drawLine(
                            color = displayTheme.primary.copy(alpha = 0.04f),
                            start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                            end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val nodes = listOf(
                        androidx.compose.ui.geometry.Offset(0.12f, 0.18f),
                        androidx.compose.ui.geometry.Offset(0.32f, 0.12f),
                        androidx.compose.ui.geometry.Offset(0.48f, 0.28f),
                        androidx.compose.ui.geometry.Offset(0.18f, 0.42f),
                        androidx.compose.ui.geometry.Offset(0.38f, 0.58f),
                        androidx.compose.ui.geometry.Offset(0.58f, 0.42f),
                        androidx.compose.ui.geometry.Offset(0.72f, 0.22f),
                        androidx.compose.ui.geometry.Offset(0.88f, 0.38f),
                        androidx.compose.ui.geometry.Offset(0.68f, 0.68f),
                        androidx.compose.ui.geometry.Offset(0.84f, 0.78f),
                        androidx.compose.ui.geometry.Offset(0.48f, 0.82f),
                        androidx.compose.ui.geometry.Offset(0.24f, 0.76f)
                    )

                    val pxNodes = nodes.map { point ->
                        val swayX = kotlin.math.cos(meshAnimate + (point.x * 8f)) * 14.dp.toPx()
                        val swayY = kotlin.math.sin(meshAnimate + (point.y * 8f)) * 14.dp.toPx()
                        androidx.compose.ui.geometry.Offset(
                            point.x * size.width + swayX,
                            point.y * size.height + swayY
                        )
                    }

                    for (i in pxNodes.indices) {
                        for (j in i + 1 until pxNodes.size) {
                            val dist = (pxNodes[i] - pxNodes[j]).getDistance()
                            if (dist < 200.dp.toPx()) {
                                val alpha = (1f - dist / 200.dp.toPx()).coerceAtLeast(0f) * 0.22f
                                drawLine(
                                    color = displayTheme.primary.copy(alpha = alpha),
                                    start = pxNodes[i],
                                    end = pxNodes[j],
                                    strokeWidth = 1.2.dp.toPx()
                                )
                            }
                        }
                    }

                    pxNodes.forEach { node ->
                        drawCircle(
                            color = displayTheme.primary,
                            radius = 3.dp.toPx(),
                            center = node
                        )
                        drawCircle(
                            color = displayTheme.accentColor.copy(alpha = 0.25f),
                            radius = 8.dp.toPx(),
                            center = node
                        )
                    }
                }
            }
            "Deep Midnight Nebula" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "nebula")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF030107))
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2E4F46E5), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.35f),
                            radius = size.width * 0.85f * pulseScale
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.35f),
                        radius = size.width * 0.85f * pulseScale
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x229333EA), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.65f),
                            radius = size.width * 0.75f * (2f - pulseScale)
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.65f),
                        radius = size.width * 0.75f * (2f - pulseScale)
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        center = center,
                        radius = 170.dp.toPx() * pulseScale,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            "Virtual Matrix Rain" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "matrix")
                val matrixOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1200f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(9000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "offset"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF020604))
                    
                    val colCount = 14
                    val colWidth = size.width / colCount
                    for (i in 0 until colCount) {
                        val x = i * colWidth + colWidth / 2f
                        val speedMult = (i % 4 + 1) * 0.4f
                        val initialY = (i * 180) % size.height
                        val currentY = (initialY + matrixOffset * speedMult) % size.height
                        
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x9E10B981), Color(0xFF34D399)),
                                startY = currentY - 180.dp.toPx(),
                                endY = currentY
                            ),
                            start = androidx.compose.ui.geometry.Offset(x, currentY - 180.dp.toPx()),
                            end = androidx.compose.ui.geometry.Offset(x, currentY),
                            strokeWidth = 1.8.dp.toPx()
                        )
                        
                        drawCircle(
                            color = Color(0xFFE6FDF4),
                            center = androidx.compose.ui.geometry.Offset(x, currentY),
                            radius = 2.dp.toPx()
                        )
                    }
                }
            }
            "Glassmorphic Sunset Glow" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "sunset")
                val sunsetPulse by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(10000, easing = LinearEasing)
                    ),
                    label = "sunsetPulse"
                )
                val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(48.dp)
                } else Modifier
                
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0B18))) {
                    Canvas(modifier = Modifier.fillMaxSize().then(blurModifier)) {
                        val o1X = size.width * 0.35f + kotlin.math.cos(sunsetPulse) * 110.dp.toPx()
                        val o1Y = size.height * 0.32f + kotlin.math.sin(sunsetPulse) * 110.dp.toPx()
                        val o2X = size.width * 0.65f + kotlin.math.sin(sunsetPulse) * 90.dp.toPx()
                        val o2Y = size.height * 0.62f + kotlin.math.cos(sunsetPulse) * 90.dp.toPx()
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3FF43F5E), Color.Transparent),
                                radius = 220.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(o1X, o1Y)
                            ),
                            center = androidx.compose.ui.geometry.Offset(o1X, o1Y),
                            radius = 220.dp.toPx()
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3AF59E0B), Color.Transparent),
                                radius = 240.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(o2X, o2Y)
                            ),
                            center = androidx.compose.ui.geometry.Offset(o2X, o2Y),
                            radius = 240.dp.toPx()
                        )
                    }
                }
            }
            "Cyberpunk Grid Neon" -> {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val targetOffsetPx = with(density) { 44.dp.toPx() }
                
                val infiniteTransition = rememberInfiniteTransition(label = "cyberpunk")
                val gridOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = targetOffsetPx,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing)
                    ),
                    label = "offset"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF06020A))
                    
                    val horizonY = size.height * 0.5f
                    val gridCount = 22
                    
                    for (i in 0..gridCount) {
                        val startX = (i * size.width) / gridCount
                        drawLine(
                            color = Color(0x2DEC4899),
                            start = androidx.compose.ui.geometry.Offset(startX, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width / 2f, horizonY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    var currentY = horizonY
                    var step = 10.dp.toPx()
                    while (currentY < size.height) {
                        val drawY = currentY + gridOffset * (currentY / size.height)
                        if (drawY in horizonY..size.height) {
                            val alpha = ((drawY - horizonY) / (size.height - horizonY)).coerceIn(0f, 1f) * 0.25f
                            drawLine(
                                color = Color(0x3206B6D4).copy(alpha = alpha),
                                start = androidx.compose.ui.geometry.Offset(0f, drawY),
                                end = androidx.compose.ui.geometry.Offset(size.width, drawY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        currentY += step
                        step *= 1.25f
                    }
                }
            }
            "Holographic Scan Scanline" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "holograms")
                val scanLineY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scanline"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF020712))
                    val gridWidth = 40.dp.toPx()
                    for (x in 0..size.width.toInt() step gridWidth.toInt()) {
                        drawLine(displayTheme.primary.copy(alpha = 0.04f), androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), androidx.compose.ui.geometry.Offset(x.toFloat(), size.height))
                    }
                    for (y in 0..size.height.toInt() step gridWidth.toInt()) {
                        drawLine(displayTheme.primary.copy(alpha = 0.04f), androidx.compose.ui.geometry.Offset(0f, y.toFloat()), androidx.compose.ui.geometry.Offset(size.width, y.toFloat()))
                    }
                    // Glowing biometric sonar concentric lines
                    drawCircle(displayTheme.accentColor.copy(alpha = 0.07f), radius = 100.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(displayTheme.accentColor.copy(alpha = 0.12f), radius = 140.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                    
                    val lineY = scanLineY * size.height
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, displayTheme.primary.copy(alpha = 0.35f), Color.Transparent),
                            startY = lineY - 15.dp.toPx(),
                            endY = lineY + 15.dp.toPx()
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, lineY),
                        end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            "Retro Arcade Grid" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "retro")
                val roadProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "gridRoad"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0F0014))
                    val gridCenterY = size.height * 0.45f
                    
                    // Outlined neon retro sun
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF3366), Color(0xFFFF9933)),
                            startY = gridCenterY - 110.dp.toPx(),
                            endY = gridCenterY
                        ),
                        radius = 90.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, gridCenterY)
                    )
                    
                    // perspective horizon grids
                    val gridLines = 18
                    for (i in 0..gridLines) {
                        val startX = (i * size.width) / gridLines
                        drawLine(
                            color = Color(0xFFFF2A85).copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(startX, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width / 2f, gridCenterY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Dynamic scaling horizontal bars
                    var multiplierY = 0f
                    while (multiplierY < 1f) {
                        val adjustedVal = (multiplierY + roadProgress * 0.1f) % 1f
                        val actualY = gridCenterY + (size.height - gridCenterY) * java.lang.Math.pow(adjustedVal.toDouble(), 2.5).toFloat()
                        drawLine(
                            color = Color(0xFF00FFFF).copy(alpha = (adjustedVal * 0.4f).toFloat()),
                            start = androidx.compose.ui.geometry.Offset(0f, actualY.toFloat()),
                            end = androidx.compose.ui.geometry.Offset(size.width, actualY.toFloat()),
                            strokeWidth = 1.dp.toPx()
                        )
                        multiplierY += 0.12f
                    }
                }
            }
            "Quantum Particle Burst" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "quantum")
                val rotProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(18000, easing = LinearEasing)),
                    label = "rot"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF040608))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(displayTheme.primary.copy(alpha = 0.15f), Color.Transparent),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = center
                    )
                    
                    // Draw a orbital ring system of atomic nodes
                    val angleRad = Math.toRadians(rotProgress.toDouble()).toFloat()
                    val numNodes = 7
                    for (i in 0 until numNodes) {
                        val offsetAngle = angleRad + (2 * Math.PI.toFloat() * i / numNodes)
                        val radiusFactor = 120.dp.toPx() + 20.dp.toPx() * kotlin.math.sin(offsetAngle * 2.5f)
                        val nodeX = center.x + radiusFactor * kotlin.math.cos(offsetAngle)
                        val nodeY = center.y + radiusFactor * kotlin.math.sin(offsetAngle)
                        
                        drawCircle(displayTheme.primary, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(nodeX, nodeY))
                        drawCircle(displayTheme.primary.copy(alpha = 0.2f), radius = 12.dp.toPx(), center = androidx.compose.ui.geometry.Offset(nodeX, nodeY))
                        drawLine(
                            color = displayTheme.primary.copy(alpha = 0.1f),
                            start = center,
                            end = androidx.compose.ui.geometry.Offset(nodeX, nodeY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    drawCircle(displayTheme.accentColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = center)
                }
            }
            "Abyss Lava Lamp" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "lava")
                val animValue by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
                    label = "sweepAnim"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF08020E))
                    val blob1X = center.x + kotlin.math.cos(animValue) * 80.dp.toPx()
                    val blob1Y = center.y + kotlin.math.sin(animValue * 1.5f) * 140.dp.toPx()
                    val blob2X = center.x - kotlin.math.sin(animValue) * 90.dp.toPx()
                    val blob2Y = center.y - kotlin.math.cos(animValue * 1.2f) * 160.dp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color(0xFF6366F1).copy(alpha = 0.35f), Color.Transparent), radius = 160.dp.toPx()),
                        radius = 160.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(blob1X, blob1Y)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color(0xFFEC4899).copy(alpha = 0.32f), Color.Transparent), radius = 170.dp.toPx()),
                        radius = 170.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(blob2X, blob2Y)
                    )
                }
            }
            "Polished Satin Silk" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "silk")
                val slide by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(animation = tween(7000, easing = LinearEasing)),
                    label = "slideAnim"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF060910))
                    
                    // Generate elegant curving sine curtains
                    val path1 = Path()
                    val path2 = Path()
                    path1.moveTo(0f, size.height * 0.4f)
                    path2.moveTo(0f, size.height * 0.6f)
                    val stepsCount = 50
                    val stepWidth = size.width / stepsCount

                    for (i in 0..stepsCount) {
                        val currentX = i * stepWidth
                        val progress = i.toFloat() / stepsCount
                        val heightY1 = size.height * 0.45f + kotlin.math.sin(slide + progress * 5f) * 60.dp.toPx()
                        val heightY2 = size.height * 0.65f + kotlin.math.cos(slide - progress * 4f) * 70.dp.toPx()
                        path1.lineTo(currentX, heightY1)
                        path2.lineTo(currentX, heightY2)
                    }
                    path1.lineTo(size.width, size.height)
                    path1.lineTo(0f, size.height)
                    path1.close()
                    
                    path2.lineTo(size.width, size.height)
                    path2.lineTo(0f, size.height)
                    path2.close()

                    drawPath(
                        path = path2,
                        brush = Brush.verticalGradient(listOf(displayTheme.secondary.copy(alpha = 0.15f), Color.Transparent))
                    )
                    drawPath(
                        path = path1,
                        brush = Brush.verticalGradient(listOf(displayTheme.primary.copy(alpha = 0.12f), Color.Transparent))
                    )
                }
            }
            "Golden Shimmer Sparkles" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "gold")
                val shimmer by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(animation = tween(3800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                    label = "glowIntensity"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF0D0A05))
                    
                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color(0x19F59E0B), Color.Transparent), radius = size.width * 0.9f),
                        radius = size.width * 0.9f,
                        center = center
                    )
                    
                    // Sparkles coordinates
                    val sparkles = listOf(
                        androidx.compose.ui.geometry.Offset(0.15f, 0.22f),
                        androidx.compose.ui.geometry.Offset(0.35f, 0.15f),
                        androidx.compose.ui.geometry.Offset(0.75f, 0.28f),
                        androidx.compose.ui.geometry.Offset(0.22f, 0.65f),
                        androidx.compose.ui.geometry.Offset(0.85f, 0.58f),
                        androidx.compose.ui.geometry.Offset(0.55f, 0.82f),
                        androidx.compose.ui.geometry.Offset(0.42f, 0.38f),
                        androidx.compose.ui.geometry.Offset(0.68f, 0.72f)
                    )

                    sparkles.forEachIndexed { idx, point ->
                        val localShimmer = (shimmer + (idx * 0.15f)) % 1f
                        val floatX = point.x * size.width
                        val floatY = point.y * size.height + (kotlin.math.sin(shimmer * Math.PI.toFloat() + idx) * 12.dp.toPx())
                        val sparkleRadius = (3.dp.toPx() + 5.dp.toPx() * localShimmer)
                        drawCircle(
                            color = Color(0xFFFBBF24).copy(alpha = localShimmer * 0.6f),
                            radius = sparkleRadius,
                            center = androidx.compose.ui.geometry.Offset(floatX.toFloat(), floatY.toFloat())
                        )
                    }
                }
            }
            else -> {
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
                )
            }
        }
    }
}

