package com.cosmibit.profitness.data.workout

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WorkoutWeekDateTest {

    @Test
    fun `week start is monday for every selected day`() {
        val expected = "2026-09-07"

        (7..13).forEach { dayOfMonth ->
            assertEquals(
                expected,
                currentWeekStart(LocalDate.of(2026, 9, dayOfMonth))
            )
        }
    }
}
