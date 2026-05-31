@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.avonix.profitness.core.theme.exerciseDisplayName
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.t
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.MySharedProgram
import com.avonix.profitness.domain.discover.SharedProgram
import com.avonix.profitness.presentation.components.AppToast
import com.avonix.profitness.presentation.components.AppToastData
import com.avonix.profitness.presentation.components.AppToastType
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

enum class DiscoverTab { Programs, Friends, Challenges }

/** Programlar sekmesi altındaki iç sekme: Topluluk, kaydedilenler, benim paylaşımlarım. */
private enum class ProgramsSubTab { Community, Saved, Mine }

@Composable
fun DiscoverScreen(
    bottomPadding: Dp,
    timerExtraPad: Dp = 0.dp,
    externalRefreshSignal: Int = 0
) {
    var selected by rememberSaveable { mutableStateOf(DiscoverTab.Programs) }
    var programsSub by rememberSaveable { mutableStateOf(ProgramsSubTab.Community) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    var selectedProgram by remember { mutableStateOf<SharedProgram?>(null) }

    val viewModel: DiscoverViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myPrograms by viewModel.myPrograms.collectAsStateWithLifecycle()
    val myProgramIds = remember(myPrograms) { myPrograms.map { it.id }.toImmutableSet() }
    val sharedProgramIds = remember(state.myShared) {
        state.myShared.mapNotNull { it.originalProgramId }.toImmutableSet()
    }
    val sharedProgramHashes = remember(state.myShared) {
        state.myShared.mapNotNull { it.sharedContentHash }.toImmutableSet()
    }
    /**
     * Hash'leri Room'dan derle — Discover feed'i UYGULA/UYGULANDI çentiğini sunucu flag'i
     * yanında bu yerel set ile de teyit etsin ki kullanıcı bir programı düzenlediğinde
     * (hash değişir) bir sonraki feed pull'unu beklemeden kart anında "UYGULA"ya döner.
     */
    val myProgramHashes = remember(myPrograms) {
        myPrograms.mapNotNull { it.contentHash }.toImmutableSet()
    }

    var currentToast by remember { mutableStateOf<AppToastData?>(null) }

    LaunchedEffect(Unit) {
        delay(120)
        viewModel.initLoad()
    }

    LaunchedEffect(externalRefreshSignal) {
        if (externalRefreshSignal > 0) {
            viewModel.refreshAfterExternalShare()
        }
    }

    LaunchedEffect(state.shareResult) {
        state.shareResult?.let { result ->
            currentToast = AppToastData(
                message = when (result) {
                    is ShareResult.Success -> "Program topluluk akışına eklendi"
                    is ShareResult.Error   -> "Paylaşım başarısız: ${result.msg}"
                },
                type = if (result is ShareResult.Success) AppToastType.Success else AppToastType.Error
            )
            viewModel.consumeShareResult()
        }
    }
    LaunchedEffect(state.applyResult) {
        state.applyResult?.let { result ->
            currentToast = AppToastData(
                message = when (result) {
                    is ApplyResult.Success -> "Program planına uygulandı"
                    is ApplyResult.Error   -> "Uygulanamadı: ${result.msg}"
                },
                type = if (result is ApplyResult.Success) AppToastType.Success else AppToastType.Error
            )
            viewModel.consumeApplyResult()
        }
    }
    LaunchedEffect(state.myActionMsg) {
        state.myActionMsg?.let { msg ->
            currentToast = AppToastData(message = msg, type = AppToastType.Info)
            viewModel.consumeMyActionMsg()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        selectedProgram?.let { program ->
            SharedProgramDetailScreen(
                program = program,
                bottomPadding = bottomPadding,
                onBack = { selectedProgram = null }
            )
        } ?: Column(
            modifier = Modifier.fillMaxSize()
        ) {
            DiscoverHeader(
                isTrending = state.sort == DiscoverSort.TRENDING,
                onToggleSort = {
                    viewModel.changeSort(
                        if (state.sort == DiscoverSort.TRENDING) DiscoverSort.NEWEST
                        else DiscoverSort.TRENDING
                    )
                }
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
                            savedCount = state.savedItems.size,
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
                                     state           = state,
                                     myProgramIds    = myProgramIds,
                                     myProgramHashes = myProgramHashes,
                                     bottomPadding   = bottomPadding,
                                     onLike          = viewModel::toggleLike,
                                     onSave          = viewModel::toggleSave,
                                     onApply         = viewModel::applyProgram,
                                     onOpenDetails   = { selectedProgram = it },
                                     onLoadMore      = viewModel::loadMore,
                                     onRefresh       = viewModel::refresh
                                )
                                ProgramsSubTab.Saved -> SavedProgramsList(
                                    state           = state,
                                    myProgramIds    = myProgramIds,
                                    myProgramHashes = myProgramHashes,
                                    bottomPadding   = bottomPadding,
                                    onLike          = viewModel::toggleLike,
                                    onSave          = viewModel::toggleSave,
                                    onApply         = viewModel::applyProgram,
                                    onOpenDetails   = { selectedProgram = it },
                                    onLoadMore      = viewModel::loadMoreSaved,
                                    onRefresh       = viewModel::refresh
                                )
                                ProgramsSubTab.Mine -> MySharedProgramsList(
                                    items           = state.myShared,
                                    isLoading       = state.myLoading,
                                    isRefreshing    = state.isRefreshing,
                                    deleteInFlight  = state.myDeleteInFlight,
                                    bottomPadding   = bottomPadding,
                                    onDelete        = viewModel::deleteShared,
                                    onRefresh       = viewModel::refresh,
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
                        sort = state.sort,
                        timerExtraPad = 0.dp
                    )
                }
            }
        }

        // ── Share FAB — sadece Programs tab'ında görünür ───────────────────
        if (selected == DiscoverTab.Programs && selectedProgram == null) {
            ShareFab(
                onClick = { showShareSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = bottomPadding + 12.dp)
            )
        }

        // ── In-app notification toast ──────────────────────────────────────
        AppToast(
            toast = currentToast,
            onDismiss = { currentToast = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showShareSheet) {
        ShareProgramSheet(
            programs  = myPrograms,
            alreadySharedProgramIds = sharedProgramIds,
            alreadySharedProgramHashes = sharedProgramHashes,
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
    onToggleSort: () -> Unit
) {
    val theme = LocalAppTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 92.dp)
        ) {
            Text(
                text       = theme.t("KEŞFET", "DISCOVER"),
                color      = theme.text0,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = theme.t("Topluluk programları ve challenge'lar", "Community programs and challenges"),
                color    = theme.text2.copy(alpha = 0.7f),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
        // Sort toggle
        SmallIconChip(
            icon     = Icons.Rounded.Whatshot,
            label    = if (isTrending) theme.t("TREND", "TRENDING") else theme.t("YENİ", "NEW"),
            selected = isTrending,
            onClick  = onToggleSort
        )
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
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DiscoverTabPill(
            label    = theme.t("PROGRAMLAR", "PROGRAMS"),
            icon     = Icons.Rounded.FitnessCenter,
            selected = selected == DiscoverTab.Programs,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Programs) }
        )
        DiscoverTabPill(
            label    = theme.t("ARKADAŞLAR", "FRIENDS"),
            icon     = Icons.Rounded.Person,
            selected = selected == DiscoverTab.Friends,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Friends) }
        )
        DiscoverTabPill(
            label    = theme.t("CHALLENGE", "CHALLENGES"),
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
            .height(58.dp)
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
    state           : DiscoverProgramsState,
    myProgramIds    : ImmutableSet<String>,
    myProgramHashes : ImmutableSet<String>,
    bottomPadding   : Dp,
    onLike          : (String) -> Unit,
    onSave          : (String) -> Unit,
    onApply         : (String) -> Unit,
    onOpenDetails   : (SharedProgram) -> Unit,
    onLoadMore      : () -> Unit,
    onRefresh       : () -> Unit
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

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.items.isEmpty() && state.isLoading -> LoadingState()
            state.items.isEmpty() && state.error != null -> ErrorState(state.error, onRefresh)
            state.items.isEmpty() -> EmptyFeedState()
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 12.dp, bottom = bottomPadding + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.items, key = { it.id }) { program ->
                        // Senior approach: content-addressable. UYGULANDI iff caller has a local
                        // program with matching content hash. Server flag is canonical; local-hash
                        // check provides instant feedback (after Room sync) without waiting for
                        // the next feed pull. Düzenleme yapılırsa hash değişir → çentik düşer.
                        //
                        // Geriye dönük güvenlik ağı: hash henüz dolmamış (NULL) eski Room satırları
                        // için, kullanıcı az önce uygula'ya basmış ve programId hâlâ duruyorsa da
                        // UYGULANDI kabul et (geçici, ilk sync'te kalkar).
                        val isAppliedByHash =
                            program.contentHash != null && program.contentHash in myProgramHashes
                        val appliedLocalProgramId = state.appliedProgramMap[program.id]
                        val isAppliedFallback = appliedLocalProgramId != null &&
                            appliedLocalProgramId !in state.localDeletingProgramIds &&
                            myProgramIds.contains(appliedLocalProgramId)
                        val isApplied = program.isAppliedByMe || isAppliedByHash || isAppliedFallback
                        SharedProgramCard(
                            program    = program,
                            isApplying = program.id in state.applyingProgramIds,
                            isApplied  = isApplied,
                            onLike     = { onLike(program.id) },
                            onSave     = { onSave(program.id) },
                            onApply    = { onApply(program.id) },
                            onOpenDetails = { onOpenDetails(program) }
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
            } // else block
        }
    }
}

