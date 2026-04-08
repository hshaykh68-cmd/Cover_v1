package com.cover.app.data.remoteconfig

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ABTestManager provides A/B testing framework with:
 * - Cohort assignment via Firebase Remote Config
 * - Conversion tracking via Firebase Analytics
 * - Experiment variant management
 * - Automatic winner selection metrics
 * 
 * This integrates with Firebase Remote Config for cohort assignment
 * and Firebase Analytics for event tracking.
 */
@Singleton
class ABTestManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    
    private val _cohortState = MutableStateFlow(ABTestCohort.CONTROL)
    val cohortState: StateFlow<ABTestCohort> = _cohortState.asStateFlow()
    
    private val _activeExperiments = MutableStateFlow<Map<String, ExperimentVariant>>(emptyMap())
    val activeExperiments: StateFlow<Map<String, ExperimentVariant>> = _activeExperiments.asStateFlow()
    
    init {
        updateCohortFromConfig()
    }
    
    /**
     * Update cohort assignment from Remote Config
     */
    fun updateCohortFromConfig() {
        val config = remoteConfigManager.getConfig()
        val cohort = ABTestCohort.fromString(config.abTestCohort)
        val variant = ExperimentVariant.fromString(config.experimentVariant)
        
        _cohortState.value = cohort
        
        // Set user properties for Firebase Analytics
        firebaseAnalytics.setUserProperty("ab_test_cohort", cohort.name.lowercase())
        firebaseAnalytics.setUserProperty("experiment_variant", variant.name.lowercase())
        
        // Track cohort assignment
        logEvent("cohort_assigned", Bundle().apply {
            putString("cohort", cohort.name)
            putString("variant", variant.name)
        })
    }
    
    /**
     * Get the user's assigned cohort
     */
    fun getCohort(): ABTestCohort {
        return _cohortState.value
    }
    
    /**
     * Check if user is in a specific cohort
     */
    fun isInCohort(cohort: ABTestCohort): Boolean {
        return _cohortState.value == cohort
    }
    
    /**
     * Register an active experiment
     */
    fun registerExperiment(experimentName: String, variant: ExperimentVariant) {
        _activeExperiments.value = _activeExperiments.value + (experimentName to variant)
        
        // Log experiment start
        logEvent("experiment_started", Bundle().apply {
            putString("experiment_name", experimentName)
            putString("variant", variant.name)
        })
    }
    
    /**
     * Track conversion event for A/B testing
     */
    fun trackConversion(
        eventType: ConversionEvent,
        experimentName: String? = null,
        additionalParams: Map<String, String> = emptyMap()
    ) {
        val bundle = Bundle().apply {
            putString("conversion_type", eventType.name)
            putString("cohort", _cohortState.value.name)
            experimentName?.let { putString("experiment", it) }
            additionalParams.forEach { (key, value) ->
                putString(key, value)
            }
        }
        
        logEvent("ab_conversion", bundle)
    }
    
    /**
     * Track paywall impression by variant
     */
    fun trackPaywallImpression(variant: String) {
        logEvent("paywall_impression", Bundle().apply {
            putString("variant", variant)
            putString("cohort", _cohortState.value.name)
        })
    }
    
    /**
     * Track purchase by cohort and variant
     */
    fun trackPurchase(
        productId: String,
        price: Double,
        experimentName: String? = null
    ) {
        logEvent("purchase_completed", Bundle().apply {
            putString("product_id", productId)
            putDouble("price", price)
            putString("cohort", _cohortState.value.name)
            experimentName?.let { putString("experiment", it) }
        })
        
        // Also track as conversion
        trackConversion(ConversionEvent.PURCHASE_COMPLETED, experimentName)
    }
    
    /**
     * Track feature usage for measuring engagement
     */
    fun trackFeatureUsage(feature: FeatureUsage) {
        logEvent("feature_used", Bundle().apply {
            putString("feature", feature.name)
            putString("cohort", _cohortState.value.name)
        })
    }
    
    /**
     * Get experiment variant for a specific test
     */
    fun getExperimentVariant(experimentName: String): ExperimentVariant {
        return _activeExperiments.value[experimentName] ?: ExperimentVariant.DEFAULT
    }
    
    /**
     * Check if a specific experiment is active
     */
    fun isExperimentActive(experimentName: String): Boolean {
        return _activeExperiments.value.containsKey(experimentName)
    }
    
    /**
     * Get effective paywall variant (supports A/B testing)
     */
    fun getPaywallVariant(): String {
        return remoteConfigManager.getConfig().paywallVariant
    }
    
    /**
     * Get effective pricing (supports A/B price testing)
     */
    fun getEffectivePricing(): PricingConfig {
        return remoteConfigManager.getEffectivePrices()
    }
    
    /**
     * Log event to Firebase Analytics
     */
    private fun logEvent(eventName: String, params: Bundle) {
        firebaseAnalytics.logEvent(eventName, params)
    }
}

/**
 * A/B test cohorts
 */
enum class ABTestCohort {
    CONTROL,    // Control group - baseline
    VARIANT_A,  // Test variant A
    VARIANT_B,  // Test variant B
    VARIANT_C;  // Test variant C
    
    companion object {
        fun fromString(value: String): ABTestCohort {
            return when (value.lowercase()) {
                "variant_a", "a" -> VARIANT_A
                "variant_b", "b" -> VARIANT_B
                "variant_c", "c" -> VARIANT_C
                else -> CONTROL
            }
        }
    }
}

/**
 * Experiment variants
 */
enum class ExperimentVariant {
    DEFAULT,
    VARIANT_A,
    VARIANT_B,
    WINNER;
    
    companion object {
        fun fromString(value: String): ExperimentVariant {
            return when (value.lowercase()) {
                "variant_a", "a" -> VARIANT_A
                "variant_b", "b" -> VARIANT_B
                "winner" -> WINNER
                else -> DEFAULT
            }
        }
    }
}

/**
 * Conversion events for tracking
 */
enum class ConversionEvent {
    PAYWALL_VIEW,
    PAYWALL_DISMISS,
    PURCHASE_STARTED,
    PURCHASE_COMPLETED,
    TRIAL_STARTED,
    AD_WATCHED,
    PREMIUM_FEATURE_USED,
    IMPORT_LIMIT_HIT,
    STORAGE_LIMIT_HIT
}

/**
 * Feature usage tracking
 */
enum class FeatureUsage {
    VAULT_ACCESS,
    DECOY_VAULT_ACCESS,
    INTRUDER_SELFIE,
    SHAKE_EXIT,
    APP_HIDE,
    FILE_IMPORT,
    FILE_EXPORT,
    CALCULATOR_USED
}
