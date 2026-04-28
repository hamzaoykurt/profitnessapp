package com.avonix.profitness.presentation.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.discover.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramShareUiState(
    val isSharing: Boolean = false,
    val result: ProgramShareResult? = null
)

sealed class ProgramShareResult {
    data object Success : ProgramShareResult()
    data class Error(val message: String) : ProgramShareResult()
}

@HiltViewModel
class ProgramShareViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProgramShareUiState())
    val state: StateFlow<ProgramShareUiState> = _state.asStateFlow()

    fun shareProgram(
        programId: String,
        title: String,
        description: String?,
        tags: List<String>,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?
    ) {
        if (_state.value.isSharing) return
        viewModelScope.launch {
            _state.update { it.copy(isSharing = true, result = null) }
            discoverRepository.shareMyProgram(
                originalProgramId = programId,
                title = title,
                description = description,
                tags = tags,
                difficulty = difficulty,
                durationWeeks = durationWeeks,
                daysPerWeek = daysPerWeek
            )
                .onSuccess {
                    _state.update { it.copy(isSharing = false, result = ProgramShareResult.Success) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSharing = false,
                            result = ProgramShareResult.Error(err.message ?: "Paylaşım başarısız")
                        )
                    }
                }
        }
    }

    fun consumeResult() {
        _state.update { it.copy(result = null) }
    }
}
