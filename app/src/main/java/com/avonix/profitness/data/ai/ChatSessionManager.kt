package com.avonix.profitness.data.ai

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatSessionManager(context: Context, private val userId: String) {

    private val prefs = context.getSharedPreferences(
        UserScopedPreferences.name(PREFS_PREFIX, userId),
        Context.MODE_PRIVATE
    )
    private val json  = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val LEGACY_PREFS_NAME = "oracle_sessions"
        private const val PREFS_PREFIX  = "oracle_sessions_"
        private const val KEY_IDS      = "session_ids"
        private const val PREFIX       = "sess_"
        private const val MAX_SESSIONS = 50

        fun clearLegacySessions(context: Context) {
            context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    fun getAllSessions(): List<ChatSession> =
        getIds().mapNotNull { getSession(it) }.sortedByDescending { it.updatedAt }

    fun getSession(id: String): ChatSession? =
        prefs.getString("$PREFIX$id", null)
            ?.let { runCatching { json.decodeFromString<ChatSession>(it) }.getOrNull() }
            ?.takeIf { it.userId == userId }

    fun save(session: ChatSession) {
        require(session.userId == userId) { "Cannot save Oracle chat session for a different user." }

        val ids = getIds().toMutableList()
        if (session.id !in ids) {
            ids.add(0, session.id)
            if (ids.size > MAX_SESSIONS) {
                val evictedId = ids.removeAt(ids.lastIndex)
                prefs.edit().remove("$PREFIX$evictedId").apply()
            }
        }
        prefs.edit()
            .putString(KEY_IDS, json.encodeToString(ids))
            .putString("$PREFIX${session.id}", json.encodeToString(session))
            .apply()
    }

    fun delete(id: String) {
        val ids = getIds().toMutableList().also { it.remove(id) }
        prefs.edit()
            .putString(KEY_IDS, json.encodeToString(ids))
            .remove("$PREFIX$id")
            .apply()
    }

    private fun getIds(): List<String> =
        prefs.getString(KEY_IDS, null)
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()
}
