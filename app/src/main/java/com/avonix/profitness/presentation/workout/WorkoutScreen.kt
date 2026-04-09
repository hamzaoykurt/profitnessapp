package com.avonix.profitness.presentation.workout

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.CinematicExerciseCard
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.coroutines.delay

// ── Data models ─────────────────────────────────────────────────────────────

data class Exercise(
    val id: String,
    val name: String,
    val target: String,
    val sets: Int,
    val reps: String,
    val image: String,
    val isCompleted: Boolean = false,
    val category: String = "Strength",
    val restSeconds: Int = 90,
    val exerciseTableId: String = "" // FK to exercises table — used for DB logging
)

data class WorkoutDay(
    val day: String,
    val title: String,
    val exercises: List<Exercise>,
    val isRestDay: Boolean = false,
    val totalKcal: Int = 0,
    val durationMin: Int = 0,
    val programDayId: String = ""   // program_days tablosundaki ID — workout log için
)

// ── Demo Data — 7 günün hepsi dolu ─────────────────────────────────────────

val DEMO_WORKOUTS = listOf(
    WorkoutDay("Pzt", "GÖĞÜS & TRİSEPS", listOf(
        Exercise("1", "Bench Press", "Göğüs", 4, "8-12",
            "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800", category = "Strength"),
        Exercise("2", "Incline DB Press", "Üst Göğüs", 3, "10",
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800", category = "Strength"),
        Exercise("3", "Dips", "Triceps", 3, "12",
            "https://images.unsplash.com/photo-1598971639058-fab3c32f850c?w=800", category = "Bodyweight"),
        Exercise("4", "Cable Flyes", "Göğüs", 3, "15",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Cable")
    ), totalKcal = 420, durationMin = 55),
    WorkoutDay("Sal", "SIRT & BiSEPS", listOf(
        Exercise("5", "Lat Pulldown", "Sırt", 4, "10-15",
            "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=800", category = "Cable"),
        Exercise("6", "Barbell Row", "Orta Sırt", 4, "8-10",
            "https://images.unsplash.com/photo-1532384661128-d446b2d55db7?w=800", category = "Strength"),
        Exercise("7", "Pull-ups", "Sırt/Biceps", 3, "Max",
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800", category = "Bodyweight"),
        Exercise("8", "Hammer Curl", "Biceps", 3, "12",
            "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800", category = "Strength")
    ), totalKcal = 380, durationMin = 50),
    WorkoutDay("Çar", "DİNLENME", emptyList(), isRestDay = true),
    WorkoutDay("Per", "OMUZ & BACAK", listOf(
        Exercise("9", "Squat", "Quadriceps", 4, "8-12",
            "https://images.unsplash.com/photo-1567013127542-490d757e51fc?w=800", category = "Strength"),
        Exercise("10", "Shoulder Press", "Omuz", 4, "10",
            "https://images.unsplash.com/photo-1584735935169-11c341bfed13?w=800", category = "Strength"),
        Exercise("11", "Lateral Raise", "Yan Omuz", 3, "15",
            "https://images.unsplash.com/photo-1530822847156-5df684ec5933?w=800", category = "Cable"),
        Exercise("12", "Romanian DL", "Hamstring", 3, "10",
            "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", category = "Strength")
    ), totalKcal = 510, durationMin = 65),
    WorkoutDay("Cum", "CORE & KARDİYO", listOf(
        Exercise("13", "Plank", "Core", 3, "60s",
            "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=800", category = "Bodyweight"),
        Exercise("14", "Mountain Climber", "Core/Cardio", 3, "30s",
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800", category = "Bodyweight"),
        Exercise("15", "Cable Crunch", "Core", 3, "20",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Cable"),
        Exercise("16", "Burpee", "Full Body", 3, "15",
            "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800", category = "Bodyweight")
    ), totalKcal = 480, durationMin = 45),
    WorkoutDay("Cmt", "DİNLENME", emptyList(), isRestDay = true),
    WorkoutDay("Paz", "MOBİLİTE & GERİNME", listOf(
        Exercise("17", "Hip Flexor Stretch", "Kalça", 2, "60s",
            "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800", category = "Bodyweight"),
        Exercise("18", "Thoracic Rotation", "Sırt Mobilite", 2, "10 rep",
            "https://images.unsplash.com/photo-1511275539165-cc46b1ee8960?w=800", category = "Bodyweight"),
        Exercise("19", "Band Pull Apart", "Omuz Mobilite", 3, "20",
            "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800", category = "Cable"),
        Exercise("20", "World's Greatest", "Full Body", 2, "5/yan",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Bodyweight")
    ), totalKcal = 220, durationMin = 35)
)

// ── Mutable state wrapper ────────────────────────────────────────────────────

data class WorkoutDayState(
    val day: WorkoutDay,
    val completedIds: Set<String> = emptySet()
) {
    val completedCount get() = completedIds.size
    val totalCount get() = day.exercises.size
    val progress get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

@Composable
fun WorkoutScreen(
    bottomPadding: Dp = 0.dp,
    onNavigateToAIBuilder: () -> Unit = {},
    onNavigateToManualBuilder: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    // ON_RESUME: arka planda Supabase sync tetikle.
    // Room Flow zaten dinleniyor — yeni veri gelince UI otomatik güncellenir.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.dayStates.isEmpty() -> {
                NoProgramView(
                    bottomPadding = bottomPadding,
                    onAI = onNavigateToAIBuilder,
                    onManual = onNavigateToManualBuilder
                )
            }
            else -> {
                WorkoutContent(
                    state = state,
                    viewModel = viewModel,
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}

@Composable
private fun NoProgramView(
    bottomPadding: Dp,
    onAI: () -> Unit,
    onManual: () -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 80.dp, bottom = bottomPadding + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon
        Text("🏋️", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))

        Text(
            "HENÜZ PROGRAMIN YOK",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Antrenman programını şimdi oluştur.\nAI sana özel bir plan hazırlasın ya da kendin düzenle.",
            color = theme.text2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(36.dp))

        // AI ile Oluştur
        Button(
            onClick = onAI,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("AI İLE OLUŞTUR", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(12.dp))

        // Manuel Oluştur
        OutlinedButton(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, theme.stroke)
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = theme.text1)
            Spacer(Modifier.width(10.dp))
            Text("MANUEL OLUŞTUR", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp, color = theme.text1)
        }
    }
}

@Composable
private fun WorkoutContent(
    state: WorkoutScreenState,
    viewModel: WorkoutViewModel,
    bottomPadding: Dp
) {
    val dayStates = state.dayStates
    val selectedDayIdx = state.selectedDayIdx
    val currentState = dayStates[selectedDayIdx]
    val currentDay   = currentState.day
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    val listState = rememberLazyListState()
    // Açılan kartı ekranın ortasına scroll et
    var expandedExIdx by remember { mutableStateOf(-1) }
    LaunchedEffect(expandedExIdx) {
        if (expandedExIdx >= 0) {
            // Header items: StreakBanner(0), Header(1), DaySelector(2), SectionLabel(3)
            val lazyIdx = 4 + expandedExIdx
            kotlinx.coroutines.delay(150)  // expand animasyonunun başlamasını bekle
            val viewportHeight = listState.layoutInfo.viewportSize.height
            listState.animateScrollToItem(lazyIdx, scrollOffset = -(viewportHeight / 5))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, bottomPadding + 16.dp)
    ) {
        // ── Streak Banner ─────────────────────────────────────────────────
        item {
            StreakBanner(streak = state.currentStreak)
        }

        // ── Header: Greeting + Progress Ring ─────────────────────────────
        item {
            AnimatedContent(
                targetState = selectedDayIdx,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 4 } +
                            fadeIn(tween(160))) togetherWith
                        (slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 5 } +
                            fadeOut(tween(120)))
                    } else {
                        (slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 4 } +
                            fadeIn(tween(160))) togetherWith
                        (slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 5 } +
                            fadeOut(tween(120)))
                    }
                },
                label = "day_header"
            ) { dayIdx ->
                WorkoutDashboardHeader(dayStates[dayIdx].day, dayStates[dayIdx].progress)
            }
        }

        // ── Day Selector Strip ────────────────────────────────────────────
        item {
            DaySelector(
                days        = dayStates,
                selectedIndex = selectedDayIdx,
                onSelect    = { viewModel.selectDay(it) }
            )
        }

        // ── Section Label ─────────────────────────────────────────────────
        if (!currentDay.isRestDay && currentDay.exercises.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.todayProgram,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${currentState.completedCount}/${currentDay.exercises.size} ${strings.completedLabel}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Content: Rest day or exercise cards ──────────────────────────
        if (currentDay.isRestDay) {
            item { RestDayView() }
        } else {
            itemsIndexed(currentDay.exercises, key = { _, ex -> ex.id }) { idx, exercise ->
                val isCompleted = exercise.id in currentState.completedIds
                var showDetail by remember { mutableStateOf(false) }
                CinematicExerciseCard(
                    exercise    = exercise,
                    index       = idx,
                    isCompleted = isCompleted,
                    onComplete  = {
                        viewModel.toggleExercise(selectedDayIdx, exercise.id)
                    },
                    onShowDetail = { showDetail = true },
                    onExpandChanged = { expanded ->
                        expandedExIdx = if (expanded) idx else -1
                    }
                )
                if (showDetail) {
                    ExerciseDetailSheet(
                        exercise  = exercise,
                        onDismiss = { showDetail = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakBanner(streak: Int) {
    val streakDays = streak
    val accent  = MaterialTheme.colorScheme.primary
    val strings = LocalAppTheme.current.strings
    val bgBrush = remember(accent) {
        Brush.horizontalGradient(listOf(accent.copy(0.18f), Amber.copy(0.12f)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 60.dp, 24.dp, 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(20.dp, 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔥", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (streakDays > 0) strings.streakTitle.format(streakDays) else strings.streakStart,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    if (streakDays > 0) strings.streakMotivate else strings.streakBegin,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Dashboard Header with Progress Ring ───────────────────────────────────────
@Composable
private fun WorkoutDashboardHeader(day: WorkoutDay, progress: Float) {
    val strings = LocalAppTheme.current.strings
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 12.dp, 24.dp, 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = strings.helloAthlete,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                // Pill stats
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (day.totalKcal > 0) StatPill("${day.totalKcal}", "kcal", Amber)
                    if (day.durationMin > 0) StatPill("${day.durationMin}", strings.unitMin, MaterialTheme.colorScheme.primary)
                }
            }

            // Right: Circular progress ring (only if not rest day and has exercises)
            if (!day.isRestDay && day.exercises.isNotEmpty()) {
                CircularProgressRing(
                    progress = progress,
                    size = 96.dp,
                    label = "${(progress * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
private fun StatPill(value: String, unit: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(12.dp, 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = unit,
                color = color.copy(0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Circular Progress Ring ─────────────────────────────────────────────────
@Composable
fun CircularProgressRing(
    progress: Float,
    size: Dp,
    label: String,
    trackColor: Color = Surface3,
    ringColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val resolvedRingColor = if (ringColor == Color.Unspecified) MaterialTheme.colorScheme.primary else ringColor

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "progress"
    )
    // Memoize sweep brush — recreating it on every animation frame (60fps × 1.2s) is expensive
    val sweepBrush = remember(resolvedRingColor) {
        Brush.sweepGradient(listOf(resolvedRingColor, resolvedRingColor.copy(0.75f), resolvedRingColor))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 9.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val startAngle = -90f

            drawCircle(
                color = trackColor,
                radius = radius,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    brush = sweepBrush,
                    startAngle = startAngle,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
        }

        val strings = LocalAppTheme.current.strings
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                color = if (progress > 0) resolvedRingColor else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = strings.unitDone,
                color = TextMuted,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}

// ── Day Selector Strip ─────────────────────────────────────────────────────
@Composable
private fun DaySelector(
    days: List<WorkoutDayState>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val accent   = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val theme    = LocalAppTheme.current
    val haptic   = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 20.dp, 16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEachIndexed { idx, state ->
            val day        = state.day
            val isSelected = idx == selectedIndex
            val iSource    = remember { MutableInteractionSource() }
            val isPressed  by iSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (isPressed) 0.92f else 1f,
                spring(Spring.DampingRatioMediumBouncy),
                label = "scale"
            )
            val hasProgress = state.progress > 0f && !day.isRestDay

            Box(
                modifier = Modifier
                    .scale(scale)
                    .weight(1f)
                    .height(64.dp)
                    .then(
                        if (isSelected)
                            Modifier.clip(RoundedCornerShape(16.dp)).background(accent)
                        else
                            Modifier.glassCard(accent, theme, RoundedCornerShape(16.dp))
                    )
                    .clickable(iSource, null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(idx)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day.day.uppercase(),
                        color = if (isSelected) onAccent else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (day.isRestDay) {
                        Icon(
                            Icons.Rounded.Hotel,
                            null,
                            tint = if (isSelected) onAccent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${day.exercises.size}",
                            color = if (isSelected) onAccent else TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        // Small progress dot
                        if (hasProgress) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) onAccent else accent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestDayView() {
    val accent  = MaterialTheme.colorScheme.primary
    val theme   = LocalAppTheme.current
    val strings = theme.strings
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 40.dp, 24.dp, 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(accent.copy(0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Hotel,
                null,
                tint = accent.copy(0.6f),
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            strings.restDayTitle, color = theme.text0,
            fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
        )
        Text(
            strings.restDaySubtitle,
            color = theme.text1, fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 0.dp)
        )
        Spacer(Modifier.height(32.dp))
        val tips = listOf(strings.recoveryTip1, strings.recoveryTip2, strings.recoveryTip3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tips.forEach { tip ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bg2)
                        .padding(20.dp, 14.dp)
                ) {
                    Text(tip, color = theme.text1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
