package com.avonix.profitness.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.core.ui.rememberResponsiveLayoutInfo
import com.avonix.profitness.presentation.aicoach.AICoachScreen
import com.avonix.profitness.presentation.aicoach.AICoachViewModel
import com.avonix.profitness.presentation.discover.DiscoverScreen
import com.avonix.profitness.presentation.discover.DiscoverViewModel
import com.avonix.profitness.presentation.profile.AchievementsDetailScreen
import com.avonix.profitness.presentation.profile.EditProfileScreen
import com.avonix.profitness.presentation.profile.ExerciseProgressionScreen
import com.avonix.profitness.presentation.profile.PerformanceDetailScreen
import com.avonix.profitness.presentation.profile.ProfileScreen
import com.avonix.profitness.presentation.profile.ProfileViewModel
import com.avonix.profitness.presentation.weight.WeightTrackingScreen
import com.avonix.profitness.presentation.program.ProgramBuilderScreen
import com.avonix.profitness.presentation.program.ProgramShareViewModel
import com.avonix.profitness.presentation.program.ProgramViewModel
import com.avonix.profitness.presentation.store.StoreScreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.presentation.components.DynamicIslandTimer
import com.avonix.profitness.presentation.workout.RestTimerState
import com.avonix.profitness.presentation.workout.WorkoutScreen
import com.avonix.profitness.presentation.workout.WorkoutViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

@Immutable
sealed class DashboardTab(val route: String, val icon: ImageVector, val label: String) {
    object Workout  : DashboardTab("workout",  Icons.Rounded.FitnessCenter, "FORGE")
    object Program  : DashboardTab("program",  Icons.Rounded.CalendarMonth, "PLAN")
    object AICoach  : DashboardTab("ai_coach", Icons.Rounded.AutoAwesome,   "ORACLE")
    object Discover : DashboardTab("discover", Icons.Rounded.Explore,       "KEŞFET")
    object Profile  : DashboardTab("profile",  Icons.Rounded.Person,        "USER")
}

private val NavIndicatorEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val NavIndicatorEaseInOut = CubicBezierEasing(0.45f, 0f, 0.2f, 1f)

private val ALL_TABS = persistentListOf(
    DashboardTab.Workout, DashboardTab.Program, DashboardTab.AICoach,
    DashboardTab.Discover, DashboardTab.Profile
)

private data class NavItemLayout(val x: Float, val width: Float) {
    val center: Float get() = x + width / 2f
}

private const val FIRST_TAB_CONTENT_DELAY_MS = 16L

