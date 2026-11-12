package com.cosmibit.profitness.presentation.workout

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.CardGreen
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Surface0
import com.cosmibit.profitness.core.theme.Surface3
import com.cosmibit.profitness.core.theme.TextPrimary
import com.cosmibit.profitness.core.theme.TextSecondary
import com.cosmibit.profitness.core.theme.bg2
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.glassChipSurface
import com.cosmibit.profitness.core.theme.insetTrackSurface
import com.cosmibit.profitness.core.theme.raisedButtonSurface
import com.cosmibit.profitness.core.theme.raisedPillSurface
import com.cosmibit.profitness.core.theme.exerciseCompactDisplayName
import com.cosmibit.profitness.core.theme.supportSuccess
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.cos
import kotlin.math.sin

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

/** A. Kahraman alan. */
@Composable
fun WorkoutHero(
    height: Dp,
    programTitle: String,
    exercise: Exercise?,
    isRestDay: Boolean,
    streak: Int,
    progress: Float,
    allDone: Boolean,
    topExtra: Dp,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = theme.effectiveAccentColor
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chipTop = maxOf(32.dp, statusTop + 8.dp) + topExtra
    val shape = if (theme.isDark) RoundedCornerShape(0.dp)
    else RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

    val fade = if (theme.isDark) 32.dp else 0.dp
    Box(modifier.fillMaxWidth().height(height).clip(shape)) {
        if (isRestDay || exercise == null) {
            SceneImage(
                url = "", modifier = Modifier.fillMaxSize().padding(bottom = fade),
                fallbackIcon = if (isRestDay) Icons.Rounded.Hotel else Icons.Rounded.FitnessCenter,
                fallbackIconSize = if (isRestDay) 56.dp else 64.dp,
                fallbackIconBias = -0.36f, fallbackGradient = true
            )
        } else {
            SceneImage(
                url = exercise.image, modifier = Modifier.fillMaxSize().padding(bottom = fade),
                requestWidthPx = 960, requestHeightPx = 960,
                alignment = Alignment.TopCenter,
                fallbackIconSize = 64.dp, fallbackIconBias = -0.36f, fallbackGradient = true
            )
        }
        // Karartma: metin yalnız 0.58f altında durur.
        Box(Modifier.fillMaxSize().padding(bottom = fade).background(Brush.verticalGradient(colorStops = SceneScrimStops)))
        if (fade > 0.dp) Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(fade)
                .background(Brush.verticalGradient(listOf(SceneInk, Color.Transparent)))
        )

        // Üst satır: seri çipi (sol) + ilerleme halkası (sağ)
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = chipTop - 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val streakLabel = if (streak > 0) theme.t("$streak günlük serin aktif", "$streak-day streak")
            else theme.t("Serini başlat", "Start your streak")
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .height(32.dp)
                    .glassChipSurface()
                    .semantics(mergeDescendants = true) { contentDescription = streakLabel }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment, null,
                    tint = if (streak > 0) SceneFlame else SceneFlame.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    streak.coerceAtLeast(0).toString(),
                    color = SceneFlame, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isRestDay && exercise != null) {
                val pct = (progress * 100).toInt()
                val arc = if (allDone) CardGreen else accent
                Box(
                    Modifier
                        .size(44.dp)
                        .glassChipSurface()
                        .semantics(mergeDescendants = true) { contentDescription = "$pct%" },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 3.dp.toPx()
                        val r = (size.minDimension - 2 * 3.dp.toPx() - sw) / 2f
                        val c = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(SceneSnow.copy(alpha = 0.15f), radius = r, center = c, style = Stroke(sw))
                        if (progress > 0f) {
                            val sweep = progress.coerceIn(0f, 1f) * 360f
                            val a = Math.toRadians((-90f + sweep).toDouble())
                            val tip = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                            val gr = 28.dp.toPx()
                            drawCircle(
                                Brush.radialGradient(listOf(arc.copy(alpha = 0.30f), Color.Transparent), tip, gr),
                                radius = gr, center = tip
                            )
                            drawArc(
                                color = arc, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                                topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(2 * r, 2 * r),
                                style = Stroke(sw, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        "$pct%", color = SceneSnow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        // Alt blok
        Column(
            Modifier.align(Alignment.BottomStart).padding(
                start = 16.dp,
                end = if (isRestDay) 16.dp else 16.dp + 68.dp + 8.dp,
                bottom = 24.dp
            )
        ) {
            if (isRestDay) {
                Text(
                    strings.restDayTitle, color = SceneSnow, fontSize = 24.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.restDaySubtitle, color = SceneMist, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    programTitle, color = SceneMist, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (exercise != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        theme.exerciseCompactDisplayName(exercise.name),
                        color = SceneSnow, fontSize = 24.sp, lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    val label = exercisePrescription(exercise, isActivityBasedExercise(exercise), theme)
                    val parts = label.split(" × ")
                    Text(
                        text = buildAnnotatedString {
                            if (parts.size == 2) {
                                append(parts[0])
                                withStyle(SpanStyle(color = SceneMist)) { append(" × ") }
                                append(parts[1])
                            } else append(label)
                        },
                        color = SceneSnow, fontSize = 44.sp, lineHeight = 48.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        // Başla / oynat düğmesi
        if (!isRestDay && exercise != null) {
            val playLabel = theme.t("Hareketi gör", "Watch exercise")
            val btn = if (allDone) CardGreen else accent
            val btnSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val btnPressed by btnSource.collectIsPressedAsState()
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp + fade)
                    .size(68.dp)
                    .drawBehind {
                        if (!allDone) {
                            val r = 64.dp.toPx()
                            val c = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
                            drawCircle(
                                Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent), c, r),
                                radius = r, center = c
                            )
                        }
                    }
                    .raisedButtonSurface(theme, pressed = btnPressed, color = btn)
                    .semantics { contentDescription = playLabel }
                    .clickable(btnSource, null, role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (allDone) Icons.Rounded.Check else Icons.Rounded.PlayArrow, null,
                    tint = SceneInk, modifier = Modifier.size(28.dp)
                )
            }
        }
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
