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
        val IS_DARK       = booleanPreferencesKey("is_dark")
        val ACCENT_ORD    = intPreferencesKey("accent_ordinal")
        val SURFACE_ORD   = intPreferencesKey("surface_style_ordinal")
        val INTENSITY_ORD = intPreferencesKey("intensity_ordinal")
    }

    /** Emits the persisted [AppThemeState] (language/notifications handled elsewhere). */
    val themeFlow: Flow<AppThemeState> = context.themeDataStore.data.map { prefs ->
        val isDark        = prefs[Keys.IS_DARK]       ?: true
        val accentOrd     = prefs[Keys.ACCENT_ORD]    ?: 0
        val surfaceOrd    = prefs[Keys.SURFACE_ORD]   ?: 0
        val intensityOrd  = prefs[Keys.INTENSITY_ORD] ?: 0
        val accent        = AccentPreset.entries.getOrElse(accentOrd)    { AccentPreset.LIME }
        val surfaceStyle  = SurfaceStyle.entries.getOrElse(surfaceOrd)   { SurfaceStyle.CLASSIC }
        val intensity     = AccentIntensity.entries.getOrElse(intensityOrd) { AccentIntensity.NEON }
        AppThemeState(
            isDark       = isDark,
            accent       = accent,
            surfaceStyle = surfaceStyle,
            intensity    = intensity
        )
    }

    suspend fun saveTheme(state: AppThemeState) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.IS_DARK]       = state.isDark
            prefs[Keys.ACCENT_ORD]    = state.accent.ordinal
            prefs[Keys.SURFACE_ORD]   = state.surfaceStyle.ordinal
            prefs[Keys.INTENSITY_ORD] = state.intensity.ordinal
        }
    }
}
