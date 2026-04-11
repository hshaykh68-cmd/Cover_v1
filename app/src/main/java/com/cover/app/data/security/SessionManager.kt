package com.cover.app.data.security

import com.cover.app.domain.model.HiddenItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the current user session including authenticated PIN and vault access.
 * This is used for temporary storage of the PIN during the app session to enable
 * thumbnail and file decryption without requiring the user to re-enter their PIN.
 */
@Singleton
class SessionManager @Inject constructor() {
    
    private val _currentPin = MutableStateFlow<String?>(null)
    val currentPin: StateFlow<String?> = _currentPin.asStateFlow()
    
    private val _currentVaultId = MutableStateFlow<String?>(null)
    val currentVaultId: StateFlow<String?> = _currentVaultId.asStateFlow()
    
    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()
    
    /**
     * Start a new session after successful PIN verification
     */
    fun startSession(pin: String, vaultId: String, isDecoy: Boolean = false) {
        _currentPin.value = pin
        _currentVaultId.value = vaultId
        _isDecoyMode.value = isDecoy
    }
    
    /**
     * Clear the session (called when locking the vault or app background timeout)
     */
    fun clearSession() {
        _currentPin.value = null
        _currentVaultId.value = null
        _isDecoyMode.value = false
    }
    
    /**
     * Check if there's an active session
     */
    fun hasActiveSession(): Boolean {
        return _currentPin.value != null && _currentVaultId.value != null
    }
    
    /**
     * Get the current PIN (returns null if no active session)
     */
    fun getPin(): String? = _currentPin.value
    
    /**
     * Get the current vault ID (returns null if no active session)
     */
    fun getVaultId(): String? = _currentVaultId.value
}
