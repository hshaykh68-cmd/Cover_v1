package com.cover.app.presentation.calculator

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cover.app.core.animation.AnimationSpecs
import com.cover.app.core.theme.*
import kotlinx.coroutines.delay

@Composable
fun CalculatorScreen(
    onRealVaultAccess: (String) -> Unit,
    onDecoyVaultAccess: (String) -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pinState by viewModel.pinDetectionState.collectAsStateWithLifecycle()
    val lockoutState by viewModel.lockoutState.collectAsStateWithLifecycle()
    val emergencyState by viewModel.emergencyState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle PIN detection states
    LaunchedEffect(pinState) {
        when (val ps = pinState) {
            is PinDetectionState.RealVaultDetected -> {
                delay(300) // Brief delay for transition
                onRealVaultAccess(ps.pin)
                viewModel.resetPinDetection()
            }
            is PinDetectionState.DecoyVaultDetected -> {
                delay(300)
                onDecoyVaultAccess(ps.pin)
                viewModel.resetPinDetection()
            }
            is PinDetectionState.FailedAttempt -> {
                delay(1500) // Show error briefly
                viewModel.resetPinDetection()
            }
            else -> {}
        }
    }

    // Handle emergency shake - exit app
    LaunchedEffect(emergencyState) {
        if (emergencyState == EmergencyState.ShakeDetected) {
            (context as? Activity)?.finishAffinity()
            viewModel.acknowledgeEmergency()
        }
    }

    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(VoidBlue, VoidBlack, VoidPurple),
                    startY = 0f,
                    endY = 2000f
                )
            )
    ) {
        // Subtle ambient glow in background
        AmbientGlow()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display area
            CalculatorDisplay(
                display = state.display,
                expression = state.expression,
                modifier = Modifier.weight(1f)
            )

            // Keypad with neumorphic design
            CalculatorKeypad(
                onNumberClick = viewModel::onNumberClick,
                onOperationClick = viewModel::onOperationClick,
                onEqualsClick = viewModel::onEqualsClick,
                onClearClick = viewModel::onClearClick,
                onDeleteClick = viewModel::onDeleteClick,
                onDecimalClick = viewModel::onDecimalClick,
                onPercentClick = viewModel::onPercentClick,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Lockout overlay with glassmorphism
        AnimatedVisibility(
            visible = lockoutState is LockoutUiState.Locked,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            val locked = lockoutState as? LockoutUiState.Locked
            LockoutOverlay(
                remainingMinutes = locked?.remainingMinutes ?: 0,
                remainingSeconds = locked?.remainingSeconds ?: 0
            )
        }

        // Failed attempt error with shake animation
        AnimatedVisibility(
            visible = pinState is PinDetectionState.FailedAttempt,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val failed = pinState as? PinDetectionState.FailedAttempt
            ErrorBanner(
                message = if (failed?.isLockedOut == true) {
                    "Too many failed attempts. Locked out."
                } else {
                    "Incorrect PIN. ${failed?.remainingAttempts ?: 0} attempts remaining."
                }
            )
        }

        // Capturing indicator with pulsing glow
        AnimatedVisibility(
            visible = pinState is PinDetectionState.Capturing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CapturingIndicator()
        }
    }

    // Back handler - stay in calculator
    BackHandler {
        // Do nothing - keep user in calculator camouflage
    }
}

@Composable
private fun AmbientGlow() {
    // Subtle animated glow in background
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Top-right cyan glow
                drawCircle(
                    color = CyanGlow.copy(alpha = glowAlpha),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.8f, size.height * 0.1f)
                )
                // Bottom-left subtle purple
                drawCircle(
                    color = Surface30.copy(alpha = glowAlpha * 0.5f),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.2f, size.height * 0.9f)
                )
            }
    )
}

