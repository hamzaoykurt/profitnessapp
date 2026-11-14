package com.cosmibit.profitness.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import com.cosmibit.profitness.core.ui.cappedDisplaySp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.AppFontFamily
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.sinemaGlass
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.presentation.components.ActivityMetricsPanel
import com.cosmibit.profitness.presentation.components.ActivityTimerSetupPanel
import com.cosmibit.profitness.presentation.components.CompleteActionButton
import com.cosmibit.profitness.presentation.components.TimedSetRow
import com.cosmibit.profitness.presentation.components.toDurationSetSecondsLabel

internal enum class SinemaSetKind { Strength, Timed, Activity }

internal fun sinemaSetKind(e: Exercise): SinemaSetKind {
    if (isDurationSetExercise(
            category = e.category, name = e.name, target = e.target, reps = e.reps, sets = e.sets,
            sportTypeRaw = e.sportType, trackingModeRaw = e.trackingMode
        )
    ) return SinemaSetKind.Timed
    return if (isActivityBasedExercise(e)) SinemaSetKind.Activity else SinemaSetKind.Strength
}

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Cam alt sheet (Liste + Set). İçerik her zaman koyu cam olduğundan tema yerel olarak koyuya çevrilir
 * (açık temada da mevcut panellerin metinleri okunur kalır).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SinemaGlassSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalAppTheme.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = SheetShape,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        CompositionLocalProvider(LocalAppTheme provides theme) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .sinemaGlass(SheetShape, fillAlpha = 0.96f)   // blur yok: sheet'te okunurluk için neredeyse opak
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.30f))
                )
                content()
            }
        }
    }
}

