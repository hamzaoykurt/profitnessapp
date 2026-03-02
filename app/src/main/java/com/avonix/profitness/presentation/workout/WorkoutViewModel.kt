package com.avonix.profitness.presentation.workout

import com.avonix.profitness.core.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class WorkoutScreenState(
    val dayStates: List<WorkoutDayState> = DEMO_WORKOUTS.map { WorkoutDayState(it) },
    val selectedDayIdx: Int = 0
)

@HiltViewModel
class WorkoutViewModel @Inject constructor() : BaseViewModel<WorkoutScreenState>(WorkoutScreenState()) {

    fun selectDay(idx: Int) {
        updateState { it.copy(selectedDayIdx = idx) }
    }

    fun toggleExercise(dayIdx: Int, exerciseId: String) {
        updateState { state ->
            val dayStates = state.dayStates.toMutableList()
            val current = dayStates[dayIdx]
            val newIds = current.completedIds.toMutableSet()
            if (exerciseId in newIds) newIds.remove(exerciseId) else newIds.add(exerciseId)
            dayStates[dayIdx] = current.copy(completedIds = newIds)
            state.copy(dayStates = dayStates.toList())
        }
    }
}
