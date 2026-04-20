package com.avonix.profitness.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.social.SocialRepository
import com.avonix.profitness.domain.social.PublicProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileState(
    val profile : PublicProfile? = null,
    val isLoading: Boolean       = false,
    val error   : String?        = null
)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val socialRepo: SocialRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PublicProfileState())
    val state: StateFlow<PublicProfileState> = _state.asStateFlow()

    private var loadedUserId: String? = null

    fun load(userId: String) {
        if (userId == loadedUserId && _state.value.profile != null) return
        loadedUserId = userId
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            socialRepo.getPublicProfile(userId)
                .onSuccess { p -> _state.update { it.copy(profile = p, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Yüklenemedi") } }
        }
    }

    /** Optimistic follow toggle — takip sayısını lokal güncelle, hata olursa rollback. */
    fun toggleFollow() {
        val current = _state.value.profile ?: return
        val newFollowing = !current.isFollowing
        val delta = if (newFollowing) 1 else -1
        val optimistic = current.copy(
            isFollowing     = newFollowing,
            followersCount  = (current.followersCount + delta).coerceAtLeast(0)
        )
        _state.update { it.copy(profile = optimistic) }

        viewModelScope.launch {
            socialRepo.toggleFollow(current.userId).onFailure { e ->
                // rollback
                _state.update { it.copy(profile = current, error = e.message ?: "Takip işlemi başarısız") }
            }
        }
    }

    fun consumeError() { _state.update { it.copy(error = null) } }
}
