package com.cosmibit.profitness.domain.ai

import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.data.store.UserPlanRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sade model: ÜCRETSİZ (AI-program 2 hak) + PREMIUM (tüm AI). */
class AiAccessPolicyTest {

    private val programFeatures = setOf(AiFeature.ProgramGenerate, AiFeature.ProgramGenerateMedia)

    @Test fun freeLocksEveryAiExceptProgramCreation() {
        AiFeature.values().filterNot { it in programFeatures }.forEach {
            assertEquals(it.name, AiAccess.Locked, AiAccessPolicy.access(UserPlan.FREE, it, freeProgramsLeft = 2))
        }
    }

    @Test fun freeProgramCreationUsesGrants() {
        programFeatures.forEach { f ->
            assertEquals(AiAccess.FreeGrant(2), AiAccessPolicy.access(UserPlan.FREE, f, 2))
            assertEquals(AiAccess.FreeGrant(1), AiAccessPolicy.access(UserPlan.FREE, f, 1))
            assertEquals(AiAccess.Locked, AiAccessPolicy.access(UserPlan.FREE, f, 0))
            // sunucu henüz sayaç dönmüyorsa istemci kilitlemez (sunucu karar verir)
            assertEquals(AiAccess.FreeGrant(null), AiAccessPolicy.access(UserPlan.FREE, f, null))
        }
    }

    @Test fun premiumHasEverythingUnlimited() {
        AiFeature.values().forEach { f ->
            assertEquals(f.name, AiAccess.Unlimited, AiAccessPolicy.access(UserPlan.PREMIUM, f, freeProgramsLeft = 0))
        }
        assertTrue(AiAccessPolicy.hasPersonalMemory(UserPlan.PREMIUM))
        assertTrue(AiAccessPolicy.hasPriority(UserPlan.PREMIUM))
        assertFalse(AiAccessPolicy.hasPersonalMemory(UserPlan.FREE))
    }

    @Test fun legacyServerPlansMapToPremium() {
        assertEquals(UserPlan.PREMIUM, UserPlan.fromServer("PRO"))
        assertEquals(UserPlan.PREMIUM, UserPlan.fromServer("ELITE"))
        assertEquals(UserPlan.PREMIUM, UserPlan.fromServer("premium"))
        assertEquals(UserPlan.FREE, UserPlan.fromServer("FREE"))
        assertNull(UserPlan.fromServer("GOLD"))
        assertNull(UserPlan.fromServer(null))
    }

    @Test fun everyServerToolIsMapped() =
        assertEquals(AiToolType.values().toSet(), AiFeature.values().map { it.tool }.toSet())

    @Test fun freeGrantLine() {
        assertEquals("2 ücretsiz AI program hakkın var", AiAccessPolicy.freeGrantLine(2, english = false))
        assertEquals("1 hakkın kaldı", AiAccessPolicy.freeGrantLine(1, english = false))
        assertEquals("Ücretsiz AI program hakların bitti", AiAccessPolicy.freeGrantLine(0, english = false))
        assertEquals("Ücretsiz planda ${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} AI program hakkı", AiAccessPolicy.freeGrantLine(null, english = false))
    }
}
