package com.cosmibit.profitness

import com.cosmibit.profitness.presentation.dashboard.TabHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabHistoryTest {
    @Test fun backWalksReverseOrder() {
        val h = TabHistory<String>()
        h.onNavigate("W", "P"); h.onNavigate("P", "Pr")
        assertEquals("P", h.pop("Pr"))
        assertEquals("W", h.pop("P"))
        assertNull(h.pop("W"))
    }

    @Test fun sameTabDoesNotGrowStack() {
        val h = TabHistory<String>()
        h.onNavigate("W", "W"); h.onNavigate("W", "W")
        assertTrue(h.isEmpty())
    }

    @Test fun noConsecutiveDuplicates() {
        val h = TabHistory<String>()
        h.onNavigate("W", "P"); h.onNavigate("W", "P")
        assertEquals(listOf("W"), h.snapshot())
    }

    @Test fun capDropsOldest() {
        val h = TabHistory<Int>(max = 3)
        for (i in 0 until 6) h.onNavigate(i, i + 1)
        assertEquals(listOf(3, 4, 5), h.snapshot())
    }

    @Test fun popSkipsEntriesEqualToCurrent() {
        val h = TabHistory<String>()
        h.onNavigate("W", "P"); h.onNavigate("P", "W"); h.onNavigate("W", "D")
        // yığın: W, P, W ; şu an D
        assertEquals("W", h.pop("D"))
        assertEquals("P", h.pop("W"))
        assertEquals("W", h.pop("P"))
    }

    @Test fun popOnEmptyIsNull() = assertNull(TabHistory<String>().pop("W"))

    @Test fun backAndForthStaysBounded() {
        val h = TabHistory<String>(max = 10)
        repeat(50) { h.onNavigate("D", "P"); h.onNavigate("P", "D") }
        assertTrue(h.size <= 10)
    }
}
