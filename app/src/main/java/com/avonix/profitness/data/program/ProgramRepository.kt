package com.avonix.profitness.data.program

import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program

interface ProgramRepository {
    /** Kullanıcının tüm programları (günler dahil). */
    suspend fun getUserPrograms(userId: String): Result<List<Program>>

    /** Aktif programı döner (is_active = true). */
    suspend fun getActiveProgram(userId: String): Result<Program?>

    /** Hazır şablondan program oluşturur, aktif yapar. */
    suspend fun createFromTemplate(userId: String, templateKey: String): Result<Program>

    /** Manuel program oluşturur; days = (dayTitle, exerciseIds, sets, reps) listesi. */
    suspend fun createManual(
        userId: String,
        name: String,
        days: List<ManualDayInput>
    ): Result<Program>

    /** Belirtilen programı aktif yapar, diğerlerini pasife alır. */
    suspend fun setActive(programId: String, userId: String): Result<Unit>

    /** Tüm egzersizleri döner. */
    suspend fun getAllExercises(): Result<List<ExerciseItem>>

    /** Program adını günceller. */
    suspend fun updateProgramName(programId: String, name: String): Result<Unit>

    /** Programı siler. */
    suspend fun deleteProgram(programId: String): Result<Unit>
}

data class ManualDayInput(
    val title: String,
    val isRestDay: Boolean = false,
    val exercises: List<ManualExerciseInput> = emptyList()
)

data class ManualExerciseInput(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int = 90,
    val orderIndex: Int
)
