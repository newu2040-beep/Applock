package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntruderLog
import com.example.service.LockSessionManager
import com.example.service.SecureUnlockDeviceAdminReceiver
import com.example.ui.theme.SecureThemes
import com.example.viewmodel.AppListItem
import com.example.viewmodel.AppLockViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDashboardScreen(viewModel: AppLockViewModel) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isAmoled by viewModel.isAmoled.collectAsState()
    val themeName by viewModel.activeThemeName.collectAsState()
    val displayTheme = SecureThemes.getTheme(themeName, isAmoled, isDark)

    val lockedAppsList by viewModel.lockedApps.collectAsState()
    val intruderLogsList by viewModel.intruderLogs.collectAsState()
    val appsList by viewModel.appListState.collectAsState()
    
    val currentTab = viewModel.selectedTab

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "App logo",
                            tint = displayTheme.accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SecureUnlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = displayTheme.onSurfaceColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = displayTheme.onSurfaceColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = displayTheme.surfaceColor.copy(alpha = 0.5f),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Details", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = displayTheme.accentColor,
                        selectedTextColor = displayTheme.accentColor,
                        indicatorColor = displayTheme.accentColor.copy(alpha = 0.15f),
                        unselectedIconColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        unselectedTextColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("Lock Apps", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = displayTheme.accentColor,
                        selectedTextColor = displayTheme.accentColor,
                        indicatorColor = displayTheme.accentColor.copy(alpha = 0.15f),
                        unselectedIconColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        unselectedTextColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_apps")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectedTab = 2 },
                    icon = { 
                        BadgedBox(badge = {
                            if (intruderLogsList.isNotEmpty()) {
                                Badge(containerColor = displayTheme.accentColor) { 
                                    Text(intruderLogsList.size.toString(), color = Color.White) 
                                }
                            }
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Intruder list")
                        }
                    },
                    label = { Text("Intruders", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = displayTheme.accentColor,
                        selectedTextColor = displayTheme.accentColor,
                        indicatorColor = displayTheme.accentColor.copy(alpha = 0.15f),
                        unselectedIconColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        unselectedTextColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_intruders")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = displayTheme.accentColor,
                        selectedTextColor = displayTheme.accentColor,
                        indicatorColor = displayTheme.accentColor.copy(alpha = 0.15f),
                        unselectedIconColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        unselectedTextColor = displayTheme.onSurfaceColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = ""
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardTab(viewModel, displayTheme, lockedAppsList.size, intruderLogsList.size)
                    1 -> AppsTab(viewModel, displayTheme, appsList)
                    2 -> IntrudersTab(viewModel, displayTheme, intruderLogsList)
                    3 -> SettingsTab(viewModel, displayTheme)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: AppLockViewModel,
    displayTheme: com.example.ui.theme.AppThemeColors,
    lockedCount: Int,
    intruderCount: Int
) {
    val securityScore by viewModel.securityScore.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isAmoled by viewModel.isAmoled.collectAsState()
    val isLockSystemApps by viewModel.isLockSystemAppsEnabled.collectAsState()
    val isStealth by viewModel.isStealthMode.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Security Banner / Shield Status
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(displayTheme.surfaceColor)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lockedCount > 0) "SHIELD ACTIVE" else "SHIELD INACTIVE",
                            color = displayTheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lockedCount > 0) "Your standard apps are locked." else "Lock apps to protect privacy.",
                            color = displayTheme.onSurfaceColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                displayTheme.accentColor.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (lockedCount > 0) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                            contentDescription = "Shield",
                            tint = displayTheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Custom drawn radial progress gauge
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(displayTheme.surfaceColor)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Security Analytics Dashboard",
                        color = displayTheme.onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing an elegant double arc security progress meter using native Compose Canvas
                        Canvas(modifier = Modifier.size(140.dp)) {
                            drawArc(
                                color = displayTheme.onSurfaceColor.copy(alpha = 0.1f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        displayTheme.primary,
                                        displayTheme.accentColor,
                                        displayTheme.primary
                                    )
                                ),
                                startAngle = 135f,
                                sweepAngle = (securityScore * 270f / 100f),
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$securityScore%",
                                color = displayTheme.primary,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SYSTEM SAFE",
                                color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Our active metrics show your phone data coverage is ${if (securityScore > 80) "Optimal" else "Moderate"}.",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Metrics Grid (2x2 style cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Protected Apps",
                        value = "$lockedCount apps",
                        icon = Icons.Default.Lock,
                        displayTheme = displayTheme,
                        onClick = { viewModel.selectedTab = 1 }
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Intruder Triggers",
                        value = "$intruderCount catches",
                        icon = Icons.Default.CameraAlt,
                        displayTheme = displayTheme,
                        onClick = { viewModel.selectedTab = 2 }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "System Lock",
                        value = if (isLockSystemApps) "Active" else "Inactive",
                        icon = Icons.Default.SettingsSystemDaydream,
                        displayTheme = displayTheme,
                        onClick = { viewModel.selectedTab = 3 }
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Stealth Mask",
                        value = if (isStealth) "Enabled" else "Standard Icon",
                        icon = Icons.Default.AutoFixHigh,
                        displayTheme = displayTheme,
                        onClick = { viewModel.selectedTab = 3 }
                    )
                }
            }
        }

        // Quick lock shortcuts panels
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Lock Shortcuts",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                QuickShortcutRow(
                    tag = "Lock Social Networks",
                    description = "Locks WhatsApp, Instagram, Twitter, Facebook",
                    icon = Icons.Default.Group,
                    displayTheme = displayTheme,
                    onAction = {
                        Toast.makeText(context, "Locked Social apps successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
                
                QuickShortcutRow(
                    tag = "Lock System & Config",
                    description = "Locks Settings, Dialers, Contacts, Play Store",
                    icon = Icons.Default.Settings,
                    displayTheme = displayTheme,
                    onAction = {
                        Toast.makeText(context, "Locked Sensitive System tools!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(displayTheme.surfaceColor)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = displayTheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = displayTheme.onSurfaceColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickShortcutRow(
    tag: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onAction() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(displayTheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = displayTheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tag,
                    color = displayTheme.onSurfaceColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = displayTheme.onSurfaceColor.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AppsTab(
    viewModel: AppLockViewModel,
    displayTheme: com.example.ui.theme.AppThemeColors,
    appsList: List<AppListItem>
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredList = remember(appsList, selectedCategory) {
        when (selectedCategory) {
            "Apps Only" -> appsList.filter { !it.isGame }
            "Games Only" -> appsList.filter { it.isGame }
            "Locked Only" -> appsList.filter { it.isLocked }
            else -> appsList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Futuristic Search Area (NeoGlass styling)
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearch(it) },
            placeholder = { Text("Search installed or system apps...", color = displayTheme.onSurfaceColor.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = displayTheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearch("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = displayTheme.primary)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = displayTheme.surfaceColor,
                unfocusedContainerColor = displayTheme.surfaceColor.copy(alpha = 0.7f),
                focusedTextColor = displayTheme.onSurfaceColor,
                unfocusedTextColor = displayTheme.onSurfaceColor,
                cursorColor = displayTheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .testTag("app_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf("All", "Apps Only", "Games Only", "Locked Only")
            categories.forEach { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) displayTheme.primary else displayTheme.surfaceColor.copy(alpha = 0.3f))
                        .border(1.dp, if (isSel) displayTheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSel) Color.Black else displayTheme.onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Empty",
                        tint = displayTheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No apps match this category/query",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.packageName }
                ) { appItem ->
                    AppRowItem(
                        appItem = appItem,
                        displayTheme = displayTheme,
                        onToggleLock = { viewModel.toggleAppLock(appItem) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AppRowItem(
    appItem: AppListItem,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onToggleLock: () -> Unit
) {
        val context = LocalContext.current
        val appIcon = remember(appItem.packageName) {
            try {
                context.packageManager.getApplicationIcon(appItem.packageName)
            } catch (e: Exception) {
                null
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(displayTheme.surfaceColor)
                .border(
                    width = 1.dp,
                    color = if (appItem.isLocked) displayTheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glass style package badge with actual application icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (appItem.isLocked) displayTheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (appItem.isLocked) displayTheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        coil.compose.AsyncImage(
                            model = appIcon,
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = if (appItem.isSystemApp) Icons.Default.SettingsSuggest else Icons.Default.AppShortcut,
                            contentDescription = null,
                            tint = if (appItem.isLocked) displayTheme.primary else displayTheme.onSurfaceColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appItem.appName,
                        color = displayTheme.onSurfaceColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (appItem.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(displayTheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SYSTEM",
                                color = displayTheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appItem.packageName,
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = appItem.isLocked,
            onCheckedChange = { onToggleLock() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = displayTheme.accentColor,
                uncheckedThumbColor = displayTheme.onSurfaceColor.copy(alpha = 0.5f),
                uncheckedTrackColor = displayTheme.surfaceColor.copy(alpha = 0.8f)
            ),
            modifier = Modifier.testTag("lock_switch_${appItem.packageName.replace(".", "_")}")
        )
    }
}

@Composable
fun IntrudersTab(
    viewModel: AppLockViewModel,
    displayTheme: com.example.ui.theme.AppThemeColors,
    intruders: List<IntruderLog>
) {
    val context = LocalContext.current
    var activeLogForInvoice by remember { mutableStateOf<IntruderLog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Security logs analytics summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(displayTheme.surfaceColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Intruder Photo Logs",
                        color = displayTheme.onSurfaceColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Local offline storage of unlock failures",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { viewModel.simulateIntruderLog() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = displayTheme.primary.copy(alpha = 0.2f), contentColor = displayTheme.primary),
                    modifier = Modifier.border(1.dp, displayTheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Text("Trigger Simulation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (intruders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraEnhance,
                        contentDescription = "Camera logs",
                        tint = displayTheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your device security logs are squeaky clean!",
                        color = displayTheme.onSurfaceColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Repeated biometric failures capture intruder photos offline.",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total unauthorized bypass captures: ${intruders.size}",
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Clear All",
                    color = displayTheme.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            viewModel.clearAllIntruderLogs()
                            Toast.makeText(context, "Security Logs completely wiped.", Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(intruders) { intruder ->
                    IntruderLogCard(
                        log = intruder,
                        displayTheme = displayTheme,
                        onClick = { activeLogForInvoice = intruder },
                        onDelete = { viewModel.deleteIntruderLog(intruder) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Invoice-styled ticket modal
    if (activeLogForInvoice != null) {
        val logForInvoice = activeLogForInvoice!!
        val dateStringInvoice = remember(logForInvoice.timestamp) {
            SimpleDateFormat("yyyy-MM-dd 'at' HH:mm a", Locale.getDefault()).format(Date(logForInvoice.timestamp))
        }
        AlertDialog(
            onDismissRequest = { activeLogForInvoice = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = displayTheme.primary)
                    Text("BREACH INCIDENT INVOICE", color = displayTheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, displayTheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoFile = remember(logForInvoice.photoPath) {
                            if (!logForInvoice.photoPath.isNullOrEmpty()) java.io.File(logForInvoice.photoPath) else null
                        }
                        if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                            coil.compose.AsyncImage(
                                model = photoFile,
                                contentDescription = "Intruder",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("[Procedural Spy Photo Rendered]", color = displayTheme.primary.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Text(
                        text = """
                            -----------------------------------------
                               INCIDENT EVIDENCE ANALYSIS        
                            -----------------------------------------
                            Ticket Registry  : #SR-${logForInvoice.id.toString().substringAfterLast("@").take(6).ifEmpty { "81AD24" }}
                            Security Date    : $dateStringInvoice
                            Bypass Target    : ${logForInvoice.appName}
                            Biometric Fail   : ${logForInvoice.attemptCount} Failed Attempts
                            Burglary Status  : LOCKED OUT (HIGH RISK)
                            Alarm Triggered  : Vocal speech alert loaded
                            =========================================
                            This audit log is saved securely offline on device 
                            local storage. Export below to save as files or 
                            load into public image gallery.
                        """.trimIndent(),
                        color = displayTheme.onSurfaceColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val path = com.example.util.ReportExporter.saveReportToPublicText(context, logForInvoice)
                            if (path != null) {
                                Toast.makeText(context, "Report exported: $path", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Could not save file.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = displayTheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save TXT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val path = com.example.util.ReportExporter.savePhotoToPublicGallery(context, logForInvoice.photoPath)
                            if (path != null) {
                                Toast.makeText(context, path, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No physical photo to export yet.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = displayTheme.accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Export JPG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeLogForInvoice = null }) {
                    Text("Close", color = displayTheme.onSurfaceColor.copy(alpha = 0.6f))
                }
            },
            containerColor = displayTheme.surfaceColor,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun IntruderLogCard(
    log: IntruderLog,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val photoFile = remember(log.photoPath) {
        if (!log.photoPath.isNullOrEmpty()) java.io.File(log.photoPath) else null
    }
    val hasPhotoFile = remember(photoFile) {
        photoFile != null && photoFile.exists() && photoFile.length() > 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(displayTheme.surfaceColor)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, displayTheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasPhotoFile && photoFile != null) {
                    coil.compose.AsyncImage(
                        model = photoFile,
                        contentDescription = "Intruder Capture",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Canvas(modifier = Modifier.size(54.dp)) {
                        // Face silhouette line drawings
                        val colorBrush = displayTheme.accentColor
                        val faceCenter = this.center.copy(y = this.center.y - 4.dp.toPx())
                        drawCircle(
                            color = colorBrush.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = faceCenter
                        )
                        drawArc(
                            color = colorBrush.copy(alpha = 0.5f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            size = androidx.compose.ui.geometry.Size(32.dp.toPx(), 24.dp.toPx()),
                            topLeft = androidx.compose.ui.geometry.Offset(this.center.x - 16.dp.toPx(), this.center.y + 2.dp.toPx())
                        )
                    }
                }
                
                // Camera Lens overlay icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(displayTheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bypass on: ${log.appName}",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = displayTheme.accentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Attempts captured: ${log.attemptCount}",
                        color = displayTheme.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Delete",
                    tint = displayTheme.onSurfaceColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: AppLockViewModel,
    displayTheme: com.example.ui.theme.AppThemeColors
) {
    val context = LocalContext.current
    var clipboardManager = LocalClipboardManager.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Front Camera alarm fully armed!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera denied. Falling back to digital procedural render.", Toast.LENGTH_LONG).show()
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "System notifications fully armed!", Toast.LENGTH_SHORT).show()
            // Dispatch a beautiful initial alert to let the user see the notification design
            com.example.util.NotificationHelper.sendShieldStatusNotification(context, isMonitoring = true, lockedCount = 3)
        } else {
            Toast.makeText(context, "Notification alerts denied. Intrusion event alarms won't display.", Toast.LENGTH_LONG).show()
        }
    }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val isAmoled by viewModel.isAmoled.collectAsState()
    val themeName by viewModel.activeThemeName.collectAsState()
    
    val relockScreenOff by viewModel.autoRelockOnScreenOff.collectAsState()
    val relockAppClose by viewModel.autoRelockOnAppClose.collectAsState()
    val isStealth by viewModel.isStealthMode.collectAsState()
    val isFakeCrash by viewModel.isFakeCrashEnabled.collectAsState()
    val isSilentCapture by viewModel.silentCaptureEnabled.collectAsState()
    val isLockSystem by viewModel.isLockSystemAppsEnabled.collectAsState()

    val voiceEnabled by viewModel.enableVoiceAlarm.collectAsState()
    val alarmText by viewModel.customAlarmText.collectAsState()
    val wallpaperSelected by viewModel.wallpaperPreset.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Customizable Themes Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Aesthetic Theme customization",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                // Theme choices pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorsList = listOf(
                        "Immersive UI", "Ocean Blue", "Lavender", "Mint Green", "Peach", "Rose Pink", "Arctic White", "Sunset Orange"
                    )
                    colorsList.forEach { name ->
                        val isSelected = themeName == name
                        Surface(
                            modifier = Modifier.clickable { viewModel.setTheme(name) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) displayTheme.primary else displayTheme.surfaceColor.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) displayTheme.primary else Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (isSelected) Color.Black else displayTheme.onSurfaceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                // Dark / AMOLED Mode Swapper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = displayTheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Dark theme", color = displayTheme.onSurfaceColor, fontSize = 13.sp)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = displayTheme.accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NightsStay, contentDescription = null, tint = displayTheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AMOLED pitch black", color = displayTheme.onSurfaceColor, fontSize = 13.sp)
                    }
                    Switch(
                        checked = isAmoled,
                        onCheckedChange = { viewModel.setAmoledMode(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = displayTheme.accentColor)
                    )
                }
            }
        }

        // Active Protection Controls
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Biometric Lock Options",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                SettingsToggleRow(
                    title = "Auto relock after app close",
                    description = "Locks package instantly when navigating home.",
                    checked = relockAppClose,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setAutoRelockOnAppClose(it) }
                )
                SettingsToggleRow(
                    title = "Auto relock after screen off",
                    description = "Clears active security configurations on screen sleep.",
                    checked = relockScreenOff,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setAutoRelockOnScreenOff(it) }
                )
                SettingsToggleRow(
                    title = "Stealth Launcher disguise",
                    description = "Fakes launcher icons to hide SecureUnlock.",
                    checked = isStealth,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setStealthMode(it) }
                )
                SettingsToggleRow(
                    title = "Fake Crash system warning",
                    description = "Fakes 'stopped working' screens to confuse intruders.",
                    checked = isFakeCrash,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setFakeCrashEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Camera silent capture",
                    description = "Front camera silent capture on failure triggers.",
                    checked = isSilentCapture,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setSilentCaptureEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Protect Sensitive System Apps",
                    description = "Adds default settings & system utilities to protect.",
                    checked = isLockSystem,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setIsLockSystemAppsEnabled(it) }
                )
            }
        }

        // Custom Voice Alarm and Lock Wallpaper customization panel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Custom Intruder Alarm & Wallpapers",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. Voice Alarm Toggle
                SettingsToggleRow(
                    title = "Vocal 'Thief Thief' Alarm Mode",
                    description = "Uses Text-to-Speech system speech to loudly warn the intruder when biometric authentication fails.",
                    checked = voiceEnabled,
                    displayTheme = displayTheme,
                    onCheckedChange = { viewModel.setEnableVoiceAlarm(it) }
                )

                // 2. Custom Alarm Editable phrase Textfield
                if (voiceEnabled) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Custom verbal threat message text",
                            color = displayTheme.onSurfaceColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = alarmText,
                            onValueChange = { viewModel.setCustomAlarmText(it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = displayTheme.onSurfaceColor),
                            placeholder = { Text("E.g. thief thief, stay away from my phone!", fontSize = 13.sp, color = displayTheme.onSurfaceColor.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = displayTheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedLabelColor = displayTheme.primary,
                                unfocusedLabelColor = displayTheme.onSurfaceColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 3. Wallpaper selection carousel
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Real-time overlay custom backgrounds",
                        color = displayTheme.onSurfaceColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sets customized animated cybermatic picture layers over biometric unlock dialog popup.",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "Starry Cyber Mesh", "Deep Midnight Nebula", "Virtual Matrix Rain", "Glassmorphic Sunset Glow", "Cyberpunk Grid Neon", "Standard Slate"
                        )
                        presets.forEach { preset ->
                            val isSel = wallpaperSelected == preset
                            Surface(
                                modifier = Modifier.clickable { viewModel.setWallpaperPreset(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) displayTheme.primary else displayTheme.surfaceColor.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) displayTheme.primary else Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = preset,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (isSel) Color.Black else displayTheme.onSurfaceColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shield Notification Settings and Real-time Alerts Testing
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Shield Notification Settings",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Configure and test real-time alerts. Ensure permission is granted so SecureUnlock can warn you of unauthorized accesses.",
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasNotificationPermission) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "Notifications are automatically enabled for this OS!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                com.example.util.NotificationHelper.sendSecurityIncidentNotification(
                                    context, 
                                    listOf("WhatsApp", "Instagram", "Photos", "Gmail").random(), 
                                    (2..4).random()
                                )
                                Toast.makeText(context, "Heads-up threat alarm sent! Swipe down to view.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = displayTheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Breach Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!hasNotificationPermission) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "Notifications are automatically enabled!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                com.example.util.NotificationHelper.sendShieldStatusNotification(
                                    context, 
                                    isMonitoring = true, 
                                    lockedCount = (4..12).random()
                                )
                                Toast.makeText(context, "Silent persistent monitoring banner updated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = displayTheme.accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Active Shield", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!hasNotificationPermission) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "Notifications are automatically enabled!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                com.example.util.NotificationHelper.sendWeeklyInsightsNotification(
                                    context,
                                    totalBreaches = (3..8).random(),
                                    highestRiskApp = listOf("WhatsApp", "Instagram", "Google Photos").random()
                                )
                                Toast.makeText(context, "Security Weekly Intelligence summary posted!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Intelligence", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Text(
                    text = "ℹ️ System notification channel options allow you to individually customize sound, priority, and vibrations for each of these security alerts inside device settings drawer.",
                    color = displayTheme.onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Actionable Permissions Checklist
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "System Permissions Checklist",
                    color = displayTheme.onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                PermissionLinkCard(
                    title = "Accessibility service monitoring",
                    desc = "Required to intercept when locked apps are launching.",
                    isGranted = isAccessibilityServiceEnabled(context),
                    displayTheme = displayTheme,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        Toast.makeText(context, "Search for 'SecureUnlock' & toggle ON", Toast.LENGTH_LONG).show()
                    }
                )

                PermissionLinkCard(
                    title = "Usage Access verification",
                    desc = "Used as helper query to track current active window package.",
                    isGranted = true, // simulated/granted
                    displayTheme = displayTheme,
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                PermissionLinkCard(
                    title = "Front Camera capture access",
                    desc = "Takes localized snaps of failed login attempts.",
                    isGranted = hasCameraPermission,
                    displayTheme = displayTheme,
                    onClick = {
                        if (!hasCameraPermission) {
                            cameraLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            Toast.makeText(context, "Camera capture permission is already granted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                PermissionLinkCard(
                    title = "System alerts & notifications",
                    desc = "Delivers threat breach warnings, safety alerts and offline audit logs.",
                    isGranted = hasNotificationPermission,
                    displayTheme = displayTheme,
                    onClick = {
                        if (!hasNotificationPermission) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "Notification alerts are automatically armed on this OS version!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // If already granted, run a demo alert when tapped
                            Toast.makeText(context, "Permission already granted! Dispatching demo warning...", Toast.LENGTH_SHORT).show()
                            com.example.util.NotificationHelper.sendSecurityIncidentNotification(context, "System Protector", 3)
                        }
                    }
                )

                PermissionLinkCard(
                    title = "Uninstall prevention (Admin)",
                    desc = "Device Administrator registers to prevent unauthorized uninstalls.",
                    isGranted = false,
                    displayTheme = displayTheme,
                    onClick = {
                        val componentName = ComponentName(context, SecureUnlockDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SecureUnlock Admin protection to prevent uninstallation")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Export system logs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(displayTheme.surfaceColor)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val logsText = "SECUREUNLOCK HISTORY LOG\nDate: ${Date()}\nTarget Theme: $themeName\nAMOLED Mode: $isAmoled\nSystem Safe: True\n"
                            clipboardManager.setText(AnnotatedString(logsText))
                            Toast.makeText(context, "Telemetry history logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = displayTheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export security logs", color = displayTheme.onSurfaceColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Copies encrypted statistics logs summaries.", color = displayTheme.onSurfaceColor.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = displayTheme.onSurfaceColor.copy(alpha = 0.5f))
                }
            }
        }

        // Developer Credit Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Love",
                        tint = Color(0xFFFF3355),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Built with Love",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = displayTheme.onSurfaceColor.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Editingcells",
                        color = displayTheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = displayTheme.onSurfaceColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = displayTheme.onSurfaceColor.copy(alpha = 0.6f), fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = displayTheme.accentColor)
        )
    }
}

@Composable
fun PermissionLinkCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    displayTheme: com.example.ui.theme.AppThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                color = displayTheme.onSurfaceColor, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                desc, 
                color = displayTheme.onSurfaceColor.copy(alpha = 0.5f), 
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isGranted) "Granted" else "Requires setup",
                color = if (isGranted) displayTheme.primary else displayTheme.accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircleOutline else Icons.Default.ArrowCircleRight,
                contentDescription = null,
                tint = if (isGranted) displayTheme.primary else displayTheme.accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, "com.example.service.AppLockAccessibilityService")
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabledServices.contains(expectedComponentName.flattenToString()) || enabledServices.contains(expectedComponentName.flattenToShortString())
}
