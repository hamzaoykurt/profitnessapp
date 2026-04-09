package com.avonix.profitness.data.workout

import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {

    // ── Reactive observe (Room Flow) ─────────────────────────────────────────

    /**
     * Bu haftanın tamamlanan egzersizlerini reaktif izler.
     * Map<programDayId, Set<exerciseId>> — her program gününün tamamlanmış egzersizleri.
     * Room'daki exercise_logs değişince otomatik emit eder.
     */
    fun observeWeeklyCompletions(userId: String, weekStart: String): Flow<Map<String, Set<String>>>

    /**
     * Ardışık antrenman serisini (streak) reaktif izler.
     * workout_logs tarihlerinden hesaplanır.
     */
    fun observeStreak(userId: String): Flow<Int>

    // ── Write (Room-first, Supabase async sync) ──────────────────────────────

    /**
     * Egzersiz tamamlanmasını Room'a anında yazar.
     * Arka planda Supabase'e sync eder.
     * @return workout_log ID
     */
    suspend fun completeExercise(
        userId: String,
        programDayId: String,
        exerciseId: String,
        setsCompleted: Int,
        repsCompleted: Int,
        durationSeconds: Int = 0
    ): Result<String>

    /**
     * Tamamlanmış egzersizi geri alır (Room'dan siler).
     */
    suspend fun uncompleteExercise(
        userId: String,
        programDayId: String,
        exerciseId: String
    ): Result<Unit>

    /** workout_log'u bitirildi olarak işaretler. */
    suspend fun finishWorkout(workoutLogId: String): Result<Unit>

    // ── Stats ────────────────────────────────────────────────────────────────

    /** user_stats'ı günceller (XP, total_exercises). */
    suspend fun updateStreak(userId: String): Result<Unit>

    /** Bonus XP ekler. */
    suspend fun addXp(userId: String, xpAmount: Int): Result<Unit>

    /** Mevcut streak'i one-shot okur. */
    suspend fun getStreak(userId: String): Result<Int>

    // ── Sync ─────────────────────────────────────────────────────────────────

    /** Supabase'den workout verilerini çekip Room'a yazar. */
    suspend fun syncFromRemote(userId: String)

    /** Room'daki unsynced kayıtları Supabase'e yazar. */
    suspend fun syncToRemote()
}
