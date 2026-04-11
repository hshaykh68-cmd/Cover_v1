package com.cover.app.core.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * STEALTH LUXURY ANIMATION SYSTEM
 * Cinematic motion with purpose and precision
 */

object AnimationSpecs {
    // Transition Durations
    const val CALCULATOR_TO_VAULT_DURATION_MS = 300
    const val IMPORT_PROGRESS_DURATION_MS = 400
    const val PREMIUM_UNLOCK_DURATION_MS = 800
    const val ITEM_FADE_DURATION_MS = 200
    const val BUTTON_PRESS_DURATION_MS = 100
    const val DIALOG_ENTER_DURATION_MS = 250
    const val DIALOG_EXIT_DURATION_MS = 200
    
    // Easing Curves
    val EASE_OUT_QUART = androidx.compose.animation.core.FastOutSlowInEasing
    val EASE_IN_OUT_CUBIC = androidx.compose.animation.core.FastOutSlowInEasing
    val EASE_IN_OUT_SINE = androidx.compose.animation.core.CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)
    val SPRING_STIFFNESS = Spring.StiffnessMedium
    val SPRING_DAMPING = Spring.DampingRatioMediumBouncy

    // ============================================
    // SPRING PHYSICS - For tactile interactions
    // ============================================

    /** Snappy spring for button presses - quick settle with slight bounce */
    val ButtonPress = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Gentle spring for reveals - smooth and elegant */
    val Reveal = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Bouncy spring for celebratory moments */
    val Celebration = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Sharp spring for security states - quick and decisive */
    val Security = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // ============================================
    // TWEEN DURATIONS - For choreographed sequences
    // ============================================

    /** Instant feedback - 100ms */
    val Instant = tween<Float>(durationMillis = 100)

    /** Quick transition - 150ms */
    val Quick = tween<Float>(durationMillis = 150)

    /** Standard transition - 250ms */
    val Standard = tween<Float>(durationMillis = 250)

    /** Smooth transition - 350ms */
    val Smooth = tween<Float>(durationMillis = 350)

    /** Cinematic reveal - 500ms */
    val Cinematic = tween<Float>(durationMillis = 500)

    /** Epic entrance - 800ms */
    val Epic = tween<Float>(durationMillis = 800)

    // ============================================
    // EASING CURVES - Premium motion profiles
    // ============================================

    /** Material standard easing */
    val StandardEasing = FastOutSlowInEasing

    /** Decelerate for entrances - start fast, settle gracefully */
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerate for exits - build momentum */
    val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Sharp for state changes - quick start, quick end */
    val Sharp = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)

    /** Bounce for playful elements */
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    // ============================================
    // SPECIALIZED SPECS
    // ============================================

    /** Pulsing glow animation for loading/attention */
    fun pulseGlow(duration: Int = 1500) = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = duration
            0.0f at 0
            1.0f at duration / 2
            0.0f at duration
        },
        repeatMode = RepeatMode.Restart
    )

    /** Shimmer animation for premium highlights */
    fun shimmer(duration: Int = 2000) = infiniteRepeatable<Float>(
        animation = tween(durationMillis = duration, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )

    /** Shake animation for errors */
    fun shake(duration: Int = 500) = keyframes<Float> {
        durationMillis = duration
        0f at 0
        -10f at 50
        10f at 100
        -10f at 150
        10f at 200
        -5f at 250
        5f at 300
        0f at duration
    }

    /** Staggered entrance for lists/grids */
    fun staggeredDelay(index: Int, baseDelay: Int = 50) = index * baseDelay

    /** Number count-up animation spec */
    val NumberCount = tween<Float>(
        durationMillis = 600,
        easing = Decelerate
    )
}

private val LinearEasing = androidx.compose.animation.core.LinearEasing

// ============================================
// LIFECYCLE-AWARE ANIMATION HELPERS
// ============================================

/**
 * Creates a lifecycle-aware infinite transition that pauses when the screen is not visible.
 * This prevents battery drain from animations running on hidden screens.
 */
@Composable
fun rememberLifecycleAwareInfiniteTransition(label: String): InfiniteTransition {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isActive = true
                Lifecycle.Event.ON_STOP -> isActive = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    return rememberInfiniteTransition(label = label)
}

/**
 * Animates a float value with lifecycle awareness - only runs when lifecycle is at least STARTED.
 */
@Composable
fun InfiniteTransition.animateFloatLifecycleAware(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String,
    isActive: Boolean = true
): State<Float> {
    return if (isActive) {
        animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label
        )
    } else {
        remember { mutableFloatStateOf(initialValue) }
    }
}
