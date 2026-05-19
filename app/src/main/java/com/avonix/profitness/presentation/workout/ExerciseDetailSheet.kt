package com.avonix.profitness.presentation.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.presentation.components.AiCreditInfoRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(
    exercise: Exercise,
    onDismiss: () -> Unit,
    weightHistory: List<SetCompletionEntity> = emptyList(),
    aiInsight: String = "",
    isAiLoading: Boolean = false,
    isFree: Boolean = true,
    aiCredits: Int = 0,
    onRequestAiInsight: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val activityMetric = remember(exercise.category, exercise.name, exercise.target, exercise.reps) {
        exerciseActivityMetric(exercise)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.bg1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.stroke) },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        exercise.name.uppercase(),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        exercise.target,
                        color = theme.text2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activityMetric == ActivityMetric.Strength) {
                    StatTile(label = "SET", value = exercise.sets.toString(), accent = accent, modifier = Modifier.weight(1f))
                    StatTile(label = theme.t("TEKRAR", "REPS"), value = exercise.reps, accent = accent, modifier = Modifier.weight(1f))
                } else {
                    val totalDistance = weightHistory.mapNotNull { it.distanceMeters }.sum()
                    StatTile(label = theme.t("SÜRE", "DURATION"), value = plannedDurationLabel(exercise.reps, theme), accent = accent, modifier = Modifier.weight(1f))
                    if (activityMetric == ActivityMetric.DurationDistance) {
                        StatTile(
                            label = theme.t("MESAFE", "DISTANCE"),
                            value = totalDistance.takeIf { it > 0f }?.let { formatDistance(it) } ?: "—",
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Progressive Overload Chart ──────────────────────────────────
            val chartData = remember(weightHistory) { buildChartData(weightHistory) }
            if (chartData.size >= 2) {
                Spacer(Modifier.height(20.dp))
                ProgressionSection(
                    chartData = chartData,
                    accent = accent,
                    theme = theme
                )
            }

            // ── AI Insight ──────────────────────────────────────────────────
            if (weightHistory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AiCreditInfoRow(
                    isFree    = isFree,
                    credits   = aiCredits,
                    costLabel = theme.t("2 Enerji / AI analiz", "2 Energy / AI analysis"),
                    theme     = theme
                )
                Spacer(Modifier.height(8.dp))
                AiProgressionCard(
                    insight = aiInsight,
                    isLoading = isAiLoading,
                    accent = accent,
                    theme = theme,
                    isFree = isFree,
                    credits = aiCredits,
                    onRefresh = onRequestAiInsight
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                theme.t("NASIL YAPILIR", "HOW TO DO IT"),
                color = theme.text2,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.bg2)
                            .border(1.dp, theme.stroke, RoundedCornerShape(10.dp))
                            .padding(14.dp, 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "${i + 1}",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            step,
                            color = theme.text1,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ShowChart, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(theme.t("İLERLEME", "PROGRESS"), color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))

            // Delta badge
            val chipColor = if (isGain) Color(0xFF22C55E) else Color(0xFFEF4444)
            val sign = if (isGain) "+" else ""
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipColor.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isGain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    null, tint = chipColor, modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$sign${"%.1f".format(delta)} kg",
                    color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Canvas chart
        val minKg = (chartData.minOf { it.maxWeightKg } - 2f).coerceAtLeast(0f)
        val maxKg = chartData.maxOf { it.maxWeightKg } + 2f
        val range = (maxKg - minKg).coerceAtLeast(1f)

        val accentColor = accent
        val gridColor = accent.copy(0.06f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height
            val padH = 16.dp.toPx()
            val padV = 8.dp.toPx()
            val chartW = w - padH * 2f
            val chartH = h - padV * 2f
            val n = chartData.lastIndex.coerceAtLeast(1)

            fun xOf(i: Int) = padH + (i.toFloat() / n) * chartW
            fun yOf(kg: Float) = padV + chartH * (1f - (kg - minKg) / range)

            // Grid
            repeat(4) { i ->
                val gy = padV + chartH * (i.toFloat() / 3f)
                drawLine(gridColor, Offset(padH, gy), Offset(w - padH, gy), 1f)
            }

            // Paths
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

            drawPath(fillPath, Brush.verticalGradient(listOf(accentColor.copy(0.2f), Color.Transparent), padV, h))
            drawPath(linePath, accentColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Dots: first, last
            listOf(0, chartData.lastIndex).forEach { i ->
                val x = xOf(i)
                val y = yOf(chartData[i].maxWeightKg)
                drawCircle(accentColor.copy(0.25f), 6.dp.toPx(), Offset(x, y))
                drawCircle(accentColor, 3.dp.toPx(), Offset(x, y))
            }
        }

        // Date labels
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(chartData.first().date.format(SHORT_DATE), color = theme.text2, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(chartData.last().date.format(SHORT_DATE), color = theme.text2, fontSize = 9.sp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  AI INSIGHT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiProgressionCard(
    insight: String,
    isLoading: Boolean,
    accent: Color,
    theme: AppThemeState,
    isFree: Boolean = true,
    credits: Int = 0,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(theme.t("AI KOÇ", "AI COACH"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            if (isFree && !isLoading) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(theme.t("2 Enerji", "2 Energy"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(theme.t("$credits kaldı", "$credits left"), color = theme.text2, fontSize = 9.sp)
                }
                Spacer(Modifier.width(6.dp))
            }
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(theme.bg3)
                        .clickable(enabled = !isLoading, onClick = onRefresh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = theme.text2, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            isLoading -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Analiz yapılıyor...", "Analyzing..."), color = theme.text2, fontSize = 12.sp)
            }
            insight.isBlank() -> Text(
                theme.t("AI koçundan egzersiz gelişim analizi al", "Get exercise progress analysis from your AI coach"),
                color = theme.text2, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLoading, onClick = onRefresh)
                    .padding(8.dp)
            )
            else -> Text(insight, color = theme.text1, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = theme.text2,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
    }
}
