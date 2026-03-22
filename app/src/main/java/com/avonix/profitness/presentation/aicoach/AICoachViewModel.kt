package com.avonix.profitness.presentation.aicoach

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.AICoachPrefs
import com.avonix.profitness.data.ai.AICoachPrefsManager
import com.avonix.profitness.data.ai.ChatSession
import com.avonix.profitness.data.ai.ChatSessionManager
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.ai.HistoryEntry
import com.avonix.profitness.data.ai.StoredMessage
import com.avonix.profitness.data.program.ManualDayInput
import com.avonix.profitness.data.program.ManualExerciseInput
import com.avonix.profitness.data.program.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject

// ── Program Oluşturma Durumu ──────────────────────────────────────────────────

sealed class ProgramStatus {
    data object Idle    : ProgramStatus()
    data object Loading : ProgramStatus()
    data class  Success(val name: String) : ProgramStatus()
    data class  Error(val msg: String)    : ProgramStatus()
}

// ── State ─────────────────────────────────────────────────────────────────────

data class AICoachState(
    val messages         : List<ChatMessage> = emptyList(),
    val isLoading        : Boolean           = false,
    val showOnboarding   : Boolean           = false,
    val showHistory      : Boolean           = false,
    val sessions         : List<ChatSession> = emptyList(),
    val currentSessionId : String            = UUID.randomUUID().toString(),
    val sessionCreatedAt : Long              = System.currentTimeMillis(),
    val programStatus    : ProgramStatus     = ProgramStatus.Idle
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val geminiRepository  : GeminiRepository,
    private val programRepository : ProgramRepository,
    private val supabase          : SupabaseClient,
    @ApplicationContext private val context: Context
) : BaseViewModel<AICoachState, Nothing>(AICoachState()) {

    private val prefsManager   = AICoachPrefsManager(context)
    private val sessionManager = ChatSessionManager(context)
    private var currentPrefs   = prefsManager.getPrefs()

    private val conversationHistory = mutableListOf<Pair<String, String>>()

    private val jsonParser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        if (!currentPrefs.onboardingCompleted) {
            updateState { it.copy(showOnboarding = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistCurrentSession()
    }

    // ── Onboarding / Prefs ────────────────────────────────────────────────────

    fun completeOnboarding(prefs: AICoachPrefs) {
        prefsManager.savePrefs(prefs)
        currentPrefs = prefs
        updateState { it.copy(showOnboarding = false) }
    }

    fun loadCurrentPrefs(): AICoachPrefs = currentPrefs

    fun openPreferences() { updateState { it.copy(showOnboarding = true) } }

    // ── Welcome ───────────────────────────────────────────────────────────────

    fun initWelcome(welcomeText: String) {
        if (uiState.value.messages.isNotEmpty()) return
        updateState {
            it.copy(messages = listOf(ChatMessage(id = "welcome", text = welcomeText, isUser = false)))
        }
    }

    // ── Mesaj Gönder ──────────────────────────────────────────────────────────

    fun sendMessage(userText: String) {
        if (userText.isBlank() || uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id     = System.currentTimeMillis().toString(),
            text   = userText,
            isUser = true
        )
        updateState { it.copy(messages = it.messages + userMessage, isLoading = true) }
        persistCurrentSession()   // Kullanıcı mesajı hemen kaydet — uygulama kapansa bile kaybolmaz

        viewModelScope.launch {
            val systemPrompt = buildSystemPrompt()
            val result = geminiRepository.chat(
                history      = conversationHistory.toList(),
                userMessage  = userText,
                systemPrompt = systemPrompt
            )

            result.fold(
                onSuccess = { responseText ->
                    conversationHistory.add("user"  to userText)
                    conversationHistory.add("model" to responseText)

                    val oracleMessage = ChatMessage(
                        id     = (System.currentTimeMillis() + 1).toString(),
                        text   = responseText,
                        isUser = false
                    )
                    updateState { it.copy(messages = it.messages + oracleMessage, isLoading = false) }
                    persistCurrentSession()   // AI yanıtından sonra tekrar kaydet
                },
                onFailure = {
                    val errorMessage = ChatMessage(
                        id     = (System.currentTimeMillis() + 1).toString(),
                        text   = "Bağlantı hatası. Lütfen tekrar dene.",
                        isUser = false
                    )
                    updateState { it.copy(messages = it.messages + errorMessage, isLoading = false) }
                }
            )
        }
    }

    // ── Sohbet Geçmişi ────────────────────────────────────────────────────────

    fun openHistory() {
        val sessions = sessionManager.getAllSessions()
        updateState { it.copy(sessions = sessions, showHistory = true) }
    }

    fun closeHistory() { updateState { it.copy(showHistory = false) } }

    fun startNewSession() {
        persistCurrentSession()
        conversationHistory.clear()
        val newId = UUID.randomUUID().toString()
        updateState {
            it.copy(
                messages         = emptyList(),
                currentSessionId = newId,
                sessionCreatedAt = System.currentTimeMillis(),
                showHistory      = false,
                isLoading        = false,
                programStatus    = ProgramStatus.Idle
            )
        }
    }

    fun loadSession(session: ChatSession) {
        persistCurrentSession()
        conversationHistory.clear()
        conversationHistory.addAll(session.history.map { it.role to it.text })
        val messages = session.messages.map {
            ChatMessage(id = it.id, text = it.text, isUser = it.isUser, timestamp = it.timestamp)
        }
        updateState {
            it.copy(
                messages         = messages,
                currentSessionId = session.id,
                sessionCreatedAt = session.createdAt,
                showHistory      = false,
                programStatus    = ProgramStatus.Idle
            )
        }
    }

    fun deleteSession(id: String) {
        sessionManager.delete(id)
        val sessions = sessionManager.getAllSessions()
        updateState { it.copy(sessions = sessions) }
        if (uiState.value.currentSessionId == id) {
            conversationHistory.clear()
            val newId = UUID.randomUUID().toString()
            updateState {
                it.copy(
                    messages         = emptyList(),
                    currentSessionId = newId,
                    sessionCreatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    // ── Program Oluşturma ─────────────────────────────────────────────────────

    /** Seçili Oracle mesajını program olarak Supabase'e kaydeder. */
    fun applyProgram(messageText: String, programName: String) {
        viewModelScope.launch {
            updateState { it.copy(programStatus = ProgramStatus.Loading) }

            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("Oturum bulunamadı")) }
                return@launch
            }

            // 1. Egzersiz listesini DB'den çek
            val exercises = programRepository.getAllExercises().getOrNull()
            if (exercises == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("Egzersizler yüklenemedi")) }
                return@launch
            }
            val exerciseList = exercises.take(120).joinToString(", ") { ex ->
                if (ex.nameEn.isNotBlank() && ex.nameEn != ex.name) "${ex.name} (${ex.nameEn})" else ex.name
            }

            // 2. Gemini'ye yapılandırılmış JSON isteği gönder
            val structurePrompt = """
Aşağıdaki antrenman programını JSON formatına çevir.
Program adı: "$programName"

Program içeriği:
$messageText

Egzersiz adları için SADECE şu listeden seç (tam adı kullan):
$exerciseList

Çıktı olarak SADECE şu JSON formatını ver, başka hiçbir şey yazma:
{"name":"$programName","days":[{"title":"Gün 1 - Alt Vücut","isRestDay":false,"exercises":[{"exerciseName":"Squat","sets":3,"reps":10,"restSeconds":60}]},{"title":"Gün 2","isRestDay":true,"exercises":[]}]}
            """.trimIndent()

            val result = geminiRepository.chat(
                history      = emptyList(),
                userMessage  = structurePrompt,
                systemPrompt = "Sen bir JSON formatter'sın. Sadece geçerli JSON çıktısı ver, başka hiçbir şey yazma, açıklama ekleme."
            )

            val rawJson = result.getOrNull()
            if (rawJson == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("Yapay zeka yanıt vermedi")) }
                return@launch
            }

            // 3. JSON'u temizle — Gemini çeşitli formatlarda dönebilir
            val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(rawJson)?.value
            if (jsonCandidate == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("JSON bulunamadı")) }
                return@launch
            }

            // 4. Manuel JSON parsing — tip farklılıklarını tolere et
            val rootObj = runCatching { jsonParser.parseToJsonElement(jsonCandidate).jsonObject }.getOrNull()
            if (rootObj == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("JSON parse hatası")) }
                return@launch
            }

            val daysArray = rootObj["days"] as? JsonArray
            if (daysArray == null) {
                updateState { it.copy(programStatus = ProgramStatus.Error("Program günleri bulunamadı")) }
                return@launch
            }

            // 5. Egzersiz adlarını DB ID'lerle eşleştir (fuzzy matching)
            val exerciseMap   = exercises.associateBy { it.name.trim().lowercase() }
            val exerciseMapEn = exercises.filter { it.nameEn.isNotBlank() }
                .associateBy { it.nameEn.trim().lowercase() }

            fun findExercise(aiName: String): com.avonix.profitness.domain.model.ExerciseItem? {
                val key = aiName.trim().lowercase()
                // 1. Birebir eşleşme (TR)
                exerciseMap[key]?.let { return it }
                // 2. Birebir eşleşme (EN)
                exerciseMapEn[key]?.let { return it }
                // 3. Parçalı eşleşme — DB adı AI adını içeriyor veya tersi
                exerciseMap.entries.firstOrNull {
                    it.key.contains(key) || key.contains(it.key)
                }?.value?.let { return it }
                exerciseMapEn.entries.firstOrNull {
                    it.key.contains(key) || key.contains(it.key)
                }?.value?.let { return it }
                // 4. Kelime bazlı eşleşme — en az 2 ortak kelime
                val words = key.split(" ", "-").filter { it.length > 2 }.toSet()
                if (words.size >= 2) {
                    exerciseMap.entries.firstOrNull { (dbKey, _) ->
                        val dbWords = dbKey.split(" ", "-").filter { it.length > 2 }.toSet()
                        (words intersect dbWords).size >= 2
                    }?.value?.let { return it }
                }
                return null
            }

            var totalExercises   = 0
            var matchedExercises = 0

            val days = daysArray.mapIndexed { _, dayEl ->
                val dayObj = dayEl.jsonObject
                val title  = dayObj["title"]?.jsonPrimitive?.contentOrNull ?: "Gün"
                val isRest = dayObj["isRestDay"]?.jsonPrimitive?.booleanOrNull ?: false

                if (isRest) {
                    ManualDayInput(title = title, isRestDay = true)
                } else {
                    val exArray = dayObj["exercises"] as? JsonArray
                    val matched = exArray?.mapIndexedNotNull { exIdx, exEl ->
                        val exObj  = exEl.jsonObject
                        val exName = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull
                            ?: return@mapIndexedNotNull null
                        totalExercises++
                        val sets = flexInt(exObj, "sets", 3)
                        val reps = flexInt(exObj, "reps", 10)
                        val rest = flexInt(exObj, "restSeconds", 90)

                        val found = findExercise(exName)
                        if (found != null) {
                            matchedExercises++
                            ManualExerciseInput(
                                exerciseId  = found.id,
                                sets        = sets,
                                reps        = reps,
                                restSeconds = rest,
                                orderIndex  = exIdx
                            )
                        } else null
                    } ?: emptyList()
                    ManualDayInput(title = title, isRestDay = false, exercises = matched)
                }
            }

            // Hiç egzersiz eşleşmediyse hata ver
            if (totalExercises > 0 && matchedExercises == 0) {
                updateState {
                    it.copy(programStatus = ProgramStatus.Error(
                        "Egzersizler eşleştirilemedi ($totalExercises egzersiz bulundu ama DB'de karşılığı yok). Lütfen farklı bir programla dene."
                    ))
                }
                return@launch
            }

            // 6. Programı oluştur
            val createResult = programRepository.createManual(userId, programName, days)
            if (createResult.isSuccess) {
                updateState { it.copy(programStatus = ProgramStatus.Success(programName)) }
            } else {
                val errMsg = createResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                updateState { it.copy(programStatus = ProgramStatus.Error("Kayıt hatası: $errMsg")) }
            }
        }
    }

    fun resetProgramStatus() {
        updateState { it.copy(programStatus = ProgramStatus.Idle) }
    }

    // ── Kaydetme ──────────────────────────────────────────────────────────────

    private fun persistCurrentSession() {
        val state = uiState.value
        val msgs  = state.messages.filter { it.id != "welcome" && it.text.isNotBlank() }
        if (msgs.none { it.isUser }) return

        val title = msgs.firstOrNull { it.isUser }?.text?.let {
            if (it.length > 48) it.take(48) + "…" else it
        } ?: return

        val session = ChatSession(
            id        = state.currentSessionId,
            title     = title,
            history   = conversationHistory.map { HistoryEntry(it.first, it.second) },
            messages  = msgs.map { StoredMessage(it.id, it.text, it.isUser, it.timestamp) },
            createdAt = state.sessionCreatedAt,
            updatedAt = System.currentTimeMillis()
        )
        sessionManager.save(session)
    }

    // ── Sistem Promptu ────────────────────────────────────────────────────────

    private suspend fun buildSystemPrompt(): String {
        val prefs = currentPrefs
        val profileContext = if (prefs.allowProfileAccess) buildProfileContext() else ""

        return """
Sen Oracle — Profitness uygulamasının yapay zeka fitness koçusun.

KONU KISITLAMASI — KESİN KURAL:
Sadece aşağıdaki konularda yardımcı ol:
• Fitness, egzersiz ve antrenman programları
• Her türlü spor dalı (futbol, basketbol, yüzme, dövüş sporları vb.)
• Beslenme, diyet ve takviye (protein, vitamin, makro)
• Sağlık ve sağlıklı yaşam
• Vücut geliştirme, kondisyon, atletizm
• Uyku ve iyileşme (recovery)
• Spor psikolojisi ve mental performans

Bu konular DIŞINDA gelen sorulara şunu söyle: "Bu konuda yardımcı olamam. Fitness, spor ve sağlık hakkındaki sorularına yardımcı olmak için buradayım."

+18 veya cinsel içerik taleplerini kesinlikle ve sertçe reddet.
Siyaset, din, finans, teknoloji, programlama veya diğer konularda cevap verme.

DÜRÜSTLÜK KURALI:
Kullanıcıyı memnun etmek için yanlış veya yanıltıcı bilgi verme.
Ne bilimsel olarak doğruysa onu söyle. Emin olmadığın konularda bunu açıkça belirt.
Bir şey işe yaramıyorsa veya yanlışsa, kibarca ama net biçimde söyle.

FORMAT KURALI — KESİN KURAL:
Yanıtlarında markdown formatlaması KULLANMA.
** (bold) veya * (italic) işaretleri KULLANMA.
## başlık işareti KULLANMA.
Madde listelerinde • kullan.
Alt başlıklar için büyük harf kullan ve altına satır atla.
Numaralandırılmış listeler için 1. 2. 3. kullan.

DİL KURALI:
Kullanıcı hangi dilde yazıyorsa o dilde cevap ver.

YANIT UZUNLUĞU: ${prefs.responseLength.systemHint}

KONUŞMA TARZI: ${prefs.communicationStyle.systemHint}
$profileContext
        """.trimIndent()
    }

    private suspend fun buildProfileContext(): String {
        val user  = supabase.auth.currentUserOrNull() ?: return ""
        val email = user.email?.takeIf { it.isNotBlank() } ?: ""
        val programPart = buildProgramContext(user.id)

        return if (email.isNotEmpty() || programPart.isNotEmpty()) {
            "\nKULLANICI BİLGİSİ:${if (email.isNotEmpty()) "\nEmail: $email" else ""}$programPart"
        } else ""
    }

    /** JSON'dan int al — "8-12" gibi string aralığı da ilk sayıya çevirir */
    private fun flexInt(obj: JsonObject, key: String, default: Int): Int {
        val el = obj[key]?.jsonPrimitive ?: return default
        return el.intOrNull
            ?: el.contentOrNull
                ?.split("-", "/", "–")
                ?.firstOrNull()
                ?.trim()
                ?.toIntOrNull()
            ?: default
    }

    private suspend fun buildProgramContext(userId: String): String {
        val program = programRepository.getActiveProgram(userId).getOrNull() ?: return ""

        val trainingDays = program.days.filter { !it.isRestDay }
        val restDayCount = program.days.size - trainingDays.size

        val sb = StringBuilder()
        sb.append("\nAktif Program: ${program.name}")
        sb.append(" (${trainingDays.size} antrenman günü, $restDayCount dinlenme günü)")

        if (trainingDays.isNotEmpty()) {
            sb.append("\nProgram İçeriği:")
            trainingDays.forEach { day ->
                val exercises = day.exercises.joinToString(", ") { it.exerciseName }
                sb.append("\n  • ${day.title}: $exercises")
            }
        }

        return sb.toString()
    }
}
