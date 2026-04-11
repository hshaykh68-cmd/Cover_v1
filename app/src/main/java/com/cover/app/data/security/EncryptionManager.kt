package com.cover.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.content.SharedPreferences
import kotlin.getValue

@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "cover_master_key"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val PREFS_FILE = "cover_encrypted_prefs"
        
        // PBKDF2 settings for PIN-derived keys
        private const val PBKDF2_ITERATIONS = 100000
        private const val SALT_LENGTH = 16
    }

    private val masterKey: MasterKey by lazy { getOrCreateMasterKey() }
    private val encryptedPrefs: SharedPreferences by lazy { createEncryptedSharedPreferences() }

    /**
     * Get or create the hardware-backed master key from Android Keystore
     */
    private fun getOrCreateMasterKey(): MasterKey {
        return MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            .build()
    }

    /**
     * Create encrypted shared preferences for storing sensitive metadata
     */
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Encrypt content with master key (for general encryption)
     */
    fun encrypt(plaintext: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey.toSecretKey())
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        
        return EncryptedData(iv, ciphertext)
    }

    /**
     * Decrypt content with master key
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey.toSecretKey(), spec)
        
        return cipher.doFinal(encryptedData.ciphertext)
    }

    /**
     * Encrypt content with a password-derived key (for vault encryption)
     * This is used when content needs to be unlocked with a specific PIN
     */
    fun encryptWithPin(plaintext: ByteArray, pin: String, salt: ByteArray): EncryptedData {
        val key = deriveKeyFromPin(pin, salt)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        
        return EncryptedData(iv, ciphertext)
    }

    /**
     * Decrypt content with a password-derived key
     */
    fun decryptWithPin(encryptedData: EncryptedData, pin: String, salt: ByteArray): ByteArray {
        val key = deriveKeyFromPin(pin, salt)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(encryptedData.ciphertext)
    }

    /**
     * Derive encryption key from PIN using PBKDF2 (public for PinManager)
     */
    fun deriveKeyFromPin(pin: String, salt: ByteArray): SecretKey {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_SIZE
        )
        val tmp = factory.generateSecret(spec)
        return javax.crypto.spec.SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Hash PIN using PBKDF2 for secure storage (returns hex string)
     */
    fun hashPinWithPBKDF2(pin: String, salt: ByteArray): String {
        val key = deriveKeyFromPin(pin, salt)
        return key.encoded.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate random salt for key derivation
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        java.security.SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Generate random IV for encryption
     */
    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * Store a string securely in encrypted shared preferences
     */
    fun storeSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    /**
     * Retrieve a string from encrypted shared preferences
     */
    fun retrieveSecureString(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    /**
     * Store a boolean securely
     */
    fun storeSecureBoolean(key: String, value: Boolean) {
        encryptedPrefs.edit().putBoolean(key, value).apply()
    }

    /**
     * Retrieve a boolean
     */
    fun retrieveSecureBoolean(key: String, default: Boolean = false): Boolean {
        return encryptedPrefs.getBoolean(key, default)
    }

    /**
     * Store an integer securely
     */
    fun storeSecureInt(key: String, value: Int) {
        encryptedPrefs.edit().putInt(key, value).apply()
    }

    /**
     * Retrieve an integer
     */
    fun retrieveSecureInt(key: String, default: Int = 0): Int {
        return encryptedPrefs.getInt(key, default)
    }

    /**
     * Store a long securely
     */
    fun storeSecureLong(key: String, value: Long) {
        encryptedPrefs.edit().putLong(key, value).apply()
    }

    /**
     * Retrieve a long
     */
    fun retrieveSecureLong(key: String, default: Long = 0L): Long {
        return encryptedPrefs.getLong(key, default)
    }

    /**
     * Clear all secure storage
     */
    fun clearSecureStorage() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Check if master key exists and is valid
     */
    fun isMasterKeyValid(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete master key (for complete reset)
     */
    fun deleteMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore errors during deletion
        }
    }

    private fun MasterKey.toSecretKey(): SecretKey {
        // Extract the actual SecretKey from MasterKey
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }
}

/**
 * Data class representing encrypted content with IV
 */
data class EncryptedData(
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    /**
     * Serialize to byte array for storage (IV + ciphertext)
     */
    fun toByteArray(): ByteArray {
        return iv + ciphertext
    }

    companion object {
        /**
         * Deserialize from byte array
         * IV is first 12 bytes, rest is ciphertext
         */
        fun fromByteArray(data: ByteArray): EncryptedData {
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            return EncryptedData(iv, ciphertext)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}