@Composable
private fun SavedProgramsList(
    state           : DiscoverProgramsState,
    myProgramIds    : ImmutableSet<String>,
    myProgramHashes : ImmutableSet<String>,
    bottomPadding   : Dp,
    onLike          : (String) -> Unit,
    onSave          : (String) -> Unit,
    onApply         : (String) -> Unit,
    onOpenDetails   : (SharedProgram) -> Unit,
    onLoadMore      : () -> Unit,
    onRefresh       : () -> Unit
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.savedCanLoadMore) {
        if (shouldLoadMore && state.savedCanLoadMore && !state.savedLoading) onLoadMore()
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.savedItems.isEmpty() && state.savedLoading -> LoadingState()
            state.savedItems.isEmpty() && state.savedError != null -> ErrorState(state.savedError, onRefresh)
            state.savedItems.isEmpty() -> EmptySavedState()
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 12.dp, bottom = bottomPadding + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.savedItems, key = { it.id }) { program ->
                        val isAppliedByHash =
                            program.contentHash != null && program.contentHash in myProgramHashes
                        val appliedLocalProgramId = state.appliedProgramMap[program.id]
                        val isAppliedFallback = appliedLocalProgramId != null &&
                            appliedLocalProgramId !in state.localDeletingProgramIds &&
                            myProgramIds.contains(appliedLocalProgramId)
                        val isApplied = program.isAppliedByMe || isAppliedByHash || isAppliedFallback
                        SharedProgramCard(
                            program    = program,
                            isApplying = program.id in state.applyingProgramIds,
                            isApplied  = isApplied,
                            onLike     = { onLike(program.id) },
                            onSave     = { onSave(program.id) },
                            onApply    = { onApply(program.id) },
                            onOpenDetails = { onOpenDetails(program) }
                        )
                    }
                    if (state.savedLoading && state.savedItems.isNotEmpty()) {
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
    }
}

