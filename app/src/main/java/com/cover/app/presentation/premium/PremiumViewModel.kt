package com.cover.app.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.billing.BillingManager
import com.cover.app.data.billing.BillingState
import com.cover.app.data.billing.PurchaseUpdate
import com.cover.app.data.remoteconfig.ABTestManager
import com.cover.app.data.remoteconfig.ConversionEvent
import com.cover.app.data.remoteconfig.PricingConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val abTestManager: ABTestManager
) : ViewModel() {

    private val _state = MutableStateFlow<PremiumUiState>(PremiumUiState.Loading)
    val state: StateFlow<PremiumUiState> = _state.asStateFlow()

    private var currentActivity: Activity? = null

    init {
        viewModelScope.launch {
            billingManager.billingState.collect { billingState ->
                when (billingState) {
                    is BillingState.ProductsLoaded -> {
                        val pricing = abTestManager.getEffectivePricing()
                        _state.value = PremiumUiState.Loaded(
                            monthlyPrice = pricing.monthly.takeIf { it.isNotEmpty() } 
                                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_MONTHLY),
                            yearlyPrice = pricing.yearly.takeIf { it.isNotEmpty() } 
                                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_YEARLY),
                            lifetimePrice = pricing.lifetime.takeIf { it.isNotEmpty() } 
                                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_LIFETIME),
                            paywallVariant = abTestManager.getPaywallVariant()
                        )
                        // Track paywall impression with variant
                        abTestManager.trackPaywallImpression(abTestManager.getPaywallVariant())
                        abTestManager.trackConversion(ConversionEvent.PAYWALL_VIEW)
                    }
                    is BillingState.Error -> {
                        _state.value = PremiumUiState.Error(billingState.message)
                    }
                    else -> {
                        if (_state.value !is PremiumUiState.Loaded) {
                            _state.value = PremiumUiState.Loading
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            billingManager.purchaseUpdate.collect { update ->
                when (update) {
                    is PurchaseUpdate.Success -> {
                        _state.value = PremiumUiState.PurchaseSuccess
                        // Track purchase conversion - we don't know which product was purchased
                        // from the Success update alone, so track generically
                        abTestManager.trackConversion(ConversionEvent.PURCHASE_COMPLETED)
                    }
                    is PurchaseUpdate.Error -> {
                        _state.value = PremiumUiState.Error("Purchase failed: ${update.message}")
                        abTestManager.trackConversion(
                            ConversionEvent.PAYWALL_DISMISS,
                            additionalParams = mapOf("reason" to "error: ${update.message}")
                        )
                    }
                    is PurchaseUpdate.Cancelled -> {
                        abTestManager.trackConversion(ConversionEvent.PAYWALL_DISMISS)
                        // Return to loaded state
                        if (_state.value is PremiumUiState.Error) {
                            val pricing = abTestManager.getEffectivePricing()
                            _state.value = PremiumUiState.Loaded(
                                monthlyPrice = pricing.monthly.takeIf { it.isNotEmpty() } 
                                    ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_MONTHLY),
                                yearlyPrice = pricing.yearly.takeIf { it.isNotEmpty() } 
                                    ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_YEARLY),
                                lifetimePrice = pricing.lifetime.takeIf { it.isNotEmpty() } 
                                    ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_LIFETIME),
                                paywallVariant = abTestManager.getPaywallVariant()
                            )
                        }
                    }
                    null -> {}
                }
            }
        }
    }

    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    fun purchaseMonthly() {
        currentActivity?.let { activity ->
            billingManager.launchPurchase(
                activity = activity,
                productId = BillingManager.PRODUCT_PREMIUM_MONTHLY,
                offerToken = null // Will be handled by BillingManager
            )
        }
    }

    fun purchaseYearly() {
        currentActivity?.let { activity ->
            billingManager.launchPurchase(
                activity = activity,
                productId = BillingManager.PRODUCT_PREMIUM_YEARLY,
                offerToken = null
            )
        }
    }

    fun purchaseLifetime() {
        currentActivity?.let { activity ->
            billingManager.launchPurchase(
                activity = activity,
                productId = BillingManager.PRODUCT_PREMIUM_LIFETIME
            )
        }
    }

    fun dismissSuccess() {
        val pricing = abTestManager.getEffectivePricing()
        _state.value = PremiumUiState.Loaded(
            monthlyPrice = pricing.monthly.takeIf { it.isNotEmpty() } 
                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_MONTHLY),
            yearlyPrice = pricing.yearly.takeIf { it.isNotEmpty() } 
                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_YEARLY),
            lifetimePrice = pricing.lifetime.takeIf { it.isNotEmpty() } 
                ?: billingManager.getPrice(BillingManager.PRODUCT_PREMIUM_LIFETIME),
            paywallVariant = abTestManager.getPaywallVariant()
        )
    }

    private fun extractPriceFromProduct(productId: String): Double {
        return when (productId) {
            BillingManager.PRODUCT_PREMIUM_MONTHLY -> 1.99
            BillingManager.PRODUCT_PREMIUM_YEARLY -> 9.99
            BillingManager.PRODUCT_PREMIUM_LIFETIME -> 24.99
            else -> 0.0
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.disconnect()
    }
}

sealed class PremiumUiState {
    object Loading : PremiumUiState()
    data class Loaded(
        val monthlyPrice: String,
        val yearlyPrice: String,
        val lifetimePrice: String,
        val paywallVariant: String = "default"
    ) : PremiumUiState()
    data class Error(val message: String) : PremiumUiState()
    object PurchaseSuccess : PremiumUiState()
}
