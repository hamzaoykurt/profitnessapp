@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.cosmibit.profitness.presentation.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected as semSelected
import androidx.compose.ui.unit.em
import com.cosmibit.profitness.core.theme.LocalSinemaHaze
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.core.theme.SinemaChip
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.sinemaGlow
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ModalBottomSheet
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.domain.discover.Difficulty
import com.cosmibit.profitness.presentation.workout.SceneImage
import com.cosmibit.profitness.presentation.workout.Scrims
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.PageAccentBloom
import com.cosmibit.profitness.core.theme.accentTextColor
import com.cosmibit.profitness.core.theme.bg3
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.bg2
import com.cosmibit.profitness.core.theme.bg0
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.exerciseDisplayName
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import com.cosmibit.profitness.core.theme.text2
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.domain.discover.DiscoverSort
import com.cosmibit.profitness.domain.discover.MySharedProgram
import com.cosmibit.profitness.domain.discover.SharedProgram
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.presentation.components.AppToast
import com.cosmibit.profitness.presentation.components.AppToastData
import com.cosmibit.profitness.presentation.components.AppToastType
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.core.theme.performanceElevatedSurface
import com.cosmibit.profitness.core.theme.performanceSignatureSurface
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

    var showMenu by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedProgram != null) {
        selectedProgram = null
    }
    BackHandler(enabled = selectedProgram == null && selected == DiscoverTab.Programs && programsSub != ProgramsSubTab.Community) {
        programsSub = ProgramsSubTab.Community
    }

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

    val toastTheme = LocalAppTheme.current
    LaunchedEffect(state.shareResult) {
        state.shareResult?.let { result ->
            currentToast = AppToastData(
                message = when (result) {
                    is ShareResult.Success -> toastTheme.t("Program topluluk akışına eklendi", "Program added to the community feed")
                    is ShareResult.Error   -> toastTheme.t("Paylaşım başarısız: ${result.msg}", "Share failed: ${result.msg}")
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
                    is ApplyResult.Success -> toastTheme.t("Program planına uygulandı", "Program applied to your plan")
                    is ApplyResult.Error   -> toastTheme.t("Uygulanamadı: ${result.msg}", "Could not apply: ${result.msg}")
                },
                type = if (result is ApplyResult.Success) AppToastType.Success else AppToastType.Error
            )
            viewModel.consumeApplyResult()
        }
    }
    LaunchedEffect(state.myActionMsg) {
        state.myActionMsg?.let { msg ->
            val localized = when {
                msg == "Paylaşım yayından kaldırıldı" -> toastTheme.t(msg, "Share unpublished")
                msg.startsWith("Silme başarısız: ") ->
                    toastTheme.t(msg, "Delete failed: " + msg.removePrefix("Silme başarısız: "))
                else -> msg
            }
            currentToast = AppToastData(message = localized, type = AppToastType.Info)
            viewModel.consumeMyActionMsg()
        }
    }

    val theme = LocalAppTheme.current
    val darkTheme = remember(theme) { theme.copy(isDark = true) }   // Keşfet her zaman koyu foto sayfaları
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val controlsTop = statusTop + 20.dp + timerExtraPad
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      CompositionLocalProvider(LocalAppTheme provides darkTheme) {
        selectedProgram?.let { program ->
            val live = state.items.firstOrNull { it.id == program.id }
                ?: state.savedItems.firstOrNull { it.id == program.id } ?: program
            val appliedByHash = live.contentHash != null && live.contentHash in myProgramHashes
            val appliedLocalId = state.appliedProgramMap[live.id]
            val appliedFallback = appliedLocalId != null &&
                appliedLocalId !in state.localDeletingProgramIds && myProgramIds.contains(appliedLocalId)
            SharedProgramDetailScreen(
                program = live,
                bottomPadding = bottomPadding,
                isApplying = live.id in state.applyingProgramIds,
                isApplied = live.isAppliedByMe || appliedByHash || appliedFallback,
                onLike = { viewModel.toggleLike(live.id) },
                onSave = { viewModel.toggleSave(live.id) },
                onApply = { viewModel.applyProgram(live.id) },
                onBack = { selectedProgram = null }
            )
        } ?: Box(Modifier.fillMaxSize()) {
            val listTop = controlsTop + 54.dp + 12.dp
            when (selected) {
                DiscoverTab.Programs -> when (programsSub) {
                    ProgramsSubTab.Community -> SinemaProgramsPager(
                        state = state, myProgramIds = myProgramIds, myProgramHashes = myProgramHashes,
                        bottomPadding = bottomPadding, topOffset = controlsTop,
                        onLike = viewModel::toggleLike, onSave = viewModel::toggleSave, onApply = viewModel::applyProgram,
                        onOpenDetails = { selectedProgram = it }, onLoadMore = viewModel::loadMore, onRefresh = viewModel::refresh
                    )
                    ProgramsSubTab.Saved -> Box(Modifier.fillMaxSize().padding(top = listTop)) {
                        SavedProgramsList(
                            state = state, myProgramIds = myProgramIds, myProgramHashes = myProgramHashes,
                            bottomPadding = bottomPadding, onBack = { programsSub = ProgramsSubTab.Community },
                            onLike = viewModel::toggleLike, onSave = viewModel::toggleSave, onApply = viewModel::applyProgram,
                            onOpenDetails = { selectedProgram = it }, onLoadMore = viewModel::loadMoreSaved, onRefresh = viewModel::refresh
                        )
                    }
                    ProgramsSubTab.Mine -> Box(Modifier.fillMaxSize().padding(top = listTop)) {
                        MySharedProgramsList(
                            items = state.myShared, isLoading = state.myLoading, isRefreshing = state.isRefreshing,
                            deleteInFlight = state.myDeleteInFlight, bottomPadding = bottomPadding,
                            onBack = { programsSub = ProgramsSubTab.Community },
                            onDelete = viewModel::deleteShared, onRefresh = viewModel::refresh,
                            onStartShare = { showShareSheet = true }
                        )
                    }
                }
                DiscoverTab.Friends -> Box(Modifier.fillMaxSize()) {
                    com.cosmibit.profitness.presentation.friends.FriendsTab(bottomPadding = bottomPadding, timerExtraPad = 0.dp, topPadding = controlsTop + 54.dp + 12.dp)
                }
                DiscoverTab.Challenges -> Box(Modifier.fillMaxSize().padding(top = controlsTop + 54.dp + 12.dp)) {
                    com.cosmibit.profitness.presentation.challenges.ChallengesTab(bottomPadding = bottomPadding, sort = state.sort, timerExtraPad = 0.dp)
                }
            }
            DiscoverTopControls(
                selected = selected, onSelect = { selected = it },
                programsSub = programsSub, onOpenMenu = { showMenu = true },
                topOffset = controlsTop
            )
        }
      }

        // ── In-app notification toast ──────────────────────────────────────
        AppToast(
            toast = currentToast,
            onDismiss = { currentToast = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showMenu) {
        CompositionLocalProvider(LocalAppTheme provides darkTheme) {
            DiscoverMenuSheet(
                programsSub = programsSub, sort = state.sort,
                savedCount = state.savedItems.size, mineCount = state.myShared.size,
                onDismiss = { showMenu = false },
                onFeed = { sort -> programsSub = ProgramsSubTab.Community; viewModel.changeSort(sort); showMenu = false },
                onSub = { sub -> programsSub = sub; showMenu = false }
            )
        }
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
    val responsive = rememberResponsiveLayoutInfo()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = responsive.horizontalPadding)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text       = theme.t("Keşfet", "Discover"),
                color      = theme.text0,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    val responsive = rememberResponsiveLayoutInfo()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .heightIn(min = responsive.compactChipMinHeight)
            .clip(shape)
            .background(if (selected) accent.copy(0.18f) else theme.bg2)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) accent else theme.text2.copy(0.6f),
            modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (selected) theme.accentTextColor else theme.text2,
            fontSize = if (responsive.isLargeFont) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DiscoverTabBar(
    selected: DiscoverTab,
    onSelect: (DiscoverTab) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = responsive.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(if (responsive.isSmallPhone) 8.dp else 10.dp)
    ) {
        DiscoverTabPill(
            label    = theme.t("Programlar", "Programs"),
            icon     = Icons.Rounded.FitnessCenter,
            selected = selected == DiscoverTab.Programs,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Programs) }
        )
        DiscoverTabPill(
            label    = theme.t("Topluluk", "Community"),
            icon     = Icons.Rounded.Person,
            selected = selected == DiscoverTab.Friends,
            accent   = accent,
            modifier = Modifier.weight(1f),
            onClick  = { onSelect(DiscoverTab.Friends) }
        )
        DiscoverTabPill(
            label    = theme.t("Challenge", "Challenges"),
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
    val responsive = rememberResponsiveLayoutInfo()
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "discover_tab_scale"
    )
    Row(
        modifier = modifier
            .heightIn(min = if (responsive.isLargeFont) 48.dp else 44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 1.5.dp.toPx() else 0f
            }
            .clip(shape)
            .background(if (selected) theme.bg2 else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = if (responsive.isSmallPhone) 5.dp else 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) accent else theme.text2.copy(0.55f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) theme.text0 else theme.text1,
            fontSize = if (responsive.isLargeFont) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
                        top = 16.dp, bottom = bottomPadding + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
    val accent = theme.effectiveAccentColor
    val responsive = rememberResponsiveLayoutInfo()
    val shape = RoundedCornerShape(24.dp)
    val stackActions = responsive.isSmallPhone || responsive.isLargeFont
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "shared_program_card_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 184.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .sinemaCard(theme, shape)
            .clickable(interactionSource = interaction, indication = null) { onOpenDetails() }
            .padding(20.dp)
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
                    maxLines   = 1,
                    overflow = TextOverflow.Ellipsis
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
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Title + description
        Text(
            text       = program.title,
            color      = theme.text0,
            fontSize   = if (responsive.isLargeFont) 20.sp else 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines   = if (responsive.isLargeFont) 3 else 2,
            lineHeight = 23.sp,
            overflow = TextOverflow.Ellipsis
        )
        if (!program.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text     = program.description,
                color    = theme.text2.copy(0.75f),
                fontSize = 13.sp,
                maxLines = if (responsive.isLargeFont) 4 else 3,
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Tags
        if (program.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                program.tags.take(4).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Sinema.line(theme))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("#$tag", color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Actions row
        val applyText = when {
            isApplied -> theme.t("UYGULANDI", "APPLIED")
            isApplying -> theme.t("UYGULANIYOR", "APPLYING")
            else -> theme.t("UYGULA", "APPLY")
        }
        val chips: @Composable () -> Unit = {
            ActionChip(
                icon = if (program.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                count = program.likesCount,
                active = program.isLikedByMe,
                activeColor = Color(0xFFEF476F),
                onClick = onLike
            )
            Spacer(Modifier.width(12.dp))
            ActionChip(
                icon = if (program.isSavedByMe) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                count = program.savesCount,
                active = program.isSavedByMe,
                activeColor = accent,
                onClick = onSave
            )
            Spacer(Modifier.width(12.dp))
            ActionChip(
                icon = Icons.Rounded.CloudDownload,
                count = program.downloadsCount,
                active = false,
                activeColor = accent,
                onClick = {}
            )
        }
        if (stackActions) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) { chips() }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    ApplyButton(
                        onClick = onApply,
                        text = applyText,
                        enabled = !isApplying && !isApplied,
                        isLoading = isApplying,
                        applied = isApplied
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                chips()
                Spacer(Modifier.weight(1f))
                ApplyButton(
                    onClick = onApply,
                    text = applyText,
                    enabled = !isApplying && !isApplied,
                    isLoading = isApplying,
                    applied = isApplied
                )
            }
        }
    }
}

