package com.cover.app.presentation.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cover.app.core.animation.AnimationSpecs
import com.cover.app.core.theme.*
import com.cover.app.domain.model.HiddenItem
import com.cover.app.presentation.components.BannerAd
import com.cover.app.presentation.components.CoachMarkOverlay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToUpgrade: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<HiddenItem?>(null) }
    var showItemOptions by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<FilterType?>(null) }
    
    // Coach marks state
    var showCoachMark by remember { mutableStateOf(false) }
    var coachMarkTitle by remember { mutableStateOf("") }
    var coachMarkDesc by remember { mutableStateOf("") }
    
    // Show first-time coach marks
    LaunchedEffect(Unit) {
        // Check if first time (would use DataStore in production)
        delay(500)
        if (state.items.isEmpty()) {
            coachMarkTitle = "Welcome to Your Vault"
            coachMarkDesc = "This is your secure space. Tap + to import photos, videos, or files. Check Intruder Logs to see failed access attempts."
            showCoachMark = true
        }
    }
    
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
                    onNavigateToSettings?.invoke()
                }
                is VaultEvent.ShowAddItemDialog -> {
                    showAddDialog = true
                }
                is VaultEvent.OpenItem -> {
                    snackbarHostState.showSnackbar("Opening ${event.item.originalName}...")
                }
                is VaultEvent.ShowItemOptions -> {
                    selectedItem = event.item
                    showItemOptions = true
                }
            }
        }
    }
    
    // Coach mark overlay
    CoachMarkOverlay(
        isVisible = showCoachMark,
        title = coachMarkTitle,
        description = coachMarkDesc,
        onDismiss = { showCoachMark = false }
    )
    
    // Add Item Dialog
    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onImportPhotos = {
                showAddDialog = false
            },
            onTakePhoto = {
                showAddDialog = false
            },
            onAddFiles = {
                showAddDialog = false
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
                title = {
                    Text(
                        "My Vault",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onLockVault() }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = CyanGlow
                        )
                    }
                },
                actions = {
                    // Settings removed - now accessible via bottom navigation
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = CyanGlow,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddItemClick() },
                containerColor = CyanGlow,
                contentColor = VoidBlack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .graphicsLayer {
                        shadowElevation = 20f
                    }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Surface20,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
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
                        colors = listOf(VoidBlue, VoidBlack),
                        startY = 0f,
                        endY = 1000f
                    )
                )
                .drawBehind {
                    // Subtle ambient glow
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.03f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                    )
                }
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing glow behind indicator
                        val infiniteTransition = rememberInfiniteTransition(label = "loading_glow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = CyanGlow.copy(alpha = glowAlpha),
                                        radius = size.width / 2
                                    )
                                }
                        )

                        CircularProgressIndicator(
                            color = CyanGlow,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                state.items.isEmpty() -> {
                    EmptyVaultStateWithActions(
                        onImportPhotos = { 
                            coachMarkTitle = "Import Photos"
                            coachMarkDesc = "Select photos from your gallery to hide them in the vault."
                            showCoachMark = true
                        },
                        onTakePhoto = {
                            coachMarkTitle = "Take Photo"
                            coachMarkDesc = "Capture photos directly to the vault without saving to your gallery."
                            showCoachMark = true
                        },
                        onAddFiles = {
                            coachMarkTitle = "Add Files"
                            coachMarkDesc = "Hide any file type - documents, audio, videos, and more."
                            showCoachMark = true
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    VaultDashboard(
                        items = state.items,
                        selectedFilter = selectedFilter,
                        onFilterSelect = { selectedFilter = it },
                        onItemClick = viewModel::onItemClick,
                        onItemLongPress = viewModel::onItemLongPress,
                        totalSize = state.items.sumOf { it.size },
                        onUpgradeClick = onNavigateToUpgrade,
                        isPremium = false // TODO: Get from state
                    )
                }
            }
        }

        // Error handling
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::onClearError,
                title = { Text("Error", color = TextPrimary) },
                text = { Text(error, color = TextSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::onClearError,
                        colors = ButtonDefaults.textButtonColors(contentColor = CyanGlow)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Surface15,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }
    }
}

enum class FilterType(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.FolderOpen),
    PHOTOS("Photos", Icons.Default.Photo),
    VIDEOS("Videos", Icons.Default.VideoLibrary),
    FILES("Files", Icons.Default.InsertDriveFile)
}

