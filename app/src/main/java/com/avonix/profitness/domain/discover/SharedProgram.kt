package com.avonix.profitness.domain.discover

import com.avonix.profitness.data.discover.dto.DiscoverFeedRowDto
import kotlinx.serialization.json.JsonElement

/** Domain model — UI tarafından tüketilir (DTO'dan bağımsız). */
data class SharedProgram(
    val id              : String,
    val creatorId       : String,
    val creatorName     : String,
    val creatorAvatarUrl: String?,
    val title           : String,
    val description     : String?,
    val programData     : JsonElement?,
    val tags            : List<String>,
    val difficulty      : Difficulty?,
    val durationWeeks   : Int?,
    val daysPerWeek     : Int?,
    val likesCount      : Int,
    val savesCount      : Int,
    val downloadsCount  : Int,
    val isLikedByMe     : Boolean,
    val isSavedByMe     : Boolean,
    val createdAtIso    : String
)

enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED;
    companion object {
        fun fromRaw(v: String?): Difficulty? = when (v?.lowercase()) {
            "beginner"     -> BEGINNER
            "intermediate" -> INTERMEDIATE
            "advanced"     -> ADVANCED
            else           -> null
        }
    }
    val raw: String get() = name.lowercase()
}

enum class DiscoverSort(val raw: String) { NEWEST("newest"), TRENDING("trending") }

internal fun DiscoverFeedRowDto.toDomain(): SharedProgram = SharedProgram(
    id               = id,
    creatorId        = creator_id,
    creatorName      = creator_display_name ?: "Anonim",
    creatorAvatarUrl = creator_avatar_url,
    title            = title,
    description      = description,
    programData      = program_data,
    tags             = tags,
    difficulty       = Difficulty.fromRaw(difficulty),
    durationWeeks    = duration_weeks,
    daysPerWeek      = days_per_week,
    likesCount       = likes_count,
    savesCount       = saves_count,
    downloadsCount   = downloads_count,
    isLikedByMe      = is_liked_by_me,
    isSavedByMe      = is_saved_by_me,
    createdAtIso     = created_at
)
