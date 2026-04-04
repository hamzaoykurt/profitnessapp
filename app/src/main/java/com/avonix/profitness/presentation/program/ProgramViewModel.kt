package com.avonix.profitness.presentation.program

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.program.ManualDayInput
import com.avonix.profitness.data.program.ManualExerciseInput
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ProgramUiState(
    val isLoading    : Boolean          = false,
    val error        : String?          = null,
    val userPrograms : List<Program>    = emptyList(),
    val exercises    : List<ExerciseItem> = emptyList(),
    // AI builder
    val aiLoading    : Boolean          = false,
    val aiError      : String?          = null
)

sealed class ProgramEvent {
    data class ShowSnackbar(val message: String) : ProgramEvent()
    object NavigateBack : ProgramEvent()
}

data class ManualDayDraft(
    val title             : String                  = "",
    val isRestDay         : Boolean                 = false,
    val selectedExercises : List<ManualExerciseInput> = emptyList()
)

@HiltViewModel
class ProgramViewModel @Inject constructor(
    private val programRepository : ProgramRepository,
    private val geminiRepository  : GeminiRepository,
    private val supabase          : SupabaseClient
) : BaseViewModel<ProgramUiState, ProgramEvent>(ProgramUiState()) {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    private var lastProgramLoadMs = 0L

    init {
        loadUserPrograms()
        loadExercises()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /** Tab geçişleri için — 3 dakika geçmediyse ve programlar varsa atla */
    fun reloadIfStale() {
        if (System.currentTimeMillis() - lastProgramLoadMs < 3 * 60_000L &&
            uiState.value.userPrograms.isNotEmpty()) return
        loadUserPrograms()
    }

    fun loadUserPrograms() {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = uiState.value.userPrograms.isEmpty()) }
            programRepository.getUserPrograms(uid)
                .onSuccess { programs ->
                    lastProgramLoadMs = System.currentTimeMillis()
                    updateState { it.copy(isLoading = false, userPrograms = programs) }
                }
                .onFailure {
                    updateState { it.copy(isLoading = false) }
                }
        }
    }

    fun loadExercises() {
        viewModelScope.launch {
            programRepository.getAllExercises()
                .onSuccess { list -> updateState { it.copy(exercises = list) } }
        }
    }

    // ── AI Program Oluşturma ──────────────────────────────────────────────────

    /**
     * [imageBase64] ve [mimeType] opsiyoneldir. Sağlanırsa Gemini görsel/PDF üzerinden programı çıkarır.
     * Listede olmayan egzersizler otomatik olarak veritabanına eklenir.
     */
    fun createFromAI(
        userPrompt: String,
        imageBase64: String? = null,
        mimeType: String? = null
    ) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        val hasMedia = imageBase64 != null && mimeType != null
        if (userPrompt.isBlank() && !hasMedia) {
            sendEvent(ProgramEvent.ShowSnackbar("Lütfen bir açıklama girin veya dosya yükleyin."))
            return
        }

        viewModelScope.launch {
            updateState { it.copy(aiLoading = true, aiError = null) }

            // 1. Egzersiz listesini hazırla
            val baseExercises = uiState.value.exercises.ifEmpty {
                programRepository.getAllExercises().getOrNull() ?: emptyList()
            }
            val exerciseList = baseExercises.take(120).joinToString(", ") { ex ->
                if (ex.nameEn.isNotBlank() && ex.nameEn != ex.name)
                    "${ex.name} (${ex.nameEn})" else ex.name
            }

            // 2. Metin tabanlı dosyalar (HTML, TXT vb.) inline_data yerine text olarak gönderilmeli
            val isTextFile = mimeType?.startsWith("text/") == true
            var textFileContent: String? = null
            var effectiveBase64 = imageBase64
            var effectiveMime = mimeType
            if (isTextFile && imageBase64 != null) {
                textFileContent = try {
                    String(android.util.Base64.decode(imageBase64, android.util.Base64.NO_WRAP), Charsets.UTF_8)
                } catch (_: Exception) { null }
                // Text dosyaları inline_data olarak gönderilemez, prompt'a eklenecek
                effectiveBase64 = null
                effectiveMime = null
            }
            val effectiveHasMedia = effectiveBase64 != null && effectiveMime != null

            // 3. Gemini prompt
            val userInstruction = if (userPrompt.isNotBlank()) "\n\nKullanıcının ek talimatı: $userPrompt" else ""

            val mediaAnalysisBlock = when {
                textFileContent != null -> """
Aşağıdaki dosya içeriğini analiz et ve içindeki antrenman programını aynen çıkar.
Her egzersizin set, tekrar ve dinlenme sürelerini dosyada yazdığı gibi koru, değiştirme.

--- DOSYA İÇERİĞİ BAŞLANGIÇ ---
$textFileContent
--- DOSYA İÇERİĞİ BİTİŞ ---
$userInstruction"""

                effectiveHasMedia -> """
Yüklenen görseli/PDF'i dikkatle analiz et ve içindeki antrenman programını eksiksiz çıkar.
KRİTİK: Her egzersizin set sayısı, tekrar sayısı ve dinlenme süresini dosyada/görselde yazdığı gibi aynen aktar. Hiçbir değeri tahmin etme veya değiştirme.
$userInstruction"""

                else -> "Kullanıcının istediği antrenman programı: $userPrompt"
            }

            val geminiPrompt = """
$mediaAnalysisBlock

Mevcut egzersiz listesi (önce buradan seç, tam adı kullan):
$exerciseList

Kural: Listede olmayan bir egzersiz kullanman gerekirse, o egzersiz için "targetMuscle" (Göğüs/Sırt/Omuz/Bacak/Kol/Karın/Genel) ve "category" (Serbest Ağırlık/Makine/Kardiyo/Vücut Ağırlığı) alanlarını mutlaka ekle.

ÇIKTI KURALI: Yalnızca geçerli JSON döndür. Markdown, açıklama, kod bloğu YASAK.
FORMAT:
{"name":"...","days":[{"title":"Gün 1 - Göğüs","isRestDay":false,"exercises":[{"exerciseName":"Bench Press","sets":4,"reps":10,"restSeconds":60,"targetMuscle":"Göğüs","category":"Serbest Ağırlık"}]},{"title":"Gün 2 - Dinlenme","isRestDay":true,"exercises":[]}]}
            """.trimIndent()

            val systemPrompt = "Sen bir fitness programı oluşturucusun. Dosya veya görsel verildiğinde içeriği titizlikle analiz et ve set/tekrar/dinlenme değerlerini orijinal kaynaktaki gibi aynen aktar. SADECE ham JSON döndür, başka hiçbir şey yazma. Markdown veya kod bloğu kullanma."

            val result = if (effectiveHasMedia) {
                geminiRepository.chatWithMedia(effectiveBase64!!, effectiveMime!!, geminiPrompt, systemPrompt)
            } else {
                geminiRepository.chat(emptyList(), geminiPrompt, systemPrompt)
            }

            val rawJson = result.getOrNull()
            if (rawJson == null) {
                updateState { it.copy(aiLoading = false, aiError = "Bağlantı hatası: ${result.exceptionOrNull()?.message}") }
                return@launch
            }

            // 3. JSON'u çıkar ve parse et (markdown kod bloğu olsa bile yakala)
            val cleaned = rawJson
                .replace(Regex("```[a-zA-Z]*\\s*"), "")  // ```json veya ``` başlıklarını sil
                .replace("```", "")
                .trim()
            val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(cleaned)?.value
            if (jsonCandidate == null) {
                updateState { it.copy(aiLoading = false, aiError = "Geçersiz yanıt, tekrar dene.") }
                return@launch
            }

            val rootObj = runCatching { jsonParser.parseToJsonElement(jsonCandidate).jsonObject }.getOrNull()
            if (rootObj == null) {
                updateState { it.copy(aiLoading = false, aiError = "Program ayrıştırılamadı, tekrar dene.") }
                return@launch
            }

            val programName = rootObj["name"]?.jsonPrimitive?.contentOrNull ?: "Oracle Programı"
            val daysArray   = rootObj["days"] as? JsonArray
            if (daysArray == null) {
                updateState { it.copy(aiLoading = false, aiError = "Program günleri bulunamadı.") }
                return@launch
            }

            // 4. Egzersiz eşleştirme — mutable map, yeni eklenenler de aranabilir
            val currentMap   = baseExercises.associateBy { it.name.trim().lowercase() }.toMutableMap()
            val currentMapEn = baseExercises.filter { it.nameEn.isNotBlank() }
                .associateBy { it.nameEn.trim().lowercase() }.toMutableMap()

            fun findExercise(aiName: String): ExerciseItem? {
                val key = aiName.trim().lowercase()
                currentMap[key]?.let { return it }
                currentMapEn[key]?.let { return it }
                currentMap.entries.firstOrNull { it.key.contains(key) || key.contains(it.key) }?.value?.let { return it }
                currentMapEn.entries.firstOrNull { it.key.contains(key) || key.contains(it.key) }?.value?.let { return it }
                val words = key.split(" ", "-").filter { it.length > 2 }.toSet()
                if (words.size >= 2) {
                    currentMap.entries.firstOrNull { (dbKey, _) ->
                        val dbWords = dbKey.split(" ", "-").filter { it.length > 2 }.toSet()
                        (words intersect dbWords).size >= 2
                    }?.value?.let { return it }
                }
                return null
            }

            val days = daysArray.map { dayEl ->
                val dayObj = dayEl.jsonObject
                val title  = dayObj["title"]?.jsonPrimitive?.contentOrNull ?: "Gün"
                val isRest = dayObj["isRestDay"]?.jsonPrimitive?.booleanOrNull ?: false

                if (isRest) {
                    ManualDayInput(title = title, isRestDay = true)
                } else {
                    val exArray = dayObj["exercises"] as? JsonArray
                    val matched = exArray?.mapIndexedNotNull { exIdx, exEl ->
                        val exObj  = exEl.jsonObject
                        val exName = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                        val sets   = flexInt(exObj, "sets", 3)
                        val reps   = flexInt(exObj, "reps", 10)
                        val rest   = flexInt(exObj, "restSeconds", 90)
                        val targetMuscle = exObj["targetMuscle"]?.jsonPrimitive?.contentOrNull ?: "Genel"
                        val category     = exObj["category"]?.jsonPrimitive?.contentOrNull ?: "Serbest Ağırlık"

                        var exercise = findExercise(exName)

                        // Bulunamadıysa veritabanına ekle — diğer kullanıcılar da erişebilsin
                        if (exercise == null) {
                            exercise = programRepository.addExercise(
                                name         = exName,
                                nameEn       = "",
                                targetMuscle = targetMuscle,
                                category     = category,
                                setsDefault  = sets,
                                repsDefault  = reps
                            ).getOrNull()
                            exercise?.let { newEx ->
                                currentMap[newEx.name.trim().lowercase()] = newEx
                            }
                        }

                        exercise?.let { ManualExerciseInput(it.id, sets, reps, rest, exIdx) }
                    } ?: emptyList()
                    ManualDayInput(title = title, isRestDay = false, exercises = matched)
                }
            }

            // 5. Programı oluştur
            programRepository.createManual(uid, programName, days)
                .onSuccess { program ->
                    // exercises listesini de güncelle (yeni egzersizler dahil)
                    programRepository.getAllExercises().getOrNull()?.let { updated ->
                        updateState { it.copy(exercises = updated) }
                    }
                    updateState { state ->
                        val updated = state.userPrograms
                            .map { it.copy(isActive = false) }
                            .toMutableList()
                            .also { it.add(0, program) }
                        state.copy(aiLoading = false, userPrograms = updated)
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"$programName\" oluşturuldu ve aktif edildi!"))
                    sendEvent(ProgramEvent.NavigateBack)
                }
                .onFailure { err ->
                    updateState { it.copy(aiLoading = false, aiError = "Kayıt hatası: ${err.message}") }
                }
        }
    }

    fun clearAiError() { updateState { it.copy(aiError = null) } }

    private fun flexInt(obj: kotlinx.serialization.json.JsonObject, key: String, default: Int): Int {
        val el = obj[key]?.jsonPrimitive ?: return default
        return el.intOrNull
            ?: el.contentOrNull?.split("-", "/", "–")?.firstOrNull()?.trim()?.toIntOrNull()
            ?: default
    }

    // ── Create From Template ──────────────────────────────────────────────────

    fun selectTemplate(templateKey: String) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            programRepository.createFromTemplate(uid, templateKey)
                .onSuccess { program ->
                    updateState { state ->
                        val updated = state.userPrograms
                            .map { it.copy(isActive = false) }
                            .toMutableList()
                            .also { it.add(0, program) }
                        state.copy(isLoading = false, userPrograms = updated)
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"${program.name}\" programı oluşturuldu ve aktif edildi."))
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = err.message) }
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: Program oluşturulamadı."))
                }
        }
    }

    // ── Create Manual ─────────────────────────────────────────────────────────

    fun createManualProgram(name: String, days: List<ManualDayDraft>) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        if (name.isBlank()) { sendEvent(ProgramEvent.ShowSnackbar("Program adı boş olamaz.")); return }
        if (days.isEmpty()) { sendEvent(ProgramEvent.ShowSnackbar("En az 1 gün eklemelisiniz.")); return }

        val inputs = days.mapIndexed { i, d ->
            ManualDayInput(
                title     = d.title.ifBlank { "GÜN ${i + 1}" },
                isRestDay = d.isRestDay,
                exercises = d.selectedExercises
            )
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            programRepository.createManual(uid, name, inputs)
                .onSuccess { program ->
                    updateState { state ->
                        val updated = state.userPrograms
                            .map { it.copy(isActive = false) }
                            .toMutableList()
                            .also { it.add(0, program) }
                        state.copy(isLoading = false, userPrograms = updated)
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"${program.name}\" oluşturuldu."))
                    sendEvent(ProgramEvent.NavigateBack)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = err.message) }
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: Program kaydedilemedi."))
                }
        }
    }

    // ── Set Active ────────────────────────────────────────────────────────────

    fun setActive(programId: String) {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            programRepository.setActive(programId, uid)
                .onSuccess {
                    updateState { state ->
                        state.copy(
                            userPrograms = state.userPrograms.map {
                                it.copy(isActive = it.id == programId)
                            }
                        )
                    }
                }
        }
    }

    // ── Update (Edit) ─────────────────────────────────────────────────────────

    fun updateManualProgram(programId: String, name: String, days: List<ManualDayDraft>) {
        if (name.isBlank()) { sendEvent(ProgramEvent.ShowSnackbar("Program adı boş olamaz.")); return }
        if (days.isEmpty()) { sendEvent(ProgramEvent.ShowSnackbar("En az 1 gün eklemelisiniz.")); return }

        val inputs = days.mapIndexed { i, d ->
            ManualDayInput(
                title     = d.title.ifBlank { "GÜN ${i + 1}" },
                isRestDay = d.isRestDay,
                exercises = d.selectedExercises
            )
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            programRepository.updateProgram(programId, name, inputs)
                .onSuccess { updated ->
                    updateState { state ->
                        state.copy(
                            isLoading    = false,
                            userPrograms = state.userPrograms.map { p ->
                                if (p.id == programId) updated else p
                            }
                        )
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("\"$name\" güncellendi."))
                    sendEvent(ProgramEvent.NavigateBack)
                }
                .onFailure { err ->
                    updateState { it.copy(isLoading = false, error = err.message) }
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: Program kaydedilemedi."))
                }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteProgram(programId: String) {
        // Optimistic: UI'dan hemen kaldır, arka planda sil
        updateState { state ->
            state.copy(userPrograms = state.userPrograms.filter { it.id != programId })
        }
        viewModelScope.launch {
            programRepository.deleteProgram(programId)
                .onSuccess {
                    sendEvent(ProgramEvent.ShowSnackbar("Program silindi."))
                }
                .onFailure { e ->
                    // Başarısız olursa listeyi yeniden yükle
                    loadUserPrograms()
                    sendEvent(ProgramEvent.ShowSnackbar("Silme başarısız: ${e.message}"))
                }
        }
    }

    fun clearError() { updateState { it.copy(error = null) } }
}
