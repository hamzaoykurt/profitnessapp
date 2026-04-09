package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_days",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("program_id")]
)
data class ProgramDayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "program_id") val programId: String,
    @ColumnInfo(name = "day_index") val dayIndex: Int,
    val title: String,
    @ColumnInfo(name = "is_rest_day") val isRestDay: Boolean
)
