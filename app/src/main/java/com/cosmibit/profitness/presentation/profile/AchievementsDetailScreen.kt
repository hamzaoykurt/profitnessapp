package com.cosmibit.profitness.presentation.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaDetailHeader
import com.cosmibit.profitness.presentation.components.SinemaGlowBar
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel

// ── Rank tanımları ──────────────────────────────────────────────────────────────

private val RANKS = listOf(
    Triple("Bronze",   0,     Color(0xFFCD7F32)),
    Triple("Silver",   1000,  Color(0xFFB0BEC5)),
    Triple("Gold",     5000,  Color(0xFFF2C23F)),
    Triple("Platinum", 15000, Color(0xFF3DB5E8)),
    Triple("Diamond",  50000, Color(0xFF64B5F6))
)

private val CATEGORY_ORDER = listOf("streak", "volume", "xp", "milestone", "total_exercises", "level")

private fun categoryLabel(category: String, theme: AppThemeState): String = when (category) {
    "streak" -> theme.t("Seri Başarımları", "Streak Achievements")
    "volume" -> theme.t("Antrenman Başarımları", "Workout Achievements")
    "xp" -> theme.t("XP Başarımları", "XP Achievements")
    "milestone" -> theme.t("Kilometre Taşları", "Milestones")
    "total_exercises" -> theme.t("Egzersiz Başarımları", "Exercise Achievements")
    "level" -> theme.t("Seviye Başarımları", "Level Achievements")
    else -> category
}

@Composable
fun AchievementsDetailScreen(
    onBack   : () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val unlockedCount = state.achievements.count { it.isUnlocked }
    val totalCount    = state.achievements.size

    // Kategoriye göre grupla
    val grouped = state.achievements.groupBy { it.category }
        .toSortedMap(compareBy { CATEGORY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: 99 })

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SinemaNavHeight + 24.dp + navBarBottom)
        ) {
            item {
                SinemaDetailHeader(
                    title    = theme.t("Başarımlar", "Achievements"),
                    subtitle = theme.t("Tüm başarımlar ve rank yol haritası", "All achievements and rank roadmap"),
                    onBack   = onBack
                )
            }

            // Dev sayaç
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    SinemaBigStat(
                        value = "$unlockedCount",
                        unit  = "/ $totalCount",
                        label = theme.t("Açılan başarım", "Unlocked"),
                        size  = 72,
                        color = theme.effectiveAccentColor
                    )
                }
            }

            item { XpProgressCard(state.xp, state.level, state.xpPerLevel, state.rank, theme, Modifier.padding(horizontal = 20.dp)) }

            item { Spacer(Modifier.height(24.dp)) }
            item { SinemaSectionLabel(theme.t("Rank yol haritası", "Rank roadmap")) }
            item { RankRoadmapSection(state.xp, state.rank, theme, Modifier.padding(horizontal = 20.dp)) }

            // ── Başarım Kategorileri ────────────────────────────────────────
            grouped.forEach { (category, achievements) ->
                val catUnlocked = achievements.count { it.isUnlocked }

                item(key = "header_$category") {
                    Spacer(Modifier.height(20.dp))
                    SinemaSectionLabel(categoryLabel(category, theme), trailing = "$catUnlocked/${achievements.size}")
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

// ── Rank Yol Haritası ────────────────────────────────────────────────────────

@Composable
private fun RankRoadmapSection(
    xp         : Int,
    currentRank: String,
    theme      : AppThemeState,
    modifier   : Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RANKS.forEach { (rankName, threshold, color) ->
            val isReached = xp >= threshold
            val isCurrent = rankName == currentRank
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isReached) color else Sinema.line(theme)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isReached) Icon(Icons.Rounded.Star, null, tint = Color.Black.copy(0.75f), modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        rankName,
                        color      = if (isReached) Sinema.text(theme) else Sinema.textSecondary(theme),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        theme.t("$threshold XP gerekli", "$threshold XP required"),
                        color    = Sinema.textSecondary(theme),
                        fontSize = 12.sp
                    )
                }
                if (isCurrent) {
                    SinemaCapsule(selected = true, height = 28.dp, contentPadding = PaddingValues(horizontal = 12.dp)) { fg ->
                        Text(
                            theme.t("MEVCUT", "CURRENT"), color = fg, fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp
                        )
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

    Column(
        modifier = modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SinemaBigStat("$level", theme.t("Seviye", "Level"), Modifier.weight(1f), size = 44)
            SinemaBigStat("$xp", theme.t("Toplam XP", "Total XP"), Modifier.weight(1.4f), size = 44)
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(theme.t("SEVİYE $level", "LEVEL $level").sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
                Text("$xpInLevel / $xpPerLevel XP", color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            SinemaGlowBar(progress)
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(theme.t("SONRAKİ RANK", "NEXT RANK").sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
                Text("$xp / $nextRankXp XP", color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            SinemaGlowBar(rankProgress)
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
    val accent = theme.effectiveAccentColor
    val isUnlocked = achievement.isUnlocked

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .then(if (isUnlocked) Modifier.sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(if (isUnlocked) accent else Sinema.line(theme)),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) Text(achievement.icon, fontSize = 22.sp)
            else Icon(Icons.Rounded.Lock, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(
                localizedAchievementText(achievement.name, theme),
                color      = if (isUnlocked) Sinema.text(theme) else Sinema.textSecondary(theme),
                fontSize   = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (achievement.description.isNotBlank()) {
                Text(
                    localizedAchievementText(achievement.description, theme),
                    color    = Sinema.textSecondary(theme),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }

        if (isUnlocked) {
            Icon(Icons.Rounded.CheckCircle, theme.t("Açıldı", "Unlocked"), tint = accent, modifier = Modifier.size(24.dp))
        } else {
            Text(
                "${achievement.threshold}",
                color      = Sinema.textSecondary(theme),
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
