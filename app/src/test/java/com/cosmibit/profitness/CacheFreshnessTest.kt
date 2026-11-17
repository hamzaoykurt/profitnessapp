package com.cosmibit.profitness

import com.cosmibit.profitness.core.ui.isCacheFresh
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheFreshnessTest {
    private val ttl = 5 * 60_000L

    @Test fun freshWithinTtlWithData() = assertTrue(isCacheFresh(true, 1_000L, 1_000L + ttl - 1, ttl))
    @Test fun staleAtTtl() = assertFalse(isCacheFresh(true, 1_000L, 1_000L + ttl, ttl))
    @Test fun noDataNeverFresh() = assertFalse(isCacheFresh(false, 1_000L, 1_001L, ttl))
    @Test fun neverLoadedNotFresh() = assertFalse(isCacheFresh(true, 0L, 10L, ttl))
    @Test fun clockWentBackNotFresh() = assertFalse(isCacheFresh(true, 5_000L, 1_000L, ttl))
}
