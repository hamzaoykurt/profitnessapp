package com.avonix.profitness.domain.model

object ExerciseNameRules {
    private val whitespaceRegex = Regex("\\s+")
    private val alternativeSeparatorRegex = Regex(
        pattern = "\\s+(?:veya|ya\\s+da|or)\\s+",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun normalizedKey(name: String): String =
        name.trim().lowercase().replace(whitespaceRegex, " ")

    fun splitAlternatives(name: String): List<String> {
        val normalized = name.trim().replace(whitespaceRegex, " ")
        if (normalized.isBlank()) return emptyList()

        return alternativeSeparatorRegex
            .split(normalized)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun isCompositeName(name: String): Boolean =
        splitAlternatives(name).size > 1
}
