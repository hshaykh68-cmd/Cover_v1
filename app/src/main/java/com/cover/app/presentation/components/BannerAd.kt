package com.cover.app.presentation.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cover.app.data.admob.AdMobManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner ad composable that shows/hides based on premium status
 */
@Composable
fun BannerAd(
    adMobManager: AdMobManager,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) {
        // Don't show ads for premium users
        return
    }

    val context = LocalContext.current
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdMobManager.BANNER_AD_UNIT_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                loadAd(adMobManager.createBannerAdRequest())
            }
        },
        update = { adView ->
            adView.loadAd(adMobManager.createBannerAdRequest())
        }
    )
}

/**
 * Adaptive banner ad that adjusts to screen width
 */
@Composable
fun AdaptiveBannerAd(
    adMobManager: AdMobManager,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    val adWidth = context.resources.displayMetrics.widthPixels
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp, max = 100.dp),
        factory = {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = AdMobManager.BANNER_AD_UNIT_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                loadAd(adMobManager.createBannerAdRequest())
            }
        },
        update = { adView ->
            adView.loadAd(adMobManager.createBannerAdRequest())
        }
    )
}
