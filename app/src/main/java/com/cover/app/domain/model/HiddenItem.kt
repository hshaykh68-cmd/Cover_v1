package com.cover.app.domain.model

sealed class HiddenItem {
    abstract val id: String
    abstract val vaultId: String
    abstract val originalName: String
    abstract val size: Long
    abstract val createdAt: Long
    abstract val encryptedPath: String

    data class Photo(
        override val id: String,
        override val vaultId: String,
        override val originalName: String,
        override val size: Long,
        val thumbnailId: String?,
        override val encryptedPath: String,
        override val createdAt: Long = System.currentTimeMillis()
    ) : HiddenItem()

    data class Video(
        override val id: String,
        override val vaultId: String,
        override val originalName: String,
        override val size: Long,
        val thumbnailId: String?,
        override val encryptedPath: String,
        val duration: Long? = null,
        override val createdAt: Long = System.currentTimeMillis()
    ) : HiddenItem()

    data class Audio(
        override val id: String,
        override val vaultId: String,
        override val originalName: String,
        override val size: Long,
        override val encryptedPath: String,
        val duration: Long? = null,
        override val createdAt: Long = System.currentTimeMillis()
    ) : HiddenItem()

    data class Document(
        override val id: String,
        override val vaultId: String,
        override val originalName: String,
        override val size: Long,
        val mimeType: String?,
        override val encryptedPath: String,
        override val createdAt: Long = System.currentTimeMillis()
    ) : HiddenItem()

    data class App(
        override val id: String,
        override val vaultId: String,
        val packageName: String,
        val appName: String,
        override val originalName: String = appName,
        override val size: Long = 0L,
        override val encryptedPath: String = "",
        override val createdAt: Long = System.currentTimeMillis()
    ) : HiddenItem()
}
