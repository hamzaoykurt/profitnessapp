package com.cosmibit.profitness.presentation.friends

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.cosmibit.profitness.domain.social.UserSummary
import com.cosmibit.profitness.presentation.profile.PublicProfileOverlay

/**
 * Friends tab embedded into DiscoverScreen.
 * - Top: search bar (debounced in ViewModel)
 * - When query empty → "TAKİP EDİLENLER" listesi
 * - When query dolu → arama sonuçları
 * - Her satırda: avatar, display, @username, XP, takip/bırak butonu, tıkla → public profile overlay
 */
@Composable
fun FriendsTab(
    bottomPadding: Dp,
    timerExtraPad: Dp = 0.dp
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val vm: FriendsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var openProfileUserId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {

        Column(Modifier.fillMaxSize()) {

            Spacer(Modifier.height(16.dp))

            // ── Search ─────────────────────────────────────────────────
            SearchBar(
                value    = state.query,
                onChange = vm::onQueryChange,
                accent   = accent
            )

            Spacer(Modifier.height(16.dp))

            // ── List ───────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = bottomPadding + timerExtraPad + 24.dp)
            ) {
                val showingSearch = state.query.isNotBlank()

                item {
                    Text(
                        if (showingSearch) theme.t("SONUÇLAR", "RESULTS") else theme.t("TAKİP ETTİKLERİN", "FOLLOWING"),
                        color = theme.text2,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                if (showingSearch) {
                    if (state.isSearching && state.searchResults.isEmpty()) {
                        item { LoadingRow() }
                    } else if (state.searchResults.isEmpty()) {
                        item {
                            EmptyBlock(
                                icon  = Icons.Rounded.Search,
                                title = theme.t("Kullanıcı bulunamadı", "User not found"),
                                sub   = theme.t("Farklı bir isim veya @kullanıcıadı dene", "Try another name or @username")
                            )
                        }
                    } else {
                        items(state.searchResults, key = { "s_" + it.userId }) { user ->
                            UserRow(
                                user    = user,
                                onTap   = { openProfileUserId = user.userId },
                                onFollow = { vm.toggleFollow(user) }
                            )
                        }
                    }
                } else {
                    // Takip edilenler
                    if (state.isFollowingLoading && state.following.isEmpty()) {
                        item { LoadingRow() }
                    } else if (state.following.isEmpty()) {
                        item {
                            EmptyBlock(
                                icon  = Icons.Rounded.PersonOff,
                                title = theme.t("Henüz kimseyi takip etmiyorsun", "You are not following anyone yet"),
                                sub   = theme.t("Yukarıdaki aramadan kullanıcı bul ve takip et", "Find and follow users from the search above")
                            )
                        }
                    } else {
                        items(state.following, key = { "f_" + it.userId }) { user ->
                            UserRow(
                                user    = user,
                                onTap   = { openProfileUserId = user.userId },
                                onFollow = { vm.toggleFollow(user) }
                            )
                        }
                    }
                }
            }
        }

        // Public profile overlay — Dialog-backed fullscreen + glow
        openProfileUserId?.let { uid ->
            PublicProfileOverlay(
                userId       = uid,
                onBack       = { openProfileUserId = null },
                timerExtraPad = timerExtraPad
            )
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = if (theme.isDark) 10.dp else 7.dp,
                shape = shape,
                ambientColor = Color.Black.copy(if (theme.isDark) 0.28f else 0.07f),
                spotColor = Color.Black.copy(if (theme.isDark) 0.34f else 0.09f)
            )
            .clip(shape)
            .background(
                if (theme.isDark) Brush.verticalGradient(listOf(theme.bg2, theme.bg1))
                else Brush.verticalGradient(listOf(Color.White, theme.bg2.copy(0.48f)))
            )
            .border(
                1.dp,
                if (theme.isDark) theme.text0.copy(0.10f) else theme.stroke.copy(0.78f),
                shape
            )
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = theme.text2, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    theme.t("@kullanıcıadı veya isim", "@username or name"),
                    color = theme.text2.copy(0.65f),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                null,
                tint = theme.text2,
                modifier = Modifier.size(18.dp).clickable { onChange("") }
            )
        }
    }
}

@Composable
private fun UserRow(
    user: UserSummary,
    onTap: () -> Unit,
    onFollow: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.82f),
        label = "friend_row_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 1.dp else if (theme.isDark) 9.dp else 6.dp,
        label = "friend_row_elevation"
    )
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 1.5.dp.toPx() else 0f
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(if (theme.isDark) 0.28f else 0.06f),
                spotColor = Color.Black.copy(if (theme.isDark) 0.34f else 0.08f)
            )
            .clip(shape)
            .background(
                if (theme.isDark) Brush.verticalGradient(listOf(theme.bg2, theme.bg1))
                else Brush.verticalGradient(listOf(Color.White, theme.bg2.copy(0.38f)))
            )
            .border(
                1.dp,
                if (theme.isDark) theme.text0.copy(0.085f) else theme.stroke.copy(0.72f),
                shape
            )
            .clickable(interactionSource = interaction, indication = null) { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (theme.isDark) theme.bg1 else theme.bg2)
                .border(1.dp, if (user.isMutual) accent.copy(0.42f) else theme.stroke.copy(0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    user.displayName.take(1).uppercase(),
                    color = theme.text1,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                user.displayName,
                color = theme.text0,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.username?.let {
                    Text("@$it", color = theme.text2, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("·", color = theme.text2.copy(0.4f), fontSize = 11.sp)
                }
                Text("${user.totalXp} XP", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (user.isMutual) {
                    Text("·", color = theme.text2.copy(0.4f), fontSize = 11.sp)
                    Text(theme.t("ARKADAŞ", "FRIEND"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }

        // Follow / Unfollow button
        FollowButton(isFollowing = user.isFollowing, onClick = onFollow)
    }
}

@Composable
private fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = 760f, dampingRatio = 0.76f),
        label = "follow_button_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 1.dp else if (isFollowing) 3.dp else 8.dp,
        label = "follow_button_elevation"
    )
    val textColor = if (isFollowing) theme.text1 else theme.effectiveOnAccentColor
    val shape = RoundedCornerShape(50)
    val fill = if (isFollowing) {
        if (theme.isDark) Brush.verticalGradient(listOf(theme.bg2, theme.bg1))
        else Brush.verticalGradient(listOf(Color.White, theme.bg2.copy(0.72f)))
    } else {
        Brush.verticalGradient(
            listOf(
                lerp(accent, Color.White, if (theme.isDark) 0.08f else 0.14f),
                accent,
                lerp(accent, Color.Black, if (theme.isDark) 0.20f else 0.12f)
            )
        )
    }

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 2.dp.toPx() else 0f
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (isFollowing) Color.Black.copy(0.10f) else accent.copy(0.26f),
                spotColor = if (isFollowing) Color.Black.copy(0.14f) else accent.copy(0.34f)
            )
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                if (isFollowing) theme.stroke.copy(0.68f) else Color.White.copy(if (theme.isDark) 0.20f else 0.34f),
                shape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (!isFollowing) {
            Icon(Icons.Rounded.PersonAdd, null, tint = textColor, modifier = Modifier.size(13.dp))
        }
        Text(
            if (isFollowing) theme.t("TAKİPTE", "FOLLOWING") else theme.t("TAKİP ET", "FOLLOW"),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LoadingRow() {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun EmptyBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = theme.text2, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = theme.text1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = theme.text2, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
