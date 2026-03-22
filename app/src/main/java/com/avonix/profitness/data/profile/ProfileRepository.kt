package com.avonix.profitness.data.profile

import com.avonix.profitness.data.profile.dto.ProfileDto
import com.avonix.profitness.data.workout.dto.UserStatsDto

interface ProfileRepository {

    /** Supabase profiles tablosundan kullanıcı profilini getirir. */
    suspend fun getProfile(userId: String): Result<ProfileDto>

    /** display_name, avatar ve fitness_goal'ı Supabase'e upsert eder. */
    suspend fun updateProfile(
        userId      : String,
        displayName : String,
        avatar      : String,
        fitnessGoal : String
    ): Result<Unit>

    /** user_stats tablosundan XP, level, streak ve toplam antrenman bilgisini getirir. */
    suspend fun getUserStats(userId: String): Result<UserStatsDto>

    /**
     * weekStart (ISO "yyyy-MM-dd") tarihinden itibaren workout_logs'daki
     * tarih listesini döner — haftanın hangi günleri antrenman yapıldığını belirtmek için.
     */
    suspend fun getWeeklyActivity(userId: String, weekStart: String): Result<List<String>>
}
