package com.cosmibit.profitness.presentation.profile

import androidx.compose.foundation.background
import com.cosmibit.profitness.core.theme.sinemaClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
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
import com.cosmibit.profitness.core.theme.AppLanguage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaLayout
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaBackdrop
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.ChallengeKind
import com.cosmibit.profitness.domain.discover.SharedProgram
import com.cosmibit.profitness.domain.social.PublicProfile
import com.cosmibit.profitness.presentation.challenges.ChallengeDetailOverlay
import com.cosmibit.profitness.presentation.challenges.displayUnit
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaGlowBar
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bir kullanıcının public profilini overlay olarak gösteren composable.
 * Leaderboard / FriendsScreen / Discover feed kartı gibi yerlerden tıklayarak açılır.
 */
@Composable
fun PublicProfileOverlay(
    userId: String,
    onBack: () -> Unit,
    timerExtraPad: Dp = 0.dp
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val vm: PublicProfileViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var openChallengeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) { vm.load(userId) }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Sinema.page(theme))) {
            SinemaBackdrop()
            when {
                state.isLoading && state.profile == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                state.error != null && state.profile == null -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            theme.t("Profil yüklenemedi", "Profile could not be loaded"),
                            color = Sinema.text(theme), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", color = Sinema.textSecondary(theme), fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                state.profile != null -> {
                    ProfileContent(
                        profile        = state.profile!!,
                        challenges     = state.challenges,
                        sharedPrograms = state.sharedPrograms,
                        activityLoading = state.activityLoading,
                        onToggleFollow = { vm.toggleFollow() },
                        onOpenChallenge = { openChallengeId = it },
                        timerExtraPad  = timerExtraPad
                    )
                }
            }

            openChallengeId?.let { challengeId ->
                ChallengeDetailOverlay(
                    challengeId = challengeId,
                    onBack = { openChallengeId = null },
                    onChanged = { vm.refreshActivity() }
                )
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(SinemaLayout.gutter(LocalWindowClass.current.widthDp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBackButton(onClick = onBack, size = 48.dp)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: PublicProfile,
    challenges: List<ChallengeSummary>,
    sharedPrograms: List<SharedProgram>,
    activityLoading: Boolean,
    onToggleFollow: () -> Unit,
    onOpenChallenge: (String) -> Unit,
    timerExtraPad: Dp
) {
    val theme = LocalAppTheme.current
    val scroll = rememberScrollState()
    val wc = LocalWindowClass.current
    val gutter = SinemaLayout.gutter(wc.widthDp)
    val coverH = SinemaLayout.cover(wc.heightDp, 0.50f, 320.dp, 480.dp)

    // XP hesaplama: level^2 * 100 → bir sonraki seviyeye kaç XP
    val xpForCurrentLevel = ((profile.level - 1) * (profile.level - 1) * 100)
    val xpForNextLevel    = (profile.level * profile.level * 100)
    val xpInLevel         = (profile.totalXp - xpForCurrentLevel).coerceAtLeast(0)
    val xpNeeded          = (xpForNextLevel - xpForCurrentLevel).coerceAtLeast(1)
    val rawProgress       = xpInLevel.toFloat() / xpNeeded
    val xpProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "xpProgress"
    )

    // Üyelik tarihi
    val joinDate = runCatching {
        val instant = Instant.parse(profile.createdAtIso)
        val locale = if (theme.language == AppLanguage.ENGLISH) Locale.ENGLISH else Locale("tr")
        DateTimeFormatter.ofPattern("MMM yyyy", locale)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrElse { "—" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        // ── Kapak: foto (yoksa Sinema sahnesi) + alt karartma + ad ─────────
        Box(Modifier.fillMaxWidth().height(coverH)) {
            if (profile.avatarUrl != null) {
                Box(Modifier.fillMaxSize().photoFadeToBackdrop()) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Sinema.BottomScrim))
                }
            } else {
                Text(
                    profile.displayName.take(1).uppercase(),
                    color = Color.White.copy(0.9f), fontSize = 110.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp)
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).widthIn(max = SinemaLayout.ContentMaxReading).fillMaxWidth()
                    .padding(start = gutter, end = gutter, bottom = 44.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SinemaCapsule(height = 30.dp, contentPadding = PaddingValues(horizontal = 12.dp)) { fg ->
                        Text(theme.t("Seviye ${profile.level}", "Level ${profile.level}"), color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (profile.isMutual) {
                        SinemaCapsule(height = 30.dp, contentPadding = PaddingValues(horizontal = 12.dp)) { fg ->
                            Text(theme.t("Karşılıklı takip", "Mutual follow"), color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    profile.displayName, color = Color.White, style = Sinema.ScreenTitle,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                profile.username?.let {
                    Text("@$it", color = Color.White.copy(0.72f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(16.dp))
                FollowActionButton(isFollowing = profile.isFollowing, onClick = onToggleFollow)
            }
        }

        // ── Sheet ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = SinemaLayout.ContentMaxReading)
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = gutter)
                .padding(top = 28.dp, bottom = 40.dp + timerExtraPad)
                .navigationBarsPadding()
        ) {
            // XP
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    SinemaBigStat("${profile.totalXp}", "XP", size = 52, color = theme.effectiveAccentColor)
                    Text(
                        theme.t("Sıradaki: ${xpForNextLevel} XP", "Next: ${xpForNextLevel} XP"),
                        color = Sinema.textSecondary(theme), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                SinemaGlowBar(xpProgress)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(28.dp)).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SinemaBigStat(profile.totalWorkouts.toString(), theme.t("Antrenman", "Workouts"), Modifier.weight(1f), size = 36)
                SinemaBigStat(profile.achievementsCount.toString(), theme.t("Başarım", "Achievements"), Modifier.weight(1f), size = 36)
                SinemaBigStat(profile.currentStreak.toString(), theme.t("Gün seri", "Day streak"), Modifier.weight(1f), size = 36)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(28.dp)).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SinemaBigStat(profile.followersCount.toString(), theme.t("Takipçi", "Followers"), Modifier.weight(1f), size = 36)
                SinemaBigStat(profile.followingCount.toString(), theme.t("Takip", "Following"), Modifier.weight(1f), size = 36)
                SinemaBigStat(profile.currentRank.take(8), "Rank", Modifier.weight(1f), size = 22)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                theme.t("Üyelik", "Member since") + ": " +
                    joinDate.uppercase(if (theme.language == AppLanguage.ENGLISH) Locale.ENGLISH else Locale("tr")),
                color = Sinema.textSecondary(theme), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(Modifier.height(12.dp))
            ActivitySection(
                challenges = challenges,
                sharedPrograms = sharedPrograms,
                loading = activityLoading,
                onOpenChallenge = onOpenChallenge
            )
        }
    }
}

/** Takip et = dolu vurgu + glow; takipte = cam. */
@Composable
private fun FollowActionButton(isFollowing: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        selected = !isFollowing, onClick = onClick, height = 48.dp,
        modifier = Modifier.widthIn(min = 160.dp),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) { fg ->
        if (isFollowing) Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(16.dp))
        else Icon(Icons.Rounded.PersonAdd, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (isFollowing) theme.t("TAKİPTE", "FOLLOWING") else theme.t("TAKİP ET", "FOLLOW"),
            color = fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun ActivitySection(
    challenges: List<ChallengeSummary>,
    sharedPrograms: List<SharedProgram>,
    loading: Boolean,
    onOpenChallenge: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.offset(x = (-20).dp)) { SinemaSectionLabel(theme.t("Aktivite", "Activity")) }
        if (loading) {
            Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            return@Column
        }

        if (challenges.isEmpty() && sharedPrograms.isEmpty()) {
            Text(
                theme.t(
                    "Henüz görünür challenge veya program paylaşımı yok.",
                    "No visible challenges or program shares yet."
                ),
                color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return@Column
        }

        challenges
            .sortedWith(
                compareBy<ChallengeSummary> { profileChallengeStatusRank(it) }
                    .thenByDescending { it.startDateIso }
            )
            .take(4)
            .forEach { challenge ->
                ChallengeActivityCard(
                    challenge = challenge,
                    onOpen = { onOpenChallenge(challenge.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

        sharedPrograms.take(4).forEach { program ->
            ActivityMiniCard(
                icon = Icons.Rounded.Bookmark,
                title = program.title,
                meta = theme.t(
                    "${program.downloadsCount} uygulama · ${program.likesCount} beğeni",
                    "${program.downloadsCount} applies · ${program.likesCount} likes"
                ),
                onClick = null
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class ProfileChallengeStatus { Upcoming, Active, Completed, Ended }

private fun profileChallengeStatus(challenge: ChallengeSummary): ProfileChallengeStatus {
    val today = java.time.LocalDate.now()
    val start = runCatching { java.time.LocalDate.parse(challenge.startDateIso) }.getOrNull()
    val end = runCatching { java.time.LocalDate.parse(challenge.endDateIso) }.getOrNull()
    return when {
        challenge.isCompleted -> ProfileChallengeStatus.Completed
        end != null && today.isAfter(end) -> ProfileChallengeStatus.Ended
        start != null && today.isBefore(start) -> ProfileChallengeStatus.Upcoming
        else -> ProfileChallengeStatus.Active
    }
}

private fun profileChallengeStatusRank(challenge: ChallengeSummary): Int = when (profileChallengeStatus(challenge)) {
    ProfileChallengeStatus.Active -> 0
    ProfileChallengeStatus.Upcoming -> 1
    ProfileChallengeStatus.Completed -> 2
    ProfileChallengeStatus.Ended -> 3
}

@Composable
private fun ChallengeActivityCard(
    challenge: ChallengeSummary,
    onOpen: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val status = profileChallengeStatus(challenge)
    val (label, icon) = when (status) {
        ProfileChallengeStatus.Active -> theme.t("AKTİF", "ACTIVE") to Icons.Rounded.Event
        ProfileChallengeStatus.Upcoming -> theme.t("YAKINDA", "UPCOMING") to Icons.Rounded.HourglassBottom
        ProfileChallengeStatus.Completed -> theme.t("TAMAM", "DONE") to Icons.Rounded.Check
        ProfileChallengeStatus.Ended -> theme.t("SONA ERDİ", "ENDED") to Icons.Rounded.Schedule
    }
    val canOpen = status == ProfileChallengeStatus.Active || status == ProfileChallengeStatus.Upcoming
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .then(if (canOpen) Modifier.sinemaClickable(role = Role.Button, onClick = onOpen) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Sinema.line(theme)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (challenge.kind == ChallengeKind.Event) icon else Icons.Rounded.EmojiEvents,
                null, tint = accent, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                challenge.title, color = Sinema.text(theme), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                theme.t(
                    "${challenge.participantsCount} katılımcı · ${challenge.targetValue} ${challenge.targetType.displayUnit(theme)}",
                    "${challenge.participantsCount} participants · ${challenge.targetValue} ${challenge.targetType.displayUnit(theme)}"
                ),
                color = Sinema.textSecondary(theme), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        SinemaCapsule(
            selected = status == ProfileChallengeStatus.Active, height = 26.dp,
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) { fg -> Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
        if (canOpen) {
            Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ActivityMiniCard(
    icon: ImageVector,
    title: String,
    meta: String,
    onClick: (() -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.sinemaClickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Sinema.line(theme)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = theme.effectiveAccentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Sinema.text(theme), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = Sinema.textSecondary(theme), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