/** Set sheet'i: kuvvet (ağırlık) / süreli set / aktivite; mantık ve alanlar CinematicExerciseCard'dakilerdir. */
@Composable
internal fun SetSheet(
    exercise: Exercise,
    setIndex: Int,
    isCompleted: Boolean,
    data: SinemaExerciseData,
    actions: SinemaWorkoutActions,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val kind = sinemaSetKind(exercise)
    SinemaGlassSheet(onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            when (kind) {
                SinemaSetKind.Strength -> StrengthSetContent(exercise, setIndex, data, actions, onDismiss)
                SinemaSetKind.Timed -> {
                    val done = setIndex in data.doneSetIndices
                    val nextSeconds = data.setDurations[setIndex]?.toIntOrNull()?.takeIf { it > 0 }
                        ?: exercise.targetDurationSeconds?.takeIf { it > 0 }
                        ?: exercise.reps.toDurationSetSecondsLabel().toIntOrNull()
                        ?: 60
                    TimedSetRow(
                        setNumber = setIndex + 1,
                        durationValue = data.setDurations[setIndex]
                            ?: exercise.targetDurationSeconds?.toString()
                            ?: exercise.reps.toDurationSetSecondsLabel(),
                        isDone = done,
                        onDurationChanged = { actions.onSetDurationChanged(exercise, setIndex, it) },
                        onToggle = { actions.onToggleSet(exercise, setIndex); onDismiss() }
                    )
                    Spacer(Modifier.height(12.dp))
                    ActivityTimerSetupPanel(
                        initialMinutes = "",
                        initialSeconds = nextSeconds,
                        onStartCountdown = { secs -> actions.onStartSetTimer(exercise, setIndex, secs); onDismiss() },
                        onStartStopwatch = { actions.onStartSetStopwatch(exercise, setIndex); onDismiss() }
                    )
                }
                SinemaSetKind.Activity -> {
                    val spec = remember(exercise.id) { sinemaTrackingSpec(exercise) }
                    ActivityMetricsPanel(
                        durationValue = data.activityDuration,
                        distanceValue = data.activityDistance,
                        elevationValue = data.activityElevation,
                        inclineValue = data.activityIncline,
                        repsValue = data.activityReps,
                        specLabel = spec.sportType.label,
                        supportsDistance = spec.supportsDistance,
                        supportsElevation = spec.supportsElevation,
                        supportsIncline = spec.supportsIncline,
                        supportsReps = spec.supportsReps,
                        isDone = isCompleted,
                        onDurationChanged = { actions.onActivityDurationChanged(exercise, it) },
                        onDistanceChanged = { actions.onActivityDistanceChanged(exercise, it) },
                        onElevationChanged = { actions.onActivityElevationChanged(exercise, it) },
                        onInclineChanged = { actions.onActivityInclineChanged(exercise, it) },
                        onRepsChanged = { actions.onActivityRepsChanged(exercise, it) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ActivityTimerSetupPanel(
                        initialMinutes = data.activityDuration,
                        onStartCountdown = { secs -> actions.onStartTimer(exercise, secs); onDismiss() },
                        onStartStopwatch = { actions.onStartStopwatch(exercise); onDismiss() }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        CompleteActionButton(
                            isCompleted = isCompleted,
                            accent = theme.effectiveAccentColor,
                            onAccent = theme.effectiveOnAccentColor,
                            onClick = { actions.onComplete(exercise); onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthSetContent(
    exercise: Exercise,
    setIndex: Int,
    data: SinemaExerciseData,
    actions: SinemaWorkoutActions,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val done = setIndex in data.doneSetIndices
    val defaultWeightKg = when {
        exercise.weightKg > 0f -> exercise.weightKg
        exercise.category.equals("Bodyweight", ignoreCase = true) && data.profileWeightKg > 0f -> data.profileWeightKg
        else -> 0f
    }
    val value = data.setWeights[setIndex] ?: ""
    val placeholder = if (defaultWeightKg > 0f) "%.0f".format(defaultWeightKg) else "0"

    Text(
        theme.t("Set ${setIndex + 1} · ${exercise.reps} tekrar", "Set ${setIndex + 1} · ${exercise.reps} reps")
            .sinemaUpper(theme),
        style = Sinema.Label, color = Color.White.copy(0.70f),
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    val step = 2.5f
    StepperField(
        value = value, placeholder = placeholder, unit = "KG", keyboardType = KeyboardType.Decimal, accent = accent,
        onValueChange = { v -> actions.onSetWeightChanged(exercise, setIndex, v.filter { it.isDigit() || it == '.' || it == ',' }) },
        onNudge = { delta ->
            val base = value.replace(',', '.').toFloatOrNull() ?: defaultWeightKg
            val next = ((base + delta * step).coerceAtLeast(0f))
            val text = if (next % 1f == 0f) next.toInt().toString() else "%.1f".format(java.util.Locale.US, next)
            actions.onSetWeightChanged(exercise, setIndex, text)
        }
    )
    // Tekrar: kayıt/taslak yoksa planlanan değer ön dolu gösterilir (adım 1).
    val plannedReps = leadingInt(exercise.reps)
    val repsValue = data.setReps[setIndex] ?: ""
    Spacer(Modifier.height(10.dp))
    StepperField(
        value = repsValue, placeholder = plannedReps?.toString() ?: "0",
        unit = theme.t("TEKRAR", "REPS"), keyboardType = KeyboardType.Number, accent = accent,
        onValueChange = { v -> actions.onSetRepsChanged(exercise, setIndex, v.filter { it.isDigit() }.take(3)) },
        onNudge = { delta ->
            val base = repsValue.toIntOrNull() ?: plannedReps ?: 0
            actions.onSetRepsChanged(exercise, setIndex, (base + delta.toInt()).coerceIn(0, 999).toString())
        }
    )
    val last = data.lastSessionData[setIndex]
    if (last != null && (last.first != null || last.second != null)) {
        Spacer(Modifier.height(8.dp))
        val lastText = buildString {
            append(theme.t("Son: ", "Last: "))
            if (last.first != null) append("${"%.1f".format(last.first)}kg")
            if (last.first != null && last.second != null) append(" x ")
            if (last.second != null) append("${last.second}rep")
        }
        Text(
            lastText, color = Color.White.copy(0.70f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
    }
    Spacer(Modifier.height(20.dp))
    val label = if (done) theme.t("GERİ AL", "UNDO") else theme.t("TAMAMLA", "COMPLETE")
    SinemaGoButton(
        label = label, icon = Icons.Rounded.Check, dim = done,
        onClick = { actions.onToggleSet(exercise, setIndex); onDismiss() }
    )
}

/** ± düğmeli dev rakam alanı (kg ve tekrar için ortak). [onNudge] −1 / +1 çarpanı alır. */
@Composable
private fun StepperField(
    value: String,
    placeholder: String,
    unit: String,
    keyboardType: KeyboardType,
    accent: Color,
    onValueChange: (String) -> Unit,
    onNudge: (Float) -> Unit
) {
    val theme = LocalAppTheme.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        SinemaCapsuleIconButton(
            icon = Icons.Rounded.Remove, description = theme.t("Azalt", "Decrease") + " " + unit,
            onClick = { onNudge(-1f) }, selected = false, size = 48.dp
        )
        Spacer(Modifier.width(12.dp))
        BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // Alan genişliğe göre küçülür (rakam başına ~0.64em); yazı ölçeği ≤×1.15 (dev rakam kuralı).
            val actual = with(LocalDensity.current) { 64.sp.toDp().value } / 64f
            val chars = maxOf(value.length, placeholder.length, 3)
            val fit = maxWidth.value / (chars * 0.64f * minOf(actual, 1.15f))
            val sp = cappedDisplaySp(minOf(64f, fit).coerceAtLeast(26f), actual)
            val big = TextStyle(
                fontFamily = AppFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = sp.sp,
                fontFeatureSettings = "tnum", textAlign = TextAlign.Center, color = Color.White
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = big,
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) Text(placeholder, style = big.copy(color = Color.White.copy(0.35f)), maxLines = 1)
                        inner()
                    }
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        SinemaCapsuleIconButton(
            icon = Icons.Rounded.Add, description = theme.t("Artır", "Increase") + " " + unit,
            onClick = { onNudge(1f) }, selected = false, size = 48.dp
        )
    }
    Text(
        unit, color = Color.White.copy(0.70f), style = Sinema.Label,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Center
    )
}
