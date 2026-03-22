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

    init {
        loadUserPrograms()
        loadExercises()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadUserPrograms() {
        val uid = currentUserId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            programRepository.getUserPrograms(uid)
                .onSuccess { programs ->
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

    fun createFromAI(userPrompt: String) {
        val uid = currentUserId() ?: run {
            sendEvent(ProgramEvent.ShowSnackbar("Giriş yapmanız gerekiyor."))
            return
        }
        if (userPrompt.isBlank()) {
            sendEvent(ProgramEvent.ShowSnackbar("Lütfen bir açıklama girin."))
            return
        }

        viewModelScope.launch {
            updateState { it.copy(aiLoading = true, aiError = null) }

            // 1. Egzersiz listesini hazırla
            val exercises = uiState.value.exercises.ifEmpty {
                programRepository.getAllExercises().getOrNull() ?: emptyList()
            }
            val exerciseList = exercises.take(120).joinToString(", ") { ex ->
                if (ex.nameEn.isNotBlank() && ex.nameEn != ex.name)
                    "${ex.name} (${ex.nameEn})" else ex.name
            }

            // 2. Gemini'ye JSON formatında program iste
            val geminiPrompt = """
Kullanıcı şu antrenman programını istiyor:
$userPrompt

Egzersiz adları için SADECE şu listeden seç (tam adı kullan):
$exerciseList

SADECE aşağıdaki JSON formatında yanıt ver, başka hiçbir şey yazma:
{"name":"Program Adı","days":[{"title":"Gün 1 - Göğüs","isRestDay":false,"exercises":[{"exerciseName":"Bench Press","sets":4,"reps":10,"restSeconds":60}]},{"title":"Gün 2","isRestDay":true,"exercises":[]}]}
            """.trimIndent()

            val result = geminiRepository.chat(
                history      = emptyList(),
                userMessage  = geminiPrompt,
                systemPrompt = "Sen bir fitness programı oluşturucusun. Sadece JSON formatında yanıt ver, açıklama ekleme."
            )

            val rawJson = result.getOrNull()
            if (rawJson == null) {
                updateState { it.copy(aiLoading = false, aiError = "Bağlantı hatası, tekrar dene.") }
                return@launch
            }

            // 3. JSON'u çıkar ve parse et
            val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(rawJson)?.value
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

            // 4. Egzersiz eşleştirme
            val exerciseMap   = exercises.associateBy { it.name.trim().lowercase() }
            val exerciseMapEn = exercises.filter { it.nameEn.isNotBlank() }
                .associateBy { it.nameEn.trim().lowercase() }

            fun findExercise(aiName: String): ExerciseItem? {
                val key = aiName.trim().lowercase()
                exerciseMap[key]?.let { return it }
                exerciseMapEn[key]?.let { return it }
                exerciseMap.entries.firstOrNull { it.key.contains(key) || key.contains(it.key) }?.value?.let { return it }
                exerciseMapEn.entries.firstOrNull { it.key.contains(key) || key.contains(it.key) }?.value?.let { return it }
                val words = key.split(" ", "-").filter { it.length > 2 }.toSet()
                if (words.size >= 2) {
                    exerciseMap.entries.firstOrNull { (dbKey, _) ->
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
                    val exArray  = dayObj["exercises"] as? JsonArray
                    val matched  = exArray?.mapIndexedNotNull { exIdx, exEl ->
                        val exObj  = exEl.jsonObject
                        val exName = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                        val sets   = flexInt(exObj, "sets", 3)
                        val reps   = flexInt(exObj, "reps", 10)
                        val rest   = flexInt(exObj, "restSeconds", 90)
                        findExercise(exName)?.let {
                            ManualExerciseInput(it.id, sets, reps, rest, exIdx)
                        }
                    } ?: emptyList()
                    ManualDayInput(title = title, isRestDay = false, exercises = matched)
                }
            }

            // 5. Programı oluştur
            programRepository.createManual(uid, programName, days)
                .onSuccess { program ->
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

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteProgram(programId: String) {
        viewModelScope.launch {
            programRepository.deleteProgram(programId)
                .onSuccess {
                    updateState { state ->
                        state.copy(userPrograms = state.userPrograms.filter { it.id != programId })
                    }
                    sendEvent(ProgramEvent.ShowSnackbar("Program silindi."))
                }
        }
    }

    fun clearError() { updateState { it.copy(error = null) } }
}
