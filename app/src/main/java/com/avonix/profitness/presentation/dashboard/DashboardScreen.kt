package com.avonix.profitness.presentation.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.absoluteValue
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.aicoach.AICoachScreen
import com.avonix.profitness.presentation.discover.DiscoverScreen
import com.avonix.profitness.presentation.profile.AchievementsDetailScreen
import com.avonix.profitness.presentation.profile.EditProfileScreen
import com.avonix.profitness.presentation.profile.ExerciseProgressionScreen
import com.avonix.profitness.presentation.profile.PerformanceDetailScreen
import com.avonix.profitness.presentation.profile.ProfileScreen
import com.avonix.profitness.presentation.weight.WeightTrackingScreen
import com.avonix.profitness.presentation.program.ProgramBuilderScreen
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
import kotlinx.coroutines.delay

sealed class DashboardTab(val route: String, val icon: ImageVector, val label: String) {
    object Workout  : DashboardTab("workout",  Icons.Rounded.FitnessCenter, "FORGE")
    object Program  : DashboardTab("program",  Icons.Rounded.CalendarMonth, "PLAN")
    object AICoach  : DashboardTab("ai_coach", Icons.Rounded.AutoAwesome,   "ORACLE")
    object Discover : DashboardTab("discover", Icons.Rounded.Explore,       "KEŞFET")
    object Profile  : DashboardTab("profile",  Icons.Rounded.Person,        "USER")
}

private val ALL_TABS = listOf(
    DashboardTab.Workout, DashboardTab.Program, DashboardTab.AICoach,
    DashboardTab.Discover, DashboardTab.Profile
)

