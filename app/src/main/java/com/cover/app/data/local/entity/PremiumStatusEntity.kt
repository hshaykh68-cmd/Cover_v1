package com.cover.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "premium_status")
data class PremiumStatusEntity(
    @PrimaryKey
    val id: String = "current_user",
    val isPremium: Boolean = false,
    val subscriptionType: String? = null, // "monthly", "yearly", "lifetime"
    val purchaseDate: Long? = null,
    val expiryDate: Long? = null,
    val purchaseToken: String? = null,
    val lastVerifiedAt: Long = System.currentTimeMillis()
)
