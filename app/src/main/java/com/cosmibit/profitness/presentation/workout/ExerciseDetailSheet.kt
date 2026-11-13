package com.cosmibit.profitness.presentation.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import com.cosmibit.profitness.presentation.components.drawGlowDot
import com.cosmibit.profitness.presentation.components.drawGlowLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM")

/**
 * Sinema hareket detayı: kenardan kenara büyük gösterim (hareket görseli; yoksa kategori fotoğrafı),
 * altta karartma + ad + kas grubu; sonra dev rakamlar, NASIL YAPILIR, notlar, kilo geçmişi, AI analiz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(
    exercise: Exercise,
    dayNotes: String = "",
    onDismiss: () -> Unit,
    weightHistory: List<SetCompletionEntity> = emptyList(),
    aiInsight: String = "",
    isAiLoading: Boolean = false,
    isFree: Boolean = true,
    aiCredits: Int = 0,
    onRequestAiInsight: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = theme.effectiveAccentColor
    val activityMetric = remember(exercise.category, exercise.name, exercise.target, exercise.reps) {
        exerciseActivityMetric(exercise)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Sinema.page(theme),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 32.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(Sinema.line(theme))
            )
        },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ── Gösterim: kenardan kenara, altta karartma + ad + kas grubu ──
            val pageBg = Sinema.page(theme)
            val hasImage = exercise.image.isNotBlank()
            val photoUrl = if (hasImage) exercise.image else categoryImageUrl(exercise.category)
            val imageRequest = remember(context, photoUrl) {
                ImageRequest.Builder(context).data(photoUrl).size(1080, 1080).crossfade(false).build()
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Sinema.surface(theme))
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = theme.t("Hareket gösterimi", "Movement demonstration"),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (hasImage) 76.dp else 0.dp),
                    contentScale = if (hasImage) ContentScale.Fit else ContentScale.Crop
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.80f to pageBg.copy(0.80f),
                                1f to pageBg
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                ) {
                    val categoryText = theme.fitnessTermDisplayName(exercise.category)
                    val targetText = theme.fitnessTermDisplayName(exercise.target)
                    val muscle = listOf(targetText, categoryText)
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                        .joinToString(" · ")
                    if (muscle.isNotBlank()) {
                        Text(
                            muscle.sinemaUpper(theme),
                            color = accent,
                            style = Sinema.Label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Sinema.surface(theme))
                                .border(1.dp, theme.stroke.copy(0.8f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        theme.exerciseDisplayName(exercise.name),
                        style = Sinema.Title.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
                        color = Sinema.text(theme),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                // Set tipi / grup
                if (!exercise.groupId.isNullOrBlank()) {
                    val groupName = exercise.groupLabel.ifBlank {
                        when (exercise.groupType) {
                            "giant_set" -> theme.t("Dev Set", "Giant Set")
                            "circuit" -> theme.t("Devre", "Circuit")
                            else -> theme.t("Süperset", "Superset")
                        }
                    }
                    val groupDetails = buildList {
                        exercise.groupRounds?.let { add(theme.t("$it tur", "$it rounds")) }
                        exercise.groupRestSeconds?.let { add(theme.t("$it sn tur arası", "$it sec between rounds")) }
                    }.joinToString(" · ")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = listOf(groupName, groupDetails).filter { it.isNotBlank() }.joinToString(" · "),
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Sinema.surface(theme))
                            .border(1.dp, theme.stroke.copy(0.8f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Dev rakamlı istatistikler ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .sinemaCard(theme, RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activityMetric == ActivityMetric.Strength) {
                        SinemaBigStat(exercise.sets.toString(), "SET", Modifier.weight(1f), size = 40, color = accent)
                        SinemaBigStat(exercise.reps, theme.t("TEKRAR", "REPS"), Modifier.weight(1f), size = 40, color = accent)
                    } else {
                        val totalDistance = weightHistory.mapNotNull { it.distanceMeters }.sum()
                        SinemaBigStat(
                            plannedDurationLabel(exercise.reps, theme), theme.t("SÜRE", "DURATION"),
                            Modifier.weight(1f), size = 40, color = accent
                        )
                        if (activityMetric == ActivityMetric.DurationDistance) {
                            SinemaBigStat(
                                totalDistance.takeIf { it > 0f }?.let { formatDistance(it) } ?: "—",
                                theme.t("MESAFE", "DISTANCE"), Modifier.weight(1f), size = 40, color = accent
                            )
                        }
                    }
                }

                // ── Notlar ──
                if (dayNotes.isNotBlank() || exercise.notes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sinemaCard(theme, RoundedCornerShape(20.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        if (dayNotes.isNotBlank()) {
                            DetailNote(theme.t("GÜN NOTU", "DAY NOTE"), dayNotes, accent, theme)
                        }
                        if (dayNotes.isNotBlank() && exercise.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = theme.stroke)
                            Spacer(Modifier.height(12.dp))
                        }
                        if (exercise.notes.isNotBlank()) {
                            DetailNote(theme.t("HAREKET NOTU", "EXERCISE NOTE"), exercise.notes, accent, theme)
                        }
                    }
                }
            }

            // ── NASIL YAPILIR ──
            Spacer(Modifier.height(12.dp))
            SinemaSectionLabel(theme.t("NASIL YAPILIR", "HOW TO DO IT"))
            val steps = if (activityMetric == ActivityMetric.Strength) {
                listOf(
                    theme.t("Doğru başlangıç pozisyonunu al ve postürüne dikkat et.", "Start in the correct position and keep your posture clean."),
                    theme.t("Hareketi kontrollü ve yavaş bir tempoda gerçekleştir.", "Move with control at a steady tempo."),
                    theme.t("Hedef kas grubunu kasılırken hissetmeye odaklan.", "Focus on feeling the target muscle contract."),
                    theme.t("Baskı anında nefes ver, geri dönüşte nefes al.", "Exhale during effort, inhale on the return.")
                )
            } else {
                listOf(
                    theme.t("Süre hedefini antrenman kartında gir ve sayacı başlat.", "Enter the duration target on the workout card and start the timer."),
                    theme.t("Mesafe kullanılan sporlarda tamamladığın metreyi ekle.", "For distance-based sports, add the meters you completed."),
                    theme.t("Temponu sürdürülebilir tut ve performansı seanslar arasında karşılaştır.", "Keep a sustainable pace and compare performance between sessions."),
                    theme.t("Bitirdiğinde hareketi tamamlayarak kaydı performans ölçütlerine aktar.", "When finished, complete the exercise to save it to performance metrics.")
                )
            }
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                steps.forEachIndexed { i, step ->
                    if (i > 0) HorizontalDivider(color = Sinema.line(theme))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .sinemaGlow(accent.copy(0.4f), 10.dp, 0.dp, CircleShape)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                color = theme.effectiveOnAccentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            step,
                            color = Sinema.text(theme),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f).padding(top = 3.dp)
                        )
                    }
                }
            }

            // ── Kilo geçmişi ──
            val chartData = remember(weightHistory) { buildChartData(weightHistory) }
            if (chartData.size >= 2) {
                Spacer(Modifier.height(20.dp))
                ProgressionSection(chartData = chartData, accent = accent, theme = theme)
            }

            // ── AI analiz ──
            if (weightHistory.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                AiProgressionSection(
                    insight = aiInsight,
                    isLoading = isAiLoading,
                    isFree = isFree,
                    credits = aiCredits,
                    accent = accent,
                    theme = theme,
                    onRefresh = onRequestAiInsight
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailNote(
    label: String,
    text: String,
    accent: Color,
    theme: AppThemeState
) {
    Text(text = label, color = accent, style = Sinema.Label)
    Spacer(Modifier.height(6.dp))
    Text(text = text, color = Sinema.text(theme), fontSize = 15.sp, lineHeight = 22.sp)
}

// ═════════════════════════════════════════════════════════════════════════════
//  PROGRESSION CHART
// ═════════════════════════════════════════════════════════════════════════════

private data class ProgressionPoint(val date: LocalDate, val maxWeightKg: Float)

private fun buildChartData(history: List<SetCompletionEntity>): List<ProgressionPoint> =
    history
        .filter { it.weightKg != null }
        .groupBy { it.date }
        .map { (date, sets) ->
            ProgressionPoint(
                date = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() },
                maxWeightKg = sets.maxOf { it.weightKg!! }
            )
        }
        .sortedBy { it.date }

private enum class ActivityMetric { Strength, Duration, DurationDistance }

private fun exerciseActivityMetric(exercise: Exercise): ActivityMetric {
    val haystack = listOf(exercise.category, exercise.name, exercise.target, exercise.reps)
        .joinToString(" ")
        .lowercase()
    val distanceKeywords = listOf(
        "koş", "kos", "run", "jog", "bisiklet", "bike", "cycle", "cycling",
        "yüz", "yuz", "swim", "yürüy", "yuruy", "walk", "kürek", "kurek", "row"
    )
    if (distanceKeywords.any { it in haystack }) return ActivityMetric.DurationDistance
    val durationKeywords = listOf(
        "kardiyo", "cardio", "dayan", "endurance", "elliptical",
        "tempo", "interval", "hiit", "plank", "yoga", "pilates", "mobility"
    )
    return if (
        durationKeywords.any { it in haystack } ||
        exercise.reps.contains("s", ignoreCase = true) ||
        exercise.reps.contains("dk", ignoreCase = true) ||
        exercise.reps.contains("min", ignoreCase = true)
    ) ActivityMetric.Duration else ActivityMetric.Strength
}

private fun plannedDurationLabel(reps: String, theme: AppThemeState): String {
    val raw = reps.trim()
    val number = raw.filter { it.isDigit() }.toIntOrNull()
    return when {
        raw.contains("dk", ignoreCase = true) || raw.contains("min", ignoreCase = true) -> raw
        raw.contains("s", ignoreCase = true) -> raw
        number != null -> "$number ${theme.t("dk", "min")}"
        else -> raw.ifBlank { "—" }
    }
}

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) "${"%.1f".format(meters / 1000f)} km" else "${meters.toInt()} m"

@Composable
private fun ProgressionSection(
    chartData: List<ProgressionPoint>,
    accent: Color,
    theme: AppThemeState
) {
    val firstKg = chartData.first().maxWeightKg
    val lastKg = chartData.last().maxWeightKg
    val delta = lastKg - firstKg
    val isGain = delta >= 0
    val minKg = (chartData.minOf { it.maxWeightKg } - 2f).coerceAtLeast(0f)
    val maxKg = chartData.maxOf { it.maxWeightKg } + 2f
    val range = (maxKg - minKg).coerceAtLeast(1f)
    val gridColor = Sinema.line(theme)
    val chartDesc = theme.t(
        "Ağırlık grafiği: ${"%.1f".format(firstKg)} kg'dan ${"%.1f".format(lastKg)} kg'a",
        "Weight chart: ${"%.1f".format(firstKg)} kg to ${"%.1f".format(lastKg)} kg"
    )

    SinemaSectionLabel(theme.t("İLERLEME", "PROGRESS"))
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            SinemaBigStat(
                "%.1f".format(lastKg), theme.t("SON AĞIRLIK", "LATEST WEIGHT"),
                Modifier.weight(1f), unit = "kg", size = 40, color = accent
            )
            Text(
                "${if (isGain) "+" else ""}${"%.1f".format(delta)} kg",
                color = if (isGain) accent else Sinema.textSecondary(theme),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Sinema.line(theme))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = chartDesc }
        ) {
            val w = size.width
            val h = size.height
            val padH = 12.dp.toPx()
            val padV = 10.dp.toPx()
            val chartW = w - padH * 2f
            val chartH = h - padV * 2f
            val n = chartData.lastIndex.coerceAtLeast(1)

            fun xOf(i: Int) = padH + (i.toFloat() / n) * chartW
            fun yOf(kg: Float) = padV + chartH * (1f - (kg - minKg) / range)

            repeat(4) { i ->
                val gy = padV + chartH * (i.toFloat() / 3f)
                drawLine(gridColor, Offset(padH, gy), Offset(w - padH, gy), 1f)
            }

            val linePath = Path()
            val fillPath = Path()
            chartData.forEachIndexed { i, pt ->
                val x = xOf(i)
                val y = yOf(pt.maxWeightKg)
                if (i == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    val px = xOf(i - 1)
                    val py = yOf(chartData[i - 1].maxWeightKg)
                    val cpX = (px + x) / 2f
                    linePath.cubicTo(cpX, py, cpX, y, x, y)
                    fillPath.cubicTo(cpX, py, cpX, y, x, y)
                }
            }
            fillPath.lineTo(xOf(chartData.lastIndex), h)
            fillPath.close()

            drawPath(fillPath, Brush.verticalGradient(listOf(accent.copy(0.18f), Color.Transparent), padV, h))
            drawGlowLine(linePath, accent, 2.5.dp.toPx())
            listOf(0, chartData.lastIndex).forEach { i ->
                drawGlowDot(Offset(xOf(i), yOf(chartData[i].maxWeightKg)), accent, 3.5.dp.toPx())
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(chartData.first().date.format(SHORT_DATE), color = Sinema.textSecondary(theme), style = Sinema.Label)
            Spacer(Modifier.weight(1f))
            Text(chartData.last().date.format(SHORT_DATE), color = Sinema.textSecondary(theme), style = Sinema.Label)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  AI INSIGHT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiProgressionSection(
    insight: String,
    isLoading: Boolean,
    isFree: Boolean,
    credits: Int,
    accent: Color,
    theme: AppThemeState,
    onRefresh: () -> Unit
) {
    val noEnergy = isFree && credits <= 0
    SinemaSectionLabel(
        theme.t("AI KOÇ", "AI COACH"),
        trailing = if (isFree) theme.t("$credits Enerji kaldı", "$credits Energy left") else null
    )
    Column(Modifier.padding(horizontal = 20.dp)) {
        if (insight.isNotBlank() || isLoading) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(theme.t("Analiz yapılıyor...", "Analyzing..."), color = Sinema.textSecondary(theme), fontSize = 14.sp)
                    }
                } else {
                    Text(insight, color = Sinema.text(theme), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        SinemaGoOrIdle(
            label = if (insight.isBlank()) theme.t("AI ANALİZİ AL", "GET AI ANALYSIS") else theme.t("YENİLE", "REFRESH"),
            icon = Icons.Rounded.AutoAwesome,
            enabled = !isLoading && !noEnergy,
            onClick = onRefresh
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (noEnergy) theme.t("Enerjin bitti. AI analizi 2 Enerji harcar.", "You're out of Energy. AI analysis costs 2 Energy.")
            else theme.t("AI analizi 2 Enerji harcar.", "AI analysis costs 2 Energy."),
            color = Sinema.textSecondary(theme),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