@Composable
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit, onLogout: () -> Unit = {}) {
    var selectedTab             by remember { mutableStateOf<DashboardTab>(DashboardTab.Workout) }
    var programInitialMode      by remember { mutableStateOf<com.avonix.profitness.presentation.program.BuilderMode>(com.avonix.profitness.presentation.program.BuilderMode.Choose) }
    var isTabSwitching          by remember { mutableStateOf(false) }
    val workoutViewModel: WorkoutViewModel = hiltViewModel()

    fun requestTab(tab: DashboardTab) {
        if (tab == selectedTab || isTabSwitching) return
        isTabSwitching = true
        selectedTab = tab
    }

    LaunchedEffect(selectedTab) {
        if (isTabSwitching) {
            delay(220)
            isTabSwitching = false
        }
    }

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

    // Nav yüksekliği 78 → 92 dp (item padding 10/12 → 14/16, icon 20 → 24)
    val navBarHeight = 92.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp
    val haptic = LocalHapticFeedback.current

    val restTimer by workoutViewModel.restTimer.collectAsStateWithLifecycle()
    // Timer aktifken diğer ekranlardaki içerik aşağı kayar
    val statusBarPad  = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val timerActive   = restTimer.isRunning || restTimer.isDone
    val timerExtraPad by animateDpAsState(
        targetValue   = if (timerActive && selectedTab != DashboardTab.Workout)
                            (statusBarPad + 60.dp) else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "dash_timer_pad"
    )
    val contentPadWithTimer = contentPad

    // ── Swipe gesture — Orientation.Horizontal doesn't compete with vertical scrollers
    var swipeAccum by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta -> swipeAccum += delta }
    val swipeModifier = Modifier.draggable(
        state       = draggableState,
        orientation = Orientation.Horizontal,
        onDragStarted = { swipeAccum = 0f },
        onDragStopped = { velocity ->
            val curIdx = ALL_TABS.indexOf(selectedTab)
            // Fast fling (velocity) OR slow-but-wide drag both trigger tab change
            val byVelocity = abs(velocity) > 500f
            val byDistance = abs(swipeAccum) > 120f
            if (byVelocity || byDistance) {
                val goNext = if (byVelocity) velocity < 0f else swipeAccum < 0f
                if (goNext && curIdx < ALL_TABS.lastIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    requestTab(ALL_TABS[curIdx + 1])
                } else if (!goNext && curIdx > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    requestTab(ALL_TABS[curIdx - 1])
                }
            }
            swipeAccum = 0f
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(modifier = Modifier.fillMaxSize())

        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = {
                val fromIdx = ALL_TABS.indexOf(initialState)
                val toIdx   = ALL_TABS.indexOf(targetState)
                // Keep tab transitions light: full-width slides make two heavy screens draw at once.
                val easeOut    = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                val easeIn     = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)
                val enterSlide = tween<IntOffset>(140, easing = easeOut)
                val enterFade  = tween<Float>(120, easing = easeOut)
                val exitSlide  = tween<IntOffset>(90, easing = easeIn)
                val exitFade   = tween<Float>(70)
                if (toIdx > fromIdx) {
                    (slideInHorizontally(enterSlide) { it / 10 } + fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { -it / 14 } + fadeOut(exitFade))
                } else {
                    (slideInHorizontally(enterSlide) { -it / 10 } + fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { it / 14 } + fadeOut(exitFade))
                }
            },
            modifier = swipeModifier,
            label = "tab_slide"
        ) { tab ->
            when (tab) {
                DashboardTab.Workout -> WorkoutScreen(
                    bottomPadding = contentPad,
                    viewModel = workoutViewModel,
                    onNavigateToAIBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.AI
                        requestTab(DashboardTab.Program)
                    },
                    onNavigateToManualBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.Manual
                        requestTab(DashboardTab.Program)
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

        AppNavBar(
            tabs     = ALL_TABS,
            selected = selectedTab,
            onSelect = { requestTab(it) },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f)
        )

        // ── Global Dynamic Island — Workout dışı tablarda üstte göster ─────
        if (selectedTab != DashboardTab.Workout) {
            DynamicIslandTimer(
                timer     = restTimer,
                topOffset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                onStop    = { workoutViewModel.stopRestTimer() },
                onDismiss = { workoutViewModel.dismissRestTimer() }
            )
        }

        if (isTabSwitching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(150f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
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
            PerformanceDetailScreen(onBack = { showPerformanceDetail = false })
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
            modifier = Modifier.zIndex(300f)
        ) {
            StoreScreen(onBack = { showStore = false })
        }
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
fun AppNavBar(
    tabs    : List<DashboardTab>,
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val shape  = RoundedCornerShape(40.dp)


    Box(
        modifier        = modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
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
                .pointerInput(tabs, selected, onSelect) {
                    awaitEachGesture {
                        // Initial pass: child clickable'lardan ÖNCE down eventı yakala
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val pointerId = downChange.id

                        var dragAccum = 0f
                        var isDragging = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break

                            dragAccum += change.positionChange().x

                            // Parmak yeterince kaydıysa drag moduna gir ve child click'i iptal et
                            if (!isDragging && abs(dragAccum) > viewConfiguration.touchSlop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                change.consume()
                            }
                        }

                        val curIdx = tabs.indexOf(selected)
                        if (abs(dragAccum) > 60f) {
                            val goNext = dragAccum < 0f
                            if (goNext && curIdx < tabs.lastIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tabs[curIdx + 1])
                            } else if (!goNext && curIdx > 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tabs[curIdx - 1])
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                NavCapsuleItem(
                    tab        = tab,
                    isSelected = tab == selected,
                    accent     = accent,
                    theme      = theme,
                    onClick    = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun NavCapsuleItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    onClick   : () -> Unit
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    // Pill width: selected shows label, unselected icon-only
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "item_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(32.dp))
            .then(
                if (isSelected)
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(accent.copy(0.20f), accent.copy(0.10f))
                        )
                    )
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = if (isSelected) 20.dp else 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = tab.icon,
            contentDescription = tab.label,
            tint               = if (isSelected) accent else theme.text2.copy(0.45f),
            modifier           = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = isSelected,
            enter   = expandHorizontally(
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            ) + fadeIn(tween(150, 60)),
            exit    = shrinkHorizontally(
                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
            ) + fadeOut(tween(80))
        ) {
            Text(
                text          = tab.label,
                color         = accent,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}
