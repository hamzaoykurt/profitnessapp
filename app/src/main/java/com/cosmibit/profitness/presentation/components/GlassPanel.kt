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
    val shadowSpot = if (hasGlow) accentColor.copy(alpha = 0.22f * resolvedGlowStrength)
                     else if (theme.isDark) Color.Black.copy(0.76f) else accentColor.copy(0.14f)
    val shadowAmbient = if (theme.isDark) Color.Black.copy(0.38f) else Color(0xFF526176).copy(0.13f)
    val rimAlpha = if (hasGlow) 0.18f + (0.16f * resolvedGlowStrength) else 0.28f
    val washAlpha = if (hasGlow) 0.025f + (0.045f * resolvedGlowStrength) else 0.035f

    Box(
        modifier = modifier
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
                        0.00f to if (theme.isDark) theme.bg3.copy(0.92f) else Color.White,
                        0.42f to theme.bg2,
                        1.00f to if (theme.isDark) theme.bg1.copy(0.98f) else theme.bg2.copy(0.96f)
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
                onDrawBehind {
                    drawRect(surface)
                    drawRect(innerDepth)
                    // 1dp rim light — top edge
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accentColor.copy(rimAlpha),
                                Color.Transparent
                            )
                        ),
                        size = Size(size.width, 1.dp.toPx())
                    )
                    // Corner accent wash (very subtle)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accentColor.copy(washAlpha), Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = size.width * 0.6f
                        )
                    )
                    drawRoundRect(
                        brush = border,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (shape as? RoundedCornerShape)?.topStart?.toPx(size, this) ?: 20.dp.toPx()
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
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
 * glassCard — Frosted-glass Modifier extension.
 * Semi-transparent layered background (shimmer + accent bleed + depth shadow)
 * with an accent-tinted border — matches the Bottom Navigation Bar's visual language.
 */
fun Modifier.glassCard(
    accent: Color,
    theme : AppThemeState,
    shape : Shape = RoundedCornerShape(20.dp)
): Modifier = composed {
    val borderBrush = remember(accent, theme.isDark, theme.stroke) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (theme.isDark) 0.28f else 0.35f),
                theme.stroke.copy(alpha = if (theme.isDark) 0.45f else 0.70f),
                accent.copy(alpha = if (theme.isDark) 0.16f else 0.20f)
            )
        )
    }

    Modifier
        .clip(shape)
        .drawWithCache {
            val base = theme.bg1.copy(alpha = if (theme.isDark) 0.75f else 0.90f)
            // In light mode the stronger white rim creates a polished glass edge.
            val shimmerAlphaTop = if (theme.isDark) 0.09f else 0.50f
            val shimmerAlphaMid = if (theme.isDark) 0.02f else 0.15f
            val shimmer = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = shimmerAlphaTop),
                    0.32f to Color.White.copy(alpha = shimmerAlphaMid),
                    0.55f to Color.Transparent
                )
            )
            val accentBleed = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to accent.copy(alpha = if (theme.isDark) 0.14f else 0.08f),
                    0.45f to accent.copy(alpha = if (theme.isDark) 0.05f else 0.03f),
                    1.00f to Color.Transparent
                ),
                start = Offset(0f, size.height * 0.5f),
                end   = Offset(size.width, size.height * 0.5f)
            )
            // Light mode uses a cool slate depth tint instead of muddy black.
            val depthColor = if (theme.isDark) Color.Black.copy(alpha = 0.30f)
                             else Color(0xFF526176).copy(alpha = 0.09f)
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

