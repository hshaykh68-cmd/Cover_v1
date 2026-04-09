package com.cover.app.presentation.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.cover.app.data.admob.AdMobManager
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.remoteconfig.InAppMessageManager
import com.cover.app.data.remoteconfig.UpsellTrigger
import com.cover.app.data.repository.PremiumRepository
import com.cover.app.data.repository.VaultRepository
import com.cover.app.data.thumbnail.ThumbnailCache
import com.cover.app.domain.model.HiddenItem
import com.cover.app.domain.usecase.ImportFilesUseCase
import com.cover.app.domain.usecase.ImportMediaUseCase
import com.cover.app.domain.usecase.ImportProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val importMediaUseCase: ImportMediaUseCase,
    private val importFilesUseCase: ImportFilesUseCase,
    private val thumbnailCache: ThumbnailCache,
    val adMobManager: AdMobManager,
    private val premiumRepository: PremiumRepository,
    private val promotionManager: PromotionManager,
    private val inAppMessageManager: InAppMessageManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GalleryEvent>()
    val events: SharedFlow<GalleryEvent> = _events.asSharedFlow()

    private val _showUpsell = MutableStateFlow<UpsellTrigger?>(null)
    val showUpsell: StateFlow<UpsellTrigger?> = _showUpsell.asStateFlow()

    private var importJob: Job? = null
    private var itemsJob: Job? = null
    private var premiumJob: Job? = null
    
    // Cache premium status as StateFlow to avoid multiple collectors
    private val premiumStatus = premiumRepository.isPremium()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        loadItems()
    }

    private fun loadItems() {
        // Items will be loaded when vaultId is available
    }

    private val _currentPin = MutableStateFlow("")

    fun setCurrentPin(pin: String) {
        _currentPin.value = pin
    }

    fun loadVaultItems(vaultId: String, pin: String) {
        _currentPin.value = pin
        
        // Cancel previous collection jobs to avoid accumulation
        premiumJob?.cancel()
        itemsJob?.cancel()
        
        // Collect premium status using cached StateFlow
        premiumJob = viewModelScope.launch {
            premiumStatus.collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
        
        // Collect vault items with job tracking
        itemsJob = viewModelScope.launch {
            vaultRepository.getItemsByVault(vaultId)
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) } }
                .collect { items ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items
                        )
                    }
                }
        }
    }
    
    fun loadDefaultVaultItems() {
        // Cancel previous collection jobs to avoid accumulation
        premiumJob?.cancel()
        itemsJob?.cancel()
        
        // Collect premium status using cached StateFlow
        premiumJob = viewModelScope.launch {
            premiumStatus.collect { isPremium ->
                _state.update { it.copy(isPremium = isPremium) }
            }
        }
        
        // Collect items from default vault (all items)
        itemsJob = viewModelScope.launch {
            vaultRepository.getAllItems()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) } }
                .collect { items ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items
                        )
                    }
                }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        premiumJob?.cancel()
        itemsJob?.cancel()
        importJob?.cancel()
    }

    fun importMedia(vaultId: String, pin: String, uris: List<Uri>) {
        importJob = viewModelScope.launch {
            // Check if should show upsell on import
            if (promotionManager.onImportAction()) {
                _showUpsell.value = UpsellTrigger.IMPORT_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.IMPORT_LIMIT)
            }
            
            importMediaUseCase(vaultId, pin, uris)
                .onStart {
                    _state.update { it.copy(isImporting = true) }
                }
                .onCompletion {
                    _state.update { it.copy(isImporting = false, importProgress = null) }
                }
                .collect { progress ->
                    _state.update { it.copy(importProgress = progress) }
                    if (progress is ImportProgress.Completed) {
                        _events.emit(GalleryEvent.ShowSnackbar(
                            "Imported ${progress.successCount} items"
                        ))
                    }
                }
        }
    }

    fun importFiles(vaultId: String, pin: String, paths: List<String>) {
        importJob = viewModelScope.launch {
            // Check if should show upsell on import
            if (promotionManager.onImportAction()) {
                _showUpsell.value = UpsellTrigger.IMPORT_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.IMPORT_LIMIT)
            }
            
            importFilesUseCase(vaultId, pin, paths)
                .onStart {
                    _state.update { it.copy(isImporting = true) }
                }
                .onCompletion {
                    _state.update { it.copy(isImporting = false, importProgress = null) }
                }
                .collect { progress ->
                    _state.update { it.copy(importProgress = progress) }
                    if (progress is ImportProgress.Completed) {
                        _events.emit(GalleryEvent.ShowSnackbar(
                            "Imported ${progress.successCount} files"
                        ))
                    }
                }
        }
    }
    
    fun importMediaToDefault(uris: List<Uri>) {
        importJob = viewModelScope.launch {
            // Get default vault ID
            val defaultVault = vaultRepository.getAllVaults().first().firstOrNull()
            if (defaultVault == null) {
                _events.emit(GalleryEvent.ShowSnackbar("No vault found. Please create a vault first."))
                return@launch
            }
            
            // Check if should show upsell on import
            if (promotionManager.onImportAction()) {
                _showUpsell.value = UpsellTrigger.IMPORT_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.IMPORT_LIMIT)
            }
            
            val pin = _currentPin.value.ifEmpty { "1234" } // Fallback pin
            importMediaUseCase(defaultVault.id, pin, uris)
                .onStart {
                    _state.update { it.copy(isImporting = true) }
                }
                .onCompletion {
                    _state.update { it.copy(isImporting = false, importProgress = null) }
                }
                .collect { progress ->
                    _state.update { it.copy(importProgress = progress) }
                    if (progress is ImportProgress.Completed) {
                        _events.emit(GalleryEvent.ShowSnackbar(
                            "Imported ${progress.successCount} items"
                        ))
                    }
                }
        }
    }
    
    fun importFilesToDefault(paths: List<String>) {
        importJob = viewModelScope.launch {
            // Get default vault ID
            val defaultVault = vaultRepository.getAllVaults().first().firstOrNull()
            if (defaultVault == null) {
                _events.emit(GalleryEvent.ShowSnackbar("No vault found. Please create a vault first."))
                return@launch
            }
            
            // Check if should show upsell on import
            if (promotionManager.onImportAction()) {
                _showUpsell.value = UpsellTrigger.IMPORT_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.IMPORT_LIMIT)
            }
            
            val pin = _currentPin.value.ifEmpty { "1234" } // Fallback pin
            importFilesUseCase(defaultVault.id, pin, paths)
                .onStart {
                    _state.update { it.copy(isImporting = true) }
                }
                .onCompletion {
                    _state.update { it.copy(isImporting = false, importProgress = null) }
                }
                .collect { progress ->
                    _state.update { it.copy(importProgress = progress) }
                    if (progress is ImportProgress.Completed) {
                        _events.emit(GalleryEvent.ShowSnackbar(
                            "Imported ${progress.successCount} files"
                        ))
                    }
                }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _state.update { it.copy(isImporting = false, importProgress = null) }
    }

    fun toggleSelectionMode() {
        _state.update { 
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedItems = if (it.isSelectionMode) emptySet() else it.selectedItems
            ) 
        }
    }

    fun toggleItemSelection(itemId: String) {
        _state.update { state ->
            val current = state.selectedItems
            val updated = if (current.contains(itemId)) {
                current - itemId
            } else {
                current + itemId
            }
            state.copy(selectedItems = updated)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedItems = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        _state.update { state ->
            state.copy(selectedItems = state.items.map { it.id }.toSet())
        }
    }

    fun openItem(item: HiddenItem) {
        viewModelScope.launch {
            val pin = _currentPin.value
            if (pin.isNotEmpty()) {
                _events.emit(GalleryEvent.OpenItem(item, pin))
            }
        }
    }

    fun showItemOptions(item: HiddenItem) {
        viewModelScope.launch {
            _events.emit(GalleryEvent.ShowItemOptions(item))
        }
    }

    companion object {
        const val DELETE_LIMIT = 5 // Free users can delete 5 items at once
        const val EXPORT_LIMIT = 5 // Free users can export 5 items at once
    }

    fun deleteSelectedItems(vaultId: String) {
        val selectedIds = _state.value.selectedItems.toList()
        viewModelScope.launch {
            // Check if should show upsell on delete
            if (promotionManager.onDeleteAction()) {
                _showUpsell.value = UpsellTrigger.DELETE_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.CUSTOM, "Unlock bulk delete and other premium features")
            }
            
            var deletedCount = 0
            selectedIds.forEach { id ->
                try {
                    vaultRepository.removeItemFromVault(id, vaultId)
                    deletedCount++
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
            clearSelection()
            _events.emit(GalleryEvent.ShowSnackbar("Deleted $deletedCount items"))
        }
    }

    fun exportSelectedItems(vaultId: String, pin: String, destinationUri: android.net.Uri) {
        viewModelScope.launch {
            // Check if should show upsell on export
            if (promotionManager.onExportAction() || _state.value.selectedItems.size > EXPORT_LIMIT) {
                _showUpsell.value = UpsellTrigger.EXPORT_LIMIT
                inAppMessageManager.triggerPromotionalUpsell(UpsellTrigger.STORAGE_LIMIT)
            }
            
            val selectedIds = _state.value.selectedItems.toList()
            var exportedCount = 0
            
            selectedIds.forEach { id ->
                try {
                    val item = _state.value.items.find { it.id == id }
                    item?.let { hiddenItem ->
                        // Get vault salt for decryption
                        val salt = vaultRepository.getVaultSalt(vaultId)
                        salt?.let { vaultSalt ->
                            // Export file using repository
                            vaultRepository.exportItem(hiddenItem, pin, vaultSalt, destinationUri)
                            exportedCount++
                        }
                    }
                } catch (e: Exception) {
                    // Log error but continue with others
                }
            }
            
            clearSelection()
            _events.emit(GalleryEvent.ShowSnackbar("Exported $exportedCount items"))
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun dismissUpsell() {
        _showUpsell.value = null
    }
    
    fun showInterstitialIfReady(activity: android.app.Activity) {
        promotionManager.showInterstitialIfReady(activity)
    }
}

data class GalleryUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val items: List<HiddenItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isPremium: Boolean = false,
    val importProgress: ImportProgress? = null,
    val snackbarMessage: String? = null,
    val error: String? = null
)

sealed class GalleryEvent {
    data class OpenItem(val item: HiddenItem, val pin: String) : GalleryEvent()
    data class ShowItemOptions(val item: HiddenItem) : GalleryEvent()
    data class ShowSnackbar(val message: String) : GalleryEvent()
    data class RequestExportDestination(val items: List<HiddenItem>) : GalleryEvent()
}
