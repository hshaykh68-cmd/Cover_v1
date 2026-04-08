package com.cover.app.data.remoteconfig

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TutorialManager handles first-launch tutorial flow controlled from cloud.
 * 
 * Features:
 * - Multi-step tutorial (3 screens as per PRD)
 * - Cloud-enabled/disabled via Remote Config
 * - Progress tracking
 * - Can be re-triggered remotely
 */
@Singleton
class TutorialManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager,
    private val inAppMessageManager: InAppMessageManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE)
    
    private val _tutorialState = MutableStateFlow<TutorialState>(TutorialState.NotStarted)
    val tutorialState: StateFlow<TutorialState> = _tutorialState.asStateFlow()
    
    companion object {
        const val PREF_TUTORIAL_COMPLETED = "tutorial_completed"
        const val PREF_TUTORIAL_STEP = "tutorial_current_step"
        const val TOTAL_STEPS = 3
    }
    
    init {
        // Check if tutorial was previously completed
        if (prefs.getBoolean(PREF_TUTORIAL_COMPLETED, false)) {
            _tutorialState.value = TutorialState.Completed
        } else {
            _tutorialState.value = TutorialState.NotStarted
        }
    }
    
    /**
     * Check if tutorial should be shown based on Remote Config
     */
    fun shouldShowTutorial(): Boolean {
        val config = remoteConfigManager.getConfig()
        return config.showTutorialOnFirstLaunch && !isTutorialCompleted()
    }
    
    /**
     * Start the tutorial flow
     */
    fun startTutorial() {
        if (_tutorialState.value is TutorialState.Completed) {
            return
        }
        
        val currentStep = prefs.getInt(PREF_TUTORIAL_STEP, 1)
        _tutorialState.value = TutorialState.InProgress(currentStep, TOTAL_STEPS)
        
        showTutorialStep(currentStep)
    }
    
    /**
     * Show specific tutorial step via In-App Messaging
     */
    private fun showTutorialStep(step: Int) {
        val (title, description) = when (step) {
            1 -> "Welcome to Cover" to "Hide photos, videos, and apps behind a working calculator. Your privacy is our priority."
            2 -> "How It Works" to "Enter your PIN followed by +0= to unlock your vault. Use +1= for decoy mode."
            3 -> "Stay Protected" to "Shake your phone to instantly lock and return to calculator mode. Intruders will be caught!"
            else -> return
        }
        
        inAppMessageManager.triggerTutorialStep(step, TOTAL_STEPS, title, description)
    }
    
    /**
     * Advance to next tutorial step
     */
    fun nextStep() {
        val currentState = _tutorialState.value
        if (currentState is TutorialState.InProgress) {
            val nextStep = currentState.currentStep + 1
            
            if (nextStep > TOTAL_STEPS) {
                completeTutorial()
            } else {
                prefs.edit().putInt(PREF_TUTORIAL_STEP, nextStep).apply()
                _tutorialState.value = TutorialState.InProgress(nextStep, TOTAL_STEPS)
                showTutorialStep(nextStep)
            }
        }
    }
    
    /**
     * Skip tutorial
     */
    fun skipTutorial() {
        _tutorialState.value = TutorialState.Skipped
        prefs.edit().putBoolean(PREF_TUTORIAL_COMPLETED, true).apply()
    }
    
    /**
     * Mark tutorial as completed
     */
    fun completeTutorial() {
        _tutorialState.value = TutorialState.Completed
        prefs.edit()
            .putBoolean(PREF_TUTORIAL_COMPLETED, true)
            .putInt(PREF_TUTORIAL_STEP, TOTAL_STEPS)
            .apply()
    }
    
    /**
     * Reset tutorial (can be triggered remotely for re-engagement)
     */
    fun resetTutorial() {
        prefs.edit()
            .putBoolean(PREF_TUTORIAL_COMPLETED, false)
            .putInt(PREF_TUTORIAL_STEP, 1)
            .apply()
        _tutorialState.value = TutorialState.NotStarted
    }
    
    /**
     * Check if tutorial is completed
     */
    fun isTutorialCompleted(): Boolean {
        return prefs.getBoolean(PREF_TUTORIAL_COMPLETED, false)
    }
    
    /**
     * Get current tutorial step
     */
    fun getCurrentStep(): Int {
        return prefs.getInt(PREF_TUTORIAL_STEP, 1)
    }
    
    /**
     * Force show tutorial regardless of completion status (for cloud re-trigger)
     */
    fun forceShowTutorial() {
        resetTutorial()
        startTutorial()
    }
}

/**
 * Tutorial state
 */
sealed class TutorialState {
    object NotStarted : TutorialState()
    data class InProgress(val currentStep: Int, val totalSteps: Int) : TutorialState()
    object Completed : TutorialState()
    object Skipped : TutorialState()
}
