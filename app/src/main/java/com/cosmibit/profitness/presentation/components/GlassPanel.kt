package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.*

/**
 * Shared premium card material. One modest hardware shadow and one cached paint
 * pass replace the former stack of overlapping shadows, glows and sheens.
 */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SinemaRadius.R20),
    glowColor: Color = Color.Transparent,
    elevation: Dp = 8.dp,
    glowStrength: Float = 1f,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .performanceElevatedSurface(
                theme, shape = shape, elevated = elevation > 0.dp,
                accent = if (glowColor == Color.Transparent) Color.Unspecified else glowColor,
                glowStrength = glowStrength
            )
    ) { content() }
}

@Composable
fun ForgeCardPro(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SinemaRadius.R24),
    accentColor: Color = Lime,
    content: @Composable () -> Unit
) = ForgeCard(modifier, shape, accentColor, 9.dp, 0.8f, content)

@Composable
fun ForgeCardSmall(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) = ForgeCard(modifier, RoundedCornerShape(SinemaRadius.R16), glowColor, 6.dp, 0.55f, content)

/** Opaque content surface. Accent is accepted for API compatibility, not decoration. */
fun Modifier.premiumSolidSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(SinemaRadius.R20),
    elevation: Dp = 8.dp
): Modifier = composed {
    this.performanceElevatedSurface(theme, shape = shape, elevated = elevation > 0.dp, accent = accent)
}

/** Compatibility alias; content cards intentionally remain readable and opaque. */
fun Modifier.glassCard(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(SinemaRadius.R20)
): Modifier = premiumSolidSurface(accent, theme, shape)

/** Quiet, opaque floating chrome. No backdrop blur or content bleeding through. */
fun Modifier.floatingGlassSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(SinemaRadius.R16),
    elevation: Dp = 10.dp
): Modifier = composed {
    this
        .shadow(
            elevation = elevation.coerceAtMost(12.dp),
            shape = shape,
            spotColor = Color.Black.copy(0.46f),
            ambientColor = Color.Transparent
        )
        .clip(shape)
        .background(Sinema.Surface)
        .border(1.dp, Sinema.Line, shape)
}

/** Recessed well for inputs and filters; depth points into the screen. */
fun Modifier.insetControlSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(SinemaRadius.R12)
): Modifier = composed {
    this.performanceInsetSurface(theme, shape)
}
