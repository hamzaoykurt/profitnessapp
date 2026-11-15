package com.cosmibit.profitness.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Karanlık tema arka plan tonu. */
enum class SurfaceStyle { CLASSIC, OLED, GRAPHITE }

enum class AppLanguage { TURKISH, ENGLISH }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Immutable
data class AppThemeState(
    val isDark              : Boolean         = true,
    val themeMode           : ThemeMode       = ThemeMode.DARK,
    val surfaceStyle        : SurfaceStyle    = SurfaceStyle.GRAPHITE,
    val language            : AppLanguage     = AppLanguage.TURKISH,
    val notificationsEnabled: Boolean         = true
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
/** Tek ikincil gri (yon-renk-v2: #9A9DA6 → #A6A9B2). */
val AppThemeState.text2: Color get() = Sinema.TextSecondary

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

/** c1 (dokun): tek sabit tema; kayıtlı eski preset/yoğunluk/özel renk yok sayılır. Yeni kodda [accentColor]. */
val AppThemeState.effectiveAccentColor: Color get() = palette.c1
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

/** c1 dolgu üstü yazı: her zaman koyu mürekkep #0E0F11 (beyaz-üstü-vurgu yok). */
@Suppress("UnusedReceiverParameter")
val AppThemeState.effectiveOnAccentColor: Color get() = Sinema.Ink

val LocalAppTheme = compositionLocalOf { AppThemeState() }

// rememberSaveable için liste. Konumlar eski düzenle aynı tutulur (1, 4, 5 = kalkan vurgu alanları; boş).
val AppThemeStateSaver = Saver<AppThemeState, List<Any?>>(
    save = { s ->
        listOf(
            s.isDark,
            0,
            s.language.ordinal,
            s.notificationsEnabled,
            0,
            null,
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
            language             = AppLanguage.entries.getOrElse(values.getOrNull(2) as? Int ?: 0) { AppLanguage.TURKISH },
            notificationsEnabled = values.getOrNull(3) as? Boolean ?: true,
            surfaceStyle         = SurfaceStyle.GRAPHITE
        )
    }
)

// ── Destek renkleri (yon-renk-v2): Success/Rest/Strength → accentHi (ölç), Reward → rewardGold; Alert sabit ──────────
/** Başarı / tamamlandı (ayrı yeşil yok: hi + onay ikonu). */
val AppThemeState.supportSuccess: Color get() = accentHi
/** Dinlenme / kardiyo / su. */
val AppThemeState.supportRest: Color get() = accentHi
/** Kuvvet / hacim / kas. */
val AppThemeState.supportStrength: Color get() = accentHi
/** Ödül: XP / rütbe / seri değeri. */
val AppThemeState.supportReward: Color get() = rewardGold
/** Uyarı / hata / atlandı. */
@Suppress("UnusedReceiverParameter")
val AppThemeState.supportAlert: Color get() = Sinema.Alert
/** Dolgu (çip/satır) için yumuşak ton α.16. */
@Suppress("UnusedReceiverParameter")
val AppThemeState.supportSoftAlpha: Float get() = 0.16f
fun AppThemeState.soft(support: Color): Color = support.copy(alpha = supportSoftAlpha)
