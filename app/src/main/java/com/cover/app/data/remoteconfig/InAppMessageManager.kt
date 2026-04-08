package com.cover.app.data.remoteconfig

import android.app.Activity
import android.content.Context
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * InAppMessageManager handles Firebase In-App Messaging for:
 * - New feature announcements
 * - Limited-time offers
 * - Win-back campaigns for churned users
 * - Tutorial messages
 * - Promotional content
 * 
 * This integrates with Firebase In-App Messaging SDK to display
 * contextual messages based on user behavior and triggers.
 */
@Singleton
class InAppMessageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {
    private val inAppMessaging: FirebaseInAppMessaging = FirebaseInAppMessaging.getInstance()
    
    private val _messageState = MutableStateFlow<InAppMessageState>(InAppMessageState.Idle)
    val messageState: StateFlow<InAppMessageState> = _messageState.asStateFlow()
    
    init {
        setupInAppMessaging()
    }
    
    /**
     * Setup Firebase In-App Messaging
     */
    private fun setupInAppMessaging() {
        // Disable automatic data collection if needed
        // inAppMessaging.setMessagesSuppressed(false)
        
        // Listen for message display
        inAppMessaging.addClickListener { inAppMessage, action ->
            handleMessageClick(inAppMessage, action)
        }
        
        inAppMessaging.addDismissListener { inAppMessage ->
            handleMessageDismiss(inAppMessage)
        }
        
        inAppMessaging.addImpressionListener { inAppMessage ->
            handleMessageImpression(inAppMessage)
        }
    }
    
    /**
     * Trigger a new feature announcement
     */
    fun triggerFeatureAnnouncement(
        featureName: String,
        description: String,
        actionText: String = "Try it now"
    ) {
        _messageState.value = InAppMessageState.Showing(
            type = MessageType.FEATURE_ANNOUNCEMENT,
            title = "New Feature: $featureName",
            body = description,
            actionText = actionText,
            campaignId = "feature_$featureName"
        )
    }
    
    /**
     * Trigger a limited-time offer message
     */
    fun triggerLimitedTimeOffer(
        discountPercent: Int,
        hoursRemaining: Int,
        productName: String = "Premium"
    ) {
        _messageState.value = InAppMessageState.Showing(
            type = MessageType.LIMITED_TIME_OFFER,
            title = "⏰ Limited Time Offer!",
            body = "Get $discountPercent% off $productName! Offer expires in $hoursRemaining hours.",
            actionText = "Claim Offer",
            campaignId = "limited_time_${System.currentTimeMillis()}"
        )
    }
    
    /**
     * Trigger win-back campaign for churned users
     */
    fun triggerWinBackCampaign(
        daysSinceLastOpen: Int,
        specialOffer: String = "50% off your first month"
    ) {
        _messageState.value = InAppMessageState.Showing(
            type = MessageType.WIN_BACK,
            title = "We miss you! 👋",
            body = "It's been $daysSinceLastOpen days since you last used Cover. Come back with $specialOffer!",
            actionText = "Get $specialOffer",
            campaignId = "winback_$daysSinceLastOpen"
        )
    }
    
    /**
     * Trigger tutorial message for first-time users
     */
    fun triggerTutorialStep(
        stepNumber: Int,
        totalSteps: Int,
        title: String,
        description: String
    ) {
        _messageState.value = InAppMessageState.Showing(
            type = MessageType.TUTORIAL,
            title = "$title ($stepNumber/$totalSteps)",
            body = description,
            actionText = if (stepNumber < totalSteps) "Next" else "Get Started",
            campaignId = "tutorial_step_$stepNumber"
        )
    }
    
