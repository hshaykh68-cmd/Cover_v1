package com.cover.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cover.app.data.security.EncryptedData
import com.cover.app.data.security.EncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val VAULT_DIRECTORY = "vault"
        private const val THUMBNAIL_DIRECTORY = "thumbnails"
        private const val TEMP_DIRECTORY = "temp"
        private const val INTRUDER_DIRECTORY = "intruder"
        private const val SECURE_DELETE_PASSES = 3
        private const val THUMBNAIL_SIZE = 300
    }

    fun getContext(): Context = context

    private val vaultDir by lazy { File(context.filesDir, VAULT_DIRECTORY).apply { mkdirs() } }
    private val thumbnailDir by lazy { File(context.filesDir, THUMBNAIL_DIRECTORY).apply { mkdirs() } }
    private val tempDir by lazy { File(context.cacheDir, TEMP_DIRECTORY).apply { mkdirs() } }
    private val intruderDir by lazy { File(context.filesDir, INTRUDER_DIRECTORY).apply { mkdirs() } }

    /**
     * Store a file securely in the vault
     * @param sourcePath Original file path
     * @param pin PIN for encryption
     * @param salt Salt for key derivation
     * @return Encrypted file identifier (random UUID)
     */
    suspend fun storeFile(
        sourcePath: String,
        pin: String,
        salt: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Source file does not exist: $sourcePath")
        }

        // Generate unique identifier
        val fileId = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, fileId)

        // Read and encrypt file content
        val fileBytes = sourceFile.readBytes()
        val encryptedData = encryptionManager.encryptWithPin(fileBytes, pin, salt)

        // Write encrypted data
        encryptedFile.writeBytes(encryptedData.toByteArray())

        // Securely delete original file
        secureDelete(sourceFile)

        fileId
    }

    /**
     * Store a bitmap (image) securely
     */
    suspend fun storeImage(
        bitmap: Bitmap,
        pin: String,
        salt: ByteArray,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): String = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, fileId)

        // Compress bitmap to bytes
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        val imageBytes = outputStream.toByteArray()

        // Encrypt and store
        val encryptedData = encryptionManager.encryptWithPin(imageBytes, pin, salt)
        encryptedFile.writeBytes(encryptedData.toByteArray())

        fileId
    }

    /**
     * Store any byte array securely
     */
    suspend fun storeBytes(
        bytes: ByteArray,
        pin: String,
        salt: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, fileId)

        val encryptedData = encryptionManager.encryptWithPin(bytes, pin, salt)
        encryptedFile.writeBytes(encryptedData.toByteArray())

        fileId
    }

    /**
     * Retrieve and decrypt a file
     * Returns decrypted bytes (never writes unencrypted to disk)
     */
    suspend fun retrieveFile(
        fileId: String,
        pin: String,
        salt: ByteArray
    ): ByteArray = withContext(Dispatchers.IO) {
        val encryptedFile = File(vaultDir, fileId)
        if (!encryptedFile.exists()) {
            throw IllegalArgumentException("File not found: $fileId")
        }

        val encryptedBytes = encryptedFile.readBytes()
        val encryptedData = EncryptedData.fromByteArray(encryptedBytes)

        encryptionManager.decryptWithPin(encryptedData, pin, salt)
    }

    /**
     * Retrieve and decrypt an image as Bitmap
     */
    suspend fun retrieveImage(
        fileId: String,
        pin: String,
        salt: ByteArray
    ): Bitmap = withContext(Dispatchers.IO) {
        val decryptedBytes = retrieveFile(fileId, pin, salt)
        BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
            ?: throw IllegalStateException("Failed to decode image")
    }

    /**
     * Generate and store encrypted thumbnail for an image
     */
    suspend fun generateAndStoreThumbnail(
        sourceImageId: String,
        pin: String,
        salt: ByteArray
    ): String = withContext(Dispatchers.IO) {
        // Decrypt source image
        val sourceBitmap = retrieveImage(sourceImageId, pin, salt)

        // Generate thumbnail
        val thumbnail = createThumbnail(sourceBitmap)

        // Store thumbnail with same encryption
        val thumbnailId = "thumb_$sourceImageId"
        val thumbnailFile = File(thumbnailDir, thumbnailId)

        val outputStream = java.io.ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val thumbnailBytes = outputStream.toByteArray()

        val encryptedData = encryptionManager.encryptWithPin(thumbnailBytes, pin, salt)
        thumbnailFile.writeBytes(encryptedData.toByteArray())

        thumbnailId
    }

    /**
     * Retrieve decrypted thumbnail
     */
    suspend fun retrieveThumbnail(
        thumbnailId: String,
        pin: String,
        salt: ByteArray
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailDir, thumbnailId)
            if (!thumbnailFile.exists()) return@withContext null

            val encryptedBytes = thumbnailFile.readBytes()
            val encryptedData = EncryptedData.fromByteArray(encryptedBytes)
            val decryptedBytes = encryptionManager.decryptWithPin(encryptedData, pin, salt)

            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete a file from secure storage
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(vaultDir, fileId)
        val thumbnail = File(thumbnailDir, "thumb_$fileId")

        val fileDeleted = if (file.exists()) secureDelete(file) else true
        val thumbDeleted = if (thumbnail.exists()) secureDelete(thumbnail) else true

        fileDeleted && thumbDeleted
    }

    /**
     * Store intruder photo
     */
    suspend fun storeIntruderPhoto(photoBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val photoId = "intruder_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val photoFile = File(intruderDir, photoId)

        // Use master key for intruder photos (no PIN required to view)
        val encryptedData = encryptionManager.encrypt(photoBytes)
        photoFile.writeBytes(encryptedData.toByteArray())

        photoId
    }

    /**
     * Retrieve intruder photo
     */
    suspend fun retrieveIntruderPhoto(photoId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(intruderDir, photoId)
            if (!photoFile.exists()) return@withContext null

            val encryptedBytes = photoFile.readBytes()
            val encryptedData = EncryptedData.fromByteArray(encryptedBytes)
            val decryptedBytes = encryptionManager.decrypt(encryptedData)

            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete intruder photo
     */
    suspend fun deleteIntruderPhoto(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(intruderDir, photoId)
        if (file.exists()) secureDelete(file) else true
    }

    /**
     * Get file size in bytes
     */
    suspend fun getFileSize(fileId: String): Long = withContext(Dispatchers.IO) {
        val file = File(vaultDir, fileId)
        if (file.exists()) file.length() else 0L
    }

    /**
     * Calculate SHA-256 hash of decrypted content
     */
    suspend fun calculateHash(
        fileId: String,
        pin: String,
        salt: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val bytes = retrieveFile(fileId, pin, salt)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Secure delete - overwrite file multiple times before deletion
     */
    private fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true

        try {
            val length = file.length()
            
            // Overwrite with random data multiple times
            repeat(SECURE_DELETE_PASSES) { pass ->
                FileOutputStream(file).use { fos ->
                    val random = java.security.SecureRandom()
                    val buffer = ByteArray(8192)
                    var remaining = length
                    
                    while (remaining > 0) {
                        random.nextBytes(buffer)
                        val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                        fos.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    fos.flush()
                }
            }

            // Finally delete the file
            return file.delete()
        } catch (e: Exception) {
            // If secure delete fails, try normal delete
            return file.delete()
        }
    }

    /**
     * Create thumbnail from bitmap
     */
    private fun createThumbnail(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        
        val scale = if (width > height) {
            THUMBNAIL_SIZE.toFloat() / width
        } else {
            THUMBNAIL_SIZE.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * Get total storage used by vault
     */
    fun getVaultStorageSize(): Long {
        return vaultDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Get total storage used by thumbnails
     */
    fun getThumbnailStorageSize(): Long {
        return thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Clear all vault data (emergency wipe)
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        var success = true
        
        vaultDir.listFiles()?.forEach { file ->
            if (!secureDelete(file)) success = false
        }
        
        thumbnailDir.listFiles()?.forEach { file ->
            if (!secureDelete(file)) success = false
        }
        
        intruderDir.listFiles()?.forEach { file ->
            if (!secureDelete(file)) success = false
        }
        
        success
    }

    /**
     * Check if file exists in vault
     */
    fun fileExists(fileId: String): Boolean {
        return File(vaultDir, fileId).exists()
    }
}
