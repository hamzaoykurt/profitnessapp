package com.cosmibit.profitness.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.presentation.aicoach.AICoachScreen
import com.cosmibit.profitness.presentation.aicoach.AICoachViewModel
import com.cosmibit.profitness.presentation.discover.DiscoverScreen
import com.cosmibit.profitness.presentation.discover.DiscoverViewModel
import com.cosmibit.profitness.presentation.profile.AchievementsDetailScreen
import com.cosmibit.profitness.presentation.profile.EditProfileScreen
import com.cosmibit.profitness.presentation.profile.ExerciseProgressionScreen
import com.cosmibit.profitness.presentation.profile.PerformanceDetailScreen
import com.cosmibit.profitness.presentation.profile.ProfileScreen
import com.cosmibit.profitness.presentation.profile.ProfileViewModel
import com.cosmibit.profitness.presentation.weight.WeightTrackingScreen
import com.cosmibit.profitness.presentation.program.ProgramBuilderScreen
import com.cosmibit.profitness.presentation.program.ProgramShareViewModel
import com.cosmibit.profitness.presentation.program.ProgramViewModel
import com.cosmibit.profitness.presentation.store.StoreScreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.presentation.components.DynamicIslandTimer
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.presentation.workout.RestTimerState
import com.cosmibit.profitness.presentation.workout.WorkoutScreen
import com.cosmibit.profitness.presentation.workout.WorkoutViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
sealed class DashboardTab(
    val route: String,
    val icon: ImageVector,
    private val trLabel: String,
    private val enLabel: String
) {
    object Workout  : DashboardTab("workout",  Icons.Rounded.FitnessCenter, "Antrenman", "Workout")
    object Program  : DashboardTab("program",  Icons.Rounded.Dashboard,     "Programlar", "Programs")
    object AICoach  : DashboardTab("ai_coach", Icons.Rounded.PsychologyAlt, "AI Koç", "AI Coach")
    object Discover : DashboardTab("discover", Icons.Rounded.Explore,       "Keşfet", "Discover")
    object Profile  : DashboardTab("profile",  Icons.Rounded.Person,        "Profil", "Profile")

    fun label(theme: AppThemeState): String = theme.t(trLabel, enLabel)
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
    var programInitialMode      by remember { mutableStateOf<com.cosmibit.profitness.presentation.program.BuilderMode>(com.cosmibit.profitness.presentation.program.BuilderMode.Choose) }
    var discoverRefreshSignal   by rememberSaveable { mutableIntStateOf(0) }
    val workoutViewModel: WorkoutViewModel = hiltViewModel()
    var showPerformanceDetail       by remember { mutableStateOf(false) }
    var showAchievementsDetail      by remember { mutableStateOf(false) }
    var showEditProfile             by remember { mutableStateOf(false) }
    var showWeightTracking          by remember { mutableStateOf(false) }
    var showExerciseProgression     by remember { mutableStateOf(false) }
    var showStore                   by remember { mutableStateOf(false) }
    var showLeaderboard             by remember { mutableStateOf(false) }
    var leaderboardInitialTab       by remember {
        mutableStateOf(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Xp)
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

    val responsive = rememberResponsiveLayoutInfo()
    val useNavRail = responsive.useNavigationRail
    val navBarHeight = responsive.bottomNavHeight
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + if (responsive.isLargeFont) 12.dp else 8.dp
    val imeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
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
                    val enterSlide = tween<IntOffset>(220, easing = easeOut)
                    val enterFade  = tween<Float>(180, easing = easeOut)
                    val exitSlide  = tween<IntOffset>(160, easing = easeIn)
                    val exitFade   = tween<Float>(120, easing = FastOutSlowInEasing)
                    if (toIdx > fromIdx) {
                        (slideInHorizontally(enterSlide) { it / 12 } + fadeIn(enterFade)) togetherWith
                        (slideOutHorizontally(exitSlide) { -it / 16 } + fadeOut(exitFade))
                    } else {
                        (slideInHorizontally(enterSlide) { -it / 12 } + fadeIn(enterFade)) togetherWith
                        (slideOutHorizontally(exitSlide) { it / 16 } + fadeOut(exitFade))
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
                        programInitialMode = com.cosmibit.profitness.presentation.program.BuilderMode.AI
                        selectedTabRoute = DashboardTab.Program.route
                    },
                    onNavigateToManualBuilder = {
                        programInitialMode = com.cosmibit.profitness.presentation.program.BuilderMode.Manual
                        selectedTabRoute = DashboardTab.Program.route
                    },
                    onNavigateToStore = { showStore = true }
                )
                DashboardTab.Program -> {
                    val capturedMode = programInitialMode
                    // Modu kullandıktan sonra sıfırla (geri dönüşte Choose'a dönmesi için)
                    LaunchedEffect(capturedMode) {
                        programInitialMode = com.cosmibit.profitness.presentation.program.BuilderMode.Choose
                    }
                    ProgramBuilderScreen(
                        initialMode       = capturedMode,
                        timerExtraPad     = timerExtraPad,
                        onNavigateToStore = { showStore = true },
                        onProgramShared   = { discoverRefreshSignal++ }
                    )
                }
                DashboardTab.AICoach -> {
                    val trainingState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                    val summary = remember(trainingState.dayStates, trainingState.hasProgramLoaded, trainingState.isLoading) {
                        com.cosmibit.profitness.presentation.aicoach.summarizeOracleTraining(
                            days = trainingState.dayStates,
                            hasProgramLoaded = trainingState.hasProgramLoaded,
                            isLoading = trainingState.isLoading
                        )
                    }
                    AICoachScreen(
                    bottomPadding      = contentPadWithTimer,
                    onNavigateToStore  = { showStore = true },
                    trainingSummary = summary
                )
                }
                DashboardTab.Discover -> DiscoverScreen(
                    bottomPadding         = contentPad,
                    timerExtraPad         = timerExtraPad,
                    externalRefreshSignal = discoverRefreshSignal
                )
                DashboardTab.Profile -> ProfileScreen(
                    onThemeChange                   = onThemeChange,
                    onNavigateToPerformance         = { showPerformanceDetail = true },
                    onNavigateToAchievements        = { showAchievementsDetail = true },
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
        } else if (!imeVisible) {
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
                    showWeightTracking = true
                },
                onNavigateToExerciseProgression = {
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
            com.cosmibit.profitness.presentation.leaderboard.LeaderboardScreen(
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

    Box(modifier = modifier.drawWithCache {
        val radial = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f  to accent.copy(alpha = if (theme.isDark) 0.16f else 0.075f),
                0.38f to accent.copy(alpha = if (theme.isDark) 0.045f else 0.018f),
                1.0f  to Color.Transparent
            ),
            center = Offset(size.width * 1.28f, -size.height * 0.10f),
            radius = size.width * 1.85f
        )
        val mineral = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to if (theme.isDark) Color(0xFF315D8A).copy(0.20f) else Color(0xFF6887A7).copy(0.09f),
                0.48f to if (theme.isDark) Color(0xFF315D8A).copy(0.045f) else Color(0xFFB7C7D7).copy(0.025f),
                1.0f to Color.Transparent
            ),
            center = Offset(-size.width * 0.16f, size.height * 0.66f),
            radius = size.width * 1.75f
        )
        onDrawBehind {
            drawRect(theme.bg0)
            drawRect(mineral)
            drawRect(radial)
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
                    elevation = 8.dp,
                    shape = shape,
                    spotColor = Color.Black.copy(if (theme.isDark) 0.46f else 0.12f),
                    ambientColor = Color.Transparent
                )
                .clip(shape)
                .background(if (theme.isDark) Color(0xFF090B10) else Color(0xFFF5F7FA))
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val itemShape = RoundedCornerShape(24.dp)
                val tabLabel = tab.label(theme)
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else 1f,
                    animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
                    label = "rail_press"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                            translationY = if (isPressed) 1.5.dp.toPx() else 0f
                        }
                        .clip(itemShape)
                        .then(
                            if (isSelected)
                                Modifier.background(if (theme.isDark) Color(0xFF171B23) else Color(0xFFE1E7EE))
                            else Modifier
                        )
                        .then(
                            Modifier
                        )
                        .clickable(indication = null, interactionSource = interactionSource) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tab)
                        }
                        .padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = tab.icon,
                        contentDescription = tabLabel,
                        tint               = if (isSelected) accent else theme.text2.copy(0.72f),
                        modifier           = Modifier.size(23.dp)
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
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val shape  = RoundedCornerShape(36.dp)
    val selectedTab = selected()
    val responsive = rememberResponsiveLayoutInfo()
    val navBodyWidth = (responsive.screenWidth - 32.dp)
        .coerceIn(292.dp, 312.dp)
    val selectedState by rememberUpdatedState(selectedTab)
    val onSelectState by rememberUpdatedState(onSelect)
    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    var indicatorReady by remember { mutableStateOf(false) }
    var isIndicatorDragging by remember { mutableStateOf(false) }
    var settleIndicatorFromDrag by remember { mutableStateOf(false) }
    var draggedIndicatorX by remember { mutableFloatStateOf(0f) }
    var draggedIndicatorWidth by remember { mutableFloatStateOf(0f) }
    var dragHoveredRoute by remember { mutableStateOf<String?>(null) }
    var revealedLabelRoute by remember { mutableStateOf<String?>(selectedTab.route) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.15.sp,
        lineHeight = 14.sp
    )
    val horizontalInsetPx = with(density) { 6.dp.toPx() }
    val collapsedIndicatorWidthPx = with(density) { 52.dp.toPx() }
    val navBodyWidthPx = with(density) { navBodyWidth.toPx() }
    val layoutEdgePx = with(density) { 12.dp.toPx() }
    val compactVisualWidthPx = with(density) { 22.dp.toPx() }
    val itemTouchWidthPx = with(density) { 48.dp.toPx() }

    fun expandedWidthFor(tab: DashboardTab): Float {
        val labelWidth = textMeasurer.measure(
            text = tab.label(theme),
            style = labelStyle,
            maxLines = 1
        ).size.width.toFloat()
        val expandedWidth = with(density) { (18.dp + 22.dp + 5.dp).toPx() } + labelWidth
        return expandedWidth.coerceAtLeast(collapsedIndicatorWidthPx)
    }

    fun layoutsFor(expandedTab: DashboardTab?): Map<String, NavItemLayout> {
        val widths = tabs.map { tab ->
            if (tab == expandedTab) expandedWidthFor(tab) else compactVisualWidthPx
        }
        val contentWidth = widths.sum()
        val gap = if (expandedTab == null) {
            with(density) { 32.dp.toPx() }
        } else if (tabs.size > 1) {
            ((navBodyWidthPx - layoutEdgePx * 2f - contentWidth) / (tabs.size - 1))
                .coerceAtLeast(with(density) { 10.dp.toPx() })
        } else {
            0f
        }
        val occupiedWidth = contentWidth + gap * (tabs.size - 1).coerceAtLeast(0)
        var cursor = if (expandedTab == null) {
            ((navBodyWidthPx - occupiedWidth) / 2f).coerceAtLeast(layoutEdgePx)
        } else {
            layoutEdgePx
        }
        return buildMap {
            tabs.forEachIndexed { index, tab ->
                val width = widths[index]
                put(tab.route, NavItemLayout(x = cursor, width = width))
                cursor += width + gap
            }
        }
    }

    val expandedLayouts = layoutsFor(selectedTab)
    val compactLayouts = layoutsFor(null)
    val selectedLayout = expandedLayouts.getValue(selectedTab.route)

    LaunchedEffect(
        selectedTab.route,
        selectedLayout.x,
        selectedLayout.width,
        isIndicatorDragging
    ) {
        val target = selectedLayout
        if (isIndicatorDragging) return@LaunchedEffect
        if (settleIndicatorFromDrag) {
            indicatorX.snapTo(draggedIndicatorX)
            indicatorWidth.snapTo(draggedIndicatorWidth)
            settleIndicatorFromDrag = false
        }
        if (!indicatorReady) {
            indicatorX.snapTo(target.x)
            indicatorWidth.snapTo(target.width)
            revealedLabelRoute = selectedTab.route
            indicatorReady = true
        } else {
            // One continuous surface moves and resizes with the selected item.
            // Keeping label/layout and indicator on the same timing removes the
            // detached ghost, midpoint seam and late-arriving text frames.
            revealedLabelRoute = selectedTab.route
            kotlinx.coroutines.coroutineScope {
                launch {
                    indicatorX.animateTo(
                        target.x,
                        tween(220, easing = NavIndicatorEaseInOut)
                    )
                }
                launch {
                    indicatorWidth.animateTo(
                        target.width,
                        tween(220, easing = NavIndicatorEaseInOut)
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(navBodyWidth)
                .height(64.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = shape,
                    spotColor = accent.copy(if (theme.isDark) 0.18f else 0.07f),
                    ambientColor = Color.Black.copy(if (theme.isDark) 0.62f else 0.12f)
                )
                .clip(shape)
                .drawWithCache {
                    val base = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to if (theme.isDark) Color(0xFF20242B) else Color.White,
                            0.36f to if (theme.isDark) Color(0xFF12161B) else Color(0xFFF6F8F9),
                            1f to if (theme.isDark) Color(0xFF090C10) else Color(0xFFE6EAED)
                        )
                    )
                    onDrawBehind {
                        drawRect(base)
                        drawRect(
                            color = Color.White.copy(if (theme.isDark) 0.10f else 0.72f),
                            size = Size(size.width, 1.dp.toPx())
                        )
                    }
                }
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(if (theme.isDark) 0.14f else 0.82f),
                            theme.stroke.copy(0.46f)
                        )
                    ),
                    shape
                )
                .pointerInput(tabs) {
                    awaitEachGesture {
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val activeLeft = if (isIndicatorDragging || settleIndicatorFromDrag) draggedIndicatorX else indicatorX.value
                        val activeWidth = if (isIndicatorDragging || settleIndicatorFromDrag) draggedIndicatorWidth else indicatorWidth.value
                        val activeRight = activeLeft + activeWidth
                        if (!indicatorReady || downChange.position.x !in activeLeft..activeRight) {
                            return@awaitEachGesture
                        }
                        val pointerId = downChange.id
                        var dragAccum = 0f
                        var gestureDragging = false
                        var lastSentTab = selectedState
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            dragAccum += change.positionChange().x
                            if (!gestureDragging && abs(dragAccum) > viewConfiguration.touchSlop) {
                                gestureDragging = true
                                draggedIndicatorX = indicatorX.value +
                                    (indicatorWidth.value - collapsedIndicatorWidthPx) / 2f
                                draggedIndicatorWidth = indicatorWidth.value
                                dragHoveredRoute = selectedState.route
                                revealedLabelRoute = null
                                isIndicatorDragging = true
                            }
                            if (gestureDragging) {
                                val layouts = tabs.map { tab ->
                                    tab to compactLayouts.getValue(tab.route)
                                }
                                val minCenter = layouts.first().second.center
                                val maxCenter = layouts.last().second.center
                                val fingerCenter = change.position.x.coerceIn(minCenter, maxCenter)
                                val nearest = layouts.minBy { (_, layout) -> abs(layout.center - fingerCenter) }
                                val target = nearest.first
                                draggedIndicatorWidth +=
                                    (collapsedIndicatorWidthPx - draggedIndicatorWidth) * 0.34f
                                val desiredIndicatorX = (fingerCenter - draggedIndicatorWidth / 2f)
                                    .coerceIn(
                                        horizontalInsetPx,
                                        size.width - horizontalInsetPx - draggedIndicatorWidth
                                    )
                                draggedIndicatorX +=
                                    (desiredIndicatorX - draggedIndicatorX) * 0.42f
                                if (target != lastSentTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastSentTab = target
                                    dragHoveredRoute = target.route
                                }
                                change.consume()
                            }
                        }
                        if (gestureDragging) {
                            settleIndicatorFromDrag = true
                            isIndicatorDragging = false
                            dragHoveredRoute = null
                            // Changing the whole page at every crossed tab made
                            // the finger tracking stutter. Commit only the final
                            // hovered destination when the gesture is released.
                            if (lastSentTab != selectedState) onSelectState(lastSentTab)
                        }
                    }
                }
        ) {
            if (indicatorReady) {
                val activeIndicatorModifier = Modifier
                    .offset {
                        val x = if (isIndicatorDragging || settleIndicatorFromDrag) draggedIndicatorX else indicatorX.value
                        IntOffset(x.roundToInt(), with(density) { 6.dp.roundToPx() })
                    }
                    .width(
                        with(density) {
                            val width = if (isIndicatorDragging || settleIndicatorFromDrag) draggedIndicatorWidth else indicatorWidth.value
                            width.toDp()
                        }
                    )
                    .height(52.dp)
                NavSelectionSurface(
                    accent = accent,
                    theme = theme,
                    modifier = activeIndicatorModifier
                )
            }

            val renderedLayouts = if (isIndicatorDragging || revealedLabelRoute == null) {
                compactLayouts
            } else {
                expandedLayouts
            }
            tabs.forEach { tab ->
                val tabLayout = renderedLayouts.getValue(tab.route)
                val targetItemX = tabLayout.center - itemTouchWidthPx / 2f
                val animatedItemX by animateFloatAsState(
                    targetValue = targetItemX,
                    animationSpec = tween(220, easing = NavIndicatorEaseInOut),
                    label = "nav_item_${tab.route}"
                )
                NavCapsuleItem(
                    tab = tab,
                    isSelected = !isIndicatorDragging && tab.route == revealedLabelRoute,
                    isHighlighted = tab.route == (
                        if (isIndicatorDragging) dragHoveredRoute else selectedTab.route
                    ),
                    labelStyle = labelStyle,
                    theme = theme,
                    onClick = { onSelect(tab) },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                animatedItemX.roundToInt(),
                                with(density) { 6.dp.roundToPx() }
                            )
                        }
                        .width(48.dp)
                )
            }
        }
    }
}

