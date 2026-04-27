package com.avonix.profitness.domain.challenges

import com.avonix.profitness.data.challenges.dto.ChallengeDetailDto
import com.avonix.profitness.data.challenges.dto.ChallengeLeaderboardEntryDto
import com.avonix.profitness.data.challenges.dto.ChallengeMovementDto
import com.avonix.profitness.data.challenges.dto.MyChallengeRowDto
import com.avonix.profitness.data.challenges.dto.PublicChallengeRowDto

/** Challenge üst tip ayrımı — klasik sayaç vs etkinlik. */
enum class ChallengeKind(val raw: String) {
    Metric("metric"),
    Event ("event");

    companion object {
        fun fromRaw(raw: String?): ChallengeKind =
            values().firstOrNull { it.raw == raw } ?: Metric
    }
}

/** Etkinlik alt modları. */
enum class EventMode(val raw: String) {
    Physical    ("physical"),
    Online      ("online"),
    MovementList("movement_list");

    companion object {
        fun fromRaw(raw: String?): EventMode? =
            values().firstOrNull { it.raw == raw }
    }
}

/** Challenge'a konu olan hedef tipi. */
enum class ChallengeTargetType(val raw: String, val label: String, val unit: String) {
    TotalWorkouts        ("total_workouts",          "Toplam Antrenman", "antrenman"),
    TotalXp              ("total_xp",                "Toplam XP",        "XP"),
    CurrentStreak        ("current_streak",          "Aktif Seri",       "gün"),
    TotalDurationMinutes ("total_duration_minutes",  "Toplam Süre",      "dk"),
    TotalDistanceM       ("total_distance_m",        "Mesafe",           "m"),
    TotalDistanceKm      ("total_distance_km",       "Mesafe",           "km"),
    MovementsCompleted   ("movements_completed",     "Hareket",          "hareket");