@Composable
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit, onLogout: () -> Unit = {}) {
    var selectedTabRoute        by rememberSaveable { mutableStateOf(DashboardTab.Workout.route) }
    val selectedTab             = ALL_TABS.firstOrNull { it.route == selectedTabRoute } ?: DashboardTab.Workout
    val composedTabs            = remember { mutableStateListOf<DashboardTab>(DashboardTab.Workout) }
    val readyTabs               = remember { mutableStateListOf<DashboardTab>(DashboardTab.Workout) }
    var warmupStage             by remember { mutableIntStateOf(0) }
    var programInitialMode      by remember { mutableStateOf<com.avonix.profitness.presentation.program.BuilderMode>(com.avonix.profitness.presentation.program.BuilderMode.Choose) }
    val workoutViewModel: WorkoutViewModel = hiltViewModel()
    var showPerformanceDetail       by remember { mutableStateOf(false) }
    var showAchievementsDetail      by remember { mutableStateOf(false) }
    var showEditProfile             by remember { mutableStateOf(false) }
    var showWeightTracking          by remember { mutableStateOf(false) }
    var showExerciseProgression     by remember { mutableStateOf(false) }
    var showStore                   by remember { mutableStateOf(false) }
    var showLeaderboard             by remember { mutableStateOf(false) }
    var leaderboardInitialTab       by remember {
        mutableStateOf(com.avonix.profitness.presentation.leaderboard.LeaderboardTab.Xp)
    }

    BackHandler(
        enabled = showStore ||
            showExerciseProgression ||
            showWeightTracking ||
            showEditProfile ||
            showLeaderboard ||
            showAchievementsDetail ||
            showPerformanceDetail
    ) {
        when {
            showStore               -> showStore = false
            showExerciseProgression -> showExerciseProgression = false
            showWeightTracking      -> showWeightTracking = false
            showEditProfile         -> showEditProfile = false
            showLeaderboard         -> showLeaderboard = false
            showAchievementsDetail  -> showAchievementsDetail = false
            showPerformanceDetail   -> showPerformanceDetail = false
        }
    }

    // Nav yüksekliği 78 → 92 dp (item padding 10/12 → 14/16, icon 20 → 24)
    val responsive = rememberResponsiveLayoutInfo()
    val useNavRail = responsive.useNavigationRail
    val navBarHeight = 92.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = if (useNavRail) 24.dp + navBarBottom else navBarHeight + navBarBottom + 8.dp
    val haptic = LocalHapticFeedback.current

    val restTimer by workoutViewModel.restTimer.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(120)
        warmupStage = 1
        delay(180)
        warmupStage = 2
        delay(220)
        warmupStage = 3
        delay(260)
        warmupStage = 4
    }
    DashboardViewModelWarmup(stage = warmupStage)

    // Timer aktifken diğer ekranlardaki içerik aşağı kayar
    val statusBarPad  = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val timerActive   = restTimer.isRunning || restTimer.isPaused || restTimer.isDone
    val timerExtraPadTarget by remember(timerActive, statusBarPad) {
        derivedStateOf {
            if (timerActive && selectedTab != DashboardTab.Workout) {
                statusBarPad + 60.dp
            } else {
                0.dp
            }
        }
    }
    val showGlobalTimer by remember {
        derivedStateOf { selectedTab != DashboardTab.Workout }
    }
    val timerExtraPad by animateDpAsState(
        targetValue   = timerExtraPadTarget,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "dash_timer_pad"
    )
    val contentPadWithTimer = contentPad

    // ── Swipe gesture — Orientation.Horizontal doesn't compete with vertical scrollers
    var swipeAccum by remember { mutableStateOf(0f) }
    val selectedTabIndex by remember {
        derivedStateOf { ALL_TABS.indexOf(selectedTab) }
    }
    val draggableState = rememberDraggableState { delta -> swipeAccum += delta }
    val swipeModifier = Modifier.draggable(
        state       = draggableState,
        orientation = Orientation.Horizontal,
        onDragStarted = { swipeAccum = 0f },
        onDragStopped = { velocity ->
            val curIdx = selectedTabIndex
            // Fast fling (velocity) OR slow-but-wide drag both trigger tab change
            val byVelocity = abs(velocity) > 500f
            val byDistance = abs(swipeAccum) > 120f
            if (byVelocity || byDistance) {
                val goNext = if (byVelocity) velocity < 0f else swipeAccum < 0f
                if (goNext && curIdx < ALL_TABS.lastIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTabRoute = ALL_TABS[curIdx + 1].route
                } else if (!goNext && curIdx > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTabRoute = ALL_TABS[curIdx - 1].route
                }
            }
            swipeAccum = 0f
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (useNavRail) responsive.navRailWidth else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = {
                val isFirstCompositionForTarget = targetState !in composedTabs
                if (isFirstCompositionForTarget) {
                    fadeIn(tween(60)) togetherWith fadeOut(tween(45))
                } else {
                    val fromIdx = ALL_TABS.indexOf(initialState)
                    val toIdx   = ALL_TABS.indexOf(targetState)
                    // Pure slide+fade — NO scale. Scale forces GPU layer changes every frame
                    // on the full composable tree which is the #1 cause of jank during transitions.
                    // Short durations keep the double-render window minimal.
                    val easeOut    = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    val easeIn     = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)
                    val enterSlide = tween<IntOffset>(180, easing = easeOut)
                    val enterFade  = tween<Float>(150, easing = easeOut)
                    val exitSlide  = tween<IntOffset>(120, easing = easeIn)
                    val exitFade   = tween<Float>(90)
                    if (toIdx > fromIdx) {
                        (slideInHorizontally(enterSlide) { it } + fadeIn(enterFade)) togetherWith
                        (slideOutHorizontally(exitSlide) { -it / 4 } + fadeOut(exitFade))
                    } else {
                        (slideInHorizontally(enterSlide) { -it } + fadeIn(enterFade)) togetherWith
                        (slideOutHorizontally(exitSlide) { it / 4 } + fadeOut(exitFade))
                    }
                }
            },
            modifier = Modifier
                .widthIn(max = responsive.contentMaxWidth)
                .fillMaxSize()
                .then(swipeModifier),
            label = "tab_slide"
        ) { tab ->
            LaunchedEffect(tab) {
                if (tab !in composedTabs) composedTabs.add(tab)
                if (tab !in readyTabs) {
                    delay(FIRST_TAB_CONTENT_DELAY_MS)
                    if (tab !in readyTabs) readyTabs.add(tab)
                }
            }

            if (tab !in readyTabs) {
                TabWarmupSurface()
            } else when (tab) {
                DashboardTab.Workout -> WorkoutScreen(
                    bottomPadding = contentPad,
                    viewModel = workoutViewModel,
                    onNavigateToAIBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.AI
                        selectedTabRoute = DashboardTab.Program.route
                    },
                    onNavigateToManualBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.Manual
                        selectedTabRoute = DashboardTab.Program.route
                    },
                    onNavigateToStore = { showStore = true }
                )
                DashboardTab.Program -> {
                    val capturedMode = programInitialMode
                    // Modu kullandıktan sonra sıfırla (geri dönüşte Choose'a dönmesi için)
                    LaunchedEffect(capturedMode) {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.Choose
                    }
                    ProgramBuilderScreen(
                        initialMode       = capturedMode,
                        timerExtraPad     = timerExtraPad,
                        onNavigateToStore = { showStore = true }
                    )
                }
                DashboardTab.AICoach -> AICoachScreen(
                    bottomPadding      = contentPadWithTimer,
                    onNavigateToStore  = { showStore = true }
                )
                DashboardTab.Discover -> DiscoverScreen(
                    bottomPadding = contentPad,
                    timerExtraPad = timerExtraPad
                )
                DashboardTab.Profile -> ProfileScreen(
                    onThemeChange                   = onThemeChange,
                    onNavigateToPerformance         = { showPerformanceDetail = true },
                    onNavigateToAchievements        = { showAchievementsDetail = true },
                    onNavigateToWeightTracking      = { showWeightTracking = true },
                    onNavigateToExerciseProgression = { showExerciseProgression = true },
                    onNavigateToLeaderboard         = { tab ->
                        leaderboardInitialTab = tab
                        showLeaderboard = true
                    },
                    onLogout                        = onLogout,
                    onEditProfile                   = { showEditProfile = true },
                    onNavigateToStore               = { showStore = true },
                    timerExtraPad                   = timerExtraPad
                )
            }
        }
        }

        if (useNavRail) {
            AppNavRail(
                tabs     = ALL_TABS,
                selected = { selectedTab },
                onSelect = { tab -> if (tab != selectedTab) selectedTabRoute = tab.route },
                modifier = Modifier.align(Alignment.CenterStart).zIndex(100f)
            )
        } else {
            AppNavBar(
                tabs     = ALL_TABS,
                selected = { selectedTab },
                onSelect = { tab -> if (tab != selectedTab) selectedTabRoute = tab.route },
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f)
            )
        }

        // ── Global Dynamic Island — Workout dışı tablarda üstte göster ─────
        if (showGlobalTimer) {
            DynamicIslandTimer(
                timer     = restTimer,
                topOffset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                onStop    = { workoutViewModel.stopVisibleTimer() },
                onTogglePause = {
                    if (restTimer.isPaused) workoutViewModel.resumeVisibleTimer()
                    else workoutViewModel.pauseVisibleTimer()
                },
                onDismiss = { workoutViewModel.dismissVisibleTimer() }
            )
        }

        // Shared spring spec for full-screen overlays — feels instant yet smooth
        val overlayEnterSpec = spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness    = Spring.StiffnessMediumLow
        )
        val overlayExitSpec = spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness    = Spring.StiffnessMedium
        )

        // ── Performance Detail Overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = showPerformanceDetail,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            PerformanceDetailScreen(
                onBack = { showPerformanceDetail = false },
                onNavigateToWeightTracking = {
                    showPerformanceDetail = false
                    showWeightTracking = true
                },
                onNavigateToExerciseProgression = {
                    showPerformanceDetail = false
                    showExerciseProgression = true
                }
            )
        }

        // ── Achievements Detail Overlay ─────────────────────────────────
        AnimatedVisibility(
            visible = showAchievementsDetail,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            AchievementsDetailScreen(onBack = { showAchievementsDetail = false })
        }

        // ── Leaderboard Overlay ─────────────────────────────────────────
        AnimatedVisibility(
            visible  = showLeaderboard,
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            com.avonix.profitness.presentation.leaderboard.LeaderboardScreen(
                onBack     = { showLeaderboard = false },
                initialTab = leaderboardInitialTab
            )
        }

        // ── Edit Profile Overlay ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showEditProfile,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            EditProfileScreen(onBack = { showEditProfile = false })
        }

        // ── Weight Tracking Overlay ───────────────────────────────────────────
        AnimatedVisibility(
            visible  = showWeightTracking,
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            WeightTrackingScreen(
                onBack            = { showWeightTracking = false },
                onNavigateToStore = { showStore = true }
            )
        }

        // ── Exercise Progression Overlay ─────────────────────────────────────
        AnimatedVisibility(
            visible  = showExerciseProgression,
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            ExerciseProgressionScreen(
                onBack            = { showExerciseProgression = false },
                onNavigateToStore = { showStore = true }
            )
        }

        // ── Store Overlay ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showStore,
            enter    = slideInVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200)),
            exit     = slideOutVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { it } + fadeOut(tween(150)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(300f)
        ) {
            StoreScreen(onBack = { showStore = false })
        }
    }
}

