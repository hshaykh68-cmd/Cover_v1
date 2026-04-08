package com.cover.app.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for secure storage of sensitive data
 * Wraps EncryptionManager to provide higher-level storage operations
 */
@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    /**
     * Store encrypted data with a key
     */
    fun storeEncryptedData(key: String, data: ByteArray) {
        val encrypted = encryptionManager.encrypt(data)
        // Store to encrypted prefs
        encryptionManager.storeSecureString("${key}_iv", android.util.Base64.encodeToString(encrypted.iv, android.util.Base64.NO_WRAP))
        encryptionManager.storeSecureString(key, android.util.Base64.encodeToString(encrypted.ciphertext, android.util.Base64.NO_WRAP))
    }

    /**
     * Retrieve and decrypt data
     */
    fun retrieveEncryptedData(key: String): ByteArray? {
        val ivString = encryptionManager.retrieveSecureString("${key}_iv") ?: return null
        val ciphertextString = encryptionManager.retrieveSecureString(key) ?: return null
        
        val iv = android.util.Base64.decode(ivString, android.util.Base64.NO_WRAP)
        val ciphertext = android.util.Base64.decode(ciphertextString, android.util.Base64.NO_WRAP)
        
        return encryptionManager.decrypt(EncryptedData(iv, ciphertext))
    }

    /**
     * Store a string securely
     */
    fun storeString(key: String, value: String) {
        encryptionManager.storeSecureString(key, value)
    }

    /**
     * Retrieve a string
     */
    fun retrieveString(key: String): String? {
        return encryptionManager.retrieveSecureString(key)
    }

    /**
     * Store a boolean securely
     */
    fun storeBoolean(key: String, value: Boolean) {
        encryptionManager.storeSecureBoolean(key, value)
    }

    /**
     * Retrieve a boolean
     */
    fun retrieveBoolean(key: String, default: Boolean = false): Boolean {
        return encryptionManager.retrieveSecureBoolean(key, default)
    }

    /**
     * Clear all stored data
     */
    fun clearAll() {
        encryptionManager.clearSecureStorage()
    }
}
