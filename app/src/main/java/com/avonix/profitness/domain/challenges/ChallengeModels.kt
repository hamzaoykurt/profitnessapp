package com.avonix.profitness.domain.challenges

import com.avonix.profitness.data.challenges.dto.ChallengeDetailDto
import com.avonix.profitness.data.challenges.dto.ChallengeLeaderboardEntryDto
import com.avonix.profitness.data.challenges.dto.MyChallengeRowDto
import com.avonix.profitness.data.challenges.dto.PublicChallengeRowDto

/** Challenge'a konu olan hedef tipi. */
enum class ChallengeTargetType(val raw: String, val label: String, val unit: String) {
    TotalWorkouts        ("total_workouts",          "Toplam Antrenman",   "antrenman"),
    TotalXp              ("total_xp",                "Toplam XP",          "XP"),
    CurrentStreak        ("current_streak",          "Aktif Seri",         "gün"),
    TotalDurationMinutes ("total_duration_minutes",  "Toplam Süre",        "dk");

    companion object {
        fun fromRaw(raw: String?): ChallengeTargetType =
            values().firstOrNull { it.raw == raw } ?: TotalWorkouts
    }
}

enum class ChallengeVisibility(val raw: String) {
    Public("public"),
    Private("private");

    companion object {
        fun fromRaw(raw: String?): ChallengeVisibility =
            if (raw == "private") Private else Public
    }
}

/** Challenge kart özeti — hem "Public" listelemede hem "My" listelemede kullanılır. */
data class ChallengeSummary(
    val id                : String,
    val title             : String,
    val description       : String,
    val targetType        : ChallengeTargetType,
    val targetValue       : Long,
    val startDateIso      : String,
    val endDateIso        : String,
    val participantsCount : Int,
    val visibility        : ChallengeVisibility,
    val creatorId         : String,
    val creatorName       : String,
    val creatorAvatar     : String?,
    val isJoined          : Boolean,
    val myProgress        : Long,
    val isCompleted       : Boolean
) {
    val progressPct: Float
        get() = if (targetValue <= 0) 0f
                else (myProgress.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
}

/** Challenge detayında leaderboard satırı. */
data class ChallengeLeaderboardEntry(
    val userId      : String,
    val displayName : String,
    val avatarUrl   : String?,
    val progress    : Long,
    val isMe        : Boolean,
    val isCompleted : Boolean
)

/** Detay + leaderboard + benim satırım. */
data class ChallengeDetail(
    val summary    : ChallengeSummary,
    val leaderboard: List<ChallengeLeaderboardEntry>
)

// ── DTO → domain mapping ─────────────────────────────────────────────────────

internal fun PublicChallengeRowDto.toDomain() = ChallengeSummary(
    id                = id,
    title             = title,
    description       = description ?: "",
    targetType        = ChallengeTargetType.fromRaw(target_type),
    targetValue       = target_value,
    startDateIso      = start_date,
    endDateIso        = end_date,
    participantsCount = participants_count,
    visibility        = ChallengeVisibility.Public,
    creatorId         = creator_id,
    creatorName       = creator_name ?: "Anonim",
    creatorAvatar     = creator_avatar,
    isJoined          = is_joined,
    myProgress        = my_progress,
    isCompleted       = is_completed
)

internal fun MyChallengeRowDto.toDomain() = ChallengeSummary(
    id                = id,
    title             = title,
    description       = description ?: "",
    targetType        = ChallengeTargetType.fromRaw(target_type),
    targetValue       = target_value,
    startDateIso      = start_date,
    endDateIso        = end_date,
    participantsCount = participants_count,
    visibility        = ChallengeVisibility.fromRaw(visibility),
    creatorId         = creator_id,
    creatorName       = creator_name ?: "Anonim",
    creatorAvatar     = creator_avatar,
    isJoined          = true,
    myProgress        = my_progress,
    isCompleted       = is_completed
)

internal fun ChallengeLeaderboardEntryDto.toDomain() = ChallengeLeaderboardEntry(
    userId      = user_id,
    displayName = display_name ?: "Anonim",
    avatarUrl   = avatar_url,
    progress    = progress,
    isMe        = is_me,
    isCompleted = is_completed
)

internal fun ChallengeDetailDto.toDomain(): ChallengeDetail {
    val summary = ChallengeSummary(
        id                = id,
        title             = title,
        description       = description ?: "",
        targetType        = ChallengeTargetType.fromRaw(target_type),
        targetValue       = target_value,
        startDateIso      = start_date,
        endDateIso        = end_date,
        participantsCount = participants_count,
        visibility        = ChallengeVisibility.fromRaw(visibility),
        creatorId         = creator_id,
        creatorName       = creator_name ?: "Anonim",
        creatorAvatar     = creator_avatar,
        isJoined          = is_joined,
        myProgress        = my_progress,
        isCompleted       = is_completed
    )
    return ChallengeDetail(
        summary     = summary,
        leaderboard = leaderboard.map { it.toDomain() }
    )
}
