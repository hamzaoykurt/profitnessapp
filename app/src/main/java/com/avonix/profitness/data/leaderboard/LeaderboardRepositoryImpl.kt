package com.avonix.profitness.data.leaderboard

import com.avonix.profitness.data.leaderboard.dto.LeaderboardAchievementRowDto
import com.avonix.profitness.data.leaderboard.dto.LeaderboardXpRowDto
import com.avonix.profitness.data.leaderboard.dto.MyAchievementRankDto
import com.avonix.profitness.data.leaderboard.dto.MyXpRankDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class LeaderboardRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : LeaderboardRepository {

    override suspend fun getXpLeaderboard(limit: Int): Result<List<LeaderboardXpRowDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_leaderboard_xp",
                    buildJsonObject { put("p_limit", limit) }
                ).decodeList<LeaderboardXpRowDto>()
            }
        }

    override suspend fun getAchievementLeaderboard(limit: Int): Result<List<LeaderboardAchievementRowDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_leaderboard_achievements",
                    buildJsonObject { put("p_limit", limit) }
                ).decodeList<LeaderboardAchievementRowDto>()
            }
        }

    override suspend fun getMyXpRank(): Result<MyXpRankDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc("get_my_rank_xp")
                    .decodeSingleOrNull<MyXpRankDto>()
                    ?: MyXpRankDto()
            }
        }

    override suspend fun getMyAchievementRank(): Result<MyAchievementRankDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc("get_my_rank_achievements")
                    .decodeSingleOrNull<MyAchievementRankDto>()
                    ?: MyAchievementRankDto()
            }
        }
}
