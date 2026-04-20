package com.avonix.profitness.data.social

import com.avonix.profitness.domain.social.FollowListKind
import com.avonix.profitness.domain.social.FriendAchievementRow
import com.avonix.profitness.domain.social.FriendXpRow
import com.avonix.profitness.domain.social.PublicProfile
import com.avonix.profitness.domain.social.UserSummary

interface SocialRepository {

    /** Username/display_name ile arama. Boş query → boş liste. */
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<UserSummary>>

    /** Takip et/bırak. Döner: yeni "isFollowing" durumu. */
    suspend fun toggleFollow(targetUserId: String): Result<Boolean>

    /** "following" veya "followers" listesi. */
    suspend fun listMyFollows(kind: FollowListKind, limit: Int = 100): Result<List<UserSummary>>

    /** Başka bir kullanıcının public profili. */
    suspend fun getPublicProfile(userId: String): Result<PublicProfile>

    /** Mevcut kullanıcının username'ini güncelle. Döner: başarılı mı (false = çakışma). */
    suspend fun updateMyUsername(newUsername: String): Result<Boolean>

    /** Arkadaş XP leaderboard. */
    suspend fun getFriendLeaderboardXp(limit: Int = 100): Result<List<FriendXpRow>>

    /** Arkadaş başarım leaderboard. */
    suspend fun getFriendLeaderboardAchievements(limit: Int = 100): Result<List<FriendAchievementRow>>
}
