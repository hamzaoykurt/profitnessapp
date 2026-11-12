package com.cosmibit.profitness.presentation.workout

import com.cosmibit.profitness.core.theme.sinemaGlow
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.LocalSinemaHaze
import com.cosmibit.profitness.core.theme.LocalSinemaNav
import com.cosmibit.profitness.core.theme.SinemaIcons
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.em
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.cosmibit.profitness.core.theme.Sinema
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

@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

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
    modifier: Modifier = Modifier
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
    val animEnabled = rememberAnimationsEnabled()

    key(selectedDayIdx) {
        val initial = remember {
            if (single) 0
            else exercises.indexOfFirst { it.id !in completedIds }.takeIf { it >= 0 } ?: if (allDone) n else 0
        }
        val pagerState = rememberPagerState(initialPage = initial) { pageCount }
        val scrolling = pagerState.isScrollInProgress
        LaunchedEffect(scrolling) {
            if (!scrolling) delay(200)
            nav.pagerBusy = scrolling
        }
        val settled = pagerState.settledPage
        LaunchedEffect(settled, single) {
            if (!single) exercises.getOrNull(settled)?.let { actions.onLoadLastSession(it) }
        }

        // Oto-geçiş: bir hareketin SON seti işaretlenince (completedIds'e tek id eklenir) ~600ms sonra bir sonraki
        // sayfaya (son hareketteyse kutlama sayfasına) kay. Bu sürede kullanıcı sayfa değiştirir/kaydırırsa iptal.
        // shortcut: yalnız UI scroll; completedIds'in toplu yüklenmesinde (added.size != 1) tetiklenmez.
        var prevDone by remember { mutableStateOf(completedIds) }
        LaunchedEffect(completedIds) {
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

        var sheet by remember { mutableStateOf<Pair<String, Int>?>(null) }   // exerciseId, setIndex
        var showList by remember { mutableStateOf(false) }

        Box(modifier.fillMaxSize().background(Color.Black)) {
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

            SinemaTopBar(
                programTitle = programTitle, exercises = exercises, completedIds = completedIds,
                days = days, selectedDayIdx = selectedDayIdx, onSelectDay = onSelectDay,
                streak = streak, showSegments = !single, dataFor = dataFor,
                topExtra = topExtra, onOpenList = { showList = true },
                modifier = Modifier.align(Alignment.TopStart)
            )
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

/** Foto: 1.5 kat yükseklikte, paralaks + Ken Burns (yalnız yerleşik sayfa). */
@Composable
private fun PhotoLayer(
    url: String,
    isSettled: Boolean,
    animEnabled: Boolean,
    offset: () -> Float,
    modifier: Modifier = Modifier
) {
    val kenBurns = if (isSettled && animEnabled) {
        rememberInfiniteTransition(label = "ken_burns").animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(Sinema.KenBurnsMs, easing = LinearEasing), RepeatMode.Reverse),
            label = "kb"
        )
    } else null
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
                val s = if (animEnabled) kenBurns?.value ?: 1f else 1f
                scaleX = s; scaleY = s
            }
    ) {
        SceneImage(
            url = url, modifier = Modifier.fillMaxSize(),
            requestWidthPx = 1080, requestHeightPx = 1920,
            alignment = Alignment.Center,
            fallbackIconSize = 64.dp, fallbackIconBias = -0.2f, fallbackGradient = true
        )
    }
}

/** Üst + alt karartma (çizim: `.tg` 140dp .55→0, `.bgd` 470dp 0→.88 @%78). */
@Composable
internal fun Scrims(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().drawWithCache {
            val h = size.height
            val topH = 140.dp.toPx().coerceAtMost(h)
            val botH = 470.dp.toPx().coerceAtMost(h)
            val top = Brush.verticalGradient(listOf(Color.Black.copy(0.55f), Color.Transparent), startY = 0f, endY = topH)
            val bottom = Brush.verticalGradient(
                0f to Color.Transparent, 0.78f to Color.Black.copy(0.88f), 1f to Color.Black.copy(0.88f),
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
            p.animateTo(1f, tween(Sinema.SweepMs, easing = LinearEasing))
            p.snapTo(0f)
        }
    }
    Box(
        Modifier.fillMaxSize().drawBehind {
            val v = p.value
            if (v > 0f && v < 1f) {
                val bw = size.width * 0.55f
                val x = -bw + (size.width + bw) * v
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(0.16f), Color.Transparent),
                        startX = x, endX = x + bw
                    ),
                    topLeft = Offset(x, 0f), size = Size(bw, size.height)
                )
            }
        }
    )
}

