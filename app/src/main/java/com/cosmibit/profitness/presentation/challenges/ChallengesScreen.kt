@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmibit.profitness.presentation.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import com.cosmibit.profitness.core.theme.text2
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaBackdrop
import com.cosmibit.profitness.core.theme.SinemaChip
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.accent2Color
import com.cosmibit.profitness.core.theme.accent3Color
import com.cosmibit.profitness.core.theme.accent3TextColor
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaGlow
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.workout.SceneImage
import androidx.compose.material3.ModalBottomSheet
import com.cosmibit.profitness.domain.challenges.ChallengeKind
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.ChallengeTargetType
import com.cosmibit.profitness.domain.challenges.ChallengeVisibility
import com.cosmibit.profitness.domain.challenges.EventMode
import com.cosmibit.profitness.domain.discover.DiscoverSort
import com.cosmibit.profitness.domain.social.UserSummary
import com.cosmibit.profitness.presentation.workout.SportType

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
    sort: DiscoverSort = DiscoverSort.NEWEST,
    timerExtraPad: Dp = 0.dp
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val vm: ChallengesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var kindFilter by rememberSaveable { mutableStateOf(ChallengeKindFilter.All) }
    var sportFilterRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var targetFilterRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var mineStatusFilter by rememberSaveable { mutableStateOf(MyChallengeStatusFilter.All) }
    var leaveTarget by remember { mutableStateOf<ChallengeSummary?>(null) }

    Box(Modifier.fillMaxSize()) {
        // Zemin: DashboardScreen kökündeki tek SinemaBackdrop (burada ikinci backdrop = çift ışık + ~270px dikiş)
        val pendingInvites = remember(state.browseList) {
            state.browseList
                .filter { it.isInvited && !it.isJoined }
                .sortedFor(DiscoverSort.NEWEST)
        }

        Column(Modifier.fillMaxSize()) {

            // ── Tek satır: Keşfet / Katıldıklarım + filtre ikonu ──────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .sinemaCapsuleGlass(RoundedCornerShape(22.dp), blur = 16.dp)
                        .padding(3.dp)
                ) {
                    ScopeChip(
                        label    = theme.t("KEŞFET", "DISCOVER"),
                        icon     = Icons.Rounded.Public,
                        isActive = state.scope == ChallengesScope.Browse,
                        accent   = accent,
                        onClick  = { vm.selectScope(ChallengesScope.Browse) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(3.dp))
                    ScopeChip(
                        label    = theme.t("KATILDIKLARIM", "JOINED"),
                        icon     = Icons.Rounded.EmojiEvents,
                        isActive = state.scope == ChallengesScope.Mine,
                        accent   = accent,
                        onClick  = { vm.selectScope(ChallengesScope.Mine) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(10.dp))
                ChallengeFilterBar(
                    scope = state.scope,
                    kindFilter = kindFilter,
                    onKindFilter = { kindFilter = it },
                    sportFilterRaw = sportFilterRaw,
                    onSportFilter = { sportFilterRaw = it },
                    targetFilterRaw = targetFilterRaw,
                    onTargetFilter = { targetFilterRaw = it },
                    mineStatusFilter = mineStatusFilter,
                    onMineStatusFilter = { mineStatusFilter = it },
                    accent = accent
                )
            }

            // ── Hata banner ───────────────────────────────────────────────
            state.error?.let { err ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .sinemaCard(theme, RoundedCornerShape(20.dp))
                        .clickable { vm.clearError() }
                        .padding(14.dp)
                ) {
                    Text(err, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.scope == ChallengesScope.Browse && pendingInvites.isNotEmpty()) {
                ChallengeInvitesSection(
                    invites = pendingInvites,
                    accent = accent,
                    inFlightIds = state.joinInFlight,
                    onOpen = { vm.openDetail(it.id) },
                    onAccept = { challenge -> vm.toggleJoin(challenge) }
                )
            }

            // ── Liste ─────────────────────────────────────────────────────
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    else -> {
                        val list = remember(
                            state.scope,
                            state.browseList,
                            state.myList,
                            sort,
                            kindFilter,
                            sportFilterRaw,
                            targetFilterRaw,
                            mineStatusFilter
                        ) {
                            val base = if (state.scope == ChallengesScope.Browse) state.browseList else state.myList
                            val visibleBase = if (state.scope == ChallengesScope.Browse) {
                                base.filterNot { it.isInvited && !it.isJoined }
                            } else {
                                base
                            }
                            visibleBase
                                .filteredFor(state.scope, kindFilter, sportFilterRaw, targetFilterRaw, mineStatusFilter)
                                .sortedFor(DiscoverSort.NEWEST) // shortcut: Yeni/Trend menüsü Challenge listesini etkilemez
                        }
                        if (list.isEmpty()) {
                            EmptyBlock(
                                text = if (state.scope == ChallengesScope.Browse)
                                    theme.t(
                                        "Bu filtrelere uygun aktif challenge yok.",
                                        "No active challenges match these filters."
                                    )
                                else theme.t(
                                    "Bu filtrelere uygun katıldığın challenge yok.",
                                    "No joined challenges match these filters."
                                ),
                                actionLabel = theme.t("CHALLENGE OLUŞTUR", "CREATE CHALLENGE"),
                                onAction = { vm.openCreate() },
                                theme = theme
                            )
                        } else {
                            // Katıldığın ve süren challenge varsa üstte büyük öne çıkan kart
                            val featured = list.firstOrNull {
                                it.isJoined && !it.isCompleted && statusOf(it.startDateIso, it.endDateIso) == CardStatus.Live
                            }
                            val ordered = if (featured != null) listOf(featured) + list.filter { it.id != featured.id } else list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start  = 16.dp,
                                    end    = 16.dp,
                                    top    = 4.dp,
                                    bottom = bottomPadding + 88.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(ordered, key = { "${state.scope}_${it.id}" }) { c ->
                                    ChallengeCard(
                                        c          = c,
                                        accent     = accent,
                                        inFlight   = state.joinInFlight.contains(c.id),
                                        featured   = c.id == featured?.id,
                                        onTap      = { vm.openDetail(c.id) },
                                        onToggle   = {
                                            // Private + not joined → detail'e git (password dialog orada)
                                            if (c.isJoined) {
                                                leaveTarget = c
                                            } else if (!c.isJoined && c.visibility == com.cosmibit.profitness.domain.challenges.ChallengeVisibility.Private && !c.isInvited) {
                                                vm.openDetail(c.id)
                                            } else {
                                                vm.toggleJoin(c)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        leaveTarget?.let { c ->
            val isPrivate = c.visibility == com.cosmibit.profitness.domain.challenges.ChallengeVisibility.Private
            com.cosmibit.profitness.presentation.components.SinemaDialog(
                onDismiss = { leaveTarget = null },
                title = theme.t("Ayrılmak istiyor musun?", "Leave this challenge?"),
                body = if (isPrivate) theme.t(
                    "Bu challenge özel. Ayrılırsan şifre olmadan geri giremezsin.",
                    "This challenge is private. If you leave, you can't rejoin without the password."
                ) else theme.t(
                    "Ayrılırsan ilerlemen listende görünmez.",
                    "If you leave, it will no longer appear in your joined list."
                ),
                confirmLabel = theme.t("AYRIL", "LEAVE"),
                confirmIcon = Icons.Rounded.Close,
                onConfirm = { val t = c; leaveTarget = null; vm.toggleJoin(t) },
                dismissLabel = theme.t("Vazgeç", "Cancel")
            )
        }

        // ── Create FAB ─────────────────────────────────────────────────
        val fabAccent = theme.effectiveAccentColor
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding + 16.dp)
                .size(56.dp)
                .sinemaGlow(fabAccent.copy(0.4f), 20.dp, 8.dp, CircleShape)
                .clip(CircleShape)
                .background(fabAccent)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = { vm.openCreate() }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = theme.t("Yeni challenge", "New challenge"),
                tint = theme.effectiveOnAccentColor,
                modifier = Modifier.size(26.dp)
            )
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
                onFormChanged = { vm.clearCreateError() },
                onSubmit  = { title, desc, tt, tv, sd, ed, vis, pw, maxParticipants ->
                    vm.submitCreate(title, desc, tt, tv, sd, ed, vis, pw, maxParticipants)
                },
                onSubmitEvent = { req -> vm.submitCreateEvent(req) }
            )
        }

        state.pendingInviteChallengeId?.let {
            InviteFriendsDialog(
                title = state.pendingInviteTitle,
                friends = state.inviteFriends,
                selected = state.inviteSelected,
                loading = state.inviteLoading,
                inFlight = state.inviteInFlight,
                message = state.inviteMessage,
                onToggle = vm::toggleInviteFriend,
                onSend = vm::sendChallengeInvites,
                onSkip = { vm.clearInviteSheet(openDetailAfter = true) }
            )
        }
    }
}

@Composable
private fun InviteFriendsDialog(
    title: String,
    friends: List<UserSummary>,
    selected: Set<String>,
    loading: Boolean,
    inFlight: Boolean,
    message: String?,
    onToggle: (String) -> Unit,
    onSend: () -> Unit,
    onSkip: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = 560.dp)
                    .padding(horizontal = 16.dp)
                    .sinemaCard(theme, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Sinema.line(theme)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(theme.t("ARKADAŞLARINI DAVET ET", "INVITE FRIENDS"), color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text(title.ifBlank { theme.t("Yeni challenge", "New challenge") }, color = theme.text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        tint = theme.text2,
                        modifier = Modifier.size(22.dp).clickable(enabled = !inFlight) { onSkip() }
                    )
                }

                Spacer(Modifier.height(14.dp))

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    friends.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Sinema.line(theme)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            theme.t(
                                "Davet göndermek için karşılıklı takip ettiğin arkadaşların olmalı.",
                                "You need mutual friends to send invites."
                            ),
                            color = theme.text2,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(friends, key = { it.userId }) { friend ->
                            InviteFriendRow(
                                user = friend,
                                selected = friend.userId in selected,
                                enabled = !inFlight,
                                onClick = { onToggle(friend.userId) }
                            )
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = theme.text2, fontSize = 11.sp)
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SinemaChip(active = false, onClick = onSkip, enabled = !inFlight, height = 48.dp, modifier = Modifier.weight(1f)) { fg ->
                        Text(theme.t("GEÇ", "SKIP"), color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    SinemaChip(active = selected.isNotEmpty(), onClick = onSend, enabled = selected.isNotEmpty() && !inFlight, height = 48.dp, modifier = Modifier.weight(1.4f)) { fg ->
                        if (inFlight) {
                            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Rounded.Send, null, tint = fg, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selected.isEmpty()) theme.t("DAVET GÖNDER", "SEND INVITE")
                                else theme.t("${selected.size} DAVET GÖNDER", "SEND ${selected.size} INVITES"),
                                color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteFriendRow(
    user: UserSummary,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .then(if (selected) Modifier.border(1.5.dp, accent, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(theme.bg1), contentAlignment = Alignment.Center) {
            if (user.avatarUrl != null) {
                AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Text(user.displayName.take(1).uppercase(), color = theme.text1, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            user.username?.let { Text("@$it", color = theme.text2, fontSize = 11.sp, maxLines = 1) }
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(1.dp, if (selected) accent else Sinema.line(theme), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(16.dp))
        }
    }
}

private enum class ChallengeKindFilter(val trLabel: String, val enLabel: String) {
    All("TÜMÜ", "ALL"),
    Metric("METRİK", "METRIC"),
    Event("ETKİNLİK", "EVENT"),
    Movement("HAREKET", "MOVEMENT")
}

private enum class MyChallengeStatusFilter(val trLabel: String, val enLabel: String) {
    All("TÜMÜ", "ALL"),
    Active("DEVAM", "ACTIVE"),
    Completed("TAMAM", "DONE"),
    Ended("BİTEN", "ENDED")
}

private fun ChallengeKindFilter.label(theme: com.cosmibit.profitness.core.theme.AppThemeState): String =
    theme.t(trLabel, enLabel)

private fun MyChallengeStatusFilter.label(theme: com.cosmibit.profitness.core.theme.AppThemeState): String =
    theme.t(trLabel, enLabel)

@Composable
private fun ChallengeInvitesSection(
    invites: List<ChallengeSummary>,
    accent: Color,
    inFlightIds: Set<String>,
    onOpen: (ChallengeSummary) -> Unit,
    onAccept: (ChallengeSummary) -> Unit
) {
    val theme = LocalAppTheme.current
    var showAll by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .sinemaCapsuleGlass(CircleShape, blur = 16.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = theme.accent3TextColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    theme.t("DAVETLER", "INVITES"),
                    color = theme.accent3TextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                theme.t("${invites.size} bekleyen challenge", "${invites.size} pending challenges"),
                color = theme.text2,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        (if (showAll) invites else invites.take(3)).forEach { challenge ->
            ChallengeInviteRow(
                challenge = challenge,
                accent = accent,
                inFlight = challenge.id in inFlightIds,
                onOpen = { onOpen(challenge) },
                onAccept = { onAccept(challenge) }
            )
            Spacer(Modifier.height(8.dp))
        }
        if (invites.size > 3) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable { showAll = !showAll },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (showAll) theme.t("Daha az göster", "Show less") else theme.t("Tümünü gör (${invites.size})", "See all (${invites.size})"),
                    color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ChallengeInviteRow(
    challenge: ChallengeSummary,
    accent: Color,
    inFlight: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit
) {
    val theme = LocalAppTheme.current
    val status = statusOf(challenge.startDateIso, challenge.endDateIso)
    val statusColor = challengeStatusAccent(
        status = status,
        completed = challenge.isCompleted,
        accent = theme.accent2Color,
        trophy = theme.accent3Color
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Sinema.line(theme)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (challenge.kind == ChallengeKind.Event) Icons.Rounded.Event else Icons.Rounded.EmojiEvents,
                null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                challenge.title,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                theme.t(
                    "${challenge.creatorName} davet etti · ${challenge.targetValue} ${challenge.targetType.displayUnit(theme)}",
                    "${challenge.creatorName} invited you · ${challenge.targetValue} ${challenge.targetType.displayUnit(theme)}"
                ),
                color = theme.text2,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.14f))
                .clickable(enabled = !inFlight && status != CardStatus.Ended && !challenge.isCompleted) { onAccept() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (inFlight) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    theme.t("KATIL", "JOIN"),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp
                )
            }
        }
    }
}

@Composable
private fun ChallengeFilterBar(
    scope: ChallengesScope,
    kindFilter: ChallengeKindFilter,
    onKindFilter: (ChallengeKindFilter) -> Unit,
    sportFilterRaw: String?,
    onSportFilter: (String?) -> Unit,
    targetFilterRaw: String?,
    onTargetFilter: (String?) -> Unit,
    mineStatusFilter: MyChallengeStatusFilter,
    onMineStatusFilter: (MyChallengeStatusFilter) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    var showFilters by remember { mutableStateOf(false) }
    val activeFilterCount = listOf(
        kindFilter != ChallengeKindFilter.All,
        sportFilterRaw != null,
        targetFilterRaw != null,
        scope == ChallengesScope.Mine && mineStatusFilter != MyChallengeStatusFilter.All
    ).count { it }

    SinemaCapsuleIconButton(
        icon = Icons.Rounded.Tune,
        description = if (activeFilterCount > 0) theme.t("Filtreler · $activeFilterCount", "Filters · $activeFilterCount") else theme.t("Filtreler", "Filters"),
        onClick = { showFilters = true },
        selected = activeFilterCount > 0,
        size = 44.dp
    )

    if (showFilters) {
        ChallengeFiltersDialog(
            scope = scope,
            kindFilter = kindFilter,
            sportFilterRaw = sportFilterRaw,
            targetFilterRaw = targetFilterRaw,
            mineStatusFilter = mineStatusFilter,
            accent = accent,
            onKindFilter = onKindFilter,
            onSportFilter = onSportFilter,
            onTargetFilter = onTargetFilter,
            onMineStatusFilter = onMineStatusFilter,
            onDismiss = { showFilters = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengeFiltersDialog(
    scope: ChallengesScope,
    kindFilter: ChallengeKindFilter,
    sportFilterRaw: String?,
    targetFilterRaw: String?,
    mineStatusFilter: MyChallengeStatusFilter,
    accent: Color,
    onKindFilter: (ChallengeKindFilter) -> Unit,
    onSportFilter: (String?) -> Unit,
    onTargetFilter: (String?) -> Unit,
    onMineStatusFilter: (MyChallengeStatusFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Sinema.surface(theme), contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(theme.t("Challenge filtreleri", "Challenge filters"), style = MaterialTheme.typography.headlineSmall, color = theme.text0, modifier = Modifier.weight(1f))
                if (kindFilter != ChallengeKindFilter.All || sportFilterRaw != null || targetFilterRaw != null || mineStatusFilter != MyChallengeStatusFilter.All) {
                    TextButton(onClick = {
                        onKindFilter(ChallengeKindFilter.All); onSportFilter(null); onTargetFilter(null); onMineStatusFilter(MyChallengeStatusFilter.All)
                    }) { Text(theme.t("Sıfırla", "Reset"), color = theme.text1) }
                }
            }
            Text(theme.t("Tür", "Type"), style = MaterialTheme.typography.labelMedium, color = theme.text1)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeKindFilter.entries.forEach { value -> FilterChip(value.label(theme), value == kindFilter, accent) { onKindFilter(value) } }
            }
            Spacer(Modifier.height(18.dp))
            Text(theme.t("Aktivite", "Activity"), style = MaterialTheme.typography.labelMedium, color = theme.text1)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(theme.t("Tümü", "All"), sportFilterRaw == null, accent) { onSportFilter(null) }
                SportType.challengeChoices.forEach { value -> FilterChip(value.displayLabel(theme), value.raw == sportFilterRaw, accent) { onSportFilter(value.raw) } }
            }
            Spacer(Modifier.height(18.dp))
            Text(theme.t("Metrik", "Metric"), style = MaterialTheme.typography.labelMedium, color = theme.text1)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(theme.t("Tümü", "All"), targetFilterRaw == null, accent) { onTargetFilter(null) }
                ChallengeTargetType.entries.forEach { value -> FilterChip(value.displayLabel(theme), value.raw == targetFilterRaw, accent) { onTargetFilter(value.raw) } }
            }
            if (scope == ChallengesScope.Mine) {
                Spacer(Modifier.height(18.dp))
                Text(theme.t("Durum", "Status"), style = MaterialTheme.typography.labelMedium, color = theme.text1)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MyChallengeStatusFilter.entries.forEach { value -> FilterChip(value.label(theme), value == mineStatusFilter, accent) { onMineStatusFilter(value) } }
                }
            }
            Spacer(Modifier.height(20.dp))
            SinemaGoButton(label = theme.t("SONUÇLARI GÖSTER", "SHOW RESULTS"), icon = Icons.Rounded.Check, onClick = onDismiss)
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    // c1 ekranda tek dolgu (FAB); ikincil seçili taş nötr cam
    val shape = RoundedCornerShape(17.dp)
    Box(
        Modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(Color.White.copy(if (active) 0.22f else 0.08f))
            .clickable(role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) Color.White else Color.White.copy(0.75f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, maxLines = 1)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Scope chip
// ═══════════════════════════════════════════════════════════════════════════

private fun List<ChallengeSummary>.filteredFor(
    scope: ChallengesScope,
    kindFilter: ChallengeKindFilter,
    sportFilterRaw: String?,
    targetFilterRaw: String?,
    mineStatusFilter: MyChallengeStatusFilter
): List<ChallengeSummary> = filter { c ->
    val kindMatches = when (kindFilter) {
        ChallengeKindFilter.All -> true
        ChallengeKindFilter.Metric -> c.kind == ChallengeKind.Metric
        ChallengeKindFilter.Event -> c.kind == ChallengeKind.Event
        ChallengeKindFilter.Movement -> c.event?.mode == EventMode.MovementList
    }
    val sportMatches = sportFilterRaw == null || c.event?.sportType?.raw == sportFilterRaw
    val targetMatches = targetFilterRaw == null || c.targetType.raw == targetFilterRaw
    val statusMatches = scope != ChallengesScope.Mine || when (mineStatusFilter) {
        MyChallengeStatusFilter.All -> true
        MyChallengeStatusFilter.Active -> !c.isCompleted && statusOf(c.startDateIso, c.endDateIso) != CardStatus.Ended
        MyChallengeStatusFilter.Completed -> c.isCompleted
        MyChallengeStatusFilter.Ended -> !c.isCompleted && statusOf(c.startDateIso, c.endDateIso) == CardStatus.Ended
    }
    kindMatches && sportMatches && targetMatches && statusMatches
}

private fun List<ChallengeSummary>.sortedFor(sort: DiscoverSort): List<ChallengeSummary> =
    when (sort) {
        DiscoverSort.NEWEST -> sortedWith(
            compareByDescending<ChallengeSummary> { it.createdAtIso.orEmpty() }
                .thenByDescending { it.startDateIso }
                .thenByDescending { it.participantsCount }
        )
        DiscoverSort.TRENDING -> sortedWith(
            compareByDescending<ChallengeSummary> { it.participantsCount }
                .thenByDescending { it.createdAtIso.orEmpty() }
                .thenByDescending { it.startDateIso }
        )
    }

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
    val shape = RoundedCornerShape(19.dp)
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .then(if (isActive) Modifier.background(Color.White.copy(0.22f)) else Modifier)
            .clickable(role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        val fg = if (isActive) Color.White else Color.White.copy(0.75f)
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
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

private fun challengeStatusAccent(
    status: CardStatus,
    completed: Boolean,
    accent: Color,
    trophy: Color
): Color = when {
    completed -> trophy
    status == CardStatus.Ended -> Color(0xFF9CA3AF)
    status == CardStatus.NotStarted -> Color.White.copy(0.6f)
    else -> accent
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

private val challengeNumberFormat: java.text.NumberFormat
    get() = java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr"))

/** Tür -> mevcut kategori foto kaynağı (categoryImageUrl anahtar kelimeleri). */
private fun challengePhotoCategory(c: ChallengeSummary): String {
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

/** Fotoğraflı poster kart: foto + alt karartma, tek durum etiketi, büyük hedef rakamı, ilerleme, KATIL/KAPALI. */
@Composable
private fun ChallengeCard(
    c: ChallengeSummary,
    accent: Color,
    inFlight: Boolean,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false
) {
    val theme = LocalAppTheme.current
    val isPrivate = c.visibility == ChallengeVisibility.Private
    val isEvent = c.kind == ChallengeKind.Event
    val status = statusOf(c.startDateIso, c.endDateIso)
    val today = java.time.LocalDate.now().toString()
    val daysLeft = daysBetween(today, c.endDateIso) ?: 0L
    val daysUntilStart = daysBetween(today, c.startDateIso) ?: 0L
    val isFull = c.maxParticipants?.let { c.participantsCount >= it && !c.isJoined } == true
    val canAct = !c.isCompleted && status != CardStatus.Ended && !isFull
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "challenge_card_press"
    )
    val unit = c.targetType.displayUnit(theme)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap)
    ) {
        SceneImage(
            url = categoryImageUrl(challengePhotoCategory(c)),
            modifier = Modifier.matchParentSize(),
            requestWidthPx = 960, requestHeightPx = 960, fallbackGradient = true
        )
        Box(Modifier.matchParentSize().background(Sinema.BottomScrim))
        Column(
            Modifier.fillMaxWidth().heightIn(min = if (featured) 440.dp else 330.dp).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Üst: TEK durum etiketi + katılımcı ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                PosterStatusLabel(
                    text = when {
                        c.isCompleted -> theme.t("TAMAMLANDI", "COMPLETED")
                        status == CardStatus.NotStarted -> theme.t("YAKINDA", "SOON")
                        status == CardStatus.Live -> theme.t("DEVAM EDİYOR", "LIVE")
                        else -> theme.t("SONA ERDİ", "ENDED")
                    },
                    dot = if (c.isCompleted) theme.accent3Color else if (status == CardStatus.Live) theme.accent2Color else Color.White.copy(0.5f)
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.People, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    c.maxParticipants?.let { "${c.participantsCount}/$it" } ?: "${c.participantsCount}",
                    color = Color.White.copy(0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }

            // ── Alt: başlık, büyük hedef, ilerleme, CTA ──
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPrivate) {
                        Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        c.title,
                        style = Sinema.Title.copy(fontSize = if (featured) 28.sp else 24.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        challengeNumberFormat.format(c.targetValue),
                        style = Sinema.Display.copy(fontSize = if (featured) 72.sp else 56.sp),
                        color = Color.White, maxLines = 1
                    )
                    Text(
                        unit, style = Sinema.Title.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
                val info = when {
                    isEvent && c.event != null ->
                        humanDate(c.event.dateIso) + (c.event.timeIso?.let { " · ${it.take(5)}" } ?: "") +
                            (c.event.location?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
                    c.isCompleted -> theme.t("Tamamlandı", "Completed")
                    status == CardStatus.NotStarted ->
                        if (daysUntilStart == 0L) theme.t("Bugün başlıyor", "Starts today")
                        else theme.t("$daysUntilStart gün sonra", "In $daysUntilStart days")
                    status == CardStatus.Ended -> theme.t("Sona erdi", "Ended")
                    daysLeft == 0L -> theme.t("Son gün", "Last day")
                    else -> theme.t("$daysLeft gün kaldı", "$daysLeft days left")
                }
                Text(
                    "${c.targetType.displayLabel(theme)} · $info".sinemaUpper(theme),
                    style = Sinema.Label, color = Color.White.copy(0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                if (c.isJoined) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${challengeNumberFormat.format(c.myProgress)} / ${challengeNumberFormat.format(c.targetValue)} $unit",
                            color = Color.White.copy(0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text("%${(c.progressPct * 100).toInt()}", color = theme.accent2Color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(pct = c.progressPct, accent = theme.accent2Color, strokeColor = Color.Transparent)
                }

                Spacer(Modifier.height(14.dp))

                when {
                    !canAct -> Row(
                        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(0.14f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(0.55f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                isFull -> theme.t("DOLU", "FULL")
                                c.isCompleted -> theme.t("TAMAMLANDI", "COMPLETED")
                                else -> theme.t("KAPALI", "CLOSED")
                            },
                            color = Color.White.copy(0.55f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em
                        )
                    }
                    c.isJoined -> SinemaCapsule(
                        Modifier.fillMaxWidth(), onClick = onToggle, enabled = !inFlight, height = 56.dp,
                        horizontalArrangement = Arrangement.Center
                    ) { fg ->
                        if (inFlight) {
                            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(theme.t("KATILDIN", "JOINED"), color = fg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em)
                    }
                    // c1 ekranda tek dolgu (FAB): kart eylemi nötr cam + c1 ikon/metin
                    else -> SinemaCapsule(
                        Modifier.fillMaxWidth(), onClick = { if (!inFlight) onToggle() }, enabled = !inFlight, height = 56.dp,
                        horizontalArrangement = Arrangement.Center
                    ) { _ ->
                        Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (c.isInvited) theme.t("DAVET", "INVITE") else theme.t("KATIL", "JOIN"),
                            color = accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em
                        )
                    }
                }
            }
        }
    }
}

/** Posterdeki tek durum etiketi: cam kapsül, nokta + metin. */
@Composable
private fun PosterStatusLabel(text: String, dot: Color) {
    Row(
        modifier = Modifier.sinemaCapsuleGlass(CircleShape, blur = 16.dp).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable
private fun ProgressBar(pct: Float, accent: Color, strokeColor: Color) {
    val shape = RoundedCornerShape(4.dp)
    val frac = pct.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(8.dp)) {
        Box(Modifier.matchParentSize().clip(shape).background(Color.White.copy(0.18f)))
        if (frac > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .sinemaGlow(accent.copy(0.4f), 10.dp, 3.dp, shape)
                    .clip(shape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun EmptyBlock(
    text: String,
    theme: com.cosmibit.profitness.core.theme.AppThemeState,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(72.dp).sinemaCapsuleGlass(CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = theme.accent3Color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp, textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            // c1 ekranda tek dolgu (FAB) → CTA nötr cam + c1 metin
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.14f))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAction)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Add, null, tint = theme.effectiveAccentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel, color = theme.effectiveAccentColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            }
        }
    }
}
