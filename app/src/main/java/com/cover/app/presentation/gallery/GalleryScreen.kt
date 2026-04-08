package com.cover.app.presentation.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cover.app.domain.model.HiddenItem
import com.cover.app.presentation.components.BannerAd
import com.cover.app.presentation.components.ImportProgressDialog
import com.cover.app.domain.usecase.ImportProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    vaultId: String,
    pin: String,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Load items when screen is displayed
    LaunchedEffect(vaultId, pin) {
        viewModel.loadVaultItems(vaultId, pin)
    }
    
    // Media picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(vaultId, pin, uris)
        }
    }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(vaultId, pin, uris.map { it.toString() })
        }
    }
    
    // Import progress dialog
    val importProgress = state.importProgress
    if (importProgress is ImportProgress.InProgress || 
        importProgress is ImportProgress.Started) {
        ImportProgressDialog(
            progress = importProgress,
            onCancel = { viewModel.cancelImport() }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Gallery (${state.items.size})",
                        color = Color.White
                    )
                },
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
                    if (!state.isPremium && state.items.size >= 50) {
                        IconButton(onClick = onNavigateToUpgrade) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Upgrade",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            if (state.isSelectionMode) Icons.Default.Close else Icons.Default.CheckCircle,
                            contentDescription = "Select",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            ImportSpeedDial(
                onImportPhotos = { mediaPickerLauncher.launch("image/*") },
                onImportVideos = { mediaPickerLauncher.launch("video/*") },
                onImportFiles = { filePickerLauncher.launch(arrayOf("*/*")) }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Banner ad at top (hidden for premium)
                BannerAd(
                    adMobManager = viewModel.adMobManager,
                    isPremium = state.isPremium,
                    modifier = Modifier.fillMaxWidth()
                )

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        state.items.isEmpty() -> {
                            EmptyGalleryState(
                                onImportClick = { mediaPickerLauncher.launch("image/*") },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            PhotoGrid(
                                items = state.items,
                                selectedItems = state.selectedItems,
                                isSelectionMode = state.isSelectionMode,
                                onItemClick = { item ->
                                    if (state.isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        viewModel.openItem(item)
                                    }
                                },
                                onItemLongPress = { item ->
                                    viewModel.showItemOptions(item)
                                }
                            )
                        }
                    }
                }

                // Banner ad at bottom (hidden for premium)
                BannerAd(
                    adMobManager = viewModel.adMobManager,
                    isPremium = state.isPremium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Selection mode bottom bar
            AnimatedVisibility(
                visible = state.isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SelectionBottomBar(
                    selectedCount = state.selectedItems.size,
                    onDeleteSelected = { viewModel.deleteSelectedItems(vaultId) },
                    onExportSelected = { 
                        // For export, need to get destination URI first (would use activity result)
                        // For now, just trigger export with empty URI - will be handled by viewModel
                    },
                    onClearSelection = { viewModel.clearSelection() }
                )
            }
        }
    }
    
    // Snackbar
    state.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar
            viewModel.clearSnackbar()
        }
    }
}

@Composable
private fun PhotoGrid(
    items: List<HiddenItem>,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongPress: (HiddenItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            PhotoGridItem(
                item = item,
                isSelected = selectedItems.contains(item.id),
                isSelectionMode = isSelectionMode,
                onClick = { onItemClick(item) },
                onLongPress = { onItemLongPress(item) }
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    item: HiddenItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else Color.Transparent
            )
    ) {
        // Thumbnail or icon
        when (item) {
            is HiddenItem.Photo -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.id) // Will need decryption logic
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is HiddenItem.Video -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            is HiddenItem.Audio -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }
            is HiddenItem.Document -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item) {
                            is HiddenItem.Audio -> Icons.Default.Audiotrack
                            is HiddenItem.Document -> Icons.Default.Description
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
        
        // Selection indicator
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else Color.Black.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.TopEnd
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // File type indicator for non-photos
        if (item !is HiddenItem.Photo && item !is HiddenItem.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = item.originalName.substringAfterLast(".", "").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ImportSpeedDial(
    onImportPhotos: () -> Unit,
    onImportVideos: () -> Unit,
    onImportFiles: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Speed dial items
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedDialItem(
                    label = "Files",
                    icon = Icons.Default.InsertDriveFile,
                    onClick = {
                        onImportFiles()
                        expanded = false
                    }
                )
                SpeedDialItem(
                    label = "Videos",
                    icon = Icons.Default.Videocam,
                    onClick = {
                        onImportVideos()
                        expanded = false
                    }
                )
                SpeedDialItem(
                    label = "Photos",
                    icon = Icons.Default.Photo,
                    onClick = {
                        onImportPhotos()
                        expanded = false
                    }
                )
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Import"
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF2C2C2E)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
    }
}

@Composable
private fun ImportProgressDialog(
    progress: ImportProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Importing...") },
        text = {
            Column {
                when (progress) {
                    is ImportProgress.Started -> {
                        Text("Preparing to import ${progress.total} items...")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is ImportProgress.InProgress -> {
                        Text("Imported ${progress.completed} of ${progress.total}")
                        LinearProgressIndicator(
                            progress = { progress.completed.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (progress.failed > 0) {
                            Text(
                                "${progress.failed} failed",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1C1C1E)
    )
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        color = Color(0xFF1C1C1E),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onExportSelected) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onClearSelection) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gallery is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to import photos and videos",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Photos")
        }
    }
}
