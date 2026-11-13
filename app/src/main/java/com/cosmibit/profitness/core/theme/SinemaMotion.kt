package com.cosmibit.profitness.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * HAREKET TOKEN'LARI — TEK KAYNAK (.ekip/plan-animasyon.md §2).
 * Süre (ms): Instant 90 (çıkış solması) · Quick 140 (basılma, renk) · Standard 220 (giriş, kapsül) ·
 * Emphasis 360 (sayfa/overlay, vurgu) · Slow 600 (sayaç, halka/çubuk dolumu).
 * Kural: giriş = Standard + EaseEnter, çıkış = Quick + EaseExit, ekranda kalan hareket = EaseStandard.
 * Karakter: sakin, sıçramasız; taşma yalnız [pop].
 */
object SinemaMotion {
    const val Instant = 90
    const val Quick = 140
    const val Standard = 220
    const val Emphasis = 360
    const val Slow = 600

    /** Ekranda kalan hareket (konum/boyut/renk değişimi). */
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Giriş ve vurgu: hızlı başlar, yumuşak oturur. */
    val EaseEnter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Çıkış: yumuşak başlar, hızlı gider. */
    val EaseExit = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** Basılma ölçeği (0.96 ↔ 1). */
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    /** Kayan taş, overlay yerleşmesi: taşmasız. */
    fun <T> settle() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    /** Tik / rozet: tek küçük taşma. */
    fun <T> pop() = spring<T>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

    /** Liste girişi kademesi: öğe başı gecikme ve en çok kademelenen öğe (toplam ≤ 240 ms). */
    const val StaggerMs = 40
    const val StaggerMax = 6
}
