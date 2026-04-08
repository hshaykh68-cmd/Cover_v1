package com.cover.app.data.local.dao

import androidx.room.*
import com.cover.app.data.local.entity.PremiumStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PremiumStatusDao {
    @Query("SELECT * FROM premium_status WHERE id = 'current_user'")
    fun getPremiumStatus(): Flow<PremiumStatusEntity?>

    @Query("SELECT * FROM premium_status WHERE id = 'current_user'")
    suspend fun getPremiumStatusSync(): PremiumStatusEntity?

    @Query("SELECT isPremium FROM premium_status WHERE id = 'current_user'")
    fun isPremium(): Flow<Boolean>

    @Query("SELECT isPremium FROM premium_status WHERE id = 'current_user'")
    suspend fun isPremiumSync(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: PremiumStatusEntity)

    @Query("UPDATE premium_status SET isPremium = :isPremium, lastVerifiedAt = :timestamp WHERE id = 'current_user'")
    suspend fun updatePremiumStatus(isPremium: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM premium_status")
    suspend fun clearAll()
}
