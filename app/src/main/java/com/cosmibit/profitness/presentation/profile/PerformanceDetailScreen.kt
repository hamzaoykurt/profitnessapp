package com.cosmibit.profitness.presentation.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.accent2Color
import com.cosmibit.profitness.core.theme.accent2TextColor
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.components.LocalFloatingTimerReserve
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ── Veri analizi: "sezon raporu" (.ekip/yon-veri-analizi.md) ───────────────────
// Yalnız ProfileState'teki gerçek veri; veri yoksa öğe gizlenir / boş durum.

private val CoverHeight = 400.dp
private val Dim = Sinema.White.copy(0.72f)
private val LabelStyle = Sinema.Label.copy(letterSpacing = 0.08.em)

@Composable
private fun PText(
    text: String,
    size: Int,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    color: Color = Sinema.White,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {
    Text(
        text, modifier = modifier, color = color, fontSize = size.sp, fontWeight = weight, fontFamily = AppFontFamily,
        letterSpacing = letterSpacing, lineHeight = lineHeight, maxLines = maxLines, textAlign = textAlign,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.sinemaUpper(LocalAppTheme.current), modifier = modifier, color = Dim, style = LabelStyle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDetailScreen(
    onBack   : () -> Unit,
    onNavigateToWeightTracking      : () -> Unit = {},
    onNavigateToExerciseProgression : () -> Unit = {},
    onStartWorkout                  : () -> Unit = onBack,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val english = theme.language == AppLanguage.ENGLISH
    var showCalculators by remember { mutableStateOf(false) }
    var rangeWeeks by rememberSaveable { mutableStateOf(13) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasWorkouts = state.totalWorkouts > 0

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SinemaNavHeight + 24.dp + navBarBottom)
        ) {
            item {
                PerformanceCover(
                    state = state, theme = theme, english = english,
                    onBack = onBack, onCalculate = { showCalculators = true }, onStartWorkout = onStartWorkout
                )
            }

            if (!state.isLoading) {
                if (hasWorkouts) {
                    item {
                        RhythmSection(
                            counts = state.weeklyWorkoutCounts, weeks = rangeWeeks, onWeeks = { rangeWeeks = it },
                            theme = theme, english = english,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)
                        )
                    }
                    item { TotalsSection(state, theme, english, Modifier.padding(horizontal = 20.dp).padding(top = 40.dp)) }
                } else {
                    item {
                        PText(
                            theme.t("Antrenman kaydettikçe ritmin burada oluşur.", "Your rhythm builds here as you log workouts."),
                            14, FontWeight.Normal, color = Dim, lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)
                        )
                    }
                }
                item {
                    ExercisePoster(
                        theme = theme, onClick = onNavigateToExerciseProgression,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 40.dp)
                    )
                }
                item {
                    BodySection(
                        state = state, theme = theme, english = english, onWeightTracking = onNavigateToWeightTracking,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 40.dp)
                    )
                }
                item { StreakSection(state, theme, english, Modifier.padding(horizontal = 20.dp).padding(top = 40.dp)) }
            }
        }
    }

    if (showCalculators) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.94f
        ModalBottomSheet(
            onDismissRequest = { showCalculators = false },
            sheetState = sheetState,
            containerColor = Sinema.elevated(theme),
            contentColor = Sinema.text(theme),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Sinema.line(theme))
                )
            }
        ) {
            CalculationsSheet(
                state = state,
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    }
}

// ── 1. Kapak: foto + dev rakam + hafta halkası ────────────────────────────────

@Composable
private fun PerformanceCover(
    state: ProfileState,
    theme: AppThemeState,
    english: Boolean,
    onBack: () -> Unit,
    onCalculate: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val strings = theme.strings
    val counts = state.weeklyWorkoutCounts
    val thisWeek = counts.lastOrNull() ?: 0
    val since = weeksSinceLastWorkout(counts)
    val todayIdx = LocalDate.now().dayOfWeek.value - 1
    val loading = state.isLoading
    val empty = !loading && state.totalWorkouts <= 0
    val haptic = LocalHapticFeedback.current
    var ringSel by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(ringSel) { if (ringSel != null) { delay(2000); ringSel = null } }

    Box(Modifier.fillMaxWidth().height(CoverHeight)) {
        // Foto: alt 200dp zemine söner (maske), üstüne %35 koyu örtü
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(categoryImageUrl("")).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Sinema.Ink.copy(0.35f))
                    val fadeStart = (200.dp.toPx() / size.height).coerceIn(0f, 1f)
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, fadeStart to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        // Üst sıra: geri · başlık · Hesapla
        Box(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp).padding(horizontal = 20.dp).height(44.dp)
        ) {
            // Görsel 44dp; dokunma hedefi 48dp (tıklama dış 48dp kutuda)
            Box(
                Modifier.align(Alignment.CenterStart).requiredSize(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                SinemaCapsule(
                    Modifier.size(44.dp), height = 44.dp, shape = CircleShape,
                    contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.Center
                ) { fg ->
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, theme.t("Geri", "Back"), tint = fg, modifier = Modifier.size(20.dp))
                }
            }
            PText(
                theme.t("Veri analizi", "Data analysis"), 16, FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center), maxLines = 1
            )
            Box(
                Modifier.align(Alignment.CenterEnd).requiredHeight(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onCalculate),
                contentAlignment = Alignment.Center
            ) {
                SinemaCapsule(height = 44.dp, contentPadding = PaddingValues(horizontal = 16.dp)) { fg ->
                    Icon(Icons.Rounded.Calculate, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    PText(theme.t("Hesapla", "Calculate"), 14, FontWeight.SemiBold, color = fg, maxLines = 1)
                }
            }
        }

        // Halka dokunuşu balonu
        ringSel?.let { i ->
            val level = state.weeklyActivity.getOrElse(i) { 0f }
            val day = java.time.DayOfWeek.of(i + 1).getDisplayName(java.time.format.TextStyle.FULL, uiLocale(english))
            GlassBubble(
                "$day · " + if (level > 0f) theme.t("aktif", "active") else theme.t("antrenman yok", "no workout"),
                Modifier.align(Alignment.TopEnd).padding(top = 196.dp, end = 20.dp)
            )
        }

        // Sol: dev rakam / boş durum · Sağ: hafta halkası
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp + LocalFloatingTimerReserve.current),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                when {
                    loading -> {
                        SectionLabel(theme.t("Bu hafta", "This week"))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.size(width = 112.dp, height = 96.dp).clip(RoundedCornerShape(16.dp)).background(Sinema.White.copy(0.08f)))
                    }
                    empty -> {
                        PText(
                            theme.t("İlk antrenmanını kaydet", "Log your first workout"), 22, FontWeight.ExtraBold,
                            lineHeight = 26.sp, letterSpacing = (-0.02).em
                        )
                        Spacer(Modifier.height(16.dp))
                        SinemaCapsule(selected = true, onClick = onStartWorkout, height = 44.dp, contentPadding = PaddingValues(horizontal = 16.dp)) { fg ->
                            PText(theme.t("Antrenmana başla", "Start a workout"), 14, FontWeight.Bold, color = fg, maxLines = 1)
                        }
                    }
                    else -> {
                        SectionLabel(theme.t("Bu hafta", "This week"))
                        Spacer(Modifier.height(10.dp))
                        com.cosmibit.profitness.core.theme.SinemaCount(thisWeek) { n ->
                            Text(
                                n.toString(),
                                style = Sinema.Display.copy(fontSize = 112.sp, lineHeight = 0.9.em, letterSpacing = (-0.04).em),
                                color = Sinema.White, maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        PText(
                            if (english && thisWeek == 1) "workout" else theme.t("antrenman", "workouts"),
                            16, FontWeight.SemiBold
                        )
                        if (thisWeek == 0 && since != null) {
                            Spacer(Modifier.height(8.dp))
                            PText(
                                theme.t("Son antrenman: $since hafta önce", "Last workout: $since weeks ago"),
                                13, FontWeight.Normal, color = Dim
                            )
                        }
                    }
                }
            }
            WeekRing(
                levels = state.weeklyActivity,
                todayIdx = todayIdx,
                loading = loading,
                centerLabel = strings.dayAbbreviations.getOrElse(todayIdx) { "" },
                selected = ringSel,
                onSelect = { i ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    ringSel = if (ringSel == i) null else i
                },
                accent = theme.accent2Color,   // c2 = ölç
                description = theme.t("Bu haftanın günlük aktivitesi", "This week's daily activity"),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

/** Cam balon (h32): dokunulan kapsül/gün bilgisi. */
@Composable
private fun GlassBubble(text: String, modifier: Modifier = Modifier) {
    SinemaCapsule(modifier, height = 32.dp, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp)) {
        PText(text, 12, FontWeight.SemiBold, maxLines = 1)
    }
}

