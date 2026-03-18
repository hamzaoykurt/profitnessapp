package com.avonix.profitness.data.auth

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun isLoggedIn(): Boolean
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
