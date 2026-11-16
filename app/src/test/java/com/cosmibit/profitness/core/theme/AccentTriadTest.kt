package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Renk v3 (palet.html P1 Güneş + Pembe): tek sabit tema + geriye uyum köprüsü. */
@Suppress("DEPRECATION")
class AccentTriadTest {
    private val theme = AppThemeState()

    @Test fun gunesTokensMatchDirection() {
        val p = SinemaPalette.Current
        assertEquals(SinemaPalette.Gunes, p)
        assertEquals(Color(0xFFFFB938), p.c1)
        assertEquals(Color(0xFFE8A22A), p.press)
        assertEquals(Color(0xFFFF7A6E), p.hi)
        assertEquals(Color(0xFFFFB8AE), p.hiText)
        assertEquals(Color(0xFF362E20), p.soft)
        assertEquals(0.07f, p.glowAlpha)
        assertEquals(0.07f, SinemaBackdropTopAlpha)
    }

    @Test fun publicApiReadsPalette() {
        assertEquals(SinemaPalette.Gunes.c1, theme.accentColor)
        assertEquals(SinemaPalette.Gunes.c1, theme.effectiveAccentColor)
        assertEquals(SinemaPalette.Gunes.hi, theme.accentHi)
        assertEquals(SinemaPalette.Gunes.press, theme.accentPress)
        assertEquals(Color(0xFFFFD98A), theme.rewardGold)
        assertEquals(Color(0xFFFFB8AE), theme.accentHiTextColor)
        assertEquals(theme.rewardGold, theme.rewardGoldTextColor)
        assertEquals(Sinema.Ink, theme.effectiveOnAccentColor)
    }

    @Test fun legacyBridgeHasNoSideHues() {
        // c2 → ölç (hi); c3 → nötr ikincil (lila/yeşil yan renk yok)
        assertEquals(theme.accentHi, theme.accent2Color)
        assertEquals(theme.accentHiTextColor, theme.accent2TextColor)
        assertEquals(Sinema.TextSecondary, theme.accent3Color)
        assertEquals(Sinema.TextSecondary, theme.accent3TextColor)
    }

    @Test fun singleSecondaryGrey() {
        assertEquals(Sinema.TextSecondary, theme.text1)
        assertEquals(Sinema.TextSecondary, theme.text2)
    }

    @Test fun glowDisciplineConstants() {
        assertEquals(0.22f, Sinema.GlowAlpha)
        assertTrue(Sinema.GlowBlur.value == 24f && Sinema.GlowDy.value == 10f)
    }
}
