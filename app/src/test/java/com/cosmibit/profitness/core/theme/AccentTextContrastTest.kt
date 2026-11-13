package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentTextContrastTest {
    private fun lin(v: Float): Double =
        if (v <= 0.03928f) v / 12.92 else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4)
    private fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val x = lum(a); val y = lum(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private val surfaces = listOf(Sinema.Page, Sinema.Surface, Color(0xFF1C1D22), Sinema.Elevated)

    @Test
    fun darkAccentTextMeets4_5OnPageAndSurfaces() {
        var min = 99.0
        for (preset in AccentPreset.entries) for (intensity in AccentIntensity.entries) {
            val theme = AppThemeState(isDark = true, accent = preset, intensity = intensity)
            for (bg in surfaces) {
                val r = ratio(theme.accentTextColor, bg)
                min = minOf(min, r)
                assertTrue("$preset/$intensity on $bg = $r", r >= 4.5)
            }
        }
        println("MIN_ACCENT_TEXT_RATIO=$min")
    }

    @Test
    fun customAccentIsDarkenedUntilReadable() {
        val theme = AppThemeState(isDark = true, customAccentArgb = 0xFFFACC15.toInt())
        assertTrue(surfaces.all { ratio(theme.accentTextColor, it) >= 4.5 })
    }

    @Test
    fun darkThemeKeepsAccent() {
        val theme = AppThemeState(isDark = true)
        assertTrue(ratio(theme.accentTextColor, Sinema.Elevated) >= 4.5)
    }
}
