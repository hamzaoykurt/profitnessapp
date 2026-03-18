package com.avonix.profitness.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
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
}
