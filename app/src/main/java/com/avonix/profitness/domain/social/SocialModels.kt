package com.avonix.profitness.domain.social

import com.avonix.profitness.data.social.dto.FriendAchievementRowDto
import com.avonix.profitness.data.social.dto.FriendXpRowDto
import com.avonix.profitness.data.social.dto.PublicProfileDto
import com.avonix.profitness.data.social.dto.UserSearchRowDto

/** Arama/takip listesi satırı — hem search sonucunda hem "takip edilenler" listesinde kullanılır. */
data class UserSummary(
    val userId      : String,
    val username    : String?,
    val displayName : String,
    val avatarUrl   : String?,
    val totalXp     : Int,
    val isFollowing : Boolean,
    val isFollower  : Boolean
) {
    /** Karşılıklı takip → arkadaş. */
    val isMutual: Boolean get() = isFollowing && isFollower
}

/** Bir kullanıcının public profili — kart detay ekranında kullanılır. */
data class PublicProfile(
    val userId            : String,
    val username          : String?,
    val displayName       : String,
    val avatarUrl         : String?,
    val currentRank       : String,
    val totalXp           : Int,
    val level             : Int,
    val currentStreak     : Int,
    val totalWorkouts     : Int,
    val createdAtIso      : String,
    val isFollowing       : Boolean,
    val isFollower        : Boolean,
    val followersCount    : Int,
    val followingCount    : Int,
    val achievementsCount : Int
) {
    val isMutual: Boolean get() = isFollowing && isFollower
}

/** Arkadaş (mutual follow) leaderboard — XP */
data class FriendXpRow(
    val userId       : String,
    val username     : String?,
    val displayName  : String,
    val avatarUrl    : String?,
    val totalXp      : Int,
    val rankPosition : Int,
    val isMe         : Boolean
)

/** Arkadaş leaderboard — başarım sayısı */
data class FriendAchievementRow(
    val userId           : String,
    val username         : String?,
    val displayName      : String,
    val avatarUrl        : String?,
    val achievementCount : Int,
    val rankPosition     : Int,
    val isMe             : Boolean
)

internal fun UserSearchRowDto.toDomain() = UserSummary(
    userId      = user_id,
    username    = username,
    displayName = display_name ?: username ?: "Anonim",
    avatarUrl   = avatar_url,
    totalXp     = total_xp,
    isFollowing = is_following,
    isFollower  = is_follower
)

internal fun PublicProfileDto.toDomain() = PublicProfile(
    userId            = user_id,
    username          = username,
    displayName       = display_name ?: username ?: "Anonim",
    avatarUrl         = avatar_url,
    currentRank       = current_rank,
    totalXp           = total_xp,
    level             = level,
    currentStreak     = current_streak,
    totalWorkouts     = total_workouts,
    createdAtIso      = created_at,
    isFollowing       = is_following,
    isFollower        = is_follower,
    followersCount    = followers_count,
    followingCount    = following_count,
    achievementsCount = achievements_count
)

internal fun FriendXpRowDto.toDomain() = FriendXpRow(
    userId       = user_id,
    username     = username,
    displayName  = display_name ?: username ?: "Anonim",
    avatarUrl    = avatar_url,
    totalXp      = total_xp,
    rankPosition = rank_position,
    isMe         = is_me
)

internal fun FriendAchievementRowDto.toDomain() = FriendAchievementRow(
    userId           = user_id,
    username         = username,
    displayName      = display_name ?: username ?: "Anonim",
    avatarUrl        = avatar_url,
    achievementCount = achievement_count,
    rankPosition     = rank_position,
    isMe             = is_me
)

/** list_my_follows RPC'si bu enum'u kullanır. */
enum class FollowListKind(val raw: String) {
    FOLLOWING("following"),
    FOLLOWERS("followers")
}
