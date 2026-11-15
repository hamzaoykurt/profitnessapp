package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Renk v2: Success/Rest/Strength → accentHi (ölç), Reward → rewardGold; Alert sabit #F4748A. */
class SupportColorTest {
    private val t = AppThemeState()

    @Test
    fun supportColorsFoldIntoTonalSystem() {
        assertEquals(t.accentHi, t.supportSuccess)
        assertEquals(t.accentHi, t.supportRest)
        assertEquals(t.accentHi, t.supportStrength)
        assertEquals(t.rewardGold, t.supportReward)
        assertEquals(Color(0xFFF4748A), t.supportAlert)
        assertEquals(0.16f, t.supportSoftAlpha)
        assertEquals(t.accentHi.copy(alpha = 0.16f), t.soft(t.supportSuccess))
    }

    @Test
    fun alertStaysDistinctFromThemeTones() {
        for (c in listOf(t.accentColor, t.accentHi, t.rewardGold, t.accentPress)) assertNotEquals(t.supportAlert, c)
    }
}
