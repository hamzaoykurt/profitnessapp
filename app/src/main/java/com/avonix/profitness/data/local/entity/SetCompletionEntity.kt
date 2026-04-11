package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "set_completions",
    primaryKeys = ["user_id", "exercise_id", "program_day_id", "set_index", "date"],
    indices = [Index(value = ["user_id", "program_day_id", "date"])]
)
data class SetCompletionEntity(
    @ColumnInfo(name = "user_id")        val userId: String,
    @ColumnInfo(name = "exercise_id")    val exerciseId: String,
    @ColumnInfo(name = "program_day_id") val programDayId: String,
    @ColumnInfo(name = "set_index")      val setIndex: Int,
    val date: String,
    @ColumnInfo(name = "weight_kg")      val weightKg: Float?  = null,
    @ColumnInfo(name = "reps_actual")    val repsActual: Int?   = null
)
