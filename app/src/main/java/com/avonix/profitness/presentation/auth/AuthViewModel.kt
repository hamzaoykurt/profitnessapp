package com.avonix.profitness.presentation.auth

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Screen state machine ──────────────────────────────────────────────────────
sealed class AuthFlowScreen {
    object Login         : AuthFlowScreen()
    object Register      : AuthFlowScreen()
    data class ForgotPassword(val prefillEmail: String = "") : AuthFlowScreen()
    /** OTP ekranı — kayıt sonrası 6 haneli kod doğrulaması */
    data class OtpVerify(val email: String) : AuthFlowScreen()
    data class EmailSent(val email: String, val type: EmailSentType) : AuthFlowScreen()
    /** Şifre sıfırlama deep link ile gelindi — yeni şifre girme ekranı */
    object NewPassword : AuthFlowScreen()
}

enum class EmailSentType { PasswordReset }

/**
 * Contextual action hint shown below the error message.
 * Guides the user toward the correct next step instead of leaving them stuck.
 */
sealed class AuthHint {
    /** "Bu email zaten kayıtlı → Giriş yap" */
    object SwitchToLogin : AuthHint()
    /** "Bu email bulunamadı → Kayıt ol" */
    object SwitchToRegister : AuthHint()
    /** "Şifreni mi unuttun?" — after wrong credentials */
    object ForgotPassword : AuthHint()
}

data class AuthState(
    val screen            : AuthFlowScreen = AuthFlowScreen.Login,
    val isLoading         : Boolean        = false,
    val otpLoading        : Boolean        = false,
    val error             : String?        = null,
    val hint              : AuthHint?      = null,
    val resendCooldown    : Boolean        = false,
    /** Supabase diskten session yüklenirken true — bu sürede auth UI gösterilmez. */
    val isSessionLoading  : Boolean        = true,
    /** Şifre sıfırlama deep linki işlenirken true — session init akışını bypass eder. */
    val isRecoveryPending : Boolean        = false,
)

sealed class AuthEvent {
    object NavigateToDashboard : AuthEvent()
    /** İlk kayıtta OTP doğrulaması sonrası — kullanıcı onboarding'e yönlendirilir */
    object NavigateToOnboarding : AuthEvent()
    /** Recovery deep linki alındı — şu an dashboard'da olsa bile AUTH rotasına git */
    object NavigateToAuthForRecovery : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<AuthState, AuthEvent>(AuthState()) {

    init {
        // Supabase session'ı diskten async yükler; bitince giriş yapıldıysa dashboard'a yönlendir.
        // Recovery deep linki işleniyorsa bu akış atlanır — recovery kendi yönlendirmesini yapar.
        viewModelScope.launch {
            val loggedIn = authRepository.awaitSessionLoaded()
            if (uiState.value.isRecoveryPending) return@launch
            if (loggedIn) {
                sendEvent(AuthEvent.NavigateToDashboard)
            } else {
                updateState { it.copy(isSessionLoading = false) }
            }
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    fun navigateTo(screen: AuthFlowScreen) {
        updateState { it.copy(screen = screen, error = null, hint = null) }
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    fun onLoginClick(email: String, password: String) {
        val trimmed = email.trim()
        if (!validateEmailPassword(trimmed, password)) return

        updateState { it.copy(isLoading = true, error = null, hint = null) }
        viewModelScope.launch {
            authRepository.signIn(trimmed, password)
                .onSuccess {
                    updateState { it.copy(isLoading = false) }
                    sendEvent(AuthEvent.NavigateToDashboard)
                }
                .onFailure { err ->
                    val msg = err.message ?: ""
                    val (errorText, hint) = resolveLoginFailure(msg, trimmed)
                    updateState { it.copy(isLoading = false, error = errorText, hint = hint) }
                }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    fun onRegisterClick(email: String, password: String, confirmPassword: String) {
        val trimmed = email.trim()
        if (!validateEmailPassword(trimmed, password)) return
        if (password != confirmPassword) {
            updateState { it.copy(error = "Şifreler eşleşmiyor.") }
            return
        }

        updateState { it.copy(isLoading = true, error = null, hint = null) }
        viewModelScope.launch {
            authRepository.signUp(trimmed, password)
                .onSuccess {
                    // Supabase email template must use {{ .Token }} to send a 6-digit OTP.
                    // Dashboard: Authentication → Email Templates → Confirm signup → use {{ .Token }}
                    updateState {
                        it.copy(
                            isLoading = false,
                            screen    = AuthFlowScreen.OtpVerify(trimmed),
                            error     = null,
                            hint      = null
                        )
                    }
                }
                .onFailure { err ->
                    val msg  = err.message ?: ""
                    val hint = if (msg.contains("User already registered", ignoreCase = true))
                        AuthHint.SwitchToLogin else null
                    updateState { it.copy(isLoading = false, error = parseRegisterError(msg), hint = hint) }
                }
        }
    }

    // ── OTP Verification ───────────────────────────────────────────────────────

    fun onVerifyOtp(email: String, code: String) {
        if (code.length != 6) {
            updateState { it.copy(error = "6 haneli kodu eksiksiz girin.") }
            return
        }
        updateState { it.copy(otpLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.verifyOtp(email, code)
                .onSuccess {
                    updateState { it.copy(otpLoading = false, screen = AuthFlowScreen.Login) }
                    sendEvent(AuthEvent.NavigateToOnboarding)
                }
                .onFailure { err ->
                    updateState { it.copy(otpLoading = false, error = parseOtpError(err.message)) }
                }
        }
    }

    fun onResendOtp(email: String) {
        if (uiState.value.resendCooldown) return
        updateState { it.copy(resendCooldown = true, error = null) }
        viewModelScope.launch {
            authRepository.resendOtp(email)
                .onFailure { err ->
                    updateState { it.copy(error = parseNetworkError(err.message)) }
                }
            kotlinx.coroutines.delay(30_000)
            updateState { it.copy(resendCooldown = false) }
        }
    }

    // ── Forgot Password ────────────────────────────────────────────────────────

    fun onForgotPasswordClick(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            updateState { it.copy(error = "Email adresinizi girin.") }
            return
        }
        if (!isValidEmail(trimmed)) {
            updateState { it.copy(error = "Geçerli bir email adresi girin.") }
            return
        }

        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(trimmed)
                .onSuccess {
                    // Supabase always returns success here regardless of whether email exists
                    // (prevents email enumeration). We mirror this — user sees the same screen.
                    updateState {
                        it.copy(
                            isLoading = false,
                            screen    = AuthFlowScreen.EmailSent(trimmed, EmailSentType.PasswordReset),
                            error     = null
                        )
                    }
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = parseNetworkError(err.message)) }
                }
        }
    }


    // ── Password Recovery (Deep Link) ─────────────────────────────────────────

    /**
     * Şifre sıfırlama e-postasındaki linke tıklanınca Android bu URL'i MainActivity'ye iletir.
     * Önce AUTH rotasına yönlendirme eventi gönderilir (kullanıcı dashboard'da olabilir),
     * ardından recovery session geri yüklenerek NewPassword ekranına geçilir.
     */
    fun onRecoveryLink(url: String) {
        sendEvent(AuthEvent.NavigateToAuthForRecovery)
        updateState { it.copy(isLoading = true, error = null, hint = null) }
        viewModelScope.launch {
            authRepository.restoreSessionFromUrl(url)
                .onSuccess {
                    updateState { it.copy(isLoading = false, screen = AuthFlowScreen.NewPassword) }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isLoading = false,
                            screen    = AuthFlowScreen.Login,
                            error     = "Bağlantı geçersiz veya süresi dolmuş. Tekrar şifre sıfırlama talebi gönder."
                        )
                    }
                }
        }
    }

