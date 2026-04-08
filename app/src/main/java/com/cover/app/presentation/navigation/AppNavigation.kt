package com.cover.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cover.app.presentation.calculator.CalculatorScreen
import com.cover.app.presentation.gallery.GalleryScreen
import com.cover.app.presentation.intruder.IntruderLogsScreen
import com.cover.app.presentation.onboarding.OnboardingScreen
import com.cover.app.presentation.onboarding.OnboardingViewModel
import com.cover.app.presentation.premium.PremiumScreen
import com.cover.app.presentation.vault.VaultScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Calculator.route
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isPinSet = onboardingViewModel.isPinSet()
    
    // If PIN not set, force onboarding
    val actualStartDestination = if (isPinSet) startDestination else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = actualStartDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Calculator.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Calculator.route) {
            CalculatorScreen(
                onRealVaultAccess = { pin ->
                    // Navigate to real vault - PIN already verified in ViewModel
                    navController.navigate(Screen.Vault.createRoute("real_vault_id"))
                },
                onDecoyVaultAccess = { pin ->
                    // Navigate to decoy vault
                    navController.navigate(Screen.Vault.createRoute("decoy_vault_id"))
                }
            )
        }
        
        composable(Screen.Vault.route) { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId") ?: ""
            VaultScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Upgrade.route)
                }
            )
        }
        
        composable(Screen.Gallery.route) { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId") ?: ""
            // Note: PIN should be passed securely, this is simplified
            val pin = "1234" // Placeholder - should come from secure storage
            GalleryScreen(
                vaultId = vaultId,
                pin = pin,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Upgrade.route)
                }
            )
        }
        
        composable(Screen.IntruderLogs.route) {
            IntruderLogsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            // SettingsScreen will be implemented in later phase
        }
        
        composable(Screen.Upgrade.route) {
            PremiumScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
