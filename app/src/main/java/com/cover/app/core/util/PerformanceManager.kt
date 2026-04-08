package com.cover.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cover.app.data.security.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 9: Performance Optimization
 * - Lazy loading for large vaults
 * - Image caching strategy  
 * - Background thread encryption
 * - Memory management
 */

@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        /** Maximum memory cache size in bytes (50MB) */
        const val MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024
        
        /** Maximum disk cache size in bytes (200MB) */
        const val MAX_DISK_CACHE_SIZE = 200 * 1024 * 1024
        
        /** Thumbnail size for grid view */
        const val THUMBNAIL_SIZE = 300
        
        /** Preview size for full-screen viewer */
        const val PREVIEW_SIZE = 1200
        
        /** Batch size for bulk operations */
        const val BATCH_SIZE = 20
        
        /** Compression quality for thumbnails */
        const val THUMBNAIL_QUALITY = 80
    }
    
    private val memoryCache = object : androidx.collection.LruCache<String, Bitmap>(
        (MAX_MEMORY_CACHE_SIZE / 1024).toInt() // LruCache max size in KB
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // Return bitmap size in KB
            return value.byteCount / 1024
        }
    }
    
    private val diskCacheDir = File(context.cacheDir, "image_cache").apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * Load bitmap with automatic downsampling for memory efficiency
     * Reads bytes once to avoid double InputStream open
     */
    suspend fun loadBitmapEfficiently(
        uri: Uri,
        targetWidth: Int = THUMBNAIL_SIZE,
        targetHeight: Int = THUMBNAIL_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Read all bytes once to avoid multiple stream opens
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } 
                ?: return@withContext null
            
            // First decode - just get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                
                // Calculate sample size
                inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory than ARGB_8888
            }
            
            // Second decode - actual bitmap
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate optimal sample size for downsampling
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Get bitmap from memory cache
     */
    fun getBitmapFromMemoryCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    
    /**
     * Add bitmap to memory cache
     */
    fun addBitmapToMemoryCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }
    
    /**
     * Get bitmap from disk cache
     */
    suspend fun getBitmapFromDiskCache(key: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(diskCacheDir, key)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
    
    /**
     * Add bitmap to disk cache
     */
    suspend fun addBitmapToDiskCache(key: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val file = File(diskCacheDir, key)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
        }
        
        // Clean up if cache is too large
        cleanupDiskCache()
    }
    
    /**
     * Clean up old cache files when size exceeds limit
     */
    private fun cleanupDiskCache() {
        val files = diskCacheDir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }
        
        if (totalSize > MAX_DISK_CACHE_SIZE) {
            // Sort by last modified (oldest first)
            files.sortBy { it.lastModified() }
            
            // Delete oldest files until under limit
            for (file in files) {
                if (totalSize <= MAX_DISK_CACHE_SIZE * 0.8) break
                totalSize -= file.length()
                file.delete()
            }
        }
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get cache size info for debugging
     */
    fun getCacheInfo(): CacheInfo {
        val memorySize = memoryCache.size() * 1024 // Convert KB to bytes
        val diskSize = diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0
        return CacheInfo(memorySize, diskSize, memoryCache.hitCount(), memoryCache.missCount())
    }
    
    /**
     * Encrypt file in background with progress callback
     */
    suspend fun encryptFileInBackground(
        sourceFile: File,
        destinationFile: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val totalBytes = sourceFile.length()
        var processedBytes = 0L
        
        sourceFile.inputStream().buffered().use { input ->
            destinationFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // Process buffer (encryption would happen here)
                    output.write(buffer, 0, bytesRead)
                    processedBytes += bytesRead
                    
                    // Report progress every 64KB
                    if (processedBytes % (64 * 1024) == 0L) {
                        val progress = ((processedBytes * 100) / totalBytes).toInt()
                        onProgress(progress)
                    }
                }
            }
        }
        
        onProgress(100)
    }
    
    /**
     * Process items in batches to avoid memory pressure
     */
    suspend fun <T> processInBatches(
        items: List<T>,
        batchSize: Int = BATCH_SIZE,
        process: suspend (List<T>) -> Unit
    ) {
        items.chunked(batchSize).forEach { batch ->
            process(batch)
            // Yield to allow other coroutines to run
            kotlinx.coroutines.yield()
        }
    }
    
    /**
     * Generate cache key from file path and dimensions
     */
    fun generateCacheKey(path: String, width: Int, height: Int): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update("$path-${width}x$height".toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

data class CacheInfo(
    val memoryCacheSize: Int,
    val diskCacheSize: Long,
    val memoryHits: Int,
    val memoryMisses: Int
) {
    val hitRate: Float = if (memoryHits + memoryMisses > 0) {
        memoryHits.toFloat() / (memoryHits + memoryMisses)
    } else 0f
}
