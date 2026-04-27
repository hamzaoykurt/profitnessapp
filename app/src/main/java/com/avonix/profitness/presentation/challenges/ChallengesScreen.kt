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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.avonix.profitness.domain.challenges.ChallengeKind
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.EventMode

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

    Box(Modifier.fillMaxSize()) {

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
                                        onToggle   = {
                                            // Private + not joined → detail'e git (password dialog orada)
                                            if (!c.isJoined && c.visibility == com.avonix.profitness.domain.challenges.ChallengeVisibility.Private) {
                                                vm.openDetail(c.id)
                                            } else {
                                                vm.toggleJoin(c)
                                            }
                                        },
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

        // ── Detail overlay (Dialog-backed: tam ekran + glow) ───────────
        state.openDetailId?.let { id ->
            ChallengeDetailOverlay(
                challengeId = id,
                onBack      = { vm.closeDetail() },
                onChanged   = { vm.refresh() }
            )
        }

        // ── Create overlay (Dialog-backed: tam ekran + glow) ───────────
        if (state.showCreateSheet) {
            CreateChallengeOverlay(
                inFlight = state.createInFlight,
                error    = state.createError,
                exercises = state.exercises,
                onDismiss = { vm.closeCreate() },
                onSubmit  = { title, desc, tt, tv, sd, ed, vis, pw ->
                    vm.submitCreate(title, desc, tt, tv, sd, ed, vis, pw)
                },
                onSubmitEvent = { req -> vm.submitCreateEvent(req) }
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

// ── Card status ──────────────────────────────────────────────────────────────
private enum class CardStatus { NotStarted, Live, Ended }

private fun statusOf(start: String, end: String): CardStatus {
    val today = java.time.LocalDate.now()
    val s = runCatching { java.time.LocalDate.parse(start) }.getOrNull() ?: return CardStatus.Live
    val e = runCatching { java.time.LocalDate.parse(end) }.getOrNull() ?: return CardStatus.Live
    return when {
        today.isBefore(s) -> CardStatus.NotStarted
        today.isAfter(e)  -> CardStatus.Ended
        else              -> CardStatus.Live
    }
}

private val TR_MONTHS = listOf("Oca","Şub","Mar","Nis","May","Haz","Tem","Ağu","Eyl","Eki","Kas","Ara")
private fun humanDate(iso: String): String {
    val d = runCatching { java.time.LocalDate.parse(iso) }.getOrNull() ?: return iso
    return "${d.dayOfMonth} ${TR_MONTHS[d.monthValue - 1]}"
}
private fun daysBetween(fromIso: String, toIso: String): Long? {
    val f = runCatching { java.time.LocalDate.parse(fromIso) }.getOrNull() ?: return null
    val t = runCatching { java.time.LocalDate.parse(toIso) }.getOrNull() ?: return null
    return java.time.temporal.ChronoUnit.DAYS.between(f, t)
}

private fun targetIcon(type: ChallengeTargetType): ImageVector = when (type) {
    ChallengeTargetType.TotalWorkouts        -> Icons.Rounded.FitnessCenter
    ChallengeTargetType.TotalXp              -> Icons.Rounded.Bolt
    ChallengeTargetType.CurrentStreak        -> Icons.Rounded.LocalFireDepartment
    ChallengeTargetType.TotalDurationMinutes -> Icons.Rounded.Timer
    ChallengeTargetType.TotalDistanceM       -> Icons.Rounded.Straighten
    ChallengeTargetType.TotalDistanceKm      -> Icons.Rounded.Speed
    ChallengeTargetType.MovementsCompleted   -> Icons.Rounded.PlaylistAddCheck
}

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
    val isEvent = c.kind == ChallengeKind.Event
    val status = statusOf(c.startDateIso, c.endDateIso)
    val today = java.time.LocalDate.now().toString()
    val daysLeft = daysBetween(today, c.endDateIso) ?: 0L
    val daysUntilStart = daysBetween(today, c.startDateIso) ?: 0L

    val borderColor = when {
        c.isCompleted -> accent.copy(0.7f)
        c.isJoined    -> accent.copy(0.55f)
        status == CardStatus.Ended -> theme.stroke.copy(0.35f)
        else -> theme.stroke.copy(0.5f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        theme.bg2.copy(0.92f),
                        theme.bg1.copy(0.70f),
                        theme.bg1.copy(0.56f)
                    )
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onTap)
    ) {
        // Subtle accent glow overlay (joined/live için belirgin)
        if (c.isJoined || status == CardStatus.Live) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(0.16f), accent.copy(0.04f), Color.Transparent),
                            radius = 520f
                        )
                    )
            )
        }

        Row(Modifier.fillMaxWidth()) {
            // Sol kenar accent strip (joined ise)
            if (c.isJoined) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(listOf(accent, accent.copy(0.4f)))
                        )
                )
            }

            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                // ── Header chips + status ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KindBadge(isEvent = isEvent, accent = accent, theme = theme)
                    if (isEvent && c.event != null) {
                        Spacer(Modifier.width(6.dp))
                        EventModeBadge(mode = c.event.mode, theme = theme)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        status = if (c.isCompleted) CardStatus.Ended else status,
                        completed = c.isCompleted,
                        accent = accent,
                        theme = theme
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Title row (lock + title) ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPrivate) {
                        Icon(Icons.Rounded.Lock, null, tint = theme.text2, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        c.title,
                        color      = theme.text0,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight  = 22.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Hero stat tile ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(0.20f), theme.bg2.copy(0.58f))
                            )
                        )
                        .border(1.dp, accent.copy(0.34f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.20f))
                            .border(1.dp, accent.copy(0.42f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            targetIcon(c.targetType),
                            null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${c.targetValue}",
                            color = theme.text0,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            c.targetType.unit,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "HEDEF",
                            color = theme.text2,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            c.targetType.label,
                            color = theme.text1,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Event-spesifik tarih+saat+konum bandı ──
                if (isEvent && c.event != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.bg2.copy(0.5f))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = accent, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            humanDate(c.event.dateIso) + (c.event.timeIso?.let { " · ${it.take(5)}" } ?: ""),
                            color = theme.text0,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (!c.event.location.isNullOrBlank()) {
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.LocationOn, null, tint = theme.text2, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                c.event.location,
                                color = theme.text1,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // ── Description ──
                if (c.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        c.description,
                        color     = theme.text1.copy(0.88f),
                        fontSize  = 13.sp,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                // ── Progress (joined ise) ──
                if (c.isJoined) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${c.myProgress} / ${c.targetValue} ${c.targetType.unit}",
                            color    = theme.text1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "%${(c.progressPct * 100).toInt()}",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    ProgressBar(pct = c.progressPct, accent = accent, theme.stroke)
                }

                Spacer(Modifier.height(12.dp))

                // ── Footer: meta info + CTA ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        // 1. satır: katılımcı + countdown
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.People, null, tint = theme.text2, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${c.participantsCount}",
                                color = theme.text1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(10.dp))
                            // Countdown / status
                            val (cdIcon, cdText, cdColor) = when {
                                c.isCompleted -> Triple(Icons.Rounded.Check, "TAMAMLANDI", accent)
                                status == CardStatus.NotStarted ->
                                    Triple(Icons.Rounded.HourglassBottom,
                                        if (daysUntilStart == 0L) "Bugün başlıyor"
                                        else "$daysUntilStart gün sonra", theme.text1)
                                status == CardStatus.Ended ->
                                    Triple(Icons.Rounded.Schedule, "Sona erdi", theme.text2)
                                daysLeft == 0L ->
                                    Triple(Icons.Rounded.Schedule, "Son gün", Color(0xFFFF8A80))
                                daysLeft <= 3L ->
                                    Triple(Icons.Rounded.Schedule, "$daysLeft gün kaldı", Color(0xFFFFB74D))
                                else ->
                                    Triple(Icons.Rounded.Schedule, "$daysLeft gün kaldı", theme.text1)
                            }
                            Icon(cdIcon, null, tint = cdColor, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(cdText, color = cdColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        // 2. satır: tarih aralığı (sadece metric için, event'te zaten üstte tek tarih var)
                        if (!isEvent) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${humanDate(c.startDateIso)} → ${humanDate(c.endDateIso)}",
                                color = theme.text2,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // CTA
                    JoinPill(
                        isJoined = c.isJoined,
                        inFlight = inFlight,
                        canAct   = !c.isCompleted && status != CardStatus.Ended,
                        accent   = accent,
                        theme    = theme,
                        onClick  = onToggle
                    )
                }
            }
        }
    }
}

