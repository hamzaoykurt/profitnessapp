package com.avonix.profitness.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "profitness_theme")

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_DARK    = booleanPreferencesKey("is_dark")
        val ACCENT_ORD = intPreferencesKey("accent_ordinal")
    }

    /** Emits the persisted [AppThemeState] (only isDark + accent; language/notifications handled elsewhere). */
    val themeFlow: Flow<AppThemeState> = context.themeDataStore.data.map { prefs ->
        val isDark     = prefs[Keys.IS_DARK]    ?: true
        val accentOrd  = prefs[Keys.ACCENT_ORD] ?: 0
        val accent     = AccentPreset.entries.getOrElse(accentOrd) { AccentPreset.LIME }
        AppThemeState(isDark = isDark, accent = accent)
    }

    suspend fun saveTheme(state: AppThemeState) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.IS_DARK]    = state.isDark
            prefs[Keys.ACCENT_ORD] = state.accent.ordinal
        }
    }
}
