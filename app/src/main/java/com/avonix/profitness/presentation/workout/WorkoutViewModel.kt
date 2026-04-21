package com.avonix.profitness.presentation.workout

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.core.notification.WorkoutNotificationManager
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.presentation.profile.computeRank
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val exerciseAiLoading: Set<String> = emptySet(),
    val userPlan         : com.avonix.profitness.data.store.UserPlan = com.avonix.profitness.data.store.UserPlan.FREE,
    val aiCredits        : Int = com.avonix.profitness.data.store.UserPlanRepository.FREE_STARTER_CREDITS
)

sealed class WorkoutEvent {
    data object ShowPaywall : WorkoutEvent()
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val programRepository  : ProgramRepository,
    private val workoutRepository  : WorkoutRepository,
    private val profileRepository  : ProfileRepository,
    private val geminiRepository   : GeminiRepository,
    private val planRepository     : com.avonix.profitness.data.store.UserPlanRepository,
    private val notificationManager: WorkoutNotificationManager,
    private val challengeRepository: ChallengeRepository,
    private val supabase           : SupabaseClient
) : BaseViewModel<WorkoutScreenState, WorkoutEvent>(WorkoutScreenState()) {

    private var observeJob: Job? = null
    private var timerJob: Job? = null
    // Set bazlı weight/reps yazımları için debounce — her hızlı karakter girişinde Room'a yazmaktan kaçınır
    private val draftPersistJobs = mutableMapOf<String, Job>()

    init {
        startObserving()
        viewModelScope.launch {
            combine(planRepository.planFlow, planRepository.creditsFlow) { plan, credits -> plan to credits }
                .collect { (plan, credits) -> updateState { it.copy(userPlan = plan, aiCredits = credits) } }
        }
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

        // Ön plan servisi başlat — uygulama arka plandayken de bildirim gösterir
        notificationManager.startWorkoutSession(exerciseName)
        notificationManager.updateRestTimer(exerciseName, restSeconds, restSeconds)

        timerJob = viewModelScope.launch {
            var remaining = restSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                updateState { it.copy(restTimer = it.restTimer.copy(secondsLeft = remaining)) }
                // Her saniye bildirimi güncelle (IO thread'inde servis intent'i gönder)
                withContext(Dispatchers.IO) {
                    notificationManager.updateRestTimer(exerciseName, remaining, restSeconds)
                }
            }
            updateState { it.copy(restTimer = it.restTimer.copy(isRunning = false, isDone = true, secondsLeft = 0)) }
            // Ses + "Hazırsın!" bildirimi
            withContext(Dispatchers.IO) {
                notificationManager.notifyTimerDone(exerciseName)
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        updateState { it.copy(restTimer = it.restTimer.copy(isRunning = false)) }
        notificationManager.stopWorkoutSession()
    }

    fun dismissRestTimer() {
        timerJob?.cancel()
        updateState { it.copy(restTimer = RestTimerState()) }
        notificationManager.stopWorkoutSession()
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

        // DB canonical key = exercises.id (exerciseTableId). UI state keyi = pe.id (exerciseId).
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }

        val isCurrentlyDone = setIndex in (currentState.setCompletions[exerciseId] ?: emptySet())

        viewModelScope.launch {
            if (isCurrentlyDone) {
                // Tik'i kaldır: reps_actual'ı null yap (weight draft olarak korunur)
                workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, null)
            } else {
                // Tik ekle: kullanıcı reps girmemişse planlanan reps'i kullan
                val plannedReps = exercise.reps.toIntOrNull()
                val reps = currentState.setReps[exerciseId]?.get(setIndex)?.toIntOrNull()
                    ?: plannedReps?.takeIf { it > 0 } ?: 1
                // Ağırlık fallback: kullanıcı girdisi > son seans > program planı. Hiçbiri yoksa null.
                val weight = currentState.setWeights[exerciseId]?.get(setIndex)?.toFloatOrNull()
                    ?: currentState.lastSessionData[exerciseId]?.get(setIndex)?.first
                    ?: exercise.weightKg.takeIf { it > 0f }
                if (weight != null) {
                    workoutRepository.upsertSetWeightDraft(userId, dbExerciseId, programDayId, setIndex, weight)
                }
                workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, reps)
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
        // Set tik atılmış olsa da olmasa da Room'a draft olarak yaz (debounced).
        scheduleDraftPersist(exerciseId, setIndex, weight = true, reps = false)
    }

    fun updateSetReps(exerciseId: String, setIndex: Int, value: String) {
        updateState { state ->
            val exerciseMap = state.setReps[exerciseId].orEmpty().toMutableMap()
            exerciseMap[setIndex] = value
            state.copy(setReps = state.setReps + (exerciseId to exerciseMap))
        }
        scheduleDraftPersist(exerciseId, setIndex, weight = false, reps = true)
    }

    /**
     * Debounced draft persist — her tuş vuruşunda Room'a yazmak yerine 500ms bekler.
     * weight=true → sadece weight_kg güncellenir, reps_actual'a dokunulmaz (tik durumu korunur).
     * reps=true → kullanıcı tikli bir setin reps'ini değiştiriyorsa reps_actual da güncellenir.
     */
    private fun scheduleDraftPersist(exerciseId: String, setIndex: Int, weight: Boolean, reps: Boolean) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val state = uiState.value
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return
        val dbExerciseId = dayState.day.exercises.find { it.id == exerciseId }
            ?.exerciseTableId?.ifBlank { exerciseId } ?: exerciseId

        val key = "$exerciseId:$setIndex:${if (weight) "w" else "r"}"
        draftPersistJobs[key]?.cancel()
        draftPersistJobs[key] = viewModelScope.launch {
            delay(500L)
            val latest = uiState.value
            if (weight) {
                val w = latest.setWeights[exerciseId]?.get(setIndex)?.toFloatOrNull()
                workoutRepository.upsertSetWeightDraft(userId, dbExerciseId, programDayId, setIndex, w)
            }
            if (reps) {
                // Sadece set zaten tikliyse reps_actual güncellenir — tikli olmayan set draft kalmalı.
                val isDone = setIndex in (latest.setCompletions[exerciseId] ?: emptySet())
                if (isDone) {
                    val r = latest.setReps[exerciseId]?.get(setIndex)?.toIntOrNull()
                    if (r != null) {
                        workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, r)
                    }
                }
            }
        }
    }

    /**
     * Seçili günün tüm egzersizleri için önceki oturum verisini yükler.
     * Kart açılmadan da önceki haftanın ağırlık/reps değerleri setWeights'e pre-fill olur.
     */
    fun loadLastSessionForSelectedDay() {
        val state = uiState.value
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        if (dayState.day.isRestDay) return
        dayState.day.exercises.forEach { ex -> loadLastSession(ex.id) }
    }

    fun loadLastSession(exerciseId: String) {
        // Zaten yüklenmişse tekrar yükleme
        if (uiState.value.lastSessionData.containsKey(exerciseId)) return

        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        // DB'de kayıtlar canonical exercises.id (exerciseTableId) ile tutulur; state key'i pe.id.
        val state = uiState.value
        val dbExerciseId = state.dayStates.flatMap { it.day.exercises }
            .find { it.id == exerciseId }
            ?.exerciseTableId?.ifBlank { exerciseId } ?: exerciseId

        viewModelScope.launch {
            workoutRepository.getLastSessionSets(userId, dbExerciseId).onSuccess { sets ->
                if (sets.isEmpty()) return@onSuccess
                val dataMap = sets.associate { it.setIndex to (it.weightKg to it.repsActual) }
                updateState { s ->
                    s.copy(lastSessionData = s.lastSessionData + (exerciseId to dataMap))
                }
                // Henüz kullanıcı girişi yoksa önceki ağırlıkları ön-doldur
                val currentWeights = uiState.value.setWeights[exerciseId]
                if (currentWeights.isNullOrEmpty()) {
                    val prefill = sets
                        .filter { it.weightKg != null }
                        .associate { it.setIndex to it.weightKg!!.toString() }
                    if (prefill.isNotEmpty()) {
                        updateState { s ->
                            s.copy(setWeights = s.setWeights + (exerciseId to prefill))
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

        viewModelScope.launch {
            if (!planRepository.consumeCredit()) {
                sendEvent(WorkoutEvent.ShowPaywall)
                return@launch
            }

            updateState { it.copy(exerciseAiLoading = it.exerciseAiLoading + exerciseId) }
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
                planRepository.refundCredit()
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
                // Egzersiz tamamlandı — sonraki harekete geçmeden önce uzun dinlenme
                startRestTimer(
                    restSeconds = exercise.exerciseRestSeconds.takeIf { it > 0 } ?: 180,
                    exerciseName = exercise.name
                )

                // Tüm setleri Room'a yaz — Flow otomatik UI'ı günceller.
                // Planlanan reps, reps_actual default'u olarak kullanılır (observer "tick" göstermesi için şart).
                val plannedReps = exercise.reps.toIntOrNull()?.takeIf { it > 0 } ?: 1
                val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }
                workoutRepository.fillExerciseSetCompletions(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId,
                    totalSets = exercise.sets,
                    defaultRepsActual = plannedReps
                )
                // Her set için efektif ağırlığı persist et (progresyon ekranı + sonraki hafta prefill için).
                // Fallback: kullanıcı girdisi > son seans > program planı. Hiçbiri yoksa yazılmaz.
                val userWeights    = currentState.setWeights[exerciseId].orEmpty()
                val lastSessionMap = currentState.lastSessionData[exerciseId].orEmpty()
                val plannedWeight  = exercise.weightKg.takeIf { it > 0f }
                repeat(exercise.sets) { i ->
                    val w = userWeights[i]?.toFloatOrNull()
                        ?: lastSessionMap[i]?.first
                        ?: plannedWeight
                    if (w != null) {
                        workoutRepository.upsertSetWeightDraft(
                            userId = userId,
                            exerciseId = dbExerciseId,
                            programDayId = programDayId,
                            setIndex = i,
                            weightKg = w
                        )
                    }
                }
                // exerciseTableId = exercises tablosundaki gerçek ID (DB logları için)
                workoutRepository.completeExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = exercise.exerciseTableId.ifBlank { exerciseId },
                    setsCompleted = exercise.sets,
                    repsCompleted = exercise.reps.toIntOrNull() ?: 0
                )

                // Stats güncelle (Supabase, arka plan) + challenge progress refresh
                viewModelScope.launch {
                    workoutRepository.updateStreak(userId)
                    profileRepository.invalidateStatsCache()
                    // FAZ 7F: user_stats güncellendikten sonra challenge progress'ini
                    // sunucuda yeniden hesaplat. Hata olsa bile workout akışını bozma.
                    runCatching { challengeRepository.refreshMyProgress() }
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

                // Stats rollback + challenge progress tazele (istismar önleme):
                // yap→geri al döngüsü XP/total_exercises/streak/challenge ilerlemesini
                // kalıcı hale getirmesin.
                viewModelScope.launch {
                    workoutRepository.rollbackStreak(userId)
                    profileRepository.invalidateStatsCache()
                    runCatching { challengeRepository.refreshMyProgress() }
                }
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
                            exerciseRestSeconds = pe.exerciseRestSeconds,
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
