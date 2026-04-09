package com.cover.app.presentation.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cover.app.core.theme.*
import com.cover.app.presentation.components.CoachMarkOverlay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onChangePin: () -> Unit = {},
    onEnableBiometric: () -> Unit = {},
    onDecoyVault: () -> Unit = {},
    onIntruderSelfie: () -> Unit = {},
    onShakeToExit: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onAbout: () -> Unit = {}
) {
    // Coach mark state for first-time users
    var showCoachMark by remember { mutableStateOf(false) }
    
    // Show coach mark on first visit explaining settings
    LaunchedEffect(Unit) {
        delay(300)
        showCoachMark = true
    }
    
    // Coach mark overlay
    CoachMarkOverlay(
        isVisible = showCoachMark,
        title = "Security Settings",
        description = "Configure your vault security here. Enable biometric unlock, set up a decoy vault for extra protection, or turn on shake-to-exit for quick hiding.",
        onDismiss = { showCoachMark = false }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        NeumorphicIconButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
                )
            )
        },
        containerColor = VoidBlack
    ) { paddingValues ->
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VoidBlue,
                            VoidBlack,
                            VoidPurple.copy(alpha = 0.3f)
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Security Section
            SettingsSection(title = "Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change PIN",
                    subtitle = "Update your vault access code",
                    tint = CyanGlow,
                    onClick = onChangePin
                )
                SettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face unlock",
                    tint = CyanGlow,
                    onClick = onEnableBiometric
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features Section
            SettingsSection(title = "Features") {
                SettingsItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Decoy Vault",
                    subtitle = "Configure decoy vault with separate PIN",
                    tint = AmberAlert,
                    onClick = onDecoyVault
                )
                SettingsItem(
                    icon = Icons.Default.PhotoCamera,
                    title = "Intruder Selfie",
                    subtitle = "Capture photo of failed unlock attempts",
                    tint = CrimsonSecurity,
                    onClick = onIntruderSelfie
                )
                SettingsItem(
                    icon = Icons.Default.Vibration,
                    title = "Shake to Exit",
                    subtitle = "Emergency exit by shaking device",
                    tint = EmeraldSuccess,
                    onClick = onShakeToExit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Section
            SettingsSection(title = "Storage") {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Cache",
                    subtitle = "Free up temporary storage space",
                    tint = TextSecondary,
                    onClick = onClearCache
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About Cover",
                    subtitle = "Version 1.0.0",
                    tint = TextSecondary,
                    onClick = onAbout
                )
            }
        }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = CyanGlow,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface10)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = CyanGlow,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "settings_item_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (isPressed) {
                    drawRect(color = CyanGlow.copy(alpha = 0.05f))
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with glassmorphic background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface20),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
