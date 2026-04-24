package com.avonix.profitness.data.challenges

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.challengePrefsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "profitness_challenges_prefs")

/**
 * Local-only flags for event challenges (FAZ 7J).
 * "Skip daily program on event date" — stored as a Set of keys: "dateIso|challengeId".
 * Auto-cleans entries older than 30 days on every read.
 */
@Singleton
class ChallengePrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SKIPPED = stringSetPreferencesKey("skip_program_entries")
    }

    private fun key(dateIso: String, challengeId: String): String = "$dateIso|$challengeId"

    val skippedFlow: Flow<Set<String>> = context.challengePrefsDataStore.data.map {
        it[Keys.SKIPPED] ?: emptySet()
    }

    suspend fun isSkipped(dateIso: String, challengeId: String): Boolean {
        val cur = context.challengePrefsDataStore.data.first()[Keys.SKIPPED] ?: emptySet()
        return key(dateIso, challengeId) in cur
    }

    /** Returns true if at least one skip flag is set for the given date. */
    suspend fun isAnySkippedForDate(dateIso: String): Boolean {
        val cur = context.challengePrefsDataStore.data.first()[Keys.SKIPPED] ?: emptySet()
        val prefix = "$dateIso|"
        return cur.any { it.startsWith(prefix) }
    }

    suspend fun setSkipped(dateIso: String, challengeId: String, enabled: Boolean) {
        context.challengePrefsDataStore.edit { prefs ->
            val cur = (prefs[Keys.SKIPPED] ?: emptySet()).toMutableSet()
            val k = key(dateIso, challengeId)
            if (enabled) cur.add(k) else cur.remove(k)
            // Prune: keep only last 30 days.
            val cutoff = java.time.LocalDate.now().minusDays(30)
            val pruned = cur.filter { entry ->
                val iso = entry.substringBefore('|')
                runCatching { java.time.LocalDate.parse(iso) }
                    .getOrNull()
                    ?.isAfter(cutoff.minusDays(1)) ?: false
            }.toSet()
            prefs[Keys.SKIPPED] = pruned
        }
    }
}
