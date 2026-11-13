package com.cosmibit.profitness.presentation.workout

import com.cosmibit.profitness.core.theme.SinemaBackdrop
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaGlow
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.accent2Color
import com.cosmibit.profitness.core.theme.accent3Color
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.LocalSinemaHaze
import com.cosmibit.profitness.presentation.components.SinemaVideo
import com.cosmibit.profitness.core.theme.LocalSinemaNav
import com.cosmibit.profitness.core.theme.SinemaIcons
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.em
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaMotion
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.exerciseCompactDisplayName
import com.cosmibit.profitness.core.theme.raisedButtonSurface
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.sinemaGlass
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.components.toDurationSetSecondsLabel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Veri / eylem sözleşmesi (WorkoutContent doldurur; ViewModel mantığı değişmez) ──────────────

/** Bir hareketin sayfada gösterilen canlı verisi (CinematicExerciseCard'a verilenlerin aynısı). */
@Immutable
class SinemaExerciseData(
    val doneSetIndices: Set<Int>,
    val setWeights: Map<Int, String>,
    val setDurations: Map<Int, String>,
    val lastSessionData: Map<Int, Pair<Float?, Int?>>,
    val profileWeightKg: Float,
    val activityDuration: String,
    val activityDistance: String,
    val activityElevation: String,
    val activityIncline: String,
    val activityReps: String
)

class SinemaWorkoutActions(
    val onToggleSet: (Exercise, Int) -> Unit,
    val onComplete: (Exercise) -> Unit,
    val onSetWeightChanged: (Exercise, Int, String) -> Unit,
    val onSetDurationChanged: (Exercise, Int, String) -> Unit,
    val onActivityDurationChanged: (Exercise, String) -> Unit,
    val onActivityDistanceChanged: (Exercise, String) -> Unit,
    val onActivityElevationChanged: (Exercise, String) -> Unit,
    val onActivityInclineChanged: (Exercise, String) -> Unit,
    val onActivityRepsChanged: (Exercise, String) -> Unit,
    val onStartTimer: (Exercise, Int) -> Unit,
    val onStartSetTimer: (Exercise, Int, Int) -> Unit,
    val onStartSetStopwatch: (Exercise, Int) -> Unit,
    val onStartStopwatch: (Exercise) -> Unit,
    val onLoadLastSession: (Exercise) -> Unit,
    val onShowDetail: (Exercise) -> Unit
)

internal fun sinemaTrackingSpec(e: Exercise) = activityTrackingSpec(
    category = e.category, name = e.name, target = e.target, reps = e.reps,
    sportTypeRaw = e.sportType, trackingModeRaw = e.trackingMode
)

/** Çizimdeki `.ct{bottom:64px}`: içerik, menü (60dp) üstünde 4dp boşlukla biter. */
internal val SinemaContentBottom = 64.dp

private const val PARALLAX = 0.25f   // foto sayfa hızının %75'iyle kayar (plan 0.5x; aşırı kırpmayı önlemek için)


// ── Pager ────────────────────────────────────────────────────────────────────────────────────

