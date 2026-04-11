package com.cover.app.presentation.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cover.app.core.theme.*
import com.cover.app.domain.model.HiddenItem
import com.cover.app.presentation.components.BannerAd
import com.cover.app.presentation.components.CoachMarkOverlay
import com.cover.app.presentation.components.NeumorphicIconButton
import com.cover.app.domain.usecase.ImportProgress
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    vaultId: String = "",
    pin: String = "",
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToUpgrade: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Coach mark state for first-time users
    var showCoachMark by remember { mutableStateOf(false) }
    
    // Show coach mark on first visit explaining gallery
    LaunchedEffect(Unit) {
        delay(500)
        if (state.items.isNotEmpty()) {
            showCoachMark = true
        }
    }
    
    // Load items when screen is displayed (only for legacy routes with vaultId)
    LaunchedEffect(vaultId, pin) {
        if (vaultId.isNotEmpty()) {
            viewModel.loadVaultItems(vaultId, pin)
        } else {
            // For new navigation, use default vault loading
            viewModel.loadDefaultVaultItems()
        }
    }
    
    // Media picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (vaultId.isNotEmpty()) {
                viewModel.importMedia(vaultId, pin, uris)
            } else {
                viewModel.importMediaToDefault(uris)
            }
        }
    }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (vaultId.isNotEmpty()) {
                viewModel.importFiles(vaultId, pin, uris.map { it.toString() })
            } else {
                viewModel.importFilesToDefault(uris.map { it.toString() })
            }
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
    
    // Coach mark overlay
    CoachMarkOverlay(
        isVisible = showCoachMark,
        title = "Your Hidden Gallery",
        description = "Your files are encrypted and stored securely. Tap the + button to import more items. Long press any item to select multiple for batch operations.",
        onDismiss = { showCoachMark = false }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gallery (${state.items.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        NeumorphicIconButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack
                        )
                    }
                },
                actions = {
                    if (!state.isPremium && state.items.size >= 50) {
                        IconButton(onClick = onNavigateToUpgrade) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Upgrade",
                                tint = AmberAlert
                            )
                        }
                    }
                    NeumorphicIconButton(
                        onClick = { viewModel.toggleSelectionMode() },
                        icon = if (state.isSelectionMode) Icons.Default.Close else Icons.Default.CheckCircle
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
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
        containerColor = VoidBlack
    ) { padding ->
        // Gradient background with glow
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
                        endY = 1000f
                    )
                )
                .drawBehind {
                    // Cinematic glow effect at top
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.03f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.5f, size.height * 0.1f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                            Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = CyanGlow,
                                    strokeWidth = 3.dp
                                )
                            }
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
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "grid_item_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface15)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    drawRect(
                        color = CyanGlow.copy(alpha = 0.3f),
                        size = size
                    )
                }
            }
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
                        tint = CyanGlow.copy(alpha = 0.9f)
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
                        tint = TextSecondary
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
                        tint = TextSecondary
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
                        tint = TextSecondary
                    )
                }
            }
        }
        
        // Selection indicator
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (isSelected) {
                            drawRect(color = CyanGlow.copy(alpha = 0.4f))
                        } else {
                            drawRect(color = VoidBlack.copy(alpha = 0.3f))
                        }
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanGlow)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(18.dp),
                            tint = VoidBlack
                        )
                    }
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
                    color = TextSecondary,
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
        
        // Main FAB with glow
        Box(
            modifier = Modifier.drawBehind {
                if (expanded) {
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.3f),
                        radius = size.width * 0.6f
                    )
                }
            }
        ) {
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = CyanGlow,
                contentColor = VoidBlack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Import"
                )
            }
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
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = Surface15,
            contentColor = CyanGlow,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = label, tint = CyanGlow)
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
        title = { Text("Importing...", color = TextPrimary) },
        text = {
            Column {
                when (progress) {
                    is ImportProgress.Started -> {
                        Text("Preparing to import ${progress.total} items...", color = TextSecondary)
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyanGlow,
                            trackColor = Surface20
                        )
                    }
                    is ImportProgress.InProgress -> {
                        Text("Imported ${progress.completed} of ${progress.total}", color = TextSecondary)
                        LinearProgressIndicator(
                            progress = { progress.completed.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(),
                            color = CyanGlow,
                            trackColor = Surface20
                        )
                        if (progress.failed > 0) {
                            Text(
                                "${progress.failed} failed",
                                color = CrimsonSecurity,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = CyanGlow)
            ) {
                Text("Cancel")
            }
        },
        containerColor = Surface15,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface15)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export button with glassmorphic background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface20)
                        .clickable(onClick = onExportSelected),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export",
                        tint = CyanGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Delete button with red background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimsonSecurity.copy(alpha = 0.15f))
                        .clickable(onClick = onDeleteSelected),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CrimsonSecurity,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Clear button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface20)
                        .clickable(onClick = onClearSelection),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
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
    val infiniteTransition = rememberInfiniteTransition(label = "empty_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow background
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
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Gallery is Empty",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your hidden memories await",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanGlow,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Photos", fontWeight = FontWeight.Medium)
        }
    }
}
