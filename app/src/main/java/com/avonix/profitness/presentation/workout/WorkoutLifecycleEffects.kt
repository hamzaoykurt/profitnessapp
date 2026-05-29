package com.avonix.profitness.presentation.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WorkoutRefreshOnResumeEffect(
    minBackgroundMillis: Long = 30_000L,
    resumeDelayMillis: Long = 1_500L,
    onStop: () -> Unit = {},
    onResume: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val latestOnStop by rememberUpdatedState(onStop)
    val latestOnResume by rememberUpdatedState(onResume)

    DisposableEffect(lifecycle, minBackgroundMillis, resumeDelayMillis) {
        var stoppedAtMs = 0L
        var pendingJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    stoppedAtMs = System.currentTimeMillis()
                    pendingJob?.cancel()
                    latestOnStop()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val backgroundMs = if (stoppedAtMs > 0L) {
                        System.currentTimeMillis() - stoppedAtMs
                    } else {
                        0L
                    }
                    if (backgroundMs >= minBackgroundMillis) {
                        pendingJob?.cancel()
                        pendingJob = scope.launch {
                            delay(resumeDelayMillis)
                            latestOnResume()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            pendingJob?.cancel()
            lifecycle.removeObserver(observer)
        }
    }
}
