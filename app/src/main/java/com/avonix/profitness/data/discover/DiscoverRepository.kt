package com.avonix.profitness.data.discover

import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.MySharedProgram
import com.avonix.profitness.domain.discover.SharedProgram

interface DiscoverRepository {

    /** Public feed — paged. */
    suspend fun getFeed(sort: DiscoverSort, limit: Int, offset: Int): Result<List<SharedProgram>>

    /** Atomic like toggle — döner: yeni "liked" durumu. */
    suspend fun toggleLike(programId: String): Result<Boolean>

    /** Atomic save toggle — döner: yeni "saved" durumu. */
    suspend fun toggleSave(programId: String): Result<Boolean>

    /** Kullanıcının programını feed'e paylaş. programId döner. */
    suspend fun shareMyProgram(
        originalProgramId: String,
        title: String,
        description: String?,
        tags: List<String>,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?
    ): Result<String>

    /** Paylaşılan programı uygulamak için snapshot'tan kopyala. Yeni program id döner. */
    suspend fun applyProgram(sharedProgramId: String): Result<String>

    /** Kullanıcının kendi paylaşımları — senkron durumu dahil. */
    suspend fun listMyShared(): Result<List<MySharedProgram>>

    /**
     * Paylaşım meta'sını günceller; [resyncSnapshot] true ise kaynak programdan
     * taze snapshot alır (kaynak silinmişse hata döner).
     */
    suspend fun updateShared(
        sharedId: String,
        title: String? = null,
        description: String? = null,
        tags: List<String>? = null,
        difficulty: String? = null,
        durationWeeks: Int? = null,
        daysPerWeek: Int? = null,
        resyncSnapshot: Boolean = true
    ): Result<Unit>

    /** Kullanıcının paylaşımını siler. */
    suspend fun deleteShared(sharedId: String): Result<Unit>
}
