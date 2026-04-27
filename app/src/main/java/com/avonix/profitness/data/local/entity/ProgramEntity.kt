package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
    /** SHA-256 of the program's canonical content (days+exercises). Set by Postgres trigger; mirrored to Room. */
    @ColumnInfo(name = "content_hash") val contentHash: String? = null,
    /** When this program was created via [apply_shared_program], the source [shared_programs.id]. */
    @ColumnInfo(name = "applied_from_shared_id") val appliedFromSharedId: String? = null
)
