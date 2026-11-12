package com.cosmibit.profitness.presentation.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDaySyncTest {
    @Test fun settleOnNewPageSelectsIt() {
        assertEquals(3, WorkoutDaySync.dayToSelect(3, 2, 7))
        assertNull(WorkoutDaySync.dayToSelect(2, 2, 7))
        assertNull(WorkoutDaySync.dayToSelect(9, 2, 7))
    }

    @Test fun externalChangeScrollsPager() {
        assertTrue(WorkoutDaySync.shouldScroll(currentPage = 2, selectedIdx = 5, dayCount = 7))
        assertFalse(WorkoutDaySync.shouldScroll(5, 5, 7))
        assertFalse(WorkoutDaySync.shouldScroll(0, 0, 1))
    }
}
