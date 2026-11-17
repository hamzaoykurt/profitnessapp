package com.cosmibit.profitness.domain.ai

import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.store.UserPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAccessPolicyTest {

    @Test fun freeHasNoAi() {
        AiFeature.values().forEach {
            assertEquals(AiAccess.Locked(UserPlan.PRO), AiAccessPolicy.access(UserPlan.FREE, it))
        }
        assertFalse(AiAccessPolicy.hasAnyAi(UserPlan.FREE))
    }

    @Test fun proAndEliteHaveEveryAiFeature() {
        listOf(UserPlan.PRO, UserPlan.ELITE).forEach { plan ->
            AiFeature.values().forEach { f -> assertTrue("$plan $f", AiAccessPolicy.canUse(plan, f)) }
        }
    }

    @Test fun eliteOnlyExtras() {
        assertTrue(AiAccessPolicy.hasPersonalMemory(UserPlan.ELITE))
        assertTrue(AiAccessPolicy.hasPriority(UserPlan.ELITE))
        assertFalse(AiAccessPolicy.hasPersonalMemory(UserPlan.PRO))
        assertFalse(AiAccessPolicy.hasPriority(UserPlan.FREE))
    }

    /** Sunucu reserve_ai_usage maliyetleriyle aynı (20260519193000_rebalance_energy_plan_hierarchy.sql). */
    @Test fun costsMatchServerTable() {
        val server = mapOf(
            AiToolType.ORACLE_CHAT to 1, AiToolType.PROGRAM_GENERATE_TEXT to 6, AiToolType.PROGRAM_GENERATE_MEDIA to 10,
            AiToolType.PROGRAM_EDIT to 4, AiToolType.WEIGHT_TREND_ANALYSIS to 2, AiToolType.EXERCISE_PROGRESS_ANALYSIS to 2,
            AiToolType.WORKOUT_PROGRESS_ANALYSIS to 2, AiToolType.ORACLE_TO_PROGRAM to 4
        )
        AiFeature.values().forEach { assertEquals(it.name, server[it.tool], it.energyCost) }
        assertEquals(AiToolType.values().toSet(), AiFeature.values().map { it.tool }.toSet())
    }

    @Test fun costLineShowsCostAndBalance() {
        assertEquals("Plan kotasından · sonra 2 Enerji · Kalan 12",
            AiAccessPolicy.costLine(UserPlan.PRO, AiFeature.WeightAnalysis, 12, english = false))
        assertEquals("Pro veya Elite plan gerekir",
            AiAccessPolicy.costLine(UserPlan.FREE, AiFeature.WeightAnalysis, 32, english = false))
    }
}
