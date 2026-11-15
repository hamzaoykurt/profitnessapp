package com.cosmibit.profitness.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import kotlinx.coroutines.flow.catch
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
        val IS_DARK       = booleanPreferencesKey("is_dark")
        val THEME_MODE    = intPreferencesKey("theme_mode")
        val SURFACE_ORD   = intPreferencesKey("surface_style_ordinal")
        val LANGUAGE_ORD  = intPreferencesKey("language_ordinal")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        // Renk v2 (tek sabit tema): eski "accent_ordinal", "intensity_ordinal", "custom_accent_argb"
        // anahtarları depoda olduğu gibi KALIR (silinmez, yazılmaz), okunmaz → yok sayılır.
    }

    /** Emits the persisted [AppThemeState]. */
    val themeFlow: Flow<AppThemeState> = context.themeDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val isDark        = prefs[Keys.IS_DARK]       ?: true
            val themeMode     = prefs[Keys.THEME_MODE]?.let { ordinal ->
                ThemeMode.entries.getOrElse(ordinal) { ThemeMode.DARK }
            } ?: if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
            val languageOrd   = prefs[Keys.LANGUAGE_ORD]  ?: 0
            val notifications = prefs[Keys.NOTIFICATIONS] ?: true
            val language      = AppLanguage.entries.getOrElse(languageOrd) { AppLanguage.TURKISH }
            AppThemeState(
                isDark               = isDark,
                themeMode            = themeMode,
                surfaceStyle         = SurfaceStyle.GRAPHITE,
                language             = language,
                notificationsEnabled = notifications
            )
        }

    suspend fun saveTheme(state: AppThemeState) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.IS_DARK]       = state.isDark
            prefs[Keys.THEME_MODE]    = state.themeMode.ordinal
            prefs[Keys.SURFACE_ORD]   = SurfaceStyle.GRAPHITE.ordinal
            prefs[Keys.LANGUAGE_ORD]  = state.language.ordinal
            prefs[Keys.NOTIFICATIONS] = state.notificationsEnabled
        }
    }
}