@Composable
fun SinemaWorkoutPager(
    programTitle: String,
    exercises: List<Exercise>,
    completedIds: Set<String>,
    days: ImmutableList<WorkoutDayState>,
    selectedDayIdx: Int,
    onSelectDay: (Int) -> Unit,
    streak: Int,
    progress: Float,
    isRestDay: Boolean,
    isSkipped: Boolean,
    allDone: Boolean,
    dataFor: (Exercise) -> SinemaExerciseData,
    actions: SinemaWorkoutActions,
    topExtra: Dp,
    listExtras: @Composable (dismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    bottomExtra: Dp = 0.dp,
    focusExerciseId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val nav = LocalSinemaNav.current
    DisposableEffect(Unit) {
        nav.pagerOwned = true
        onDispose { nav.pagerOwned = false; nav.pagerBusy = false }
    }
    val n = exercises.size
    val single = isRestDay || isSkipped || n == 0
    val pageCount = if (single) 1 else n + if (allDone) 1 else 0
    val animEnabled = rememberSinemaMotionOn()

    // Yatay pager = gün. Kaydırınca ViewModel güncellenir, dışarıdan gün değişince pager gider.
    // Pager ve durumu gün değişiminden bağımsız (key DIŞINDA): kaydırmalar boyunca alt ağaç yeniden kurulmaz.
    val dayCount = days.size
    val hState = rememberPagerState(initialPage = selectedDayIdx.coerceIn(0, (dayCount - 1).coerceAtLeast(0))) { dayCount.coerceAtLeast(1) }
    val selectedNow by rememberUpdatedState(selectedDayIdx)
    val onSelectNow by rememberUpdatedState(onSelectDay)
    var pendingDay by remember { mutableStateOf<Int?>(null) }   // VM'e bildirilen, onayı beklenen gün
    var userDriven by remember { mutableStateOf(false) }        // son yatay kaydırma kullanıcı sürüklemesi mi
    LaunchedEffect(hState) {
        hState.interactionSource.interactions.collect { if (it is DragInteraction.Start) userDriven = true }
    }
    LaunchedEffect(hState, dayCount) {
        snapshotFlow { hState.isScrollInProgress to hState.settledPage }.collect { (moving, settledDay) ->
            if (moving || !userDriven) return@collect
            userDriven = false
            WorkoutDaySync.dayToSelect(settledDay, selectedNow, dayCount)?.let { pendingDay = it; onSelectNow(it) }
        }
    }
    LaunchedEffect(selectedDayIdx, dayCount) {
        pendingDay = WorkoutDaySync.resolvePending(pendingDay, selectedDayIdx)
        if (WorkoutDaySync.shouldScroll(hState.currentPage, selectedDayIdx, dayCount, userDriven, pendingDay)) {
            hState.animateScrollToPage(selectedDayIdx, animationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing))
        }
    }
    // VM isteği onaylamazsa takılma: bekleme düşer, pager VM'in gününe döner.
    LaunchedEffect(pendingDay) {
        if (pendingDay == null) return@LaunchedEffect
        delay(WorkoutDaySync.PENDING_TIMEOUT_MS)
        pendingDay = null
        if (WorkoutDaySync.shouldScroll(hState.currentPage, selectedNow, dayCount, userDriven)) {
            hState.animateScrollToPage(selectedNow, animationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing))
        }
    }
    val hScrolling = hState.isScrollInProgress

    // Dikey pager durumu güne bağlı; yalnız seçili günün yatay sayfası kullanır.
    val pagerState = key(selectedDayIdx) {
        val initial = remember {
            if (single) 0
            else exercises.indexOfFirst { it.id !in completedIds }.takeIf { it >= 0 } ?: if (allDone) n else 0
        }
        rememberPagerState(initialPage = initial) { pageCount }
    }
    // Yüzen kronometreden gelen istek: kronometrenin hareketine kay (bu günde yoksa yalnız isteği düşür).
    LaunchedEffect(focusExerciseId, pagerState) {
        val id = focusExerciseId ?: return@LaunchedEffect
        val idx = if (single) -1 else exercises.indexOfFirst { it.id == id }
        if (idx >= 0 && idx != pagerState.currentPage) {
            if (animEnabled) pagerState.animateScrollToPage(idx, animationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing))
            else pagerState.scrollToPage(idx)
        }
        onFocusConsumed()
    }
    run {
        val scrolling = pagerState.isScrollInProgress || hScrolling
        LaunchedEffect(scrolling) {
            if (!scrolling) delay(200)
            nav.pagerBusy = scrolling
        }
        val settled = pagerState.settledPage
        LaunchedEffect(pagerState, settled, single) {
            if (!single) exercises.getOrNull(settled)?.let { actions.onLoadLastSession(it) }
        }

        // Oto-geçiş: bir hareketin SON seti işaretlenince (completedIds'e tek id eklenir) ~600ms sonra bir sonraki
        // sayfaya (son hareketteyse kutlama sayfasına) kay. Bu sürede kullanıcı sayfa değiştirir/kaydırırsa iptal.
        // shortcut: yalnız UI scroll; completedIds'in toplu yüklenmesinde (added.size != 1) tetiklenmez.
        var prevDone by remember(selectedDayIdx) { mutableStateOf(completedIds) }
        LaunchedEffect(pagerState, completedIds) {
            val added = completedIds - prevDone
            prevDone = completedIds
            if (single || added.size != 1) return@LaunchedEffect
            val page = exercises.indexOfFirst { it.id == added.first() }
            if (page < 0 || page != pagerState.currentPage) return@LaunchedEffect
            delay(600)
            if (pagerState.currentPage == page && !pagerState.isScrollInProgress && page + 1 < pagerState.pageCount) {
                pagerState.animateScrollToPage(page + 1, animationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing))
            }
        }

        var sheet by remember(selectedDayIdx) { mutableStateOf<Pair<String, Int>?>(null) }   // exerciseId, setIndex
        var showList by remember(selectedDayIdx) { mutableStateOf(false) }

        CompositionLocalProvider(LocalPagerBottomExtra provides bottomExtra) {
        Box(modifier.fillMaxSize()) {
            // Zemin: DashboardScreen kökündeki tek SinemaBackdrop (fotosuz sayfalar onu gösterir)
            HorizontalPager(
                state = hState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0,
                key = { it }
            ) { dayPage ->
              val active = dayPage == selectedDayIdx
              // Önizleme → gerçek sayfa: aynı foto katmanı (aynı url + istek boyutu = aynı cache key) altta aynı
              // konumda kalır; gerçek içerik üstünde kısa solarak gelir → sıçrama/yeniden yükleme yok.
              val reveal = remember { Animatable(if (active) 1f else 0f) }
              LaunchedEffect(active) {
                  when {
                      !active -> reveal.snapTo(0f)
                      animEnabled -> reveal.animateTo(1f, tween(Sinema.NavMs, easing = FastOutSlowInEasing))
                      else -> reveal.snapTo(1f)
                  }
              }
              Box(Modifier.fillMaxSize().clipToBounds()) {
              DayPreviewPage(days.getOrNull(dayPage), showText = !active || reveal.value < 1f)
              if (active) {
              Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal.value }) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing)
                ),
                key = { if (single) "single" else exercises.getOrNull(it)?.id ?: "done" }
            ) { page ->
                val offset = { page - pagerState.currentPage - pagerState.currentPageOffsetFraction }
                val isSettled = page == settled
                Box(Modifier.fillMaxSize().clipToBounds()) {
                when {
                    single -> RestPage(isSkipped = isSkipped)
                    page < n -> {
                        val ex = exercises[page]
                        ExercisePage(
                            exercise = ex, page = page, pageCount = pageCount, total = n,
                            isCompleted = ex.id in completedIds,
                            isNext = exercises.firstOrNull { it.id !in completedIds }?.id == ex.id,
                            progress = progress,
                            data = dataFor(ex),
                            actions = actions,
                            isSettled = isSettled, animEnabled = animEnabled, offset = offset,
                            onOpenSheet = { setIdx -> sheet = ex.id to setIdx }
                        )
                    }
                    else -> CelebrationPage(
                        programTitle = programTitle, total = n,
                        photoUrl = exercises.last().image.ifBlank { categoryImageUrl(exercises.last().category) },
                        isSettled = isSettled, animEnabled = animEnabled, offset = offset
                    )
                }
                }
            }
              }
              }
              }
            }

            SinemaTopBar(
                programTitle = programTitle, exercises = exercises, completedIds = completedIds,
                days = days, selectedDayIdx = selectedDayIdx, liveDayIdx = hState.currentPage, onSelectDay = onSelectDay,
                streak = streak, showSegments = !single, dataFor = dataFor,
                topExtra = topExtra, onOpenList = { showList = true },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        }

        sheet?.let { (exId, setIdx) ->
            exercises.firstOrNull { it.id == exId }?.let { ex ->
                SetSheet(
                    exercise = ex, setIndex = setIdx, isCompleted = ex.id in completedIds,
                    data = dataFor(ex), actions = actions, onDismiss = { sheet = null }
                )
            }
        }
        if (showList) {
            ExerciseListSheet(
                exercises = exercises, completedIds = completedIds, pagerState = pagerState,
                extras = listExtras, onDismiss = { showList = false }
            )
        }
    }
}

