package com.avonix.profitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity
import com.avonix.profitness.data.local.relation.ProgramExerciseWithName
import com.avonix.profitness.data.local.relation.ProgramWithDays
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    // ── Observe ──────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM programs WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    fun observeActiveProgram(userId: String): Flow<ProgramWithDays?>

    @Transaction
    @Query("SELECT * FROM programs WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeUserPrograms(userId: String): Flow<List<ProgramWithDays>>

    // ── Raw JOIN: program_exercises + exercises ──────────────────────────────

    @Query("""
        SELECT pe.id, pe.program_day_id, pe.exercise_id, pe.sets, pe.reps,
               pe.weight_kg, pe.rest_seconds, pe.order_index,
               e.name AS exercise_name, e.target_muscle, e.category, e.image_url
        FROM program_exercises pe
        INNER JOIN exercises e ON pe.exercise_id = e.id
        WHERE pe.program_day_id = :dayId
        ORDER BY pe.order_index
    """)
    fun observeExercisesForDay(dayId: String): Flow<List<ProgramExerciseWithName>>

    @Query("""
        SELECT pe.id, pe.program_day_id, pe.exercise_id, pe.sets, pe.reps,
               pe.weight_kg, pe.rest_seconds, pe.order_index,
               e.name AS exercise_name, e.target_muscle, e.category, e.image_url
        FROM program_exercises pe
        INNER JOIN exercises e ON pe.exercise_id = e.id
        WHERE pe.program_day_id IN (:dayIds)
        ORDER BY pe.order_index
    """)
    fun getExercisesForDays(dayIds: List<String>): List<ProgramExerciseWithName>

    @Query("""
        SELECT pe.id, pe.program_day_id, pe.exercise_id, pe.sets, pe.reps,
               pe.weight_kg, pe.rest_seconds, pe.order_index,
               e.name AS exercise_name, e.target_muscle, e.category, e.image_url
        FROM program_exercises pe
        INNER JOIN exercises e ON pe.exercise_id = e.id
        WHERE pe.program_day_id IN (:dayIds)
        ORDER BY pe.order_index
    """)
    fun observeExercisesForDays(dayIds: List<String>): Flow<List<ProgramExerciseWithName>>

    // ── One-shot reads ───────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM programs WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    suspend fun getActiveProgram(userId: String): ProgramWithDays?

    @Transaction
    @Query("SELECT * FROM programs WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getUserPrograms(userId: String): List<ProgramWithDays>

    @Query("SELECT * FROM program_days WHERE program_id = :programId ORDER BY day_index")
    suspend fun getDaysForProgram(programId: String): List<ProgramDayEntity>

    // ── Write ────────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertProgram(program: ProgramEntity)

    @Upsert
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Upsert
    suspend fun upsertDays(days: List<ProgramDayEntity>)

    @Upsert
    suspend fun upsertExercises(exercises: List<ProgramExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<ProgramDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ProgramExerciseEntity>)

    @Query("UPDATE programs SET is_active = 0 WHERE user_id = :userId AND is_active = 1")
    suspend fun deactivateAll(userId: String)

    @Query("UPDATE programs SET is_active = 1 WHERE id = :programId")
    suspend fun activate(programId: String)

    @Query("UPDATE programs SET name = :name WHERE id = :programId")
    suspend fun updateName(programId: String, name: String)

    // ── Delete ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM programs WHERE id = :programId")
    suspend fun deleteProgram(programId: String)

    @Query("DELETE FROM program_days WHERE program_id = :programId")
    suspend fun deleteDaysForProgram(programId: String)

    @Query("DELETE FROM program_exercises WHERE program_day_id IN (:dayIds)")
    suspend fun deleteExercisesForDays(dayIds: List<String>)

    @Query("DELETE FROM programs WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    // ── Bulk sync helper ─────────────────────────────────────────────────────

    @Transaction
    suspend fun replaceAllForUser(
        userId: String,
        programs: List<ProgramEntity>,
        days: List<ProgramDayEntity>,
        exercises: List<ProgramExerciseEntity>
    ) {
        deleteAllForUser(userId)
        if (programs.isNotEmpty()) upsertPrograms(programs)
        if (days.isNotEmpty()) upsertDays(days)
        if (exercises.isNotEmpty()) upsertExercises(exercises)
    }
}
