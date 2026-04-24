package com.avonix.profitness.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.social.PublicProfile

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
                    onToggleFollow = { vm.toggleFollow() },
                    timerExtraPad  = timerExtraPad
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (theme.isDark) Color.Black.copy(0.55f) else theme.bg2.copy(0.9f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = theme.text0, modifier = Modifier.size(20.dp))
            }
        }
    }
    }
}

@Composable
private fun ProfileContent(
    profile: PublicProfile,
    onToggleFollow: () -> Unit,
    timerExtraPad: Dp
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val scroll = rememberScrollState()

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

        Spacer(Modifier.height(14.dp))

        // Name + @username + mutual badge
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

        Spacer(Modifier.height(18.dp))

        // Follow / Unfollow
        FollowActionButton(isFollowing = profile.isFollowing, onClick = onToggleFollow)

        Spacer(Modifier.height(26.dp))

        // Stats row
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatTile("XP", profile.totalXp.toString(), Icons.Rounded.Workspaces)
            StatTile("BAŞARIM", profile.achievementsCount.toString(), Icons.Rounded.EmojiEvents)
            StatTile("RANK", profile.currentRank.uppercase().take(6), Icons.Rounded.MilitaryTech)
        }

        Spacer(Modifier.height(20.dp))

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
private fun StatTile(label: String, value: String, icon: ImageVector) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = theme.text2, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
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
