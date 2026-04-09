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

data class AiEditExerciseResult(
    val exerciseId  : String,
    val name        : String,
    val targetMuscle: String,
    val sets        : Int,
    val reps        : Int,
    val restSeconds : Int
)

data class AiEditDayResult(
    val title    : String,
    val isRestDay: Boolean,
    val exercises: List<AiEditExerciseResult> = emptyList()
)

data class ProgramUiState(
    val isLoading    : Boolean          = false,
    val error        : String?          = null,
    val userPrograms : List<Program>    = emptyList(),
    val exercises    : List<ExerciseItem> = emptyList(),
    // AI builder
    val aiLoading    : Boolean          = false,
    val aiError      : String?          = null,
    // AI edit
    val aiEditLoading: Boolean          = false,
    val aiEditError  : String?          = null,
    val aiEditResult : Pair<String, List<AiEditDayResult>>? = null,
    // Exercise request
    val requestLoading: Boolean         = false
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

    /**
     * 0.0 (tamamen farklı) ile 1.0 (aynı) arasında benzerlik skoru döner.
     * Levenshtein edit distance'a dayalı: similarity = 1 - distance / maxLen
     */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val la = a.length; val lb = b.length
        if (la == 0 || lb == 0) return 0.0
        val dp = Array(la + 1) { IntArray(lb + 1) }
        for (i in 0..la) dp[i][0] = i
        for (j in 0..lb) dp[0][j] = j
        for (i in 1..la) for (j in 1..lb) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return 1.0 - dp[la][lb].toDouble() / maxOf(la, lb)
    }

    private fun findExerciseByName(
        aiName: String,
        trMap: Map<String, ExerciseItem>,
        enMap: Map<String, ExerciseItem>,
        threshold: Double = 0.82
    ): ExerciseItem? {
        val key = aiName.trim().lowercase()

        // 1. Tam eşleşme
        trMap[key]?.let { return it }
        enMap[key]?.let { return it }

        // 2. Levenshtein benzerlik — eşik üzerindeki en iyi sonuç
        val allEntries = (trMap.entries + enMap.entries)
        return allEntries
            .map { it.value to similarity(key, it.key) }
            .filter { it.second >= threshold }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    init {
        observeData()
    }

    // ── Reactive Observation ─────────────────────────────────────────────────

    private fun observeData() {
        val uid = currentUserId() ?: return

        // Room Flow: programlar değişince otomatik güncellenir
        viewModelScope.launch {
            programRepository.observeUserPrograms(uid).collect { programs ->
                updateState { it.copy(isLoading = false, userPrograms = programs) }
            }
        }

        // Room Flow: egzersiz listesi değişince otomatik güncellenir
        viewModelScope.launch {
            programRepository.observeExercises().collect { list ->
                updateState { it.copy(exercises = list) }
            }
        }

        // İlk yüklemede Supabase'den sync et
        viewModelScope.launch {
            programRepository.syncFromRemote(uid)
        }
    }

    /** Tab geçişleri veya geri dönüş için — artık sadece sync tetikler, Flow otomatik günceller. */
    fun reloadIfStale() {
        val uid = currentUserId() ?: return
        viewModelScope.launch { programRepository.syncFromRemote(uid) }
    }

    fun loadUserPrograms() {
        val uid = currentUserId() ?: return
        viewModelScope.launch { programRepository.syncFromRemote(uid) }
    }

    fun loadExercises() {
        viewModelScope.launch {
            programRepository.getAllExercises()
                .onSuccess { list -> updateState { it.copy(exercises = list) } }
        }
    }

    // ── Hareket Talebi ────────────────────────────────────────────────────────

    fun requestExercise(name: String, targetMuscle: String, notes: String) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        viewModelScope.launch {
            updateState { it.copy(requestLoading = true) }
            programRepository.requestExercise(uid, name, targetMuscle, notes)
                .onSuccess {
                    updateState { it.copy(requestLoading = false) }
                    sendEvent(ProgramEvent.ShowSnackbar("Talebiniz alındı, en kısa sürede eklenecek!"))
                }
                .onFailure {
                    updateState { it.copy(requestLoading = false) }
                    sendEvent(ProgramEvent.ShowSnackbar("Talep gönderilemedi, tekrar deneyin."))
                }
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

            // 1. Egzersiz listesini hazırla (eşleştirme için — prompt'a gönderilmez)
            val baseExercises = uiState.value.exercises.ifEmpty {
                programRepository.getAllExercises().getOrNull() ?: emptyList()
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

Her egzersiz için standart Türkçe veya İngilizce adını kullan.
"targetMuscle" değerleri: Göğüs / Sırt / Omuz / Bacak / Kol / Karın / Genel
"category" değerleri: Serbest Ağırlık / Makine / Kardiyo / Vücut Ağırlığı

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

            // 4. Egzersiz eşleştirme — exact + Levenshtein, yeni eklenenler de aranabilir
            val currentMap   = baseExercises.associateBy { it.name.trim().lowercase() }.toMutableMap()
            val currentMapEn = baseExercises.filter { it.nameEn.isNotBlank() }
                .associateBy { it.nameEn.trim().lowercase() }.toMutableMap()

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

                        var exercise = findExerciseByName(exName, currentMap, currentMapEn)

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

    // ── AI ile Program Düzenleme ───────────────────────────────────────────────

    /**
     * [currentName] ve [currentDays]: Composable'daki güncel state — program nesnesi değil.
     * Böylece önceki AI düzenlemeleri ikinci çağrıda da korunur.
     */
    fun editWithAI(
        programId       : String,
        currentName     : String,
        currentDays     : List<ManualDayDraft>,
        userInstruction : String
    ) {
        if (userInstruction.isBlank()) {
            sendEvent(ProgramEvent.ShowSnackbar("Lütfen bir talimat girin."))
            return
        }
        viewModelScope.launch {
            updateState { it.copy(aiEditLoading = true, aiEditError = null) }

            val baseExercises = uiState.value.exercises.ifEmpty {
                programRepository.getAllExercises().getOrNull() ?: emptyList()
            }
            // Güncel Composable state'ini JSON olarak serileştir (egzersiz adını DB'den al)
            val exerciseNameMap = baseExercises.associateBy { it.id }
            val currentProgramJson = buildString {
                append("{\"name\":\"${currentName.replace("\"", "\\\"")}\",\"days\":[")
                currentDays.forEachIndexed { i, day ->
                    if (i > 0) append(",")
                    append("{\"title\":\"${day.title.replace("\"", "\\\"")}\",\"isRestDay\":${day.isRestDay}")
                    if (!day.isRestDay && day.selectedExercises.isNotEmpty()) {
                        append(",\"exercises\":[")
                        day.selectedExercises.forEachIndexed { j, ex ->
                            if (j > 0) append(",")
                            val exName = exerciseNameMap[ex.exerciseId]?.name ?: ex.exerciseId
                            append("{\"exerciseName\":\"${exName.replace("\"", "\\\"")}\",\"sets\":${ex.sets},\"reps\":${ex.reps},\"restSeconds\":${ex.restSeconds}}")
                        }
                        append("]")
                    }
                    append("}")
                }
                append("]}")
            }

            val geminiPrompt = """
Mevcut antrenman programı:
$currentProgramJson

Kullanıcının düzenleme isteği: $userInstruction

Her egzersiz için standart Türkçe veya İngilizce adını kullan.
"targetMuscle" değerleri: Göğüs / Sırt / Omuz / Bacak / Kol / Karın / Genel
"category" değerleri: Serbest Ağırlık / Makine / Kardiyo / Vücut Ağırlığı
Kullanıcının isteğini uygula, değiştirilmeyen günleri olduğu gibi bırak, tüm programı güncellenmiş haliyle döndür.

ÇIKTI KURALI: Yalnızca geçerli JSON döndür. Markdown, açıklama, kod bloğu YASAK.
FORMAT:
{"name":"...","days":[{"title":"Gün 1 - Göğüs","isRestDay":false,"exercises":[{"exerciseName":"Bench Press","sets":4,"reps":10,"restSeconds":60,"targetMuscle":"Göğüs","category":"Serbest Ağırlık"}]},{"title":"Gün 2 - Dinlenme","isRestDay":true,"exercises":[]}]}
            """.trimIndent()

            val systemPrompt = "Sen bir fitness programı düzenleyicisisin. Mevcut programı kullanıcının isteğine göre güncelle. Değiştirilmesi istenmeyen günleri olduğu gibi koru. SADECE ham JSON döndür, başka hiçbir şey yazma."

            val result = geminiRepository.chat(emptyList(), geminiPrompt, systemPrompt)
            val rawJson = result.getOrNull()
            if (rawJson == null) {
                updateState { it.copy(aiEditLoading = false, aiEditError = "Bağlantı hatası: ${result.exceptionOrNull()?.message}") }
                return@launch
            }

            val cleaned = rawJson
                .replace(Regex("```[a-zA-Z]*\\s*"), "")
                .replace("```", "")
                .trim()
            val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(cleaned)?.value
            if (jsonCandidate == null) {
                updateState { it.copy(aiEditLoading = false, aiEditError = "Geçersiz yanıt, tekrar dene.") }
                return@launch
            }

            val rootObj = runCatching { jsonParser.parseToJsonElement(jsonCandidate).jsonObject }.getOrNull()
            if (rootObj == null) {
                updateState { it.copy(aiEditLoading = false, aiEditError = "Program ayrıştırılamadı, tekrar dene.") }
                return@launch
            }

            val newName   = rootObj["name"]?.jsonPrimitive?.contentOrNull ?: currentName
            val daysArray = rootObj["days"] as? JsonArray
            if (daysArray == null) {
                updateState { it.copy(aiEditLoading = false, aiEditError = "Program günleri bulunamadı.") }
                return@launch
            }

            // Egzersiz eşleştirme — exact + Levenshtein, yeni eklenenler de aranabilir
            val editMap   = baseExercises.associateBy { it.name.trim().lowercase() }.toMutableMap()
            val editMapEn = baseExercises.filter { it.nameEn.isNotBlank() }
                .associateBy { it.nameEn.trim().lowercase() }.toMutableMap()

            val editedDays = daysArray.map { dayEl ->
                val dayObj = dayEl.jsonObject
                val title  = dayObj["title"]?.jsonPrimitive?.contentOrNull ?: "Gün"
                val isRest = dayObj["isRestDay"]?.jsonPrimitive?.booleanOrNull ?: false

                if (isRest) {
                    AiEditDayResult(title = title, isRestDay = true)
                } else {
                    val exArray = dayObj["exercises"] as? JsonArray
                    val matched = exArray?.mapNotNull { exEl ->
                        val exObj        = exEl.jsonObject
                        val exName       = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val sets         = flexInt(exObj, "sets", 3)
                        val reps         = flexInt(exObj, "reps", 10)
                        val rest         = flexInt(exObj, "restSeconds", 90)
                        val targetMuscle = exObj["targetMuscle"]?.jsonPrimitive?.contentOrNull ?: "Genel"
                        val category     = exObj["category"]?.jsonPrimitive?.contentOrNull ?: "Serbest Ağırlık"

                        var exercise = findExerciseByName(exName, editMap, editMapEn)
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
                                editMap[newEx.name.trim().lowercase()] = newEx
                            }
                        }

                        exercise?.let {
                            AiEditExerciseResult(
                                exerciseId   = it.id,
                                name         = it.name,
                                targetMuscle = targetMuscle,
                                sets         = sets,
                                reps         = reps,
                                restSeconds  = rest
                            )
                        }
                    } ?: emptyList()
                    AiEditDayResult(title = title, isRestDay = false, exercises = matched)
                }
            }

            updateState { it.copy(aiEditLoading = false, aiEditResult = Pair(newName, editedDays)) }
        }
    }

    fun clearAiEditResult() { updateState { it.copy(aiEditResult = null) } }
    fun clearAiEditError()  { updateState { it.copy(aiEditError  = null) } }

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
                    sendEvent(ProgramEvent.ShowSnackbar("Hata: ${err.message ?: "Program kaydedilemedi."}"))
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
