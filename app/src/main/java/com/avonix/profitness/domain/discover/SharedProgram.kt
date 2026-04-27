package com.avonix.profitness.domain.discover

import com.avonix.profitness.data.discover.dto.DiscoverFeedRowDto
import com.avonix.profitness.data.discover.dto.MySharedProgramRowDto
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
    /**
     * Server-computed: caller has at least one local program whose canonical content
     * hash matches this snapshot. UI uses this to render "UYGULANDI" vs "UYGULA".
     * Auto-flips to false when the user edits an applied program (hash changes).
     */
    val isAppliedByMe   : Boolean,
    /** SHA-256 hash of this snapshot's canonical content. Lets the client cross-check
     *  against locally-cached program hashes for instant UI feedback. */
    val contentHash     : String?,
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

/**
 * Kullanıcının kendi paylaşımları — admin/yönetim modeli.
 * Feed kartından farklı: like/save flag'leri yok, senkron durumu + kaynağa referans var.
 */
data class MySharedProgram(
    val id              : String,
    val originalProgramId: String?,
    val title           : String,
    val description     : String?,
    val tags            : List<String>,
    val difficulty      : Difficulty?,
    val durationWeeks   : Int?,
    val daysPerWeek     : Int?,
    val likesCount      : Int,
    val savesCount      : Int,
    val downloadsCount  : Int,
    val createdAtIso    : String,
    val updatedAtIso    : String?,
    val sourceExists    : Boolean,
    val sourceProgramName: String?,
    val isOutOfSync     : Boolean,
    /** Hash frozen on the shared snapshot at publish time. */
    val sharedContentHash: String?,
    /** Current hash of the source program (NULL if source deleted). */
    val sourceContentHash: String?
)

internal fun MySharedProgramRowDto.toDomain(): MySharedProgram = MySharedProgram(
    id               = id,
    originalProgramId = original_program_id,
    title            = title,
    description      = description,
    tags             = tags,
    difficulty       = Difficulty.fromRaw(difficulty),
    durationWeeks    = duration_weeks,
    daysPerWeek      = days_per_week,
    likesCount       = likes_count,
    savesCount       = saves_count,
    downloadsCount   = downloads_count,
    createdAtIso     = created_at,
    updatedAtIso     = updated_at,
    sourceExists     = source_exists,
    sourceProgramName = source_program_name,
    isOutOfSync      = is_out_of_sync,
    sharedContentHash = shared_content_hash,
    sourceContentHash = source_content_hash
)

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
    isAppliedByMe    = is_applied_by_me,
    contentHash      = content_hash,
    createdAtIso     = created_at
)
