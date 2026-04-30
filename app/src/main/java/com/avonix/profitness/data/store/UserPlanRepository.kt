package com.avonix.profitness.data.store

import kotlinx.coroutines.flow.Flow

enum class UserPlan(val displayName: String) {
    FREE("Ücretsiz"),
    PRO("Pro"),
    ELITE("Elite")
}

data class CheckoutResult(
    val orderId: String?,
    val status: String,
    val checkoutUrl: String?,
    val message: String,
    val sandboxAvailable: Boolean = false
)

data class BillingProduct(
    val sku: String,
    val kind: String,
    val plan: UserPlan?,
    val creditAmount: Int,
    val title: String,
    val priceLabel: String,
    val badge: String? = null,
    val billingPeriod: String? = null
)

data class BillingUsage(
    val tool: String,
    val status: String,
    val source: String,
    val creditCost: Int,
    val createdAt: String
)

data class BillingSnapshot(
    val plan: UserPlan = UserPlan.FREE,
    val status: String = "free",
    val credits: Int = UserPlanRepository.FREE_STARTER_CREDITS,
    val products: List<BillingProduct> = emptyList(),
    val recentUsage: List<BillingUsage> = emptyList()
)

interface UserPlanRepository {
    val billingSnapshotFlow: Flow<BillingSnapshot>

    /** Aktif plan değişikliklerini reaktif olarak yayar. */
    val planFlow: Flow<UserPlan>

    /** Kalan AI kredi değişikliklerini reaktif olarak yayar. */
    val creditsFlow: Flow<Int>

    /** Server'dan aktif plan/kredi özetini yeniler. */
    suspend fun refresh()

    /** Plan checkout kaydı oluşturur; ödeme doğrulanmadan planı hesaba yazmaz. */
    suspend fun upgradePlan(plan: UserPlan, yearly: Boolean = false): CheckoutResult

    /** Ödeme sağlayıcısı bağlanana kadar client tarafında iptal mutasyonu yapılmaz. */
    suspend fun downgradeFree()

    /** Kredi checkout kaydı oluşturur; ödeme doğrulanmadan kredi eklemez. */
    suspend fun addCredits(amount: Int): CheckoutResult

    /** Test ortamında pending siparişi sandbox ödeme gibi tamamlar. */
    suspend fun completeSandboxCheckout(orderId: String): CheckoutResult

    /**
     * Eski UI kapıları için sadece güvenli ön kontrol.
     * Gerçek harcama ve limit kontrolü Supabase Edge Function içinde yapılır.
     */
    suspend fun consumeCredit(): Boolean

    /** Gerçek iade server tarafında yapılır; client no-op. */
    suspend fun refundCredit()

    companion object {
        /** Yeni FREE hesaplar bu kadar krediyle başlar. */
        const val FREE_STARTER_CREDITS = 5
    }
}
