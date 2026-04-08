package com.cover.app.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockoutManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private const val KEY_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_END = "lockout_end_time"
        private const val MAX_ATTEMPTS = 3
        private const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    private val _lockoutState = MutableStateFlow<LockoutState>(LockoutState.Unlocked)
    val lockoutState: StateFlow<LockoutState> = _lockoutState.asStateFlow()

    sealed class LockoutState {
        object Unlocked : LockoutState()
        data class Locked(val remainingMinutes: Int, val remainingSeconds: Int) : LockoutState()
    }

    init {
        updateLockoutState()
    }

    /**
     * Record a failed PIN attempt
     * Returns true if max attempts reached and lockout triggered
     */
    fun recordFailedAttempt(): Boolean {
        val currentAttempts = getFailedAttempts()
        val newAttempts = currentAttempts + 1
        
        encryptionManager.storeSecureInt(KEY_ATTEMPTS, newAttempts)

        if (newAttempts >= MAX_ATTEMPTS) {
            triggerLockout()
            return true
        }

        return false
    }

    /**
     * Reset failed attempts counter (successful login)
     */
    fun resetAttempts() {
        encryptionManager.storeSecureInt(KEY_ATTEMPTS, 0)
        encryptionManager.storeSecureLong(KEY_LOCKOUT_END, 0)
        _lockoutState.value = LockoutState.Unlocked
    }

    /**
     * Check if currently locked out
     */
    fun isLockedOut(): Boolean {
        val lockoutEnd = encryptionManager.retrieveSecureLong(KEY_LOCKOUT_END, 0)
        return lockoutEnd > System.currentTimeMillis()
    }

    /**
     * Get remaining lockout time in milliseconds
     */
    fun getRemainingLockoutTime(): Long {
        val lockoutEnd = encryptionManager.retrieveSecureLong(KEY_LOCKOUT_END, 0)
        return maxOf(0, lockoutEnd - System.currentTimeMillis())
    }

    /**
     * Get current failed attempt count
     */
    fun getFailedAttempts(): Int {
        return encryptionManager.retrieveSecureInt(KEY_ATTEMPTS, 0)
    }

    /**
     * Trigger lockout period
     */
    private fun triggerLockout() {
        val endTime = System.currentTimeMillis() + LOCKOUT_DURATION_MS
        encryptionManager.storeSecureLong(KEY_LOCKOUT_END, endTime)
        updateLockoutState()
    }

    /**
     * Update lockout state flow
     */
    fun updateLockoutState() {
        val remainingTime = getRemainingLockoutTime()
        
        if (remainingTime > 0) {
            val totalSeconds = remainingTime / 1000
            val minutes = (totalSeconds / 60).toInt()
            val seconds = (totalSeconds % 60).toInt()
            _lockoutState.value = LockoutState.Locked(minutes, seconds)
        } else {
            _lockoutState.value = LockoutState.Unlocked
            // Clear attempts if lockout expired
            if (getFailedAttempts() >= MAX_ATTEMPTS) {
                resetAttempts()
            }
        }
    }

    /**
     * Check if this is a decoy PIN (ends with +1= pattern)
     */
    fun isDecoyPinAttempt(input: String): Boolean {
        return input.endsWith("+1=")
    }

    /**
     * Check if this is a real vault PIN (ends with +0= pattern)
     */
    fun isRealVaultPinAttempt(input: String): Boolean {
        return input.endsWith("+0=")
    }
}
