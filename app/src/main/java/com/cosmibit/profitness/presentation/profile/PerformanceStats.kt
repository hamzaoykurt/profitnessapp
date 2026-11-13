package com.cosmibit.profitness.presentation.profile

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Veri analizi sayfasının saf hesapları (Compose'tan bağımsız, birim testli).
 * Girdi: `ProfileState.weeklyWorkoutCounts` (en eskiden en yeniye, son eleman = bu hafta) ve `weeklyActivity`.
 */

/** Son [weeks] haftalık dilim (veri daha kısaysa olduğu kadar). */
internal fun weekSlice(counts: List<Int>, weeks: Int): List<Int> = counts.takeLast(weeks)

/** Haftalık ortalama antrenman (dilim boşsa 0). */
internal fun weeklyAverage(slice: List<Int>): Float =
    if (slice.isEmpty()) 0f else slice.sum().toFloat() / slice.size

/** Dilimdeki en yüksek haftalık sayı. */
internal fun bestWeek(slice: List<Int>): Int = slice.maxOrNull() ?: 0

/** Bu hafta kaç hafta önce son antrenman var: 0 = bu hafta, null = hiç yok. */
internal fun weeksSinceLastWorkout(counts: List<Int>): Int? {
    val i = counts.indexOfLast { it > 0 }
    return if (i < 0) null else counts.lastIndex - i
}

/**
 * Ritim bölümünün kahramanı ortalama mı? Ortalama 1/hf'nin altındaysa (örn. 0,1/hf) kahraman "En iyi hafta" olur;
 * dilimde hiç antrenman yoksa ortalama (0) dürüstçe kalır.
 */
internal fun averageIsHero(average: Float, best: Int): Boolean = best == 0 || average >= 1f

/** Hafta halkası yayı alfası: 0 → iz (0), >0 → .35 + .65·değer. */
internal fun ringArcAlpha(level: Float): Float {
    val v = level.coerceIn(0f, 1f)
    return if (v <= 0f) 0f else 0.35f + 0.65f * v
}

/** Kapsül genişliği (dp): 13 haftada 16, 4 haftada 56. */
internal fun capsuleWidthDp(weeks: Int): Float = if (weeks >= 13) 16f else 56f

/** Kapsül yüksekliği oranı (0..1): sayı / dilimdeki en iyi hafta. */
internal fun capsuleFraction(count: Int, best: Int): Float =
    if (best <= 0) 0f else (count.toFloat() / best).coerceIn(0f, 1f)

/** [weeksAgo] hafta önceki haftanın Pazartesi'si (0 = bu hafta). */
internal fun weekStart(today: LocalDate, weeksAgo: Int): LocalDate =
    today.with(DayOfWeek.MONDAY).minusWeeks(weeksAgo.toLong())

/** BMI ölçeği konumu (0..1): 15..35 aralığı, bölge sınırları 18,5 / 25 / 30. */
internal fun bmiScaleFraction(bmi: Double): Float = ((bmi - 15.0) / 20.0).coerceIn(0.0, 1.0).toFloat()

/** 0 Zayıf, 1 Normal, 2 Fazla kilolu, 3 Obez. */
internal fun bmiZone(bmi: Double): Int = when {
    bmi < 18.5 -> 0
    bmi < 25.0 -> 1
    bmi < 30.0 -> 2
    else -> 3
}

/** Aktif seri / en uzun seri oranı (en uzun yoksa 0). */
internal fun streakRatio(current: Int, longest: Int): Float {
    val top = maxOf(current, longest)
    return if (top <= 0) 0f else (current.toFloat() / top).coerceIn(0f, 1f)
}

internal fun uiLocale(english: Boolean): Locale =
    if (english) Locale.ENGLISH else Locale.forLanguageTag("tr-TR")

/** Binlik ayraçlı, ondalıklı sayı (TR: 2.115 / 38,4). */
internal fun formatNumber(value: Double, decimals: Int, english: Boolean): String =
    NumberFormat.getInstance(uiLocale(english)).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)

/** Toplam süre dakika olarak (0 → 0; sıfırdan büyükse en az 1). */
internal fun durationMinutes(seconds: Int): Int = if (seconds <= 0) 0 else (seconds / 60).coerceAtLeast(1)
