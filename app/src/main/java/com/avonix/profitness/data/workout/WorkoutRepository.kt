package com.avonix.profitness.data.workout

import com.avonix.profitness.data.local.dao.ExerciseProgressSummary
import com.avonix.profitness.data.local.entity.SetCompletionEntity
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

    /**
     * Set tamamlamalarını reaktif izler.
     * Map<exerciseId, Set<setIndex>> — her egzersizin tamamlanan set indexleri.
     */
    fun observeSetCompletions(userId: String, weekStart: String): Flow<Map<String, Set<Int>>>

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

    // ── Set Completion ────────────────────────────────────────────────────────

    /** Tek bir set'i tamamlandı olarak Room'a ekler (ağırlık ve gerçek tekrar opsiyonel). */
    suspend fun addSetCompletion(
        userId: String, exerciseId: String, programDayId: String, setIndex: Int,
        weightKg: Float? = null, repsActual: Int? = null
    ): Result<Unit>

    /** Tek bir set'in tamamlanmasını Room'dan kaldırır. */
    suspend fun removeSetCompletion(userId: String, exerciseId: String, programDayId: String, setIndex: Int): Result<Unit>

    /** Bir egzersizin bugünkü tüm set tamamlamalarını Room'dan siler. */
    suspend fun clearExerciseSetCompletions(userId: String, exerciseId: String, programDayId: String): Result<Unit>

    /** Bir egzersizin tüm setlerini (0..totalSets-1) tamamlandı olarak Room'a yazar. */
    suspend fun fillExerciseSetCompletions(
        userId: String, exerciseId: String, programDayId: String,
        totalSets: Int, defaultRepsActual: Int = 1
    ): Result<Unit>

    /**
     * Draft ağırlık kaydı — kullanıcı ağırlık girer ama seti tamamlamaz.
     * reps_actual = null olarak tutulur, observer "tick" olarak göstermez,
     * ancak progresyon ekranında ve bir sonraki oturumun ön-dolgusunda görünür.
     */
    suspend fun upsertSetWeightDraft(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, weightKg: Float?
    ): Result<Unit>

    /** Tek bir setin reps_actual değerini günceller (weight_kg'ye dokunmaz). */
    suspend fun upsertSetRepsActual(
        userId: String, exerciseId: String, programDayId: String,
        setIndex: Int, repsActual: Int?
    ): Result<Unit>

    // ── Progressive Overload ─────────────────────────────────────────────────

    /** Önceki antrenmanın set verilerini getirir (ağırlık ön-doldurma için). */
    suspend fun getLastSessionSets(userId: String, exerciseId: String): Result<List<SetCompletionEntity>>

    /** Egzersiz bazlı ağırlık geçmişi (progresyon grafiği için). */
    suspend fun getExerciseWeightHistory(userId: String, exerciseId: String, weeks: Int = 8): Result<List<SetCompletionEntity>>

    /** Ağırlık takibi yapılmış egzersizlerin özet listesi. */
    suspend fun getTrackedExerciseSummaries(userId: String): Result<List<ExerciseProgressSummary>>
}
