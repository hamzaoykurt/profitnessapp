package com.avonix.profitness.domain.challenges

import java.net.URI

private const val MAX_ONLINE_EVENT_URL_LENGTH = 2048

fun normalizeOnlineEventUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.length > MAX_ONLINE_EVENT_URL_LENGTH) return null
    if (value.any { it.isISOControl() || it.isWhitespace() }) return null

    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "https" && scheme != "http") return null
    if (uri.host.isNullOrBlank()) return null
    if (!uri.userInfo.isNullOrBlank()) return null

    return uri.toASCIIString()
}

fun isValidOnlineEventUrl(raw: String?): Boolean =
    normalizeOnlineEventUrl(raw) != null
