package com.avonix.profitness.data.profile

import com.avonix.profitness.data.profile.dto.AchievementDto
import com.avonix.profitness.data.profile.dto.ProfileDto
import com.avonix.profitness.data.profile.dto.UserAchievementDto
import com.avonix.profitness.data.workout.dto.UserStatsDto
import com.avonix.profitness.data.workout.dto.WorkoutLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<ProfileDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["profiles"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                    ?: ProfileDto(user_id = userId)
            }
        }

    override suspend fun updateProfile(
        userId      : String,
        displayName : String,
        avatar      : String,
        fitnessGoal : String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["profiles"].upsert(
                buildJsonObject {
                    put("user_id",      userId)
                    put("display_name", displayName)
                    put("avatar_url",   avatar)
                    put("fitness_goal", fitnessGoal)
                }
            )
            Unit
        }
    }

    override suspend fun updateRank(userId: String, rank: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["profiles"].upsert(
                    buildJsonObject {
                        put("user_id",      userId)
                        put("current_rank", rank)
                    }
                )
                Unit
            }
        }

    override suspend fun getUserStats(userId: String): Result<UserStatsDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()
                    ?: UserStatsDto(user_id = userId)
            }
        }

    override suspend fun getWeeklyActivity(
        userId    : String,
        weekStart : String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["workout_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", weekStart)
                    }
                }
                .decodeList<WorkoutLogDto>()
                .map { it.date }
        }
    }

    override suspend fun getWorkoutDates(
        userId   : String,
        fromDate : String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["workout_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", fromDate)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<WorkoutLogDto>()
                .map { it.date }
        }
    }

    override suspend fun getUnlockedAchievementKeys(userId: String): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Join user_achievements → achievements via two separate queries
                val userAch = supabase.postgrest["user_achievements"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<UserAchievementDto>()
                    .map { it.achievement_id }
                    .toSet()

                if (userAch.isEmpty()) return@runCatching emptySet()

                supabase.postgrest["achievements"]
                    .select()
                    .decodeList<AchievementDto>()
                    .filter { it.id in userAch }
                    .map { it.key }
                    .toSet()
            }
        }

    override suspend fun unlockAchievement(userId: String, achievementKey: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Önce achievement id'yi bul
                val ach = supabase.postgrest["achievements"]
                    .select { filter { eq("key", achievementKey) } }
                    .decodeSingleOrNull<AchievementDto>()
                    ?: return@runCatching

                // Zaten unlock edilmişse insert etme
                val existing = supabase.postgrest["user_achievements"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("achievement_id", ach.id)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<UserAchievementDto>()

                if (existing == null) {
                    supabase.postgrest["user_achievements"].insert(
                        buildJsonObject {
                            put("user_id",        userId)
                            put("achievement_id", ach.id)
                        }
                    )
                }
            }
        }

    override suspend fun getAllAchievements(): Result<List<AchievementDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["achievements"]
                    .select()
                    .decodeList<AchievementDto>()
            }
        }
}
