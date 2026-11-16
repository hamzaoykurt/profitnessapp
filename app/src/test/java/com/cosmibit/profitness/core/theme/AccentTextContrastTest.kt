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
        // c2 (#FF7A6E) üstü ink: tamamlanan set onay diski (örtü, metin değil)
        assertTrue(ratio(theme.effectiveOnAccentColor, theme.accentHi) >= 4.5)
    }

    @Test fun c2AsIconAndLineOnSurfaces() {
        // c2 (#FF7A6E) ikon/çizgi/halka: UI bileşeni eşiği 3.0 (metin değil; metin = c2 açık, ≥4.5 aşağıda)
        for (bg in listOf(Sinema.Page, Sinema.Surface, Sinema.Elevated)) {
            val r = ratio(theme.accentHi, bg)
            assertTrue("c2/$bg = $r", r >= 3.0)
        }
    }

    @Test fun accentTextOnSurfaces() = assertAll("accentText", theme.accentTextColor)
    @Test fun accentFillAsTextOnSurfaces() = assertAll("c1", theme.accentColor)
    @Test fun accentHiTextOnSurfaces() {
        // c2 açık #FFB8AE metin/rozet yazısı ≥4.5 (c2 çıplak ≥4.5 olması şart değil: ikon eşiği yukarıda ≥3.0)
        assertAll("hiText", theme.accentHiTextColor)
        assertTrue(theme.accentHiTextColor == androidx.compose.ui.graphics.Color(0xFFFFB8AE))
    }
    @Test fun rewardGoldTextOnSurfaces() {
        assertAll("gold", theme.rewardGold)
        assertAll("goldText", theme.rewardGoldTextColor)
    }
    @Test fun neutralRampOnSurfaces() {
        assertAll("text", Sinema.Text)
        assertAll("text2", Sinema.TextSecondary)
        for (bg in listOf(Sinema.Page, Sinema.Surface, Sinema.Elevated)) assertTrue(ratio(Sinema.TextTertiary, bg) >= 4.5)
        for (bg in listOf(Sinema.Page, Sinema.Surface, Sinema.Elevated)) assertTrue("alert on $bg", ratio(Sinema.Alert, bg) >= 4.5)
    }
}
