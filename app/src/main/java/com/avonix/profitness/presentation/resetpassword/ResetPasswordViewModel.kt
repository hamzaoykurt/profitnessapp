package com.avonix.profitness.presentation.resetpassword

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordState(
    val isExchanging : Boolean = true,   // PKCE code exchange bekleniyor
    val isUpdating   : Boolean = false,  // Yeni şifre kaydediliyor
    val error        : String? = null,
    val isLinkInvalid: Boolean = false,  // Code geçersiz veya süresi dolmuş
)

sealed class ResetPasswordEvent {
    /** Şifre başarıyla güncellendi — Login ekranına yönlendir. */
    object Done : ResetPasswordEvent()
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<ResetPasswordState, ResetPasswordEvent>(ResetPasswordState()) {

    /**
     * Ekran ilk açıldığında çağrılır.
     * PKCE code → Supabase session dönüşümünü yapar; başarısız olursa hata gösterir.
     */
    fun exchangeCode(code: String) {
        viewModelScope.launch {
            authRepository.exchangeRecoveryCode(code)
                .onSuccess {
                    updateState { it.copy(isExchanging = false) }
                }
                .onFailure {
                    updateState { it.copy(isExchanging = false, isLinkInvalid = true) }
                }
        }
    }

    fun submit(password: String, confirmPassword: String) {
        if (password.length < 6) {
            updateState { it.copy(error = "Şifre en az 6 karakter olmalı.") }
            return
        }
        if (password != confirmPassword) {
            updateState { it.copy(error = "Şifreler eşleşmiyor.") }
            return
        }
        updateState { it.copy(isUpdating = true, error = null) }
        viewModelScope.launch {
            authRepository.updatePassword(password)
                .onSuccess {
                    // Recovery session'ı kapat; kullanıcı yeni şifresiyle giriş yapacak.
                    runCatching { authRepository.signOut() }
                    sendEvent(ResetPasswordEvent.Done)
                }
                .onFailure { err ->
                    updateState { it.copy(isUpdating = false, error = parseError(err.message)) }
                }
        }
    }

    fun clearError() = updateState { it.copy(error = null) }

    private fun parseError(message: String?): String = when {
        message == null -> "Şifre güncellenemedi."
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) -> "İnternet bağlantısını kontrol edin."
        message.contains("weak", ignoreCase = true)    -> "Şifre çok zayıf. En az 6 karakter kullanın."
        else -> "Şifre güncellenemedi. Tekrar dene."
    }
}
