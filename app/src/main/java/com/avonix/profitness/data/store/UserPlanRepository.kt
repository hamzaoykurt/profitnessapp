package com.avonix.profitness.data.store

import kotlinx.coroutines.flow.Flow

enum class UserPlan(val displayName: String) {
    FREE("Ücretsiz"),
    PRO("Pro"),
    ELITE("Elite")
}

interface UserPlanRepository {
    /** Aktif plan değişikliklerini reaktif olarak yayar. */
    val planFlow: Flow<UserPlan>

    /** Kalan AI kredi değişikliklerini reaktif olarak yayar. */
    val creditsFlow: Flow<Int>

    /** Planı yükseltir; Pro/Elite → krediyi sıfırlar (artık sınırsız). */
    suspend fun upgradePlan(plan: UserPlan)

    /** Ücretli planı iptal edip FREE'ye geçer; başlangıç kredisini geri yükler. */
    suspend fun downgradeFree()

    /** Satın alınan krediyi mevcut bakiyeye ekler. */
    suspend fun addCredits(amount: Int)

    /**
     * Bir AI işlemi için kredi harcar.
     * - Pro/Elite plan → her zaman `true` döner (sınırsız).
     * - FREE plan + yeterli kredi → `true`, krediyi azaltır.
     * - FREE plan + kredi = 0 → `false` döner (UI paywall göstermeli).
     */
    suspend fun consumeCredit(): Boolean

    /**
     * AI çağrısı başarısız olduğunda tüketilen krediyi iade eder.
     * Pro/Elite plan → no-op (zaten sonsuz).
     */
    suspend fun refundCredit()

    companion object {
        /** Yeni FREE hesaplar bu kadar krediyle başlar. */
        const val FREE_STARTER_CREDITS = 5
    }
}
