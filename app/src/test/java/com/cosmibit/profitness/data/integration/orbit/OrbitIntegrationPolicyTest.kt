package com.cosmibit.profitness.data.integration.orbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitIntegrationPolicyTest {
    private fun connected(entitled: Boolean = true, enabled: Boolean = true) =
        OrbitIntegrationPolicy.sanitize(
            OrbitIntegrationState(
                status = OrbitConnectionStatus.CONNECTED,
                fitnessSyncEntitled = entitled,
                syncEnabled = enabled
            )
        )

    @Test fun `1 fitness-only user remains disconnected`() = assertFalse(OrbitIntegrationState().canSync)
    @Test fun `2 Orbit account not connected never syncs`() = assertFalse(OrbitIntegrationState(syncEnabled = true, fitnessSyncEntitled = true).canSync)
    @Test fun `3 connected without premium never syncs`() = assertFalse(connected(entitled = false).canSync)
    @Test fun `4 connected premium and enabled can sync`() = assertTrue(connected().canSync)
    @Test fun `5 completing workout is eligible only after all gates`() = assertTrue(connected().canSync)
    @Test fun `6 multiple workouts reuse the same eligibility decision`() = repeat(3) { assertTrue(connected().canSync) }
    @Test fun `7 corrected workout can trigger a replacement summary`() = assertTrue(connected().canSync)
    @Test fun `8 temporary outage disables outbound sync`() = assertFalse(OrbitIntegrationPolicy.sanitize(OrbitIntegrationState(status = OrbitConnectionStatus.TEMPORARILY_UNAVAILABLE, fitnessSyncEntitled = true, syncEnabled = true)).canSync)
    @Test fun `9 disconnect removes sync eligibility`() = assertFalse(OrbitIntegrationPolicy.sanitize(OrbitIntegrationState()).canSync)
    @Test fun `10 reconnect requires authorization before sync`() = assertFalse(OrbitIntegrationPolicy.sanitize(OrbitIntegrationState(status = OrbitConnectionStatus.RECONNECT_REQUIRED, fitnessSyncEntitled = true, syncEnabled = true)).canSync)
    @Test fun `11 duplicate requests do not change policy state`() = assertEquals(connected(), connected())
    @Test fun `12 entitlement loss forces sync off`() = assertFalse(connected(entitled = false).syncEnabled)
    @Test fun `13 unknown server status cannot authorize access`() = assertEquals(OrbitConnectionStatus.NOT_CONNECTED, OrbitIntegrationPolicy.status("forged"))
    @Test fun `14 mobile settings receives a stable connected state`() = assertEquals(OrbitConnectionStatus.CONNECTED, connected().status)
    @Test fun `15 desktop adaptive settings receives a stable disconnected state`() = assertEquals(OrbitConnectionStatus.NOT_CONNECTED, OrbitIntegrationPolicy.status("not_connected"))
}
