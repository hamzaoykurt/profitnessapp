package com.avonix.profitness.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import kotlinx.coroutines.flow.Flow

/** Egzersiz başına ağırlık takip özeti — profil/progresyon ekranı için */
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
    @ColumnInfo(name = "total_volume")  val totalVolume   : Float
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
    ) {
        val rows = updateWeight(userId, exerciseId, programDayId, setIndex, date, weightKg)
        if (rows == 0) {
            insert(SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, weightKg, null))
        }
    }

    /** Upsert reps_actual — kayıt varsa sadece reps_actual'ı günceller, yoksa yeni kayıt oluşturur. */
    @Transaction
    suspend fun upsertRepsActual(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, date: String, repsActual: Int?
    ) {
        val rows = updateRepsActual(userId, exerciseId, programDayId, setIndex, date, repsActual)
        if (rows == 0) {
            insert(SetCompletionEntity(userId, exerciseId, programDayId, setIndex, date, null, repsActual))
        }
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

    /** Egzersiz bazlı ağırlık geçmişi (progresyon grafiği için) */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND exercise_id = :exerciseId
          AND date >= :since AND weight_kg IS NOT NULL
        ORDER BY date ASC, set_index ASC
    """)
    suspend fun getHistoryForExercise(userId: String, exerciseId: String, since: String): List<SetCompletionEntity>

    /**
     * Ağırlık takibi yapılmış egzersizlerin özet listesi (profil/progresyon ekranı için).
     * Her egzersiz için: max/avg/last weight, total sets/reps/volume ve son antrenman tarihi.
     * last_weight = son antrenmandaki max ağırlık (subquery ile).
     */
    @Query("""
        SELECT sc.exercise_id,
               e.name,
               COALESCE(e.image_url, '')                                AS image_url,
               e.target_muscle,
               MAX(sc.weight_kg)                                        AS max_weight,
               AVG(sc.weight_kg)                                        AS avg_weight,
               COUNT(DISTINCT sc.date)                                  AS session_count,
               COUNT(*)                                                 AS total_sets,
               COALESCE(SUM(sc.reps_actual), 0)                         AS total_reps,
               COALESCE(SUM(sc.weight_kg * sc.reps_actual), 0)          AS total_volume,
               (SELECT MAX(date) FROM set_completions
                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                    AND weight_kg IS NOT NULL)                          AS last_date,
               (SELECT MAX(weight_kg) FROM set_completions
                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                    AND date = (SELECT MAX(date) FROM set_completions
                                  WHERE user_id = :userId AND exercise_id = sc.exercise_id
                                    AND weight_kg IS NOT NULL))         AS last_weight
        FROM set_completions sc
        INNER JOIN exercises e ON e.id = sc.exercise_id
        WHERE sc.user_id = :userId AND sc.weight_kg IS NOT NULL
        GROUP BY sc.exercise_id
        ORDER BY last_date DESC, max_weight DESC
    """)
    suspend fun getTrackedExerciseSummaries(userId: String): List<ExerciseProgressSummary>
}
