package com.cover.app.core.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cover.app.core.animation.AnimationSpecs

// ============================================
// NEUMORPHIC MODIFIERS - Soft 3D effects
// ============================================

/**
 * Applies neumorphic shadow effect (soft UI) to the element.
 * Creates embossed or debossed appearance with subtle shadows.
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 8.dp,
    pressed: Boolean = false,
    shadowColor: Color = VoidBlack,
    highlightColor: Color = Surface40,
    backgroundColor: Color = Surface15,
    pressedBackgroundColor: Color = Surface20
): Modifier = composed {
    val density = LocalDensity.current
    val elevationPx = with(density) { elevation.toPx() }

    this
        .graphicsLayer {
            this.shape = shape
            this.shadowElevation = elevationPx
        }
        .drawBehind {
            if (!pressed) {
                // Top-left highlight
                drawCircle(
                    color = highlightColor.copy(alpha = 0.15f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.2f, size.height * 0.2f)
                )
                // Bottom-right shadow
                drawCircle(
                    color = shadowColor.copy(alpha = 0.3f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.8f, size.height * 0.8f)
                )
            } else {
                // Inverted for pressed state (debossed)
                drawCircle(
                    color = shadowColor.copy(alpha = 0.2f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.3f, size.height * 0.3f)
                )
                drawCircle(
                    color = highlightColor.copy(alpha = 0.1f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.7f, size.height * 0.7f)
                )
            }
        }
        .background(
            color = if (pressed) pressedBackgroundColor else backgroundColor,
            shape = shape
        )
}

/**
 * Pressable neumorphic button with automatic pressed state handling
 */
fun Modifier.neumorphicButton(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(50), // Circular for calculator
    elevation: Dp = 12.dp,
    backgroundColor: Color = Surface15,
    pressedBackgroundColor: Color = Surface20,
    shadowColor: Color = VoidBlack,
    highlightColor: Color = Surface40
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationSpecs.ButtonPress,
        label = "button_scale"
    )

    this
        .scale(scale)
        .neumorphic(
            shape = shape,
            elevation = elevation,
            pressed = isPressed,
            shadowColor = shadowColor,
            highlightColor = highlightColor,
            backgroundColor = backgroundColor,
            pressedBackgroundColor = pressedBackgroundColor
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// ============================================
// GLASSMORPHISM MODIFIERS
// ============================================

/**
 * Applies glassmorphism effect with blur and transparency
 */
fun Modifier.glassmorphic(
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.15f,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = composed {
    this
        .graphicsLayer {
            this.shape = shape
            this.clip = true
        }
        .background(
            color = Surface20.copy(alpha = alpha),
            shape = shape
        )
        .padding(1.dp) // Border padding
        .drawBehind {
            // Subtle border glow
            drawRect(
                color = CyanGlow.copy(alpha = 0.1f),
                size = size
            )
        }
}

// ============================================
// GLOW EFFECTS
// ============================================

/**
 * Adds cyan glow effect to the element
 */
fun Modifier.cyanGlow(
    intensity: Float = 0.5f,
    spread: Dp = 8.dp
): Modifier = composed {
    val density = LocalDensity.current
    val spreadPx = with(density) { spread.toPx() }

    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.color = CyanGlow.copy(alpha = intensity).toArgb()
                this.maskFilter = android.graphics.BlurMaskFilter(
                    spreadPx,
                    android.graphics.BlurMaskFilter.Blur.OUTER
                )
            }
            canvas.nativeCanvas.drawCircle(
                size.width / 2,
                size.height / 2,
                size.width / 2,
                paint
            )
        }
    }
}

/**
 * Gradient border glow effect
 */
fun Modifier.gradientGlow(
    colorStart: Color = CyanGlow,
    colorEnd: Color = GlowCyanEnd,
    width: Dp = 2.dp
): Modifier = composed {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }

    this.drawBehind {
        val brush = Brush.linearGradient(
            colors = listOf(colorStart, colorEnd),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        // Draw only the border using Stroke style
        drawRect(
            brush = brush,
            size = size,
            style = Stroke(width = widthPx)
        )
    }
}

// ============================================
// SHIMMER EFFECT
// ============================================

/**
 * Shimmer loading effect modifier with animated gradient
 */
fun Modifier.shimmer(
    shimmerColor: Color = CyanGlow.copy(alpha = 0.3f),
    baseColor: Color = Surface10,
    animationDuration: Int = 1500
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    this.drawBehind {
        // Base background
        drawRect(color = baseColor)
        
        // Calculate shimmer offset based on animation
        val shimmerWidth = size.width * 0.5f
        val startX = size.width * translateAnimation
        
        // Shimmer gradient
        val brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                shimmerColor,
                baseColor
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + shimmerWidth, size.height)
        )
        
        drawRect(brush = brush)
    }
}

// ============================================
// BACKGROUND GRADIENTS
// ============================================

/**
 * Deep void gradient background
 */
fun Modifier.voidGradient(): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(VoidBlue, VoidBlack),
        tileMode = TileMode.Clamp
    )
)

/**
 * Cyan glow gradient for accent areas
 */
fun Modifier.cyanGradient(): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = listOf(CyanGlow, GlowCyanEnd),
        tileMode = TileMode.Clamp
    )
)
