package com.avonix.profitness.data.profile

import com.avonix.profitness.data.profile.dto.ProfileDto
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
}
