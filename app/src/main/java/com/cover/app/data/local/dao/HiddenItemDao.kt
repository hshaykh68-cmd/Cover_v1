package com.cover.app.data.local.dao

import androidx.room.*
import com.cover.app.data.local.entity.HiddenItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenItemDao {
    @Query("SELECT * FROM hidden_items WHERE vaultId = :vaultId ORDER BY createdAt DESC")
    fun getItemsByVault(vaultId: String): Flow<List<HiddenItemEntity>>

    @Query("SELECT * FROM hidden_items WHERE vaultId = :vaultId ORDER BY createdAt DESC")
    suspend fun getItemsByVaultOnce(vaultId: String): List<HiddenItemEntity>

    @Query("SELECT * FROM hidden_items WHERE vaultId = :vaultId AND type = :type ORDER BY createdAt DESC")
    fun getItemsByVaultAndType(vaultId: String, type: HiddenItemEntity.ItemType): Flow<List<HiddenItemEntity>>

    @Query("SELECT * FROM hidden_items WHERE id = :id")
    suspend fun getItemById(id: String): HiddenItemEntity?

    @Query("SELECT COUNT(*) FROM hidden_items WHERE vaultId = :vaultId")
    suspend fun getItemCountByVault(vaultId: String): Int

    @Query("SELECT COUNT(*) FROM hidden_items")
    suspend fun getTotalItemCount(): Int

    @Query("SELECT SUM(size) FROM hidden_items WHERE vaultId = :vaultId")
    suspend fun getVaultSize(vaultId: String): Long?

    @Query("SELECT SUM(size) FROM hidden_items")
    suspend fun getTotalSize(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HiddenItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HiddenItemEntity>)

    @Delete
    suspend fun deleteItem(item: HiddenItemEntity)

    @Query("DELETE FROM hidden_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM hidden_items WHERE vaultId = :vaultId")
    suspend fun deleteItemsByVault(vaultId: String)

    @Query("DELETE FROM hidden_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM hidden_items WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: HiddenItemEntity.ItemType): Flow<List<HiddenItemEntity>>
    
    @Query("SELECT * FROM hidden_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<HiddenItemEntity>>
}
