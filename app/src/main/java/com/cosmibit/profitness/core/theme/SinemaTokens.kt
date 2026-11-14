package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * SİNEMA ortak tokenları (.ekip/yon-cam-siyah.md "Tek koyu tema"). TEK KOYU TEMA: saf siyah/beyaz yok,
 * karartma/gölge siyah değil sayfa rengi (+alfa). Bileşene hex gömülmez, buradan okunur.
 * `t` parametreleri geriye uyum için durur; değer temaya bakmaz.
 */
object Sinema {
    /** Yalnız vurgu üstü yazı rengi (cam tinti DEĞİL). */
    val Ink = Color(0xFF0E0F11)
    val White = Color.White

    // ── Tek koyu tema değerleri ────────────────────────────────────────────
    val Page = Color(0xFF0C0D10)
    val Surface = Color(0xFF15171C)
    val Elevated = Color(0xFF1D2027)
    val Text = Color(0xFFF4F5F7)
    val TextSecondary = Color(0xFFA6A9B2)
    val Line = Color.White.copy(0.09f)

    fun page(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = Page
    fun surface(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = Surface
    fun elevated(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = Elevated
    fun text(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = Text
    fun textSecondary(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = TextSecondary
    fun line(@Suppress("UNUSED_PARAMETER") t: AppThemeState) = Line

    /** Karartma rengi: sayfa rengi + alfa (siyah değil). */
    fun scrim(alpha: Float) = Page.copy(alpha = alpha)

    // ── Fotoğraf üstü karartma (alfalar kontrast hesabının dayanağı) ───────
    /** ALT: 0→%30H: 0 · %48H: .72 · %100H: .90 (sayfa rengi) */
    val BottomScrim: Brush get() = Brush.verticalGradient(
        0f to Color.Transparent,
        0.30f to Color.Transparent,
        0.48f to Page.copy(0.72f),
        1f to Page.copy(0.90f)
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

/**
 * Tek tipografi ölçeği (sp). Satır içi `fontSize = N.sp` yerine bunlar kullanılır; ağırlık/renk çağrı yerinde kalır.
 * Ekran başlığı ve dev rakamlar Sinema.ScreenTitle/Display/Metric + SinemaFitText ile kalır. Stiller: SinemaType.*Style.
 */
object SinemaType {
    val Caption = 12.sp
    val BodySm = 13.sp
    val Body = 14.sp
    val BodyLg = 16.sp
    val Title = 18.sp
    val Headline = 22.sp

    val CaptionStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = Caption, lineHeight = 16.sp)
    val BodySmStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = BodySm, lineHeight = 18.sp)
    val BodyStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = Body, lineHeight = 20.sp)
    val BodyLgStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = BodyLg, lineHeight = 24.sp)
    val TitleStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = Title, lineHeight = 24.sp)
    val HeadlineStyle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = Headline, lineHeight = 28.sp, letterSpacing = (-0.01).em)
}

/** Tek yarıçap ölçeği: 12 / 16 / 20 / 24 / 28 / kapsül. */
object SinemaRadius {
    val R12 = 12.dp
    val R16 = 16.dp
    val R20 = 20.dp
    val R24 = 24.dp
    val R28 = 28.dp
    val Pill = RoundedCornerShape(percent = 50)
}

/** Label stili BÜYÜK HARF: Türkçe için i/İ doğru dönüşümü. */
fun String.sinemaUpper(theme: AppThemeState): String =
    uppercase(if (theme.language == AppLanguage.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("tr-TR"))
