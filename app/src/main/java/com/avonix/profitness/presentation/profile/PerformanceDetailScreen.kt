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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*

@Composable
fun PerformanceDetailScreen(
    onBack   : () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val state  by viewModel.uiState.collectAsStateWithLifecycle()

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
                        Text("Tüm istatistikler ve trendler", color = theme.text2, fontSize = 11.sp)
                    }
                }
            }

            // ── Haftalık Antrenman Bar Chart ──────────────────────────────────
            item {
                WorkoutBarChart(
                    counts  = state.weeklyWorkoutCounts,
                    accent  = accent,
                    theme   = theme,
                    modifier= Modifier.padding(20.dp, 8.dp, 20.dp, 0.dp)
                )
            }

            // ── Temel Metrikler ───────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp, 28.dp, 20.dp, 0.dp)) {
                    Text(
                        "TEMEL METRİKLER",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = accent,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    RealMetricsGrid(state = state, theme = theme, accent = accent)
                }
            }

            // ── Hedef İlerleme ────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp, 32.dp, 20.dp, 0.dp)) {
                    Text(
                        "HEDEF İLERLEMESİ",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = accent,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    RealGoalProgressList(state = state, theme = theme, accent = accent)
                }
            }

            // ── Rank Yol Haritası ─────────────────────────────────────────────
            item {
                RankRoadmap(
                    totalWorkouts = state.totalWorkouts,
                    currentRank   = state.rank,
                    accent        = accent,
                    theme         = theme,
                    modifier      = Modifier.padding(20.dp, 32.dp, 20.dp, 0.dp)
                )
            }
        }
    }
}

// ── Haftalık Antrenman Bar Chart (Gerçek Veri) ────────────────────────────────

@Composable
private fun WorkoutBarChart(
    counts  : List<Int>,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val maxVal   = counts.max().toFloat().coerceAtLeast(1f)
    val totalSum = counts.sum()
    val avgStr   = if (counts.isNotEmpty()) String.format("%.1f", totalSum.toFloat() / counts.size) else "0"

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
                    Text("HAFTALIK ANTRENMANlar", color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Son 13 hafta", color = theme.text2, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Ort: $avgStr/hf",
                        color      = accent,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val barW = size.width / (counts.size * 2 - 1)
                val maxH = size.height - 8.dp.toPx()

                counts.forEachIndexed { i, v ->
                    val barH = (v / maxVal) * maxH
                    val left = i * (barW + barW)
                    val top  = size.height - barH
                    drawRoundRect(
                        color        = accent.copy(if (i == counts.lastIndex) 1f else 0.45f),
                        topLeft      = Offset(left, top),
                        size         = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("13 hf önce", color = theme.text2, fontSize = 9.sp)
                Text("Bu hafta", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Gerçek Metrik Grid ────────────────────────────────────────────────────────

private data class RealMetric(
    val value : String,
    val unit  : String,
    val label : String,
    val icon  : ImageVector,
    val color : Color
)

@Composable
private fun RealMetricsGrid(
    state  : ProfileState,
    theme  : AppThemeState,
    accent : Color
) {
    val totalMin = state.totalDurationSeconds / 60
    val totalHr  = totalMin / 60
    val durationStr = if (totalHr > 0) "$totalHr" else "$totalMin"
    val durationUnit= if (totalHr > 0) "sa" else "dk"

    val metrics = listOf(
        RealMetric(state.totalWorkouts.toString(), "antrenman",  "TOPLAM ANTRENMAN",  Icons.Rounded.FitnessCenter, accent),
        RealMetric(durationStr,                    durationUnit, "TOPLAM SÜRE",        Icons.Rounded.Timer,         CardCyan),
        RealMetric(state.currentStreak.toString(), "gün",        "AKTİF SERİ",         Icons.Rounded.Whatshot,      CardCoral),
        RealMetric(state.longestStreak.toString(), "gün",        "EN UZUN SERİ",       Icons.Rounded.EmojiEvents,   CardGreen),
        RealMetric(state.level.toString(),         "seviye",     "MEVCUT SEVİYE",      Icons.Rounded.Star,          Color(0xFFFFD700)),
        RealMetric(state.xp.toString(),            "XP",         "TOPLAM XP",          Icons.Rounded.Bolt,          CardPurple),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { m ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.bg1)
                            .border(1.dp, m.color.copy(0.2f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(m.color.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(m.icon, null, tint = m.color, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(m.value, color = theme.text0, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Text(m.unit, color = m.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                }
                                Text(m.label, color = theme.text2, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Gerçek Hedef İlerlemesi ───────────────────────────────────────────────────

@Composable
private fun RealGoalProgressList(
    state  : ProfileState,
    theme  : AppThemeState,
    accent : Color
) {
    val xpInLevel  = state.xp % state.xpPerLevel.coerceAtLeast(1)
    val nextRankAt = when (state.rank) {
        "Bronze"   -> 10
        "Silver"   -> 30
        "Gold"     -> 100
        "Platinum" -> 300
        else       -> state.totalWorkouts
    }

    data class Goal(val label: String, val current: Float, val target: Float, val unit: String, val color: Color)

    val goals = listOf(
        Goal("Seviye Yükselme XP",   xpInLevel.toFloat(),          state.xpPerLevel.toFloat(), "XP",  accent),
        Goal("Aktif Seri Hedefi",    state.currentStreak.toFloat(), 30f,                        "gün", CardCoral),
        Goal("Sonraki Rank Antrenman", state.totalWorkouts.toFloat(), nextRankAt.toFloat(),      "ant", CardGreen),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(18.dp))
    ) {
        goals.forEachIndexed { idx, goal ->
            val progress = (goal.current / goal.target.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val pct      = (progress * 100).toInt()
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(goal.label, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${goal.current.toInt()} / ${goal.target.toInt()} ${goal.unit}", color = theme.text1, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(theme.bg3)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(goal.color, goal.color.copy(0.6f))))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("%$pct tamamlandı", color = goal.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (idx < goals.lastIndex) HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// ── Rank Yol Haritası ─────────────────────────────────────────────────────────

private val RANKS = listOf(
    Triple("Bronze",   0,   Color(0xFFCD7F32)),
    Triple("Silver",   10,  Color(0xFFB0BEC5)),
    Triple("Gold",     30,  Color(0xFFFFD700)),
    Triple("Platinum", 100, Color(0xFF00E5FF)),
    Triple("Diamond",  300, Color(0xFF64B5F6))
)

@Composable
private fun RankRoadmap(
    totalWorkouts: Int,
    currentRank  : String,
    accent       : Color,
    theme        : AppThemeState,
    modifier     : Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "RANK YOL HARİTASI",
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RANKS.forEach { (rankName, threshold, color) ->
                    val isReached  = totalWorkouts >= threshold
                    val isCurrent  = rankName == currentRank
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isReached) color.copy(0.25f) else theme.bg2)
                                .border(1.5.dp, if (isReached) color else theme.stroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isReached) "★" else "○",
                                color    = if (isReached) color else theme.text2,
                                fontSize = 16.sp
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                rankName.uppercase(),
                                color      = if (isReached) color else theme.text2,
                                fontSize   = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.SemiBold
                            )
                            Text(
                                "$threshold antrenman gerekli",
                                color    = theme.text2,
                                fontSize = 10.sp
                            )
                        }
                        if (isCurrent) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(color.copy(0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("MEVCUT", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
