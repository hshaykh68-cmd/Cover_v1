package com.cover.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cover.app.data.local.entity.HiddenAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {
    
    @Query("SELECT * FROM hidden_apps WHERE vaultId = :vaultId")
    fun getHiddenAppsByVault(vaultId: String): Flow<List<HiddenAppEntity>>
    
    @Query("SELECT packageName FROM hidden_apps WHERE vaultId = :vaultId AND isHidden = 1")
    suspend fun getHiddenPackageNames(vaultId: String): List<String>
    
    @Query("SELECT * FROM hidden_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getHiddenAppByPackage(packageName: String): HiddenAppEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenApp(entity: HiddenAppEntity)
    
    @Query("DELETE FROM hidden_apps WHERE id = :id")
    suspend fun deleteHiddenAppById(id: String)
    
    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun deleteHiddenAppByPackage(packageName: String)
    
    @Query("UPDATE hidden_apps SET isHidden = :isHidden WHERE id = :id")
    suspend fun updateHiddenStatus(id: String, isHidden: Boolean)
    
    @Query("SELECT COUNT(*) FROM hidden_apps WHERE vaultId = :vaultId")
    suspend fun getHiddenAppCount(vaultId: String): Int
    
    @Query("DELETE FROM hidden_apps WHERE vaultId = :vaultId")
    suspend fun clearHiddenAppsForVault(vaultId: String)
}
