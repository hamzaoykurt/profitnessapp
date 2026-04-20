package com.avonix.profitness.data.social.dto

import kotlinx.serialization.Serializable

/** RPC: public.search_users(p_query, p_limit) */
@Serializable
data class UserSearchRowDto(
    val user_id      : String,
    val username     : String? = null,
    val display_name : String? = null,
    val avatar_url   : String? = null,
    val total_xp     : Int = 0,
    val is_following : Boolean = false,
    val is_follower  : Boolean = false
)

/** RPC: public.list_my_follows(p_kind, p_limit) — mevcut migration'dan gelen şema.
 *  is_mutual = karşılıklı takip. */
@Serializable
data class FollowListRowDto(
    val user_id      : String,
    val username     : String? = null,
    val display_name : String? = null,
    val avatar_url   : String? = null,
    val total_xp     : Int = 0,
    val is_mutual    : Boolean = false
)

/** RPC: public.get_public_profile(p_user_id) */
@Serializable
data class PublicProfileDto(
    val user_id            : String,
    val username           : String? = null,
    val display_name       : String? = null,
    val avatar_url         : String? = null,
    val current_rank       : String = "bronze",
    val total_xp           : Int = 0,
    val created_at         : String,
    val is_following       : Boolean = false,
    val is_follower        : Boolean = false,
    val followers_count    : Int = 0,
    val following_count    : Int = 0,
    val achievements_count : Int = 0
)

/** RPC: public.get_friend_leaderboard_xp(p_limit) */
@Serializable
data class FriendXpRowDto(
    val user_id       : String,
    val username      : String? = null,
    val display_name  : String? = null,
    val avatar_url    : String? = null,
    val total_xp      : Int = 0,
    val rank_position : Int = 0,
    val is_me         : Boolean = false
)

/** RPC: public.get_friend_leaderboard_achievements(p_limit) */
@Serializable
data class FriendAchievementRowDto(
    val user_id           : String,
    val username          : String? = null,
    val display_name      : String? = null,
    val avatar_url        : String? = null,
    val achievement_count : Int = 0,
    val rank_position     : Int = 0,
    val is_me             : Boolean = false
)
