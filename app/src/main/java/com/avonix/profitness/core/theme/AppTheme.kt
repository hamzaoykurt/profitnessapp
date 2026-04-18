package com.avonix.profitness.core.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color

enum class AccentPreset(
    val color      : Color,   // dark mode accent (neon)
    val label      : String,
    val onColor    : Color,
    val lightColor : Color,   // light mode accent (saturated, readable)
    val onLightColor: Color   // text on light accent
) {
    LIME  (Color(0xFFCBFF4D), "LIME",   Color.Black, Color(0xFF5C8A00), Color.White),
    PURPLE(Color(0xFFA855F7), "PURPLE", Color.White, Color(0xFF7C3AED), Color.White),
    CYAN  (Color(0xFF00E5D3), "CYAN",   Color.Black, Color(0xFF0891B2), Color.White),
    ORANGE(Color(0xFFF97316), "ORANGE", Color.Black, Color(0xFFEA580C), Color.White),
    PINK  (Color(0xFFEC4899), "PINK",   Color.White, Color(0xFFDB2777), Color.White),
    BLUE  (Color(0xFF3B82F6), "BLUE",   Color.White, Color(0xFF2563EB), Color.White),
    RED   (Color(0xFFEF4444), "RED",    Color.White, Color(0xFFDC2626), Color.White),
    YELLOW(Color(0xFFFACC15), "YELLOW", Color.Black, Color(0xFFCA8A04), Color.Black),
    GREEN (Color(0xFF22C55E), "GREEN",  Color.Black, Color(0xFF16A34A), Color.White),
    TEAL  (Color(0xFF14B8A6), "TEAL",   Color.Black, Color(0xFF0D9488), Color.White),
    INDIGO(Color(0xFF6366F1), "INDIGO", Color.White, Color(0xFF4F46E5), Color.White),
    ROSE  (Color(0xFFFB7185), "ROSE",   Color.Black, Color(0xFFE11D48), Color.White),
}

/** Karanlık tema arka plan tonu. */
enum class SurfaceStyle { CLASSIC, OLED, GRAPHITE }

/** Accent renginin doygunluk/parlaklık tercihi. */
enum class AccentIntensity { NEON, PASTEL }

enum class AppLanguage { TURKISH, ENGLISH }

data class AppThemeState(
    val isDark              : Boolean         = true,
    val accent              : AccentPreset    = AccentPreset.LIME,
    val surfaceStyle        : SurfaceStyle    = SurfaceStyle.CLASSIC,
    val intensity           : AccentIntensity = AccentIntensity.NEON,
    val language            : AppLanguage     = AppLanguage.TURKISH,
    val notificationsEnabled: Boolean         = true
)

// Surface helpers — dark (3 stil) / warm-earthy light
//
// OLED stratejisi: bg0 ve bg1 saf siyah (#000000) — ekran ve kart yüzeyi AMOLED
// pikselleri gerçekten kapatır. Derinlik, hafif yükseltilmiş bg2/bg3 + parlak
// stroke ile sağlanır. Bu, "near-black gri" kartların oluşturduğu "kirli"
// OLED hissini önler.
val AppThemeState.bg0: Color get() = when {
    !isDark                               -> Color(0xFFFAF8F5)
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF000000)
    surfaceStyle == SurfaceStyle.GRAPHITE -> Color(0xFF14141A)
    else                                  -> Color(0xFF0A0A0F)
}
val AppThemeState.bg1: Color get() = when {
    !isDark                               -> Color(0xFFF2EDE7)
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF000000)
    surfaceStyle == SurfaceStyle.GRAPHITE -> Color(0xFF1C1C23)
    else                                  -> Color(0xFF111117)
}
val AppThemeState.bg2: Color get() = when {
    !isDark                               -> Color(0xFFE8E0D5)
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF0A0A0F)
    surfaceStyle == SurfaceStyle.GRAPHITE -> Color(0xFF24242D)
    else                                  -> Color(0xFF18181F)
}
val AppThemeState.bg3: Color get() = when {
    !isDark                               -> Color(0xFFDDD2C2)
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF131319)
    surfaceStyle == SurfaceStyle.GRAPHITE -> Color(0xFF2E2E38)
    else                                  -> Color(0xFF21212A)
}
val AppThemeState.stroke: Color get() = when {
    !isDark                               -> Color(0xFFCEC0AD)
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF262632)   // parlak — saf siyahta görünür olsun
    surfaceStyle == SurfaceStyle.GRAPHITE -> Color(0xFF3A3A45)
    else                                  -> Color(0xFF2A2A35)
}
val AppThemeState.text0: Color get() = if (isDark) Color(0xFFF8F8F8) else Color(0xFF1A1410)
val AppThemeState.text1: Color get() = if (isDark) Color(0xFF9A9AB0) else Color(0xFF5C4E3E)
val AppThemeState.text2: Color get() = if (isDark) Color(0xFF5A5A72) else Color(0xFF9E8A72)

/** Pastel varyant: doygunluğu azaltır, koyu zeminde gözü yormaz. */
private fun Color.toPastel(): Color {
    // Beyaza %35 harmanla → daha soft, daha az doygun
    val blend = 0.35f
    return Color(
        red   = red   + (1f - red)   * blend,
        green = green + (1f - green) * blend,
        blue  = blue  + (1f - blue)  * blend,
        alpha = alpha
    )
}

// Effective accent — uses light variant when not in dark mode, pastel uygulanabilir
val AppThemeState.effectiveAccentColor: Color get() {
    val base = if (isDark) accent.color else accent.lightColor
    return if (intensity == AccentIntensity.PASTEL) base.toPastel() else base
}
val AppThemeState.effectiveOnAccentColor: Color get() =
    if (isDark) accent.onColor else accent.onLightColor

val LocalAppTheme = compositionLocalOf { AppThemeState() }

// Pack all settings into a Long to ensure Bundle compatibility.
// Layout (LSB → MSB):
//   bit 0      : isDark
//   bits 1–4   : accent ordinal (4 bit → 16 preset)
//   bits 5–7   : language ordinal (3 bit)
//   bit 8      : notificationsEnabled
//   bits 9–10  : surfaceStyle ordinal (2 bit → 4 stil)
//   bit 11     : intensity ordinal (1 bit)
val AppThemeStateSaver = Saver<AppThemeState, Long>(
    save = { s ->
        var v = 0L
        if (s.isDark) v = v or 1L
        v = v or (s.accent.ordinal.toLong() shl 1)
        v = v or (s.language.ordinal.toLong() shl 5)
        if (s.notificationsEnabled) v = v or (1L shl 8)
        v = v or (s.surfaceStyle.ordinal.toLong() shl 9)
        v = v or (s.intensity.ordinal.toLong() shl 11)
        v
    },
    restore = { v ->
        AppThemeState(
            isDark               = (v and 1L) != 0L,
            accent               = AccentPreset.entries[((v shr 1) and 0xF).toInt()],
            language             = AppLanguage.entries[((v shr 5) and 0x7).toInt()],
            notificationsEnabled = (v and (1L shl 8)) != 0L,
            surfaceStyle         = SurfaceStyle.entries[((v shr 9) and 0x3).toInt()],
            intensity            = AccentIntensity.entries[((v shr 11) and 0x1).toInt()]
        )
    }
)
