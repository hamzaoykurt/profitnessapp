package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "set_completions",
    primaryKeys = ["user_id", "exercise_id", "program_day_id", "set_index", "date"],
    indices = [
        Index(value = ["user_id", "program_day_id", "date"]),
        Index(value = ["user_id", "exercise_id", "date"]),
        Index(value = ["user_id", "dirty", "updated_at_ms"])
    ]
)
data class SetCompletionEntity(
    @ColumnInfo(name = "user_id")        val userId: String,
    @ColumnInfo(name = "exercise_id")    val exerciseId: String,
    @ColumnInfo(name = "program_day_id") val programDayId: String,
    @ColumnInfo(name = "set_index")      val setIndex: Int,
    val date: String,
    @ColumnInfo(name = "weight_kg")      val weightKg: Float?  = null,
    @ColumnInfo(name = "reps_actual")    val repsActual: Int?   = null,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int? = null,
    @ColumnInfo(name = "distance_meters")  val distanceMeters: Float? = null,
    @ColumnInfo(name = "elevation_meters") val elevationMeters: Float? = null,
    @ColumnInfo(name = "incline_percent")  val inclinePercent: Float? = null,
    @ColumnInfo(name = "synced", defaultValue = "1") val synced: Boolean = false,
    @ColumnInfo(name = "dirty", defaultValue = "0") val dirty: Boolean = true,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
    @ColumnInfo(name = "updated_at_ms", defaultValue = "0") val updatedAtMs: Long = System.currentTimeMillis()
)
