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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.bg0
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.bg2
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import com.cosmibit.profitness.core.theme.text2
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.performanceElevatedSurface
import com.cosmibit.profitness.domain.challenges.ChallengeKind
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.ChallengeTargetType
import com.cosmibit.profitness.domain.challenges.ChallengeVisibility
import com.cosmibit.profitness.domain.challenges.EventMode
import com.cosmibit.profitness.domain.discover.DiscoverSort
import com.cosmibit.profitness.domain.social.UserSummary
import com.cosmibit.profitness.presentation.workout.SportType
import com.cosmibit.profitness.presentation.components.PremiumIconButton
import com.cosmibit.profitness.presentation.components.premiumSolidSurface
import com.cosmibit.profitness.presentation.components.insetControlSurface

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
    val accent = MaterialTheme.colorScheme.primary
    val vm: ChallengesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var kindFilter by rememberSaveable { mutableStateOf(ChallengeKindFilter.All) }
    var sportFilterRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var targetFilterRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var mineStatusFilter by rememberSaveable { mutableStateOf(MyChallengeStatusFilter.All) }

    Box(Modifier.fillMaxSize()) {
        val pendingInvites = remember(state.browseList) {
            state.browseList
                .filter { it.isInvited && !it.isJoined }
                .sortedFor(DiscoverSort.NEWEST)
        }

        Column(Modifier.fillMaxSize()) {

            // ── Scope switcher ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .premiumSolidSurface(accent, theme, RoundedCornerShape(14.dp), elevation = 6.dp)
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
                    label    = theme.t("KATILDIKLARIM (${state.myList.size})", "JOINED (${state.myList.size})"),
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

            if (state.scope == ChallengesScope.Browse && pendingInvites.isNotEmpty()) {
                ChallengeInvitesSection(
                    invites = pendingInvites,
                    accent = accent,
                    inFlightIds = state.joinInFlight,
                    onOpen = { vm.openDetail(it.id) },
                    onAccept = { challenge -> vm.toggleJoin(challenge) }
                )
            }

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
                                .sortedFor(sort)
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
                                            if (!c.isJoined && c.visibility == com.cosmibit.profitness.domain.challenges.ChallengeVisibility.Private && !c.isInvited) {
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
            }
        }

        // ── Create FAB ─────────────────────────────────────────────────
        PremiumIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = theme.t("Yeni challenge", "New challenge"),
            onClick = { vm.openCreate() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = bottomPadding + 12.dp)
                .size(54.dp),
            shape = CircleShape
        )

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
                    .premiumSolidSurface(accent, theme, RoundedCornerShape(24.dp), elevation = 20.dp)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(0.16f)),
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.bg2.copy(0.55f)),
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.bg2)
                            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(14.dp))
                            .clickable(enabled = !inFlight) { onSkip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(theme.t("GEÇ", "SKIP"), color = theme.text1, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected.isEmpty()) accent.copy(0.35f) else accent)
                            .clickable(enabled = selected.isNotEmpty() && !inFlight) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (inFlight) {
                            CircularProgressIndicator(color = theme.effectiveOnAccentColor, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Send, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (selected.isEmpty()) {
                                        theme.t("DAVET GÖNDER", "SEND INVITE")
                                    } else {
                                        theme.t("${selected.size} DAVET GÖNDER", "SEND ${selected.size} INVITES")
                                    },
                                    color = theme.effectiveOnAccentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
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
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) Modifier.insetControlSurface(accent, theme, RoundedCornerShape(16.dp))
                else Modifier.background(theme.bg2)
            )
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
                .border(1.dp, if (selected) accent else theme.stroke, CircleShape),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(0.14f))
                    .border(1.dp, accent.copy(0.36f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    theme.t("DAVETLER", "INVITES"),
                    color = accent,
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

        invites.take(3).forEach { challenge ->
            ChallengeInviteRow(
                challenge = challenge,
                accent = accent,
                inFlight = challenge.id in inFlightIds,
                onOpen = { onOpen(challenge) },
                onAccept = { onAccept(challenge) }
            )
            Spacer(Modifier.height(8.dp))
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
        accent = accent
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1.copy(0.78f))
            .border(1.dp, statusColor.copy(0.34f), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(statusColor.copy(0.16f))
                .border(1.dp, statusColor.copy(0.28f), RoundedCornerShape(14.dp)),
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
                .height(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent)
                .clickable(enabled = !inFlight && status != CardStatus.Ended && !challenge.isCompleted) { onAccept() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (inFlight) {
                CircularProgressIndicator(color = theme.effectiveOnAccentColor, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    theme.t("KATIL", "JOIN"),
                    color = theme.effectiveOnAccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val hasSecondaryFilter = kindFilter != ChallengeKindFilter.All || sportFilterRaw != null || targetFilterRaw != null
    val activeFilterCount = listOf(
        kindFilter != ChallengeKindFilter.All,
        sportFilterRaw != null,
        targetFilterRaw != null,
        scope == ChallengesScope.Mine && mineStatusFilter != MyChallengeStatusFilter.All
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(theme.bg1)
                    .clickable { showFilters = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Tune, null, tint = if (activeFilterCount > 0) accent else theme.text1, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (activeFilterCount > 0) theme.t("Filtreler · $activeFilterCount", "Filters · $activeFilterCount") else theme.t("Filtreler", "Filters"),
                    color = theme.text0,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (hasSecondaryFilter || mineStatusFilter != MyChallengeStatusFilter.All) {
                TextButton(onClick = {
                    onKindFilter(ChallengeKindFilter.All); onSportFilter(null); onTargetFilter(null); onMineStatusFilter(MyChallengeStatusFilter.All)
                }) { Text(theme.t("Sıfırla", "Reset"), color = theme.text1) }
            }
        }
    }

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
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(theme.bg1).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(theme.t("Challenge filtreleri", "Challenge filters"), style = MaterialTheme.typography.headlineSmall, color = theme.text0, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = theme.text1) }
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
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(theme.t("Sonuçları göster", "Show results")) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPickerDialog(
    title: String,
    options: List<Pair<String?, String>>,
    selectedRaw: String?,
    accent: Color,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke.copy(0.45f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = theme.text0,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.Close,
                    null,
                    tint = theme.text2,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss)
                )
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (raw, label) ->
                    FilterChip(
                        label = label,
                        active = raw == selectedRaw,
                        accent = accent,
                        onClick = { onSelect(raw) }
                    )
                }
            }
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
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) accent.copy(0.22f) else theme.bg1.copy(0.62f))
            .border(
                1.dp,
                if (active) accent.copy(0.56f) else theme.stroke.copy(0.42f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) accent else theme.text2,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
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

private fun challengeStatusAccent(
    status: CardStatus,
    completed: Boolean,
    accent: Color
): Color = when {
    completed -> Color(0xFF38BDF8)
    status == CardStatus.Ended -> Color(0xFF9CA3AF)
    status == CardStatus.NotStarted -> Color(0xFFFFB74D)
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
    val isEnded = status == CardStatus.Ended && !c.isCompleted
    val cardAccent = challengeStatusAccent(status, c.isCompleted, accent)
    val isFull = c.maxParticipants?.let { c.participantsCount >= it && !c.isJoined } == true
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "challenge_card_press"
    )

    val borderColor = when {
        c.isCompleted -> cardAccent.copy(0.78f)
        isEnded -> cardAccent.copy(0.45f)
        c.isJoined    -> accent.copy(0.55f)
        else -> theme.stroke.copy(0.5f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .performanceElevatedSurface(theme, RoundedCornerShape(22.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                // ── Header chips + status ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KindBadge(isEvent = isEvent, accent = accent, theme = theme)
                    if (c.isInvited && !c.isJoined) {
                        Spacer(Modifier.width(6.dp))
                        InviteBadge(accent = accent, theme = theme)
                    }
                    if (isEvent && c.event != null) {
                        Spacer(Modifier.width(6.dp))
                        EventModeBadge(mode = c.event.mode, theme = theme)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        status = if (c.isCompleted) CardStatus.Ended else status,
                        completed = c.isCompleted,
                        accent = cardAccent,
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
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardAccent.copy(0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            targetIcon(c.targetType),
                            null,
                            tint = cardAccent,
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
                            c.targetType.displayUnit(theme),
                            color = cardAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.t("HEDEF", "TARGET"),
                            color = theme.text2,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            c.targetType.displayLabel(theme),
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
                        Icon(Icons.Rounded.CalendarMonth, null, tint = cardAccent, modifier = Modifier.size(13.dp))
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
                            "${c.myProgress} / ${c.targetValue} ${c.targetType.displayUnit(theme)}",
                            color    = theme.text1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "%${(c.progressPct * 100).toInt()}",
                            color = cardAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    ProgressBar(pct = c.progressPct, accent = cardAccent, theme.stroke)
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
                                c.maxParticipants?.let { "${c.participantsCount}/$it" } ?: "${c.participantsCount}",
                                color = theme.text1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(10.dp))
                            // Countdown / status
                            val (cdIcon, cdText, cdColor) = when {
                                c.isCompleted -> Triple(Icons.Rounded.Check, "Tamamlandı", cardAccent)
                                status == CardStatus.NotStarted ->
                                    Triple(Icons.Rounded.HourglassBottom,
                                        if (daysUntilStart == 0L) "Bugün başlıyor"
                                        else "$daysUntilStart gün sonra", cardAccent)
                                status == CardStatus.Ended ->
                                    Triple(Icons.Rounded.Schedule, "Sona erdi", cardAccent)
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
                        isInvited = c.isInvited,
                        isFull = isFull,
                        inFlight = inFlight,
                        canAct   = !c.isCompleted && status != CardStatus.Ended && !isFull,
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
    theme: com.cosmibit.profitness.core.theme.AppThemeState
) {
    val endedColor = challengeStatusAccent(CardStatus.Ended, false, accent)
    val (label, fg, bg) = when {
        completed -> Triple("TAMAM", accent, accent.copy(0.22f))
        status == CardStatus.NotStarted -> Triple("YAKINDA", Color(0xFFFFB74D), Color(0xFFFFB74D).copy(0.18f))
        status == CardStatus.Live       -> Triple("DEVAM EDİYOR", accent, accent.copy(0.18f))
        else                            -> Triple("SONA ERDİ", endedColor, endedColor.copy(0.12f))
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
    isInvited: Boolean,
    isFull: Boolean,
    inFlight: Boolean,
    canAct: Boolean,
    accent: Color,
    theme: com.cosmibit.profitness.core.theme.AppThemeState,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && canAct && !inFlight) 0.98f else 1f,
        animationSpec = tween(120),
        label = "challenge_join_press"
    )
    val bg: Color = when {
        !canAct  -> theme.bg2
        isJoined -> theme.bg2
        else     -> accent
    }
    Row(
        modifier = Modifier
            .heightIn(min = 46.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(15.dp))
            .background(bg)
            .clickable(
                enabled = !inFlight && canAct,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inFlight) {
            CircularProgressIndicator(
                color = if (isJoined) accent else theme.effectiveOnAccentColor,
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
                isFull    -> theme.t("DOLU", "FULL")
                !canAct  -> theme.t("KAPALI", "CLOSED")
                isJoined -> theme.t("KATILDIN", "JOINED")
                isInvited -> theme.t("DAVET", "INVITE")
                else     -> theme.t("KATIL", "JOIN")
            }
            val fg = when {
                !canAct  -> theme.text2
                isJoined -> accent
                else     -> theme.effectiveOnAccentColor
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
                .background(accent)
        )
    }
}

@Composable
private fun KindBadge(
    isEvent: Boolean,
    accent: Color,
    theme: com.cosmibit.profitness.core.theme.AppThemeState
) {
    val (icon, label, tint) = if (isEvent)
        Triple(Icons.Rounded.Event, theme.t("ETKİNLİK", "EVENT"), accent)
    else
        Triple(Icons.Rounded.EmojiEvents, theme.t("METRİK", "METRIC"), theme.text1)

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
private fun InviteBadge(
    accent: Color,
    theme: com.cosmibit.profitness.core.theme.AppThemeState
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(0.16f))
            .border(1.dp, accent.copy(0.38f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.PersonAdd, null, tint = accent, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            theme.t("DAVET", "INVITE"),
            color = theme.text0,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun EventModeBadge(
    mode: EventMode,
    theme: com.cosmibit.profitness.core.theme.AppThemeState
) {
    val (icon, label) = when (mode) {
        EventMode.Physical     -> Icons.Rounded.LocationOn to theme.t("FİZİKSEL", "PHYSICAL")
        EventMode.Online       -> Icons.Rounded.Link to "ONLINE"
        EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to theme.t("HAREKET", "MOVEMENT")
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
private fun EmptyBlock(text: String, theme: com.cosmibit.profitness.core.theme.AppThemeState) {
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
