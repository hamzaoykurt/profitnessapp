package com.avonix.profitness.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.challenges.ChallengePrefsRepository
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.domain.challenges.ChallengeDetail
import com.avonix.profitness.domain.challenges.ChallengeKind
import com.avonix.profitness.domain.challenges.UpdateEventChallengeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengeDetailState(
    val isLoading     : Boolean          = true,
    val detail        : ChallengeDetail? = null,
    val error         : String?          = null,
    val inFlight      : Boolean          = false,
    /** Movement ids currently mid-flight to server. */
    val pendingMovementIds: Set<String>  = emptySet(),
    /** Skip-program toggle for today (only relevant if event is today). */
    val skipProgramToday: Boolean        = false,
    /** Manual progress kaydı in-flight. */
    val submittingProgress: Boolean      = false,
    /** Sahip silme/güncelleme in-flight. */
    val ownerActionInFlight: Boolean     = false,
    /** Silme tamamlandı → UI overlay'i kapatsın. */
    val deleted          : Boolean       = false,
    /** Mevcut auth user id — isOwner kontrolü için. */
    val currentUserId    : String?       = null
) {
    val isOwner: Boolean
        get() {
            val uid = currentUserId ?: return false
            val sum = detail?.summary ?: return false
            return sum.creatorId == uid
        }
}

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    private val repo: ChallengeRepository,
    private val prefs: ChallengePrefsRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengeDetailState())
    val state: StateFlow<ChallengeDetailState> = _state.asStateFlow()

    private var loadedId: String? = null

    fun load(challengeId: String, force: Boolean = false) {
        if (!force && loadedId == challengeId && _state.value.detail != null) return
        loadedId = challengeId
        val uid = supabase.auth.currentSessionOrNull()?.user?.id
        _state.update { it.copy(isLoading = true, error = null, currentUserId = uid) }
        viewModelScope.launch {
            repo.getChallengeDetail(challengeId).fold(
                onSuccess = { d ->
                    _state.update { it.copy(isLoading = false, detail = d, error = null) }
                    // Sync skip-program flag if event is today.
                    if (d.summary.kind == ChallengeKind.Event) {
                        val evDate = d.summary.event?.dateIso
                        if (evDate != null && evDate == java.time.LocalDate.now().toString()) {
                            val s = prefs.isSkipped(evDate, d.summary.id)
                            _state.update { it.copy(skipProgramToday = s) }
                        }
                    }
                },
                onFailure = { t -> _state.update { it.copy(isLoading = false, error = t.message) } }
            )
        }
    }

    fun join(password: String? = null) {
        val id = loadedId ?: return
        _state.update { it.copy(inFlight = true, error = null) }
        viewModelScope.launch {
            repo.joinChallenge(id, password).fold(
                onSuccess = { load(id, force = true); _state.update { it.copy(inFlight = false) } },
                onFailure = { t -> _state.update { it.copy(inFlight = false, error = t.message) } }
            )
        }
    }

    fun leave() {
        val id = loadedId ?: return
        _state.update { it.copy(inFlight = true, error = null) }
        viewModelScope.launch {
            repo.leaveChallenge(id).fold(
                onSuccess = { load(id, force = true); _state.update { it.copy(inFlight = false) } },
                onFailure = { t -> _state.update { it.copy(inFlight = false, error = t.message) } }
            )
        }
    }

    /** Optimistically flip movement completion, then reconcile with server. */
    fun toggleMovement(movementId: String) {
        val id = loadedId ?: return
        val cur = _state.value.detail ?: return
        if (movementId in _state.value.pendingMovementIds) return

        val movement = cur.movements.firstOrNull { it.id == movementId } ?: return
        val nowCompleted = !movement.myCompleted

        // Optimistic flip + track in-flight.
        _state.update { s ->
            val newMovements = s.detail?.movements?.map {
                if (it.id == movementId) it.copy(myCompleted = nowCompleted) else it
            } ?: emptyList()
            val newSummary = s.detail?.summary?.let { sum ->
                val ev = sum.event
                val delta = if (nowCompleted) 1 else -1
                val newCount = ((ev?.myCompletedCount ?: 0) + delta).coerceAtLeast(0)
                val newEvent = ev?.copy(myCompletedCount = newCount)
                sum.copy(
                    event = newEvent,
                    myProgress = (sum.myProgress + delta).coerceAtLeast(0)
                )
            }
            s.copy(
                detail = s.detail?.copy(
                    movements = newMovements,
                    summary = newSummary ?: s.detail.summary
                ),
                pendingMovementIds = s.pendingMovementIds + movementId
            )
        }

        viewModelScope.launch {
            val res = if (nowCompleted) repo.completeMovement(id, movementId)
                      else repo.uncompleteMovement(id, movementId)
            res.fold(
                onSuccess = {
                    // Reconcile — pull fresh detail (progress recomputed server-side).
                    load(id, force = true)
                },
                onFailure = { t ->
                    // Rollback.
                    _state.update { s ->
                        val rolled = s.detail?.movements?.map {
                            if (it.id == movementId) it.copy(myCompleted = !nowCompleted) else it
                        } ?: emptyList()
                        val rolledSummary = s.detail?.summary?.let { sum ->
                            val ev = sum.event
                            val delta = if (nowCompleted) -1 else 1
                            val newCount = ((ev?.myCompletedCount ?: 0) + delta).coerceAtLeast(0)
                            sum.copy(
                                event = ev?.copy(myCompletedCount = newCount),
                                myProgress = (sum.myProgress + delta).coerceAtLeast(0)
                            )
                        }
                        s.copy(
                            detail = s.detail?.copy(
                                movements = rolled,
                                summary = rolledSummary ?: s.detail.summary
                            ),
                            error = t.message
                        )
                    }
                }
            )
            _state.update { it.copy(pendingMovementIds = it.pendingMovementIds - movementId) }
        }
    }

    fun setSkipProgramToday(enabled: Boolean) {
        val d = _state.value.detail ?: return
        val dateIso = d.summary.event?.dateIso ?: return
        if (dateIso != java.time.LocalDate.now().toString()) return
        _state.update { it.copy(skipProgramToday = enabled) }
        viewModelScope.launch {
            prefs.setSkipped(dateIso, d.summary.id, enabled)
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    /** Physical/Online event'lerde manuel ilerleme ekler. */
    fun addManualProgress(amount: Long) {
        val id = loadedId ?: return
        if (amount <= 0L) return
        if (_state.value.submittingProgress) return
        _state.update { it.copy(submittingProgress = true, error = null) }
        viewModelScope.launch {
            repo.addManualProgress(id, amount).fold(
                onSuccess = {
                    load(id, force = true)
                    _state.update { it.copy(submittingProgress = false) }
                },
                onFailure = { t ->
                    _state.update { it.copy(submittingProgress = false, error = t.message) }
                }
            )
        }
    }

    /** Sahip → event challenge'ı siler. UI overlay'i kapatması için `deleted=true` set eder. */
    fun deleteChallenge() {
        val id = loadedId ?: return
        if (_state.value.ownerActionInFlight) return
        _state.update { it.copy(ownerActionInFlight = true, error = null) }
        viewModelScope.launch {
            repo.deleteEventChallenge(id).fold(
                onSuccess = {
                    _state.update { it.copy(ownerActionInFlight = false, deleted = true) }
                },
                onFailure = { t ->
                    _state.update { it.copy(ownerActionInFlight = false, error = t.message) }
                }
            )
        }
    }

    /** Sahip → event challenge alanlarını günceller. */
    fun updateChallenge(req: UpdateEventChallengeRequest) {
        val id = loadedId ?: return
        if (_state.value.ownerActionInFlight) return
        _state.update { it.copy(ownerActionInFlight = true, error = null) }
        viewModelScope.launch {
            repo.updateEventChallenge(req).fold(
                onSuccess = {
                    load(id, force = true)
                    _state.update { it.copy(ownerActionInFlight = false) }
                },
                onFailure = { t ->
                    _state.update { it.copy(ownerActionInFlight = false, error = t.message) }
                }
            )
        }
    }
}
