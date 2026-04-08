package com.cover.app.presentation.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.intruder.IntruderCaptureManager
import com.cover.app.data.location.LocationManager
import com.cover.app.data.remoteconfig.FeatureFlag
import com.cover.app.data.remoteconfig.PromotionManager
import com.cover.app.data.security.LockoutManager
import com.cover.app.data.security.PinManager
import com.cover.app.data.security.ShakeDetector
import com.cover.app.data.vault.DecoyVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val lockoutManager: LockoutManager,
    private val intruderCaptureManager: IntruderCaptureManager,
    private val locationManager: LocationManager,
    private val shakeDetector: ShakeDetector,
    private val decoyVaultManager: DecoyVaultManager,
    private val pinManager: PinManager,
    private val promotionManager: PromotionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private val _pinDetectionState = MutableStateFlow<PinDetectionState>(PinDetectionState.None)
    val pinDetectionState: StateFlow<PinDetectionState> = _pinDetectionState.asStateFlow()

    private val _lockoutState = MutableStateFlow<LockoutUiState>(LockoutUiState.Unlocked)
    val lockoutState: StateFlow<LockoutUiState> = _lockoutState.asStateFlow()

    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()

    private var pinBuffer = StringBuilder()
    private var lastWasEquals = false
    private var currentPinAttempt: String? = null

    init {
        // Monitor lockout state - only poll when locked
        viewModelScope.launch {
            // Check if initially locked
            if (lockoutManager.isLockedOut()) {
                startLockoutPolling()
            }
            
            // Monitor for lockout state changes
            lockoutManager.lockoutState.collect { state ->
                if (state is LockoutManager.LockoutState.Locked) {
                    startLockoutPolling()
                }
            }
        }

        // Monitor shake for emergency close (if enabled)
        if (promotionManager.isFeatureEnabled(FeatureFlag.SHAKE_EXIT)) {
            viewModelScope.launch {
                shakeDetector.shakeEvents.collect {
                    _emergencyState.value = EmergencyState.ShakeDetected
                }
            }
            // Start shake detection
            shakeDetector.startListening()
        }
    }

    fun onNumberClick(number: String) {
        if (isLockedOut()) return

        val currentState = _state.value
        
        if (lastWasEquals) {
            _state.value = currentState.copy(display = number, expression = "")
            lastWasEquals = false
        } else {
            val newDisplay = if (currentState.display == "0") {
                number
            } else {
                currentState.display + number
            }
            _state.value = currentState.copy(display = newDisplay)
        }
        
        pinBuffer.append(number)
        checkPinPattern()
    }

    fun onDecimalClick() {
        if (isLockedOut()) return

        val currentState = _state.value
        if (!currentState.display.contains(".")) {
            _state.value = currentState.copy(display = currentState.display + ".")
        }
    }

    fun onOperationClick(operation: Operation) {
        if (isLockedOut()) return

        val currentState = _state.value
        val displayValue = currentState.display
        
        // Track in PIN buffer
        when (operation) {
            Operation.ADD -> pinBuffer.append("+")
            Operation.MULTIPLY -> pinBuffer.append("*")
            else -> {}
        }
        
        _state.value = currentState.copy(
            expression = "$displayValue ${operation.symbol}",
            display = "0",
            previousValue = displayValue.toDoubleOrNull() ?: 0.0,
            currentOperation = operation
        )
        
        checkPinPattern()
    }

    fun onEqualsClick() {
        if (isLockedOut()) return

        val currentState = _state.value
        val currentValue = currentState.display.toDoubleOrNull() ?: 0.0
        val previousValue = currentState.previousValue
        val operation = currentState.currentOperation
        
        // Track in PIN buffer
        pinBuffer.append("=")
        
        // Check if this is a PIN entry before calculating
        if (checkPinPatternBeforeCalculate()) {
            return // PIN detected, don't calculate
        }
        
        val result = when (operation) {
            Operation.ADD -> previousValue + currentValue
            Operation.SUBTRACT -> previousValue - currentValue
            Operation.MULTIPLY -> previousValue * currentValue
            Operation.DIVIDE -> if (currentValue != 0.0) previousValue / currentValue else Double.NaN
            Operation.MODULO -> previousValue % currentValue
            null -> currentValue
        }
        
        val resultString = if (result.isNaN()) {
            "Error"
        } else {
            if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                "%.8f".format(result).trimEnd('0').trimEnd('.')
            }
        }
        
        _state.value = currentState.copy(
            display = resultString,
            expression = "",
            previousValue = 0.0,
            currentOperation = null
        )
        
        lastWasEquals = true
        pinBuffer.clear() // Clear buffer after normal calculation
    }

    fun onClearClick() {
        _state.value = CalculatorState()
        pinBuffer.clear()
        _pinDetectionState.value = PinDetectionState.None
    }

    fun onDeleteClick() {
        val currentState = _state.value
        val currentDisplay = currentState.display
        
        if (currentDisplay.length > 1) {
            _state.value = currentState.copy(
                display = currentDisplay.dropLast(1)
            )
        } else {
            _state.value = currentState.copy(display = "0")
        }
        
        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
        }
    }

    fun onPercentClick() {
        if (isLockedOut()) return

        val currentState = _state.value
        val value = currentState.display.toDoubleOrNull() ?: 0.0
        _state.value = currentState.copy(display = (value / 100).toString())
    }

    private fun checkPinPatternBeforeCalculate(): Boolean {
        val buffer = pinBuffer.toString()
        
        // Check for direct PIN entry: {4-8 digits}=
        val directPinRegex = Regex("^(\\d{4,8})=\$")
        val directMatch = directPinRegex.find(buffer)
        
        if (directMatch != null) {
            val pin = directMatch.groupValues[1]
            handlePinAttempt(pin, isDecoy = false)
            return true
        }
        
        // Check for real vault PIN: {4-8 digits}+0=
        val realPinRegex = Regex("(\\d{4,8})\\+0=\$")
        val realMatch = realPinRegex.find(buffer)
        
        if (realMatch != null) {
            val pin = realMatch.groupValues[1]
            handlePinAttempt(pin, isDecoy = false)
            return true
        }
        
        // Check for decoy vault PIN: {4-8 digits}+1= (only if enabled)
        val decoyEnabled = promotionManager.isFeatureEnabled(FeatureFlag.DECOY_VAULT)
        if (decoyEnabled) {
            val decoyPinRegex = Regex("(\\d{4,8})\\+1=\$")
            val decoyMatch = decoyPinRegex.find(buffer)
            
            if (decoyMatch != null) {
                val pin = decoyMatch.groupValues[1]
                handlePinAttempt(pin, isDecoy = true)
                return true
            }
        }
        
        return false
    }

    private fun checkPinPattern() {
        // This is checked when buffer changes, actual detection happens on =
        val buffer = pinBuffer.toString()
        
        // Prevent buffer from growing too large
        if (buffer.length > 15 && !buffer.contains("=")) {
            pinBuffer.delete(0, buffer.length - 10)
        }
    }

    private fun handlePinAttempt(pin: String, isDecoy: Boolean) {
        currentPinAttempt = pin
        
        // Check if decoy vault is enabled
        if (isDecoy && !promotionManager.isFeatureEnabled(FeatureFlag.DECOY_VAULT)) {
            // Decoy vault disabled - treat as normal failed attempt
            _pinDetectionState.value = PinDetectionState.FailedAttempt(
                pin = pin,
                isLockedOut = false,
                remainingAttempts = 3
            )
            pinBuffer.clear()
            return
        }
        
        viewModelScope.launch {
            _pinDetectionState.value = PinDetectionState.Capturing
            
            // Check if intruder selfie is enabled
            val intruderEnabled = promotionManager.isFeatureEnabled(FeatureFlag.INTRUDER_SELFIE)
            
            if (intruderEnabled) {
                // 1. Initialize camera for capture
                intruderCaptureManager.initializeCamera()
                
                // 2. Get location
                val location = locationManager.getCurrentLocation()
                
                // 3. Capture intruder photo
                val photoIds = intruderCaptureManager.captureIntruderSequence(
                    attemptedPin = pin,
                    isDecoyVault = isDecoy,
                    location = location
                )
            }
            
            // 4. Verify PIN using PinManager
            val isValid = if (isDecoy) {
                pinManager.verifyDecoyPin(pin)
            } else {
                pinManager.verifyRealPin(pin)
            }
            
            if (isValid) {
                // Success - reset attempts
                lockoutManager.resetAttempts()
                
                _pinDetectionState.value = if (isDecoy) {
                    PinDetectionState.DecoyVaultDetected(pin)
                } else {
                    PinDetectionState.RealVaultDetected(pin)
                }
            } else {
                // Failed attempt
                val lockedOut = lockoutManager.recordFailedAttempt()
                
                _pinDetectionState.value = PinDetectionState.FailedAttempt(
                    pin = pin,
                    isLockedOut = lockedOut,
                    remainingAttempts = 3 - lockoutManager.getFailedAttempts()
                )
            }
            
            // Cleanup
            pinBuffer.clear()
            if (intruderEnabled) {
                intruderCaptureManager.shutdown()
            }
        }
    }

    private fun startLockoutPolling() {
        viewModelScope.launch {
            while (lockoutManager.isLockedOut()) {
                lockoutManager.updateLockoutState()
                val state = lockoutManager.lockoutState.value
                _lockoutState.value = when (state) {
                    is LockoutManager.LockoutState.Locked -> 
                        LockoutUiState.Locked(state.remainingMinutes, state.remainingSeconds)
                    else -> LockoutUiState.Unlocked
                }
                delay(1000)
            }
            // Lockout ended
            _lockoutState.value = LockoutUiState.Unlocked
        }
    }

    private fun isLockedOut(): Boolean {
        return lockoutManager.isLockedOut()
    }

    fun resetPinDetection() {
        _pinDetectionState.value = PinDetectionState.None
        pinBuffer.clear()
    }

    fun acknowledgeEmergency() {
        _emergencyState.value = EmergencyState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Only stop shake detection if it was started (feature enabled)
        if (promotionManager.isFeatureEnabled(FeatureFlag.SHAKE_EXIT)) {
            shakeDetector.stopListening()
        }
        intruderCaptureManager.shutdown()
    }
}

data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val previousValue: Double = 0.0,
    val currentOperation: Operation? = null
)

sealed class PinDetectionState {
    object None : PinDetectionState()
    object Capturing : PinDetectionState()
    data class RealVaultDetected(val pin: String) : PinDetectionState()
    data class DecoyVaultDetected(val pin: String) : PinDetectionState()
    data class FailedAttempt(
        val pin: String,
        val isLockedOut: Boolean,
        val remainingAttempts: Int
    ) : PinDetectionState()
}

sealed class LockoutUiState {
    object Unlocked : LockoutUiState()
    data class Locked(val remainingMinutes: Int, val remainingSeconds: Int) : LockoutUiState()
}

sealed class EmergencyState {
    object Idle : EmergencyState()
    object ShakeDetected : EmergencyState()
}

enum class Operation(val symbol: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("×"),
    DIVIDE("÷"),
    MODULO("%")
}
