package com.cover.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isDecoy: Boolean,
    val salt: ByteArray,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultEntity
        return id == other.id &&
                name == other.name &&
                isDecoy == other.isDecoy &&
                salt.contentEquals(other.salt) &&
                createdAt == other.createdAt &&
                lastAccessedAt == other.lastAccessedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + isDecoy.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastAccessedAt.hashCode()
        return result
    }
}
