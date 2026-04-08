package com.cover.app.data.repository

import com.cover.app.data.billing.BillingManager
import com.cover.app.data.local.dao.PremiumStatusDao
import com.cover.app.data.local.entity.PremiumStatusEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepository @Inject constructor(
    private val premiumStatusDao: PremiumStatusDao,
    private val billingManager: BillingManager
) {
    /**
     * Check if user has premium status
     */
    fun isPremium(): Flow<Boolean> {
        return premiumStatusDao.isPremium()
    }

    /**
     * Check premium status synchronously
     */
    suspend fun isPremiumSync(): Boolean {
        return premiumStatusDao.isPremiumSync()
    }

    /**
     * Get full premium status details
     */
    fun getPremiumStatus(): Flow<PremiumStatusEntity?> {
        return premiumStatusDao.getPremiumStatus()
    }

    /**
     * Update premium status from billing manager
     */
    suspend fun updatePremiumStatusFromBilling() {
        val isPremium = billingManager.isPremium.value
        premiumStatusDao.updatePremiumStatus(isPremium)
    }

    /**
     * Set premium status manually (for testing or after purchase)
     */
    suspend fun setPremiumStatus(
        isPremium: Boolean,
        subscriptionType: String? = null,
        purchaseDate: Long? = null,
        expiryDate: Long? = null,
        purchaseToken: String? = null
    ) {
        val status = PremiumStatusEntity(
            isPremium = isPremium,
            subscriptionType = subscriptionType,
            purchaseDate = purchaseDate,
            expiryDate = expiryDate,
            purchaseToken = purchaseToken
        )
        premiumStatusDao.insertOrUpdate(status)
    }

    /**
     * Clear premium status
     */
    suspend fun clearPremiumStatus() {
        premiumStatusDao.clearAll()
    }

    /**
     * Check if feature is available (free or premium)
     */
    suspend fun canUseFeature(requiresPremium: Boolean): Boolean {
        return if (requiresPremium) {
            isPremiumSync()
        } else {
            true
        }
    }

    /**
     * Get storage limit based on premium status
     */
    suspend fun getStorageLimitMB(): Int {
        return if (isPremiumSync()) -1 else 100 // -1 = unlimited, 100MB for free
    }

    /**
     * Check if ads should be shown
     */
    suspend fun shouldShowAds(): Boolean {
        return !isPremiumSync()
    }
}
