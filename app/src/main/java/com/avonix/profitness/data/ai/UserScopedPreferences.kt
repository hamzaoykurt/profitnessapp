package com.avonix.profitness.data.ai

import java.security.MessageDigest
import java.util.Locale

internal object UserScopedPreferences {
    fun name(prefix: String, userId: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                String.format(Locale.US, "%02x", byte.toInt() and 0xff)
            }

        return "$prefix$digest"
    }
}
