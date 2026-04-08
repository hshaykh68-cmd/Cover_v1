package com.cover.app.data.local.dao

import androidx.room.*
import com.cover.app.data.local.entity.IntruderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntruderLogDao {
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntruderLogEntity>>

    @Query("SELECT * FROM intruder_logs WHERE isDecoyVault = 0 ORDER BY timestamp DESC")
    fun getRealVaultLogs(): Flow<List<IntruderLogEntity>>

    @Query("SELECT * FROM intruder_logs WHERE isDecoyVault = 1 ORDER BY timestamp DESC")
    fun getDecoyVaultLogs(): Flow<List<IntruderLogEntity>>

    @Query("SELECT * FROM intruder_logs WHERE id = :id")
    suspend fun getLogById(id: String): IntruderLogEntity?

    @Query("SELECT COUNT(*) FROM intruder_logs")
    suspend fun getTotalLogCount(): Int

    @Query("SELECT COUNT(*) FROM intruder_logs WHERE isDecoyVault = :isDecoy")
    suspend fun getLogCountByVaultType(isDecoy: Boolean): Int

    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<IntruderLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IntruderLogEntity)

    @Delete
    suspend fun deleteLog(log: IntruderLogEntity)

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteLogById(id: String)

    @Query("DELETE FROM intruder_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteLogsBefore(beforeTimestamp: Long)

    @Query("DELETE FROM intruder_logs")
    suspend fun deleteAllLogs()

    @Query("UPDATE intruder_logs SET isUploaded = 1 WHERE id = :id")
    suspend fun markAsUploaded(id: String)

    @Query("SELECT * FROM intruder_logs WHERE isUploaded = 0 ORDER BY timestamp DESC")
    suspend fun getUnuploadedLogs(): List<IntruderLogEntity>
}
