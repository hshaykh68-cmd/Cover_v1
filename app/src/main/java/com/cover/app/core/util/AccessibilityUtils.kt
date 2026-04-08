package com.cover.app.core.util

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityEventCompat
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Phase 8: Accessibility Compliance (WCAG 2.1 AA)
 * - Minimum touch targets 48dp
 * - Content descriptions for all icons
 * - Color contrast compliance
 * - Screen reader support
 */

object AccessibilityUtils {
    
    /** Minimum touch target size per Material Design guidelines */
    const val MIN_TOUCH_TARGET_SIZE_DP = 48
    
    /** Minimum color contrast ratio for WCAG 2.1 AA (4.5:1 for normal text) */
    const val MIN_CONTRAST_RATIO_NORMAL = 4.5f
    
    /** Minimum color contrast ratio for large text (3:1) */
    const val MIN_CONTRAST_RATIO_LARGE = 3.0f
    
    /**
     * Calculate relative luminance of a color
     */
    fun calculateLuminance(red: Float, green: Float, blue: Float): Float {
        val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    
    /**
     * Calculate contrast ratio between two luminances
     */
    fun calculateContrastRatio(luminance1: Float, luminance2: Float): Float {
        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}

/**
 * Modifier extension for accessible touch targets
 * Ensures minimum 48dp touch target
 */
fun Modifier.minimumTouchTarget(): Modifier {
    return this.sizeIn(
        minWidth = AccessibilityUtils.MIN_TOUCH_TARGET_SIZE_DP.dp,
        minHeight = AccessibilityUtils.MIN_TOUCH_TARGET_SIZE_DP.dp
    )
}

/**
 * Modifier for icon buttons with content description
 */
fun Modifier.iconButtonAccessibility(
    contentDescription: String,
    stateDescription: String? = null
): Modifier {
    return this
        .minimumTouchTarget()
        .semantics {
            this.contentDescription = contentDescription
            stateDescription?.let { this.stateDescription = it }
            role = Role.Button
        }
}

/**
 * Modifier for toggle buttons (checkbox/switch)
 */
fun Modifier.toggleAccessibility(
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
): Modifier {
    return this
        .minimumTouchTarget()
        .semantics {
            this.contentDescription = contentDescription
            this.stateDescription = if (checked) "Checked" else "Not checked"
            role = Role.Checkbox
            toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On 
                             else androidx.compose.ui.state.ToggleableState.Off
        }
}

/**
 * Modifier for image content descriptions
 */
fun Modifier.imageAccessibility(
    contentDescription: String,
    isDecorative: Boolean = false
): Modifier {
    return if (isDecorative) {
        this.semantics { 
            // Hide from screen reader for decorative images
        }
    } else {
        this.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
}

/**
 * Modifier for list items
 */
fun Modifier.listItemAccessibility(
    index: Int,
    totalCount: Int,
    contentDescription: String
): Modifier {
    return this.semantics {
        this.contentDescription = "$contentDescription, item $index of $totalCount"
        collectionInfo = androidx.compose.ui.semantics.CollectionInfo(
            rowCount = totalCount,
            columnCount = 1
        )
    }
}

/**
 * Modifier for progress indicators
 */
fun Modifier.progressAccessibility(
    progress: Float,
    max: Float = 100f,
    label: String
): Modifier {
    val percent = ((progress / max) * 100).toInt()
    return this.semantics {
        this.progressBarRangeInfo = ProgressBarRangeInfo(
            current = progress,
            range = 0f..max,
            steps = 0
        )
        this.contentDescription = "$label, $percent percent"
    }
}

/**
 * Accessible Icon Button with 48dp touch target
 */
@Composable
fun AccessibleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        enabled = enabled
    ) {
        content()
    }
}

/**
 * Accessible Switch with proper labeling
 */
@Composable
fun AccessibleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .minimumTouchTarget()
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = if (checked) "On" else "Off"
                toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On
                                 else androidx.compose.ui.state.ToggleableState.Off
            },
        enabled = enabled
    )
}

/**
 * Accessible Card with selection state
 */
@Composable
fun AccessibleCard(
    onClick: () -> Unit,
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .minimumTouchTarget()
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = if (selected) "Selected" else "Not selected"
                this.selected = selected
            }
    ) {
        content()
    }
}

/**
 * Heading semantics for screen reader navigation
 */
fun Modifier.heading(): Modifier {
    return this.semantics { 
        heading()
    }
}

/**
 * Live region for announcements
 */
fun Modifier.liveRegion(mode: androidx.compose.ui.semantics.LiveRegionMode = LiveRegionMode.Polite): Modifier {
    return this.semantics {
        liveRegion = mode
    }
}