/** Ø132, 7 yay (Pzt…Paz), kalınlık 10, round cap; dolgu = gün tamamlanma oranı, bugün tam vurgu + glow. */
@Composable
private fun WeekRing(
    levels: List<Float>,
    todayIdx: Int,
    loading: Boolean,
    centerLabel: String,
    selected: Int?,
    onSelect: (Int) -> Unit,
    accent: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(132.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val dx = p.x - c.x
                    val dy = p.y - c.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 36.dp.toPx() || dist > 72.dp.toPx()) return@detectTapGestures
                    val deg = (Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0 + 360.0) % 360.0
                    onSelect((deg / (360.0 / 7.0)).toInt().coerceIn(0, 6))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val r = 56.dp.toPx()
            val tl = Offset(size.width / 2f - r, size.height / 2f - r)
            val step = 360f / 7f
            val gap = Math.toDegrees(((4.dp.toPx() + stroke) / r).toDouble()).toFloat() // görünür boşluk 4dp
            for (i in 0 until 7) {
                val level = if (loading) 0f else levels.getOrElse(i) { 0f }.coerceIn(0f, 1f)
                val isToday = i == todayIdx
                val chosen = i == selected
                val start = -90f + i * step + gap / 2f
                val sweep = step - gap
                val arcTl = tl
                val arcSize = Size(2 * r, 2 * r)
                val active = level > 0f
                val full = (isToday && active) || chosen
                val color = when {
                    full -> accent
                    active -> accent.copy(ringArcAlpha(level))
                    isToday && !loading -> Sinema.White.copy(0.40f) // bugün, henüz antrenman yok
                    else -> Sinema.White.copy(0.14f)
                }
                drawArc(color, start, sweep, false, arcTl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        if (centerLabel.isNotEmpty()) PText(centerLabel, 13, FontWeight.Bold, maxLines = 1)
    }
}

// ── 2. Ritim: aralık seçici + ortalama + kapsül grafiği ───────────────────────

@Composable
private fun RhythmSection(
    counts: List<Int>,
    weeks: Int,
    onWeeks: (Int) -> Unit,
    theme: AppThemeState,
    english: Boolean,
    modifier: Modifier = Modifier
) {
    val slice = weekSlice(counts, weeks)
    val avg = weeklyAverage(slice)
    val best = bestWeek(slice)
    val heroAvg = averageIsHero(avg, best)
    val avgText = formatNumber(avg.toDouble(), 1, english)
    val perWeek = theme.t("/hf", "/wk")
    val unitWorkouts = theme.t("antrenman", "workouts")

    Column(modifier) {
        RangeSegment(weeks = weeks, onWeeks = onWeeks, english = english)

        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                SectionLabel(if (heroAvg) theme.t("Ortalama", "Average") else theme.t("En iyi hafta", "Best week"))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (heroAvg) avgText else best.toString(),
                        style = Sinema.Metric.copy(fontSize = 56.sp, lineHeight = 1.0.em, letterSpacing = (-0.03).em),
                        color = Sinema.White, maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    PText(if (heroAvg) perWeek else unitWorkouts, 16, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PText(
                    if (heroAvg) theme.t("En iyi hafta", "Best week") else theme.t("Ortalama", "Average"),
                    12, FontWeight.SemiBold, color = Dim
                )
                Spacer(Modifier.height(4.dp))
                PText(if (heroAvg) best.toString() else avgText + perWeek, 20, FontWeight.Bold)
            }
        }

        RhythmChart(slice = slice, weeks = weeks, best = best, avg = avg, theme = theme, english = english)

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PText(theme.t("$weeks hf önce", "$weeks wk ago"), 11, FontWeight.Medium, color = Dim)
            PText(theme.t("Bu hafta", "This week"), 11, FontWeight.Medium, color = Dim)
        }
    }
}

/** `.glass` segment 168×44: "4 hf | 13 hf"; seçili = dolu vurgu (.stone). */
@Composable
private fun RangeSegment(weeks: Int, onWeeks: (Int) -> Unit, english: Boolean) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    // Görsel 168x44 cam; dokunma hedefi her yarı 84x48 (şeffaf katman)
    Box(Modifier.width(168.dp).requiredHeight(48.dp), contentAlignment = Alignment.Center) {
        SinemaCapsule(
            Modifier.fillMaxWidth(), height = 44.dp, contentPadding = PaddingValues(4.dp)
        ) {
            listOf(4, 13).forEach { w ->
                val sel = w == weeks
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (sel) Modifier.sinemaGlow(accent.copy(0.4f), 12.dp, 4.dp, CircleShape) else Modifier)
                        .clip(CircleShape)
                        .background(if (sel) accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    PText(
                        if (english) "$w wk" else "$w hf", 13, if (sel) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (sel) theme.effectiveOnAccentColor else Sinema.White, maxLines = 1
                    )
                }
            }
        }
        Row(Modifier.matchParentSize()) {
            listOf(4, 13).forEach { w ->
                val sel = w == weeks
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { this.selected = sel }
                        .clickable(role = Role.RadioButton) { if (!sel) onWeeks(w) }
                )
            }
        }
    }
}

