package com.cosmibit.profitness.presentation.profile

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceStatsTest {
    private val sample = listOf(2, 3, 1, 4, 3, 0, 2, 3, 4, 3, 5, 3, 3)

    @Test fun averageAndBestOverSlices() {
        assertEquals(36f / 13f, weeklyAverage(weekSlice(sample, 13)), 1e-4f)
        assertEquals(5, bestWeek(weekSlice(sample, 13)))
        assertEquals(listOf(5, 3, 3, 0).size, weekSlice(sample, 4).size)
        assertEquals(3.5f, weeklyAverage(weekSlice(sample, 4)), 1e-4f) // 3,5,3,3 -> 14/4
        assertEquals(5, bestWeek(weekSlice(sample, 4)))
        assertEquals(0f, weeklyAverage(emptyList()), 0f)
    }

    @Test fun lowDataMakesBestWeekTheHero() {
        val few = List(11) { 0 } + 1 + 0 // 13 hafta, tek antrenman, bu hafta 0
        val avg = weeklyAverage(few)
        assertEquals(1f / 13f, avg, 1e-4f)
        assertFalse(averageIsHero(avg, bestWeek(few)))
        assertTrue(averageIsHero(2.8f, 5))
        assertTrue(averageIsHero(0f, 0)) // hiç veri: ortalama (0) dürüst kalır
    }

    @Test fun weeksSinceLast() {
        assertEquals(0, weeksSinceLastWorkout(sample))
        assertEquals(6, weeksSinceLastWorkout(List(6) { 0 } + 1 + List(6) { 0 }))
        assertNull(weeksSinceLastWorkout(List(13) { 0 }))
    }

    @Test fun ringAlphaAndCapsules() {
        assertEquals(0f, ringArcAlpha(0f), 0f)
        assertEquals(0.35f + 0.65f * 0.5f, ringArcAlpha(0.5f), 1e-4f)
        assertEquals(1f, ringArcAlpha(5f), 1e-4f)
        assertEquals(16f, capsuleWidthDp(13), 0f)
        assertEquals(56f, capsuleWidthDp(4), 0f)
        assertEquals(0.6f, capsuleFraction(3, 5), 1e-4f)
        assertEquals(0f, capsuleFraction(0, 0), 0f)
    }

    @Test fun weekStartIsMonday() {
        val thursday = LocalDate.of(2026, 11, 12)
        assertEquals(LocalDate.of(2026, 11, 9), weekStart(thursday, 0))
        assertEquals(LocalDate.of(2026, 11, 2), weekStart(thursday, 1))
    }

    @Test fun bmiScaleAndZones() {
        assertEquals((22.6 - 15.0) / 20.0, bmiScaleFraction(22.6).toDouble(), 1e-4)
        assertEquals(0f, bmiScaleFraction(10.0), 0f)
        assertEquals(1f, bmiScaleFraction(50.0), 0f)
        assertEquals(listOf(0, 1, 2, 3), listOf(17.0, 22.0, 27.0, 31.0).map { bmiZone(it) })
    }

    @Test fun streakAndFormat() {
        assertEquals(12f / 21f, streakRatio(12, 21), 1e-4f)
        assertEquals(1f, streakRatio(5, 3), 0f)
        assertEquals(0f, streakRatio(0, 0), 0f)
        assertEquals("2.115", formatNumber(2115.0, 0, false))
        assertEquals("38,4", formatNumber(38.4, 1, false))
        assertEquals("38.4", formatNumber(38.4, 1, true))
        assertEquals(1, durationMinutes(20))
        assertEquals(35, durationMinutes(2100))
        assertEquals(0, durationMinutes(0))
    }
}
