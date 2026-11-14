package com.cosmibit.profitness.presentation.workout

import org.junit.Assert.assertEquals
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
}
