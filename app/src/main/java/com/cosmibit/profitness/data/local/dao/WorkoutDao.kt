package com.cosmibit.profitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cosmibit.profitness.data.local.entity.ExerciseLogEntity
import com.cosmibit.profitness.data.local.entity.WorkoutLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // ── Observe ──────────────────────────────────────────────────────────────

    /**
     * Bu haftanın tamamlanan egzersizlerini reaktif izle.
     * Flow her exercise_logs değişikliğinde yeniden emit eder.
     */
    @Query("""
        SELECT el.exercise_id
        FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.user_id = :userId
          AND wl.date >= :weekStart
          AND wl.program_day_id = :programDayId
          AND el.is_completed = 1
    """)
    fun observeCompletedExercises(
        userId: String,
        weekStart: String,
        programDayId: String
    ): Flow<List<String>>

    /**
     * Seri hesaplaması için tüm workout tarihlerini getir (en yeniden eskiye).
     */
    @Query("""
        SELECT DISTINCT date FROM workout_logs
        WHERE user_id = :userId
        ORDER BY date DESC
    """)
    fun observeWorkoutDates(userId: String): Flow<List<String>>

    // ── One-shot reads ───────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM workout_logs
        WHERE user_id = :userId AND program_day_id = :programDayId AND date = :date
        LIMIT 1
    """)
    suspend fun getLogForToday(userId: String, programDayId: String, date: String): WorkoutLogEntity?

    @Query("""
        SELECT el.exercise_id
        FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.user_id = :userId
          AND wl.date >= :weekStart
          AND el.is_completed = 1
    """)
    suspend fun getWeeklyCompletedExerciseIds(userId: String, weekStart: String): List<String>

    /**
     * Haftalık tamamlamaları program_day_id bazında döner.
     * Her satır: (program_day_id, exercise_id) çifti.
     */
    @Query("""
        SELECT wl.program_day_id, el.exercise_id
        FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.user_id = :userId
          AND wl.date >= :weekStart
          AND wl.program_day_id IS NOT NULL
          AND el.is_completed = 1
    """)
    suspend fun getWeeklyCompletionPairs(userId: String, weekStart: String): List<CompletionPair>

    @Query("""
        SELECT wl.program_day_id, el.exercise_id
        FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.user_id = :userId
          AND wl.date >= :weekStart
          AND wl.program_day_id IS NOT NULL
          AND el.is_completed = 1
    """)
    fun observeWeeklyCompletionPairs(userId: String, weekStart: String): Flow<List<CompletionPair>>

    @Query("SELECT DISTINCT date FROM workout_logs WHERE user_id = :userId ORDER BY date DESC")
    suspend fun getWorkoutDates(userId: String): List<String>

    @Query("SELECT * FROM workout_logs WHERE user_id = :userId ORDER BY date ASC, id ASC")
    suspend fun getAllLogsForUser(userId: String): List<WorkoutLogEntity>

    @Query("""
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.user_id = :userId
        ORDER BY wl.date ASC, el.id ASC
    """)
    suspend fun getAllExerciseLogsForUser(userId: String): List<ExerciseLogEntity>

    @Query("SELECT COUNT(*) FROM workout_logs WHERE user_id = :userId AND date < :beforeDate")
    suspend fun countWorkoutLogsBefore(userId: String, beforeDate: String): Int

    @Query("SELECT * FROM workout_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<WorkoutLogEntity>

    @Query("SELECT * FROM exercise_logs WHERE synced = 0")
    suspend fun getUnsyncedExerciseLogs(): List<ExerciseLogEntity>

    @Query("""
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_logs wl ON el.workout_log_id = wl.id
        WHERE wl.id = :workoutLogId
    """)
    suspend fun getExerciseLogsForWorkout(workoutLogId: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM workout_logs WHERE user_id = :userId AND date >= :weekStart")
    suspend fun getLogsForWeek(userId: String, weekStart: String): List<WorkoutLogEntity>

    // ── Write ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLogEntity)

    @Upsert
    suspend fun upsertLog(log: WorkoutLogEntity)

    @Upsert
    suspend fun upsertLogs(logs: List<WorkoutLogEntity>)

    @Upsert
    suspend fun upsertExerciseLogs(logs: List<ExerciseLogEntity>)

    @Query("UPDATE workout_logs SET synced = 1 WHERE id = :logId")
    suspend fun markLogSynced(logId: String)

    @Query("UPDATE exercise_logs SET synced = 1 WHERE id = :logId")
    suspend fun markExerciseLogSynced(logId: String)

    @Query("UPDATE workout_logs SET finished_at = :finishedAt WHERE id = :logId")
    suspend fun finishWorkout(logId: String, finishedAt: String)

    /**
     * Geri alınan egzersizi senkron edilene kadar yerel silme kaydı olarak tutar.
     * Böylece çevrimdışı/başarısız uzak silme sonrasında eski kayıt yeniden çekilip
     * tamamlandı durumunu diriltemez.
     */
    @Query("""
        UPDATE exercise_logs
        SET is_completed = 0, synced = 0
        WHERE workout_log_id = :workoutLogId AND exercise_id = :exerciseId
    """)
    suspend fun markExerciseLogIncomplete(workoutLogId: String, exerciseId: String): Int

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteExerciseLogById(id: String)

    /** Bir workout_log'a bağlı kalan exercise_logs sayısı. */
    @Query("SELECT COUNT(*) FROM exercise_logs WHERE workout_log_id = :workoutLogId AND is_completed = 1")
    suspend fun countExerciseLogsForWorkout(workoutLogId: String): Int

    // ── Bulk sync helper ─────────────────────────────────────────────────────

    @Transaction
    suspend fun replaceWeeklyData(
        userId: String,
        weekStart: String,
        logs: List<WorkoutLogEntity>,
        exerciseLogs: List<ExerciseLogEntity>
    ) {
        // Uzak silme henüz tamamlanmamış egzersiz tombstone'larını pull sırasında
        // koru. Aksi halde synced parent log silinirken cascade ile tombstone da
        // silinir ve uzaktaki eski tamamlanma yeniden yerleşir.
        val existingLogs = getLogsForWeek(userId, weekStart)
        val unsyncedExerciseLogs = existingLogs
            .flatMap { getExerciseLogsForWorkout(it.id) }
            .filterNot { it.synced }
        val protectedLogIds = unsyncedExerciseLogs.mapTo(mutableSetOf()) { it.workoutLogId }
        val syncedLogIds = existingLogs
            .filter { it.synced && it.id !in protectedLogIds }
            .map { it.id }
        for (id in syncedLogIds) {
            deleteLogById(id)
        }
        // Remote'dan gelen kayıtları synced olarak ekle
        if (logs.isNotEmpty()) upsertLogs(logs.map { it.copy(synced = true) })
        if (exerciseLogs.isNotEmpty()) upsertExerciseLogs(exerciseLogs.map { it.copy(synced = true) })
        // Aynı unique anahtara sahip uzak kayıt tombstone'u ezmiş olabilir;
        // yerel pending işlem her zaman son sözü söyler.
        if (unsyncedExerciseLogs.isNotEmpty()) upsertExerciseLogs(unsyncedExerciseLogs)
    }

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteLogById(id: String)
}

data class CompletionPair(
    val program_day_id: String,
    val exercise_id: String
)
