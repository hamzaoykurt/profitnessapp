package com.cosmibit.profitness.presentation.aicoach

import com.cosmibit.profitness.presentation.workout.WorkoutDayState

data class OracleTrainingSummary(
    val trainingDays: Int,
    val exercises: Int,
    val completed: Int,
    val todayExercises: Int = 0,
    val todayCompleted: Int = 0,
    val todayIsRest: Boolean = false,
    val todayTitle: String = ""
)

/** An overview of the loaded program and selected day, not a readiness or recovery prediction. */
fun summarizeOracleTraining(
    days: List<WorkoutDayState>,
    hasProgramLoaded: Boolean,
    isLoading: Boolean,
    selectedDayIdx: Int = -1
): OracleTrainingSummary? {
    if (!hasProgramLoaded || isLoading) return null
    val selectedDay = days.getOrNull(selectedDayIdx)
    val todayExercises = selectedDay?.totalCount ?: 0
    val todayCompleted = selectedDay?.day?.exercises?.count { it.id in selectedDay.completedIds } ?: 0
    return OracleTrainingSummary(
        trainingDays = days.count { !it.day.isRestDay && it.totalCount > 0 },
        exercises = days.sumOf { it.totalCount },
        completed = days.sumOf { day -> day.day.exercises.count { it.id in day.completedIds } },
        todayExercises = todayExercises,
        todayCompleted = todayCompleted,
        todayIsRest = selectedDay != null && (selectedDay.day.isRestDay || todayExercises == 0),
        todayTitle = selectedDay?.day?.title.orEmpty()
    )
}
