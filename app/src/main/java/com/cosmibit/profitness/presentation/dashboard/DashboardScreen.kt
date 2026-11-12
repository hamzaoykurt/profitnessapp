package com.cosmibit.profitness.presentation.dashboard

import dev.chrisbanes.haze.hazeSource
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

    /** Alt menüdeki Sinema çizim simgesi (yan menü/rail Material simgesini kullanır). */
    val sinemaIcon: ImageVector get() = when (this) {
        Workout  -> SinemaIcons.Dumbbell
        Program  -> SinemaIcons.List
        AICoach  -> SinemaIcons.Spark
        Discover -> SinemaIcons.Compass
        Profile  -> SinemaIcons.User
    }
}

private val ALL_TABS = persistentListOf(
    DashboardTab.Workout, DashboardTab.Program, DashboardTab.AICoach,
    DashboardTab.Discover, DashboardTab.Profile
)

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
    // SİNEMA: alt menü (60dp) içeriğin üstünde durur; listelerin alt boşluğu = menü 60 + 16 + sistem çubuğu.
    val contentPad   = if (useNavRail) navBarHeight + navBarBottom + if (responsive.isLargeFont) 12.dp else 8.dp
                       else SinemaNavHeight + 16.dp + navBarBottom
    val sinemaNav = remember { SinemaNavState() }
    val navHaze = remember { dev.chrisbanes.haze.HazeState() }
    val imeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppTheme.current

    // Kapak sayfaları (Antrenman foto pager'ı, Programlar/Profil kapağı) açık temada da açık renk durum çubuğu ikonu ister.
    // shortcut: kapak her zaman üstte varsayıldı; sekme geçişinde Antrenman'ın kendi efekti 350ms sonra eski değeri
    // geri yazabildiği için ikinci kez uygulanır.
    val statusView = androidx.compose.ui.platform.LocalView.current
    val coverTab = true   // tüm sekmeler foto/koyu kapaklı: durum çubuğu ikonları hep açık
    LaunchedEffect(coverTab, theme.isDark) {
        val window = (statusView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, statusView)
        controller.isAppearanceLightStatusBars = !theme.isDark && !coverTab
        delay(350)
        controller.isAppearanceLightStatusBars = !theme.isDark && !coverTab
    }

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
        Box(Modifier.fillMaxSize().then(if (useNavRail) Modifier else Modifier.hazeSource(navHaze))) {
        AppBackground(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (useNavRail) responsive.navRailWidth else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
        CompositionLocalProvider(LocalSinemaNav provides sinemaNav) {
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
                    val programState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                    val todayInfo = remember(programState.dayStates, programState.selectedDayIdx, programState.hasProgramLoaded, programState.isLoading, theme) {
                        com.cosmibit.profitness.presentation.program.buildProgramTodayInfo(
                            programState.dayStates, programState.selectedDayIdx, theme, programState.hasProgramLoaded, programState.isLoading
                        )
                    }
                    ProgramBuilderScreen(
                        todayInfo         = todayInfo,
                        onStartWorkout    = { selectedTabRoute = DashboardTab.Workout.route },
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
            val navItems = remember(theme) { ALL_TABS.map { SinemaNavItem(it.sinemaIcon, it.label(theme)) } }
            // Foto sayfaları (Antrenman, AI Koç, Keşfet): şeffaf menü; düz zemin sayfaları (Programlar, Profil): sayfa renginde menü.
            val navOnPhoto = selectedTab != DashboardTab.Program && selectedTab != DashboardTab.Profile && !sinemaNav.lightPage
            SinemaNavBar(
                items         = navItems,
                selectedIndex = ALL_TABS.indexOf(selectedTab),
                onSelect      = { i -> ALL_TABS[i].let { tab -> if (tab != selectedTab) selectedTabRoute = tab.route } },
                visible       = !imeVisible && !(sinemaNav.pagerOwned && sinemaNav.pagerBusy),
                onPhoto       = navOnPhoto,
                haze          = navHaze,
                modifier      = Modifier.align(Alignment.BottomCenter).zIndex(100f)
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
    // Sinema: düz sayfa rengi (eski çok halelı kahve atmosfer kaldırıldı)
    Box(modifier.background(Sinema.page(LocalAppTheme.current)))
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
                .background(theme.bg1)
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
                                Modifier.background(theme.bg3)
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
