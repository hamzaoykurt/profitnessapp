package com.avonix.profitness.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import kotlinx.coroutines.flow.Flow

/** Egzersiz başına ağırlık takip özeti — profil/progresyon ekranı için */
data class ExerciseProgressSummary(
    @ColumnInfo(name = "exercise_id")  val exerciseId   : String,
    @ColumnInfo(name = "name")         val name         : String,
    @ColumnInfo(name = "image_url")    val imageUrl     : String,
    @ColumnInfo(name = "target_muscle")val targetMuscle : String,
    @ColumnInfo(name = "max_weight")   val maxWeight    : Float,
    @ColumnInfo(name = "session_count")val sessionCount : Int
)

@Dao
interface SetCompletionDao {

    /** Belirli bir haftanın tüm set completions'larını reaktif izle */
    @Query("""
        SELECT * FROM set_completions
        WHERE user_id = :userId AND date >= :weekStart
    """)
    fun observeForWeek(userId: String, weekStart: String): Flow<List<SetCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SetCompletionEntity)

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

    /** Ağırlık takibi yapılmış egzersizlerin özet listesi (profil/progresyon ekranı için) */
    @Query("""
        SELECT sc.exercise_id, e.name, COALESCE(e.image_url, '') AS image_url,
               e.target_muscle,
               MAX(sc.weight_kg) AS max_weight,
               COUNT(DISTINCT sc.date) AS session_count
        FROM set_completions sc
        INNER JOIN exercises e ON e.id = sc.exercise_id
        WHERE sc.user_id = :userId AND sc.weight_kg IS NOT NULL
        GROUP BY sc.exercise_id
        ORDER BY session_count DESC, max_weight DESC
    """)
    suspend fun getTrackedExerciseSummaries(userId: String): List<ExerciseProgressSummary>
}
