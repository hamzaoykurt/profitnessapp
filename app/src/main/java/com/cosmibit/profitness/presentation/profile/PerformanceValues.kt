package com.cosmibit.profitness.presentation.profile

/** Profil Performans hücresi türü (saf mantık; seri bilgisi bilerek YOK — üstteki istatistik satırında). */
sealed interface PerfValue {
    data class Week(val count: Int) : PerfValue
    data class Volume(val kg: Double) : PerfValue
    data class TopLift(val name: String, val kg: Float) : PerfValue
    data class Workouts(val count: Int) : PerfValue
    data class Duration(val seconds: Int) : PerfValue
    data class Distance(val meters: Float) : PerfValue
}

/**
 * Öncelik: Bu hafta antrenman (toplam antrenman > 0 ise, 0 da olsa) -> Toplam hacim -> Rekor -> Toplam süre -> Mesafe ->
 * Toplam antrenman. En çok 3 değer; hiç veri yoksa boş liste (boş durum metni gösterilir).
 */
fun performanceValues(
    weekWorkouts: Int, totalWorkouts: Int, totalVolumeKg: Double,
    topLiftName: String, topLiftKg: Float, totalDurationSeconds: Int, totalDistanceMeters: Float
): List<PerfValue> = buildList {
    if (totalWorkouts > 0 || weekWorkouts > 0) add(PerfValue.Week(weekWorkouts))
    if (totalVolumeKg > 0) add(PerfValue.Volume(totalVolumeKg))
    if (topLiftKg > 0f && topLiftName.isNotBlank()) add(PerfValue.TopLift(topLiftName, topLiftKg))
    if (totalDurationSeconds >= 60) add(PerfValue.Duration(totalDurationSeconds))
    if (totalDistanceMeters > 0f) add(PerfValue.Distance(totalDistanceMeters))
    if (totalWorkouts > 0) add(PerfValue.Workouts(totalWorkouts))
}.take(3)
