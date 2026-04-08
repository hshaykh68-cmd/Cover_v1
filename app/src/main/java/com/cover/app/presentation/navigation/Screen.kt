package com.cover.app.presentation.navigation

sealed class Screen(val route: String) {
    object Calculator : Screen("calculator")
    object Vault : Screen("vault/{vaultId}") {
        fun createRoute(vaultId: String) = "vault/$vaultId"
    }
    object Gallery : Screen("gallery/{vaultId}") {
        fun createRoute(vaultId: String) = "gallery/$vaultId"
    }
    object Settings : Screen("settings")
    object Upgrade : Screen("upgrade")
    object IntruderLogs : Screen("intruder_logs")
    object Onboarding : Screen("onboarding")
}
