package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Renk v2 (.ekip/yon-renk-v2.md): tek sabit tema Kor + geriye uyum köprüsü. */
@Suppress("DEPRECATION")
class AccentTriadTest {
    private val theme = AppThemeState()

    @Test fun korTokensMatchDirection() {
        val p = SinemaPalette.Current
        assertEquals(SinemaPalette.Kor, p)
        assertEquals(Color(0xFFF98942), p.c1)
        assertEquals(Color(0xFFE07227), p.press)
        assertEquals(Color(0xFFFBD5C1), p.hi)
        assertEquals(Color(0xFF352721), p.soft)
        assertEquals(0.16f, p.glowAlpha)
        assertEquals(0.16f, SinemaBackdropTopAlpha)
    }

    @Test fun publicApiReadsPalette() {
        assertEquals(SinemaPalette.Kor.c1, theme.accentColor)
        assertEquals(SinemaPalette.Kor.c1, theme.effectiveAccentColor)
        assertEquals(SinemaPalette.Kor.hi, theme.accentHi)
        assertEquals(SinemaPalette.Kor.press, theme.accentPress)
        assertEquals(Color(0xFFEBCD90), theme.rewardGold)
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
