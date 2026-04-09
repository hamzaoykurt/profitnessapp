package com.avonix.profitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import kotlinx.coroutines.flow.Flow

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
}
