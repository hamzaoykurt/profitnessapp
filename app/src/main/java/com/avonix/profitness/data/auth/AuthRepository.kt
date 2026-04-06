package com.avonix.profitness.data.auth

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun isLoggedIn(): Boolean
    /** Supabase diskten session yüklenene kadar bekler; yüklendikten sonra giriş durumunu döner. */
    suspend fun awaitSessionLoaded(): Boolean
    suspend fun sendPasswordReset(email: String): Result<Unit>
    /** Kayıt sonrası gelen 6 haneli OTP kodunu doğrula. */
    suspend fun verifyOtp(email: String, code: String): Result<Unit>
    /** OTP kodunu yeniden gönder. */
    suspend fun resendOtp(email: String): Result<Unit>
    /**
     * Şifre sıfırlama deep link URL'inden recovery session'ı geri yükler.
     * URL formatı: profitness://reset-password#access_token=...&type=recovery
     */
    suspend fun restoreSessionFromUrl(url: String): Result<Unit>
    /** Recovery session aktifken kullanıcının şifresini günceller. */
    suspend fun updatePassword(newPassword: String): Result<Unit>
}
