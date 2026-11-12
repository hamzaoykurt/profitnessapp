package com.cosmibit.profitness.presentation.workout

/** Yatay gün pager'ı ile ViewModel `selectedDayIdx` senkron kararları (saf, test edilebilir). */
internal object WorkoutDaySync {
    /** VM bekleyen isteği onaylamazsa bu süre sonra bekleme düşer, pager VM değerine döner. */
    const val PENDING_TIMEOUT_MS = 1_500L

    /** Pager yeni sayfaya OTURDU: ViewModel'e bildirilecek gün (yoksa null). */
    fun dayToSelect(settledPage: Int, selectedIdx: Int, dayCount: Int): Int? =
        settledPage.takeIf { it != selectedIdx && it in 0 until dayCount }

    /**
     * Dışarıdan gün değişti: pager o sayfaya kaydırılmalı mı?
     * Kullanıcı sürüklerken ([userDragging]) veya kendi isteğimizin onayı beklenirken ([pendingIdx])
     * VM'den gelen farklı/eski değer pager'ı geri çekmez.
     */
    fun shouldScroll(
        currentPage: Int,
        selectedIdx: Int,
        dayCount: Int,
        userDragging: Boolean = false,
        pendingIdx: Int? = null
    ): Boolean =
        !userDragging && pendingIdx == null &&
            selectedIdx in 0 until dayCount && currentPage != selectedIdx

    /** VM yeni değer yayınladı: istediğimiz gün geldiyse bekleme biter; başka değer = eski/ara değer, beklemeye devam. */
    fun resolvePending(pendingIdx: Int?, selectedIdx: Int): Int? =
        pendingIdx?.takeIf { it != selectedIdx }
}
