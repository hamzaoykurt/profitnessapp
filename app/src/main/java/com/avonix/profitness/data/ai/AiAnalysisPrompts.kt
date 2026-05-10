package com.avonix.profitness.data.ai

import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.local.entity.WeightLogEntity
import kotlin.math.abs

data class AiAnalysisPrompt(
    val systemPrompt: String,
    val userMessage: String
)

object AiAnalysisPrompts {

    fun exerciseProgression(
        exerciseName: String,
        targetMuscle: String,
        history: List<SetCompletionEntity>
    ): AiAnalysisPrompt? {
        val sessions = history
            .filter { it.weightKg != null || it.durationSeconds != null || it.distanceMeters != null }
            .groupBy { it.date }
            .entries
            .sortedBy { it.key }
            .map { (date, sets) -> ExerciseSessionSummary(date, sets) }

        if (sessions.isEmpty()) return null

        val latest = sessions.last()
        val previous = sessions.dropLast(1).lastOrNull()
        val best = sessions.maxWithOrNull(
            compareBy<ExerciseSessionSummary> { it.maxWeightKg ?: 0f }
                .thenBy { it.volumeKg }
                .thenBy { it.distanceMeters }
                .thenBy { it.durationSeconds }
        )
        val recent = sessions.takeLast(8).joinToString("\n") { it.toPromptLine() }

        val recentThree = sessions.takeLast(3)
        val previousThree = sessions.dropLast(3).takeLast(3)
        val recentAvgVolume = recentThree.map { it.volumeKg }.filter { it > 0.0 }.averageOrNull()
        val previousAvgVolume = previousThree.map { it.volumeKg }.filter { it > 0.0 }.averageOrNull()
        val volumeTrend = when {
            recentAvgVolume == null || previousAvgVolume == null -> "Yeterli karşılaştırma yok"
            recentAvgVolume > previousAvgVolume * 1.05 -> "Son 3 seans hacmi önceki 3 seansa göre artıyor (${kg(recentAvgVolume)} vs ${kg(previousAvgVolume)})"
            recentAvgVolume < previousAvgVolume * 0.95 -> "Son 3 seans hacmi önceki 3 seansa göre düşüyor (${kg(recentAvgVolume)} vs ${kg(previousAvgVolume)})"
            else -> "Son 3 seans hacmi stabil (${kg(recentAvgVolume)} vs ${kg(previousAvgVolume)})"
        }

        val systemPrompt = """
            Sen Profitness'in veri odaklı performans koçusun.
            Türkçe yanıt ver. Verilmeyen bilgiyi uydurma; sadece sağlanan antrenman geçmişine dayan.
            Genel motivasyon konuşması yapma. Kullanıcının sayısal verilerini açıkça kullan.
            Tek seans varsa bunu trend gibi yorumlama; "şimdilik referans seans" de.
            Ağırlık verisi varsa sonraki antrenman için kg/tekrar/set bazında net ve gerçekçi hedef ver.
            Üst vücut egzersizlerinde genelde 2.5 kg veya 1-2 tekrar; alt vücutta 2.5-5 kg mantıklı artış aralığıdır.
            Form bozulursa ağırlığı artırmayı önerme. Sağlık/tıbbi iddia verme.
            4-6 kısa cümle yaz, markdown veya başlık kullanma.
        """.trimIndent()

        val userMessage = """
            Egzersiz: $exerciseName
            Hedef kas/grup: ${targetMuscle.ifBlank { "Bilinmiyor" }}
            Seans sayısı: ${sessions.size}
            Tarih aralığı: ${sessions.first().date} - ${sessions.last().date}
            En iyi seans: ${best?.toPromptLine() ?: "Yok"}
            Son seans: ${latest.toPromptLine()}
            Önceki seans: ${previous?.toPromptLine() ?: "Yok"}
            Hacim trendi: $volumeTrend

            Son seanslar:
            $recent

            Bu veriye göre gerçekçi gelişim analizi yap. Bir sonraki antrenmanda hangi ağırlık/tekrar hedeflenmeli, ne zaman artırılmalı ve neye dikkat edilmeli net söyle.
        """.trimIndent()

        return AiAnalysisPrompt(systemPrompt, userMessage)
    }

