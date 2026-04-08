package com.cover.app.presentation.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import coil.compose.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cover.app.domain.model.HiddenItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    items: List<HiddenItem>,
    initialIndex: Int,
    vaultId: String,
    pin: String,
    onNavigateBack: () -> Unit,
    onDeleteItem: (HiddenItem) -> Unit,
    onExportItem: (HiddenItem) -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf<HiddenItem?>(null) }
    
    // Load decrypted content for current page
    val currentItem = items.getOrNull(pagerState.currentPage)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // Media Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            MediaPage(
                item = item,
                vaultId = vaultId,
                pin = pin,
                viewModel = viewModel
            )
        }
        
        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = {
                        val currentItem = items.getOrNull(pagerState.currentPage)
                        Text(
                            text = currentItem?.originalName ?: "",
                            color = Color.White,
                            maxLines = 1
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
                        val currentItem = items.getOrNull(pagerState.currentPage)
                        currentItem?.let { item ->
                            IconButton(onClick = { onExportItem(item) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showDeleteConfirm = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF453A)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                
                // Page indicator
                if (items.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        // Delete confirmation dialog
        showDeleteConfirm?.let { item ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete \"${item.originalName}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteItem(item)
                            showDeleteConfirm = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
private fun MediaPage(
    item: HiddenItem,
    vaultId: String,
    pin: String,
    viewModel: MediaViewerViewModel
) {
    val context = LocalContext.current
    var decryptedBitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember(item.id) { mutableStateOf(true) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    
    // Load decrypted content
    LaunchedEffect(item.id) {
        isLoading = true
        error = null
        decryptedBitmap = viewModel.decryptItem(item, vaultId, pin)
        isLoading = false
        if (decryptedBitmap == null && item is HiddenItem.Photo) {
            error = "Failed to decrypt"
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            is HiddenItem.Photo -> {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    error != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error!!, color = Color.Gray)
                        }
                    }
                    decryptedBitmap != null -> {
                        androidx.compose.foundation.Image(
                            bitmap = decryptedBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            is HiddenItem.Video -> {
                // Video player placeholder - video needs different handling
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = item.originalName, color = Color.White)
                }
            }
        }
    }
}
