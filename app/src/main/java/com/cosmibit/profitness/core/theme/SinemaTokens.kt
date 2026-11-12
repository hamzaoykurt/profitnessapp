package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * SİNEMA ortak tokenları (.ekip/plan-sinema.md §0). Fotoğraf sayfaları temadan bağımsız koyudur;
 * düz zemin sayfaları temaya göre 000000/FFFFFF. Bileşene hex gömülmez, buradan okunur.
 */
object Sinema {
    val Ink = Color(0xFF0E0F11)          // cam tint tabanı
    val White = Color.White

    // ── Temaya göre düz zemin sayfaları ────────────────────────────────────
    fun page(t: AppThemeState) = if (t.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    fun surface(t: AppThemeState) = if (t.isDark) Color(0xFF141416) else Color(0xFFF2F3F5)
    fun text(t: AppThemeState) = if (t.isDark) Color(0xFFFFFFFF) else Color(0xFF0E0F11)
    fun textSecondary(t: AppThemeState) = if (t.isDark) Color(0xFFA1A1AA) else Color(0xFF5C5F66)
    fun line(t: AppThemeState) = if (t.isDark) Color.White.copy(0.12f) else Color(0xFF0E0F11).copy(0.10f)

    // ── Fotoğraf üstü karartma (alfalar kontrast hesabının dayanağı) ───────
    /** ALT: 0→%30H: 0 · %48H: .72 · %100H: .90 */
    val BottomScrim: Brush get() = Brush.verticalGradient(
        0f to Color.Transparent,
        0.30f to Color.Transparent,
        0.48f to Color.Black.copy(0.72f),
        1f to Color.Black.copy(0.90f)
    )
    const val TopScrimStrongAlpha = 0.70f
    const val TopScrimMidAlpha = 0.60f

    // ── Tipografi (Sora, italik yok, rakamlar tnum) ───────────────────────
    private const val TNUM = "tnum"
    val Display = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 104.sp,
        lineHeight = 0.95.em, letterSpacing = (-0.05).em, fontFeatureSettings = TNUM
    )
    val Title = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp,
        lineHeight = 1.1.em, letterSpacing = (-0.01).em, fontFeatureSettings = TNUM
    )
    val ScreenTitle = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp,
        lineHeight = 1.1.em, letterSpacing = (-0.02).em
    )
    val Metric = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp,
        lineHeight = 1.0.em, letterSpacing = (-0.04).em, fontFeatureSettings = TNUM
    )
    val Body = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    )
    val Label = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        lineHeight = 16.sp, letterSpacing = 0.10.em, fontFeatureSettings = TNUM
    )
    val Nav = TextStyle(
        fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp
    )

    // ── Hareket süreleri (ms) ──────────────────────────────────────────────
    const val PagerSettleMs = 320
    const val KenBurnsMs = 14000
    const val SweepMs = 500
    const val NavMs = 200
}

/** Label stili BÜYÜK HARF: Türkçe için i/İ doğru dönüşümü. */
fun String.sinemaUpper(theme: AppThemeState): String =
    uppercase(if (theme.language == AppLanguage.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("tr-TR"))
