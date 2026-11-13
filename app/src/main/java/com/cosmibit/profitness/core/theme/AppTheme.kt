package com.cosmibit.profitness.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class AccentPreset(
    val color      : Color,   // dark mode accent (neon)
    val label      : String,
    val onColor    : Color
) {
    LIME  (Color(0xFFCBFF4D), "LIME",   Color.Black),
    PURPLE(Color(0xFFA855F7), "PURPLE", Color.White),
    CYAN  (Color(0xFF00E5D3), "CYAN",   Color.Black),
    ORANGE(Color(0xFFF36A21), "ORANGE", Color.Black),
    PINK  (Color(0xFFEC4899), "PINK",   Color.White),
    BLUE  (Color(0xFF3B82F6), "BLUE",   Color.White),
    RED   (Color(0xFFEF4444), "RED",    Color.White),
    YELLOW(Color(0xFFFACC15), "YELLOW", Color.Black),
    GREEN (Color(0xFF22C55E), "GREEN",  Color.Black),
    TEAL  (Color(0xFF14B8A6), "TEAL",   Color.Black),
    INDIGO(Color(0xFF6366F1), "INDIGO", Color.White),
    ROSE  (Color(0xFFFB7185), "ROSE",   Color.Black),
    MINT  (Color(0xFF34D399), "MINT",   Color.Black),
    SKY   (Color(0xFF38BDF8), "SKY",    Color.Black),
    VIOLET(Color(0xFF8B5CF6), "VIOLET", Color.White),
    AMBER (Color(0xFFF59E0B), "AMBER",  Color.Black),
}

/** Karanlık tema arka plan tonu. */
enum class SurfaceStyle { CLASSIC, OLED, GRAPHITE }

/** Accent renginin doygunluk/parlaklık tercihi. */
enum class AccentIntensity { NEON, PASTEL, VIVID, SOFT }

enum class AppLanguage { TURKISH, ENGLISH }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Immutable
data class AppThemeState(
    val isDark              : Boolean         = true,
    val themeMode           : ThemeMode       = ThemeMode.DARK,
    val accent              : AccentPreset    = AccentPreset.ORANGE,
    val surfaceStyle        : SurfaceStyle    = SurfaceStyle.GRAPHITE,
    val intensity           : AccentIntensity = AccentIntensity.SOFT,
    val language            : AppLanguage     = AppLanguage.TURKISH,
    val notificationsEnabled: Boolean         = true,
    val customAccentArgb    : Int?            = null
)

// Surface helpers — dark forged depth.
//
// OLED stratejisi: bg0 ve bg1 saf siyah (#000000) — ekran ve kart yüzeyi AMOLED
// pikselleri gerçekten kapatır. Derinlik, hafif yükseltilmiş bg2/bg3 + parlak
// stroke ile sağlanır. Bu, "near-black gri" kartların oluşturduğu "kirli"
// OLED hissini önler.
val AppThemeState.bg0: Color get() = when {
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF010203)
    else                                  -> Sinema.Page      // #0C0D10
}
val AppThemeState.bg1: Color get() = when {
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF050609)
    else                                  -> Sinema.Surface   // #15171C
}
val AppThemeState.bg2: Color get() = when {
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF0C0F14)
    else                                  -> Sinema.Elevated  // #1D2027
}
val AppThemeState.bg3: Color get() = when {
    surfaceStyle == SurfaceStyle.OLED     -> Color(0xFF151A21)
    else                                  -> Color(0xFF262A32)
}
val AppThemeState.stroke: Color get() = when {
    else                                  -> Color(0xFF26282D)   // beyaz .09 / kart bileşkesi (opak: .copy(alpha) çağrıları için)
}
val AppThemeState.text0: Color get() = Sinema.Text
val AppThemeState.text1: Color get() = Sinema.TextSecondary
val AppThemeState.text2: Color get() = Color(0xFF9A9DA6)

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

// Effective accent — vurgu yoğunluğu stilleri
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