@Composable
private fun RhythmChart(
    slice: List<Int>,
    weeks: Int,
    best: Int,
    avg: Float,
    theme: AppThemeState,
    english: Boolean
) {
    val accent = theme.accent2Color   // c2 = ölç (çubuklar)
    val motion = rememberSinemaMotionOn()
    val haptic = LocalHapticFeedback.current
    val grow = remember { Animatable(1f) }
    LaunchedEffect(weeks) {
        if (motion) {
            grow.snapTo(0.4f)
            grow.animateTo(1f, tween(SinemaMotion.Standard, easing = SinemaMotion.EaseStandard))
        } else grow.snapTo(1f)
    }
    var sel by remember(weeks) { mutableStateOf<Int?>(null) }
    LaunchedEffect(sel) { if (sel != null) { delay(2000); sel = null } }
    val n = slice.size
    val chartH = 120.dp
    val maxBar = 100.dp
    val avgFrac = if (best > 0) (avg / best).coerceIn(0f, 1f) else 0f
    val summary = theme.t(
        "Haftalık antrenmanlar, son $weeks hafta. Ortalama ${formatNumber(avg.toDouble(), 1, english)}, en iyi hafta $best.",
        "Weekly workouts, last $weeks weeks. Average ${formatNumber(avg.toDouble(), 1, english)}, best week $best."
    )

    Box(Modifier.fillMaxWidth().height(chartH).semantics { contentDescription = summary }) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(n, weeks) {
                detectTapGestures { p ->
                    if (n == 0) return@detectTapGestures
                    val bw = capsuleWidthDp(weeks).dp.toPx()
                    val pitch = if (n > 1) (size.width - bw) / (n - 1) else 1f
                    val i = ((p.x - bw / 2f) / pitch).roundToInt().coerceIn(0, n - 1)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sel = if (sel == i) null else i
                }
            }
        ) {
            if (n == 0) return@Canvas
            val bw = capsuleWidthDp(weeks).dp.toPx().coerceAtMost(size.width / n)
            val pitch = if (n > 1) (size.width - bw) / (n - 1) else 0f
            val radius = CornerRadius(8.dp.toPx().coerceAtMost(bw / 2f))
            slice.forEachIndexed { i, c ->
                val x = i * pitch
                val isNow = i == n - 1
                val chosen = i == sel
                if (c <= 0) {
                    val d = 6.dp.toPx()
                    drawCircle(Sinema.White.copy(if (chosen) 0.5f else 0.20f), d / 2f, Offset(x + bw / 2f, size.height - d / 2f))
                } else {
                    val h = (capsuleFraction(c, best) * maxBar.toPx() * grow.value).coerceAtLeast(8.dp.toPx())
                    val full = isNow || chosen
                    drawRoundRect(if (full) accent else accent.copy(0.55f), Offset(x, size.height - h), Size(bw, h), radius)
                }
            }
            if (best > 0) {
                val y = size.height - avgFrac * maxBar.toPx()
                drawLine(
                    Sinema.White.copy(0.40f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
            }
        }
        if (best > 0) {
            PText(
                theme.t("ort.", "avg."), 11, FontWeight.SemiBold, color = Dim,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = maxBar * avgFrac + 4.dp)
            )
        }
        sel?.let { i ->
            val c = slice.getOrElse(i) { 0 }
            val start = weekStart(LocalDate.now(), n - 1 - i)
            val label = (if (english && c == 1) "1 workout" else "$c " + theme.t("antrenman", "workouts")) +
                " · " + weekRangeLabel(start, english)
            val frac = capsuleFraction(c, best)
            val barTop = chartH - maxBar * frac
            GlassBubble(
                label,
                Modifier.layout { m, cons ->
                    val pl = m.measure(cons.copy(minWidth = 0, minHeight = 0))
                    val bw = capsuleWidthDp(weeks).dp.roundToPx()
                    val pitch = if (n > 1) (cons.maxWidth - bw).toFloat() / (n - 1) else 0f
                    val cx = (i * pitch + bw / 2f).roundToInt()
                    val x = (cx - pl.width / 2).coerceIn(0, (cons.maxWidth - pl.width).coerceAtLeast(0))
                    val y = (barTop - 32.dp - 6.dp).roundToPx()
                    layout(cons.maxWidth, cons.maxHeight) { pl.place(x, y) }
                }
            )
        }
    }
}

/** "3–9 Kas" / "28 Eki–3 Kas". */
private fun weekRangeLabel(start: LocalDate, english: Boolean): String {
    val end = start.plusDays(6)
    val loc = uiLocale(english)
    val mon = DateTimeFormatter.ofPattern("MMM", loc)
    return if (start.month == end.month) {
        "${start.dayOfMonth}–${end.dayOfMonth} ${mon.format(end)}"
    } else {
        "${start.dayOfMonth} ${mon.format(start)}–${end.dayOfMonth} ${mon.format(end)}"
    }
}

// ── 3. Toplamlar: skor tabelası ───────────────────────────────────────────────

@Composable
private fun TotalsSection(state: ProfileState, theme: AppThemeState, english: Boolean, modifier: Modifier = Modifier) {
    val minutes = durationMinutes(state.totalDurationSeconds)
    val km = state.totalDistanceMeters >= 1000f
    Column(modifier) {
        SectionLabel(theme.t("Toplamlar", "Totals"))
        TotalRow(formatNumber(state.totalWorkouts.toDouble(), 0, english), null, theme.t("Antrenman", "Workouts"), Icons.Rounded.FitnessCenter, hero = true)
        if (minutes > 0) {
            TotalRow(formatNumber(minutes.toDouble(), 0, english), theme.t("dk", "min"), theme.t("Süre", "Duration"), Icons.Rounded.Schedule)
        }
        if (state.totalDistanceMeters > 0f) {
            TotalRow(
                if (km) formatNumber(state.totalDistanceMeters / 1000.0, 1, english) else formatNumber(state.totalDistanceMeters.toDouble(), 0, english),
                if (km) "km" else "m", theme.t("Mesafe", "Distance"), Icons.Rounded.Straighten
            )
        }
        if (state.totalExercises > 0) {
            TotalRow(formatNumber(state.totalExercises.toDouble(), 0, english), theme.t("kez", "times"), theme.t("Egzersiz", "Exercises"), Icons.Rounded.Repeat)
        }
    }
}

