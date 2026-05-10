package com.avonix.profitness.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import kotlinx.coroutines.flow.Flow

/** Hareket başına performans özeti — profil/progresyon ekranı için */
data class ExerciseProgressSummary(
    @ColumnInfo(name = "exercise_id")   val exerciseId    : String,
    @ColumnInfo(name = "name")          val name          : String,
    @ColumnInfo(name = "image_url")     val imageUrl      : String,
    @ColumnInfo(name = "target_muscle") val targetMuscle  : String,
    @ColumnInfo(name = "max_weight")    val maxWeight     : Float,
    @ColumnInfo(name = "avg_weight")    val avgWeight     : Float,
    @ColumnInfo(name = "last_weight")   val lastWeight    : Float,
    @ColumnInfo(name = "last_date")     val lastDate      : String?,
    @ColumnInfo(name = "session_count") val sessionCount  : Int,
    @ColumnInfo(name = "total_sets")    val totalSets     : Int,
    @ColumnInfo(name = "total_reps")    val totalReps     : Int,
    @ColumnInfo(name = "total_volume")  val totalVolume   : Float,
    @ColumnInfo(name = "total_duration_seconds") val totalDurationSeconds: Int,
    @ColumnInfo(name = "total_distance_meters")  val totalDistanceMeters : Float,
    @ColumnInfo(name = "total_elevation_meters") val totalElevationMeters: Float
)

@Dao
interface SetCompletionDao {

