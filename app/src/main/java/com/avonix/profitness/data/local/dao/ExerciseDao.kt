package com.avonix.profitness.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avonix.profitness.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY category, name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises
        WHERE created_by IS NULL OR created_by = :userId
        ORDER BY category, name
    """)
    fun observeVisible(userId: String?): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY category, name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("""
        SELECT * FROM exercises
        WHERE created_by IS NULL OR created_by = :userId
        ORDER BY category, name
    """)
    suspend fun getVisible(userId: String?): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}
