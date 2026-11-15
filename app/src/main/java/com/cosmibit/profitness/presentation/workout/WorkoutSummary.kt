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

/** Bitiş özetinin DB'den gelen kısmı: süre (dk, hesaplanamıyorsa null) + kırılan rekor hareket adları. */
data class WorkoutFinishExtras(val minutes: Int?, val recordNames: List<String>)

private const val MIN_SESSION_MS = 60_000L
private const val MAX_SESSION_MS = 6 * 60 * 60_000L

/**
 * Süre = günün ilk ile son tamamlanmış set kaydı arasındaki fark (dk).
 * shortcut: ayrı "oturum başlangıcı" alanı yok; updated_at_ms kullanılır (set sonradan düzenlenirse son sınır kayabilir).
 * 1 dk'dan kısa (tek set) veya 6 saatten uzun (eşitleme/anormal) ise null → ekranda gösterilmez.
 */
internal fun computeSessionMinutes(firstMs: Long?, lastMs: Long?): Int? {
    if (firstMs == null || lastMs == null) return null
    val diff = lastMs - firstMs
    if (diff < MIN_SESSION_MS || diff > MAX_SESSION_MS) return null
    return ((diff + 30_000L) / 60_000L).toInt()
}

/**
 * Kırılan rekor: bugünkü en yüksek kg, önceki günlerin en yükseğinden büyük (önceki kayıt yoksa rekor sayılmaz).
 * [exercises] = (dbId, ad) sırayla; aynı hareket tekrarlanırsa bir kez döner.
 */
internal fun computeBrokenRecords(
    exercises: List<Pair<String, String>>,
    todayMaxKg: Map<String, Float>,
    previousMaxKg: Map<String, Float>
): List<String> = exercises.distinctBy { it.first }.mapNotNull { (id, name) ->
    val today = todayMaxKg[id] ?: return@mapNotNull null
    val prev = previousMaxKg[id]?.takeIf { it > 0f } ?: return@mapNotNull null
    name.takeIf { today > prev + 0.001f }
}

/** En fazla [max] ad + kalan sayısı ("+N" için). */
internal fun splitRecordBadges(names: List<String>, max: Int = 3): Pair<List<String>, Int> =
    names.take(max) to (names.size - max).coerceAtLeast(0)
