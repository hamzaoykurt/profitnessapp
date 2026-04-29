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

    fun getAllSessions(userId: String): List<ChatSession> =
        getIds(userId).mapNotNull { getSession(userId, it) }.sortedByDescending { it.updatedAt }

    fun getSession(userId: String, id: String): ChatSession? =
        prefs.getString(sessionKey(userId, id), null)
            ?.let { runCatching { json.decodeFromString<ChatSession>(it) }.getOrNull() }

    fun save(userId: String, session: ChatSession) {
        val ids = getIds(userId).toMutableList()
        if (session.id !in ids) {
            ids.add(0, session.id)
            if (ids.size > MAX_SESSIONS) {
                val removedId = ids.removeAt(ids.lastIndex)
                prefs.edit().remove(sessionKey(userId, removedId)).apply()
            }
        }
        prefs.edit()
            .putString(idsKey(userId), json.encodeToString(ids))
            .putString(sessionKey(userId, session.id), json.encodeToString(session))
            .apply()
    }

    fun delete(userId: String, id: String) {
        val ids = getIds(userId).toMutableList().also { it.remove(id) }
        prefs.edit()
            .putString(idsKey(userId), json.encodeToString(ids))
            .remove(sessionKey(userId, id))
            .apply()
    }

    private fun getIds(userId: String): List<String> =
        prefs.getString(idsKey(userId), null)
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()

    private fun idsKey(userId: String): String = "${KEY_IDS}_${safeUserId(userId)}"

    private fun sessionKey(userId: String, id: String): String =
        "$PREFIX${safeUserId(userId)}_$id"

    private fun safeUserId(userId: String): String =
        userId.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
