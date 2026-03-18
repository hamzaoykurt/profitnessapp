package com.avonix.profitness.domain.model

enum class ProgramType { TEMPLATE, AI, MANUAL }

data class Program(
    val id: String,
    val userId: String,
    val name: String,
    val type: ProgramType,
    val isActive: Boolean,
    val days: List<ProgramDay> = emptyList(),
    val createdAt: String = ""
)

data class ProgramDay(
    val id: String,
    val programId: String,
    val dayIndex: Int,
    val title: String,
    val isRestDay: Boolean,
    val exercises: List<ProgramExercise> = emptyList()
)

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
