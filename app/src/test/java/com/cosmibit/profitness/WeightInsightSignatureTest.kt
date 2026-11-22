package com.cosmibit.profitness

import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import com.cosmibit.profitness.presentation.weight.insightSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Kilo AI analizi önbelleği: aynı kayıtlar -> aynı imza (kredi harcanmaz); ekleme/silme/düzenleme -> eskir. */
class WeightInsightSignatureTest {
    private fun e(id: String, kg: Double, at: String, note: String = "") =
        WeightLogEntity(id = id, userId = "u", weightKg = kg, note = note, recordedAt = at)
    private val base = listOf(e("a", 62.0, "2026-05-01T08:00:00"), e("b", 64.0, "2026-05-08T08:00:00"))

    @Test fun sameEntriesSameSignature() = assertEquals(insightSignature(base), insightSignature(base.reversed()))
    @Test fun addedEntryInvalidates() = assertNotEquals(insightSignature(base), insightSignature(base + e("c", 63.0, "2026-05-15T08:00:00")))
    @Test fun deletedEntryInvalidates() = assertNotEquals(insightSignature(base), insightSignature(base.take(1)))
    @Test fun editedLatestInvalidates() = assertNotEquals(insightSignature(base), insightSignature(listOf(base[0], e("b", 64.5, "2026-05-08T08:00:00"))))
    @Test fun editedOlderEntryInvalidates() = assertNotEquals(insightSignature(base), insightSignature(listOf(e("a", 61.5, "2026-05-01T08:00:00"), base[1])))
    @Test fun editedNoteOrTimestampInvalidates() {
        assertNotEquals(insightSignature(base), insightSignature(listOf(e("a", 62.0, "2026-05-01T08:00:00", note = "açlık"), base[1])))
        assertNotEquals(insightSignature(base), insightSignature(listOf(e("a", 62.0, "2026-05-02T08:00:00"), base[1])))
    }
}
