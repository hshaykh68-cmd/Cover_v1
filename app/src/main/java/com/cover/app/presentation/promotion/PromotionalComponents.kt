package com.cover.app.presentation.promotion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Forced paywall dialog that blocks user until they interact
 */
@Composable
fun ForcedPaywallDialog(
    isVisible: Boolean,
    title: String = "Upgrade to Premium",
    message: String = "Unlock unlimited storage and all premium features",
    discountPercent: Int? = null,
    timeRemaining: String? = null,
    onUpgrade: () -> Unit,
    onClose: () -> Unit,
    onMaybeLater: (() -> Unit)? = null
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = { /* Force interaction - don't dismiss on outside tap */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFFD700)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Limited time badge
                if (discountPercent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFF453A),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "SAVE $discountPercent%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                
                if (timeRemaining != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏰ $timeRemaining remaining",
                        color = Color(0xFFFF453A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Feature list
                PaywallFeatureItem("✓ Unlimited photo & video storage")
                PaywallFeatureItem("✓ No ads - ever")
                PaywallFeatureItem("✓ Advanced intruder detection")
                PaywallFeatureItem("✓ Priority support")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // CTA Button
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF30D158)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Upgrade Now",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Close button (small, less prominent)
                if (onMaybeLater != null) {
                    TextButton(
                        onClick = onMaybeLater,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe Later",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Hidden close button (double tap to close)
                var closeTapCount by remember { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .clickable {
                            closeTapCount++
                            if (closeTapCount >= 3) {
                                onClose()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "•",
                        color = Color.Gray.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PaywallFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

/**
 * Upsell bottom sheet for specific actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionUpsellSheet(
    isVisible: Boolean,
    action: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFF9F0A)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Premium Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "You've reached the free limit for $action. Upgrade to Premium for unlimited access.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onUpgrade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF30D158)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Upgrade to Premium",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Not Now",
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Countdown timer for limited time offers
 */
@Composable
fun OfferCountdownTimer(
    endTime: Long,
    onExpire: () -> Unit
) {
    var timeLeft by remember { mutableLongStateOf(endTime - System.currentTimeMillis()) }
    
    LaunchedEffect(endTime) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft = endTime - System.currentTimeMillis()
        }
        onExpire()
    }
    
    val hours = (timeLeft / (1000 * 60 * 60)) % 24
    val minutes = (timeLeft / (1000 * 60)) % 60
    val seconds = (timeLeft / 1000) % 60
    
    Surface(
        color = Color(0xFFFF453A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

/**
 * Floating promo badge
 */
@Composable
fun FloatingPromoBadge(
    message: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            color = Color(0xFF30D158),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { isVisible = false },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Watch ad to continue dialog
 */
@Composable
fun WatchAdDialog(
    isVisible: Boolean,
    action: String,
    onWatchAd: () -> Unit,
    onUpgrade: () -> Unit,
    onCancel: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Watch Ad to Continue") },
        text = {
            Column {
                Text(
                    text = "You've used your free $action for today.",
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Watch a short ad to continue or upgrade to Premium for unlimited access.",
                    color = Color.White
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF30D158)
                )
            ) {
                Text("Watch Ad", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onUpgrade) {
                Text("Upgrade", color = Color(0xFF30D158))
            }
        },
        containerColor = Color(0xFF1C1C1E)
    )
}
