package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kontrast (WCAG): c1 üstü koyu yazı, c1/hi/altın metin ≥4.5 — Page/Surface/Elevated ve soft üstünde. */
class AccentTextContrastTest {
    private fun lin(v: Float): Double =
        if (v <= 0.04045f) v / 12.92 else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4)
    private fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val x = lum(a); val y = lum(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private val theme = AppThemeState()
    private val surfaces = listOf(Sinema.Page, Sinema.Surface, Sinema.Elevated, theme.accentSoft)

    private fun assertAll(name: String, fg: Color, min: Double = 4.5) {
        for (bg in surfaces) {
            val r = ratio(fg, bg)
            assertTrue("$name on $bg = $r", r >= min)
        }
    }

    @Test fun inkOnAccentFill() {
        val r = ratio(theme.effectiveOnAccentColor, theme.accentColor)
        assertTrue("ink/c1 = $r", r >= 7.0)
        assertTrue(ratio(theme.effectiveOnAccentColor, theme.accentPress) >= 4.5)
        assertTrue(ratio(theme.effectiveOnAccentColor, theme.accentHi) >= 4.5)  // tamamlanan set onay diski
    }

    @Test fun accentTextOnSurfaces() = assertAll("accentText", theme.accentTextColor)
    @Test fun accentFillAsTextOnSurfaces() = assertAll("c1", theme.accentColor)
    @Test fun accentHiTextOnSurfaces() {
        assertAll("hi", theme.accentHi)
        assertAll("hiText", theme.accentHiTextColor)
    }
    @Test fun rewardGoldTextOnSurfaces() {
        assertAll("gold", theme.rewardGold)
        assertAll("goldText", theme.rewardGoldTextColor)
    }
    @Test fun neutralRampOnSurfaces() {
        assertAll("text", Sinema.Text)
        assertAll("text2", Sinema.TextSecondary)
        for (bg in listOf(Sinema.Page, Sinema.Surface, Sinema.Elevated)) assertTrue(ratio(Sinema.TextTertiary, bg) >= 4.5)
        assertAll("alert", Sinema.Alert)
    }
}