@Composable
private fun NavSelectionSurface(
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(22.dp)
) {
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = accent.copy(if (theme.isDark) 0.28f else 0.10f),
                ambientColor = Color.Transparent
            )
            .clip(shape)
            .background(if (theme.isDark) Color(0xFF17121F) else Color(0xFFF3EFF8))
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(if (theme.isDark) 0.30f else 0.18f),
                        0.68f to accent.copy(if (theme.isDark) 0.17f else 0.10f),
                        1f to accent.copy(if (theme.isDark) 0.07f else 0.04f)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(if (theme.isDark) 0.15f else 0.58f),
                shape
            )
    )
}

@Composable
private fun NavCapsuleItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    isHighlighted: Boolean,
    labelStyle: TextStyle,
    theme     : AppThemeState,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(90, easing = FastOutSlowInEasing),
        label = "nav_press"
    )
    val tabLabel = tab.label(theme)

    Box(
        modifier = modifier
            .height(52.dp)
            .zIndex(if (isSelected) 1f else 0f)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .scale(pressScale)
                .wrapContentWidth(unbounded = true)
                .padding(horizontal = if (isSelected) 9.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tabLabel,
                tint = if (isHighlighted) MaterialTheme.colorScheme.primary else theme.text2.copy(alpha = 0.74f),
                modifier = Modifier.size(22.dp)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = expandHorizontally(
                    animationSpec = tween(120, easing = NavIndicatorEaseOut)
                ) + fadeIn(tween(65, delayMillis = 40, easing = NavIndicatorEaseOut)),
                exit = shrinkHorizontally(
                    animationSpec = tween(75, easing = NavIndicatorEaseInOut)
                ) + fadeOut(tween(20, easing = FastOutSlowInEasing))
            ) {
                Text(
                    text = tabLabel,
                    color = theme.text0,
                    style = labelStyle,
                    maxLines = 1
                )
            }
        }
    }
}
