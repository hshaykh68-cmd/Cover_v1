package com.cover.app.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.cover.app.data.local.entity.HiddenItemEntity
import com.cover.app.data.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportMediaUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository
) {
    /**
     * Import media files (photos/videos) to vault with real-time progress
     */
    operator fun invoke(
        vaultId: String,
        pin: String,
        uris: List<Uri>
    ): Flow<ImportProgress> = flow {
        val total = uris.size
        var completed = 0
        var failed = 0
        
        emit(ImportProgress.Started(total))
        
        val contentResolver = context.contentResolver
        
        uris.chunked(IMPORT_BATCH_SIZE).forEach { batch ->
            batch.forEach { uri ->
                try {
                    importSingleMedia(contentResolver, vaultId, pin, uri)
                    completed++
                } catch (e: Exception) {
                    failed++
                }
                emit(ImportProgress.InProgress(completed, total, failed))
            }
        }
        
        emit(ImportProgress.Completed(completed, failed))
    }
    
    private suspend fun importSingleMedia(
        contentResolver: ContentResolver,
        vaultId: String,
        pin: String,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        // Get file path from URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE)
        val cursor = contentResolver.query(uri, projection, null, null, null)
            ?: return@withContext Result.failure(IllegalArgumentException("Cannot query URI"))
        
        val path = cursor.use {
            if (it.moveToFirst()) {
                val pathIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                it.getString(pathIndex)
            } else null
        } ?: return@withContext Result.failure(IllegalArgumentException("Cannot get file path"))
        
        // Determine type
        val mimeType = contentResolver.getType(uri) ?: ""
        val type = when {
            mimeType.startsWith("image/") -> HiddenItemEntity.ItemType.PHOTO
            mimeType.startsWith("video/") -> HiddenItemEntity.ItemType.VIDEO
            else -> HiddenItemEntity.ItemType.FILE
        }
        
        // Import to vault
        vaultRepository.addItemToVault(vaultId, path, pin, type)
    }
    
    companion object {
        private const val IMPORT_BATCH_SIZE = 5 // Process 5 files concurrently
    }
}

class ImportFilesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository
) {
    /**
     Import generic files (documents, etc.)
     */
    operator fun invoke(
        vaultId: String,
        pin: String,
        paths: List<String>
    ): Flow<ImportProgress> = flow {
        val total = paths.size
        var completed = 0
        var failed = 0
        
        emit(ImportProgress.Started(total))
        
        paths.chunked(IMPORT_BATCH_SIZE).forEach { batch ->
            batch.forEach { path ->
                try {
                    val type = detectFileType(path)
                    vaultRepository.addItemToVault(vaultId, path, pin, type)
                    completed++
                } catch (e: Exception) {
                    failed++
                }
                emit(ImportProgress.InProgress(completed, total, failed))
            }
        }
        
        emit(ImportProgress.Completed(completed, failed))
    }
    
    private fun detectFileType(path: String): HiddenItemEntity.ItemType {
        val extension = path.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> HiddenItemEntity.ItemType.PHOTO
            "mp4", "avi", "mkv", "mov", "webm" -> HiddenItemEntity.ItemType.VIDEO
            "mp3", "wav", "aac", "flac", "ogg" -> HiddenItemEntity.ItemType.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> HiddenItemEntity.ItemType.DOCUMENT
            else -> HiddenItemEntity.ItemType.FILE
        }
    }
    
    companion object {
        private const val IMPORT_BATCH_SIZE = 5
    }
}

sealed class ImportProgress {
    data class Started(val total: Int) : ImportProgress()
    data class InProgress(
        val completed: Int,
        val total: Int,
        val failed: Int
    ) : ImportProgress() {
        val percentage: Int get() = ((completed.toFloat() / total) * 100).toInt()
    }
    data class Completed(val successCount: Int, val failedCount: Int) : ImportProgress()
}

sealed class ImportResult {
    object Success : ImportResult()
    data class Failure(val path: String, val error: String) : ImportResult()
}
