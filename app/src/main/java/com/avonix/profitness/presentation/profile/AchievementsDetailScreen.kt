package com.avonix.profitness.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*

// ── Rank tanımları ──────────────────────────────────────────────────────────────

private val RANKS = listOf(
    Triple("Bronze",   0,     Color(0xFFCD7F32)),
    Triple("Silver",   1000,  Color(0xFFB0BEC5)),
    Triple("Gold",     5000,  Color(0xFFFFD700)),
    Triple("Platinum", 15000, Color(0xFF00E5FF)),
    Triple("Diamond",  50000, Color(0xFF64B5F6))
)

private fun achievementColor(category: String): Pair<Color, Color> = when (category) {
    "streak"          -> Pair(Color(0xFFF97316), Color(0xFFEF4444))
    "volume"          -> Pair(Color(0xFF9B59FF), Color(0xFF6C35DE))
    "xp"              -> Pair(Color(0xFFFFD700), Color(0xFFFF8C00))
    "milestone"       -> Pair(Color(0xFF00E5D3), Color(0xFF3B82F6))
    "total_exercises" -> Pair(Color(0xFF06B6D4), Color(0xFF0891B2))
    "level"           -> Pair(Color(0xFFA855F7), Color(0xFF7C3AED))
    else              -> Pair(Color(0xFFEC4899), Color(0xFFA855F7))
}

private val CATEGORY_LABELS = mapOf(
    "streak"          to "Seri Başarımları",
    "volume"          to "Antrenman Başarımları",
    "xp"              to "XP Başarımları",
    "milestone"       to "Kilometre Taşları",
    "total_exercises" to "Egzersiz Başarımları",
    "level"           to "Seviye Başarımları"
)

@Composable
fun AchievementsDetailScreen(
    onBack   : () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val state  by viewModel.uiState.collectAsStateWithLifecycle()

    val unlockedCount = state.achievements.count { it.isUnlocked }
    val totalCount    = state.achievements.size

    // Kategoriye göre grupla
    val grouped = state.achievements.groupBy { it.category }
        .toSortedMap(compareBy { CATEGORY_LABELS.keys.toList().indexOf(it).takeIf { i -> i >= 0 } ?: 99 })

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // ── Top bar ─────────────────────────────────────────────────────
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
                            Icons.Rounded.ArrowBackIos, null,
                            tint     = theme.text0,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "BAŞARIMLAR",
                            color         = theme.text0,
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text("Tüm başarımlar ve rank yol haritası", color = theme.text2, fontSize = 11.sp)
                    }
                    // Sayaç badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.12f))
                            .border(1.dp, accent.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$unlockedCount/$totalCount",
                            color      = accent,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ── Rank Yol Haritası ───────────────────────────────────────────
            item {
                RankRoadmapSection(
                    xp          = state.xp,
                    currentRank = state.rank,
                    accent      = accent,
                    theme       = theme,
                    modifier    = Modifier.padding(20.dp, 8.dp, 20.dp, 0.dp)
                )
            }

            // ── XP İlerleme Kartı ───────────────────────────────────────────
            item {
                XpProgressCard(
                    xp         = state.xp,
                    level      = state.level,
                    xpPerLevel = state.xpPerLevel,
                    rank       = state.rank,
                    accent     = accent,
                    theme      = theme,
                    modifier   = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp)
                )
            }

            // ── Başarım Kategorileri ────────────────────────────────────────
            grouped.forEach { (category, achievements) ->
                val categoryLabel = CATEGORY_LABELS[category] ?: category.uppercase()
                val (colorFrom, _) = achievementColor(category)
                val catUnlocked = achievements.count { it.isUnlocked }

                item(key = "header_$category") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 28.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colorFrom)
                            )
                            Text(
                                categoryLabel.uppercase(),
                                color         = colorFrom,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            "$catUnlocked/${achievements.size}",
                            color    = theme.text2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(
                    items = achievements.sortedByDescending { it.isUnlocked },
                    key   = { it.key }
                ) { ach ->
                    AchievementRow(
                        achievement = ach,
                        theme       = theme,
                        modifier    = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Rank Yol Haritası Section ────────────────────────────────────────────────

@Composable
private fun RankRoadmapSection(
    xp         : Int,
    currentRank: String,
    accent     : Color,
    theme      : AppThemeState,
    modifier   : Modifier = Modifier
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
                    val isReached = xp >= threshold
                    val isCurrent = rankName == currentRank
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
                                "$threshold XP gerekli",
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
                                Text(
                                    "MEVCUT",
                                    color         = color,
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── XP İlerleme Kartı ───────────────────────────────────────────────────────

@Composable
private fun XpProgressCard(
    xp        : Int,
    level     : Int,
    xpPerLevel: Int,
    rank      : String,
    accent    : Color,
    theme     : AppThemeState,
    modifier  : Modifier = Modifier
) {
    val xpInLevel = xp % xpPerLevel.coerceAtLeast(1)
    val progress  = xpInLevel.toFloat() / xpPerLevel.coerceAtLeast(1)

    val nextRankXp = when (rank) {
        "Bronze"   -> 1000
        "Silver"   -> 5000
        "Gold"     -> 15000
        "Platinum" -> 50000
        else       -> xp
    }
    val rankProgress = if (nextRankXp > 0) (xp.toFloat() / nextRankXp).coerceIn(0f, 1f) else 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Seviye ilerleme
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SEVİYE $level", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("$xpInLevel / $xpPerLevel XP", color = theme.text1, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(theme.bg3)) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(accent, accent.copy(0.6f))))
                    )
                }
            }

            HorizontalDivider(color = theme.stroke)

            // Sonraki rank ilerleme
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SONRAKİ RANK", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("$xp / $nextRankXp XP", color = theme.text1, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(theme.bg3)) {
                    Box(
                        Modifier
                            .fillMaxWidth(rankProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(CardGreen, CardGreen.copy(0.6f))))
                    )
                }
            }

            // Toplam XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOPLAM XP", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "$xp XP",
                    color      = accent,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ── Achievement Row ─────────────────────────────────────────────────────────

@Composable
private fun AchievementRow(
    achievement: AchievementUiModel,
    theme      : AppThemeState,
    modifier   : Modifier = Modifier
) {
    val (colorFrom, colorTo) = achievementColor(achievement.category)
    val isUnlocked = achievement.isUnlocked

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorFrom.copy(if (isUnlocked) 0.1f else 0.03f),
                        colorTo.copy(0.02f)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(600f, 0f)
                )
            )
            .border(
                1.dp,
                if (isUnlocked) colorFrom.copy(0.3f) else theme.stroke,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colorFrom.copy(if (isUnlocked) 0.15f else 0.05f))
                    .border(1.dp, if (isUnlocked) colorFrom.copy(0.4f) else theme.stroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isUnlocked) achievement.icon else "🔒",
                    fontSize = 22.sp
                )
            }

            // Info
            Column(Modifier.weight(1f)) {
                Text(
                    achievement.name,
                    color      = if (isUnlocked) theme.text0 else theme.text2,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (achievement.description.isNotBlank()) {
                    Text(
                        achievement.description,
                        color    = theme.text2,
                        fontSize = 10.sp,
                        maxLines = 2
                    )
                }
            }

            // Status
            if (isUnlocked) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorFrom.copy(0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("✓", color = colorFrom, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            } else {
                Text(
                    "${achievement.threshold}",
                    color      = theme.text2,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
