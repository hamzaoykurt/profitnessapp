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
import androidx.compose.ui.draw.scale
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
    val borderAlpha = if (glowColor != Color.Transparent) 0.38f else if (theme.isDark) 0.22f else 0.30f

    Box(
        modifier = modifier
            .clip(shape)
            .background(theme.bg2)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accentColor.copy(borderAlpha),
                        theme.stroke.copy(if (theme.isDark) 0.55f else 0.70f)
                    )
                ),
                shape = shape
            )
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
    .background(theme.bg1.copy(alpha = if (theme.isDark) 0.78f else 0.92f))
    .border(
        width = 1.dp,
        color = accent.copy(alpha = if (theme.isDark) 0.24f else 0.30f),
        shape = shape
    )

