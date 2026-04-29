package com.avonix.profitness.core.security

fun Throwable?.toUserSafeMessage(fallback: String = "İşlem gerçekleştirilemedi. Tekrar dene."): String {
    val message = this?.message ?: return fallback
    return message.toUserSafeMessage(fallback)
}

fun String?.toUserSafeMessage(fallback: String = "İşlem gerçekleştirilemedi. Tekrar dene."): String {
    val message = this ?: return fallback
    return when {
        message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("Unable to resolve", ignoreCase = true) ->
            "İnternet bağlantısını kontrol edin."

        message.contains("unauthenticated", ignoreCase = true) ||
            message.contains("not authenticated", ignoreCase = true) ||
            message.contains("JWT", ignoreCase = true) ->
            "Oturum süreniz dolmuş olabilir. Tekrar giriş yapın."

        message.contains("permission", ignoreCase = true) ||
            message.contains("policy", ignoreCase = true) ||
            message.contains("forbidden", ignoreCase = true) ||
            message.contains("not_owner", ignoreCase = true) ->
            "Bu işlem için yetkiniz yok."

        else -> fallback
    }
}
