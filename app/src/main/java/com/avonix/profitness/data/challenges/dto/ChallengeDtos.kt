package com.avonix.profitness.data.challenges.dto

import kotlinx.serialization.Serializable

/** RPC: public.list_public_challenges(p_limit, p_offset) */
@Serializable
data class PublicChallengeRowDto(
    val id                 : String,
    val title              : String,
    val description        : String? = null,
    val target_type        : String,
    val target_value       : Long,
    val start_date         : String,
    val end_date           : String,
    val participants_count : Int = 0,
    val creator_id         : String,
    val creator_name       : String? = null,
    val creator_avatar     : String? = null,
    val is_joined          : Boolean = false,
    val my_progress        : Long = 0,
    val is_completed       : Boolean = false,
    val created_at         : String? = null
)

/** RPC: public.list_my_challenges() */
@Serializable
data class MyChallengeRowDto(
    val id                 : String,
    val title              : String,
    val description        : String? = null,
    val target_type        : String,
    val target_value       : Long,
    val start_date         : String,
    val end_date           : String,
    val participants_count : Int = 0,
    val visibility         : String = "public",
    val creator_id         : String,
    val creator_name       : String? = null,
    val creator_avatar     : String? = null,
    val my_progress        : Long = 0,
    val is_completed       : Boolean = false,
    val joined_at          : String? = null
)

/** RPC: public.get_challenge_detail(p_challenge_id).leaderboard[] */
@Serializable
data class ChallengeLeaderboardEntryDto(
    val user_id      : String,
    val display_name : String? = null,
    val avatar_url   : String? = null,
    val progress     : Long = 0,
    val joined_at    : String? = null,
    val is_completed : Boolean = false,
    val is_me        : Boolean = false
)

/** RPC: public.get_challenge_detail(p_challenge_id) — jsonb tek satır. */
@Serializable
data class ChallengeDetailDto(
    val id                 : String,
    val title              : String,
    val description        : String? = null,
    val target_type        : String,
    val target_value       : Long,
    val start_date         : String,
    val end_date           : String,
    val visibility         : String = "public",
    val participants_count : Int = 0,
    val creator_id         : String,
    val creator_name       : String? = null,
    val creator_avatar     : String? = null,
    val is_joined          : Boolean = false,
    val my_progress        : Long = 0,
    val is_completed       : Boolean = false,
    val leaderboard        : List<ChallengeLeaderboardEntryDto> = emptyList()
)
