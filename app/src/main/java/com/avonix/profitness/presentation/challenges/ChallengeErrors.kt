package com.avonix.profitness.presentation.challenges

internal fun Throwable.toChallengeUiMessage(
    fallback: String = "İşlem tamamlanamadı. Lütfen tekrar dene."
): String {
    val raw = message.orEmpty()
    val lower = raw.lowercase()

    return when {
        raw.isBlank() -> fallback
        "not_authenticated" in lower -> "Oturum süren dolmuş olabilir. Tekrar giriş yap."
        "invalid_title" in lower -> "Başlık 3-120 karakter arasında olmalı."
        "invalid_target_value" in lower -> "Hedef sıfırdan büyük olmalı."
        "invalid_event_mode" in lower -> "Etkinlik tipi geçersiz."
        "invalid_visibility" in lower -> "Görünürlük seçimi geçersiz."
        "physical_location_required" in lower -> "Fiziksel etkinlik için başlangıç konumu gerekli."
        "online_url_required" in lower -> "Online etkinlik için bağlantı gerekli."
        "group_challenges_event_mode_fields_chk" in lower -> "Fiziksel etkinlik için başlangıç konumu gerekli."
        "event_location" in lower && "physical" in lower -> "Fiziksel etkinlik için başlangıç konumu gerekli."
        containsSensitiveTransportDetails(lower) -> fallback
        else -> fallback
    }
}

private fun containsSensitiveTransportDetails(message: String): Boolean =
    listOf(
        "authorization",
        "bearer",
        "apikey",
        "headers",
        "http method",
        "supabase.co",
        "/rest/v1/rpc",
        "jwt",
        "url:"
    ).any { it in message }
