package com.cover.app.presentation.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.remoteconfig.AppStatus
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.remoteconfig.TutorialManager
import com.cover.app.data.remoteconfig.InAppMessageManager
import com.cover.app.data.remoteconfig.InAppMessageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val promotionManager: PromotionManager,
    private val tutorialManager: TutorialManager,
    private val inAppMessageManager: InAppMessageManager
) : ViewModel() {

    private val _appStatus = MutableStateFlow<AppStatus>(AppStatus.OK)
    val appStatus: StateFlow<AppStatus> = _appStatus.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _showLimitedOffer = MutableStateFlow(false)
    val showLimitedOffer: StateFlow<Boolean> = _showLimitedOffer.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _inAppMessage = MutableStateFlow<InAppMessageState>(InAppMessageState.Idle)
    val inAppMessage: StateFlow<InAppMessageState> = _inAppMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Check app status on launch
            checkAppStatus()
            
            // Initialize promotions
            promotionManager.onAppLaunch()
            
            // Check if tutorial should be shown
            if (tutorialManager.shouldShowTutorial()) {
                _showTutorial.value = true
                tutorialManager.startTutorial()
            }
            
            // Monitor promotion state
            promotionManager.promotionState.collect { state ->
                if (state.showPaywall && !state.isPremium) {
                    _showPaywall.value = true
                }
                if (state.showLimitedTimeOffer && !state.isPremium) {
                    _showLimitedOffer.value = true
                }
            }
        }
        
        // Monitor in-app messages
        viewModelScope.launch {
            inAppMessageManager.messageState.collect { state ->
                _inAppMessage.value = state
            }
        }
        
        // Monitor tutorial state
        viewModelScope.launch {
            tutorialManager.tutorialState.collect { state ->
                when (state) {
                    is com.cover.app.data.remoteconfig.TutorialState.Completed,
                    is com.cover.app.data.remoteconfig.TutorialState.Skipped -> {
                        _showTutorial.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkAppStatus(currentVersionCode: Int = 1) {
        _appStatus.value = promotionManager.checkAppStatus(currentVersionCode)
    }

    fun openPlayStore() {
        // Intent to open Play Store would go here
    }

    fun dismissPaywall() {
        _showPaywall.value = false
        promotionManager.markPaywallShown()
    }

    fun dismissLimitedOffer() {
        _showLimitedOffer.value = false
    }

    fun startLimitedOffer() {
        promotionManager.startLimitedTimeOffer()
    }
    
    // Tutorial actions
    fun onTutorialNext() {
        tutorialManager.nextStep()
    }
    
    fun onTutorialSkip() {
        tutorialManager.skipTutorial()
        _showTutorial.value = false
    }
    
    fun onTutorialComplete() {
        tutorialManager.completeTutorial()
        _showTutorial.value = false
    }
    
    // In-app message actions
    fun onInAppMessageDismiss() {
        inAppMessageManager.dismissMessage()
    }
    
    fun onInAppMessageAction() {
        inAppMessageManager.takeMessageAction()
    }
}
