package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SupportColorTest {
    @Test
    fun darkAndLightPickTheirVariant() {
        val dark = AppThemeState(isDark = true)
        val light = AppThemeState(isDark = false)
        assertEquals(Color(0xFF46CC8C), dark.supportSuccess)
        assertEquals(Color(0xFF167544), light.supportSuccess)
        assertEquals(Color(0xFFF2C23F), dark.supportReward)
        assertEquals(Color(0xFF7F5B00), light.supportReward)
        assertEquals(0.16f, dark.supportSoftAlpha)
        assertEquals(0.12f, light.supportSoftAlpha)
    }

    @Test
    fun supportColorsIgnoreAccentPreset() {
        val a = AppThemeState(isDark = true, accent = AccentPreset.ORANGE)
        for (p in AccentPreset.entries) {
            val t = AppThemeState(isDark = true, accent = p)
            assertEquals(a.supportRest, t.supportRest)
            assertNotEquals(t.supportStrength, t.supportAlert)
        }
    }
}
