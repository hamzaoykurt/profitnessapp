package com.avonix.profitness.presentation.weight

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.local.entity.WeightLogEntity
import com.avonix.profitness.data.weight.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
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
    val isAiLoading      : Boolean               = false,
    val isLoading        : Boolean               = true,
    /** Add/Edit sheet görünürlüğü */
    val showSheet        : Boolean               = false,
    /** Düzenleme modunda seçili kayıt (null = yeni ekleme) */
    val editingEntry     : WeightLogEntity?      = null,
    /** Sheet'teki geçici girdi */
    val sheetWeightInput : String                = "",
    val sheetNoteInput   : String                = "",
    val isSaving         : Boolean               = false
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed class WeightTrackingEvent {
    data class ShowSnackbar(val message: String) : WeightTrackingEvent()
    data object ShowPaywall : WeightTrackingEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WeightTrackingViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val geminiRepository : GeminiRepository,
    private val planRepository   : com.avonix.profitness.data.store.UserPlanRepository,
    private val supabase         : SupabaseClient
) : BaseViewModel<WeightTrackingState, WeightTrackingEvent>(WeightTrackingState()) {

    private var observeJob   : Job? = null
    private var aiInsightJob : Job? = null

    init { loadData() }

    // ── İlk yükleme ───────────────────────────────────────────────────────────

    fun loadData() {
        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            updateState { it.copy(isLoading = true) }

            // Remote pull — ağ yoksa Room verisiyle devam eder
            weightRepository.pullFromRemote(userId)

            // Room Flow'u dinle — kayıt ekleme/silme/güncelleme anında yansır
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
                                isLoading     = false
                            )
                        }

                        // Yeterli veri varsa ve daha önce AI insight yoksa üret
                        if (sorted.size >= 2 && uiState.value.aiInsight.isBlank()) {
                            generateAiInsight(sorted)
                        }
                    }
            }
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
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
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
                // Veri değişince AI insight yenile
                aiInsightJob?.cancel()
                updateState { it.copy(aiInsight = "") }
            } else {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Kayıt başarısız. Lütfen tekrar deneyin."))
            }
        }
    }

    fun deleteEntry(entry: WeightLogEntity) {
        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            val result = weightRepository.deleteEntry(entry.id, userId)
            if (result.isSuccess) {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Kayıt silindi"))
                aiInsightJob?.cancel()
                updateState { it.copy(aiInsight = "") }
            } else {
                sendEvent(WeightTrackingEvent.ShowSnackbar("Silme başarısız"))
            }
        }
    }

    // ── AI Insight ────────────────────────────────────────────────────────────

    /**
     * Gemini'den kilo trendine göre kısa, kişisel analiz üretir.
     * Sonuç state'e yazılır; ViewModel hayatta olduğu sürece cache'lenir.
     */
    fun generateAiInsight(entries: List<WeightLogEntity> = uiState.value.entries) {
        if (entries.size < 2) return
        aiInsightJob?.cancel()
        aiInsightJob = viewModelScope.launch {
            if (!planRepository.consumeCredit()) {
                sendEvent(WeightTrackingEvent.ShowPaywall)
                return@launch
            }
            updateState { it.copy(isAiLoading = true, aiInsight = "") }

            // Son 30 kaydı AI'a gönder (token limiti gözetilerek)
            val recent = entries.take(30)
            val dataStr = recent.joinToString("; ") { entry ->
                val date = entry.recordedAt.take(10)   // "YYYY-MM-DD"
                "$date: ${entry.weightKg} kg"
            }

            val latest   = recent.first().weightKg
            val oldest   = recent.last().weightKg
            val deltaKg  = latest - oldest
            val deltaSign= if (deltaKg >= 0) "+${"%.1f".format(deltaKg)}" else "${"%.1f".format(deltaKg)}"
            val daySpan  = recent.size.coerceAtLeast(1)

            val systemPrompt = """
                Sen kişisel bir fitness koçusun. Kullanıcının kilo takip verilerini analiz edip
                kısa, motive edici ve bilgilendirici Türkçe yorumlar yapıyorsun.
                Yanıtın 2-4 cümle olsun. Sağlıklı kilo değişim hızını (0.5-1 kg/hafta) göz önünde bulundur.
                Asla spesifik tıbbi tavsiye verme.
            """.trimIndent()

            val userMessage = """
                Kilo geçmişim ($daySpan kayıt):
                $dataStr

                Toplam değişim: $deltaSign kg
                Mevcut kilo: $latest kg

                Lütfen bu trendi kısaca yorumla ve motivasyon ver.
            """.trimIndent()

            val result = geminiRepository.chat(
                history      = emptyList(),
                userMessage  = userMessage,
                systemPrompt = systemPrompt
            )

            updateState { st ->
                st.copy(
                    aiInsight   = result.getOrElse { planRepository.refundCredit(); "Trend analizi şu an mevcut değil." },
                    isAiLoading = false
                )
            }
        }
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
