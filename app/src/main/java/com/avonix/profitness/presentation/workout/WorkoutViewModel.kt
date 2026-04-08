package com.avonix.profitness.presentation.workout

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.presentation.profile.computeRank
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class WorkoutScreenState(
    val dayStates: List<WorkoutDayState> = emptyList(),
    val selectedDayIdx: Int = 0,
    val isLoading: Boolean = true,
    val hasProgramLoaded: Boolean = false,
    val currentProgramId: String = "",
    val currentStreak: Int = 0,
    // workoutLogId'ler gün bazında tutulur — Map<dayIdx, logId>
    val workoutLogIds: Map<Int, String> = emptyMap()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val programRepository : ProgramRepository,
    private val workoutRepository : WorkoutRepository,
    private val profileRepository : ProfileRepository,
    private val supabase          : SupabaseClient
) : BaseViewModel<WorkoutScreenState, Nothing>(WorkoutScreenState()) {

    private var lastLoadMs = 0L

    init {
        loadActiveProgram()
    }

    fun reload() {
        val now = System.currentTimeMillis()
        // Yükleme zaten devam ediyorsa ya da data tazeyse atla — startup çift yüklemesini ve tab patlamasını önler
        if (uiState.value.isLoading) return
        if (uiState.value.hasProgramLoaded && now - lastLoadMs < 30_000) return
        updateState { it.copy(isLoading = !uiState.value.hasProgramLoaded) }
        loadActiveProgram()
    }

    /** Program değişikliğinden sonra disk cache'i de temizleyip zorla yeniler. */
    fun forceReload() {
        lastLoadMs = 0L
        programRepository.invalidateActiveCache()
        updateState { it.copy(isLoading = true) }
        loadActiveProgram()
    }

    private fun loadActiveProgram() {
        viewModelScope.launch {
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (userId == null) {
                updateState {
                    it.copy(
                        isLoading = false,
                        hasProgramLoaded = false,
                        dayStates = DEMO_WORKOUTS.map { d -> WorkoutDayState(d) }
                    )
                }
                return@launch
            }

            programRepository.getActiveProgram(userId)
                .onSuccess { program ->
                    if (program == null || program.days.isEmpty()) {
                        updateState {
                            it.copy(
                                isLoading = false,
                                hasProgramLoaded = false,
                                dayStates = emptyList()
                            )
                        }
                        return@onSuccess
                    }

                    val sortedDays = program.days.sortedBy { it.dayIndex }
                    val dayByIndex = sortedDays.associateBy { it.dayIndex }

                    // Her zaman 7 gün göster — program günü yoksa DİNLENME doldur
                    val dayStates = (0..6).map { weekdayIdx ->
                        val day = dayByIndex[weekdayIdx]
                        if (day != null) {
                            val workoutDay = WorkoutDay(
                                day = DAY_LABELS[weekdayIdx],
                                title = day.title,
                                isRestDay = day.isRestDay,
                                programDayId = day.id,
                                exercises = day.exercises.map { pe ->
                                    Exercise(
                                        id = pe.id,
                                        name = pe.exerciseName,
                                        target = pe.targetMuscle,
                                        sets = pe.sets,
                                        reps = pe.reps.toString(),
                                        image = pe.imageUrl.ifBlank { categoryImageFallback(pe.category) },
                                        category = pe.category.ifBlank { "Strength" },
                                        restSeconds = pe.restSeconds,
                                        exerciseTableId = pe.exerciseId
                                    )
                                }
                            )
                            WorkoutDayState(workoutDay)
                        } else {
                            WorkoutDayState(
                                WorkoutDay(
                                    day = DAY_LABELS[weekdayIdx],
                                    title = "DİNLENME",
                                    isRestDay = true,
                                    exercises = emptyList()
                                )
                            )
                        }
                    }

                    val todayIdx = LocalDate.now().dayOfWeek.value - 1

                    // Bu haftanın Pazartesisini hesapla — pazartesi 00:00'da reset
                    val weekStart = LocalDate.now()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .toString()

                    // DB'den haftalık tamamlamalar ve seri paralel çek
                    val (weeklyCompletions, dbStreak) = coroutineScope {
                        val completionsDeferred = async {
                            workoutRepository.getWeeklyCompletions(userId, weekStart).getOrDefault(emptyMap())
                        }
                        val streakDeferred = async {
                            workoutRepository.getStreak(userId).getOrDefault(0)
                        }
                        completionsDeferred.await() to streakDeferred.await()
                    }

                    // completedIds'i DB'den gelen verilerle doldur
                    // DB'de exercises.id saklanır ama UI'da program_exercises.id kullanılır
                    // exercises.id → program_exercises.id mapping yapılmalı
                    val previousState = uiState.value
                    val sameProgram = previousState.currentProgramId == program.id
                    val finalDayStates = dayStates.mapIndexed { idx, newDs ->
                        val programDay = sortedDays.firstOrNull { it.dayIndex == idx }
                        val dbExerciseIds = programDay?.let { weeklyCompletions[it.id] } ?: emptySet()

                        // exercises.id (DB) → program_exercises.id (UI) çevirisi
                        val exerciseIdToPeId = programDay?.exercises
                            ?.associate { it.exerciseId to it.id }
                            ?: emptyMap()
                        val dbCompleted = dbExerciseIds.mapNotNull { exerciseIdToPeId[it] }.toSet()

                        val memCompleted = if (sameProgram) previousState.dayStates.getOrNull(idx)?.completedIds ?: emptySet() else emptySet()
                        newDs.copy(completedIds = dbCompleted + memCompleted)
                    }

                    lastLoadMs = System.currentTimeMillis()
                    updateState {
                        it.copy(
                            isLoading = false,
                            hasProgramLoaded = true,
                            dayStates = finalDayStates,
                            selectedDayIdx = todayIdx,
                            currentProgramId = program.id,
                            currentStreak = dbStreak,
                            workoutLogIds = emptyMap()
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isLoading = false,
                            hasProgramLoaded = false,
                            dayStates = DEMO_WORKOUTS.map { d -> WorkoutDayState(d) }
                        )
                    }
                }
        }
    }

    fun selectDay(idx: Int) {
        updateState { it.copy(selectedDayIdx = idx) }
    }

    fun toggleExercise(dayIdx: Int, exerciseId: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            val dayStates = currentState.dayStates.toMutableList()
            val current = dayStates[dayIdx]
            val newIds = current.completedIds.toMutableSet()
            val isCompleting = exerciseId !in newIds
            if (isCompleting) newIds.add(exerciseId) else newIds.remove(exerciseId)
            dayStates[dayIdx] = current.copy(completedIds = newIds)
            updateState { it.copy(dayStates = dayStates.toList()) }

            // Log to Supabase when completing (not un-completing)
            if (isCompleting) {
                val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return@launch
                val exercise = current.day.exercises.find { it.id == exerciseId } ?: return@launch
                val programDayId = current.day.programDayId
                if (programDayId.isBlank()) return@launch

                // Lazy workout log oluşturma — seçili gün için log yoksa oluştur
                val logId = currentState.workoutLogIds[dayIdx]
                    ?: workoutRepository.startWorkout(userId, programDayId).getOrNull()
                    ?: return@launch

                // Yeni log ID'yi cache'le
                if (dayIdx !in currentState.workoutLogIds) {
                    updateState { it.copy(workoutLogIds = it.workoutLogIds + (dayIdx to logId)) }
                }

                workoutRepository.logExercise(
                    workoutLogId = logId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId },
                    setsCompleted = exercise.sets,
                    repsCompleted = exercise.reps.toIntOrNull() ?: 0
                )
                workoutRepository.updateStreak(userId)
                profileRepository.invalidateStatsCache()

                val updatedStreak = workoutRepository.getStreak(userId).getOrDefault(0)
                updateState { it.copy(currentStreak = updatedStreak) }

                // Başarım/XP kontrolü ayrı coroutine'de — ana tamamlama akışını bloklamaz
                viewModelScope.launch {
                    val updatedDay = uiState.value.dayStates.getOrNull(dayIdx)
                    if (updatedDay != null && !updatedDay.day.isRestDay) {
                        val allDone = updatedDay.day.exercises.isNotEmpty() &&
                            updatedDay.day.exercises.all { it.id in updatedDay.completedIds }
                        if (allDone) {
                            workoutRepository.addXp(userId, 50)
                        }
                    }
                    checkAndUnlockAchievements(userId)
                }
            }
        }
    }

    /** Egzersiz tamamlanınca başarımları ve rank'ı güncelle */
    private suspend fun checkAndUnlockAchievements(userId: String) {
        val stats       = profileRepository.getUserStats(userId).getOrNull() ?: return
        val allAch      = profileRepository.getAllAchievements().getOrNull() ?: return
        val unlockedKeys= profileRepository.getUnlockedAchievementKeys(userId).getOrNull() ?: return

        val toCheck = mapOf(
            "xp"              to stats.xp,
            "level"           to stats.level,
            "volume"          to stats.total_workouts,
            "streak"          to stats.current_streak,
            "milestone"       to stats.total_workouts,
            "total_exercises" to stats.total_exercises
        )

        allAch
            .filter { it.key !in unlockedKeys }
            .forEach { ach ->
                val value = toCheck[ach.category] ?: return@forEach
                if (value >= ach.threshold) {
                    profileRepository.unlockAchievement(userId, ach.key)
                }
            }

        // Rank güncelleme
        val newRank = computeRank(stats.xp)
        val profile = profileRepository.getProfile(userId).getOrNull()
        if (profile != null && profile.current_rank != newRank) {
            profileRepository.updateRank(userId, newRank)
        }
    }

    companion object {
        private val DAY_LABELS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

        private fun categoryImageFallback(category: String): String = when {
            category.contains("Göğüs", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800"
            category.contains("Sırt", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1532384661128-d446b2d55db7?w=800"
            category.contains("Omuz", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1584735935169-11c341bfed13?w=800"
            category.contains("Bacak", ignoreCase = true) ||
            category.contains("Quad", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1567013127542-490d757e51fc?w=800"
            category.contains("Core", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=800"
            category.contains("Kardiyo", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800"
            else ->
                "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"
        }
    }
}
