package com.cover.app.presentation.navigation

sealed class Screen(val route: String) {
    object Calculator : Screen("calculator")
    object Onboarding : Screen("onboarding")
    
    // Authenticated screens with bottom nav
    object Home : Screen("home")
    object Gallery : Screen("gallery")
    object IntruderLogs : Screen("intruder_logs")
    object Settings : Screen("settings")
    
    // Detail screens
    object Upgrade : Screen("upgrade")
    object MediaViewer : Screen("media_viewer/{itemId}?initialIndex={initialIndex}") {
        fun createRoute(itemId: String, initialIndex: Int = 0) = "media_viewer/$itemId?initialIndex=$initialIndex"
    }
}
