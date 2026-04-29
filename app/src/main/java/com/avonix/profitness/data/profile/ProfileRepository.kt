package com.avonix.profitness.data.profile

import com.avonix.profitness.data.profile.dto.AchievementDto
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
        fitnessGoal : String,
        heightCm    : Double,
        weightKg    : Double,
        gender      : String,
        birthDate   : String = ""
    ): Result<Unit>

    /** Rank'ı profiles tablosuna yazar. */
    suspend fun updateRank(userId: String, rank: String): Result<Unit>

    /** user_stats tablosundan XP, level, streak ve toplam antrenman bilgisini getirir. */
    suspend fun getUserStats(userId: String): Result<UserStatsDto>

    /**
     * weekStart (ISO "yyyy-MM-dd") tarihinden itibaren workout_logs'daki
     * tarih listesini döner — haftanın hangi günleri antrenman yapıldığını belirtmek için.
     */
    suspend fun getWeeklyActivity(userId: String, weekStart: String): Result<List<String>>

    /**
     * Bu haftanın her günü (tarih bazında) için egzersiz tamamlanma oranını döner.
     * Her workout_log → (tamamlanan / toplam egzersiz) oranı — program_day_id üzerinden.
     * Map<date "yyyy-MM-dd", ratio 0.0–1.0>
     */
    suspend fun getWeeklyCompletionRatios(userId: String, weekStart: String): Result<Map<String, Float>>

    /**
     * fromDate (ISO "yyyy-MM-dd") tarihinden itibaren tüm workout_logs tarihlerini döner.
     * Haftalık/aylık grafik için kullanılır.
     */
    suspend fun getWorkoutDates(userId: String, fromDate: String): Result<List<String>>

    /** Kullanıcının açtığı başarım key listesini döner. */
    suspend fun getUnlockedAchievementKeys(userId: String): Result<Set<String>>

    /** Başarım ID'ye göre user_achievements'a insert eder (zaten varsa ignore). */
    suspend fun unlockAchievement(userId: String, achievementKey: String): Result<Unit>

    /** Profil fotografi yukler ve URL'ini profiles.avatar_url'e kaydeder */
    suspend fun uploadProfilePhoto(userId: String, imageBytes: ByteArray, mimeType: String): Result<String>

    /** Tüm tanımlı başarımları achievements tablosundan getirir. */
    suspend fun getAllAchievements(): Result<List<AchievementDto>>

    /** Workout tamamlanması sonrası stats cache'ini temizler. */
    fun invalidateStatsCache()
}
