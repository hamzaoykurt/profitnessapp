package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
        assertEquals(Color(0xFFFF5A5A), t.supportAlert)
        assertEquals(0.16f, t.supportSoftAlpha)
        assertEquals(t.accentHi.copy(alpha = 0.16f), t.soft(t.supportSuccess))
    }

    @Test
    fun alertStaysDistinctFromThemeTones() {
        for (c in listOf(t.accentColor, t.accentHi, t.rewardGold, t.accentPress)) assertNotEquals(t.supportAlert, c)
    }

    /** Hata #F4748A pembe c2 #FF5C9D'ye yakın: renk tek başına ayırmaz → hata hep ikon + metinle verilir. Burada yalnız ölçülebilir fark: ışıklık/doygunluk. */
    @Test
    fun alertIsMeasurablyDifferentFromC2() {
        fun d(a: Color, b: Color) = Math.sqrt(
            Math.pow((a.red - b.red).toDouble(), 2.0) + Math.pow((a.green - b.green).toDouble(), 2.0) + Math.pow((a.blue - b.blue).toDouble(), 2.0)
        )
        // RGB uzaklığı küçük ama sıfır değil; ayrım ikon+metinle (bkz. rapor: karışma riski)
        assertTrue(d(t.supportAlert, t.accentHi) > 0.12)
        assertNotEquals(t.supportAlert, t.accentHiTextColor)
    }
}
