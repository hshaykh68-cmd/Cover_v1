package com.cover.app.data.repository

import com.cover.app.data.local.dao.HiddenAppDao
import com.cover.app.data.local.dao.HiddenItemDao
import com.cover.app.data.local.dao.IntruderLogDao
import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.local.entity.HiddenAppEntity
import com.cover.app.data.local.entity.HiddenItemEntity
import com.cover.app.data.local.entity.IntruderLogEntity
import com.cover.app.data.local.entity.VaultEntity
import com.cover.app.data.security.EncryptionManager
import com.cover.app.data.storage.SecureStorageManager
import com.cover.app.domain.model.HiddenItem
import com.cover.app.domain.model.IntruderLog
import com.cover.app.domain.model.Vault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    private val vaultDao: VaultDao,
    private val hiddenItemDao: HiddenItemDao,
    private val intruderLogDao: IntruderLogDao,
    private val hiddenAppDao: HiddenAppDao,
    private val secureStorage: SecureStorageManager,
    private val encryptionManager: EncryptionManager
) {
    fun getAllVaults(): Flow<List<Vault>> {
        return vaultDao.getAllVaults().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getRealVaults(): Flow<List<Vault>> {
        return vaultDao.getRealVaults().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun createVault(name: String, isDecoy: Boolean): Vault {
        val salt = encryptionManager.generateSalt()
        val entity = VaultEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isDecoy = isDecoy,
            salt = salt
        )
        vaultDao.insertVault(entity)
        return entity.toDomainModel()
    }

    suspend fun getVaultById(id: String): Vault? {
        return vaultDao.getVaultById(id)?.toDomainModel()
    }

    suspend fun getVaultSalt(vaultId: String): ByteArray? {
        return vaultDao.getVaultById(vaultId)?.salt
    }

    suspend fun deleteVault(vaultId: String) {
        // Delete all items first using one-shot query (not Flow)
        val items = hiddenItemDao.getItemsByVaultOnce(vaultId)
        items.forEach { item ->
            secureStorage.deleteFile(item.encryptedPath)
        }
        // Delete all items from database
        hiddenItemDao.deleteItemsByVault(vaultId)
        // Delete the vault
        vaultDao.deleteVaultById(vaultId)
    }

    // Hidden Items
    fun getItemsByVault(vaultId: String): Flow<List<HiddenItem>> {
        return hiddenItemDao.getItemsByVault(vaultId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun addItemToVault(
        vaultId: String,
        sourcePath: String,
        pin: String,
        type: HiddenItemEntity.ItemType
    ): Result<String> {
        return try {
            val vault = vaultDao.getVaultById(vaultId)
                ?: return Result.failure(IllegalStateException("Vault not found"))

            val fileId = secureStorage.storeFile(sourcePath, pin, vault.salt)
            val size = secureStorage.getFileSize(fileId)

            val entity = HiddenItemEntity(
                id = UUID.randomUUID().toString(),
                vaultId = vaultId,
                encryptedPath = fileId,
                originalName = sourcePath.substringAfterLast("/"),
                type = type,
                size = size,
                thumbnailEncryptedPath = null
            )

            hiddenItemDao.insertItem(entity)
            Result.success(entity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeItemFromVault(itemId: String, vaultId: String) {
        val item = hiddenItemDao.getItemById(itemId) ?: return
        secureStorage.deleteFile(item.encryptedPath)
        hiddenItemDao.deleteItemById(itemId)
    }

    suspend fun getTotalItemCount(): Int {
        return hiddenItemDao.getTotalItemCount()
    }

    // Intruder Logs
    suspend fun logIntruderAttempt(
        attemptedPin: String?,
        isDecoyVault: Boolean,
        photoBytes: ByteArray? = null,
        location: com.cover.app.data.intruder.IntruderLog.Location? = null
    ): String {
        var photoId: String? = null
        
        // Save photo if captured
        photoBytes?.let { bytes ->
            photoId = secureStorage.storeIntruderPhoto(bytes)
        }
        
        val entity = IntruderLogEntity(
            id = UUID.randomUUID().toString(),
            attemptedPin = null, // Don't store PIN for security
            isDecoyVault = isDecoyVault,
            photoEncryptedPath = photoId,
            locationLat = location?.latitude,
            locationLng = location?.longitude
        )
        intruderLogDao.insertLog(entity)
        return entity.id
    }

    fun getIntruderLogs(): Flow<List<IntruderLog>> {
        return intruderLogDao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun deleteIntruderLog(logId: String) {
        val log = intruderLogDao.getLogById(logId)
        log?.photoEncryptedPath?.let { photoId ->
            secureStorage.deleteIntruderPhoto(photoId)
        }
        intruderLogDao.deleteLogById(logId)
    }

    suspend fun exportItem(
        item: HiddenItem,
        pin: String,
        vaultSalt: ByteArray,
        destinationUri: android.net.Uri
    ): Result<Unit> {
        return try {
            // Decrypt file
            val decryptedBytes = secureStorage.retrieveFile(item.encryptedPath, pin, vaultSalt)
                ?: return Result.failure(IllegalStateException("Failed to decrypt file"))

            // Write to destination (ContentResolver)
            val context = secureStorage.getContext()
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                output.write(decryptedBytes)
            } ?: return Result.failure(IllegalStateException("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllIntruderLogs() {
        // Delete all photos first
        val logs = intruderLogDao.getRecentLogs(1000)
        logs.forEach { log ->
            log.photoEncryptedPath?.let { photoId ->
                secureStorage.deleteIntruderPhoto(photoId)
            }
        }
        intruderLogDao.deleteAllLogs()
    }

    // Hidden Apps (Launcher integration)
    fun getHiddenAppsByVault(vaultId: String): Flow<List<HiddenAppEntity>> {
        return hiddenAppDao.getHiddenAppsByVault(vaultId)
    }

    suspend fun getHiddenPackageNames(vaultId: String): List<String> {
        return hiddenAppDao.getHiddenPackageNames(vaultId)
    }

    suspend fun hideApp(vaultId: String, packageName: String, appName: String): Result<Unit> {
        return try {
            // Check if already hidden
            val existing = hiddenAppDao.getHiddenAppByPackage(packageName)
            if (existing != null) {
                return Result.success(Unit) // Already hidden
            }

            val entity = HiddenAppEntity(
                id = UUID.randomUUID().toString(),
                vaultId = vaultId,
                packageName = packageName,
                appName = appName,
                isHidden = true
            )
            hiddenAppDao.insertHiddenApp(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unhideApp(packageName: String) {
        hiddenAppDao.deleteHiddenAppByPackage(packageName)
    }

    suspend fun unhideAppById(appId: String) {
        hiddenAppDao.deleteHiddenAppById(appId)
    }

    suspend fun getHiddenAppCount(vaultId: String): Int {
        return hiddenAppDao.getHiddenAppCount(vaultId)
    }

    suspend fun clearHiddenAppsForVault(vaultId: String) {
        hiddenAppDao.clearHiddenAppsForVault(vaultId)
    }

    private fun VaultEntity.toDomainModel(): Vault {
        return Vault(
            id = this.id,
            name = this.name,
            isDecoy = this.isDecoy,
            createdAt = this.createdAt,
            lastAccessedAt = this.lastAccessedAt
        )
    }

    private fun HiddenItemEntity.toDomainModel(): HiddenItem {
        return when (this.type) {
            HiddenItemEntity.ItemType.PHOTO -> HiddenItem.Photo(
                id = this.id,
                vaultId = this.vaultId,
                originalName = this.originalName,
                size = this.size,
                thumbnailId = this.thumbnailEncryptedPath,
                encryptedPath = this.encryptedPath,
                createdAt = this.createdAt
            )
            HiddenItemEntity.ItemType.VIDEO -> HiddenItem.Video(
                id = this.id,
                vaultId = this.vaultId,
                originalName = this.originalName,
                size = this.size,
                thumbnailId = this.thumbnailEncryptedPath,
                encryptedPath = this.encryptedPath,
                createdAt = this.createdAt
            )
            HiddenItemEntity.ItemType.AUDIO -> HiddenItem.Audio(
                id = this.id,
                vaultId = this.vaultId,
                originalName = this.originalName,
                size = this.size,
                encryptedPath = this.encryptedPath,
                createdAt = this.createdAt
            )
            HiddenItemEntity.ItemType.DOCUMENT, HiddenItemEntity.ItemType.FILE -> HiddenItem.Document(
                id = this.id,
                vaultId = this.vaultId,
                originalName = this.originalName,
                size = this.size,
                mimeType = null,
                encryptedPath = this.encryptedPath,
                createdAt = this.createdAt
            )
            HiddenItemEntity.ItemType.APP -> HiddenItem.App(
                id = this.id,
                vaultId = this.vaultId,
                packageName = this.originalName,
                appName = this.originalName
            )
        }
    }

    private fun IntruderLogEntity.toDomainModel(): IntruderLog {
        return IntruderLog(
            id = this.id,
            timestamp = this.timestamp,
            photoId = this.photoEncryptedPath,
            location = if (this.locationLat != null && this.locationLng != null) {
                IntruderLog.Location(this.locationLat, this.locationLng)
            } else null,
            attemptedPinHash = this.attemptedPin,
            isDecoyVault = this.isDecoyVault
        )
    }
}
