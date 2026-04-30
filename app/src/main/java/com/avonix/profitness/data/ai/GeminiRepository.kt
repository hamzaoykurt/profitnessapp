package com.avonix.profitness.data.ai

/**
 * Konuşma geçmişini ve kullanıcı mesajını Gemini API'ye gönderir,
 * Oracle'ın yanıt metnini döndürür.
 *
 * @param history Önceki mesajlar — List<Pair<role, text>> ("user" | "model")
 * @param userMessage Kullanıcının yeni mesajı (geçmişe henüz eklenmemiş)
 * @param systemPrompt Oracle'ın kimliğini ve kullanıcı bağlamını tanımlayan sistem talimatı
 */
interface GeminiRepository {
    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        systemPrompt: String,
        tool: AiToolType = AiToolType.ORACLE_CHAT
    ): Result<String>

    /** Resim veya PDF içeren multimodal istek. [imageBase64] Base64 string, [mimeType] örn. "image/jpeg" veya "application/pdf". */
    suspend fun chatWithMedia(
        imageBase64: String,
        mimeType: String,
        userMessage: String,
        systemPrompt: String,
        tool: AiToolType = AiToolType.PROGRAM_GENERATE_MEDIA
    ): Result<String>
}

enum class AiToolType {
    ORACLE_CHAT,
    PROGRAM_GENERATE_TEXT,
    PROGRAM_GENERATE_MEDIA,
    PROGRAM_EDIT,
    WEIGHT_TREND_ANALYSIS,
    EXERCISE_PROGRESS_ANALYSIS,
    WORKOUT_PROGRESS_ANALYSIS,
    ORACLE_TO_PROGRAM
}

class AiAccessException(
    val code: String,
    override val message: String
) : Exception(message)
