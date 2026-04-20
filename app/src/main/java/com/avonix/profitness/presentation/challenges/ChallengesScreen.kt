package com.avonix.profitness.presentation.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility

/**
 * Challenges tab — DiscoverScreen'e gömülü.
 * - Üstte KEŞFET / KATILDIKLARIM scope switcher
 * - Her kart: başlık, hedef, ilerleme barı, katılımcı sayısı, join/leave butonu
 * - Sağ alt "+" FAB → CreateChallengeOverlay
 * - Kart tıklanınca → ChallengeDetailOverlay (yatay slide)
 */
@Composable
fun ChallengesTab(
    bottomPadding: Dp,
    timerExtraPad: Dp = 0.dp
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val vm: ChallengesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(theme.bg0)) {

        Column(Modifier.fillMaxSize()) {

            // ── Scope switcher ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg1.copy(0.6f))
                    .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                ScopeChip(
                    label    = "KEŞFET",
                    icon     = Icons.Rounded.Public,
                    isActive = state.scope == ChallengesScope.Browse,
                    accent   = accent,
                    onClick  = { vm.selectScope(ChallengesScope.Browse) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(3.dp))
                ScopeChip(
                    label    = "KATILDIKLARIM (${state.myList.size})",
                    icon     = Icons.Rounded.EmojiEvents,
                    isActive = state.scope == ChallengesScope.Mine,
                    accent   = accent,
                    onClick  = { vm.selectScope(ChallengesScope.Mine) },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Hata banner ───────────────────────────────────────────────
            state.error?.let { err ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5252).copy(0.14f))
                        .border(1.dp, Color(0xFFFF5252).copy(0.5f), RoundedCornerShape(10.dp))
                        .clickable { vm.clearError() }
                        .padding(12.dp)
                ) {
                    Text(err, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }

            // ── Liste ─────────────────────────────────────────────────────
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    else -> {
                        val list = if (state.scope == ChallengesScope.Browse) state.browseList else state.myList
                        if (list.isEmpty()) {
                            EmptyBlock(
                                text = if (state.scope == ChallengesScope.Browse)
                                    "Aktif public challenge bulunmuyor.\nİlk sen yarat → sağ alt +"
                                else "Henüz challenge'a katılmadın.\nKEŞFET sekmesinden birine katıl.",
                                theme = theme
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start  = 20.dp,
                                    end    = 20.dp,
                                    top    = 4.dp,
                                    bottom = bottomPadding + 80.dp
                                )
                            ) {
                                items(list, key = { "${state.scope}_${it.id}" }) { c ->
                                    ChallengeCard(
                                        c          = c,
                                        accent     = accent,
                                        inFlight   = state.joinInFlight.contains(c.id),
                                        onTap      = { vm.openDetail(c.id) },
                                        onToggle   = { vm.toggleJoin(c) },
                                        modifier   = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Refresh button (top-right floating) ──────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 14.dp, top = 0.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(theme.bg1.copy(0.6f))
                        .border(1.dp, theme.stroke.copy(0.5f), CircleShape)
                        .clickable { vm.refresh() },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, null, tint = theme.text2, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── Create FAB ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding + 12.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent, accent.copy(0.7f))))
                .clickable { vm.openCreate() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, "Yeni challenge", tint = Color.Black, modifier = Modifier.size(26.dp))
        }

        // ── Detail overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.openDetailId != null,
            enter   = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit    = slideOutHorizontally(targetOffsetX  = { it }) + fadeOut()
        ) {
            state.openDetailId?.let { id ->
                ChallengeDetailOverlay(
                    challengeId = id,
                    onBack      = { vm.closeDetail() },
                    onChanged   = { vm.refresh() }
                )
            }
        }

        // ── Create overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showCreateSheet,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY  = { it }) + fadeOut()
        ) {
            CreateChallengeOverlay(
                inFlight = state.createInFlight,
                error    = state.createError,
                onDismiss = { vm.closeCreate() },
                onSubmit  = { title, desc, tt, tv, sd, ed, vis, pw ->
                    vm.submitCreate(title, desc, tt, tv, sd, ed, vis, pw)
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Scope chip
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ScopeChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
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
        Icon(icon, null, tint = if (isActive) accent else theme.text2, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color      = if (isActive) theme.text0 else theme.text2,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Challenge card
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChallengeCard(
    c: ChallengeSummary,
    accent: Color,
    inFlight: Boolean,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val isPrivate = c.visibility == ChallengeVisibility.Private

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1.copy(0.6f))
            .border(
                1.dp,
                if (c.isCompleted) accent.copy(0.55f) else theme.stroke.copy(0.5f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onTap)
            .padding(14.dp)
    ) {
        // Başlık satırı
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPrivate) {
                        Icon(Icons.Rounded.Lock, null, tint = theme.text2, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        c.title,
                        color      = theme.text0,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines   = 1
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${c.targetType.label} · ${c.targetValue} ${c.targetType.unit}",
                    color      = accent,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (c.isCompleted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(0.25f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("TAMAM", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (c.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                c.description,
                color     = theme.text1,
                fontSize  = 12.sp,
                maxLines  = 2
            )
        }

        // Progress (join etmişse)
        if (c.isJoined) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(pct = c.progressPct, accent = accent, theme.stroke)
            Spacer(Modifier.height(6.dp))
            Text(
                "${c.myProgress} / ${c.targetValue} ${c.targetType.unit}",
                color    = theme.text2,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        // Alt bilgi satırı
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${c.participantsCount} katılımcı · ${c.startDateIso} → ${c.endDateIso}",
                color    = theme.text2,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (c.isJoined) theme.bg2 else accent)
                    .border(
                        1.dp,
                        if (c.isJoined) theme.stroke else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = !inFlight, onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        color = if (c.isJoined) accent else Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        if (c.isJoined) "AYRIL" else "KATIL",
                        color = if (c.isJoined) theme.text0 else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(pct: Float, accent: Color, strokeColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(strokeColor.copy(0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(0.6f))))
        )
    }
}

@Composable
private fun EmptyBlock(text: String, theme: com.avonix.profitness.core.theme.AppThemeState) {
    Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = theme.text2,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