@Composable
private fun navInset(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
            PhotoLayer(photo, isSettled, animEnabled, offset)
            Scrims()
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
                Text(
                    "%02d / %02d".format(page + 1, total), style = Sinema.Label, color = Color.White.copy(0.70f)
                )
            }
            Spacer(Modifier.height(6.dp))
            // Başlık; dokununca hareket detayı ("Hareketi gör") açılır — çizimde ayrı düğme yok
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
    val defaultWeightKg = when {
        exercise.weightKg > 0f -> exercise.weightKg
        exercise.category.equals("Bodyweight", ignoreCase = true) && data.profileWeightKg > 0f -> data.profileWeightKg
        else -> 0f
    }
    @Composable
    fun capsule(i: Int, modifier: Modifier) {
        val done = i in data.doneSetIndices
        val label = theme.t("Set ${i + 1}", "Set ${i + 1}").sinemaUpper(theme)
        val value: String
        var extra: String? = null
        if (kind == SinemaSetKind.Timed) {
            val secs = data.setDurations[i] ?: exercise.targetDurationSeconds?.toString() ?: exercise.reps.toDurationSetSecondsLabel()
            value = theme.t("${secs.ifBlank { "0" }} sn", "${secs.ifBlank { "0" }} sec")
        } else {
            // Çizimde "10 tekrar"; ağırlık gerçek veri olduğu için girilmişse/varsa yanına eklenir.
            val w = data.setWeights[i]?.takeIf { it.isNotBlank() }
            val weight = if (w != null) "$w kg" else if (defaultWeightKg > 0f) "${"%.0f".format(defaultWeightKg)} kg" else null
            val reps = theme.t("${exercise.reps} tekrar", "${exercise.reps} reps")
            if (twoCols) value = weight ?: reps else { value = reps; extra = weight }
        }
        CapsuleRow(
            label = label, value = value,
            extra = extra,
            done = done, height = h, accent = accent, onAccent = onAccent,
            toggleDescription = if (done) theme.t("GERİ AL", "UNDO") else theme.t("TAMAMLA", "COMPLETE"),
            onToggle = { actions.onToggleSet(exercise, i) }, onOpen = { onOpenSheet(i) }, modifier = modifier
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!twoCols) {
            repeat(sets) { i -> capsule(i, Modifier.fillMaxWidth()) }
        } else {
            (0 until sets).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i -> capsule(i, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
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
    LaunchedEffect(done) {
        if (done && !first) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
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
        if (pressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "start_scale"
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

@Composable
private fun RestPage(isSkipped: Boolean) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val animEnabled = rememberAnimationsEnabled()
    Box(Modifier.fillMaxSize()) {
        PhotoLayer(categoryImageUrl(""), isSettled = true, animEnabled = animEnabled, offset = { 0f })
        Scrims()
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
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 1f to accent.copy(0.30f))
            )
        )
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
                    val f by animateFloatAsState(frac, tween(450), label = "seg")
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(0.30f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(accent))
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
            DayPill(days, selectedDayIdx, onSelectDay, theme)
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
                Icon(SinemaIcons.Flame, null, tint = accent, modifier = Modifier.size(14.dp))
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
        Row(
            Modifier
                .height(32.dp)
                .sinemaCapsuleGlass(CircleShape, blur = 16.dp)
                .clickable(role = Role.DropdownList) { open = !open }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.width(5.dp))
            Icon(SinemaIcons.Chevron, null, tint = Color.White, modifier = Modifier.size(14.dp))
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
                    // Çizim `.dd`: 96 genişlik, 6 iç boşluk, r18, siyah %60 + kenar (pencere dışı olduğundan blur yok)
                    Modifier.width(96.dp).sinemaCapsuleGlass(RoundedCornerShape(18.dp), fillAlpha = 0.6f).padding(6.dp)
                ) {
                    days.forEachIndexed { i, st ->
                        val sel = i == selectedIdx
                        Box(
                            Modifier
                                .fillMaxWidth().height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(if (sel) Modifier.background(accent) else Modifier)
                                .clickable(role = Role.Button) { open = false; onSelect(i) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                localizedWorkoutDayLabel(st.day.day, theme),
                                color = if (sel) theme.effectiveOnAccentColor else Color.White,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
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
