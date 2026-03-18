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

class WorkoutRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : WorkoutRepository {

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

    override suspend fun getWeeklyCompletions(userId: String, weekStart: String): Result<Map<String, Set<String>>> =
        withContext(Dispatchers.IO) {
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
                result
            }
        }

    override suspend fun getStreak(userId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()
                    ?.current_streak ?: 0
            }
        }

    override suspend fun updateStreak(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                val existing = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()

                if (existing == null) {
                    supabase.postgrest["user_stats"]
                        .insert(buildJsonObject {
                            put("user_id", userId)
                            put("current_streak", 1)
                            put("longest_streak", 1)
                            put("total_exercises", 1)
                            put("updated_at", todayStr)
                        })
                } else {
                    val lastDate = existing.updated_at.take(10) // "YYYY-MM-DD"
                    // Bugün zaten seri başlatıldıysa (current_streak > 0) sadece egzersiz sayısını artır
                    if (lastDate == todayStr && existing.current_streak > 0) {
                        supabase.postgrest["user_stats"]
                            .update({
                                set("total_exercises", existing.total_exercises + 1)
                            }) { filter { eq("user_id", userId) } }
                    } else {
                        // Yeni gün, dün yapıldıysa seri devam et, yoksa sıfırdan başlat
                        val newStreak = if (lastDate == yesterdayStr) existing.current_streak + 1 else 1
                        val newLongest = maxOf(existing.longest_streak, newStreak)
                        supabase.postgrest["user_stats"]
                            .update({
                                set("current_streak", newStreak)
                                set("longest_streak", newLongest)
                                set("total_exercises", existing.total_exercises + 1)
                                set("updated_at", todayStr)
                            }) { filter { eq("user_id", userId) } }
                    }
                }
                Unit
            }
        }
}
