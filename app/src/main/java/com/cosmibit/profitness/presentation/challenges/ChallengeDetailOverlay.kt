package com.cosmibit.profitness.presentation.challenges

import com.cosmibit.profitness.core.theme.accentColor
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import com.cosmibit.profitness.presentation.components.safeTopPadding
import com.cosmibit.profitness.presentation.components.safeBottomPadding
import com.cosmibit.profitness.presentation.components.FullScreenDialogWindow
import android.content.Intent
import com.cosmibit.profitness.core.theme.sinemaClickable
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
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
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
import com.cosmibit.profitness.core.theme.sinemaRaised
import com.cosmibit.profitness.core.theme.SinemaRaise
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.SinemaRadius
import com.cosmibit.profitness.core.theme.SinemaBackdrop
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.CardCoral
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.accentHi
import com.cosmibit.profitness.core.theme.accentHiTextColor
import com.cosmibit.profitness.core.theme.rewardGold
import com.cosmibit.profitness.core.theme.rewardGoldTextColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.exerciseDisplayName
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.presentation.components.SinemaTypeCover
import com.cosmibit.profitness.presentation.components.coverHueFor
import com.cosmibit.profitness.presentation.components.coverIconFor
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaFitText
import com.cosmibit.profitness.core.ui.SinemaLayout
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
        // alt dock yüksekliği ölçülür: içerik sonu dock'un altında kalmaz (sabit tahmin yok)
        val density = LocalDensity.current
        var dockHeightPx by remember { mutableIntStateOf(0) }
        val dockPad = with(density) { dockHeightPx.toDp() }
        Box(Modifier.fillMaxSize()) {
        SinemaBackdrop(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = if (dockHeightPx > 0) dockPad + 12.dp
                    else 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
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
                                    .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24), tint = theme.accentHi)
                                    .padding(18.dp)
                            ) {
                                Text(state.error ?: "", color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    detailNow != null -> {
                        val detail = detailNow
                        val c = detail.summary
                        val isEvent = c.kind == ChallengeKind.Event
                        val ev = c.event
                        val unit = c.targetType.displayUnit(theme)

                        val ended = isChallengeEnded(c)
                        val tr = theme.t("tr", "en") == "tr"
                        val coverKey = challengeCoverKey(c)
                        val hue = coverHueFor(coverKey)

                        // ── Kapak ──
                        item {
                            ChallengeDetailHero(
                                c = c,
                                isEvent = isEvent,
                                hue = hue,
                                ghostIcon = coverIconFor(coverKey),
                                targetIcon = metricTargetIcon(c.targetType),
                                onBack = onBack,
                                menu = if (state.isOwner) {
                                    {
                                        Box {
                                            SinemaCapsuleIconButton(
                                                icon = Icons.Rounded.MoreVert,
                                                description = theme.t("Menü", "Menu"),
                                                onClick = { showOwnerMenu = true },
                                                selected = false,
                                                size = 44.dp, soft = false
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

                        if (c.description.isNotBlank()) {
                            item {
                                Text(
                                    c.description,
                                    color = Sinema.textSecondary(theme),
                                    fontSize = SinemaType.Body,
                                    lineHeight = 20.sp,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp)
                                )
                            }
                        }

                        // ── İlerleme: tek yerde ──
                        item { ChallengeProgressCard(c, unit, hue) }

                        // ── Üç istatistik bloğu ──
                        item {
                            val participantText = c.maxParticipants?.let { "${c.participantsCount} / $it" } ?: "${c.participantsCount}"
                            ChallengeStatBlocks(
                                participants = participantText,
                                start = formatShortDate(c.startDateIso, tr),
                                end = formatShortDate(c.endDateIso, tr)
                            )
                        }

                        if (isEvent && ev != null) {
                            item {
                                EventSection(
                                    ev = ev, tr = tr, hue = hue,
                                    showSkip = c.isJoined && ev.dateIso == LocalDate.now().toString(),
                                    skipOn = state.skipProgramToday,
                                    onSkip = { vm.setSkipProgramToday(!state.skipProgramToday) }
                                )
                            }
                        }

                        if (isEvent && ev?.mode == EventMode.MovementList && c.isJoined
                            && detail.movements.isNotEmpty()
                        ) {
                            item {
                                SinemaSectionLabel(
                                    theme.t("Hareketler", "Movements"), Modifier.padding(top = 16.dp),
                                    trailing = "${detail.movements.count { it.myCompleted }}/${detail.movements.size}"
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

                        val participantRows = detail.visibleParticipantRows(
                            currentUserId = state.currentUserId,
                            isOwner = state.isOwner
                        )

                        item {
                            SinemaSectionLabel(
                                theme.t("Sıralama", "Leaderboard"), Modifier.padding(top = 20.dp),
                                trailing = participantRows.size.takeIf { it > 0 }?.toString()
                            )
                        }

                        if (participantRows.isEmpty()) {
                            item { LeaderboardEmpty(canJoin = !ended && !c.isJoined) }
                        } else {
                            itemsIndexed(
                                participantRows,
                                key = { _, e -> "lb_${e.userId}" }
                            ) { index, entry ->
                                LeaderboardRowV2(rank = index + 1, entry = entry, unit = unit, target = c.targetValue, hue = hue)
                            }
                        }
                    }
                }
            }

            // Durum çubuğu altı yumuşak scrim: kayan içerik saat/simgelerle çakışmasın (dashboard ile aynı dil)
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .height(maxOf(com.cosmibit.profitness.presentation.components.LocalSafeTopInset.current, WindowInsets.statusBars.asPaddingValues().calculateTopPadding()) + 14.dp)
                    .background(Brush.verticalGradient(listOf(Sinema.Page.copy(alpha = 0.94f), Sinema.Page.copy(alpha = 0.6f), Color.Transparent)))
            )

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
                        .onSizeChanged { dockHeightPx = it.height }
                        // dock arkası scrim: üstte saydam -> sayfa rengi (gri plaka yok; kesik içerik yok)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.28f to Sinema.page(theme).copy(0.90f),
                                0.5f to Sinema.page(theme).copy(0.98f),
                                1f to Sinema.page(theme)
                            )
                        )
                        .safeBottomPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isChallengeEnded(c)) {
                        // sona erdi: soluk ama okunur nötr yükseltilmiş durum (tıklanmaz)
                        SinemaCapsule(
                            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, height = 52.dp,
                            horizontalArrangement = Arrangement.Center
                        ) { _ ->
                            Icon(Icons.Rounded.Flag, null, tint = Sinema.TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isEvent) theme.t("Etkinlik sona erdi", "Event ended") else theme.t("Challenge sona erdi", "Challenge ended"),
                                color = Sinema.TextSecondary, fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1
                            )
                        }
                    } else if (!c.isJoined) {
                        SinemaGoButton(
                            label = strings.joinLabel,
                            icon = Icons.Rounded.Bolt,
                            onClick = { if (!state.inFlight) action() },
                            dim = state.inFlight
                        )
                    } else {
                        if (canAddProgress) {
                            SinemaGoButton(
                                label = theme.t("İlerleme ekle", "Add progress"),
                                icon = Icons.Rounded.Add,
                                onClick = { if (!state.submittingProgress) showProgressDialog = true },
                                dim = state.submittingProgress
                            )
                        }
                        SinemaCapsule(
                            Modifier.fillMaxWidth(), onClick = action, enabled = !state.inFlight, height = 52.dp,
                            horizontalArrangement = Arrangement.Center
                        ) { fg ->
                            if (state.inFlight) {
                                CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(strings.leaveLabel, color = fg, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.Bold, letterSpacing = 0.sp, maxLines = 1)
                        }
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
                    confirmLabel = strings.leaveLabel,
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
internal fun EdgeToEdgeDialogBars() = FullScreenDialogWindow()

// ═══════════════════════════════════════════════════════════════════════════
//  DİYALOG YARDIMCILARI
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun SinemaCardDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(SinemaRadius.R28)).padding(24.dp),
            content = content
        )
    }
}

@Composable
private fun DialogTextAction(label: String, onClick: () -> Unit, color: Color? = null, enabled: Boolean = true) {
    val theme = LocalAppTheme.current
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).sinemaClickable(enabled = enabled, onClick = onClick, scale = 0.98f).clip(RoundedCornerShape(SinemaRadius.R24)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color ?: Sinema.textSecondary(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold)
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
        Text(strings.privateJoinHint, color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 20.sp)
        Spacer(Modifier.height(16.dp))
        SinemaGlassField(
            value = password, onValueChange = { password = it },
            label = theme.t("Şifre", "Password"), placeholder = "••••••",
            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(20.dp))
        SinemaGoOrIdle(
            label = strings.joinLabel, icon = Icons.Rounded.Bolt,
            enabled = password.isNotBlank(), onClick = { onSubmit(password) }
        )
        DialogTextAction(strings.privateJoinCancel, onCancel)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EVENT INFO CARD
// ═══════════════════════════════════════════════════════════════════════════

private fun isChallengeEnded(c: ChallengeSummary): Boolean {
    val end = runCatching { LocalDate.parse(c.endDateIso) }.getOrNull() ?: return false
    return LocalDate.now().isAfter(end)
}

private fun formatShortDate(iso: String, tr: Boolean): String = runCatching {
    LocalDate.parse(iso.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale(if (tr) "tr" else "en")))
}.getOrDefault(iso)

private fun formatEventDate(dateIso: String, timeIso: String?, tr: Boolean): String {
    val d = runCatching {
        LocalDate.parse(dateIso.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale(if (tr) "tr" else "en")))
    }.getOrDefault(dateIso)
    return if (timeIso.isNullOrBlank()) d else "$d · ${timeIso.take(5)}"
}

