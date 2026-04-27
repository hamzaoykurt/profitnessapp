package com.avonix.profitness.presentation.challenges

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import com.avonix.profitness.domain.challenges.UpdateEventChallengeRequest
import java.time.LocalDate
import kotlinx.coroutines.delay

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
    var showOwnerMenu          by remember { mutableStateOf(false) }
    var showDeleteConfirm      by remember { mutableStateOf(false) }
    var showProgressDialog     by remember { mutableStateOf(false) }
    var showEditOverlay        by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) { vm.load(challengeId) }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            onChanged()
            onBack()
        }
    }

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
                        letterSpacing = 2.sp,
                        modifier   = Modifier.weight(1f)
                    )
                    if (state.isOwner) {
                        val isEventOwner = state.detail?.summary?.kind == ChallengeKind.Event
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(theme.bg1)
                                    .border(1.dp, theme.stroke, CircleShape)
                                    .clickable { showOwnerMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.MoreVert, null,
                                    tint = theme.text0,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            OwnerActionMenu(
                                expanded = showOwnerMenu,
                                accent = accent,
                                canEdit = isEventOwner, // metric için edit yok, sadece sil
                                onDismiss = { showOwnerMenu = false },
                                onEdit = { showOwnerMenu = false; showEditOverlay = true },
                                onDelete = { showOwnerMenu = false; showDeleteConfirm = true }
                            )
                        }
                    }
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
                            val unit = c.targetType.unit
                            StatChip("Hedef", "${c.targetValue} $unit", Icons.Rounded.Flag, accent, Modifier.weight(1f))
                            StatChip("İlerleme", "${c.myProgress} $unit", Icons.Rounded.EmojiEvents, accent, Modifier.weight(1f))
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

                    // ── "İlerleme Ekle" — sadece metric target'lı event'lerde ──
                    if (isEvent && c.isJoined && ev != null
                        && ev.mode != EventMode.MovementList
                        && c.targetValue > 0
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(0.16f))
                                    .border(1.dp, accent.copy(0.4f), RoundedCornerShape(14.dp))
                                    .clickable(enabled = !state.submittingProgress) {
                                        showProgressDialog = true
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.submittingProgress) {
                                    CircularProgressIndicator(
                                        color = accent, strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "İLERLEME EKLE",
                                            color = accent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
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
                        itemsIndexed(
                            detail.leaderboard.sortedByDescending { it.progress },
                            key = { _, e -> "lb_${e.userId}" }
                        ) { index, entry ->
                            LeaderboardEntryRow(rank = index + 1, entry = entry, unit = c.targetType.unit)
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

        // ── Add manual progress dialog ───────────────────────────────────────
        if (showProgressDialog) {
            val c = state.detail?.summary
            if (c != null) {
                AddProgressDialog(
                    accent = accent,
                    unit = c.targetType.unit,
                    targetLabel = c.targetType.label,
                    onCancel = { showProgressDialog = false },
                    onSubmit = { amount ->
                        showProgressDialog = false
                        vm.addManualProgress(amount)
                        onChanged()
                    }
                )
            }
        }

        // ── Delete confirm dialog ────────────────────────────────────────────
        if (showDeleteConfirm) {
            DeleteConfirmDialog(
                accent = accent,
                inFlight = state.ownerActionInFlight,
                onCancel = { showDeleteConfirm = false },
                onConfirm = {
                    vm.deleteChallenge()
                    onChanged()
                }
            )
        }

        // ── Edit overlay (owner) ─────────────────────────────────────────────
        if (showEditOverlay) {
            val d = state.detail
            if (d != null) {
                EditEventChallengeOverlay(
                    summary = d.summary,
                    inFlight = state.ownerActionInFlight,
                    onClose = { showEditOverlay = false },
                    onSubmit = { req ->
                        vm.updateChallenge(req)
                        showEditOverlay = false
                        onChanged()
                    }
                )
            }
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
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(theme.bg1.copy(0.85f), theme.bg1.copy(0.55f))
                )
            )
            .border(1.dp, accent.copy(0.28f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // ── Mode header pill ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, label) = when (ev.mode) {
                EventMode.Physical     -> Icons.Rounded.LocationOn to "FİZİKSEL"
                EventMode.Online       -> Icons.Rounded.Link to "ONLINE"
                EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to "HAREKET LİSTESİ"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(0.28f), accent.copy(0.14f))
                        )
                    )
                    .border(1.dp, accent.copy(0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        label,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Date / Time chips (side-by-side) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventMetaChip(
                icon  = Icons.Rounded.CalendarMonth,
                label = "TARİH",
                value = ev.dateIso,
                accent = accent,
                modifier = Modifier.weight(1f)
            )
            if (!ev.timeIso.isNullOrBlank()) {
                EventMetaChip(
                    icon  = Icons.Rounded.Schedule,
                    label = "SAAT",
                    value = ev.timeIso.take(5),
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!ev.timezone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                ev.timezone,
                color = theme.text2,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Mode-specific body
        when (ev.mode) {
            EventMode.Physical -> {
                if (!ev.location.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    LocationRow(
                        icon = Icons.Rounded.LocationOn,
                        title = "BAŞLANGIÇ",
                        value = ev.location,
                        accent = accent,
                        actionLabel = if (ev.geoLat != null && ev.geoLng != null) "HARİTA" else null,
                        onAction = {
                            if (ev.geoLat != null && ev.geoLng != null) {
                                val uri = "geo:${ev.geoLat},${ev.geoLng}?q=${ev.geoLat},${ev.geoLng}(${ev.location})".toUri()
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            }
                        }
                    )
                }
                // Bitiş konumu (rotalı etkinlik)
                if (!ev.endLocation.isNullOrBlank() || (ev.endGeoLat != null && ev.endGeoLng != null)) {
                    Spacer(Modifier.height(8.dp))
                    LocationRow(
                        icon = Icons.Rounded.Flag,
                        title = "BİTİŞ",
                        value = ev.endLocation ?: "${ev.endGeoLat}, ${ev.endGeoLng}",
                        accent = accent,
                        actionLabel = if (ev.endGeoLat != null && ev.endGeoLng != null) "HARİTA" else null,
                        onAction = {
                            if (ev.endGeoLat != null && ev.endGeoLng != null) {
                                val uri = "geo:${ev.endGeoLat},${ev.endGeoLng}?q=${ev.endGeoLat},${ev.endGeoLng}(${ev.endLocation ?: "Bitiş"})".toUri()
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            }
                        }
                    )
                }
                // Rota — both endpoints present. 3 travelmode seçeneği (bisiklet/koşu/yürüyüş)
                if (ev.geoLat != null && ev.geoLng != null && ev.endGeoLat != null && ev.endGeoLng != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "ROTAYI AÇ",
                        color = theme.text2,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TravelModeChip(
                            label = "BİSİKLET",
                            icon = Icons.Rounded.DirectionsBike,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                openRoute(ctx, ev.geoLat, ev.geoLng, ev.endGeoLat, ev.endGeoLng, "bicycling")
                            }
                        )
                        TravelModeChip(
                            label = "KOŞU",
                            icon = Icons.Rounded.DirectionsRun,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Google Maps "running" travelmode'u kabul etmediği için walking ile açıyoruz
                                openRoute(ctx, ev.geoLat, ev.geoLng, ev.endGeoLat, ev.endGeoLng, "walking")
                            }
                        )
                        TravelModeChip(
                            label = "YÜRÜYÜŞ",
                            icon = Icons.Rounded.DirectionsWalk,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                openRoute(ctx, ev.geoLat, ev.geoLng, ev.endGeoLat, ev.endGeoLng, "walking")
                            }
                        )
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

@Composable
private fun EventMetaChip(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2.copy(0.55f))
            .border(1.dp, theme.stroke.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = theme.text2,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = theme.text0,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun LocationRow(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2.copy(0.5f))
            .border(1.dp, theme.stroke.copy(0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = theme.text2,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                value,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.20f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    actionLabel,
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
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
private fun LeaderboardEntryRow(rank: Int = 0, entry: ChallengeLeaderboardEntry, unit: String = "") {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val highlight = entry.isMe
    val isMedal = rank in 1..3
    val medalColor = MEDAL_COLORS.getOrNull(rank - 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    highlight -> accent.copy(0.12f)
                    isMedal && medalColor != null -> medalColor.copy(0.08f)
                    else -> theme.bg1.copy(0.5f)
                }
            )
            .border(
                1.dp,
                when {
                    highlight -> accent.copy(0.4f)
                    isMedal && medalColor != null -> medalColor.copy(0.35f)
                    else -> theme.stroke.copy(0.4f)
                },
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank > 0) {
            RankBadge(rank, accent, theme)
            Spacer(Modifier.width(8.dp))
        }
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
            if (unit.isNotBlank()) "${entry.progress} $unit" else entry.progress.toString(),
            color      = if (highlight) accent else theme.text0,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ChallengeHero(c: ChallengeSummary, accent: Color, isEvent: Boolean) {
    val theme = LocalAppTheme.current
    val today = LocalDate.now()
    val startDate = runCatching { LocalDate.parse(c.startDateIso) }.getOrNull()
    val endDate = runCatching { LocalDate.parse(c.endDateIso) }.getOrNull()
    val daysLeft = if (endDate != null)
        java.time.temporal.ChronoUnit.DAYS.between(today, endDate) else 0L
    val daysToStart = if (startDate != null)
        java.time.temporal.ChronoUnit.DAYS.between(today, startDate) else 0L

    val (statusLabel, statusColor) = when {
        c.isCompleted -> "TAMAMLANDI" to accent
        startDate != null && today.isBefore(startDate) -> "YAKINDA" to Color(0xFFFFB74D)
        endDate != null && today.isAfter(endDate) -> "SONA ERDİ" to theme.text2
        daysLeft == 0L -> "SON GÜN" to Color(0xFFFF8A80)
        else -> "DEVAM EDİYOR" to accent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(0.28f), accent.copy(0.06f))
                )
            )
            .border(1.dp, accent.copy(0.4f), RoundedCornerShape(20.dp))
    ) {
        // Subtle radial accent overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(0.18f), Color.Transparent),
                        radius = 700f
                    )
                )
        )
        Column(Modifier.padding(18.dp)) {
            // Üst row: kategori + status pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isEvent) "ETKİNLİK · ${c.targetType.label.uppercase()}"
                    else c.targetType.label.uppercase(),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(0.18f))
                        .border(1.dp, statusColor.copy(0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                c.title,
                color      = theme.text0,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 26.sp
            )

            // Countdown row
            Spacer(Modifier.height(6.dp))
            val countdownText = when {
                c.isCompleted -> null
                startDate != null && today.isBefore(startDate) ->
                    if (daysToStart == 0L) "Bugün başlıyor"
                    else "$daysToStart gün sonra başlıyor"
                endDate != null && today.isAfter(endDate) -> "Bitti"
                daysLeft == 0L -> "Bugün son gün"
                else -> "$daysLeft gün kaldı"
            }
            if (countdownText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = theme.text1, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        countdownText,
                        color = theme.text1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // İlerleme bölgesi (yalnız joined için anlamlı; ama herkese gösterilsin — 0%)
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${c.myProgress} / ${c.targetValue} ${c.targetType.unit}",
                    color    = theme.text1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "%${(c.progressPct * 100).toInt()}",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(6.dp))
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
        }
    }
}

private val MEDAL_COLORS = listOf(
    Color(0xFFFFD54F),  // gold
    Color(0xFFB0BEC5),  // silver
    Color(0xFFD7945A)   // bronze
)

@Composable
private fun RankBadge(rank: Int, accent: Color, theme: com.avonix.profitness.core.theme.AppThemeState) {
    val color = MEDAL_COLORS.getOrNull(rank - 1) ?: theme.text2
    val isMedal = rank in 1..3
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (isMedal) color.copy(0.22f) else theme.bg2.copy(0.6f))
            .border(1.dp, if (isMedal) color.copy(0.6f) else theme.stroke.copy(0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isMedal) {
            Icon(
                Icons.Rounded.EmojiEvents, null,
                tint = color, modifier = Modifier.size(14.dp)
            )
        } else {
            Text(
                "$rank",
                color = theme.text1,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  OWNER ACTION MENU (3-dot dropdown — anchored to button)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun OwnerActionMenu(
    expanded: Boolean,
    accent: Color,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = LocalAppTheme.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = theme.bg1,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        if (canEdit) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Düzenle",
                        color = theme.text0,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(18.dp))
                },
                onClick = onEdit,
                colors = MenuDefaults.itemColors(textColor = theme.text0)
            )
            HorizontalDivider(color = theme.stroke.copy(0.5f), thickness = 0.5.dp)
        }
        DropdownMenuItem(
            text = {
                Text(
                    "Sil",
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            },
            onClick = onDelete,
            colors = MenuDefaults.itemColors(textColor = Color(0xFFFF8A80))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DELETE CONFIRM DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DeleteConfirmDialog(
    accent: Color,
    inFlight: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val theme = LocalAppTheme.current
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
                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "ETKİNLİĞİ SİL",
                    color = theme.text0,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Bu etkinliği sildiğinde tüm katılımcı ilerlemesi de silinecek. Bu işlem geri alınamaz.",
                color = theme.text1,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(20.dp))
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
                        .clickable(enabled = !inFlight, onClick = onCancel)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VAZGEÇ", color = theme.text1, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5252))
                        .clickable(enabled = !inFlight, onClick = onConfirm)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (inFlight) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("SİL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD MANUAL PROGRESS DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AddProgressDialog(
    accent: Color,
    unit: String,
    targetLabel: String,
    onCancel: () -> Unit,
    onSubmit: (Long) -> Unit
) {
    val theme = LocalAppTheme.current
    var text by remember { mutableStateOf("") }
    val parsed = text.trim().toLongOrNull() ?: 0L

    // Kronometre — sadece dakika bazlı target için
    val showStopwatch = unit == "dk"
    var swRunning by remember { mutableStateOf(false) }
    var swStartMs by remember { mutableStateOf(0L) }
    var swAccumMs by remember { mutableStateOf(0L) }
    var swTickMs by remember { mutableStateOf(0L) }
    val swElapsedMs = swAccumMs + (if (swRunning) swTickMs - swStartMs else 0L)
    LaunchedEffect(swRunning) {
        while (swRunning) {
            swTickMs = System.currentTimeMillis()
            delay(250)
        }
    }

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
                Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "İLERLEMENİ GİR",
                    color = theme.text0,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Kaç $unit $targetLabel?",
                color = theme.text1,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(7) },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                if (text.isEmpty()) {
                                    Text("0", color = theme.text2, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                                inner()
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(unit, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                )
            }

            // Kronometre paneli — sadece dakika bazlı target
            if (showStopwatch) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(0.10f))
                        .border(1.dp, accent.copy(0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "KRONOMETRE",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.weight(1f))
                        val totalSec = (swElapsedMs / 1000).coerceAtLeast(0)
                        val mm = totalSec / 60
                        val ss = totalSec % 60
                        Text(
                            "%02d:%02d".format(mm, ss),
                            color = theme.text0,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Başlat / Durdur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (swRunning) Color(0xFFFF5252).copy(0.22f) else accent.copy(0.22f))
                                .border(
                                    1.dp,
                                    if (swRunning) Color(0xFFFF5252).copy(0.5f) else accent.copy(0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (swRunning) {
                                        // Durdur — biriken süreyi accum'a kaydet
                                        swAccumMs += (System.currentTimeMillis() - swStartMs)
                                        swRunning = false
                                    } else {
                                        swStartMs = System.currentTimeMillis()
                                        swTickMs = swStartMs
                                        swRunning = true
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (swRunning) "DURDUR" else "BAŞLAT",
                                color = if (swRunning) Color(0xFFFF8A80) else accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        // Sıfırla
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.bg2)
                                .border(1.dp, theme.stroke, RoundedCornerShape(10.dp))
                                .clickable(enabled = !swRunning && swElapsedMs > 0L) {
                                    swAccumMs = 0L
                                    swStartMs = 0L
                                    swTickMs = 0L
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "SIFIRLA",
                                color = if (!swRunning && swElapsedMs > 0L) theme.text0 else theme.text2.copy(0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        // Aktar (geçen süreyi text alanına yaz)
                        val elapsedMin = (swElapsedMs / 60_000L).coerceAtLeast(0L)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (elapsedMin > 0L) accent else accent.copy(0.3f))
                                .clickable(enabled = elapsedMin > 0L) {
                                    text = elapsedMin.toString()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "AKTAR",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Kronometre durduğunda 'Aktar' tuşuyla geçen dakikayı yukarıdaki alana yazabilirsin.",
                        color = theme.text2,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
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
                    Text("İPTAL", color = theme.text1, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (parsed > 0L) accent else accent.copy(0.35f))
                        .clickable(enabled = parsed > 0L) { onSubmit(parsed) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("KAYDET", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EDIT EVENT CHALLENGE OVERLAY (owner only — minimal field set)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun EditEventChallengeOverlay(
    summary: ChallengeSummary,
    inFlight: Boolean,
    onClose: () -> Unit,
    onSubmit: (UpdateEventChallengeRequest) -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val ev = summary.event

    var title       by remember(summary.id) { mutableStateOf(summary.title) }
    var description by remember(summary.id) { mutableStateOf(summary.description) }
    var dateIso     by remember(summary.id) { mutableStateOf(ev?.dateIso ?: summary.startDateIso) }
    var timeIso     by remember(summary.id) { mutableStateOf(ev?.timeIso ?: "") }
    var location    by remember(summary.id) { mutableStateOf(ev?.location ?: "") }
    var geoLat      by remember(summary.id) { mutableStateOf(ev?.geoLat?.toString() ?: "") }
    var geoLng      by remember(summary.id) { mutableStateOf(ev?.geoLng?.toString() ?: "") }
    var endLocation by remember(summary.id) { mutableStateOf(ev?.endLocation ?: "") }
    var endLat      by remember(summary.id) { mutableStateOf(ev?.endGeoLat?.toString() ?: "") }
    var endLng      by remember(summary.id) { mutableStateOf(ev?.endGeoLng?.toString() ?: "") }
    var onlineUrl   by remember(summary.id) { mutableStateOf(ev?.onlineUrl ?: "") }
    var targetValue by remember(summary.id) { mutableStateOf(summary.targetValue.takeIf { it > 0 }?.toString() ?: "") }

    val isPhysical = ev?.mode == EventMode.Physical
    val isOnline   = ev?.mode == EventMode.Online

    Dialog(
        onDismissRequest = onClose,
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
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = theme.text0, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "ETKİNLİĞİ DÜZENLE",
                            color = theme.text0,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }
                item { EditField("Başlık", title, { title = it }, accent) }
                item { EditField("Açıklama", description, { description = it }, accent, lines = 3) }
                item { EditField("Tarih (YYYY-MM-DD)", dateIso, { dateIso = it }, accent) }
                item { EditField("Saat (HH:MM, opsiyonel)", timeIso, { timeIso = it }, accent) }
                if (isPhysical) {
                    item { EditField("Başlangıç konumu", location, { location = it }, accent) }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.weight(1f)) { EditField("Lat", geoLat, { geoLat = it }, accent, numeric = true) }
                            Box(Modifier.weight(1f)) { EditField("Lng", geoLng, { geoLng = it }, accent, numeric = true) }
                        }
                    }
                    item { EditField("Bitiş konumu (opsiyonel)", endLocation, { endLocation = it }, accent) }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.weight(1f)) { EditField("Bitiş Lat", endLat, { endLat = it }, accent, numeric = true) }
                            Box(Modifier.weight(1f)) { EditField("Bitiş Lng", endLng, { endLng = it }, accent, numeric = true) }
                        }
                    }
                }
                if (isOnline) {
                    item { EditField("Online URL", onlineUrl, { onlineUrl = it }, accent) }
                }
                item { EditField("Hedef değer (${summary.targetType.unit})", targetValue, { targetValue = it.filter { ch -> ch.isDigit() } }, accent, numeric = true) }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (title.isBlank() || inFlight) accent.copy(0.35f) else accent)
                            .clickable(enabled = title.isNotBlank() && !inFlight) {
                                onSubmit(
                                    UpdateEventChallengeRequest(
                                        challengeId = summary.id,
                                        title       = title.trim(),
                                        description = description.trim().ifBlank { null },
                                        dateIso     = dateIso.trim(),
                                        timeIso     = timeIso.trim().ifBlank { null },
                                        location    = location.trim().ifBlank { null },
                                        geoLat      = geoLat.toDoubleOrNull(),
                                        geoLng      = geoLng.toDoubleOrNull(),
                                        endLocation = endLocation.trim().ifBlank { null },
                                        endGeoLat   = endLat.toDoubleOrNull(),
                                        endGeoLng   = endLng.toDoubleOrNull(),
                                        targetValue = targetValue.toLongOrNull(),
                                        onlineUrl   = onlineUrl.trim().ifBlank { null }
                                    )
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (inFlight) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("KAYDET", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    accent: Color,
    lines: Int = 1,
    numeric: Boolean = false
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Text(
            label.uppercase(),
            color = theme.text2,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = lines == 1,
                maxLines = lines,
                textStyle = TextStyle(color = theme.text0, fontSize = 14.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                    imeAction = if (lines == 1) ImeAction.Next else ImeAction.Default
                )
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
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(0.16f), accent.copy(0.04f))
                )
            )
            .border(1.dp, accent.copy(0.32f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = theme.text0,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(),
            color = theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

// ── Travel mode chip + Maps route helper ────────────────────────────────────
@Composable
private fun TravelModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(0.16f))
            .border(1.dp, accent.copy(0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

private fun openRoute(
    ctx: android.content.Context,
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    travelmode: String
) {
    val uri = ("https://www.google.com/maps/dir/?api=1" +
        "&origin=$originLat,$originLng" +
        "&destination=$destLat,$destLng" +
        "&travelmode=$travelmode").toUri()
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
