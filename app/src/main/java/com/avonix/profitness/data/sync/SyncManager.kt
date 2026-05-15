package com.avonix.profitness.data.sync

import android.content.Context
import com.avonix.profitness.data.local.dao.ExerciseDao
import com.avonix.profitness.data.local.dao.ProgramDao
import com.avonix.profitness.data.local.dao.SetCompletionDao
import com.avonix.profitness.data.local.dao.WorkoutDao
import com.avonix.profitness.data.local.entity.ExerciseEntity
import com.avonix.profitness.data.local.entity.ExerciseLogEntity
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.local.entity.WorkoutLogEntity
import com.avonix.profitness.data.program.dto.ExerciseDto
import com.avonix.profitness.data.program.dto.ProgramDayDto
import com.avonix.profitness.data.program.dto.ProgramDto
import com.avonix.profitness.data.program.dto.ProgramExerciseWithNameDto
import com.avonix.profitness.data.workout.dto.ExerciseLogDto
import com.avonix.profitness.data.workout.dto.WorkoutLogDto
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class WorkoutLogUpsert(
    val id: String,
    val user_id: String,
    val program_day_id: String?,
    val date: String,
    val started_at: String? = null,
    val finished_at: String? = null
)

@Serializable
private data class ExerciseLogUpsert(
    val id: String,
    val workout_log_id: String,
    val exercise_id: String,
    val sets_completed: Int,
    val reps_completed: Int,
    val is_completed: Boolean = true,
    val duration_seconds: Int = 0
)

@Serializable
private data class SetCompletionUpsert(
    val user_id: String,
    val exercise_id: String,
    val program_day_id: String,
    val set_index: Int,
    val date: String,
    val weight_kg: Float? = null,
    val reps_actual: Int? = null,
    val duration_seconds: Int? = null,
    val distance_meters: Float? = null,
    val elevation_meters: Float? = null,
    val incline_percent: Float? = null,
    val updated_at: String? = null
)

