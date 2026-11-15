package com.cosmibit.profitness.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * RENK SİSTEMİ v2 (.ekip/yon-renk-v2.md, A "Tonal"): TEK SABİT TEMA "Kor"; kullanıcı seçimi yok.
 * Tek ton ailesi: c1 (dokun) → press (basılı) → hi (ölç, aynı ton açık) → soft (c1 α.14 dolgu izi).
 * İşaretle = sabit kum-altın [Sinema.Gold] (yalnız ödül nesneleri), hata = [Sinema.Alert].
 * Token'lar yalnız buradan okunur; ileride tema eklemek = yeni bir [SinemaPalette] değeri + [SinemaPalette.Current] seçimi.
 */
@Immutable
data class SinemaPalette(
    val name: String,
    /** Dokun: birincil düğme, seçili taş/gün, açık anahtar, link. */
    val c1: Color,
    /** Basılı hal. */
    val press: Color,
    /** Ölç: ilerleme, grafik çizgisi, fark değeri, tamamlanan set/gün, AI kıvılcımı. */
    val hi: Color,
    /** Kart üstü dolgu izi (c1 α.14'ün #15171C üstü bileşkesi). */
    val soft: Color,
    /** SinemaBackdrop tepe ışığı alfası. */
    val glowAlpha: Float
) {
    companion object {
        val Kor = SinemaPalette(
            name = "Kor",
            c1 = Color(0xFFF98942),
            press = Color(0xFFE07227),
            hi = Color(0xFFFBD5C1),
            soft = Color(0xFF352721),
            glowAlpha = 0.16f
        )

        /** Uygulamanın tek teması. */
        val Current: SinemaPalette = Kor
    }
}

/** Etkin palet (tek sabit tema; kayıtlı eski renk tercihi yok sayılır). */
@Suppress("UnusedReceiverParameter")
val AppThemeState.palette: SinemaPalette get() = SinemaPalette.Current

/** c1 = DOKUN. [effectiveAccentColor] ile aynı değer. */
val AppThemeState.accentColor: Color get() = palette.c1
val AppThemeState.accentPress: Color get() = palette.press
/** hi = ÖLÇ (eski c2'nin yerini alır). */
val AppThemeState.accentHi: Color get() = palette.hi
val AppThemeState.accentSoft: Color get() = palette.soft
/** İşaretle = kum-altın; yalnız ödül (kupa, rekor, seri, XP, rütbe). Dolgu düğme/zemin/paragraf olmaz. */
@Suppress("UnusedReceiverParameter")
val AppThemeState.rewardGold: Color get() = Sinema.Gold

/** Metin varyantları: #1D2027 (en açık yüzey) üstünde ≥4.5 → Page/Surface üstünde de geçer. */
val AppThemeState.accentHiTextColor: Color get() = accentHi.lightenForTextContrast(Sinema.Elevated)
val AppThemeState.rewardGoldTextColor: Color get() = rewardGold.lightenForTextContrast(Sinema.Elevated)

// ── Geriye uyum köprüsü (çağrı yerleri rolüne göre ayrılınca silinecek) ─────────────────────────

@Deprecated("c2 kalktı: ölç rolü için accentHi kullan.", ReplaceWith("accentHi"))
val AppThemeState.accent2Color: Color get() = accentHi

@Deprecated("c2 kalktı: ölç rolü için accentHiTextColor kullan.", ReplaceWith("accentHiTextColor"))
val AppThemeState.accent2TextColor: Color get() = accentHiTextColor

/** GEÇİCİ: lila yan renk kalktı; şimdilik nötr ikincil metin/ikon rengine düşer. */
@Deprecated("c3 kalktı: ödülse rewardGold, değilse nötr (Sinema.TextSecondary / Sinema.Text) kullan.")
@Suppress("UnusedReceiverParameter")
val AppThemeState.accent3Color: Color get() = Sinema.TextSecondary

@Deprecated("c3 kalktı: ödülse rewardGoldTextColor, değilse nötr (Sinema.TextSecondary) kullan.")
@Suppress("UnusedReceiverParameter")
val AppThemeState.accent3TextColor: Color get() = Sinema.TextSecondary
