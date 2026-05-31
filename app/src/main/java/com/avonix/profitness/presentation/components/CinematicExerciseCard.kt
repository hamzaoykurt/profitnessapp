package com.avonix.profitness.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.workout.Exercise
import com.avonix.profitness.presentation.workout.ExerciseMetric
import com.avonix.profitness.presentation.workout.activityTrackingSpec
import com.avonix.profitness.presentation.workout.isDurationSetExercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CinematicExerciseCard(
    exercise: Exercise,
    index: Int,
    isCompleted: Boolean = false,
    doneSetIndices: Set<Int> = emptySet(),
    onToggleSet: (setIndex: Int) -> Unit = {},
    onComplete: () -> Unit = {},
    onShowDetail: (() -> Unit)? = null,
    onExpandChanged: ((Boolean) -> Unit)? = null,
    // Progressive overload — per-set weight input
    setWeights: Map<Int, String> = emptyMap(),
    setDurations: Map<Int, String> = emptyMap(),
    lastSessionData: Map<Int, Pair<Float?, Int?>> = emptyMap(),
    profileWeightKg: Float = 0f,
    activityDuration: String = "",
    activityDistance: String = "",
    activityElevation: String = "",
    activityIncline: String = "",
    activityReps: String = "",
    onSetWeightChanged: (setIndex: Int, value: String) -> Unit = { _, _ -> },
    onSetDurationChanged: (setIndex: Int, value: String) -> Unit = { _, _ -> },
    onActivityDurationChanged: (String) -> Unit = {},
    onActivityDistanceChanged: (String) -> Unit = {},
    onActivityElevationChanged: (String) -> Unit = {},
    onActivityInclineChanged: (String) -> Unit = {},
    onActivityRepsChanged: (String) -> Unit = {},
    // Timer — ViewModel'den gelir, lokal state yok
    timerSeconds: Int = 0,
    timerRunning: Boolean = false,
    timerDone: Boolean = false,
    onStartTimer: (seconds: Int) -> Unit = {},
    onStartSetTimer: (setIndex: Int, seconds: Int) -> Unit = { _, _ -> },
    onStartSetStopwatchTimer: (setIndex: Int) -> Unit = {},
    onStartStopwatchTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {}
) {
    val accent   = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val haptic   = LocalHapticFeedback.current
    val context  = LocalContext.current
    val theme    = LocalAppTheme.current
    var isExpanded by remember { mutableStateOf(false) }
    var showActivityTimerSetup by remember { mutableStateOf(false) }
    var showTimedSetTimerSetup by remember { mutableStateOf(false) }
    val trackingSpec = remember(
        exercise.category, exercise.name, exercise.target, exercise.reps,
        exercise.sportType, exercise.trackingMode
    ) {
        activityTrackingSpec(
            category = exercise.category,
            name = exercise.name,
            target = exercise.target,
            reps = exercise.reps,
            sportTypeRaw = exercise.sportType,
            trackingModeRaw = exercise.trackingMode
        )
    }
    val activityMetric = trackingSpec.metric
    val durationSetBased = isDurationSetExercise(
        category = exercise.category,
        name = exercise.name,
        target = exercise.target,
        reps = exercise.reps,
        sets = exercise.sets,
        sportTypeRaw = exercise.sportType,
        trackingModeRaw = exercise.trackingMode
    )
    val activityBased = activityMetric != ExerciseMetric.Strength && !durationSetBased
    val nextTimedSetIndex = remember(durationSetBased, doneSetIndices, exercise.sets) {
        if (durationSetBased) {
            (0 until exercise.sets).firstOrNull { it !in doneSetIndices } ?: 0
        } else {
            0
        }
    }
    val nextTimedSetDurationSeconds = remember(durationSetBased, nextTimedSetIndex, setDurations, exercise.reps, exercise.targetDurationSeconds) {
        if (durationSetBased) {
            setDurations[nextTimedSetIndex]?.toIntOrNull()?.takeIf { it > 0 }
                ?: exercise.targetDurationSeconds?.takeIf { it > 0 }
                ?: exercise.reps.toDurationSetSecondsLabel().toIntOrNull()
                ?: 60
        } else {
            0
        }
    }

    // Timer bu egzersiz için başladığında kartı otomatik aç
    LaunchedEffect(timerRunning) {
        if (timerRunning && !isExpanded) {
            isExpanded = true
            onExpandChanged?.invoke(true)
        }
    }

    // Press scale — snappy spring
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "card_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue   = if (isCompleted) 0.25f else 0f,
        animationSpec = tween(600),
        label         = "glow"
    )
    val imageOverlayBrush = remember(isExpanded) {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Black.copy(0.4f),
                Color.Black.copy(if (isExpanded) 0.92f else 0.82f)
            )
        )
    }
    val imageRequest = remember(context, exercise.image) {
        ImageRequest.Builder(context)
            .data(exercise.image)
            .size(720, 360)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp)
            .scale(cardScale)
    ) {
        ForgeCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            glowColor = if (isCompleted) accent else Color.Transparent,
            elevation = if (isCompleted) 10.dp else 6.dp
        ) {
            // animateContentSize gives the same bouncy height expansion as before,
            // but is measured via placement — no explicit height state, no layout-per-frame jank
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessLow
                        )
                    )
            ) {
                // ── Fixed-height image header ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication        = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = !isExpanded
                            onExpandChanged?.invoke(isExpanded)
                        }
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (isCompleted) {
                        Box(modifier = Modifier.fillMaxSize().background(accent.copy(glowAlpha)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(imageOverlayBrush)
                    )

                    // Info button — top-right corner
                    if (onShowDetail != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.45f))
                                .clickable { onShowDetail() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Info, null,
                                tint = Snow.copy(0.9f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val catColor = when (exercise.category) {
                                    "Bodyweight" -> CardCyan
                                    "Cable"      -> CardPurple
                                    else         -> Lime
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(catColor.copy(0.2f))
                                        .padding(8.dp, 3.dp)
                                ) {
                                    Text(
                                        text = theme.fitnessTermDisplayName(exercise.category).uppercase(),
                                        color = catColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = theme.exerciseDisplayName(exercise.name).uppercase(),
                                        color = Snow,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 28.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCompleted) {
                                        Icon(
                                            Icons.Rounded.CheckCircle, null,
                                            tint = accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = theme.fitnessTermDisplayName(exercise.target),
                                    color = Mist,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            val doneCount = doneSetIndices.size
                            StatBadge(
                                sets = exercise.sets,
                                reps = if (durationSetBased) {
                                    exercise.targetDurationSeconds?.toString() ?: exercise.reps
                                } else {
                                    exercise.reps
                                },
                                done = doneCount,
                                isActivity = activityBased,
                                isDurationSet = durationSetBased
                            )
                        }
                    }
                }

                // ── Expanded panel — rendered as part of the same Column so
                //    animateContentSize handles the height change smoothly ──────────
                if (isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.92f))
                            .padding(20.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))
                        if (activityBased) {
                            ActivityMetricsPanel(
                                durationValue = activityDuration,
                                distanceValue = activityDistance,
                                elevationValue = activityElevation,
                                inclineValue = activityIncline,
                                repsValue = activityReps,
                                specLabel = trackingSpec.sportType.label,
                                supportsDistance = trackingSpec.supportsDistance,
                                supportsElevation = trackingSpec.supportsElevation,
                                supportsIncline = trackingSpec.supportsIncline,
                                supportsReps = trackingSpec.supportsReps,
                                isDone = isCompleted,
                                onDurationChanged = onActivityDurationChanged,
                                onDistanceChanged = onActivityDistanceChanged,
                                onElevationChanged = onActivityElevationChanged,
                                onInclineChanged = onActivityInclineChanged,
                                onRepsChanged = onActivityRepsChanged
                            )
                        } else if (durationSetBased) {
                            repeat(exercise.sets) { i ->
                                TimedSetRow(
                                    setNumber = i + 1,
                                    durationValue = setDurations[i]
                                        ?: exercise.targetDurationSeconds?.toString()
                                        ?: exercise.reps.toDurationSetSecondsLabel(),
                                    isDone = i in doneSetIndices,
                                    onDurationChanged = { onSetDurationChanged(i, it) },
                                    onToggle = { onToggleSet(i) }
                                )
                                if (i < exercise.sets - 1) Spacer(Modifier.height(6.dp))
                            }
                        } else {
                            repeat(exercise.sets) { i ->
                                val lastData = lastSessionData[i]
                                val defaultWeightKg = when {
                                    exercise.weightKg > 0f -> exercise.weightKg
                                    exercise.category.equals("Bodyweight", ignoreCase = true) && profileWeightKg > 0f -> profileWeightKg
                                    else -> 0f
                                }
                                SetRow(
                                    setNumber       = i + 1,
                                    reps            = exercise.reps,
                                    isDone          = i in doneSetIndices,
                                    weightValue     = setWeights[i] ?: "",
                                    lastWeightKg    = lastData?.first,
                                    lastRepsActual  = lastData?.second,
                                    defaultWeightKg = defaultWeightKg,
                                    onWeightChanged = { onSetWeightChanged(i, it) },
                                    onToggle        = { onToggleSet(i) }
                                )
                                if (i < exercise.sets - 1) Spacer(Modifier.height(6.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        if (activityBased && showActivityTimerSetup) {
                            ActivityTimerSetupPanel(
                                initialMinutes = activityDuration,
                                onStartCountdown = { seconds ->
                                    showActivityTimerSetup = false
                                    onStartTimer(seconds)
                                },
                                onStartStopwatch = {
                                    showActivityTimerSetup = false
                                    onStartStopwatchTimer()
                                }
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        if (durationSetBased && showTimedSetTimerSetup) {
                            ActivityTimerSetupPanel(
                                initialMinutes = "",
                                initialSeconds = nextTimedSetDurationSeconds,
                                onStartCountdown = { seconds ->
                                    showTimedSetTimerSetup = false
                                    onStartSetTimer(nextTimedSetIndex, seconds)
                                },
                                onStartStopwatch = {
                                    showTimedSetTimerSetup = false
                                    onStartSetStopwatchTimer(nextTimedSetIndex)
                                }
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        HorizontalDivider(color = Snow.copy(0.08f))
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activityBased) {
                                RestTimerChip(
                                    seconds        = timerSeconds,
                                    isRunning      = timerRunning,
                                    isDone         = timerDone,
                                    defaultSeconds = activityDuration.toDurationSeconds(),
                                    idleLabel      = theme.t("SAYAÇ", "TIMER"),
                                    doneLabel      = theme.t("SÜRE BİTTİ", "TIME'S UP"),
                                    onStart        = {
                                        if (timerRunning) onStopTimer()
                                        else showActivityTimerSetup = !showActivityTimerSetup
                                    },
                                    onStop         = onStopTimer
                                )
                            } else if (durationSetBased) {
                                RestTimerChip(
                                    seconds        = timerSeconds,
                                    isRunning      = timerRunning,
                                    isDone         = timerDone,
                                    defaultSeconds = nextTimedSetDurationSeconds,
                                    idleLabel      = theme.t("SET SAYACI", "SET TIMER"),
                                    doneLabel      = theme.t("SET BİTTİ", "SET DONE"),
                                    onStart        = {
                                        if (timerRunning) onStopTimer()
                                        else showTimedSetTimerSetup = !showTimedSetTimerSetup
                                    },
                                    onStop         = onStopTimer
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            CompleteActionButton(
                                isCompleted = isCompleted,
                                accent = accent,
                                onAccent = onAccent,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onComplete()
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricsPanel(
    durationValue: String,
    distanceValue: String,
    elevationValue: String,
    inclineValue: String,
    repsValue: String,
    specLabel: String,
    supportsDistance: Boolean,
    supportsElevation: Boolean,
    supportsIncline: Boolean,
    supportsReps: Boolean,
    isDone: Boolean,
    onDurationChanged: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onElevationChanged: (String) -> Unit,
    onInclineChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val backgroundBrush = remember(isDone, accent) {
        Brush.verticalGradient(
            listOf(
                if (isDone) accent.copy(0.18f) else Surface3.copy(0.70f),
                Surface2.copy(0.48f)
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isDone) accent.copy(0.42f) else Snow.copy(0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = when {
                supportsDistance -> theme.t("$specLabel · süre ve mesafe", "$specLabel · duration and distance")
                supportsReps -> theme.t("$specLabel · süre ve sayı", "$specLabel · duration and reps")
                else -> theme.t("$specLabel · süre", "$specLabel · duration")
            },
            color = if (isDone) accent else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WeightInputField(
                value = durationValue,
                onValueChange = onDurationChanged,
                placeholder = "0",
                label = theme.t("Süre", "Duration"),
                isDone = isDone,
                accent = accent,
                suffix = theme.t("dk", "min"),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            if (supportsDistance) {
                WeightInputField(
                    value = distanceValue,
                    onValueChange = onDistanceChanged,
                    placeholder = "0",
                    label = theme.t("Mesafe", "Distance"),
                    isDone = isDone,
                    accent = accent,
                    suffix = "m",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }
            if (supportsReps) {
                WeightInputField(
                    value = repsValue,
                    onValueChange = onRepsChanged,
                    placeholder = "0",
                    label = theme.t("Sayı", "Count"),
                    isDone = isDone,
                    accent = accent,
                    suffix = theme.t("adet", "reps"),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (supportsElevation || supportsIncline) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (supportsElevation) {
                    WeightInputField(
                        value = elevationValue,
                        onValueChange = onElevationChanged,
                        placeholder = "0",
                        label = theme.t("Yükselti", "Elevation"),
                        isDone = isDone,
                        accent = accent,
                        suffix = "m",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (supportsIncline) {
                    WeightInputField(
                        value = inclineValue,
                        onValueChange = onInclineChanged,
                        placeholder = "0",
                        label = theme.t("Eğim", "Incline"),
                        isDone = isDone,
                        accent = accent,
                        suffix = "%",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricDisplayTile(
    label: String,
    value: String,
    accent: Color,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(Surface3.copy(0.86f), Surface2.copy(0.64f)))
    }
    Row(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isDone) accent.copy(0.34f) else Snow.copy(0.08f),
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDone) accent.copy(0.22f) else Surface1.copy(0.76f))
                .border(1.dp, if (isDone) accent.copy(0.48f) else Snow.copy(0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Timer,
                null,
                tint = if (isDone) accent else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                color = if (isDone) accent else Snow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActivityTimerSetupPanel(
    initialMinutes: String,
    initialSeconds: Int? = null,
    onStartCountdown: (Int) -> Unit,
    onStartStopwatch: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    var mode by remember { mutableStateOf("stopwatch") }
    val initialTotalSeconds = remember(initialMinutes, initialSeconds) {
        initialSeconds ?: initialMinutes.toDurationSeconds()
    }
    var hours by remember(initialTotalSeconds) { mutableStateOf(if (initialTotalSeconds >= 3600) (initialTotalSeconds / 3600).toString() else "") }
    var minutes by remember(initialTotalSeconds) { mutableStateOf(((initialTotalSeconds % 3600) / 60).takeIf { it > 0 }?.toString() ?: "") }
    var seconds by remember(initialTotalSeconds) { mutableStateOf((initialTotalSeconds % 60).takeIf { it > 0 }?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface3.copy(0.55f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerModeChip(
                label = theme.t("KRONOMETRE", "STOPWATCH"),
                selected = mode == "stopwatch",
                onClick = { mode = "stopwatch" },
                modifier = Modifier.weight(1f)
            )
            TimerModeChip(
                label = theme.t("GERİ SAYIM", "COUNTDOWN"),
                selected = mode == "countdown",
                onClick = { mode = "countdown" },
                modifier = Modifier.weight(1f)
            )
        }
        if (mode == "countdown") {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTimeInput(hours, { hours = it }, theme.t("saat", "hr"), Modifier.weight(1f))
                SmallTimeInput(minutes, { minutes = it }, theme.t("dk", "min"), Modifier.weight(1f))
                SmallTimeInput(seconds, { seconds = it }, theme.t("sn", "sec"), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        val countdownSeconds = (hours.toIntOrNull() ?: 0) * 3600 +
            (minutes.toIntOrNull() ?: 0) * 60 +
            (seconds.toIntOrNull() ?: 0)
        val enabled = mode == "stopwatch" || countdownSeconds > 0
        Button(
            onClick = {
                if (mode == "stopwatch") onStartStopwatch()
                else onStartCountdown(countdownSeconds)
            },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (mode == "stopwatch") {
                    theme.t("KRONOMETREYİ BAŞLAT", "START STOPWATCH")
                } else {
                    theme.t("GERİ SAYIMI BAŞLAT", "START COUNTDOWN")
                },
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TimerModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(0.22f) else Surface3.copy(0.6f))
            .border(1.dp, if (selected) accent.copy(0.5f) else Snow.copy(0.08f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SmallTimeInput(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    WeightInputField(
        value = value,
        onValueChange = { onValueChange(it.take(2)) },
        placeholder = "0",
        isDone = false,
        accent = accent,
        suffix = suffix,
        keyboardType = KeyboardType.Number,
        modifier = modifier
    )
}

@Composable
private fun CompleteActionButton(
    isCompleted: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val bg = remember(isCompleted, accent) {
        if (isCompleted) {
            Brush.horizontalGradient(listOf(Surface3.copy(0.88f), Surface2.copy(0.72f)))
        } else {
            Brush.horizontalGradient(listOf(accent, accent.copy(0.86f)))
        }
    }
    Row(
        modifier = Modifier
            .heightIn(min = 54.dp)
            .widthIn(min = 168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                1.dp,
                if (isCompleted) Snow.copy(0.10f) else Snow.copy(0.18f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Surface1.copy(0.74f) else Color.White.copy(0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = if (isCompleted) TextSecondary else onAccent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = if (isCompleted) theme.t("GERİ AL", "UNDO") else theme.t("TAMAMLA", "COMPLETE"),
            color = if (isCompleted) TextSecondary else onAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Set Row — ağırlık girişli; set/tekrar programdan gelir ───────────────────
@Composable
private fun TimedSetRow(
    setNumber: Int,
    durationValue: String,
    isDone: Boolean,
    onDurationChanged: (String) -> Unit,
    onToggle: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val durationSeconds = durationValue.toDurationSetSecondsLabel()
    val durationDisplay = durationSeconds.ifBlank { "0" }
    val bgAlpha by animateFloatAsState(
        if (isDone) 0.20f else 0.08f,
        tween(250),
        label = "timed_set_bg"
    )
    val backgroundBrush = remember(isDone, accent, bgAlpha) {
        Brush.horizontalGradient(
            listOf(
                if (isDone) accent.copy(bgAlpha) else Surface3.copy(0.68f),
                Surface2.copy(0.48f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(1.dp, if (isDone) accent.copy(0.50f) else Snow.copy(0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WeightInputField(
                value = durationSeconds,
                onValueChange = onDurationChanged,
                placeholder = "60",
                label = theme.t("Süre", "Duration"),
                isDone = isDone,
                accent = accent,
                suffix = theme.t("sn", "sec"),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1.05f)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.widthIn(min = 80.dp).weight(0.75f)) {
                Text("Set $setNumber", color = if (isDone) accent else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(theme.t("$durationDisplay sn", "$durationDisplay sec"), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            SetToggleButton(isDone = isDone, accent = accent, onToggle = onToggle)
        }
    }
}

@Composable
private fun SetToggleButton(
    isDone: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDone) accent.copy(0.22f) else Surface1.copy(0.75f))
                .border(1.dp, if (isDone) accent.copy(0.65f) else TextMuted.copy(0.42f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) accent else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    reps: String,
    isDone: Boolean,
    weightValue: String,
    lastWeightKg: Float?,
    lastRepsActual: Int?,
    defaultWeightKg: Float,
    onWeightChanged: (String) -> Unit,
    onToggle: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val bgAlpha by animateFloatAsState(
        if (isDone) 0.20f else 0.08f,
        tween(250),
        label = "set_bg"
    )
    val backgroundBrush = remember(isDone, accent, bgAlpha) {
        Brush.horizontalGradient(
            listOf(
                if (isDone) accent.copy(bgAlpha) else Surface3.copy(0.68f),
                Surface2.copy(0.48f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isDone) accent.copy(0.50f) else Snow.copy(0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        // Üst satır: ağırlık input + set no + program tekrarı + checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ağırlık input
            WeightInputField(
                value = weightValue,
                onValueChange = onWeightChanged,
                placeholder = if (defaultWeightKg > 0) "${"%.0f".format(defaultWeightKg)}" else "0",
                label = theme.t("Ağırlık", "Weight"),
                isDone = isDone,
                accent = accent,
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1.05f)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.widthIn(min = 80.dp).weight(0.75f)) {
                Text(
                    text = "Set $setNumber",
                    color = if (isDone) accent else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = theme.t("$reps tekrar", "$reps reps"),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            SetToggleButton(isDone = isDone, accent = accent, onToggle = onToggle)
        }

        // Alt satır: önceki antrenman bilgisi
        if (lastWeightKg != null || lastRepsActual != null) {
            Spacer(Modifier.height(8.dp))
            val lastText = buildString {
                append(theme.t("Son: ", "Last: "))
                if (lastWeightKg != null) append("${"%.1f".format(lastWeightKg)}kg")
                if (lastWeightKg != null && lastRepsActual != null) append(" x ")
                if (lastRepsActual != null) append("${lastRepsActual}rep")
            }
            Text(
                text = lastText,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun WeightInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    isDone: Boolean,
    accent: Color,
    suffix: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(Surface3.copy(0.86f), Surface2.copy(0.64f)))
    }
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isDone) accent.copy(0.34f) else Snow.copy(0.08f),
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = { newVal ->
                    // Sadece sayısal karakter ve nokta/virgül izin ver
                    val filtered = newVal.filter { it.isDigit() || it == '.' || it == ',' }
                    onValueChange(filtered)
                },
                textStyle = TextStyle(
                    color = if (isDone) accent else Snow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextMuted.copy(0.82f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start
                            )
                        }
                        inner()
                    }
                }
            )
            Text(
                text = suffix,
                color = if (isDone) accent.copy(0.72f) else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

// ── Rest Timer Chip ───────────────────────────────────────────────────────────
@Composable
private fun RestTimerChip(
    seconds: Int,
    isRunning: Boolean,
    isDone: Boolean,
    defaultSeconds: Int = 90,
    idleLabel: String = "SÜRE",
    doneLabel: String = "DİNLENDİN! ✓",
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive) {
                pulseScale.animateTo(1.06f, tween(700))
                pulseScale.animateTo(1f, tween(700))
            }
        } else {
            pulseScale.snapTo(1f)
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val isIdle = !isRunning && !isDone
    val chipColor = when {
        isDone    -> Amber
        isRunning -> accent
        else      -> accent
    }
    val chipBackground = remember(isDone, isRunning, accent) {
        when {
            isDone -> Brush.horizontalGradient(
                listOf(Amber.copy(0.28f), Amber.copy(0.12f))
            )
            isRunning -> Brush.horizontalGradient(
                listOf(accent.copy(0.34f), accent.copy(0.18f))
            )
            else -> Brush.horizontalGradient(
                listOf(accent.copy(0.20f), Surface3.copy(0.62f))
            )
        }
    }

    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 132.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(chipBackground)
            .border(
                1.dp,
                chipColor.copy(if (isIdle) 0.62f else 0.74f),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (isRunning) onStop() else onStart()
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .then(if (isRunning) Modifier.scale(pulseScale.value) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(chipColor.copy(if (isIdle) 0.22f else 0.30f))
                .border(1.dp, chipColor.copy(0.36f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Timer,
                null,
                tint = chipColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = when {
                isDone    -> doneLabel
                isRunning -> "${seconds}s"
                else      -> idleLabel
            },
            color = if (isIdle) Snow else chipColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = if (isIdle) 0.9.sp else 0.2.sp
        )
    }
}

@Composable
fun StatBadge(sets: Int, reps: String, done: Int = 0, isActivity: Boolean = false, isDurationSet: Boolean = false) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp, 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActivity) {
                Icon(Icons.Rounded.Timer, null, tint = Amber, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(text = reps, color = Snow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                Text(text = "${sets}x", color = Amber, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                val displayReps = if (isDurationSet) reps.toDurationSetDisplayLabel(LocalAppTheme.current) else reps
                Text(text = displayReps, color = Snow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (done > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = LocalAppTheme.current.t("(${done} tamam)", "(${done} done)"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun String.toDurationSetDisplayLabel(theme: AppThemeState): String {
    val seconds = toDurationSetSecondsLabel()
    return if (seconds.isBlank()) this else "$seconds ${theme.t("sn", "sec")}"
}

private fun String.toDurationSetSecondsLabel(): String {
    val normalized = lowercase().normalizeTurkishForWorkout().trim()
    if (normalized.isBlank()) return ""
    val numeric = normalized
        .filter { it.isDigit() || it == ',' || it == '.' }
        .replace(',', '.')
        .toFloatOrNull()
        ?: return ""
    val seconds = when {
        normalized.contains("dk") || normalized.contains("min") || normalized.contains("dakika") ->
            (numeric * 60f).toInt()
        else -> numeric.toInt()
    }
    return seconds.coerceAtLeast(1).toString()
}

private fun String.normalizeTurkishForWorkout(): String =
    replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')

private fun String.toDurationSeconds(): Int =
    replace(',', '.')
        .toFloatOrNull()
        ?.let { (it * 60f).toInt().coerceAtLeast(1) }
        ?: 0

private fun String.toDurationLabel(): String {
    val totalSeconds = toDurationSeconds()
    if (totalSeconds <= 0) return "0:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (seconds > 0 || isEmpty()) append("${seconds}s")
    }.trim()
}
