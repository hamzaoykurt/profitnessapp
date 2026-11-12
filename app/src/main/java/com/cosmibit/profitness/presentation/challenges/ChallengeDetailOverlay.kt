package com.cosmibit.profitness.presentation.challenges

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.CardCoral
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.exerciseDisplayName
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.domain.challenges.ChallengeEventInfo
import com.cosmibit.profitness.domain.challenges.ChallengeDetail
import com.cosmibit.profitness.domain.challenges.ChallengeKind
import com.cosmibit.profitness.domain.challenges.ChallengeLeaderboardEntry
import com.cosmibit.profitness.domain.challenges.ChallengeMovement
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.ChallengeTargetType
import com.cosmibit.profitness.domain.challenges.ChallengeVisibility
import com.cosmibit.profitness.domain.challenges.EventMode
import com.cosmibit.profitness.presentation.workout.SportType
import com.cosmibit.profitness.domain.challenges.UpdateEventChallengeRequest
import com.cosmibit.profitness.domain.challenges.UpdateMetricChallengeRequest
import com.cosmibit.profitness.domain.challenges.normalizeOnlineEventUrl
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.presentation.components.SinemaDetailHeader
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaGlowBar
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import com.cosmibit.profitness.presentation.workout.SceneImage
import java.time.LocalDate
import kotlinx.coroutines.delay

private val DangerText = Color(0xFFFF6B6B)

