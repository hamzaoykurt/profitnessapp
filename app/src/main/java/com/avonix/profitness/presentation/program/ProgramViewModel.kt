package com.avonix.profitness.presentation.program

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.program.ManualDayInput
import com.avonix.profitness.data.program.ManualExerciseInput
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userPrograms: List<Program> = emptyList(),
    val exercises: List<ExerciseItem> = emptyList()
)

sealed class ProgramEvent {
    data class ShowSnackbar(val message: String) : ProgramEvent()
    object NavigateBack : ProgramEvent()
}

// ── Geçici manuel gün durumu ──────────────────────────────────────────────────

data class ManualDayDraft(
    val title: String = "",
    val isRestDay: Boolean = false,
    val selectedExercises: List<ManualExerciseInput> = emptyList()
)

@HiltViewModel
class ProgramViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<ProgramUiState, ProgramEvent>(ProgramUiState()) {

    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    init {
        loadUserPrograms()
        loadExercises()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadUserPrograms() {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            programRepository.getUserPrograms(uid)
                .onSuccess { programs ->
                    updateState { it.copy(isLoading = false, userPrograms = programs) }
                }
                .onFailure {
                    updateState { it.copy(isLoading = false) }
                }
        }
    }

    fun loadExercises() {
        viewModelScope.launch {
            programRepository.getAllExercises()
                .onSuccess { list ->
                    updateState { it.copy(exercises = list) }
                }
        }
    }

    // ── Create From Template ──────────────────────────────────────────────────

    fun selectTemplate(templateKey: String) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            programRepository.createFromTemplate(uid, templateKey)
                .onSuccess { program ->
                    updateState { state ->
                        val updated = state.userPrograms
                            .map { it.copy(isActive = false) }
                            .toMutableList()
                            .also { it.add(0, program) }
                        state.copy(isLoading = false, userPrograms = updated)
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"${program.name}\" programı oluşturuldu ve aktif edildi."))
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = err.message) }
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: Program oluşturulamadı."))
                }
        }
    }

    // ── Create Manual ─────────────────────────────────────────────────────────

    fun createManualProgram(name: String, days: List<ManualDayDraft>) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        if (name.isBlank()) {
            sendEvent(ProgramEvent.ShowSnackbar("Program adı boş olamaz."))
            return
        }
        if (days.isEmpty()) {
            sendEvent(ProgramEvent.ShowSnackbar("En az 1 gün eklemelisiniz."))
            return
        }
        val inputs = days.mapIndexed { i, d ->
            ManualDayInput(
                title = d.title.ifBlank { "GÜN ${i + 1}" },
                isRestDay = d.isRestDay,
                exercises = d.selectedExercises
            )
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            programRepository.createManual(uid, name, inputs)
                .onSuccess { program ->
                    updateState { state ->
                        val updated = state.userPrograms
                            .map { it.copy(isActive = false) }
                            .toMutableList()
                            .also { it.add(0, program) }
                        state.copy(isLoading = false, userPrograms = updated)
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"${program.name}\" oluşturuldu."))
                    sendEvent(ProgramEvent.NavigateBack)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = err.message) }
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: Program kaydedilemedi."))
                }
        }
    }

    // ── Set Active ────────────────────────────────────────────────────────────

    fun setActive(programId: String) {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            programRepository.setActive(programId, uid)
                .onSuccess {
                    updateState { state ->
                        state.copy(
                            userPrograms = state.userPrograms.map {
                                it.copy(isActive = it.id == programId)
                            }
                        )
                    }
                }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteProgram(programId: String) {
        viewModelScope.launch {
            programRepository.deleteProgram(programId)
                .onSuccess {
                    updateState { state ->
                        state.copy(userPrograms = state.userPrograms.filter { it.id != programId })
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("Program silindi."))
                }
        }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}
