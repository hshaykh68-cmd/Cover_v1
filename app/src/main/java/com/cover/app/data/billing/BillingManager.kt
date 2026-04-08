package com.cover.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Product IDs - must match Google Play Console
        const val PRODUCT_PREMIUM_MONTHLY = "premium_monthly"
        const val PRODUCT_PREMIUM_YEARLY = "premium_yearly"
        const val PRODUCT_PREMIUM_LIFETIME = "premium_lifetime"
        
        // Fallback prices (will be overridden by Play Store)
        const val PRICE_MONTHLY = "$1.99"
        const val PRICE_YEARLY = "$9.99"
        const val PRICE_LIFETIME = "$24.99"
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Loading)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _purchaseUpdate = MutableStateFlow<PurchaseUpdate?>(null)
    val purchaseUpdate: StateFlow<PurchaseUpdate?> = _purchaseUpdate.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val purchaseListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseUpdate.value = PurchaseUpdate.Cancelled
            }
            else -> {
                _purchaseUpdate.value = PurchaseUpdate.Error(
                    billingResult.responseCode,
                    billingResult.debugMessage
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchaseListener)
        .enablePendingPurchases()
        .build()

    init {
        connectBilling()
    }

    private fun connectBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.value = BillingState.Connected
                    queryPurchases()
                    queryProducts()
                } else {
                    _billingState.value = BillingState.Error(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Disconnected
                // Retry connection
                connectBilling()
            }
        })
    }

    /**
     * Query existing purchases to restore premium status
     */
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { it.isAcknowledged && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (hasActiveSub) {
                    _isPremium.value = true
                } else {
                    // Check for lifetime
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    ) { inappResult, inappPurchases ->
                        val hasLifetime = inappPurchases.any { 
                            it.products.contains(PRODUCT_PREMIUM_LIFETIME) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED 
                        }
                        _isPremium.value = hasLifetime
                    }
                }
            }
        }
    }

    /**
     * Query product details for display
     */
    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.associateBy { it.productId }
                _billingState.value = BillingState.ProductsLoaded(details)
            }
        }
    }

    /**
     * Launch purchase flow
     */
    fun launchPurchase(activity: Activity, productId: String, offerToken: String? = null) {
        val currentState = _billingState.value
        if (currentState !is BillingState.ProductsLoaded) return

        val productDetails = currentState.products[productId] ?: return

        val productDetailsParams = if (offerToken != null) {
            // Subscription with offer
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            // One-time purchase
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Handle a purchase - acknowledge if needed
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPremium.value = true
                    _purchaseUpdate.value = PurchaseUpdate.Success
                } else {
                    _purchaseUpdate.value = PurchaseUpdate.Error(result.responseCode, result.debugMessage)
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isPremium.value = true
            _purchaseUpdate.value = PurchaseUpdate.Success
        }
    }

    /**
     * Check if user has premium
     */
    fun checkPremiumStatus(): Boolean {
        return _isPremium.value
    }

    /**
     * Get formatted price for display
     */
    fun getPrice(productId: String): String {
        val state = _billingState.value
        if (state is BillingState.ProductsLoaded) {
            val product = state.products[productId]
            return product?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                ?: product?.oneTimePurchaseOfferDetails?.formattedPrice
                ?: when (productId) {
                    PRODUCT_PREMIUM_MONTHLY -> PRICE_MONTHLY
                    PRODUCT_PREMIUM_YEARLY -> PRICE_YEARLY
                    PRODUCT_PREMIUM_LIFETIME -> PRICE_LIFETIME
                    else -> ""
                }
        }
        return when (productId) {
            PRODUCT_PREMIUM_MONTHLY -> PRICE_MONTHLY
            PRODUCT_PREMIUM_YEARLY -> PRICE_YEARLY
            PRODUCT_PREMIUM_LIFETIME -> PRICE_LIFETIME
            else -> ""
        }
    }

    /**
     * Disconnect billing client
     */
    fun disconnect() {
        billingClient.endConnection()
    }
}

sealed class BillingState {
    object Loading : BillingState()
    object Connected : BillingState()
    object Disconnected : BillingState()
    data class ProductsLoaded(
        val products: Map<String, ProductDetails>
    ) : BillingState()
    data class Error(val message: String) : BillingState()
}

sealed class PurchaseUpdate {
    object Success : PurchaseUpdate()
    object Cancelled : PurchaseUpdate()
    data class Error(val code: Int, val message: String) : PurchaseUpdate()
}