// ── Sayfa katmanları ─────────────────────────────────────────────────────────────────────────

/** Foto: 1.5 kat yükseklikte, paralaks. Statik (sürekli zoom yok): sayfa boştayken üstündeki cam blur'ları yeniden hesaplanmaz. */
@Composable
private fun PhotoLayer(
    url: String,
    @Suppress("UNUSED_PARAMETER") isSettled: Boolean,
    @Suppress("UNUSED_PARAMETER") animEnabled: Boolean,
    offset: () -> Float,
    modifier: Modifier = Modifier,
    /** Doluysa foto altta poster kalır, üstünde dikey video oynar (yalnız [playVideo] iken). */
    videoUrl: String? = null,
    playVideo: Boolean = false
) {
    Box(
        modifier
            .fillMaxSize()
            .layout { m, c ->
                val h = c.maxHeight
                val big = (h * 1.5f).toInt()
                val p = m.measure(c.copy(minHeight = big, maxHeight = big))
                layout(p.width, h) { p.place(0, -(h * 0.25f).toInt()) }
            }
            .graphicsLayer {
                translationY = -offset().coerceIn(-1f, 1f) * size.height * (PARALLAX / 1.5f)
            }
    ) {
        SceneImage(
            url = url, modifier = Modifier.fillMaxSize(),
            requestWidthPx = 1080, requestHeightPx = 1920,
            alignment = Alignment.Center,
            fallbackIconSize = 64.dp, fallbackIconBias = -0.2f, fallbackGradient = true
        )
        if (!videoUrl.isNullOrBlank()) {
            SinemaVideo(url = videoUrl, posterUrl = null, playing = playVideo, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Üst + alt karartma (çizim: `.tg` 140dp .55→0, `.bgd` 470dp 0→[bottomAlpha] @[solidAt]). Renk sayfa rengi (siyah değil).
 * Egzersiz sayfası set satırları için .65 kullanır (yon-cam-siyah "Set satırları": karartma FOTODA, camda değil).
 */
@Composable
internal fun Scrims(modifier: Modifier = Modifier, bottomAlpha: Float = 0.88f, solidAt: Float = 0.78f) {
    Box(
        modifier.fillMaxSize().drawWithCache {
            val h = size.height
            val topH = 140.dp.toPx().coerceAtMost(h)
            val botH = 470.dp.toPx().coerceAtMost(h)
            val top = Brush.verticalGradient(listOf(Sinema.scrim(0.55f), Color.Transparent), startY = 0f, endY = topH)
            val bottom = Brush.verticalGradient(
                0f to Color.Transparent, solidAt to Sinema.scrim(bottomAlpha), 1f to Sinema.scrim(bottomAlpha),
                startY = h - botH, endY = h
            )
            onDrawBehind {
                drawRect(bottom, topLeft = Offset(0f, h - botH), size = Size(size.width, botH))
                drawRect(top, size = Size(size.width, topH))
            }
        }
    )
}

/** Ödül süpürmesi: `trigger` her arttığında 500ms'lik bir ışık bandı geçer. */
@Composable
private fun SweepLayer(trigger: Int, animEnabled: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && animEnabled) {
            p.snapTo(0f)
            p.animateTo(1f, tween(Sinema.SweepMs, easing = SinemaMotion.EaseStandard))
            p.snapTo(0f)
        }
    }
    Box(
        // Fırça bir kez kurulur (boyut değişene kadar); her karede yalnız yatay ötelenir.
        Modifier.fillMaxSize().drawWithCache {
            val bw = size.width * 0.55f
            val band = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(0.16f), Color.Transparent),
                startX = 0f, endX = bw
            )
            val bandSize = Size(bw, size.height)
            onDrawBehind {
                val v = p.value
                if (v > 0f && v < 1f) {
                    val x = -bw + (size.width + bw) * v
                    translate(left = x) { drawRect(band, size = bandSize) }
                }
            }
        }
    )
}

