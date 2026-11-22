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

    @Test fun `selectedDayIdx isolates workout day and rest day stats cleanly`() {
        val days = listOf(
            WorkoutDayState(
                WorkoutDay("Mon", "Göğüs & Triceps", persistentListOf(exercise("a"), exercise("b"), exercise("c"))),
                persistentSetOf("a", "b")
            ),
            WorkoutDayState(WorkoutDay("Tue", "Dinlenme", persistentListOf(), isRestDay = true))
        )

        val workoutSummary = summarizeOracleTraining(days, true, false, selectedDayIdx = 0)
        assertEquals(1, workoutSummary?.trainingDays)
        assertEquals(3, workoutSummary?.todayExercises)
        assertEquals(2, workoutSummary?.todayCompleted)
        assertEquals(false, workoutSummary?.todayIsRest)
        assertEquals("Göğüs & Triceps", workoutSummary?.todayTitle)

        val restSummary = summarizeOracleTraining(days, true, false, selectedDayIdx = 1)
        assertEquals(0, restSummary?.todayExercises)
        assertEquals(true, restSummary?.todayIsRest)
        assertEquals("Dinlenme", restSummary?.todayTitle)
    }
}
