package com.cover.app.domain.model

data class IntruderLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoId: String? = null,
    val location: Location? = null,
    val attemptedPinHash: String? = null,
    val isDecoyVault: Boolean = false
) {
    data class Location(
        val latitude: Double,
        val longitude: Double
    )
}