/** Yüzen kronometre görünürken sayfa içeriği (BAŞLA vb.) bu kadar yukarı alınır; kapsül içeriği örtmez. */
private val LocalPagerBottomExtra = androidx.compose.runtime.compositionLocalOf { 0.dp }

@Composable
private fun navInset(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalPagerBottomExtra.current

@Composable
private fun ExercisePage(
    exercise: Exercise,
    page: Int,
    pageCount: Int,
    total: Int,
    isCompleted: Boolean,
    isNext: Boolean,
    progress: Float,
    data: SinemaExerciseData,
    actions: SinemaWorkoutActions,
    isSettled: Boolean,
    animEnabled: Boolean,
    offset: () -> Float,
    onOpenSheet: (Int) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    val haptic = LocalHapticFeedback.current
    val kind = remember(exercise.id, exercise.sets, exercise.trackingMode, exercise.sportType) { sinemaSetKind(exercise) }

    // Ödül: set sayısı artınca süpürme + LongPress titreşimi (ilk kompozisyonda değil).
    val doneCount = data.doneSetIndices.size
    var sweepKey by remember { mutableIntStateOf(0) }
    var prevDone by remember { mutableIntStateOf(doneCount) }
    LaunchedEffect(doneCount) {
        if (doneCount > prevDone) {
            sweepKey++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevDone = doneCount
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        val photo = exercise.image.ifBlank { categoryImageUrl(exercise.category) }
        val haze = remember { HazeState() }
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            PhotoLayer(photo, isSettled, animEnabled, offset, videoUrl = exercise.videoUrl, playVideo = isSettled)
            Scrims(bottomAlpha = 0.65f, solidAt = 0.45f)
            SweepLayer(sweepKey, animEnabled)
        }

        CompositionLocalProvider(LocalSinemaHaze provides haze) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = SinemaContentBottom + navInset()),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Label: kas grubu (vurgu rengi) + sıra
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.target.sinemaUpper(theme), style = Sinema.Label, color = accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                // Görünür "Hareketi gör" hapı: detay sheet'ini (görsel/animasyon + nasıl yapılır) açar.
                HowToPill(onClick = { actions.onShowDetail(exercise) })
                Spacer(Modifier.width(12.dp))
                Text(
                    "%02d / %02d".format(page + 1, total), style = Sinema.Label, color = Color.White.copy(0.70f)
                )
            }
            // Başlık; dokununca da hareket detayı açılır (üstteki "Hareketi gör" hapıyla aynı eylem)
            val watch = theme.t("Hareketi gör", "Watch exercise")
            Text(
                theme.exerciseCompactDisplayName(exercise.name), style = Sinema.Title, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = watch }
                    .clickable(role = Role.Button) { actions.onShowDetail(exercise) }
            )
            Spacer(Modifier.height(6.dp))
            // Display
            DisplayPrescription(
                text = exercisePrescription(exercise, kind == SinemaSetKind.Activity, theme),
                compact = compact, floorSp = if (kind == SinemaSetKind.Activity) 36 else if (compact) 56 else 72
            )
            Spacer(Modifier.height(12.dp))
            // Set kapsülleri
            SetCapsules(exercise, kind, data, isCompleted, compact, accent, onAccent, actions, onOpenSheet)
            Spacer(Modifier.height(10.dp))
            // İlerleme satırı
            ProgressRow(progress, showChevron = page < pageCount - 1, animEnabled = animEnabled)
            Spacer(Modifier.height(8.dp))
            // BAŞLA
            StartButton(
                label = theme.t("BAŞLA", "START"),
                dim = isCompleted,
                onClick = {
                    val firstOpen = (0 until exercise.sets).firstOrNull { it !in data.doneSetIndices } ?: 0
                    onOpenSheet(firstOpen)
                }
            )
        }
        }
    }
}

/** Küçük cam hap: oynat ikonu + "Hareketi gör"; 32dp görsel, 48dp dokunma hedefi. */
@Composable
private fun HowToPill(onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        onClick = onClick, height = 32.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) { fg ->
        Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = theme.effectiveAccentColor, modifier = Modifier.size(16.dp))
        Text(
            theme.t("Hareketi gör", "How to"), color = fg, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, maxLines = 1
        )
    }
}

