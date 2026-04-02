package com.avonix.profitness.data.auth

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun isLoggedIn(): Boolean
    suspend fun sendPasswordReset(email: String): Result<Unit>
    /** Kayıt sonrası gelen 6 haneli OTP kodunu doğrula. */
    suspend fun verifyOtp(email: String, code: String): Result<Unit>
    /** OTP kodunu yeniden gönder. */
    suspend fun resendOtp(email: String): Result<Unit>
}
