package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_en") val nameEn: String = "",
    @ColumnInfo(name = "target_muscle") val targetMuscle: String,
    val category: String,
    @ColumnInfo(name = "sets_default") val setsDefault: Int,
    @ColumnInfo(name = "reps_default") val repsDefault: Int,
    val description: String = "",
    @ColumnInfo(name = "image_url") val imageUrl: String = ""
)
