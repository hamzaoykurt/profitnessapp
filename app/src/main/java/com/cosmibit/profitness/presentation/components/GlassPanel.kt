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
    shape: Shape = RoundedCornerShape(20.dp),
    glowColor: Color = Color.Transparent,
    elevation: Dp = 8.dp,
    glowStrength: Float = 1f,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .performanceElevatedSurface(theme, shape = shape, elevated = elevation > 0.dp)
    ) { content() } // glowColor/glowStrength: API uyumu için kabul edilir, düz yüzeyde çizilmez
}

@Composable
fun ForgeCardPro(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentColor: Color = Lime,
    content: @Composable () -> Unit
) = ForgeCard(modifier, shape, accentColor, 9.dp, 0.8f, content)

@Composable
fun ForgeCardSmall(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) = ForgeCard(modifier, RoundedCornerShape(16.dp), glowColor, 6.dp, 0.55f, content)

/** Opaque content surface. Accent is accepted for API compatibility, not decoration. */
fun Modifier.premiumSolidSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = if (theme.isDark) 8.dp else 5.dp
): Modifier = composed {
    // Ordinary content cards stay neutral. Accent reflections are reserved for
    // selected/hero surfaces so a page never becomes a patchwork of colour.
    this.performanceElevatedSurface(theme, shape = shape, elevated = elevation > 0.dp)
}

/** Compatibility alias; content cards intentionally remain readable and opaque. */
fun Modifier.glassCard(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier = premiumSolidSurface(accent, theme, shape)

/** Quiet, opaque floating chrome. No backdrop blur or content bleeding through. */
fun Modifier.floatingGlassSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 10.dp
): Modifier = composed {
    this
        .shadow(
            elevation = elevation.coerceAtMost(12.dp),
            shape = shape,
            spotColor = Color.Black.copy(if (theme.isDark) 0.46f else 0.10f),
            ambientColor = Color.Transparent
        )
        .clip(shape)
        .background(if (theme.isDark) Color(0xFF221D19) else Color(0xFFFFFDF9))
}

/** Recessed well for inputs and filters; depth points into the screen. */
fun Modifier.insetControlSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier = composed {
    this
        .clip(shape)
        .background(if (theme.isDark) Color(0xFF120F0D) else Color(0xFFEEE7DD))
}
