package com.avonix.profitness.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id        : String,
    val title     : String,
    val history   : List<HistoryEntry>,     // Gemini API geçmişi ("user"/"model")
    val messages  : List<StoredMessage>,    // UI mesajları
    val createdAt : Long,
    val updatedAt : Long
)

@Serializable
data class HistoryEntry(
    val role: String,   // "user" veya "model"
    val text: String
)

@Serializable
data class StoredMessage(
    val id        : String,
    val text      : String,
    val isUser    : Boolean,
    val timestamp : String
)
