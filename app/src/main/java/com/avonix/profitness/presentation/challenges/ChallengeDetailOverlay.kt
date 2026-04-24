package com.avonix.profitness.presentation.challenges

import android.content.Intent
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.PageAccentBloom
import com.avonix.profitness.core.theme.strings
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeEventInfo
import com.avonix.profitness.domain.challenges.ChallengeKind
import com.avonix.profitness.domain.challenges.ChallengeLeaderboardEntry
import com.avonix.profitness.domain.challenges.ChallengeMovement
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.EventMode
import java.time.LocalDate

@Composable
fun ChallengeDetailOverlay(
    challengeId: String,
    onBack     : () -> Unit,
    onChanged  : () -> Unit
) {
    val theme  = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary
    val vm: ChallengeDetailViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // Private join password dialog
    var showJoinPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) { vm.load(challengeId) }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
    Box(Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
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
                        if (state.detail?.summary?.kind == ChallengeKind.Event) strings.eventLabel else strings.challengeLabel,
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
                    val isEvent = c.kind == ChallengeKind.Event
                    val ev = c.event

                    // ── Header hero ─────────────────────────────────
                    item { ChallengeHero(c, accent, isEvent) }

                    // ── Event info card (if event) ──────────────────
                    if (isEvent && ev != null) {
                        item { EventInfoCard(ev, accent) }
                    }

                    // ── Stat row ────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatChip("Hedef", "${c.targetValue}", Icons.Rounded.Flag, accent, Modifier.weight(1f))
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
                                    when {
                                        c.isJoined -> { vm.leave(); onChanged() }
                                        c.visibility == ChallengeVisibility.Private -> {
                                            showJoinPasswordDialog = true
                                        }
                                        else -> { vm.join(null); onChanged() }
                                    }
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
                                    if (c.isJoined) strings.leaveLabel else strings.joinLabel,
                                    color = if (c.isJoined) theme.text0 else Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    // ── Skip-program toggle (today + event + joined) ─
                    if (isEvent && c.isJoined && ev?.dateIso == LocalDate.now().toString()) {
                        item {
                            SkipProgramToggle(
                                enabled = state.skipProgramToday,
                                accent  = accent,
                                onToggle = { vm.setSkipProgramToday(!state.skipProgramToday) }
                            )
                        }
                    }

                    // ── Movement checklist (if movement_list event + joined) ─
                    if (isEvent && ev?.mode == EventMode.MovementList && c.isJoined
                        && detail.movements.isNotEmpty()
                    ) {
                        item {
                            Text(
                                "HAREKETLER",
                                color = theme.text0,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                            )
                        }
                        items(detail.movements, key = { "mv_${it.id}" }) { mv ->
                            MovementRow(
                                movement = mv,
                                accent = accent,
                                pending = mv.id in state.pendingMovementIds,
                                onToggle = {
                                    vm.toggleMovement(mv.id)
                                    onChanged()
                                }
                            )
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

        // ── Private join password dialog ─────────────────────────────────────
        if (showJoinPasswordDialog) {
            PrivateJoinPasswordDialog(
                accent = accent,
                onCancel = { showJoinPasswordDialog = false },
                onSubmit = { pwd ->
                    showJoinPasswordDialog = false
                    vm.join(pwd)
                    onChanged()
                }
            )
        }
    }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PRIVATE JOIN PASSWORD DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PrivateJoinPasswordDialog(
    accent  : Color,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.72f))
            .clickable(enabled = true, onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    strings.privateJoinTitle,
                    color = theme.text0,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                strings.privateJoinHint,
                color = theme.text1,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.text0, fontSize = 15.sp),
                    cursorBrush = SolidColor(accent),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    decorationBox = { inner ->
                        if (password.isEmpty()) {
                            Text("••••••", color = theme.text2, fontSize = 15.sp)
                        }
                        inner()
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.bg2)
                        .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.privateJoinCancel,
                        color = theme.text1,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (password.isBlank()) accent.copy(0.35f) else accent)
                        .clickable(enabled = password.isNotBlank()) { onSubmit(password) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.joinLabel,
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EVENT INFO CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EventInfoCard(ev: ChallengeEventInfo, accent: Color) {
    val theme = LocalAppTheme.current
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1.copy(0.7f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, label) = when (ev.mode) {
                EventMode.Physical     -> Icons.Rounded.LocationOn to "FİZİKSEL"
                EventMode.Online       -> Icons.Rounded.Link to "ONLINE"
                EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to "HAREKET LİSTESİ"
            }
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        // Date + time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = theme.text2, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                ev.dateIso + (ev.timeIso?.let { " · ${it.take(5)}" } ?: ""),
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (!ev.timezone.isNullOrBlank()) {
            Text(
                ev.timezone,
                color = theme.text2,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 20.dp, top = 1.dp)
            )
        }

        // Mode-specific body
        when (ev.mode) {
            EventMode.Physical -> {
                if (!ev.location.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = theme.text2, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            ev.location,
                            color = theme.text0,
                            fontSize = 13.sp
                        )
                    }
                    if (ev.geoLat != null && ev.geoLng != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(0.16f))
                                .clickable {
                                    val uri = "geo:${ev.geoLat},${ev.geoLng}?q=${ev.geoLat},${ev.geoLng}(${ev.location ?: "Etkinlik"})".toUri()
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "HARİTADA AÇ",
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
            EventMode.Online -> {
                if (!ev.onlineUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, ev.onlineUrl.toUri()))
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            ev.onlineUrl,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1
                        )
                    }
                }
            }
            EventMode.MovementList -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${ev.myCompletedCount} / ${ev.movementsCount} hareket tamamlandı",
                    color = theme.text1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SKIP PROGRAM TOGGLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SkipProgramToggle(
    enabled: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent.copy(0.12f) else theme.bg1.copy(0.6f))
            .border(
                1.dp,
                if (enabled) accent.copy(0.4f) else theme.stroke.copy(0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (enabled) accent else Color.Transparent)
                .border(
                    1.5.dp,
                    if (enabled) accent else theme.stroke,
                    RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (enabled) {
                Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "BUGÜN PROGRAMI ATLA",
                color = theme.text0,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "Bu etkinliği yapacağın için anasayfadaki günlük program gizlenir",
                color = theme.text2,
                fontSize = 10.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MOVEMENT ROW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MovementRow(
    movement: ChallengeMovement,
    accent: Color,
    pending: Boolean,
    onToggle: () -> Unit
) {
    val theme = LocalAppTheme.current
    val done = movement.myCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (done) accent.copy(0.10f) else theme.bg1.copy(0.6f))
            .border(
                1.dp,
                if (done) accent.copy(0.35f) else theme.stroke.copy(0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !pending, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (done) accent else Color.Transparent)
                .border(
                    1.5.dp,
                    if (done) accent else theme.stroke,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                pending -> CircularProgressIndicator(
                    color = accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp)
                )
                done -> Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                movement.exerciseName,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
            val sub = buildString {
                movement.suggestedSets?.let { append("${it} set") }
                movement.suggestedReps?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it} tekrar")
                }
                movement.suggestedDurSec?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it}s")
                }
            }
            if (sub.isNotEmpty()) {
                Text(sub, color = theme.text2, fontSize = 11.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Leaderboard row / hero / stat chip (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

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
private fun ChallengeHero(c: ChallengeSummary, accent: Color, isEvent: Boolean) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isEvent) "ETKİNLİK" else c.targetType.label.uppercase(),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                if (isEvent) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "· ${c.targetType.label.uppercase()}",
                        color = theme.text2,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                c.title,
                color      = theme.text0,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))
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
