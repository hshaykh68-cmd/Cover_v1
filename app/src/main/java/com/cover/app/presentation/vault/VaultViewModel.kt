package com.cover.app.presentation.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.admob.AdMobManager
import com.cover.app.data.repository.PremiumRepository
import com.cover.app.data.repository.VaultRepository
import com.cover.app.domain.model.HiddenItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val premiumRepository: PremiumRepository,
    val adMobManager: AdMobManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vaultId: String = savedStateHandle["vaultId"] ?: ""

    private val _state = MutableStateFlow(VaultUiState(vaultId = vaultId))
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

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
