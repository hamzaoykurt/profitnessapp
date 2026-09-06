package com.cosmibit.profitness.data.integration.orbit

import kotlinx.coroutines.flow.StateFlow

enum class OrbitConnectionStatus {
    NOT_CONNECTED,
    CONNECTED,
    RECONNECT_REQUIRED,
    TEMPORARILY_UNAVAILABLE
}

data class OrbitIntegrationState(
    val status: OrbitConnectionStatus = OrbitConnectionStatus.NOT_CONNECTED,
    val isLoading: Boolean = false,
    val accountLabel: String? = null,
    val syncEnabled: Boolean = false,
    val fitnessSyncEntitled: Boolean = false,
    val lastSyncedAt: String? = null,
    val lastErrorCode: String? = null,
    val manageUrl: String? = null,
    val isInitialized: Boolean = false
) {
    val isConnected: Boolean get() = status == OrbitConnectionStatus.CONNECTED
    val canSync: Boolean get() = isConnected && syncEnabled && fitnessSyncEntitled
}

interface OrbitIntegrationRepository {
    val state: StateFlow<OrbitIntegrationState>
    suspend fun refresh(): Result<OrbitIntegrationState>
    suspend fun beginConnection(): Result<String>
    suspend fun disconnect(): Result<Unit>
    suspend fun setSyncEnabled(enabled: Boolean): Result<Unit>
    suspend fun requestSync(timeZone: String): Result<Unit>
}

object OrbitIntegrationPolicy {
    fun status(raw: String): OrbitConnectionStatus = when (raw) {
        "connected" -> OrbitConnectionStatus.CONNECTED
        "reconnect_required" -> OrbitConnectionStatus.RECONNECT_REQUIRED
        "temporarily_unavailable" -> OrbitConnectionStatus.TEMPORARILY_UNAVAILABLE
        else -> OrbitConnectionStatus.NOT_CONNECTED
    }

    fun sanitize(state: OrbitIntegrationState): OrbitIntegrationState {
        val allowed = state.status == OrbitConnectionStatus.CONNECTED && state.fitnessSyncEntitled
        return state.copy(syncEnabled = state.syncEnabled && allowed, isLoading = false, isInitialized = true)
    }
}
