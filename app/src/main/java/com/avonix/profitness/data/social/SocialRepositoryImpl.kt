package com.avonix.profitness.data.social

import com.avonix.profitness.data.social.dto.FollowListRowDto
import com.avonix.profitness.data.social.dto.FriendAchievementRowDto
import com.avonix.profitness.data.social.dto.FriendXpRowDto
import com.avonix.profitness.data.social.dto.PublicProfileDto
import com.avonix.profitness.data.social.dto.UserSearchRowDto
import com.avonix.profitness.domain.social.FollowListKind
import com.avonix.profitness.domain.social.FriendAchievementRow
import com.avonix.profitness.domain.social.FriendXpRow
import com.avonix.profitness.domain.social.PublicProfile
import com.avonix.profitness.domain.social.UserSummary
import com.avonix.profitness.domain.social.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class SocialRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : SocialRepository {

    override suspend fun searchUsers(query: String, limit: Int): Result<List<UserSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (query.isBlank()) return@runCatching emptyList<UserSummary>()
                supabase.postgrest.rpc(
                    "search_users",
                    buildJsonObject {
                        put("p_query", query.trim())
                        put("p_limit", limit)
                    }
                ).decodeList<UserSearchRowDto>().map { it.toDomain() }
            }
        }

    override suspend fun toggleFollow(targetUserId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "toggle_follow",
                    buildJsonObject { put("p_target_user", targetUserId) }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun listMyFollows(kind: FollowListKind, limit: Int): Result<List<UserSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rows = supabase.postgrest.rpc(
                    "list_my_follows",
                    buildJsonObject {
                        put("p_kind", kind.raw)
                        put("p_limit", limit)
                    }
                ).decodeList<FollowListRowDto>()

                // kind=following  → bu listedekileri ben takip ediyorum; is_mutual = karşı taraf da beni takip ediyor mu
                // kind=followers  → bu listedekiler beni takip ediyor; is_mutual = ben de takip ediyor muyum
                rows.map { r ->
                    val iAmFollowingThem = when (kind) {
                        FollowListKind.FOLLOWING -> true
                        FollowListKind.FOLLOWERS -> r.is_mutual
                    }
                    val theyFollowMe = when (kind) {
                        FollowListKind.FOLLOWING -> r.is_mutual
                        FollowListKind.FOLLOWERS -> true
                    }
                    UserSummary(
                        userId      = r.user_id,
                        username    = r.username,
                        displayName = r.display_name ?: r.username ?: "Anonim",
                        avatarUrl   = r.avatar_url,
                        totalXp     = r.total_xp,
                        isFollowing = iAmFollowingThem,
                        isFollower  = theyFollowMe
                    )
                }
            }
        }

    override suspend fun getPublicProfile(userId: String): Result<PublicProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_public_profile",
                    buildJsonObject { put("p_user_id", userId) }
                ).decodeSingle<PublicProfileDto>().toDomain()
            }
        }

    override suspend fun updateMyUsername(newUsername: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "update_my_username",
                    buildJsonObject { put("p_new_username", newUsername.trim()) }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun getFriendLeaderboardXp(limit: Int): Result<List<FriendXpRow>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_friend_leaderboard_xp",
                    buildJsonObject { put("p_limit", limit) }
                ).decodeList<FriendXpRowDto>().map { it.toDomain() }
            }
        }

    override suspend fun getFriendLeaderboardAchievements(limit: Int): Result<List<FriendAchievementRow>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_friend_leaderboard_achievements",
                    buildJsonObject { put("p_limit", limit) }
                ).decodeList<FriendAchievementRowDto>().map { it.toDomain() }
            }
        }
}
