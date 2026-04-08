package com.cover.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoEncryptedPath: String?, // Intruder selfie filename
    val locationLat: Double?,
    val locationLng: Double?,
    val attemptedPin: String?, // Hashed attempted PIN
    val isDecoyVault: Boolean, // Whether decoy was accessed
    val isUploaded: Boolean = false // For future cloud upload feature
)
