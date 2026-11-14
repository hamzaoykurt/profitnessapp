package com.cosmibit.profitness.presentation.friends

import com.cosmibit.profitness.core.theme.SinemaMotion
import com.cosmibit.profitness.core.theme.sinemaClickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaChip
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.SinemaScene
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.accent2TextColor
import com.cosmibit.profitness.core.theme.accent2Color
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaGlow
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.domain.social.UserSummary
import com.cosmibit.profitness.presentation.profile.PublicProfileOverlay
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.SinemaRadius

private val xpFormat: java.text.NumberFormat get() = java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr"))

/**
 * Topluluk sekmesi (Keşfet içinde).
 * - Arka plan: SinemaScene (vurgu orbları); üstte büyük başlık, cam arama kapsülü
 * - Arama boşken: takip edilenler = yatay büyük avatar kartları; doluyken: sonuç satırları
 * - Takip yoksa tek eylemli davetkar boş durum (uydurma öneri yok)
 * `topPadding`: sahne üst çubuğun altına da uzansın diye içerik bu kadar aşağı itilir.
 */
@Composable
fun FriendsTab(
    bottomPadding: Dp,
    timerExtraPad: Dp = 0.dp,
    topPadding: Dp = 0.dp
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val vm: FriendsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }

    var openProfileUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val showingSearch = state.query.isNotBlank()

    Box(Modifier.fillMaxSize()) {
        // Zemin: DashboardScreen kökündeki tek SinemaBackdrop (ikinci zemin çizilmez)
        Column(Modifier.fillMaxSize().padding(top = topPadding)) {
            Text(
                theme.t("Topluluk", "Community"), style = Sinema.ScreenTitle, color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp)
            )
            Text(
                if (state.following.isNotEmpty())
                    theme.t("${state.following.size} kişiyi takip ediyorsun", "You follow ${state.following.size} people")
                else theme.t("Kişi bul, takip et, birlikte çalış", "Find people, follow, train together"),
                color = Color.White.copy(0.70f), fontSize = SinemaType.Body,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 14.dp)
            )

            SearchCapsule(value = state.query, onChange = vm::onQueryChange, accent = accent, focus = focus)

            Spacer(Modifier.height(18.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (showingSearch) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPadding + timerExtraPad + 24.dp)
                    ) {
                        item { SectionLabel(theme.t("SONUÇLAR", "RESULTS")) }
                        if (state.isSearching && state.searchResults.isEmpty()) {
                            item { LoadingRow() }
                        } else if (state.searchResults.isEmpty()) {
                            item {
                                Text(
                                    theme.t("Kullanıcı bulunamadı. Farklı bir isim veya @kullanıcıadı dene.", "User not found. Try another name or @username."),
                                    color = Color.White.copy(0.70f), fontSize = SinemaType.Body, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp)
                                )
                            }
                        } else {
                            items(state.searchResults, key = { "s_" + it.userId }) { user ->
                                UserRow(user = user, onTap = { openProfileUserId = user.userId }, onFollow = { vm.toggleFollow(user) })
                            }
                        }
                    }
                } else if (state.isFollowingLoading && state.following.isEmpty()) {
                    LoadingRow()
                } else if (state.following.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = bottomPadding + timerExtraPad),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(72.dp).sinemaCard(theme, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PersonAdd, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            theme.t("Henüz kimseyi takip etmiyorsun", "You are not following anyone yet"),
                            color = Color.White, fontSize = SinemaType.Headline, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            theme.t("Takip ettiklerin burada büyük kartlarda görünür.", "People you follow show up here as big cards."),
                            color = Color.White.copy(0.65f), fontSize = SinemaType.Body, lineHeight = 20.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        SinemaGoButton(
                            label = theme.t("KİŞİ ARA", "FIND PEOPLE"), icon = Icons.Rounded.Search,
                            onClick = { runCatching { focus.requestFocus() } },
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        SectionLabel(theme.t("TAKİP ETTİKLERİN", "FOLLOWING"))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(bottom = bottomPadding + timerExtraPad + 16.dp)
                        ) {
                            items(state.following, key = { "f_" + it.userId }) { user ->
                                FollowingCard(user = user, onTap = { openProfileUserId = user.userId }, onFollow = { vm.toggleFollow(user) })
                            }
                        }
                    }
                }
            }
        }

        // Public profile overlay — Dialog-backed fullscreen + glow
        openProfileUserId?.let { uid ->
            PublicProfileOverlay(
                userId        = uid,
                onBack        = { openProfileUserId = null },
                timerExtraPad = timerExtraPad
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = Sinema.Label, color = Color.White.copy(0.70f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchCapsule(
    value: String,
    onChange: (String) -> Unit,
    accent: Color,
    focus: FocusRequester
) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        height = 52.dp, edge = Color.Transparent,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) { fg ->
        Icon(Icons.Rounded.Search, null, tint = fg.copy(0.75f), modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(theme.t("@kullanıcıadı veya isim", "@username or name"), color = Color.White.copy(0.60f), fontSize = SinemaType.Body)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = SinemaType.Body, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(48.dp).sinemaClickable(onClick = { onChange("") }, role = Role.Button).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close, theme.t("Aramayı temizle", "Clear search"),
                    tint = fg.copy(0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Büyük takip kartı: kapak (avatar) + karartma, glow halkalı avatar, XP büyük rakam. */
@Composable
private fun FollowingCard(user: UserSummary, onTap: () -> Unit, onFollow: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(SinemaRadius.R28)
    Box(
        Modifier.width(184.dp).height(300.dp)
            .sinemaClickable(role = Role.Button, onClick = onTap, scale = 0.98f)
            .sinemaCard(theme, shape)
    ) {
        if (user.avatarUrl != null) {
            AsyncImage(
                model = user.avatarUrl, contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize().graphicsLayer { alpha = 0.55f }
            )
        }
        Box(Modifier.matchParentSize().background(Sinema.BottomScrim))
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.size(92.dp)
                    .clip(CircleShape)
                    .background(Sinema.surface(theme))
                    .border(3.dp, theme.accent2Color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl, contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(user.displayName.take(1).uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                user.displayName, color = Color.White, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
            Text(
                listOfNotNull(user.username?.let { "@$it" }, if (user.isMutual) theme.t("ARKADAŞ", "FRIEND") else null).joinToString(" · "),
                color = Color.White.copy(0.65f), fontSize = SinemaType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(xpFormat.format(user.totalXp), style = Sinema.Metric.copy(fontSize = 34.sp), color = Color.White, maxLines = 1)
                Text(
                    "XP", color = theme.accent2TextColor, fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 5.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            FollowButton(isFollowing = user.isFollowing, onClick = onFollow)
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
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(SinemaRadius.R24)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .sinemaClickable(onClick = onTap, scale = 0.98f)
            .sinemaCard(theme, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Text(user.displayName.take(1).uppercase(), color = Color.White, fontSize = SinemaType.Title, fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.weight(1f)) {
            Text(user.displayName, color = Color.White, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.username?.let {
                    Text("@$it", color = Color.White.copy(0.65f), fontSize = SinemaType.Caption, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("·", color = Color.White.copy(0.4f), fontSize = SinemaType.Caption)
                }
                Text("${xpFormat.format(user.totalXp)} XP", color = theme.accent2TextColor, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold)
                if (user.isMutual) {
                    Text("·", color = Color.White.copy(0.4f), fontSize = SinemaType.Caption)
                    Text(theme.t("ARKADAŞ", "FRIEND"), color = Color.White.copy(0.65f), fontSize = SinemaType.Caption, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }

        FollowButton(isFollowing = user.isFollowing, onClick = onFollow)
    }
}

@Composable
private fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    SinemaChip(active = !isFollowing, onClick = onClick, height = 36.dp) { fg ->
        if (!isFollowing) {
            Icon(Icons.Rounded.PersonAdd, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            if (isFollowing) theme.t("TAKİPTE", "FOLLOWING") else theme.t("TAKİP ET", "FOLLOW"),
            color = fg,
            fontSize = SinemaType.Caption,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LoadingRow() {
    val accent = LocalAppTheme.current.effectiveAccentColor
    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}
