package com.avonix.profitness.presentation.discover

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.MySharedProgram
import com.avonix.profitness.domain.discover.SharedProgram
import android.widget.Toast

enum class DiscoverTab { Programs, Friends, Challenges }

/** Programlar sekmesi altındaki iç sekme: Topluluk akışı vs. Benim paylaşımlarım. */
private enum class ProgramsSubTab { Community, Mine }

@Composable
fun DiscoverScreen(
    bottomPadding: Dp,
    timerExtraPad: Dp = 0.dp
) {
    var selected by rememberSaveable { mutableStateOf(DiscoverTab.Programs) }
    var programsSub by rememberSaveable { mutableStateOf(ProgramsSubTab.Community) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }

    val viewModel: DiscoverViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myPrograms by viewModel.myPrograms.collectAsStateWithLifecycle()
    val myProgramIds = remember(myPrograms) { myPrograms.map { it.id }.toSet() }
    val context = LocalContext.current

    // Share / Apply / Error toast'ları
    LaunchedEffect(state.shareResult) {
        state.shareResult?.let { result ->
            val msg = when (result) {
                is ShareResult.Success  -> "Program topluluk akışına eklendi ✓"
                is ShareResult.Error    -> "Paylaşım başarısız: ${result.msg}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeShareResult()
        }
    }
    LaunchedEffect(state.applyResult) {
        state.applyResult?.let { result ->
            val msg = when (result) {
                is ApplyResult.Success  -> "Program planına uygulandı ✓"
                is ApplyResult.Error    -> "Uygulanamadı: ${result.msg}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeApplyResult()
        }
    }
    LaunchedEffect(state.myActionMsg) {
        state.myActionMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeMyActionMsg()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = timerExtraPad)
        ) {
            DiscoverHeader(
                isTrending = state.sort == DiscoverSort.TRENDING,
                onToggleSort = {
                    viewModel.changeSort(
                        if (state.sort == DiscoverSort.TRENDING) DiscoverSort.NEWEST
                        else DiscoverSort.TRENDING
                    )
                },
                onRefresh = { viewModel.refresh() }
            )
            DiscoverTabBar(selected) { selected = it }

            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f),
                label = "discover_tab"
            ) { tab ->
                when (tab) {
                    DiscoverTab.Programs -> Column(Modifier.fillMaxSize()) {
                        ProgramsSubTabBar(
                            selected = programsSub,
                            mineCount = state.myShared.size,
                            onSelect = { programsSub = it }
                        )
                        AnimatedContent(
                            targetState = programsSub,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
                            modifier = Modifier.weight(1f),
                            label = "programs_sub"
                        ) { sub ->
                            when (sub) {
                                ProgramsSubTab.Community -> ProgramsList(
                                     state         = state,
                                     myProgramIds  = myProgramIds,
                                     bottomPadding = bottomPadding,
                                     onLike        = viewModel::toggleLike,
                                     onSave        = viewModel::toggleSave,
                                     onApply       = viewModel::applyProgram,
                                    onLoadMore    = viewModel::loadMore,
                                    onRefresh     = viewModel::refresh
                                )
                                ProgramsSubTab.Mine -> MySharedProgramsList(
                                    items           = state.myShared,
                                    isLoading       = state.myLoading,
                                    syncInFlight    = state.mySyncInFlight,
                                    deleteInFlight  = state.myDeleteInFlight,
                                    bottomPadding   = bottomPadding,
                                    onSync          = viewModel::syncShared,
                                    onDelete        = viewModel::deleteShared,
                                    onStartShare    = { showShareSheet = true }
                                )
                            }
                        }
                    }
                    DiscoverTab.Friends -> com.avonix.profitness.presentation.friends.FriendsTab(
                        bottomPadding = bottomPadding,
                        timerExtraPad = 0.dp
                    )
                    DiscoverTab.Challenges -> com.avonix.profitness.presentation.challenges.ChallengesTab(
                        bottomPadding = bottomPadding,
                        timerExtraPad = 0.dp
                    )
                }
            }
        }

        // ── Share FAB — sadece Programs tab'ında görünür ───────────────────
        if (selected == DiscoverTab.Programs) {
            ShareFab(
                onClick = { showShareSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = bottomPadding + 12.dp)
            )
        }
    }

    if (showShareSheet) {
        ShareProgramSheet(
            programs  = myPrograms,
            preselectedProgramId = null,
            onDismiss = { showShareSheet = false },
            onConfirm = { programId, title, desc, tags, difficulty, weeks, days ->
                viewModel.shareProgram(programId, title, desc, tags, difficulty, weeks, days)
                showShareSheet = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HEADER + TABS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DiscoverHeader(
    isTrending: Boolean,
    onToggleSort: () -> Unit,
    onRefresh: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "KEŞFET",
                color      = theme.text0,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = "Topluluk programları & challenge'lar",
                color    = theme.text2.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        // Sort toggle
        SmallIconChip(
            icon     = Icons.Rounded.Whatshot,
            label    = if (isTrending) "TREND" else "YENİ",
            selected = isTrending,
            onClick  = onToggleSort
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Yenile", tint = accent)
        }
    }
}

@Composable
private fun SmallIconChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accent.copy(0.18f) else theme.bg2.copy(0.5f))
            .border(1.dp, if (selected) accent.copy(0.4f) else theme.stroke.copy(0.35f), shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) accent else theme.text2.copy(0.6f),
            modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (selected) accent else theme.text2.copy(0.7f),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun DiscoverTabBar(
    selected: DiscoverTab,
    onSelect: (DiscoverTab) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DiscoverTabPill(
            label    = "PROGRAMLAR",
            icon     = Icons.Rounded.FitnessCenter,
            selected = selected == DiscoverTab.Programs,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Programs) }
        )
        DiscoverTabPill(
            label    = "ARKADAŞLAR",
            icon     = Icons.Rounded.Person,
            selected = selected == DiscoverTab.Friends,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Friends) }
        )
        DiscoverTabPill(
            label    = "CHALLENGE",
            icon     = Icons.Rounded.EmojiEvents,
            selected = selected == DiscoverTab.Challenges,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Challenges) }
        )
    }
}

