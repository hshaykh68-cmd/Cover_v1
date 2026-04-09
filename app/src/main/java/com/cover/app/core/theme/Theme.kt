package com.cover.app.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cover.app.data.remoteconfig.ThemeManager

// ============================================
// STEALTH LUXURY COLOR SCHEME
// ============================================

private val StealthDarkColorScheme = darkColorScheme(
    // Primary - cyan glow system
    primary = CyanGlow,
    onPrimary = VoidBlack,
    primaryContainer = CyanGlow.copy(alpha = 0.2f),
    onPrimaryContainer = CyanGlow,

    // Secondary - glassmorphic surfaces
    secondary = Surface30,
    onSecondary = TextPrimary,
    secondaryContainer = Surface20,
    onSecondaryContainer = TextPrimary,

    // Tertiary - accent amber
    tertiary = AmberAlert,
    onTertiary = VoidBlack,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = AmberAlert,

    // Background - deep void
    background = VoidBlack,
    onBackground = TextPrimary,

    // Surfaces - glassmorphic layers
    surface = Surface10,
    onSurface = TextPrimary,
    surfaceVariant = Surface15,
    onSurfaceVariant = TextSecondary,

    // Error - crimson security
    error = CrimsonSecurity,
    onError = TextPrimary,
    errorContainer = CrimsonSecurity.copy(alpha = 0.2f),
    onErrorContainer = CrimsonSecurity,

    // Outline - subtle borders
    outline = Surface30,
    outlineVariant = Surface20,

    // Inverse - for contrast elements
    inverseSurface = Surface30,
    inverseOnSurface = TextPrimary,
    inversePrimary = CyanGlow
)

// Light scheme (rarely used but available)
private val StealthLightColorScheme = lightColorScheme(
    primary = GlowCyanEnd,
    onPrimary = Color.White,
    secondary = Color(0xFF666666),
    onSecondary = Color.White,
    tertiary = AmberAlert,
    onTertiary = VoidBlack,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    error = CrimsonSecurity,
    onError = Color.White
)

@Composable
fun CoverTheme(
    darkTheme: Boolean = true, // Default to dark theme for privacy
    dynamicColor: Boolean = false, // Disable dynamic colors for consistent branding
    themeManager: ThemeManager? = null,
    content: @Composable () -> Unit
) {
    // Get dynamic color scheme from ThemeManager if provided (cloud-controlled)
    val dynamicColorScheme = themeManager?.getDynamicColorScheme()

    val colorScheme = when {
        // Prioritize cloud-controlled theme from ThemeManager
        dynamicColorScheme != null -> dynamicColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StealthDarkColorScheme
        else -> StealthLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Deep black status bar for seamless edge-to-edge
            window.statusBarColor = VoidBlack.toArgb()
            window.navigationBarColor = VoidBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
