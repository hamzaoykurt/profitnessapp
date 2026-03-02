package com.avonix.profitness.presentation.auth

import com.avonix.profitness.core.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    // TODO: Inject Firebase Auth Repository here
) : BaseViewModel<AuthState>(AuthState()) {

    fun onLoginClick(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            updateState { it.copy(error = "Lütfen tüm alanları doldurun.") }
            return
        }

        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            delay(1000)
            // Demo mode: accept any credentials
            if (email.contains("@") && pass.length >= 6) {
                updateState { it.copy(isLoading = false, isSuccess = true) }
            } else {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Geçersiz email veya şifre (min. 6 karakter)"
                    )
                }
            }
        }
    }

    fun onRegisterClick(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            updateState { it.copy(error = "Lütfen tüm alanları doldurun.") }
            return
        }
        if (pass.length < 6) {
            updateState { it.copy(error = "Şifre en az 6 karakter olmalı.") }
            return
        }

        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            delay(1200)
            updateState { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}
