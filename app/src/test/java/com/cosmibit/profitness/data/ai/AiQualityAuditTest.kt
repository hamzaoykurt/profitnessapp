package com.cosmibit.profitness.data.ai

import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import com.cosmibit.profitness.data.profile.CoverImageMath
import com.cosmibit.profitness.data.profile.ProfileCover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AiQualityAuditTest {

    // ── AiAnalysisPrompts.weightTrend ─────────────────────────────────────────

    @Test fun `weightTrend requires at least two entries`() {
        val single = listOf(WeightLogEntity("1", "u", 75.0, recordedAt = "2026-11-10T08:00:00"))
        assertNull(AiAnalysisPrompts.weightTrend(emptyList()))
        assertNull(AiAnalysisPrompts.weightTrend(single))
    }

    @Test fun `weightTrend guards same-day entries and avoids fake weekly rate`() {
        val entries = listOf(
            WeightLogEntity("1", "u", 74.2, note = "sabah", recordedAt = "2026-11-18T07:30:00"),
            WeightLogEntity("2", "u", 75.1, note = "akşam", recordedAt = "2026-11-18T21:00:00")
        )
        val tr = AiAnalysisPrompts.weightTrend(entries, isEnglish = false)!!
        assertTrue(tr.userMessage.contains("Aynı gün ölçümleri"))
        assertTrue(tr.systemPrompt.contains("haftalık artış/düşüş hızı çıkarma"))
        assertFalse(tr.userMessage.contains("/week"))

        val en = AiAnalysisPrompts.weightTrend(entries, isEnglish = true)!!
        assertTrue(en.userMessage.contains("Same-day measurements only"))
        assertTrue(en.systemPrompt.contains("Respond in English"))
    }

    @Test fun `weightTrend sorts by full timestamp includes profile BMI rolling avg and notes`() {
        val entries = listOf(
            WeightLogEntity("3", "u", 78.4, note = "akşam", recordedAt = "2026-11-18T20:00:00"),
            WeightLogEntity("1", "u", 80.0, note = "başlangıç", recordedAt = "2026-11-04T07:30:00"),
            WeightLogEntity("2", "u", 79.2, recordedAt = "2026-11-11T07:30:00")
        )
        val profile = WeightProfileContext(
            heightCm = 180.0,
            fitnessGoal = "Yağ yakarken kas korumak",
            gender = "male",
            age = 29
        )
        val prompt = AiAnalysisPrompts.weightTrend(entries, profile = profile, isEnglish = false)!!
        assertTrue(prompt.userMessage.contains("Boy: 180 cm"))
        assertTrue(prompt.userMessage.contains("Yaş: 29"))
        assertTrue(prompt.userMessage.contains("Yağ yakarken kas korumak"))
        assertTrue(prompt.userMessage.contains("Son 7 gün ort."))
        assertTrue(prompt.userMessage.contains("[akşam]"))
        assertTrue(prompt.userMessage.contains("14 günde haftalık ortalama"))
        // TR locale uses comma decimals
        assertTrue(prompt.userMessage.contains("78,4 kg"))
    }

    // ── AiAnalysisPrompts.exerciseProgression ─────────────────────────────────

    @Test fun `exerciseProgression handles bodyweight reps-only without emitting 0 kg`() {
        val history = listOf(
            SetCompletionEntity("u", "pushup", "d1", 0, "2026-11-10", weightKg = null, repsActual = 12),
            SetCompletionEntity("u", "pushup", "d1", 1, "2026-11-10", weightKg = 0f, repsActual = 10),
            SetCompletionEntity("u", "pushup", "d1", 0, "2026-11-14", weightKg = null, repsActual = 15),
            SetCompletionEntity("u", "pushup", "d1", 1, "2026-11-14", weightKg = 0f, repsActual = 14)
        )
        val prompt = AiAnalysisPrompts.exerciseProgression(
            exerciseName = "Push-Up",
            targetMuscle = "Göğüs",
            history = history,
            planContext = ExercisePlanContext(plannedSets = 3, plannedReps = "12-15", restSeconds = 60, isBodyweight = true),
            today = LocalDate.of(2026, 11, 16),
            isEnglish = false
        )
        assertNotNull(prompt)
        assertFalse(prompt!!.userMessage.contains("0 kg"))
        assertTrue(prompt.userMessage.contains("29 tekrar"))
        assertTrue(prompt.userMessage.contains("vücut ağırlığı"))
    }

    @Test fun `exerciseProgression computes Epley e1RM cardio pace and 21-day detraining safety cap`() {
        val strengthHistory = listOf(
            SetCompletionEntity("u", "bench", "d1", 0, "2026-10-01", weightKg = 80f, repsActual = 5)
        )
        val detrained = AiAnalysisPrompts.exerciseProgression(
            exerciseName = "Bench Press",
            targetMuscle = "Göğüs",
            history = strengthHistory,
            planContext = ExercisePlanContext(plannedSets = 4, plannedReps = "8", restSeconds = 90),
            today = LocalDate.of(2026, 11, 20),
            isEnglish = true
        )!!
        assertTrue(detrained.userMessage.contains("e1RM 93.3 kg"))
        assertTrue(detrained.userMessage.contains("DETRAINING GAP: 50 days"))
        assertTrue(detrained.userMessage.contains("85-90%"))

        val cardioHistory = listOf(
            SetCompletionEntity("u", "run", "d1", 0, "2026-11-10", durationSeconds = 1800, distanceMeters = 5000f),
            SetCompletionEntity("u", "run", "d1", 0, "2026-11-15", durationSeconds = 1650, distanceMeters = 5000f)
        )
        val cardio = AiAnalysisPrompts.exerciseProgression(
            exerciseName = "Koşu",
            targetMuscle = "Kardiyo",
            history = cardioHistory,
            today = LocalDate.of(2026, 11, 20),
            isEnglish = false
        )!!
        assertTrue(cardio.userMessage.contains("5,00 km"))
        assertTrue(cardio.userMessage.contains("tempo 5:30/km"))
    }

    // ── AiAccessException.requiresPaywall ─────────────────────────────────────

    @Test fun `AiAccessException separates paywall codes from rate and quota limits`() {
        assertTrue(AiAccessException("premium_required", "p").requiresPaywall)
        assertTrue(AiAccessException("free_grants_used", "f").requiresPaywall)
        assertTrue(AiAccessException("insufficient_entitlement", "i").requiresPaywall)

        assertFalse(AiAccessException("rate_limited", "r").requiresPaywall)
        assertFalse(AiAccessException("ai_quota_exhausted", "q").requiresPaywall)
        assertFalse(AiAccessException("fair_use_limit", "u").requiresPaywall)
    }

    // ── ProfileCover & CoverImageMath ─────────────────────────────────────────

    @Test fun `ProfileCover default and CoverImageMath aspect helpers`() {
        assertTrue(ProfileCover().isDefault)
        assertFalse(ProfileCover(remoteUrl = "https://cdn.example/cover.jpg").isDefault)
        assertFalse(ProfileCover(preset = "ember").isDefault)

        val crop = CoverImageMath.centerCrop(2000, 1000, 1.25f)
        assertEquals(1250, crop.width)
        assertEquals(1000, crop.height)
        assertEquals(375, crop.left)
    }
}
