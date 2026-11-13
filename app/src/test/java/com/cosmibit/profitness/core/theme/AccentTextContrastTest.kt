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

    @Test
    fun darkAccentTextMeets3to1OnPage() {
        for (preset in AccentPreset.entries) for (intensity in AccentIntensity.entries) {
            val theme = AppThemeState(isDark = true, accent = preset, intensity = intensity)
            assertTrue("$preset/$intensity", ratio(theme.accentTextColor, theme.bg0) >= 3.0)
        }
    }

    @Test
    fun customAccentIsDarkenedUntilReadable() {
        val theme = AppThemeState(isDark = true, customAccentArgb = 0xFFFACC15.toInt())
        assertTrue(ratio(theme.accentTextColor, theme.bg3) >= 4.5)
    }

    @Test
    fun darkThemeKeepsAccent() {
        val theme = AppThemeState(isDark = true)
        assertTrue(theme.accentTextColor == theme.effectiveAccentColor)
    }
}