    fun weightTrend(entries: List<WeightLogEntity>): AiAnalysisPrompt? {
        if (entries.size < 2) return null

        val sorted = entries
            .mapNotNull { entry ->
                val date = entry.recordedAt.take(10).takeIf { it.length == 10 } ?: return@mapNotNull null
                date to entry.weightKg
            }
            .sortedBy { it.first }

        if (sorted.size < 2) return null

        val first = sorted.first()
        val latest = sorted.last()
        val delta = latest.second - first.second
        val recent = sorted.takeLast(30)
        val dataStr = recent.joinToString("; ") { (date, weight) -> "$date: ${kg(weight)}" }
        val direction = when {
            abs(delta) < 0.2 -> "stabil"
            delta > 0.0 -> "artış"
            else -> "düşüş"
        }

        val systemPrompt = """
            Sen Profitness'in vücut ağırlığı trend koçusun.
            Türkçe yanıt ver. Sadece verilen kilo kayıtlarına dayan, tıbbi teşhis veya tedavi önerisi verme.
            Kilo hedefi bilinmiyorsa hedef uydurma; trendi nötr değerlendir.
            Haftada yaklaşık 0.5-1 kg değişimin çoğu kullanıcı için daha sürdürülebilir olduğunu hatırla, ama kesin tıbbi hüküm verme.
            3-5 kısa cümle yaz. Sayıları kullan, gereksiz motivasyon cümlesi yazma.
        """.trimIndent()

        val userMessage = """
            Kayıt sayısı: ${sorted.size}
            Tarih aralığı: ${first.first} - ${latest.first}
            İlk kayıt: ${kg(first.second)}
            Son kayıt: ${kg(latest.second)}
            Toplam değişim: ${signedKg(delta)}
            Trend yönü: $direction
            Son kayıtlar: $dataStr

            Bu kilo trendini gerçekçi yorumla. Ölçüm dalgalanmalarını dikkate al, sürdürülebilirlik ve sonraki takip adımı öner.
        """.trimIndent()

        return AiAnalysisPrompt(systemPrompt, userMessage)
    }
}

private data class ExerciseSessionSummary(
    val date: String,
    val sets: List<SetCompletionEntity>
) {
    val maxWeightKg: Float? = sets.mapNotNull { it.weightKg }.maxOrNull()
    val avgWeightKg: Double? = sets.mapNotNull { it.weightKg?.toDouble() }.averageOrNull()
    val totalReps: Int = sets.mapNotNull { it.repsActual }.sum()
    val volumeKg: Double = sets.sumOf { ((it.weightKg ?: 0f) * (it.repsActual ?: 0)).toDouble() }
    val durationSeconds: Int = sets.sumOf { it.durationSeconds ?: 0 }
    val distanceMeters: Float = sets.mapNotNull { it.distanceMeters }.sum()
    private val bestSet: SetCompletionEntity? = sets
        .filter { it.weightKg != null || it.repsActual != null }
        .maxWithOrNull(compareBy<SetCompletionEntity> { it.weightKg ?: 0f }.thenBy { it.repsActual ?: 0 })

    fun toPromptLine(): String {
        val parts = mutableListOf<String>()
        parts += "${sets.size} set"
        if (totalReps > 0) parts += "$totalReps tekrar"
        maxWeightKg?.let { parts += "max ${kg(it)}" }
        avgWeightKg?.let { parts += "ort ${kg(it)}" }
        if (volumeKg > 0.0) parts += "hacim ${kg(volumeKg)}"
        if (durationSeconds > 0) parts += "süre ${formatDuration(durationSeconds)}"
        if (distanceMeters > 0f) parts += "mesafe ${formatDistance(distanceMeters)}"
        bestSet?.let { set ->
            val weight = set.weightKg?.let { kg(it) }
            val reps = set.repsActual?.let { "$it tekrar" }
            if (weight != null || reps != null) parts += "en iyi set ${listOfNotNull(weight, reps).joinToString(" x ")}"
        }
        return "$date: ${parts.joinToString(", ")}"
    }
}

private fun Iterable<Double>.averageOrNull(): Double? =
    toList().takeIf { it.isNotEmpty() }?.average()

private fun kg(value: Float): String = kg(value.toDouble())

private fun kg(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} kg" else "%.1f kg".format(value)

private fun signedKg(value: Double): String =
    if (value >= 0.0) "+${kg(value)}" else "-${kg(abs(value))}"

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}dk ${seconds}sn"
        minutes > 0 -> "${minutes}dk"
        else -> "${seconds}sn"
    }
}

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) "%.2f km".format(meters / 1000f) else "${meters.toInt()} m"
