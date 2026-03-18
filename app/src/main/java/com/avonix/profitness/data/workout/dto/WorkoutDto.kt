package com.avonix.profitness.data.workout.dto

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutLogDto(
    val id: String,
    val user_id: String,
    val program_day_id: String? = null,
    val date: String = "",
    val started_at: String? = null,     // DB'de NULL olabilir (default yok)
    val finished_at: String? = null,
    val total_duration_seconds: Int? = null
)

@Serializable
data class ExerciseLogDto(
    val id: String = "",
    val workout_log_id: String,
    val exercise_id: String,
    val sets_completed: Int? = null,
    val reps_completed: Int? = null,
    val is_completed: Boolean = false,
    val duration_seconds: Int? = null
)

@Serializable
data class UserStatsDto(
    val user_id: String,
    val xp: Int = 0,
    val level: Int = 1,
    val current_streak: Int = 0,
    val longest_streak: Int = 0,
    val total_workouts: Int = 0,
    val total_exercises: Int = 0,
    val total_duration_seconds: Int = 0,
    val updated_at: String = ""
)
