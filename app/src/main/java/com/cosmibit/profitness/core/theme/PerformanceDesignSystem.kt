package com.cosmibit.profitness.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Shared 8pt-based product tokens for the Performance Luxury UI. */
@Immutable
object PerformanceSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val huge = 48.dp
    val display = 64.dp
}

@Immutable
object PerformanceRadius {
    val control = 14.dp
    val button = 16.dp
    val card = 20.dp
    val major = 24.dp
}

object PerformanceMotion {
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val fast = tween<Float>(120, easing = standardEasing)
    val standard = tween<Float>(240, easing = standardEasing)
}

/** Opaque content surface. Borders are intentionally omitted; hierarchy is tonal. */
fun Modifier.performanceSurface(
    theme: AppThemeState,
    level: Int = 1,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    elevated: Boolean = false
): Modifier {
    val color = when (level.coerceIn(1, 3)) {
        1 -> theme.bg1
        2 -> theme.bg2
        else -> theme.bg3
    }
    return this
        .then(
            if (elevated && !theme.isDark) Modifier.shadow(
                elevation = 3.dp,
                shape = shape,
                ambientColor = WarmShadow.copy(alpha = 0.10f),
                spotColor = WarmShadow.copy(alpha = 0.10f)
            ) else Modifier
        )
        .clip(shape)
        .background(color)
}

private val WarmShadow = Color(0xFF3A2A1A)

/** Flat opaque card: solid fill, one soft shadow, at most one hairline (dark only). */
fun Modifier.performanceElevatedSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    elevated: Boolean = true
): Modifier = this
    .then(
        if (elevated) Modifier.shadow(
            if (theme.isDark) 4.dp else 3.dp, shape,
            ambientColor = if (theme.isDark) Color.Black.copy(0.30f) else WarmShadow.copy(0.10f),
            spotColor = if (theme.isDark) Color.Black.copy(0.30f) else WarmShadow.copy(0.10f)
        ) else Modifier
    )
    .clip(shape)
    .background(if (theme.isDark) theme.bg2 else theme.bg1)
    .then(
        if (theme.isDark) Modifier.border(1.dp, theme.stroke.copy(alpha = 0.5f), shape) else Modifier
    )

/** Flat control: solid fill, soft shadow (0dp while pressed). */
fun Modifier.performanceControlSurface(
    theme: AppThemeState,
    color: Color,
    pressed: Boolean,
    shape: Shape = RoundedCornerShape(PerformanceRadius.button)
): Modifier = this
    .shadow(
        elevation = if (pressed) 0.dp else 2.dp,
        shape = shape,
        ambientColor = if (theme.isDark) Color.Black.copy(0.30f) else WarmShadow.copy(0.10f),
        spotColor = if (theme.isDark) Color.Black.copy(0.30f) else WarmShadow.copy(0.10f)
    )
    .clip(shape)
    .background(color)

/** Rare hero/selected material: one cached warm aura, never animated. */
fun Modifier.performanceSignatureSurface(
    theme: AppThemeState,
    accent: Color,
    shape: Shape = RoundedCornerShape(PerformanceRadius.major)
): Modifier = this
    .performanceElevatedSurface(theme, shape)
    .drawWithCache {
        onDrawBehind {
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
