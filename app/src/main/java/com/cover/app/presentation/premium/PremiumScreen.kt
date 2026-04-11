package com.cover.app.presentation.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cover.app.core.animation.AnimationSpecs
import com.cover.app.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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
                            VoidBlue,
                            VoidBlack,
                            VoidPurple
                        ),
                        startY = 0f,
                        endY = 1500f
                    )
                )
                .drawBehind {
                    // Cinematic glow effect
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.05f),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.5f, size.height * 0.1f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                // Cinematic animated header
                PremiumHeader()

                Spacer(modifier = Modifier.height(32.dp))

            // Loading or error state
            when (state) {
                is PremiumUiState.Loading -> {
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing glow behind spinner
                        val infiniteTransition = rememberInfiniteTransition(label = "loading_glow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = CyanGlow.copy(alpha = glowAlpha),
                                        radius = size.width / 2
                                    )
                                }
                        )
                        CircularProgressIndicator(
                            color = CyanGlow,
                            strokeWidth = 3.dp
                        )
                    }
                }
                is PremiumUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonSecurity.copy(alpha = 0.15f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = (state as PremiumUiState.Error).message,
                            color = CrimsonSecurity,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    val loadedState = state as? PremiumUiState.Loaded
                    
                    // Pricing cards
                    PricingCard(
                        title = "Lifetime",
                        price = loadedState?.lifetimePrice ?: "$24.99",
                        subtitle = "One-time payment",
                        features = listOf(
                            "Unlimited storage",
                            "All premium features",
                            "No ads forever",
                            "Priority support"
                        ),
                        isPopular = true,
                        isBestValue = true,
                        onClick = { viewModel.purchaseLifetime() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PricingCard(
                        title = "Yearly",
                        price = loadedState?.yearlyPrice ?: "$9.99",
                        subtitle = "Save 58%",
                        originalPrice = loadedState?.monthlyPrice?.let { "$${(1.99 * 12).toInt()}" } ?: "$23.88",
                        features = listOf(
                            "Unlimited storage",
                            "All premium features",
                            "No ads"
                        ),
                        isPopular = false,
                        isBestValue = false,
                        onClick = { viewModel.purchaseYearly() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PricingCard(
                        title = "Monthly",
                        price = loadedState?.monthlyPrice ?: "$1.99",
                        subtitle = "Flexible",
                        features = listOf(
                            "Unlimited storage",
                            "All premium features",
                            "Cancel anytime"
                        ),
                        isPopular = false,
                        isBestValue = false,
                        onClick = { viewModel.purchaseMonthly() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features list
            Text(
                text = "Premium Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureItem(
                icon = Icons.Default.Cloud,
                title = "Unlimited Storage",
                description = "Hide unlimited photos, videos & files"
            )
            
            FeatureItem(
                icon = Icons.Default.DoNotDisturb,
                title = "Ad-Free Experience",
                description = "No interruptions while using the app"
            )
            
            FeatureItem(
                icon = Icons.Default.VisibilityOff,
                title = "Advanced App Hiding",
                description = "Hide apps completely from launcher"
            )
            
            FeatureItem(
                icon = Icons.Default.CameraAlt,
                title = "Intruder Alerts",
                description = "Unlimited intruder photo capture"
            )
            
            FeatureItem(
                icon = Icons.Default.Help,
                title = "Priority Support",
                description = "Get help within 24 hours"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Terms
            Text(
                text = "Subscriptions auto-renew unless cancelled. Manage in Google Play Store.",
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Purchase success dialog
    if (state is PremiumUiState.PurchaseSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccess() },
            title = { Text("Welcome to Premium!", color = TextPrimary) },
            text = { Text("Thank you for upgrading. You now have access to all premium features.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSuccess()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = VoidBlack
                    )
                ) {
                    Text("Continue", fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Surface15,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
}
}

@Composable
private fun PremiumHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header_anim")

    // Pulsing glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Scale animation for star
    val starScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Animated star icon with glow
    Box(
        modifier = Modifier
            .size(100.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow rings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = CyanGlow.copy(alpha = glowAlpha * 0.5f),
                        radius = size.width * 0.5f
                    )
                    drawCircle(
                        color = CyanGlow.copy(alpha = glowAlpha * 0.3f),
                        radius = size.width * 0.4f
                    )
                }
        )

        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = starScale
                    scaleY = starScale
                },
            tint = CyanGlow
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Go Premium",
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        letterSpacing = (-0.5).sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Unlock unlimited storage and exclusive features",
        fontSize = 16.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    subtitle: String,
    originalPrice: String? = null,
    features: List<String>,
    isPopular: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.ButtonPress,
        label = "card_scale"
    )

    // Shimmer animation for best value badge
    val shimmerProgress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isPopular) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Surface15,
                            Surface10,
                            CyanGlow.copy(alpha = 0.05f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Surface15, Surface10)
                    )
                }
            )
            .drawBehind {
                // Border glow for popular card
                if (isPopular) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CyanGlow.copy(alpha = 0.2f),
                                CyanGlow.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        size = size
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        // Badges
        if (isPopular || isBestValue) {
            Row {
                if (isBestValue) {
                    // Shimmering gold badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AmberAlert.copy(alpha = 0.8f + 0.2f * shimmerProgress),
                                        AmberAlert.copy(alpha = 0.6f),
                                        AmberAlert.copy(alpha = 0.8f + 0.2f * (1 - shimmerProgress))
                                    )
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "BEST VALUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoidBlack
                        )
                    }
                }
                if (isPopular && isBestValue) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (isPopular) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanGlow)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "POPULAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoidBlack
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = if (isPopular) CyanGlow else TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (originalPrice != null) {
                    Text(
                        text = originalPrice,
                        fontSize = 14.sp,
                        color = TextTertiary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Features with glassmorphic icons
        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface20),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = feature,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Select button with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isPopular) {
                        Brush.horizontalGradient(
                            colors = listOf(CyanGlow, GlowCyanEnd)
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Surface20, Surface15)
                        )
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPopular) VoidBlack else TextPrimary
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glassmorphic icon container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface15)
                .drawBehind {
                    drawRect(
                        color = CyanGlow.copy(alpha = 0.1f),
                        size = size
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = CyanGlow
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

private val LinearEasing = androidx.compose.animation.core.LinearEasing
