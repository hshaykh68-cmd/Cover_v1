package com.cover.app.presentation.premium

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                title = { Text("Upgrade", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Spacer(modifier = Modifier.height(24.dp))
            
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                tint = Color(0xFFFFD700)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Go Premium",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unlock unlimited storage and exclusive features",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Loading or error state
            when (state) {
                is PremiumUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color(0xFF30D158)
                    )
                }
                is PremiumUiState.Error -> {
                    Text(
                        text = (state as PremiumUiState.Error).message,
                        color = Color(0xFFFF453A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                color = Color.Gray,
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
            title = { Text("Welcome to Premium!") },
            text = { Text("Thank you for upgrading. You now have access to all premium features.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.dismissSuccess()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF30D158)
                    )
                ) {
                    Text("Continue", color = Color.Black)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }
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
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) Color(0xFF1C3A1C) else Color(0xFF1C1C1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Badges
            if (isPopular || isBestValue) {
                Row {
                    if (isBestValue) {
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BEST VALUE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (isPopular && isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (isPopular) {
                        Surface(
                            color = Color(0xFF30D158),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "POPULAR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF30D158)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = price,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (originalPrice != null) {
                        Text(
                            text = originalPrice,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Features
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) Color(0xFF30D158) else Color(0xFF2C2C2E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Select",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPopular) Color.Black else Color.White
                )
            }
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
        Surface(
            color = Color(0xFF2C2C2E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = Color(0xFF64D2FF)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
