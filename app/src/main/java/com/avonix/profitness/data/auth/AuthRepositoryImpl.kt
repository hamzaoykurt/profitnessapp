package com.avonix.profitness.data.auth

import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.first
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@Serializable
private data class EmailCheckResult(val registered: Boolean)

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
                // ── Adım 1: Sadece ONAYLANMIŞ emaili engelle ─────────────────────
                // "OTP aldı ama doğrulamadı" senaryosu → email_confirmed_at NULL →
                // registered = false → tekrar kayıt izni (yeni OTP gönderilir).
                // "Daha önce doğrulamış" senaryosu → email_confirmed_at DOLU →
                // registered = true → "Zaten kayıtlı" hatası.
                val check = supabase.postgrest.rpc(
                    "check_email_registered",
                    buildJsonObject { put("p_email", email) }
                ).decodeSingle<EmailCheckResult>()

                if (check.registered) {
                    throw IllegalStateException("User already registered")
                }

                // ── Adım 2: Kayıt ─────────────────────────────────────────────────
                // Onaylanmamış kayıt varsa Supabase OTP'yi yeniden gönderir.
                // Yeni kayıtsa kullanıcı oluşturulur ve OTP gönderilir.
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

    override suspend fun restoreSessionFromUrl(url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(url)

                // PKCE akışı: Supabase ?code=xxx ile yönlendirir (varsayılan yeni proje davranışı)
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    supabase.auth.exchangeCodeForSession(code)
                    return@runCatching
                }

                // Implicit akış (eski): #access_token=X&refresh_token=Y&type=recovery
                val fragment = uri.fragment
                    ?: throw IllegalArgumentException("Geçersiz şifre sıfırlama bağlantısı.")
                val params = fragment.split("&").mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx > 0) part.substring(0, idx) to Uri.decode(part.substring(idx + 1)) else null
                }.toMap()

                val type = params["type"] ?: ""
                if (type != "recovery") throw IllegalArgumentException("Bu bir şifre sıfırlama linki değil.")

                val accessToken  = params["access_token"]  ?: throw IllegalArgumentException("access_token bulunamadı.")
                val refreshToken = params["refresh_token"] ?: throw IllegalArgumentException("refresh_token bulunamadı.")

                supabase.auth.importAuthToken(accessToken, refreshToken, autoRefresh = true)
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