@Composable
private fun SharedProgramDetailScreen(
    program: SharedProgram,
    bottomPadding: Dp,
    isApplying: Boolean,
    isApplied: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val detail = remember(program.id, program.programData) { program.toDetailPlan() }
    val goBottom = (bottomPadding - 12.dp).coerceAtLeast(0.dp)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = goBottom + 56.dp + 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SharedProgramDetailHero(program, onLike, onSave) }
            if (!program.description.isNullOrBlank()) {
                item {
                    Text(
                        text = program.description,
                        color = Color.White.copy(0.78f), fontSize = 14.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            if (detail.days.isEmpty()) {
                item { EmptyProgramDetailCard() }
            } else {
                items(detail.days, key = { it.index }) { day -> SharedProgramDayCard(day = day) }
            }
        }

        SinemaCapsuleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            description = theme.t("Geri", "Back"),
            onClick = onBack, selected = false, size = 48.dp,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp)
        )

        // Sabit UYGULA: arkası yumuşak geçiş
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Black.copy(0.85f), 1f to Color.Black)
                )
            )
            SinemaGoButton(
                label = when {
                    isApplied -> theme.t("UYGULANDI", "APPLIED")
                    isApplying -> theme.t("UYGULANIYOR", "APPLYING")
                    else -> theme.t("UYGULA", "APPLY")
                },
                icon = if (isApplied) SinemaIcons.Check else SinemaIcons.Bolt,
                onClick = { if (!isApplying && !isApplied) onApply() },
                dim = isApplying,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 40.dp, bottom = goBottom)
            )
        }
    }
}

