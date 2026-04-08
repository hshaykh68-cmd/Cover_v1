package com.cover.app.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import com.cover.app.data.security.EncryptionManager
import com.cover.app.data.storage.SecureStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance thumbnail cache system
 * - LRU in-memory cache for fast access
 * - Encrypted disk storage for persistence
 * - Async generation on background thread
 * - Size-limited to prevent memory issues
 */
@Singleton
class ThumbnailCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorageManager,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 50 // Max 50 thumbnails in memory
        private const val THUMBNAIL_WIDTH = 300
        private const val THUMBNAIL_HEIGHT = 300
        private const val THUMBNAIL_QUALITY = 80
    }

    // LRU memory cache using LinkedHashMap
    private val memoryCache = object : LinkedHashMap<String, Bitmap>(
        MAX_MEMORY_CACHE_SIZE,
        0.75f,
        true // accessOrder = true for LRU
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>?): Boolean {
            return size > MAX_MEMORY_CACHE_SIZE
        }
    }

    // Thread-safe set for tracking in-progress generations
    private val inProgressGenerations = ConcurrentHashMap.newKeySet<String>()

    /**
     * Get thumbnail synchronously from memory cache (instant)
     * Returns null if not cached - use loadThumbnailAsync for async loading
     */
    fun getThumbnailFromMemory(itemId: String): Bitmap? {
        return synchronized(memoryCache) {
            memoryCache[itemId]
        }
    }

    /**
     * Load thumbnail asynchronously with encryption/decryption
     * This is the primary method for gallery grid - returns memory cache immediately,
     * then loads from disk if needed
     */
    suspend fun loadThumbnail(
        itemId: String,
        encryptedFileId: String,
        pin: String,
        salt: ByteArray,
        isVideo: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Check memory cache first (fastest)
        getThumbnailFromMemory(itemId)?.let { return@withContext it }

        // 2. Check encrypted disk storage
        val thumbnailId = "thumb_$itemId"
        val cachedBitmap = secureStorage.retrieveThumbnail(thumbnailId, pin, salt)
        if (cachedBitmap != null) {
            // Store in memory cache for next access
            putInMemoryCache(itemId, cachedBitmap)
            return@withContext cachedBitmap
        }

        // 3. Generate new thumbnail if not in progress
        if (inProgressGenerations.add(itemId)) {
            try {
                val decryptedBytes = secureStorage.retrieveFile(encryptedFileId, pin, salt)
                val bitmap = generateThumbnail(decryptedBytes, isVideo)
                
                // Cache in memory
                bitmap?.let { putInMemoryCache(itemId, it) }
                
                // Store encrypted thumbnail for persistence
                bitmap?.let {
                    val outputStream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
                    val thumbBytes = outputStream.toByteArray()
                    
                    // Store encrypted thumbnail
                    secureStorage.storeBytes(thumbBytes, pin, salt)
                }
                
                return@withContext bitmap
            } finally {
                inProgressGenerations.remove(itemId)
            }
        }

        // Generation in progress by another caller
        return@withContext null
    }

    /**
     * Pre-generate thumbnails for a batch of items in background
     * Use this when importing to have thumbnails ready
     */
    suspend fun pregenerateThumbnails(
        items: List<ThumbnailRequest>,
        pin: String,
        salt: ByteArray
    ) = withContext(Dispatchers.IO) {
        items.forEach { request ->
            try {
                loadThumbnail(
                    itemId = request.itemId,
                    encryptedFileId = request.encryptedFileId,
                    pin = pin,
                    salt = salt,
                    isVideo = request.isVideo
                )
            } catch (e: Exception) {
                // Silently fail for individual items, continue with others
            }
        }
    }

    /**
     * Generate thumbnail from decrypted file bytes
     */
    private fun generateThumbnail(fileBytes: ByteArray, isVideo: Boolean): Bitmap? {
        return if (isVideo) {
            generateVideoThumbnail(fileBytes)
        } else {
            generateImageThumbnail(fileBytes)
        }
    }

    private fun generateImageThumbnail(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        // Calculate sample size
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > THUMBNAIL_HEIGHT || width > THUMBNAIL_WIDTH) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= THUMBNAIL_HEIGHT &&
                (halfWidth / inSampleSize) >= THUMBNAIL_WIDTH
            ) {
                inSampleSize *= 2
            }
        }

        options.apply {
            inJustDecodeBounds = false
            this.inSampleSize = inSampleSize
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun generateVideoThumbnail(bytes: ByteArray): Bitmap? {
        // For videos, we need to write to temp file first
        val tempFile = java.io.File.createTempFile("video_thumb", ".mp4")
        tempFile.writeBytes(bytes)
        
        return try {
            ThumbnailUtils.createVideoThumbnail(
                tempFile.absolutePath,
                MediaStore.Images.Thumbnails.MINI_KIND
            )
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Put bitmap in memory cache (thread-safe)
     */
    private fun putInMemoryCache(itemId: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            memoryCache[itemId] = bitmap
        }
    }

    /**
     * Clear memory cache (call on low memory)
     */
    fun clearMemoryCache() {
        synchronized(memoryCache) {
            memoryCache.clear()
        }
    }

    /**
     * Get current memory cache size
     */
    fun getMemoryCacheSize(): Int {
        return synchronized(memoryCache) { memoryCache.size }
    }

    /**
     * Preloading request data class
     */
    data class ThumbnailRequest(
        val itemId: String,
        val encryptedFileId: String,
        val isVideo: Boolean
    )
}
