package com.avonix.profitness.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

@Composable
fun PerformanceDetailScreen(onBack: () -> Unit) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.bg1)
                            .border(1.dp, theme.stroke, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIos,
                            null,
                            tint     = theme.text0,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                    Column {
                        Text(
                            "PERFORMANS",
                            color         = theme.text0,
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text("Tüm ölçütler ve trendler", color = theme.text2, fontSize = 11.sp)
                    }
                }
            }

            // ── Body Fat Trend Chart ──────────────────────────────────────────
            item {
                TrendChartCard(
                    title    = "YAĞ ORANI TRENDİ",
                    subtitle = "Son 8 hafta (%)",
                    values   = listOf(17.2f, 16.8f, 16.1f, 15.5f, 15.0f, 14.6f, 14.2f, 14.0f),
                    color    = CardCyan,
                    theme    = theme,
                    modifier = Modifier.padding(20.dp, 8.dp, 20.dp, 0.dp)
                )
            }

            // ── Full Metrics Grid ─────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp, 28.dp, 20.dp, 0.dp)) {
                    Text(
                        "TÜM ÖLÇÜTLERİ",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = accent,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    DetailMetricsGrid(theme = theme, accent = accent)
                }
            }

            // ── Goals Progress ────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp, 32.dp, 20.dp, 0.dp)) {
                    Text(
                        "HEDEF İLERLEMESİ",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = accent,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalProgressList(theme = theme, accent = accent)
                }
            }

            // ── Monthly Workout Activity ──────────────────────────────────────
            item {
                MonthlyActivityCard(accent = accent, theme = theme)
            }
        }
    }
}

// ── Trend Line Chart ──────────────────────────────────────────────────────────

