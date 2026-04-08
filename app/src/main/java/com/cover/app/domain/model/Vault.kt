package com.cover.app.domain.model

data class Vault(
    val id: String,
    val name: String,
    val isDecoy: Boolean,
    val itemCount: Int = 0,
    val totalSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)
