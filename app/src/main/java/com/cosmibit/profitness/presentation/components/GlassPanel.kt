package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.*

// ╔══════════════════════════════════════════════════════════════════╗
// ║          NEON FORGE — Card System                               ║
// ║  Surface hierarchy + Lime rim light + ambient occlusion         ║
// ╚══════════════════════════════════════════════════════════════════╝

/**
 * ForgeCard — Primary surface card.
 * Layered glass surface, restrained accent bloom, inner rim and soft AO shadow.
 */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    glowColor: Color = Color.Transparent,
    elevation: Dp = 16.dp,
    glowStrength: Float = 1f,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val hasGlow = glowColor != Color.Transparent && glowStrength > 0f
    val resolvedGlowStrength = glowStrength.coerceIn(0f, 1f)
    val accentColor = if (hasGlow) glowColor else MaterialTheme.colorScheme.primary
    val shadowSpot = if (hasGlow) accentColor.copy(
                         alpha = (if (theme.isDark) 0.34f else 0.10f) * resolvedGlowStrength
                     )
                     else if (theme.isDark) Color.Black.copy(0.76f) else accentColor.copy(0.14f)
    val shadowAmbient = if (theme.isDark) Color.Black.copy(0.62f) else Color(0xFF574D68).copy(0.16f)
    val rimAlpha = if (hasGlow) {
        (if (theme.isDark) 0.24f else 0.10f) + (0.20f * resolvedGlowStrength)
    } else 0.28f
    val washAlpha = if (hasGlow) 0.025f + (0.045f * resolvedGlowStrength) else 0.035f

    Box(
        modifier = modifier
            .then(
                if (hasGlow) {
                    Modifier.shadow(
                        elevation = elevation + (6.dp * resolvedGlowStrength),
                        shape = shape,
                        spotColor = accentColor.copy(
                            alpha = (if (theme.isDark) 0.38f else 0.08f) * resolvedGlowStrength
                        ),
                        ambientColor = Color.Transparent
                    )
                } else Modifier
            )
            .shadow(
                elevation = elevation + 8.dp,
                shape = shape,
                spotColor = if (theme.isDark) Color.Black.copy(0.72f) else Color(0xFF6B5F79).copy(0.10f),
                ambientColor = Color.Transparent
            )
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = shadowSpot,
                ambientColor = shadowAmbient
            )
            .clip(shape)
            .drawWithCache {
                val surface = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to if (theme.isDark) Color(0xFF25262E) else Color(0xFFFFFFFF),
                        0.44f to if (theme.isDark) Color(0xFF17181E) else Color(0xFFFFFDFF),
                        1.00f to if (theme.isDark) Color(0xFF0D0E12) else Color(0xFFF3F0F6)
                    )
                )
                val innerDepth = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.58f to Color.Transparent,
                        1.00f to if (theme.isDark) Color.Black.copy(0.22f) else Color(0xFF526176).copy(0.07f)
                    )
                )
                val border = Brush.linearGradient(
                    listOf(
                        Color.White.copy(if (theme.isDark) 0.14f else 0.88f),
                        accentColor.copy(if (hasGlow) 0.22f * resolvedGlowStrength else 0.08f),
                        theme.stroke.copy(if (theme.isDark) 0.66f else 0.82f)
                    )
                )
                val glassSheen = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(if (theme.isDark) 0.09f else 0.34f),
                        0.16f to Color.White.copy(if (theme.isDark) 0.025f else 0.10f),
                        0.38f to Color.Transparent
                    )
                )
                onDrawWithContent {
                    drawRect(surface)
                    drawRect(innerDepth)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accentColor.copy(washAlpha), Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = size.width * 0.6f
                        )
                    )
                    drawContent()

                    // Polished glass layers must sit above image content to remain visible.
                    drawRect(glassSheen)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.10f * resolvedGlowStrength),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.90f, size.height * 0.06f),
                            radius = size.width * 0.42f
                        )
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(if (theme.isDark) 0.20f else 0.72f),
                                accentColor.copy(rimAlpha),
                                Color.Transparent
                            )
                        ),
                        size = Size(size.width, 1.25.dp.toPx())
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(if (theme.isDark) 0.34f else 0.07f))
                        ),
                        topLeft = Offset(0f, size.height * 0.82f),
                        size = Size(size.width, size.height * 0.18f)
                    )
                    drawRoundRect(
                        brush = border,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (shape as? RoundedCornerShape)?.topStart?.toPx(size, this) ?: 20.dp.toPx()
                        ),
                        style = Stroke(width = 1.25.dp.toPx())
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun ForgeCardPro(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentColor: Color = Lime,
    content: @Composable () -> Unit
) = ForgeCard(
    modifier = modifier,
    shape = shape,
    glowColor = accentColor,
    elevation = 20.dp,
    content = content
)

@Composable
fun ForgeCardSmall(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) = ForgeCard(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    glowColor = glowColor,
    elevation = 10.dp,
    content = content
)

/**
 * Historical API name retained for callers. This is now the default premium
 * information surface: mostly solid and architectural. Actual translucent glass
 * remains reserved for navigation, media overlays and floating input chrome.
 */
