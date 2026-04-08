package com.cover.app.data.remoteconfig

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Remote Config keys
        const val KEY_FORCE_UPGRADE = "force_upgrade"
        const val KEY_MIN_VERSION = "min_version_code"
        const val KEY_UPGRADE_MESSAGE = "upgrade_message"
        
        // Monetization keys
        const val KEY_SHOW_PAYWALL_ON_LAUNCH = "show_paywall_on_launch"
        const val KEY_PAYWALL_DELAY_MS = "paywall_delay_ms"
        const val KEY_LIMITED_TIME_OFFER_ENABLED = "limited_time_offer_enabled"
        const val KEY_OFFER_DISCOUNT_PERCENT = "offer_discount_percent"
        const val KEY_OFFER_HOURS = "offer_hours"
        const val KEY_OFFER_MESSAGE = "offer_message"
        
        // Enhanced monetization config
        const val KEY_PRICING_HIGHLIGHT = "pricing_highlight"
        const val KEY_YEARLY_SAVE_BADGE = "yearly_save_badge"
        const val KEY_CURRENT_PROMO = "current_promo"
        const val KEY_PROMO_DISCOUNT_PERCENT = "promo_discount_percent"
        const val KEY_PROMO_URGENCY_HOURS = "promo_urgency_hours"
        const val KEY_TRIAL_ENABLED = "trial_enabled"
        const val KEY_TRIAL_DAYS = "trial_days"
        
        // Aggressive promotion keys
        const val KEY_PROMO_FREQUENCY = "promo_frequency_sessions"
        const val KEY_FORCE_WATCH_AD_THRESHOLD = "force_watch_ad_threshold"
        const val KEY_PREMIUM_FEATURE_NAG_INTERVAL = "premium_feature_nag_interval"
        const val KEY_IMPORT_LIMIT_FREE = "import_limit_free"
        const val KEY_STORAGE_LIMIT_MB = "storage_limit_mb"
        const val KEY_SHOW_UPSELL_ON_IMPORT = "show_upsell_on_import"
        const val KEY_SHOW_UPSELL_ON_DELETE = "show_upsell_on_delete"
        const val KEY_SHOW_UPSELL_ON_EXPORT = "show_upsell_on_export"
        
        // Enhanced feature limit config
        const val KEY_MAX_FREE_ITEMS = "max_free_items"
        const val KEY_MAX_FREE_VAULTS = "max_free_vaults"
        const val KEY_ENABLE_APP_HIDING_FREE = "enable_app_hiding_free"
        const val KEY_ENABLE_INTRUDER_CAPTURE_FREE = "enable_intruder_capture_free"
        const val KEY_ENABLE_SHAKE_CLOSE_FREE = "enable_shake_close_free"
        const val KEY_MAX_IMPORT_PER_DAY_FREE = "max_import_per_day_free"
        
        // UI/UX Config
        const val KEY_PRIMARY_ACCENT_COLOR = "primary_accent_color"
        const val KEY_CALCULATOR_STYLE = "calculator_style"
        const val KEY_VAULT_GRID_COLUMNS = "vault_grid_columns"
        const val KEY_SHOW_TUTORIAL_ON_FIRST_LAUNCH = "show_tutorial_on_first_launch"
        const val KEY_UPGRADE_BUTTON_STYLE = "upgrade_button_style"
        
        // Upsell Trigger Config
        const val KEY_SHOW_UPSELL_ON_ITEM_COUNT = "show_upsell_on_item_count"
        const val KEY_SHOW_UPSELL_AFTER_APP_OPENS = "show_upsell_after_app_opens"
        const val KEY_SHOW_UPSELL_ON_DECOY_ACCESS = "show_upsell_on_decoy_access"
        const val KEY_SHOW_UPSELL_AFTER_FAILED_CAPTURE = "show_upsell_after_failed_capture"
        const val KEY_FULLSCREEN_UPSELL_FREQUENCY_DAYS = "fullscreen_upsell_frequency_days"
        
        // A/B Testing keys
        const val KEY_AB_TEST_COHORT = "ab_test_cohort"
        const val KEY_EXPERIMENT_VARIANT = "experiment_variant"
        
        // Realtime config
        const val KEY_CONFIG_FETCH_INTERVAL_MINUTES = "config_fetch_interval_minutes"
        const val KEY_CONFIG_MINIMUM_FETCH_INTERVAL = "config_minimum_fetch_interval"
        const val KEY_ENABLE_REALTIME_CONFIG = "enable_realtime_config"
        const val KEY_CONFIG_VERSION = "config_version"
        
        // A/B test keys
        const val KEY_PAYWALL_VARIANT = "paywall_variant"
        const val KEY_PRICE_TEST_ENABLED = "price_test_enabled"
        const val KEY_TEST_MONTHLY_PRICE = "test_monthly_price"
        const val KEY_TEST_YEARLY_PRICE = "test_yearly_price"
        
        // Feature flags
        const val KEY_ENABLE_DECOY_VAULT = "enable_decoy_vault"
        const val KEY_ENABLE_INTRUDER_SELFIE = "enable_intruder_selfie"
        const val KEY_ENABLE_SHAKE_EXIT = "enable_shake_exit"
        const val KEY_ENABLE_BREAK_IN_ALERTS = "enable_break_in_alerts"
        
        // App kill switch
        const val KEY_APP_ENABLED = "app_enabled"
        const val KEY_KILL_MESSAGE = "kill_message"
        
        // Default values
        const val DEFAULT_PAYWALL_DELAY_MS = 3000L
        const val DEFAULT_PROMO_FREQUENCY = 3
        const val DEFAULT_IMPORT_LIMIT_FREE = 10
        const val DEFAULT_STORAGE_LIMIT_MB = 100
        const val DEFAULT_OFFER_DISCOUNT = 30
        const val DEFAULT_OFFER_HOURS = 24
        const val DEFAULT_MAX_FREE_ITEMS = 50
        const val DEFAULT_MAX_FREE_VAULTS = 1
        const val DEFAULT_MAX_IMPORT_PER_DAY = 10
        const val DEFAULT_VAULT_GRID_COLUMNS = 3
        const val DEFAULT_TRIAL_DAYS = 3
        const val DEFAULT_PROMO_URGENCY_HOURS = 24
        const val DEFAULT_CONFIG_FETCH_INTERVAL = 60
        const val DEFAULT_CONFIG_MINIMUM_FETCH = 15
    }

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private val _configLoaded = MutableStateFlow(false)
    val configLoaded: StateFlow<Boolean> = _configLoaded.asStateFlow()

    private val _config = MutableStateFlow(RemoteConfig())
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()

    init {
        setupRemoteConfig()
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        // Enable real-time config updates if configured
        remoteConfig.addOnConfigUpdateListener(object : com.google.firebase.remoteconfig.ConfigUpdateListener {
            override fun onUpdate(configUpdate: com.google.firebase.remoteconfig.ConfigUpdate) {
                // Activate the fetched config immediately
                remoteConfig.activate().addOnCompleteListener {
                    if (it.isSuccessful) {
                        updateLocalConfig()
                        _configLoaded.value = true
                    }
                }
            }

            override fun onError(error: com.google.firebase.remoteconfig.FirebaseRemoteConfigException) {
                // Silently fail - we'll use cached values
            }
        })
    }

    private fun setupRemoteConfig() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (isDebugBuild()) 60 else 3600 // 1 min debug, 1 hour prod
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values
        val defaults = mapOf(
            KEY_FORCE_UPGRADE to false,
            KEY_MIN_VERSION to 1,
            KEY_UPGRADE_MESSAGE to "A new version is available. Please update to continue.",
            KEY_SHOW_PAYWALL_ON_LAUNCH to true,
            KEY_PAYWALL_DELAY_MS to DEFAULT_PAYWALL_DELAY_MS,
            KEY_LIMITED_TIME_OFFER_ENABLED to false,
            KEY_OFFER_DISCOUNT_PERCENT to DEFAULT_OFFER_DISCOUNT,
            KEY_OFFER_HOURS to DEFAULT_OFFER_HOURS,
            KEY_OFFER_MESSAGE to "Limited time offer! Save {discount}% on Premium!",
            
            // Enhanced monetization defaults
            KEY_PRICING_HIGHLIGHT to "yearly",
            KEY_YEARLY_SAVE_BADGE to "58%",
            KEY_CURRENT_PROMO to "none",
            KEY_PROMO_DISCOUNT_PERCENT to 30,
            KEY_PROMO_URGENCY_HOURS to DEFAULT_PROMO_URGENCY_HOURS,
            KEY_TRIAL_ENABLED to false,
            KEY_TRIAL_DAYS to DEFAULT_TRIAL_DAYS,
            
            KEY_PROMO_FREQUENCY to DEFAULT_PROMO_FREQUENCY,
            KEY_FORCE_WATCH_AD_THRESHOLD to 5,
            KEY_PREMIUM_FEATURE_NAG_INTERVAL to 3,
            KEY_IMPORT_LIMIT_FREE to DEFAULT_IMPORT_LIMIT_FREE,
            KEY_STORAGE_LIMIT_MB to DEFAULT_STORAGE_LIMIT_MB,
            KEY_SHOW_UPSELL_ON_IMPORT to true,
            KEY_SHOW_UPSELL_ON_DELETE to true,
            KEY_SHOW_UPSELL_ON_EXPORT to true,
            
            // Enhanced feature limit defaults
            KEY_MAX_FREE_ITEMS to DEFAULT_MAX_FREE_ITEMS,
            KEY_MAX_FREE_VAULTS to DEFAULT_MAX_FREE_VAULTS,
            KEY_ENABLE_APP_HIDING_FREE to false,
            KEY_ENABLE_INTRUDER_CAPTURE_FREE to true,
            KEY_ENABLE_SHAKE_CLOSE_FREE to true,
            KEY_MAX_IMPORT_PER_DAY_FREE to DEFAULT_MAX_IMPORT_PER_DAY,
            
            // UI/UX defaults
            KEY_PRIMARY_ACCENT_COLOR to "#2196F3",
            KEY_CALCULATOR_STYLE to "ios",
            KEY_VAULT_GRID_COLUMNS to DEFAULT_VAULT_GRID_COLUMNS,
            KEY_SHOW_TUTORIAL_ON_FIRST_LAUNCH to true,
            KEY_UPGRADE_BUTTON_STYLE to "fab",
            
            // Upsell trigger defaults (comma-separated lists as strings)
            KEY_SHOW_UPSELL_ON_ITEM_COUNT to "40,45,50",
            KEY_SHOW_UPSELL_AFTER_APP_OPENS to "3,7,14",
            KEY_SHOW_UPSELL_ON_DECOY_ACCESS to true,
            KEY_SHOW_UPSELL_AFTER_FAILED_CAPTURE to true,
            KEY_FULLSCREEN_UPSELL_FREQUENCY_DAYS to 7,
            
            // A/B Testing defaults
            KEY_AB_TEST_COHORT to "control",
            KEY_EXPERIMENT_VARIANT to "default",
            
            // Realtime config defaults
            KEY_CONFIG_FETCH_INTERVAL_MINUTES to DEFAULT_CONFIG_FETCH_INTERVAL,
            KEY_CONFIG_MINIMUM_FETCH_INTERVAL to DEFAULT_CONFIG_MINIMUM_FETCH,
            KEY_ENABLE_REALTIME_CONFIG to true,
            KEY_CONFIG_VERSION to "1.0.0",
            
            KEY_PAYWALL_VARIANT to "default",
            KEY_PRICE_TEST_ENABLED to false,
            KEY_TEST_MONTHLY_PRICE to "",
            KEY_TEST_YEARLY_PRICE to "",
            KEY_ENABLE_DECOY_VAULT to true,
            KEY_ENABLE_INTRUDER_SELFIE to true,
            KEY_ENABLE_SHAKE_EXIT to true,
            KEY_ENABLE_BREAK_IN_ALERTS to true,
            KEY_APP_ENABLED to true,
            KEY_KILL_MESSAGE to "This app version is no longer supported."
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    /**
     * Fetch and activate remote config
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
            updateLocalConfig()
            _configLoaded.value = true
            true
        } catch (e: Exception) {
            // Use cached values
            updateLocalConfig()
            false
        }
    }

    /**
     * Update local config object from Remote Config
     */
    private fun updateLocalConfig() {
        _config.value = RemoteConfig(
            forceUpgrade = remoteConfig.getBoolean(KEY_FORCE_UPGRADE),
            minVersionCode = remoteConfig.getLong(KEY_MIN_VERSION).toInt(),
            upgradeMessage = remoteConfig.getString(KEY_UPGRADE_MESSAGE),
            
            showPaywallOnLaunch = remoteConfig.getBoolean(KEY_SHOW_PAYWALL_ON_LAUNCH),
            paywallDelayMs = remoteConfig.getLong(KEY_PAYWALL_DELAY_MS),
            
            limitedTimeOfferEnabled = remoteConfig.getBoolean(KEY_LIMITED_TIME_OFFER_ENABLED),
            offerDiscountPercent = remoteConfig.getLong(KEY_OFFER_DISCOUNT_PERCENT).toInt(),
            offerHours = remoteConfig.getLong(KEY_OFFER_HOURS).toInt(),
            offerMessage = remoteConfig.getString(KEY_OFFER_MESSAGE),
            
            // Enhanced monetization config
            pricingHighlight = remoteConfig.getString(KEY_PRICING_HIGHLIGHT),
            yearlySaveBadge = remoteConfig.getString(KEY_YEARLY_SAVE_BADGE),
            currentPromo = remoteConfig.getString(KEY_CURRENT_PROMO),
            promoDiscountPercent = remoteConfig.getLong(KEY_PROMO_DISCOUNT_PERCENT).toInt(),
            promoUrgencyHours = remoteConfig.getLong(KEY_PROMO_URGENCY_HOURS).toInt(),
            trialEnabled = remoteConfig.getBoolean(KEY_TRIAL_ENABLED),
            trialDays = remoteConfig.getLong(KEY_TRIAL_DAYS).toInt(),
            
            promoFrequencySessions = remoteConfig.getLong(KEY_PROMO_FREQUENCY).toInt(),
            forceWatchAdThreshold = remoteConfig.getLong(KEY_FORCE_WATCH_AD_THRESHOLD).toInt(),
            premiumFeatureNagInterval = remoteConfig.getLong(KEY_PREMIUM_FEATURE_NAG_INTERVAL).toInt(),
            
            importLimitFree = remoteConfig.getLong(KEY_IMPORT_LIMIT_FREE).toInt(),
            storageLimitMB = remoteConfig.getLong(KEY_STORAGE_LIMIT_MB).toInt(),
            
            // Enhanced feature limits
            maxFreeItems = remoteConfig.getLong(KEY_MAX_FREE_ITEMS).toInt(),
            maxFreeVaults = remoteConfig.getLong(KEY_MAX_FREE_VAULTS).toInt(),
            enableAppHidingFree = remoteConfig.getBoolean(KEY_ENABLE_APP_HIDING_FREE),
            enableIntruderCaptureFree = remoteConfig.getBoolean(KEY_ENABLE_INTRUDER_CAPTURE_FREE),
            enableShakeCloseFree = remoteConfig.getBoolean(KEY_ENABLE_SHAKE_CLOSE_FREE),
            maxImportPerDayFree = remoteConfig.getLong(KEY_MAX_IMPORT_PER_DAY_FREE).toInt(),
            
            // UI/UX Config
            primaryAccentColor = remoteConfig.getString(KEY_PRIMARY_ACCENT_COLOR),
            calculatorStyle = remoteConfig.getString(KEY_CALCULATOR_STYLE),
            vaultGridColumns = remoteConfig.getLong(KEY_VAULT_GRID_COLUMNS).toInt(),
            showTutorialOnFirstLaunch = remoteConfig.getBoolean(KEY_SHOW_TUTORIAL_ON_FIRST_LAUNCH),
            upgradeButtonStyle = remoteConfig.getString(KEY_UPGRADE_BUTTON_STYLE),
            
            // Upsell triggers
            showUpsellOnItemCount = parseIntList(remoteConfig.getString(KEY_SHOW_UPSELL_ON_ITEM_COUNT)),
            showUpsellAfterAppOpens = parseIntList(remoteConfig.getString(KEY_SHOW_UPSELL_AFTER_APP_OPENS)),
            showUpsellOnDecoyAccess = remoteConfig.getBoolean(KEY_SHOW_UPSELL_ON_DECOY_ACCESS),
            showUpsellAfterFailedCapture = remoteConfig.getBoolean(KEY_SHOW_UPSELL_AFTER_FAILED_CAPTURE),
            fullscreenUpsellFrequencyDays = remoteConfig.getLong(KEY_FULLSCREEN_UPSELL_FREQUENCY_DAYS).toInt(),
            
            // A/B Testing
            abTestCohort = remoteConfig.getString(KEY_AB_TEST_COHORT),
            experimentVariant = remoteConfig.getString(KEY_EXPERIMENT_VARIANT),
            
            // Realtime config
            configFetchIntervalMinutes = remoteConfig.getLong(KEY_CONFIG_FETCH_INTERVAL_MINUTES).toInt(),
            configMinimumFetchInterval = remoteConfig.getLong(KEY_CONFIG_MINIMUM_FETCH_INTERVAL).toInt(),
            enableRealtimeConfig = remoteConfig.getBoolean(KEY_ENABLE_REALTIME_CONFIG),
            configVersion = remoteConfig.getString(KEY_CONFIG_VERSION),
            
            showUpsellOnImport = remoteConfig.getBoolean(KEY_SHOW_UPSELL_ON_IMPORT),
            showUpsellOnDelete = remoteConfig.getBoolean(KEY_SHOW_UPSELL_ON_DELETE),
            showUpsellOnExport = remoteConfig.getBoolean(KEY_SHOW_UPSELL_ON_EXPORT),
            
            paywallVariant = remoteConfig.getString(KEY_PAYWALL_VARIANT),
            priceTestEnabled = remoteConfig.getBoolean(KEY_PRICE_TEST_ENABLED),
            testMonthlyPrice = remoteConfig.getString(KEY_TEST_MONTHLY_PRICE),
            testYearlyPrice = remoteConfig.getString(KEY_TEST_YEARLY_PRICE),
            
            enableDecoyVault = remoteConfig.getBoolean(KEY_ENABLE_DECOY_VAULT),
            enableIntruderSelfie = remoteConfig.getBoolean(KEY_ENABLE_INTRUDER_SELFIE),
            enableShakeExit = remoteConfig.getBoolean(KEY_ENABLE_SHAKE_EXIT),
            enableBreakInAlerts = remoteConfig.getBoolean(KEY_ENABLE_BREAK_IN_ALERTS),
            
            appEnabled = remoteConfig.getBoolean(KEY_APP_ENABLED),
            killMessage = remoteConfig.getString(KEY_KILL_MESSAGE)
        )
    }

    /**
     * Check if app version is blocked
     */
    fun isVersionBlocked(currentVersionCode: Int): Boolean {
        return _config.value.forceUpgrade && currentVersionCode < _config.value.minVersionCode
    }

    /**
     * Check if app is globally disabled
     */
    fun isAppDisabled(): Boolean {
        return !_config.value.appEnabled
    }

    /**
     * Get current config values
     */
    fun getConfig(): RemoteConfig = _config.value

    /**
     * Should show paywall based on session count
     */
    fun shouldShowPaywall(sessionCount: Int): Boolean {
        val config = _config.value
        return config.showPaywallOnLaunch && 
               sessionCount % config.promoFrequencySessions == 0 && 
               sessionCount > 0
    }

    /**
     * Check if limited time offer is active
     */
    fun isLimitedTimeOfferActive(offerStartTime: Long?): Boolean {
        if (!_config.value.limitedTimeOfferEnabled || offerStartTime == null) return false
        
        val offerDurationMs = _config.value.offerHours * 60 * 60 * 1000L
        return System.currentTimeMillis() - offerStartTime < offerDurationMs
    }

    /**
     * Check if user hit import limit
     */
    fun hasHitImportLimit(importCount: Int, isPremium: Boolean): Boolean {
        if (isPremium) return false
        return importCount >= _config.value.importLimitFree
    }

    /**
     * Check if storage limit exceeded
     */
    fun hasExceededStorageLimit(usedMB: Int, isPremium: Boolean): Boolean {
        if (isPremium) return false
        return _config.value.storageLimitMB > 0 && usedMB >= _config.value.storageLimitMB
    }

    /**
     * Get effective prices (support A/B testing)
     */
    fun getEffectivePrices(): PricingConfig {
        val config = _config.value
        return if (config.priceTestEnabled && config.testMonthlyPrice.isNotEmpty()) {
            PricingConfig(
                monthly = config.testMonthlyPrice,
                yearly = config.testYearlyPrice.takeIf { it.isNotEmpty() } ?: "$9.99"
            )
        } else {
            PricingConfig(
                monthly = "$1.99",
                yearly = "$9.99",
                lifetime = "$24.99"
            )
        }
    }

    private fun isDebugBuild(): Boolean {
        return context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    
    /**
     * Parse comma-separated integer list from config string
     */
    private fun parseIntList(value: String): List<Int> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
    }
    
    /**
     * Check if specific item count should trigger upsell
     */
    fun shouldTriggerUpsellOnItemCount(itemCount: Int): Boolean {
        return itemCount in _config.value.showUpsellOnItemCount
    }
    
    /**
     * Check if specific app open count should trigger upsell
     */
    fun shouldTriggerUpsellOnAppOpens(appOpenCount: Int): Boolean {
        return appOpenCount in _config.value.showUpsellAfterAppOpens
    }
    
    /**
     * Check if user has exceeded max items for free tier
     */
    fun hasExceededMaxItems(itemCount: Int, isPremium: Boolean): Boolean {
        if (isPremium) return false
        return _config.value.maxFreeItems > 0 && itemCount >= _config.value.maxFreeItems
    }
    
    /**
     * Check if user has exceeded max vaults for free tier
     */
    fun hasExceededMaxVaults(vaultCount: Int, isPremium: Boolean): Boolean {
        if (isPremium) return false
        return _config.value.maxFreeVaults > 0 && vaultCount >= _config.value.maxFreeVaults
    }
    
    /**
     * Check if app hiding is enabled for free users
     */
    fun isAppHidingEnabledForFree(isPremium: Boolean): Boolean {
        return isPremium || _config.value.enableAppHidingFree
    }
    
    /**
     * Check if intruder capture is enabled for free users
     */
    fun isIntruderCaptureEnabledForFree(isPremium: Boolean): Boolean {
        return isPremium || _config.value.enableIntruderCaptureFree
    }
    
    /**
     * Check if shake exit is enabled for free users
     */
    fun isShakeExitEnabledForFree(isPremium: Boolean): Boolean {
        return isPremium || _config.value.enableShakeCloseFree
    }
}

