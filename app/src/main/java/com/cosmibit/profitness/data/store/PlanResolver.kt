package com.cosmibit.profitness.data.store

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime

/**
 * İstemci savunması: ETKİN plan. Sunucu (billing-status) süresi geçmiş aboneliği hâlâ "active" dönebiliyor
 * (bkz. supabase/proposals/20261118_entitlement_expiry.sql); burada da aynı kural uygulanır:
 * FREE  <- plan FREE/bilinmiyor, status active/trialing değil, ya da currentPeriodEnd geçmiş.
 * Eski "PRO"/"ELITE" -> PREMIUM ([UserPlan.fromServer]).
 */
object PlanResolver {
    fun effectivePlan(planRaw: String?, status: String?, periodEndIso: String?, now: Instant = Instant.now()): UserPlan {
        val plan = UserPlan.fromServer(planRaw) ?: return UserPlan.FREE
        if (plan == UserPlan.FREE) return UserPlan.FREE
        if (status?.lowercase() !in setOf("active", "trialing")) return UserPlan.FREE
        val end = periodEndIso?.let(::parseInstant)
        if (end != null && end.isBefore(now)) return UserPlan.FREE
        return plan
    }

    /** Süre bittiyse ekranda gösterilecek durum. */
    fun effectiveStatus(status: String?, periodEndIso: String?, now: Instant = Instant.now()): String {
        val end = periodEndIso?.let(::parseInstant)
        return if (end != null && end.isBefore(now) && status?.lowercase() in setOf("active", "trialing")) "expired"
        else status ?: "free"
    }

    private fun parseInstant(s: String): Instant? =
        runCatching { OffsetDateTime.parse(s).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(s) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(s.replace(" ", "T")).toInstant() }.getOrNull()
}

/** cancel_sandbox_plan RPC sonucu. */
sealed interface CancelPlanResult {
    /** Ücretsiz plana geçildi (ya da zaten Ücretsiz). */
    data class Canceled(val previousSource: String?) : CancelPlanResult
    /** Gerçek abonelik: mağazadan (Google Play) yönetilir. */
    data object ManageInStore : CancelPlanResult
    data class Failed(val reason: String) : CancelPlanResult

    companion object {
        /** {ok, plan, previousSource} | {ok:false, reason} ayrıştırma (saf; testli). */
        fun parse(json: JsonObject): CancelPlanResult {
            val ok = json["ok"]?.jsonPrimitive?.booleanOrNull == true
            val reason = json["reason"]?.jsonPrimitive?.contentOrNull
            val source = json["previousSource"]?.jsonPrimitive?.contentOrNull
            return when {
                ok -> Canceled(source)
                reason == "manage_in_store" -> ManageInStore
                else -> Failed(reason ?: "unknown")
            }
        }
    }
}
