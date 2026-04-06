package com.avonix.profitness.data.workout

import com.avonix.profitness.data.workout.dto.ExerciseLogDto
import com.avonix.profitness.data.workout.dto.UserStatsDto
import com.avonix.profitness.data.workout.dto.WorkoutLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.avonix.profitness.data.cache.DiskCache
import kotlinx.serialization.Serializable

@Serializable
private data class CompletionEntry(val dayId: String, val exerciseIds: List<String>)

class WorkoutRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val disk: DiskCache
) : WorkoutRepository {

    // ── In-memory session cache ──────────────────────────────────────────────
    @Volatile private var _completions: Map<String, Set<String>>? = null
    @Volatile private var _completionsKey: String? = null          // "userId|weekStart"
    @Volatile private var _streak: Int? = null
    @Volatile private var _streakUid: String? = null

    private fun invalidateWorkoutCache() {
        _completions = null; _completionsKey = null
        _streak = null; _streakUid = null
        disk.removeByPrefix("completions_")
        disk.removeByPrefix("streak_")
    }

    override suspend fun startWorkout(userId: String, programDayId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // "2026-03-18"

                // Bugün için zaten log varsa yenisini oluşturma — sadece id döndür
                val existing = supabase.postgrest["workout_logs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("program_day_id", programDayId)
                            eq("date", today)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<WorkoutLogDto>()

                if (existing != null) return@runCatching existing.id

                // Bugüne ait log yok — yeni oluştur
                supabase.postgrest["workout_logs"]
                    .insert(buildJsonObject {
                        put("user_id", userId)
                        put("program_day_id", programDayId)
                        put("date", today)
                    })

                // Fetch the just-created log via SELECT
                supabase.postgrest["workout_logs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("program_day_id", programDayId)
                            eq("date", today)
                        }
                        order("started_at", Order.DESCENDING)
                        limit(1)
                    }
                    .decodeSingle<WorkoutLogDto>()
                    .id
            }
        }

    override suspend fun logExercise(
        workoutLogId: String,
        exerciseId: String,
        setsCompleted: Int,
        repsCompleted: Int,
        durationSeconds: Int
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            invalidateWorkoutCache()
            runCatching {
                supabase.postgrest["exercise_logs"]
                    .insert(buildJsonObject {
                        put("workout_log_id", workoutLogId)
                        put("exercise_id", exerciseId)
                        put("sets_completed", setsCompleted)
                        put("reps_completed", repsCompleted)
                        put("is_completed", true)
                        put("duration_seconds", durationSeconds)
                    })
                Unit
            }
        }

    override suspend fun finishWorkout(workoutLogId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["workout_logs"]
                    .update({ set("finished_at", "now()") }) {
                        filter { eq("id", workoutLogId) }
                    }
                Unit
            }
        }

    private fun completionsToDisk(key: String, map: Map<String, Set<String>>) {
        val entries = map.map { (k, v) -> CompletionEntry(k, v.toList()) }
        disk.put(key, entries)
    }
    private fun completionsFromDisk(key: String): Map<String, Set<String>>? {
        return disk.get<List<CompletionEntry>>(key)
            ?.associate { it.dayId to it.exerciseIds.toSet() }
    }

    override suspend fun getWeeklyCompletions(userId: String, weekStart: String): Result<Map<String, Set<String>>> =
        withContext(Dispatchers.IO) {
        val key = "$userId|$weekStart"
        _completions?.takeIf { _completionsKey == key }?.let { return@withContext Result.success(it) }
        completionsFromDisk("completions_$key")?.let {
            _completions = it; _completionsKey = key; return@withContext Result.success(it)
        }
        runCatching {
                // Bu haftaya ait tüm workout_log'ları çek
                val logs = supabase.postgrest["workout_logs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            gte("date", weekStart)
                        }
                    }
                    .decodeList<WorkoutLogDto>()

                if (logs.isEmpty()) return@runCatching emptyMap()

                // Her log için exercise_logs çek ve program_day_id → Set<exercise_id> map'i oluştur
                val result = mutableMapOf<String, MutableSet<String>>()
                for (log in logs) {
                    val dayId = log.program_day_id ?: continue
                    val exercises = supabase.postgrest["exercise_logs"]
                        .select {
                            filter { eq("workout_log_id", log.id) }
                        }
                        .decodeList<ExerciseLogDto>()
                    if (exercises.isNotEmpty()) {
                        result.getOrPut(dayId) { mutableSetOf() }
                            .addAll(exercises.map { it.exercise_id })
                    }
                }
                @Suppress("UNCHECKED_CAST")
                result as Map<String, Set<String>>
            }.also { r -> r.getOrNull()?.let {
                _completions = it; _completionsKey = key
                completionsToDisk("completions_$key", it)
            }}
        }

    override suspend fun getStreak(userId: String): Result<Int> =
        withContext(Dispatchers.IO) {
        _streak?.takeIf { _streakUid == userId }?.let { return@withContext Result.success(it) }
        disk.get<Int>("streak_$userId")?.let {
            _streak = it; _streakUid = userId; return@withContext Result.success(it)
        }
        runCatching {
                val today = LocalDate.now()

                // workout_logs tarihlerinden ardışık gün serisi hesapla
                val distinctDates = supabase.postgrest["workout_logs"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("date", Order.DESCENDING)
                    }
                    .decodeList<WorkoutLogDto>()
                    .mapNotNull { runCatching { LocalDate.parse(it.date.take(10)) }.getOrNull() }
                    .distinct()
                    .sortedDescending()

                if (distinctDates.isEmpty()) {
                    // Workout_log yoksa user_stats'tan fallback
                    return@runCatching supabase.postgrest["user_stats"]
                        .select { filter { eq("user_id", userId) } }
                        .decodeSingleOrNull<UserStatsDto>()
                        ?.current_streak ?: 0
                }

                // En son workout tarihinden geriye doğru ardışık gün say
                val mostRecent = distinctDates.first()
                // Eğer son antrenman bugün veya dün değilse seri 0
                if (mostRecent.isBefore(today.minusDays(1))) return@runCatching 0

                var streak = 0
                var expected = mostRecent
                for (date in distinctDates) {
                    if (date == expected) {
                        streak++
                        expected = expected.minusDays(1)
                    } else {
                        break // boşluk bulundu
                    }
                }
                streak
            }.also { r -> r.getOrNull()?.let {
                _streak = it; _streakUid = userId
                disk.put("streak_$userId", it)
            }}
        }

    override suspend fun addXp(userId: String, xpAmount: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            invalidateWorkoutCache()
            runCatching {
                val existing = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()
                    ?: return@runCatching  // stats yoksa skip (updateStreak halleder)
                val newXp    = existing.xp + xpAmount
                val newLevel = (newXp / 500) + 1
                supabase.postgrest["user_stats"]
                    .update({
                        set("xp",    newXp)
                        set("level", newLevel)
                    }) { filter { eq("user_id", userId) } }
                Unit
            }
        }

    override suspend fun updateStreak(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            invalidateWorkoutCache()
            runCatching {
                val xpPerExercise = 10

                val existing = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()

                if (existing == null) {
                    // İlk kez — yeni satır oluştur (seri ve total_workouts workout_logs'dan hesaplanır)
                    supabase.postgrest["user_stats"]
                        .insert(buildJsonObject {
                            put("user_id", userId)
                            put("total_exercises", 1)
                            put("xp", xpPerExercise)
                            put("level", 1)
                        })
                } else {
                    // XP ve total_exercises artır — seri artık workout_logs'dan hesaplanıyor
                    val newXp    = existing.xp + xpPerExercise
                    val newLevel = (newXp / 500) + 1
                    supabase.postgrest["user_stats"]
                        .update({
                            set("total_exercises", existing.total_exercises + 1)
                            set("xp", newXp)
                            set("level", newLevel)
                        }) { filter { eq("user_id", userId) } }
                }
                Unit
            }
        }
}