data class RemoteConfig(
    // Upgrade blocking
    val forceUpgrade: Boolean = false,
    val minVersionCode: Int = 1,
    val upgradeMessage: String = "",
    
    // Paywall settings
    val showPaywallOnLaunch: Boolean = true,
    val paywallDelayMs: Long = 3000,
    
    // Limited time offers
    val limitedTimeOfferEnabled: Boolean = false,
    val offerDiscountPercent: Int = 30,
    val offerHours: Int = 24,
    val offerMessage: String = "",
    
    // Enhanced monetization config
    val pricingHighlight: String = "yearly",
    val yearlySaveBadge: String = "58%",
    val currentPromo: String = "none",
    val promoDiscountPercent: Int = 30,
    val promoUrgencyHours: Int = 24,
    val trialEnabled: Boolean = false,
    val trialDays: Int = 3,
    
    // Aggressive promotion
    val promoFrequencySessions: Int = 3,
    val forceWatchAdThreshold: Int = 5,
    val premiumFeatureNagInterval: Int = 3,
    
    // Feature limiting (basic)
    val importLimitFree: Int = 10,
    val storageLimitMB: Int = 100,
    
    // Enhanced feature limits
    val maxFreeItems: Int = 50,
    val maxFreeVaults: Int = 1,
    val enableAppHidingFree: Boolean = false,
    val enableIntruderCaptureFree: Boolean = true,
    val enableShakeCloseFree: Boolean = true,
    val maxImportPerDayFree: Int = 10,
    
    // UI/UX Config
    val primaryAccentColor: String = "#2196F3",
    val calculatorStyle: String = "ios",
    val vaultGridColumns: Int = 3,
    val showTutorialOnFirstLaunch: Boolean = true,
    val upgradeButtonStyle: String = "fab",
    
    // Upsell triggers
    val showUpsellOnItemCount: List<Int> = listOf(40, 45, 50),
    val showUpsellAfterAppOpens: List<Int> = listOf(3, 7, 14),
    val showUpsellOnDecoyAccess: Boolean = true,
    val showUpsellAfterFailedCapture: Boolean = true,
    val fullscreenUpsellFrequencyDays: Int = 7,
    
    // A/B testing
    val abTestCohort: String = "control",
    val experimentVariant: String = "default",
    
    // Realtime config
    val configFetchIntervalMinutes: Int = 60,
    val configMinimumFetchInterval: Int = 15,
    val enableRealtimeConfig: Boolean = true,
    val configVersion: String = "1.0.0",
    
    // Upsell triggers (basic)
    val showUpsellOnImport: Boolean = true,
    val showUpsellOnDelete: Boolean = true,
    val showUpsellOnExport: Boolean = true,
    
    // A/B testing (legacy)
    val paywallVariant: String = "default",
    val priceTestEnabled: Boolean = false,
    val testMonthlyPrice: String = "",
    val testYearlyPrice: String = "",
    
    // Feature flags
    val enableDecoyVault: Boolean = true,
    val enableIntruderSelfie: Boolean = true,
    val enableShakeExit: Boolean = true,
    val enableBreakInAlerts: Boolean = true,
    
    // Kill switch
    val appEnabled: Boolean = true,
    val killMessage: String = ""
)

data class PricingConfig(
    val monthly: String,
    val yearly: String,
    val lifetime: String = "$24.99"
)
