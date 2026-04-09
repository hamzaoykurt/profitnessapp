package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workout_log_id"),
        Index("exercise_id"),
        Index(value = ["workout_log_id", "exercise_id"], unique = true)
    ]
)
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_log_id") val workoutLogId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "sets_completed") val setsCompleted: Int,
    @ColumnInfo(name = "reps_completed") val repsCompleted: Int,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = true,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int = 0,
    val synced: Boolean = false
)
