package com.avonix.profitness.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.CreateEventChallengeRequest
import com.avonix.profitness.domain.challenges.EventMode
import com.avonix.profitness.domain.model.ExerciseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChallengesScope { Browse, Mine }

data class ChallengesUiState(
    val scope          : ChallengesScope      = ChallengesScope.Browse,
    val browseList     : List<ChallengeSummary> = emptyList(),
    val myList         : List<ChallengeSummary> = emptyList(),
    val isLoading      : Boolean               = true,
    val isRefreshing   : Boolean               = false,
    val error          : String?               = null,
    val openDetailId   : String?               = null,
    val showCreateSheet: Boolean               = false,
    val createInFlight : Boolean               = false,
    val createError    : String?               = null,
    val joinInFlight   : Set<String>           = emptySet(),
    /** Event mode'da hareket seçici için yüklenen tüm hareketler (lazy). */
    val exercises      : List<ExerciseItem>    = emptyList(),
    val exercisesLoading: Boolean              = false
)

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val repo        : ChallengeRepository,
    private val programRepo : ProgramRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengesUiState())
    val state: StateFlow<ChallengesUiState> = _state.asStateFlow()

    init { loadAll() }

    fun selectScope(scope: ChallengesScope) {
        _state.update { it.copy(scope = scope) }
    }

    fun refresh() = loadAll(isRefresh = true)

    private fun loadAll(isRefresh: Boolean = false) {
        _state.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }
        viewModelScope.launch {
            val browseDef = async { repo.listPublicChallenges(limit = 50, offset = 0) }
            val myDef     = async { repo.listMyChallenges() }

            val browseRes = browseDef.await()
            val myRes     = myDef.await()

            val err = browseRes.exceptionOrNull()?.message ?: myRes.exceptionOrNull()?.message

            _state.update {
                it.copy(
                    browseList   = browseRes.getOrNull().orEmpty(),
                    myList       = myRes.getOrNull().orEmpty(),
                    isLoading    = false,
                    isRefreshing = false,
                    error        = err
                )
            }
        }
    }

    // ── Detail overlay ───────────────────────────────────────────────────────

    fun openDetail(id: String)  { _state.update { it.copy(openDetailId = id) } }
    fun closeDetail()            { _state.update { it.copy(openDetailId = null) } }

    // ── Create sheet ─────────────────────────────────────────────────────────

    fun openCreate() {
        _state.update { it.copy(showCreateSheet = true, createError = null) }
        // Lazy load exercises the first time create overlay opens.
        if (_state.value.exercises.isEmpty() && !_state.value.exercisesLoading) {
            _state.update { it.copy(exercisesLoading = true) }
            viewModelScope.launch {
                val res = programRepo.getAllExercises()
                _state.update {
                    it.copy(
                        exercises       = res.getOrNull().orEmpty(),
                        exercisesLoading = false
                    )
                }
            }
        }
    }

    fun closeCreate() { _state.update { it.copy(showCreateSheet = false, createError = null) } }

    fun submitCreate(
        title: String,
        description: String,
        targetType: ChallengeTargetType,
        targetValue: Long,
        startDateIso: String,
        endDateIso: String,
        visibility: ChallengeVisibility,
        password: String?
    ) {
        if (title.isBlank()) {
            _state.update { it.copy(createError = "Başlık boş olamaz") }
            return
        }
        if (targetValue <= 0) {
            _state.update { it.copy(createError = "Hedef sıfırdan büyük olmalı") }
            return
        }
        _state.update { it.copy(createInFlight = true, createError = null) }
        viewModelScope.launch {
            val res = repo.createChallenge(
                title        = title,
                description  = description,
                targetType   = targetType,
                targetValue  = targetValue,
                startDateIso = startDateIso,
                endDateIso   = endDateIso,
                visibility   = visibility,
                password     = password
            )
            res.fold(
                onSuccess = {
                    _state.update {
                        it.copy(createInFlight = false, showCreateSheet = false, createError = null)
                    }
                    loadAll(isRefresh = true)
                },
                onFailure = { t ->
                    _state.update {
                        it.copy(
                            createInFlight = false,
                            createError    = t.message ?: "Challenge oluşturulamadı"
                        )
                    }
                }
            )
        }
    }

    fun submitCreateEvent(req: CreateEventChallengeRequest) {
        if (req.title.isBlank()) {
            _state.update { it.copy(createError = "Başlık boş olamaz") }
            return
        }
        if (req.mode == EventMode.Physical && req.location.isNullOrBlank()) {
            _state.update { it.copy(createError = "Fiziksel etkinlik için başlangıç konumu gerekli") }
            return
        }
        if (req.mode == EventMode.Online && req.onlineUrl.isNullOrBlank()) {
            _state.update { it.copy(createError = "Online etkinlik için bağlantı gerekli") }
            return
        }
        if (req.mode == EventMode.MovementList && req.movements.isEmpty()) {
            _state.update { it.copy(createError = "Hareket listesi için en az bir hareket seç") }
            return
        }
        if (req.targetType != null && (req.targetValue ?: 0L) <= 0L) {
            _state.update { it.copy(createError = "Hedef sıfırdan büyük olmalı") }
            return
        }
        _state.update { it.copy(createInFlight = true, createError = null) }
        viewModelScope.launch {
            val res = repo.createEventChallenge(req)
            res.fold(
                onSuccess = {
                    _state.update {
                        it.copy(createInFlight = false, showCreateSheet = false, createError = null)
                    }
                    loadAll(isRefresh = true)
                },
                onFailure = { t ->
                    _state.update {
                        it.copy(
                            createInFlight = false,
                            createError    = t.message ?: "Etkinlik oluşturulamadı"
                        )
                    }
                }
            )
        }
    }

    // ── Join / Leave (optimistic) ────────────────────────────────────────────

    fun toggleJoin(challenge: ChallengeSummary, password: String? = null) {
        val id = challenge.id
        if (_state.value.joinInFlight.contains(id)) return

        val wasJoined = challenge.isJoined
        markInFlight(id, true)

        // Optimistic local flip
        _state.update { s ->
            s.copy(
                browseList = s.browseList.map { if (it.id == id) it.copy(isJoined = !wasJoined) else it },
                myList     = if (wasJoined) s.myList.filterNot { it.id == id } else s.myList
            )
        }

        viewModelScope.launch {
            val res = if (wasJoined) repo.leaveChallenge(id) else repo.joinChallenge(id, password)
            res.fold(
                onSuccess = { loadAll(isRefresh = true) },
                onFailure = { t ->
                    // Rollback
                    _state.update { s ->
                        s.copy(
                            browseList = s.browseList.map { if (it.id == id) it.copy(isJoined = wasJoined) else it },
                            error      = t.message
                        )
                    }
                }
            )
            markInFlight(id, false)
        }
    }

    private fun markInFlight(id: String, inFlight: Boolean) {
        _state.update {
            it.copy(
                joinInFlight = if (inFlight) it.joinInFlight + id else it.joinInFlight - id
            )
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
}
