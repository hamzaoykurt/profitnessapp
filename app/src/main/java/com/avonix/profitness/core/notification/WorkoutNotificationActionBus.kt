package com.avonix.profitness.core.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class WorkoutNotificationAction {
    Pause,
    Resume,
    Stop,
    Dismiss
}

@Singleton
class WorkoutNotificationActionBus @Inject constructor() {
    private val _actions = MutableSharedFlow<WorkoutNotificationAction>(extraBufferCapacity = 8)
    val actions = _actions.asSharedFlow()

    fun dispatch(action: WorkoutNotificationAction) {
        _actions.tryEmit(action)
    }
}
