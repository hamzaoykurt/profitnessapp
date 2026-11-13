package com.cosmibit.profitness

import com.cosmibit.profitness.presentation.components.formatTimerClock
import com.cosmibit.profitness.presentation.workout.timerElapsedSeconds
import com.cosmibit.profitness.presentation.workout.timerTickDelayMs
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingWorkoutTimerTest {

    @Test
    fun formatsClock() {
        assertEquals("0:00", formatTimerClock(-3))
        assertEquals("0:05", formatTimerClock(5))
        assertEquals("3:33", formatTimerClock(213))
        assertEquals("1:02:09", formatTimerClock(3729))
    }

    @Test
    fun elapsedUsesWallTimeNotTickCount() {
        // 90 sn arka planda kaldıktan sonra tek tik bile gerçek süreyi verir.
        assertEquals(10 + 90, timerElapsedSeconds(10, anchorMs = 1_000L, nowMs = 91_400L))
        assertEquals(0, timerElapsedSeconds(0, anchorMs = 5_000L, nowMs = 4_000L))
        assertEquals(600L, timerTickDelayMs(anchorMs = 0L, nowMs = 2_400L))
    }
}
