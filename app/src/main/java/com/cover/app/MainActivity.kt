package com.cover.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.cover.app.core.theme.CoverTheme
import com.cover.app.data.remoteconfig.AppStatus
import com.cover.app.presentation.main.AppEntryPoint
import com.cover.app.presentation.main.MainViewModel
import com.cover.app.presentation.navigation.AppNavigation
import com.cover.app.presentation.promotion.ForcedPaywallDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntryPoint {
                        val navController = rememberNavController()
                        val mainViewModel: MainViewModel = hiltViewModel()
                        val showPaywall by mainViewModel.showPaywall.collectAsState()
                        val showLimitedOffer by mainViewModel.showLimitedOffer.collectAsState()
                        val appStatus by mainViewModel.appStatus.collectAsState()
                        
                        // Handle app kill switch and force update
                        when (val status = appStatus) {
                            is AppStatus.KILLED -> {
                                // Show kill dialog and close app
                                AlertDialog(
                                    onDismissRequest = { finish() },
                                    title = { Text("App Unavailable") },
                                    text = { Text(status.message) },
                                    confirmButton = {
                                        TextButton(onClick = { finish() }) {
                                            Text("Close")
                                        }
                                    }
                                )
                                return@AppEntryPoint
                            }
                            is AppStatus.FORCE_UPDATE -> {
                                // Show force update dialog
                                AlertDialog(
                                    onDismissRequest = { },
                                    title = { Text("Update Required") },
                                    text = { Text(status.message) },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            mainViewModel.openPlayStore()
                                            finish()
                                        }) {
                                            Text("Update Now")
                                        }
                                    },
                                    dismissButton = null // Cannot dismiss
                                )
                                return@AppEntryPoint
                            }
                            else -> {
                                // App is OK, show normal UI
                            }
                        }
                        
                        AppNavigation(
                            navController = navController
                        )
                        
                        // Forced paywall dialog
                        ForcedPaywallDialog(
                            isVisible = showPaywall,
                            title = "Unlock Premium Features",
                            message = "Get unlimited storage, no ads, and all premium features",
                            onUpgrade = {
                                mainViewModel.dismissPaywall()
                                navController.navigate("upgrade")
                            },
                            onClose = {
                                mainViewModel.dismissPaywall()
                            },
                            onMaybeLater = {
                                mainViewModel.dismissPaywall()
                            }
                        )
                    }
                }
            }
        }
    }
}
