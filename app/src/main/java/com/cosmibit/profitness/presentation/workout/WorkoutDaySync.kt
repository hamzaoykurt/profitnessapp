package com.cosmibit.profitness.presentation.workout

/** Yatay gün pager'ı ile ViewModel `selectedDayIdx` senkron kararları (saf, test edilebilir). */
internal object WorkoutDaySync {
    /** Pager yeni sayfaya OTURDU: ViewModel'e bildirilecek gün (yoksa null). */
    fun dayToSelect(settledPage: Int, selectedIdx: Int, dayCount: Int): Int? =
        settledPage.takeIf { it != selectedIdx && it in 0 until dayCount }

    /** Dışarıdan gün değişti: pager o sayfaya kaydırılmalı mı? */
    fun shouldScroll(currentPage: Int, selectedIdx: Int, dayCount: Int): Boolean =
        selectedIdx in 0 until dayCount && currentPage != selectedIdx
}
