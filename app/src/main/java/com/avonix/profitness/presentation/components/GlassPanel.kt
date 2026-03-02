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
    val accentColor = if (glowColor != Color.Transparent) glowColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = if (glowColor != Color.Transparent) accentColor.copy(0.35f) else Color.Black.copy(0.8f),
                ambientColor = Color.Black.copy(0.4f)
            )
            .clip(shape)
            .background(Surface2)
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

