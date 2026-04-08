package com.cover.app.presentation.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.content.AppHiderManager
import com.cover.app.data.local.dao.HiddenAppDao
import com.cover.app.data.local.entity.HiddenAppEntity
import com.cover.app.data.security.LockoutManager
import com.cover.app.data.security.PinManager
import com.cover.app.data.security.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the custom launcher that manages:
 * - Calculator PIN detection
 * - App visibility (hidden vs shown based on vault unlock)
 * - Installed app loading
 * - Vault state management
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinManager: PinManager,
    private val lockoutManager: LockoutManager,
    private val shakeDetector: ShakeDetector,
    private val appHiderManager: AppHiderManager,
    private val hiddenAppDao: HiddenAppDao
) : ViewModel() {

    private val packageManager = context.packageManager

    // Calculator state for PIN entry
    private val _calculatorState = MutableStateFlow(CalculatorState())
    val calculatorState: StateFlow<CalculatorState> = _calculatorState.asStateFlow()

    // PIN detection state
    private val _pinState = MutableStateFlow<PinDetectionState>(PinDetectionState.None)
    val pinState: StateFlow<PinDetectionState> = _pinState.asStateFlow()

    // Lockout state
    private val _lockoutState = MutableStateFlow<LockoutUiState>(LockoutUiState.Unlocked)
    val lockoutState: StateFlow<LockoutUiState> = _lockoutState.asStateFlow()

    // Launcher state - controls whether to show calculator or app grid
    private val _launcherState = MutableStateFlow<LauncherDisplayState>(LauncherDisplayState.Calculator)
    val launcherState: StateFlow<LauncherDisplayState> = _launcherState.asStateFlow()

    // All installed apps
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    // Hidden package names from vault
    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())

    // Currently visible apps (filtered based on vault state)
    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    // Current vault state
    private val _vaultState = MutableStateFlow<VaultUnlockState>(VaultUnlockState.Locked)
    val vaultState: StateFlow<VaultUnlockState> = _vaultState.asStateFlow()

    private var pinBuffer = StringBuilder()
    private var lastWasEquals = false
    private var currentVaultId: String? = null
    private var isDecoyVault = false

    init {
        loadInstalledApps()
        monitorLockoutState()
        monitorShakeForEmergency()
    }

    /**
     * Load all installed apps that can be launched
     */
    private fun loadInstalledApps() {
        viewModelScope.launch {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val apps = packageManager.queryIntentActivities(launcherIntent, 0)
                .filter { resolveInfo ->
                    // Exclude our own launcher to avoid recursion
                    resolveInfo.activityInfo.packageName != context.packageName
                }
                .map { resolveInfo ->
                    AppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        icon = resolveInfo.loadIcon(packageManager),
                        activityName = resolveInfo.activityInfo.name
                    )
                }
                .sortedBy { it.label.lowercase() }

            _allApps.value = apps
            updateVisibleApps()
        }
    }

    /**
     * Refresh apps list
     */
    fun refreshApps() {
        loadInstalledApps()
    }

    /**
     * Update which apps are visible based on vault unlock state and hidden list
     */
    private fun updateVisibleApps() {
        val all = _allApps.value
        val hidden = _hiddenPackages.value
        val vaultLocked = _vaultState.value is VaultUnlockState.Locked

        _visibleApps.value = if (vaultLocked) {
            // Vault locked: hide apps that are marked as hidden
            all.filter { it.packageName !in hidden }
        } else {
            // Vault unlocked: show all apps
            all
        }
    }

    /**
     * Load hidden apps for a specific vault
     */
    fun loadHiddenAppsForVault(vaultId: String) {
        viewModelScope.launch {
            val hidden = hiddenAppDao.getHiddenPackageNames(vaultId)
            _hiddenPackages.value = hidden.toSet()
            updateVisibleApps()
        }
    }

    /**
     * Hide an app in the vault
     */
    fun hideApp(packageName: String, appName: String) {
        val vaultId = currentVaultId ?: return

        viewModelScope.launch {
            val entity = HiddenAppEntity(
                id = UUID.randomUUID().toString(),
                vaultId = vaultId,
                packageName = packageName,
                appName = appName,
                isHidden = true
            )
            hiddenAppDao.insertHiddenApp(entity)
            loadHiddenAppsForVault(vaultId)
        }
    }

    /**
     * Unhide an app
     */
    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            hiddenAppDao.deleteHiddenAppByPackage(packageName)
            currentVaultId?.let { loadHiddenAppsForVault(it) }
        }
    }

    // ===== Calculator PIN Detection =====

    fun onNumberClick(number: String) {
        if (isLockedOut()) return

        val current = _calculatorState.value

        if (lastWasEquals) {
            _calculatorState.value = current.copy(display = number, expression = "")
            lastWasEquals = false
        } else {
            val newDisplay = if (current.display == "0") {
                number
            } else {
                current.display + number
            }
            _calculatorState.value = current.copy(display = newDisplay)
        }

        pinBuffer.append(number)
    }

    fun onDecimalClick() {
        if (isLockedOut()) return
        val current = _calculatorState.value
        if (!current.display.contains(".")) {
            _calculatorState.value = current.copy(display = current.display + ".")
        }
    }

    fun onOperationClick(operation: Operation) {
        if (isLockedOut()) return

        val current = _calculatorState.value
        val displayValue = current.display

        when (operation) {
            Operation.ADD -> pinBuffer.append("+")
            Operation.MULTIPLY -> pinBuffer.append("*")
            else -> {}
        }

        _calculatorState.value = current.copy(
            expression = "$displayValue ${operation.symbol}",
            display = "0",
            previousValue = displayValue.toDoubleOrNull() ?: 0.0,
            currentOperation = operation
        )
    }

    fun onEqualsClick() {
        if (isLockedOut()) return

        val current = _calculatorState.value
        val currentValue = current.display.toDoubleOrNull() ?: 0.0
        val previousValue = current.previousValue
        val operation = current.currentOperation

        pinBuffer.append("=")

        // Check for PIN pattern before calculating
        if (checkPinPattern()) {
            return
        }

        // Perform calculation
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

        _calculatorState.value = current.copy(
            display = resultString,
            expression = "",
            previousValue = 0.0,
            currentOperation = null
        )

        lastWasEquals = true
        pinBuffer.clear()
    }

    fun onClearClick() {
        _calculatorState.value = CalculatorState()
        pinBuffer.clear()
        _pinState.value = PinDetectionState.None
    }

    fun onDeleteClick() {
        val current = _calculatorState.value
        val currentDisplay = current.display

        if (currentDisplay.length > 1) {
            _calculatorState.value = current.copy(
                display = currentDisplay.dropLast(1)
            )
        } else {
            _calculatorState.value = current.copy(display = "0")
        }

        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
        }
    }

    fun onPercentClick() {
        if (isLockedOut()) return
        val current = _calculatorState.value
        val value = current.display.toDoubleOrNull() ?: 0.0
        _calculatorState.value = current.copy(display = (value / 100).toString())
    }

    private fun checkPinPattern(): Boolean {
        val buffer = pinBuffer.toString()

        // Check for real vault PIN: {4-8 digits}+0=
        val realPinRegex = Regex("(\\d{4,8})\\+0=$")
        val realMatch = realPinRegex.find(buffer)

        if (realMatch != null) {
            val pin = realMatch.groupValues[1]
            handlePinAttempt(pin, isDecoy = false)
            return true
        }

        // Check for decoy vault PIN: {4-8 digits}+1=
        val decoyPinRegex = Regex("(\\d{4,8})\\+1=$")
        val decoyMatch = decoyPinRegex.find(buffer)

        if (decoyMatch != null) {
            val pin = decoyMatch.groupValues[1]
            handlePinAttempt(pin, isDecoy = true)
            return true
        }

        return false
    }

    private fun handlePinAttempt(pin: String, isDecoy: Boolean) {
        _pinState.value = PinDetectionState.Verifying

        viewModelScope.launch {
            delay(300) // Brief delay for UX

            val isValid = if (isDecoy) {
                pinManager.verifyDecoyPin(pin)
            } else {
                pinManager.verifyRealPin(pin)
            }

            if (isValid) {
                lockoutManager.resetAttempts()
                currentVaultId = if (isDecoy) {
                    pinManager.getDecoyVaultId(pin)
                } else {
                    pinManager.getRealVaultId(pin)
                }
                this@LauncherViewModel.isDecoyVault = isDecoy

                // Unlock the vault and show all apps
                _vaultState.value = VaultUnlockState.Unlocked(isDecoy)
                currentVaultId?.let { loadHiddenAppsForVault(it) }

                // Switch to app grid view
                _launcherState.value = LauncherDisplayState.AppGrid

                _pinState.value = if (isDecoy) {
                    PinDetectionState.DecoyVaultUnlocked
                } else {
                    PinDetectionState.RealVaultUnlocked
                }
            } else {
                val lockedOut = lockoutManager.recordFailedAttempt()
                _pinState.value = PinDetectionState.FailedAttempt(
                    isLockedOut = lockedOut,
                    remainingAttempts = 3 - lockoutManager.getFailedAttempts()
                )

                // Re-lock if it was previously unlocked
                _vaultState.value = VaultUnlockState.Locked
                updateVisibleApps()
            }

            // Reset calculator
            _calculatorState.value = CalculatorState()
            pinBuffer.clear()
        }
    }

    private fun monitorLockoutState() {
        viewModelScope.launch {
            while (true) {
                lockoutManager.updateLockoutState()
                val state = lockoutManager.lockoutState.value
                _lockoutState.value = when (state) {
                    is LockoutManager.LockoutState.Locked ->
                        LockoutUiState.Locked(state.remainingMinutes, state.remainingSeconds)
                    else -> LockoutUiState.Unlocked
                }
                delay(1000)
            }
        }
    }

    private fun monitorShakeForEmergency() {
        viewModelScope.launch {
            shakeDetector.shakeEvents.collect {
                // Emergency: lock vault and return to calculator
                _vaultState.value = VaultUnlockState.Locked
                _launcherState.value = LauncherDisplayState.Calculator
                updateVisibleApps()
                onClearClick()
            }
        }
        shakeDetector.startListening()
    }

    private fun isLockedOut(): Boolean {
        return lockoutManager.isLockedOut()
    }

    /**
     * Launch an app
     */
    fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(app.packageName)
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Lock the vault and return to calculator
     */
    fun lockVault() {
        _vaultState.value = VaultUnlockState.Locked
        _launcherState.value = LauncherDisplayState.Calculator
        updateVisibleApps()
        onClearClick()
    }

    override fun onCleared() {
        super.onCleared()
        shakeDetector.stopListening()
    }
}

// ===== State Classes =====

data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val previousValue: Double = 0.0,
    val currentOperation: Operation? = null
)

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable,
    val activityName: String
)

sealed class LauncherDisplayState {
    object Calculator : LauncherDisplayState()
    object AppGrid : LauncherDisplayState()
}

sealed class VaultUnlockState {
    object Locked : VaultUnlockState()
    data class Unlocked(val isDecoy: Boolean) : VaultUnlockState()
}

sealed class PinDetectionState {
    object None : PinDetectionState()
    object Verifying : PinDetectionState()
    data class FailedAttempt(val isLockedOut: Boolean, val remainingAttempts: Int) : PinDetectionState()
    object RealVaultUnlocked : PinDetectionState()
    object DecoyVaultUnlocked : PinDetectionState()
}

sealed class LockoutUiState {
    object Unlocked : LockoutUiState()
    data class Locked(val remainingMinutes: Int, val remainingSeconds: Int) : LockoutUiState()
}

enum class Operation(val symbol: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("×"),
    DIVIDE("÷"),
    MODULO("%")
}
