package com.cosmibit.profitness.presentation.workout

/** Bitiş sayfası özeti: yalnız gerçek kayıtlı set/ağırlık/tekrar verisinden hesaplanır. */
data class WorkoutSummary(val completedSets: Int, val volumeKg: Float)

/** Bir hareketin özet girdisi. [kind] Activity ise hacme katılmaz (set sayısına 1 olarak girer). */
internal class SummaryExerciseInput(
    val kind: SinemaSetKind,
    val exerciseCompleted: Boolean,
    val plannedSets: Int,
    val doneSetIndices: Set<Int>,
    val setWeights: Map<Int, String>,
    val setReps: Map<Int, String>,
    val plannedReps: String,
    val defaultWeightKg: Float
)

/** "8-12" / "10" → ilk tamsayı; yoksa null. */
internal fun leadingInt(text: String): Int? = Regex("\\d+").find(text)?.value?.toIntOrNull()

/**
 * Tamamlanan set = yapılan set sayısı (Activity: hareket bitmişse 1).
 * Hacim = Σ ağırlık × tekrar (yalnız kuvvet setleri; ağırlık kaydı/varsayılanı yoksa o set hacme girmez).
 */
internal fun computeWorkoutSummary(inputs: List<SummaryExerciseInput>): WorkoutSummary {
    var sets = 0
    var volume = 0f
    for (e in inputs) {
        if (e.kind == SinemaSetKind.Activity) {
            if (e.exerciseCompleted) sets += 1
            continue
        }
        sets += e.doneSetIndices.count { it in 0 until e.plannedSets.coerceAtLeast(1) }
        if (e.kind != SinemaSetKind.Strength) continue
        for (i in e.doneSetIndices) {
            val w = e.setWeights[i]?.replace(',', '.')?.toFloatOrNull()?.takeIf { it > 0f }
                ?: e.defaultWeightKg.takeIf { it > 0f } ?: continue
            val r = e.setReps[i]?.toIntOrNull()?.takeIf { it > 0 } ?: leadingInt(e.plannedReps) ?: continue
            volume += w * r
        }
    }
    return WorkoutSummary(completedSets = sets, volumeKg = volume)
}
