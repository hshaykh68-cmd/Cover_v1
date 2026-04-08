package com.cover.app.presentation.intruder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import com.cover.app.data.remoteconfig.FeatureFlag
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.repository.VaultRepository
import com.cover.app.data.storage.SecureStorageManager
import com.cover.app.domain.model.IntruderLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntruderLogsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val secureStorage: SecureStorageManager,
    private val promotionManager: PromotionManager
) : ViewModel() {

    private val _state = MutableStateFlow(IntruderLogsState())
    val state: StateFlow<IntruderLogsState> = _state.asStateFlow()

    init {
        // Only load logs if break-in alerts feature is enabled
        if (promotionManager.isFeatureEnabled(FeatureFlag.BREAK_IN_ALERTS)) {
            loadLogs()
        } else {
            _state.update { it.copy(isLoading = false, logs = emptyList()) }
        }
    }

    private fun loadLogs() {
        viewModelScope.launch {
            vaultRepository.getIntruderLogs()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .collect { logs ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            logs = logs
                        )
                    }
                    // Load photos for logs that have them
                    loadIntruderPhotos(logs)
                }
        }
    }

    private fun loadIntruderPhotos(logs: List<IntruderLog>) {
        viewModelScope.launch {
            val photos = mutableMapOf<String, Bitmap>()
            logs.filter { it.photoId != null }.forEach { log ->
                log.photoId?.let { photoId ->
                    // Intruder photos use master key encryption, no PIN needed
                    secureStorage.retrieveIntruderPhoto(photoId)?.let { bitmap ->
                        photos[log.id] = bitmap
                    }
                }
            }
            _state.update { it.copy(photos = photos) }
        }
    }

    fun deleteLog(logId: String) {
        viewModelScope.launch {
            vaultRepository.deleteIntruderLog(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            vaultRepository.clearAllIntruderLogs()
        }
    }
}

data class IntruderLogsState(
    val isLoading: Boolean = false,
    val logs: List<IntruderLog> = emptyList(),
    val photos: Map<String, Bitmap> = emptyMap()
)
