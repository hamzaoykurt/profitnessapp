package com.avonix.profitness.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Unit
            }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Unit
            }
        }

    override suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { supabase.auth.signOut() }
        }

    override fun isLoggedIn(): Boolean =
        supabase.auth.currentSessionOrNull() != null

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { supabase.auth.resetPasswordForEmail(email) }
        }

    override suspend fun verifyOtp(email: String, code: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.verifyEmailOtp(
                    type  = OtpType.Email.SIGNUP,
                    email = email,
                    token = code
                )
                Unit
            }
        }

    override suspend fun resendOtp(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)
                Unit
            }
        }
}