@Composable
private fun TotalRow(value: String, unit: String?, label: String, icon: ImageVector, hero: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .drawBehind {
                val w = 1.dp.toPx()
                drawLine(Sinema.White.copy(0.10f), Offset(0f, size.height - w / 2f), Offset(size.width, size.height - w / 2f), w)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = Sinema.Metric.copy(fontSize = if (hero) 56.sp else 40.sp, lineHeight = 1.0.em, letterSpacing = (-0.03).em),
                color = Sinema.White, maxLines = 1, modifier = Modifier.alignByBaseline()
            )
            if (unit != null) {
                Spacer(Modifier.width(5.dp))
                PText(unit, 14, FontWeight.SemiBold, color = Dim, modifier = Modifier.alignByBaseline())
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PText(label, 13, FontWeight.SemiBold, color = Dim, maxLines = 1)
            Icon(icon, null, tint = Dim, modifier = Modifier.size(20.dp))
        }
    }
}

// ── 4. Hareket gelişimi posteri ───────────────────────────────────────────────

@Composable
private fun ExercisePoster(theme: AppThemeState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val title = theme.t("Hareket gelişimi", "Exercise progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(shape)
            .border(1.dp, Sinema.White.copy(0.09f), shape)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(categoryImageUrl("Bacak")).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.30f to Color.Transparent, 1f to Color.Black.copy(0.70f))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp).fillMaxWidth(0.68f)) {
            PText(title, 22, FontWeight.ExtraBold, lineHeight = 24.sp)
            Spacer(Modifier.height(6.dp))
            PText(
                theme.t("Ağırlık, süre, mesafe ve hareket bazlı gelişim", "Weight, duration, distance and movement-based progress"),
                13, FontWeight.Normal, color = Sinema.White.copy(0.80f), lineHeight = 17.sp
            )
        }
        SinemaCapsule(
            Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 14.dp).size(44.dp),
            selected = true, height = 44.dp, shape = CircleShape,
            contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.Center
        ) { fg -> Icon(Icons.Rounded.ArrowForward, null, tint = fg, modifier = Modifier.size(20.dp)) }
    }
}

// ── 5. Vücut: tek cam blok ────────────────────────────────────────────────────

@Composable
private fun BodySection(
    state: ProfileState,
    theme: AppThemeState,
    english: Boolean,
    onWeightTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = theme.effectiveAccentColor
    val hasWeight = state.weightKg > 0
    val hasHeight = state.heightCm > 0
    val hasBmi = state.bmi > 0
    val hasFat = state.bodyFatPct > 0
    val none = !hasWeight && !hasHeight && !hasBmi && !hasFat
    val zoneNames = listOf(
        theme.t("Zayıf", "Underweight"), theme.t("Normal", "Normal"),
        theme.t("Fazla kilolu", "Overweight"), theme.t("Obez", "Obese")
    )

    SinemaCapsule(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), height = 1.dp,
        contentPadding = PaddingValues(20.dp), verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(theme.t("Vücut", "Body"))
                PText(
                    theme.t("Kilo takibi ›", "Weight tracking ›"), 13, FontWeight.SemiBold, color = accent,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onWeightTracking)
                )
            }
            if (none) {
                Spacer(Modifier.height(6.dp))
                PText(
                    theme.t("Boy ve kilonu ekle, BMI ve yağ oranı burada görünsün.", "Add your height and weight to see BMI and body fat here."),
                    14, FontWeight.Normal, color = Dim, lineHeight = 20.sp
                )
            } else {
                if (hasWeight) {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatNumber(state.weightKg, 1, english),
                            style = Sinema.Metric.copy(fontSize = 64.sp, lineHeight = 1.0.em),
                            color = Sinema.White, maxLines = 1, modifier = Modifier.alignByBaseline()
                        )
                        Spacer(Modifier.width(6.dp))
                        PText("kg", 18, FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                    }
                }
                if (hasBmi) {
                    Spacer(Modifier.height(18.dp))
                    PText("BMI ${formatNumber(state.bmi, 1, english)} · ${zoneNames[bmiZone(state.bmi)]}", 13, FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    BmiScale(state.bmi, theme.accent2Color)
                }
                if (hasHeight || hasFat) {
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hasHeight) BodyStat(theme.t("Boy", "Height"), formatNumber(state.heightCm, 0, english), "cm", Modifier.weight(1f))
                        if (hasFat) BodyStat(theme.t("Yağ", "Body fat"), "%" + formatNumber(state.bodyFatPct, 1, english), null, Modifier.weight(1f))
                        if (!(hasHeight && hasFat)) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyStat(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        PText(label, 12, FontWeight.SemiBold, color = Dim)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            PText(value, 24, FontWeight.ExtraBold, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                PText(unit, 13, FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

/** 4 bölgeli BMI hapı (Zayıf/Normal/Fazla kilolu/Obez, 15–35 ölçeği) + işaretçi. Bölgeler beyaz alfa, vurgu yalnız işaretçide. */
@Composable
private fun BmiScale(bmi: Double, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val w = size.width
        val top = 4.dp.toPx()
        val h = 8.dp.toPx()
        val widths = listOf(49f, 91f, 70f, 70f).map { it / 280f * w }
        val alphas = listOf(0.10f, 0.22f, 0.10f, 0.10f)
        val clip = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, top, w, top + h, CornerRadius(h / 2f)))
        }
        clipPath(clip) {
            var x = 0f
            widths.forEachIndexed { i, zw ->
                drawRect(Sinema.White.copy(alphas[i]), Offset(x, top), Size(zw, h))
                if (i > 0) drawLine(Sinema.White.copy(0.30f), Offset(x, top), Offset(x, top + h), 1.dp.toPx())
                x += zw
            }
        }
        val cx = bmiScaleFraction(bmi) * w
        drawCircle(accent, 8.dp.toPx(), Offset(cx, top + h / 2f))
        drawCircle(Sinema.White, 6.dp.toPx(), Offset(cx, top + h / 2f))
    }
}

// ── 6. Seri & seviye ──────────────────────────────────────────────────────────

