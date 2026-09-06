package com.cosmibit.profitness.presentation.aicoach

import com.cosmibit.profitness.presentation.workout.WorkoutDayState

data class OracleTrainingSummary(val trainingDays: Int, val exercises: Int, val completed: Int)

/** An overview of the loaded program, not a readiness or recovery prediction. */
fun summarizeOracleTraining(
    days: List<WorkoutDayState>,
    hasProgramLoaded: Boolean,
    isLoading: Boolean
): OracleTrainingSummary? {
    if (!hasProgramLoaded || isLoading) return null
    return OracleTrainingSummary(
        trainingDays = days.count { !it.day.isRestDay && it.totalCount > 0 },
        exercises = days.sumOf { it.totalCount },
        completed = days.sumOf { day -> day.day.exercises.count { it.id in day.completedIds } }
    )
}
