package com.cosmibit.profitness.data.ai

import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class AiAnalysisPrompt(
    val systemPrompt: String,
    val userMessage: String
)

data class WeightProfileContext(
    val heightCm: Double = 0.0,
    val fitnessGoal: String = "",
    val gender: String = "",
    val age: Int? = null
)

data class ExercisePlanContext(
    val plannedSets: Int = 0,
    val plannedReps: String = "",
    val restSeconds: Int = 0,
    val isBodyweight: Boolean = false
)

object AiAnalysisPrompts {

    private const val DETRAINING_THRESHOLD_DAYS = 21L

    fun exerciseProgression(
        exerciseName: String,
        targetMuscle: String,
        history: List<SetCompletionEntity>,
        planContext: ExercisePlanContext? = null,
        today: LocalDate = LocalDate.now(),
        isEnglish: Boolean = false
    ): AiAnalysisPrompt? {
        val locale = localeFor(isEnglish)
        val sessions = history
            .filter { !it.deleted && hasTrackableData(it) }
            .groupBy { it.date.take(10) }
            .entries
            .sortedBy { it.key }
            .map { (date, sets) -> ExerciseSessionSummary(date, sets.sortedBy { it.setIndex }) }

        if (sessions.isEmpty()) return null

        val latest = sessions.last()
        val previous = sessions.dropLast(1).lastOrNull()
        val isWeighted = sessions.any { it.maxWeightKg != null }
        val isCardio = !isWeighted && sessions.any { it.durationSeconds > 0 || it.distanceMeters > 0f }

        val best = when {
            isWeighted -> sessions.maxWithOrNull(
                compareBy<ExerciseSessionSummary> { it.bestE1RM ?: 0.0 }
                    .thenBy { it.maxWeightKg ?: 0f }
                    .thenBy { it.volumeKg }
            )
            isCardio -> sessions.maxWithOrNull(
                compareBy<ExerciseSessionSummary> { it.distanceMeters }
                    .thenBy { it.durationSeconds }
            )
            else -> sessions.maxWithOrNull(
                compareBy<ExerciseSessionSummary> { it.totalReps }
                    .thenBy { it.bestSetReps ?: 0 }
            )
        }

        val recent = sessions.takeLast(8).joinToString("\n") { it.toPromptLine(locale, isEnglish) }
        val trendSummary = buildExerciseTrendSummary(sessions, isWeighted, isCardio, locale, isEnglish)

        val lastDate = runCatching { LocalDate.parse(latest.date) }.getOrNull()
        val daysSinceLast = lastDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0L) }
        val detrainingNote = if (daysSinceLast != null && daysSinceLast >= DETRAINING_THRESHOLD_DAYS) {
            if (isEnglish) {
                "DETRAINING GAP: $daysSinceLast days since last logged session. Do NOT recommend exceeding the previous peak load; advise resuming around 85-90% of previous working weight with solid technique first."
            } else {
                "ARA UYARISI: Son kayıttan bu yana $daysSinceLast gün geçmiş. Eski zirve ağırlığın üstüne çıkmayı önerME; önce form güvenliği için önceki çalışma ağırlığının yaklaşık %85-90'ı ile kontrollü dönüş öner."
            }
        } else null

        val planLine = planContext?.let { ctx ->
            val parts = buildList {
                if (ctx.plannedSets > 0 && ctx.plannedReps.isNotBlank()) add("${ctx.plannedSets}x${ctx.plannedReps}")
                else if (ctx.plannedSets > 0) add(if (isEnglish) "${ctx.plannedSets} sets" else "${ctx.plannedSets} set")
                if (ctx.restSeconds > 0) add(if (isEnglish) "${ctx.restSeconds}s rest" else "${ctx.restSeconds}sn dinlenme")
                if (ctx.isBodyweight) add(if (isEnglish) "bodyweight" else "vücut ağırlığı")
            }
            if (parts.isEmpty()) null else parts.joinToString(", ")
        }

        val systemPrompt = if (isEnglish) {
            """
                You are Profitness's data-driven performance coach.
                Respond in English. Never invent numbers; rely strictly on the provided workout history and plan.
                Avoid generic hype or filler. Reference the user's real numbers directly.
                If there is only 1 session, do NOT treat it as a trend; call it a baseline reference session.
                If a detraining gap (>=21 days) is flagged, prioritize a safe ~85-90% ramp-back over load increases.
                For weighted strength work, realistic increments are ~2.5 kg or 1-2 reps for upper body, ~2.5-5 kg for lower body, and only when technique stays clean.
                For bodyweight-only movements, guide progression via reps, tempo, or controlled sets without inventing kg targets.
                For cardio/timed work, guide progression via duration, distance, or pace consistency.
                Provide informational training guidance only; never make medical diagnoses or claims.
                Write 4-6 concise sentences with no markdown, bullet symbols, or headings.
            """.trimIndent()
        } else {
            """
                Sen Profitness'in veri odaklı performans koçusun.
                Türkçe yanıt ver. Verilmeyen bilgiyi uydurma; sadece sağlanan antrenman geçmişine ve plana dayan.
                Genel motivasyon konuşması yapma. Kullanıcının sayısal verilerini açıkça kullan.
                Tek seans varsa bunu trend gibi yorumlama; "şimdilik referans seans" de.
                Uzun ara (>=21 gün) bildirildiyse ağırlık artışı yerine önceki yükün yaklaşık %85-90'ıyla güvenli dönüş öner.
                Ağırlıklı hareketlerde üst vücutta genelde 2,5 kg veya 1-2 tekrar, alt vücutta 2,5-5 kg mantıklı artış aralığıdır; form bozuluyorsa ağırlık artışı önerme.
                Sadece vücut ağırlığıyla yapılan hareketlerde kg hedefi uydurma; tekrar, set veya kontrollü tempo üzerinden hedef ver.
                Kardiyo/süre odaklı hareketlerde süre, mesafe veya tempo üzerinden ilerleme öner.
                Yalnızca genel antrenman bilgilendirmesi yap; tıbbi teşhis veya iddia verme.
                4-6 kısa cümle yaz; markdown, madde işareti veya başlık kullanma.
            """.trimIndent()
        }

        val userMessage = if (isEnglish) {
            buildString {
                appendLine("Exercise: $exerciseName")
                appendLine("Target muscle/group: ${targetMuscle.ifBlank { "Unknown" }}")
                if (planLine != null) appendLine("Current program target: $planLine")
                appendLine("Session count: ${sessions.size}")
                appendLine("Date range: ${sessions.first().date} - ${latest.date}")
                if (daysSinceLast != null) appendLine("Days since last session: $daysSinceLast")
                if (detrainingNote != null) appendLine(detrainingNote)
                appendLine("Best session: ${best?.toPromptLine(locale, true) ?: "None"}")
                appendLine("Latest session: ${latest.toPromptLine(locale, true)}")
                appendLine("Previous session: ${previous?.toPromptLine(locale, true) ?: "None"}")
                appendLine("Trend summary: $trendSummary")
                appendLine()
                appendLine("Recent sessions:")
                appendLine(recent)
                appendLine()
                append("Give a realistic progression analysis based on these numbers. Clearly state the next session target (weight/reps/duration/distance), when to progress, and what technique or recovery cue to watch.")
            }
        } else {
            buildString {
                appendLine("Egzersiz: $exerciseName")
                appendLine("Hedef kas/grup: ${targetMuscle.ifBlank { "Bilinmiyor" }}")
                if (planLine != null) appendLine("Program hedefi: $planLine")
                appendLine("Seans sayısı: ${sessions.size}")
                appendLine("Tarih aralığı: ${sessions.first().date} - ${latest.date}")
                if (daysSinceLast != null) appendLine("Son seanstan beri geçen gün: $daysSinceLast")
                if (detrainingNote != null) appendLine(detrainingNote)
                appendLine("En iyi seans: ${best?.toPromptLine(locale, false) ?: "Yok"}")
                appendLine("Son seans: ${latest.toPromptLine(locale, false)}")
                appendLine("Önceki seans: ${previous?.toPromptLine(locale, false) ?: "Yok"}")
                appendLine("Trend özeti: $trendSummary")
                appendLine()
                appendLine("Son seanslar:")
                appendLine(recent)
                appendLine()
                append("Bu veriye göre gerçekçi gelişim analizi yap. Bir sonraki antrenmanda hangi ağırlık/tekrar/süre/mesafe hedeflenmeli, ne zaman artırılmalı ve form/toparlanma açısından neye dikkat edilmeli net söyle.")
            }
        }

        return AiAnalysisPrompt(systemPrompt, userMessage)
    }

    fun weightTrend(
        entries: List<WeightLogEntity>,
        profile: WeightProfileContext? = null,
        isEnglish: Boolean = false
    ): AiAnalysisPrompt? {
        if (entries.size < 2) return null
        val locale = localeFor(isEnglish)

        val parsed = entries.mapNotNull { entry ->
            val date = parseLocalDate(entry.recordedAt) ?: return@mapNotNull null
            ParsedWeightEntry(
                entry = entry,
                date = date,
                sortKey = parseSortInstantMillis(entry.recordedAt)
            )
        }.sortedWith(compareBy<ParsedWeightEntry> { it.sortKey }.thenBy { it.entry.recordedAt })

        if (parsed.size < 2) return null

        val first = parsed.first()
        val latest = parsed.last()
        val delta = latest.entry.weightKg - first.entry.weightKg
        val spanDays = ChronoUnit.DAYS.between(first.date, latest.date).coerceAtLeast(0L)

        val dailyGroups = parsed.groupBy { it.date }.toSortedMap()
        val dailyLines = dailyGroups.entries.toList().takeLast(30).joinToString("; ") { (date, list) ->
            val avgKg = list.map { it.entry.weightKg }.average()
            val note = list.mapNotNull { it.entry.note.trim().takeIf { n -> n.isNotBlank() }?.take(60) }.lastOrNull()
            val countTag = if (list.size > 1) {
                if (isEnglish) " (${list.size} logs)" else " (${list.size} ölçüm)"
            } else ""
            val noteTag = if (note != null) " [$note]" else ""
            "$date: ${kg(avgKg, locale)}$countTag$noteTag"
        }

        val weeklyRateLine = when {
            spanDays == 0L -> if (isEnglish) {
                "Same-day measurements only (records from multiple days are needed for a real trend)"
            } else {
                "Aynı gün ölçümleri (trend için farklı günlerden kayıt gerekli)"
            }
            spanDays >= 7L -> {
                val perWeek = delta / (spanDays / 7.0)
                if (isEnglish) "${signedKg(perWeek, locale)}/week across $spanDays days"
                else "$spanDays günde haftalık ortalama ${signedKg(perWeek, locale)}"
            }
            else -> if (isEnglish) {
                "Short window ($spanDays days; too short for a reliable weekly rate)"
            } else {
                "Kısa aralık ($spanDays gün; haftalık hız hesabı için henüz kısa)"
            }
        }

        val recent7 = parsed.filter { ChronoUnit.DAYS.between(it.date, latest.date) in 0L..6L }
            .map { it.entry.weightKg }.averageOrNull()
        val prev7 = parsed.filter { ChronoUnit.DAYS.between(it.date, latest.date) in 7L..13L }
            .map { it.entry.weightKg }.averageOrNull()
        val rollingComparison = if (recent7 != null && prev7 != null) {
            val diff = recent7 - prev7
            if (isEnglish) {
                "Last 7-day avg ${kg(recent7, locale)} vs prior 7-day avg ${kg(prev7, locale)} (${signedKg(diff, locale)})"
            } else {
                "Son 7 gün ort. ${kg(recent7, locale)} vs önceki 7 gün ort. ${kg(prev7, locale)} (${signedKg(diff, locale)})"
            }
        } else null

        val direction = when {
            spanDays == 0L -> if (isEnglish) "same-day fluctuation" else "aynı gün içi ölçüm farkı"
            abs(delta) < 0.2 -> if (isEnglish) "stable" else "stabil"
            delta > 0.0 -> if (isEnglish) "upward" else "artış"
            else -> if (isEnglish) "downward" else "düşüş"
        }

        val profileBlock = profile?.let { p ->
            val lines = buildList {
                if (p.fitnessGoal.isNotBlank()) {
                    add(if (isEnglish) "Stated goal: ${p.fitnessGoal}" else "Kayıtlı hedef: ${p.fitnessGoal}")
                }
                if (p.heightCm > 0.0) {
                    val bmi = latest.entry.weightKg / ((p.heightCm / 100.0) * (p.heightCm / 100.0))
                    val bmiStr = String.format(locale, "%.1f", bmi)
                    add(
                        if (isEnglish) "Height: ${p.heightCm.roundToInt()} cm (current BMI $bmiStr)"
                        else "Boy: ${p.heightCm.roundToInt()} cm (güncel BMI $bmiStr)"
                    )
                }
                p.age?.let { add(if (isEnglish) "Age: $it" else "Yaş: $it") }
            }
            if (lines.isEmpty()) null else lines.joinToString(" · ")
        }

        val systemPrompt = if (isEnglish) {
            """
                You are Profitness's body weight trend coach.
                Respond in English. Base your read strictly on the provided entries and profile context; never invent goals or numbers.
                If all entries fall on the same day, state clearly that multiple days are needed for a real trend and do NOT extrapolate a weekly loss/gain rate.
                Interpret normal day-to-day water/food fluctuations calmly. Note that roughly 0.5-1% body weight or ~0.5-1 kg per week is often a sustainable reference range, without giving absolute rules.
                Keep an informational coaching tone only; never make medical diagnoses, prescribe diets, or suggest medication.
                Write 3-5 concise sentences with real numbers and one practical next weigh-in habit; no markdown or headings.
            """.trimIndent()
        } else {
            """
                Sen Profitness'in vücut ağırlığı trend koçusun.
                Türkçe yanıt ver. Sadece verilen kilo kayıtlarına ve profil bilgilerine dayan; hedef veya sayı uydurma.
                Tüm ölçümler aynı güne aitse bunun gerçek bir trend olmadığını açıkça söyle ve haftalık artış/düşüş hızı çıkarma.
                Günlük su ve öğün kaynaklı dalgalanmaları dikkate al; haftada yaklaşık 0,5-1 kg değişimin çoğu kullanıcı için daha sürdürülebilir bir referans olduğunu hatırla ama kesin hüküm verme.
                Yalnızca genel bilgilendirme amaçlı koçluk yorumu yap; tıbbi teşhis, tedavi veya diyet reçetesi verme.
                3-5 kısa cümle yaz; gerçek sayıları ve bir sonraki pratik takip adımını belirt. Markdown veya başlık kullanma.
            """.trimIndent()
        }

        val userMessage = if (isEnglish) {
            buildString {
                if (profileBlock != null) appendLine("Profile context: $profileBlock")
                appendLine("Entries: ${parsed.size} (${dailyGroups.size} distinct days)")
                appendLine("Date range: ${first.date} - ${latest.date} ($spanDays days)")
                appendLine("First entry: ${kg(first.entry.weightKg, locale)} (${first.date})")
                appendLine("Latest entry: ${kg(latest.entry.weightKg, locale)} (${latest.date})")
                appendLine("Total change: ${signedKg(delta, locale)}")
                appendLine("Pace: $weeklyRateLine")
                if (rollingComparison != null) appendLine("7-day rolling: $rollingComparison")
                appendLine("Direction: $direction")
                appendLine("Daily log summary: $dailyLines")
                appendLine()
                append("Evaluate this weight trend realistically. Account for short-term weigh-in fluctuations, alignment with the stated goal if known, and suggest one practical next tracking step.")
            }
        } else {
            buildString {
                if (profileBlock != null) appendLine("Profil bilgisi: $profileBlock")
                appendLine("Kayıt sayısı: ${parsed.size} (${dailyGroups.size} farklı gün)")
                appendLine("Tarih aralığı: ${first.date} - ${latest.date} ($spanDays gün)")
                appendLine("İlk kayıt: ${kg(first.entry.weightKg, locale)} (${first.date})")
                appendLine("Son kayıt: ${kg(latest.entry.weightKg, locale)} (${latest.date})")
                appendLine("Toplam değişim: ${signedKg(delta, locale)}")
                appendLine("Hız özeti: $weeklyRateLine")
                if (rollingComparison != null) appendLine("7 günlük ortalama: $rollingComparison")
                appendLine("Trend yönü: $direction")
                appendLine("Günlük kayıt özeti: $dailyLines")
                appendLine()
                append("Bu kilo trendini gerçekçi yorumla. Ölçüm dalgalanmalarını, biliniyorsa profil hedefini dikkate al; sürdürülebilirlik ve sonraki takip adımı öner.")
            }
        }

        return AiAnalysisPrompt(systemPrompt, userMessage)
    }

    private fun buildExerciseTrendSummary(
        sessions: List<ExerciseSessionSummary>,
        isWeighted: Boolean,
        isCardio: Boolean,
        locale: Locale,
        isEnglish: Boolean
    ): String {
        if (sessions.size < 2) {
            return if (isEnglish) {
                "Single session recorded so far (baseline reference only)"
            } else {
                "Şimdilik tek seans kayıtlı (yalnızca referans seans)"
            }
        }

        val recentSlice = if (sessions.size >= 6) sessions.takeLast(3) else listOf(sessions.last())
        val priorSlice = if (sessions.size >= 6) sessions.dropLast(3).takeLast(3) else listOf(sessions.first())
        val windowLabel = if (sessions.size >= 6) {
            if (isEnglish) "Recent 3 vs prior 3 sessions" else "Son 3 seans vs önceki 3 seans"
        } else {
            if (isEnglish) "Latest vs first session" else "Son seans vs ilk seans"
        }

        return when {
            isWeighted -> {
                val rVol = recentSlice.map { it.volumeKg }.filter { it > 0.0 }.averageOrNull()
                val pVol = priorSlice.map { it.volumeKg }.filter { it > 0.0 }.averageOrNull()
                val rTop = recentSlice.mapNotNull { it.maxWeightKg?.toDouble() }.averageOrNull()
                val pTop = priorSlice.mapNotNull { it.maxWeightKg?.toDouble() }.averageOrNull()
                val rE1rm = recentSlice.mapNotNull { it.bestE1RM }.averageOrNull()
                val pE1rm = priorSlice.mapNotNull { it.bestE1RM }.averageOrNull()
                buildList {
                    if (rTop != null && pTop != null) {
                        add(
                            if (isEnglish) "$windowLabel top weight ${kg(pTop, locale)} -> ${kg(rTop, locale)}"
                            else "$windowLabel en yüksek ağırlık ${kg(pTop, locale)} -> ${kg(rTop, locale)}"
                        )
                    }
                    if (rVol != null && pVol != null) {
                        val verb = when {
                            rVol > pVol * 1.05 -> if (isEnglish) "increasing" else "artıyor"
                            rVol < pVol * 0.95 -> if (isEnglish) "decreasing" else "düşüyor"
                            else -> if (isEnglish) "stable" else "stabil"
                        }
                        add(
                            if (isEnglish) "volume $verb (${kg(pVol, locale)} -> ${kg(rVol, locale)})"
                            else "hacim $verb (${kg(pVol, locale)} -> ${kg(rVol, locale)})"
                        )
                    }
                    if (rE1rm != null && pE1rm != null) {
                        add("est. 1RM ${kg(pE1rm, locale)} -> ${kg(rE1rm, locale)}")
                    }
                }.ifEmpty {
                    listOf(if (isEnglish) "Not enough weighted volume comparison yet" else "Yeterli ağırlık karşılaştırması yok")
                }.joinToString("; ")
            }
            isCardio -> {
                val rDist = recentSlice.map { it.distanceMeters.toDouble() }.filter { it > 0.0 }.averageOrNull()
                val pDist = priorSlice.map { it.distanceMeters.toDouble() }.filter { it > 0.0 }.averageOrNull()
                val rDur = recentSlice.map { it.durationSeconds.toDouble() }.filter { it > 0.0 }.averageOrNull()
                val pDur = priorSlice.map { it.durationSeconds.toDouble() }.filter { it > 0.0 }.averageOrNull()
                buildList {
                    if (rDist != null && pDist != null) {
                        add(
                            if (isEnglish) "$windowLabel distance ${formatDistance(pDist.toFloat(), locale)} -> ${formatDistance(rDist.toFloat(), locale)}"
                            else "$windowLabel mesafe ${formatDistance(pDist.toFloat(), locale)} -> ${formatDistance(rDist.toFloat(), locale)}"
                        )
                    }
                    if (rDur != null && pDur != null) {
                        add(
                            if (isEnglish) "duration ${formatDuration(pDur.roundToInt(), true)} -> ${formatDuration(rDur.roundToInt(), true)}"
                            else "süre ${formatDuration(pDur.roundToInt(), false)} -> ${formatDuration(rDur.roundToInt(), false)}"
                        )
                    }
                }.ifEmpty {
                    listOf(if (isEnglish) "Cardio comparison available across sessions above" else "Kardiyo karşılaştırması yukarıdaki seanslarda")
                }.joinToString("; ")
            }
            else -> {
                val rReps = recentSlice.map { it.totalReps.toDouble() }.filter { it > 0.0 }.averageOrNull()
                val pReps = priorSlice.map { it.totalReps.toDouble() }.filter { it > 0.0 }.averageOrNull()
                if (rReps != null && pReps != null) {
                    if (isEnglish) "$windowLabel total reps ${pReps.roundToInt()} -> ${rReps.roundToInt()}"
                    else "$windowLabel toplam tekrar ${pReps.roundToInt()} -> ${rReps.roundToInt()}"
                } else {
                    if (isEnglish) "Bodyweight reps comparison across sessions"
                    else "Vücut ağırlığı tekrar karşılaştırması"
                }
            }
        }
    }

    private fun hasTrackableData(set: SetCompletionEntity): Boolean =
        (set.weightKg ?: 0f) > 0f ||
            (set.repsActual ?: 0) > 0 ||
            (set.durationSeconds ?: 0) > 0 ||
            (set.distanceMeters ?: 0f) > 0f
}

