package com.avonix.profitness.data.ai

import com.avonix.profitness.data.ai.dto.GeminiContent
import com.avonix.profitness.data.ai.dto.GeminiGenerationConfig
import com.avonix.profitness.data.ai.dto.GeminiInlineData
import com.avonix.profitness.data.ai.dto.GeminiPart
import com.avonix.profitness.data.ai.dto.GeminiRequest
import com.avonix.profitness.data.ai.dto.GeminiSystemInstruction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

private const val AI_EDGE_FUNCTION = "ai-generate"

class GeminiRepositoryImpl(
    private val httpClient: HttpClient,
    private val supabase: SupabaseClient,
    supabaseUrl: String,
    private val supabaseAnonKey: String
) : GeminiRepository {

    private val edgeFunctionUrl = "${supabaseUrl.trimEnd('/')}/functions/v1/$AI_EDGE_FUNCTION"

    override suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        systemPrompt: String,
        tool: AiToolType
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val contents: List<GeminiContent> = history.map { (role, text) ->
                GeminiContent(role = role, parts = listOf(GeminiPart(text = text)))
            } + GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))

            invokeEdgeFunction(
                tool = tool,
                requestBody = GeminiRequest(
                    system_instruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                    contents = contents,
                    generationConfig = GeminiGenerationConfig(temperature = 0.7, maxOutputTokens = 4096)
                )
            )
        }
    }

    override suspend fun chatWithMedia(
        imageBase64: String,
        mimeType: String,
        userMessage: String,
        systemPrompt: String,
        tool: AiToolType
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val parts = listOf(
                GeminiPart(inline_data = GeminiInlineData(mime_type = mimeType, data = imageBase64)),
                GeminiPart(text = userMessage)
            )

            invokeEdgeFunction(
                tool = tool,
                requestBody = GeminiRequest(
                    system_instruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                    contents = listOf(GeminiContent(role = "user", parts = parts)),
                    generationConfig = GeminiGenerationConfig(temperature = 0.4, maxOutputTokens = 4096)
                )
            )
        }
    }

    private suspend fun invokeEdgeFunction(
        tool: AiToolType,
        requestBody: GeminiRequest
    ): String {
        val accessToken = supabase.auth.currentSessionOrNull()?.accessToken
            ?: error("AI özelliği için giriş yapmanız gerekiyor.")

        val response = httpClient.post(edgeFunctionUrl) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("apikey", supabaseAnonKey)
            contentType(ContentType.Application.Json)
            setBody(
                AiGenerateRequest(
                    tool = tool.name,
                    idempotencyKey = UUID.randomUUID().toString(),
                    request = requestBody
                )
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<AiEdgeResponse>() }.getOrNull()
            val code = errorBody?.code ?: "ai_gateway_error"
            val message = errorBody?.message ?: "AI hizmeti şu anda yanıt vermiyor."
            if (response.status.value == 402 || response.status.value == 429) {
                throw AiAccessException(code, message)
            }
            error(message)
        }

        val body = response.body<AiEdgeResponse>()
        return body.text?.trim()?.takeIf { it.isNotBlank() }
            ?: error("Gemini boş yanıt döndürdü.")
    }
}

@Serializable
private data class AiGenerateRequest(
    val tool: String,
    val idempotencyKey: String,
    val request: GeminiRequest
)

@Serializable
private data class AiEdgeResponse(
    val text: String? = null,
    val code: String? = null,
    val message: String? = null
)