/** "3 × 10": genişlik aşılırsa 4sp adımla `floorSp`'ye kadar küçülür. */
@Composable
private fun DisplayPrescription(text: String, compact: Boolean, floorSp: Int) {
    var size by remember(text, compact) { mutableIntStateOf(if (compact) 80 else 104) }
    Text(
        text, style = Sinema.Display.copy(fontSize = size.sp), color = Color.White,
        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.didOverflowWidth && size > floorSp) size = (size - 4).coerceAtLeast(floorSp) }
    )
}

@Composable
private fun SetCapsules(
    exercise: Exercise,
    kind: SinemaSetKind,
    data: SinemaExerciseData,
    isCompleted: Boolean,
    compact: Boolean,
    accent: Color,
    onAccent: Color,
    actions: SinemaWorkoutActions,
    onOpenSheet: (Int) -> Unit
) {
    val theme = LocalAppTheme.current
    val h = 52.dp
    if (kind == SinemaSetKind.Activity) {
        val spec = remember(exercise.id) { sinemaTrackingSpec(exercise) }
        val dur = data.activityDuration.takeIf { it.isNotBlank() }?.let { theme.t("$it dk", "$it min") } ?: "—"
        CapsuleRow(
            label = spec.sportType.label.sinemaUpper(theme), value = dur,
            extra = null, done = isCompleted, height = 56.dp, accent = accent, onAccent = onAccent,
            toggleDescription = null,
            onToggle = { actions.onComplete(exercise) }, onOpen = { onOpenSheet(0) },
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    val sets = exercise.sets.coerceAtLeast(1)
    val twoCols = sets >= 5
    val locale = if (theme.language == com.cosmibit.profitness.core.theme.AppLanguage.ENGLISH) java.util.Locale.ENGLISH
        else java.util.Locale.forLanguageTag("tr-TR")
    val defaultWeightKg = when {
        exercise.weightKg > 0f -> exercise.weightKg
        exercise.category.equals("Bodyweight", ignoreCase = true) && data.profileWeightKg > 0f -> data.profileWeightKg
        else -> 0f
    }
    // Aktif satır: hareket bitmediyse ilk tamamlanmamış set (tek satır).
    val activeIdx = if (isCompleted) -1 else (0 until sets).firstOrNull { it !in data.doneSetIndices } ?: -1
    @Composable
    fun row(i: Int, modifier: Modifier) {
        val done = i in data.doneSetIndices
        val primary: SetValue
        var secondary: SetValue? = null
        if (kind == SinemaSetKind.Timed) {
            val secs = data.setDurations[i] ?: exercise.targetDurationSeconds?.toString() ?: exercise.reps.toDurationSetSecondsLabel()
            primary = SetValue(secs.ifBlank { "0" }, theme.t("sn", "sec"))
        } else {
            // kg birincil (başlıkta "3 × 12" zaten yazıyor; değişen bilgi yük). Ağırlık yoksa tekrar birincil olur.
            val w = data.setWeights[i]?.takeIf { it.isNotBlank() }
            val weight = when {
                w != null -> SetValue(formatWeightNumber(w, locale), "kg")
                defaultWeightKg > 0f -> SetValue(formatWeightNumber(defaultWeightKg, locale), "kg")
                else -> null
            }
            val reps = SetValue(exercise.reps.ifBlank { "—" }, theme.t("tekrar", "reps"))
            if (weight == null) primary = reps
            else if (twoCols) primary = weight
            else { primary = weight; secondary = reps }
        }
        SetRow(
            index = i, primary = primary, secondary = secondary,
            state = when { done -> SetRowState.Done; i == activeIdx -> SetRowState.Active; else -> SetRowState.Pending },
            accent = accent, onAccent = onAccent,
            toggleDescription = if (done) theme.t("GERİ AL", "UNDO") else theme.t("TAMAMLA", "COMPLETE"),
            onToggle = { actions.onToggleSet(exercise, i) }, onOpen = { onOpenSheet(i) }, modifier = modifier
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!twoCols) {
            repeat(sets) { i -> row(i, Modifier.fillMaxWidth()) }
        } else {
            (0 until sets).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    pair.forEach { i -> row(i, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Set satırı değeri: büyük rakam + küçük birim ("29" + "kg"). */
@Immutable
internal data class SetValue(val number: String, val unit: String)

internal enum class SetRowState { Pending, Active, Done }

/**
 * Set satırı (yon-cam-siyah "Set satırları" + mockup sol varyant): `[sıra diski 28] 12 [kg 92] [tekrar 1fr] [onay 30]`.
 * Bekleyen h52 cam .10→.06 · Aktif h60 cam .14 + vurgu kenar .75→.25 + vurgu hale .22, rakam 24sp ·
 * Tamamlanan h52 cam .10 + vurgu %18 örtü + vurgu kenar .45, dolu vurgu onay. Satır içi yazı #F4F5F7 (alfa ile soldurma yok).
 * Satıra dokunmak set sheet'ini açar; onay dairesi (48dp hedef) seti işaretler.
 */
@Composable
private fun SetRow(
    index: Int,
    primary: SetValue,
    secondary: SetValue?,
    state: SetRowState,
    accent: Color,
    onAccent: Color,
    toggleDescription: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val active = state == SetRowState.Active
    val done = state == SetRowState.Done
    val motion = com.cosmibit.profitness.core.theme.rememberSinemaMotionOn()
    val h by androidx.compose.animation.core.animateDpAsState(
        if (active) 60.dp else 52.dp, if (motion) tween(SinemaMotion.Standard, easing = SinemaMotion.EaseStandard) else androidx.compose.animation.core.snap(),
        label = "set_row_h"
    )
    val shape = RoundedCornerShape(h / 2)
    val onAcc = if (onAccent == Color.Black) Sinema.Ink else onAccent
    val measure = theme.accent2Color   // c2 = ölç: tamamlanan set
    val numSize = if (active) 24.sp else 20.sp
    val setLabel = theme.t("Set ${index + 1}", "Set ${index + 1}")
    Row(
        modifier
            .height(h)
            .sinemaCapsuleGlass(
                shape,
                fillTop = when (state) { SetRowState.Pending -> 0.10f; SetRowState.Active -> 0.14f; SetRowState.Done -> 0.10f },
                fillBottom = when (state) { SetRowState.Pending -> 0.06f; SetRowState.Active -> 0.14f; SetRowState.Done -> 0.10f },
                overlay = if (done) measure.copy(0.18f) else null,
                edgeColors = when (state) {
                    SetRowState.Pending -> null
                    SetRowState.Active -> listOf(accent.copy(0.75f), accent.copy(0.25f))
                    SetRowState.Done -> listOf(measure.copy(0.45f))
                },
                glow = if (active) accent.copy(0.22f) else null,
                shadow = !active
            )
            .semantics { contentDescription = setLabel }
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // sıra diski: 28dp, "1" 13sp SemiBold
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (active) Modifier.background(Sinema.Text)
                    else Modifier.border(1.5.dp, Color.White.copy(0.30f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}", color = if (active) Sinema.Ink else Sinema.Text,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                style = TextStyle(fontFamily = com.cosmibit.profitness.core.theme.AppFontFamily, fontFeatureSettings = "tnum")
            )
        }
        Spacer(Modifier.width(12.dp))
        if (secondary != null) {
            SetValueText(primary, numSize, Modifier.width(92.dp))
            SetValueText(secondary, numSize, Modifier.weight(1f))
        } else {
            SetValueText(primary, numSize, Modifier.weight(1f))
        }
        SetCheck(state, accent, measure, Sinema.Ink, toggleDescription, onToggle)
    }
}

@Composable
private fun SetValueText(v: SetValue, numSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = numSize, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em)) { append(v.number) }
            withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) { append("\u2009" + v.unit) }
        },
        color = Sinema.Text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontFamily = com.cosmibit.profitness.core.theme.AppFontFamily, fontFeatureSettings = "tnum"),
        modifier = modifier
    )
}

