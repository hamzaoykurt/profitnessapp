package com.cosmibit.profitness.presentation.weight

import com.cosmibit.profitness.domain.ai.isAiLocked
import androidx.lifecycle.viewModelScope
import com.cosmibit.profitness.core.BaseViewModel
import com.cosmibit.profitness.data.ai.AiAccessException
import com.cosmibit.profitness.data.ai.AiAnalysisPrompts
import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.ai.GeminiRepository
import com.cosmibit.profitness.data.ai.WeightProfileContext
import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import com.cosmibit.profitness.data.profile.ProfileRepository
import com.cosmibit.profitness.data.weight.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ── Domain: grafik için tek bir veri noktası ──────────────────────────────────

data class WeightPoint(
    val date    : LocalDate,
    val weightKg: Double
)

// ── Haftalık özet ─────────────────────────────────────────────────────────────

data class WeeklySummary(
    val thisWeekAvg : Double?,
    val lastWeekAvg : Double?,
    /** Pozitif = artış, negatif = düşüş, null = yeterli veri yok */
    val deltaKg     : Double?
)

// ── UI State ──────────────────────────────────────────────────────────────────

data class WeightTrackingState(
    val entries          : List<WeightLogEntity> = emptyList(),
    /** Son 90 gün — grafik veri noktaları (tarih-ağırlık çiftleri) */
    val chartPoints      : List<WeightPoint>     = emptyList(),
    val weeklySummary    : WeeklySummary         = WeeklySummary(null, null, null),
    /** Profil'de kayıtlı mevcut kilo (onboarding/edit'ten gelen) */
    val profileWeightKg  : Double                = 0.0,
    /** En son kayıt ağırlığı (null = henüz kayıt yok) */
    val latestWeightKg   : Double?               = null,
    /** İlk kayda göre toplam değişim */
    val totalDeltaKg     : Double?               = null,
    /** Gemini AI insight metni */
    val aiInsight        : String                = "",
    /** Gösterilen analiz son kayıtlardan ESKİ (kayıt eklendi/silindi): "Analizi güncelle" önerilir. */
    val aiInsightStale   : Boolean               = false,
    val isAiLoading      : Boolean               = false,
    val isLoading        : Boolean               = true,
    /** Add/Edit sheet görünürlüğü */
    val showSheet        : Boolean               = false,
    /** Düzenleme modunda seçili kayıt (null = yeni ekleme) */
    val editingEntry     : WeightLogEntity?      = null,
    /** Sheet'teki geçici girdi */
    val sheetWeightInput : String                = "",
    val sheetNoteInput   : String                = "",
    val isSaving         : Boolean               = false,
    val userPlan         : com.cosmibit.profitness.data.store.UserPlan = com.cosmibit.profitness.data.store.UserPlan.FREE,
    val aiCredits        : Int                   = com.cosmibit.profitness.data.store.UserPlanRepository.INITIAL_CREDITS_PLACEHOLDER
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed class WeightTrackingEvent {
    data class ShowSnackbar(val message: String) : WeightTrackingEvent()
    data object ShowPaywall : WeightTrackingEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WeightTrackingViewModel @Inject constructor(
    private val weightRepository : WeightRepository,
    private val profileRepository: ProfileRepository,
    private val geminiRepository : GeminiRepository,
    private val planRepository   : com.cosmibit.profitness.data.store.UserPlanRepository,
    private val supabase         : SupabaseClient,
    private val disk             : com.cosmibit.profitness.data.cache.DiskCache
) : BaseViewModel<WeightTrackingState, WeightTrackingEvent>(WeightTrackingState()) {

    /** Kalıcı AI analizi: ekran yeniden açılınca kredi harcamadan gösterilir; [signature] kayıtlar değişince eskir. */
    @kotlinx.serialization.Serializable
    data class CachedInsight(val signature: String, val text: String)

    private fun insightKey(userId: String) = "weight_ai_insight_$userId"
    private var cachedInsight: CachedInsight? = null
    private var cacheLoaded = false

    private var observeJob   : Job? = null
    private var aiInsightJob : Job? = null

    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id ?: supabase.auth.currentUserOrNull()?.id

    init {
        loadData()
        viewModelScope.launch {
            combine(planRepository.planFlow, planRepository.creditsFlow) { plan, credits -> plan to credits }
                .collect { (plan, credits) -> updateState { it.copy(userPlan = plan, aiCredits = credits) } }
        }
    }

    // ── İlk yükleme ───────────────────────────────────────────────────────────

    fun loadData() {
        viewModelScope.launch {
            val userId = currentUserId() ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            updateState { it.copy(isLoading = true) }
            var pulled = false

            // ROOM-FIRST: önce yerel kayıtları dinle (anında dolu), ağ çekişi arkadan gelir ve Flow'a yansır.
            observeJob?.cancel()
            observeJob = viewModelScope.launch {
                weightRepository.observeAll(userId)
                    .catch { /* DB hatası sessizce geç */ }
                    .collect { entries ->
                        val sorted    = entries.sortedByDescending { it.recordedAt }
                        val points    = buildChartPoints(sorted)
                        val weekly    = buildWeeklySummary(sorted)
                        val latest    = sorted.firstOrNull()?.weightKg
                        val oldest    = sorted.lastOrNull()?.weightKg
                        val totalDelta = if (latest != null && oldest != null && sorted.size >= 2)
                            latest - oldest else null

                        updateState {
                            it.copy(
                                entries       = sorted,
                                chartPoints   = points,
                                weeklySummary = weekly,
                                latestWeightKg= latest,
                                totalDeltaKg  = totalDelta,
                                // yerelde kayıt yoksa ağ cevabına kadar "yükleniyor" (boş durum yanıp sönmesin)
                                isLoading     = sorted.isEmpty() && !pulled
                            )
                        }

                        // AI analizi OTOMATİK BAŞLAMAZ (kredi): yalnız kullanıcı "AI ile analiz et"e basınca.
                        // Kayıtlı analiz varsa gösterilir; kayıtlar değiştiyse "eski" işaretlenir.
                        if (!cacheLoaded) {
                            cacheLoaded = true
                            cachedInsight = runCatching { disk.getOnIo<CachedInsight>(insightKey(userId)) }.getOrNull()
                        }
                        applyCachedInsight(sorted)
                    }
            }

            // Remote pull — ağ yoksa Room verisiyle devam eder
            weightRepository.pullFromRemote(userId)
            pulled = true
            val current = uiState.value.entries
            updateState { it.copy(isLoading = false) }
            applyCachedInsight(current)
        }
    }

    // ── Add / Edit ────────────────────────────────────────────────────────────

    fun openAddSheet() {
        val latest = uiState.value.latestWeightKg
        updateState {
            it.copy(
                showSheet        = true,
                editingEntry     = null,
                sheetWeightInput = latest?.toString() ?: "",
                sheetNoteInput   = ""
            )
        }
    }

    fun openEditSheet(entry: WeightLogEntity) {
        updateState {
            it.copy(
                showSheet        = true,
                editingEntry     = entry,
                sheetWeightInput = entry.weightKg.toString(),
                sheetNoteInput   = entry.note
            )
        }
    }

    fun closeSheet() = updateState { it.copy(showSheet = false, editingEntry = null) }

    fun onWeightInputChange(value: String) = updateState { it.copy(sheetWeightInput = value) }
    fun onNoteInputChange(value: String)   = updateState { it.copy(sheetNoteInput = value) }

    fun saveEntry() {
        val state  = uiState.value
        val rawKg  = state.sheetWeightInput.replace(",", ".").toDoubleOrNull()
        if (rawKg == null || rawKg <= 0.0 || rawKg > 500.0) {
            sendEvent(WeightTrackingEvent.ShowSnackbar("Geçerli bir ağırlık girin (örn. 75.5)"))
            return
        }

        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            updateState { it.copy(isSaving = true) }

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val entry = state.editingEntry?.copy(
                weightKg   = rawKg,
                note       = state.sheetNoteInput.trim(),
                synced     = false
            ) ?: WeightLogEntity(
                id         = UUID.randomUUID().toString(),
                userId     = userId,
                weightKg   = rawKg,
                note       = state.sheetNoteInput.trim(),
                recordedAt = now
            )

            val result = if (state.editingEntry == null)
                weightRepository.addEntry(entry)
            else
                weightRepository.updateEntry(entry)

            updateState { it.copy(isSaving = false, showSheet = false, editingEntry = null) }

            if (result.isSuccess) {
                sendEvent(WeightTrackingEvent.ShowSnackbar(
                    if (state.editingEntry == null) "Ağırlık kaydedildi" else "Kayıt güncellendi"
                ))
                // Veri değişti: analiz silinmez, Flow üzerinden "eski" işaretlenir (yeniden analiz kullanıcıya kalır)
            } else {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Kayıt başarısız. Lütfen tekrar deneyin."))
            }
        }
    }

    fun deleteEntry(entry: WeightLogEntity) {
        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            val result = weightRepository.deleteEntry(entry.id, userId)
            if (result.isSuccess) {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Kayıt silindi"))
            } else {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Silme başarısız"))
            }
        }
    }

    // ── AI Insight ────────────────────────────────────────────────────────────

    /**
     * Gemini'den kilo trendine göre kısa, kişisel analiz üretir.
     * Sonuç state ve [DiskCache]'e yazılır; sunucu tarafı `reserve_ai_usage` ile yetkilendirilir.
     */
    fun generateAiInsight(
        entries: List<WeightLogEntity> = uiState.value.entries,
        isEnglish: Boolean = false
    ) {
        if (entries.size < 2) return
        if (uiState.value.isAiLoading) return
        val previous = uiState.value.aiInsight
        updateState { it.copy(isAiLoading = true) }
        aiInsightJob = viewModelScope.launch {
            if (planRepository.isAiLocked(com.cosmibit.profitness.domain.ai.AiFeature.WeightAnalysis)) {
                updateState { it.copy(isAiLoading = false) }
                sendEvent(WeightTrackingEvent.ShowPaywall)
                return@launch
            }

            val userId = currentUserId()
            val profileContext = userId?.let { uid ->
                profileRepository.getProfile(uid).getOrNull()?.let { dto ->
                    val age = runCatching {
                        dto.birth_date?.takeIf { it.length >= 10 }?.let {
                            Period.between(LocalDate.parse(it.take(10)), LocalDate.now()).years.takeIf { y -> y in 8..100 }
                        }
                    }.getOrNull()
                    WeightProfileContext(
                        heightCm = dto.height_cm ?: 0.0,
                        fitnessGoal = dto.fitness_goal.orEmpty(),
                        gender = dto.gender.orEmpty(),
                        age = age
                    )
                }
            }

            val prompt = AiAnalysisPrompts.weightTrend(entries, profileContext, isEnglish)
            if (prompt == null) {
                updateState {
                    it.copy(
                        isAiLoading = false,
                        aiInsight = if (isEnglish) "At least two valid weight entries are required for trend analysis."
                        else "Trend analizi için en az iki geçerli kilo kaydı gerekiyor."
                    )
                }
                return@launch
            }

            val result = geminiRepository.chat(
                history      = emptyList(),
                userMessage  = prompt.userMessage,
                systemPrompt = prompt.systemPrompt,
                tool         = AiToolType.WEIGHT_TREND_ANALYSIS
            )

            val err = result.exceptionOrNull()
            if (err is AiAccessException) {
                updateState { it.copy(isAiLoading = false, aiInsight = previous) }
                if (err.requiresPaywall) {
                    sendEvent(WeightTrackingEvent.ShowPaywall)
                } else {
                    sendEvent(WeightTrackingEvent.ShowSnackbar(err.message.ifBlank {
                        if (isEnglish) "AI limit reached. Please try again shortly."
                        else "AI limitine ulaşıldı. Lütfen biraz sonra tekrar dene."
                    }))
                }
                return@launch
            }

            val text = result.getOrNull()?.trim().orEmpty()
            if (text.isBlank()) {
                updateState { it.copy(isAiLoading = false, aiInsight = previous) }
                sendEvent(WeightTrackingEvent.ShowSnackbar(
                    if (isEnglish) "Analysis could not be completed right now. Please try again."
                    else "Analiz şu an yapılamadı. Biraz sonra tekrar dene."
                ))
                return@launch
            }
            val saved = CachedInsight(insightSignature(entries), text)
            cachedInsight = saved
            if (userId != null) runCatching { disk.putOnIo(insightKey(userId), saved) }
            updateState { it.copy(aiInsight = text, aiInsightStale = saved.signature != insightSignature(it.entries), isAiLoading = false) }
        }
    }

    private fun applyCachedInsight(sorted: List<WeightLogEntity>) {
        if (uiState.value.isAiLoading) return
        val c = cachedInsight ?: return
        updateState { it.copy(aiInsight = c.text, aiInsightStale = c.signature != insightSignature(sorted)) }
    }

    // ── Yardımcı hesaplamalar ─────────────────────────────────────────────────

    private fun buildChartPoints(sorted: List<WeightLogEntity>): List<WeightPoint> {
        val cutoff = LocalDate.now().minusDays(89)
        return sorted
            .mapNotNull { entry ->
                val date = runCatching { LocalDate.parse(entry.recordedAt.take(10)) }.getOrNull()
                    ?: return@mapNotNull null
                if (date < cutoff) return@mapNotNull null
                WeightPoint(date, entry.weightKg)
            }
            .sortedBy { it.date }   // grafik soldakinden sağa doğru
    }

    private fun buildWeeklySummary(sorted: List<WeightLogEntity>): WeeklySummary {
        val today      = LocalDate.now()
        val thisMonday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val lastMonday = thisMonday.minusWeeks(1)

        fun avg(from: LocalDate, to: LocalDate): Double? {
            val vals = sorted.mapNotNull { entry ->
                val d = runCatching { LocalDate.parse(entry.recordedAt.take(10)) }.getOrNull()
                    ?: return@mapNotNull null
                if (d in from..to) entry.weightKg else null
            }
            return if (vals.isNotEmpty()) vals.average() else null
        }

        val thisAvg = avg(thisMonday, today)
        val lastAvg = avg(lastMonday, thisMonday.minusDays(1))
        val delta   = if (thisAvg != null && lastAvg != null) thisAvg - lastAvg else null

        return WeeklySummary(thisAvg, lastAvg, delta)
    }

}

/** Analiz geçerlilik imzası: tüm kayıtların sıralı id, değer, zaman ve not özeti. */
internal fun insightSignature(entries: List<WeightLogEntity>): String =
    entries
        .sortedWith(compareBy<WeightLogEntity> { it.recordedAt }.thenBy { it.id })
        .joinToString(";") { "${it.id}|${it.weightKg}|${it.recordedAt}|${it.note.trim()}" }
