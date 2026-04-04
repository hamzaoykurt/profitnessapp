package com.avonix.profitness.data.profile

import com.avonix.profitness.data.profile.dto.AchievementDto
import com.avonix.profitness.data.profile.dto.ProfileDto
import com.avonix.profitness.data.profile.dto.UserAchievementDto
import com.avonix.profitness.data.workout.dto.ExerciseLogDto
import com.avonix.profitness.data.workout.dto.UserStatsDto
import com.avonix.profitness.data.workout.dto.WorkoutLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import com.avonix.profitness.data.cache.DiskCache

@Serializable
private data class ProgramExerciseCountDto(
    val id: String,
    val program_day_id: String
)

class ProfileRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val disk: DiskCache
) : ProfileRepository {

    // ── In-memory session cache ──────────────────────────────────────────────
    @Volatile private var _profile: ProfileDto? = null
    @Volatile private var _profileUid: String? = null
    @Volatile private var _allAchievements: List<AchievementDto>? = null
    @Volatile private var _unlockedKeys: Set<String>? = null
    @Volatile private var _unlockedKeysUid: String? = null
    @Volatile private var _stats: UserStatsDto? = null
    @Volatile private var _statsUid: String? = null

    private fun invalidateProfile() {
        _profile = null; _profileUid = null
        disk.removeByPrefix("profile_")
    }
    private fun invalidateStats() {
        _stats = null; _statsUid = null
        disk.removeByPrefix("stats_")
    }

    override suspend fun getProfile(userId: String): Result<ProfileDto> {
        _profile?.takeIf { _profileUid == userId }?.let { return Result.success(it) }
        disk.get<ProfileDto>("profile_$userId")?.let {
            _profile = it; _profileUid = userId; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["profiles"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                    ?: ProfileDto(user_id = userId)
            }.also { r -> r.getOrNull()?.let {
                _profile = it; _profileUid = userId
                disk.put("profile_$userId", it)
            }}
        }
    }

    override suspend fun updateProfile(
        userId      : String,
        displayName : String,
        avatar      : String,
        fitnessGoal : String,
        heightCm    : Double,
        weightKg    : Double,
        gender      : String,
        birthDate   : String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        invalidateProfile()
        runCatching {
            supabase.postgrest["profiles"].upsert(
                buildJsonObject {
                    put("user_id",      userId)
                    put("display_name", displayName)
                    put("avatar_url",   avatar)
                    put("fitness_goal", fitnessGoal)
                    if (heightCm > 0) put("height_cm", heightCm)
                    if (weightKg > 0) put("weight_kg", weightKg)
                    if (gender.isNotBlank()) put("gender", gender)
                    if (birthDate.isNotBlank() && birthDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        put("birth_date", birthDate)
                    }
                }
            )
            Unit
        }
    }

    override suspend fun uploadProfilePhoto(userId: String, imageBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            invalidateProfile()
            runCatching {
                val path = "avatars/$userId.jpg"
                // Var olan dosyayi sil (varsa)
                runCatching { supabase.storage.from("profile-photos").delete(path) }
                // Yukle
                supabase.storage.from("profile-photos").upload(path, imageBytes, upsert = true)
                // Public URL al
                val url = supabase.storage.from("profile-photos").publicUrl(path)
                // avatar_url'i guncelle
                supabase.postgrest["profiles"].upsert(
                    buildJsonObject {
                        put("user_id",   userId)
                        put("avatar_url", url)
                    }
                )
                url
            }
        }

    override suspend fun updateRank(userId: String, rank: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            invalidateProfile()
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

    override suspend fun getUserStats(userId: String): Result<UserStatsDto> {
        _stats?.takeIf { _statsUid == userId }?.let { return Result.success(it) }
        disk.get<UserStatsDto>("stats_$userId")?.let {
            _stats = it; _statsUid = userId; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val base = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()
                    ?: UserStatsDto(user_id = userId)

                // total_workouts ve streak'i her zaman workout_logs'tan doğrudan hesapla
                // Böylece program silme veya değişiklikleri bu değerleri bozmaz
                val workoutLogs = supabase.postgrest["workout_logs"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<WorkoutLogDto>()

                val distinctDates = workoutLogs
                    .mapNotNull { runCatching { java.time.LocalDate.parse(it.date.take(10)) }.getOrNull() }
                    .distinct()
                    .sortedDescending()

                val totalWorkouts = distinctDates.size

                // Ardışık gün serisi hesapla
                val today = java.time.LocalDate.now()
                val streak = if (distinctDates.isEmpty()) {
                    base.current_streak  // fallback to stored value
                } else {
                    val mostRecent = distinctDates.first()
                    if (mostRecent.isBefore(today.minusDays(1))) {
                        0  // Son antrenman dünden eski → seri kırılmış
                    } else {
                        var count = 0
                        var expected = mostRecent
                        for (d in distinctDates) {
                            if (d == expected) { count++; expected = expected.minusDays(1) }
                            else break
                        }
                        count
                    }
                }

                // longest_streak: saklanmış değer ile hesaplananın büyüğü
                val longestStreak = maxOf(base.longest_streak, streak)

                base.copy(
                    total_workouts  = totalWorkouts,
                    current_streak  = streak,
                    longest_streak  = longestStreak
                )
            }.also { r -> r.getOrNull()?.let {
                _stats = it; _statsUid = userId
                disk.put("stats_$userId", it)
            }}
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

    override suspend fun getWeeklyCompletionRatios(
        userId    : String,
        weekStart : String
    ): Result<Map<String, Float>> = withContext(Dispatchers.IO) {
        runCatching {
            // Bu haftanın workout_logs'larını çek
            val logs = supabase.postgrest["workout_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", weekStart)
                    }
                }
                .decodeList<WorkoutLogDto>()

            if (logs.isEmpty()) return@runCatching emptyMap()

            val result = mutableMapOf<String, Float>()
            for (log in logs) {
                val programDayId = log.program_day_id ?: continue

                // Bu program günündeki toplam egzersiz sayısı
                val total = supabase.postgrest["program_exercises"]
                    .select { filter { eq("program_day_id", programDayId) } }
                    .decodeList<ProgramExerciseCountDto>()
                    .size

                if (total == 0) continue // dinlenme günü veya egzersiz yok

                // Bu workout_log için tamamlanan egzersizler
                val completed = supabase.postgrest["exercise_logs"]
                    .select { filter { eq("workout_log_id", log.id) } }
                    .decodeList<ExerciseLogDto>()
                    .size

                val ratio = (completed.toFloat() / total).coerceIn(0f, 1f)
                // Aynı gün birden fazla log varsa en yüksek oranı al
                result[log.date] = maxOf(result[log.date] ?: 0f, ratio)
            }
            result
        }
    }

    override suspend fun getUnlockedAchievementKeys(userId: String): Result<Set<String>> {
        _unlockedKeys?.takeIf { _unlockedKeysUid == userId }?.let { return Result.success(it) }
        disk.get<List<String>>("unlocked_$userId")?.toSet()?.let {
            _unlockedKeys = it; _unlockedKeysUid = userId; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
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
            }.also { r -> r.getOrNull()?.let {
                _unlockedKeys = it; _unlockedKeysUid = userId
                disk.put("unlocked_$userId", it.toList())
            }}
        }
    }

    override suspend fun unlockAchievement(userId: String, achievementKey: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            _unlockedKeys = null; _unlockedKeysUid = null; disk.removeByPrefix("unlocked_")
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

    override suspend fun getAllAchievements(): Result<List<AchievementDto>> {
        _allAchievements?.let { return Result.success(it) }
        disk.get<List<AchievementDto>>("achievements")?.let {
            _allAchievements = it; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["achievements"]
                    .select()
                    .decodeList<AchievementDto>()
            }.also { r -> r.getOrNull()?.let { _allAchievements = it; disk.put("achievements", it) } }
        }
    }

    override fun invalidateStatsCache() { invalidateStats() }
}
