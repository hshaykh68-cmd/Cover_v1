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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cover.app.domain.model.IntruderLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: IntruderLogsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedLog by remember { mutableStateOf<IntruderLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruder Logs", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (state.logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllLogs() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail or icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                if (log.photoId != null) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Intruder photo",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Gray
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "No photo",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFF453A)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (log.isDecoyVault) "Decoy vault accessed" else "Real vault attempt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (log.isDecoyVault) Color(0xFFFF9F0A) else MaterialTheme.colorScheme.error
                )
                log.location?.let { location ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${location.latitude.toString().take(8)}, ${location.longitude.toString().take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray
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
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(
                text = "Intruder Details",
                color = Color.White
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
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
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
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyLogsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No intruder activity",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your vault is secure. Failed login attempts will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
