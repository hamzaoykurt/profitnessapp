package com.avonix.profitness.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity

/**
 * program_exercises JOIN exercises — tek satır sonucu.
 * Room raw query ile doldurulur, @Relation kullanılmaz.
 */
data class ProgramExerciseWithName(
    val id: String,
    @ColumnInfo(name = "program_day_id") val programDayId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val sets: Int,
    val reps: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "target_muscle") val targetMuscle: String,
    val category: String,
    @ColumnInfo(name = "image_url") val imageUrl: String
)

/** ProgramDay + child ProgramExercise'lar (basit @Relation — exercise join olmadan). */
data class DayWithExercises(
    @Embedded val day: ProgramDayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "program_day_id"
    )
    val exercises: List<ProgramExerciseEntity>
)

/** Program + günleri (her günde egzersizleriyle). */
data class ProgramWithDays(
    @Embedded val program: ProgramEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "program_id",
        entity = ProgramDayEntity::class
    )
    val days: List<DayWithExercises>
)
