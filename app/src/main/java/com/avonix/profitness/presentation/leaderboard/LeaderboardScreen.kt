package com.avonix.profitness.presentation.leaderboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.glassCard

@Composable
fun LeaderboardScreen(
    onBack   : () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(),
    initialTab: LeaderboardTab = LeaderboardTab.Xp
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val state  by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) { viewModel.selectTab(initialTab) }

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
                            "SIRALAMA",
                            color         = theme.text0,
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Diğer kullanıcılarla kıyasla",
                            color    = theme.text2,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Rounded.Refresh, null,
                            tint = theme.text2,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Tab seçici ──────────────────────────────────────────────────
            item {
                TabSwitcher(
                    selected = state.selectedTab,
                    onSelect = viewModel::selectTab,
                    accent   = accent,
                    theme    = theme,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ── Benim pozisyonum özet kartı ─────────────────────────────────
            item {
                val summary = when (state.selectedTab) {
                    LeaderboardTab.Xp           -> state.myXp
                    LeaderboardTab.Achievements -> state.myAchievements
                }
                MyPositionCard(
                    tab      = state.selectedTab,
                    summary  = summary,
                    accent   = accent,
                    theme    = theme,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ── Liste başlığı ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "TOP 100",
                        color         = theme.text0,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        when (state.selectedTab) {
                            LeaderboardTab.Xp           -> "XP SIRALAMASI"
                            LeaderboardTab.Achievements -> "BAŞARIM SIRALAMASI"
                        },
                        color         = accent,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // ── Durumlar: loading / error / liste ───────────────────────────
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = accent) }
                }
            } else if (state.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .glassCard(accent, theme)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sıralama yüklenemedi\n${state.error}",
                            color      = theme.text2,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val rows = when (state.selectedTab) {
                    LeaderboardTab.Xp           -> state.xpRows
                    LeaderboardTab.Achievements -> state.achievementRows
                }
                if (rows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Henüz sıralamada kimse yok",
                                color    = theme.text2,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(items = rows, key = { "${state.selectedTab}_${it.userId}" }) { row ->
                        LeaderboardRowItem(
                            row    = row,
                            tab    = state.selectedTab,
                            accent = accent,
                            theme  = theme,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Tab Switcher ──────────────────────────────────────────────────────────────

@Composable
private fun TabSwitcher(
    selected: LeaderboardTab,
    onSelect: (LeaderboardTab) -> Unit,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        TabButton(
            label    = "XP",
            icon     = Icons.Rounded.Bolt,
            isActive = selected == LeaderboardTab.Xp,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardTab.Xp) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        TabButton(
            label    = "Başarım",
            icon     = Icons.Rounded.EmojiEvents,
            isActive = selected == LeaderboardTab.Achievements,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardTab.Achievements) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    label   : String,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accent  : Color,
    theme   : AppThemeState,
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(
                    Brush.horizontalGradient(listOf(accent.copy(0.22f), accent.copy(0.10f)))
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint     = if (isActive) accent else theme.text2,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color      = if (isActive) theme.text0 else theme.text2,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Benim pozisyonum kartı ───────────────────────────────────────────────────

@Composable
private fun MyPositionCard(
    tab     : LeaderboardTab,
    summary : MyPositionSummary,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val scoreLabel = when (tab) {
        LeaderboardTab.Xp           -> "XP"
        LeaderboardTab.Achievements -> "Başarım"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(accent, theme)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(0.30f), accent.copy(0.08f))
                        )
                    )
                    .border(1.dp, accent.copy(0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${if (summary.position == 0L) "-" else summary.position}",
                    color      = accent,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SENİN POZİSYONUN",
                    color         = accent,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    if (summary.totalUsers > 0)
                        "${summary.position} / ${summary.totalUsers} kullanıcı arasında"
                    else "Henüz sıralama yok",
                    color    = theme.text2,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    summary.score.toString(),
                    color      = theme.text0,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    scoreLabel,
                    color    = theme.text2,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Satır ────────────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRowItem(
    row     : LeaderboardRow,
    tab     : LeaderboardTab,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val medalColor = when (row.position) {
        1L   -> Color(0xFFFFD700)
        2L   -> Color(0xFFB0BEC5)
        3L   -> Color(0xFFCD7F32)
        else -> theme.text2
    }
    val highlight = row.isMe

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) accent.copy(0.12f) else theme.bg1.copy(0.6f))
            .border(
                1.dp,
                if (highlight) accent.copy(0.5f) else theme.stroke.copy(0.5f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pozisyon / madalya
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (row.position in 1L..3L) {
                Icon(
                    Icons.Rounded.EmojiEvents, null,
                    tint     = medalColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    "#${row.position}",
                    color      = theme.text2,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(theme.bg2)
                .border(1.dp, theme.stroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (row.avatar.startsWith("http")) {
                AsyncImage(
                    model = row.avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(row.avatar, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        // İsim + "Sen" etiketi
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.displayName,
                    color      = theme.text0,
                    fontSize   = 14.sp,
                    fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                    maxLines   = 1
                )
                if (highlight) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SEN", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(
                when (tab) {
                    LeaderboardTab.Xp           -> "${row.score} XP"
                    LeaderboardTab.Achievements -> "${row.score} başarım"
                },
                color    = theme.text2,
                fontSize = 11.sp
            )
        }

        // Skor
        Text(
            row.score.toString(),
            color      = if (highlight) accent else theme.text0,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Black
        )
    }
}
