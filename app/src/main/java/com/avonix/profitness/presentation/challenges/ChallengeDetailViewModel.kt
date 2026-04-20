package com.avonix.profitness.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.domain.challenges.ChallengeDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengeDetailState(
    val isLoading  : Boolean          = true,
    val detail     : ChallengeDetail? = null,
    val error      : String?          = null,
    val inFlight   : Boolean          = false
)

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    private val repo: ChallengeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengeDetailState())
    val state: StateFlow<ChallengeDetailState> = _state.asStateFlow()

    private var loadedId: String? = null

    fun load(challengeId: String, force: Boolean = false) {
        if (!force && loadedId == challengeId && _state.value.detail != null) return
        loadedId = challengeId
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repo.getChallengeDetail(challengeId).fold(
                onSuccess = { d -> _state.update { it.copy(isLoading = false, detail = d, error = null) } },
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

    fun clearError() { _state.update { it.copy(error = null) } }
}
