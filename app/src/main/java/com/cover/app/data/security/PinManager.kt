package com.cover.app.data.security

import android.content.Context
import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.local.entity.VaultEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val PREFS_NAME = "pin_secure_prefs"
        private const val KEY_REAL_PIN_HASH = "real_pin_hash"
        private const val KEY_REAL_PIN_SALT = "real_pin_salt"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_hash"
        private const val KEY_DECOY_PIN_SALT = "decoy_pin_salt"
        private const val KEY_REAL_VAULT_ID = "real_vault_id"
        private const val KEY_DECOY_VAULT_ID = "decoy_vault_id"
        private const val KEY_PIN_SET = "pin_set"
        private const val MIN_PIN_LENGTH = 4
        private const val MAX_PIN_LENGTH = 8

        /**
         * Validate PIN format
         * Static version for use without instance
         */
        fun isValidPin(pin: String): Boolean {
            return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
        }
    }

    /**
     * Check if PIN has been set up
     */
    fun isPinSet(): Boolean {
        return encryptionManager.retrieveSecureBoolean(KEY_PIN_SET, false)
    }

    /**
     * Setup initial PINs and create vaults
     * Called during first app launch / onboarding
     */
    suspend fun setupPins(realPin: String, decoyPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate PINs
            if (!isValidPin(realPin)) {
                return@withContext Result.failure(IllegalArgumentException("Real PIN must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits"))
            }
            if (!isValidPin(decoyPin)) {
                return@withContext Result.failure(IllegalArgumentException("Decoy PIN must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits"))
            }
            if (realPin == decoyPin) {
                return@withContext Result.failure(IllegalArgumentException("Real and decoy PINs must be different"))
            }

            // Generate salts
            val realSalt = generateSalt()
            val decoySalt = generateSalt()

            // Hash PINs
            val realPinHash = hashPin(realPin, realSalt)
            val decoyPinHash = hashPin(decoyPin, decoySalt)

            // Create vaults
            val realVault = VaultEntity(
                id = UUID.randomUUID().toString(),
                name = "My Vault",
                isDecoy = false,
                salt = generateSalt() // Different salt for encryption
            )
            val decoyVault = VaultEntity(
                id = UUID.randomUUID().toString(),
                name = "Personal Files",
                isDecoy = true,
                salt = generateSalt()
            )

            vaultDao.insertVault(realVault)
            vaultDao.insertVault(decoyVault)

            // Store PIN hashes securely using EncryptionManager
            encryptionManager.storeSecureString(KEY_REAL_PIN_HASH, realPinHash)
            encryptionManager.storeSecureString(KEY_REAL_PIN_SALT, bytesToHex(realSalt))
            encryptionManager.storeSecureString(KEY_DECOY_PIN_HASH, decoyPinHash)
            encryptionManager.storeSecureString(KEY_DECOY_PIN_SALT, bytesToHex(decoySalt))
            encryptionManager.storeSecureString(KEY_REAL_VAULT_ID, realVault.id)
            encryptionManager.storeSecureString(KEY_DECOY_VAULT_ID, decoyVault.id)
            encryptionManager.storeSecureBoolean(KEY_PIN_SET, true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify real vault PIN
     */
    fun verifyRealPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        
        val storedHash = encryptionManager.retrieveSecureString(KEY_REAL_PIN_HASH) ?: return false
        val saltHex = encryptionManager.retrieveSecureString(KEY_REAL_PIN_SALT) ?: return false
        val salt = hexToBytes(saltHex)
        
        val computedHash = hashPin(pin, salt)
        return computedHash == storedHash
    }

    /**
     * Verify decoy vault PIN
     */
    fun verifyDecoyPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        
        val storedHash = encryptionManager.retrieveSecureString(KEY_DECOY_PIN_HASH) ?: return false
        val saltHex = encryptionManager.retrieveSecureString(KEY_DECOY_PIN_SALT) ?: return false
        val salt = hexToBytes(saltHex)
        
        val computedHash = hashPin(pin, salt)
        return computedHash == storedHash
    }

    /**
     * Get real vault ID if PIN is correct
     */
    fun getRealVaultId(pin: String): String? {
        return if (verifyRealPin(pin)) {
            encryptionManager.retrieveSecureString(KEY_REAL_VAULT_ID)
        } else null
    }

    /**
     * Get decoy vault ID if PIN is correct
     */
    fun getDecoyVaultId(pin: String): String? {
        return if (verifyDecoyPin(pin)) {
            encryptionManager.retrieveSecureString(KEY_DECOY_VAULT_ID)
        } else null
    }

    /**
     * Change real PIN (requires old PIN)
     */
    suspend fun changeRealPin(oldPin: String, newPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!verifyRealPin(oldPin)) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect old PIN"))
        }
        if (!isValidPin(newPin)) {
            return@withContext Result.failure(IllegalArgumentException("New PIN must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits"))
        }

        val realSalt = generateSalt()
        val realPinHash = hashPin(newPin, realSalt)

        encryptionManager.storeSecureString(KEY_REAL_PIN_HASH, realPinHash)
        encryptionManager.storeSecureString(KEY_REAL_PIN_SALT, bytesToHex(realSalt))

        Result.success(Unit)
    }

    /**
     * Change decoy PIN (requires real PIN for security)
     */
    suspend fun changeDecoyPin(realPin: String, newDecoyPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!verifyRealPin(realPin)) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect real PIN"))
        }
        if (!isValidPin(newDecoyPin)) {
            return@withContext Result.failure(IllegalArgumentException("New decoy PIN must be $MIN_PIN_LENGTH-$MAX_PIN_LENGTH digits"))
        }

        val decoySalt = generateSalt()
        val decoyPinHash = hashPin(newDecoyPin, decoySalt)

        encryptionManager.storeSecureString(KEY_DECOY_PIN_HASH, decoyPinHash)
        encryptionManager.storeSecureString(KEY_DECOY_PIN_SALT, bytesToHex(decoySalt))

        Result.success(Unit)
    }

    /**
     * Hash PIN with salt using PBKDF2 (100,000 iterations)
     */
    private fun hashPin(pin: String, salt: ByteArray): String {
        return encryptionManager.hashPinWithPBKDF2(pin, salt)
    }

    /**
     * Generate random salt
     */
    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Convert bytes to hex string
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert hex string to bytes
     */
    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}
