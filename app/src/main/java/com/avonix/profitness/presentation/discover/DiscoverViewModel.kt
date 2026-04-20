package com.avonix.profitness.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.discover.DiscoverRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.SharedProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Feed ekran state'i — sealed değil, tek state; empty/loading/error alt-bayraklarla. */
data class DiscoverProgramsState(
    val items       : List<SharedProgram> = emptyList(),
    val sort        : DiscoverSort        = DiscoverSort.NEWEST,
    val isLoading   : Boolean             = false,
    val isRefreshing: Boolean             = false,
    val canLoadMore : Boolean             = true,
    val error       : String?             = null,
    val shareResult : ShareResult?        = null,
    val applyResult : ApplyResult?        = null
)

sealed class ShareResult { object Success : ShareResult(); data class Error(val msg: String) : ShareResult() }
sealed class ApplyResult { object Success : ApplyResult(); data class Error(val msg: String) : ApplyResult() }

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepo: DiscoverRepository,
    private val programRepo : ProgramRepository,
    private val supabase    : SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverProgramsState())
    val state: StateFlow<DiscoverProgramsState> = _state.asStateFlow()

    companion object { private const val PAGE_SIZE = 20 }

    init { loadFirstPage() }

    fun changeSort(sort: DiscoverSort) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        loadFirstPage()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        loadFirstPage(isRefresh = true)
    }

    private fun loadFirstPage(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) _state.update { it.copy(isLoading = true, error = null) }
            discoverRepo.getFeed(_state.value.sort, PAGE_SIZE, 0)
                .onSuccess { items ->
                    _state.update { it.copy(
                        items        = items,
                        isLoading    = false,
                        isRefreshing = false,
                        canLoadMore  = items.size >= PAGE_SIZE,
                        error        = null
                    ) }
                }
                .onFailure { err ->
                    _state.update { it.copy(
                        isLoading    = false,
                        isRefreshing = false,
                        error        = err.message ?: "Feed yüklenemedi"
                    ) }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || !s.canLoadMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            discoverRepo.getFeed(s.sort, PAGE_SIZE, s.items.size)
                .onSuccess { more ->
                    _state.update { it.copy(
                        items       = it.items + more,
                        isLoading   = false,
                        canLoadMore = more.size >= PAGE_SIZE
                    ) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    /** Optimistic update: UI anında değişir, hata olursa rollback. */
    fun toggleLike(programId: String) {
        val before = _state.value.items.firstOrNull { it.id == programId } ?: return
        mutateLocal(programId) { item ->
            val newLiked = !item.isLikedByMe
            item.copy(
                isLikedByMe = newLiked,
                likesCount  = (item.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            discoverRepo.toggleLike(programId).onFailure {
                // rollback
                mutateLocal(programId) { before }
                _state.update { it.copy(error = "Beğeni başarısız") }
            }
        }
    }

    fun toggleSave(programId: String) {
        val before = _state.value.items.firstOrNull { it.id == programId } ?: return
        mutateLocal(programId) { item ->
            val newSaved = !item.isSavedByMe
            item.copy(
                isSavedByMe = newSaved,
                savesCount  = (item.savesCount + if (newSaved) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            discoverRepo.toggleSave(programId).onFailure {
                mutateLocal(programId) { before }
                _state.update { it.copy(error = "Kaydetme başarısız") }
            }
        }
    }

    private fun mutateLocal(programId: String, block: (SharedProgram) -> SharedProgram) {
        _state.update { s ->
            s.copy(items = s.items.map { if (it.id == programId) block(it) else it })
        }
    }

    /** Kullanıcının aktif programını feed'e paylaş. */
    fun shareMyActiveProgram(
        title: String,
        description: String?,
        tags: List<String>,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?
    ) {
        viewModelScope.launch {
            val uid = supabase.auth.currentUserOrNull()?.id
            if (uid == null) {
                _state.update { it.copy(shareResult = ShareResult.Error("Oturum yok")) }; return@launch
            }
            val activeResult = programRepo.getActiveProgram(uid)
            val active = activeResult.getOrNull()
            if (active == null) {
                _state.update { it.copy(shareResult = ShareResult.Error("Aktif program yok")) }
                return@launch
            }
            discoverRepo.shareMyProgram(
                originalProgramId = active.id,
                title             = title,
                description       = description,
                tags              = tags,
                difficulty        = difficulty,
                durationWeeks     = durationWeeks,
                daysPerWeek       = daysPerWeek
            )
                .onSuccess {
                    _state.update { it.copy(shareResult = ShareResult.Success) }
                    loadFirstPage(isRefresh = true)
                }
                .onFailure { err ->
                    _state.update { it.copy(shareResult = ShareResult.Error(err.message ?: "Paylaşım başarısız")) }
                }
        }
    }

    fun applyProgram(sharedProgramId: String) {
        viewModelScope.launch {
            discoverRepo.applyProgram(sharedProgramId)
                .onSuccess {
                    _state.update { it.copy(applyResult = ApplyResult.Success) }
                    // downloadsCount local update
                    mutateLocal(sharedProgramId) { p -> p.copy(downloadsCount = p.downloadsCount + 1) }
                }
                .onFailure { err ->
                    _state.update { it.copy(applyResult = ApplyResult.Error(err.message ?: "Uygulanamadı")) }
                }
        }
    }

    fun consumeShareResult() { _state.update { it.copy(shareResult = null) } }
    fun consumeApplyResult() { _state.update { it.copy(applyResult = null) } }
    fun consumeError()       { _state.update { it.copy(error = null) } }
}
