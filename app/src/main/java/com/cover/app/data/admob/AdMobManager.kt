package com.cover.app.data.admob

import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Ad Unit IDs - use test IDs during development, replace with real IDs for production
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Test ID
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test ID
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test ID
        
        // Production IDs (replace these)
        // const val BANNER_AD_UNIT_ID = "ca-app-pub-YOUR_PUBLISHER_ID/BANNER_ID"
        // const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-YOUR_PUBLISHER_ID/INTERSTITIAL_ID"
        // const val REWARDED_AD_UNIT_ID = "ca-app-pub-YOUR_PUBLISHER_ID/REWARDED_ID"
        
        // Ad display settings
        const val INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes between interstitials
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShown = 0L

    init {
        initializeMobileAds()
    }

    private fun initializeMobileAds() {
        MobileAds.initialize(context) { initializationStatus ->
            _isInitialized.value = true
        }
        
        // Set test devices for development
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
    }

    /**
     * Create a banner ad request
     */
    fun createBannerAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Load interstitial ad
     */
    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Show interstitial ad if ready and enough time has passed
     */
    fun showInterstitialIfReady(activity: android.app.Activity, onAdClosed: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialShown < INTERSTITIAL_INTERVAL_MS) {
            onAdClosed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShown = System.currentTimeMillis()
                    onAdClosed()
                    // Preload next ad
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    // Ad is showing
                }
            }
            ad.show(activity)
        } else {
            onAdClosed()
            loadInterstitialAd()
        }
    }

    /**
     * Load rewarded ad
     */
    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    /**
     * Show rewarded ad
     */
    fun showRewardedAd(
        activity: android.app.Activity,
        onRewardEarned: (Int) -> Unit,
        onAdClosed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    onAdClosed()
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onAdClosed()
                }
            }
            
            ad.show(activity) { rewardItem ->
                onRewardEarned(rewardItem.amount)
            }
        } else {
            onAdClosed()
            loadRewardedAd()
        }
    }

    /**
     * Preload all ads
     */
    fun preloadAds() {
        loadInterstitialAd()
        loadRewardedAd()
    }

    /**
     * Check if should show ads (based on premium status)
     */
    suspend fun shouldShowAds(premiumRepository: com.cover.app.data.repository.PremiumRepository): Boolean {
        return !premiumRepository.isPremiumSync()
    }
}
