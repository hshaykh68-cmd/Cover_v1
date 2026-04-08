package com.cover.app.data.remoteconfig

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ThemeManager handles dynamic theming controlled from cloud.
 * 
 * Features:
 * - Primary accent color from Remote Config
 * - Calculator style presets (iOS, Material, Samsung)
 * - Vault grid columns configuration
 * - Runtime theme switching
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    init {
        // Listen for remote config changes
        remoteConfigManager.configLoaded.value
        updateThemeFromConfig()
    }

    /**
     * Update theme based on cloud configuration
     */
    fun updateThemeFromConfig() {
        val config = remoteConfigManager.getConfig()
        
        // Parse primary accent color
        val accentColor = parseColor(config.primaryAccentColor) ?: Color(0xFF2196F3)
        
        // Parse calculator style
        val style = CalculatorStyle.fromString(config.calculatorStyle)
        
        // Update theme state
        _themeState.update { currentState ->
            currentState.copy(
                primaryAccentColor = accentColor,
                calculatorStyle = style,
                vaultGridColumns = config.vaultGridColumns.coerceIn(2, 5),
                upgradeButtonStyle = UpgradeButtonStyle.fromString(config.upgradeButtonStyle),
                colorScheme = generateColorScheme(accentColor)
            )
        }
    }

    /**
     * Get dynamic color scheme based on cloud accent color
     */
    @Composable
    fun getDynamicColorScheme(): ColorScheme {
        val state by _themeState.collectAsState()
        return state.colorScheme
    }

    /**
     * Get calculator style configuration
     */
    fun getCalculatorStyle(): CalculatorStyle {
        return _themeState.value.calculatorStyle
    }

    /**
     * Get vault grid column count
     */
    fun getVaultGridColumns(): Int {
        return _themeState.value.vaultGridColumns
    }

    /**
     * Get upgrade button style
     */
    fun getUpgradeButtonStyle(): UpgradeButtonStyle {
        return _themeState.value.upgradeButtonStyle
    }

    /**
     * Get primary accent color
     */
    fun getPrimaryAccentColor(): Color {
        return _themeState.value.primaryAccentColor
    }

    /**
     * Generate a dark color scheme based on the primary accent color
     */
    private fun generateColorScheme(primary: Color): ColorScheme {
        return darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = primary.copy(alpha = 0.7f),
            onSecondary = Color.Black,
            secondaryContainer = primary.copy(alpha = 0.1f),
            onSecondaryContainer = primary,
            tertiary = primary.copy(alpha = 0.5f),
            onTertiary = Color.Black,
            tertiaryContainer = primary.copy(alpha = 0.08f),
            onTertiaryContainer = primary,
            background = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF1C1C1E),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Color(0xFF8E8E93),
            error = Color(0xFFFF453A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFF453A).copy(alpha = 0.15f),
            onErrorContainer = Color(0xFFFF453A)
        )
    }

    /**
     * Parse hex color string to Color
     */
    private fun parseColor(colorString: String): Color? {
        return try {
            val hex = colorString.removePrefix("#")
            when (hex.length) {
                6 -> Color(
                    red = hex.substring(0, 2).toInt(16) / 255f,
                    green = hex.substring(2, 4).toInt(16) / 255f,
                    blue = hex.substring(4, 6).toInt(16) / 255f
                )
                8 -> Color(
                    alpha = hex.substring(0, 2).toInt(16) / 255f,
                    red = hex.substring(2, 4).toInt(16) / 255f,
                    green = hex.substring(4, 6).toInt(16) / 255f,
                    blue = hex.substring(6, 8).toInt(16) / 255f
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Theme state containing all cloud-configurable UI parameters
 */
data class ThemeState(
    val primaryAccentColor: Color = Color(0xFF2196F3),
    val calculatorStyle: CalculatorStyle = CalculatorStyle.IOS,
    val vaultGridColumns: Int = 3,
    val upgradeButtonStyle: UpgradeButtonStyle = UpgradeButtonStyle.FAB,
    val colorScheme: ColorScheme = darkColorScheme(
        primary = Color(0xFF2196F3),
        onPrimary = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF1C1C1E),
        onSurface = Color.White
    )
)

/**
 * Calculator style presets
 */
enum class CalculatorStyle {
    IOS,       // iOS-style dark calculator
    MATERIAL,  // Material Design 3 calculator
    SAMSUNG;   // Samsung-style calculator

    companion object {
        fun fromString(value: String): CalculatorStyle {
            return when (value.lowercase()) {
                "material" -> MATERIAL
                "samsung" -> SAMSUNG
                else -> IOS
            }
        }
    }
}

/**
 * Upgrade button style options
 */
enum class UpgradeButtonStyle {
    FAB,     // Floating Action Button
    BANNER,  // Bottom banner
    INLINE;  // Inline button

    companion object {
        fun fromString(value: String): UpgradeButtonStyle {
            return when (value.lowercase()) {
                "banner" -> BANNER
                "inline" -> INLINE
                else -> FAB
            }
        }
    }
}
