package com.avonix.profitness.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates high-frequency sync triggers from UI lifecycle/tab events.
 *
 * Local Room flows remain the source of truth. This class only decides when a
 * remote refresh is worth starting, dedupes in-flight work, and delays requests
 * slightly so tab transitions can finish before network/DB work begins.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val syncManager: SyncManager
) {
    private val mutex = Mutex()
    private val lastSuccessAt = mutableMapOf<String, Long>()
    private val inFlight = mutableMapOf<String, kotlinx.coroutines.Deferred<Result<Unit>>>()

    suspend fun refreshWorkout(
        userId: String,
        force: Boolean = false,
        ttlMillis: Long = WORKOUT_TTL_MS,
        debounceMillis: Long = UI_DEBOUNCE_MS
    ): Result<Unit> = schedule(
        key = "workout:$userId",
        force = force,
        ttlMillis = ttlMillis,
        debounceMillis = debounceMillis
    ) {
        syncManager.pullPrograms(userId).getOrThrow()
        syncManager.pullWorkoutLogs(userId).getOrThrow()
        syncManager.pullWorkoutLogDates(userId).getOrThrow()
    }

    suspend fun refreshPrograms(
        userId: String,
        force: Boolean = false,
        ttlMillis: Long = PROGRAM_TTL_MS,
        debounceMillis: Long = UI_DEBOUNCE_MS
    ): Result<Unit> = schedule(
        key = "programs:$userId",
        force = force,
        ttlMillis = ttlMillis,
        debounceMillis = debounceMillis
    ) {
        syncManager.pullPrograms(userId).getOrThrow()
    }

    private suspend fun schedule(
        key: String,
        force: Boolean,
        ttlMillis: Long,
        debounceMillis: Long,
        block: suspend () -> Unit
    ): Result<Unit> = coroutineScope {
        val deferred = mutex.withLock {
            val now = System.currentTimeMillis()
            val isFresh = !force && lastSuccessAt[key]?.let { now - it < ttlMillis } == true
            if (isFresh) {
                return@coroutineScope Result.success(Unit)
            }

            inFlight[key]?.let { return@withLock it }

            async(Dispatchers.IO + SupervisorJob()) {
                if (debounceMillis > 0) delay(debounceMillis)
                runCatching { block() }
                    .onSuccess {
                        mutex.withLock { lastSuccessAt[key] = System.currentTimeMillis() }
                    }
                    .also {
                        mutex.withLock { inFlight.remove(key) }
                    }
            }.also { inFlight[key] = it }
        }

        deferred.await()
    }

    private companion object {
        const val UI_DEBOUNCE_MS = 350L
        const val WORKOUT_TTL_MS = 3 * 60 * 1000L
        const val PROGRAM_TTL_MS = 2 * 60 * 1000L
    }
}
