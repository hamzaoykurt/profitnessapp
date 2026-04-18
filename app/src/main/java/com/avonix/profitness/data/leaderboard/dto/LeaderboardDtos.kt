package com.avonix.profitness.data.leaderboard.dto

import kotlinx.serialization.Serializable

/**
 * Supabase RPC: public.get_leaderboard_xp(p_limit)
 * Döner: user_id, display_name, avatar_url, xp, rank_position
 */
@Serializable
data class LeaderboardXpRowDto(
    val user_id      : String,
    val display_name : String = "Anonim",
    val avatar_url   : String? = null,
    val xp           : Int = 0,
    val rank_position: Long = 0L
)

/**
 * Supabase RPC: public.get_leaderboard_achievements(p_limit)
 * Döner: user_id, display_name, avatar_url, achievement_count, rank_position
 */
@Serializable
data class LeaderboardAchievementRowDto(
    val user_id          : String,
    val display_name     : String = "Anonim",
    val avatar_url       : String? = null,
    val achievement_count: Long = 0L,
    val rank_position    : Long = 0L
)

/** Supabase RPC: public.get_my_rank_xp() */
@Serializable
data class MyXpRankDto(
    val xp           : Int = 0,
    val rank_position: Long = 0L,
    val total_users  : Long = 0L
)

/** Supabase RPC: public.get_my_rank_achievements() */
@Serializable
data class MyAchievementRankDto(
    val achievement_count: Long = 0L,
    val rank_position    : Long = 0L,
    val total_users      : Long = 0L
)
