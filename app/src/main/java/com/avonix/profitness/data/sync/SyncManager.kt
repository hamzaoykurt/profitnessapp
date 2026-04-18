package com.avonix.profitness.data.sync

import com.avonix.profitness.data.local.dao.ExerciseDao
import com.avonix.profitness.data.local.dao.ProgramDao
import com.avonix.profitness.data.local.dao.WorkoutDao
import com.avonix.profitness.data.local.entity.ExerciseEntity
import com.avonix.profitness.data.local.entity.ExerciseLogEntity
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity
import com.avonix.profitness.data.local.entity.WorkoutLogEntity
import com.avonix.profitness.data.program.dto.ExerciseDto
import com.avonix.profitness.data.program.dto.ProgramDayDto
import com.avonix.profitness.data.program.dto.ProgramDto
import com.avonix.profitness.data.program.dto.ProgramExerciseWithNameDto
import com.avonix.profitness.data.workout.dto.ExerciseLogDto
import com.avonix.profitness.data.workout.dto.WorkoutLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

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
    private val supabase: SupabaseClient,
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) {
    private val syncMutex = Mutex()

    // ═════════════════════════════════════════════════════════════════════════
    //  PULL: Supabase → Room
    // ═════════════════════════════════════════════════════════════════════════

    /** Tüm veriyi Supabase'den çekip Room'a yazar. App başlangıcı ve resume'da çağrılır. */
    suspend fun pullAll(userId: String) = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                pullExercises()
                pullPrograms(userId)
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

            for (dto in programDtos) {
                val dayDtos = supabase.postgrest["program_days"]
                    .select {
                        filter { eq("program_id", dto.id) }
                        order("day_index", Order.ASCENDING)
                    }
                    .decodeList<ProgramDayDto>()

                for (dayDto in dayDtos) {
                    allDays.add(dayDto.toEntity())

                    val exerciseDtos = supabase.postgrest["program_exercises"]
                        .select(columns = Columns.raw("*, exercises(name, target_muscle, category, image_url)")) {
                            filter { eq("program_day_id", dayDto.id) }
                            order("order_index", Order.ASCENDING)
                        }
                        .decodeList<ProgramExerciseWithNameDto>()

                    allProgramExercises.addAll(exerciseDtos.map { it.toEntity() })
                }
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

            val allExLogs = mutableListOf<ExerciseLogEntity>()
            for (log in logDtos) {
                val exDtos = supabase.postgrest["exercise_logs"]
                    .select { filter { eq("workout_log_id", log.id) } }
                    .decodeList<ExerciseLogDto>()
                allExLogs.addAll(exDtos.map { it.toEntity(synced = true) })
            }

            workoutDao.replaceWeeklyData(
                userId = userId,
                weekStart = weekStart,
                logs = logDtos.map { it.toEntity(synced = true) },
                exerciseLogs = allExLogs
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUSH: Room → Supabase
    // ═════════════════════════════════════════════════════════════════════════

    /** Henüz sync edilmemiş workout_logs ve exercise_logs'u Supabase'e yazar. */
    suspend fun pushUnsyncedWorkouts() = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Unsynced workout_logs
                val unsyncedLogs = workoutDao.getUnsyncedLogs()
                for (log in unsyncedLogs) {
                    runCatching {
                        // Supabase'de zaten var mı kontrol et
                        val existing = supabase.postgrest["workout_logs"]
                            .select {
                                filter {
                                    eq("user_id", log.userId)
                                    eq("program_day_id", log.programDayId ?: "")
                                    eq("date", log.date)
                                }
                                limit(1)
                            }
                            .decodeSingleOrNull<WorkoutLogDto>()

                        if (existing == null) {
                            supabase.postgrest["workout_logs"]
                                .insert(buildJsonObject {
                                    put("id", log.id)
                                    put("user_id", log.userId)
                                    put("program_day_id", log.programDayId)
                                    put("date", log.date)
                                })
                        }
                        workoutDao.markLogSynced(log.id)
                    }
                }

                // 2. Unsynced exercise_logs
                val unsyncedExLogs = workoutDao.getUnsyncedExerciseLogs()
                for (exLog in unsyncedExLogs) {
                    runCatching {
                        supabase.postgrest["exercise_logs"]
                            .insert(buildJsonObject {
                                put("id", exLog.id)
                                put("workout_log_id", exLog.workoutLogId)
                                put("exercise_id", exLog.exerciseId)
                                put("sets_completed", exLog.setsCompleted)
                                put("reps_completed", exLog.repsCompleted)
                                put("is_completed", true)
                                put("duration_seconds", exLog.durationSeconds)
                            })
                        workoutDao.markExerciseLogSynced(exLog.id)
                    }
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
        createdAt = created_at
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
        orderIndex = order_index
    )

    private fun ExerciseDto.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        nameEn = name_en,
        targetMuscle = target_muscle,
        category = category,
        setsDefault = sets_default,
        repsDefault = reps_default,
        description = description
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
}