/** Etkinlik bilgisi: tek yükseltilmiş kart; tarih / konumlar / rota / bağlantı / program atlama satırları (çizgi yok). */
@Composable
private fun EventSection(
    ev: ChallengeEventInfo,
    tr: Boolean,
    hue: Color,
    showSkip: Boolean,
    skipOn: Boolean,
    onSkip: () -> Unit
) {
    val theme = LocalAppTheme.current
    val ctx = LocalContext.current
    val marked = theme.t("İşaretli konum", "Pinned location")
    val mapLabel = theme.t("Haritada gör", "View on map")
    Column(Modifier.fillMaxWidth()) {
    SinemaSectionLabel(theme.t("Etkinlik", "Event"), Modifier.padding(top = 20.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DetailInfoRow(Icons.Rounded.CalendarMonth, theme.accentHi, theme.t("Tarih", "Date"), formatEventDate(ev.dateIso, ev.timeIso, tr))
        when (ev.mode) {
            EventMode.Physical -> {
                val startQuery = ev.location?.trim()?.takeIf { it.isNotBlank() } ?: coordinateQuery(ev.geoLat, ev.geoLng)
                val endQuery = ev.endLocation?.trim()?.takeIf { it.isNotBlank() } ?: coordinateQuery(ev.endGeoLat, ev.endGeoLng)
                if (startQuery != null) {
                    DetailInfoRow(
                        Icons.Rounded.LocationOn, hue, theme.t("Başlangıç", "Start"),
                        ev.location?.takeIf { it.isNotBlank() } ?: marked
                    ) { DetailSoftAction(mapLabel) { openMapSearch(ctx, startQuery) } }
                }
                if (endQuery != null) {
                    DetailInfoRow(
                        Icons.Rounded.Flag, hue, theme.t("Bitiş", "Finish"),
                        ev.endLocation?.takeIf { it.isNotBlank() } ?: marked
                    ) { DetailSoftAction(mapLabel) { openMapSearch(ctx, endQuery) } }
                }
                if (startQuery != null) {
                    Text(
                        theme.t("Rotayı aç", "Open route").sinemaUpper(theme),
                        color = Sinema.textSecondary(theme), style = Sinema.Label,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RouteOption(Icons.Rounded.DirectionsBike, theme.t("Bisiklet", "Cycling"), hue, { openEventRoute(ctx, startQuery, endQuery, "bicycling") }, Modifier.weight(1f))
                        // Google Maps "running" travelmode'u kabul etmediği için walking ile açılır
                        RouteOption(Icons.Rounded.DirectionsRun, theme.t("Koşu", "Run"), hue, { openEventRoute(ctx, startQuery, endQuery, "walking") }, Modifier.weight(1f))
                        RouteOption(Icons.Rounded.DirectionsWalk, theme.t("Yürüyüş", "Walk"), hue, { openEventRoute(ctx, startQuery, endQuery, "walking") }, Modifier.weight(1f))
                    }
                }
            }
            EventMode.Online -> {
                val safeOnlineUrl = normalizeOnlineEventUrl(ev.onlineUrl)
                if (safeOnlineUrl != null) {
                    DetailInfoRow(Icons.Rounded.Link, hue, theme.t("Bağlantı", "Link"), safeOnlineUrl) {
                        DetailSoftAction(theme.t("Aç", "Open"), Icons.Rounded.OpenInNew) {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, safeOnlineUrl.toUri())) }
                        }
                    }
                } else if (!ev.onlineUrl.isNullOrBlank()) {
                    Text(
                        theme.t("Geçersiz online bağlantı", "Invalid online link"),
                        color = CardCoral, fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            EventMode.MovementList -> Unit
        }
        if (showSkip) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).sinemaClickable(onClick = onSkip, scale = 0.98f).padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) { stateDescription = if (skipOn) theme.t("Açık", "On") else theme.t("Kapalı", "Off") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconDisk(Icons.Rounded.Schedule, null, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(theme.t("Bugün programı atla", "Skip today's plan"), color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(width = 46.dp, height = 28.dp).clip(CircleShape)
                        .background(if (skipOn) theme.accentHi else Sinema.line(theme)).padding(3.dp),
                    contentAlignment = if (skipOn) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(if (skipOn) theme.effectiveOnAccentColor else Sinema.textSecondary(theme)))
                }
            }
        }
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
            // çerçeve yok: boşken gömük nötr dolgu
            .background(if (done) theme.accentHi else Sinema.line(theme)),
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
            .heightIn(min = 52.dp)
            .sinemaClickable(enabled = !pending, onClick = onToggle, scale = 0.98f)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckDot(done = done, accent = accent, size = 26.dp, pending = pending)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                theme.exerciseDisplayName(movement.exerciseName),
                color = if (done) Sinema.textSecondary(theme) else Sinema.text(theme),
                fontSize = SinemaType.Body,
                fontWeight = FontWeight.Bold,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
            )
            val sub = movementSpecLabel(movement, theme)
            if (sub.isNotEmpty()) {
                Text(sub, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption)
            }
        }
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
    // Dialog penceresi içinde DropdownMenu konumu/ofseti güvenilmez (sağ kenara yapışıyordu): kendi konumlanan Popup.
    // Sağ kenardan 16dp içeride, düğmenin 8dp altında; dışarı dokunma ve geri tuşu kapatır (focusable).
    if (!expanded) return
    val density = LocalDensity.current
    val edge = with(density) { 16.dp.roundToPx() }
    val gap = with(density) { 8.dp.roundToPx() }
    val positioner = remember(edge, gap) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize
            ): androidx.compose.ui.unit.IntOffset {
                val x = (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge)
                val below = anchorBounds.bottom + gap
                val y = if (below + popupContentSize.height <= windowSize.height) below
                        else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
                return androidx.compose.ui.unit.IntOffset(x, y)
            }
        }
    }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = positioner,
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
      Column(
        Modifier
            .padding(4.dp)
            .widthIn(min = 168.dp)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Card)
            .padding(vertical = 6.dp)
      ) {
        val theme = LocalAppTheme.current
        if (canEdit) {
            DropdownMenuItem(
                text = {
                    Text(theme.t("Düzenle", "Edit"), color = Color.White, fontSize = SinemaType.Body, fontWeight = FontWeight.Bold)
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp)) },
                onClick = onEdit,
                colors = MenuDefaults.itemColors(textColor = Color.White)
            )
        }
        DropdownMenuItem(
            text = {
                Text(theme.t("Sil", "Delete"), color = DangerText, fontSize = SinemaType.Body, fontWeight = FontWeight.Bold)
            },
            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = DangerText, modifier = Modifier.size(18.dp)) },
            onClick = onDelete,
            colors = MenuDefaults.itemColors(textColor = DangerText)
        )
      }
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
            color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        SinemaCapsule(
            Modifier.fillMaxWidth(), onClick = onConfirm, enabled = !inFlight, height = 56.dp,
horizontalArrangement = Arrangement.Center
        ) { _ ->
            if (inFlight) {
                CircularProgressIndicator(color = DangerText, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Rounded.Delete, null, tint = DangerText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.t("SİL", "DELETE"), color = DangerText, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em)
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
            color = Sinema.textSecondary(theme), fontSize = SinemaType.Body
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
                SinemaFitText(
                    "%02d:%02d".format(totalSec / 60, totalSec % 60),
                    style = Sinema.Metric, baseSp = 40f, floorSp = 26f, color = Sinema.text(theme),
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                Text(theme.t("KRONOMETRE", "STOPWATCH"), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            val elapsedMin = (swElapsedMs / 60_000L).coerceAtLeast(0L)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp,
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
                        color = if (swRunning) DangerText else fg, fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                }
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp,
                    horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp),
                    enabled = !swRunning && swElapsedMs > 0L,
                    onClick = { swAccumMs = 0L; swStartMs = 0L; swTickMs = 0L }
                ) { fg ->
                    Text(
                        theme.t("SIFIRLA", "RESET"),
                        color = if (!swRunning && swElapsedMs > 0L) fg else fg.copy(0.4f),
                        fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                }
                SinemaCapsule(
                    Modifier.weight(1f), height = 44.dp, selected = elapsedMin > 0L,
                    horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp),
                    enabled = elapsedMin > 0L,
                    onClick = { text = elapsedMin.toString() }
                ) { fg ->
                    Text(theme.t("AKTAR", "APPLY"), color = if (elapsedMin > 0L) fg else fg.copy(0.4f), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "Kronometre durduğunda 'Aktar' tuşuyla geçen dakikayı yukarıdaki alana yazabilirsin.",
                    "When the stopwatch is stopped, use Apply to copy elapsed minutes into the field above."
                ),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, lineHeight = 16.sp
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
        Box(Modifier.fillMaxSize()) {
        SinemaBackdrop(Modifier.fillMaxSize())
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
                    Modifier.fillMaxWidth(), selected = type == targetType, neutralSelected = true, onClick = { onTargetType(type) },
                    height = 52.dp
                ) { fg ->
                    Icon(metricTargetIcon(type), null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(type.displayLabel(theme), color = fg, fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(type.displayUnit(theme), color = fg.copy(0.7f), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold)
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
        Box(Modifier.fillMaxSize()) {
        SinemaBackdrop(Modifier.fillMaxSize())
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

internal fun metricTargetIcon(type: ChallengeTargetType): ImageVector = when (type) {
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
