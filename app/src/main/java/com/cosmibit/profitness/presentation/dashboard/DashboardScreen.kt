package com.cosmibit.profitness.presentation.dashboard

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.cosmibit.profitness.core.ui.LocalTabActive
import com.cosmibit.profitness.presentation.profile.ExerciseProgressionViewModel
import com.cosmibit.profitness.presentation.store.StoreViewModel
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
import com.cosmibit.profitness.presentation.components.FloatingWorkoutTimer
import com.cosmibit.profitness.presentation.components.LocalFloatingTimerReserve
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
private const val WARMUP_STAGES = 5

@Composable
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit, onLogout: () -> Unit = {}) {
    var selectedTabRoute        by rememberSaveable { mutableStateOf(DashboardTab.Workout.route) }
    val selectedTab             = ALL_TABS.firstOrNull { it.route == selectedTabRoute } ?: DashboardTab.Workout
    val composedTabs            = remember { mutableStateListOf<DashboardTab>(DashboardTab.Workout) }
    val readyTabs               = remember { mutableStateListOf<DashboardTab>(DashboardTab.Workout) }
    val initialTab              = remember { selectedTab }
    /** Son kullanıcı etkileşimi (dokunma / sekme değişimi): ısıtmalar bundan 1.5 sn sonrasına ertelenir. */
    val lastInteractionMs       = remember { longArrayOf(0L) }
    val focusManager            = androidx.compose.ui.platform.LocalFocusManager.current
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

    // ── Sekme geçmişi: geri = bir önceki sekme; uygulamadan çıkış yalnız Antrenman kökünde ve geçmiş boşken.
    // EN DÜŞÜK öncelik: bu BackHandler overlay'lerinkinden ve sekme içi (TabBackScope) handler'lardan ÖNCE kaydedilir,
    // yani onlar (alt sayfa, mod, sheet) önce kapanır; sekme geçmişi en son devreye girer.
    val tabHistory = remember { TabHistory<DashboardTab>(max = 10) }
    val historyPrev = remember { arrayOf(selectedTab) }
    val poppingBack = remember { booleanArrayOf(false) }
    var historySize by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedTab) {
        val prev = historyPrev[0]
        if (poppingBack[0]) poppingBack[0] = false
        else tabHistory.onNavigate(prev, selectedTab)
        historyPrev[0] = selectedTab
        historySize = tabHistory.size
    }
    BackHandler(enabled = historySize > 0 || selectedTab != DashboardTab.Workout) {
        val target = tabHistory.pop(selectedTab) ?: DashboardTab.Workout
        historySize = tabHistory.size
        if (target != selectedTab) {
            poppingBack[0] = true
            selectedTabRoute = target.route
        }
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
    val contentPad   = if (useNavRail) 16.dp + navBarBottom   // rail: alt menü yok
                       else SinemaNavHeight + 16.dp + navBarBottom
    val sinemaNav = remember { SinemaNavState() }
    val imeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppTheme.current

    // Kapak sayfaları (Antrenman foto pager'ı, Programlar/Profil kapağı) açık temada da açık renk durum çubuğu ikonu ister.
    // shortcut: kapak her zaman üstte varsayıldı; sekme geçişinde Antrenman'ın kendi efekti 350ms sonra eski değeri
    // geri yazabildiği için ikinci kez uygulanır.
    val statusView = androidx.compose.ui.platform.LocalView.current
    val coverTab = true   // tüm sekmeler foto/koyu kapaklı: durum çubuğu ikonları hep açık
    LaunchedEffect(coverTab) {
        val window = (statusView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, statusView)
        controller.isAppearanceLightStatusBars = false   // tek koyu tema: durum çubuğu ikonları hep açık
        delay(350)
        controller.isAppearanceLightStatusBars = false
    }

    val restTimer by workoutViewModel.restTimer.collectAsStateWithLifecycle()

    // SOĞUK AÇILIŞ: ilk karelerde ısıtma YOK. İlk kare çizildikten ~1.2 sn sonra başlar, aşamalar arası ≥450ms,
    // tek seferde tek aşama; kullanıcı son 1.5 sn içinde dokunduysa/sekme değiştirdiyse bekler.
    // (Eski: 120-780ms içinde 4 aşama + 1.5 sn'de Programlar sekmesini gizli kompoze etme = açılışta kasma.)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        for (stage in 1..WARMUP_STAGES) {
            delay(if (stage == 1) 1_200 else 450)
            while (android.os.SystemClock.uptimeMillis() - lastInteractionMs[0] < 1_500) delay(250)
            withFrameNanos { }
            warmupStage = stage
        }
    }
    DashboardViewModelWarmup(stage = warmupStage)

    // Yüzen kronometre (alt menünün üstü): görünürken listeler/BAŞLA bu kadar yukarı alınır, kapsül içeriği örtmez.
    val anyOverlay = showPerformanceDetail || showAchievementsDetail || showLeaderboard ||
        showEditProfile || showWeightTracking || showExerciseProgression || showStore
    val timerVisible = (restTimer.isRunning || restTimer.isPaused || restTimer.isDone) && !imeVisible
    val motionOn = rememberSinemaMotionOn()
    val timerReserve by animateDpAsState(
        targetValue   = if (timerVisible) FloatingTimerReserve else 0.dp,
        animationSpec = tween(if (motionOn) 220 else 0, easing = SinemaMotion.EaseStandard),
        label         = "dash_timer_reserve"
    )
    val contentPadWithTimer = contentPad + timerReserve
    // Alt ekranlar (overlay) için: kapsül nav bar'ın üstünde durur, bu yüzden nav bar payı da eklenir.
    val overlayTimerReserve = timerReserve + navBarBottom * (timerReserve / FloatingTimerReserve)
    // Bitti geri bildirimi kısa süre görünür, sonra kronometre kapanır (eski Dynamic Island davranışı).
    LaunchedEffect(restTimer.isDone) {
        if (restTimer.isDone) {
            delay(3_500)
            workoutViewModel.dismissVisibleTimer()
        }
    }

    // ── Swipe gesture — Orientation.Horizontal doesn't compete with vertical scrollers
    var swipeAccum by remember { mutableStateOf(0f) }
    val selectedTabIndex by remember {
        derivedStateOf { ALL_TABS.indexOfFirst { it.route == selectedTabRoute }.coerceAtLeast(0) }
    }
    val draggableState = rememberDraggableState { delta -> swipeAccum += delta }
    // Yatay kaydırma: iç yatay listeler (çip satırı, kart listesi, pager) kendi ucuna gelince artan kaydırma/fling sekme geçişine döner.
    val nestedAccum = remember { floatArrayOf(0f) }
    fun changeTab(goNext: Boolean) {
        val curIdx = selectedTabIndex
        if (goNext && curIdx < ALL_TABS.lastIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            selectedTabRoute = ALL_TABS[curIdx + 1].route
        } else if (!goNext && curIdx > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            selectedTabRoute = ALL_TABS[curIdx - 1].route
        }
    }
    val tabNestedScroll = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (abs(available.x) > abs(available.y)) nestedAccum[0] += available.x
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                val acc = nestedAccum[0]; nestedAccum[0] = 0f
                if (abs(acc) > 120f || (abs(acc) > 24f && abs(available.x) > 500f)) changeTab(goNext = acc < 0f)
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    val swipeModifier = Modifier.nestedScroll(tabNestedScroll).draggable(
        state       = draggableState,
        orientation = Orientation.Horizontal,
        onDragStarted = { swipeAccum = 0f },
        onDragStopped = { velocity ->
            // Fast fling (velocity) OR slow-but-wide drag both trigger tab change
            val byVelocity = abs(velocity) > 500f
            val byDistance = abs(swipeAccum) > 120f
            if (byVelocity || byDistance) changeTab(goNext = if (byVelocity) velocity < 0f else swipeAccum < 0f)
            swipeAccum = 0f
        }
    )

    CompositionLocalProvider(LocalFloatingTimerReserve provides overlayTimerReserve) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
        SinemaBackdrop(Modifier.fillMaxSize())   // tek zemin kaynağı: sekmeler bunun üstünde geçer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (useNavRail) responsive.navRailWidth else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
        CompositionLocalProvider(LocalSinemaNav provides sinemaNav) {
        // PERF: sekmeler bellekte CANLI tutulur (eskiden AnimatedContent her girişte içeriği baştan kompoze ediyordu).
        // Gezilen sekme kompoze kalır (state, liste konumu, VM verisi); görünmeyen yerleştirilmez -> çizilmez, dokunulmaz.
        // Geçiş aynı: giren kısa yatay kayma (w/10) + solma, çıkan hızlı solma + kayma; zemin (SinemaBackdrop) sabit.
        val forward = remember { arrayOf(selectedTab, selectedTab) }.let { h ->
            if (h[1] != selectedTab) { h[0] = h[1]; h[1] = selectedTab }
            ALL_TABS.indexOf(h[1]) > ALL_TABS.indexOf(h[0])
        }
        LaunchedEffect(selectedTab) {
            lastInteractionMs[0] = android.os.SystemClock.uptimeMillis()
            focusManager.clearFocus()   // gizlenen sekmedeki alan odakta kalıp klavyeyi tutmasın (eskiden dispose ediyordu)
            if (selectedTab !in composedTabs) composedTabs.add(selectedTab)
        }
        Box(
            Modifier.fillMaxSize()
                // etkileşimi yalnız GÖZLEMLE (tüketme): ısıtma zamanlaması için
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastInteractionMs[0] = android.os.SystemClock.uptimeMillis()
                        }
                    }
                }
                .then(swipeModifier),
            contentAlignment = Alignment.TopCenter
        ) {
        ALL_TABS.forEach { tab -> if (tab == selectedTab || tab in composedTabs) key(tab.route) {
            LaunchedEffect(Unit) {
                if (tab !in readyTabs) {
                    delay(FIRST_TAB_CONTENT_DELAY_MS)
                    if (tab !in readyTabs) readyTabs.add(tab)
                }
            }
            KeptTab(
                active      = tab == selectedTab,
                forward     = forward,
                motionOn    = motionOn,
                animateFirst = tab != initialTab,
                warmDraw    = false
            ) {
            // contentMax yalnız liste sekmelerinde (Programlar, Profil); foto sayfaları (Antrenman, AI Koç, Keşfet) tam genişlik.
            val listTab = tab == DashboardTab.Program || tab == DashboardTab.Profile
            Box(
                if (listTab) Modifier.widthIn(max = responsive.contentMaxWidth).fillMaxSize()
                else Modifier.fillMaxSize()
            ) {
            if (tab !in readyTabs) {
                TabWarmupSurface()
            } else when (tab) {
                DashboardTab.Workout -> WorkoutScreen(
                    bottomPadding = contentPad,
                    floatingTimerPad = timerReserve,
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
                        timerExtraPad     = timerReserve,
                        onNavigateToStore = { showStore = true },
                        onProgramShared   = { discoverRefreshSignal++ }
                    )
                }
                DashboardTab.AICoach -> {
                    val trainingState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                    val summary = remember(trainingState.dayStates, trainingState.selectedDayIdx, trainingState.hasProgramLoaded, trainingState.isLoading) {
                        com.cosmibit.profitness.presentation.aicoach.summarizeOracleTraining(
                            days = trainingState.dayStates,
                            hasProgramLoaded = trainingState.hasProgramLoaded,
                            isLoading = trainingState.isLoading,
                            selectedDayIdx = trainingState.selectedDayIdx
                        )
                    }
                    AICoachScreen(
                    bottomPadding      = contentPadWithTimer,
                    onNavigateToStore  = { showStore = true },
                    trainingSummary = summary
                )
                }
                DashboardTab.Discover -> DiscoverScreen(
                    bottomPadding         = contentPadWithTimer,
                    timerExtraPad         = 0.dp,
                    externalRefreshSignal = discoverRefreshSignal,
                    onOpenLeaderboard     = {
                        leaderboardInitialTab = com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Xp
                        showLeaderboard = true
                    }
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
                    timerExtraPad                   = timerReserve
                )
            }
            }
            }   // KeptTab
        } }     // key / forEach
        }       // sekme kutusu (swipe)
        }
        }
        }

        // Durum çubuğu altı yumuşak scrim: kayan içerik saat/simgelerle çakışmasın (Antrenman fotoğrafı hariç)
        if (selectedTab != DashboardTab.Workout) {
            val statusTopPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(statusTopPad + 14.dp).zIndex(99f)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Sinema.page(theme).copy(alpha = 0.94f), androidx.compose.ui.graphics.Color.Transparent)))
            )
        }

        val navItems = remember(theme) { ALL_TABS.map { SinemaNavItem(it.sinemaIcon, it.label(theme)) } }
        if (useNavRail) {
            SinemaNavRail(
                items         = navItems,
                selectedIndex = ALL_TABS.indexOf(selectedTab),
                onSelect      = { i -> ALL_TABS[i].let { tab -> if (tab != selectedTab) selectedTabRoute = tab.route } },
                modifier      = Modifier.align(Alignment.CenterStart).zIndex(100f)
            )
        } else {
            // Foto sayfaları (Antrenman, AI Koç, Keşfet): şeffaf menü; düz zemin sayfaları (Programlar, Profil): sayfa renginde menü.
            val navOnPhoto = selectedTab != DashboardTab.Program && selectedTab != DashboardTab.Profile && !sinemaNav.lightPage
            SinemaNavBar(
                items         = navItems,
                selectedIndex = ALL_TABS.indexOf(selectedTab),
                onSelect      = { i -> ALL_TABS[i].let { tab -> if (tab != selectedTab) selectedTabRoute = tab.route } },
                visible       = !imeVisible && !(selectedTab == DashboardTab.Workout && sinemaNav.pagerOwned && sinemaNav.pagerBusy),
                onPhoto       = navOnPhoto,
                modifier      = Modifier.align(Alignment.BottomCenter).zIndex(100f)
            )
        }

        // ── Yüzen kronometre — her sekmede ve alt ekranda (overlay'lerin de üstünde) ─────
        FloatingWorkoutTimer(
            timer         = if (imeVisible) RestTimerState() else restTimer,
            light         = false,
            onOpen        = {
                workoutViewModel.requestFocusExercise(restTimer.exerciseId)  // VM 3 sn içinde tüketilmezse düşürür
                showStore = false; showExerciseProgression = false; showWeightTracking = false
                showEditProfile = false; showLeaderboard = false; showAchievementsDetail = false
                showPerformanceDetail = false
                if (selectedTab != DashboardTab.Workout) selectedTabRoute = DashboardTab.Workout.route
            },
            onTogglePause = {
                if (restTimer.isPaused) workoutViewModel.resumeVisibleTimer()
                else workoutViewModel.pauseVisibleTimer()
            },
            onStop        = {
                workoutViewModel.stopVisibleTimer()
                workoutViewModel.dismissVisibleTimer()
            },
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (useNavRail) responsive.navRailWidth + 16.dp else 16.dp, end = 16.dp,
                    bottom = navBarBottom + if (useNavRail || anyOverlay) 16.dp else SinemaNavHeight + 6.dp
                )
                .zIndex(400f)
        )

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
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
            modifier = Modifier.zIndex(200f)
        ) {
            PerformanceDetailScreen(
                onBack = { showPerformanceDetail = false },
                onNavigateToWeightTracking = {
                    showWeightTracking = true
                },
                onNavigateToExerciseProgression = {
                    showExerciseProgression = true
                },
                onStartWorkout = {
                    showPerformanceDetail = false
                    selectedTabRoute = DashboardTab.Workout.route
                }
            )
        }

        // ── Achievements Detail Overlay ─────────────────────────────────
        AnimatedVisibility(
            visible = showAchievementsDetail,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
            modifier = Modifier.zIndex(200f)
        ) {
            AchievementsDetailScreen(onBack = { showAchievementsDetail = false })
        }

        // ── Leaderboard Overlay ─────────────────────────────────────────
        AnimatedVisibility(
            visible  = showLeaderboard,
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
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
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
            modifier = Modifier.zIndex(200f)
        ) {
            EditProfileScreen(onBack = { showEditProfile = false })
        }

        // ── Weight Tracking Overlay ───────────────────────────────────────────
        AnimatedVisibility(
            visible  = showWeightTracking,
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
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
            enter    = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit     = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(SinemaMotion.Quick)),
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
            enter    = slideInVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(tween(SinemaMotion.Standard)),
            exit     = slideOutVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { it } + fadeOut(tween(SinemaMotion.Quick)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(300f)
        ) {
            StoreScreen(onBack = { showStore = false })
        }
    }
    }
}

