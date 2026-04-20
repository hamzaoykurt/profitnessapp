package com.avonix.profitness.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.social.SocialRepository
import com.avonix.profitness.domain.social.FollowListKind
import com.avonix.profitness.domain.social.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Arkadaşlar ekranı tek state — arama sonucu + takip edilenler listesi + loading bayrakları. */
data class FriendsUiState(
    val query        : String             = "",
    val isSearching  : Boolean            = false,
    val searchResults: List<UserSummary>  = emptyList(),
    val following    : List<UserSummary>  = emptyList(),
    val isFollowingLoading: Boolean       = false,
    val error        : String?            = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val socialRepo: SocialRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadFollowing()
        // Arama debounce — her tuş basışında RPC atmasın diye
        viewModelScope.launch {
            queryFlow
                .drop(1)
                .debounce(280)
                .distinctUntilChanged()
                .collect { q -> runSearch(q) }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
        if (q.isBlank()) _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
    }

    private fun runSearch(q: String) {
        if (q.isBlank()) return
        _state.update { it.copy(isSearching = true, error = null) }
        viewModelScope.launch {
            socialRepo.searchUsers(q, limit = 20)
                .onSuccess { res ->
                    // Yarış koşulu: cevap geldiğinde kullanıcı farklı yazmış olabilir; sadece aktif query eşleşirse uygula.
                    if (_state.value.query.trim() == q.trim()) {
                        _state.update { it.copy(searchResults = res, isSearching = false) }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSearching = false, error = e.message ?: "Arama başarısız") }
                }
        }
    }

    fun loadFollowing() {
        _state.update { it.copy(isFollowingLoading = true) }
        viewModelScope.launch {
            socialRepo.listMyFollows(FollowListKind.FOLLOWING, limit = 100)
                .onSuccess { list ->
                    _state.update { it.copy(following = list, isFollowingLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isFollowingLoading = false, error = e.message) }
                }
        }
    }

    /** Optimistic toggle — hem search list'te hem following list'te senkronize çalışır. */
    fun toggleFollow(target: UserSummary) {
        val willFollow = !target.isFollowing

        _state.update { s ->
            val newSearch = s.searchResults.map {
                if (it.userId == target.userId) it.copy(isFollowing = willFollow) else it
            }
            val newFollowing = if (willFollow) {
                // Takibe alındı → listeye ekle (en üste) — zaten varsa koru
                val already = s.following.any { it.userId == target.userId }
                if (already) s.following else listOf(target.copy(isFollowing = true)) + s.following
            } else {
                s.following.filter { it.userId != target.userId }
            }
            s.copy(searchResults = newSearch, following = newFollowing)
        }

        viewModelScope.launch {
            socialRepo.toggleFollow(target.userId).onFailure { e ->
                // rollback
                _state.update { s ->
                    val revertSearch = s.searchResults.map {
                        if (it.userId == target.userId) it.copy(isFollowing = target.isFollowing) else it
                    }
                    val revertFollowing = if (willFollow) {
                        s.following.filter { it.userId != target.userId }
                    } else {
                        if (s.following.any { it.userId == target.userId }) s.following
                        else listOf(target) + s.following
                    }
                    s.copy(
                        searchResults = revertSearch,
                        following     = revertFollowing,
                        error         = e.message ?: "Takip işlemi başarısız"
                    )
                }
            }
        }
    }

    fun consumeError() { _state.update { it.copy(error = null) } }
}
