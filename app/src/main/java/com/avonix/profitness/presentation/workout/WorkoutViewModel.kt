package com.avonix.profitness.presentation.workout

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.core.notification.WorkoutNotificationManager
import com.avonix.profitness.data.ai.AiAccessException
import com.avonix.profitness.data.ai.AiAnalysisPrompts
import com.avonix.profitness.data.ai.AiToolType
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.sync.SyncCoordinator
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.domain.challenges.ChallengeKind
import com.avonix.profitness.domain.challenges.ChallengeMovement
import com.avonix.profitness.domain.challenges.EventMode
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.presentation.profile.computeRank
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class TimerPurpose { Rest, Activity, TimedSet }

enum class TimerMode { Countdown, Stopwatch }

data class RestTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isDone: Boolean = false,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = 0,
    val exerciseName: String = "",
    val exerciseId: String = "",
    val setIndex: Int = -1,
    val purpose: TimerPurpose = TimerPurpose.Rest,
    val mode: TimerMode = TimerMode.Countdown,
    val elapsedSeconds: Int = 0
) {
    val progress get() = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
    val displaySeconds get() = if (mode == TimerMode.Stopwatch) elapsedSeconds else secondsLeft
}

@Stable
data class DraftInput(
    val weight: String = "",
    val reps: String = "",
    val duration: String = "",
    val hasWeight: Boolean = false,
    val hasReps: Boolean = false,
    val hasDuration: Boolean = false
)

internal fun draftInputKey(exerciseId: String, setIndex: Int): String = "$exerciseId:$setIndex"

private fun DraftInput?.orEmpty(): DraftInput = this ?: DraftInput()

@Stable
data class WorkoutScreenState(
    val dayStates: ImmutableList<WorkoutDayState> = persistentListOf(),
    val selectedDayIdx: Int = 0,
    val isLoading: Boolean = true,
    val hasProgramLoaded: Boolean = false,
    val currentProgramId: String = "",
    val currentStreak: Int = 0,
    val setCompletions: Map<String, Set<Int>> = emptyMap(),
    // Progressive overload — per-set weight input. Reps are read from the program.
    val setWeights: Map<String, Map<Int, String>> = emptyMap(),
    val setReps: Map<String, Map<Int, String>> = emptyMap(),
    val setDurations: Map<String, Map<Int, String>> = emptyMap(),
    val lastSessionData: Map<String, Map<Int, Pair<Float?, Int?>>> = emptyMap(),
    val activityDurations: Map<String, String> = emptyMap(),
    val activityDistances: Map<String, String> = emptyMap(),
    val activityElevations: Map<String, String> = emptyMap(),
    val activityInclines: Map<String, String> = emptyMap(),
    val activityReps: Map<String, String> = emptyMap(),
    val challengeSyncVersion: Int = 0,
    val profileWeightKg: Float = 0f,
    // ExerciseDetailSheet — progresyon
    val exerciseHistory: Map<String, List<SetCompletionEntity>> = emptyMap(),
    val exerciseAiInsight: Map<String, String> = emptyMap(),
    val exerciseAiLoading: Set<String> = emptySet(),
    val userPlan         : com.avonix.profitness.data.store.UserPlan = com.avonix.profitness.data.store.UserPlan.FREE,
    val aiCredits        : Int = com.avonix.profitness.data.store.UserPlanRepository.INITIAL_CREDITS_PLACEHOLDER
)

sealed class WorkoutEvent {
    data object ShowPaywall : WorkoutEvent()
}