@Composable
private fun TabWarmupSurface() {
    Box(modifier = Modifier.fillMaxSize())
}

/**
 * Canlı tutulan sekme kabı. [active] değilken içerik kompoze kalır ama YERLEŞTİRİLMEZ (çizilmez, dokunma almaz,
 * geri tuşu callback'leri devre dışı). Giriş/çıkış animasyonu eski AnimatedContent ile aynı; azaltılmış harekette anında.
 * [warmDraw]: gizli oluşturulan sekme bir kez görünmez (alpha 0) çizilir; ilk gerçek girişte çizim önbellekleri hazırdır.
 */
@Composable
private fun KeptTab(
    active: Boolean,
    forward: Boolean,
    motionOn: Boolean,
    animateFirst: Boolean,
    warmDraw: Boolean,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(if (active && !animateFirst) 1f else 0f) }
    val shift = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(active) }
    // Giriş animasyonu bitti mi (ağır alt içerik bunu bekler: geçiş sırasında çizilmez). Bir kez true olunca kalır.
    val settled = remember { mutableStateOf(false) }
    val firstRun = remember { booleanArrayOf(true) }
    LaunchedEffect(active) {
        val isFirst = firstRun[0]
        firstRun[0] = false
        val ease = SinemaMotion.EaseStandard
        if (active) {
            placed = true
            if ((isFirst && !animateFirst) || !motionOn) {
                alpha.snapTo(1f); shift.snapTo(0f)
                settled.value = true
                return@LaunchedEffect
            }
            alpha.snapTo(0f); shift.snapTo(if (forward) 0.1f else -0.1f)
            coroutineScope {
                launch { alpha.animateTo(1f, tween(SinemaMotion.Quick, easing = ease)) }
                shift.animateTo(0f, tween(SinemaMotion.Standard, easing = ease))
            }
            settled.value = true
        } else {
            if (isFirst) {
                if (warmDraw) {
                    alpha.snapTo(0f)
                    placed = true
                    repeat(3) { withFrameNanos { } }
                }
                placed = false
                return@LaunchedEffect
            }
            if (motionOn) coroutineScope {
                launch { alpha.animateTo(0f, tween(SinemaMotion.Instant, easing = ease)) }
                shift.animateTo(if (forward) -0.1f else 0.1f, tween(SinemaMotion.Quick, easing = ease))
            }
            placed = false
        }
    }
    val activeState = rememberUpdatedState(active)
    Box(
        Modifier
            .zIndex(if (active) 1f else 0f)
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) {
                    if (placed) p.placeWithLayer(0, 0) {
                        this.alpha = alpha.value
                        translationX = shift.value * p.width
                    }
                }
            }
    ) {
        // Gizli sekme, dashboard her yeniden kompoze olduğunda (sekme değişimi, kronometre vb.) baştan kompoze OLMASIN:
        // içerik yalnız aktifken güncel lambda ile çağrılır; gizliyken FrozenTabBody atlanır (kendi state'leri yine çalışır).
        val holder = remember { TabContentHolder(content) }
        if (active || holder.wasActive) { holder.content = content; holder.version++ }
        holder.wasActive = active
        CompositionLocalProvider(
            LocalTabActive provides activeState,
            com.cosmibit.profitness.presentation.components.LocalTabSettled provides settled
        ) {
            TabBackScope(active = active) { FrozenTabBody(holder, holder.version) }
        }
    }
}

