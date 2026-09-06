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
    shape: Shape = RoundedCornerShape(16.dp),
    glowColor: Color = Color.Transparent,
    elevation: Dp = 8.dp,
    glowStrength: Float = 1f,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .performanceElevatedSurface(theme, shape = shape, elevated = elevation > 0.dp)
            .then(
                if (glowColor != Color.Transparent && glowStrength > 0f) {
                    Modifier.drawWithCache {
                        val reflectedAccent = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to glowColor.copy(
                                    alpha = (if (theme.isDark) 0.12f else 0.055f) * glowStrength.coerceIn(0f, 1f)
                                ),
                                0.38f to glowColor.copy(
                                    alpha = (if (theme.isDark) 0.028f else 0.012f) * glowStrength.coerceIn(0f, 1f)
                                ),
                                1f to Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width * 1.16f, -size.height * 0.28f),
                            radius = size.maxDimension * 1.25f
                        )
                        onDrawWithContent {
                            drawRect(reflectedAccent)
                            drawContent()
                        }
                    }
                } else Modifier
            )
    ) { content() }
}

@Composable
fun ForgeCardPro(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = Lime,
    content: @Composable () -> Unit
) = ForgeCard(modifier, shape, accentColor, 9.dp, 0.8f, content)

@Composable
fun ForgeCardSmall(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) = ForgeCard(modifier, RoundedCornerShape(12.dp), glowColor, 6.dp, 0.55f, content)

/** Opaque content surface. Accent is accepted for API compatibility, not decoration. */
fun Modifier.premiumSolidSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(16.dp),
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
    shape: Shape = RoundedCornerShape(16.dp)
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
        .background(if (theme.isDark) Color(0xFF15181D) else Color(0xFFFBFCFC))
}

/** Recessed well for inputs and filters; depth points into the screen. */
fun Modifier.insetControlSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier = composed {
    this
        .clip(shape)
        .background(if (theme.isDark) Color(0xFF080A0C) else Color(0xFFE5EAED))
        .drawWithCache {
            val depth = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (theme.isDark) 0.34f else 0.11f),
                    Color.Transparent,
                    Color.White.copy(alpha = if (theme.isDark) 0.035f else 0.34f)
                )
            )
            val reflectedLight = Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (theme.isDark) 0.14f else 0.04f),
                    Color.Transparent,
                    Color.White.copy(alpha = if (theme.isDark) 0.035f else 0.24f),
                    Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
            )
            onDrawWithContent {
                drawRect(depth)
                drawRect(reflectedLight)
                drawContent()
            }
        }
}
