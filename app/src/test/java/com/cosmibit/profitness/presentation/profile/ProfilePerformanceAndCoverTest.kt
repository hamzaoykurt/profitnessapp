package com.cosmibit.profitness.presentation.profile

import com.cosmibit.profitness.data.profile.CoverImageMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePerformanceAndCoverTest {

    @Test fun performanceHasNoStreakAndMaxThree() {
        val v = performanceValues(2, 40, 12400.0, "Incline DB Press", 35f, 6300, 0f)
        assertEquals(listOf(PerfValue.Week(2), PerfValue.Volume(12400.0), PerfValue.TopLift("Incline DB Press", 35f)), v)
    }

    @Test fun fallsBackToDurationAndWorkouts() {
        val v = performanceValues(0, 5, 0.0, "", 0f, 3000, 0f)
        assertEquals(listOf(PerfValue.Week(0), PerfValue.Duration(3000), PerfValue.Workouts(5)), v)
    }

    @Test fun emptyWhenNoData() = assertTrue(performanceValues(0, 0, 0.0, "", 0f, 0, 0f).isEmpty())

    @Test fun hoursMinutesSplit() {
        assertEquals("1" to "sa 45 dk", splitHoursMinutes(6300, false))
        assertEquals("45" to "dk", splitHoursMinutes(2700, false))
        assertEquals("2" to "sa", splitHoursMinutes(7200, false))
    }

    @Test fun centerCropWideAndTall() {
        // 4000x2000 (2:1) -> 1.25 oranı: genişlik 2500, ortalı
        assertEquals(CoverImageMath.Rect(750, 0, 2500, 2000), CoverImageMath.centerCrop(4000, 2000, 1.25f))
        // 1000x2000 (dikey) -> yükseklik 800, ortalı
        assertEquals(CoverImageMath.Rect(0, 600, 1000, 800), CoverImageMath.centerCrop(1000, 2000, 1.25f))
    }

    @Test fun sampleAndFit() {
        assertEquals(2, CoverImageMath.sampleSize(4000, 3000, 1080, 864))
        assertEquals(1, CoverImageMath.sampleSize(1200, 900, 1080, 864))
        assertEquals(1080 to 864, CoverImageMath.fitWithin(2500, 2000, 1080))
        assertEquals(800 to 640, CoverImageMath.fitWithin(800, 640, 1080))
    }
}
