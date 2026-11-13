package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.Amber
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.exerciseCompactDisplayName
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.presentation.workout.RestTimerState
import com.cosmibit.profitness.presentation.workout.TimerPurpose

/** Kronometre saati: 0:05, 3:33, 1:02:09 (saat varsa). Negatif değer 0 sayılır. */
internal fun formatTimerClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/** Yüzen kronometre görünür mü (çalışıyor, duraklatılmış ya da kısa "bitti" geri bildirimi). */
internal val RestTimerState.isFloatingVisible: Boolean get() = isRunning || isPaused || isDone

/**
 * Tüm ana sayfalarda alt menünün hemen üstünde duran küçük cam kronometre kapsülü.
 * Süre (vurgu, tabular Sora 800) + hareket adı (tek satır) + duraklat/devam + durdur.
 * Kapsüle dokununca [onOpen] (Antrenman'daki ilgili hareket). Hareket azaltmada animasyonsuz açılır/kapanır.
 * `light`: düz açık zeminli sayfalarda açık cam (koyu yazı).
 */
@Composable
fun FloatingWorkoutTimer(
    timer: RestTimerState,
    light: Boolean,
    onOpen: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = rememberSinemaMotionOn()
    val visible = timer.isFloatingVisible
    // Kapanış animasyonu sırasında durum sıfırlansa da son içerik görünsün.
    var shown by remember { mutableStateOf(timer) }
    if (visible) shown = timer

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (motion) {
            fadeIn(tween(180)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 } +
                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.92f)
        } else EnterTransition.None,
        exit = if (motion) {
            fadeOut(tween(140)) + slideOutVertically(tween(160)) { it / 2 } + scaleOut(tween(160), targetScale = 0.94f)
        } else ExitTransition.None
    ) {
        TimerCapsule(shown, light, onOpen, onTogglePause, onStop)
    }
}

@Composable
private fun TimerCapsule(
    timer: RestTimerState,
    light: Boolean,
    onOpen: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    // Açık camda vurgu biraz koyulaşır (beyaz zeminde kontrast).
    val timeColor = when {
        timer.isPaused -> Amber
        light -> lerp(accent, Color.Black, 0.30f)
        else -> accent
    }
    val clock = formatTimerClock(timer.displaySeconds)
    val name = theme.exerciseCompactDisplayName(timer.exerciseName).ifBlank { theme.t("Antrenman", "Workout") }
    val label = when {
        timer.isDone && timer.purpose == TimerPurpose.Rest -> theme.t("Hazırsın!", "Ready!")
        timer.isDone -> theme.t("Kaydedildi · $name", "Saved · $name")
        timer.isPaused -> theme.t("Duraklatıldı · $name", "Paused · $name")
        timer.purpose == TimerPurpose.TimedSet -> "$name · Set ${timer.setIndex + 1}"
        else -> name
    }
    val a11y = theme.t("Kronometre $clock, $label. Antrenmana git", "Timer $clock, $label. Go to workout")

    SinemaCapsule(
        modifier = Modifier
            .widthIn(min = 180.dp, max = 340.dp)
            .semantics { contentDescription = a11y },
        onClick = onOpen,
        height = 48.dp,
        light = light,
        contentPadding = PaddingValues(start = 16.dp, end = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) { fg ->
        if (timer.isDone) {
            Icon(Icons.Rounded.CheckCircle, null, tint = timeColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            clock,
            style = Sinema.Label.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
            color = timeColor, maxLines = 1
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label, color = fg.copy(alpha = 0.86f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(4.dp))
        if (!timer.isDone) {
            SinemaCapsuleIconButton(
                icon = if (timer.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                description = if (timer.isPaused) theme.t("Devam et", "Resume") else theme.t("Duraklat", "Pause"),
                onClick = onTogglePause, selected = false, size = 34.dp, light = light
            )
            SinemaCapsuleIconButton(
                icon = Icons.Rounded.Stop,
                description = theme.t("Durdur ve kaydet", "Stop and save"),
                onClick = onStop, selected = false, size = 34.dp, light = light
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}
