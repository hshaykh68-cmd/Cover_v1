package com.cover.app.presentation.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.repository.VaultRepository
import com.cover.app.data.security.SessionManager
import com.cover.app.data.storage.SecureStorageManager
import com.cover.app.domain.model.HiddenItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val secureStorage: SecureStorageManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()
    
    private val _items = MutableStateFlow<List<HiddenItem>>(emptyList())
    val items: StateFlow<List<HiddenItem>> = _items.asStateFlow()
    
    /**
     * Load all items from the current vault using SessionManager
     */
    fun loadItems() {
        viewModelScope.launch {
            val vaultId = sessionManager.getVaultId() ?: return@launch
            vaultRepository.getItemsByVault(vaultId)
                .collect { items ->
                    _items.value = items
                }
        }
    }
    
    /**
     * Get the current PIN from session
     */
    fun getPin(): String? = sessionManager.getPin()
    
    /**
     * Get the current vault ID from session
     */
    fun getVaultId(): String? = sessionManager.getVaultId()
    
    /**
     * Check if there's an active session
     */
    fun hasSession(): Boolean = sessionManager.hasActiveSession()

    fun setCurrentIndex(index: Int) {
        _state.value = _state.value.copy(currentIndex = index)
    }

    fun toggleControls() {
        _state.value = _state.value.copy(showControls = !_state.value.showControls)
    }

    /**
     * Decrypt a photo item and return as Bitmap
     */
    suspend fun decryptItem(item: HiddenItem, vaultId: String, pin: String): Bitmap? = 
        withContext(Dispatchers.IO) {
            try {
                val salt = vaultRepository.getVaultSalt(vaultId) ?: return@withContext null
                
                // For photos, decrypt and convert to bitmap
                if (item is HiddenItem.Photo) {
                    val decryptedBytes = secureStorage.retrieveFile(item.encryptedPath, pin, salt)
                        ?: return@withContext null
                    
                    BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Export item to external storage
     */
    suspend fun exportItem(
        item: HiddenItem,
        vaultId: String,
        pin: String,
        destinationUri: android.net.Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val salt = vaultRepository.getVaultSalt(vaultId) ?: return@withContext false
            val result = vaultRepository.exportItem(item, pin, salt, destinationUri)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete item from vault
     */
    suspend fun deleteItem(item: HiddenItem, vaultId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            vaultRepository.removeItemFromVault(item.id, vaultId)
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class ViewerState(
    val currentIndex: Int = 0,
    val showControls: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<HiddenItem> = emptyList()
)
