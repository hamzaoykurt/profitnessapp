package com.avonix.profitness.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.security.toUserSafeMessage
import com.avonix.profitness.data.challenges.ChallengePrefsRepository
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.domain.challenges.ChallengeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardEventsUiState(
    val isLoading     : Boolean = false,
    val today         : List<ChallengeSummary> = emptyList(),
    val upcoming      : List<ChallengeSummary> = emptyList(),
    val error         : String? = null,
    val skipProgramToday: Boolean = false
)

/**
 * FAZ 7J: Event challenges on the main dashboard (above daily program).
 *
 * Exposes:
 *   - today's joined events (banner)
 *   - upcoming events (next 7 days — section)
 *   - whether the user has toggled "skip program today" on any event today
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo  : ChallengeRepository,
    private val prefs : ChallengePrefsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardEventsUiState())
    val state: StateFlow<DashboardEventsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val todayDef    = async { repo.listMyEventsForDate(today) }
            val upcomingDef = async { repo.listMyUpcomingEvents(7) }

            val todayRes    = todayDef.await()
            val upcomingRes = upcomingDef.await()
            val err = todayRes.exceptionOrNull()?.toUserSafeMessage("Etkinlikler yüklenemedi.")
                ?: upcomingRes.exceptionOrNull()?.toUserSafeMessage("Etkinlikler yüklenemedi.")

            val todayList = todayRes.getOrNull().orEmpty()
            val skipFlag = prefs.isAnySkippedForDate(today)

            _state.update {
                it.copy(
                    isLoading        = false,
                    today            = todayList,
                    upcoming         = upcomingRes.getOrNull().orEmpty(),
                    error            = err,
                    skipProgramToday = skipFlag
                )
            }
        }
    }
}