@Composable
private fun SharedProgramDetailHero(program: SharedProgram, onLike: () -> Unit, onSave: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    Box(Modifier.fillMaxWidth()) {
        SceneImage(
            url = categoryImageUrl(program.tags.firstOrNull().orEmpty()),
            modifier = Modifier.matchParentSize(),
            requestWidthPx = 1080, requestHeightPx = 1080, fallbackGradient = true
        )
        Scrims(Modifier.matchParentSize())
        Column(
            Modifier.fillMaxWidth().heightIn(min = 500.dp).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    program.tags.firstOrNull()?.sinemaUpper(theme).orEmpty(), style = Sinema.Label, color = accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Text(program.creatorName, style = Sinema.Label, color = Color.White.copy(0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                program.title, style = Sinema.Title.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold), color = Color.White,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
            )
            program.daysPerWeek?.let { d ->
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text("$d", style = Sinema.Display.copy(fontSize = 96.sp), color = Color.White, maxLines = 1)
                    Text(
                        theme.t("gün / hafta", "days / week"), style = Sinema.Title.copy(fontSize = 22.sp),
                        color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 10.dp, bottom = 12.dp)
                    )
                }
            }
            val diffLabel = when (program.difficulty) {
                Difficulty.BEGINNER -> theme.t("Başlangıç", "Beginner")
                Difficulty.INTERMEDIATE -> theme.t("Orta", "Intermediate")
                Difficulty.ADVANCED -> theme.t("İleri", "Advanced")
                null -> null
            }
            val weeks = program.durationWeeks?.let { theme.t("$it hafta", "$it weeks") }
            val meta = listOfNotNull(weeks, diffLabel).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta.sinemaUpper(theme), style = Sinema.Label, color = Color.White.copy(0.78f), modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SinemaCapsule(selected = program.isLikedByMe, onClick = onLike, height = 48.dp) { fg ->
                    Icon(if (program.isLikedByMe) Icons.Rounded.Favorite else SinemaIcons.Heart, theme.t("Beğen", "Like"), tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(countFormat.format(program.likesCount), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                SinemaCapsule(selected = program.isSavedByMe, onClick = onSave, height = 48.dp) { fg ->
                    Icon(if (program.isSavedByMe) Icons.Rounded.Bookmark else SinemaIcons.Bookmark, theme.t("Kaydet", "Save"), tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(countFormat.format(program.savesCount), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                SinemaCapsule(height = 48.dp) { fg ->
                    Icon(SinemaIcons.Download, theme.t("Uygulanma sayısı", "Applied count"), tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(countFormat.format(program.downloadsCount), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(32.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }
}

@Composable
private fun SharedProgramDayCard(day: SharedProgramDetailDay) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = day.index.toString(), style = Sinema.Metric.copy(fontSize = 44.sp),
                color = Color.White.copy(0.55f), maxLines = 1
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, lineHeight = 22.sp
                )
                if (day.isRestDay) {
                    Text(theme.t("DİNLENME", "REST"), style = Sinema.Label, color = Color.White.copy(0.65f), modifier = Modifier.padding(top = 2.dp))
                } else if (day.exercises.isNotEmpty()) {
                    Text(
                        theme.t("${day.exercises.size} hareket", "${day.exercises.size} exercises").sinemaUpper(theme),
                        style = Sinema.Label, color = Color.White.copy(0.65f), modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        if (day.exercises.isEmpty()) {
            Text(
                text = if (day.isRestDay) {
                    theme.t("Bu gün dinlenme için ayrılmış.", "This day is reserved for rest.")
                } else {
                    theme.t("Bu gün için hareket bulunamadı.", "No exercises found for this day.")
                },
                color = Color.White.copy(0.65f), fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp)
            )
        } else {
            Spacer(Modifier.height(14.dp))
            day.exercises.forEachIndexed { i, exercise ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
                SharedProgramExerciseRow(exercise = exercise)
            }
        }
    }
}

@Composable
private fun SharedProgramExerciseRow(exercise: SharedProgramDetailExercise) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = theme.exerciseDisplayName(exercise.name), color = Color.White, fontSize = 15.sp,
            fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 19.sp, modifier = Modifier.weight(1f)
        )
        val badges = exercise.badges(theme)
        if (badges.isNotEmpty()) {
            Text(
                badges.joinToString(" · "), style = Sinema.Label, color = Color.White.copy(0.75f),
                maxLines = 1, modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun EmptyProgramDetailCard() {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            theme.t("Program içeriği bulunamadı", "Program content not found"),
            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.t(
                "Bu paylaşımda görüntülenecek gün ve hareket snapshot'ı yok.",
                "This share does not include a day and exercise snapshot to display."
            ),
            color = Color.White.copy(0.65f), fontSize = 13.sp, textAlign = TextAlign.Center
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
    fun badges(theme: com.cosmibit.profitness.core.theme.AppThemeState): List<String> = buildList {
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
    val responsive = rememberResponsiveLayoutInfo()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = if (pressed) 0.92f else 1f
                scaleY = if (pressed) 0.92f else 1f
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Sinema.line(theme))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = if (responsive.isLargeFont) 9.dp else 8.dp),
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
            fontSize = if (responsive.isLargeFont) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
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
    SinemaChip(
        active = !applied,
        onClick = onClick,
        enabled = enabled,
        height = 44.dp
    ) { fg ->
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = fg)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
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
            .background(theme.bg2),
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(stiffness = 820f, dampingRatio = 0.72f),
        label = "share_fab_scale"
    )
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 3.dp.toPx() else 0f
            }
            .shadow(
                if (pressed) 2.dp else 12.dp,
                shape,
                ambientColor = accent.copy(0.28f),
                spotColor = accent.copy(0.40f)
            )
            .clip(shape)
            .background(accent)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Share, theme.t("Programı paylaş", "Share program"),
            tint = theme.effectiveOnAccentColor, modifier = Modifier.size(24.dp))
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
    val responsive = rememberResponsiveLayoutInfo()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = responsive.horizontalPadding)
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    val responsive = rememberResponsiveLayoutInfo()
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = if (responsive.isLargeFont) 52.dp else 40.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.98f else 1f
                scaleY = if (pressed) 0.98f else 1f
            }
            .then(
                if (selected) Modifier.clip(shape).background(theme.bg3)
                else Modifier.clip(shape).background(theme.bg2)
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 5.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) theme.accentTextColor else theme.text2,
            fontSize = if (responsive.isLargeFont) 8.sp else 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MY SHARED PROGRAMS LIST — sync + delete yönetimi
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
//  SİNEMA: fotoğraflı liste kartı (Kaydedilenler + Paylaşımlarım ortak)
// ═══════════════════════════════════════════════════════════════════════════

/** Kas grubu foto + alt karartma + etiket/başlık + gün/hafta; altta `content` (sayaçlar, eylem). */
@Composable
private fun SinemaPhotoCard(
    title: String,
    tag: String?,
    rightLabel: String?,
    daysPerWeek: Int?,
    durationWeeks: Int?,
    difficulty: Difficulty?,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth()
            .sinemaCard(theme, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
    ) {
        SceneImage(
            url = categoryImageUrl(tag.orEmpty()),
            modifier = Modifier.matchParentSize(),
            requestWidthPx = 960, requestHeightPx = 960, fallbackGradient = true
        )
        Box(Modifier.matchParentSize().background(Sinema.BottomScrim))
        Column(
            Modifier.fillMaxWidth().heightIn(min = 340.dp).padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tag?.sinemaUpper(theme).orEmpty(), style = Sinema.Label, color = accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (!rightLabel.isNullOrBlank()) {
                    Text(rightLabel, style = Sinema.Label, color = Color.White.copy(0.70f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                title, style = Sinema.Title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            daysPerWeek?.let { d ->
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Text("$d", style = Sinema.Display.copy(fontSize = 64.sp), color = Color.White, maxLines = 1)
                    Text(
                        theme.t("gün / hafta", "days / week"), style = Sinema.Title.copy(fontSize = 18.sp),
                        color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )
                }
            }
            val diffLabel = when (difficulty) {
                Difficulty.BEGINNER -> theme.t("Başlangıç", "Beginner")
                Difficulty.INTERMEDIATE -> theme.t("Orta", "Intermediate")
                Difficulty.ADVANCED -> theme.t("İleri", "Advanced")
                null -> null
            }
            val weeks = durationWeeks?.let { theme.t("$it hafta", "$it weeks") }
            val meta = listOfNotNull(weeks, diffLabel).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta.sinemaUpper(theme), style = Sinema.Label, color = Color.White.copy(0.78f), modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PhotoCount(icon: ImageVector, iconOn: ImageVector?, n: Int, on: Boolean, desc: String, click: (() -> Unit)?) {
    val accent = LocalAppTheme.current.effectiveAccentColor
    val c = if (on) accent else Color.White.copy(0.85f)
    Row(
        Modifier.height(44.dp)
            .then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
            .semantics { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (on && iconOn != null) iconOn else icon, null, tint = c, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(countFormat.format(n), color = c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavedProgramPhotoCard(
    program: SharedProgram,
    isApplying: Boolean,
    isApplied: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onOpen: () -> Unit
) {
    val theme = LocalAppTheme.current
    SinemaPhotoCard(
        title = program.title, tag = program.tags.firstOrNull(), rightLabel = program.creatorName,
        daysPerWeek = program.daysPerWeek, durationWeeks = program.durationWeeks, difficulty = program.difficulty,
        onClick = onOpen
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PhotoCount(SinemaIcons.Heart, Icons.Rounded.Favorite, program.likesCount, program.isLikedByMe, theme.t("Beğen", "Like"), onLike)
            PhotoCount(SinemaIcons.Bookmark, Icons.Rounded.Bookmark, program.savesCount, program.isSavedByMe, theme.t("Kaydet / kaldır", "Save / remove"), onSave)
            PhotoCount(SinemaIcons.Download, null, program.downloadsCount, false, theme.t("Uygulanma sayısı", "Applied count"), null)
        }
        Spacer(Modifier.height(6.dp))
        SinemaGoButton(
            label = when {
                isApplied -> theme.t("UYGULANDI", "APPLIED")
                isApplying -> theme.t("UYGULANIYOR", "APPLYING")
                else -> theme.t("UYGULA", "APPLY")
            },
            icon = if (isApplied) SinemaIcons.Check else SinemaIcons.Bolt,
            onClick = { if (!isApplying && !isApplied) onApply() },
            dim = isApplying
        )
    }
}

/** Alt liste başlığı: geri (akışa dön) + başlık + isteğe bağlı sağ düğme. */
@Composable
private fun SubListHeader(
    title: String,
    count: Int,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SinemaCapsuleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            description = theme.t("Akışa dön", "Back to feed"),
            onClick = onBack, selected = false, size = 48.dp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (count > 0) "$title  $count" else title, color = Color.White,
            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Davetkar boş durum (Sinema dili): nötr ikon diski, başlık, açıklama, tek ana eylem. */
@Composable
private fun SinemaEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit
) {
    val theme = LocalAppTheme.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).sinemaCard(theme, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Color.White.copy(0.65f), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        SinemaGoButton(label = actionLabel, icon = actionIcon, onClick = onAction, modifier = Modifier.widthIn(max = 280.dp))
    }
}

@Composable
private fun SavedProgramsList(
    state           : DiscoverProgramsState,
    myProgramIds    : ImmutableSet<String>,
    myProgramHashes : ImmutableSet<String>,
    bottomPadding   : Dp,
    onBack          : () -> Unit,
    onLike          : (String) -> Unit,
    onSave          : (String) -> Unit,
    onApply         : (String) -> Unit,
    onOpenDetails   : (SharedProgram) -> Unit,
    onLoadMore      : () -> Unit,
    onRefresh       : () -> Unit
) {
    val theme = LocalAppTheme.current
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

    Column(Modifier.fillMaxSize()) {
        SubListHeader(theme.t("Kaydedilenler", "Saved"), state.savedItems.size, onBack)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                state.savedItems.isEmpty() && state.savedLoading -> LoadingState()
                state.savedItems.isEmpty() && state.savedError != null -> ErrorState(state.savedError, onRefresh)
                state.savedItems.isEmpty() -> SinemaEmptyState(
                    icon = SinemaIcons.Bookmark,
                    title = theme.t("Henüz kaydettiğin program yok", "No saved programs yet"),
                    body = theme.t(
                        "Toplulukta beğendiğin programların yer imine dokun, burada toplansın.",
                        "Tap the bookmark on programs you like in the community and they will gather here."
                    ),
                    actionLabel = theme.t("PROGRAMLARA GÖZ AT", "BROWSE PROGRAMS"),
                    actionIcon = SinemaIcons.Compass,
                    onAction = onBack
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.savedItems, key = { it.id }) { program ->
                        val isAppliedByHash = program.contentHash != null && program.contentHash in myProgramHashes
                        val appliedLocalProgramId = state.appliedProgramMap[program.id]
                        val isAppliedFallback = appliedLocalProgramId != null &&
                            appliedLocalProgramId !in state.localDeletingProgramIds &&
                            myProgramIds.contains(appliedLocalProgramId)
                        SavedProgramPhotoCard(
                            program = program,
                            isApplying = program.id in state.applyingProgramIds,
                            isApplied = program.isAppliedByMe || isAppliedByHash || isAppliedFallback,
                            onLike = { onLike(program.id) },
                            onSave = { onSave(program.id) },
                            onApply = { onApply(program.id) },
                            onOpen = { onOpenDetails(program) }
                        )
                    }
                    if (state.savedLoading && state.savedItems.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MySharedProgramsList(
    items          : ImmutableList<MySharedProgram>,
    isLoading      : Boolean,
    isRefreshing   : Boolean,
    deleteInFlight : ImmutableSet<String>,
    bottomPadding  : Dp,
    onBack         : () -> Unit,
    onDelete       : (String) -> Unit,
    onRefresh      : () -> Unit,
    onStartShare   : () -> Unit
) {
    val theme = LocalAppTheme.current
    Column(Modifier.fillMaxSize()) {
        SubListHeader(theme.t("Paylaşımlarım", "My shares"), items.size, onBack) {
            if (items.isNotEmpty()) {
                SinemaCapsuleIconButton(
                    icon = SinemaIcons.Plus,
                    description = theme.t("Program paylaş", "Share program"),
                    onClick = onStartShare, selected = true, size = 48.dp
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                items.isEmpty() && isLoading -> LoadingState()
                items.isEmpty() -> SinemaEmptyState(
                    icon = SinemaIcons.Plus,
                    title = theme.t("Henüz bir program paylaşmadın", "You have not shared a program yet"),
                    body = theme.t(
                        "Programlarından birini topluluğa aç; beğeni ve kayıtları buradan takip et.",
                        "Share one of your programs with the community and follow its likes and saves here."
                    ),
                    actionLabel = theme.t("PROGRAM PAYLAŞ", "SHARE PROGRAM"),
                    actionIcon = Icons.Rounded.Share,
                    onAction = onStartShare
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        MySharedCard(
                            item = item,
                            isDeleting = item.id in deleteInFlight,
                            onDelete = { onDelete(item.id) }
                        )
                    }
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
    val danger = Color(0xFFEF476F)
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

    SinemaPhotoCard(
        title = item.title, tag = item.tags.firstOrNull(), rightLabel = null,
        daysPerWeek = item.daysPerWeek, durationWeeks = item.durationWeeks, difficulty = item.difficulty,
        onClick = null
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PhotoCount(SinemaIcons.Heart, null, item.likesCount, false, theme.t("Beğeni", "Likes"), null)
            PhotoCount(SinemaIcons.Bookmark, null, item.savesCount, false, theme.t("Kaydedenler", "Saves"), null)
            PhotoCount(SinemaIcons.Download, null, item.downloadsCount, false, theme.t("Uygulanma sayısı", "Applied count"), null)
        }
        StatusBanner(
            icon = if (item.sourceExists) Icons.Rounded.FitnessCenter else Icons.Rounded.Warning,
            color = Color.White.copy(0.75f),
            text = when {
                !item.sourceExists -> theme.t(
                    "Kaynak program silinmiş. Paylaşım snapshot olarak korunuyor.",
                    "The source program was deleted. The share is preserved as a snapshot."
                )
                item.isOutOfSync -> theme.t(
                    "Kaynak programın değişmiş. Bu paylaşım eski sürüm olarak kalıyor.",
                    "The source program changed. This share stays as the old version."
                )
                else -> theme.t(
                    "Paylaşım sabit bir snapshot olarak korunuyor.",
                    "The share is preserved as a fixed snapshot."
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        SinemaCapsule(
            Modifier.fillMaxWidth(), onClick = { showDeleteDialog = true }, enabled = !isDeleting,
            height = 52.dp, horizontalArrangement = Arrangement.Center
        ) { fg ->
            if (isDeleting) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = fg)
            } else {
                Icon(Icons.Rounded.DeleteOutline, null, tint = fg, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(theme.t("Yayından kaldır", "Unpublish"), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  SİNEMA: tam ekran foto sayfaları (çizim vKesfet)
// ═══════════════════════════════════════════════════════════════════════════

private val countFormat: java.text.NumberFormat get() = java.text.NumberFormat.getIntegerInstance(Locale("tr"))

/** Tek sade üst yapı: büyük cam segment (Programlar · Topluluk · Challenge) + kayan vurgu taşı; Programlar'da sağda tek cam yer imi düğmesi (menü). */
@Composable
private fun DiscoverTopControls(
    selected: DiscoverTab,
    onSelect: (DiscoverTab) -> Unit,
    programsSub: ProgramsSubTab,
    onOpenMenu: () -> Unit,
    topOffset: Dp
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    val motion = rememberSinemaMotionOn()
    val tabs = listOf(
        DiscoverTab.Programs to theme.t("Programlar", "Programs"),
        DiscoverTab.Friends to theme.t("Topluluk", "Community"),
        DiscoverTab.Challenges to "Challenge"
    )
    Row(
        Modifier.fillMaxWidth().padding(top = topOffset).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            Modifier.weight(1f).height(54.dp)
                .sinemaCapsuleGlass(RoundedCornerShape(27.dp), blur = 16.dp)
                .padding(3.dp)
        ) {
            val tabW = maxWidth / 3
            val idx = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
            val x by animateDpAsState(
                tabW * idx,
                if (motion) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) else snap(),
                label = "discover_stone"
            )
            val stoneShape = RoundedCornerShape(24.dp)
            Box(
                Modifier.offset(x = x).width(tabW).fillMaxHeight()
                    .sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, stoneShape)
                    .clip(stoneShape)
                    .background(accent)
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEach { (tab, label) ->
                    val on = tab == selected
                    val fg by animateColorAsState(if (on) onAccent else Color.White.copy(0.78f), tween(180), label = "discover_tab_fg")
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clip(stoneShape)
                            .semantics { semSelected = on }
                            .clickable(role = Role.Tab) { onSelect(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }
        if (selected == DiscoverTab.Programs) {
            Spacer(Modifier.width(10.dp))
            SinemaCapsuleIconButton(
                icon = SinemaIcons.Bookmark,
                description = theme.t("Kaydedilenler ve paylaşımlarım", "Saved and my shares"),
                onClick = onOpenMenu,
                selected = programsSub != ProgramsSubTab.Community,
                size = 48.dp
            )
        }
    }
}

/** Yer imi menüsü: Yeni / Trend (akış), Kaydedilenler, Paylaşımlarım. */
@Composable
private fun DiscoverMenuSheet(
    programsSub: ProgramsSubTab,
    sort: DiscoverSort,
    savedCount: Int,
    mineCount: Int,
    onDismiss: () -> Unit,
    onFeed: (DiscoverSort) -> Unit,
    onSub: (ProgramsSubTab) -> Unit
) {
    val theme = LocalAppTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Sinema.surface(theme),
        contentColor = Color.White
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                theme.t("Programlar", "Programs"), color = Color.White,
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 4.dp)
            )
            @Composable
            fun row(active: Boolean, icon: ImageVector, label: String, count: Int?, onClick: () -> Unit) {
                SinemaCapsule(
                    Modifier.fillMaxWidth(), selected = active, onClick = onClick, height = 52.dp
                ) { fg ->
                    Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1)
                    if (count != null) Text(count.toString(), color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            val feed = programsSub == ProgramsSubTab.Community
            row(feed && sort == DiscoverSort.NEWEST, SinemaIcons.Compass, theme.t("Yeni", "New"), null) { onFeed(DiscoverSort.NEWEST) }
            row(feed && sort == DiscoverSort.TRENDING, Icons.Rounded.Whatshot, theme.t("Trend", "Trending"), null) { onFeed(DiscoverSort.TRENDING) }
            row(programsSub == ProgramsSubTab.Saved, SinemaIcons.Bookmark, theme.t("Kaydedilenler", "Saved"), savedCount) { onSub(ProgramsSubTab.Saved) }
            row(programsSub == ProgramsSubTab.Mine, SinemaIcons.Plus, theme.t("Paylaşımlarım", "My shares"), mineCount) { onSub(ProgramsSubTab.Mine) }
        }
    }
}

@Composable
private fun SinemaProgramsPager(
    state: DiscoverProgramsState,
    myProgramIds: ImmutableSet<String>,
    myProgramHashes: ImmutableSet<String>,
    bottomPadding: Dp,
    topOffset: Dp,
    onLike: (String) -> Unit,
    onSave: (String) -> Unit,
    onApply: (String) -> Unit,
    onOpenDetails: (SharedProgram) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit
) {
    val items = state.items
    when {
        items.isEmpty() && state.isLoading -> Box(Modifier.fillMaxSize().padding(top = topOffset + 110.dp)) { LoadingState() }
        items.isEmpty() && state.error != null -> Box(Modifier.fillMaxSize().padding(top = topOffset + 110.dp)) { ErrorState(state.error, onRefresh) }
        items.isEmpty() -> Box(Modifier.fillMaxSize().padding(top = topOffset + 110.dp)) { EmptyFeedState() }
        else -> {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState { items.size }
            val settled = pagerState.settledPage
            LaunchedEffect(settled, items.size, state.canLoadMore) {
                if (state.canLoadMore && !state.isLoading && settled >= items.size - 3) onLoadMore()
            }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                androidx.compose.foundation.pager.VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { items[it].id }
                ) { page ->
                    val program = items[page]
                    val isAppliedByHash = program.contentHash != null && program.contentHash in myProgramHashes
                    val appliedLocalId = state.appliedProgramMap[program.id]
                    val isAppliedFallback = appliedLocalId != null &&
                        appliedLocalId !in state.localDeletingProgramIds && myProgramIds.contains(appliedLocalId)
                    ProgramPage(
                        program = program, index = page, total = items.size,
                        isApplying = program.id in state.applyingProgramIds,
                        isApplied = program.isAppliedByMe || isAppliedByHash || isAppliedFallback,
                        isSettled = page == settled,
                        contentBottom = (bottomPadding - 12.dp).coerceAtLeast(0.dp),
                        onLike = { onLike(program.id) }, onSave = { onSave(program.id) },
                        onApply = { onApply(program.id) }, onOpen = { onOpenDetails(program) }
                    )
                }
                // Üst segment çubukları (çizim `.bars`): en çok 12 parça
                val segs = items.size.coerceAtMost(12)
                val cur = if (items.size <= 12) pagerState.currentPage else (pagerState.currentPage * 12 / items.size)
                val accent = LocalAppTheme.current.effectiveAccentColor
                Row(
                    Modifier.fillMaxWidth().padding(top = topOffset - 12.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(segs) { i ->
                        val f by animateFloatAsState(if (i <= cur) 1f else 0f, tween(450), label = "dseg")
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(0.30f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramPage(
    program: SharedProgram,
    index: Int,
    total: Int,
    isApplying: Boolean,
    isApplied: Boolean,
    isSettled: Boolean,
    contentBottom: Dp,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onOpen: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val haze = remember { dev.chrisbanes.haze.HazeState() }
    val motion = rememberSinemaMotionOn()
    val kb by rememberInfiniteTransition(label = "dkb").animateFloat(
        1f, if (isSettled && motion) 1.08f else 1f,
        infiniteRepeatable(tween(Sinema.KenBurnsMs, easing = LinearEasing), RepeatMode.Reverse), label = "kb"
    )
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val compact = maxHeight < 700.dp
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            SceneImage(
                url = categoryImageUrl(program.tags.firstOrNull().orEmpty()),
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = kb; scaleY = kb },
                requestWidthPx = 1080, requestHeightPx = 1920, fallbackGradient = true
            )
            Scrims()
        }
        CompositionLocalProvider(LocalSinemaHaze provides haze) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = contentBottom),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row {
                    Text(
                        program.tags.firstOrNull()?.sinemaUpper(theme).orEmpty(), style = Sinema.Label, color = accent,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text("%02d / %02d".format(index + 1, total), style = Sinema.Label, color = Color.White.copy(0.70f))
                }
                Row(
                    Modifier.padding(top = 10.dp, bottom = 8.dp).clickable(role = Role.Button, onClick = onOpen),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!program.creatorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(program.creatorAvatarUrl).crossfade(true).build(),
                                contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                program.creatorName.take(1).uppercase(), color = theme.effectiveOnAccentColor,
                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(program.creatorName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    program.title, style = Sinema.Title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)
                )
                program.daysPerWeek?.let { d ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text("$d", style = Sinema.Display.copy(fontSize = if (compact) 80.sp else 104.sp), color = Color.White, maxLines = 1)
                        Text(
                            theme.t("gün / hafta", "days / week"), style = Sinema.Title.copy(fontSize = 26.sp),
                            color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
                        )
                    }
                }
                val diffLabel = when (program.difficulty) {
                    Difficulty.BEGINNER -> theme.t("Başlangıç", "Beginner")
                    Difficulty.INTERMEDIATE -> theme.t("Orta", "Intermediate")
                    Difficulty.ADVANCED -> theme.t("İleri", "Advanced")
                    null -> null
                }
                val weeks = program.durationWeeks?.let { theme.t("$it hafta", "$it weeks") }
                if (weeks != null || diffLabel != null) {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOfNotNull(weeks, diffLabel).forEach { t ->
                            Box(
                                Modifier.height(28.dp).sinemaCapsuleGlass(RoundedCornerShape(14.dp), blur = 16.dp).padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(t, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, maxLines = 1) }
                        }
                    }
                }
                Row(Modifier.padding(top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    @Composable
                    fun count(icon: ImageVector, iconOn: ImageVector?, n: Int, on: Boolean, desc: String, click: (() -> Unit)?) {
                        val c = if (on) accent else Color.White.copy(0.85f)
                        Row(
                            Modifier.height(40.dp)
                                .then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
                                .semantics { contentDescription = desc },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (on && iconOn != null) iconOn else icon, null, tint = c, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(countFormat.format(n), color = c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    count(SinemaIcons.Heart, Icons.Rounded.Favorite, program.likesCount, program.isLikedByMe, theme.t("Beğen", "Like"), onLike)
                    count(SinemaIcons.Bookmark, Icons.Rounded.Bookmark, program.savesCount, program.isSavedByMe, theme.t("Kaydet", "Save"), onSave)
                    count(SinemaIcons.Download, null, program.downloadsCount, false, theme.t("Uygulanma sayısı", "Applied count"), null)
                }
                Spacer(Modifier.height(8.dp))
                SinemaGoButton(
                    label = when {
                        isApplied -> theme.t("UYGULANDI", "APPLIED")
                        isApplying -> theme.t("UYGULANIYOR", "APPLYING")
                        else -> theme.t("UYGULA", "APPLY")
                    },
                    icon = if (isApplied) SinemaIcons.Check else SinemaIcons.Bolt,
                    onClick = { if (!isApplying && !isApplied) onApply() },
                    dim = isApplying
                )
            }
        }
    }
}