    companion object {
        fun fromRaw(raw: String?): ChallengeTargetType =
            values().firstOrNull { it.raw == raw } ?: TotalWorkouts

        /** Classic metric challenge oluşturma ekranında gösterilecek hedef tipler. */
        val selectableForMetric: List<ChallengeTargetType> = listOf(
            TotalWorkouts, TotalXp, CurrentStreak,
            TotalDurationMinutes, TotalDistanceM, TotalDistanceKm
        )
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

/** Event challenge için zaman/konum/mod bilgisi (kind==Event olduğunda dolu). */
data class ChallengeEventInfo(
    val mode             : EventMode,
    val dateIso          : String,
    val timeIso          : String?,
    val timezone         : String,
    val location         : String?,
    val geoLat           : Double?,
    val geoLng           : Double?,
    val endLocation      : String?,
    val endGeoLat        : Double?,
    val endGeoLng        : Double?,
    val onlineUrl        : String?,
    val movementsCount   : Int,
    val myCompletedCount : Int
)

/** Event challenge'ın hareket listesi satırı. */
data class ChallengeMovement(
    val id              : String,
    val exerciseId      : String,
    val exerciseName    : String,
    val sortIndex       : Int,
    val suggestedSets   : Int?,
    val suggestedReps   : Int?,
    val suggestedDurSec : Int?,
    val myCompleted     : Boolean
)

/** Challenge kart özeti — hem "Public" hem "My" listelemede kullanılır. */
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
    val isCompleted       : Boolean,
    val kind              : ChallengeKind = ChallengeKind.Metric,
    val event             : ChallengeEventInfo? = null
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

/** Detay + leaderboard + hareket listesi (event modunda dolu). */
data class ChallengeDetail(
    val summary    : ChallengeSummary,
    val leaderboard: List<ChallengeLeaderboardEntry>,
    val movements  : List<ChallengeMovement> = emptyList()
)

// ── Create event request / movement input models (typed — Plan A) ────────────

/** Hareket seçici → create_event_challenge batch'i için tek öğe. */
data class MovementInput(
    val exerciseId      : String,
    val sortIndex       : Int,
    val suggestedSets   : Int?,
    val suggestedReps   : Int?,
    val suggestedDurSec : Int?
)

/** create_event_challenge RPC için typed request. */
data class CreateEventChallengeRequest(
    val title        : String,
    val description  : String?,
    val mode         : EventMode,
    val dateIso      : String,
    val timeIso      : String?,
    val timezone     : String,
    val location     : String?,
    val geoLat       : Double?,
    val geoLng       : Double?,
    val endLocation  : String? = null,
    val endGeoLat    : Double? = null,
    val endGeoLng    : Double? = null,
    val onlineUrl    : String?,
    val targetType   : ChallengeTargetType? = null,
    val targetValue  : Long? = null,
    val visibility   : ChallengeVisibility = ChallengeVisibility.Public,
    val password     : String? = null,
    val movements    : List<MovementInput> = emptyList()
)

/** update_event_challenge RPC için typed request (sahip yönetimi). */
data class UpdateEventChallengeRequest(
    val challengeId  : String,
    val title        : String,
    val description  : String?,
    val dateIso      : String,
    val timeIso      : String?,
    val location     : String?,
    val geoLat       : Double?,
    val geoLng       : Double?,
    val endLocation  : String?,
    val endGeoLat    : Double?,
    val endGeoLng    : Double?,
    val targetValue  : Long?,
    val onlineUrl    : String?
)

// ── DTO → domain mapping ─────────────────────────────────────────────────────

private fun buildEventInfo(
    kind             : String,
    eventMode        : String?,
    eventDate        : String?,
    eventTime        : String?,
    eventTimezone    : String,
    eventLocation    : String?,
    eventGeoLat      : Double?,
    eventGeoLng      : Double?,
    eventEndLocation : String?,
    eventEndGeoLat   : Double?,
    eventEndGeoLng   : Double?,
    eventOnlineUrl   : String?,
    movementsCount   : Int,
    myCompletedCount : Int
): ChallengeEventInfo? {
    if (kind != "event") return null
    val mode = EventMode.fromRaw(eventMode) ?: return null
    val date = eventDate ?: return null
    return ChallengeEventInfo(
        mode             = mode,
        dateIso          = date,
        timeIso          = eventTime,
        timezone         = eventTimezone,
        location         = eventLocation,
        geoLat           = eventGeoLat,
        geoLng           = eventGeoLng,
        endLocation      = eventEndLocation,
        endGeoLat        = eventEndGeoLat,
        endGeoLng        = eventEndGeoLng,
        onlineUrl        = eventOnlineUrl,
        movementsCount   = movementsCount,
        myCompletedCount = myCompletedCount
    )
}

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
    isCompleted       = is_completed,
    kind              = ChallengeKind.fromRaw(kind),
    event             = buildEventInfo(
        kind, event_mode, event_date, event_time, event_timezone,
        event_location, event_geo_lat, event_geo_lng,
        event_end_location, event_end_geo_lat, event_end_geo_lng,
        event_online_url,
        movements_count, my_completed_count
    )
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
    isCompleted       = is_completed,
    kind              = ChallengeKind.fromRaw(kind),
    event             = buildEventInfo(
        kind, event_mode, event_date, event_time, event_timezone,
        event_location, event_geo_lat, event_geo_lng,
        event_end_location, event_end_geo_lat, event_end_geo_lng,
        event_online_url,
        movements_count, my_completed_count
    )
)

internal fun ChallengeLeaderboardEntryDto.toDomain() = ChallengeLeaderboardEntry(
    userId      = user_id,
    displayName = display_name ?: "Anonim",
    avatarUrl   = avatar_url,
    progress    = progress,
    isMe        = is_me,
    isCompleted = is_completed
)

internal fun ChallengeMovementDto.toDomain() = ChallengeMovement(
    id              = id,
    exerciseId      = exercise_id,
    exerciseName    = exercise_name ?: "Egzersiz",
    sortIndex       = sort_index,
    suggestedSets   = suggested_sets,
    suggestedReps   = suggested_reps,
    suggestedDurSec = suggested_dur_s,
    myCompleted     = my_completed
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
        isCompleted       = is_completed,
        kind              = ChallengeKind.fromRaw(kind),
        event             = buildEventInfo(
            kind, event_mode, event_date, event_time, event_timezone,
            event_location, event_geo_lat, event_geo_lng,
            event_end_location, event_end_geo_lat, event_end_geo_lng,
            event_online_url,
            movements.size,
            movements.count { it.my_completed }
        )
    )
    return ChallengeDetail(
        summary     = summary,
        leaderboard = leaderboard.map { it.toDomain() },
        movements   = movements.map { it.toDomain() }
    )
}
