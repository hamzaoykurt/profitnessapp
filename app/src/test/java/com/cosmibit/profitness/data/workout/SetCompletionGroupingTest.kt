package com.cosmibit.profitness.data.workout

import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SetCompletionGroupingTest {

    @Test
    fun `same exercise on different program days keeps separate completions`() {
        val completions = listOf(
            completion(programDayId = "monday", exerciseId = "squat", setIndex = 0),
            completion(programDayId = "monday", exerciseId = "squat", setIndex = 1),
            completion(programDayId = "friday", exerciseId = "squat", setIndex = 0)
        ).toSetCompletionMap()

        assertEquals(setOf(0, 1), completions[SetCompletionKey("monday", "squat")])
        assertEquals(setOf(0), completions[SetCompletionKey("friday", "squat")])
        assertEquals(2, completions.size)
    }

    private fun completion(
        programDayId: String,
        exerciseId: String,
        setIndex: Int
    ) = SetCompletionEntity(
        userId = "user",
        exerciseId = exerciseId,
        programDayId = programDayId,
        setIndex = setIndex,
        date = "2026-09-08",
        repsActual = 10
    )
}
