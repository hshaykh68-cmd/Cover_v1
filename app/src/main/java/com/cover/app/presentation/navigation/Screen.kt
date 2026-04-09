package com.cover.app.presentation.navigation

sealed class Screen(val route: String) {
    object Calculator : Screen("calculator")
    object Onboarding : Screen("onboarding")
    
    // Authenticated screens with bottom nav
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object IntruderLogs : Screen("intruder_logs")
    object Settings : Screen("settings")
    
    // Legacy routes (for navigation from calculator)
    object Vault : Screen("vault/{vaultId}") {
        fun createRoute(vaultId: String) = "vault/$vaultId"
    }
    object LegacyGallery : Screen("gallery/{vaultId}") {
        fun createRoute(vaultId: String) = "gallery/$vaultId"
    }
    
    // Detail screens
    object Upgrade : Screen("upgrade")
    object MediaViewer : Screen("media_viewer/{itemId}") {
        fun createRoute(itemId: String) = "media_viewer/$itemId"
    }
}