@Composable
fun ChallengeDetailOverlay(
    challengeId: String,
    onBack     : () -> Unit,
    onChanged  : () -> Unit
) {
    val theme  = LocalAppTheme.current
    val strings = theme.strings
    val accent = theme.effectiveAccentColor
    val vm: ChallengeDetailViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var showJoinPasswordDialog by remember { mutableStateOf(false) }
    var showOwnerMenu          by remember { mutableStateOf(false) }
    var showDeleteConfirm      by remember { mutableStateOf(false) }
    var showProgressDialog     by remember { mutableStateOf(false) }
    var showEditOverlay        by remember { mutableStateOf(false) }
    var showLeaveConfirm       by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) { vm.load(challengeId) }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            showDeleteConfirm = false
            vm.consumeDeleted()
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
        EdgeToEdgeDialogBars()
        Box(Modifier.fillMaxSize().background(Sinema.page(theme))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 170.dp)
            ) {
                val detailNow = state.detail
                when {
                    state.isLoading -> {
                        item {
                            SinemaDetailHeader(
                                title = strings.challengeLabel, subtitle = null, onBack = onBack
                            )
                        }
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent)
                            }
                        }
                    }
                    state.error != null && detailNow == null -> {
                        item {
                            SinemaDetailHeader(
                                title = strings.challengeLabel, subtitle = null, onBack = onBack
                            )
                        }
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Text(state.error ?: "", color = Sinema.text(theme), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    detailNow != null -> {
                        val detail = detailNow
                        val c = detail.summary
                        val isEvent = c.kind == ChallengeKind.Event
                        val ev = c.event
                        val unit = c.targetType.displayUnit(theme)

                        // ── Kapak: tür fotoğrafı + karartma + başlık + dev hedef ──
                        item {
                            ChallengeHero(
                                c = c,
                                isEvent = isEvent,
                                onBack = onBack,
                                menu = if (state.isOwner) {
                                    {
                                        Box {
                                            SinemaCapsuleIconButton(
                                                icon = Icons.Rounded.MoreVert,
                                                description = theme.t("Menü", "Menu"),
                                                onClick = { showOwnerMenu = true },
                                                selected = false,
                                                size = 44.dp
                                            )
                                            OwnerActionMenu(
                                                expanded = showOwnerMenu,
                                                canEdit = true,
                                                onDismiss = { showOwnerMenu = false },
                                                onEdit = { showOwnerMenu = false; showEditOverlay = true },
                                                onDelete = { showOwnerMenu = false; showDeleteConfirm = true }
                                            )
                                        }
                                    }
                                } else null
                            )
                        }

                        if (isEvent && ev != null) {
                            item { EventInfoCard(ev, accent) }
                        }

                        // ── İlerleme + katılımcı ──
                        item {
                            val participantText = c.maxParticipants?.let { "${c.participantsCount}/$it" } ?: "${c.participantsCount}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SinemaBigStat(
                                    value = "${c.myProgress}", unit = unit, size = 34,
                                    label = theme.t("İlerleme", "Progress"), modifier = Modifier.weight(1f)
                                )
                                SinemaBigStat(
                                    value = participantText, size = 34,
                                    label = theme.t("Katılımcı", "Participants"), modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (c.description.isNotBlank()) {
                            item {
                                Text(
                                    c.description,
                                    color = Sinema.textSecondary(theme),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (c.visibility == ChallengeVisibility.Private) {
                                    Icon(Icons.Rounded.Lock, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(theme.t("Özel", "Private"), color = Sinema.textSecondary(theme), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    "${c.startDateIso} → ${c.endDateIso}",
                                    color = Sinema.textSecondary(theme),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isEvent && c.isJoined && ev?.dateIso == LocalDate.now().toString()) {
                            item {
                                SkipProgramToggle(
                                    enabled = state.skipProgramToday,
                                    accent = accent,
                                    onToggle = { vm.setSkipProgramToday(!state.skipProgramToday) }
                                )
                            }
                        }

                        if (isEvent && ev?.mode == EventMode.MovementList && c.isJoined
                            && detail.movements.isNotEmpty()
                        ) {
                            item { SinemaSectionLabel(theme.t("Hareketler", "Movements"), Modifier.padding(top = 8.dp)) }
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

                        val participantRows = detail.visibleParticipantRows(
                            currentUserId = state.currentUserId,
                            isOwner = state.isOwner
                        )

                        item { SinemaSectionLabel(theme.t("Katılımcılar", "Participants"), Modifier.padding(top = 8.dp)) }

                        if (participantRows.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        theme.t("Henüz katılımcı yok", "No participants yet"),
                                        color = Sinema.textSecondary(theme),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                participantRows,
                                key = { _, e -> "lb_${e.userId}" }
                            ) { index, entry ->
                                LeaderboardEntryRow(rank = index + 1, entry = entry, unit = unit)
                            }
                        }
                    }
                }
            }

            // ── Alta sabit eylem: arkası yumuşak geçiş ──
            val bottomDetail = state.detail
            if (bottomDetail != null) {
                val c = bottomDetail.summary
                val isEvent = c.kind == ChallengeKind.Event
                val ev = c.event
                val canAddProgress = isEvent && c.isJoined && ev != null &&
                    ev.mode != EventMode.MovementList && c.targetValue > 0
                val action = {
                    when {
                        c.isJoined -> { showLeaveConfirm = true }
                        c.visibility == ChallengeVisibility.Private && !c.isInvited -> {
                            showJoinPasswordDialog = true
                        }
                        else -> { vm.join(null); onChanged() }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Sinema.page(theme).copy(0.92f),
                                1f to Sinema.page(theme)
                            )
                        )
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canAddProgress) {
                        SinemaGoButton(
                            label = theme.t("İLERLEME EKLE", "ADD PROGRESS"),
                            icon = Icons.Rounded.Add,
                            onClick = { if (!state.submittingProgress) showProgressDialog = true },
                            dim = state.submittingProgress
                        )
                    }
                    if (c.isJoined) {
                        SinemaCapsule(
                            Modifier.fillMaxWidth(), onClick = action, enabled = !state.inFlight, height = 52.dp,
                            light = !theme.isDark, horizontalArrangement = Arrangement.Center
                        ) { fg ->
                            if (state.inFlight) {
                                CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(strings.leaveLabel.sinemaUpper(theme), color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em, maxLines = 1)
                        }
                    } else {
                        SinemaGoButton(
                            label = strings.joinLabel.sinemaUpper(theme),
                            icon = Icons.Rounded.Bolt,
                            onClick = { if (!state.inFlight) action() },
                            dim = state.inFlight
                        )
                    }
                }
            }

            if (showLeaveConfirm) {
                val isPrivate = state.detail?.summary?.visibility == ChallengeVisibility.Private
                com.cosmibit.profitness.presentation.components.SinemaDialog(
                    onDismiss = { showLeaveConfirm = false },
                    title = theme.t("Ayrılmak istiyor musun?", "Leave this challenge?"),
                    body = if (isPrivate) theme.t(
                        "Bu challenge özel. Ayrılırsan şifre olmadan geri giremezsin.",
                        "This challenge is private. If you leave, you can't rejoin without the password."
                    ) else theme.t(
                        "Ayrılırsan ilerlemen listende görünmez.",
                        "If you leave, it will no longer appear in your joined list."
                    ),
                    confirmLabel = strings.leaveLabel.sinemaUpper(theme),
                    confirmIcon = Icons.Rounded.Check,
                    onConfirm = { showLeaveConfirm = false; vm.leave(); onChanged() },
                    dismissLabel = theme.t("Vazgeç", "Cancel")
                )
            }

            if (showJoinPasswordDialog) {
                PrivateJoinPasswordDialog(
                    onCancel = { showJoinPasswordDialog = false },
                    onSubmit = { pwd ->
                        showJoinPasswordDialog = false
                        vm.join(pwd)
                        onChanged()
                    }
                )
            }

            if (showProgressDialog) {
                val c = state.detail?.summary
                if (c != null) {
                    AddProgressDialog(
                        unit = c.targetType.displayUnit(theme),
                        targetLabel = c.targetType.displayLabel(theme),
                        onCancel = { showProgressDialog = false },
                        onSubmit = { amount ->
                            showProgressDialog = false
                            vm.addManualProgress(amount)
                            onChanged()
                        }
                    )
                }
            }

            if (showDeleteConfirm) {
                DeleteConfirmDialog(
                    inFlight = state.ownerActionInFlight,
                    onCancel = { showDeleteConfirm = false },
                    onConfirm = { vm.deleteChallenge() }
                )
            }

            if (showEditOverlay) {
                val d = state.detail
                if (d != null) {
                    if (d.summary.kind == ChallengeKind.Event) {
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
                    } else {
                        EditMetricChallengeOverlay(
                            summary = d.summary,
                            inFlight = state.ownerActionInFlight,
                            onClose = { showEditOverlay = false },
                            onSubmit = { req ->
                                vm.updateMetricChallenge(req)
                                showEditOverlay = false
                                onChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeToEdgeDialogBars() {
    val theme = LocalAppTheme.current
    val view = LocalView.current

    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !theme.isDark
        }
    }
}

/** Tür -> kategori foto kaynağı (liste ekranındaki poster ile aynı eşleme). */
private fun heroPhotoCategory(c: ChallengeSummary): String {
    val sport = c.event?.sportType
    return when {
        sport == SportType.Strength -> "Göğüs"
        sport == SportType.YogaPilates -> "Core"
        sport != null -> "Kardiyo"
        c.targetType == ChallengeTargetType.TotalDistanceM ||
            c.targetType == ChallengeTargetType.TotalDistanceKm ||
            c.targetType == ChallengeTargetType.TotalDurationMinutes -> "Kardiyo"
        c.targetType == ChallengeTargetType.TotalWorkouts ||
            c.targetType == ChallengeTargetType.MovementsCompleted -> "Göğüs"
        else -> ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HERO (foto kapak)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ChallengeHero(
    c: ChallengeSummary,
    isEvent: Boolean,
    onBack: () -> Unit,
    menu: (@Composable () -> Unit)?
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val today = LocalDate.now()
    val startDate = runCatching { LocalDate.parse(c.startDateIso) }.getOrNull()
    val endDate = runCatching { LocalDate.parse(c.endDateIso) }.getOrNull()
    val daysLeft = if (endDate != null) java.time.temporal.ChronoUnit.DAYS.between(today, endDate) else 0L
    val daysToStart = if (startDate != null) java.time.temporal.ChronoUnit.DAYS.between(today, startDate) else 0L
    val ended = endDate != null && today.isAfter(endDate)
    val notStarted = startDate != null && today.isBefore(startDate)
    val live = !c.isCompleted && !ended && !notStarted
    val unit = c.targetType.displayUnit(theme)
    val numFmt = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr")) }

    val statusLabel = when {
        c.isCompleted -> theme.t("TAMAMLANDI", "COMPLETED")
        notStarted -> theme.t("YAKINDA", "UPCOMING")
        ended -> theme.t("SONA ERDİ", "ENDED")
        else -> theme.t("DEVAM EDİYOR", "ACTIVE")
    }
    val countdownText = when {
        c.isCompleted -> null
        notStarted ->
            if (daysToStart == 0L) theme.t("Bugün başlıyor", "Starts today")
            else theme.t("$daysToStart gün sonra başlıyor", "Starts in $daysToStart days")
        ended -> theme.t("Bitti", "Ended")
        daysLeft == 0L -> theme.t("Bugün son gün", "Last day today")
        else -> theme.t("$daysLeft gün kaldı", "$daysLeft days left")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Color.Black)
    ) {
        SceneImage(
            url = categoryImageUrl(heroPhotoCategory(c)),
            modifier = Modifier.matchParentSize(),
            requestWidthPx = 1080, requestHeightPx = 1080, fallbackGradient = true
        )
        Box(Modifier.matchParentSize().background(Sinema.BottomScrim))
        Box(
            Modifier.fillMaxWidth().height(150.dp).background(
                Brush.verticalGradient(0f to Color.Black.copy(0.55f), 1f to Color.Transparent)
            )
        )
        Column(
            Modifier.matchParentSize().statusBarsPadding().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack, accent = accent, size = 48.dp)
                Spacer(Modifier.weight(1f))
                menu?.invoke()
            }
            Column {
                // TEK durum etiketi
                SinemaCapsule(height = 30.dp, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp)) { fg ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (c.isCompleted || live) accent else fg.copy(0.5f)))
                    Spacer(Modifier.width(6.dp))
                    Text(statusLabel, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c.visibility == ChallengeVisibility.Private) {
                        Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        c.title,
                        style = Sinema.Title.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        numFmt.format(c.targetValue),
                        style = Sinema.Display.copy(fontSize = 72.sp),
                        color = Color.White, maxLines = 1
                    )
                    Text(
                        unit, style = Sinema.Title.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                    )
                }
                val kindLabel = if (isEvent) "${theme.strings.eventLabel} · ${c.targetType.displayLabel(theme)}" else c.targetType.displayLabel(theme)
                Text(
                    (if (countdownText != null) "$kindLabel · $countdownText" else kindLabel).sinemaUpper(theme),
                    style = Sinema.Label, color = Color.White.copy(0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${numFmt.format(c.myProgress)} / ${numFmt.format(c.targetValue)} $unit",
                        color = Color.White.copy(0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text("%${(c.progressPct * 100).toInt()}", color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(8.dp))
                SinemaGlowBar(progress = c.progressPct, color = accent)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DİYALOG YARDIMCILARI
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SinemaCardDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(28.dp)).padding(24.dp),
            content = content
        )
    }
}

@Composable
private fun DialogTextAction(label: String, onClick: () -> Unit, color: Color? = null, enabled: Boolean = true) {
    val theme = LocalAppTheme.current
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color ?: Sinema.textSecondary(theme), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PRIVATE JOIN PASSWORD DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PrivateJoinPasswordDialog(
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    var password by remember { mutableStateOf("") }

    SinemaCardDialog(onDismiss = onCancel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = theme.effectiveAccentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.privateJoinTitle, style = Sinema.Title.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = Sinema.text(theme))
        }
        Spacer(Modifier.height(10.dp))
        Text(strings.privateJoinHint, color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(16.dp))
        SinemaGlassField(
            value = password, onValueChange = { password = it },
            label = theme.t("Şifre", "Password"), placeholder = "••••••",
            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(20.dp))
        SinemaGoOrIdle(
            label = strings.joinLabel.sinemaUpper(theme), icon = Icons.Rounded.Bolt,
            enabled = password.isNotBlank(), onClick = { onSubmit(password) }
        )
        DialogTextAction(strings.privateJoinCancel, onCancel)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EVENT INFO CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EventInfoCard(ev: ChallengeEventInfo, accent: Color) {
    val theme = LocalAppTheme.current
    val ctx = LocalContext.current
    val secondary = Sinema.textSecondary(theme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, label) = when (ev.mode) {
                EventMode.Physical -> Icons.Rounded.LocationOn to ev.mode.displayLabel(theme)
                EventMode.Online -> Icons.Rounded.Link to ev.mode.displayLabel(theme)
                EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to ev.mode.displayLabel(theme)
            }
            Icon(icon, null, tint = secondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label.sinemaUpper(theme), color = secondary, style = Sinema.Label)
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventMetaChip(
                icon = Icons.Rounded.CalendarMonth, label = theme.t("TARİH", "DATE"),
                value = ev.dateIso, modifier = Modifier.weight(1f)
            )
            if (!ev.timeIso.isNullOrBlank()) {
                EventMetaChip(
                    icon = Icons.Rounded.Schedule, label = theme.t("SAAT", "TIME"),
                    value = ev.timeIso.take(5), modifier = Modifier.weight(1f)
                )
            }
        }
        if (!ev.timezone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(ev.timezone, color = secondary, fontSize = 11.sp)
        }

        when (ev.mode) {
            EventMode.Physical -> {
                val startQuery = ev.location?.trim()?.takeIf { it.isNotBlank() }
                    ?: coordinateQuery(ev.geoLat, ev.geoLng)
                val endQuery = ev.endLocation?.trim()?.takeIf { it.isNotBlank() }
                    ?: coordinateQuery(ev.endGeoLat, ev.endGeoLng)
                if (!ev.location.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    LocationRow(
                        icon = Icons.Rounded.LocationOn,
                        title = theme.t("BAŞLANGIÇ", "START"),
                        value = ev.location,
                        accent = accent,
                        actionLabel = if (startQuery != null) theme.t("HARİTA", "MAP") else null,
                        onAction = { startQuery?.let { openMapSearch(ctx, it) } }
                    )
                }
                if (!ev.endLocation.isNullOrBlank() || endQuery != null) {
                    Spacer(Modifier.height(8.dp))
                    LocationRow(
                        icon = Icons.Rounded.Flag,
                        title = theme.t("BİTİŞ", "FINISH"),
                        value = ev.endLocation ?: endQuery.orEmpty(),
                        accent = accent,
                        actionLabel = if (endQuery != null) theme.t("HARİTA", "MAP") else null,
                        onAction = { endQuery?.let { openMapSearch(ctx, it) } }
                    )
                }
                if (startQuery != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(theme.t("ROTAYI AÇ", "OPEN ROUTE"), color = secondary, style = Sinema.Label)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TravelModeChip(
                            label = theme.t("BİSİKLET", "CYCLING"), icon = Icons.Rounded.DirectionsBike,
                            modifier = Modifier.weight(1f),
                            onClick = { openEventRoute(ctx, startQuery, endQuery, "bicycling") }
                        )
                        TravelModeChip(
                            label = theme.t("KOŞU", "RUN"), icon = Icons.Rounded.DirectionsRun,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Google Maps "running" travelmode'u kabul etmediği için walking ile açıyoruz
                                openEventRoute(ctx, startQuery, endQuery, "walking")
                            }
                        )
                        TravelModeChip(
                            label = theme.t("YÜRÜYÜŞ", "WALK"), icon = Icons.Rounded.DirectionsWalk,
                            modifier = Modifier.weight(1f),
                            onClick = { openEventRoute(ctx, startQuery, endQuery, "walking") }
                        )
                    }
                }
            }
            EventMode.Online -> {
                val safeOnlineUrl = normalizeOnlineEventUrl(ev.onlineUrl)
                if (safeOnlineUrl != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, safeOnlineUrl.toUri()))
                                }
                            }
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            safeOnlineUrl, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline, maxLines = 1
                        )
                    }
                } else if (!ev.onlineUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        theme.t("Geçersiz online bağlantı", "Invalid online link"),
                        color = CardCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            EventMode.MovementList -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    theme.t(
                        "${ev.myCompletedCount} / ${ev.movementsCount} hareket tamamlandı",
                        "${ev.myCompletedCount} / ${ev.movementsCount} movements completed"
                    ),
                    color = Sinema.text(theme), fontSize = 13.sp, fontWeight = FontWeight.Bold
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
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = Sinema.textSecondary(theme), style = Sinema.Label)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = Sinema.text(theme), style = Sinema.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
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
            .clip(RoundedCornerShape(16.dp))
            .background(Sinema.line(theme))
            .then(if (actionLabel != null) Modifier.clickable(onClick = onAction) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Sinema.textSecondary(theme), style = Sinema.Label)
            Spacer(Modifier.height(2.dp))
            Text(value, color = Sinema.text(theme), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        }
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Text(actionLabel, color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
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
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(done = enabled, accent = accent, size = 24.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                theme.t("BUGÜN PROGRAMI ATLA", "SKIP TODAY'S PLAN"),
                color = Sinema.text(theme), style = Sinema.Label
            )
            Spacer(Modifier.height(2.dp))
            Text(
                theme.t(
                    "Bu etkinliği yapacağın için anasayfadaki günlük program gizlenir",
                    "Your daily plan on the home screen is hidden because you are doing this event"
                ),
                color = Sinema.textSecondary(theme), fontSize = 12.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CheckDot(done: Boolean, accent: Color, size: androidx.compose.ui.unit.Dp, pending: Boolean = false) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (done) accent else Color.Transparent)
            .border(1.5.dp, if (done) accent else Sinema.textSecondary(theme).copy(0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            pending -> CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            done -> Icon(Icons.Rounded.Check, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(size * 0.62f))
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
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .clickable(enabled = !pending, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(done = done, accent = accent, size = 26.dp, pending = pending)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                theme.exerciseDisplayName(movement.exerciseName),
                color = if (done) Sinema.textSecondary(theme) else Sinema.text(theme),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
            val sub = movementSpecLabel(movement, theme)
            if (sub.isNotEmpty()) {
                Text(sub, color = Sinema.textSecondary(theme), fontSize = 12.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Katılımcı satırı
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LeaderboardEntryRow(rank: Int = 0, entry: ChallengeLeaderboardEntry, unit: String = "") {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val highlight = entry.isMe
    val displayName = if (entry.isMe) theme.t("Sen", "You") else entry.displayName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank > 0) {
            Text(
                "$rank",
                style = Sinema.Title.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
                color = if (rank == 1) accent else Sinema.textSecondary(theme),
                modifier = Modifier.width(26.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(2.dp, if (highlight) accent else Sinema.line(theme), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(Sinema.line(theme)),
            contentAlignment = Alignment.Center
        ) {
            val av = entry.avatarUrl
            if (av != null && av.startsWith("http")) {
                AsyncImage(model = av, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(av ?: "🏋️", fontSize = 16.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayName,
                color = Sinema.text(theme),
                fontSize = 14.sp,
                fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (entry.isCompleted) {
                Text(theme.t("Tamamladı ✓", "Completed ✓"), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            if (unit.isNotBlank()) "${entry.progress} $unit" else entry.progress.toString(),
            color = if (highlight) accent else Sinema.text(theme),
            style = Sinema.Title.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  OWNER ACTION MENU (3-dot dropdown — koyu cam yüzey, tehlike = kırmızı yazı)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun OwnerActionMenu(
    expanded: Boolean,
    canEdit: Boolean = true,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416).copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        val theme = LocalAppTheme.current
        if (canEdit) {
            DropdownMenuItem(
                text = {
                    Text(theme.t("Düzenle", "Edit"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp)) },
                onClick = onEdit,
                colors = MenuDefaults.itemColors(textColor = Color.White)
            )
        }
        DropdownMenuItem(
            text = {
                Text(theme.t("Sil", "Delete"), color = DangerText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = DangerText, modifier = Modifier.size(18.dp)) },
            onClick = onDelete,
            colors = MenuDefaults.itemColors(textColor = DangerText)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DELETE CONFIRM DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DeleteConfirmDialog(
    inFlight: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val theme = LocalAppTheme.current
    SinemaCardDialog(onDismiss = { if (!inFlight) onCancel() }) {
        Text(
            theme.t("Etkinliği sil", "Delete event"),
            style = Sinema.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = Sinema.text(theme)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            theme.t(
                "Bu etkinliği sildiğinde tüm katılımcı ilerlemesi de silinecek. Bu işlem geri alınamaz.",
                "Deleting this event also removes all participant progress. This action cannot be undone."
            ),
            color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        SinemaCapsule(
            Modifier.fillMaxWidth(), onClick = onConfirm, enabled = !inFlight, height = 56.dp,
            light = !theme.isDark, horizontalArrangement = Arrangement.Center
        ) { _ ->
            if (inFlight) {
                CircularProgressIndicator(color = DangerText, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Rounded.Delete, null, tint = DangerText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.t("SİL", "DELETE"), color = DangerText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em)
            }
        }
        DialogTextAction(theme.t("Vazgeç", "Cancel"), onCancel, enabled = !inFlight)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD MANUAL PROGRESS DIALOG
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AddProgressDialog(
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

    SinemaCardDialog(onDismiss = onCancel) {
        Text(
            theme.t("İlerlemeni gir", "Enter progress"),
            style = Sinema.Title.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = Sinema.text(theme)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            theme.t("Kaç $unit $targetLabel?", "How many $unit of $targetLabel?"),
            color = Sinema.textSecondary(theme), fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
        SinemaGlassField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() }.take(7) },
            label = theme.t("Miktar", "Amount"), placeholder = "0", suffix = unit,
            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
        )

        if (showStopwatch) {
            Spacer(Modifier.height(18.dp))
            val totalSec = (swElapsedMs / 1000).coerceAtLeast(0)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%02d:%02d".format(totalSec / 60, totalSec % 60),
                    style = Sinema.Metric.copy(fontSize = 40.sp), color = Sinema.text(theme)
                )
                Spacer(Modifier.weight(1f))
                Text(theme.t("KRONOMETRE", "STOPWATCH"), color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            val elapsedMin = (swElapsedMs / 60_000L).coerceAtLeast(0L)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp, light = !theme.isDark,
                    horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp),
                    onClick = {
                        if (swRunning) {
                            swAccumMs += (System.currentTimeMillis() - swStartMs)
                            swRunning = false
                        } else {
                            swStartMs = System.currentTimeMillis()
                            swTickMs = swStartMs
                            swRunning = true
                        }
                    }
                ) { fg ->
                    Text(
                        if (swRunning) theme.t("DURDUR", "STOP") else theme.t("BAŞLAT", "START"),
                        color = if (swRunning) DangerText else fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                }
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp, light = !theme.isDark,
                    horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp),
                    enabled = !swRunning && swElapsedMs > 0L,
                    onClick = { swAccumMs = 0L; swStartMs = 0L; swTickMs = 0L }
                ) { fg ->
                    Text(
                        theme.t("SIFIRLA", "RESET"),
                        color = if (!swRunning && swElapsedMs > 0L) fg else fg.copy(0.4f),
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                }
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp, selected = elapsedMin > 0L, light = !theme.isDark,
                    horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp),
                    enabled = elapsedMin > 0L,
                    onClick = { text = elapsedMin.toString() }
                ) { fg ->
                    Text(theme.t("AKTAR", "APPLY"), color = if (elapsedMin > 0L) fg else fg.copy(0.4f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "Kronometre durduğunda 'Aktar' tuşuyla geçen dakikayı yukarıdaki alana yazabilirsin.",
                    "When the stopwatch is stopped, use Apply to copy elapsed minutes into the field above."
                ),
                color = Sinema.textSecondary(theme), fontSize = 12.sp, lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(22.dp))
        SinemaGoOrIdle(
            label = theme.t("KAYDET", "SAVE"), icon = Icons.Rounded.Check,
            enabled = parsed > 0L, onClick = { onSubmit(parsed) }
        )
        DialogTextAction(theme.t("İptal", "Cancel"), onCancel)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EDIT METRIC CHALLENGE OVERLAY (owner)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun EditMetricChallengeOverlay(
    summary: ChallengeSummary,
    inFlight: Boolean,
    onClose: () -> Unit,
    onSubmit: (UpdateMetricChallengeRequest) -> Unit
) {
    val theme  = LocalAppTheme.current

    var title        by remember(summary.id) { mutableStateOf(summary.title) }
    var description  by remember(summary.id) { mutableStateOf(summary.description) }
    var targetType   by remember(summary.id) { mutableStateOf(summary.targetType) }
    var targetValue  by remember(summary.id) { mutableStateOf(summary.targetValue.takeIf { it > 0 }?.toString() ?: "") }
    var startDateIso by remember(summary.id) { mutableStateOf(summary.startDateIso) }
    var endDateIso   by remember(summary.id) { mutableStateOf(summary.endDateIso) }

    val parsedTarget = targetValue.toLongOrNull() ?: 0L
    val parsedStartDate = runCatching { LocalDate.parse(startDateIso.trim()) }.getOrNull()
    val parsedEndDate = runCatching { LocalDate.parse(endDateIso.trim()) }.getOrNull()
    val canSave = title.isNotBlank() &&
        parsedTarget in 1L..Int.MAX_VALUE.toLong() &&
        parsedStartDate != null &&
        parsedEndDate != null &&
        !parsedEndDate.isBefore(parsedStartDate) &&
        !inFlight

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        EdgeToEdgeDialogBars()
        Box(Modifier.fillMaxSize().background(Sinema.page(theme))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item { SinemaDetailHeader(title = theme.t("Challenge'ı düzenle", "Edit challenge"), subtitle = null, onBack = onClose) }
                item { EditField(theme.t("Başlık", "Title"), title, { title = it }) }
                item { EditField(theme.t("Açıklama", "Description"), description, { description = it }, lines = 3) }
                item { MetricTargetTypeSection(targetType, { targetType = it }) }
                item { EditField(theme.t("Hedef değer", "Target value") + " (${targetType.displayUnit(theme)})", targetValue, { targetValue = it.filter { ch -> ch.isDigit() } }, numeric = true) }
                item { EditField(theme.t("Başlangıç tarihi (YYYY-MM-DD)", "Start date (YYYY-MM-DD)"), startDateIso, { startDateIso = it }) }
                item { EditField(theme.t("Bitiş tarihi (YYYY-MM-DD)", "End date (YYYY-MM-DD)"), endDateIso, { endDateIso = it }) }

                item {
                    SinemaGoOrIdle(
                        label = theme.t("KAYDET", "SAVE"), icon = Icons.Rounded.Check, enabled = canSave,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        onClick = {
                            onSubmit(
                                UpdateMetricChallengeRequest(
                                    challengeId = summary.id,
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    targetType = targetType,
                                    targetValue = parsedTarget,
                                    startDateIso = parsedStartDate.toString(),
                                    endDateIso = parsedEndDate.toString()
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTargetTypeSection(
    targetType: ChallengeTargetType,
    onTargetType: (ChallengeTargetType) -> Unit
) {
    val theme = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(theme.t("HEDEF TİPİ", "TARGET TYPE"), color = Sinema.textSecondary(theme), style = Sinema.Label)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChallengeTargetType.selectableForMetric.forEach { type ->
                SinemaCapsule(
                    Modifier.fillMaxWidth(), selected = type == targetType, onClick = { onTargetType(type) },
                    height = 52.dp, light = !theme.isDark
                ) { fg ->
                    Icon(metricTargetIcon(type), null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(type.displayLabel(theme), color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(type.displayUnit(theme), color = fg.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
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
    val ev = summary.event

    var title       by remember(summary.id) { mutableStateOf(summary.title) }
    var description by remember(summary.id) { mutableStateOf(summary.description) }
    var dateIso     by remember(summary.id) { mutableStateOf(ev?.dateIso ?: summary.startDateIso) }
    var timeIso     by remember(summary.id) { mutableStateOf(ev?.timeIso ?: "") }
    var location    by remember(summary.id) { mutableStateOf(ev?.location ?: "") }
    var endLocation by remember(summary.id) { mutableStateOf(ev?.endLocation ?: "") }
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
        EdgeToEdgeDialogBars()
        Box(Modifier.fillMaxSize().background(Sinema.page(theme))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item { SinemaDetailHeader(title = theme.t("Etkinliği düzenle", "Edit event"), subtitle = null, onBack = onClose) }
                item { EditField(theme.t("Başlık", "Title"), title, { title = it }) }
                item { EditField(theme.t("Açıklama", "Description"), description, { description = it }, lines = 3) }
                item { EditField(theme.t("Tarih (YYYY-MM-DD)", "Date (YYYY-MM-DD)"), dateIso, { dateIso = it }) }
                item { EditField(theme.t("Saat (HH:MM, opsiyonel)", "Time (HH:MM, optional)"), timeIso, { timeIso = it }) }
                if (isPhysical) {
                    item { EditField(theme.t("Başlangıç konumu", "Start location"), location, { location = it }) }
                    item { EditField(theme.t("Bitiş konumu (opsiyonel)", "End location (optional)"), endLocation, { endLocation = it }) }
                }
                if (isOnline) {
                    item { EditField("Online URL", onlineUrl, { onlineUrl = it }) }
                }
                item { EditField(theme.t("Hedef değer", "Target value") + " (${summary.targetType.displayUnit(theme)})", targetValue, { targetValue = it.filter { ch -> ch.isDigit() } }, numeric = true) }

                item {
                    SinemaGoOrIdle(
                        label = theme.t("KAYDET", "SAVE"), icon = Icons.Rounded.Check,
                        enabled = title.isNotBlank() && !inFlight,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        onClick = {
                            onSubmit(
                                UpdateEventChallengeRequest(
                                    challengeId = summary.id,
                                    title       = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    dateIso     = dateIso.trim(),
                                    timeIso     = timeIso.trim().ifBlank { null },
                                    location    = location.trim().ifBlank { null },
                                    geoLat      = ev?.geoLat,
                                    geoLng      = ev?.geoLng,
                                    endLocation = endLocation.trim().ifBlank { null },
                                    endGeoLat   = ev?.endGeoLat,
                                    endGeoLng   = ev?.endGeoLng,
                                    targetValue = targetValue.toLongOrNull(),
                                    onlineUrl   = onlineUrl.trim().ifBlank { null }
                                )
                            )
                        }
                    )
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
    lines: Int = 1,
    numeric: Boolean = false
) {
    SinemaGlassField(
        value = value, onValueChange = onChange, label = label,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
        imeAction = if (lines == 1) ImeAction.Next else ImeAction.Default,
        capitalization = if (numeric) androidx.compose.ui.text.input.KeyboardCapitalization.None
        else androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
        singleLine = lines == 1
    )
}

// ── Travel mode chip + Maps route helper ────────────────────────────────────
@Composable
private fun TravelModeChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        modifier, onClick = onClick, height = 48.dp, light = !theme.isDark,
        horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp)
    ) { fg ->
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

private fun metricTargetIcon(type: ChallengeTargetType): ImageVector = when (type) {
    ChallengeTargetType.TotalWorkouts        -> Icons.Rounded.FitnessCenter
    ChallengeTargetType.TotalXp              -> Icons.Rounded.Bolt
    ChallengeTargetType.CurrentStreak        -> Icons.Rounded.LocalFireDepartment
    ChallengeTargetType.TotalDurationMinutes -> Icons.Rounded.Timer
    ChallengeTargetType.TotalDistanceM       -> Icons.Rounded.Straighten
    ChallengeTargetType.TotalDistanceKm      -> Icons.Rounded.Speed
    ChallengeTargetType.MovementsCompleted   -> Icons.Rounded.PlaylistAddCheck
}

private fun movementSpecLabel(
    movement: ChallengeMovement,
    theme: com.cosmibit.profitness.core.theme.AppThemeState
): String = buildString {
    val hasStrength = movement.suggestedSets != null || movement.suggestedReps != null
    if (hasStrength) {
        movement.suggestedSets?.let { append("${it} set") }
        movement.suggestedReps?.let {
            if (isNotEmpty()) append(" · ")
            append("${it} ${theme.t("tekrar", "reps")}")
        }
        return@buildString
    }
    movement.suggestedDurSec?.let { append(formatMovementDuration(it)) }
}

private fun formatMovementDuration(seconds: Int): String =
    if (seconds >= 60) "${(seconds / 60).coerceAtLeast(1)} dk" else "${seconds}s"

private fun ChallengeDetail.visibleParticipantRows(
    currentUserId: String?,
    isOwner: Boolean
): List<ChallengeLeaderboardEntry> {
    if (leaderboard.isNotEmpty()) {
        return leaderboard.sortedWith(
            compareByDescending<ChallengeLeaderboardEntry> { it.progress }
                .thenBy { it.displayName.lowercase() }
        )
    }

    val creatorIsMe = currentUserId != null && currentUserId == summary.creatorId
    val fallback = buildList {
        if (summary.creatorId.isNotBlank() && (isOwner || creatorIsMe)) {
            add(
                ChallengeLeaderboardEntry(
                    userId = summary.creatorId,
                    displayName = summary.creatorName.ifBlank { "Anonim" },
                    avatarUrl = summary.creatorAvatar,
                    progress = if (creatorIsMe) summary.myProgress else 0L,
                    isMe = creatorIsMe,
                    isCompleted = creatorIsMe && summary.isCompleted
                )
            )
        }
        if (summary.isJoined && currentUserId != null && currentUserId != summary.creatorId) {
            add(
                ChallengeLeaderboardEntry(
                    userId = currentUserId,
                    displayName = "Sen",
                    avatarUrl = null,
                    progress = summary.myProgress,
                    isMe = true,
                    isCompleted = summary.isCompleted
                )
            )
        }
    }

    return fallback.sortedWith(
        compareByDescending<ChallengeLeaderboardEntry> { it.progress }
            .thenBy { it.displayName.lowercase() }
    )
}

private fun openEventRoute(
    ctx: android.content.Context,
    start: String,
    end: String?,
    travelmode: String
) {
    val destination = end ?: start
    val uri = buildString {
        append("https://www.google.com/maps/dir/?api=1")
        append("&destination=${Uri.encode(destination)}")
        if (end != null) append("&waypoints=${Uri.encode(start)}")
        append("&travelmode=$travelmode")
    }.toUri()
    openInMaps(ctx, uri)
}

private fun openMapSearch(
    ctx: android.content.Context,
    query: String
) {
    val uri = ("https://www.google.com/maps/search/?api=1" +
        "&query=${Uri.encode(query)}").toUri()
    openInMaps(ctx, uri)
}

private fun coordinateQuery(lat: Double?, lng: Double?): String? =
    if (lat != null && lng != null) "$lat,$lng" else null

private fun openInMaps(ctx: android.content.Context, uri: Uri) {
    fun Intent.readyFor(ctx: android.content.Context): Intent = apply {
        if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
        .setPackage("com.google.android.apps.maps")
        .readyFor(ctx)
    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).readyFor(ctx)

    runCatching {
        ctx.startActivity(mapsIntent)
    }.recoverCatching {
        ctx.startActivity(fallbackIntent)
    }
}
