package com.cover.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cover.app.core.animation.PulseAnimation
import com.cover.app.core.theme.*

/**
 * Phase 8: Empty States & Custom Illustrations
 * Beautiful, helpful empty states for all screens
 */

@Composable
fun EmptyVaultState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.VisibilityOff,
        title = "Your Vault is Empty",
        description = "Start hiding your photos, videos, and files. They'll be encrypted and stored securely.",
        actionText = "Import Items",
        onActionClick = onImportClick,
        modifier = modifier,
        accentColor = CyanGlow
    )
}

@Composable
fun EmptyGalleryState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.PhotoLibrary,
        title = "No Hidden Photos Yet",
        description = "Import photos from your gallery to keep them private and secure.",
        actionText = "Add Photos",
        onActionClick = onImportClick,
        modifier = modifier,
        accentColor = EmeraldSuccess
    )
}

@Composable
fun EmptyFilesState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.FolderOpen,
        title = "No Hidden Files",
        description = "Store documents, PDFs, and any file type securely in your vault.",
        actionText = "Add Files",
        onActionClick = onImportClick,
        modifier = modifier,
        accentColor = AmberAlert
    )
}

@Composable
fun EmptyIntruderState(
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.Verified,
        title = "No Intruders Detected",
        description = "Great! No one has tried to access your vault. We'll capture photos of anyone who tries.",
        actionText = null,
        onActionClick = null,
        modifier = modifier,
        accentColor = CyanGlow
    )
}

@Composable
fun EmptyAppsState(
    onHideAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.Apps,
        title = "No Hidden Apps",
        description = "Hide apps from your launcher completely. They'll only be accessible from this vault.",
        actionText = "Hide Apps",
        onActionClick = onHideAppsClick,
        modifier = modifier,
        accentColor = CrimsonSecurity
    )
}

@Composable
fun FirstTimeUserState(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "first_time_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon with gradient orb
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing gradient orb background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyanGlow.copy(alpha = glowAlpha),
                                    CyanGlow.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.6f
                            ),
                            radius = size.width * 0.5f
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface15),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Cover",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your secret vault disguised as a calculator. Enter your PIN followed by +0= to unlock.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanGlow,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyStateIllustration(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanGlow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_illustration_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with animated gradient orb background
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing gradient orb
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = glowAlpha),
                                    accentColor.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.6f
                            ),
                            radius = size.width * 0.5f
                        )
                    }
            )
            // Icon container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface15),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ErrorStateIllustration(
    title: String = "Something Went Wrong",
    description: String = "We encountered an error. Please try again.",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.Error,
        title = title,
        description = description,
        actionText = "Try Again",
        onActionClick = onRetry,
        modifier = modifier,
        accentColor = CrimsonSecurity
    )
}

@Composable
fun NoSearchResultsState(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.Search,
        title = "No Results Found",
        description = "We couldn't find any items matching \"$query\". Try a different search term.",
        actionText = null,
        onActionClick = null,
        modifier = modifier,
        accentColor = TextTertiary
    )
}

@Composable
fun StorageFullState(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateIllustration(
        icon = Icons.Default.SdStorage,
        title = "Storage Full",
        description = "You've reached your storage limit. Upgrade to Premium for unlimited storage.",
        actionText = "Upgrade Now",
        onActionClick = onUpgrade,
        modifier = modifier,
        accentColor = GoldPremium
    )
}
