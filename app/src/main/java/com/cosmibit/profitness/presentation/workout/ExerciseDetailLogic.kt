package com.cosmibit.profitness.presentation.workout

import com.cosmibit.profitness.data.local.entity.SetCompletionEntity

/** "Hareketi gör" sayfasının saf mantığı: kas haritası eşlemesi + geçmiş istatistiği (UI'dan bağımsız, test edilebilir). */

internal enum class MuscleRegion { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARM, ABS, GLUTES, QUADS, HAMSTRINGS, CALVES }

private fun foldKey(value: String): String = value.trim().lowercase()
    .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
    .replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
    .replace(Regex("\\s+"), " ")

// Sıra önemli: "arka kol" "kol"dan, "ön bacak" "bacak"tan önce gelmeli. Segment başına ilk eşleşen kural kazanır.
private val MuscleRules: List<Pair<List<String>, Set<MuscleRegion>>> = listOf(
    listOf("arka kol", "triceps", "triseps") to setOf(MuscleRegion.TRICEPS),
    listOf("on kol", "forearm") to setOf(MuscleRegion.FOREARM),
    listOf("biceps", "biseps", "pazi") to setOf(MuscleRegion.BICEPS),
    listOf("arms", "kol") to setOf(MuscleRegion.BICEPS, MuscleRegion.TRICEPS),
    listOf("on bacak", "quad") to setOf(MuscleRegion.QUADS),
    listOf("arka bacak", "hamstring") to setOf(MuscleRegion.HAMSTRINGS),
    listOf("baldir", "calf", "calves") to setOf(MuscleRegion.CALVES),
    listOf("legs", "bacak") to setOf(MuscleRegion.QUADS, MuscleRegion.HAMSTRINGS, MuscleRegion.CALVES),
    listOf("gogus", "chest", "pec") to setOf(MuscleRegion.CHEST),
    listOf("sirt", "back", "lats") to setOf(MuscleRegion.BACK),
    listOf("omuz", "shoulder", "delt") to setOf(MuscleRegion.SHOULDERS),
    listOf("karin", "core", "abs", "abdominal") to setOf(MuscleRegion.ABS),
    listOf("kalca", "glute") to setOf(MuscleRegion.GLUTES)
)

/** Hedef kas metninden (TR/EN, "Chest (Göğüs)" gibi) vurgulanacak bölgeler; eşleşme yoksa boş küme (harita gizlenir). */
internal fun muscleRegionsFor(target: String): Set<MuscleRegion> {
    if (target.isBlank()) return emptySet()
    val result = linkedSetOf<MuscleRegion>()
    foldKey(target).split(Regex("[,/·()&+;|]")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { segment ->
        MuscleRules.firstOrNull { (keys, _) -> keys.any { it in segment } }?.let { result += it.second }
    }
    return result
}

internal data class HistorySession(val date: String, val sets: List<SetCompletionEntity>) {
    /** Seansin en yüksek planlı/kayıtlı ağırlığı; kg kaydı yoksa null. */
    val topWeightKg: Float? get() = sets.mapNotNull { it.weightKg }.filter { it > 0f }.maxOrNull()
    val totalDurationSeconds: Int get() = sets.sumOf { it.durationSeconds ?: 0 }
    val totalDistanceMeters: Float get() = sets.sumOf { (it.distanceMeters ?: 0f).toDouble() }.toFloat()
}

internal data class ExerciseRecord(val weightKg: Float, val reps: Int?, val date: String)

internal data class ExerciseHistoryStats(
    /** Eskiden yeniye seanslar; yalnız gerçek kaydı olan setler (bkz. [hasLoggedData]), boş seans yok. */
    val sessions: List<HistorySession>,
    val record: ExerciseRecord?
) {
    val last: HistorySession? get() = sessions.lastOrNull()
    val previous: HistorySession? get() = sessions.getOrNull(sessions.size - 2)

    /**
     * Son seansın en yüksek seti − kg'lı bir önceki seansın en yüksek seti (aynı metrik: en yüksek set vs en yüksek set).
     * Son seans kg'sız ise, karşılaştırılacak kg'lı seans yoksa ya da fark şüpheliyse ([isSuspiciousWeightJump]) null.
     */
    val topWeightDeltaKg: Float?
        get() {
            val a = last?.topWeightKg ?: return null
            val b = sessions.dropLast(1).lastOrNull { it.topWeightKg != null }?.topWeightKg ?: return null
            if (isSuspiciousWeightJump(b, a)) return null
            return a - b
        }
}

/** Setin gösterilecek gerçek bir kaydı var mı (kg > 0, tekrar > 0, süre > 0 veya mesafe > 0). Hepsi boşsa "—" satırı olur, gösterilmez. */
internal fun SetCompletionEntity.hasLoggedData(): Boolean =
    (weightKg ?: 0f) > 0f || (repsActual ?: 0) > 0 || (durationSeconds ?: 0) > 0 || (distanceMeters ?: 0f) > 0f

/**
 * Şüpheli fark: en yüksek set yarıdan fazla düşmüş ya da iki katından fazla artmışsa (ör. 35 → 5 kg) büyük olasılıkla
 * eksik/hatalı giriş veya farklı ekipman; "önceki seansa göre" metni yanıltıcı olur, gösterilmez.
 */
internal fun isSuspiciousWeightJump(previousKg: Float, lastKg: Float): Boolean =
    previousKg > 0f && (lastKg < previousKg * 0.5f || lastKg > previousKg * 2f)

internal fun buildHistoryStats(history: List<SetCompletionEntity>): ExerciseHistoryStats {
    val logged = history.filter { !it.deleted && it.hasLoggedData() }
    val sessions = logged
        .groupBy { it.date }
        .map { (date, sets) -> HistorySession(date, sets.sortedBy { it.setIndex }) }
        .sortedBy { it.date }
    val best = logged
        .filter { (it.weightKg ?: 0f) > 0f }
        .maxWithOrNull(compareBy<SetCompletionEntity> { it.weightKg!! }.thenBy { it.repsActual ?: 0 })
    return ExerciseHistoryStats(
        sessions = sessions,
        record = best?.let { ExerciseRecord(it.weightKg!!, it.repsActual, it.date) }
    )
}

internal data class ChartPoint(val date: String, val value: Float)

/** Grafik serisi: kuvvet = seans en yüksek kg; süre hareketi = seans toplam dakika. Değeri olmayan seans atlanır. */
internal fun chartSeries(sessions: List<HistorySession>, strength: Boolean): List<ChartPoint> =
    sessions.mapNotNull { s ->
        val v = if (strength) s.topWeightKg else s.totalDurationSeconds.takeIf { it > 0 }?.let { it / 60f }
        v?.let { ChartPoint(s.date, it) }
    }
