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
    val control = 10.dp
    val button = 12.dp
    val card = 16.dp
    val major = 20.dp
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
                elevation = 5.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.07f)
            ) else Modifier
        )
        .clip(shape)
        .background(color)
}

/** Opaque sculpted material. Continuous light across the face, never a glass strip. */
fun Modifier.performanceElevatedSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    elevated: Boolean = true
): Modifier = this
    .then(
        if (elevated) Modifier.shadow(
            if (theme.isDark) 6.dp else 5.dp, shape,
            ambientColor = Color.Black.copy(if (theme.isDark) 0.18f else 0.08f),
            spotColor = Color.Black.copy(if (theme.isDark) 0.26f else 0.12f)
        ) else Modifier
    )
    .clip(shape)
    .drawWithCache {
        val face = Brush.linearGradient(
            colorStops = if (theme.isDark) arrayOf(
                0f to Color(0xFF1D232C),
                0.34f to Color(0xFF151A21),
                0.72f to Color(0xFF0E1218),
                1f to Color(0xFF090C11)
            ) else arrayOf(
                0f to Color(0xFFFFFFFF),
                0.46f to Color(0xFFF8FAFB),
                0.78f to Color(0xFFEEF2F4),
                1f to Color(0xFFE2E7EA)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        val softKeyLight = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (theme.isDark) 0.035f else 0.42f),
                Color.Transparent
            ),
            center = Offset(-size.width * 0.12f, -size.height * 0.34f),
            radius = size.maxDimension * 1.15f
        )
        val lowerDepth = Brush.verticalGradient(
            colorStops = arrayOf(
                0.55f to Color.Transparent,
                1f to Color.Black.copy(if (theme.isDark) 0.22f else 0.055f)
            )
        )
        onDrawBehind {
            drawRect(face)
            drawRect(softKeyLight)
            drawRect(lowerDepth)
            drawRect(
                color = Color.White.copy(if (theme.isDark) 0.085f else 0.70f),
                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())
            )
        }
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(if (theme.isDark) 0.14f else 0.84f),
                theme.stroke.copy(if (theme.isDark) 0.58f else 0.46f),
                Color.Black.copy(if (theme.isDark) 0.30f else 0.05f)
            )
        ),
        shape = shape
    )

/**
 * Soft dimensional control inspired by moulded hardware: a translucent face,
 * diffuse key light and a shallow inward response while pressed.
 */
fun Modifier.performanceControlSurface(
    theme: AppThemeState,
    color: Color,
    pressed: Boolean,
    shape: Shape = RoundedCornerShape(PerformanceRadius.button)
): Modifier = this
    .shadow(
        elevation = if (pressed) 1.dp else 5.dp,
        shape = shape,
        ambientColor = Color.Black.copy(if (theme.isDark) 0.20f else 0.10f),
        spotColor = Color.Black.copy(if (theme.isDark) 0.34f else 0.16f)
    )
    .clip(shape)
    .background(color)
    .drawWithCache {
        val keyLight = Brush.linearGradient(
            colors = if (pressed) {
                listOf(Color.Black.copy(0.18f), Color.Transparent, Color.White.copy(0.05f))
            } else {
                listOf(Color.White.copy(if (theme.isDark) 0.16f else 0.52f), Color.Transparent, Color.Black.copy(0.14f))
            },
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        val innerDepth = Brush.verticalGradient(
            colors = if (pressed) {
                listOf(Color.Black.copy(0.22f), Color.Transparent, Color.White.copy(0.04f))
            } else {
                listOf(Color.White.copy(0.08f), Color.Transparent, Color.Black.copy(0.10f))
            }
        )
        onDrawWithContent {
            drawRect(keyLight)
            drawRect(innerDepth)
            drawContent()
        }
    }

/** Rare hero/selected material: one cached warm aura, never animated. */
fun Modifier.performanceSignatureSurface(
    theme: AppThemeState,
    accent: Color,
    shape: Shape = RoundedCornerShape(PerformanceRadius.major)
): Modifier = this
    .performanceElevatedSurface(theme, shape)
    .drawWithCache {
        val aura = Brush.radialGradient(
            colorStops = arrayOf(
                0f to accent.copy(if (theme.isDark) 0.16f else 0.075f),
                0.34f to accent.copy(if (theme.isDark) 0.045f else 0.018f),
                1f to Color.Transparent
            ),
            center = Offset(size.width * 1.18f, -size.height * 0.22f),
            radius = size.maxDimension * 1.28f
        )
        onDrawBehind {
            drawRect(aura)
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
