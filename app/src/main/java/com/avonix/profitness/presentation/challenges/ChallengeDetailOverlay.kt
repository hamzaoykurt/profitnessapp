package com.avonix.profitness.presentation.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeLeaderboardEntry
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeVisibility

@Composable
fun ChallengeDetailOverlay(
    challengeId: String,
    onBack     : () -> Unit,
    onChanged  : () -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val vm: ChallengeDetailViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(challengeId) { vm.load(challengeId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(theme.bg1)
                            .border(1.dp, theme.stroke, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew, null,
                            tint = theme.text0,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "CHALLENGE",
                        color      = theme.text0,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                state.error != null && state.detail == null -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF5252).copy(0.14f))
                            .border(1.dp, Color(0xFFFF5252).copy(0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(state.error ?: "", color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
                state.detail != null -> {
                    val detail = state.detail!!
                    val c = detail.summary

                    // ── Header card ─────────────────────────────────
                    item {
                        ChallengeHero(c, accent)
                    }

                    // ── Stat row ────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatChip("Hedef", "${c.targetValue}",  Icons.Rounded.Flag, accent, Modifier.weight(1f))
                            StatChip("İlerleme", "${c.myProgress}", Icons.Rounded.EmojiEvents, accent, Modifier.weight(1f))
                            StatChip("Katılımcı", "${c.participantsCount}", Icons.Rounded.People, accent, Modifier.weight(1f))
                        }
                    }

                    // ── Description ─────────────────────────────────
                    if (c.description.isNotBlank()) {
                        item {
                            Text(
                                c.description,
                                color    = theme.text1,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // ── Date & visibility footer ─────────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (c.visibility == ChallengeVisibility.Private) {
                                Icon(Icons.Rounded.Lock, null, tint = theme.text2, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Özel", color = theme.text2, fontSize = 11.sp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                "${c.startDateIso} → ${c.endDateIso}",
                                color    = theme.text2,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // ── Join/Leave button ───────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (c.isJoined) theme.bg2 else accent)
                                .border(
                                    1.dp,
                                    if (c.isJoined) theme.stroke else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !state.inFlight) {
                                    if (c.isJoined) vm.leave() else vm.join(null)
                                    onChanged()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.inFlight) {
                                CircularProgressIndicator(
                                    color = if (c.isJoined) accent else Color.Black,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    if (c.isJoined) "AYRIL" else "KATIL",
                                    color = if (c.isJoined) theme.text0 else Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    // ── Leaderboard header ──────────────────────────
                    item {
                        Text(
                            "SIRALAMA",
                            color      = theme.text0,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier   = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                        )
                    }

                    if (detail.leaderboard.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Henüz katılımcı yok",
                                    color    = theme.text2,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        items(detail.leaderboard, key = { "lb_${it.userId}" }) { entry ->
                            LeaderboardEntryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryRow(entry: ChallengeLeaderboardEntry) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val highlight = entry.isMe

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) accent.copy(0.12f) else theme.bg1.copy(0.5f))
            .border(
                1.dp,
                if (highlight) accent.copy(0.4f) else theme.stroke.copy(0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(theme.bg2)
                .border(1.dp, theme.stroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val av = entry.avatarUrl
            if (av != null && av.startsWith("http")) {
                AsyncImage(
                    model = av,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(av ?: "🏋️", fontSize = 16.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.displayName,
                    color      = theme.text0,
                    fontSize   = 13.sp,
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
            if (entry.isCompleted) {
                Text("Tamamladı ✓", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            entry.progress.toString(),
            color      = if (highlight) accent else theme.text0,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChallengeHero(c: ChallengeSummary, accent: Color) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(0.22f), accent.copy(0.06f))
                )
            )
            .border(1.dp, accent.copy(0.4f), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                c.targetType.label.uppercase(),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                c.title,
                color      = theme.text0,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))
            // big progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(theme.stroke.copy(0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(c.progressPct)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(0.6f))))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${c.myProgress} / ${c.targetValue} ${c.targetType.unit}",
                color    = theme.text1,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(label, color = theme.text2, fontSize = 10.sp)
    }
}