@Composable
private fun LockoutOverlay(
    remainingMinutes: Int,
    remainingSeconds: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.95f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        // Glassmorphic card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = Surface10,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(32.dp)
        ) {
            // Pulsing lock icon
            val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lock_scale"
            )

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = CrimsonSecurity
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Locked Out",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Too many failed attempts.",
                fontSize = 16.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Try again in:",
                fontSize = 14.sp,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = AmberAlert
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    // Shake animation for error
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(message) {
        shakeAnim.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 500
                -10f at 50
                10f at 100
                -10f at 150
                10f at 200
                0f at 500
            }
        )
    }

    Surface(
        color = CrimsonSecurity.copy(alpha = 0.9f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeAnim.value.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CapturingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "capturing_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Surface(
        color = Surface10,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(24.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .drawBehind {
                    drawCircle(
                        color = CyanGlow.copy(alpha = glowAlpha),
                        radius = size.width * 0.8f
                    )
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = CyanGlow,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Verifying...",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CalculatorDisplay(
    display: String,
    expression: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Expression preview
        AnimatedVisibility(
            visible = expression.isNotEmpty(),
            enter = fadeIn() + slideInVertically { -10 },
            exit = fadeOut() + slideOutVertically { -10 }
        ) {
            Text(
                text = expression,
                fontSize = 24.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
        if (expression.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main display with animated text change
        AnimatedContent(
            targetState = display,
            transitionSpec = {
                fadeIn(animationSpec = tween(100)) with
                fadeOut(animationSpec = tween(100))
            },
            label = "display_change"
        ) { targetDisplay ->
            Text(
                text = targetDisplay,
                fontSize = if (targetDisplay.length > 10) 48.sp else 64.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CalculatorKeypad(
    onNumberClick: (String) -> Unit,
    onOperationClick: (Operation) -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDecimalClick: () -> Unit,
    onPercentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticFeedback = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: Clear, Delete, %, Divide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FunctionButton(
                text = "AC",
                onClick = { hapticFeedback(); onClearClick() },
                modifier = Modifier.weight(1f),
                accentColor = CrimsonSecurity
            )
            IconFunctionButton(
                icon = Icons.Default.ArrowBack,
                onClick = { hapticFeedback(); onDeleteClick() },
                modifier = Modifier.weight(1f),
                accentColor = AmberAlert
            )
            FunctionButton(
                text = "%",
                onClick = { hapticFeedback(); onPercentClick() },
                modifier = Modifier.weight(1f),
                accentColor = CyanGlow
            )
            OperationButton(
                operation = Operation.DIVIDE,
                onClick = { hapticFeedback(); onOperationClick(Operation.DIVIDE) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: 7, 8, 9, Multiply
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NumberButton(number = "7", onClick = { hapticFeedback(); onNumberClick("7") }, modifier = Modifier.weight(1f))
            NumberButton(number = "8", onClick = { hapticFeedback(); onNumberClick("8") }, modifier = Modifier.weight(1f))
            NumberButton(number = "9", onClick = { hapticFeedback(); onNumberClick("9") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.MULTIPLY,
                onClick = { hapticFeedback(); onOperationClick(Operation.MULTIPLY) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: 4, 5, 6, Subtract
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NumberButton(number = "4", onClick = { hapticFeedback(); onNumberClick("4") }, modifier = Modifier.weight(1f))
            NumberButton(number = "5", onClick = { hapticFeedback(); onNumberClick("5") }, modifier = Modifier.weight(1f))
            NumberButton(number = "6", onClick = { hapticFeedback(); onNumberClick("6") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.SUBTRACT,
                onClick = { hapticFeedback(); onOperationClick(Operation.SUBTRACT) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 4: 1, 2, 3, Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NumberButton(number = "1", onClick = { hapticFeedback(); onNumberClick("1") }, modifier = Modifier.weight(1f))
            NumberButton(number = "2", onClick = { hapticFeedback(); onNumberClick("2") }, modifier = Modifier.weight(1f))
            NumberButton(number = "3", onClick = { hapticFeedback(); onNumberClick("3") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.ADD,
                onClick = { hapticFeedback(); onOperationClick(Operation.ADD) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 5: 0, ., =, M
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NumberButton(
                number = "0",
                onClick = { hapticFeedback(); onNumberClick("0") },
                modifier = Modifier.weight(1f)
            )
            NumberButton(
                number = ".",
                onClick = { hapticFeedback(); onDecimalClick() },
                modifier = Modifier.weight(1f)
            )
            EqualsButton(onClick = { hapticFeedback(); onEqualsClick() }, modifier = Modifier.weight(1f))
            // Hidden vault access button disguised as memory button
            VaultAccessButton(onClick = { hapticFeedback() }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumberButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neumorphicButton(
                onClick = onClick,
                shape = CircleShape,
                elevation = 12.dp,
                backgroundColor = CalculatorButtonNumber,
                pressedBackgroundColor = CalculatorButtonNumber.copy(alpha = 0.8f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun FunctionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neumorphicButton(
                onClick = onClick,
                shape = CircleShape,
                elevation = 12.dp,
                backgroundColor = CalculatorButtonFunction,
                pressedBackgroundColor = CalculatorButtonFunction.copy(alpha = 0.8f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor
        )
    }
}

@Composable
private fun IconFunctionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neumorphicButton(
                onClick = onClick,
                shape = CircleShape,
                elevation = 12.dp,
                backgroundColor = CalculatorButtonFunction,
                pressedBackgroundColor = CalculatorButtonFunction.copy(alpha = 0.8f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun OperationButton(
    operation: Operation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = when (operation) {
        Operation.ADD -> "+"
        Operation.SUBTRACT -> "−"
        Operation.MULTIPLY -> "×"
        Operation.DIVIDE -> "÷"
        Operation.MODULO -> "%"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Operation buttons use stiffer spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "operation_scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(CircleShape)
            .background(
                color = if (isPressed) CyanGlow.copy(alpha = 0.15f) else CyanGlow.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .neumorphic(
                shape = CircleShape,
                elevation = 12.dp,
                pressed = isPressed,
                backgroundColor = Color.Transparent,
                pressedBackgroundColor = Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyanGlow
        )
    }
}

@Composable
private fun EqualsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = AnimationSpecs.ButtonPress,
        label = "equals_scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isPressed) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.9f),
                        if (isPressed) EmeraldSuccess.copy(alpha = 0.7f) else EmeraldSuccess
                    )
                )
            )
            .drawBehind {
                // Outer glow effect
                if (!isPressed) {
                    drawCircle(
                        color = EmeraldSuccess.copy(alpha = 0.3f),
                        radius = size.width * 0.6f,
                        center = center
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "=",
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = VoidBlack
        )
    }
}

@Composable
private fun VaultAccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neumorphicButton(
                onClick = onClick,
                shape = CircleShape,
                elevation = 12.dp,
                backgroundColor = CalculatorButtonFunction,
                pressedBackgroundColor = CalculatorButtonFunction.copy(alpha = 0.8f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "M",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = CyanGlow.copy(alpha = 0.7f)
        )
    }
}
