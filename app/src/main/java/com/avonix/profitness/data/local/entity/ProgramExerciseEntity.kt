package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_day_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("program_day_id"), Index("exercise_id")]
)
data class ProgramExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "program_day_id") val programDayId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val sets: Int,
    val reps: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Float = 0f,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int = 90,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0
)
