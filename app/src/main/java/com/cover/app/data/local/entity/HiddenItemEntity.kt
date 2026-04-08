package com.cover.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hidden_items",
    foreignKeys = [
        ForeignKey(
            entity = VaultEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vaultId")]
)
data class HiddenItemEntity(
    @PrimaryKey
    val id: String,
    val vaultId: String,
    val encryptedPath: String, // Random UUID filename in secure storage
    val originalName: String, // Original filename (encrypted in database via SQLCipher)
    val type: ItemType,
    val size: Long,
    val thumbnailEncryptedPath: String?, // Thumbnail ID if applicable
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class ItemType {
        PHOTO,
        VIDEO,
        AUDIO,
        DOCUMENT,
        FILE,
        APP
    }
}
