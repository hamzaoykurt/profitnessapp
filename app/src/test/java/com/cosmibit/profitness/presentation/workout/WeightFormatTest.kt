package com.cosmibit.profitness.presentation.workout

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WeightFormatTest {
    private val tr = Locale.forLanguageTag("tr-TR")

    @Test
    fun `whole numbers drop the trailing zero`() {
        assertEquals("29", formatWeightNumber(29.0f, tr))
        assertEquals("29", formatWeightNumber("29.0", tr))
        assertEquals("100", formatWeightNumber(100f, Locale.ENGLISH))
    }

    @Test
    fun `decimals use the locale separator`() {
        assertEquals("27,5", formatWeightNumber(27.5f, tr))
        assertEquals("27,5", formatWeightNumber("27.5", tr))
        assertEquals("27,5", formatWeightNumber("27,5", tr))
        assertEquals("27.5", formatWeightNumber(27.5f, Locale.ENGLISH))
        assertEquals("27,25", formatWeightNumber(27.25f, tr))
        assertEquals("27,3", formatWeightNumber(27.3f, tr))
    }

    @Test
    fun `non numeric input is returned as is`() {
        assertEquals("abc", formatWeightNumber(" abc ", tr))
    }
}