@Stable
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val programRepository  : ProgramRepository,
    private val workoutRepository  : WorkoutRepository,
    private val profileRepository  : ProfileRepository,
    private val geminiRepository   : GeminiRepository,
    private val planRepository     : com.avonix.profitness.data.store.UserPlanRepository,
    private val notificationManager: WorkoutNotificationManager,
    private val syncCoordinator    : SyncCoordinator,
    private val challengeRepository: ChallengeRepository,
    private val supabase           : SupabaseClient
) : BaseViewModel<WorkoutScreenState, WorkoutEvent>(WorkoutScreenState()) {

    private var observeJob: Job? = null
    private var sessionJob: Job? = null
    private var timerJob: Job? = null
    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()
    private val _draftInputs = MutableStateFlow<Map<String, DraftInput>>(emptyMap())
    val draftInputs: StateFlow<Map<String, DraftInput>> = _draftInputs.asStateFlow()
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

    fun startRestTimer(restSeconds: Int, exerciseName: String, exerciseId: String = "") {
        timerJob?.cancel()
        _restTimer.value = RestTimerState(
            isRunning = true, isPaused = false, isDone = false,
            secondsLeft = restSeconds, totalSeconds = restSeconds,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            purpose = TimerPurpose.Rest,
            mode = TimerMode.Countdown
        )

        // Ön plan servisi başlat — uygulama arka plandayken de bildirim gösterir
        notificationManager.startWorkoutSession(exerciseName)
        notificationManager.updateRestTimer(exerciseName, restSeconds, restSeconds)

        timerJob = viewModelScope.launch {
            var remaining = restSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update { it.copy(secondsLeft = remaining) }
                // Bildirim güncellemeleri UI timer'ından daha seyrek akar.
                withContext(Dispatchers.IO) {
                    notificationManager.updateRestTimer(exerciseName, remaining, restSeconds)
                }
            }
            _restTimer.update { it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0) }
            // Ses + "Hazırsın!" bildirimi
            withContext(Dispatchers.IO) {
                notificationManager.notifyTimerDone(exerciseName)
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimer.update { it.copy(isRunning = false, isPaused = false) }
        notificationManager.stopWorkoutSession()
    }

    fun dismissRestTimer() {
        timerJob?.cancel()
        _restTimer.value = RestTimerState()
        notificationManager.stopWorkoutSession()
    }

    fun startActivityCountdownTimer(exerciseId: String, exerciseName: String, seconds: Int) {
        val total = seconds.coerceAtLeast(1)
        timerJob?.cancel()
        notificationManager.stopWorkoutSession()
        _restTimer.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            isDone = false,
            secondsLeft = total,
            totalSeconds = total,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            purpose = TimerPurpose.Activity,
            mode = TimerMode.Countdown,
            elapsedSeconds = 0
        )
        notificationManager.updateActivityTimer(
            exerciseName = exerciseName,
            elapsedSeconds = 0,
            totalSeconds = total,
            isStopwatch = false
        )
        timerJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update {
                    it.copy(
                        secondsLeft = remaining,
                        elapsedSeconds = total - remaining
                    )
                }
                notificationManager.updateActivityTimer(
                    exerciseName = exerciseName,
                    elapsedSeconds = total - remaining,
                    totalSeconds = total,
                    isStopwatch = false
                )
            }
            saveActivityDuration(exerciseId, total)
            notificationManager.notifyActivityTimerSaved(exerciseName, total)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0, elapsedSeconds = total)
            }
        }
    }

    fun startActivityStopwatchTimer(exerciseId: String, exerciseName: String) {
        timerJob?.cancel()
        notificationManager.stopWorkoutSession()
        _restTimer.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            isDone = false,
            secondsLeft = 0,
            totalSeconds = 0,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            purpose = TimerPurpose.Activity,
            mode = TimerMode.Stopwatch,
            elapsedSeconds = 0
        )
        notificationManager.updateActivityTimer(
            exerciseName = exerciseName,
            elapsedSeconds = 0,
            totalSeconds = 0,
            isStopwatch = true
        )
        timerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                delay(1000L)
                elapsed++
                _restTimer.update { it.copy(elapsedSeconds = elapsed, secondsLeft = elapsed) }
                notificationManager.updateActivityTimer(
                    exerciseName = exerciseName,
                    elapsedSeconds = elapsed,
                    totalSeconds = 0,
                    isStopwatch = true
                )
            }
        }
    }

    fun startTimedSetCountdownTimer(exerciseId: String, exerciseName: String, setIndex: Int, seconds: Int) {
        val total = seconds.coerceAtLeast(1)
        val notificationName = "$exerciseName Set ${setIndex + 1}"
        timerJob?.cancel()
        notificationManager.stopWorkoutSession()
        _restTimer.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            isDone = false,
            secondsLeft = total,
            totalSeconds = total,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            setIndex = setIndex,
            purpose = TimerPurpose.TimedSet,
            mode = TimerMode.Countdown,
            elapsedSeconds = 0
        )
        notificationManager.updateActivityTimer(
            exerciseName = notificationName,
            elapsedSeconds = 0,
            totalSeconds = total,
            isStopwatch = false
        )
        timerJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update {
                    it.copy(
                        secondsLeft = remaining,
                        elapsedSeconds = total - remaining
                    )
                }
                notificationManager.updateActivityTimer(
                    exerciseName = notificationName,
                    elapsedSeconds = total - remaining,
                    totalSeconds = total,
                    isStopwatch = false
                )
            }
            saveTimedSetDuration(exerciseId, setIndex, total)
            notificationManager.notifyActivityTimerSaved(notificationName, total)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0, elapsedSeconds = total)
            }
        }
    }

    fun startTimedSetStopwatchTimer(exerciseId: String, exerciseName: String, setIndex: Int) {
        val notificationName = "$exerciseName Set ${setIndex + 1}"
        timerJob?.cancel()
        notificationManager.stopWorkoutSession()
        _restTimer.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            isDone = false,
            secondsLeft = 0,
            totalSeconds = 0,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            setIndex = setIndex,
            purpose = TimerPurpose.TimedSet,
            mode = TimerMode.Stopwatch,
            elapsedSeconds = 0
        )
        notificationManager.updateActivityTimer(
            exerciseName = notificationName,
            elapsedSeconds = 0,
            totalSeconds = 0,
            isStopwatch = true
        )
        timerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                delay(1000L)
                elapsed++
                _restTimer.update { it.copy(elapsedSeconds = elapsed, secondsLeft = elapsed) }
                notificationManager.updateActivityTimer(
                    exerciseName = notificationName,
                    elapsedSeconds = elapsed,
                    totalSeconds = 0,
                    isStopwatch = true
                )
            }
        }
    }

    fun stopVisibleTimer() {
        val timer = _restTimer.value
        if (timer.purpose == TimerPurpose.Activity) {
            val secondsToSave = when (timer.mode) {
                TimerMode.Stopwatch -> timer.elapsedSeconds
                TimerMode.Countdown -> (timer.totalSeconds - timer.secondsLeft).coerceAtLeast(0)
            }
            timerJob?.cancel()
            if (secondsToSave > 0) saveActivityDuration(timer.exerciseId, secondsToSave)
            if (secondsToSave > 0) notificationManager.notifyActivityTimerSaved(timer.exerciseName, secondsToSave)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = secondsToSave > 0, elapsedSeconds = secondsToSave)
            }
        } else if (timer.purpose == TimerPurpose.TimedSet) {
            val secondsToSave = when (timer.mode) {
                TimerMode.Stopwatch -> timer.elapsedSeconds
                TimerMode.Countdown -> (timer.totalSeconds - timer.secondsLeft).coerceAtLeast(0)
            }
            val notificationName = "${timer.exerciseName} Set ${timer.setIndex + 1}"
            timerJob?.cancel()
            if (secondsToSave > 0) saveTimedSetDuration(timer.exerciseId, timer.setIndex, secondsToSave)
            if (secondsToSave > 0) notificationManager.notifyActivityTimerSaved(notificationName, secondsToSave)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = secondsToSave > 0, secondsLeft = 0, elapsedSeconds = secondsToSave)
            }
        } else {
            stopRestTimer()
        }
    }

    fun pauseVisibleTimer() {
        val timer = _restTimer.value
        if (!timer.isRunning) return
        timerJob?.cancel()
        if (timer.purpose == TimerPurpose.Activity || timer.purpose == TimerPurpose.TimedSet) {
            notificationManager.updateActivityTimer(
                exerciseName = if (timer.purpose == TimerPurpose.TimedSet) "${timer.exerciseName} Set ${timer.setIndex + 1}" else timer.exerciseName,
                elapsedSeconds = timer.elapsedSeconds,
                totalSeconds = timer.totalSeconds,
                isStopwatch = timer.mode == TimerMode.Stopwatch,
                isPaused = true
            )
        } else {
            notificationManager.stopWorkoutSession()
        }
        _restTimer.update { it.copy(isRunning = false, isPaused = true, isDone = false) }
    }

    fun resumeVisibleTimer() {
        val timer = _restTimer.value
        if (!timer.isPaused || timer.isDone) return
        timerJob?.cancel()
        _restTimer.update { it.copy(isRunning = true, isPaused = false) }

        when (timer.purpose) {
            TimerPurpose.Rest -> resumeRestCountdown(timer)
            TimerPurpose.Activity -> when (timer.mode) {
                TimerMode.Countdown -> resumeActivityCountdown(timer)
                TimerMode.Stopwatch -> resumeActivityStopwatch(timer)
            }
            TimerPurpose.TimedSet -> when (timer.mode) {
                TimerMode.Countdown -> resumeTimedSetCountdown(timer)
                TimerMode.Stopwatch -> resumeTimedSetStopwatch(timer)
            }
        }
    }

    fun dismissVisibleTimer() {
        val timer = _restTimer.value
        if (timer.purpose == TimerPurpose.Activity || timer.purpose == TimerPurpose.TimedSet) {
            timerJob?.cancel()
            _restTimer.value = RestTimerState()
            notificationManager.stopWorkoutSession()
        } else {
            dismissRestTimer()
        }
    }

    private fun resumeRestCountdown(timer: RestTimerState) {
        val total = timer.totalSeconds.coerceAtLeast(timer.secondsLeft)
        var remaining = timer.secondsLeft.coerceAtLeast(0)
        notificationManager.startWorkoutSession(timer.exerciseName)
        notificationManager.updateRestTimer(timer.exerciseName, remaining, total)
        timerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update { it.copy(secondsLeft = remaining) }
                withContext(Dispatchers.IO) {
                    notificationManager.updateRestTimer(timer.exerciseName, remaining, total)
                }
            }
            _restTimer.update { it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0) }
            withContext(Dispatchers.IO) {
                notificationManager.notifyTimerDone(timer.exerciseName)
            }
        }
    }

    private fun resumeActivityCountdown(timer: RestTimerState) {
        val total = timer.totalSeconds.coerceAtLeast(1)
        var remaining = timer.secondsLeft.coerceIn(0, total)
        notificationManager.updateActivityTimer(
            exerciseName = timer.exerciseName,
            elapsedSeconds = total - remaining,
            totalSeconds = total,
            isStopwatch = false
        )
        timerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update {
                    it.copy(
                        secondsLeft = remaining,
                        elapsedSeconds = total - remaining
                    )
                }
                notificationManager.updateActivityTimer(
                    exerciseName = timer.exerciseName,
                    elapsedSeconds = total - remaining,
                    totalSeconds = total,
                    isStopwatch = false
                )
            }
            saveActivityDuration(timer.exerciseId, total)
            notificationManager.notifyActivityTimerSaved(timer.exerciseName, total)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0, elapsedSeconds = total)
            }
        }
    }

    private fun resumeActivityStopwatch(timer: RestTimerState) {
        var elapsed = timer.elapsedSeconds.coerceAtLeast(0)
        notificationManager.updateActivityTimer(
            exerciseName = timer.exerciseName,
            elapsedSeconds = elapsed,
            totalSeconds = 0,
            isStopwatch = true
        )
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                elapsed++
                _restTimer.update { it.copy(elapsedSeconds = elapsed, secondsLeft = elapsed) }
                notificationManager.updateActivityTimer(
                    exerciseName = timer.exerciseName,
                    elapsedSeconds = elapsed,
                    totalSeconds = 0,
                    isStopwatch = true
                )
            }
        }
    }

    private fun resumeTimedSetCountdown(timer: RestTimerState) {
        val total = timer.totalSeconds.coerceAtLeast(1)
        var remaining = timer.secondsLeft.coerceIn(0, total)
        val notificationName = "${timer.exerciseName} Set ${timer.setIndex + 1}"
        notificationManager.updateActivityTimer(
            exerciseName = notificationName,
            elapsedSeconds = total - remaining,
            totalSeconds = total,
            isStopwatch = false
        )
        timerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _restTimer.update {
                    it.copy(
                        secondsLeft = remaining,
                        elapsedSeconds = total - remaining
                    )
                }
                notificationManager.updateActivityTimer(
                    exerciseName = notificationName,
                    elapsedSeconds = total - remaining,
                    totalSeconds = total,
                    isStopwatch = false
                )
            }
            saveTimedSetDuration(timer.exerciseId, timer.setIndex, total)
            notificationManager.notifyActivityTimerSaved(notificationName, total)
            _restTimer.update {
                it.copy(isRunning = false, isPaused = false, isDone = true, secondsLeft = 0, elapsedSeconds = total)
            }
        }
    }

    private fun resumeTimedSetStopwatch(timer: RestTimerState) {
        var elapsed = timer.elapsedSeconds.coerceAtLeast(0)
        val notificationName = "${timer.exerciseName} Set ${timer.setIndex + 1}"
        notificationManager.updateActivityTimer(
            exerciseName = notificationName,
            elapsedSeconds = elapsed,
            totalSeconds = 0,
            isStopwatch = true
        )
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                elapsed++
                _restTimer.update { it.copy(elapsedSeconds = elapsed, secondsLeft = elapsed) }
                notificationManager.updateActivityTimer(
                    exerciseName = notificationName,
                    elapsedSeconds = elapsed,
                    totalSeconds = 0,
                    isStopwatch = true
                )
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REACTIVE OBSERVATION — Room Flow'ları combine eder
    // ═════════════════════════════════════════════════════════════════════════

    @OptIn(FlowPreview::class)
    private fun startObserving() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            if (sessionJob?.isActive == true) return
            sessionJob = viewModelScope.launch {
                supabase.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage }
                val resolvedUserId = supabase.auth.currentSessionOrNull()?.user?.id
                if (resolvedUserId == null) {
                    updateState { prev ->
                        if (prev.hasProgramLoaded || prev.dayStates.isNotEmpty()) {
                            prev.copy(isLoading = false)
                        } else {
                            prev.copy(
                                isLoading = false,
                                hasProgramLoaded = false,
                                dayStates = persistentListOf(),
                                currentProgramId = ""
                            )
                        }
                    }
                } else {
                    startObservingForUser(resolvedUserId)
                }
            }
            return
        }

        // İlk seferde arka planda sync başlat
        startObservingForUser(userId)
    }

    private fun startObservingForUser(userId: String) {
        viewModelScope.launch {
            syncCoordinator.refreshWorkout(userId)
        }
        loadProfileWeight(userId)

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
            .debounce(100)
            .distinctUntilChanged()
            .collect { (innerTriple, setCompletions, _) ->
                val (program, completions, streak) = innerTriple
                if (program == null || program.days.isEmpty()) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            hasProgramLoaded = false,
                            dayStates = persistentListOf(),
                            currentStreak = streak,
                            setCompletions = setCompletions
                        )
                    }
                    return@collect
                }

                val dayStates = buildDayStates(program, completions, setCompletions)
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
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            startObserving()
            return
        }
        // Observe zaten aktifse tekrar başlatma — sadece sync tetikle
        if (observeJob?.isActive != true) startObserving()
        loadProfileWeight(userId)
        viewModelScope.launch {
            syncCoordinator.refreshWorkout(
                userId = userId,
                ttlMillis = RESUME_SYNC_TTL_MS,
                debounceMillis = RESUME_SYNC_DEBOUNCE_MS
            )
        }
    }

    fun triggerInitialSync() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            syncCoordinator.refreshWorkout(userId, debounceMillis = 600)
        }
    }

    /** Program değişikliğinden sonra zorla sync. */
    fun forceRefresh() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        if (observeJob?.isActive != true) startObserving()
        viewModelScope.launch {
            syncCoordinator.refreshWorkout(userId, force = true, debounceMillis = 0)
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

        val isCurrentlyDone = setIndex in (currentState.setCompletions[dbExerciseId] ?: emptySet())
        markSetCompletionOptimistically(dbExerciseId, setIndex, !isCurrentlyDone)
        val isDurationSet = isDurationSetBased(exercise)

        viewModelScope.launch {
            if (isCurrentlyDone) {
                // Tik'i kaldır: reps_actual'ı null yap (weight draft olarak korunur)
                workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, null)
            } else {
                val setDraft = _draftInputs.value[draftInputKey(exerciseId, setIndex)]
                if (isDurationSet) {
                    val durationSeconds = setDraft
                        ?.takeIf { it.hasDuration }
                        ?.duration
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 }
                        ?: currentState.setDurations[exerciseId]?.get(setIndex)
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 }
                        ?: exercise.plannedDurationSeconds()
                        ?: 60
                    workoutRepository.upsertSetActivityMetrics(
                        userId = userId,
                        exerciseId = dbExerciseId,
                        programDayId = programDayId,
                        setIndex = setIndex,
                        durationSeconds = durationSeconds,
                        distanceMeters = null
                    )
                    workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, 1)
                } else {
                // Tik ekle: tekrar sayısı programda tanımlı olan değerden gelir.
                val reps = setDraft
                    ?.takeIf { it.hasReps }
                    ?.reps
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: exercise.reps.toIntOrNull()?.takeIf { it > 0 }
                    ?: 1
                // Ağırlık fallback: kullanıcı girdisi > son seans > program planı > vücut ağırlığı hareketinde profil kilosu.
                val hasStateWeight = currentState.lastSessionData[exerciseId]?.get(setIndex)?.first != null
                val persistedWeights = if (hasStateWeight) emptyMap() else loadPersistedExerciseWeights(userId, dbExerciseId)
                val weight = setDraft
                    ?.takeIf { it.hasWeight }
                    ?.weight
                    ?.toFloatOrNull()
                    ?: currentState.setWeights[exerciseId]?.get(setIndex)?.toFloatOrNull()
                    ?: currentState.lastSessionData[exerciseId]?.get(setIndex)?.first
                    ?: persistedWeights[setIndex]
                    ?: defaultWeightFor(exercise, currentState.profileWeightKg)
                if (weight != null) {
                    workoutRepository.upsertSetWeightDraft(userId, dbExerciseId, programDayId, setIndex, weight)
                }
                workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, reps)
                }

                val completedAfter = (currentState.setCompletions[dbExerciseId] ?: emptySet()) + setIndex
                val shouldAutoComplete =
                    !isActivityBased(exercise) &&
                    exercise.sets > 0 &&
                    completedAfter.size >= exercise.sets &&
                    exerciseId !in dayState.completedIds

                if (shouldAutoComplete) {
                    autoCompleteStrengthExercise(
                        userId = userId,
                        dayIdx = currentState.selectedDayIdx,
                        exercise = exercise,
                        programDayId = programDayId,
                        exerciseId = exerciseId,
                        dbExerciseId = dbExerciseId,
                        currentState = currentState
                    )
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PROGRESSIVE OVERLOAD — set bazlı ağırlık/tekrar girişi
    // ═════════════════════════════════════════════════════════════════════════

    fun updateSetWeight(exerciseId: String, setIndex: Int, value: String) {
        val key = draftInputKey(exerciseId, setIndex)
        _draftInputs.update { drafts ->
            drafts + (key to drafts[key].orEmpty().copy(weight = value, hasWeight = true))
        }
        // Set tik atılmış olsa da olmasa da Room'a draft olarak yaz (debounced).
        scheduleDraftPersist(exerciseId, setIndex, weight = true, reps = false)
    }

    fun updateSetDuration(exerciseId: String, setIndex: Int, value: String) {
        val clean = value.filter { it.isDigit() }.take(4)
        val key = draftInputKey(exerciseId, setIndex)
        _draftInputs.update { drafts ->
            drafts + (key to drafts[key].orEmpty().copy(duration = clean, hasDuration = true))
        }
        scheduleSetDurationPersist(exerciseId, setIndex)
    }

    fun updateActivityDuration(exerciseId: String, value: String) {
        updateState { state ->
            state.copy(activityDurations = state.activityDurations + (exerciseId to value))
        }
        scheduleActivityDraftPersist(exerciseId)
    }

    fun updateActivityDistance(exerciseId: String, value: String) {
        updateState { state ->
            state.copy(activityDistances = state.activityDistances + (exerciseId to value))
        }
        scheduleActivityDraftPersist(exerciseId)
    }

    fun updateActivityElevation(exerciseId: String, value: String) {
        updateState { state ->
            state.copy(activityElevations = state.activityElevations + (exerciseId to value))
        }
        scheduleActivityDraftPersist(exerciseId)
    }

    fun updateActivityIncline(exerciseId: String, value: String) {
        updateState { state ->
            state.copy(activityInclines = state.activityInclines + (exerciseId to value))
        }
        scheduleActivityDraftPersist(exerciseId)
    }

    fun updateActivityReps(exerciseId: String, value: String) {
        val clean = value.filter { it.isDigit() }.take(6)
        updateState { state ->
            state.copy(activityReps = state.activityReps + (exerciseId to clean))
        }
        scheduleActivityDraftPersist(exerciseId)
    }

    fun updateSetReps(exerciseId: String, setIndex: Int, value: String) {
        val key = draftInputKey(exerciseId, setIndex)
        _draftInputs.update { drafts ->
            drafts + (key to drafts[key].orEmpty().copy(reps = value, hasReps = true))
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
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }

        val key = "$exerciseId:$setIndex:${if (weight) "w" else "r"}"
        draftPersistJobs[key]?.cancel()
        draftPersistJobs[key] = viewModelScope.launch {
            delay(500L)
            val latestDraft = _draftInputs.value[draftInputKey(exerciseId, setIndex)]
            val latest = uiState.value
            if (weight) {
                val weightValue = latestDraft
                    ?.takeIf { it.hasWeight }
                    ?.weight
                    ?: latest.setWeights[exerciseId]?.get(setIndex).orEmpty()
                updateState { state ->
                    val exerciseMap = state.setWeights[exerciseId].orEmpty().toMutableMap()
                    exerciseMap[setIndex] = weightValue
                    state.copy(setWeights = state.setWeights + (exerciseId to exerciseMap))
                }
                val w = weightValue.toFloatOrNull()
                workoutRepository.upsertSetWeightDraft(userId, dbExerciseId, programDayId, setIndex, w)
            }
            if (reps) {
                val repsValue = latestDraft
                    ?.takeIf { it.hasReps }
                    ?.reps
                    ?: latest.setReps[exerciseId]?.get(setIndex).orEmpty()
                updateState { state ->
                    val exerciseMap = state.setReps[exerciseId].orEmpty().toMutableMap()
                    exerciseMap[setIndex] = repsValue
                    state.copy(setReps = state.setReps + (exerciseId to exerciseMap))
                }
                // Sadece set zaten tikliyse reps_actual güncellenir — tikli olmayan set draft kalmalı.
                val isDone = setIndex in (latest.setCompletions[dbExerciseId] ?: emptySet())
                if (isDone) {
                    val r = repsValue.toIntOrNull()
                    if (r != null) {
                        workoutRepository.upsertSetRepsActual(userId, dbExerciseId, programDayId, setIndex, r)
                    }
                }
            }
        }
    }

    private fun scheduleSetDurationPersist(exerciseId: String, setIndex: Int) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val state = uiState.value
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }

        val key = "$exerciseId:$setIndex:dur"
        draftPersistJobs[key]?.cancel()
        draftPersistJobs[key] = viewModelScope.launch {
            delay(500L)
            val latest = uiState.value
            val latestDraft = _draftInputs.value[draftInputKey(exerciseId, setIndex)]
            val durationValue = latestDraft
                ?.takeIf { it.hasDuration }
                ?.duration
                ?: latest.setDurations[exerciseId]?.get(setIndex).orEmpty()
            updateState { state ->
                val exerciseMap = state.setDurations[exerciseId].orEmpty().toMutableMap()
                exerciseMap[setIndex] = durationValue
                state.copy(setDurations = state.setDurations + (exerciseId to exerciseMap))
            }
            workoutRepository.upsertSetActivityMetrics(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = setIndex,
                durationSeconds = durationValue.toIntOrNull(),
                distanceMeters = null
            )
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
        val exercisesToLoad = dayState.day.exercises
            .filterNot { state.lastSessionData.containsKey(it.id) }
        if (exercisesToLoad.isEmpty()) return

        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val dbIds = exercisesToLoad.map { it.exerciseTableId.ifBlank { it.id } }.distinct()
        viewModelScope.launch {
            workoutRepository.getSessionSetsForExercises(userId, dbIds).onSuccess { sessionSets ->
                updateState { latest ->
                    var lastSessionData = latest.lastSessionData
                    var setWeights = latest.setWeights
                    var setReps = latest.setReps

                    exercisesToLoad.forEach { exercise ->
                        val dbExerciseId = exercise.exerciseTableId.ifBlank { exercise.id }
                        val todaySets = sessionSets.today[dbExerciseId].orEmpty()
                        val sourceSets = todaySets.ifEmpty { sessionSets.previous[dbExerciseId].orEmpty() }
                        val plannedReps = exercise.reps
                        val totalSets = exercise.sets

                        if (sourceSets.isNotEmpty()) {
                            val dataMap = sourceSets.associate { it.setIndex to (it.weightKg to it.repsActual) }
                            lastSessionData = lastSessionData + (exercise.id to dataMap)

                            if (setWeights[exercise.id].isNullOrEmpty()) {
                                val prefill = sourceSets
                                    .filter { it.weightKg != null }
                                    .associate { it.setIndex to it.weightKg!!.toString() }
                                if (prefill.isNotEmpty()) {
                                    setWeights = setWeights + (exercise.id to prefill)
                                }
                            }

                            if (setReps[exercise.id].isNullOrEmpty() && totalSets > 0 && plannedReps.isNotEmpty()) {
                                val savedReps = sourceSets
                                    .filter { it.repsActual != null }
                                    .associate { it.setIndex to it.repsActual!!.toString() }
                                val repsPrefill = (0 until totalSets).associate { i ->
                                    i to (savedReps[i] ?: plannedReps)
                                }
                                setReps = setReps + (exercise.id to repsPrefill)
                            }
                        } else if (setReps[exercise.id].isNullOrEmpty() && totalSets > 0 && plannedReps.isNotEmpty()) {
                            val repsPrefill = (0 until totalSets).associate { i -> i to plannedReps }
                            setReps = setReps + (exercise.id to repsPrefill)
                        }
                    }

                    latest.copy(
                        lastSessionData = lastSessionData,
                        setWeights = setWeights,
                        setReps = setReps
                    )
                }
            }
        }
    }

    fun loadLastSession(exerciseId: String) {
        // Zaten yüklenmişse tekrar yükleme
        if (uiState.value.lastSessionData.containsKey(exerciseId)) return

        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        // DB'de kayıtlar canonical exercises.id (exerciseTableId) ile tutulur; state key'i pe.id.
        val state = uiState.value
        val exercise = state.dayStates.flatMap { it.day.exercises }.find { it.id == exerciseId }
        val dbExerciseId = exercise?.exerciseTableId?.ifBlank { exerciseId } ?: exerciseId
        val plannedReps = exercise?.reps   // Program oluşturulurken seçilen tekrar sayısı (e.g. "12")
        val totalSets   = exercise?.sets ?: 0

        viewModelScope.launch {
            val today = LocalDate.now().toString()

            // 1. Önce bugünkü kayıtları yükle (uygulama kapanıp açılsa da draft ağırlıklar korunur)
            val todaySets = workoutRepository.getSetsForDate(userId, dbExerciseId, today)
                .getOrNull().orEmpty()

            if (todaySets.isNotEmpty()) {
                // Bugüne ait kayıtlar var — bunları lastSessionData olarak sakla ve setWeights'i doldur
                val dataMap = todaySets.associate { it.setIndex to (it.weightKg to it.repsActual) }
                updateState { s ->
                    s.copy(lastSessionData = s.lastSessionData + (exerciseId to dataMap))
                }
                val currentWeights = uiState.value.setWeights[exerciseId]
                if (currentWeights.isNullOrEmpty()) {
                    val prefill = todaySets
                        .filter { it.weightKg != null }
                        .associate { it.setIndex to it.weightKg!!.toString() }
                    if (prefill.isNotEmpty()) {
                        updateState { s ->
                            s.copy(setWeights = s.setWeights + (exerciseId to prefill))
                        }
                    }
                }
                prefillActivityInputs(exerciseId, todaySets)
                prefillSetDurations(exerciseId, todaySets)
                // Tekrar sayılarını doldur: bugünkü repsActual > program planı
                val currentReps = uiState.value.setReps[exerciseId]
                if (currentReps.isNullOrEmpty() && totalSets > 0 && !plannedReps.isNullOrEmpty()) {
                    val savedReps = todaySets.filter { it.repsActual != null }
                        .associate { it.setIndex to it.repsActual!!.toString() }
                    val repsPrefill = (0 until totalSets).associate { i ->
                        i to (savedReps[i] ?: plannedReps)
                    }
                    updateState { s -> s.copy(setReps = s.setReps + (exerciseId to repsPrefill)) }
                }
                return@launch
            }

            // 2. Bugün kayıt yoksa önceki seansı yükle (prefill için)
            workoutRepository.getLastSessionSets(userId, dbExerciseId).onSuccess { sets ->
                if (sets.isEmpty()) {
                    // Hiç DB kaydı yok — program planındaki tekrar sayısını göster
                    if (totalSets > 0 && !plannedReps.isNullOrEmpty()) {
                        val currentReps = uiState.value.setReps[exerciseId]
                        if (currentReps.isNullOrEmpty()) {
                            val repsPrefill = (0 until totalSets).associate { i -> i to plannedReps }
                            updateState { s -> s.copy(setReps = s.setReps + (exerciseId to repsPrefill)) }
                        }
                    }
                    return@onSuccess
                }
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
                prefillActivityInputs(exerciseId, sets)
                prefillSetDurations(exerciseId, sets)
                // Tekrar sayılarını doldur: önceki seans repsActual > program planı
                val currentReps = uiState.value.setReps[exerciseId]
                if (currentReps.isNullOrEmpty() && totalSets > 0 && !plannedReps.isNullOrEmpty()) {
                    val savedReps = sets.filter { it.repsActual != null }
                        .associate { it.setIndex to it.repsActual!!.toString() }
                    val repsPrefill = (0 until totalSets).associate { i ->
                        i to (savedReps[i] ?: plannedReps)
                    }
                    updateState { s -> s.copy(setReps = s.setReps + (exerciseId to repsPrefill)) }
                }
            }
        }
    }

    fun loadExerciseHistory(exerciseId: String) {
        if (uiState.value.exerciseHistory.containsKey(exerciseId)) return
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val state = uiState.value
        val dbExerciseId = state.dayStates
            .flatMap { it.day.exercises }
            .find { it.id == exerciseId }
            ?.exerciseTableId
            ?.ifBlank { exerciseId }
            ?: exerciseId
        viewModelScope.launch {
            workoutRepository.getExerciseWeightHistory(userId, dbExerciseId).onSuccess { history ->
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
                val state = uiState.value
                val dbExerciseId = state.dayStates
                    .flatMap { it.day.exercises }
                    .find { it.id == exerciseId }
                    ?.exerciseTableId
                    ?.ifBlank { exerciseId }
                    ?: exerciseId
                workoutRepository.getExerciseWeightHistory(userId, dbExerciseId)
                    .getOrNull() ?: emptyList()
            }

            val exercise = uiState.value.dayStates
                .flatMap { it.day.exercises }
                .find { it.id == exerciseId }
            val prompt = AiAnalysisPrompts.exerciseProgression(
                exerciseName = exerciseName,
                targetMuscle = listOfNotNull(exercise?.target, exercise?.category)
                    .filter { it.isNotBlank() }
                    .joinToString(" / "),
                history = history
            )

            if (prompt == null) {
                updateState {
                    it.copy(
                        exerciseAiInsight = it.exerciseAiInsight + (exerciseId to "Bu hareket için henüz analiz yapacak kadar ağırlık, süre veya mesafe verisi yok. 1-2 seans veri girdikten sonra net bir hedef çıkarabilirim."),
                        exerciseAiLoading = it.exerciseAiLoading - exerciseId
                    )
                }
                return@launch
            }

            if (!planRepository.consumeCredit()) {
                updateState { it.copy(exerciseAiLoading = it.exerciseAiLoading - exerciseId) }
                sendEvent(WorkoutEvent.ShowPaywall)
                return@launch
            }

            val result = geminiRepository.chat(
                emptyList(),
                prompt.userMessage,
                prompt.systemPrompt,
                AiToolType.WORKOUT_PROGRESS_ANALYSIS
            )
            if (result.exceptionOrNull() is AiAccessException) {
                updateState { it.copy(exerciseAiLoading = it.exerciseAiLoading - exerciseId) }
                sendEvent(WorkoutEvent.ShowPaywall)
                return@launch
            }
            result.onSuccess { response ->
                viewModelScope.launch { planRepository.refresh() }
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
            val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }
            val activityBased = isActivityBased(exercise)

            if (isCompleting) {
                val durationSeconds = currentState.activityDurations[exerciseId]?.toDurationSecondsOrNull()
                val distanceMeters = currentState.activityDistances[exerciseId]?.toFloatInputOrNull()
                val elevationMeters = currentState.activityElevations[exerciseId]?.toFloatInputOrNull()
                val inclinePercent = currentState.activityInclines[exerciseId]?.toFloatInputOrNull()
                val activityRepsActual = activityRepsActualFor(exercise, currentState, exerciseId)

                markExerciseCompletedOptimistically(dayIdx, exerciseId)
                if (activityBased) {
                    markSetCompletionOptimistically(dbExerciseId, ACTIVITY_SET_INDEX, true)
                    workoutRepository.upsertSetActivityMetrics(
                        userId = userId,
                        exerciseId = dbExerciseId,
                        programDayId = programDayId,
                        setIndex = ACTIVITY_SET_INDEX,
                        durationSeconds = durationSeconds,
                        distanceMeters = distanceMeters,
                        elevationMeters = elevationMeters,
                        inclinePercent = inclinePercent
                    )
                    workoutRepository.upsertSetRepsActual(
                        userId = userId,
                        exerciseId = dbExerciseId,
                        programDayId = programDayId,
                        setIndex = ACTIVITY_SET_INDEX,
                        repsActual = activityRepsActual
                    )
                } else {
                    markExerciseSetsCompletedOptimistically(dbExerciseId, exercise.sets)
                    // Tüm setleri Room'a yaz — Flow otomatik UI'ı günceller.
                    val plannedReps = exercise.reps.toIntOrNull()?.takeIf { it > 0 } ?: 1
                    workoutRepository.fillExerciseSetCompletions(
                        userId = userId,
                        exerciseId = dbExerciseId,
                        programDayId = programDayId,
                        totalSets = exercise.sets,
                        defaultRepsActual = plannedReps
                    )
                    persistStrengthSetWeights(
                        userId = userId,
                        exercise = exercise,
                        exerciseId = exerciseId,
                        dbExerciseId = dbExerciseId,
                        programDayId = programDayId,
                        currentState = currentState
                    )
                }
                // exerciseTableId = exercises tablosundaki gerçek ID (DB logları için)
                workoutRepository.completeExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = dbExerciseId,
                    setsCompleted = if (activityBased) 1 else exercise.sets,
                    repsCompleted = if (activityBased && supportsActivityReps(exercise)) {
                        activityRepsActual
                    } else if (activityBased) {
                        0
                    } else {
                        exercise.reps.toIntOrNull() ?: 0
                    },
                    durationSeconds = durationSeconds ?: 0
                )
                syncMatchingMovementListEvents(exercise, dbExerciseId)

                refreshStatsAfterCompletion(userId)

                // Başarım/XP kontrolü
                viewModelScope.launch {
                    checkDayCompletion(dayIdx, userId)
                    checkAndUnlockAchievements(userId)
                }
            } else {
                markExerciseIncompleteOptimistically(dayIdx, exerciseId)
                markExerciseSetCompletionsClearedOptimistically(dbExerciseId)
                // Önce egzersiz tamamlanma logunu geri al; kart/ilerleme UI'ı hemen normale döner.
                // Set kayıtlarını ayrıca temizliyoruz, ama remote set-sync bunu geciktirse bile
                // "Geri Al" aksiyonu kullanıcıya anında yansımış olur.
                workoutRepository.uncompleteExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = dbExerciseId
                )
                // Tüm setleri Room'dan sil — Flow otomatik UI'ı günceller
                workoutRepository.clearExerciseSetCompletions(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId
                )

                // Stats rollback + challenge progress tazele (istismar önleme):
                // yap→geri al döngüsü XP/total_exercises/streak/challenge ilerlemesini
                // kalıcı hale getirmesin.
                refreshStatsAfterRollback(userId)
            }
        }
    }

    private suspend fun autoCompleteStrengthExercise(
        userId: String,
        dayIdx: Int,
        exercise: Exercise,
        programDayId: String,
        exerciseId: String,
        dbExerciseId: String,
        currentState: WorkoutScreenState
    ) {
        markExerciseCompletedOptimistically(dayIdx, exerciseId)
        markExerciseSetsCompletedOptimistically(dbExerciseId, exercise.sets)

        val plannedReps = exercise.reps.toIntOrNull()?.takeIf { it > 0 } ?: 1
        workoutRepository.fillExerciseSetCompletions(
            userId = userId,
            exerciseId = dbExerciseId,
            programDayId = programDayId,
            totalSets = exercise.sets,
            defaultRepsActual = plannedReps
        )

        persistStrengthSetWeights(
            userId = userId,
            exercise = exercise,
            exerciseId = exerciseId,
            dbExerciseId = dbExerciseId,
            programDayId = programDayId,
            currentState = currentState
        )

        workoutRepository.completeExercise(
            userId = userId,
            programDayId = programDayId,
            exerciseId = dbExerciseId,
            setsCompleted = exercise.sets,
            repsCompleted = exercise.reps.toIntOrNull() ?: 0,
            durationSeconds = 0
        )
        syncMatchingMovementListEvents(exercise, dbExerciseId)

        refreshStatsAfterCompletion(userId)
        viewModelScope.launch {
            checkDayCompletion(dayIdx, userId)
            checkAndUnlockAchievements(userId)
        }
    }

    private fun autoCompleteActivityExercise(exerciseId: String, durationSeconds: Int) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val currentState = uiState.value
        val dayIdx = currentState.selectedDayIdx
        val dayState = currentState.dayStates.getOrNull(dayIdx) ?: return
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        if (!isActivityBased(exercise)) return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return

        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }
        val distanceMeters = currentState.activityDistances[exerciseId]?.toFloatInputOrNull()
        val elevationMeters = currentState.activityElevations[exerciseId]?.toFloatInputOrNull()
        val inclinePercent = currentState.activityInclines[exerciseId]?.toFloatInputOrNull()
        val activityRepsActual = activityRepsActualFor(exercise, currentState, exerciseId)
        val wasCompleted = exerciseId in dayState.completedIds
        val key = "$exerciseId:activity"
        draftPersistJobs[key]?.cancel()

        markSetCompletionOptimistically(dbExerciseId, ACTIVITY_SET_INDEX, true)
        if (!wasCompleted) markExerciseCompletedOptimistically(dayIdx, exerciseId)

        viewModelScope.launch {
            workoutRepository.upsertSetActivityMetrics(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = ACTIVITY_SET_INDEX,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                elevationMeters = elevationMeters,
                inclinePercent = inclinePercent
            )
            workoutRepository.upsertSetRepsActual(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = ACTIVITY_SET_INDEX,
                repsActual = activityRepsActual
            )

            if (!wasCompleted) {
                workoutRepository.completeExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = dbExerciseId,
                    setsCompleted = 1,
                    repsCompleted = if (supportsActivityReps(exercise)) activityRepsActual else 0,
                    durationSeconds = durationSeconds
                )
                syncMatchingMovementListEvents(exercise, dbExerciseId)

                refreshStatsAfterCompletion(userId)
                viewModelScope.launch {
                    checkDayCompletion(dayIdx, userId)
                    checkAndUnlockAchievements(userId)
                }
            }
        }
    }

    private suspend fun syncMatchingMovementListEvents(exercise: Exercise, dbExerciseId: String) {
        if (exercise.challengeId != null) return
        val today = LocalDate.now().toString()
        val events = challengeRepository.listMyEventsForDate(today)
            .getOrNull()
            .orEmpty()
            .filter { summary ->
                summary.isJoined &&
                    summary.kind == ChallengeKind.Event &&
                    summary.event?.mode == EventMode.MovementList
            }
        if (events.isEmpty()) return

        var completedAny = false
        events.forEach { summary ->
            val detail = challengeRepository.getChallengeDetail(summary.id).getOrNull() ?: return@forEach
            val movementIds = detail.movements
                .filter { movement ->
                    !movement.myCompleted && movement.matchesExercise(exercise, dbExerciseId)
                }
                .map { it.id }
            if (movementIds.isNotEmpty()) {
                val completedCount = challengeRepository.completeMultipleMovements(summary.id, movementIds)
                    .getOrNull()
                    ?: 0
                completedAny = completedAny || completedCount > 0
            }
        }

        if (completedAny) {
            runCatching { challengeRepository.refreshMyProgress() }
            updateState { state -> state.copy(challengeSyncVersion = state.challengeSyncVersion + 1) }
        }
    }

    private fun ChallengeMovement.matchesExercise(exercise: Exercise, dbExerciseId: String): Boolean {
        val ids = listOf(exercise.id, exercise.exerciseTableId, dbExerciseId)
            .filter { it.isNotBlank() }
            .toSet()
        if (exerciseId.isNotBlank() && exerciseId in ids) return true
        return exerciseName.normalizedExerciseMatchKey() == exercise.name.normalizedExerciseMatchKey()
    }

    private fun String.normalizedExerciseMatchKey(): String =
        lowercase()
            .normalizeWorkoutVmText()
            .filter { it.isLetterOrDigit() }

    private fun markExerciseCompletedOptimistically(dayIdx: Int, exerciseId: String) {
        updateState { state ->
            val updatedDays = state.dayStates.mapIndexed { idx, dayState ->
                if (idx == dayIdx) {
                    dayState.copy(completedIds = (dayState.completedIds + exerciseId).toImmutableSet())
                } else {
                    dayState
                }
            }
            state.copy(dayStates = updatedDays.toImmutableList())
        }
    }

    private fun markExerciseIncompleteOptimistically(dayIdx: Int, exerciseId: String) {
        updateState { state ->
            val updatedDays = state.dayStates.mapIndexed { idx, dayState ->
                if (idx == dayIdx) {
                    dayState.copy(completedIds = (dayState.completedIds - exerciseId).toImmutableSet())
                } else {
                    dayState
                }
            }
            state.copy(dayStates = updatedDays.toImmutableList())
        }
    }

    private fun markSetCompletionOptimistically(
        dbExerciseId: String,
        setIndex: Int,
        isDone: Boolean
    ) {
        updateState { state ->
            val current = state.setCompletions[dbExerciseId].orEmpty()
            val updated = if (isDone) current + setIndex else current - setIndex
            state.copy(
                setCompletions = if (updated.isEmpty()) {
                    state.setCompletions - dbExerciseId
                } else {
                    state.setCompletions + (dbExerciseId to updated)
                }
            )
        }
    }

    private fun markExerciseSetsCompletedOptimistically(dbExerciseId: String, totalSets: Int) {
        if (totalSets <= 0) return
        updateState { state ->
            state.copy(
                setCompletions = state.setCompletions + (dbExerciseId to (0 until totalSets).toSet())
            )
        }
    }

    private fun markExerciseSetCompletionsClearedOptimistically(dbExerciseId: String) {
        updateState { state ->
            state.copy(setCompletions = state.setCompletions - dbExerciseId)
        }
    }

    private suspend fun persistStrengthSetWeights(
        userId: String,
        exercise: Exercise,
        exerciseId: String,
        dbExerciseId: String,
        programDayId: String,
        currentState: WorkoutScreenState
    ) {
        val userWeights = currentState.setWeights[exerciseId].orEmpty()
        val stateLastSessionMap = currentState.lastSessionData[exerciseId].orEmpty()
        val persistedWeights = loadPersistedExerciseWeights(userId, dbExerciseId)
        val defaultWeight = defaultWeightFor(exercise, currentState.profileWeightKg)

        repeat(exercise.sets) { i ->
            val w = _draftInputs.value[draftInputKey(exerciseId, i)]
                ?.takeIf { it.hasWeight }
                ?.weight
                ?.toFloatOrNull()
                ?: userWeights[i]?.toFloatOrNull()
                ?: stateLastSessionMap[i]?.first
                ?: persistedWeights[i]
                ?: defaultWeight
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
    }

    private suspend fun loadPersistedExerciseWeights(
        userId: String,
        dbExerciseId: String
    ): Map<Int, Float> {
        val today = LocalDate.now().toString()
        val todayWeights = workoutRepository.getSetsForDate(userId, dbExerciseId, today)
            .getOrNull()
            .orEmpty()
            .filter { it.weightKg != null }
            .associate { it.setIndex to it.weightKg!! }
        if (todayWeights.isNotEmpty()) return todayWeights

        return workoutRepository.getLastSessionSets(userId, dbExerciseId)
            .getOrNull()
            .orEmpty()
            .filter { it.weightKg != null }
            .associate { it.setIndex to it.weightKg!! }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private fun buildDayStates(
        program: Program,
        completions: Map<String, Set<String>>,
        setCompletions: Map<String, Set<Int>>
    ): ImmutableList<WorkoutDayState> {
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
                            weightKg = pe.weightKg,
                            sportType = pe.sportType,
                            trackingMode = pe.trackingMode,
                            targetDurationSeconds = pe.targetDurationSeconds,
                            targetDistanceMeters = pe.targetDistanceMeters,
                            targetElevationMeters = pe.targetElevationMeters,
                            targetInclinePercent = pe.targetInclinePercent
                        )
                    }.toImmutableList()
                )

                // DB'deki completions exercises.id kullanır.
                // UI'daki exercise.id = program_exercises.id.
                // Mapping: exercises.id → program_exercises.id
                val dbExerciseIds = completions[day.id] ?: emptySet()
                val exerciseIdToPeId = day.exercises.associate { it.exerciseId to it.id }
                val loggedCompletedPeIds = dbExerciseIds.mapNotNull { exerciseIdToPeId[it] }.toSet()
                val setCompletedPeIds = workoutDay.exercises
                    .filter { exercise ->
                        val dbExerciseId = exercise.exerciseTableId.ifBlank { exercise.id }
                        val completedSets = setCompletions[dbExerciseId].orEmpty()
                        if (isActivityBased(exercise)) {
                            ACTIVITY_SET_INDEX in completedSets
                        } else {
                            exercise.sets > 0 && completedSets.size >= exercise.sets
                        }
                    }
                    .map { it.id }
                    .toSet()
                val completedPeIds = (loggedCompletedPeIds + setCompletedPeIds).toImmutableSet()

                WorkoutDayState(workoutDay, completedIds = completedPeIds)
            } else {
                WorkoutDayState(
                    WorkoutDay(
                        day = DAY_LABELS[weekdayIdx],
                        title = "DİNLENME",
                        isRestDay = true,
                        exercises = persistentListOf()
                    )
                )
            }
        }.toImmutableList()
    }

    private suspend fun checkDayCompletion(dayIdx: Int, userId: String) {
        val dayState = uiState.value.dayStates.getOrNull(dayIdx) ?: return
        if (dayState.day.isRestDay) return
        val allDone = dayState.day.exercises.isNotEmpty() &&
            dayState.day.exercises.all { it.id in dayState.completedIds }
        if (allDone) {
            workoutRepository.addXp(userId, 50)
            profileRepository.invalidateStatsCache()
        }
    }

    private fun refreshStatsAfterCompletion(userId: String) {
        viewModelScope.launch {
            workoutRepository.updateStreak(userId)
            profileRepository.invalidateStatsCache()
            runCatching { challengeRepository.refreshMyProgress() }
        }
    }

    private fun refreshStatsAfterRollback(userId: String) {
        viewModelScope.launch {
            workoutRepository.rollbackStreak(userId)
            profileRepository.invalidateStatsCache()
            runCatching { challengeRepository.refreshMyProgress() }
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

        val achievementsToUnlock = allAch
            .filter { it.key !in unlockedKeys }
            .mapNotNull { ach ->
                val value = toCheck[ach.category] ?: return@mapNotNull null
                ach.key.takeIf { value >= ach.threshold }
            }

        if (achievementsToUnlock.isNotEmpty()) {
            profileRepository.unlockAchievements(userId, achievementsToUnlock)
            profileRepository.invalidateStatsCache()
        }

        val newRank = computeRank(stats.xp)
        val profile = profileRepository.getProfile(userId).getOrNull()
        if (profile != null && profile.current_rank != newRank) {
            profileRepository.updateRank(userId, newRank)
        }
    }

    private fun defaultWeightFor(exercise: Exercise, profileWeightKg: Float): Float? {
        exercise.weightKg.takeIf { it > 0f }?.let { return it }
        return profileWeightKg
            .takeIf { it > 0f && exercise.category.equals("Bodyweight", ignoreCase = true) }
    }

    private fun prefillActivityInputs(exerciseId: String, sets: List<SetCompletionEntity>) {
        val activity = sets.firstOrNull {
            it.durationSeconds != null || it.distanceMeters != null ||
                it.elevationMeters != null || it.inclinePercent != null
        } ?: return
        updateState { state ->
            val duration = activity.durationSeconds
                ?.takeIf { it > 0 }
                ?.let { (it / 60f).trimmedString() }
            val distance = activity.distanceMeters
                ?.takeIf { it > 0f }
                ?.trimmedString()
            val elevation = activity.elevationMeters
                ?.takeIf { it > 0f }
                ?.trimmedString()
            val incline = activity.inclinePercent
                ?.takeIf { it > 0f }
                ?.trimmedString()
            val reps = activity.repsActual
                ?.takeIf { it > 1 }
                ?.toString()
            state.copy(
                activityDurations = duration
                    ?.let { state.activityDurations + (exerciseId to it) }
                    ?: state.activityDurations,
                activityDistances = distance
                    ?.let { state.activityDistances + (exerciseId to it) }
                    ?: state.activityDistances,
                activityElevations = elevation
                    ?.let { state.activityElevations + (exerciseId to it) }
                    ?: state.activityElevations,
                activityInclines = incline
                    ?.let { state.activityInclines + (exerciseId to it) }
                    ?: state.activityInclines,
                activityReps = reps
                    ?.let { state.activityReps + (exerciseId to it) }
                    ?: state.activityReps
            )
        }
    }

    private fun prefillSetDurations(exerciseId: String, sets: List<SetCompletionEntity>) {
        val durations = sets
            .filter { it.durationSeconds != null && it.durationSeconds > 0 }
            .associate { it.setIndex to it.durationSeconds!!.toString() }
        if (durations.isEmpty()) return
        updateState { state ->
            val current = state.setDurations[exerciseId].orEmpty()
            state.copy(setDurations = state.setDurations + (exerciseId to (durations + current)))
        }
    }

    fun toggleChallengeActivity(dayIdx: Int, exercise: Exercise) {
        viewModelScope.launch {
            val currentState = uiState.value
            val dayState = currentState.dayStates.getOrNull(dayIdx) ?: return@launch
            val programDayId = dayState.day.programDayId
            if (programDayId.isBlank()) return@launch
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return@launch
            val isCompleting = exercise.id !in dayState.completedIds
            val dbExerciseId = exercise.exerciseTableId.ifBlank { return@launch }

            if (isCompleting) {
                val durationSeconds = currentState.activityDurations[exercise.id]?.toDurationSecondsOrNull()
                val distanceMeters = currentState.activityDistances[exercise.id]?.toFloatInputOrNull()
                val elevationMeters = currentState.activityElevations[exercise.id]?.toFloatInputOrNull()
                val inclinePercent = currentState.activityInclines[exercise.id]?.toFloatInputOrNull()
                val activityRepsActual = activityRepsActualFor(exercise, currentState, exercise.id)

                markExerciseCompletedOptimistically(dayIdx, exercise.id)
                markSetCompletionOptimistically(dbExerciseId, ACTIVITY_SET_INDEX, true)
                workoutRepository.upsertSetActivityMetrics(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId,
                    setIndex = ACTIVITY_SET_INDEX,
                    durationSeconds = durationSeconds,
                    distanceMeters = distanceMeters,
                    elevationMeters = elevationMeters,
                    inclinePercent = inclinePercent
                )
                workoutRepository.upsertSetRepsActual(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId,
                    setIndex = ACTIVITY_SET_INDEX,
                    repsActual = activityRepsActual
                )
                workoutRepository.completeExercise(
                    userId = userId,
                    programDayId = programDayId,
                    exerciseId = dbExerciseId,
                    setsCompleted = 1,
                    repsCompleted = if (supportsActivityReps(exercise)) activityRepsActual else 0,
                    durationSeconds = durationSeconds ?: 0
                )
                refreshStatsAfterCompletion(userId)
            } else {
                markExerciseIncompleteOptimistically(dayIdx, exercise.id)
                markExerciseSetCompletionsClearedOptimistically(dbExerciseId)
                workoutRepository.uncompleteExercise(userId, programDayId, dbExerciseId)
                workoutRepository.clearExerciseSetCompletions(userId, dbExerciseId, programDayId)
                refreshStatsAfterRollback(userId)
            }
        }
    }

    private fun saveActivityDuration(exerciseId: String, seconds: Int) {
        if (exerciseId.isBlank() || seconds <= 0) return
        val minutes = (seconds / 60f).trimmedString()
        updateState { state ->
            state.copy(activityDurations = state.activityDurations + (exerciseId to minutes))
        }
        autoCompleteActivityExercise(exerciseId, seconds)
    }

    private fun saveTimedSetDuration(exerciseId: String, setIndex: Int, seconds: Int) {
        if (exerciseId.isBlank() || setIndex < 0 || seconds <= 0) return
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val currentState = uiState.value
        val dayIdx = currentState.selectedDayIdx
        val dayState = currentState.dayStates.getOrNull(dayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }
        val completedAfter = (currentState.setCompletions[dbExerciseId] ?: emptySet()) + setIndex

        updateState { state ->
            val exerciseMap = state.setDurations[exerciseId].orEmpty().toMutableMap()
            exerciseMap[setIndex] = seconds.toString()
            state.copy(setDurations = state.setDurations + (exerciseId to exerciseMap))
        }
        markSetCompletionOptimistically(dbExerciseId, setIndex, true)

        viewModelScope.launch {
            workoutRepository.upsertSetActivityMetrics(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = setIndex,
                durationSeconds = seconds,
                distanceMeters = null
            )
            workoutRepository.upsertSetRepsActual(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = setIndex,
                repsActual = 1
            )

            val shouldAutoComplete =
                !isActivityBased(exercise) &&
                exercise.sets > 0 &&
                completedAfter.size >= exercise.sets &&
                exerciseId !in dayState.completedIds

            if (shouldAutoComplete) {
                autoCompleteStrengthExercise(
                    userId = userId,
                    dayIdx = dayIdx,
                    exercise = exercise,
                    programDayId = programDayId,
                    exerciseId = exerciseId,
                    dbExerciseId = dbExerciseId,
                    currentState = currentState
                )
            }
        }
    }

    private fun isActivityBased(exercise: Exercise): Boolean {
        return classifyExerciseMetric(
            category = exercise.category,
            name = exercise.name,
            target = exercise.target,
            reps = exercise.reps,
            sportTypeRaw = exercise.sportType,
            trackingModeRaw = exercise.trackingMode
        ) != ExerciseMetric.Strength && !isDurationSetBased(exercise)
    }

    private fun supportsActivityReps(exercise: Exercise): Boolean =
        isActivityBased(exercise) && activityTrackingSpec(
            category = exercise.category,
            name = exercise.name,
            target = exercise.target,
            reps = exercise.reps,
            sportTypeRaw = exercise.sportType,
            trackingModeRaw = exercise.trackingMode
        ).supportsReps

    private fun activityRepsActualFor(exercise: Exercise, state: WorkoutScreenState, exerciseId: String): Int =
        if (supportsActivityReps(exercise)) {
            state.activityReps[exerciseId]
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: exercise.reps.toIntOrNull()?.takeIf { it > 0 }
                ?: 1
        } else {
            1
        }

    private fun isDurationSetBased(exercise: Exercise): Boolean {
        return isDurationSetExercise(
            category = exercise.category,
            name = exercise.name,
            target = exercise.target,
            reps = exercise.reps,
            sets = exercise.sets,
            sportTypeRaw = exercise.sportType,
            trackingModeRaw = exercise.trackingMode
        )
    }

    private fun Exercise.plannedDurationSeconds(): Int? {
        targetDurationSeconds?.let { return it }
        val value = reps.lowercase().normalizeWorkoutVmText()
        return when {
            value.contains("dk") || value.contains("min") || value.contains("dakika") ->
                value.filter { it.isDigit() || it == ',' || it == '.' }
                    .replace(',', '.')
                    .toFloatOrNull()
                    ?.let { (it * 60f).toInt().coerceAtLeast(1) }
            else -> value.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1)
        }
    }

    private fun String.normalizeWorkoutVmText(): String =
        replace('\u0131', 'i')
            .replace('\u011f', 'g')
            .replace('\u00fc', 'u')
            .replace('\u015f', 's')
            .replace('\u00f6', 'o')
            .replace('\u00e7', 'c')

    private fun String.toFloatInputOrNull(): Float? =
        replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }

    private fun String.toDurationSecondsOrNull(): Int? =
        toFloatInputOrNull()?.let { (it * 60f).toInt().coerceAtLeast(1) }

    private fun Float.trimmedString(): String =
        if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

    private fun loadProfileWeight(userId: String) {
        viewModelScope.launch {
            profileRepository.getProfile(userId).onSuccess { profile ->
                updateState { it.copy(profileWeightKg = profile.weight_kg?.toFloat() ?: 0f) }
            }
        }
    }

    private fun persistActivityMetricsNow(exerciseId: String, durationSeconds: Int? = null) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val state = uiState.value
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }

        viewModelScope.launch {
            val latest = uiState.value
            workoutRepository.upsertSetActivityMetrics(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = ACTIVITY_SET_INDEX,
                durationSeconds = durationSeconds
                    ?: latest.activityDurations[exerciseId]?.toDurationSecondsOrNull(),
                distanceMeters = latest.activityDistances[exerciseId]?.toFloatInputOrNull(),
                elevationMeters = latest.activityElevations[exerciseId]?.toFloatInputOrNull(),
                inclinePercent = latest.activityInclines[exerciseId]?.toFloatInputOrNull()
            )
            if (
                supportsActivityReps(exercise) &&
                ACTIVITY_SET_INDEX in latest.setCompletions[dbExerciseId].orEmpty()
            ) {
                workoutRepository.upsertSetRepsActual(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId,
                    setIndex = ACTIVITY_SET_INDEX,
                    repsActual = activityRepsActualFor(exercise, latest, exerciseId)
                )
            }
        }
    }

    private fun scheduleActivityDraftPersist(exerciseId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val state = uiState.value
        val dayState = state.dayStates.getOrNull(state.selectedDayIdx) ?: return
        val programDayId = dayState.day.programDayId
        if (programDayId.isBlank()) return
        val exercise = dayState.day.exercises.find { it.id == exerciseId } ?: return
        val dbExerciseId = exercise.exerciseTableId.ifBlank { exerciseId }

        val key = "$exerciseId:activity"
        draftPersistJobs[key]?.cancel()
        draftPersistJobs[key] = viewModelScope.launch {
            delay(500L)
            val latest = uiState.value
            workoutRepository.upsertSetActivityMetrics(
                userId = userId,
                exerciseId = dbExerciseId,
                programDayId = programDayId,
                setIndex = ACTIVITY_SET_INDEX,
                durationSeconds = latest.activityDurations[exerciseId]?.toDurationSecondsOrNull(),
                distanceMeters = latest.activityDistances[exerciseId]?.toFloatInputOrNull(),
                elevationMeters = latest.activityElevations[exerciseId]?.toFloatInputOrNull(),
                inclinePercent = latest.activityInclines[exerciseId]?.toFloatInputOrNull()
            )
            if (
                supportsActivityReps(exercise) &&
                ACTIVITY_SET_INDEX in latest.setCompletions[dbExerciseId].orEmpty()
            ) {
                workoutRepository.upsertSetRepsActual(
                    userId = userId,
                    exerciseId = dbExerciseId,
                    programDayId = programDayId,
                    setIndex = ACTIVITY_SET_INDEX,
                    repsActual = activityRepsActualFor(exercise, latest, exerciseId)
                )
            }
        }
    }

    companion object {
        private const val ACTIVITY_SET_INDEX = 0
        private val DAY_LABELS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        private const val RESUME_SYNC_DEBOUNCE_MS = 1_500L
        private const val RESUME_SYNC_TTL_MS = 10 * 60_000L

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
