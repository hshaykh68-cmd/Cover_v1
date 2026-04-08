package com.cover.app.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media scanner utilities for removing hidden files from system gallery
 * and managing MediaStore entries
 */
@Singleton
class MediaScannerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Remove file from MediaStore (hides from gallery apps)
     */
    suspend fun removeFromMediaStore(filePath: String) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        
        // Query for the file in MediaStore
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(filePath)
        
        val uri = when {
            isImage(filePath) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo(filePath) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            isAudio(filePath) -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> return@withContext
        }
        
        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val deleteUri = Uri.withAppendedPath(uri, id.toString())
                contentResolver.delete(deleteUri, null, null)
            }
        }
    }

    /**
     * Add file back to MediaStore (when exporting from vault)
     */
    suspend fun addToMediaStore(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext

        val contentResolver = context.contentResolver
        
        // Scan the file to add it back to MediaStore
        val values = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, filePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(filePath))
            put(MediaStore.MediaColumns.SIZE, file.length())
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
        }

        val uri = when {
            isImage(filePath) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo(filePath) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            isAudio(filePath) -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> return@withContext
        }

        contentResolver.insert(uri, values)
    }

    /**
     * Scan directory to trigger MediaStore update
     */
    suspend fun scanDirectory(directory: File) = withContext(Dispatchers.IO) {
        // Use MediaScannerConnection to scan files
        val paths = directory.listFiles()?.map { it.absolutePath }?.toTypedArray() ?: return@withContext
        
        val semaphore = java.util.concurrent.Semaphore(0)
        android.media.MediaScannerConnection.scanFile(
            context,
            paths,
            null
        ) { _, _ ->
            semaphore.release()
        }
        
        // Wait for scan to complete with timeout
        semaphore.tryAcquire(5, java.util.concurrent.TimeUnit.SECONDS)
    }

    /**
     * Get real file path from content URI
     */
    suspend fun getFilePathFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                return@withContext cursor.getString(columnIndex)
            }
        }
        
        // Try to get path from document URI
        if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
            val docId = android.provider.DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            if (split.size >= 2) {
                val type = split[0]
                val path = split[1]
                return@withContext when (type) {
                    "primary" -> "${android.os.Environment.getExternalStorageDirectory()}/$path"
                    else -> null
                }
            }
        }
        
        null
    }

    private fun isImage(path: String): Boolean {
        val extensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        return extensions.any { path.lowercase().endsWith(".$it") }
    }

    private fun isVideo(path: String): Boolean {
        val extensions = listOf("mp4", "avi", "mkv", "mov", "webm", "flv", "wmv")
        return extensions.any { path.lowercase().endsWith(".$it") }
    }

    private fun isAudio(path: String): Boolean {
        val extensions = listOf("mp3", "wav", "aac", "flac", "ogg", "m4a", "wma")
        return extensions.any { path.lowercase().endsWith(".$it") }
    }

    private fun getMimeType(path: String): String {
        return when {
            isImage(path) -> "image/jpeg"
            isVideo(path) -> "video/mp4"
            isAudio(path) -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }
}
