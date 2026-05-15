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
import com.avonix.profitness.presentation.components.AppBackButton
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
                    AppBackButton(onClick = onBack, accent = accent, size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.t("SIRALAMA", "LEADERBOARD"),
                            color         = theme.text0,
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            theme.t("Diğer kullanıcılarla kıyasla", "Compare with other users"),
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

            // ── Kapsam seçici (Global / Arkadaşlar) ─────────────────────────
            item {
                ScopeSwitcher(
                    selected = state.selectedScope,
                    onSelect = viewModel::selectScope,
                    accent   = accent,
                    theme    = theme,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // ── Benim pozisyonum özet kartı ─────────────────────────────────
            item {
                val summary = when (state.selectedScope) {
                    LeaderboardScope.Global -> when (state.selectedTab) {
                        LeaderboardTab.Xp           -> state.myXp
                        LeaderboardTab.Achievements -> state.myAchievements
                        LeaderboardTab.Streak       -> state.myStreak
                    }
                    LeaderboardScope.Friends -> when (state.selectedTab) {
                        LeaderboardTab.Xp           -> state.myFriendXp
                        LeaderboardTab.Achievements -> state.myFriendAchievements
                        LeaderboardTab.Streak       -> state.myFriendStreak
                    }
                }
                MyPositionCard(
                    tab      = state.selectedTab,
                    scope    = state.selectedScope,
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
                        when (state.selectedScope) {
                            LeaderboardScope.Global  -> "TOP 100"
                            LeaderboardScope.Friends -> theme.t("ARKADAŞLAR", "FRIENDS")
                        },
                        color         = theme.text0,
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        when (state.selectedTab) {
                            LeaderboardTab.Xp           -> theme.t("XP SIRALAMASI", "XP RANKING")
                            LeaderboardTab.Achievements -> theme.t("BAŞARIM SIRALAMASI", "ACHIEVEMENT RANKING")
                            LeaderboardTab.Streak       -> theme.t("SERİ SIRALAMASI", "STREAK RANKING")
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
                            theme.t("Sıralama yüklenemedi", "Could not load leaderboard") + "\n${state.error}",
                            color      = theme.text2,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val rows = when (state.selectedScope) {
                    LeaderboardScope.Global -> when (state.selectedTab) {
                        LeaderboardTab.Xp           -> state.xpRows
                        LeaderboardTab.Achievements -> state.achievementRows
                        LeaderboardTab.Streak       -> state.streakRows
                    }
                    LeaderboardScope.Friends -> when (state.selectedTab) {
                        LeaderboardTab.Xp           -> state.friendXpRows
                        LeaderboardTab.Achievements -> state.friendAchievementRows
                        LeaderboardTab.Streak       -> state.friendStreakRows
                    }
                }
                if (rows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (state.selectedScope) {
                                    LeaderboardScope.Global  -> theme.t("Henüz sıralamada kimse yok", "No one is ranked yet")
                                    LeaderboardScope.Friends -> theme.t(
                                        "Henüz arkadaşın yok.\nArkadaş eklemek için birini takip et — karşılıklı takip arkadaşlık sayılır.",
                                        "You do not have friends yet.\nFollow someone to add a friend. Mutual follows count as friends."
                                    )
                                },
                                color    = theme.text2,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(items = rows, key = { "${state.selectedScope}_${state.selectedTab}_${it.userId}" }) { row ->
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
            label    = theme.t("Başarım", "Achievement"),
            icon     = Icons.Rounded.EmojiEvents,
            isActive = selected == LeaderboardTab.Achievements,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardTab.Achievements) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        TabButton(
            label    = "Seri",
            icon     = Icons.Rounded.LocalFireDepartment,
            isActive = selected == LeaderboardTab.Streak,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardTab.Streak) },
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Scope Switcher (Global / Arkadaşlar) ─────────────────────────────────────

@Composable
private fun ScopeSwitcher(
    selected: LeaderboardScope,
    onSelect: (LeaderboardScope) -> Unit,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        TabButton(
            label    = "Global",
            icon     = Icons.Rounded.Public,
            isActive = selected == LeaderboardScope.Global,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardScope.Global) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(3.dp))
        TabButton(
            label    = theme.t("Arkadaşlar", "Friends"),
            icon     = Icons.Rounded.People,
            isActive = selected == LeaderboardScope.Friends,
            accent   = accent,
            theme    = theme,
            onClick  = { onSelect(LeaderboardScope.Friends) },
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
    scope   : LeaderboardScope,
    summary : MyPositionSummary,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val scoreLabel = when (tab) {
        LeaderboardTab.Xp           -> "XP"
        LeaderboardTab.Achievements -> theme.t("Başarım", "Achievement")
        LeaderboardTab.Streak       -> theme.t("Gün", "Days")
    }
    val scopeLabel = when (scope) {
        LeaderboardScope.Global  -> theme.t("kullanıcı arasında", "among users")
        LeaderboardScope.Friends -> theme.t("arkadaş arasında", "among friends")
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
                    theme.t("SENİN POZİSYONUN", "YOUR POSITION"),
                    color         = accent,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    if (summary.totalUsers > 0 && summary.position > 0L)
                        "${summary.position} / ${summary.totalUsers} $scopeLabel"
                    else if (scope == LeaderboardScope.Friends)
                        theme.t("Arkadaş listen boş veya henüz yerleşmedin", "Your friends list is empty or you are not ranked yet")
                    else theme.t("Henüz sıralama yok", "No ranking yet"),
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
                        Text(theme.t("SEN", "YOU"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(
                when (tab) {
                    LeaderboardTab.Xp           -> "${row.score} XP"
                    LeaderboardTab.Achievements -> "${row.score} ${theme.t("başarım", "achievements")}"
                    LeaderboardTab.Streak       -> "${row.score} ${theme.t("gün", "days")}"
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
