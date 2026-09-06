package com.cosmibit.profitness.presentation.aicoach

import com.cosmibit.profitness.presentation.workout.Exercise
import com.cosmibit.profitness.presentation.workout.WorkoutDay
import com.cosmibit.profitness.presentation.workout.WorkoutDayState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OracleTrainingSummaryTest {
    private fun exercise(id: String) = Exercise(id, "Exercise", "Target", 3, "10", "")

    @Test fun `unloaded and loading programs never display made up totals`() {
        assertNull(summarizeOracleTraining(emptyList(), false, false))
        assertNull(summarizeOracleTraining(emptyList(), true, true))
    }

    @Test fun `loaded empty program has genuine zero totals`() {
        assertEquals(OracleTrainingSummary(0, 0, 0), summarizeOracleTraining(emptyList(), true, false))
    }

    @Test fun `only current exercise ids count as completions`() {
        val days = listOf(
            WorkoutDayState(WorkoutDay("Mon", "Training", persistentListOf(exercise("a"), exercise("b"))),
                persistentSetOf("a", "removed-exercise")),
            WorkoutDayState(WorkoutDay("Tue", "Rest", persistentListOf(), isRestDay = true)),
            WorkoutDayState(WorkoutDay("Wed", "Empty", persistentListOf()))
        )
        assertEquals(OracleTrainingSummary(1, 2, 1), summarizeOracleTraining(days, true, false))
    }
}
