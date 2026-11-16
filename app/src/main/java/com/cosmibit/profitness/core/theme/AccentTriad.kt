package com.cosmibit.profitness.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * RENK SİSTEMİ v3 (.ekip/mockup/palet.html P1 "Güneş + Mercan"): TEK SABİT TEMA; kullanıcı seçimi yok.
 * c1 = güneş sarısı (dokun: ana eylem/seçili), c2 = sıcak mercan (hi: ölçülenler, ikon, çip kenarı; DOLGU değil),
 * press = c1 biraz koyu, soft = c1 α.14 dolgu izi, ödül = c1 açık [Sinema.Gold] (ayrı altın yok), hata = [Sinema.Alert].
 * Token'lar yalnız buradan okunur; ileride tema eklemek = yeni bir [SinemaPalette] değeri + [SinemaPalette.Current] seçimi.
 */
@Immutable
data class SinemaPalette(
    val name: String,
    /** Dokun: birincil düğme, seçili taş/gün, açık anahtar, link. */
    val c1: Color,
    /** Basılı hal. */
    val press: Color,
    /** c2 (mercan): ilerleme halkası/çubuk, grafik, ikon, tamamlanan set örtüsü, AI kıvılcımı. */
    val hi: Color,
    /** c2 açık: pembe METİN/rozet yazısı (Page/Surface/Elevated üstünde ≥4.5). */
    val hiText: Color,
    /** Kart üstü dolgu izi (c1 α.14'ün #15171C üstü bileşkesi). */
    val soft: Color,
    /** SinemaBackdrop tepe ışığı alfası. */
    val glowAlpha: Float
) {
    companion object {
        val Gunes = SinemaPalette(
            name = "Güneş + Mercan",
            c1 = Color(0xFFFFB938),
            press = Color(0xFFE8A22A),
            hi = Color(0xFFFF7A6E),
            hiText = Color(0xFFFFB8AE),
            soft = Color(0xFF362E20),   // c1 α.14 üzeri #15171C
            glowAlpha = 0.14f           // palet.html --ga
        )

        /** Uygulamanın tek teması. */
        val Current: SinemaPalette = Gunes
    }
}

/** Etkin palet (tek sabit tema; kayıtlı eski renk tercihi yok sayılır). */
@Suppress("UnusedReceiverParameter")
val AppThemeState.palette: SinemaPalette get() = SinemaPalette.Current

/** c1 = DOKUN. [effectiveAccentColor] ile aynı değer. */
val AppThemeState.accentColor: Color get() = palette.c1
val AppThemeState.accentPress: Color get() = palette.press
/** hi = c2 pembe (ölç + ikon + çip kenarı; dolgu yok). */
val AppThemeState.accentHi: Color get() = palette.hi
val AppThemeState.accentSoft: Color get() = palette.soft
/** Ödül = c1 açık ton (#FFE29A; ayrı altın yok): kupa, rekor, seri, XP, rütbe. Dolgu düğme/zemin olmaz. */
@Suppress("UnusedReceiverParameter")
val AppThemeState.rewardGold: Color get() = Sinema.Gold

/** Metin varyantları: pembe metin = c2 açık (#FFB0CE), altın metin = #FFE29A; ikisi de Elevated üstünde ≥4.5. */
val AppThemeState.accentHiTextColor: Color get() = palette.hiText
val AppThemeState.rewardGoldTextColor: Color get() = rewardGold.lightenForTextContrast(Sinema.Elevated)

// ── Geriye uyum köprüsü (çağrı yerleri rolüne göre ayrılınca silinecek) ─────────────────────────

@Deprecated("c2 kalktı: ölç rolü için accentHi kullan.", ReplaceWith("accentHi"))
val AppThemeState.accent2Color: Color get() = accentHi

@Deprecated("c2 kalktı: ölç rolü için accentHiTextColor kullan.", ReplaceWith("accentHiTextColor"))
val AppThemeState.accent2TextColor: Color get() = accentHiTextColor

/** Köprü: c3 yok; nötr ikincil. Yeni kodda rol: ikon/çip = accentHi. */
@Deprecated("c3 kalktı: ödülse rewardGold, değilse nötr (Sinema.TextSecondary / Sinema.Text) kullan.")
@Suppress("UnusedReceiverParameter")
val AppThemeState.accent3Color: Color get() = Sinema.TextSecondary

@Deprecated("c3 kalktı: ödülse rewardGoldTextColor, değilse nötr (Sinema.TextSecondary) kullan.")
@Suppress("UnusedReceiverParameter")
val AppThemeState.accent3TextColor: Color get() = Sinema.TextSecondary
