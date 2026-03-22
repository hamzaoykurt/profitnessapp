package com.avonix.profitness.presentation.profile

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class ProfileState(
    val displayName         : String       = "",
    val avatar              : String       = "🏋️",
    val rank                : String       = "Bronze",
    val level               : Int          = 1,
    val xp                  : Int          = 0,
    val xpPerLevel          : Int          = 1000,
    val currentStreak       : Int          = 0,
    val longestStreak       : Int          = 0,
    val totalWorkouts       : Int          = 0,
    val totalDurationSeconds: Int          = 0,
    /** 7 eleman — Pazartesi=0 … Pazar=6; true = o gün antrenman yapıldı */
    val weeklyActivity      : List<Boolean> = List(7) { false },
    val isLoading           : Boolean      = true,
    val isSaving            : Boolean      = false
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed class ProfileEvent {
    data class ShowSnackbar(val message: String) : ProfileEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabase         : SupabaseClient
) : BaseViewModel<ProfileState, ProfileEvent>(ProfileState()) {

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            val profile = profileRepository.getProfile(userId).getOrNull()
            val stats   = profileRepository.getUserStats(userId).getOrNull()

            // Bu haftanın Pazartesi'sinden itibaren aktif günler
            val monday    = LocalDate.now().with(DayOfWeek.MONDAY)
            val weekStart = monday.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val activeDates = profileRepository
                .getWeeklyActivity(userId, weekStart)
                .getOrNull()
                .orEmpty()
                .toSet()

            val todayIdx = LocalDate.now().dayOfWeek.value - 1   // 0=Pzt … 6=Paz
            val weeklyActivity = (0..6).map { i ->
                val date = monday.plusDays(i.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                i <= todayIdx && date in activeDates
            }

            val lvl       = stats?.level ?: 1
            val xpForNext = lvl * 1000          // her seviye 1000 * seviye XP gerektiriyor

            updateState {
                it.copy(
                    displayName          = profile?.display_name.orEmpty(),
                    avatar               = profile?.avatar_url?.ifBlank { "🏋️" } ?: "🏋️",
                    rank                 = profile?.current_rank ?: "Bronze",
                    level                = lvl,
                    xp                   = stats?.xp ?: 0,
                    xpPerLevel           = xpForNext,
                    currentStreak        = stats?.current_streak ?: 0,
                    longestStreak        = stats?.longest_streak ?: 0,
                    totalWorkouts        = stats?.total_workouts ?: 0,
                    totalDurationSeconds = stats?.total_duration_seconds ?: 0,
                    weeklyActivity       = weeklyActivity,
                    isLoading            = false
                )
            }
        }
    }

    /** Profil bilgilerini DB'ye kaydeder ve state'i günceller. */
    fun updateProfile(displayName: String, avatar: String, fitnessGoal: String) {
        viewModelScope.launch {
            updateState { it.copy(isSaving = true) }

            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                updateState { it.copy(isSaving = false) }
                return@launch
            }

            val result = profileRepository.updateProfile(userId, displayName, avatar, fitnessGoal)
            if (result.isSuccess) {
                updateState {
                    it.copy(
                        displayName = displayName,
                        avatar      = avatar,
                        isSaving    = false
                    )
                }
                sendEvent(ProfileEvent.ShowSnackbar("Profil güncellendi"))
            } else {
                updateState { it.copy(isSaving = false) }
                sendEvent(ProfileEvent.ShowSnackbar("Güncelleme başarısız"))
            }
        }
    }
}