    /**
     * Trigger promotional upsell message
     */
    fun triggerPromotionalUpsell(
        trigger: UpsellTrigger,
        customMessage: String? = null
    ) {
        val (title, body) = when (trigger) {
            UpsellTrigger.IMPORT_LIMIT -> "Storage Full" to "You've reached your import limit. Upgrade to hide unlimited items."
            UpsellTrigger.STORAGE_LIMIT -> "Storage Full" to "You're running out of space. Upgrade for more storage."
            UpsellTrigger.DELETE_LIMIT -> "Delete Limit Reached" to "You've reached your delete limit. Upgrade to delete unlimited items."
            UpsellTrigger.EXPORT_LIMIT -> "Export Limit Reached" to "You've reached your export limit. Upgrade to export unlimited items."
            UpsellTrigger.DECOY_ACCESS -> "Want More Security?" to "Unlock hidden vaults and advanced security features."
            UpsellTrigger.FAILED_CAPTURE -> "Stay Protected" to "Upgrade for unlimited intruder detection."
            UpsellTrigger.FREQUENCY_BASED -> "Special Offer" to "You've been using Cover for a while. Here's a special discount!"
            UpsellTrigger.CUSTOM -> "Upgrade to Premium" to (customMessage ?: "Unlock all features!")
        }
        
        _messageState.value = InAppMessageState.Showing(
            type = MessageType.UPSELL,
            title = title,
            body = body,
            actionText = "Upgrade Now",
            campaignId = "upsell_${trigger.name.lowercase()}"
        )
    }
    
    /**
     * Mark message as dismissed
     */
    fun dismissMessage() {
        val currentState = _messageState.value
        if (currentState is InAppMessageState.Showing) {
            _messageState.value = InAppMessageState.Dismissed(currentState.campaignId)
        }
    }
    
    /**
     * Mark message action as taken
     */
    fun takeMessageAction() {
        val currentState = _messageState.value
        if (currentState is InAppMessageState.Showing) {
            _messageState.value = InAppMessageState.ActionTaken(
                campaignId = currentState.campaignId,
                actionText = currentState.actionText
            )
        }
    }
    
    /**
     * Suppress all in-app messages (e.g., during critical flows)
     */
    fun suppressMessages(suppress: Boolean) {
        inAppMessaging.setMessagesSuppressed(suppress)
    }
    
    /**
     * Trigger data refresh from Firebase
     */
    fun triggerMessageRefresh() {
        inAppMessaging.triggerEvent("message_refresh")
    }
    
    /**
     * Log custom event to trigger in-app messages
     */
    fun logTriggerEvent(eventName: String) {
        inAppMessaging.triggerEvent(eventName)
    }
    
    /**
     * Handle message click
     */
    private fun handleMessageClick(inAppMessage: InAppMessage, action: Action) {
        val campaignId = inAppMessage.campaignMetadata?.campaignId ?: "unknown"
        _messageState.value = InAppMessageState.ActionTaken(
            campaignId = campaignId,
            actionText = action.button?.text?.toString() ?: "action"
        )
    }
    
    /**
     * Handle message dismiss
     */
    private fun handleMessageDismiss(inAppMessage: InAppMessage) {
        val campaignId = inAppMessage.campaignMetadata?.campaignId ?: "unknown"
        _messageState.value = InAppMessageState.Dismissed(campaignId)
    }
    
    /**
     * Handle message impression
     */
    private fun handleMessageImpression(inAppMessage: InAppMessage) {
        val campaignId = inAppMessage.campaignMetadata?.campaignId ?: "unknown"
        // Could track impression analytics here
    }
}

/**
 * In-app message state
 */
sealed class InAppMessageState {
    object Idle : InAppMessageState()
    data class Showing(
        val type: MessageType,
        val title: String,
        val body: String,
        val actionText: String,
        val campaignId: String
    ) : InAppMessageState()
    data class Dismissed(val campaignId: String) : InAppMessageState()
    data class ActionTaken(val campaignId: String, val actionText: String) : InAppMessageState()
}

/**
 * Message types
 */
enum class MessageType {
    FEATURE_ANNOUNCEMENT,
    LIMITED_TIME_OFFER,
    WIN_BACK,
    TUTORIAL,
    UPSELL
}

/**
 * Upsell trigger types
 */
enum class UpsellTrigger {
    IMPORT_LIMIT,
    STORAGE_LIMIT,
    DELETE_LIMIT,
    EXPORT_LIMIT,
    DECOY_ACCESS,
    FAILED_CAPTURE,
    FREQUENCY_BASED,
    CUSTOM
}
