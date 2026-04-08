package com.cover.app.presentation.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * LauncherActivity that acts as a custom launcher/home screen.
 * 
 * This activity handles:
 * 1. Showing calculator UI by default (disguise mode)
 * 2. Detecting PIN entry to unlock hidden apps view
 * 3. Displaying all installed apps with hidden ones filtered based on vault state
 * 
 * When set as default launcher, this replaces the stock launcher entirely.
 * The app appears as "Calculator" to anyone checking settings.
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LauncherScreen(
                viewModel = viewModel,
                onOpenVault = { /* Navigate to main vault activity if needed */ }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    override fun onBackPressed() {
        // Do nothing - we are the launcher, going back should stay here
        // This prevents exiting the launcher with back button
    }
}
