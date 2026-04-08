package com.cover.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onChangePin: () -> Unit = {},
    onEnableBiometric: () -> Unit = {},
    onDecoyVault: () -> Unit = {},
    onIntruderSelfie: () -> Unit = {},
    onShakeToExit: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onAbout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    onClick = onChangePin
                )
                SettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face unlock",
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
                    onClick = onDecoyVault
                )
                SettingsItem(
                    icon = Icons.Default.PhotoCamera,
                    title = "Intruder Selfie",
                    subtitle = "Capture photo of failed unlock attempts",
                    onClick = onIntruderSelfie
                )
                SettingsItem(
                    icon = Icons.Default.Vibration,
                    title = "Shake to Exit",
                    subtitle = "Emergency exit by shaking device",
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
                    onClick = onAbout
                )
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
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
