package com.cover.app.presentation.intruder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cover.app.core.theme.*
import com.cover.app.domain.model.IntruderLog
import com.cover.app.presentation.components.CoachMarkOverlay
import com.cover.app.presentation.components.NeumorphicIconButton
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderLogsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: IntruderLogsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<IntruderLog?>(null) }
    
    // Coach mark state for first-time users
    var showCoachMark by remember { mutableStateOf(false) }
    
    // Show coach mark explaining intruder selfies feature
    LaunchedEffect(state.logs) {
        if (state.logs.isNotEmpty() && !showCoachMark) {
            delay(500)
            showCoachMark = true
        }
    }
    
    // Coach mark overlay
    CoachMarkOverlay(
        isVisible = showCoachMark,
        title = "Intruder Selfies",
        description = "When someone enters the wrong PIN, we capture their photo secretly. This helps you identify unauthorized access attempts to your vault.",
        onDismiss = { showCoachMark = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruder Logs", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        NeumorphicIconButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack
                        )
                    }
                },
                actions = {
                    if (state.logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllLogs() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = CrimsonSecurity
                            )
                        }
                    }
                },
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
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VoidBlack,
                            VoidPurple.copy(alpha = 0.2f),
                            VoidBlack
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CrimsonSecurity,
                            strokeWidth = 3.dp
                        )
                    }
                }
                state.logs.isEmpty() -> {
                    EmptyLogsState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    IntruderLogsList(
                        logs = state.logs,
                        onLogClick = { selectedLog = it },
                        onDeleteLog = { viewModel.deleteLog(it.id) }
                    )
                }
            }
        }

        // Detail dialog
        selectedLog?.let { log ->
            IntruderDetailDialog(
                log = log,
                photoBitmap = state.photos[log.id],
                onDismiss = { selectedLog = null },
                onDelete = {
                    viewModel.deleteLog(log.id)
                    selectedLog = null
                }
            )
        }
    }
}

@Composable
private fun IntruderLogsList(
    logs: List<IntruderLog>,
    onLogClick: (IntruderLog) -> Unit,
    onDeleteLog: (IntruderLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            IntruderLogCard(
                log = log,
                onClick = { onLogClick(log) },
                onDelete = { onDeleteLog(log) }
            )
        }
    }
}

@Composable
private fun IntruderLogCard(
    log: IntruderLog,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface10)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail or icon with glassmorphic background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface15),
                contentAlignment = Alignment.Center
            ) {
                if (log.photoId != null) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Intruder photo",
                        modifier = Modifier.size(32.dp),
                        tint = TextSecondary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "No photo",
                        modifier = Modifier.size(32.dp),
                        tint = CrimsonSecurity
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (log.isDecoyVault) "Decoy vault accessed" else "Real vault attempt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (log.isDecoyVault) AmberAlert else CrimsonSecurity
                )
                log.location?.let { location ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${location.latitude.toString().take(8)}, ${location.longitude.toString().take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Delete button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonSecurity.copy(alpha = 0.1f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = CrimsonSecurity,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun IntruderDetailDialog(
    log: IntruderLog,
    photoBitmap: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface15,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Intruder Details",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo
                if (photoBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Intruder photo",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface20),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details
                DetailRow("Time", dateFormat.format(Date(log.timestamp)))
                DetailRow("Type", if (log.isDecoyVault) "Decoy Vault" else "Real Vault Attempt")
                log.location?.let {
                    DetailRow("Location", "${it.latitude}, ${it.longitude}")
                }
                log.attemptedPinHash?.let {
                    DetailRow("PIN Hash", it.take(16) + "...")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = CyanGlow)
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = CrimsonSecurity
                )
            ) {
                Text("Delete")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            color = TextTertiary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyLogsState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = AnimationSpecs.EASE_IN_OUT_SINE),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_glow"
    )

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing glow behind shield
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            color = CyanGlow.copy(alpha = glowAlpha),
                            radius = size.width * 0.5f
                        )
                    }
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CyanGlow
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Intruder Activity",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your vault is secure. Failed login attempts will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
