package com.avonix.profitness.data.workout

interface WorkoutRepository {
    /** Creates a workout_log for today's session. Returns the log ID. */
    suspend fun startWorkout(userId: String, programDayId: String): Result<String>

    /** Appends a completed exercise to exercise_logs. */
    suspend fun logExercise(
        workoutLogId: String,
        exerciseId: String,
        setsCompleted: Int,
        repsCompleted: Int,
        durationSeconds: Int = 0
    ): Result<Unit>

    /** Marks the workout_log as finished. */
    suspend fun finishWorkout(workoutLogId: String): Result<Unit>

    /**
     * Updates user_stats streak for today.
     * - If user has no stats yet: inserts with streak = 1.
     * - If last update was yesterday: increments streak.
     * - If last update was today: only increments total_exercises.
     * - Otherwise (gap > 1 day): resets streak to 1.
     */
    suspend fun updateStreak(userId: String): Result<Unit>

    /** Reads current_streak from user_stats. Returns 0 if no record exists. */
    suspend fun getStreak(userId: String): Result<Int>

    /**
     * Bu haftanın Pazartesi'sinden itibaren tamamlanan egzersizleri döner.
     * Map<programDayId, Set<exerciseId>> — günün tamamlanmış egzersiz ID'leri.
     * Pazartesi 00:00'da yeni haftaya geçer.
     */
    suspend fun getWeeklyCompletions(userId: String, weekStart: String): Result<Map<String, Set<String>>>
}