// ── Status pill (NOT STARTED / LIVE / ENDED / DONE) ─────────────────────────
@Composable
private fun StatusPill(
    status: CardStatus,
    completed: Boolean,
    accent: Color,
    theme: com.avonix.profitness.core.theme.AppThemeState
) {
    val (label, fg, bg) = when {
        completed -> Triple("TAMAM", accent, accent.copy(0.22f))
        status == CardStatus.NotStarted -> Triple("YAKINDA", Color(0xFFFFB74D), Color(0xFFFFB74D).copy(0.18f))
        status == CardStatus.Live       -> Triple("DEVAM EDİYOR", accent, accent.copy(0.18f))
        else                            -> Triple("SONA ERDİ", theme.text2, theme.bg2.copy(0.6f))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, fg.copy(0.42f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

// ── Join / Leave CTA pill — gradient + icon ─────────────────────────────────
@Composable
private fun JoinPill(
    isJoined: Boolean,
    inFlight: Boolean,
    canAct: Boolean,
    accent: Color,
    theme: com.avonix.profitness.core.theme.AppThemeState,
    onClick: () -> Unit
) {
    val bg: Brush = when {
        !canAct  -> Brush.horizontalGradient(listOf(theme.bg2, theme.bg2))
        isJoined -> Brush.horizontalGradient(listOf(theme.bg2, theme.bg2.copy(0.7f)))
        else     -> Brush.horizontalGradient(listOf(accent, accent.copy(0.7f)))
    }
    val border = if (isJoined && canAct) accent.copy(0.5f) else Color.Transparent
    Row(
        modifier = Modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(15.dp))
            .clickable(enabled = !inFlight && canAct, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inFlight) {
            CircularProgressIndicator(
                color = if (isJoined) accent else Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
            )
        } else {
            val icon = when {
                !canAct  -> Icons.Rounded.Lock
                isJoined -> Icons.Rounded.Check
                else     -> Icons.Rounded.Bolt
            }
            val label = when {
                !canAct  -> "KAPALI"
                isJoined -> "KATILDIN"
                else     -> "KATIL"
            }
            val fg = when {
                !canAct  -> theme.text2
                isJoined -> accent
                else     -> Color.Black
            }
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (!isJoined && canAct) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = fg, modifier = Modifier.size(14.dp))
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
private fun KindBadge(
    isEvent: Boolean,
    accent: Color,
    theme: com.avonix.profitness.core.theme.AppThemeState
) {
    val (icon, label, tint) = if (isEvent)
        Triple(Icons.Rounded.Event, "ETKİNLİK", accent)
    else
        Triple(Icons.Rounded.EmojiEvents, "METRİK", theme.text1)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isEvent) accent.copy(0.16f) else theme.bg2.copy(0.72f))
            .border(
                1.dp,
                if (isEvent) accent.copy(0.45f) else theme.stroke.copy(0.5f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun EventModeBadge(
    mode: EventMode,
    theme: com.avonix.profitness.core.theme.AppThemeState
) {
    val (icon, label) = when (mode) {
        EventMode.Physical     -> Icons.Rounded.LocationOn to "FİZİKSEL"
        EventMode.Online       -> Icons.Rounded.Link to "ONLINE"
        EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to "HAREKET"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.bg2.copy(0.42f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = theme.text2, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            label,
            color = theme.text2,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
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
