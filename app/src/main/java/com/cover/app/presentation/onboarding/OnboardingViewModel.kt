package com.cover.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cover.app.data.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    fun setupPins(realPin: String, decoyPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = pinManager.setupPins(realPin, decoyPin)
            result.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    fun isPinSet(): Boolean {
        return pinManager.isPinSet()
    }
}
