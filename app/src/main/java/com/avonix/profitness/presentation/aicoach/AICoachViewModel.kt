package com.avonix.profitness.presentation.aicoach

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.program.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class AICoachState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val programRepository: ProgramRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<AICoachState, Nothing>(AICoachState()) {

    // Gemini'ye gönderilecek konuşma geçmişi ("user"|"model", metin)
    // Karşılama mesajı geçmişe dahil edilmez — sadece UI'da görünür
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    // Karşılama mesajı — mevcut dile göre init'te set edilecek
    fun initWelcome(welcomeText: String) {
        if (uiState.value.messages.isNotEmpty()) return
        updateState { it.copy(messages = listOf(ChatMessage(id = "welcome", text = welcomeText, isUser = false))) }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = userText,
            isUser = true
        )
        updateState { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            val systemPrompt = buildSystemPrompt()
            val result = geminiRepository.chat(
                history = conversationHistory.toList(),
                userMessage = userText,
                systemPrompt = systemPrompt
            )

            result.fold(
                onSuccess = { responseText ->
                    // Başarılı yanıtı geçmişe ekle
                    conversationHistory.add("user" to userText)
                    conversationHistory.add("model" to responseText)

                    val oracleMessage = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = responseText,
                        isUser = false
                    )
                    updateState { it.copy(messages = it.messages + oracleMessage, isLoading = false) }
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = "Bağlantı hatası. Lütfen tekrar dene.",
                        isUser = false
                    )
                    updateState { it.copy(messages = it.messages + errorMessage, isLoading = false) }
                }
            )
        }
    }

    // ── Sistem Promptu ────────────────────────────────────────────────────────

    private suspend fun buildSystemPrompt(): String {
        val userId = supabase.auth.currentUserOrNull()?.id

        // Aktif programı çek (FAZ 4C — kişiselleştirilmiş bağlam)
        val programContext = if (userId != null) {
            buildProgramContext(userId)
        } else {
            ""
        }

        return """
You are Oracle — the AI fitness coach of the Profitness app. You are an expert in exercise science, sports nutrition, recovery, and athletic performance.

PERSONA:
- Concise, direct, and evidence-based (no vague or generic advice)
- Motivating but realistic — no toxic positivity or empty encouragement
- Refer to yourself as "Oracle" when appropriate

LANGUAGE RULE: Always respond in the SAME language as the user's message.
- Turkish message → Turkish response
- English message → English response

SCOPE: Only answer questions about fitness, exercise, sports nutrition, recovery, sleep, and sports psychology.
For medical conditions or injuries, always advise consulting a doctor.

RESPONSE FORMAT:
- Maximum 4 short paragraphs — plain conversational text
- No markdown headers, bullet lists, or bold text — write naturally
- End with a concrete, actionable next step the user can take today

$programContext
        """.trimIndent()
    }

    private suspend fun buildProgramContext(userId: String): String {
        val program = programRepository.getActiveProgram(userId).getOrNull() ?: return ""
        val todayIdx = LocalDate.now().dayOfWeek.value - 1 // 0=Pzt, 6=Paz
        val todayDay = program.days.firstOrNull { it.dayIndex == todayIdx }

        val programLine = "USER ACTIVE PROGRAM: ${program.name} (${program.days.size} training days)"
        val todayLine = if (todayDay != null && !todayDay.isRestDay) {
            val exerciseNames = todayDay.exercises.joinToString(", ") { it.exerciseName }
            "TODAY'S SESSION (${todayDay.title}): $exerciseNames"
        } else {
            "TODAY: Rest day"
        }

        return "\n$programLine\n$todayLine"
    }
}
