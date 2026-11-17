package com.cosmibit.profitness.domain.ai

import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.data.store.UserPlanRepository
import kotlinx.coroutines.flow.first

/**
 * AI özellikleri ve sunucudaki `reserve_ai_usage` araç adı ([tool]).
 * [freeGrant]: Ücretsiz planda ömür boyu [UserPlanRepository.FREE_AI_PROGRAM_GRANTS] hak ile açılır
 * (AI ile program oluşturma / rastgele bir programı yapıştırıp içe aktarma). Diğerleri yalnız Premium.
 */
enum class AiFeature(val tool: AiToolType, val freeGrant: Boolean) {
    CoachChat(AiToolType.ORACLE_CHAT, false),
    CoachToProgram(AiToolType.ORACLE_TO_PROGRAM, false),
    ProgramGenerate(AiToolType.PROGRAM_GENERATE_TEXT, true),
    ProgramGenerateMedia(AiToolType.PROGRAM_GENERATE_MEDIA, true),
    ProgramEdit(AiToolType.PROGRAM_EDIT, false),
    WeightAnalysis(AiToolType.WEIGHT_TREND_ANALYSIS, false),
    ExerciseAnalysis(AiToolType.EXERCISE_PROGRESS_ANALYSIS, false),
    WorkoutAnalysis(AiToolType.WORKOUT_PROGRESS_ANALYSIS, false);
}

/** İstemci tarafı AI erişim kararı (yalnız UI kilidi; gerçek yetki ve sayaç sunucuda). */
sealed interface AiAccess {
    /** Premium: açık (adil kullanım sınırı sunucuda). */
    data object Unlimited : AiAccess
    /** Ücretsiz hak ile açık. [left]: kalan hak (null = sunucu henüz bildirmiyor). */
    data class FreeGrant(val left: Int?) : AiAccess
    /** Premium gerekir (Mağaza'ya yönlendir). */
    data object Locked : AiAccess
}

/**
 * TEK KAYNAK: plan -> AI yetkisi (sade model: ÜCRETSİZ + PREMIUM, Enerji/kredi yok).
 *  - FREE: AI-dışı her şey açık; AI'dan yalnız program oluşturma/içe aktarma, ömür boyu 2 hak. AI Koç, AI düzenleme,
 *    Kilo/hareket/antrenman AI analizi kilitli.
 *  - PREMIUM: tüm AI (AI Koç, sınırsız program üretimi/düzenleme, analizler, kişisel hafıza, öncelik); sunucuda adil kullanım tavanı.
 * Sunucu gerçek yetkilendirme kaynağıdır; burası onu önceden gösterir, asla gevşetemez.
 */
object AiAccessPolicy {

    fun access(plan: UserPlan, feature: AiFeature, freeProgramsLeft: Int? = null): AiAccess = when {
        plan == UserPlan.PREMIUM -> AiAccess.Unlimited
        feature.freeGrant && (freeProgramsLeft == null || freeProgramsLeft > 0) -> AiAccess.FreeGrant(freeProgramsLeft)
        else -> AiAccess.Locked
    }

    fun canUse(plan: UserPlan, feature: AiFeature, freeProgramsLeft: Int? = null): Boolean =
        access(plan, feature, freeProgramsLeft) !is AiAccess.Locked

    /** Premium ayrıcalıkları (eski Elite özellikleri dahil, tek paket). */
    fun hasPersonalMemory(plan: UserPlan): Boolean = plan == UserPlan.PREMIUM
    fun hasPriority(plan: UserPlan): Boolean = plan == UserPlan.PREMIUM

    /** Ücretsiz hak satırı: "2 ücretsiz AI program hakkın var" / "1 hakkın kaldı" / "Ücretsiz hakların bitti". */
    fun freeGrantLine(left: Int?, english: Boolean): String = when {
        left == null -> if (english) "Free plan: ${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} AI programs included"
                        else "Ücretsiz planda ${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} AI program hakkı"
        left <= 0 -> if (english) "Your free AI programs are used up" else "Ücretsiz AI program hakların bitti"
        left >= UserPlanRepository.FREE_AI_PROGRAM_GRANTS ->
            if (english) "You have $left free AI programs" else "$left ücretsiz AI program hakkın var"
        else -> if (english) "$left free AI program left" else "$left hakkın kaldı"
    }
}

/**
 * Tıklama anında istemci kilidi: plan BİLİNİYORSA (sunucu özeti yüklendi) ve yetmiyorsa true.
 * Plan henüz yüklenmediyse kilitlemez (Premium kullanıcıyı yanlışlıkla kilitlememek için); karar sunucuya kalır.
 */
suspend fun UserPlanRepository.isAiLocked(feature: AiFeature): Boolean {
    val s = billingSnapshotFlow.first()
    return s.isLoaded && !AiAccessPolicy.canUse(s.plan, feature, s.freeAiProgramsLeft)
}
