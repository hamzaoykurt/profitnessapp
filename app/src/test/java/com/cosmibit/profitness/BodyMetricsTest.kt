package com.cosmibit.profitness

import com.cosmibit.profitness.presentation.profile.bodyMetricsFor
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyMetricsTest {
    @Test fun latestLogWinsOverProfile() {
        val b = bodyMetricsFor(62.0, 64.0, 175.0, 30, true)
        assertEquals(64.0, b.weightKg, 0.0)
        assertEquals(64.0 / (1.75 * 1.75), b.bmi, 1e-9)
    }

    @Test fun noLogFallsBackToProfile() = assertEquals(62.0, bodyMetricsFor(62.0, null, 175.0, 30, true).weightKg, 0.0)

    @Test fun zeroLogIgnored() = assertEquals(62.0, bodyMetricsFor(62.0, 0.0, 175.0, 30, false).weightKg, 0.0)

    @Test fun noHeightNoBmi() = assertEquals(0.0, bodyMetricsFor(62.0, 64.0, 0.0, 30, true).bmi, 0.0)
}
