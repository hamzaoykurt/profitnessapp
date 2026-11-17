package com.cosmibit.profitness.domain.ai

import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.store.UserPlan
import kotlinx.coroutines.flow.first

/**
 * AI özellikleri: sunucudaki `reserve_ai_usage` araç adı ([tool]) ve plan kotası bittiğinde düşen Enerji ([energyCost]).
 * Maliyetler `supabase/migrations/20260519193000_rebalance_energy_plan_hierarchy.sql` ile aynıdır (değişirse ikisi birlikte).
 * AI Koç sohbetinde Enerji her 10 mesajda 1 düşer (kota bittikten sonra).
 */
enum class AiFeature(val tool: AiToolType, val energyCost: Int, val minPlan: UserPlan) {
    CoachChat(AiToolType.ORACLE_CHAT, 1, UserPlan.PRO),
    CoachToProgram(AiToolType.ORACLE_TO_PROGRAM, 4, UserPlan.PRO),
    ProgramGenerate(AiToolType.PROGRAM_GENERATE_TEXT, 6, UserPlan.PRO),
    ProgramGenerateMedia(AiToolType.PROGRAM_GENERATE_MEDIA, 10, UserPlan.PRO),
    ProgramEdit(AiToolType.PROGRAM_EDIT, 4, UserPlan.PRO),
    WeightAnalysis(AiToolType.WEIGHT_TREND_ANALYSIS, 2, UserPlan.PRO),
    ExerciseAnalysis(AiToolType.EXERCISE_PROGRESS_ANALYSIS, 2, UserPlan.PRO),
    WorkoutAnalysis(AiToolType.WORKOUT_PROGRESS_ANALYSIS, 2, UserPlan.PRO);
}

/** İstemci tarafı AI erişim kararı (yalnız UI kilidi; gerçek yetki ve harcama sunucuda). */
sealed interface AiAccess {
    /** Kullanılabilir. [energyCost]: plan kotası bittiğinde düşecek Enerji. */
    data class Allowed(val energyCost: Int) : AiAccess
    /** Plan yetmiyor: [requiredPlan] ile açılır (Mağaza'ya yönlendir). */
    data class Locked(val requiredPlan: UserPlan) : AiAccess
}

/**
 * TEK KAYNAK: plan -> AI yetkisi.
 *  - FREE: AI yok (Kilo analizi, AI Koç sohbeti, AI program üretimi/düzenleme, hareket/antrenman AI analizi kilitli).
 *  - PRO: tüm AI üretim/analiz + AI Koç; aylık plan kotası, kota bitince Enerji.
 *  - ELITE: Pro + kişisel profil/hafıza + öncelikli işlem; daha yüksek kota.
 * Sunucu (reserve_ai_usage) gerçek yetkilendirme kaynağıdır; burası onu ÖNCEDEN gösterir, asla gevşetemez.
 */
object AiAccessPolicy {

    fun access(plan: UserPlan, feature: AiFeature): AiAccess =
        if (plan.ordinal >= feature.minPlan.ordinal) AiAccess.Allowed(feature.energyCost)
        else AiAccess.Locked(feature.minPlan)

    fun canUse(plan: UserPlan, feature: AiFeature): Boolean = access(plan, feature) is AiAccess.Allowed

    /** Free'de hiçbir AI özelliği yok. */
    fun hasAnyAi(plan: UserPlan): Boolean = AiFeature.values().any { canUse(plan, it) }

    /** Elite ayrıcalıkları (Mağaza metinleriyle uyumlu): kişisel profil/hafıza ve öncelik. */
    fun hasPersonalMemory(plan: UserPlan): Boolean = plan == UserPlan.ELITE
    fun hasPriority(plan: UserPlan): Boolean = plan == UserPlan.ELITE

    /**
     * Düğme altı maliyet satırı. Ücretli planda istemci kota kullanımını bilmez: "plan kotası, sonra N Enerji".
     * Örn. "Plan kotasından · sonra 2 Enerji · Kalan 12".
     */
    fun costLine(plan: UserPlan, feature: AiFeature, credits: Int, english: Boolean): String {
        val cost = if (feature == AiFeature.CoachChat)
            (if (english) "1 Energy per 10 messages" else "10 mesajda 1 Enerji")
        else (if (english) "${feature.energyCost} Energy" else "${feature.energyCost} Enerji")
        val left = if (english) "Left $credits" else "Kalan $credits"
        return when (access(plan, feature)) {
            is AiAccess.Locked -> if (english) "Pro or Elite plan required" else "Pro veya Elite plan gerekir"
            is AiAccess.Allowed -> if (english) "Plan quota · then $cost · $left" else "Plan kotasından · sonra $cost · $left"
        }
    }
}

/**
 * Tıklama anında istemci kilidi: plan BİLİNİYORSA (sunucu özeti yüklendi) ve yetmiyorsa true.
 * Plan henüz yüklenmediyse kilitlemez (Elite kullanıcıyı yanlışlıkla kilitlememek için); karar sunucuya kalır.
 */
suspend fun com.cosmibit.profitness.data.store.UserPlanRepository.isAiLocked(feature: AiFeature): Boolean {
    val s = billingSnapshotFlow.first()
    return s.isLoaded && !AiAccessPolicy.canUse(s.plan, feature)
}
