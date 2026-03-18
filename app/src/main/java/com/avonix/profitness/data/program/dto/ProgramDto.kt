package com.avonix.profitness.data.program.dto

import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.domain.model.ProgramDay
import com.avonix.profitness.domain.model.ProgramExercise
import com.avonix.profitness.domain.model.ProgramType
import kotlinx.serialization.Serializable

@Serializable
data class ProgramDto(
    val id: String,
    val user_id: String,
    val name: String,
    val type: String,
    val is_active: Boolean,
    val created_at: String = ""
)

fun ProgramDto.toDomain() = Program(
    id = id,
    userId = user_id,
    name = name,
    type = runCatching { ProgramType.valueOf(type.uppercase()) }.getOrDefault(ProgramType.MANUAL),
    isActive = is_active,
    createdAt = created_at
)

@Serializable
data class ProgramDayDto(
    val id: String,
    val program_id: String,
    val day_index: Int,
    val title: String,
    val is_rest_day: Boolean
)

fun ProgramDayDto.toDomain() = ProgramDay(
    id = id,
    programId = program_id,
    dayIndex = day_index,
    title = title,
    isRestDay = is_rest_day
)

@Serializable
data class ProgramExerciseWithNameDto(
    val id: String,
    val program_day_id: String,
    val exercise_id: String,
    val sets: Int,
    val reps: Int,
    val weight_kg: Float? = null,   // DB'de NULL olabilir
    val rest_seconds: Int = 90,
    val order_index: Int = 0,
    // joined from exercises table
    val exercises: ExerciseNameDto? = null
)

@Serializable
data class ExerciseNameDto(
    val name: String,
    val target_muscle: String,
    val category: String? = null,   // DB'de NULL olabilir
    val image_url: String? = null   // DB'de NULL olabilir
)

fun ProgramExerciseWithNameDto.toDomain() = ProgramExercise(
    id = id,
    programDayId = program_day_id,
    exerciseId = exercise_id,
    exerciseName = exercises?.name ?: "",
    targetMuscle = exercises?.target_muscle ?: "",
    sets = sets,
    reps = reps,
    weightKg = weight_kg ?: 0f,
    restSeconds = rest_seconds,
    orderIndex = order_index,
    category = exercises?.category ?: "",
    imageUrl = exercises?.image_url ?: ""
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val name_en: String = "",
    val target_muscle: String,
    val category: String,
    val sets_default: Int,
    val reps_default: Int,
    val description: String = ""
)

fun ExerciseDto.toDomain() = ExerciseItem(
    id = id,
    name = name,
    nameEn = name_en,
    targetMuscle = target_muscle,
    category = category,
    setsDefault = sets_default,
    repsDefault = reps_default,
    description = description
)