@Stable
private class TabContentHolder(var content: @Composable () -> Unit) {
    var version = 0
    var wasActive = false
}

@Composable
private fun FrozenTabBody(holder: TabContentHolder, @Suppress("UNUSED_PARAMETER") version: Int) {
    holder.content()
}

/**
 * Gizli sekmedeki BackHandler'lar geri tuşunu yutmasın: sekme kendi alt dispatcher'ını alır; üst dispatcher'daki
 * yönlendirici yalnız sekme aktifken ve içeride etkin callback varken açıktır (öncelik sırası eskisiyle aynı).
 */
@Composable
private fun TabBackScope(active: Boolean, content: @Composable () -> Unit) {
    val parent = LocalOnBackPressedDispatcherOwner.current
    if (parent == null) { content(); return }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activeState = rememberUpdatedState(active)
    val childRef = remember { arrayOfNulls<OnBackPressedDispatcher>(1) }
    val forwarder = remember {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { childRef[0]?.onBackPressed() }
        }
    }
    // Etkinlik değişimi doğrudan callback'ten uygulanır (yeniden kompozisyon beklenmez: SideEffect içindeki
    // state okuması abonelik oluşturmaz; aksi halde Manuel/AI modundaki geri, sekme geçmişine kaçıyordu).
    val child = remember {
        OnBackPressedDispatcher(null) { enabled -> forwarder.isEnabled = activeState.value && enabled }
            .also { childRef[0] = it }
    }
    SideEffect { forwarder.isEnabled = active && child.hasEnabledCallbacks() }
    DisposableEffect(parent, lifecycleOwner) {
        parent.onBackPressedDispatcher.addCallback(lifecycleOwner, forwarder)
        onDispose { forwarder.remove() }
    }
    val childOwner = remember(lifecycleOwner) {
        object : OnBackPressedDispatcherOwner {
            override val onBackPressedDispatcher: OnBackPressedDispatcher get() = child
            override val lifecycle: androidx.lifecycle.Lifecycle get() = lifecycleOwner.lifecycle
        }
    }
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides childOwner, content = content)
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
        // Keşfet akışı arka planda yüklensin (sekme açılmadan dolu); initLoad tek seferlik.
        val discoverVm = hiltViewModel<DiscoverViewModel>()
        LaunchedEffect(Unit) { discoverVm.initLoad() }
    }
    if (stage >= 4) {
        // yerel önbellekten anında doldur + arka planda tazele (sekme açılmadan hazır)
        val profileVm = hiltViewModel<ProfileViewModel>()
        LaunchedEffect(Unit) { profileVm.initLoad() }
    }
    if (stage >= 5) {
        // Alt ekranlar (Hareket performansı, Mağaza): VM'ler dashboard kapsamında; önceden Room'dan dolar.
        // Kilo VM'i ISITILMAZ: açılışta otomatik AI trend analizi (kredi harcar) başlatıyor.
        hiltViewModel<StoreViewModel>()
        val progressionVm = hiltViewModel<ExerciseProgressionViewModel>()
        LaunchedEffect(Unit) { progressionVm.load() }
    }
}

/** Kronometre kapsülü (48dp) + menüyle arası; görünürken içerik alt boşluğuna eklenir. */
private val FloatingTimerReserve = 56.dp

