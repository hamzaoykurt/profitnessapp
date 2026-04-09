package com.avonix.profitness.presentation.workout

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.presentation.profile.computeRank
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val setCompletions: Map<String, Set<Int>> = emptyMap()  // exerciseId → tamamlanan set indexleri
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<WorkoutScreenState, Nothing>(WorkoutScreenState()) {

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REACTIVE OBSERVATION — Room Flow'ları combine eder
    // ═════════════════════════════════════════════════════════════════════════

    private fun startObserving() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            updateState {
                it.copy(
                    isLoading = false,
                    hasProgramLoaded = false,
                    dayStates = DEMO_WORKOUTS.map { d -> WorkoutDayState(d) }
                )
            }
            return
        }

        // İlk seferde arka planda sync başlat
        viewModelScope.launch {
            runCatching {
                programRepository.syncFromRemote(userId)
                workoutRepository.syncFromRemote(userId)
            }
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toString()

            combine(
                programRepository.observeActiveProgram(userId),
                workoutRepository.observeWeeklyCompletions(userId, weekStart),
                workoutRepository.observeStreak(userId),
                workoutRepository.observeSetCompletions(userId, weekStart)
            ) { program, completions, streak, setCompletions ->
                Triple(Triple(program, completions, streak), setCompletions, Unit)
            }
            .distinctUntilChanged()
            .collect { (innerTriple, setCompletions, _) ->
                val (program, completions, streak) = innerTriple
                if (program == null || program.days.isEmpty()) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            hasProgramLoaded = false,
                            dayStates = emptyList(),
                            currentStreak = streak,
                            setCompletions = setCompletions
                        )
                    }
                    return@collect
                }

                val dayStates = buildDayStates(program, completions)
                val todayIdx = LocalDate.now().dayOfWeek.value - 1

                updateState { prev ->
                    prev.copy(
                        isLoading = false,
                        hasProgramLoaded = true,
                        dayStates = dayStates,
                        // İlk yükleme veya program değişikliğinde bugünü seç
                        selectedDayIdx = if (prev.currentProgramId != program.id) todayIdx else prev.selectedDayIdx,
                        currentProgramId = program.id,
                        currentStreak = streak,
                        setCompletions = setCompletions
                    )
                }
            }
        }
    }

    /**
     * Ekran resume veya tab geçişinde çağrılır.
     * Arka planda Supabase'den güncel veri çeker.
     * Room Flow zaten dinleniyor — yeni veri gelince UI otomatik güncellenir.
     */
    fun refresh() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        // Observe zaten aktifse tekrar başlatma — sadece sync tetikle
        if (observeJob?.isActive != true) startObserving()
        viewModelScope.launch {
            runCatching {
                workoutRepository.syncFromRemote(userId)
                programRepository.syncFromRemote(userId)
            }
        }
    }

    /** Program değişikliğinden sonra zorla sync. */
    fun forceRefresh() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        if (observeJob?.isActive != true) startObserving()
        viewModelScope.launch {
            runCatching {
                programRepository.syncFromRemote(userId)
                workoutRepository.syncFromRemote(userId)
            }
        }
    }

    fun selectDay(idx: Int) {
        updateState { it.copy(selectedDayIdx = idx) }
    }

    fun toggleSet(exerciseId: String, setIndex: Int) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val currentState = uiState.value
        val dayState = currentState.dayStates.getOrNull(currentState.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return

        val isCurrentlyDone = setIndex in (currentState.setCompletions[exerciseId] ?: emptySet())

        viewModelScope.launch {
            if (isCurrentlyDone) {
                workoutRepository.removeSetCompletion(userId, exerciseId, programDayId, setIndex)
            } else {
                workoutRepository.addSetCompletion(userId, exerciseId, programDayId, setIndex)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TOGGLE EXERCISE — Room-first, anında UI yansıması
    // ═════════════════════════════════════════════════════════════════════════

    fun toggleExercise(dayIdx: Int, exerciseId: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            val dayState = currentState.dayStates.getOrNull(dayIdx) ?: return@launch
            val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return@launch
            val programDayId = dayState.day.programDayId
            if (programDayId.isBlank()) return@launch

            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return@launch
            val isCompleting = exerciseId !in dayState.completedIds

            if (isCompleting) {
                // Tüm setleri Room'a yaz — Flow otomatik UI'ı günceller
                workoutRepository.fillExerciseSetCompletions(
                    userId = userId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId },
                    programDayId = programDayId,
                    totalSets = exercise.sets
                )
                // exerciseTableId = exercises tablosundaki gerçek ID (DB logları için)
                workoutRepository.completeExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId },
                    setsCompleted = exercise.sets,
                    repsCompleted = exercise.reps.toIntOrNull() ?: 0
                )

                // Stats güncelle (Supabase, arka plan)
                viewModelScope.launch {
                    workoutRepository.updateStreak(userId)
                    profileRepository.invalidateStatsCache()
                }

                // Başarım/XP kontrolü
                viewModelScope.launch {
                    checkDayCompletion(dayIdx, userId)
                    checkAndUnlockAchievements(userId)
                }
            } else {
                // Tüm setleri Room'dan sil — Flow otomatik UI'ı günceller
                workoutRepository.clearExerciseSetCompletions(
                    userId = userId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId },
                    programDayId = programDayId
                )
                workoutRepository.uncompleteExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId }
                )
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private fun buildDayStates(
        program: Program,
        completions: Map<String, Set<String>>
    ): List<WorkoutDayState> {
        val sortedDays = program.days.sortedBy { it.dayIndex }
        val dayByIndex = sortedDays.associateBy { it.dayIndex }

        return (0..6).map { weekdayIdx ->
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

                // DB'deki completions exercises.id kullanır.
                // UI'daki exercise.id = program_exercises.id.
                // Mapping: exercises.id → program_exercises.id
                val dbExerciseIds = completions[day.id] ?: emptySet()
                val exerciseIdToPeId = day.exercises.associate { it.exerciseId to it.id }
                val completedPeIds = dbExerciseIds.mapNotNull { exerciseIdToPeId[it] }.toSet()

                WorkoutDayState(workoutDay, completedIds = completedPeIds)
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
    }

    private suspend fun checkDayCompletion(dayIdx: Int, userId: String) {
        val dayState = uiState.value.dayStates.getOrNull(dayIdx) ?: return
        if (dayState.day.isRestDay) return
        val allDone = dayState.day.exercises.isNotEmpty() &&
            dayState.day.exercises.all { it.id in dayState.completedIds }
        if (allDone) {
            workoutRepository.addXp(userId, 50)
        }
    }

    private suspend fun checkAndUnlockAchievements(userId: String) {
        val stats = profileRepository.getUserStats(userId).getOrNull() ?: return
        val allAch = profileRepository.getAllAchievements().getOrNull() ?: return
        val unlockedKeys = profileRepository.getUnlockedAchievementKeys(userId).getOrNull() ?: return

        val toCheck = mapOf(
            "xp" to stats.xp,
            "level" to stats.level,
            "volume" to stats.total_workouts,
            "streak" to stats.current_streak,
            "milestone" to stats.total_workouts,
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
