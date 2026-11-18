package com.cosmibit.profitness.domain.progress

import com.cosmibit.profitness.presentation.profile.computeRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Eşikler eski computeRank ile birebir (Bronze 0, Silver 1000, Gold 5000, Platinum 15000, Diamond 50000). */
class RankTiersTest {
    private fun legacy(xp: Int) = when {
        xp >= 50000 -> "Diamond"; xp >= 15000 -> "Platinum"; xp >= 5000 -> "Gold"; xp >= 1000 -> "Silver"; else -> "Bronze"
    }

    @Test fun sameAsLegacyRank() {
        listOf(0, 1, 999, 1000, 2100, 4999, 5000, 14999, 15000, 49999, 50000, 120000).forEach {
            assertEquals("xp=$it", legacy(it), computeRank(it))
        }
    }

    @Test fun nextTierAndProgress() {
        assertEquals("Gold", RankTiers.nextTier(2100)?.name)
        assertEquals(2100f / 5000f, RankTiers.progressToNext(2100), 1e-6f)
        assertNull(RankTiers.nextTier(60000))
        assertEquals(1f, RankTiers.progressToNext(60000), 0f)
    }

    @Test fun levelStep() {
        assertEquals(400, RankTiers.xpToNextLevel(2100))
        assertEquals(500, RankTiers.xpToNextLevel(2000))
    }
}