/** Onay dairesi: görsel 30dp, dokunma 48dp. Bekleyen halka beyaz .40 · aktif 2dp c1 · tamamlanan dolu c2 + Ink tik. */
@Composable
private fun SetCheck(state: SetRowState, accent: Color, doneColor: Color, onDone: Color, description: String, onToggle: () -> Unit) {
    val done = state == SetRowState.Done
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    val motion = com.cosmibit.profitness.core.theme.rememberSinemaMotionOn()
    LaunchedEffect(done) {
        if (done && !first && motion) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, SinemaMotion.pop())
        }
        first = false
    }
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Checkbox, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(30.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(CircleShape)
                .then(
                    when (state) {
                        SetRowState.Done -> Modifier.background(doneColor)
                        SetRowState.Active -> Modifier.border(2.dp, accent, CircleShape)
                        SetRowState.Pending -> Modifier.border(1.5.dp, Color.White.copy(0.40f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(SinemaIcons.Check, null, tint = onDone, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CapsuleRow(
    label: String,
    value: String,
    extra: String?,
    done: Boolean,
    height: Dp,
    accent: Color,
    onAccent: Color,
    toggleDescription: String?,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        modifier, selected = done, onClick = onOpen, height = height,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp)
    ) { fg ->
        Text(
            label, style = Sinema.Label.copy(fontWeight = FontWeight.Bold), color = fg, maxLines = 1,
            softWrap = false, modifier = Modifier.widthIn(min = 50.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (extra != null) "$value · $extra" else value, color = fg.copy(0.80f), fontSize = 14.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f)
        )
        CheckCircle(done, accent, onAccent, toggleDescription ?: if (done) theme.t("GERİ AL", "UNDO") else theme.t("TAMAMLA", "COMPLETE"), onToggle, height)
    }
}

@Composable
private fun CheckCircle(done: Boolean, accent: Color, onAccent: Color, description: String, onToggle: () -> Unit, hit: Dp) {
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    val motion = rememberSinemaMotionOn()
    LaunchedEffect(done) {
        if (done && !first && motion) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, SinemaMotion.pop())
        }
        first = false
    }
    Box(
        Modifier
            .size(hit)
            .semantics { contentDescription = description }
            .clickable(role = Role.Checkbox, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(onAccent)
                    else Modifier.border(2.dp, Color.White.copy(0.50f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(SinemaIcons.Check, null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ProgressRow(progress: Float, showChevron: Boolean, animEnabled: Boolean) {
    val theme = LocalAppTheme.current
    val pct = (progress * 100).toInt()
    val bob = if (animEnabled && showChevron) {
        rememberInfiniteTransition(label = "chev").animateFloat(
            0f, 3f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "chev_y"
        )
    } else null
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            theme.t("%$pct", "$pct%"), color = Color.White.copy(0.75f), fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em, style = TextStyle(fontFeatureSettings = "tnum")
        )
        Spacer(Modifier.weight(1f))
        if (showChevron) {
            val d = LocalDensity.current
            Icon(
                SinemaIcons.Chevron, null, tint = Color.White.copy(0.75f),
                modifier = Modifier.size(16.dp).graphicsLayer { translationY = with(d) { (bob?.value ?: 0f).dp.toPx() } }
            )
        }
    }
}

@Composable
private fun StartButton(label: String, dim: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f, SinemaMotion.press(), label = "start_scale"
    )
    val shape = RoundedCornerShape(28.dp)
    // Çizim: düz vurgu dolgusu + üstte inset 1px beyaz %40 + altta `0 8px 20px` vurgu %40 parlaması.
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (dim) 0.5f else 1f }
            .sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, shape)
            .clip(shape)
            .background(accent)
            .drawWithContent {
                drawContent()
                val line = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(0f to Color.White.copy(0.40f), (line * 6f / size.height) to Color.Transparent),
                    cornerRadius = CornerRadius(28.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(line * 1.5f)
                )
            }
            .clickable(src, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(SinemaIcons.Play, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                label, color = theme.effectiveOnAccentColor, fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em
            )
        }
    }
}

// ── Dinlenme / atlanan program / kutlama ─────────────────────────────────────────────────────

/**
 * Yatay kaydırma sırasında yan gün önizlemesi: foto + gün adı + hareket sayısı (oturunca gerçek sayfa üstüne gelir).
 * Foto, gerçek sayfanın açılış sayfasıyla aynı: ilk tamamlanmamış hareket; hepsi bittiyse son hareket (kutlama).
 * [showText] false iken yalnız foto katmanı kalır (gerçek içeriğin altında, sıçramasız geçiş için).
 */
@Composable
private fun DayPreviewPage(st: WorkoutDayState?, showText: Boolean = true) {
    val theme = LocalAppTheme.current
    if (st == null || (st.day.isRestDay && st.day.exercises.isEmpty())) {
        if (showText) RestPage(isSkipped = false)
        else Box(Modifier.fillMaxSize())   // dinlenme: foto yok, ortak zemin görünür
        return
    }
    val exs = st.day.exercises
    val photoEx = exs.firstOrNull { it.id !in st.completedIds } ?: exs.lastOrNull()
    Box(Modifier.fillMaxSize().clipToBounds()) {
        PhotoLayer(photoEx?.let { it.image.ifBlank { categoryImageUrl(it.category) } } ?: categoryImageUrl(""), isSettled = false, animEnabled = false, offset = { 0f })
        Scrims()
        if (showText) Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = SinemaContentBottom + navInset() + 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(compactWorkoutTitle(st.day.title, theme), style = Sinema.Display.copy(fontSize = 40.sp), color = Color.White, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t("${st.day.exercises.size} hareket", "${st.day.exercises.size} exercises"),
                style = Sinema.Body, color = Color.White.copy(0.85f)
            )
        }
    }
}

@Composable
private fun RestPage(isSkipped: Boolean) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val animEnabled = rememberSinemaMotionOn()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = SinemaContentBottom + navInset() + 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                if (isSkipped) strings.skippedProgramTitle else strings.restDayTitle,
                style = Sinema.Display.copy(fontSize = 40.sp), color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSkipped) strings.skippedProgramBody else strings.restDaySubtitle,
                style = Sinema.Body, color = Color.White.copy(0.85f)
            )
        }
    }
}

