package com.cosmibit.profitness.presentation.workout

import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDetailLogicTest {
    private fun set(date: String, idx: Int, kg: Float?, reps: Int?, dur: Int? = null) = SetCompletionEntity(
        userId = "u", exerciseId = "e", programDayId = "d", setIndex = idx, date = date,
        weightKg = kg, repsActual = reps, durationSeconds = dur
    )

    @Test
    fun `delta compares top weight of last two sessions`() {
        val stats = buildHistoryStats(
            listOf(
                set("2026-11-01", 0, 27.5f, 10), set("2026-11-01", 1, 27.5f, 9),
                set("2026-11-08", 0, 29f, 12), set("2026-11-08", 1, 27.5f, 10)
            )
        )
        assertEquals(2, stats.sessions.size)
        assertEquals("2026-11-08", stats.last?.date)
        assertEquals(1.5f, stats.topWeightDeltaKg!!, 0.001f)
    }

    @Test
    fun `single session has no delta and record comes from heaviest set then reps`() {
        val stats = buildHistoryStats(listOf(set("2026-11-01", 0, 30f, 6), set("2026-11-01", 1, 30f, 8), set("2026-11-01", 2, 25f, 12)))
        assertNull(stats.topWeightDeltaKg)
        assertEquals(30f, stats.record!!.weightKg, 0.001f)
        assertEquals(8, stats.record!!.reps)
    }

    @Test
    fun `no weights means no record and no delta, empty history is empty`() {
        val stats = buildHistoryStats(listOf(set("2026-11-01", 0, null, 15), set("2026-11-02", 0, null, 15)))
        assertNull(stats.record)
        assertNull(stats.topWeightDeltaKg)
        assertNull(buildHistoryStats(emptyList()).last)
    }

    @Test
    fun `chart series skips sessions without value`() {
        val sessions = buildHistoryStats(listOf(set("2026-11-01", 0, null, 10), set("2026-11-02", 0, 20f, 10), set("2026-11-03", 0, 22.5f, 8))).sessions
        assertEquals(listOf(20f, 22.5f), chartSeries(sessions, strength = true).map { it.value })
        val dur = buildHistoryStats(listOf(set("2026-11-01", 0, null, null, 600), set("2026-11-02", 0, null, null, 1200))).sessions
        assertEquals(listOf(10f, 20f), chartSeries(dur, strength = false).map { it.value })
    }

    @Test
    fun `muscle regions map TR and EN targets and ignore unknown`() {
        assertEquals(setOf(MuscleRegion.CHEST), muscleRegionsFor("CHEST (GÖĞÜS)"))
        assertEquals(setOf(MuscleRegion.TRICEPS), muscleRegionsFor("Arka kol"))
        assertEquals(setOf(MuscleRegion.HAMSTRINGS), muscleRegionsFor("Arka bacak"))
        assertEquals(setOf(MuscleRegion.BICEPS, MuscleRegion.TRICEPS), muscleRegionsFor("Arms"))
        assertTrue(MuscleRegion.QUADS in muscleRegionsFor("Chest, Quads"))
        assertTrue(muscleRegionsFor("Kardiyo").isEmpty())
        assertTrue(muscleRegionsFor("").isEmpty())
        assertNotNull(muscleRegionsFor("Omuz").singleOrNull())
    }
}
