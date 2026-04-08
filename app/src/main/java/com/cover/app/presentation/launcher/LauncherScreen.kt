package com.cover.app.presentation.launcher

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * LauncherScreen that displays:
 * 1. Calculator UI when locked (disguise mode)
 * 2. App grid when vault is unlocked
 * 3. Ability to hide/show apps based on vault state
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onOpenVault: () -> Unit
) {
    val launcherState by viewModel.launcherState.collectAsStateWithLifecycle()
    val calculatorState by viewModel.calculatorState.collectAsStateWithLifecycle()
    val pinState by viewModel.pinState.collectAsStateWithLifecycle()
    val lockoutState by viewModel.lockoutState.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle PIN detection feedback
    LaunchedEffect(pinState) {
        when (pinState) {
            is PinDetectionState.RealVaultUnlocked,
            is PinDetectionState.DecoyVaultUnlocked -> {
                // Transition to app grid happens automatically via launcherState
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main content switches between Calculator and App Grid
        AnimatedContent(
            targetState = launcherState,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                is LauncherDisplayState.Calculator -> {
                    CalculatorMode(
                        calculatorState = calculatorState,
                        onNumberClick = viewModel::onNumberClick,
                        onOperationClick = viewModel::onOperationClick,
                        onEqualsClick = viewModel::onEqualsClick,
                        onClearClick = viewModel::onClearClick,
                        onDeleteClick = viewModel::onDeleteClick,
                        onDecimalClick = viewModel::onDecimalClick,
                        onPercentClick = viewModel::onPercentClick
                    )
                }
                is LauncherDisplayState.AppGrid -> {
                    AppGridMode(
                        apps = visibleApps,
                        vaultState = vaultState,
                        onAppClick = { app ->
                            viewModel.launchApp(app)
                        },
                        onHideApp = { app ->
                            viewModel.hideApp(app.packageName, app.label)
                        },
                        onLockVault = viewModel::lockVault,
                        allApps = visibleApps // For showing hide option
                    )
                }
            }
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

        // PIN error feedback
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

        // Verifying indicator
        AnimatedVisibility(
            visible = pinState is PinDetectionState.Verifying,
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
}

@Composable
private fun CalculatorMode(
    calculatorState: CalculatorState,
    onNumberClick: (String) -> Unit,
    onOperationClick: (Operation) -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDecimalClick: () -> Unit,
    onPercentClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Display area
        CalculatorDisplay(
            display = calculatorState.display,
            expression = calculatorState.expression,
            modifier = Modifier.weight(1f)
        )

        // Keypad
        CalculatorKeypad(
            onNumberClick = onNumberClick,
            onOperationClick = onOperationClick,
            onEqualsClick = onEqualsClick,
            onClearClick = onClearClick,
            onDeleteClick = onDeleteClick,
            onDecimalClick = onDecimalClick,
            onPercentClick = onPercentClick,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AppGridMode(
    apps: List<AppInfo>,
    vaultState: VaultUnlockState,
    onAppClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onLockVault: () -> Unit,
    allApps: List<AppInfo>
) {
    var showHideMenu by remember { mutableStateOf<AppInfo?>(null) }
    val isUnlocked = vaultState is VaultUnlockState.Unlocked

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        Surface(
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUnlocked) "Apps" else "Calculator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                if (isUnlocked) {
                    IconButton(onClick = onLockVault) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock vault",
                            tint = Color(0xFFFF453A)
                        )
                    }
                }
            }
        }

        // App grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = {
                        if (isUnlocked) {
                            showHideMenu = app
                        }
                    }
                )
            }
        }
    }

    // Hide app dialog
    if (showHideMenu != null) {
        AlertDialog(
            onDismissRequest = { showHideMenu = null },
            title = { Text("Hide App") },
            text = { Text("Hide ${showHideMenu?.label} from the launcher?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHideMenu?.let { onHideApp(it) }
                        showHideMenu = null
                    }
                ) {
                    Text("Hide", color = Color(0xFFFF453A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideMenu = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppIconItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onClickLabel = "Launch ${app.label}"
            )
    ) {
        // App icon
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // App name
        Text(
            text = app.label,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ===== Calculator Components (reused from CalculatorScreen) =====

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
        if (expression.isNotEmpty()) {
            Text(
                text = expression,
                fontSize = 24.sp,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

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
            modifier = Modifier.size(24.dp)
        )
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