fun Modifier.premiumSolidSurface(
    accent: Color,
    theme : AppThemeState,
    shape : Shape = RoundedCornerShape(20.dp),
    elevation: Dp = if (theme.isDark) 10.dp else 8.dp
): Modifier = composed {
    val borderBrush = remember(accent, theme.isDark, theme.stroke) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (theme.isDark) 0.19f else 0.98f),
                theme.stroke.copy(alpha = if (theme.isDark) 0.78f else 0.72f),
                accent.copy(alpha = if (theme.isDark) 0.11f else 0.045f)
            )
        )
    }

    Modifier
        .shadow(
            elevation = elevation + 8.dp,
            shape = shape,
            spotColor = if (theme.isDark) Color.Black.copy(0.72f)
                        else Color(0xFF675A76).copy(0.10f),
            ambientColor = Color.Transparent
        )
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = if (theme.isDark) Color.Black.copy(0.72f)
                        else Color(0xFF665A73).copy(0.13f),
            ambientColor = if (theme.isDark) Color.Black.copy(0.42f)
                           else Color(0xFF8B7B98).copy(0.07f)
        )
        .clip(shape)
        .drawWithCache {
            val base = Brush.verticalGradient(
                colorStops = if (theme.isDark) arrayOf(
                    0.00f to Color(0xFF24252D),
                    0.36f to Color(0xFF1A1B21),
                    1.00f to Color(0xFF111216)
                ) else arrayOf(
                    0.00f to Color(0xFFFFFFFF),
                    0.55f to Color(0xFFFFFDFF),
                    1.00f to Color(0xFFF4F1F7)
                )
            )
            val shimmerAlphaTop = if (theme.isDark) 0.105f else 0.78f
            val shimmerAlphaMid = if (theme.isDark) 0.012f else 0.10f
            val shimmer = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = shimmerAlphaTop),
                    0.32f to Color.White.copy(alpha = shimmerAlphaMid),
                    0.55f to Color.Transparent
                )
            )
            val accentBleed = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to accent.copy(alpha = if (theme.isDark) 0.055f else 0.018f),
                    0.45f to accent.copy(alpha = if (theme.isDark) 0.018f else 0.006f),
                    1.00f to Color.Transparent
                ),
                start = Offset(0f, size.height * 0.5f),
                end   = Offset(size.width, size.height * 0.5f)
            )
            // Light mode uses a cool slate depth tint instead of muddy black.
            val depthColor = if (theme.isDark) Color.Black.copy(alpha = 0.24f)
                             else Color(0xFF526176).copy(alpha = 0.045f)
            val depth = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.48f to Color.Transparent,
                    1.00f to depthColor
                )
            )
            onDrawBehind {
                drawRect(base)
                drawRect(accentBleed)
                drawRect(depth)
                drawRect(shimmer)
            }
        }
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Compatibility alias for older call sites. Despite its historical name this
 * deliberately resolves to the solid architectural surface above.
 */
fun Modifier.glassCard(
    accent: Color,
    theme : AppThemeState,
    shape : Shape = RoundedCornerShape(20.dp)
): Modifier = premiumSolidSurface(accent, theme, shape)

/**
 * True translucent chrome, reserved for floating navigation, media controls
 * and input docks. Keeping this separate prevents every content card from
 * looking like the same sheet of glass.
 */
fun Modifier.floatingGlassSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 18.dp
): Modifier = composed {
    Modifier
        .shadow(
            elevation = elevation + 10.dp,
            shape = shape,
            spotColor = Color.Black.copy(if (theme.isDark) 0.74f else 0.10f),
            ambientColor = Color.Transparent
        )
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = accent.copy(alpha = if (theme.isDark) 0.24f else 0.08f),
            ambientColor = if (theme.isDark) Color.Black.copy(0.56f)
                           else Color(0xFF445064).copy(0.12f)
        )
        .clip(shape)
        .drawWithCache {
            val base = Brush.verticalGradient(
                listOf(
                    if (theme.isDark) Color(0xFF292A32).copy(0.90f) else Color.White.copy(0.88f),
                    if (theme.isDark) Color(0xFF111217).copy(0.88f) else Color(0xFFF5F1F8).copy(0.80f)
                )
            )
            val glint = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (theme.isDark) 0.18f else 0.92f),
                    Color.Transparent,
                    accent.copy(alpha = if (theme.isDark) 0.08f else 0.025f)
                )
            )
            onDrawBehind {
                drawRect(base)
                drawRect(glint)
            }
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(if (theme.isDark) 0.16f else 0.92f),
                    accent.copy(if (theme.isDark) 0.22f else 0.08f),
                    theme.stroke.copy(0.74f)
                )
            ),
            shape
        )
}

/**
 * Recessed control well for search, filters and compact form fields. It uses
 * an inner top shadow and lower rim instead of an outer card shadow.
 */
fun Modifier.insetControlSurface(
    accent: Color,
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(14.dp)
): Modifier = composed {
    Modifier
        .clip(shape)
        .drawWithCache {
            val base = if (theme.isDark) theme.bg0 else theme.bg3.copy(alpha = 0.72f)
            val innerShadow = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to if (theme.isDark) Color.Black.copy(0.64f) else Color(0xFF526176).copy(0.13f),
                    0.18f to if (theme.isDark) Color.Black.copy(0.18f) else Color(0xFF526176).copy(0.035f),
                    0.52f to Color.Transparent
                )
            )
            val lowerRim = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.62f to Color.Transparent,
                    1.00f to Color.White.copy(if (theme.isDark) 0.055f else 0.46f)
                )
            )
            onDrawBehind {
                drawRect(base)
                drawRect(innerShadow)
                drawRect(lowerRim)
            }
        }
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    theme.stroke.copy(if (theme.isDark) 0.90f else 0.74f),
                    accent.copy(if (theme.isDark) 0.10f else 0.045f),
                    Color.White.copy(if (theme.isDark) 0.07f else 0.70f)
                )
            ),
            shape
        )
}