    /**
     * Belirli bir haftanın TAMAMLANMIŞ setlerini reaktif izle.
     * reps_actual NULL → "draft" (ağırlık girilmiş ama tik atılmamış), UI'da tick gösterilmez.
     */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND date >= :weekStart AND reps_actual IS NOT NULL
    """)
    fun observeForWeek(userId: String, weekStart: String): Flow<List<SetCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SetCompletionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SetCompletionEntity>)

    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId
        ORDER BY date ASC, exercise_id ASC, set_index ASC
    """)
    suspend fun getAllForUser(userId: String): List<SetCompletionEntity>

    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND set_index = :setIndex AND date = :date
        LIMIT 1
    """)
    suspend fun getSet(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String
    ): SetCompletionEntity?

    /** Partial update — sadece weight_kg'yi günceller, reps_actual'a dokunmaz. */
    @Query("""
        UPDATE set_completions
        SET weight_kg = :weightKg
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND set_index = :setIndex AND date = :date
    """)
    suspend fun updateWeight(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, weightKg: Float?
    ): Int

    /** Partial update — sadece reps_actual'ı günceller, weight_kg'ye dokunmaz. */
    @Query("""
        UPDATE set_completions
        SET reps_actual = :repsActual
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND set_index = :setIndex AND date = :date
    """)
    suspend fun updateRepsActual(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, repsActual: Int?
    ): Int

    /** Upsert weight — kayıt varsa sadece ağırlığı günceller, yoksa yeni draft oluşturur. */
    @Transaction
    suspend fun upsertWeight(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, weightKg: Float?
    ): SetCompletionEntity {
        val rows = updateWeight(userId, exerciseId, programDayId, setIndex, date, weightKg)
        if (rows == 0) {
            insert(SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, weightKg, null, null, null))
        }
        return getSet(userId, exerciseId, programDayId, setIndex, date)
            ?: SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, weightKg, null, null, null)
    }

    /** Upsert reps_actual — kayıt varsa sadece reps_actual'ı günceller, yoksa yeni kayıt oluşturur. */
    @Transaction
    suspend fun upsertRepsActual(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, repsActual: Int?
    ): SetCompletionEntity {
        val rows = updateRepsActual(userId, exerciseId, programDayId, setIndex, date, repsActual)
        if (rows == 0) {
            insert(SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, null, repsActual, null, null))
        }
        return getSet(userId, exerciseId, programDayId, setIndex, date)
            ?: SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, null, repsActual, null, null)
    }

    /** Partial update — süre ve mesafeyi günceller, ağırlık/tekrar/tik durumuna dokunmaz. */
    @Query("""
        UPDATE set_completions
        SET duration_seconds = :durationSeconds,
            distance_meters = :distanceMeters,
            elevation_meters = :elevationMeters,
            incline_percent = :inclinePercent
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND set_index = :setIndex AND date = :date
    """)
    suspend fun updateActivityMetrics(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, durationSeconds: Int?, distanceMeters: Float?,
        elevationMeters: Float?, inclinePercent: Float?
    ): Int

    /** Upsert süre/mesafe — kayıt varsa metrikleri günceller, yoksa draft oluşturur. */
    @Transaction
    suspend fun upsertActivityMetrics(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, durationSeconds: Int?, distanceMeters: Float?,
        elevationMeters: Float? = null, inclinePercent: Float? = null
    ): SetCompletionEntity {
        val rows = updateActivityMetrics(
            userId, exerciseId, programDayId, setIndex, date,
            durationSeconds, distanceMeters, elevationMeters, inclinePercent
        )
        if (rows == 0) {
            insert(
                SetCompletionEntity(
                    userId, exerciseId, programDayId, setIndex, date,
                    null, null, durationSeconds, distanceMeters, elevationMeters, inclinePercent
                )
            )
        }
        return getSet(userId, exerciseId, programDayId, setIndex, date)
            ?: SetCompletionEntity(
                userId, exerciseId, programDayId, setIndex, date,
                null, null, durationSeconds, distanceMeters, elevationMeters, inclinePercent
            )
    }

    @Query("""
        DELETE FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND set_index = :setIndex AND date = :date
    """)
    suspend fun delete(userId: String, exerciseId: String, programDayId: String, setIndex: Int, date: String)

    @Query("""
        DELETE FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND program_day_id = :programDayId AND date = :date
    """)
    suspend fun deleteAllForExercise(userId: String, exerciseId: String, programDayId: String, date: String)

    @Query("""
        DELETE FROM set_completions
        WHERE user_id = :userId AND program_day_id = :programDayId AND date = :date
    """)
    suspend fun deleteAllForDay(userId: String, programDayId: String, date: String)

    /** Belirli bir güne ait set kayıtlarını çeker (draft + completed). */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId AND date = :date
        ORDER BY set_index ASC
    """)
    suspend fun getForDate(userId: String, exerciseId: String, date: String): List<SetCompletionEntity>

    /** Belirli bir güne ait set kayıtlarını egzersiz listesi için tek sorguda çeker. */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id IN (:exerciseIds) AND date = :date
        ORDER BY exercise_id ASC, set_index ASC
    """)
    suspend fun getForDate(userId: String, exerciseIds: List<String>, date: String): List<SetCompletionEntity>

    /** Önceki antrenmanın set verilerini çeker (ağırlık ön-doldurma için) */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND date = (
              SELECT MAX(date) FROM set_completions
              WHERE user_id = :userId AND exercise_id = :exerciseId AND date < :today
          )
        ORDER BY set_index ASC
    """)
    suspend fun getLastSessionSets(userId: String, exerciseId: String, today: String): List<SetCompletionEntity>

    /** Önceki antrenman setlerini egzersiz listesi için tek sorguda çeker. */
    @Query("""
        SELECT sc.* FROM set_completions sc
        INNER JOIN (
            SELECT exercise_id, MAX(date) AS last_date
            FROM set_completions
            WHERE user_id = :userId AND exercise_id IN (:exerciseIds) AND date < :today
            GROUP BY exercise_id
        ) latest
          ON latest.exercise_id = sc.exercise_id
         AND latest.last_date = sc.date
        WHERE sc.user_id = :userId AND sc.exercise_id IN (:exerciseIds)
        ORDER BY sc.exercise_id ASC, sc.set_index ASC
    """)
    suspend fun getLastSessionSets(userId: String, exerciseIds: List<String>, today: String): List<SetCompletionEntity>

    /** Egzersiz bazlı performans geçmişi (progresyon ekranı için) */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND date >= :since
          AND (weight_kg IS NOT NULL OR duration_seconds IS NOT NULL OR distance_meters IS NOT NULL)
        ORDER BY date ASC, set_index ASC
    """)
    suspend fun getHistoryForExercise(userId: String, exerciseId: String, since: String): List<SetCompletionEntity>

    /**
     * Performans kaydı yapılmış egzersizlerin özet listesi (profil/progresyon ekranı için).
     * Her egzersiz için: ağırlık, süre, mesafe, set/tekrar/hacim ve son antrenman tarihi.
     * last_weight = son antrenmandaki max ağırlık (subquery ile).
     */
    @Query("""
        SELECT sc.exercise_id,
               e.name,
               COALESCE(e.image_url, '')                                AS image_url,
               e.target_muscle,
               COALESCE(MAX(sc.weight_kg), 0)                           AS max_weight,
               COALESCE(AVG(sc.weight_kg), 0)                           AS avg_weight,
               COUNT(DISTINCT sc.date)                                  AS session_count,
               COUNT(*)                                                 AS total_sets,
               COALESCE(SUM(sc.reps_actual), 0)                         AS total_reps,
               COALESCE(SUM(sc.weight_kg * sc.reps_actual), 0)          AS total_volume,
               COALESCE(SUM(sc.duration_seconds), 0)                    AS total_duration_seconds,
               COALESCE(SUM(sc.distance_meters), 0)                     AS total_distance_meters,
               COALESCE(SUM(sc.elevation_meters), 0)                    AS total_elevation_meters,
               (SELECT MAX(date) FROM set_completions
                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                    AND (weight_kg IS NOT NULL OR duration_seconds IS NOT NULL OR distance_meters IS NOT NULL)) AS last_date,
               COALESCE((SELECT MAX(weight_kg) FROM set_completions
                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                  AND date = (SELECT MAX(date) FROM set_completions
                                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                                    AND weight_kg IS NOT NULL)), 0)     AS last_weight
        FROM set_completions sc
        INNER JOIN exercises e ON e.id = sc.exercise_id
        WHERE sc.user_id = :userId
          AND (sc.weight_kg IS NOT NULL OR sc.duration_seconds IS NOT NULL OR sc.distance_meters IS NOT NULL)
        GROUP BY sc.exercise_id
        ORDER BY last_date DESC, max_weight DESC
    """)
    suspend fun getTrackedExerciseSummaries(userId: String): List<ExerciseProgressSummary>
}
