package com.cover.app.data.local.dao

import androidx.room.*
import com.cover.app.data.local.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vaults ORDER BY createdAt DESC")
    fun getAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE isDecoy = 0 ORDER BY createdAt DESC")
    fun getRealVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE isDecoy = 1 ORDER BY createdAt DESC")
    fun getDecoyVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getVaultById(id: String): VaultEntity?

    @Query("SELECT COUNT(*) FROM vaults WHERE isDecoy = 0")
    suspend fun getRealVaultCount(): Int

    @Query("SELECT COUNT(*) FROM vaults WHERE isDecoy = 1")
    suspend fun getDecoyVaultCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVault(vault: VaultEntity)

    @Update
    suspend fun updateVault(vault: VaultEntity)

    @Query("UPDATE vaults SET lastAccessedAt = :timestamp WHERE id = :vaultId")
    suspend fun updateLastAccessed(vaultId: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteVault(vault: VaultEntity)

    @Query("DELETE FROM vaults WHERE id = :id")
    suspend fun deleteVaultById(id: String)

    @Query("DELETE FROM vaults")
    suspend fun deleteAllVaults()
}
