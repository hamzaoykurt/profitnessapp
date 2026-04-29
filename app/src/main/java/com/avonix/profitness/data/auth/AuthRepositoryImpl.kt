package com.avonix.profitness.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.first
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
                // Supabase, signUpWith sonrası onaylanmamış kullanıcı için bile
                // geçici session oluşturur. Bunu temizliyoruz; kullanıcı OTP'yi
                // doğruladıktan sonra gerçek session açılacak.
                runCatching { supabase.auth.signOut() }
                Unit
            }
        }

    override suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { supabase.auth.signOut() }
        }

    override fun isLoggedIn(): Boolean {
        supabase.auth.currentSessionOrNull() ?: return false
        // Onaylanmamış kullanıcılar için Supabase geçici session oluşturur.
        // emailConfirmedAt null ise email henüz doğrulanmamış → login sayılmaz.
        val user = supabase.auth.currentUserOrNull() ?: return false
        return user.emailConfirmedAt != null
    }

    override suspend fun awaitSessionLoaded(): Boolean {
        // LoadingFromStorage bitene kadar bekle, ardından isLoggedIn() ile kontrol et.
        supabase.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage }
        return isLoggedIn()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // redirectUrl: Supabase token'ı doğruladıktan sonra uygulamayı açar.
                // AndroidManifest'teki intent-filter ile eşleşmeli.
                // Supabase Dashboard → Auth → URL Configuration → Redirect URLs listesine
                // "profitness://reset-password" eklenmiş olmalı.
                supabase.auth.resetPasswordForEmail(
                    email       = email,
                    redirectUrl = "profitness://reset-password"
                )
            }
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

    override suspend fun exchangeRecoveryCode(code: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.exchangeCodeForSession(code)
                Unit
            }
        }

    override suspend fun updatePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.auth.updateUser { password = newPassword }
                Unit
            }
        }
}