@Composable
private fun SharedProgramCard(
    program   : SharedProgram,
    isApplying: Boolean,
    isApplied : Boolean,
    onLike    : () -> Unit,
    onSave    : () -> Unit,
    onApply   : () -> Unit,
    onOpenDetails: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 184.dp)
            .glassCard(accent, theme, shape)
            .clickable { onOpenDetails() }
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
        Spacer(Modifier.height(14.dp))

        // Title + description
        Text(
            text       = program.title,
            color      = theme.text0,
            fontSize   = 19.sp,
            fontWeight = FontWeight.Black,
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
                    isApplied -> theme.t("UYGULANDI", "APPLIED")
                    isApplying -> theme.t("UYGULANIYOR", "APPLYING")
                    else -> theme.t("UYGULA", "APPLY")
                },
                enabled = !isApplying && !isApplied,
                isLoading = isApplying,
                applied = isApplied
            )
        }
    }
}

@Composable
private fun SharedProgramDetailScreen(
    program: SharedProgram,
    bottomPadding: Dp,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val detail = remember(program.id, program.programData) { program.toDetailPlan() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bg1, Color.Black)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(theme.bg2.copy(0.45f))
                    .border(1.dp, theme.stroke.copy(0.35f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = theme.text0, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = theme.t("Program içeriği", "Program content"),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = program.title,
                    color = theme.text0,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    lineHeight = 28.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 4.dp,
                bottom = bottomPadding + 84.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SharedProgramDetailHeader(program = program, dayCount = detail.days.size)
            }

            if (detail.days.isEmpty()) {
                item { EmptyProgramDetailCard() }
            } else {
                items(detail.days, key = { it.index }) { day ->
                    SharedProgramDayCard(day = day)
                }
            }
        }
    }
}

