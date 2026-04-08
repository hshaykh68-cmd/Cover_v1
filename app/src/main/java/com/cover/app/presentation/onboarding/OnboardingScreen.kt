package com.cover.app.presentation.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text("Setup Cover", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / steps.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF30D158),
                trackColor = Color(0xFF2C2C2E)
            )

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
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color(0xFF30D158)
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF30D158)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Cover",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your private vault disguised as a calculator.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        FeatureItem(
            icon = Icons.Default.Edit,
            title = "Calculator Camouflage",
            description = "Looks like a normal calculator"
        )

        FeatureItem(
            icon = Icons.Default.Lock,
            title = "Military-Grade Encryption",
            description = "AES-256 encryption for all files"
        )

        FeatureItem(
            icon = Icons.Default.Warning,
            title = "Intruder Detection",
            description = "Photos of anyone who tries to break in"
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF30D158)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF2C2C2E),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Example: $example",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFFF9F0A),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { 
                if (it.length <= 8 && it.all { c -> c.isDigit() }) onPinChange(it)
            },
            label = { Text("Enter PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF30D158),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF30D158),
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { 
                if (it.length <= 8 && it.all { c -> c.isDigit() }) onConfirmPinChange(it)
            },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF30D158),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF30D158),
                unfocusedLabelColor = Color.Gray
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFFF453A),
                fontSize = 14.sp
            )
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
                    contentColor = Color.White
                )
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF30D158)
                )
            ) {
                Text("Next", color = Color.Black)
            }
        }
    }
}

@Composable
private fun CompleteStep(
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF30D158)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup Complete!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your vault is ready. Remember your PINs:\n\nReal PIN: 4-8 digits + 0 =\nDecoy PIN: 4-8 digits + 1 =",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Tip:",
                    color = Color(0xFFFF9F0A),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shake your phone to instantly close the app when someone approaches.",
                    color = Color.Gray,
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
                containerColor = Color(0xFF30D158)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Enter Vault",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF2C2C2E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = Color(0xFF64D2FF)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
