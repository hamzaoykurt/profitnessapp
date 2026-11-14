package com.cosmibit.profitness.presentation.leaderboard

import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaDetailHeader
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow

@Composable
fun LeaderboardScreen(
    onBack   : () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(),
    initialTab: LeaderboardTab = LeaderboardTab.Xp
) {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(initialTab) { viewModel.selectTab(initialTab) }

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SinemaNavHeight + 24.dp + navBarBottom)
        ) {
            item {
                SinemaDetailHeader(
                    title    = theme.t("Sıralama", "Leaderboard"),
                    subtitle = theme.t("Diğer kullanıcılarla kıyasla", "Compare with other users"),
                    onBack   = onBack,
                    trailing = {
                        Box(
                            Modifier
                                .size(48.dp)
                                .sinemaClickable(onClick = { viewModel.refresh() }, role = Role.Button, scale = 0.98f)
                                .sinemaCard(theme, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Refresh, theme.t("Yenile", "Refresh"), tint = Sinema.text(theme), modifier = Modifier.size(22.dp))
                        }
                    }
                )
            }

            // ── Dönem/sekme seçici ──────────────────────────────────────────
            item {
                SinemaSegmentRow(
                    options  = listOf(LeaderboardTab.Xp, LeaderboardTab.Achievements, LeaderboardTab.Streak),
                    selected = state.selectedTab,
                    label    = {
                        when (it) {
                            LeaderboardTab.Xp -> "XP"
                            LeaderboardTab.Achievements -> theme.t("Başarım", "Achievement")
                            LeaderboardTab.Streak -> theme.t("Seri", "Streak")
                        }
                    },
                    onSelect = viewModel::selectTab,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // ── Kapsam seçici ───────────────────────────────────────────────
            item {
                SinemaSegmentRow(
                    options  = listOf(LeaderboardScope.Global, LeaderboardScope.Friends),
                    selected = state.selectedScope,
                    label    = {
                        when (it) {
                            LeaderboardScope.Global -> "Global"
                            LeaderboardScope.Friends -> theme.t("Arkadaşlar", "Friends")
                        }
                    },
                    onSelect = viewModel::selectScope,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    height   = 36.dp
                )
            }

            // ── Benim pozisyonum ────────────────────────────────────────────
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
                    theme    = theme,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                SinemaSectionLabel(
                    when (state.selectedScope) {
                        LeaderboardScope.Global  -> "TOP 100"
                        LeaderboardScope.Friends -> theme.t("Arkadaşlar", "Friends")
                    },
                    trailing = when (state.selectedTab) {
                        LeaderboardTab.Xp           -> theme.t("XP sıralaması", "XP ranking")
                        LeaderboardTab.Achievements -> theme.t("Başarım sıralaması", "Achievement ranking")
                        LeaderboardTab.Streak       -> theme.t("Seri sıralaması", "Streak ranking")
                    }
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = accent) }
                }
            } else if (state.error != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            theme.t("Sıralama yüklenemedi", "Could not load leaderboard") + "\n${state.error}",
                            color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 20.sp
                        )
                        SinemaGoButton(
                            label = theme.t("Tekrar dene", "Try again"), icon = Icons.Rounded.Refresh,
                            onClick = { viewModel.refresh() }
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
                                color    = Sinema.textSecondary(theme),
                                fontSize = SinemaType.Body, lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val podium = rows.take(3)
                    item(key = "podium_${state.selectedScope}_${state.selectedTab}") {
                        Podium(rows = podium, tab = state.selectedTab, theme = theme, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                    itemsIndexed(items = rows.drop(3), key = { _, r -> "${state.selectedScope}_${state.selectedTab}_${r.userId}" }) { i, row ->
                        LeaderboardRowItem(
                            row    = row,
                            tab    = state.selectedTab,
                            theme  = theme,
                            modifier = Modifier.animateItem().sinemaListEnter(i).padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Avatar (halkalı) ─────────────────────────────────────────────────────────

@Composable
private fun RingAvatar(row: LeaderboardRow, size: Dp, ring: Color, theme: AppThemeState) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(Sinema.line(theme)),
        contentAlignment = Alignment.Center
    ) {
        if (row.avatar.startsWith("http")) {
            AsyncImage(model = row.avatar, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Text(row.avatar, fontSize = (size.value * 0.46f).sp)
        }
    }
}

private fun scoreUnit(tab: LeaderboardTab, theme: AppThemeState): String = when (tab) {
    LeaderboardTab.Xp           -> "XP"
    LeaderboardTab.Achievements -> theme.t("başarım", "achievements")
    LeaderboardTab.Streak       -> theme.t("gün", "days")
}

// ── Podyum: ilk 3 (2 - 1 - 3 düzeni, 1. vurgu + glow) ────────────────────────

@Composable
private fun Podium(rows: List<LeaderboardRow>, tab: LeaderboardTab, theme: AppThemeState, modifier: Modifier = Modifier) {
    val ordered = listOfNotNull(rows.getOrNull(1), rows.getOrNull(0), rows.getOrNull(2))
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { row ->
            PodiumCard(row = row, tab = tab, theme = theme, modifier = Modifier.weight(if (row.position == 1L) 1.15f else 1f))
        }
    }
}

@Composable
private fun PodiumCard(row: LeaderboardRow, tab: LeaderboardTab, theme: AppThemeState, modifier: Modifier = Modifier) {
    val accent = theme.effectiveAccentColor
    val first = row.position == 1L
    val shape = RoundedCornerShape(SinemaRadius.R28)
    val gold = theme.accent3Color   // c3: 1. podyum işareti
    val fg = Sinema.text(theme)
    val fg2 = if (first) theme.accent3TextColor else Sinema.textSecondary(theme)
    Column(
        modifier = modifier
            .sinemaCard(theme, shape)
            .then(
                if (row.isMe) Modifier.border(1.5.dp, accent, shape)   // kendi sıran: c1 kenar
                else if (first) Modifier.border(1.5.dp, gold, shape) else Modifier
            )
            .padding(horizontal = 8.dp)
            .padding(top = if (first) 22.dp else 16.dp, bottom = if (first) 22.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("#${row.position}", color = fg2, style = Sinema.Label)
        Spacer(Modifier.height(8.dp))
        RingAvatar(row, if (first) 64.dp else 52.dp, if (first) gold else if (row.isMe) accent else Sinema.line(theme), theme)
        Spacer(Modifier.height(10.dp))
        Text(
            row.displayName, color = fg, fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            row.score.toString(), color = fg,
            style = Sinema.Metric.copy(fontSize = if (first) 32.sp else 26.sp), maxLines = 1
        )
        Text(scoreUnit(tab, theme), color = fg2, fontSize = SinemaType.Caption, maxLines = 1)
        if (row.isMe) {
            Spacer(Modifier.height(6.dp))
            Text(theme.t("SEN", "YOU"), color = accent, fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        }
    }
}

// ── Benim pozisyonum kartı ───────────────────────────────────────────────────

@Composable
private fun MyPositionCard(
    tab     : LeaderboardTab,
    scope   : LeaderboardScope,
    summary : MyPositionSummary,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val accent = theme.effectiveAccentColor
    val scopeLabel = when (scope) {
        LeaderboardScope.Global  -> theme.t("kullanıcı arasında", "among users")
        LeaderboardScope.Friends -> theme.t("arkadaş arasında", "among friends")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R28))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            SinemaBigStat(
                value = "#${if (summary.position == 0L) "-" else summary.position}",
                label = theme.t("Senin pozisyonun", "Your position"),
                size = 52, color = theme.accent2TextColor, modifier = Modifier.weight(1f)
            )
            SinemaBigStat(
                value = summary.score.toString(), label = scoreUnit(tab, theme), size = 32
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (summary.totalUsers > 0 && summary.position > 0L)
                "${summary.position} / ${summary.totalUsers} $scopeLabel"
            else if (scope == LeaderboardScope.Friends)
                theme.t("Arkadaş listen boş veya henüz yerleşmedin", "Your friends list is empty or you are not ranked yet")
            else theme.t("Henüz sıralama yok", "No ranking yet"),
            color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm
        )
    }
}

// ── Satır ────────────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRowItem(
    row     : LeaderboardRow,
    tab     : LeaderboardTab,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val accent = theme.effectiveAccentColor
    val me = row.isMe
    val shape = RoundedCornerShape(SinemaRadius.R24)
    val fg = Sinema.text(theme)
    val fg2 = Sinema.textSecondary(theme)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .sinemaCard(theme, shape)
            .then(if (me) Modifier.border(1.5.dp, accent, shape) else Modifier)   // kendi sıran: c1 kenar
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${row.position}", color = fg2, fontSize = SinemaType.Body, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(44.dp), maxLines = 1
        )
        RingAvatar(row, 44.dp, if (me) accent else Sinema.line(theme), theme)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.displayName, color = fg, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (me) {
                    Spacer(Modifier.width(8.dp))
                    Text(theme.t("SEN", "YOU"), color = accent, fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
            Text(scoreUnit(tab, theme), color = fg2, fontSize = SinemaType.Caption)
        }
        Text(row.score.toString(), color = fg, style = Sinema.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold))
    }
}
