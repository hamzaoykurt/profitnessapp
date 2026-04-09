package com.avonix.profitness.data.program

import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {

    // ── Reactive observe (Room Flow — single source of truth) ────────────────

    /** Aktif programı reaktif izler. Room değişince otomatik emit eder. */
    fun observeActiveProgram(userId: String): Flow<Program?>

    /** Kullanıcının tüm programlarını reaktif izler. */
    fun observeUserPrograms(userId: String): Flow<List<Program>>

    /** Egzersiz master listesini reaktif izler. */
    fun observeExercises(): Flow<List<ExerciseItem>>

    // ── One-shot reads (Room'dan — anında döner) ─────────────────────────────

    /** Kullanıcının tüm programları (günler + egzersizler dahil). */
    suspend fun getUserPrograms(userId: String): Result<List<Program>>

    /** Aktif programı döner (is_active = true). */
    suspend fun getActiveProgram(userId: String): Result<Program?>

    /** Tüm egzersizleri döner. */
    suspend fun getAllExercises(): Result<List<ExerciseItem>>

    // ── Write (Supabase + Room sync) ─────────────────────────────────────────

    /** Hazır şablondan program oluşturur, aktif yapar. */
    suspend fun createFromTemplate(userId: String, templateKey: String): Result<Program>

    /** Manuel program oluşturur. */
    suspend fun createManual(userId: String, name: String, days: List<ManualDayInput>): Result<Program>

    /** Belirtilen programı aktif yapar, diğerlerini pasife alır. */
    suspend fun setActive(programId: String, userId: String): Result<Unit>

    /** Program adını günceller. */
    suspend fun updateProgramName(programId: String, name: String): Result<Unit>

    /** Mevcut programın adını ve günlerini/egzersizlerini günceller. */
    suspend fun updateProgram(programId: String, name: String, days: List<ManualDayInput>): Result<Program>

    /** Programı siler. */
    suspend fun deleteProgram(programId: String): Result<Unit>

    /** Yeni bir egzersiz veritabanına ekler. */
    suspend fun addExercise(
        name: String,
        nameEn: String,
        targetMuscle: String,
        category: String,
        setsDefault: Int,
        repsDefault: Int
    ): Result<ExerciseItem>

    /** Listede olmayan bir hareket için talep gönderir. */
    suspend fun requestExercise(
        userId: String,
        name: String,
        targetMuscle: String,
        notes: String
    ): Result<Unit>

    // ── Sync ─────────────────────────────────────────────────────────────────

    /** Supabase'den programları çekip Room'a yazar. */
    suspend fun syncFromRemote(userId: String)
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
