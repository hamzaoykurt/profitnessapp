package com.avonix.profitness.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.PageAccentBloom
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeKind
import com.avonix.profitness.domain.discover.SharedProgram
import com.avonix.profitness.domain.social.PublicProfile
import com.avonix.profitness.presentation.challenges.ChallengeDetailOverlay
import com.avonix.profitness.presentation.components.AppBackButton
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
    val accent = MaterialTheme.colorScheme.primary
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
        Box(Modifier.fillMaxSize().background(theme.bg0)) {
            PageAccentBloom()

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
                        Text("Profil yüklenemedi", color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(state.error ?: "", color = theme.text2, fontSize = 12.sp, textAlign = TextAlign.Center)
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
                    .padding(16.dp),
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
    val accent = MaterialTheme.colorScheme.primary
    val scroll = rememberScrollState()

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
        DateTimeFormatter.ofPattern("MMM yyyy", Locale("tr"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrElse { "—" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(top = 96.dp, bottom = 40.dp + timerExtraPad)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(theme.bg2)
                .border(2.dp, accent.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profile.avatarUrl != null) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    profile.displayName.take(1).uppercase(),
                    color = theme.text0,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Seviye rozeti
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(0.15f))
                .border(1.dp, accent.copy(0.4f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(12.dp))
            Text(
                "SEVİYE ${profile.level}",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        // Ad + @kullanici
        Text(profile.displayName, color = theme.text0, fontSize = 22.sp, fontWeight = FontWeight.Black)
        profile.username?.let {
            Text("@$it", color = theme.text2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        if (profile.isMutual) {
            Spacer(Modifier.height(6.dp))
            Text(
                "KARŞILIKLI TAKİP",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(0.12f))
                    .border(1.dp, accent.copy(0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Follow / Unfollow
        FollowActionButton(isFollowing = profile.isFollowing, onClick = onToggleFollow)

        Spacer(Modifier.height(24.dp))

        // XP Progress bar
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Workspaces, null, tint = accent, modifier = Modifier.size(13.dp))
                    Text(
                        "${profile.totalXp} XP",
                        color = theme.text0,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Sıradaki: ${xpForNextLevel} XP",
                    color = theme.text2,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = theme.bg2,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(Modifier.height(20.dp))

        // Stats row: başarım / rank / antrenman
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatTile(profile.achievementsCount.toString(), "BAŞARIM", Icons.Rounded.EmojiEvents)
            StatTile(profile.currentRank.uppercase().take(6), "RANK", Icons.Rounded.MilitaryTech)
            StatTile(profile.totalWorkouts.toString(), "ANTRENMAN", Icons.Rounded.FitnessCenter)
        }

        Spacer(Modifier.height(16.dp))

        // Streak + Üyelik tarihi
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .border(0.5.dp, theme.stroke.copy(0.25f), RoundedCornerShape(20.dp))
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seri
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(18.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "${profile.currentStreak} GÜN",
                        color = theme.text0,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("GÜNCEL SERİ", color = theme.text2, fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }

            Box(Modifier.width(1.dp).height(36.dp).background(theme.stroke.copy(0.3f)))

            // Üyelik tarihi
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = accent.copy(0.8f), modifier = Modifier.size(18.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        joinDate.uppercase(Locale("tr")),
                        color = theme.text0,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("ÜYELİK", color = theme.text2, fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Followers / Following row
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .border(0.5.dp, theme.stroke.copy(0.25f), RoundedCornerShape(20.dp))
                .padding(vertical = 14.dp)
        ) {
            FollowCountBlock(profile.followersCount, "TAKİPÇİ", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(theme.stroke.copy(0.3f)))
            FollowCountBlock(profile.followingCount, "TAKİP", Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        ActivitySection(
            challenges = challenges,
            sharedPrograms = sharedPrograms,
            loading = activityLoading,
            onOpenChallenge = onOpenChallenge
        )
    }
}

@Composable
private fun FollowActionButton(isFollowing: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val bg = if (isFollowing) theme.bg2 else accent
    val fg = if (isFollowing) theme.text0 else Color.Black

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                if (isFollowing) 1.dp else 0.dp,
                if (isFollowing) theme.stroke.copy(0.4f) else Color.Transparent,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!isFollowing) Icon(Icons.Rounded.PersonAdd, null, tint = fg, modifier = Modifier.size(15.dp))
        Text(
            if (isFollowing) "TAKİPTE" else "TAKİP ET",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
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
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AKTİVİTE",
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(1.dp).background(theme.stroke.copy(0.25f)))
        }
        Spacer(Modifier.height(10.dp))
        if (loading) {
            Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            return@Column
        }

        if (challenges.isEmpty() && sharedPrograms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.bg1)
                    .border(0.5.dp, theme.stroke.copy(0.25f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Henüz görünür challenge veya program paylaşımı yok.", color = theme.text2, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
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
                    accent = accent,
                    onOpen = { onOpenChallenge(challenge.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

        sharedPrograms.take(4).forEach { program ->
            ActivityMiniCard(
                icon = Icons.Rounded.Bookmark,
                title = program.title,
                meta = "${program.downloadsCount} uygulama · ${program.likesCount} beğeni",
                accent = accent,
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
    accent: Color,
    onOpen: () -> Unit
) {
    val theme = LocalAppTheme.current
    val status = profileChallengeStatus(challenge)
    val (label, color, icon) = when (status) {
        ProfileChallengeStatus.Active -> Triple("AKTİF", accent, Icons.Rounded.Event)
        ProfileChallengeStatus.Upcoming -> Triple("YAKINDA", Color(0xFFFFB74D), Icons.Rounded.HourglassBottom)
        ProfileChallengeStatus.Completed -> Triple("TAMAM", Color(0xFF38BDF8), Icons.Rounded.Check)
        ProfileChallengeStatus.Ended -> Triple("SONA ERDİ", Color(0xFF9CA3AF), Icons.Rounded.Schedule)
    }
    val canOpen = status == ProfileChallengeStatus.Active || status == ProfileChallengeStatus.Upcoming
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(0.8.dp, color.copy(0.35f), RoundedCornerShape(16.dp))
            .then(if (canOpen) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (challenge.kind == ChallengeKind.Event) icon else Icons.Rounded.EmojiEvents,
                null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    challenge.title,
                    color = theme.text0,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    color = color,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(color.copy(0.12f))
                        .border(1.dp, color.copy(0.3f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Text(
                "${challenge.participantsCount} katılımcı · ${challenge.targetValue} ${challenge.targetType.unit}",
                color = theme.text2,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (canOpen) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ActivityMiniCard(
    icon: ImageVector,
    title: String,
    meta: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(0.5.dp, theme.stroke.copy(0.25f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = theme.text2, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: ImageVector) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(0.1f))
                .border(1.dp, accent.copy(0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = theme.text0, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(label, color = theme.text2, fontSize = 8.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FollowCountBlock(count: Int, label: String, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = theme.text2, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    }
}
