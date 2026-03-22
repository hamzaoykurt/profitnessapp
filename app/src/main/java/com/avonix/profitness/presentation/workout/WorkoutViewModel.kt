package com.avonix.profitness.presentation.workout

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
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
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<WorkoutScreenState, Nothing>(WorkoutScreenState()) {

    init {
        loadActiveProgram()
    }

    fun reload() {
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

                    // DB'den bu haftanın tamamlanan egzersizlerini çek
                    val weeklyCompletions = workoutRepository
                        .getWeeklyCompletions(userId, weekStart)
                        .getOrDefault(emptyMap())

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

                    // Gerçek ardışık gün serisi (user_stats.current_streak)
                    val dbStreak = workoutRepository.getStreak(userId).getOrDefault(0)

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

                // Gerçek ardışık seriyi DB'den oku (updateStreak zaten güncelledi)
                val updatedStreak = workoutRepository.getStreak(userId).getOrDefault(0)
                updateState { it.copy(currentStreak = updatedStreak) }
            }
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