@Composable
private fun StreakSection(state: ProfileState, theme: AppThemeState, english: Boolean, modifier: Modifier = Modifier) {
    val accent = theme.accent2Color   // c2 = ölç (en uzun seriye oran çubuğu)
    Column(modifier) {
        SectionLabel(theme.t("Aktif seri", "Current streak"))
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                state.currentStreak.toString(),
                style = Sinema.Metric.copy(fontSize = 72.sp, lineHeight = 1.0.em),
                color = Sinema.White, maxLines = 1, modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            PText(theme.t("gün", "days"), 18, FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
        }
        if (state.longestStreak > 0) {
            val ratio = streakRatio(state.currentStreak, state.longestStreak)
            Box(
                Modifier.padding(top = 14.dp).fillMaxWidth().height(10.dp).clip(CircleShape).background(Sinema.White.copy(0.12f))
            ) {
                if (ratio > 0f) Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(accent))
            }
            PText(
                theme.t("En uzun ${state.longestStreak} gün", "Longest ${state.longestStreak} days"), 12, FontWeight.Medium,
                color = Dim, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakStat(state.level.toString(), theme.t("Seviye", "Level"), Modifier.weight(1f))
            StreakStat(formatNumber(state.xp.toDouble(), 0, english), "XP", Modifier.weight(1f))
            if (state.streakRankPosition > 0L) {
                StreakStat("#${state.streakRankPosition}", theme.t("Sıralama", "Rank"), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StreakStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        PText(value, 28, FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        PText(label, 12, FontWeight.SemiBold, color = Dim, maxLines = 1)
    }
}

private enum class CalculatorMode { OneRm, Bmi, BodyFat, Bmr, HeartRate, Protein, Vo2Max }
private enum class OneRmFormula { Epley, Brzycki, Lombardi }
private enum class CalcGender { Male, Female }
private enum class ProteinGoal { General, MuscleGain, FatLoss }

private data class ActivityFactor(
    val label: String,
    val factor: Double
)

private val activityFactors = listOf(
    ActivityFactor("Sedanter", 1.2),
    ActivityFactor("Hafif", 1.375),
    ActivityFactor("Orta", 1.55),
    ActivityFactor("Yoğun", 1.725)
)


@Composable
private fun CalculationsSheet(
    state: ProfileState,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CalculatorMode.OneRm) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 40.dp, end = 20.dp, bottom = 24.dp)
    ) {
        Text(theme.t("Hesaplamalar", "Calculations"), style = Sinema.ScreenTitle, color = Sinema.text(theme))
        Spacer(Modifier.height(4.dp))
        Text(
            theme.t("1RM, BMI, yağ oranı, kalori, nabız, protein ve VO2 max", "1RM, BMI, body fat, calories, heart rate, protein and VO2 max"),
            color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp
        )

        Spacer(Modifier.height(20.dp))

        CalculatorModeGrid(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            theme = theme
        )

        Spacer(Modifier.height(20.dp))

        when (selectedMode) {
            CalculatorMode.OneRm -> OneRepMaxCalculator(theme = theme)
            CalculatorMode.Bmi -> BmiCalculator(state = state, theme = theme)
            CalculatorMode.BodyFat -> BodyFatCalculator(state = state, theme = theme)
            CalculatorMode.Bmr -> BmrCalculator(state = state, theme = theme)
            CalculatorMode.HeartRate -> HeartRateCalculator(state = state, theme = theme)
            CalculatorMode.Protein -> ProteinCalculator(state = state, theme = theme)
            CalculatorMode.Vo2Max -> Vo2MaxCalculator(theme = theme)
        }
    }
}

@Composable
private fun CalculatorModeGrid(
    selectedMode: CalculatorMode,
    onModeSelected: (CalculatorMode) -> Unit,
    theme: AppThemeState
) {
    val modes = listOf(
        CalculatorMode.OneRm,
        CalculatorMode.Bmi,
        CalculatorMode.BodyFat,
        CalculatorMode.Bmr,
        CalculatorMode.HeartRate,
        CalculatorMode.Protein,
        CalculatorMode.Vo2Max
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { mode ->
                    CalculatorModeButton(
                        mode = mode,
                        selected = mode == selectedMode,
                        theme = theme,
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CalculatorModeButton(
    mode: CalculatorMode,
    selected: Boolean,
    theme: AppThemeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SinemaCapsule(
        modifier = modifier, selected = selected, onClick = onClick, height = 48.dp,
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) { fg ->
        Icon(calculatorModeIcon(mode), null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(calculatorModeTitle(mode, theme), color = fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun OneRepMaxCalculator(
    theme: AppThemeState
) {
    var liftedWeight by remember { mutableStateOf("100") }
    var reps by remember { mutableStateOf("5") }
    var formula by remember { mutableStateOf(OneRmFormula.Epley) }
    val weight = liftedWeight.toPositiveDoubleOrNull()
    val repetitionCount = reps.trim().toIntOrNull()?.takeIf { it in 1..30 }
    val oneRm = if (weight != null && repetitionCount != null) {
        estimateOneRepMax(weight, repetitionCount, formula)
    } else null

    CalculatorPanel(
        title = theme.t("Tek Tekrar Maksimumu", "One-Rep Max"),
        subtitle = theme.t("Calculator.net 1RM formülleri", "Calculator.net 1RM formulas"),
        icon = Icons.Rounded.FitnessCenter,
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Ağırlık kg", "Weight kg"),
                value = liftedWeight,
                onValueChange = { liftedWeight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Tekrar", "Reps"),
                value = reps,
                onValueChange = { reps = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Spacer(Modifier.height(12.dp))

        FormulaSelector(
            selectedFormula = formula,
            onFormulaSelected = { formula = it },
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (oneRm != null) {
            CalculationResultCard(
                title = theme.t("Tahmini 1RM", "Estimated 1RM"),
                value = "${formatOneDecimal(oneRm)} kg",
                subtitle = theme.t("${oneRmFormulaLabel(formula)} formülü", "${oneRmFormulaLabel(formula)} formula"),
                theme = theme
            )

            Spacer(Modifier.height(12.dp))

            listOf(95, 90, 85, 80, 75, 70).forEach { pct ->
                CalculationRow(
                    label = "%$pct",
                    value = "${formatOneDecimal(oneRm * pct / 100.0)} kg",
                    theme = theme
                )
            }
        } else {
            EmptyCalculationHint(theme.t("Ağırlık ve 1-30 arası tekrar gir.", "Enter weight and 1-30 reps."))
        }
    }
}

@Composable
private fun BmiCalculator(
    state: ProfileState,
    theme: AppThemeState
) {
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    val weightKg = weight.toPositiveDoubleOrNull()
    val heightCm = height.toPositiveDoubleOrNull()
    val bmi = calculateBmi(weightKg, heightCm)
    val category = bmi?.let { bmiCategoryLabel(it, theme) }

    CalculatorPanel(
        title = theme.t("Vücut Kitle İndeksi", "Body Mass Index"),
        subtitle = theme.t("CDC metrik BMI hesabı", "CDC metric BMI calculation"),
        icon = Icons.Rounded.Analytics,
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Boy cm", "Height cm"),
                value = height,
                onValueChange = { height = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (bmi != null) {
            CalculationResultCard(
                title = "BMI",
                value = formatOneDecimal(bmi),
                subtitle = category.orEmpty(),
                theme = theme
            )
        } else {
            EmptyCalculationHint(theme.t("Kilo ve boy değerlerini gir.", "Enter weight and height."))
        }
    }
}

@Composable
private fun BodyFatCalculator(
    state: ProfileState,
    theme: AppThemeState
) {
    var gender by remember(state.gender) { mutableStateOf(profileGender(state.gender)) }
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    var neck by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }

    val ageYears = age.trim().toIntOrNull()?.takeIf { it in 8..100 }
    val weightKg = weight.toPositiveDoubleOrNull()
    val heightCm = height.toPositiveDoubleOrNull()
    val navy = navyBodyFatPct(
        gender = gender,
        heightCm = heightCm,
        neckCm = neck.toPositiveDoubleOrNull(),
        waistCm = waist.toPositiveDoubleOrNull(),
        hipCm = hip.toPositiveDoubleOrNull()
    )
    val bmiBased = bmiBodyFatPct(
        gender = gender,
        age = ageYears,
        weightKg = weightKg,
        heightCm = heightCm
    )

    CalculatorPanel(
        title = theme.t("Vücut Yağ Oranı", "Body Fat Percentage"),
        subtitle = theme.t("U.S. Navy ve BMI yöntemi", "U.S. Navy and BMI method"),
        icon = Icons.Rounded.Person,
        theme = theme
    ) {
        GenderSelector(
            selectedGender = gender,
            onGenderSelected = { gender = it },
            theme = theme
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Boy cm", "Height cm"),
                value = height,
                onValueChange = { height = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Boyun cm", "Neck cm"),
                value = neck,
                onValueChange = { neck = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Bel cm", "Waist cm"),
                value = waist,
                onValueChange = { waist = it },
                modifier = Modifier.weight(1f)
            )
            if (gender == CalcGender.Female) {
                CalculationInput(
                    label = theme.t("Kalça cm", "Hip cm"),
                    value = hip,
                    onValueChange = { hip = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (navy != null) {
            CalculationResultCard(
                title = "U.S. Navy",
                value = "%${formatOneDecimal(navy)}",
                subtitle = bodyFatCategory(navy, gender, theme),
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
        }

        if (bmiBased != null) {
            CalculationResultCard(
                title = theme.t("BMI yöntemi", "BMI method"),
                value = "%${formatOneDecimal(bmiBased)}",
                subtitle = theme.t("Hızlı tahmin", "Quick estimate"),
                theme = theme
            )
        }

        if (navy == null && bmiBased == null) {
            EmptyCalculationHint(theme.t("Profil verileri yoksa yaş, kilo, boy ve ölçüleri gir.", "If profile data is missing, enter age, weight, height and measurements."))
        }
    }
}

@Composable
private fun BmrCalculator(
    state: ProfileState,
    theme: AppThemeState
) {
    var gender by remember(state.gender) { mutableStateOf(profileGender(state.gender)) }
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    var activity by remember { mutableStateOf(activityFactors[1]) }

    val bmr = calculateBmr(
        gender = gender,
        age = age.trim().toIntOrNull()?.takeIf { it in 8..100 },
        weightKg = weight.toPositiveDoubleOrNull(),
        heightCm = height.toPositiveDoubleOrNull()
    )
    val tdee = bmr?.times(activity.factor)

    CalculatorPanel(
        title = theme.t("Kalori İhtiyacı", "Calorie Needs"),
        subtitle = theme.t("Mifflin-St Jeor BMR + aktivite", "Mifflin-St Jeor BMR + activity"),
        icon = Icons.Rounded.LocalFireDepartment,
        theme = theme
    ) {
        GenderSelector(
            selectedGender = gender,
            onGenderSelected = { gender = it },
            theme = theme
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        CalculationInput(
            label = theme.t("Boy cm", "Height cm"),
            value = height,
            onValueChange = { height = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ActivitySelector(
            selectedActivity = activity,
            onActivitySelected = { activity = it },
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (bmr != null && tdee != null) {
            CalculationResultCard(
                title = "BMR",
                value = "${formatWhole(bmr)} kcal",
                subtitle = theme.t("Dinlenme metabolizması", "Resting metabolism"),
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
            CalculationResultCard(
                title = theme.t("Günlük ihtiyaç", "Daily needs"),
                value = "${formatWhole(tdee)} kcal",
                subtitle = theme.t("${activity.label} aktivite", "${activity.localizedLabel(theme)} activity"),
                theme = theme
            )
        } else {
            EmptyCalculationHint(theme.t("Yaş, kilo ve boy değerlerini gir.", "Enter age, weight and height."))
        }
    }
}

@Composable
private fun HeartRateCalculator(
    state: ProfileState,
    theme: AppThemeState
) {
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var restingHr by remember { mutableStateOf("60") }

    val ageYears = age.trim().toIntOrNull()?.takeIf { it in 8..100 }
    val resting = restingHr.trim().toIntOrNull()?.takeIf { it in 30..120 }
    val maxHr = ageYears?.let(::calculateMaxHeartRate)
    val zones = if (maxHr != null && resting != null && resting < maxHr) {
        heartRateZones(maxHr, resting)
    } else emptyList()

    CalculatorPanel(
        title = theme.t("Nabız Bölgeleri", "Heart Rate Zones"),
        subtitle = theme.t("Tanaka max nabız + Karvonen HRR", "Tanaka max heart rate + Karvonen HRR"),
        icon = Icons.Rounded.Favorite,
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Dinlenik nabız", "Resting heart rate"),
                value = restingHr,
                onValueChange = { restingHr = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Spacer(Modifier.height(14.dp))

        if (maxHr != null && zones.isNotEmpty()) {
            CalculationResultCard(
                title = theme.t("Tahmini max nabız", "Estimated max heart rate"),
                value = "${maxHr.roundToInt()} bpm",
                subtitle = theme.t("208 - 0.7 x yaş", "208 - 0.7 x age"),
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            zones.forEach { zone ->
                CalculationRow(
                    label = zone.localizedLabel(theme),
                    value = "${zone.low}-${zone.high} bpm",
                    theme = theme
                )
            }
        } else {
            EmptyCalculationHint(theme.t("Yaş ve 30-120 arası dinlenik nabız gir.", "Enter age and resting heart rate between 30-120."))
        }
    }
}

@Composable
private fun ProteinCalculator(
    state: ProfileState,
    theme: AppThemeState
) {
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var goal by remember { mutableStateOf(ProteinGoal.MuscleGain) }
    val weightKg = weight.toPositiveDoubleOrNull()
    val range = weightKg?.let { proteinRange(it, goal) }

    CalculatorPanel(
        title = theme.t("Protein Hedefi", "Protein Target"),
        subtitle = theme.t("Sporcu g/kg aralıkları", "Athlete g/kg ranges"),
        icon = Icons.Rounded.Restaurant,
        theme = theme
    ) {
        CalculationInput(
            label = theme.t("Kilo kg", "Weight kg"),
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ProteinGoalSelector(
            selectedGoal = goal,
            onGoalSelected = { goal = it },
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (range != null) {
            CalculationResultCard(
                title = theme.t("Günlük protein", "Daily protein"),
                value = "${formatWhole(range.first)}-${formatWhole(range.second)} g",
                subtitle = proteinGoalLabel(goal, theme),
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            CalculationRow(theme.t("Alt sınır", "Lower limit"), "${formatOneDecimal(proteinGramPerKg(goal).first)} g/kg", theme)
            CalculationRow(theme.t("Üst sınır", "Upper limit"), "${formatOneDecimal(proteinGramPerKg(goal).second)} g/kg", theme)
        } else {
            EmptyCalculationHint(theme.t("Kilo değerini gir.", "Enter weight."))
        }
    }
}

@Composable
private fun Vo2MaxCalculator(
    theme: AppThemeState
) {
    var distance by remember { mutableStateOf("2400") }
    val distanceMeters = distance.toPositiveDoubleOrNull()
    val vo2Max = distanceMeters?.let(::cooperVo2Max)

    CalculatorPanel(
        title = "VO2 Max",
        subtitle = theme.t("Cooper 12 dakika koşu tahmini", "Cooper 12-minute run estimate"),
        icon = Icons.Rounded.DirectionsRun,
        theme = theme
    ) {
        CalculationInput(
            label = theme.t("12 dk mesafe m", "12 min distance m"),
            value = distance,
            onValueChange = { distance = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Number
        )

        Spacer(Modifier.height(14.dp))

        if (vo2Max != null) {
            CalculationResultCard(
                title = theme.t("Tahmini VO2 max", "Estimated VO2 max"),
                value = formatOneDecimal(vo2Max),
                subtitle = theme.t("ml/kg/dk", "ml/kg/min"),
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            CalculationRow(theme.t("Formül", "Formula"), theme.t("(mesafe - 504.9) / 44.73", "(distance - 504.9) / 44.73"), theme)
        } else {
            EmptyCalculationHint(theme.t("12 dakikada koşulan mesafeyi metre olarak gir.", "Enter the distance run in 12 minutes in meters."))
        }
    }
}

@Composable
private fun FormulaSelector(
    selectedFormula: OneRmFormula,
    onFormulaSelected: (OneRmFormula) -> Unit,
    theme: AppThemeState
) {
    SinemaSegmentRow(
        options = listOf(OneRmFormula.Epley, OneRmFormula.Brzycki, OneRmFormula.Lombardi),
        selected = selectedFormula,
        label = { oneRmFormulaLabel(it) },
        onSelect = onFormulaSelected
    )
}

@Composable
private fun GenderSelector(
    selectedGender: CalcGender,
    onGenderSelected: (CalcGender) -> Unit,
    theme: AppThemeState
) {
    SinemaSegmentRow(
        options = listOf(CalcGender.Male, CalcGender.Female),
        selected = selectedGender,
        label = { it.localizedLabel(theme) },
        onSelect = onGenderSelected
    )
}

@Composable
private fun ActivitySelector(
    selectedActivity: ActivityFactor,
    onActivitySelected: (ActivityFactor) -> Unit,
    theme: AppThemeState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        activityFactors.chunked(2).forEach { row ->
            SinemaSegmentRow(
                options = row,
                selected = selectedActivity,
                label = { it.localizedLabel(theme) },
                onSelect = onActivitySelected
            )
        }
    }
}

@Composable
private fun ProteinGoalSelector(
    selectedGoal: ProteinGoal,
    onGoalSelected: (ProteinGoal) -> Unit,
    theme: AppThemeState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ProteinGoal.General, ProteinGoal.MuscleGain, ProteinGoal.FatLoss).forEach { goal ->
            SinemaCapsule(
                modifier = Modifier.fillMaxWidth(), selected = selectedGoal == goal, onClick = { onGoalSelected(goal) },
                height = 44.dp, horizontalArrangement = Arrangement.Center
            ) { fg ->
                Text(proteinGoalLabel(goal, theme), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CalculatorPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    theme: AppThemeState,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Sinema.line(theme)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = theme.effectiveAccentColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = Sinema.text(theme), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Sinema.textSecondary(theme), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CalculationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    SinemaGlassField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardType = keyboardType,
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.hasFocus) {
                    coroutineScope.launch {
                        delay(250)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
    )
}

@Composable
private fun CalculationResultCard(
    title: String,
    value: String,
    subtitle: String,
    theme: AppThemeState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = Sinema.Metric.copy(fontSize = 52.sp), color = Sinema.text(theme), maxLines = 1)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = theme.accent2TextColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Sinema.textSecondary(theme), fontSize = 14.sp)
        Text(value, color = Sinema.text(theme), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptyCalculationHint(message: String) {
    val theme = LocalAppTheme.current
    Text(message, color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp)
}

private fun calculatorModeTitle(mode: CalculatorMode, theme: AppThemeState): String = when (mode) {
    CalculatorMode.OneRm -> "1RM"
    CalculatorMode.Bmi -> "BMI"
    CalculatorMode.BodyFat -> theme.t("Yağ Oranı", "Body Fat")
    CalculatorMode.Bmr -> theme.t("Kalori", "Calories")
    CalculatorMode.HeartRate -> theme.t("Nabız", "Heart Rate")
    CalculatorMode.Protein -> "Protein"
    CalculatorMode.Vo2Max -> "VO2 Max"
}

private fun calculatorModeIcon(mode: CalculatorMode): ImageVector = when (mode) {
    CalculatorMode.OneRm -> Icons.Rounded.FitnessCenter
    CalculatorMode.Bmi -> Icons.Rounded.Analytics
    CalculatorMode.BodyFat -> Icons.Rounded.Person
    CalculatorMode.Bmr -> Icons.Rounded.LocalFireDepartment
    CalculatorMode.HeartRate -> Icons.Rounded.Favorite
    CalculatorMode.Protein -> Icons.Rounded.Restaurant
    CalculatorMode.Vo2Max -> Icons.Rounded.DirectionsRun
}

private fun CalcGender.localizedLabel(theme: AppThemeState): String =
    if (this == CalcGender.Male) theme.t("Erkek", "Male") else theme.t("Kadın", "Female")

private fun ActivityFactor.localizedLabel(theme: AppThemeState): String = when (label) {
    "Sedanter" -> theme.t("Sedanter", "Sedentary")
    "Hafif" -> theme.t("Hafif", "Light")
    "Orta" -> theme.t("Orta", "Moderate")
    "Yoğun" -> theme.t("Yoğun", "Intense")
    else -> label
}

private fun oneRmFormulaLabel(formula: OneRmFormula): String = when (formula) {
    OneRmFormula.Epley -> "Epley"
    OneRmFormula.Brzycki -> "Brzycki"
    OneRmFormula.Lombardi -> "Lombardi"
}

private fun estimateOneRepMax(weightKg: Double, reps: Int, formula: OneRmFormula): Double? {
    if (reps == 1) return weightKg
    return when (formula) {
        OneRmFormula.Epley -> weightKg * (1.0 + reps / 30.0)
        OneRmFormula.Brzycki -> if (reps < 37) weightKg * 36.0 / (37.0 - reps) else null
        OneRmFormula.Lombardi -> weightKg * reps.toDouble().pow(0.10)
    }
}

private fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val heightM = heightCm / 100.0
    return (weightKg / (heightM * heightM)).takeIf { it.isFinite() && it > 0.0 }
}

private fun bmiCategoryLabel(bmi: Double, theme: AppThemeState): String = when {
    bmi < 18.5 -> theme.t("Zayıf", "Underweight")
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> theme.t("Fazla kilolu", "Overweight")
    else       -> theme.t("Obez", "Obese")
}

private fun navyBodyFatPct(
    gender: CalcGender,
    heightCm: Double?,
    neckCm: Double?,
    waistCm: Double?,
    hipCm: Double?
): Double? {
    if (heightCm == null || neckCm == null || waistCm == null) return null
    return when (gender) {
        CalcGender.Male -> {
            val waistMinusNeck = waistCm - neckCm
            if (waistMinusNeck <= 0.0) null
            else 495.0 / (1.0324 - 0.19077 * log10(waistMinusNeck) + 0.15456 * log10(heightCm)) - 450.0
        }
        CalcGender.Female -> {
            val hipValue = hipCm ?: return null
            val waistPlusHipMinusNeck = waistCm + hipValue - neckCm
            if (waistPlusHipMinusNeck <= 0.0) null
            else 495.0 / (1.29579 - 0.35004 * log10(waistPlusHipMinusNeck) + 0.22100 * log10(heightCm)) - 450.0
        }
    }?.takeIf { it.isFinite() }?.coerceIn(2.0, 75.0)
}

private fun bmiBodyFatPct(
    gender: CalcGender,
    age: Int?,
    weightKg: Double?,
    heightCm: Double?
): Double? {
    val bmi = calculateBmi(weightKg, heightCm) ?: return null
    val ageYears = age ?: return null
    val raw = if (gender == CalcGender.Male) {
        1.20 * bmi + 0.23 * ageYears - 16.2
    } else {
        1.20 * bmi + 0.23 * ageYears - 5.4
    }
    return raw.takeIf { it.isFinite() }?.coerceIn(3.0, 60.0)
}

private fun bodyFatCategory(bodyFatPct: Double, gender: CalcGender, theme: AppThemeState): String =
    if (gender == CalcGender.Male) {
        when {
            bodyFatPct < 6.0  -> theme.t("Esansiyel", "Essential")
            bodyFatPct < 14.0 -> theme.t("Atletik", "Athletic")
            bodyFatPct < 18.0 -> "Fitness"
            bodyFatPct < 25.0 -> theme.t("Ortalama", "Average")
            else              -> theme.t("Yüksek", "High")
        }
    } else {
        when {
            bodyFatPct < 14.0 -> theme.t("Esansiyel", "Essential")
            bodyFatPct < 21.0 -> theme.t("Atletik", "Athletic")
            bodyFatPct < 25.0 -> "Fitness"
            bodyFatPct < 32.0 -> theme.t("Ortalama", "Average")
            else              -> theme.t("Yüksek", "High")
        }
    }

private fun calculateBmr(
    gender: CalcGender,
    age: Int?,
    weightKg: Double?,
    heightCm: Double?
): Double? {
    if (age == null || weightKg == null || heightCm == null) return null
    val offset = if (gender == CalcGender.Male) 5.0 else -161.0
    return (10.0 * weightKg + 6.25 * heightCm - 5.0 * age + offset).takeIf { it.isFinite() && it > 0.0 }
}

private data class HeartRateZone(
    val label: String,
    val low: Int,
    val high: Int
)

private fun calculateMaxHeartRate(age: Int): Double = 208.0 - 0.7 * age

private fun heartRateZones(maxHr: Double, restingHr: Int): List<HeartRateZone> {
    val reserve = maxHr - restingHr
    return listOf(
        heartRateZone("Z1 Toparlanma", reserve, restingHr, 0.50, 0.60),
        heartRateZone("Z2 Aerobik", reserve, restingHr, 0.60, 0.70),
        heartRateZone("Z3 Tempo", reserve, restingHr, 0.70, 0.80),
        heartRateZone("Z4 Eşik", reserve, restingHr, 0.80, 0.90),
        heartRateZone("Z5 Maksimum", reserve, restingHr, 0.90, 1.00)
    )
}

private fun heartRateZone(
    label: String,
    reserve: Double,
    restingHr: Int,
    lowPct: Double,
    highPct: Double
): HeartRateZone =
    HeartRateZone(
        label = label,
        low = (restingHr + reserve * lowPct).roundToInt(),
        high = (restingHr + reserve * highPct).roundToInt()
    )

private fun HeartRateZone.localizedLabel(theme: AppThemeState): String = when (label) {
    "Z1 Toparlanma" -> theme.t("Z1 Toparlanma", "Z1 Recovery")
    "Z2 Aerobik" -> theme.t("Z2 Aerobik", "Z2 Aerobic")
    "Z3 Tempo" -> "Z3 Tempo"
    "Z4 Eşik" -> theme.t("Z4 Eşik", "Z4 Threshold")
    "Z5 Maksimum" -> theme.t("Z5 Maksimum", "Z5 Maximum")
    else -> label
}

private fun proteinGoalLabel(goal: ProteinGoal, theme: AppThemeState): String = when (goal) {
    ProteinGoal.General -> theme.t("Genel fitness", "General fitness")
    ProteinGoal.MuscleGain -> theme.t("Kas kazanımı", "Muscle gain")
    ProteinGoal.FatLoss -> theme.t("Yağ kaybı / definasyon", "Fat loss / cutting")
}

private fun proteinGramPerKg(goal: ProteinGoal): Pair<Double, Double> = when (goal) {
    ProteinGoal.General -> 1.2 to 1.6
    ProteinGoal.MuscleGain -> 1.6 to 2.2
    ProteinGoal.FatLoss -> 1.8 to 2.7
}

private fun proteinRange(weightKg: Double, goal: ProteinGoal): Pair<Double, Double> {
    val grams = proteinGramPerKg(goal)
    return weightKg * grams.first to weightKg * grams.second
}

private fun cooperVo2Max(distanceMeters: Double): Double? =
    ((distanceMeters - 504.9) / 44.73).takeIf { distanceMeters >= 800.0 && it.isFinite() && it > 0.0 }

private fun profileGender(rawGender: String): CalcGender =
    if (rawGender.lowercase() in listOf("female", "kadın", "kadin", "woman")) CalcGender.Female else CalcGender.Male

private fun profileAgeInput(state: ProfileState): String =
    runCatching {
        val date = LocalDate.parse(state.birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
        Period.between(date, LocalDate.now()).years.takeIf { it in 8..100 }?.toString()
    }.getOrNull().orEmpty()

private fun profileNumberInput(value: Double, decimals: Int = 1): String =
    if (value > 0.0) {
        if (decimals == 0) "%.0f".format(value) else "%.1f".format(value)
    } else ""

private fun String.toPositiveDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

private fun formatOneDecimal(value: Double): String = "%.1f".format(value)

private fun formatWhole(value: Double): String = "%.0f".format(value)
