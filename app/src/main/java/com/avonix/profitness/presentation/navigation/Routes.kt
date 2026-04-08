package com.avonix.profitness.presentation.navigation

/**
 * Navigation route constants for Profitness App.
 */
object Routes {
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"

    /** Şifre sıfırlama ekranı — deep link'ten gelen PKCE code parametresi ile açılır. */
    const val RESET_PASSWORD = "reset_password/{code}"
    fun resetPassword(code: String) = "reset_password/$code"
}
