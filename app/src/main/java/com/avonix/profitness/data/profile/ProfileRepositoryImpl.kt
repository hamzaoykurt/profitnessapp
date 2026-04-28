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

@Serializable
private data class RatioEntry(val date: String, val ratio: Float)

@Serializable
private data class UserAchievementUpsert(
    val user_id: String,
    val achievement_id: String
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
    @Volatile private var _allWorkoutDates: List<java.time.LocalDate>? = null
    @Volatile private var _allWorkoutDatesUid: String? = null
    @Volatile private var _ratios: Map<String, Float>? = null
    @Volatile private var _ratiosKey: String? = null           // "userId|weekStart"
    @Volatile private var _workoutDates: List<String>? = null
    @Volatile private var _workoutDatesKey: String? = null     // "userId|fromDate"

    private fun invalidateProfile() {
        _profile = null; _profileUid = null
        disk.removeByPrefix("profile_")
    }
    private fun invalidateStats() {
        _stats = null; _statsUid = null
        _allWorkoutDates = null; _allWorkoutDatesUid = null
        _ratios = null; _ratiosKey = null
        _workoutDates = null; _workoutDatesKey = null
        disk.removeByPrefix("stats_")
        disk.removeByPrefix("ratios_")
        disk.removeByPrefix("wdates_")
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
                // Public URL al — temiz URL DB'ye kaydedilir
                val url = supabase.storage.from("profile-photos").publicUrl(path)
                val now = System.currentTimeMillis()
                // avatar_url ve avatar_updated_at güncelle
                supabase.postgrest["profiles"].upsert(
                    buildJsonObject {
                        put("user_id",           userId)
                        put("avatar_url",         url)
                        put("avatar_updated_at",  now)
                    }
                )
                // UI'a cache-busting URL döndür
                "$url?t=$now"
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
                val distinctDates = getAllWorkoutDates(userId)

                val totalWorkouts = distinctDates.size

                // Toleranslı günlük seri:
                //   - En yeni aktivite günü bugünden 7+ gün önceyse seri ölmüş.
                //   - Ardışık iki aktivite günü arasında 7+ gün boşluk varsa
                //     seri o noktada biter. Dinlenme günleri 7 günü aşmadıkça kırılmaz.
                val today = java.time.LocalDate.now()
                val streak = if (distinctDates.isEmpty()) {
                    base.current_streak  // fallback to stored value
                } else {
                    val mostRecent = distinctDates.first()
                    if (java.time.temporal.ChronoUnit.DAYS.between(mostRecent, today) >= 7L) {
                        0
                    } else {
                        var count = 1
                        var prev = mostRecent
                        for (i in 1 until distinctDates.size) {
                            val d = distinctDates[i]
                            val gap = java.time.temporal.ChronoUnit.DAYS.between(d, prev)
                            if (gap >= 7L) break
                            count++
                            prev = d
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
    ): Result<List<String>> {
        val key = "$userId|$fromDate"
        _workoutDates?.takeIf { _workoutDatesKey == key }?.let { return Result.success(it) }
        disk.get<List<String>>("wdates_$key")?.let {
            _workoutDates = it; _workoutDatesKey = key; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val from = java.time.LocalDate.parse(fromDate)
                getAllWorkoutDates(userId)
                    .filter { !it.isBefore(from) }
                    .sorted()
                    .map { it.toString() }
            }.also { r -> r.getOrNull()?.let {
                _workoutDates = it; _workoutDatesKey = key
                disk.put("wdates_$key", it)
            }}
        }
    }

    override suspend fun getWeeklyCompletionRatios(
        userId    : String,
        weekStart : String
    ): Result<Map<String, Float>> {
        val key = "$userId|$weekStart"
        _ratios?.takeIf { _ratiosKey == key }?.let { return Result.success(it) }
        disk.get<List<RatioEntry>>("ratios_$key")?.associate { it.date to it.ratio }?.let {
            _ratios = it; _ratiosKey = key; return Result.success(it)
        }
        return withContext(Dispatchers.IO) {
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

                val logIds = logs.map { it.id }
                val programDayIds = logs.mapNotNull { it.program_day_id }.distinct()

                val totalsByDay = if (programDayIds.isEmpty()) {
                    emptyMap()
                } else {
                    supabase.postgrest["program_exercises"]
                        .select { filter { isIn("program_day_id", programDayIds) } }
                        .decodeList<ProgramExerciseCountDto>()
                        .groupingBy { it.program_day_id }
                        .eachCount()
                }

                val completedByLog = if (logIds.isEmpty()) {
                    emptyMap()
                } else {
                    supabase.postgrest["exercise_logs"]
                        .select { filter { isIn("workout_log_id", logIds) } }
                        .decodeList<ExerciseLogDto>()
                        .groupingBy { it.workout_log_id }
                        .eachCount()
                }

                val result = mutableMapOf<String, Float>()
                for (log in logs) {
                    val programDayId = log.program_day_id ?: continue
                    val total = totalsByDay[programDayId] ?: 0
                    if (total == 0) continue // dinlenme günü veya egzersiz yok
                    val completed = completedByLog[log.id] ?: 0
                    val ratio = (completed.toFloat() / total).coerceIn(0f, 1f)
                    // Aynı gün birden fazla log varsa en yüksek oranı al
                    result[log.date] = maxOf(result[log.date] ?: 0f, ratio)
                }
                @Suppress("UNCHECKED_CAST")
                result as Map<String, Float>
            }.also { r -> r.getOrNull()?.let {
                _ratios = it; _ratiosKey = key
                disk.put("ratios_$key", it.map { (d, v) -> RatioEntry(d, v) })
            }}
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
        unlockAchievements(userId, listOf(achievementKey))

    override suspend fun unlockAchievements(userId: String, achievementKeys: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (achievementKeys.isEmpty()) return@withContext Result.success(Unit)
            _unlockedKeys = null; _unlockedKeysUid = null; disk.removeByPrefix("unlocked_")
            runCatching {
                val targetKeys = achievementKeys.distinct()
                val achievements = supabase.postgrest["achievements"]
                    .select { filter { isIn("key", targetKeys) } }
                    .decodeList<AchievementDto>()

                if (achievements.isEmpty()) return@runCatching

                val achievementIds = achievements.map { it.id }
                val existingIds = supabase.postgrest["user_achievements"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            isIn("achievement_id", achievementIds)
                        }
                    }
                    .decodeList<UserAchievementDto>()
                    .map { it.achievement_id }
                    .toSet()

                val payload = achievements
                    .filter { it.id !in existingIds }
                    .map { UserAchievementUpsert(user_id = userId, achievement_id = it.id) }

                if (payload.isNotEmpty()) {
                    supabase.postgrest["user_achievements"]
                        .upsert(payload, onConflict = "user_id,achievement_id", defaultToNull = false)
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

    private suspend fun getAllWorkoutDates(userId: String): List<java.time.LocalDate> {
        _allWorkoutDates?.takeIf { _allWorkoutDatesUid == userId }?.let { return it }
        return supabase.postgrest["workout_logs"]
            .select {
                filter { eq("user_id", userId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<WorkoutLogDto>()
            .mapNotNull { runCatching { java.time.LocalDate.parse(it.date.take(10)) }.getOrNull() }
            .distinct()
            .sortedDescending()
            .also {
                _allWorkoutDates = it
                _allWorkoutDatesUid = userId
            }
    }
}
