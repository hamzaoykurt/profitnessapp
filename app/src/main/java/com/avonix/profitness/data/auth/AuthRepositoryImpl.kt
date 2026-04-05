package com.avonix.profitness.data.auth

import android.net.Uri
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
                // supabase-kt 2.x: signUpWith doğrudan UserInfo döndürür.
                // Supabase "prevent email enumeration" açıkken kayıtlı bir e-posta için
                // 200 döner ama gelen UserInfo'nun identities listesi null veya boş olur.
                // Yeni kullanıcı için identities en az 1 eleman içerir.
                val userInfo = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                if (userInfo != null && userInfo.identities.isNullOrEmpty()) {
                    // Sahte başarı — email zaten kayıtlı
                    runCatching { supabase.auth.signOut() }
                    throw IllegalStateException("User already registered")
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

    override suspend fun restoreSessionFromUrl(url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Deep link formatı: profitness://reset-password#access_token=X&refresh_token=Y&type=recovery
                // Fragment'ı manuel parse edip supabase-kt'ye aktarıyoruz.
                val fragment = Uri.parse(url).fragment
                    ?: throw IllegalArgumentException("Recovery URL fragment boş.")
                val params = fragment.split("&").mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx > 0) part.substring(0, idx) to Uri.decode(part.substring(idx + 1)) else null
                }.toMap()

                val type = params["type"] ?: ""
                if (type != "recovery") throw IllegalArgumentException("Bu bir şifre sıfırlama linki değil.")

                val accessToken  = params["access_token"]  ?: throw IllegalArgumentException("access_token bulunamadı.")
                val refreshToken = params["refresh_token"] ?: throw IllegalArgumentException("refresh_token bulunamadı.")

                // supabase-kt 2.x — session'ı doğrudan içe aktar
                supabase.auth.importAuthToken(accessToken, refreshToken, autoRefresh = true)
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
