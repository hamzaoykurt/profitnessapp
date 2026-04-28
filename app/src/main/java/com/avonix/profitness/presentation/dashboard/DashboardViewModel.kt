package com.avonix.profitness.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.data.challenges.ChallengePrefsRepository
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.domain.challenges.ChallengeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private var refreshJob: Job? = null
    private var lastLoadMs: Long = 0L

    init { refresh(force = true) }

    fun reloadIfStale() {
        val hasLoaded = lastLoadMs > 0L
        val isFresh = System.currentTimeMillis() - lastLoadMs < EVENTS_TTL_MS
        if (hasLoaded && isFresh) return
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        if (refreshJob?.isActive == true) return
        val hasData = state.value.today.isNotEmpty() || state.value.upcoming.isNotEmpty()
        val hasLoaded = lastLoadMs > 0L
        val isFresh = System.currentTimeMillis() - lastLoadMs < EVENTS_TTL_MS
        if (!force && hasLoaded && isFresh) return

        _state.update { it.copy(isLoading = !hasData, error = null) }
        refreshJob = viewModelScope.launch {
            val today = LocalDate.now().toString()
            val todayDef    = async { repo.listMyEventsForDate(today) }
            val upcomingDef = async { repo.listMyUpcomingEvents(7) }

            val todayRes    = todayDef.await()
            val upcomingRes = upcomingDef.await()
            val err = todayRes.exceptionOrNull()?.message ?: upcomingRes.exceptionOrNull()?.message

            val todayList = todayRes.getOrNull().orEmpty()
            val skipFlag = prefs.isAnySkippedForDate(today)
            lastLoadMs = System.currentTimeMillis()

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

    private companion object {
        const val EVENTS_TTL_MS = 2 * 60_000L
    }
}
