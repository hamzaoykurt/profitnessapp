package com.avonix.profitness.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class AIProgramJson(
    val name : String,
    val days : List<AIProgramDay>
)

@Serializable
data class AIProgramDay(
    val title      : String,
    val isRestDay  : Boolean       = false,
    val exercises  : List<AIExerciseEntry> = emptyList()
)

@Serializable
data class AIExerciseEntry(
    val exerciseName : String,
    val sets         : Int = 3,
    val reps         : Int = 10,
    val restSeconds  : Int = 90
)
