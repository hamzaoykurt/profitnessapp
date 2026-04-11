package com.avonix.profitness.data.workout

import com.avonix.profitness.data.local.dao.ExerciseProgressSummary
import com.avonix.profitness.data.local.dao.SetCompletionDao
import com.avonix.profitness.data.local.dao.WorkoutDao
import com.avonix.profitness.data.local.entity.ExerciseLogEntity
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.local.entity.WorkoutLogEntity
import com.avonix.profitness.data.sync.SyncManager
import com.avonix.profitness.data.workout.dto.UserStatsDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val workoutDao: WorkoutDao,
    private val setCompletionDao: SetCompletionDao,
    private val syncManager: SyncManager
) : WorkoutRepository {

    // ═════════════════════════════════════════════════════════════════════════
    //  OBSERVE — Room Flow (reactive)
    // ═════════════════════════════════════════════════════════════════════════

    override fun observeWeeklyCompletions(
        userId: String,
        weekStart: String
    ): Flow<Map<String, Set<String>>> =
        workoutDao.observeWeeklyCompletionPairs(userId, weekStart)
            .map { pairs ->
                pairs.groupBy(
                    keySelector = { it.program_day_id },
                    valueTransform = { it.exercise_id }
                ).mapValues { it.value.toSet() }
            }
            .flowOn(Dispatchers.IO)

    override fun observeStreak(userId: String): Flow<Int> =
        workoutDao.observeWorkoutDates(userId)
            .map { dates -> calculateStreak(dates) }
            .flowOn(Dispatchers.IO)

    override fun observeSetCompletions(userId: String, weekStart: String): Flow<Map<String, Set<Int>>> =
        setCompletionDao.observeForWeek(userId, weekStart)
            .map { list ->
                list.groupBy { it.exerciseId }
                    .mapValues { entry -> entry.value.map { it.setIndex }.toSet() }
            }
            .flowOn(Dispatchers.IO)

    // ═════════════════════════════════════════════════════════════════════════
    //  WRITE — Room-first, Supabase async
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun completeExercise(
        userId: String,
        programDayId: String,
        exerciseId: String,
        setsCompleted: Int,
        repsCompleted: Int,
        durationSeconds: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 1. Bugün için workout_log var mı kontrol et (Room'dan)
            val existingLog = workoutDao.getLogForToday(userId, programDayId, today)
            val logId = existingLog?.id ?: UUID.randomUUID().toString()

            if (existingLog == null) {
                // Yeni workout_log oluştur (Room'a yaz, unsynced)
                workoutDao.insertLog(WorkoutLogEntity(
                    id = logId,
                    userId = userId,
                    programDayId = programDayId,
                    date = today,
                    synced = false
                ))
            }

            // 2. Exercise log yaz (Room'a, unsynced)
            workoutDao.insertExerciseLog(ExerciseLogEntity(
                id = UUID.randomUUID().toString(),
                workoutLogId = logId,
                exerciseId = exerciseId,
                setsCompleted = setsCompleted,
                repsCompleted = repsCompleted,
                isCompleted = true,
                durationSeconds = durationSeconds,
                synced = false
            ))

            // 3. Arka planda Supabase'e sync et
            runCatching { syncManager.pushUnsyncedWorkouts() }

            logId
        }
    }

    override suspend fun uncompleteExercise(
        userId: String,
        programDayId: String,
        exerciseId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val log = workoutDao.getLogForToday(userId, programDayId, today) ?: return@runCatching Unit

            // Room'dan sil
            workoutDao.deleteExerciseLog(log.id, exerciseId)

            // Supabase'den de sil (best-effort)
            runCatching {
                supabase.postgrest["exercise_logs"]
                    .delete {
                        filter {
                            eq("workout_log_id", log.id)
                            eq("exercise_id", exerciseId)
                        }
                    }
            }
            Unit
        }
    }

    override suspend fun finishWorkout(workoutLogId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val now = java.time.Instant.now().toString()
                workoutDao.finishWorkout(workoutLogId, now)
                runCatching {
                    supabase.postgrest["workout_logs"]
                        .update({ set("finished_at", "now()") }) {
                            filter { eq("id", workoutLogId) }
                        }
                }
                Unit
            }
        }

    // ═════════════════════════════════════════════════════════════════════════
    //  STATS
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun updateStreak(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val xpPerExercise = 10

                val existing = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()

                if (existing == null) {
                    supabase.postgrest["user_stats"]
                        .insert(buildJsonObject {
                            put("user_id", userId)
                            put("total_exercises", 1)
                            put("xp", xpPerExercise)
                            put("level", 1)
                        })
                } else {
                    val newXp = existing.xp + xpPerExercise
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

    override suspend fun addXp(userId: String, xpAmount: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val existing = supabase.postgrest["user_stats"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingleOrNull<UserStatsDto>()
                    ?: return@runCatching
                val newXp = existing.xp + xpAmount
                val newLevel = (newXp / 500) + 1
                supabase.postgrest["user_stats"]
                    .update({
                        set("xp", newXp)
                        set("level", newLevel)
                    }) { filter { eq("user_id", userId) } }
                Unit
            }
        }

    override suspend fun getStreak(userId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dates = workoutDao.getWorkoutDates(userId)
                calculateStreak(dates)
            }
        }

    // ═════════════════════════════════════════════════════════════════════════
    //  SYNC
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun syncFromRemote(userId: String) {
        syncManager.pullWorkoutLogs(userId)
    }

    override suspend fun syncToRemote() {
        syncManager.pushUnsyncedWorkouts()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SET COMPLETION
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun addSetCompletion(
        userId: String, exerciseId: String, programDayId: String, setIndex: Int,
        weightKg: Float?, repsActual: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            setCompletionDao.insert(
                SetCompletionEntity(userId, exerciseId, programDayId, setIndex, today, weightKg, repsActual)
            )
        }
    }

    override suspend fun removeSetCompletion(
        userId: String, exerciseId: String, programDayId: String, setIndex: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            setCompletionDao.delete(userId, exerciseId, programDayId, setIndex, today)
        }
    }

    override suspend fun clearExerciseSetCompletions(
        userId: String, exerciseId: String, programDayId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            setCompletionDao.deleteAllForExercise(userId, exerciseId, programDayId, today)
        }
    }

    override suspend fun fillExerciseSetCompletions(
        userId: String, exerciseId: String, programDayId: String, totalSets: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repeat(totalSets) { i ->
                setCompletionDao.insert(SetCompletionEntity(userId, exerciseId, programDayId, i, today))
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PROGRESSIVE OVERLOAD
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun getLastSessionSets(
        userId: String, exerciseId: String
    ): Result<List<SetCompletionEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            setCompletionDao.getLastSessionSets(userId, exerciseId, today)
        }
    }

    override suspend fun getExerciseWeightHistory(
        userId: String, exerciseId: String, weeks: Int
    ): Result<List<SetCompletionEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val since = LocalDate.now().minusWeeks(weeks.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            setCompletionDao.getHistoryForExercise(userId, exerciseId, since)
        }
    }

    override suspend fun getTrackedExerciseSummaries(userId: String): Result<List<ExerciseProgressSummary>> =
        withContext(Dispatchers.IO) {
            runCatching { setCompletionDao.getTrackedExerciseSummaries(userId) }
        }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private fun calculateStreak(dateStrings: List<String>): Int {
        if (dateStrings.isEmpty()) return 0

        val dates = dateStrings
            .mapNotNull { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            .distinct()
            .sortedDescending()

        if (dates.isEmpty()) return 0

        val today = LocalDate.now()
        val mostRecent = dates.first()

        // Son antrenman bugün veya dün değilse seri 0
        if (mostRecent.isBefore(today.minusDays(1))) return 0

        var streak = 0
        var expected = mostRecent
        for (date in dates) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}