@Composable
private fun CelebrationPage(
    programTitle: String,
    total: Int,
    photoUrl: String,
    isSettled: Boolean,
    animEnabled: Boolean,
    offset: () -> Float
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    var sweepKey by remember { mutableIntStateOf(0) }
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(isSettled) { if (isSettled && !played) { played = true; sweepKey++ } }
    Box(Modifier.fillMaxSize()) {
        PhotoLayer(photoUrl, isSettled, animEnabled, offset)
        Scrims()
        SweepLayer(sweepKey, animEnabled)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = SinemaContentBottom + navInset() + 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text("%d / %d".format(total, total), style = Sinema.Label, color = Color.White.copy(0.70f))
            Spacer(Modifier.height(6.dp))
            Text(theme.t("%100", "100%"), style = Sinema.Display, color = Color.White, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(programTitle, style = Sinema.Title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Sabit üst çubuk ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SinemaTopBar(
    programTitle: String,
    exercises: List<Exercise>,
    completedIds: Set<String>,
    days: ImmutableList<WorkoutDayState>,
    selectedDayIdx: Int,
    liveDayIdx: Int,
    onSelectDay: (Int) -> Unit,
    streak: Int,
    showSegments: Boolean,
    dataFor: (Exercise) -> SinemaExerciseData,
    topExtra: Dp,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = statusTop + 8.dp + topExtra)
    ) {
        if (showSegments) {
            // Çizim: segmentler status+8'de (3dp), program satırı 12dp altında başlar.
            Row(
                Modifier
                    .fillMaxWidth().height(12.dp)
                    .semantics { contentDescription = programTitle }
                    .clickable(role = Role.Button, onClick = onOpenList),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top
            ) {
                exercises.forEach { ex ->
                    val frac = if (ex.id in completedIds) 1f
                    else (dataFor(ex).doneSetIndices.size.toFloat() / ex.sets.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val f by animateFloatAsState(frac, tween(SinemaMotion.Slow, easing = SinemaMotion.EaseStandard), label = "seg")
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(0.30f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(theme.accent2Color))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                programTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).then(if (showSegments) Modifier.clickable(role = Role.Button, onClick = onOpenList) else Modifier)
            )
            Spacer(Modifier.width(8.dp))
            DayPill(days, liveDayIdx, onSelectDay, theme)
            Spacer(Modifier.width(8.dp))
            val streakLabel = if (streak > 0) theme.t("$streak günlük serin aktif", "$streak-day streak")
            else theme.t("Serini başlat", "Start your streak")
            Row(
                Modifier
                    .height(32.dp)
                    .sinemaCapsuleGlass(CircleShape, blur = 16.dp)
                    .semantics(mergeDescendants = true) { contentDescription = streakLabel }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(SinemaIcons.Flame, null, tint = theme.accent3Color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    streak.coerceAtLeast(0).toString(), color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

@Composable
private fun DayPill(
    days: ImmutableList<WorkoutDayState>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit,
    theme: AppThemeState
) {
    var open by remember { mutableStateOf(false) }
    val accent = theme.effectiveAccentColor
    val selectedLabel = days.getOrNull(selectedIdx)?.let { localizedWorkoutDayLabel(it.day.day, theme) } ?: ""
    Box {
        SinemaCapsule(
            onClick = { open = !open }, height = 32.dp, shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) { fg ->
            Text(selectedLabel, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.width(5.dp))
            // gün sayısı kadar ince nokta: kaydırmayla güncellenir
            if (days.size in 2..8) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    days.forEachIndexed { i, _ ->
                        Box(Modifier.size(if (i == selectedIdx) 5.dp else 3.dp).clip(CircleShape).background(fg.copy(if (i == selectedIdx) 1f else 0.4f)))
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            Icon(SinemaIcons.Chevron, null, tint = fg, modifier = Modifier.size(14.dp))
        }
        if (open) {
            val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect, windowSize: IntSize,
                        layoutDirection: LayoutDirection, popupContentSize: IntSize
                    ) = IntOffset(anchorBounds.right - popupContentSize.width, anchorBounds.bottom + gap)
                },
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    // Sinema menü yüzeyi: sinemaCard r20 (pencere dışı olduğundan blur yok), satırlar 48dp
                    Modifier.width(132.dp).sinemaCard(theme, RoundedCornerShape(20.dp)).padding(6.dp)
                ) {
                    days.forEachIndexed { i, st ->
                        val sel = i == selectedIdx
                        Box(
                            Modifier
                                .fillMaxWidth().height(48.dp)
                                .then(if (sel) Modifier.sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, RoundedCornerShape(14.dp)) else Modifier)
                                .clip(RoundedCornerShape(14.dp))
                                .then(if (sel) Modifier.background(accent) else Modifier)
                                .clickable(role = Role.Button) { open = false; onSelect(i) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                localizedWorkoutDayLabel(st.day.day, theme),
                                color = if (sel) theme.effectiveOnAccentColor else Sinema.text(theme),
                                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Liste sheet'i ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseListSheet(
    exercises: List<Exercise>,
    completedIds: Set<String>,
    pagerState: PagerState,
    extras: @Composable (dismiss: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val nextId = exercises.firstOrNull { it.id !in completedIds }?.id
    SinemaGlassSheet(onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            item(key = "extras") { extras(onDismiss) }
            itemsIndexed(exercises, key = { _, e -> e.id }) { idx, ex ->
                Row(
                    Modifier
                        .fillMaxWidth().height(72.dp)
                        .clickable(role = Role.Button) {
                            onDismiss()
                            scope.launch { pagerState.animateScrollToPage(idx, animationSpec = tween(Sinema.PagerSettleMs, easing = FastOutSlowInEasing)) }
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))) {
                        SceneImage(
                            url = ex.image.ifBlank { categoryImageUrl(ex.category) },
                            modifier = Modifier.fillMaxSize(), requestWidthPx = 224, requestHeightPx = 224
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.exerciseCompactDisplayName(ex.name), color = Color.White, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            exercisePrescription(ex, isActivityBasedExercise(ex), theme),
                            color = Color.White.copy(0.70f), fontSize = 13.sp, maxLines = 1,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    SceneStatusMark(completed = ex.id in completedIds, isNext = ex.id == nextId)
                }
            }
        }
    }
}
