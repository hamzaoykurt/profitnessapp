package com.avonix.profitness.core.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color

enum class AccentPreset(val color: Color, val label: String, val onColor: Color) {
    LIME  (Color(0xFFCBFF4D), "LIME",   Color.Black),
    PURPLE(Color(0xFFA855F7), "PURPLE", Color.White),
    CYAN  (Color(0xFF00E5D3), "CYAN",   Color.Black),
    ORANGE(Color(0xFFF97316), "ORANGE", Color.Black),
    PINK  (Color(0xFFEC4899), "PINK",   Color.White),
    BLUE  (Color(0xFF3B82F6), "BLUE",   Color.White),
}

data class AppThemeState(
    val isDark: Boolean = true,
    val accent: AccentPreset = AccentPreset.LIME
)

// Surface helpers — dark/light
val AppThemeState.bg0:    Color get() = if (isDark) Color(0xFF0A0A0F) else Color(0xFFF5F5FA)
val AppThemeState.bg1:    Color get() = if (isDark) Color(0xFF111117) else Color(0xFFEDEDF4)
val AppThemeState.bg2:    Color get() = if (isDark) Color(0xFF18181F) else Color(0xFFE4E4EE)
val AppThemeState.bg3:    Color get() = if (isDark) Color(0xFF21212A) else Color(0xFFDADAE8)
val AppThemeState.stroke: Color get() = if (isDark) Color(0xFF2A2A35) else Color(0xFFCCCCDC)
val AppThemeState.text0:  Color get() = if (isDark) Color(0xFFF8F8F8) else Color(0xFF0F0F14)
val AppThemeState.text1:  Color get() = if (isDark) Color(0xFF9A9AB0) else Color(0xFF55556A)
val AppThemeState.text2:  Color get() = if (isDark) Color(0xFF5A5A72) else Color(0xFF9A9AB0)

val LocalAppTheme = compositionLocalOf { AppThemeState() }

val AppThemeStateSaver = Saver<AppThemeState, Pair<Boolean, Int>>(
    save    = { Pair(it.isDark, it.accent.ordinal) },
    restore = { AppThemeState(it.first, AccentPreset.entries[it.second]) }
)
