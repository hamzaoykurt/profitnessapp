package com.avonix.profitness.presentation.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.social.UserSummary
import com.avonix.profitness.presentation.profile.PublicProfileOverlay

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
                        if (showingSearch) "SONUÇLAR" else "TAKİP ETTİKLERİN",
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
                                title = "Kullanıcı bulunamadı",
                                sub   = "Farklı bir isim veya @kullanıcıadı dene"
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
                                title = "Henüz kimseyi takip etmiyorsun",
                                sub   = "Yukarıdaki aramadan kullanıcı bul ve takip et"
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

        // Public profile overlay — tıklanan kullanıcı için
        AnimatedVisibility(
            visible = openProfileUserId != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit  = slideOutHorizontally(targetOffsetX = { it })  + fadeOut()
        ) {
            openProfileUserId?.let { uid ->
                PublicProfileOverlay(
                    userId       = uid,
                    onBack       = { openProfileUserId = null },
                    timerExtraPad = timerExtraPad
                )
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, null, tint = theme.text2, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "@kullanıcıadı veya isim",
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1)
            .border(0.5.dp, theme.stroke.copy(0.2f), RoundedCornerShape(18.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(theme.bg2),
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
                    Text("ARKADAŞ", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
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
    val bg = if (isFollowing) theme.bg2 else accent
    val textColor = if (isFollowing) theme.text1 else Color.Black

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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (!isFollowing) {
            Icon(Icons.Rounded.PersonAdd, null, tint = textColor, modifier = Modifier.size(13.dp))
        }
        Text(
            if (isFollowing) "TAKİPTE" else "TAKİP ET",
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
