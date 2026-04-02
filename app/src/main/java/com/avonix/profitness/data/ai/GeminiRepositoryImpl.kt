package com.avonix.profitness.data.ai

import com.avonix.profitness.data.ai.dto.GeminiCandidate
import com.avonix.profitness.data.ai.dto.GeminiContent
import com.avonix.profitness.data.ai.dto.GeminiGenerationConfig
import com.avonix.profitness.data.ai.dto.GeminiInlineData
import com.avonix.profitness.data.ai.dto.GeminiPart
import com.avonix.profitness.data.ai.dto.GeminiRequest
import com.avonix.profitness.data.ai.dto.GeminiResponse
import com.avonix.profitness.data.ai.dto.GeminiSystemInstruction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GEMINI_BASE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

class GeminiRepositoryImpl(
    private val httpClient: HttpClient,
    private val apiKey: String
) : GeminiRepository {

    override suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val contents: List<GeminiContent> = history.map { (role, text) ->
                GeminiContent(role = role, parts = listOf(GeminiPart(text = text)))
            } + GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))

            val requestBody = GeminiRequest(
                system_instruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                contents = contents,
                generationConfig = GeminiGenerationConfig(temperature = 0.7, maxOutputTokens = 2000)
            )

            val response: GeminiResponse = httpClient.post(GEMINI_BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            response.error?.let { err ->
                error("Gemini API hatası [${err.code}]: ${err.message}")
            }

            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: error("Gemini boş yanıt döndürdü.")
        }
    }

    override suspend fun chatWithMedia(
        imageBase64: String,
        mimeType: String,
        userMessage: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val parts = listOf(
                GeminiPart(inline_data = GeminiInlineData(mime_type = mimeType, data = imageBase64)),
                GeminiPart(text = userMessage)
            )

            val requestBody = GeminiRequest(
                system_instruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                contents = listOf(GeminiContent(role = "user", parts = parts)),
                generationConfig = GeminiGenerationConfig(temperature = 0.4, maxOutputTokens = 1200)
            )

            val response: GeminiResponse = httpClient.post(GEMINI_BASE_URL) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            response.error?.let { err ->
                error("Gemini API hatası [${err.code}]: ${err.message}")
            }

            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: error("Gemini boş yanıt döndürdü.")
        }
    }
}