private data class ParsedWeightEntry(
    val entry: WeightLogEntity,
    val date: LocalDate,
    val sortKey: Long
)

private data class ExerciseSessionSummary(
    val date: String,
    val sets: List<SetCompletionEntity>
) {
    private val weightedSets = sets.filter { (it.weightKg ?: 0f) > 0f }
    val maxWeightKg: Float? = weightedSets.maxOfOrNull { it.weightKg!! }
    val avgWeightKg: Double? = weightedSets.map { it.weightKg!!.toDouble() }.averageOrNull()
    val totalReps: Int = sets.sumOf { (it.repsActual ?: 0).coerceAtLeast(0) }
    val bestSetReps: Int? = sets.mapNotNull { it.repsActual?.takeIf { r -> r > 0 } }.maxOrNull()
    val volumeKg: Double = weightedSets.sumOf { (it.weightKg!! * (it.repsActual ?: 0).coerceAtLeast(0)).toDouble() }
    val durationSeconds: Int = sets.sumOf { (it.durationSeconds ?: 0).coerceAtLeast(0) }
    val distanceMeters: Float = sets.sumOf { (it.distanceMeters ?: 0f).coerceAtLeast(0f).toDouble() }.toFloat()
    val bestE1RM: Double? = weightedSets.mapNotNull { set ->
        val w = set.weightKg?.toDouble() ?: return@mapNotNull null
        val r = set.repsActual?.takeIf { it in 1..12 } ?: return@mapNotNull null
        if (r == 1) w else w * (1.0 + r / 30.0)
    }.maxOrNull()

    private val bestSet: SetCompletionEntity? = sets
        .filter { (it.weightKg ?: 0f) > 0f || (it.repsActual ?: 0) > 0 }
        .maxWithOrNull(compareBy<SetCompletionEntity> { it.weightKg ?: 0f }.thenBy { it.repsActual ?: 0 })

    fun toPromptLine(locale: Locale, isEnglish: Boolean): String {
        val parts = mutableListOf<String>()
        parts += if (isEnglish) "${sets.size} sets" else "${sets.size} set"
        if (totalReps > 0) parts += if (isEnglish) "$totalReps reps" else "$totalReps tekrar"
        maxWeightKg?.let { parts += "max ${kg(it, locale)}" }
        avgWeightKg?.let { parts += (if (isEnglish) "avg " else "ort ") + kg(it, locale) }
        if (volumeKg > 0.0) parts += (if (isEnglish) "vol " else "hacim ") + kg(volumeKg, locale)
        bestE1RM?.let { parts += "e1RM ${kg(it, locale)}" }
        if (durationSeconds > 0) parts += (if (isEnglish) "time " else "süre ") + formatDuration(durationSeconds, isEnglish)
        if (distanceMeters > 0f) parts += (if (isEnglish) "dist " else "mesafe ") + formatDistance(distanceMeters, locale)
        if (distanceMeters >= 200f && durationSeconds > 0) {
            val paceSecPerKm = (durationSeconds / (distanceMeters / 1000.0)).roundToInt()
            val paceMin = paceSecPerKm / 60
            val paceSec = paceSecPerKm % 60
            parts += (if (isEnglish) "pace " else "tempo ") + "%d:%02d/km".format(paceMin, paceSec)
        }
        bestSet?.let { set ->
            val weight = set.weightKg?.takeIf { it > 0f }?.let { kg(it, locale) }
            val reps = set.repsActual?.takeIf { it > 0 }?.let { if (isEnglish) "$it reps" else "$it tekrar" }
            if (weight != null || reps != null) {
                val prefix = if (isEnglish) "best set " else "en iyi set "
                parts += prefix + listOfNotNull(weight, reps).joinToString(" x ")
            }
        }
        return "$date: ${parts.joinToString(", ")}"
    }
}