@Composable
private fun TabWarmupSurface() {
    Box(modifier = Modifier.fillMaxSize())
}

@Composable
private fun DashboardViewModelWarmup(stage: Int) {
    if (stage >= 1) {
        hiltViewModel<ProgramViewModel>()
        hiltViewModel<ProgramShareViewModel>()
    }
    if (stage >= 2) {
        hiltViewModel<AICoachViewModel>()
    }
    if (stage >= 3) {
        hiltViewModel<DiscoverViewModel>()
    }
    if (stage >= 4) {
        hiltViewModel<ProfileViewModel>()
    }
}

// ── Global Rest Timer Banner ───────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
//  APP BACKGROUND
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    // Light mode: much subtler bloom — warm earthy bg doesn't need heavy neon glow
    val radialPeak = if (theme.isDark) 0.16f else 0.06f
    val radialMid  = if (theme.isDark) 0.10f else 0.03f
    val radialEdge = if (theme.isDark) 0.04f else 0.01f
    val sweepPeak  = if (theme.isDark) 0.07f else 0.03f
    val sweepMid   = if (theme.isDark) 0.02f else 0.005f

    Box(modifier = modifier.drawWithCache {
        val radial = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f  to accent.copy(alpha = radialPeak),
                0.30f to accent.copy(alpha = radialMid),
                0.60f to accent.copy(alpha = radialEdge),
                1.0f  to Color.Transparent
            ),
            center = Offset(size.width, 0f),
            radius = size.width * 2.2f
        )
        val sweep = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to accent.copy(alpha = sweepPeak),
                0.5f to accent.copy(alpha = sweepMid),
                1.0f to Color.Transparent
            ),
            start = Offset(size.width, 0f),
            end   = Offset(size.width * 0.3f, size.height)
        )
        onDrawBehind {
            drawRect(theme.bg0)
            drawRect(radial)
            drawRect(sweep)
        }
    })
}