@Composable
private fun DiscoverTabPill(
    label: String, icon: ImageVector, selected: Boolean, accent: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(accent.copy(0.22f), accent.copy(0.10f)))
                else          Brush.linearGradient(listOf(theme.bg2.copy(0.5f), theme.bg1.copy(0.5f)))
            )
            .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke.copy(0.35f), shape)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (selected) accent else theme.text2.copy(0.55f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) accent else theme.text2.copy(0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PROGRAMS LIST
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgramsList(
    state         : DiscoverProgramsState,
    myProgramIds  : Set<String>,
    bottomPadding : Dp,
    onLike        : (String) -> Unit,
    onSave        : (String) -> Unit,
    onApply       : (String) -> Unit,
    onLoadMore    : () -> Unit,
    onRefresh     : () -> Unit
) {
    val listState = rememberLazyListState()

    // Infinite scroll — sona 3 item kala yeni sayfa iste
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.canLoadMore) {
        if (shouldLoadMore && state.canLoadMore && !state.isLoading) onLoadMore()
    }

    when {
        state.items.isEmpty() && state.isLoading -> LoadingState()
        state.items.isEmpty() && state.error != null -> ErrorState(state.error, onRefresh)
        state.items.isEmpty() -> EmptyFeedState()
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 12.dp, bottom = bottomPadding + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(state.items, key = { it.id }) { program ->
                val appliedLocalProgramId = state.appliedProgramMap[program.id]
                val isApplied = appliedLocalProgramId != null &&
                    appliedLocalProgramId !in state.localDeletingProgramIds &&
                    myProgramIds.contains(appliedLocalProgramId)
                SharedProgramCard(
                    program    = program,
                    isApplying = program.id in state.applyingProgramIds,
                    isApplied  = isApplied,
                    onLike     = { onLike(program.id) },
                    onSave     = { onSave(program.id) },
                    onApply    = { onApply(program.id) }
                )
            }
            if (state.isLoading && state.items.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
                }
            }
        }
    }
}

