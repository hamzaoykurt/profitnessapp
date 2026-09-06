package com.cosmibit.profitness.data.integration.orbit

import com.cosmibit.profitness.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Fitness writes finish first; Orbit notification is coalesced and never blocks the workout flow. */
@Singleton
class OrbitSyncCoordinator @Inject constructor(
    private val syncManager: SyncManager,
    private val orbitRepository: OrbitIntegrationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Job? = null

    fun fitnessDataChanged(userId: String) {
        val current = orbitRepository.state.value
        if (current.isInitialized && !current.canSync) return
        pending?.cancel()
        pending = scope.launch {
            delay(900)
            if (!orbitRepository.state.value.isInitialized) runCatching { orbitRepository.refresh() }
            if (!orbitRepository.state.value.canSync) return@launch
            runCatching { syncManager.pushUnsyncedWorkouts() }
            runCatching { syncManager.pushSetCompletions(userId) }
            runCatching { orbitRepository.requestSync(ZoneId.systemDefault().id) }
        }
    }
}
