package com.cover.app.data.remoteconfig

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.cover.app.data.admob.AdMobManager
import com.cover.app.data.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager,
    private val premiumRepository: PremiumRepository,
    private val adMobManager: AdMobManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("promotions", Context.MODE_PRIVATE)
    
    private val _promotionState = MutableStateFlow(PromotionState())
    val promotionState: StateFlow<PromotionState> = _promotionState.asStateFlow()
    
    companion object {
        const val PREF_SESSION_COUNT = "session_count"
        const val PREF_LAST_PROMO_TIME = "last_promo_time"
        const val PREF_OFFER_START_TIME = "offer_start_time"
        const val PREF_IMPORT_COUNT = "import_count"
        const val PREF_AD_WATCH_COUNT = "ad_watch_count"
        const val PREF_LAST_AD_WATCH = "last_ad_watch"
    }
    
    init {
        // Load initial state
        loadState()
        
        // Monitor remote config changes
        CoroutineScope(Dispatchers.Default).launch {
            remoteConfigManager.config.collect { config ->
                checkAndUpdatePromotions()
            }
        }
        
        // Monitor premium status
        CoroutineScope(Dispatchers.Default).launch {
            premiumRepository.isPremium().collect { isPremium ->
                _promotionState.update { it.copy(isPremium = isPremium) }
            }
        }
    }
    
    private fun loadState() {
        _promotionState.value = PromotionState(
            sessionCount = prefs.getInt(PREF_SESSION_COUNT, 0),
            importCount = prefs.getInt(PREF_IMPORT_COUNT, 0),
            offerStartTime = prefs.getLong(PREF_OFFER_START_TIME, 0).takeIf { it > 0 },
            adWatchCount = prefs.getInt(PREF_AD_WATCH_COUNT, 0)
        )
    }
    
    private fun checkAndUpdatePromotions() {
        val config = remoteConfigManager.getConfig()
        val state = _promotionState.value
        
        // Check limited time offer
        val isOfferActive = remoteConfigManager.isLimitedTimeOfferActive(state.offerStartTime)
        
        // Check if should show paywall
        val shouldShowPaywall = remoteConfigManager.shouldShowPaywall(state.sessionCount)
        
        // Check import limit
        val importLimitHit = remoteConfigManager.hasHitImportLimit(state.importCount, state.isPremium)
        
        // Check storage limit
        val storageLimitHit = remoteConfigManager.hasExceededStorageLimit(
            prefs.getInt("storage_used_mb", 0), 
            state.isPremium
        )
        
        // Check if should force watch ad
        val shouldForceAd = state.adWatchCount >= config.forceWatchAdThreshold && !state.isPremium
        
        _promotionState.update {
            it.copy(
                showPaywall = shouldShowPaywall && !state.hasShownPaywallThisSession,
                showLimitedTimeOffer = isOfferActive,
                offerDiscountPercent = config.offerDiscountPercent,
                offerHoursRemaining = if (isOfferActive && state.offerStartTime != null) {
                    val elapsed = System.currentTimeMillis() - state.offerStartTime
                    val remaining = (config.offerHours * 60 * 60 * 1000) - elapsed
                    (remaining / (60 * 60 * 1000)).toInt()
                } else 0,
                hitImportLimit = importLimitHit,
                hitStorageLimit = storageLimitHit,
                forceAdWatch = shouldForceAd,
                upsellAction = determineUpsellAction(config, importLimitHit, storageLimitHit)
            )
        }
    }
    
    private fun determineUpsellAction(config: RemoteConfig, importLimit: Boolean, storageLimit: Boolean): UpsellAction? {
        return when {
            importLimit -> UpsellAction.IMPORT_LIMIT
            storageLimit -> UpsellAction.STORAGE_LIMIT
            else -> null
        }
    }
    
    /**
     * Call when app launches to increment session and check promotions
     */
    suspend fun onAppLaunch() {
        incrementSessionCount()
        remoteConfigManager.fetchAndActivate()
        checkAndUpdatePromotions()
    }
    
    /**
     * Call when user imports an item
     * Returns true if upsell should be shown
     */
    suspend fun onImportAction(): Boolean {
        prefs.edit().putInt(PREF_IMPORT_COUNT, _promotionState.value.importCount + 1).apply()
        loadState()
        checkAndUpdatePromotions()
        return remoteConfigManager.getConfig().showUpsellOnImport && !_promotionState.value.isPremium
    }
    
    /**
     * Call when user exports an item
     */
    fun onExportAction(): Boolean {
        return remoteConfigManager.getConfig().showUpsellOnExport && !_promotionState.value.isPremium
    }
    
    /**
     * Call when user deletes an item
     */
    fun onDeleteAction(): Boolean {
        return remoteConfigManager.getConfig().showUpsellOnDelete && !_promotionState.value.isPremium
    }
    
    /**
     * Start limited time offer
     */
    fun startLimitedTimeOffer() {
        val startTime = System.currentTimeMillis()
        prefs.edit().putLong(PREF_OFFER_START_TIME, startTime).apply()
        loadState()
        checkAndUpdatePromotions()
    }
    
    /**
     * Mark paywall as shown this session
     */
    fun markPaywallShown() {
        _promotionState.update { it.copy(hasShownPaywallThisSession = true, showPaywall = false) }
    }
    
    /**
     * Record ad watch
     */
    fun recordAdWatch() {
        prefs.edit()
            .putInt(PREF_AD_WATCH_COUNT, _promotionState.value.adWatchCount + 1)
            .putLong(PREF_LAST_AD_WATCH, System.currentTimeMillis())
            .apply()
        _promotionState.update { 
            it.copy(adWatchCount = it.adWatchCount + 1, forceAdWatch = false) 
        }
    }
    
    /**
     * Get effective pricing (supports A/B testing)
     */
    fun getEffectivePricing(): PricingConfig {
        return remoteConfigManager.getEffectivePrices()
    }
    
    /**
     * Check if feature is enabled via remote config
     */
    fun isFeatureEnabled(feature: FeatureFlag): Boolean {
        val config = remoteConfigManager.getConfig()
        return when (feature) {
            FeatureFlag.DECOY_VAULT -> config.enableDecoyVault
            FeatureFlag.INTRUDER_SELFIE -> config.enableIntruderSelfie
            FeatureFlag.SHAKE_EXIT -> config.enableShakeExit
            FeatureFlag.BREAK_IN_ALERTS -> config.enableBreakInAlerts
        }
    }
    
    /**
     * Check app kill switch and version blocking
     */
    fun checkAppStatus(currentVersionCode: Int): AppStatus {
        return when {
            remoteConfigManager.isAppDisabled() -> AppStatus.KILLED(
                remoteConfigManager.getConfig().killMessage
            )
            remoteConfigManager.isVersionBlocked(currentVersionCode) -> AppStatus.FORCE_UPDATE(
                remoteConfigManager.getConfig().upgradeMessage
            )
            else -> AppStatus.OK
        }
    }
    
    /**
     * Show interstitial if conditions met
     */
    fun showInterstitialIfReady(activity: Activity) {
        if (!_promotionState.value.isPremium) {
            adMobManager.showInterstitialIfReady(activity)
        }
    }
    
    /**
     * Show rewarded ad for continued access
     */
    fun showRewardedAd(activity: Activity, onComplete: () -> Unit) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = { _ ->
                recordAdWatch()
            },
            onAdClosed = onComplete
        )
    }
    
    private suspend fun incrementSessionCount() {
        val newCount = prefs.getInt(PREF_SESSION_COUNT, 0) + 1
        prefs.edit()
            .putInt(PREF_SESSION_COUNT, newCount)
            .putLong(PREF_LAST_PROMO_TIME, System.currentTimeMillis())
            .apply()
        _promotionState.update { it.copy(sessionCount = newCount) }
    }
    
    fun resetCounters() {
        prefs.edit()
            .putInt(PREF_IMPORT_COUNT, 0)
            .putInt(PREF_AD_WATCH_COUNT, 0)
            .apply()
        loadState()
    }
}

data class PromotionState(
    val isPremium: Boolean = false,
    val sessionCount: Int = 0,
    val importCount: Int = 0,
    val adWatchCount: Int = 0,
    val offerStartTime: Long? = null,
    val showPaywall: Boolean = false,
    val hasShownPaywallThisSession: Boolean = false,
    val showLimitedTimeOffer: Boolean = false,
    val offerDiscountPercent: Int = 30,
    val offerHoursRemaining: Int = 0,
    val hitImportLimit: Boolean = false,
    val hitStorageLimit: Boolean = false,
    val forceAdWatch: Boolean = false,
    val upsellAction: UpsellAction? = null
)

enum class FeatureFlag {
    DECOY_VAULT,
    INTRUDER_SELFIE,
    SHAKE_EXIT,
    BREAK_IN_ALERTS
}

enum class UpsellAction {
    IMPORT_LIMIT,
    STORAGE_LIMIT,
    EXPORT_LIMIT,
    DELETE_LIMIT
}

sealed class AppStatus {
    object OK : AppStatus()
    data class FORCE_UPDATE(val message: String) : AppStatus()
    data class KILLED(val message: String) : AppStatus()
}
