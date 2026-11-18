package com.cosmibit.profitness

import com.cosmibit.profitness.data.store.CancelPlanResult
import com.cosmibit.profitness.data.store.PlanResolver
import com.cosmibit.profitness.data.store.UserPlan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PlanResolverTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    @Test fun expiredPeriodIsFree() =
        assertEquals(UserPlan.FREE, PlanResolver.effectivePlan("ELITE", "active", "2026-10-06T00:00:00+00:00", now))

    @Test fun activePeriodIsPremium() =
        assertEquals(UserPlan.PREMIUM, PlanResolver.effectivePlan("PRO", "active", "2026-12-18T00:00:00+00:00", now))

    @Test fun noPeriodEndActiveIsPremium() =
        assertEquals(UserPlan.PREMIUM, PlanResolver.effectivePlan("PREMIUM", "trialing", null, now))

    @Test fun inactiveStatusIsFree() {
        assertEquals(UserPlan.FREE, PlanResolver.effectivePlan("ELITE", "canceled", null, now))
        assertEquals(UserPlan.FREE, PlanResolver.effectivePlan("FREE", "active", null, now))
        assertEquals(UserPlan.FREE, PlanResolver.effectivePlan("GOLD", "active", null, now))
    }

    @Test fun expiredStatusLabel() {
        assertEquals("expired", PlanResolver.effectiveStatus("active", "2026-10-06T00:00:00Z", now))
        assertEquals("active", PlanResolver.effectiveStatus("active", "2026-12-06T00:00:00Z", now))
    }

    private fun parse(s: String) = CancelPlanResult.parse(Json.parseToJsonElement(s).jsonObject)

    @Test fun cancelResultParsing() {
        assertEquals(CancelPlanResult.Canceled("sandbox"), parse("""{"ok":true,"plan":"FREE","previousSource":"sandbox"}"""))
        assertEquals(CancelPlanResult.ManageInStore, parse("""{"ok":false,"reason":"manage_in_store","plan":"PRO"}"""))
        assertEquals(CancelPlanResult.Failed("unauthorized"), parse("""{"ok":false,"reason":"unauthorized"}"""))
        assertEquals(CancelPlanResult.Canceled(null), parse("""{"ok":true,"plan":"FREE","previousSource":null}"""))
    }
}
