package com.cover.app.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cover.app.domain.model.HiddenItem
import com.cover.app.presentation.components.BannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<HiddenItem?>(null) }
    var showItemOptions by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VaultEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is VaultEvent.LockVault -> {
                    onNavigateBack()
                }
                is VaultEvent.NavigateToSettings -> {
                    onNavigateToSettings()
                }
                is VaultEvent.ShowAddItemDialog -> {
                    showAddDialog = true
                }
                is VaultEvent.OpenItem -> {
                    // TODO: Navigate to item viewer
                    snackbarHostState.showSnackbar("Opening ${event.item.originalName}...")
                }
                is VaultEvent.ShowItemOptions -> {
                    selectedItem = event.item
                    showItemOptions = true
                }
            }
        }
    }
    
    // Add Item Dialog
    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onImportPhotos = {
                showAddDialog = false
                // TODO: Navigate to import screen
            },
            onTakePhoto = {
                showAddDialog = false
                // TODO: Open camera
            },
            onAddFiles = {
                showAddDialog = false
                // TODO: Open file picker
            }
        )
    }
    
    // Item Options Dialog
    if (showItemOptions && selectedItem != null) {
        ItemOptionsDialog(
            item = selectedItem!!,
            onDismiss = { 
                showItemOptions = false
                selectedItem = null
            },
            onDelete = {
                viewModel.onDeleteItem(selectedItem!!)
                showItemOptions = false
                selectedItem = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vault") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onLockVault() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onSettingsClick() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddItemClick() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                state.items.isEmpty() -> {
                    EmptyVaultState(
                        onAddClick = { viewModel.onAddItemClick() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    VaultGrid(
                        items = state.items,
                        onItemClick = viewModel::onItemClick,
                        onItemLongPress = viewModel::onItemLongPress
                    )
                }
            }
        }

        // Error handling
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::onClearError,
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = viewModel::onClearError) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
private fun EmptyVaultState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Vault is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add photos, videos, or files",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Items")
        }
    }
}

@Composable
private fun VaultGrid(
    items: List<HiddenItem>,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongPress: (HiddenItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items, key = { it.id }) { item ->
            VaultItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onLongPress = { onItemLongPress(item) }
            )
        }
    }
}

@Composable
private fun VaultItemCard(
    item: HiddenItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val icon = when (item) {
        is HiddenItem.Photo -> Icons.Default.Image
        is HiddenItem.Video -> Icons.Default.PlayCircle
        is HiddenItem.Audio -> Icons.Default.Audiotrack
        is HiddenItem.Document -> Icons.Default.Description
        is HiddenItem.App -> Icons.Default.Adb
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.originalName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onImportPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onAddFiles: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Vault") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Import Photos/Videos") },
                    leadingContent = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.clickable(onClick = onImportPhotos)
                )
                ListItem(
                    headlineContent = { Text("Take Photo") },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, null) },
                    modifier = Modifier.clickable(onClick = onTakePhoto)
                )
                ListItem(
                    headlineContent = { Text("Add Files") },
                    leadingContent = { Icon(Icons.Default.Description, null) },
                    modifier = Modifier.clickable(onClick = onAddFiles)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1C1C1E)
    )
}

@Composable
private fun ItemOptionsDialog(
    item: HiddenItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.originalName) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Delete") },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF453A)) },
                    colors = ListItemDefaults.colors(
                        headlineColor = Color(0xFFFF453A)
                    ),
                    modifier = Modifier.clickable(onClick = onDelete)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1C1C1E)
    )
}
