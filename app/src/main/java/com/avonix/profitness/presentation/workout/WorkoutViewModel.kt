package com.avonix.profitness.presentation.workout

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.presentation.profile.computeRank
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class RestTimerState(
    val isRunning: Boolean = false,
    val isDone: Boolean = false,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = 0,
    val exerciseName: String = ""
) {
    val progress get() = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
}

data class WorkoutScreenState(
    val dayStates: List<WorkoutDayState> = emptyList(),
    val selectedDayIdx: Int = 0,
    val isLoading: Boolean = true,
    val hasProgramLoaded: Boolean = false,
    val currentProgramId: String = "",
    val currentStreak: Int = 0,
    val setCompletions: Map<String, Set<Int>> = emptyMap(),
    val restTimer: RestTimerState = RestTimerState(),
    // Progressive overload — per-set weight & reps input
    val setWeights: Map<String, Map<Int, String>> = emptyMap(),
    val setReps: Map<String, Map<Int, String>> = emptyMap(),
    val lastSessionData: Map<String, Map<Int, Pair<Float?, Int?>>> = emptyMap(),
    // ExerciseDetailSheet — progresyon
    val exerciseHistory: Map<String, List<SetCompletionEntity>> = emptyMap(),
    val exerciseAiInsight: Map<String, String> = emptyMap(),
    val exerciseAiLoading: Set<String> = emptySet()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
    private val geminiRepository: GeminiRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<WorkoutScreenState, Nothing>(WorkoutScreenState()) {

    private var observeJob: Job? = null
    private var timerJob: Job? = null

    init {
        startObserving()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REST TIMER — ViewModel'de yaşar, tab/ekran değişiminden etkilenmez
    // ═════════════════════════════════════════════════════════════════════════

    fun startRestTimer(restSeconds: Int, exerciseName: String) {
        timerJob?.cancel()
        updateState { it.copy(restTimer = RestTimerState(
            isRunning = true, isDone = false,
            secondsLeft = restSeconds, totalSeconds = restSeconds,
            exerciseName = exerciseName
        ))}
        timerJob = viewModelScope.launch {
            var remaining = restSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                updateState { it.copy(restTimer = it.restTimer.copy(secondsLeft = remaining)) }
            }
            updateState { it.copy(restTimer = it.restTimer.copy(isRunning = false, isDone = true, secondsLeft = 0)) }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        updateState { it.copy(restTimer = it.restTimer.copy(isRunning = false)) }
    }

    fun dismissRestTimer() {
        timerJob?.cancel()
        updateState { it.copy(restTimer = RestTimerState()) }
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
                val weight = currentState.setWeights[exerciseId]?.get(setIndex)?.toFloatOrNull()
                val reps = currentState.setReps[exerciseId]?.get(setIndex)?.toIntOrNull()
                workoutRepository.addSetCompletion(userId, exerciseId, programDayId, setIndex, weight, reps)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PROGRESSIVE OVERLOAD — set bazlı ağırlık/tekrar girişi
    // ═════════════════════════════════════════════════════════════════════════

    fun updateSetWeight(exerciseId: String, setIndex: Int, value: String) {
        updateState { state ->
            val exerciseMap = state.setWeights[exerciseId].orEmpty().toMutableMap()
            exerciseMap[setIndex] = value
            state.copy(setWeights = state.setWeights + (exerciseId to exerciseMap))
        }
        // Set zaten işaretliyse anında Room'a yaz (uygulama kapanınca kaybolmasın)
        persistWeightIfDone(exerciseId, setIndex)
    }

    fun updateSetReps(exerciseId: String, setIndex: Int, value: String) {
        updateState { state ->
            val exerciseMap = state.setReps[exerciseId].orEmpty().toMutableMap()
            exerciseMap[setIndex] = value
            state.copy(setReps = state.setReps + (exerciseId to exerciseMap))
        }
        persistWeightIfDone(exerciseId, setIndex)
    }

    private fun persistWeightIfDone(exerciseId: String, setIndex: Int) {
        val state = uiState.value
        val isDone = setIndex in (state.setCompletions[exerciseId] ?: emptySet())
        if (!isDone) return

        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return

        viewModelScope.launch {
            val weight = state.setWeights[exerciseId]?.get(setIndex)?.toFloatOrNull()
            val reps   = state.setReps[exerciseId]?.get(setIndex)?.toIntOrNull()
            workoutRepository.addSetCompletion(userId, exerciseId, programDayId, setIndex, weight, reps)
        }
    }

    fun loadLastSession(exerciseId: String) {
        // Zaten yüklenmişse tekrar yükleme
        if (uiState.value.lastSessionData.containsKey(exerciseId)) return

        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            workoutRepository.getLastSessionSets(userId, exerciseId).onSuccess { sets ->
                if (sets.isEmpty()) return@onSuccess
                val dataMap = sets.associate { it.setIndex to (it.weightKg to it.repsActual) }
                updateState { state ->
                    state.copy(lastSessionData = state.lastSessionData + (exerciseId to dataMap))
                }
                // Henüz kullanıcı girişi yoksa önceki ağırlıkları ön-doldur
                val currentWeights = uiState.value.setWeights[exerciseId]
                if (currentWeights.isNullOrEmpty()) {
                    val prefill = sets
                        .filter { it.weightKg != null }
                        .associate { it.setIndex to it.weightKg!!.toString() }
                    if (prefill.isNotEmpty()) {
                        updateState { state ->
                            state.copy(setWeights = state.setWeights + (exerciseId to prefill))
                        }
                    }
                }
            }
        }
    }

    fun loadExerciseHistory(exerciseId: String) {
        if (uiState.value.exerciseHistory.containsKey(exerciseId)) return
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            workoutRepository.getExerciseWeightHistory(userId, exerciseId).onSuccess { history ->
                updateState { it.copy(exerciseHistory = it.exerciseHistory + (exerciseId to history)) }
            }
        }
    }

    fun analyzeProgression(exerciseId: String, exerciseName: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        if (exerciseId in uiState.value.exerciseAiLoading) return

        updateState { it.copy(exerciseAiLoading = it.exerciseAiLoading + exerciseId) }

        viewModelScope.launch {
            val history = uiState.value.exerciseHistory[exerciseId] ?: run {
                workoutRepository.getExerciseWeightHistory(userId, exerciseId)
                    .getOrNull() ?: emptyList()
            }

            if (history.isEmpty()) {
                updateState { it.copy(exerciseAiLoading = it.exerciseAiLoading - exerciseId) }
                return@launch
            }

            val summary = history
                .filter { it.weightKg != null }
                .groupBy { it.date }
                .entries
                .sortedBy { it.key }
                .joinToString("\n") { (date, sets) ->
                    val maxKg = sets.maxOf { it.weightKg!! }
                    val totalSets = sets.size
                    "$date: ${maxKg}kg x $totalSets set"
                }

            val systemPrompt = "Sen bir fitness koçusun. Kısa ve öz (3-4 cümle) Türkçe analiz yap."
            val userMessage = """
                Egzersiz: $exerciseName
                Tarih bazlı max ağırlık ve set sayısı:
                $summary

                Gelişim trendi nasıl? Bir sonraki antrenman için somut öneri ver (kg bazında).
            """.trimIndent()

            val result = geminiRepository.chat(emptyList(), userMessage, systemPrompt)
            result.onSuccess { response ->
                updateState { state ->
                    state.copy(
                        exerciseAiInsight = state.exerciseAiInsight + (exerciseId to response),
                        exerciseAiLoading = state.exerciseAiLoading - exerciseId
                    )
                }
            }.onFailure {
                updateState { state ->
                    state.copy(
                        exerciseAiInsight = state.exerciseAiInsight + (exerciseId to "Analiz yüklenemedi. Tekrar deneyin."),
                        exerciseAiLoading = state.exerciseAiLoading - exerciseId
                    )
                }
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
                // Rest timer otomatik başlat
                startRestTimer(
                    restSeconds = exercise.restSeconds.takeIf { it > 0 } ?: 90,
                    exerciseName = exercise.name
                )

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
                            exerciseTableId = pe.exerciseId,
                            weightKg = pe.weightKg
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
