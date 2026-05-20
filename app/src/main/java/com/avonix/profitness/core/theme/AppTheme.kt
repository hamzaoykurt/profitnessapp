package com.avonix.profitness.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

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
    MINT  (Color(0xFF34D399), "MINT",   Color.Black, Color(0xFF059669), Color.White),
    SKY   (Color(0xFF38BDF8), "SKY",    Color.Black, Color(0xFF0284C7), Color.White),
    VIOLET(Color(0xFF8B5CF6), "VIOLET", Color.White, Color(0xFF7C3AED), Color.White),
    AMBER (Color(0xFFF59E0B), "AMBER",  Color.Black, Color(0xFFD97706), Color.White),
}

/** Karanlık tema arka plan tonu. */
enum class SurfaceStyle { CLASSIC, OLED, GRAPHITE }

/** Accent renginin doygunluk/parlaklık tercihi. */
enum class AccentIntensity { NEON, PASTEL, VIVID, SOFT }

enum class AppLanguage { TURKISH, ENGLISH }

@Immutable
data class AppThemeState(
    val isDark              : Boolean         = true,
    val accent              : AccentPreset    = AccentPreset.LIME,
    val surfaceStyle        : SurfaceStyle    = SurfaceStyle.OLED,
    val intensity           : AccentIntensity = AccentIntensity.NEON,
    val language            : AppLanguage     = AppLanguage.TURKISH,
    val notificationsEnabled: Boolean         = true,
    val customAccentArgb    : Int?            = null
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
private fun Color.toVivid(): Color {
    val boost = 0.18f
    return Color(
        red   = red   + (if (red > 0.5f) 1f - red else -red) * boost,
        green = green + (if (green > 0.5f) 1f - green else -green) * boost,
        blue  = blue  + (if (blue > 0.5f) 1f - blue else -blue) * boost,
        alpha = alpha
    )
}

private fun Color.toSoft(): Color {
    val blend = 0.22f
    return Color(
        red   = red   + (0.86f - red)   * blend,
        green = green + (0.86f - green) * blend,
        blue  = blue  + (0.86f - blue)  * blend,
        alpha = alpha
    )
}

private fun Color.blendToward(target: Color, amount: Float): Color =
    Color(
        red   = red   + (target.red - red)     * amount,
        green = green + (target.green - green) * amount,
        blue  = blue  + (target.blue - blue)   * amount,
        alpha = 1f
    )

private fun channelToLinear(value: Float): Double =
    if (value <= 0.03928f) value / 12.92 else Math.pow(((value + 0.055) / 1.055).toDouble(), 2.4)

private fun Color.relativeLuminance(): Double =
    0.2126 * channelToLinear(red) + 0.7152 * channelToLinear(green) + 0.0722 * channelToLinear(blue)

private fun Color.contrastRatio(other: Color): Double {
    val a = relativeLuminance()
    val b = other.relativeLuminance()
    val lighter = maxOf(a, b)
    val darker = minOf(a, b)
    return (lighter + 0.05) / (darker + 0.05)
}

fun Color.toSafeAccentColor(): Color {
    var adjusted = copy(alpha = 1f)
    repeat(18) {
        if (adjusted.contrastRatio(Color.Black) >= 3.0) return adjusted
        adjusted = adjusted.blendToward(Color.White, 0.10f)
    }
    return adjusted
}

fun Color.readableOnAccentColor(): Color =
    if (contrastRatio(Color.Black) >= contrastRatio(Color.White)) Color.Black else Color.White

fun Int.toSafeAccentArgb(): Int = Color(this).toSafeAccentColor().toArgb()

val AppThemeState.effectiveAccentColor: Color get() {
    val base = customAccentArgb?.let { Color(it) } ?: if (isDark) accent.color else accent.lightColor
    val styled = when (intensity) {
        AccentIntensity.NEON   -> base
        AccentIntensity.PASTEL -> base.toPastel()
        AccentIntensity.VIVID  -> base.toVivid()
        AccentIntensity.SOFT   -> base.toSoft()
    }
    return styled.toSafeAccentColor()
}
val AppThemeState.effectiveOnAccentColor: Color get() =
    effectiveAccentColor.readableOnAccentColor()

val AppThemeState.accentDisplayLabel: String get() =
    if (customAccentArgb != null) t("OZEL", "CUSTOM") else accent.label

val LocalAppTheme = compositionLocalOf { AppThemeState() }

// Pack all settings into a Long to ensure Bundle compatibility.
// Layout (LSB → MSB):
//   bit 0      : isDark
//   bits 1–4   : accent ordinal (4 bit → 16 preset)
//   bits 5–7   : language ordinal (3 bit)
//   bit 8      : notificationsEnabled
//   bits 9–10  : surfaceStyle ordinal (2 bit → 4 stil)
//   bit 11     : intensity ordinal (1 bit)
val AppThemeStateSaver = Saver<AppThemeState, List<Any?>>(
    save = { s ->
        listOf(
            s.isDark,
            s.accent.ordinal,
            s.language.ordinal,
            s.notificationsEnabled,
            s.intensity.ordinal,
            s.customAccentArgb
        )
    },
    restore = { values ->
        AppThemeState(
            isDark               = values.getOrNull(0) as? Boolean ?: true,
            accent               = AccentPreset.entries.getOrElse(values.getOrNull(1) as? Int ?: 0) { AccentPreset.LIME },
            language             = AppLanguage.entries.getOrElse(values.getOrNull(2) as? Int ?: 0) { AppLanguage.TURKISH },
            notificationsEnabled = values.getOrNull(3) as? Boolean ?: true,
            surfaceStyle         = SurfaceStyle.OLED,
            intensity            = AccentIntensity.entries.getOrElse(values.getOrNull(4) as? Int ?: 0) { AccentIntensity.NEON },
            customAccentArgb     = (values.getOrNull(5) as? Int)?.toSafeAccentArgb()
        )
    }
)
