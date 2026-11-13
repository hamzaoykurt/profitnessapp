package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 3 renk sistemi: Success/Rest/Strength -> c2, Reward -> c3; yalnız Alert sabit (.ekip/yon-renk-hareket.md). */
class SupportColorTest {
    @Test
    fun supportColorsFoldIntoTriad() {
        val dark = AppThemeState(isDark = true)
        assertEquals(dark.accent2Color, dark.supportSuccess)
        assertEquals(dark.accent2Color, dark.supportRest)
        assertEquals(dark.accent2Color, dark.supportStrength)
        assertEquals(dark.accent3Color, dark.supportReward)
        assertEquals(Color(0xFFF4748A), dark.supportAlert)
        assertEquals(0.16f, dark.supportSoftAlpha)
    }

    @Test
    fun alertStaysFixedAndDistinctFromTriad() {
        for (p in AccentPreset.entries) {
            val t = AppThemeState(isDark = true, accent = p)
            assertEquals(Color(0xFFF4748A), t.supportAlert)
            assertNotEquals(t.accent2Color, t.supportAlert)
            assertNotEquals(t.accent3Color, t.supportAlert)
        }
    }
}