// ═══════════════════════════════════════════════════════════════════════════
//  APP NAV BAR — floating capsule, icon only, accent fill on selected
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AppNavRail(
    tabs    : ImmutableList<DashboardTab>,
    selected: () -> DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayoutInfo()
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val shape  = RoundedCornerShape(32.dp)
    val selectedTab = selected()

    Box(
        modifier = modifier
            .width(responsive.navRailWidth)
            .fillMaxHeight()
            .systemBarsPadding()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(68.dp)
                .shadow(
                    elevation    = 18.dp,
                    shape        = shape,
                    spotColor    = accent.copy(0.20f),
                    ambientColor = Color.Black.copy(0.45f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            theme.bg2.copy(if (theme.isDark) 0.96f else 0.98f),
                            theme.bg1.copy(if (theme.isDark) 0.96f else 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(if (theme.isDark) 0.12f else 0.60f),
                            theme.stroke.copy(0.40f)
                        )
                    ),
                    shape = shape
                )
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val itemShape = RoundedCornerShape(24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .then(
                            if (isSelected)
                                Modifier.background(
                                    Brush.linearGradient(
                                        listOf(accent.copy(0.30f), accent.copy(0.13f), Color.White.copy(0.06f))
                                    )
                                )
                            else Modifier
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(accent.copy(0.62f), Color.White.copy(0.20f), accent.copy(0.24f))
                                    ),
                                    shape = itemShape
                                )
                            else Modifier
                        )
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tab)
                        }
                        .padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = tab.icon,
                        contentDescription = tab.label,
                        tint               = if (isSelected) accent else theme.text2.copy(0.45f),
                        modifier           = Modifier.size(23.dp)
                    )
                    Text(
                        text          = tab.label.take(4),
                        color         = if (isSelected) accent else theme.text2.copy(0.55f),
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        maxLines      = 1
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavBar(
    tabs    : ImmutableList<DashboardTab>,
    selected: () -> DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayoutInfo()
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val shape  = RoundedCornerShape(40.dp)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val selectedTab = selected()
    val selectedState by rememberUpdatedState(selectedTab)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemLayouts = remember(tabs) { mutableStateMapOf<DashboardTab, NavItemLayout>() }
    var navWidthPx by remember { mutableStateOf(0f) }
    var dragX by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    fun nearestTabAt(x: Float): DashboardTab? =
        itemLayouts.entries.minByOrNull { (_, layout) -> abs(layout.center - x) }?.key

    val indicatorEaseOut = NavIndicatorEaseOut
    val indicatorEaseInOut = NavIndicatorEaseInOut
    val visualTab = dragX?.let { nearestTabAt(it) } ?: selectedTab
    val visualLayout = itemLayouts[visualTab] ?: itemLayouts[selectedTab]
    val targetWidth = visualLayout?.width ?: 0f
    val collapsedIndicatorWidth = with(density) { (if (responsive.isSmallPhone) 52.dp else 56.dp).toPx() }
    val targetCenter = when {
        isDragging && dragX != null && targetWidth > 0f ->
            dragX!!.coerceIn(targetWidth / 2f, (navWidthPx - targetWidth / 2f).coerceAtLeast(targetWidth / 2f))
        visualLayout != null -> visualLayout.center
        else -> 0f
    }
    val indicatorCenter by animateFloatAsState(
        targetValue = targetCenter,
        animationSpec = if (isDragging) snap() else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "nav_drag_center"
    )
    val indicatorWidthAnim = remember { Animatable(0f) }
    LaunchedEffect(selectedTab, targetWidth, isDragging) {
        if (targetWidth <= 0f) return@LaunchedEffect
        if (isDragging || indicatorWidthAnim.value == 0f) {
            indicatorWidthAnim.snapTo(targetWidth)
            return@LaunchedEffect
        }
        indicatorWidthAnim.animateTo(
            targetValue = collapsedIndicatorWidth.coerceAtMost(targetWidth),
            animationSpec = tween(110, easing = indicatorEaseInOut)
        )
        indicatorWidthAnim.animateTo(
            targetValue = targetWidth,
            animationSpec = tween(220, easing = indicatorEaseOut)
        )
    }
    val indicatorWidth = indicatorWidthAnim.value


    Box(
        modifier        = modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation    = 20.dp,
                    shape        = shape,
                    spotColor    = accent.copy(0.25f),
                    ambientColor = Color.Black.copy(0.55f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            theme.bg2.copy(if (theme.isDark) 0.96f else 0.98f),
                            theme.bg1.copy(if (theme.isDark) 0.96f else 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(if (theme.isDark) 0.12f else 0.60f),
                            theme.stroke.copy(0.40f)
                        )
                    ),
                    shape = shape
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .onSizeChanged { navWidthPx = it.width.toFloat() }
                .pointerInput(tabs) {
                    awaitEachGesture {
                        // Initial pass: child clickable'lardan ÖNCE down eventı yakala
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val pointerId = downChange.id

                        var dragAccum = 0f
                        var gestureDragging = false
                        var lastSentTab = selectedState

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break

                            dragAccum += change.positionChange().x

                            // Parmak yeterince kaydıysa drag moduna gir ve child click'i iptal et
                            if (!gestureDragging && abs(dragAccum) > viewConfiguration.touchSlop) {
                                gestureDragging = true
                                isDragging = true
                            }
                            if (gestureDragging) {
                                val x = change.position.x.coerceIn(0f, navWidthPx)
                                dragX = x
                                nearestTabAt(x)?.let { target ->
                                    if (target != lastSentTab) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectState(target)
                                        lastSentTab = target
                                    }
                                }
                                change.consume()
                            }
                        }

                        isDragging = false
                        dragX = null
                    }
                },
        ) {
            if (indicatorWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((indicatorCenter - indicatorWidth / 2f).roundToInt(), 0) }
                        .width(with(density) { indicatorWidth.toDp() })
                        .height(52.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(0.30f), accent.copy(0.13f), Color.White.copy(0.06f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(accent.copy(0.62f), Color.White.copy(0.20f), accent.copy(0.24f))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(if (responsive.isSmallPhone) 2.dp else 4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    NavCapsuleItem(
                        tab        = tab,
                        isSelected = tab == selectedTab,
                        showSelectedChrome = false,
                        accent     = accent,
                        theme      = theme,
                        showLabel  = !responsive.isSmallPhone,
                        onClick    = { onSelect(tab) },
                        modifier   = Modifier.onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInParent()
                            itemLayouts[tab] = NavItemLayout(position.x, coordinates.size.width.toFloat())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavCapsuleItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    showSelectedChrome: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    showLabel : Boolean,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val selectedGlow by animateFloatAsState(
        targetValue   = if (isSelected && showSelectedChrome) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "item_glow"
    )
    val itemShape = RoundedCornerShape(32.dp)

    Row(
        modifier = modifier
            .width(
                when {
                    showLabel && isSelected -> 124.dp
                    showLabel -> 56.dp
                    else -> 52.dp
                }
            )
            .drawBehind {
                if (selectedGlow > 0f) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.22f * selectedGlow),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx(), 32.dp.toPx())
                    )
                }
            }
            .clip(itemShape)
            .then(
                if (isSelected && showSelectedChrome)
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(accent.copy(0.28f), accent.copy(0.12f), Color.White.copy(0.06f))
                        )
                    )
                else Modifier
            )
            .then(
                if (isSelected && showSelectedChrome)
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(accent.copy(0.62f), Color.White.copy(0.20f), accent.copy(0.24f))
                        ),
                        shape = itemShape
                    )
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(
                horizontal = when {
                    showLabel && isSelected -> 14.dp
                    showLabel -> 0.dp
                    else -> 13.dp
                },
                vertical = 14.dp
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = if (isSelected && showLabel) {
            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.Center
        }
    ) {
        Icon(
            imageVector        = tab.icon,
            contentDescription = tab.label,
            tint               = if (isSelected) accent else theme.text2.copy(0.45f),
            modifier           = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = isSelected && showLabel,
            enter   = expandHorizontally(
                animationSpec = tween(190, easing = NavIndicatorEaseOut),
                expandFrom = Alignment.Start
            ) + fadeIn(tween(150, delayMillis = 35)),
            exit    = shrinkHorizontally(
                animationSpec = tween(120, easing = NavIndicatorEaseInOut),
                shrinkTowards = Alignment.Start
            ) + fadeOut(tween(90))
        ) {
            Text(
                text          = tab.label,
                color         = accent,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines      = 1
            )
        }
    }
}
