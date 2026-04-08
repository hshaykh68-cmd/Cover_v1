package com.cover.app.presentation.calculator

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display area
            CalculatorDisplay(
                display = state.display,
                expression = state.expression,
                modifier = Modifier.weight(1f)
            )

            // Keypad
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

        // Lockout overlay
        AnimatedVisibility(
            visible = lockoutState is LockoutUiState.Locked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val locked = lockoutState as? LockoutUiState.Locked
            LockoutOverlay(
                remainingMinutes = locked?.remainingMinutes ?: 0,
                remainingSeconds = locked?.remainingSeconds ?: 0
            )
        }

        // Failed attempt error
        AnimatedVisibility(
            visible = pinState is PinDetectionState.FailedAttempt,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
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

        // Capturing indicator
        AnimatedVisibility(
            visible = pinState is PinDetectionState.Capturing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Verifying...", color = Color.White)
                }
            }
        }
    }

    // Back handler - stay in calculator
    BackHandler {
        // Do nothing - keep user in calculator camouflage
    }
}

@Composable
private fun LockoutOverlay(
    remainingMinutes: Int,
    remainingSeconds: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFF453A)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Locked Out",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Too many failed attempts.",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Try again in:",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%02d:%02d", remainingMinutes, remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9F0A)
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = Color(0xFFFF453A),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
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
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
        if (expression.isNotEmpty()) {
            Text(
                text = expression,
                fontSize = 24.sp,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main display
        Text(
            text = display,
            fontSize = if (display.length > 10) 48.sp else 64.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Clear, Delete, %, Divide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyButton(
                text = "AC",
                onClick = onClearClick,
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color(0xFFFF453A),
                modifier = Modifier.weight(1f)
            )
            IconKeyButton(
                icon = Icons.Default.ArrowBack,
                onClick = onDeleteClick,
                backgroundColor = Color(0xFF2C2C2E),
                contentColor = Color(0xFFFF9F0A),
                modifier = Modifier.weight(1f)
            )
            KeyButton(
                text = "%",
                onClick = onPercentClick,
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color(0xFF64D2FF),
                modifier = Modifier.weight(1f)
            )
            OperationButton(
                operation = Operation.DIVIDE,
                onClick = { onOperationClick(Operation.DIVIDE) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: 7, 8, 9, Multiply
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberButton(number = "7", onClick = { onNumberClick("7") }, modifier = Modifier.weight(1f))
            NumberButton(number = "8", onClick = { onNumberClick("8") }, modifier = Modifier.weight(1f))
            NumberButton(number = "9", onClick = { onNumberClick("9") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.MULTIPLY,
                onClick = { onOperationClick(Operation.MULTIPLY) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: 4, 5, 6, Subtract
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberButton(number = "4", onClick = { onNumberClick("4") }, modifier = Modifier.weight(1f))
            NumberButton(number = "5", onClick = { onNumberClick("5") }, modifier = Modifier.weight(1f))
            NumberButton(number = "6", onClick = { onNumberClick("6") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.SUBTRACT,
                onClick = { onOperationClick(Operation.SUBTRACT) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 4: 1, 2, 3, Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberButton(number = "1", onClick = { onNumberClick("1") }, modifier = Modifier.weight(1f))
            NumberButton(number = "2", onClick = { onNumberClick("2") }, modifier = Modifier.weight(1f))
            NumberButton(number = "3", onClick = { onNumberClick("3") }, modifier = Modifier.weight(1f))
            OperationButton(
                operation = Operation.ADD,
                onClick = { onOperationClick(Operation.ADD) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 5: 0, ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberButton(
                number = "0",
                onClick = { onNumberClick("0") },
                modifier = Modifier.weight(2f)
            )
            KeyButton(
                text = ".",
                onClick = onDecimalClick,
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            EqualsButton(onClick = onEqualsClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumberButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeyButton(
        text = number,
        onClick = onClick,
        backgroundColor = Color(0xFF2C2C2E),
        textColor = Color.White,
        modifier = modifier
    )
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

    KeyButton(
        text = symbol,
        onClick = onClick,
        backgroundColor = Color(0xFFFF9F0A),
        textColor = Color.Black,
        modifier = modifier
    )
}

@Composable
private fun EqualsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color(0xFF30D158))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "=",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun IconKeyButton(
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}
