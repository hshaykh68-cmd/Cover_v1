package com.cover.app.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cover.app.core.animation.AnimationSpecs
import com.cover.app.core.animation.rememberLifecycleAwareInfiniteTransition
import com.cover.app.core.theme.*
import com.cover.app.data.security.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var realPin by remember { mutableStateOf("") }
    var confirmRealPin by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var confirmDecoyPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val steps = listOf("Welcome", "Real PIN", "Decoy PIN", "Complete")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Cover", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
                )
            )
        },
        containerColor = VoidBlack
    ) { padding ->
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VoidBlue,
                            VoidBlack,
                            VoidPurple.copy(alpha = 0.2f)
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.03f),
                        radius = size.width * 0.3f,
                        center = Offset(size.width * 0.5f, size.height * 0.2f)
                    )
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator with glow
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / steps.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = CyanGlow,
                    trackColor = Surface20,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                0 -> WelcomeStep(
                    onNext = { currentStep++ }
                )
                1 -> PinSetupStep(
                    title = "Create Real PIN",
                    subtitle = "This unlocks your real vault. Format: 4-8 digits + 0 =",
                    example = "1234 + 0 =",
                    pin = realPin,
                    onPinChange = { realPin = it; errorMessage = null },
                    confirmPin = confirmRealPin,
                    onConfirmPinChange = { confirmRealPin = it; errorMessage = null },
                    errorMessage = errorMessage,
                    onNext = {
                        when {
                            !PinManager.isValidPin(realPin) -> errorMessage = "PIN must be 4-8 digits"
                            realPin != confirmRealPin -> errorMessage = "PINs don't match"
                            else -> currentStep++
                        }
                    },
                    onBack = { currentStep-- }
                )
                2 -> PinSetupStep(
                    title = "Create Decoy PIN",
                    subtitle = "This opens a fake vault. Format: 4-8 digits + 1 =",
                    example = "5678 + 1 =",
                    pin = decoyPin,
                    onPinChange = { decoyPin = it; errorMessage = null },
                    confirmPin = confirmDecoyPin,
                    onConfirmPinChange = { confirmDecoyPin = it; errorMessage = null },
                    errorMessage = errorMessage,
                    onNext = {
                        when {
                            !PinManager.isValidPin(decoyPin) -> errorMessage = "PIN must be 4-8 digits"
                            decoyPin == realPin -> errorMessage = "Decoy PIN must differ from real PIN"
                            decoyPin != confirmDecoyPin -> errorMessage = "PINs don't match"
                            else -> {
                                isLoading = true
                                viewModel.setupPins(realPin, decoyPin) { success, error ->
                                    isLoading = false
                                    if (success) {
                                        currentStep++
                                    } else {
                                        errorMessage = error ?: "Setup failed"
                                    }
                                }
                            }
                        }
                    },
                    onBack = { currentStep-- }
                )
                3 -> CompleteStep(
                    onFinish = onComplete
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CyanGlow,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    val infiniteTransition = rememberLifecycleAwareInfiniteTransition(label = "shield_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = AnimationSpecs.EASE_IN_OUT_SINE),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_glow"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = AnimationSpecs.EASE_IN_OUT_SINE),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            color = CyanGlow.copy(alpha = glowAlpha),
                            radius = size.width * 0.5f
                        )
                        drawCircle(
                            color = CyanGlow.copy(alpha = glowAlpha * 0.5f),
                            radius = size.width * 0.4f
                        )
                    }
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = CyanGlow
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Cover",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your private vault disguised as a calculator.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        FeatureItem(
            icon = Icons.Default.Edit,
            title = "Calculator Camouflage",
            description = "Looks like a normal calculator",
            tint = CyanGlow
        )

        FeatureItem(
            icon = Icons.Default.Lock,
            title = "Military-Grade Encryption",
            description = "AES-256 encryption for all files",
            tint = EmeraldSuccess
        )

        FeatureItem(
            icon = Icons.Default.Warning,
            title = "Intruder Detection",
            description = "Photos of anyone who tries to break in",
            tint = CrimsonSecurity
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanGlow,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PinSetupStep(
    title: String,
    subtitle: String,
    example: String,
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    errorMessage: String?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Example badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Surface15)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Example: $example",
                color = AmberAlert,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Input 1
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 8 && it.all { c -> c.isDigit() }) onPinChange(it)
            },
            label = { Text("Enter PIN", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanGlow,
                unfocusedBorderColor = Surface20,
                focusedLabelColor = CyanGlow,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = Surface10,
                unfocusedContainerColor = Surface10,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PIN Input 2
        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 8 && it.all { c -> c.isDigit() }) onConfirmPinChange(it)
            },
            label = { Text("Confirm PIN", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanGlow,
                unfocusedBorderColor = Surface20,
                focusedLabelColor = CyanGlow,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = Surface10,
                unfocusedContainerColor = Surface10,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonSecurity.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CrimsonSecurity,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = CrimsonSecurity,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Surface20)
                )
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGlow,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CompleteStep(
    onFinish: () -> Unit
) {
    val infiniteTransition = rememberLifecycleAwareInfiniteTransition(label = "complete_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = AnimationSpecs.EASE_IN_OUT_SINE),
            repeatMode = RepeatMode.Reverse
        ),
        label = "complete_glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Success check with glow
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            color = EmeraldSuccess.copy(alpha = glowAlpha),
                            radius = size.width * 0.5f
                        )
                    }
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = EmeraldSuccess
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup Complete!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your vault is ready. Remember your PINs:\n\nReal PIN: 4-8 digits + 0 =\nDecoy PIN: 4-8 digits + 1 =",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tip card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface10)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberAlert,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Tip:",
                        color = AmberAlert,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shake your phone to instantly close the app when someone approaches.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldSuccess,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Enter Vault",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tint: Color = CyanGlow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with glassmorphic background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface15),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
