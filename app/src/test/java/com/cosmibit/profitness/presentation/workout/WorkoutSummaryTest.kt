package com.cosmibit.profitness.presentation.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSummaryTest {
    private fun strength(
        done: Set<Int>, weights: Map<Int, String> = emptyMap(), reps: Map<Int, String> = emptyMap(),
        planned: String = "10", default: Float = 0f
    ) = SummaryExerciseInput(SinemaSetKind.Strength, false, 3, done, weights, reps, planned, default)

    @Test
    fun `volume uses recorded weight and reps`() {
        val s = computeWorkoutSummary(listOf(strength(setOf(0, 1), mapOf(0 to "50", 1 to "52,5"), mapOf(0 to "8", 1 to "10"))))
        assertEquals(2, s.completedSets)
        assertEquals(50f * 8 + 52.5f * 10, s.volumeKg, 0.001f)
    }

    @Test
    fun `missing reps fall back to planned and missing weight to default`() {
        val s = computeWorkoutSummary(listOf(strength(setOf(0), planned = "8-12", default = 20f)))
        assertEquals(20f * 8, s.volumeKg, 0.001f)
    }

    @Test
    fun `sets without any weight count but add no volume`() {
        val s = computeWorkoutSummary(listOf(strength(setOf(0, 2))))
        assertEquals(2, s.completedSets)
        assertEquals(0f, s.volumeKg, 0.001f)
    }

    @Test
    fun `activity counts once and timed adds no volume`() {
        val act = SummaryExerciseInput(SinemaSetKind.Activity, true, 1, emptySet(), emptyMap(), emptyMap(), "", 0f)
        val timed = SummaryExerciseInput(SinemaSetKind.Timed, false, 3, setOf(0, 1), mapOf(0 to "30"), emptyMap(), "30", 10f)
        val s = computeWorkoutSummary(listOf(act, timed))
        assertEquals(3, s.completedSets)
        assertEquals(0f, s.volumeKg, 0.001f)
    }

    @Test
    fun `session minutes from first and last completion`() {
        assertEquals(45, computeSessionMinutes(0L, 45 * 60_000L))
        assertNull(computeSessionMinutes(null, 10L))
        assertNull(computeSessionMinutes(0L, 30_000L))          // tek set / <1 dk
        assertNull(computeSessionMinutes(0L, 7 * 60 * 60_000L)) // >6 sa anormal
    }

    @Test
    fun `records need a previous max and a strictly higher weight`() {
        val ex = listOf("a" to "Bench", "b" to "Squat", "c" to "Row", "a" to "Bench")
        val r = computeBrokenRecords(
            ex, todayMaxKg = mapOf("a" to 82.5f, "b" to 100f, "c" to 60f),
            previousMaxKg = mapOf("a" to 80f, "b" to 100f)   // c: önceki yok → rekor değil
        )
        assertEquals(listOf("Bench"), r)
    }

    @Test
    fun `record badges cap at three with overflow count`() {
        val (shown, more) = splitRecordBadges(listOf("A", "B", "C", "D", "E"))
        assertEquals(listOf("A", "B", "C"), shown)
        assertEquals(2, more)
        assertEquals(0, splitRecordBadges(listOf("A")).second)
    }
}
