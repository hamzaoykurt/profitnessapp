package com.avonix.profitness.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProgramType { TEMPLATE, AI, MANUAL }

@Serializable
data class Program(
    val id: String,
    val userId: String,
    val name: String,
    val type: ProgramType,
    val isActive: Boolean,
    val days: List<ProgramDay> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class ProgramDay(
    val id: String,
    val programId: String,
    val dayIndex: Int,
    val title: String,
    val isRestDay: Boolean,
    val exercises: List<ProgramExercise> = emptyList()
)

@Serializable
data class ProgramExercise(
    val id: String,
    val programDayId: String,
    val exerciseId: String,
    val exerciseName: String,  // denormalized for display
    val targetMuscle: String,  // denormalized for display
    val sets: Int,
    val reps: Int,
    val weightKg: Float = 0f,
    val restSeconds: Int = 90,
    val orderIndex: Int,
    val category: String = "",
    val imageUrl: String = ""
)

@Serializable
data class ExerciseItem(
    val id: String,
    val name: String,
    val nameEn: String,
    val targetMuscle: String,
    val category: String,
    val setsDefault: Int,
    val repsDefault: Int,
    val description: String = ""
)
