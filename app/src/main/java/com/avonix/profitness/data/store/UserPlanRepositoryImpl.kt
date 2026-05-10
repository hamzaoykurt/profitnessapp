package com.avonix.profitness.data.store

import android.content.Context
import com.avonix.profitness.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val BILLING_STATUS_FUNCTION = "billing-status"
private const val BILLING_CHECKOUT_FUNCTION = "billing-checkout"
private const val BILLING_SANDBOX_COMPLETE_FUNCTION = "billing-sandbox-complete"

@Singleton
class UserPlanRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) : UserPlanRepository {

    private val jsonConfig = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) { json(jsonConfig) }
    }

    private val statusState = MutableStateFlow(BillingSnapshot())

    override val billingSnapshotFlow: Flow<BillingSnapshot> = statusState

    override val planFlow: Flow<UserPlan> = statusState.map { it.plan }

    override val creditsFlow: Flow<Int> = statusState.map { it.credits.coerceAtLeast(0) }

    override suspend fun refresh() {
        val status = invokeBillingStatus()
        statusState.value = status.toDomain()
    }

    override suspend fun upgradePlan(plan: UserPlan, yearly: Boolean): CheckoutResult {
        require(plan != UserPlan.FREE) { "Free plan checkout is not supported." }
        val suffix = if (yearly) "yearly" else "monthly"
        val sku = when (plan) {
            UserPlan.PRO -> "sub_pro_$suffix"
            UserPlan.ELITE -> "sub_elite_$suffix"
            UserPlan.FREE -> error("Unsupported plan")
        }
        return startCheckout(sku)
    }

    override suspend fun downgradeFree() {
        refresh()
    }

    override suspend fun addCredits(amount: Int): CheckoutResult {
        val sku = when (amount) {
            10 -> "credits_10"
            50 -> "credits_50"
            200 -> "credits_200"
            else -> error("Desteklenmeyen kredi paketi.")
        }
        return startCheckout(sku)
    }

    override suspend fun completeSandboxCheckout(orderId: String): CheckoutResult {
        val dto: CheckoutResponseDto = postEdge(
            BILLING_SANDBOX_COMPLETE_FUNCTION,
            SandboxCompleteRequestDto(orderId)
        )
        refresh()
        return CheckoutResult(
            orderId = orderId,
            status = dto.status.ifBlank { "paid" },
            checkoutUrl = null,
            message = dto.message,
            sandboxAvailable = false
        )
    }

    override suspend fun consumeCredit(): Boolean {
        runCatching { refresh() }
        return true
    }

    override suspend fun refundCredit() {
        refresh()
    }

    private suspend fun invokeBillingStatus(): BillingStatusDto =
        postEdge(BILLING_STATUS_FUNCTION, EmptyBody())

    private suspend fun startCheckout(sku: String): CheckoutResult {
        val dto: CheckoutResponseDto = postEdge(BILLING_CHECKOUT_FUNCTION, CheckoutRequestDto(sku))
        refresh()
        return CheckoutResult(
            orderId = dto.orderId,
            status = dto.status,
            checkoutUrl = dto.checkoutUrl,
            message = dto.message,
            sandboxAvailable = dto.sandboxAvailable
        )
    }

    private suspend inline fun <reified Req : Any, reified Res> postEdge(
        functionName: String,
        body: Req
    ): Res {
        val accessToken = supabase.auth.currentSessionOrNull()?.accessToken
            ?: error("Satın alma ve AI limitleri için giriş yapmanız gerekiyor.")

        val response = httpClient.post(edgeUrl(functionName)) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<EdgeErrorDto>() }.getOrNull()
            error(errorBody?.message ?: "Satın alma altyapısı şu anda yanıt vermiyor.")
        }
        return response.body()
    }

    private fun edgeUrl(functionName: String): String =
        "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$functionName"

    @Serializable
    private class EmptyBody

    @Serializable
    private data class BillingStatusDto(
        val plan: String,
        val status: String,
        val credits: Int,
        val sandboxAvailable: Boolean = false,
        val products: List<BillingProductDto> = emptyList(),
        val recentUsage: List<BillingUsageDto> = emptyList()
    ) {
        fun toDomain(): BillingSnapshot = BillingSnapshot(
            plan = runCatching { UserPlan.valueOf(plan) }.getOrDefault(UserPlan.FREE),
            status = status,
            credits = credits,
            sandboxAvailable = sandboxAvailable,
            products = products.map { it.toDomain() },
            recentUsage = recentUsage.map { it.toDomain() }
        )
    }

    @Serializable
    private data class BillingProductDto(
        val sku: String,
        val kind: String,
        val plan: String? = null,
        val credit_amount: Int = 0,
        val title: String,
        val price_label: String,
        val metadata: ProductMetadataDto = ProductMetadataDto()
    ) {
        fun toDomain(): BillingProduct = BillingProduct(
            sku = sku,
            kind = kind,
            plan = plan?.let { runCatching { UserPlan.valueOf(it) }.getOrNull() },
            creditAmount = credit_amount,
            title = title,
            priceLabel = price_label,
            badge = metadata.badge,
            billingPeriod = metadata.billing_period
        )
    }

    @Serializable
    private data class ProductMetadataDto(
        val badge: String? = null,
        val billing_period: String? = null
    )

    @Serializable
    private data class BillingUsageDto(
        val tool: String,
        val status: String,
        val entitlement_source: String,
        val credit_cost: Int,
        val created_at: String
    ) {
        fun toDomain(): BillingUsage = BillingUsage(
            tool = tool,
            status = status,
            source = entitlement_source,
            creditCost = credit_cost,
            createdAt = created_at
        )
    }

    @Serializable
    private data class CheckoutRequestDto(
        val sku: String
    )

    @Serializable
    private data class SandboxCompleteRequestDto(
        val orderId: String
    )

    @Serializable
    private data class CheckoutResponseDto(
        val orderId: String? = null,
        val status: String = "",
        val checkoutUrl: String? = null,
        val message: String,
        val sandboxAvailable: Boolean = false
    )

    @Serializable
    private data class EdgeErrorDto(
        val code: String? = null,
        val message: String? = null
    )
}
