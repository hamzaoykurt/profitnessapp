package com.cosmibit.profitness.presentation.workout

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Ağırlık sayısı yazımı (birim hariç): gereksiz ",0" yok, yerel ondalık ayırıcı, en fazla 2 hane.
 * 29.0 → "29" · 27.5 (tr) → "27,5" · 27.5 (en) → "27.5" · 27.25 → "27,25".
 */
fun formatWeightNumber(kg: Float, locale: Locale): String {
    val df = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale))
    df.roundingMode = RoundingMode.HALF_UP
    // Float → BigDecimal(toString): 27.3f'nin ikili gösterim hatası yuvarlamayı bozmasın
    return df.format(BigDecimal(kg.toString()))
}

/**
 * Kullanıcı girdisi (ör. "29.0", "27,5") için aynı yazım; sayı değilse girdiyi olduğu gibi döner.
 */
fun formatWeightNumber(raw: String, locale: Locale): String {
    val v = raw.trim().replace(',', '.').toFloatOrNull() ?: return raw.trim()
    return formatWeightNumber(v, locale)
}
