package com.avonix.profitness.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.discover.DiscoverRepository
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.MySharedProgram
import com.avonix.profitness.domain.discover.SharedProgram
import com.avonix.profitness.domain.model.Program
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    val applyResult : ApplyResult?        = null,
    val applyingProgramIds: Set<String>   = emptySet(),
    val appliedProgramMap : Map<String, String> = emptyMap(),
    val localDeletingProgramIds: Set<String> = emptySet(),
    // Kullanıcının kendi paylaşımları
    val myShared         : List<MySharedProgram> = emptyList(),
    val myLoading        : Boolean               = false,
    val mySyncInFlight   : Set<String>           = emptySet(),
    val myDeleteInFlight : Set<String>           = emptySet(),
    val myActionMsg      : String?               = null
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

    /** Kullanıcının kendi programları — sheet'teki seçici ve paylaşımlarımda UYGULA fallback için. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val myPrograms: StateFlow<List<Program>> =
        flowOf(supabase.auth.currentUserOrNull()?.id)
            .flatMapLatest { uid ->
                if (uid == null) flowOf(emptyList())
                else programRepo.observeUserPrograms(uid)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object { private const val PAGE_SIZE = 20 }

    init {
        loadFirstPage()
        loadMyShared()
    }

    fun changeSort(sort: DiscoverSort) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        loadFirstPage()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        loadFirstPage(isRefresh = true)
        loadMyShared()
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

    /**
     * Kullanıcının seçtiği [programId] programını feed'e paylaşır.
     * [programId] null ise aktif programı paylaşır (geriye dönük uyum).
     */
    fun shareProgram(
        programId: String?,
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
            val originalId: String = if (programId != null) programId else {
                val active = programRepo.getActiveProgram(uid).getOrNull()
                if (active == null) {
                    _state.update { it.copy(shareResult = ShareResult.Error("Aktif program yok")) }
                    return@launch
                }
                active.id
            }
            discoverRepo.shareMyProgram(
                originalProgramId = originalId,
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
                    loadMyShared()
                }
                .onFailure { err ->
                    _state.update { it.copy(shareResult = ShareResult.Error(err.message ?: "Paylaşım başarısız")) }
                }
        }
    }

    fun applyProgram(sharedProgramId: String) {
        val currentState = _state.value
        if (sharedProgramId in currentState.applyingProgramIds) {
            return
        }
        _state.update {
            it.copy(
                applyingProgramIds = it.applyingProgramIds + sharedProgramId,
                applyResult = null
            )
        }
        viewModelScope.launch {
            discoverRepo.applyProgram(sharedProgramId)
                .onSuccess { localProgramId ->
                    // DB'de yeni program oluştu + aktif yapıldı → Room'u sync et, aksi halde
                    // Plan sekmesinde program görünmez, bu yüzden uygulama hissedilmez.
                    supabase.auth.currentUserOrNull()?.id?.let { uid ->
                        runCatching { programRepo.syncFromRemote(uid) }
                    }
                    _state.update {
                        it.copy(
                            applyResult = ApplyResult.Success,
                            applyingProgramIds = it.applyingProgramIds - sharedProgramId,
                            appliedProgramMap = it.appliedProgramMap + (sharedProgramId to localProgramId)
                        )
                    }
                    mutateLocal(sharedProgramId) { p -> p.copy(downloadsCount = p.downloadsCount + 1) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            applyResult = ApplyResult.Error(err.message ?: "Uygulanamadı"),
                            applyingProgramIds = it.applyingProgramIds - sharedProgramId
                        )
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Paylaşımlarım (My Shared Programs)
    // ═══════════════════════════════════════════════════════════════════════

    fun loadMyShared() {
        viewModelScope.launch {
            _state.update { it.copy(myLoading = true) }
            discoverRepo.listMyShared()
                .onSuccess { list ->
                    _state.update { it.copy(myShared = list, myLoading = false) }
                }
                .onFailure {
                    _state.update { it.copy(myLoading = false) }
                }
        }
    }

    /**
     * Paylaşım kaydının snapshot'ını kaynak programdan yeniden alır.
     * Kaynak silinmişse backend hata döner; kullanıcı silmeyi tercih eder.
     */
    fun syncShared(sharedId: String) {
        val s = _state.value
        if (sharedId in s.mySyncInFlight) return
        _state.update { it.copy(mySyncInFlight = it.mySyncInFlight + sharedId) }
        viewModelScope.launch {
            discoverRepo.updateShared(
                sharedId       = sharedId,
                resyncSnapshot = true
            )
                .onSuccess {
                    // Senkron sonrası: listeyi tazele + feed'de güncel kart görünsün
                    loadMyShared()
                    loadFirstPage(isRefresh = true)
                    _state.update {
                        it.copy(
                            mySyncInFlight = it.mySyncInFlight - sharedId,
                            myActionMsg    = "Paylaşım güncel programla senkron ✓"
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            mySyncInFlight = it.mySyncInFlight - sharedId,
                            myActionMsg    = "Senkron başarısız: ${err.message ?: "bilinmeyen hata"}"
                        )
                    }
                }
        }
    }

    fun deleteShared(sharedId: String) {
        val s = _state.value
        if (sharedId in s.myDeleteInFlight) return
        // Optimistic: listeden anında düş
        val snapshot = s.myShared
        _state.update {
            it.copy(
                myShared         = it.myShared.filter { m -> m.id != sharedId },
                myDeleteInFlight = it.myDeleteInFlight + sharedId
            )
        }
        viewModelScope.launch {
            discoverRepo.deleteShared(sharedId)
                .onSuccess {
                    // Feed'de de kalmasın
                    _state.update {
                        it.copy(
                            items            = it.items.filter { p -> p.id != sharedId },
                            myDeleteInFlight = it.myDeleteInFlight - sharedId,
                            myActionMsg      = "Paylaşım silindi"
                        )
                    }
                }
                .onFailure { err ->
                    // Rollback
                    _state.update {
                        it.copy(
                            myShared         = snapshot,
                            myDeleteInFlight = it.myDeleteInFlight - sharedId,
                            myActionMsg      = "Silme başarısız: ${err.message ?: "bilinmeyen hata"}"
                        )
                    }
                }
        }
    }

    fun consumeShareResult() { _state.update { it.copy(shareResult = null) } }
    fun consumeApplyResult() { _state.update { it.copy(applyResult = null) } }
    fun consumeMyActionMsg() { _state.update { it.copy(myActionMsg = null) } }
    fun consumeError()       { _state.update { it.copy(error = null) } }

    fun markLocalProgramDeleting(programId: String) {
        _state.update { it.copy(localDeletingProgramIds = it.localDeletingProgramIds + programId) }
    }

    fun unmarkLocalProgramDeleting(programId: String) {
        _state.update { it.copy(localDeletingProgramIds = it.localDeletingProgramIds - programId) }
    }

    fun confirmLocalProgramDeleted(programId: String) {
        _state.update { state ->
            state.copy(
                localDeletingProgramIds = state.localDeletingProgramIds - programId,
                appliedProgramMap = state.appliedProgramMap.filterValues { it != programId }
            )
        }
    }
}
