package com.avonix.profitness.presentation.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.avonix.profitness.core.theme.*

// ╔══════════════════════════════════════════════════════════════════╗
// ║          NEON FORGE — Card System                               ║
// ║  Surface hierarchy + Lime rim light + ambient occlusion         ║
// ╚══════════════════════════════════════════════════════════════════╝

/**
 * ForgeCard — Primary surface card.
 * Surface2 background, lime-tinted 1dp top rim line, deep AO shadow.
 */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    glowColor: Color = Color.Transparent,
    elevation: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val accentColor = if (glowColor != Color.Transparent) glowColor else MaterialTheme.colorScheme.primary
    val shadowSpot = if (glowColor != Color.Transparent) accentColor.copy(0.35f)
                     else if (theme.isDark) Color.Black.copy(0.8f) else accentColor.copy(0.20f)
    val shadowAmbient = if (theme.isDark) Color.Black.copy(0.4f) else Color.Black.copy(0.10f)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = shadowSpot,
                ambientColor = shadowAmbient
            )
            .clip(shape)
            .background(theme.bg2)
            .drawWithCache {
                onDrawBehind {
                    // 1dp rim light — top edge
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accentColor.copy(0.35f),
                                Color.Transparent
                            )
                        ),
                        size = Size(size.width, 1.dp.toPx())
                    )
                    // Corner accent wash (very subtle)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accentColor.copy(0.04f), Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = size.width * 0.6f
                        )
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
) = ForgeCard(modifier, shape, accentColor, 20.dp, content)

@Composable
fun ForgeCardSmall(
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) = ForgeCard(modifier, RoundedCornerShape(16.dp), glowColor, 10.dp, content)

/**
 * glassCard — Frosted-glass Modifier extension.
 * Semi-transparent layered background (shimmer + accent bleed + depth shadow)
 * with an accent-tinted border — matches the Bottom Navigation Bar's visual language.
 */
fun Modifier.glassCard(
    accent: Color,
    theme : AppThemeState,
    shape : Shape = RoundedCornerShape(20.dp)
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val base = theme.bg1.copy(alpha = if (theme.isDark) 0.75f else 0.90f)
        // In light mode use a warm tint shimmer instead of harsh white
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
        // Light mode: use a warm brown shadow instead of black
        val depthColor = if (theme.isDark) Color.Black.copy(alpha = 0.30f)
                         else Color(0xFF6B4E2A).copy(alpha = 0.10f)
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
        brush = Brush.linearGradient(
            listOf(
                accent.copy(alpha = if (theme.isDark) 0.28f else 0.35f),
                theme.stroke.copy(alpha = if (theme.isDark) 0.45f else 0.70f),
                accent.copy(alpha = if (theme.isDark) 0.16f else 0.20f)
            )
        ),
        shape = shape
    )

