package com.avonix.profitness.data.ai

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatSessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("oracle_sessions", Context.MODE_PRIVATE)
    private val json  = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val KEY_IDS      = "session_ids"
        private const val PREFIX       = "sess_"
        private const val MAX_SESSIONS = 50
    }

    fun getAllSessions(): List<ChatSession> =
        getIds().mapNotNull { getSession(it) }.sortedByDescending { it.updatedAt }

    fun getSession(id: String): ChatSession? =
        prefs.getString("$PREFIX$id", null)
            ?.let { runCatching { json.decodeFromString<ChatSession>(it) }.getOrNull() }

    fun save(session: ChatSession) {
        val ids = getIds().toMutableList()
        if (session.id !in ids) {
            ids.add(0, session.id)
            if (ids.size > MAX_SESSIONS) {
                prefs.edit().remove("$PREFIX${ids.removeLast()}").apply()
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
