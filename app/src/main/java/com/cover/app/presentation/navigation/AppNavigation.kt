package com.cover.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.cover.app.domain.model.HiddenItem
import com.cover.app.presentation.calculator.CalculatorScreen
import com.cover.app.presentation.components.BottomNavBar
import com.cover.app.presentation.gallery.GalleryScreen
import com.cover.app.presentation.intruder.IntruderLogsScreen
import com.cover.app.presentation.onboarding.OnboardingScreen
import com.cover.app.presentation.onboarding.OnboardingViewModel
import com.cover.app.presentation.premium.PremiumScreen
import com.cover.app.presentation.settings.SettingsScreen
import com.cover.app.presentation.vault.VaultScreen
import com.cover.app.presentation.viewer.MediaViewerScreen

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
    
    // Track current route for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Show bottom nav only on authenticated screens
    val showBottomNav = currentRoute in listOf(
        Screen.Home.route,
        Screen.Gallery.route,
        Screen.IntruderLogs.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid building a large stack
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = actualStartDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Unauthenticated flow
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
                    onRealVaultAccess = {
                        // Navigate to home (authenticated main screen)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Calculator.route) { inclusive = false }
                        }
                    },
                    onDecoyVaultAccess = {
                        // Navigate to home (decoy mode)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Calculator.route) { inclusive = false }
                        }
                    }
                )
            }
            
            // Authenticated flow with bottom navigation
            composable(Screen.Home.route) {
                VaultScreen(
                    onNavigateBack = {
                        // Lock vault - go back to calculator
                        navController.navigate(Screen.Calculator.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToUpgrade = {
                        navController.navigate(Screen.Upgrade.route)
                    },
                    onOpenItem = { item, items ->
                        val index = items.indexOf(item)
                        navController.navigate(Screen.MediaViewer.createRoute(item.id, index))
                    }
                )
            }
            
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onNavigateBack = {
                        // Navigate to home when back pressed
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToUpgrade = {
                        navController.navigate(Screen.Upgrade.route)
                    }
                )
            }
            
            composable(Screen.IntruderLogs.route) {
                IntruderLogsScreen(
                    onNavigateBack = {
                        // Navigate to home when back pressed
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        // Navigate to home when back pressed
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            // Detail screens (no bottom nav)
            composable(Screen.Upgrade.route) {
                PremiumScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Media Viewer
            composable(
                route = Screen.MediaViewer.route,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType },
                    navArgument("initialIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
                
                MediaViewerScreen(
                    initialIndex = initialIndex,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDeleteItem = { },
                    onExportItem = { }
                )
            }
        }
    }
}