@Composable
private fun SharedProgramCard(
    program   : SharedProgram,
    isApplying: Boolean,
    isApplied : Boolean,
    onLike    : () -> Unit,
    onSave    : () -> Unit,
    onApply   : () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.bg2.copy(0.55f), theme.bg1.copy(0.55f))))
            .border(1.dp, theme.stroke.copy(0.35f), shape)
            .padding(16.dp)
    ) {
        // Author row
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBubble(url = program.creatorAvatarUrl, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = program.creatorName,
                    color      = theme.text0,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1
                )
                val metaParts = buildList {
                    program.difficulty?.name?.lowercase()?.let { add(it) }
                    program.daysPerWeek?.let { add("$it gün/hafta") }
                    program.durationWeeks?.let { add("$it hafta") }
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text     = metaParts.joinToString(" · "),
                        color    = theme.text2.copy(0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Title + description
        Text(
            text       = program.title,
            color      = theme.text0,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines   = 2
        )
        if (!program.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text     = program.description,
                color    = theme.text2.copy(0.75f),
                fontSize = 13.sp,
                maxLines = 3,
                lineHeight = 18.sp
            )
        }

        // Tags
        if (program.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                program.tags.take(4).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("#$tag", color = accent.copy(0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Actions row
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionChip(
                icon     = if (program.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                count    = program.likesCount,
                active   = program.isLikedByMe,
                activeColor = Color(0xFFEF476F),
                onClick  = onLike
            )
            Spacer(Modifier.width(12.dp))
            ActionChip(
                icon     = if (program.isSavedByMe) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                count    = program.savesCount,
                active   = program.isSavedByMe,
                activeColor = accent,
                onClick  = onSave
            )
            Spacer(Modifier.width(12.dp))
            ActionChip(
                icon   = Icons.Rounded.CloudDownload,
                count  = program.downloadsCount,
                active = false,
                activeColor = accent,
                onClick = {}   // salt sayaç
            )
            Spacer(Modifier.weight(1f))
            ApplyButton(
                onClick = onApply,
                text = when {
                    isApplied -> "UYGULANDI"
                    isApplying -> "UYGULANIYOR"
                    else -> "UYGULA"
                },
                enabled = !isApplying && !isApplied,
                isLoading = isApplying,
                applied = isApplied
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon        : ImageVector,
    count       : Int,
    active      : Boolean,
    activeColor : Color,
    onClick     : () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) activeColor else theme.text2.copy(0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = if (active) activeColor else theme.text2.copy(0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ApplyButton(
    onClick: () -> Unit,
    text: String = "UYGULA",
    enabled: Boolean = true,
    isLoading: Boolean = false,
    applied: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        applied -> Brush.linearGradient(listOf(accent.copy(0.18f), accent.copy(0.10f)))
        enabled -> Brush.linearGradient(listOf(accent, accent.copy(0.75f)))
        else -> Brush.linearGradient(listOf(accent.copy(0.45f), accent.copy(0.35f)))
    }
    val textColor = if (applied) accent else Color.Black
    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (applied) accent.copy(0.35f) else Color.Transparent,
                shape = shape
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = textColor
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun AvatarBubble(url: String?, size: Dp) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(theme.bg2)
            .border(1.dp, accent.copy(0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                Icons.Rounded.Person, null,
                tint = theme.text2.copy(0.6f),
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  STATES + PLACEHOLDERS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ErrorState(msg: String, onRetry: () -> Unit) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Yüklenemedi", color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(msg, color = theme.text2.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        ApplyButton(
            onClick = onRetry,
            text = "TEKRAR DENE"
        )
    }
}

@Composable
private fun EmptyFeedState() {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Henüz paylaşılan program yok",
            color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "İlk paylaşan sen ol — sağ alttaki + butonuna dokun.",
            color = theme.text2.copy(0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChallengesPlaceholder(bottomPadding: Dp) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Grup Challenge'ları", color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Arkadaşlarınla ya da toplulukla meydan okumaya katıl, ilerleme takip et, ödül kazan.",
            color = theme.text2.copy(0.7f), fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(0.14f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("ÇOK YAKINDA", color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SHARE FAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ShareFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent, accent.copy(0.7f))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Share, "Programı paylaş",
            tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PROGRAMS SUB-TAB: Topluluk vs. Benim paylaşımlarım
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgramsSubTabBar(
    selected: ProgramsSubTab,
    mineCount: Int,
    onSelect: (ProgramsSubTab) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubTabChip(
            label = "TOPLULUK",
            selected = selected == ProgramsSubTab.Community,
            onClick = { onSelect(ProgramsSubTab.Community) },
            modifier = Modifier.weight(1f)
        )
        SubTabChip(
            label = if (mineCount > 0) "PAYLAŞIMLARIM · $mineCount" else "PAYLAŞIMLARIM",
            selected = selected == ProgramsSubTab.Mine,
            onClick = { onSelect(ProgramsSubTab.Mine) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SubTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(0.18f) else theme.bg2.copy(0.4f))
            .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke.copy(0.3f), shape)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) accent else theme.text2.copy(0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MY SHARED PROGRAMS LIST — sync + delete yönetimi
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MySharedProgramsList(
    items          : List<MySharedProgram>,
    isLoading      : Boolean,
    syncInFlight   : Set<String>,
    deleteInFlight : Set<String>,
    bottomPadding  : Dp,
    onSync         : (String) -> Unit,
    onDelete       : (String) -> Unit,
    onStartShare   : () -> Unit
) {
    val theme = LocalAppTheme.current

    when {
        items.isEmpty() && isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        items.isEmpty() -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Henüz bir program paylaşmadın",
                color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Programlarından birini topluluğa paylaşmak için dokun.",
                color = theme.text2.copy(0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            val accent = MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(accent, accent.copy(0.75f))))
                    .clickable { onStartShare() }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Share, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PROGRAM PAYLAŞ", color = Color.Black,
                    fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 4.dp, bottom = bottomPadding + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MySharedCard(
                    item         = item,
                    isSyncing    = item.id in syncInFlight,
                    isDeleting   = item.id in deleteInFlight,
                    onSync       = { onSync(item.id) },
                    onDelete     = { onDelete(item.id) }
                )
            }
        }
    }
}

@Composable
private fun MySharedCard(
    item       : MySharedProgram,
    isSyncing  : Boolean,
    isDeleting : Boolean,
    onSync     : () -> Unit,
    onDelete   : () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val warn = Color(0xFFF5A524)
    val danger = Color(0xFFEF476F)
    val shape = RoundedCornerShape(18.dp)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = theme.bg1,
            title = { Text("Paylaşımı sil?", color = theme.text0, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${item.title}\" topluluk akışından kalıcı olarak kaldırılacak. Kendi programın silinmez.",
                    color = theme.text2, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Sil", color = danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal", color = theme.text2)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.bg2.copy(0.55f), theme.bg1.copy(0.55f))))
            .border(1.dp, theme.stroke.copy(0.35f), shape)
            .padding(16.dp)
    ) {
        Text(
            text = item.title,
            color = theme.text0,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        if (!item.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.description,
                color = theme.text2.copy(0.75f),
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        // Meta chips row
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatLabel(Icons.Rounded.Favorite, item.likesCount.toString())
            Spacer(Modifier.width(10.dp))
            StatLabel(Icons.Rounded.Bookmark, item.savesCount.toString())
            Spacer(Modifier.width(10.dp))
            StatLabel(Icons.Rounded.CloudDownload, item.downloadsCount.toString())
        }

        Spacer(Modifier.height(12.dp))

        // Senkron durumu
        when {
            !item.sourceExists -> StatusBanner(
                icon = Icons.Rounded.Warning,
                color = danger,
                text  = "Kaynak program silinmiş. Sadece silebilirsin."
            )
            item.isOutOfSync -> StatusBanner(
                icon = Icons.Rounded.Warning,
                color = warn,
                text  = "Kaynak programın güncellenmiş. 'Senkronize et' ile paylaşımı yenile."
            )
            else -> StatusBanner(
                icon = Icons.Rounded.Sync,
                color = accent,
                text  = "Paylaşım güncel programınla senkron."
            )
        }

        Spacer(Modifier.height(12.dp))

        // Aksiyonlar
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sync
            val canSync = item.sourceExists && !isDeleting
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canSync) accent.copy(0.14f) else theme.bg2.copy(0.3f)
                    )
                    .border(
                        1.dp,
                        if (canSync) accent.copy(0.4f) else theme.stroke.copy(0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = canSync && !isSyncing) { onSync() }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = accent)
                } else {
                    Icon(
                        Icons.Rounded.Sync, null,
                        tint = if (canSync) accent else theme.text2.copy(0.4f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (item.isOutOfSync) "SENKRONİZE ET" else "YENİDEN SENKRON",
                    color = if (canSync) accent else theme.text2.copy(0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            // Delete
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(danger.copy(0.12f))
                    .border(1.dp, danger.copy(0.35f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isDeleting) { showDeleteDialog = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = danger)
                } else {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = danger, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("SİL", color = danger,
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            }
        }
    }
}

@Composable
private fun StatLabel(icon: ImageVector, text: String) {
    val theme = LocalAppTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = theme.text2.copy(0.6f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = theme.text2.copy(0.75f),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusBanner(icon: ImageVector, color: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color.copy(0.95f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
    }
}
