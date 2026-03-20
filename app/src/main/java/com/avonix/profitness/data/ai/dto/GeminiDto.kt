package com.avonix.profitness.data.ai.dto

import kotlinx.serialization.Serializable

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val system_instruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 600
)

// ── Response ──────────────────────────────────────────────────────────────────

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorBody? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
data class GeminiErrorBody(
    val code: Int = 0,
    val message: String = "",
    val status: String = ""
)