/**
 * Supabase ↔ Room senkronizasyon katmanı.
 *
 * - **Pull**: Supabase → Room (program, exercises, workout/exercise logs)
 * - **Push**: Room → Supabase (unsynced workout/exercise logs)
 *
 * Tüm sync operasyonları Mutex ile sıralanır; eş zamanlı çağrılar bekletilir.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val setCompletionDao: SetCompletionDao
) {
    private val syncMutex = Mutex()
    private val prefs by lazy {
        context.getSharedPreferences("profitness_set_completion_sync", Context.MODE_PRIVATE)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PULL: Supabase → Room
    // ═════════════════════════════════════════════════════════════════════════

    /** Tüm veriyi Supabase'den çekip Room'a yazar. App başlangıcı ve resume'da çağrılır. */
    suspend fun pullAll(userId: String) = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                pullExercises()
                pullPrograms(userId)
                pushUnsyncedWorkoutsWithoutLock().getOrThrow()
                pushSetCompletions(userId).getOrThrow()
                pullSetCompletions(userId).getOrThrow()
                pullWorkoutLogs(userId)
                // Seri sürekliliği için tüm geçmiş tarihleri de çek (uninstall sonrası restore)
                pullWorkoutLogDates(userId)
            }
        }
    }

    /**
     * Geçmişteki tüm workout_logs satırlarını (bu hafta öncesi dahil) Room'a upsert eder.
     * Yalnızca tarih bazlı seri hesaplaması için gerekli minimum alanları getirir.
     * Bu haftanın detaylı exercise_logs'ları `pullWorkoutLogs`'ta yönetilir.
     */
    suspend fun pullWorkoutLogDates(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            val historicalLogs = supabase.postgrest["workout_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                        lt("date", weekStart)
                    }
                }
                .decodeList<WorkoutLogDto>()

            if (historicalLogs.isNotEmpty()) {
                workoutDao.upsertLogs(historicalLogs.map { it.toEntity(synced = true) })
            }
        }
    }

    /** Sadece programları çeker — program oluşturma/güncelleme sonrası. */
    suspend fun pullPrograms(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val programDtos = supabase.postgrest["programs"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<ProgramDto>()

            val allDays = mutableListOf<ProgramDayEntity>()
            val allProgramExercises = mutableListOf<ProgramExerciseEntity>()
            val programIds = programDtos.map { it.id }

            val dayDtos = if (programIds.isEmpty()) {
                emptyList()
            } else {
                supabase.postgrest["program_days"]
                    .select {
                        filter { isIn("program_id", programIds) }
                        order("day_index", Order.ASCENDING)
                    }
                    .decodeList<ProgramDayDto>()
            }

            allDays.addAll(dayDtos.map { it.toEntity() })
            val dayIds = dayDtos.map { it.id }

            if (dayIds.isNotEmpty()) {
                val exerciseDtos = supabase.postgrest["program_exercises"]
                        .select(columns = Columns.raw("*, exercises(name, target_muscle, category, image_url, sport_type, tracking_mode)")) {
                            filter { isIn("program_day_id", dayIds) }
                            order("order_index", Order.ASCENDING)
                        }
                        .decodeList<ProgramExerciseWithNameDto>()

                allProgramExercises.addAll(exerciseDtos.map { it.toEntity() })
            }

            programDao.replaceAllForUser(
                userId = userId,
                programs = programDtos.map { it.toEntity() },
                days = allDays,
                exercises = allProgramExercises
            )
        }
    }

    /** Egzersiz master listesini çeker. */
    suspend fun pullExercises() = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = supabase.postgrest["exercises"]
                .select { order("category", Order.ASCENDING) }
                .decodeList<ExerciseDto>()
            exerciseDao.upsertAll(dtos.map { it.toEntity() })
        }
    }

    /** Bu haftanın workout log'larını çeker. */
    suspend fun pullWorkoutLogs(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            val logDtos = supabase.postgrest["workout_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", weekStart)
                    }
                }
                .decodeList<WorkoutLogDto>()

            val logIds = logDtos.map { it.id }
            val allExLogs = if (logIds.isEmpty()) {
                emptyList()
            } else {
                supabase.postgrest["exercise_logs"]
                    .select { filter { isIn("workout_log_id", logIds) } }
                    .decodeList<ExerciseLogDto>()
                    .map { it.toEntity(synced = true) }
            }

            workoutDao.replaceWeeklyData(
                userId = userId,
                weekStart = weekStart,
                logs = logDtos.map { it.toEntity(synced = true) },
                exerciseLogs = allExLogs
            )
        }
    }

    /** Set bazlı ağırlık/tekrar kayıtlarını Supabase'den Room'a delta olarak geri yükler. */
    suspend fun pullSetCompletions(userId: String, forceFull: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching {
            val lastPullAt = if (forceFull) null else readLastSetCompletionPullAt(userId)
            val remote = supabase.postgrest["set_completions"]
                .select {
                    filter {
                        eq("user_id", userId)
                        if (!lastPullAt.isNullOrBlank()) {
                            gt("updated_at", lastPullAt)
                        }
                    }
                    order("date", Order.ASCENDING)
                    order("set_index", Order.ASCENDING)
                }
                .decodeList<SetCompletionUpsert>()

            if (remote.isNotEmpty()) {
                setCompletionDao.upsertAll(remote.map { it.toEntity() })
                remote.mapNotNull { it.updated_at }.maxOrNull()?.let { newest ->
                    persistLastSetCompletionPullAt(userId, newest)
                }
            }
        }
    }

    suspend fun pullWorkoutLogDatesIfLocalEmpty(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            if (workoutDao.getWorkoutDates(userId).isEmpty()) {
                pullWorkoutLogDates(userId).getOrThrow()
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUSH: Room → Supabase
    // ═════════════════════════════════════════════════════════════════════════

    /** Henüz sync edilmemiş workout_logs ve exercise_logs'u Supabase'e yazar. */
    suspend fun pushUnsyncedWorkouts() = syncMutex.withLock {
        pushUnsyncedWorkoutsWithoutLock()
    }

    private suspend fun pushUnsyncedWorkoutsWithoutLock() =
        withContext(Dispatchers.IO) {
            runCatching {
                val unsyncedLogs = workoutDao.getUnsyncedLogs()
                if (unsyncedLogs.isNotEmpty()) {
                    val payload = unsyncedLogs.map { log ->
                        WorkoutLogUpsert(
                            id = log.id,
                            user_id = log.userId,
                            program_day_id = log.programDayId,
                            date = log.date,
                            started_at = log.startedAt,
                            finished_at = log.finishedAt
                        )
                    }
                    supabase.postgrest["workout_logs"]
                        .upsert(payload, onConflict = "id", defaultToNull = false)
                    unsyncedLogs.forEach { workoutDao.markLogSynced(it.id) }
                }

                val unsyncedExLogs = workoutDao.getUnsyncedExerciseLogs()
                if (unsyncedExLogs.isNotEmpty()) {
                    val payload = unsyncedExLogs.map { exLog ->
                        ExerciseLogUpsert(
                            id = exLog.id,
                            workout_log_id = exLog.workoutLogId,
                            exercise_id = exLog.exerciseId,
                            sets_completed = exLog.setsCompleted,
                            reps_completed = exLog.repsCompleted,
                            is_completed = true,
                            duration_seconds = exLog.durationSeconds
                        )
                    }
                    supabase.postgrest["exercise_logs"]
                        .upsert(payload, onConflict = "id", defaultToNull = false)
                    unsyncedExLogs.forEach { workoutDao.markExerciseLogSynced(it.id) }
                }
            }
        }

    /** Dirty lokal set kayıtlarını profile bağlı kalıcı Supabase tablosuna yazar. */
    suspend fun pushSetCompletions(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val entries = setCompletionDao.getDirtyForUser(userId)
            if (entries.isEmpty()) return@runCatching

            entries.filter { it.deleted }.forEach { entity ->
                supabase.postgrest["set_completions"].delete {
                    filter {
                        eq("user_id", entity.userId)
                        eq("exercise_id", entity.exerciseId)
                        eq("program_day_id", entity.programDayId)
                        eq("set_index", entity.setIndex)
                        eq("date", entity.date)
                    }
                }
                setCompletionDao.hardDelete(
                    entity.userId,
                    entity.exerciseId,
                    entity.programDayId,
                    entity.setIndex,
                    entity.date
                )
            }

            val upserts = entries.filterNot { it.deleted }
            if (upserts.isNotEmpty()) {
                supabase.postgrest["set_completions"]
                    .upsert(
                        upserts.map { it.toUpsert() },
                        onConflict = "user_id,exercise_id,program_day_id,set_index,date",
                        defaultToNull = false
                    )
                setCompletionDao.upsertAll(upserts.map { it.copy(synced = true, dirty = false, deleted = false) })
            }
        }
    }

    suspend fun pushSetCompletion(entity: SetCompletionEntity) = withContext(Dispatchers.IO) {
        runCatching {
            if (entity.deleted) {
                deleteSetCompletion(entity.userId, entity.exerciseId, entity.programDayId, entity.setIndex, entity.date).getOrThrow()
                setCompletionDao.hardDelete(entity.userId, entity.exerciseId, entity.programDayId, entity.setIndex, entity.date)
                return@runCatching
            }
            supabase.postgrest["set_completions"]
                .upsert(
                    entity.toUpsert(),
                    onConflict = "user_id,exercise_id,program_day_id,set_index,date",
                    defaultToNull = false
                )
            setCompletionDao.upsertAll(listOf(entity.copy(synced = true, dirty = false, deleted = false)))
        }
    }

    suspend fun deleteSetCompletion(
        userId: String,
        exerciseId: String,
        programDayId: String,
        setIndex: Int,
        date: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["set_completions"].delete {
                filter {
                    eq("user_id", userId)
                    eq("exercise_id", exerciseId)
                    eq("program_day_id", programDayId)
                    eq("set_index", setIndex)
                    eq("date", date)
                }
            }
        }
    }

    suspend fun deleteSetCompletionsForExercise(
        userId: String,
        exerciseId: String,
        programDayId: String,
        date: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["set_completions"].delete {
                filter {
                    eq("user_id", userId)
                    eq("exercise_id", exerciseId)
                    eq("program_day_id", programDayId)
                    eq("date", date)
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DTO → Entity mappers
    // ═════════════════════════════════════════════════════════════════════════

    private fun ProgramDto.toEntity() = ProgramEntity(
        id = id,
        userId = user_id,
        name = name,
        type = type,
        isActive = is_active,
        createdAt = created_at,
        contentHash = content_hash,
        appliedFromSharedId = applied_from_shared_id
    )

    private fun ProgramDayDto.toEntity() = ProgramDayEntity(
        id = id,
        programId = program_id,
        dayIndex = day_index,
        title = title,
        isRestDay = is_rest_day
    )

    private fun ProgramExerciseWithNameDto.toEntity() = ProgramExerciseEntity(
        id = id,
        programDayId = program_day_id,
        exerciseId = exercise_id,
        sets = sets,
        reps = reps,
        weightKg = weight_kg ?: 0f,
        restSeconds = rest_seconds,
        orderIndex = order_index,
        targetDurationSeconds = target_duration_seconds,
        targetDistanceMeters = target_distance_meters,
        targetElevationMeters = target_elevation_meters,
        targetInclinePercent = target_incline_percent
    )

    private fun ExerciseDto.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        nameEn = name_en,
        targetMuscle = target_muscle,
        category = category,
        setsDefault = sets_default,
        repsDefault = reps_default,
        description = description,
        sportType = sport_type,
        trackingMode = tracking_mode,
        createdBy = created_by
    )

    private fun WorkoutLogDto.toEntity(synced: Boolean) = WorkoutLogEntity(
        id = id,
        userId = user_id,
        programDayId = program_day_id,
        date = date,
        startedAt = started_at,
        finishedAt = finished_at,
        synced = synced
    )

    private fun ExerciseLogDto.toEntity(synced: Boolean) = ExerciseLogEntity(
        id = id,
        workoutLogId = workout_log_id,
        exerciseId = exercise_id,
        setsCompleted = sets_completed ?: 0,
        repsCompleted = reps_completed ?: 0,
        isCompleted = is_completed,
        durationSeconds = duration_seconds ?: 0,
        synced = synced
    )

    private fun SetCompletionEntity.toUpsert() = SetCompletionUpsert(
        user_id = userId,
        exercise_id = exerciseId,
        program_day_id = programDayId,
        set_index = setIndex,
        date = date,
        weight_kg = weightKg,
        reps_actual = repsActual,
        duration_seconds = durationSeconds,
        distance_meters = distanceMeters,
        elevation_meters = elevationMeters,
        incline_percent = inclinePercent
    )

    private fun SetCompletionUpsert.toEntity() = SetCompletionEntity(
        userId = user_id,
        exerciseId = exercise_id,
        programDayId = program_day_id,
        setIndex = set_index,
        date = date,
        weightKg = weight_kg,
        repsActual = reps_actual,
        durationSeconds = duration_seconds,
        distanceMeters = distance_meters,
        elevationMeters = elevation_meters,
        inclinePercent = incline_percent,
        synced = true,
        dirty = false,
        deleted = false,
        updatedAtMs = 0L
    )

    private suspend fun readLastSetCompletionPullAt(userId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(setCompletionPullKey(userId), null)
    }

    private fun persistLastSetCompletionPullAt(userId: String, value: String) {
        prefs.edit().putString(setCompletionPullKey(userId), value).apply()
    }

    private fun setCompletionPullKey(userId: String): String = "set_pull_after:$userId"
}
