package com.cover.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to track which apps are hidden from the custom launcher.
 * Apps are NOT actually disabled - just filtered from view until vault is unlocked.
 */
@Entity(
    tableName = "hidden_apps",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId"), Index("packageName")]
)
data class HiddenAppEntity(
    @PrimaryKey
    val id: String,
    val vaultId: String,
    val packageName: String,
    val appName: String,
    val isHidden: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