private fun localeFor(isEnglish: Boolean): Locale =
    if (isEnglish) Locale.ENGLISH else Locale.forLanguageTag("tr-TR")

private fun parseLocalDate(raw: String): LocalDate? =
    runCatching { OffsetDateTime.parse(raw).toLocalDate() }
        .getOrElse { runCatching { LocalDateTime.parse(raw).toLocalDate() }.getOrNull() }
        ?: runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()

private fun parseSortInstantMillis(raw: String): Long =
    runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        .getOrElse {
            runCatching { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        }
        ?: runCatching { LocalDate.parse(raw.take(10)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrDefault(0L)

private fun Iterable<Double>.averageOrNull(): Double? =
    toList().takeIf { it.isNotEmpty() }?.average()

private fun kg(value: Float, locale: Locale): String = kg(value.toDouble(), locale)

private fun kg(value: Double, locale: Locale): String =
    if (abs(value - value.roundToInt()) < 0.05) {
        "${value.roundToInt()} kg"
    } else {
        String.format(locale, "%.1f kg", value)
    }

private fun signedKg(value: Double, locale: Locale): String =
    if (value >= 0.0) "+${kg(value, locale)}" else "-${kg(abs(value), locale)}"

private fun formatDuration(totalSeconds: Int, isEnglish: Boolean): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minUnit = if (isEnglish) "min" else "dk"
    val secUnit = if (isEnglish) "sec" else "sn"
    return when {
        minutes > 0 && seconds > 0 -> "$minutes$minUnit $seconds$secUnit"
        minutes > 0 -> "$minutes$minUnit"
        else -> "$seconds$secUnit"
    }
}

private fun formatDistance(meters: Float, locale: Locale): String =
    if (meters >= 1000f) String.format(locale, "%.2f km", meters / 1000f) else "${meters.toInt()} m"
