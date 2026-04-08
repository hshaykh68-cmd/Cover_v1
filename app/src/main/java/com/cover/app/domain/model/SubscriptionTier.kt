package com.cover.app.domain.model

enum class SubscriptionTier {
    FREE,
    MONTHLY,
    YEARLY,
    LIFETIME;

    val isPremium: Boolean
        get() = this != FREE

    val hasAds: Boolean
        get() = this == FREE
}
