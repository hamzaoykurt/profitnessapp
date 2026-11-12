package com.cosmibit.profitness.presentation.workout

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Surface0
import com.cosmibit.profitness.core.theme.Surface3
import com.cosmibit.profitness.core.theme.TextPrimary
import com.cosmibit.profitness.core.theme.TextSecondary
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.insetTrackSurface
import com.cosmibit.profitness.core.theme.raisedPillSurface
import com.cosmibit.profitness.core.theme.supportSuccess
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import kotlinx.collections.immutable.ImmutableList

// "Sahne" yönü (.ekip/yon-ui-gorsel.md): görsel üstü renkler iki temada da aynıdır
// çünkü karartma katmanı her zaman vardır. Tek yerde tutulur.
private val SceneFlame = Color(0xFFF2C23F)
private val SceneInk = Surface0          // 0F0D0C
private val SceneSnow = TextPrimary      // F8F4EE
private val SceneMist = TextSecondary    // C4BBB1
private val SceneWell = Surface3         // 26211D

internal val SceneScrimStops = arrayOf(
    0f to SceneInk.copy(alpha = 0.50f),
    0.30f to SceneInk.copy(alpha = 0.10f),
    0.58f to SceneInk.copy(alpha = 0.80f),
    0.80f to SceneInk.copy(alpha = 0.92f),
    1f to SceneInk
)

/** Kartın etkinlik (süre/mesafe) tabanlı olup olmadığı — CinematicExerciseCard ile aynı kural. */
internal fun isActivityBasedExercise(e: Exercise): Boolean {
    val spec = activityTrackingSpec(
        category = e.category, name = e.name, target = e.target, reps = e.reps,
        sportTypeRaw = e.sportType, trackingModeRaw = e.trackingMode
    )
    val durationSet = isDurationSetExercise(
        category = e.category, name = e.name, target = e.target, reps = e.reps, sets = e.sets,
        sportTypeRaw = e.sportType, trackingModeRaw = e.trackingMode
    )
    return spec.metric != ExerciseMetric.Strength && !durationSet
}

/** "3 × 10" ya da etkinlik için "N dk hedef" — satır ve kahraman aynı metni kullanır. */
fun exercisePrescription(e: Exercise, activityBased: Boolean, theme: AppThemeState): String =
    if (activityBased) {
        val minutes = e.targetDurationSeconds?.takeIf { it > 0 }?.let { ((it + 59) / 60).toString() } ?: e.reps
        theme.t("$minutes dk hedef", "$minutes min target")
    } else {
        "${e.sets} × ${e.reps}"
    }

/**
 * Görsel + yedek. Yedek (görsel yok / yükleniyor / hata) aynı: koyu gradyan + ikon.
 * Görsel gelince 250ms crossfade (sistem animasyonları kapalıysa anında).
 */
@Composable
fun SceneImage(
    url: String,
    modifier: Modifier = Modifier,
    requestWidthPx: Int = 960,
    requestHeightPx: Int = 960,
    alignment: Alignment = Alignment.Center,
    grayscale: Boolean = false,
    fallbackIcon: ImageVector = Icons.Rounded.FitnessCenter,
    fallbackIconSize: Dp = 24.dp,
    fallbackIconBias: Float = 0f,
    fallbackGradient: Boolean = false
) {
    val context = LocalContext.current
    val animate = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (fallbackGradient) Brush.verticalGradient(listOf(SceneWell, SceneInk))
                    else Brush.verticalGradient(listOf(SceneWell, SceneWell))
                )
        ) {
            Icon(
                fallbackIcon, null,
                tint = SceneMist.copy(alpha = 0.25f),
                modifier = Modifier.align(BiasAlignment(0f, fallbackIconBias)).size(fallbackIconSize)
            )
        }
        if (url.isNotBlank()) {
            val request = remember(url, requestWidthPx, requestHeightPx, animate) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(requestWidthPx, requestHeightPx)
                    .crossfade(if (animate) 250 else 0)
                    .build()
            }
            val filter = remember(grayscale) {
                if (grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = alignment,
                colorFilter = filter
            )
        }
    }
}

/** Satır durum işareti: tamamlandı / sıradaki / bekleyen. */
@Composable
fun SceneStatusMark(completed: Boolean, isNext: Boolean, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val base = modifier.size(28.dp)
    when {
        completed -> Box(base.raisedPillSurface(theme, color = theme.supportSuccess), Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = SceneInk, modifier = Modifier.size(16.dp))
        }
        isNext -> Box(modifier = base.insetTrackSurface(theme).border(2.dp, accent, CircleShape)) {}
        else -> Box(modifier = base.insetTrackSurface(theme).border(1.5.dp, theme.text0.copy(alpha = 0.2f), CircleShape)) {}
    }
}

/** B. Gün şeridi: 48dp kapsül, seçili gün 40dp hap. */
@Composable
fun SceneDayStrip(
    days: ImmutableList<WorkoutDayState>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val haptic = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .insetTrackSurface(theme, CircleShape)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        days.forEachIndexed { idx, state ->
            val selected = idx == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(role = Role.Tab) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(idx)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .then(if (selected) Modifier.raisedPillSurface(theme) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedWorkoutDayLabel(state.day.day, theme),
                        color = if (selected) theme.effectiveOnAccentColor else theme.text1,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