@Composable
private fun VaultDashboard(
    items: List<HiddenItem>,
    selectedFilter: FilterType?,
    onFilterSelect: (FilterType?) -> Unit,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongPress: (HiddenItem) -> Unit,
    totalSize: Long,
    onUpgradeClick: () -> Unit,
    isPremium: Boolean
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Storage indicator
        StorageIndicator(
            usedBytes = totalSize,
            isPremium = isPremium,
            onUpgradeClick = onUpgradeClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick filters
        QuickFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelect = onFilterSelect,
            itemCounts = mapOf(
                FilterType.ALL to items.size,
                FilterType.PHOTOS to items.count { it is HiddenItem.Photo },
                FilterType.VIDEOS to items.count { it is HiddenItem.Video },
                FilterType.FILES to items.count { it !is HiddenItem.Photo && it !is HiddenItem.Video }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recent items section
        if (items.isNotEmpty()) {
            Text(
                text = "Recent Items",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            RecentItemsRow(
                items = items.take(6),
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // All items grid
        Text(
            text = "All Items",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Filter items based on selection
        val filteredItems = when (selectedFilter) {
            FilterType.PHOTOS -> items.filterIsInstance<HiddenItem.Photo>()
            FilterType.VIDEOS -> items.filterIsInstance<HiddenItem.Video>()
            FilterType.FILES -> items.filter { it !is HiddenItem.Photo && it !is HiddenItem.Video }
            else -> items
        }
        
        VaultGrid(
            items = filteredItems,
            onItemClick = onItemClick,
            onItemLongPress = onItemLongPress
        )
    }
}

@Composable
private fun StorageIndicator(
    usedBytes: Long,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit
) {
    val maxBytes = if (isPremium) 100L * 1024 * 1024 * 1024 else 2L * 1024 * 1024 * 1024 // 100GB or 2GB
    val usedPercent = (usedBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val usedGB = usedBytes / (1024 * 1024 * 1024f)
    val maxGB = maxBytes / (1024 * 1024 * 1024f)

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = usedPercent,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )

    // Glassmorphic card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface10)
            .drawBehind {
                // Subtle border glow
                drawRect(
                    color = CyanGlow.copy(alpha = 0.05f),
                    size = size
                )
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                if (!isPremium) {
                    TextButton(
                        onClick = onUpgradeClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = AmberAlert)
                    ) {
                        Text("Upgrade", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated progress bar with glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Surface20)
            ) {
                // Progress fill with gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (usedPercent > 0.9f) {
                                    listOf(AmberAlert, CrimsonSecurity)
                                } else {
                                    listOf(CyanGlow, GlowCyanEnd)
                                }
                            )
                        )
                )

                // Glow effect on leading edge
                if (animatedProgress > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .offset(x = (animatedProgress * 1000).dp * 0.001f)
                            .background(
                                color = if (usedPercent > 0.9f) AmberAlert else CyanGlow,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%.1f", usedGB)} GB of ${maxGB.toInt()} GB used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "${String.format("%.0f", usedPercent * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (usedPercent > 0.9f) CrimsonSecurity else CyanGlow,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuickFilterChips(
    selectedFilter: FilterType?,
    onFilterSelect: (FilterType?) -> Unit,
    itemCounts: Map<FilterType, Int>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.entries.forEach { filter ->
            val isSelected = selectedFilter == filter || (filter == FilterType.ALL && selectedFilter == null)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = AnimationSpecs.ButtonPress,
                label = "chip_scale"
            )

            Surface(
                onClick = { onFilterSelect(if (filter == FilterType.ALL) null else filter) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) CyanGlow.copy(alpha = 0.15f) else Surface10,
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        filter.icon,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) CyanGlow else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${filter.label} (${itemCounts[filter] ?: 0})",
                        color = if (isSelected) CyanGlow else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentItemsRow(
    items: List<HiddenItem>,
    onItemClick: (HiddenItem) -> Unit,
    onItemLongPress: (HiddenItem) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(items, key = { it.id }) { item ->
            RecentItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onLongPress = { onItemLongPress(item) }
            )
        }
    }
}

@Composable
private fun RecentItemCard(
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = AnimationSpecs.ButtonPress,
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .width(100.dp)
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface15)
            .drawBehind {
                // Border glow when pressed
                if (isPressed) {
                    drawRect(
                        color = CyanGlow.copy(alpha = 0.2f),
                        size = size
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
                modifier = Modifier.size(32.dp),
                tint = CyanGlow
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.originalName.take(10) + if (item.originalName.length > 10) "..." else "",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyVaultStateWithActions(
    onImportPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onAddFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated glow behind icon
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
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vault is Empty",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your secure space awaits",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Quick action buttons with glassmorphism
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionButton(
                icon = Icons.Default.PhotoLibrary,
                label = "Import Photos",
                description = "Import from gallery",
                onClick = onImportPhotos,
                modifier = Modifier.fillMaxWidth()
            )
            QuickActionButton(
                icon = Icons.Default.PhotoCamera,
                label = "Take Photo",
                description = "Capture directly to vault",
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth()
            )
            QuickActionButton(
                icon = Icons.Default.Description,
                label = "Add Files",
                description = "Documents, audio, etc.",
                onClick = onAddFiles,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.ButtonPress,
        label = "action_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface10)
            .drawBehind {
                // Subtle glow border when pressed
                if (isPressed) {
                    drawRect(
                        color = CyanGlow.copy(alpha = 0.15f),
                        size = size
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface20),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyVaultState(
    onAddClick: () -> Unit,
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
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vault is Empty",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add photos, videos, or files",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanGlow,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add Items", fontWeight = FontWeight.Medium)
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
