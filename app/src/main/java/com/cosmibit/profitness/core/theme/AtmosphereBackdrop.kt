package com.cosmibit.profitness.core.theme

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Orb periods in seconds. One linear clock runs for their product, so every orb is back at its
// start phase when the clock wraps (no visible jump).
private const val PERIOD_1 = 22
private const val PERIOD_2 = 31
private const val PERIOD_3 = 41
private const val CLOCK_SECONDS = PERIOD_1 * PERIOD_2 * PERIOD_3
private const val STATIC_CLOCK = 5f
private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * Shared page atmosphere: bg0 base + three large soft radial orbs (accent, warm-dark accent,
 * amber-cream) drifting on Lissajous paths, plus one static diagonal light streak.
 * Per frame: 3 radial-gradient draws (brushes cached, orbs moved with translate), one animation
 * driver read only in the draw phase. Frozen when the system animator scale is 0 or the app is
 * below STARTED. No blur, no saveLayer.
 */
@Composable
fun AtmosphereBackdrop(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val animate = remember(started) {
        started && runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f) > 0f
    }
    val clock: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "atmosphere").animateFloat(
            initialValue = 0f,
            targetValue = CLOCK_SECONDS.toFloat(),
            animationSpec = infiniteRepeatable(
                tween(CLOCK_SECONDS * 1000, easing = LinearEasing), RepeatMode.Restart
            ),
            label = "atmosphere_clock"
        )
    } else {
        remember { mutableStateOf(STATIC_CLOCK) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val dark = theme.isDark
                val w = size.width
                val h = size.height
                val amber = Color(0xFFF2C23F)
                val c1 = if (dark) accent else lerp(accent, Color.White, 0.20f)
                // c2: bright, airy accent derivative (sky-opening feel); amber orb adds warm glow
                val c2 = if (dark) lerp(accent, Color.White, 0.38f) else lerp(accent, amber, 0.45f)
                val a1 = if (dark) 0.32f else 0.18f
                val a2 = if (dark) 0.24f else 0.15f
                val a3 = if (dark) 0.16f else 0.14f
                val r1 = w * 1.05f
                val r2 = w * 0.85f
                val r3 = w * 0.75f
                fun orb(c: Color, a: Float, r: Float) = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to c.copy(a),
                        0.45f to c.copy(a * 0.42f),
                        1f to Color.Transparent
                    ),
                    center = Offset.Zero,
                    radius = r
                )
                val b1 = orb(c1, a1, r1)
                val b2 = orb(c2, a2, r2)
                val b3 = orb(amber, a3, r3)
                val streak = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.50f to Color.White.copy(0.045f),
                        0.60f to Color.Transparent,
                        1f to Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                val bg = theme.bg0

                fun drawOrb(
                    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
                    brush: Brush, radius: Float, t: Float, period: Int,
                    cx: Float, cy: Float, ax: Float, ay: Float, fy: Float, phase: Float
                ) {
                    val a = TWO_PI * (t % period) / period + phase
                    val x = w * (cx + ax * cos(a))
                    val y = h * (cy + ay * sin(a * fy))
                    scope.translate(x, y) { drawCircle(brush, radius, Offset.Zero) }
                }

                onDrawBehind {
                    val t = clock.value
                    drawRect(bg)
                    drawOrb(this, b1, r1, t, PERIOD_1, 0.74f, 0.20f, 0.42f, 0.22f, 2f, 0.0f)
                    drawOrb(this, b2, r2, t, PERIOD_2, 0.26f, 0.38f, 0.40f, 0.24f, 3f, 2.1f)
                    drawOrb(this, b3, r3, t, PERIOD_3, 0.55f, 0.70f, 0.45f, 0.26f, 2f, 4.2f)
                    drawRect(streak)
                }
            }
    )
}
