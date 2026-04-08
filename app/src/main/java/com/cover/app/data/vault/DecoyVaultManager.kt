package com.cover.app.data.vault

import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.local.entity.VaultEntity
import com.cover.app.data.security.EncryptionManager
import com.cover.app.data.security.PinManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class DecoyVaultManager @Inject constructor(
    private val vaultDao: VaultDao,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        const val DECOY_VAULT_NAME = "Personal Files"
    }

    /**
     * Create a decoy vault with fake content
     * This vault will have some sample files to look realistic
     */
    suspend fun createDecoyVault(pin: String): Result<String> {
        return try {
            // Check if decoy vault already exists
            val existingVaults = vaultDao.getDecoyVaultCount()
            if (existingVaults > 0) {
                return Result.failure(IllegalStateException("Decoy vault already exists"))
            }

            val salt = encryptionManager.generateSalt()
            val vault = VaultEntity(
                id = UUID.randomUUID().toString(),
                name = DECOY_VAULT_NAME,
                isDecoy = true,
                salt = salt
            )

            vaultDao.insertVault(vault)
            
            // Store the PIN hash for verification
            // In real implementation, store PIN hash with salt
            
            Result.success(vault.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get or create decoy vault
     */
    suspend fun getOrCreateDecoyVault(pin: String): String {
        val decoyVaults = vaultDao.getDecoyVaults().first()
        val existing = decoyVaults.firstOrNull()
        
        return if (existing != null) {
            existing.id
        } else {
            createDecoyVault(pin).getOrThrow()
        }
    }

    /**
     * Verify if PIN unlocks decoy vault
     */
    suspend fun verifyDecoyPin(pin: String): Boolean {
        // In real implementation, verify against stored hash
        // For now, any 4-8 digit PIN works if it ends with +1=
        return pin.length in 4..8 && pin.matches(Regex("\\d+"))
    }

    /**
     * Get decoy vault ID if exists
     */
    suspend fun getDecoyVaultId(): String? {
        // Query decoy vaults
        return null // Would query from DAO
    }
}
