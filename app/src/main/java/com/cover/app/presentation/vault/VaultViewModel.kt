package com.cover.app.presentation.vault

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.admob.AdMobManager
import com.cover.app.data.local.dao.VaultDao
import com.cover.app.data.repository.PremiumRepository
import com.cover.app.data.repository.VaultRepository
import com.cover.app.data.security.SessionManager
import com.cover.app.data.thumbnail.ThumbnailCache
import com.cover.app.domain.model.HiddenItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val premiumRepository: PremiumRepository,
    private val encryptionManager: com.cover.app.data.security.EncryptionManager,
    val adMobManager: AdMobManager,
    private val thumbnailCache: ThumbnailCache,
    private val sessionManager: SessionManager,
    private val vaultDao: VaultDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vaultId: String = savedStateHandle["vaultId"] ?: sessionManager.getVaultId() ?: ""

    private val _state = MutableStateFlow(VaultUiState(vaultId = vaultId))
    val state: StateFlow<VaultUiState> = _state.asStateFlow()
    
    // Thumbnail cache for loaded bitmaps
    private val _thumbnails = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap?>> = _thumbnails.asStateFlow()
    
    companion object {
        private const val COACH_VAULT_SHOWN = "coach_vault_shown"
    }

    private val _events = MutableSharedFlow<VaultEvent>()
    val events: SharedFlow<VaultEvent> = _events.asSharedFlow()

    init {
        loadVaultItems()
        loadPremiumStatus()
        // Preload ads
        adMobManager.preloadAds()
    }

    private fun loadPremiumStatus() {
        viewModelScope.launch {
            premiumRepository.isPremium().collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    private fun loadVaultItems() {
        viewModelScope.launch {
            vaultRepository.getItemsByVault(vaultId)
                .onStart {
                    _state.update { it.copy(isLoading = true) }
                }
                .catch { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
                .collect { items ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items,
                            itemCount = items.size
                        )
                    }
                    // Preload thumbnails for photo and video items
                    loadThumbnailsForItems(items)
                }
        }
    }
    
    /**
     * Load thumbnails for photo and video items using ThumbnailCache
     */
    private fun loadThumbnailsForItems(items: List<HiddenItem>) {
        viewModelScope.launch {
            val pin = sessionManager.getPin() ?: return@launch
            val vault = vaultDao.getVaultById(vaultId) ?: return@launch
            val salt = vault.salt.toByteArray()
            
            items.forEach { item ->
                when (item) {
                    is HiddenItem.Photo -> {
                        if (item.thumbnailId != null) {
                            val bitmap = thumbnailCache.loadThumbnail(
                                itemId = item.id,
                                encryptedFileId = item.encryptedPath,
                                pin = pin,
                                salt = salt,
                                isVideo = false
                            )
                            _thumbnails.update { it + (item.id to bitmap) }
                        }
                    }
                    is HiddenItem.Video -> {
                        if (item.thumbnailId != null) {
                            val bitmap = thumbnailCache.loadThumbnail(
                                itemId = item.id,
                                encryptedFileId = item.encryptedPath,
                                pin = pin,
                                salt = salt,
                                isVideo = true
                            )
                            _thumbnails.update { it + (item.id to bitmap) }
                        }
                    }
                    else -> {} // Other types don't have thumbnails
                }
            }
        }
    }
    
    /**
     * Get thumbnail bitmap for an item (if loaded)
     */
    fun getThumbnail(itemId: String): Bitmap? {
        return _thumbnails.value[itemId]
    }
    
    /**
     * Load thumbnail on demand for a specific item
     */
    fun loadThumbnailForItem(item: HiddenItem) {
        viewModelScope.launch {
            val pin = sessionManager.getPin() ?: return@launch
            val vault = vaultDao.getVaultById(vaultId) ?: return@launch
            val salt = vault.salt.toByteArray()
            
            when (item) {
                is HiddenItem.Photo -> {
                    if (item.thumbnailId != null && _thumbnails.value[item.id] == null) {
                        val bitmap = thumbnailCache.loadThumbnail(
                            itemId = item.id,
                            encryptedFileId = item.encryptedPath,
                            pin = pin,
                            salt = salt,
                            isVideo = false
                        )
                        _thumbnails.update { it + (item.id to bitmap) }
                    }
                }
                is HiddenItem.Video -> {
                    if (item.thumbnailId != null && _thumbnails.value[item.id] == null) {
                        val bitmap = thumbnailCache.loadThumbnail(
                            itemId = item.id,
                            encryptedFileId = item.encryptedPath,
                            pin = pin,
                            salt = salt,
                            isVideo = true
                        )
                        _thumbnails.update { it + (item.id to bitmap) }
                    }
                }
                else -> {}
            }
        }
    }

    fun onAddItemClick() {
        viewModelScope.launch {
            _events.emit(VaultEvent.ShowAddItemDialog)
        }
    }

    fun onItemClick(item: HiddenItem) {
        viewModelScope.launch {
            _events.emit(VaultEvent.OpenItem(item))
        }
    }

    fun onItemLongPress(item: HiddenItem) {
        viewModelScope.launch {
            _events.emit(VaultEvent.ShowItemOptions(item))
        }
    }

    fun onDeleteItem(item: HiddenItem) {
        viewModelScope.launch {
            try {
                vaultRepository.removeItemFromVault(item.id, vaultId)
                _events.emit(VaultEvent.ShowSnackbar("Item deleted"))
            } catch (e: Exception) {
                _events.emit(VaultEvent.ShowSnackbar("Failed to delete item"))
            }
        }
    }

    fun onLockVault() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _events.emit(VaultEvent.LockVault)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(VaultEvent.NavigateToSettings)
        }
    }

    fun onClearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Check if coach mark has been shown before
     */
    fun isCoachMarkShown(): Boolean {
        return encryptionManager.retrieveSecureBoolean(COACH_VAULT_SHOWN, false)
    }
    
    /**
     * Mark coach mark as shown
     */
    fun markCoachMarkShown() {
        encryptionManager.storeSecureBoolean(COACH_VAULT_SHOWN, true)
    }
}

data class VaultUiState(
    val vaultId: String = "",
    val isLoading: Boolean = false,
    val items: List<HiddenItem> = emptyList(),
    val itemCount: Int = 0,
    val error: String? = null,
    val isPremium: Boolean = false
)

sealed class VaultEvent {
    object ShowAddItemDialog : VaultEvent()
    data class OpenItem(val item: HiddenItem) : VaultEvent()
    data class ShowItemOptions(val item: HiddenItem) : VaultEvent()
    data class ShowSnackbar(val message: String) : VaultEvent()
    object LockVault : VaultEvent()
    object NavigateToSettings : VaultEvent()
}
