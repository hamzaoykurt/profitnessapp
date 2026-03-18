package com.avonix.profitness.presentation.auth

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AuthEvent {
    object NavigateToDashboard : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<AuthState, AuthEvent>(AuthState()) {

    fun onLoginClick(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            updateState { it.copy(error = "Lütfen tüm alanları doldurun.") }
            return
        }
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signIn(email, pass)
                .onSuccess {
                    updateState { it.copy(isLoading = false) }
                    sendEvent(AuthEvent.NavigateToDashboard)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = parseAuthError(err.message)) }
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
            authRepository.signUp(email, pass)
                .onSuccess {
                    updateState { it.copy(isLoading = false) }
                    sendEvent(AuthEvent.NavigateToDashboard)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = parseAuthError(err.message)) }
                }
        }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun parseAuthError(message: String?): String = when {
        message == null -> "Bir hata oluştu."
        message.contains("Invalid login credentials", ignoreCase = true) ->
            "Hatalı email veya şifre."
        message.contains("Email not confirmed", ignoreCase = true) ->
            "Email adresinizi doğrulayın."
        message.contains("User already registered", ignoreCase = true) ->
            "Bu email adresi zaten kayıtlı."
        message.contains("Password should be at least", ignoreCase = true) ->
            "Şifre en az 6 karakter olmalı."
        message.contains("Unable to resolve", ignoreCase = true) ||
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."
        else -> "Hata: ${message.take(100)}"
    }
}
