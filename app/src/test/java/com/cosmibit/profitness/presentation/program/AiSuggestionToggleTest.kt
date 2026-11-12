package com.cosmibit.profitness.presentation.program

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSuggestionToggleTest {
    @Test
    fun addsAndRemovesExactToken() {
        val added = toggleAiSuggestion("", "Güç")
        assertEquals("Güç", added)
        assertTrue(aiSuggestionSelected(added, "Güç"))
        assertEquals("", toggleAiSuggestion(added, "Güç"))
    }

    @Test
    fun doesNotTouchSubstringOfOtherText() {
        val prompt = "Güçlü olmak istiyorum, Haftada 3 gün"
        assertFalse(aiSuggestionSelected(prompt, "Güç"))
        val toggled = toggleAiSuggestion(prompt, "Güç")
        assertEquals("Güçlü olmak istiyorum, Haftada 3 gün, Güç", toggled)
        assertEquals(prompt, toggleAiSuggestion(toggled, "Güç"))
    }
}