    fun onNewPasswordSubmit(password: String, confirmPassword: String) {
        if (password.length < 6) {
            updateState { it.copy(error = "Şifre en az 6 karakter olmalı.") }
            return
        }
        if (password != confirmPassword) {
            updateState { it.copy(error = "Şifreler eşleşmiyor.") }
            return
        }
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.updatePassword(password)
                .onSuccess {
                    updateState { it.copy(isLoading = false) }
                    sendEvent(AuthEvent.NavigateToDashboard)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = parseUpdatePasswordError(err.message)) }
                }
        }
    }

    // ── Session ────────────────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun logout() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearError() {
        updateState { it.copy(error = null, hint = null) }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun validateEmailPassword(email: String, password: String): Boolean {
        return when {
            email.isBlank() || password.isBlank() -> {
                updateState { it.copy(error = "Lütfen tüm alanları doldurun.") }
                false
            }
            !isValidEmail(email) -> {
                updateState { it.copy(error = "Geçerli bir email adresi girin.") }
                false
            }
            password.length < 6 -> {
                updateState { it.copy(error = "Şifre en az 6 karakter olmalı.") }
                false
            }
            else -> true
        }
    }

    /**
     * Returns a user-facing error string and optional contextual hint for login failures.
     * Security note: we intentionally do NOT distinguish "wrong password" from "unknown email"
     * to prevent email enumeration. The hint for ForgotPassword is safe to show on any
     * credential failure since it only sends a reset link if the email exists.
     */
    private fun resolveLoginFailure(message: String, email: String): Pair<String, AuthHint?> {
        return when {
            message.contains("Email not confirmed", ignoreCase = true) ->
                "Email adresiniz doğrulanmamış. Gelen kutunuzu kontrol edin." to null
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Hatalı email veya şifre." to AuthHint.ForgotPassword
            message.contains("Unable to validate email", ignoreCase = true) ||
            message.contains("invalid format", ignoreCase = true) ->
                "Geçerli bir email adresi girin." to null
            message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("Unable to resolve", ignoreCase = true) ->
                "İnternet bağlantısını kontrol edin." to null
            else -> "Giriş yapılamadı. Tekrar dene." to null
        }
    }

    private fun parseRegisterError(message: String): String = when {
        message.contains("User already registered", ignoreCase = true) ->
            "Bu email adresi zaten kayıtlı."
        message.contains("Password should be at least", ignoreCase = true) ->
            "Şifre en az 6 karakter olmalı."
        message.contains("Unable to validate email", ignoreCase = true) ||
        message.contains("invalid format", ignoreCase = true) ->
            "Geçerli bir email adresi girin."
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."
        else -> "Kayıt olunamadı. Tekrar dene."
    }

    private fun parseOtpError(message: String?): String = when {
        message == null -> "Doğrulama başarısız."
        message.contains("expired", ignoreCase = true) ->
            "Kodun süresi dolmuş. Yeni kod gönder."
        message.contains("invalid", ignoreCase = true) ||
        message.contains("incorrect", ignoreCase = true) ->
            "Kod hatalı. Tekrar dene."
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."
        else -> "Kod doğrulanamadı. Tekrar dene."
    }

    private fun parseUpdatePasswordError(message: String?): String = when {
        message == null -> "Şifre güncellenemedi."
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."
        message.contains("weak", ignoreCase = true) ->
            "Şifre çok zayıf. En az 6 karakter kullanın."
        else -> "Şifre güncellenemedi. Tekrar dene."
    }

    private fun parseNetworkError(message: String?): String = when {
        message == null -> "Bir hata oluştu."
        message.contains("network", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."
        else -> "İşlem gerçekleştirilemedi. Tekrar dene."
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
