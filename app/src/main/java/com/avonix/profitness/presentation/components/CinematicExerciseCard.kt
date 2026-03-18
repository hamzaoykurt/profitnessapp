package com.avonix.profitness.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.workout.Exercise
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CinematicExerciseCard(
    exercise: Exercise,
    index: Int,
    isCompleted: Boolean = false,
    onComplete: () -> Unit = {},
    onShowDetail: (() -> Unit)? = null
) {
    val accent   = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val haptic   = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    // Per-set completion state
    val setsDone = remember(exercise.id) { mutableStateListOf(*Array(exercise.sets) { false }) }
    val allSetsDone = setsDone.all { it }

    // FAZ 3D: Sync set states when isCompleted changes from outside
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            for (i in setsDone.indices) setsDone[i] = true
        }
    }

    // Rest timer state — FAZ 3E: use exercise.restSeconds instead of hardcoded 60
    var timerSeconds by remember(exercise.id) { mutableStateOf(exercise.restSeconds) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerDone    by remember { mutableStateOf(false) }

    // Count-down effect
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerSeconds > 0 && timerRunning) {
                delay(1000L)
                timerSeconds--
            }
            if (timerSeconds == 0) {
                timerRunning = false
                timerDone    = true
            }
        }
    }

    val cardHeight by animateDpAsState(
        targetValue = when {
            isExpanded -> if (isCompleted) 240.dp else (220 + exercise.sets * 44).dp
            else       -> 180.dp
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "height"
    )

    // Completion glow pulse
    val glowAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.25f else 0f,
        animationSpec = tween(600),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp)
    ) {
        ForgeCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                },
            glowColor = if (isCompleted) accent else Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── Background Image ─────────────────────────────────────────────
                AsyncImage(
                    model = exercise.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // ── Completion overlay ───────────────────────────────────────────
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accent.copy(glowAlpha))
                    )
                }

                // ── Deep Scrim Overlay ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(0.4f),
                                    Color.Black.copy(if (isExpanded) 0.95f else 0.88f)
                                )
                            )
                        )
                )

                // ── Content ──────────────────────────────────────────────────────
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
                            // Category pill
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
                                // FAZ 3C: Info button for how-to detail sheet
                                if (onShowDetail != null) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        null,
                                        tint = Snow.copy(0.5f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onShowDetail() }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (isCompleted) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = exercise.target,
                                color = Mist,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Stat Badge
                        val doneCount = setsDone.count { it }
                        StatBadge(
                            sets    = exercise.sets,
                            reps    = exercise.reps,
                            done    = doneCount
                        )
                    }

                    // ── Expanded Content ─────────────────────────────────────────
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = Snow.copy(0.1f))
                            Spacer(Modifier.height(12.dp))

                            // ── Set Tracker rows ────────────────────────────────
                            setsDone.forEachIndexed { i, done ->
                                SetRow(
                                    setNumber = i + 1,
                                    reps      = exercise.reps,
                                    isDone    = done,
                                    onToggle  = { setsDone[i] = !setsDone[i] }
                                )
                                if (i < setsDone.lastIndex) Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Snow.copy(0.08f))
                            Spacer(Modifier.height(12.dp))

                            // ── Rest Timer + Complete row ────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rest timer button
                                RestTimerChip(
                                    seconds     = timerSeconds,
                                    isRunning   = timerRunning,
                                    isDone      = timerDone,
                                    defaultSeconds = exercise.restSeconds,
                                    onStart     = {
                                        timerSeconds = exercise.restSeconds
                                        timerDone    = false
                                        timerRunning = true
                                    },
                                    onStop      = { timerRunning = false }
                                )

                                // Complete button
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
                                        Icons.Rounded.CheckCircle,
                                        null,
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
}

// ── Set Row ──────────────────────────────────────────────────────────────────
@Composable
private fun SetRow(
    setNumber: Int,
    reps: String,
    isDone: Boolean,
    onToggle: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val bgAlpha by animateFloatAsState(
        if (isDone) 0.15f else 0.08f,
        tween(300),
        label = "set_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDone) accent.copy(bgAlpha) else Surface3.copy(0.5f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isDone) accent else TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Set $setNumber",
            color = if (isDone) accent else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$reps tekrar",
            color = if (isDone) accent.copy(0.7f) else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
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
            while (true) {
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
        Icon(
            Icons.Rounded.Timer,
            null,
            tint = chipColor,
            modifier = Modifier.size(16.dp)
        )
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
            Text(
                text = "${sets}x",
                color = Amber,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = reps,
                color = Snow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
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
