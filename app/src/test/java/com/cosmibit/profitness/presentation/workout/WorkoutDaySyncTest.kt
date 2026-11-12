package com.cosmibit.profitness.presentation.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDaySyncTest {
    @Test fun settleOnNewPageSelectsIt() {
        assertEquals(3, WorkoutDaySync.dayToSelect(3, 2, 7))
        assertNull(WorkoutDaySync.dayToSelect(2, 2, 7))
        assertNull(WorkoutDaySync.dayToSelect(9, 2, 7))
    }

    @Test fun externalChangeScrollsPager() {
        assertTrue(WorkoutDaySync.shouldScroll(currentPage = 2, selectedIdx = 5, dayCount = 7))
        assertFalse(WorkoutDaySync.shouldScroll(5, 5, 7))
        assertFalse(WorkoutDaySync.shouldScroll(0, 0, 1))
        assertFalse(WorkoutDaySync.shouldScroll(0, 7, 7))
    }

    @Test fun userDragIsNeverPulledBack() {
        // Kullanıcı 3'ten 4'e sürüklerken VM'den 3 gelir: pager geri çekilmez.
        assertFalse(WorkoutDaySync.shouldScroll(currentPage = 4, selectedIdx = 3, dayCount = 7, userDragging = true))
    }

    @Test fun staleVmValueWhilePendingDoesNotPullBack() {
        // 2→3 (istek 3), hemen 3→4 (istek 4). VM önce eski 3'ü onaylar: bekleme sürer, geri çekme yok.
        var pending: Int? = 4
        pending = WorkoutDaySync.resolvePending(pending, selectedIdx = 3)
        assertEquals(4, pending)
        assertFalse(WorkoutDaySync.shouldScroll(currentPage = 4, selectedIdx = 3, dayCount = 7, pendingIdx = pending))
        // VM 4'ü yayınlar: bekleme biter, pager zaten 4'te.
        pending = WorkoutDaySync.resolvePending(pending, selectedIdx = 4)
        assertNull(pending)
        assertFalse(WorkoutDaySync.shouldScroll(currentPage = 4, selectedIdx = 4, dayCount = 7, pendingIdx = pending))
    }

    @Test fun pendingTimeoutFallsBackToVmValue() {
        // VM isteği reddetti (değer 2'de kaldı); zaman aşımıyla pending=null → pager VM gününe döner.
        assertTrue(WorkoutDaySync.shouldScroll(currentPage = 3, selectedIdx = 2, dayCount = 7, pendingIdx = null))
        assertNull(WorkoutDaySync.resolvePending(null, 2))
    }
}
