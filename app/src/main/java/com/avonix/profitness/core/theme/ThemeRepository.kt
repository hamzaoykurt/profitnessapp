package com.avonix.profitness.core.theme

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
        val ACCENT_ORD    = intPreferencesKey("accent_ordinal")
        val SURFACE_ORD   = intPreferencesKey("surface_style_ordinal")
        val INTENSITY_ORD = intPreferencesKey("intensity_ordinal")
        val LANGUAGE_ORD  = intPreferencesKey("language_ordinal")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val CUSTOM_ACCENT = intPreferencesKey("custom_accent_argb")
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
            val accentOrd     = prefs[Keys.ACCENT_ORD]    ?: 0
            val intensityOrd  = prefs[Keys.INTENSITY_ORD] ?: 0
            val languageOrd   = prefs[Keys.LANGUAGE_ORD]  ?: 0
            val notifications = prefs[Keys.NOTIFICATIONS] ?: true
            val customAccent  = prefs[Keys.CUSTOM_ACCENT]?.toSafeAccentArgb()
            val accent        = AccentPreset.entries.getOrElse(accentOrd)    { AccentPreset.LIME }
            val intensity     = AccentIntensity.entries.getOrElse(intensityOrd) { AccentIntensity.NEON }
            val language      = AppLanguage.entries.getOrElse(languageOrd) { AppLanguage.TURKISH }
            AppThemeState(
                isDark               = isDark,
                accent               = accent,
                surfaceStyle         = SurfaceStyle.OLED,
                intensity            = intensity,
                language             = language,
                notificationsEnabled = notifications,
                customAccentArgb     = customAccent
            )
        }

    suspend fun saveTheme(state: AppThemeState) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.IS_DARK]       = state.isDark
            prefs[Keys.ACCENT_ORD]    = state.accent.ordinal
            prefs[Keys.SURFACE_ORD]   = SurfaceStyle.OLED.ordinal
            prefs[Keys.INTENSITY_ORD] = state.intensity.ordinal
            prefs[Keys.LANGUAGE_ORD]  = state.language.ordinal
            prefs[Keys.NOTIFICATIONS] = state.notificationsEnabled
            val customAccent = state.customAccentArgb
            if (customAccent == null) {
                prefs.remove(Keys.CUSTOM_ACCENT)
            } else {
                prefs[Keys.CUSTOM_ACCENT] = customAccent.toSafeAccentArgb()
            }
        }
    }
}