@Composable
private fun TrendChartCard(
    title   : String,
    subtitle: String,
    values  : List<Float>,
    color   : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val minVal = values.min()
    val maxVal = values.max()
    val range  = (maxVal - minVal).coerceAtLeast(0.1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = theme.text2, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "↓ ${String.format("%.1f", maxVal - minVal)}%",
                        color      = color,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Line chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w        = size.width
                val h        = size.height
                val stepX    = w / (values.size - 1)
                val padding  = 8.dp.toPx()

                // Points
                val points = values.mapIndexed { i, v ->
                    val x = i * stepX
                    val y = h - padding - ((v - minVal) / range) * (h - padding * 2)
                    Offset(x, y)
                }

                // Gradient fill under line
                val fillPath = Path().apply {
                    moveTo(points.first().x, h)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, h)
                    close()
                }
                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        colors    = listOf(color.copy(0.3f), Color.Transparent),
                        startY    = 0f,
                        endY      = h
                    )
                )

                // Line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path        = linePath,
                    color       = color,
                    style       = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Dots
                points.forEachIndexed { i, pt ->
                    drawCircle(color = color, radius = if (i == points.lastIndex) 5.dp.toPx() else 3.dp.toPx(), center = pt)
                    drawCircle(color = Color.Black, radius = if (i == points.lastIndex) 2.5.dp.toPx() else 1.5.dp.toPx(), center = pt)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Week labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8").forEach { w ->
                    Text(w, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Detailed Metrics Grid ─────────────────────────────────────────────────────

private data class DetailMetric(
    val value    : String,
    val unit     : String,
    val label    : String,
    val icon     : ImageVector,
    val color    : Color,
    val change   : String,
    val improving: Boolean
)

@Composable
private fun DetailMetricsGrid(theme: AppThemeState, accent: Color) {
    val metrics = listOf(
        DetailMetric("14",    "%",    "YAĞ ORANI",        Icons.Rounded.Speed,         CardCyan,                "↓ 3.2%",  true),
        DetailMetric("72",    "kg",   "KAS KÜTLESİ",      Icons.Rounded.FitnessCenter, accent,                  "↑ 1.8kg", true),
        DetailMetric("127",   "gün",  "AKTİF GÜN",        Icons.Rounded.CalendarToday, CardPurple,              "→ +12",   true),
        DetailMetric("12",    "seri", "GÜNLÜK SERİ",       Icons.Rounded.Whatshot,      CardCoral,               "↑ En iyi", true),
        DetailMetric("48",    "ml",   "VO2 MAX",           Icons.Rounded.Air,           CardGreen,               "↑ +2",    true),
        DetailMetric("22.4",  "BMI",  "VÜCUT KİTLE İND.", Icons.Rounded.Star,          Color(0xFFFFD700),        "→ Sağlıklı", true),
        DetailMetric("8.420", "kcal", "HAFTALIK KALORİ",  Icons.Rounded.Favorite,      CardCoral,               "↑ +320",  true),
        DetailMetric("76",    "bpm",  "DİNLENME NABZI",   Icons.Rounded.MonitorHeart,  Color(0xFFFF6B6B),        "↓ -4",    true),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMetrics.forEach { metric ->
                    DetailMetricCard(metric = metric, theme = theme, modifier = Modifier.weight(1f))
                }
                // Fill empty space if odd number
                if (rowMetrics.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    metric  : DetailMetric,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1)
            .border(1.dp, metric.color.copy(0.18f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(metric.color.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(17.dp))
                }
                Text(
                    metric.change,
                    color      = if (metric.improving) CardGreen else CardCoral,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.value,
                        color      = theme.text0,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        metric.unit,
                        color      = metric.color,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    metric.label,
                    color         = theme.text2,
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Goal Progress ─────────────────────────────────────────────────────────────

private data class GoalItem(
    val label   : String,
    val current : Float,
    val target  : Float,
    val unit    : String,
    val color   : Color
)

@Composable
private fun GoalProgressList(theme: AppThemeState, accent: Color) {
    val goals = listOf(
        GoalItem("Yağ Oranı Hedefi",    current = 14f,  target = 10f,  unit = "%",  color = CardCyan),
        GoalItem("Antrenman / Hafta",    current = 5f,   target = 6f,   unit = "gün", color = accent),
        GoalItem("Günlük Adım",          current = 7800f, target = 10000f, unit = "adım", color = CardGreen),
        GoalItem("Aktif Seri Günü",      current = 12f,  target = 30f,  unit = "gün", color = CardCoral),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(18.dp)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        goals.forEachIndexed { idx, goal ->
            GoalProgressRow(goal = goal, theme = theme)
            if (idx < goals.lastIndex) {
                HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun GoalProgressRow(goal: GoalItem, theme: AppThemeState) {
    val progress = (goal.current / goal.target).coerceIn(0f, 1f)
    val pct      = (progress * 100).toInt()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(goal.label, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${goal.current.toInt()} / ${goal.target.toInt()} ${goal.unit}",
                color    = theme.text1,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(theme.bg3)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(goal.color, goal.color.copy(0.6f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("%$pct tamamlandı", color = goal.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Monthly Activity Bar Chart ────────────────────────────────────────────────

@Composable
private fun MonthlyActivityCard(accent: Color, theme: AppThemeState) {
    val workoutsPerWeek = listOf(3, 5, 4, 6, 3, 5, 4, 5, 6, 4, 5, 3)
    val maxVal          = workoutsPerWeek.max().toFloat()

    Column(modifier = Modifier.padding(20.dp, 32.dp, 20.dp, 0.dp)) {
        Text(
            "AYLIK AKTİVİTE",
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Son 12 Hafta", color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Ort: 4.6 antrenman/hafta", color = theme.text2, fontSize = 10.sp)
                }

                Spacer(Modifier.height(16.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    val barW    = size.width / (workoutsPerWeek.size * 2 - 1)
                    val gap     = barW
                    val maxH    = size.height - 8.dp.toPx()

                    workoutsPerWeek.forEachIndexed { i, v ->
                        val barH  = (v / maxVal) * maxH
                        val left  = i * (barW + gap)
                        val top   = size.height - barH

                        drawRoundRect(
                            color        = accent.copy(if (i == workoutsPerWeek.lastIndex) 1f else 0.5f),
                            topLeft      = Offset(left, top),
                            size         = Size(barW, barH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12 hf önce", color = theme.text2, fontSize = 9.sp)
                    Text("Bu hafta", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