/**
 * Koyulaştırarak [background] üstünde en az [minRatio] kontrast sağlar (siyaha doğru).
 * Açık temada vurgu renginin METİN olarak kullanımı içindir; dolgu/ikon rengi değişmez.
 */
fun Color.darkenForContrast(background: Color, minRatio: Double = 4.5): Color {
    var adjusted = copy(alpha = 1f)
    repeat(40) {
        if (adjusted.contrastRatio(background) >= minRatio) return adjusted
        adjusted = adjusted.blendToward(Color.Black, 0.06f)
    }
    return adjusted
}

fun Int.toSafeAccentArgb(): Int = Color(this).toSafeAccentColor().toArgb()

val AppThemeState.effectiveAccentColor: Color get() {
    val base = customAccentArgb?.let { Color(it) } ?: accent.color
    val styled = when (intensity) {
        AccentIntensity.NEON   -> base
        AccentIntensity.PASTEL -> base.toPastel()
        AccentIntensity.VIVID  -> base.toVivid()
        AccentIntensity.SOFT   -> base.toSoft()
    }
    return styled.toSafeAccentColor()
}
/** Vurgu rengiyle yazılan etiket/link metni: tek koyu temada vurgu rengi. */
val AppThemeState.accentTextColor: Color get() =
    effectiveAccentColor.lightenForTextContrast(Sinema.Elevated)

/** Aydınlatarak [background] üstünde küçük metin kontrastı (>=4.5) sağlar; yalnız metin rengi içindir. */
fun Color.lightenForTextContrast(background: Color, minRatio: Double = 4.5): Color {
    var adjusted = copy(alpha = 1f)
    repeat(30) {
        if (adjusted.contrastRatio(background) >= minRatio) return adjusted
        adjusted = adjusted.blendToward(Color.White, 0.05f)
    }
    return adjusted
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
            s.customAccentArgb,
            s.themeMode.ordinal
        )
    },
    restore = { values ->
        AppThemeState(
            isDark               = values.getOrNull(0) as? Boolean ?: true,
            themeMode            = ThemeMode.entries.getOrElse(
                values.getOrNull(6) as? Int
                    ?: if (values.getOrNull(0) as? Boolean ?: true) ThemeMode.DARK.ordinal else ThemeMode.LIGHT.ordinal
            ) { ThemeMode.DARK },
            accent               = AccentPreset.entries.getOrElse(values.getOrNull(1) as? Int ?: AccentPreset.ORANGE.ordinal) { AccentPreset.ORANGE },
            language             = AppLanguage.entries.getOrElse(values.getOrNull(2) as? Int ?: 0) { AppLanguage.TURKISH },
            notificationsEnabled = values.getOrNull(3) as? Boolean ?: true,
            surfaceStyle         = SurfaceStyle.GRAPHITE,
            intensity            = AccentIntensity.entries.getOrElse(values.getOrNull(4) as? Int ?: AccentIntensity.SOFT.ordinal) { AccentIntensity.SOFT },
            customAccentArgb     = (values.getOrNull(5) as? Int)?.toSafeAccentArgb()
        )
    }
)

// ── Destek renkleri: vurgu preset'inden bağımsız 5 sabit anlam rengi ──────────
/** Başarı / tamamlandı. */
val AppThemeState.supportSuccess: Color get() = Color(0xFF46CC8C)
/** Dinlenme / kardiyo / su. */
val AppThemeState.supportRest: Color get() = Color(0xFF3DB5E8)
/** Kuvvet / hacim / kas. */
val AppThemeState.supportStrength: Color get() = Color(0xFFA48BF7)
/** Ödül: XP / rütbe / seri değeri. */
val AppThemeState.supportReward: Color get() = Color(0xFFF2C23F)
/** Uyarı / hata / atlandı. */
val AppThemeState.supportAlert: Color get() = Color(0xFFF4748A)
/** Dolgu (çip/satır) için yumuşak ton: koyu α.16, açık α.12. */
val AppThemeState.supportSoftAlpha: Float get() = 0.16f
fun AppThemeState.soft(support: Color): Color = support.copy(alpha = supportSoftAlpha)