@Composable
private fun SharedProgramDetailHeader(program: SharedProgram, dayCount: Int) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.bg2.copy(0.55f), theme.bg1.copy(0.72f))))
            .border(1.dp, theme.stroke.copy(0.35f), shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(program.creatorName, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = buildList {
                        if (dayCount > 0) add(theme.t("$dayCount gün", "$dayCount days"))
                        program.daysPerWeek?.let { add(theme.t("$it gün/hafta", "$it days/week")) }
                        program.durationWeeks?.let { add(theme.t("$it hafta", "$it weeks")) }
                    }.ifEmpty { listOf(theme.t("Paylaşılan program", "Shared program")) }.joinToString(" · "),
                    color = theme.text2.copy(0.7f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        if (!program.description.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = program.description,
                color = theme.text2.copy(0.82f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SharedProgramDayCard(day: SharedProgramDetailDay) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.bg2.copy(0.70f), theme.bg1.copy(0.60f))))
            .border(1.dp, theme.stroke.copy(0.35f), shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.index.toString(),
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = day.title,
                color = theme.text0,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            if (day.isRestDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = theme.text2.copy(0.7f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(theme.t("Dinlenme", "Rest"), color = theme.text2.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(theme.stroke.copy(0.25f))
        )

        if (day.exercises.isEmpty()) {
            Text(
                text = if (day.isRestDay) {
                    theme.t("Bu gün dinlenme için ayrılmış.", "This day is reserved for rest.")
                } else {
                    theme.t("Bu gün için hareket bulunamadı.", "No exercises found for this day.")
                },
                color = theme.text2.copy(0.75f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                day.exercises.forEach { exercise ->
                    SharedProgramExerciseRow(exercise = exercise)
                }
            }
        }
    }
}

@Composable
private fun SharedProgramExerciseRow(exercise: SharedProgramDetailExercise) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.bg2.copy(0.62f), theme.bg1.copy(0.44f))))
            .border(1.dp, theme.stroke.copy(0.25f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = theme.exerciseDisplayName(exercise.name),
                color = theme.text0,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 18.sp
            )
            val badges = exercise.badges(theme)
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEachIndexed { index, badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(accent.copy(if (index == 0) 0.16f else 0.10f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badge,
                                color = accent.copy(if (index == 0) 1f else 0.82f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramDetailCard() {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg2.copy(0.50f))
            .border(1.dp, theme.stroke.copy(0.35f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(theme.t("Program içeriği bulunamadı", "Program content not found"), color = theme.text0, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.t(
                "Bu paylaşımda görüntülenecek gün ve hareket snapshot'ı yok.",
                "This share does not include a day and exercise snapshot to display."
            ),
            color = theme.text2.copy(0.72f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

private data class SharedProgramDetailPlan(
    val days: List<SharedProgramDetailDay>
)

private data class SharedProgramDetailDay(
    val index: Int,
    val title: String,
    val isRestDay: Boolean,
    val exercises: List<SharedProgramDetailExercise>
)

private data class SharedProgramDetailExercise(
    val name: String,
    val sets: Int?,
    val reps: Int?,
    val restSeconds: Int?,
    val targetDurationSeconds: Int?,
    val targetDistanceMeters: Float?,
    val orderIndex: Int
) {
    fun badges(theme: com.avonix.profitness.core.theme.AppThemeState): List<String> = buildList {
        targetDurationSeconds?.takeIf { it > 0 }?.let { add(formatDurationBadge(it)) }
        targetDistanceMeters?.takeIf { it > 0f }?.let { add(formatDistanceBadge(it)) }
        if (isEmpty()) {
            sets?.takeIf { it > 0 }?.let { add("$it SET") }
            reps?.takeIf { it > 0 }?.let { add(theme.t("$it TEK", "$it REP")) }
        }
    }
}

private fun SharedProgram.toDetailPlan(): SharedProgramDetailPlan {
    val root = programData.asObjectOrNull()
    val dayElements = root?.array("days") ?: programData.asArrayOrNull() ?: emptyList()
    val days = dayElements.mapIndexedNotNull { index, element ->
        val day = element.asObjectOrNull() ?: return@mapIndexedNotNull null
        val dayIndex = day.int("day_index")
            ?: day.int("dayIndex")
            ?: day.int("index")
            ?: index + 1
        val exercises = (day.array("exercises") ?: emptyList())
            .mapIndexedNotNull { exerciseIndex, exerciseElement ->
                val ex = exerciseElement.asObjectOrNull() ?: return@mapIndexedNotNull null
                SharedProgramDetailExercise(
                    name = ex.string("exercise_name")
                        ?: ex.string("exerciseName")
                        ?: ex.string("name")
                        ?: ex.obj("exercise")?.string("name")
                        ?: "Exercise",
                    sets = ex.int("sets"),
                    reps = ex.int("reps"),
                    restSeconds = ex.int("rest_seconds") ?: ex.int("restSeconds"),
                    targetDurationSeconds = ex.int("target_duration_seconds")
                        ?: ex.int("targetDurationSeconds"),
                    targetDistanceMeters = ex.float("target_distance_meters")
                        ?: ex.float("targetDistanceMeters"),
                    orderIndex = ex.int("order_index")
                        ?: ex.int("orderIndex")
                        ?: exerciseIndex
                )
            }
            .sortedBy { it.orderIndex }
        SharedProgramDetailDay(
            index = dayIndex,
            title = day.string("title")
                ?: day.string("name")
                ?: "Day $dayIndex",
            isRestDay = day.bool("is_rest_day")
                ?: day.bool("isRestDay")
                ?: false,
            exercises = exercises
        )
    }.sortedBy { it.index }
    return SharedProgramDetailPlan(days = days)
}

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject.obj(key: String): JsonObject? = get(key).asObjectOrNull()

private fun JsonObject.array(key: String): JsonArray? = get(key).asArrayOrNull()

private fun JsonObject.string(key: String): String? =
    get(key)?.runCatchingPrimitive { content.takeIf { it.isNotBlank() } }

private fun JsonObject.int(key: String): Int? =
    get(key)?.runCatchingPrimitive { intOrNull }

private fun JsonObject.float(key: String): Float? =
    get(key)?.runCatchingPrimitive { floatOrNull }

private fun JsonObject.bool(key: String): Boolean? =
    get(key)?.runCatchingPrimitive { booleanOrNull }

private inline fun <T> JsonElement.runCatchingPrimitive(block: kotlinx.serialization.json.JsonPrimitive.() -> T?): T? =
    runCatching { jsonPrimitive.block() }.getOrNull()

private fun formatDurationBadge(seconds: Int): String =
    if (seconds >= 60 && seconds % 60 == 0) "${seconds / 60} DK" else "$seconds SN"

private fun formatDistanceBadge(meters: Float): String =
    if (meters >= 1000f) "${trimMetric(meters / 1000f)} KM" else "${trimMetric(meters)} M"

private fun trimMetric(value: Float): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")

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
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) activeColor.copy(0.14f) else theme.bg2.copy(0.55f))
            .border(
                1.dp,
                if (active) activeColor.copy(0.26f) else theme.stroke.copy(0.35f),
                RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
    text: String,
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
            .heightIn(min = 44.dp)
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
        Text(theme.t("Yüklenemedi", "Could not load"), color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(msg, color = theme.text2.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        ApplyButton(
            onClick = onRetry,
            text = theme.t("TEKRAR DENE", "TRY AGAIN")
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
        Text(theme.t("Henüz paylaşılan program yok", "No shared programs yet"),
            color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.t(
                "İlk paylaşan sen ol. Sağ alttaki + butonuna dokun.",
                "Be the first to share. Tap the + button at the bottom right."
            ),
            color = theme.text2.copy(0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptySavedState() {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(theme.t("Henüz kaydedilmiş program yok", "No saved programs yet"),
            color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.t(
                "Toplulukta beğendiğin programların yer imine dokununca burada görünür.",
                "Programs you bookmark in the community will appear here."
            ),
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
        Text(theme.t("Grup Challenge'ları", "Group Challenges"), color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = theme.t(
                "Arkadaşlarınla ya da toplulukla meydan okumaya katıl, ilerleme takip et, ödül kazan.",
                "Join challenges with friends or the community, track progress, and earn rewards."
            ),
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
            Text(theme.t("ÇOK YAKINDA", "COMING SOON"), color = MaterialTheme.colorScheme.primary,
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
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent, accent.copy(0.7f))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Share, theme.t("Programı paylaş", "Share program"),
            tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PROGRAMS SUB-TAB: Topluluk vs. Benim paylaşımlarım
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgramsSubTabBar(
    selected: ProgramsSubTab,
    savedCount: Int,
    mineCount: Int,
    onSelect: (ProgramsSubTab) -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SubTabChip(
            label = theme.t("TOPLULUK", "COMMUNITY"),
            selected = selected == ProgramsSubTab.Community,
            onClick = { onSelect(ProgramsSubTab.Community) },
            modifier = Modifier.weight(1f)
        )
        SubTabChip(
            label = if (savedCount > 0) {
                theme.t("KAYDEDİLENLER · $savedCount", "SAVED · $savedCount")
            } else {
                theme.t("KAYDEDİLENLER", "SAVED")
            },
            selected = selected == ProgramsSubTab.Saved,
            onClick = { onSelect(ProgramsSubTab.Saved) },
            modifier = Modifier.weight(1f)
        )
        SubTabChip(
            label = if (mineCount > 0) {
                theme.t("PAYLAŞIMLARIM · $mineCount", "MY SHARES · $mineCount")
            } else {
                theme.t("PAYLAŞIMLARIM", "MY SHARES")
            },
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
            .height(40.dp)
            .clip(shape)
            .background(if (selected) accent.copy(0.18f) else theme.bg2.copy(0.4f))
            .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke.copy(0.3f), shape)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) accent else theme.text2.copy(0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MY SHARED PROGRAMS LIST — sync + delete yönetimi
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MySharedProgramsList(
    items          : ImmutableList<MySharedProgram>,
    isLoading      : Boolean,
    isRefreshing   : Boolean,
    deleteInFlight : ImmutableSet<String>,
    bottomPadding  : Dp,
    onDelete       : (String) -> Unit,
    onRefresh      : () -> Unit,
    onStartShare   : () -> Unit
) {
    val theme = LocalAppTheme.current

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            items.isEmpty() && isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(theme.t("Henüz bir program paylaşmadın", "You have not shared a program yet"),
                    color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = theme.t(
                        "Programlarından birini topluluğa paylaşmak için dokun.",
                        "Tap to share one of your programs with the community."
                    ),
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
                    Text(theme.t("PROGRAM PAYLAŞ", "SHARE PROGRAM"), color = Color.Black,
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
                        isDeleting   = item.id in deleteInFlight,
                        onDelete     = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MySharedCard(
    item       : MySharedProgram,
    isDeleting : Boolean,
    onDelete   : () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val danger = Color(0xFFEF476F)
    val shape = RoundedCornerShape(18.dp)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = theme.bg1,
            title = { Text(theme.t("Yayından kaldır?", "Unpublish?"), color = theme.text0, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    theme.t(
                        "\"${item.title}\" topluluk akışından kaldırılacak. Daha önce kaydedenler program snapshot'ına erişmeye devam eder.",
                        "\"${item.title}\" will be removed from the community feed. People who saved it earlier can still access the program snapshot."
                    ),
                    color = theme.text2, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(theme.t("Yayından kaldır", "Unpublish"), color = danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(theme.t("İptal", "Cancel"), color = theme.text2)
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
                text  = theme.t(
                    "Kaynak program silinmiş. Paylaşım snapshot olarak korunuyor.",
                    "The source program was deleted. The share is preserved as a snapshot."
                )
            )
            item.isOutOfSync -> StatusBanner(
                icon = Icons.Rounded.FitnessCenter,
                color = accent,
                text  = theme.t(
                    "Kaynak programın değişmiş. Bu paylaşım eski sürüm olarak kalıyor.",
                    "The source program changed. This share stays as the old version."
                )
            )
            else -> StatusBanner(
                icon = Icons.Rounded.FitnessCenter,
                color = accent,
                text  = theme.t(
                    "Paylaşım sabit bir snapshot olarak korunuyor.",
                    "The share is preserved as a fixed snapshot."
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Aksiyon: yayından kaldırma (paylaşımlar artık snapshot — senkron yok)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(danger.copy(0.12f))
                .border(1.dp, danger.copy(0.35f), RoundedCornerShape(12.dp))
                .clickable(enabled = !isDeleting) { showDeleteDialog = true }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDeleting) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = danger)
            } else {
                Icon(Icons.Rounded.DeleteOutline, null, tint = danger, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(theme.t("YAYINDAN KALDIR", "UNPUBLISH"), color = danger,
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
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
