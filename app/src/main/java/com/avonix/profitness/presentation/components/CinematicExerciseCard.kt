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
    // Progressive overload — per-set weight & reps
    setWeights: Map<Int, String> = emptyMap(),
    setReps: Map<Int, String> = emptyMap(),
    lastSessionData: Map<Int, Pair<Float?, Int?>> = emptyMap(),
    onSetWeightChanged: (setIndex: Int, value: String) -> Unit = { _, _ -> },
    onSetRepsChanged: (setIndex: Int, value: String) -> Unit = { _, _ -> },
    // Timer — ViewModel'den gelir, lokal state yok
    timerSeconds: Int = 0,
    timerRunning: Boolean = false,
    timerDone: Boolean = false,
    onStartTimer: (seconds: Int) -> Unit = {},
    onStopTimer: () -> Unit = {}
) {
    val accent   = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val context  = LocalContext.current
    val haptic   = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    val imageRequest = remember(exercise.image) {
        ImageRequest.Builder(context)
            .data(exercise.image)
            .size(720, 360)
            .crossfade(false)
            .build()
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
        animationSpec = tween(90, easing = FastOutSlowInEasing),
        label         = "card_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue   = if (isCompleted) 0.25f else 0f,
        animationSpec = tween(260),
        label         = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp)
            .scale(cardScale)
    ) {
        ForgeCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication        = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                    onExpandChanged?.invoke(isExpanded)
                },
            glowColor = if (isCompleted) accent else Color.Transparent,
            elevation = if (isExpanded) 8.dp else 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // ── Fixed-height image header ────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
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
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(0.4f),
                                        Color.Black.copy(if (isExpanded) 0.92f else 0.82f)
                                    )
                                )
                            )
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
                                        text = exercise.category.uppercase(),
                                        color = catColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exercise.name.uppercase(),
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
                                Text(text = exercise.target, color = Mist, style = MaterialTheme.typography.bodySmall)
                            }
                            val doneCount = doneSetIndices.size
                            StatBadge(sets = exercise.sets, reps = exercise.reps, done = doneCount)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(90)),
                    exit = shrinkVertically(
                        animationSpec = tween(100, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(60))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.92f))
                            .padding(20.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))
                        repeat(exercise.sets) { i ->
                            val lastData = lastSessionData[i]
                            SetRow(
                                setNumber       = i + 1,
                                reps            = exercise.reps,
                                isDone          = i in doneSetIndices,
                                weightValue     = setWeights[i] ?: "",
                                repsValue       = setReps[i] ?: "",
                                lastWeightKg    = lastData?.first,
                                lastRepsActual  = lastData?.second,
                                plannedWeightKg = exercise.weightKg,
                                onWeightChanged = { onSetWeightChanged(i, it) },
                                onRepsChanged   = { onSetRepsChanged(i, it) },
                                onToggle        = {
                                    val wasAlreadyDone = i in doneSetIndices
                                    onToggleSet(i)
                                    if (!wasAlreadyDone) onStartTimer(exercise.restSeconds)
                                }
                            )
                            if (i < exercise.sets - 1) Spacer(Modifier.height(6.dp))
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Snow.copy(0.08f))
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RestTimerChip(
                                seconds        = timerSeconds,
                                isRunning      = timerRunning,
                                isDone         = timerDone,
                                defaultSeconds = exercise.restSeconds,
                                onStart        = { onStartTimer(exercise.restSeconds) },
                                onStop         = onStopTimer
                            )

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onComplete()
                                    isExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) Surface3 else accent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(16.dp, 8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle, null,
                                    tint = if (isCompleted) TextSecondary else onAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isCompleted) "GERİ AL" else "TAMAMLA",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (isCompleted) TextSecondary else onAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Set Row — ağırlık & tekrar girişli ───────────────────────────────────────
@Composable
private fun SetRow(
    setNumber: Int,
    reps: String,
    isDone: Boolean,
    weightValue: String,
    repsValue: String,
    lastWeightKg: Float?,
    lastRepsActual: Int?,
    plannedWeightKg: Float,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onToggle: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val bgAlpha by animateFloatAsState(
        if (isDone) 0.15f else 0.08f,
        tween(250),
        label = "set_bg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDone) accent.copy(bgAlpha) else Surface3.copy(0.5f))
            .padding(12.dp, 8.dp)
    ) {
        // Üst satır: checkbox + set no + ağırlık input + tekrar input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Icon(
                imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) accent else TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    }
            )
            Spacer(Modifier.width(8.dp))

            // Set number
            Text(
                text = "Set $setNumber",
                color = if (isDone) accent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )

            Spacer(Modifier.width(8.dp))

            // Ağırlık input
            WeightInputField(
                value = weightValue,
                onValueChange = onWeightChanged,
                placeholder = if (plannedWeightKg > 0) "${"%.0f".format(plannedWeightKg)}" else "kg",
                isDone = isDone,
                accent = accent,
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))

            // Tekrar input
            WeightInputField(
                value = repsValue,
                onValueChange = onRepsChanged,
                placeholder = reps,
                isDone = isDone,
                accent = accent,
                suffix = "rep",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }

        // Alt satır: önceki antrenman bilgisi
        if (lastWeightKg != null || lastRepsActual != null) {
            Spacer(Modifier.height(3.dp))
            val lastText = buildString {
                append("Son: ")
                if (lastWeightKg != null) append("${"%.1f".format(lastWeightKg)}kg")
                if (lastWeightKg != null && lastRepsActual != null) append(" x ")
                if (lastRepsActual != null) append("${lastRepsActual}rep")
            }
            Text(
                text = lastText,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 26.dp)
            )
        }
    }
}

@Composable
private fun WeightInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDone: Boolean,
    accent: Color,
    suffix: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface3.copy(0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newVal ->
                // Sadece sayısal karakter ve nokta/virgül izin ver
                val filtered = newVal.filter { it.isDigit() || it == '.' || it == ',' }
                onValueChange(filtered)
            },
            textStyle = TextStyle(
                color = if (isDone) accent else Snow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            }
        )
        Text(
            text = suffix,
            color = if (isDone) accent.copy(0.6f) else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

// ── Rest Timer Chip ───────────────────────────────────────────────────────────
@Composable
private fun RestTimerChip(
    seconds: Int,
    isRunning: Boolean,
    isDone: Boolean,
    defaultSeconds: Int = 90,
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
    val chipColor = when {
        isDone    -> Amber
        isRunning -> accent
        else      -> Surface3
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipColor.copy(0.15f))
            .border(1.dp, chipColor.copy(0.3f), RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (isRunning) onStop() else onStart()
            }
            .padding(12.dp, 8.dp)
            .then(if (isRunning) Modifier.scale(pulseScale.value) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Timer, null, tint = chipColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = when {
                isDone    -> "DİNLENDİN! ✓"
                isRunning -> "${seconds}s"
                else      -> "${defaultSeconds}s REST"
            },
            color = chipColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatBadge(sets: Int, reps: String, done: Int = 0) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp, 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${sets}x", color = Amber, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(text = reps, color = Snow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (done > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "($done✓)",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
