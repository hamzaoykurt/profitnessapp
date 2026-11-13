package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * 3 RENK SİSTEMİ (.ekip/yon-renk-hareket.md): kullanıcı yalnız c1'i (vurgu) seçer;
 * c2 (ölç) = ton+90°, c3 (işaretle) = ton−120° OKLCH'de otomatik türer.
 * Saf Kotlin (Android bağımlılığı yok) → birim testlenir. Sonuç ARGB anahtarıyla önbelleğe alınır.
 */
data class AccentTriad(val c2: Int, val c3: Int)

object AccentTriadMath {
    private const val ERROR_HUE = 11.0          // supportAlert #F4748A
    private const val ERROR_GUARD = 40.0
    private const val ELEVATED_RGB = 0x1D2027

    private val cache = HashMap<Int, AccentTriad>()

    fun derive(c1Argb: Int): AccentTriad = synchronized(cache) {
        cache.getOrPut(c1Argb or (0xFF shl 24)) { compute(c1Argb) }
    }

    private fun compute(c1Argb: Int): AccentTriad {
        val (l1, a1, b1) = srgbToOklab(c1Argb)
        val chroma1 = hypot(a1, b1)
        val hue1 = if (chroma1 < 0.04) 185.0 else norm(Math.toDegrees(atan2(b1, a1)))
        val c2 = build(hue1 + 90.0, 0.76, chroma1.coerceIn(0.10, 0.15))
        val c3 = build(hue1 - 120.0, 0.82, (chroma1 * 1.15).coerceIn(0.12, 0.17))
        return AccentTriad(c2, c3)
    }

    private fun build(hueIn: Double, lightness: Double, chroma: Double): Int {
        val hue = protectErrorHue(norm(hueIn))
        var l = lightness + if (hue in 75.0..125.0) 0.06 else 0.0
        var rgb = oklchToSrgb(l, chroma, hue)
        // WCAG 4.5:1, en açık yüzey (#1D2027) üstünde; kural 6
        var guard = 0
        while (contrast(rgb, ELEVATED_RGB) < 4.5 && guard++ < 40) {
            l += 0.01
            rgb = oklchToSrgb(l, chroma, hue)
        }
        return rgb
    }

    /** Hata tonuna 40°'den yakınsa en yakın sınıra (331° / 51°) it. */
    private fun protectErrorHue(h: Double): Double {
        val d = angDiff(h, ERROR_HUE)
        if (abs(d) >= ERROR_GUARD) return h
        return if (d <= 0) norm(ERROR_HUE - ERROR_GUARD) else norm(ERROR_HUE + ERROR_GUARD)
    }

    // ── OKLab ──────────────────────────────────────────────────────────────
    private fun norm(h: Double): Double = ((h % 360.0) + 360.0) % 360.0
    private fun angDiff(a: Double, b: Double): Double { val d = norm(a - b); return if (d > 180) d - 360 else d }

    private fun toLinear(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    private fun fromLinear(v: Double) = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055

    private fun srgbToOklab(argb: Int): Triple<Double, Double, Double> {
        val r = toLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = toLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = toLinear((argb and 0xFF) / 255.0)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return Triple(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        )
    }

    /** Linear sRGB (gamut dışı olabilir). */
    private fun oklchToLinear(l: Double, c: Double, h: Double): DoubleArray {
        val a = c * cos(Math.toRadians(h))
        val b = c * sin(Math.toRadians(h))
        val l_ = (l + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
        val m_ = (l - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
        val s_ = (l - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
        return doubleArrayOf(
            4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_,
            -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_,
            -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_
        )
    }

    private fun inGamut(lin: DoubleArray) = lin.all { it >= -0.0005 && it <= 1.0005 }

    /** Sığmıyorsa L ve h sabit, C ikili aramayla düşer. */
    private fun oklchToSrgb(l: Double, c: Double, h: Double): Int {
        var lin = oklchToLinear(l, c, h)
        if (!inGamut(lin)) {
            var lo = 0.0
            var hi = c
            repeat(24) {
                val mid = (lo + hi) / 2
                if (inGamut(oklchToLinear(l, mid, h))) lo = mid else hi = mid
            }
            lin = oklchToLinear(l, lo, h)
        }
        fun ch(v: Double) = (fromLinear(v.coerceIn(0.0, 1.0)) * 255.0 + 0.5).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(lin[0]) shl 16) or (ch(lin[1]) shl 8) or ch(lin[2])
    }

    private fun lum(rgb: Int): Double {
        fun c(s: Int) = toLinear(((rgb shr s) and 0xFF) / 255.0)
        return 0.2126 * c(16) + 0.7152 * c(8) + 0.0722 * c(0)
    }

    fun contrast(a: Int, b: Int): Double {
        val x = lum(a); val y = lum(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}

/** c2 = ÖLÇ (ilerleme, grafik, tamamlanan set/gün). Kullanıcıya seçim sunulmaz. */
val AppThemeState.accent2Color: Color get() = Color(AccentTriadMath.derive(effectiveAccentColor.toArgbInt()).c2)

/** c3 = İŞARETLE (rozet, rekor, seri, kupa, AI ikonu). Dolgu/düğme/zemin ışığı olmaz. */
val AppThemeState.accent3Color: Color get() = Color(AccentTriadMath.derive(effectiveAccentColor.toArgbInt()).c3)

/** Metin varyantları: #1D2027 üstünde ≥4.5 (türetme zaten sağlar; güvence). */
val AppThemeState.accent2TextColor: Color get() = accent2Color.lightenForTextContrast(Sinema.Elevated)
val AppThemeState.accent3TextColor: Color get() = accent3Color.lightenForTextContrast(Sinema.Elevated)

private fun Color.toArgbInt(): Int =
    (0xFF shl 24) or
        ((red * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
        ((green * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
        (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
