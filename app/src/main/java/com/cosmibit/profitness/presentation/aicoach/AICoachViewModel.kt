package com.cosmibit.profitness.presentation.aicoach

import com.cosmibit.profitness.domain.ai.isAiLocked
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.cosmibit.profitness.core.BaseViewModel
import com.cosmibit.profitness.data.ai.AiAccessException
import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.ai.AICoachPrefs
import com.cosmibit.profitness.data.ai.AICoachPrefsManager
import com.cosmibit.profitness.data.ai.ChatSession
import com.cosmibit.profitness.data.ai.ChatSessionManager
import com.cosmibit.profitness.data.ai.GeminiRepository
import com.cosmibit.profitness.data.ai.HistoryEntry
import com.cosmibit.profitness.data.ai.StoredMessage
import com.cosmibit.profitness.data.program.ManualDayInput
import com.cosmibit.profitness.data.program.ManualExerciseInput
import com.cosmibit.profitness.data.program.ProgramRepository
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.data.store.UserPlanRepository
import com.cosmibit.profitness.data.weight.WeightRepository
import com.cosmibit.profitness.data.workout.WorkoutRepository
import com.cosmibit.profitness.domain.model.ExerciseNameRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.Period
import java.time.format.TextStyle
import java.util.Locale
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
    val programStatus    : ProgramStatus     = ProgramStatus.Idle,
    val userPlan         : UserPlan          = UserPlan.FREE,
    val aiCredits        : Int               = UserPlanRepository.INITIAL_CREDITS_PLACEHOLDER,
    val isBillingLoaded  : Boolean           = false
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed class AICoachEvent {
    /** Enerji bitti / plan yetersiz → Paywall göster. */
    data object ShowPaywall : AICoachEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val geminiRepository  : GeminiRepository,
    private val programRepository : ProgramRepository,
    private val planRepository    : UserPlanRepository,
    private val weightRepository  : WeightRepository,
    private val workoutRepository : WorkoutRepository,
    private val supabase          : SupabaseClient,
    @ApplicationContext private val context: Context
) : BaseViewModel<AICoachState, AICoachEvent>(AICoachState()) {

    private var currentPrefs   = AICoachPrefs()
    private var activeUserId   : String? = null

    private val conversationHistory = mutableListOf<Pair<String, String>>()

    private val jsonParser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ChatSessionManager.clearLegacySessions(context)
            AICoachPrefsManager.clearLegacyPrefs(context)

            val userId = currentUserId()
            val loadedPrefs = userId
                ?.let { AICoachPrefsManager(context, it).getPrefs() }
                ?: AICoachPrefs()
            withContext(Dispatchers.Main) {
                activeUserId = userId
                currentPrefs = loadedPrefs
                updateState { it.copy(showOnboarding = !loadedPrefs.onboardingCompleted) }
            }
        }
        // Plan ve Enerji değişikliklerini reaktif dinle
        viewModelScope.launch {
            planRepository.billingSnapshotFlow
                .collect { snapshot ->
                    updateState {
                        it.copy(
                            userPlan = snapshot.plan,
                            aiCredits = snapshot.credits.coerceAtLeast(0),
                            isBillingLoaded = snapshot.isLoaded
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistCurrentSession()
    }

    private fun currentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id ?: supabase.auth.currentUserOrNull()?.id

    private fun currentPrefsManager(): AICoachPrefsManager? =
        currentUserId()?.let { AICoachPrefsManager(context, it) }

    private fun currentSessionManager(): ChatSessionManager? =
        currentUserId()?.let { ChatSessionManager(context, it) }

    private fun ensureCurrentUserScope(): String? {
        val userId = currentUserId()
        if (userId != activeUserId) {
            activeUserId = userId
            currentPrefs = userId
                ?.let { AICoachPrefsManager(context, it).getPrefs() }
                ?: AICoachPrefs()
            conversationHistory.clear()
            updateState {
                it.copy(
                    messages         = emptyList(),
                    sessions         = emptyList(),
                    showHistory      = false,
                    showOnboarding   = !currentPrefs.onboardingCompleted,
                    currentSessionId = UUID.randomUUID().toString(),
                    sessionCreatedAt = System.currentTimeMillis(),
                    isLoading        = false,
                    programStatus    = ProgramStatus.Idle
                )
            }
        }
        return userId
    }

    // ── Onboarding / Prefs ────────────────────────────────────────────────────

    fun completeOnboarding(prefs: AICoachPrefs) {
        ensureCurrentUserScope()
        currentPrefsManager()?.savePrefs(prefs)
        currentPrefs = prefs
        updateState { it.copy(showOnboarding = false) }
    }

    fun loadCurrentPrefs(): AICoachPrefs = currentPrefs

    fun openPreferences() { updateState { it.copy(showOnboarding = true) } }

    fun closePreferencesIfCompleted() {
        if (currentPrefs.onboardingCompleted) {
            updateState { it.copy(showOnboarding = false) }
        }
    }

    // ── Welcome ───────────────────────────────────────────────────────────────

    fun initWelcome(welcomeText: String) {
        ensureCurrentUserScope()
        if (uiState.value.messages.isNotEmpty()) return
        updateState {
            it.copy(messages = listOf(ChatMessage(id = "welcome", text = welcomeText, isUser = false)))
        }
    }

    // ── Mesaj Gönder ──────────────────────────────────────────────────────────

    fun sendMessage(userText: String) {
        if (ensureCurrentUserScope() == null) return
        if (userText.isBlank() || uiState.value.isLoading) return
        val userMessage = ChatMessage(
            id     = System.currentTimeMillis().toString(),
            text   = userText,
            isUser = true
        )

        updateState {
            it.copy(
                messages  = it.messages + userMessage,
                isLoading = true
            )
        }
        persistCurrentSession()   // Kullanıcı mesajı UI ve geçmişe AI çağrısını beklemeden düşsün

        viewModelScope.launch {
            runCatching {
                // Enerji kontrolü — FREE plan + 0 Enerji → paywall, devam etme
                if (planRepository.isAiLocked(com.cosmibit.profitness.domain.ai.AiFeature.CoachChat)) {
                    updateState {
                        it.copy(
                            messages  = it.messages.filterNot { msg -> msg.id == userMessage.id },
                            isLoading = false
                        )
                    }
                    sendEvent(AICoachEvent.ShowPaywall)
                    return@launch
                }

                val systemPrompt = buildSystemPrompt()
                geminiRepository.chat(
                    history      = conversationHistory.toList(),
                    userMessage  = userText,
                    systemPrompt = systemPrompt
                )
            }.fold(
                onSuccess = { result ->
                    handleChatResult(result, userText)
                },
                onFailure = { error ->
                    showChatError(error)
                }
            )
        }
    }

    private suspend fun handleChatResult(result: Result<String>, userText: String) {
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
            onFailure = { error ->
                val accessErr = error as? AiAccessException
                if (accessErr != null) {
                    updateState { state -> state.copy(isLoading = false) }
                    if (accessErr.requiresPaywall) {
                        sendEvent(AICoachEvent.ShowPaywall)
                    } else {
                        showChatError(accessErr)
                    }
                    return
                }
                showChatError(error)
            }
        )
    }

    private fun showChatError(error: Throwable) {
        val message = error.message
            ?.takeIf { it.isNotBlank() && it.length <= 140 }
            ?: "Bağlantı hatası. Lütfen tekrar dene."
        val errorMessage = ChatMessage(
            id     = (System.currentTimeMillis() + 1).toString(),
            text   = message,
            isUser = false
        )
        updateState { it.copy(messages = it.messages + errorMessage, isLoading = false) }
    }

    // ── Sohbet Geçmişi ────────────────────────────────────────────────────────

    fun openHistory() {
        val sessions = if (ensureCurrentUserScope() == null) {
            emptyList()
        } else {
            currentSessionManager()?.getAllSessions().orEmpty()
        }
        updateState { it.copy(sessions = sessions, showHistory = true) }
    }

    fun closeHistory() { updateState { it.copy(showHistory = false) } }

    fun startNewSession() {
        if (ensureCurrentUserScope() == null) return
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
        val userId = ensureCurrentUserScope() ?: return
        if (session.userId != userId) return
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
        if (ensureCurrentUserScope() == null) return
        currentSessionManager()?.delete(id)
        val sessions = currentSessionManager()?.getAllSessions().orEmpty()
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
            if (planRepository.isAiLocked(com.cosmibit.profitness.domain.ai.AiFeature.CoachToProgram)) {
                sendEvent(AICoachEvent.ShowPaywall)
                return@launch
            }
            updateState { it.copy(programStatus = ProgramStatus.Loading) }

            val userId = currentUserId()
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
            val exerciseList = exercises.take(72).joinToString(", ") { ex ->
                if (ex.nameEn.isNotBlank() && ex.nameEn != ex.name) "${ex.name} (${ex.nameEn})" else ex.name
            }

            // 2. Gemini'ye yapılandırılmış JSON isteği gönder
            val structurePrompt = """
Aşağıdaki antrenman programını JSON formatına çevir.
Program adı: "$programName"

Program içeriği:
$messageText

Egzersiz adları için önce şu listeden en uygun tam adı kullan; karşılığı yoksa yeni hareket adını aynen yaz:
$exerciseList

Aralık varsa tek bir sabit sayı seç. Kaynakta yazan başlangıç kilosunu weightKg alanına kaydet.
Süre bazlı hareketlerde targetDurationSeconds (saniye), mesafe bazlı hareketlerde targetDistanceMeters (metre) alanını doldur.
Isınma, ana antrenman, core/postür, finisher, gün içi ve aktif toparlanma bilgisini section alanında koru; uygulama ayrıntılarını notes alanına yaz.
Süperset/dev set/devre üyelerine aynı groupId değerini ver. groupType yalnızca straight, superset, giant_set veya circuit olabilir. Tur sayısını groupRounds, tur arası dinlenmeyi saniye olarak groupRestSeconds alanına yaz.
Hafif aktivite içeren toparlanma gününü boş dinlenme günü yapma.

Çıktı olarak SADECE şu JSON formatını ver, başka hiçbir şey yazma:
{"name":"$programName","days":[{"title":"Gün 1 - Alt Vücut","isRestDay":false,"notes":"","exercises":[{"exerciseName":"Squat","sets":3,"reps":10,"restSeconds":60,"weightKg":20,"targetDurationSeconds":null,"targetDistanceMeters":null,"targetMuscle":"Bacak","category":"Serbest Ağırlık","section":"Ana Antrenman","notes":"Kontrollü uygula.","groupId":"gun1-ss1","groupType":"superset","groupLabel":"Süper Set 1","groupRounds":2,"groupRestSeconds":90}]},{"title":"Gün 2","isRestDay":true,"notes":"Tam dinlenme","exercises":[]}]}
            """.trimIndent()

            val result = geminiRepository.chat(
                history      = emptyList(),
                userMessage  = structurePrompt,
                systemPrompt = "Sen bir JSON formatter'sın. Sadece geçerli JSON çıktısı ver, başka hiçbir şey yazma, açıklama ekleme.",
                tool         = AiToolType.ORACLE_TO_PROGRAM
            )

            val rawJson = result.getOrNull()
            if (rawJson == null) {
                val accessErr = result.exceptionOrNull() as? AiAccessException
                if (accessErr != null) {
                    if (accessErr.requiresPaywall) {
                        updateState { it.copy(programStatus = ProgramStatus.Idle) }
                        sendEvent(AICoachEvent.ShowPaywall)
                    } else {
                        updateState {
                            it.copy(
                                programStatus = ProgramStatus.Error(
                                    accessErr.message.ifBlank { "AI limiti geçici olarak doldu. Kısa süre sonra tekrar dene." }
                                )
                            )
                        }
                    }
                    return@launch
                }
                updateState { it.copy(programStatus = ProgramStatus.Error("Yapay zeka yanıt vermedi")) }
                return@launch
            }

            // 3. JSON'u temizle — Gemini markdown kod bloğu dönebilir
            val cleaned = rawJson
                .replace(Regex("```[a-zA-Z]*\\s*"), "")
                .replace("```", "")
                .trim()
            val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(cleaned)?.value
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

            // 5. Egzersiz adlarını DB ID'lerle eşleştir (Türkçe uyumlu normalize fuzzy matching)
            val exerciseMap   = exercises.associateBy { ExerciseNameRules.normalizedKey(it.name) }.toMutableMap()
            val exerciseMapEn = exercises.filter { it.nameEn.isNotBlank() }
                .associateBy { ExerciseNameRules.normalizedKey(it.nameEn) }
                .toMutableMap()

            fun findExercise(aiName: String): com.cosmibit.profitness.domain.model.ExerciseItem? {
                val key = ExerciseNameRules.normalizedKey(aiName)
                if (key.isBlank()) return null
                // 1. Birebir eşleşme (TR / EN)
                exerciseMap[key]?.let { return it }
                exerciseMapEn[key]?.let { return it }
                // 2. Parçalı eşleşme — DB adı AI adını içeriyor veya tersi
                exerciseMap.entries.firstOrNull {
                    it.key.contains(key) || key.contains(it.key)
                }?.value?.let { return it }
                exerciseMapEn.entries.firstOrNull {
                    it.key.contains(key) || key.contains(it.key)
                }?.value?.let { return it }
                // 3. Kelime bazlı eşleşme — en az 2 ortak kelime
                val words = key.split(" ", "-").filter { it.length > 2 }.toSet()
                if (words.size >= 2) {
                    (exerciseMap.entries + exerciseMapEn.entries).firstOrNull { (dbKey, _) ->
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
                val title  = dayObj["title"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null } ?: "Gün"
                val isRest = dayObj["isRestDay"]?.jsonPrimitive?.booleanOrNull ?: false
                val dayNotes = dayObj["notes"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().take(600)

                if (isRest) {
                    ManualDayInput(title = title, isRestDay = true, notes = dayNotes)
                } else {
                    val exArray = dayObj["exercises"] as? JsonArray
                    val matched = exArray?.mapIndexedNotNull { exIdx, exEl ->
                        val exObj  = exEl.jsonObject
                        val exName = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
                            ?: return@mapIndexedNotNull null
                        totalExercises++
                        val sets = flexInt(exObj, "sets", 3).coerceIn(1, 20)
                        val reps = flexInt(exObj, "reps", 10).coerceIn(1, 200)
                        val rest = flexInt(exObj, "restSeconds", 90).coerceIn(0, 600)
                        val weight = exObj["weightKg"]?.jsonPrimitive?.contentOrNull
                            ?.replace(',', '.')?.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                        val durationSec = flexIntOrNull(exObj, "targetDurationSeconds")?.takeIf { it > 0 }
                        val distanceM = exObj["targetDistanceMeters"]?.jsonPrimitive?.contentOrNull
                            ?.replace(',', '.')?.toFloatOrNull()?.takeIf { it > 0f }
                        val targetMuscle = exObj["targetMuscle"]?.jsonPrimitive?.contentOrNull ?: "Genel"
                        val category = exObj["category"]?.jsonPrimitive?.contentOrNull ?: "Serbest Ağırlık"

                        val found = findExercise(exName)
                            ?: programRepository.addExercise(exName, exName, targetMuscle, category, sets, reps)
                                .getOrNull()
                                ?.also {
                                    exerciseMap[ExerciseNameRules.normalizedKey(it.name)] = it
                                    if (it.nameEn.isNotBlank()) {
                                        exerciseMapEn[ExerciseNameRules.normalizedKey(it.nameEn)] = it
                                    }
                                }
                        if (found != null) {
                            matchedExercises++
                            ManualExerciseInput(
                                exerciseId  = found.id,
                                sets        = sets,
                                reps        = reps,
                                restSeconds = rest,
                                weightKg    = weight,
                                targetDurationSeconds = durationSec,
                                targetDistanceMeters = distanceM,
                                orderIndex  = exIdx,
                                section     = exObj["section"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().take(80),
                                notes       = exObj["notes"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().take(600),
                                groupId     = exObj["groupId"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null },
                                groupType   = when (exObj["groupType"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()) {
                                    "superset" -> "superset"
                                    "giant_set", "giantset", "dev set" -> "giant_set"
                                    "circuit", "devre" -> "circuit"
                                    else -> "straight"
                                },
                                groupLabel  = exObj["groupLabel"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().take(80),
                                groupRounds = flexIntOrNull(exObj, "groupRounds")?.coerceIn(1, 20),
                                groupRestSeconds = flexIntOrNull(exObj, "groupRestSeconds")?.coerceIn(0, 3600)
                            )
                        } else null
                    } ?: emptyList()
                    ManualDayInput(title = title, isRestDay = false, notes = dayNotes, exercises = matched)
                }
            }

            if (days.none { !it.isRestDay && it.exercises.isNotEmpty() }) {
                updateState {
                    it.copy(programStatus = ProgramStatus.Error(
                        "Programda geçerli egzersiz bulunamadı ($totalExercises egzersiz işlendi). Lütfen farklı bir mesajla tekrar dene."
                    ))
                }
                return@launch
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
        val userId = currentUserId() ?: return
        val state = uiState.value
        val msgs  = state.messages.filter { it.id != "welcome" && it.text.isNotBlank() }
        if (msgs.none { it.isUser }) return

        val title = msgs.firstOrNull { it.isUser }?.text?.let {
            if (it.length > 48) it.take(48) + "…" else it
        } ?: return

        val session = ChatSession(
            id        = state.currentSessionId,
            userId    = userId,
            title     = title,
            history   = conversationHistory.map { HistoryEntry(it.first, it.second) },
            messages  = msgs.map { StoredMessage(it.id, it.text, it.isUser, it.timestamp) },
            createdAt = state.sessionCreatedAt,
            updatedAt = System.currentTimeMillis()
        )
        runCatching { ChatSessionManager(context, userId).save(session) }
    }

    // ── Sistem Promptu ────────────────────────────────────────────────────────

    private suspend fun buildSystemPrompt(): String {
        val prefs = currentPrefs
        val profileContext = if (prefs.allowProfileAccess) buildProfileContext() else ""

        return """
Sen AI Koç — Profitness uygulamasının yapay zeka fitness koçusun.

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
        return withContext(Dispatchers.IO) {
            runCatching {
                val userId = currentUserId() ?: return@runCatching ""
                val programPart = buildProgramContext(userId)

                // Profil bilgilerini çek
                val profileDto = runCatching {
                    supabase.postgrest["profiles"]
                        .select { filter { eq("user_id", userId) } }
                        .decodeSingleOrNull<com.cosmibit.profitness.data.profile.dto.ProfileDto>()
                }.getOrNull()

                // Room'daki en güncel kilo kaydını profil kilosundan öne al
                val latestWeightLog = runCatching {
                    weightRepository.getRecent(userId, limit = 1).getOrNull()?.firstOrNull()
                }.getOrNull()

                // En çok takip edilen 5 hareket özeti (ilerleme & yük bağlamı)
                val trackedSummaries = runCatching {
                    workoutRepository.getTrackedExerciseSummaries(userId).getOrNull().orEmpty()
                }.getOrDefault(emptyList())

                val trLocale = Locale.forLanguageTag("tr-TR")
                val today = LocalDate.now()
                val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, trLocale)
                val age = profileDto?.birth_date?.let { raw ->
                    runCatching {
                        val dob = LocalDate.parse(raw.take(10))
                        Period.between(dob, today).years.takeIf { it in 8..100 }
                    }.getOrNull()
                }

                val sb = StringBuilder()
                sb.append("\nKULLANICI PROFİLİ:")
                sb.append("\nBugünün Tarihi: $today ($dayName)")

                profileDto?.display_name?.takeIf { it.isNotBlank() }?.let { sb.append("\nİsim: $it") }
                profileDto?.gender?.takeIf { it.isNotBlank() }?.let { sb.append("\nCinsiyet: $it") }
                age?.let { sb.append("\nYaş: $it") }

                val h = profileDto?.height_cm ?: 0.0
                val w = latestWeightLog?.weightKg?.takeIf { it > 0.0 } ?: (profileDto?.weight_kg ?: 0.0)
                if (h > 0) sb.append("\nBoy: ${h.toInt()} cm")
                if (w > 0) {
                    val weightLabel = if (latestWeightLog != null) {
                        val datePart = latestWeightLog.recordedAt.take(10)
                        "${String.format(trLocale, "%.1f", w)} kg (son ölçüm: $datePart)"
                    } else {
                        "${String.format(trLocale, "%.1f", w)} kg"
                    }
                    sb.append("\nKilo: $weightLabel")
                }
                if (h > 0 && w > 0) {
                    val bmi = w / ((h / 100) * (h / 100))
                    val bmiLabel = when {
                        bmi < 18.5 -> "Zayıf"
                        bmi < 25.0 -> "Normal kilolu"
                        bmi < 30.0 -> "Fazla kilolu"
                        else       -> "Obez"
                    }
                    sb.append("\nBMI: ${String.format(trLocale, "%.1f", bmi)} ($bmiLabel)")
                }
                profileDto?.fitness_goal?.takeIf { it.isNotBlank() }?.let {
                    sb.append("\nFitness Hedefi: $it")
                    sb.append("\nNOT: Kullanıcının fitness hedefini göz önünde bulundurarak öneride bulun.")
                }

                if (trackedSummaries.isNotEmpty()) {
                    sb.append("\nÖne Çıkan Takip Edilen Hareketler:")
                    trackedSummaries.take(5).forEach { ex ->
                        val details = buildList {
                            if (ex.maxWeight > 0f) {
                                add("maks ${String.format(trLocale, "%.1f", ex.maxWeight)} kg")
                            }
                            if (ex.lastWeight > 0f) {
                                add("son ${String.format(trLocale, "%.1f", ex.lastWeight)} kg")
                            }
                            add("${ex.sessionCount} seans")
                            ex.lastDate?.takeIf { it.isNotBlank() }?.let { add("son: ${it.take(10)}") }
                        }.joinToString(" · ")
                        sb.append("\n  • ${ex.name}: $details")
                    }
                }

                if (programPart.isNotEmpty()) sb.append(programPart)

                sb.toString()
            }.getOrDefault("")
        }
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

    private fun flexIntOrNull(obj: JsonObject, key: String): Int? {
        val el = obj[key]?.jsonPrimitive ?: return null
        return el.intOrNull
            ?: el.contentOrNull?.split("-", "/", "–")?.firstOrNull()?.trim()?.toIntOrNull()
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
                sb.append("\n  • ${day.title}${day.notes.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()}:")
                day.exercises.forEach { exercise ->
                    val details = buildList {
                        add("${exercise.sets}x${exercise.reps}")
                        if (exercise.weightKg > 0f) add("${exercise.weightKg} kg")
                        add("${exercise.restSeconds} sn dinlenme")
                        exercise.section.takeIf { it.isNotBlank() }?.let(::add)
                        exercise.groupLabel.takeIf { it.isNotBlank() }?.let(::add)
                        exercise.groupRounds?.let { add("$it tur") }
                        exercise.notes.takeIf { it.isNotBlank() }?.let(::add)
                    }.joinToString(" · ")
                    sb.append("\n    - ${exercise.exerciseName}: $details")
                }
            }
        }

        return sb.toString()
    }
}
