package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_logs",
    indices = [
        Index("user_id"),
        Index("program_day_id"),
        Index("date"),
        Index(value = ["user_id", "program_day_id", "date"], unique = true)
    ]
)
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "program_day_id") val programDayId: String?,
    val date: String,
    @ColumnInfo(name = "started_at") val startedAt: String? = null,
    @ColumnInfo(name = "finished_at") val finishedAt: String? = null,
    val synced: Boolean = false
)
