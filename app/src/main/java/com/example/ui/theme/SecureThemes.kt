package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class AppThemeColors(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val accentColor: Color
)

object SecureThemes {
    val themes = listOf(
        AppThemeColors(
            name = "Immersive UI",
            primary = Color(0xFF22D3EE),
            secondary = Color(0xFF6366F1),
            backgroundStart = Color(0xFF05070A),
            backgroundEnd = Color(0xFF090D16),
            surfaceColor = Color(0x0CFFFFFF),
            onSurfaceColor = Color(0xFFF1F5F9),
            accentColor = Color(0xFF22D3EE)
        ),
        AppThemeColors(
            name = "Ocean Blue",
            primary = Color(0xFF82B1FF),
            secondary = Color(0xFF448AFF),
            backgroundStart = Color(0xFF040B16),
            backgroundEnd = Color(0xFF0F1E36),
            surfaceColor = Color(0x221A3B66),
            onSurfaceColor = Color(0xFFE1F5FE),
            accentColor = Color(0xFF2979FF)
        ),
        AppThemeColors(
            name = "Lavender",
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFF9A82E3),
            backgroundStart = Color(0xFF0E0B16),
            backgroundEnd = Color(0xFF1B1429),
            surfaceColor = Color(0x222E1A47),
            onSurfaceColor = Color(0xFFFFF0FF),
            accentColor = Color(0xFFA58AF5)
        ),
        AppThemeColors(
            name = "Mint Green",
            primary = Color(0xFFA7F3D0),
            secondary = Color(0xFF10B981),
            backgroundStart = Color(0xFF04140E),
            backgroundEnd = Color(0xFF0E2B1F),
            surfaceColor = Color(0x2213422F),
            onSurfaceColor = Color(0xFFECFDF5),
            accentColor = Color(0xFF34D399)
        ),
        AppThemeColors(
            name = "Peach",
            primary = Color(0xFFFFCC80),
            secondary = Color(0xFFFFB74D),
            backgroundStart = Color(0xFF140D04),
            backgroundEnd = Color(0xFF2B1B0F),
            surfaceColor = Color(0x224C2F19),
            onSurfaceColor = Color(0xFFFFF3E0),
            accentColor = Color(0xFFFF9100)
        ),
        AppThemeColors(
            name = "Rose Pink",
            primary = Color(0xFFFBCFE8),
            secondary = Color(0xFFEC4899),
            backgroundStart = Color(0xFF14040D),
            backgroundEnd = Color(0xFF2B0E1E),
            surfaceColor = Color(0x22491533),
            onSurfaceColor = Color(0xFFFDF2F8),
            accentColor = Color(0xFFF472B6)
        ),
        AppThemeColors(
            name = "Arctic White",
            primary = Color(0xFFE2E8F0),
            secondary = Color(0xFF94A3B8),
            backgroundStart = Color(0xFF0A0F1D),
            backgroundEnd = Color(0xFF1E2640),
            surfaceColor = Color(0x332B395E),
            onSurfaceColor = Color(0xFFF8FAFC),
            accentColor = Color(0xFF38BDF8)
        ),
        AppThemeColors(
            name = "Sunset Orange",
            primary = Color(0xFFFECACA),
            secondary = Color(0xFFEF4444),
            backgroundStart = Color(0xFF1A0505),
            backgroundEnd = Color(0xFF381010),
            surfaceColor = Color(0x225C1B1B),
            onSurfaceColor = Color(0xFFFEF2F2),
            accentColor = Color(0xFFF87171)
        )
    )

    fun getTheme(name: String, isAmoled: Boolean, isDark: Boolean): AppThemeColors {
        val baseTheme = themes.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: themes[0]
        if (!isDark) {
            return AppThemeColors(
                name = baseTheme.name,
                primary = baseTheme.secondary,
                secondary = baseTheme.primary,
                backgroundStart = Color(0xFFF5F6FA),
                backgroundEnd = Color(0xFFE4E8F0),
                surfaceColor = Color(0x7FFFFFFF),
                onSurfaceColor = Color(0xFF1A2130),
                accentColor = baseTheme.secondary
            )
        }
        if (isAmoled) {
            return baseTheme.copy(
                backgroundStart = Color(0xFF000000),
                backgroundEnd = Color(0xFF000000),
                surfaceColor = Color(0x33262626)
            )
        }
        return baseTheme
    }
}
