package com.avonix.profitness.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

/**
 * ForgeButton — Apple/Substack-inspired 3D tactile button.
 *
 * Physics:
 * - Press → scale 0.96 + shadow collapses (feels like pressing INTO screen)
 * - Release → spring bounce back with DampingRatioMediumBouncy
 * - Active: amber→ember fire gradient + glowing shadow ring
 * - Disabled: flat dark, no glow
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Physical press: scale down + shadow collapse
    val scale by animateFloatAsState(
        targetValue    = when {
            !isEnabled  -> 1f
            isPressed   -> 0.96f
            else        -> 1f
        },
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )
    val elevation by animateDpAsState(
        targetValue   = if (isPressed && isEnabled) 4.dp else 18.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btnElev"
    )

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .scale(scale)
            // Amber glow shadow — collapses on press
            .shadow(
                elevation    = if (isEnabled) elevation else 0.dp,
                shape        = shape,
                spotColor    = if (isEnabled) Forge500.copy(0.6f) else Color.Transparent,
                ambientColor = if (isEnabled) Forge600.copy(0.3f) else Color.Transparent
            )
            .clip(shape)
            .background(
                if (isEnabled)
                    Brush.linearGradient(
                        colors = listOf(Forge500, Lava500, Ember500),
                        start  = Offset(0f, 0f),
                        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                else
                    Brush.linearGradient(listOf(Depth2, Depth2))
            )
            .drawWithContent {
                drawContent()
                if (isEnabled) {
                    // Top shimmer — "gloss" layer
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x40FFFFFF), Color(0x00FFFFFF)),
                            endY   = size.height * 0.5f
                        ),
                        size = Size(size.width, size.height * 0.5f)
                    )
                    // Bottom depth shadow
                    drawRect(
                        brush   = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x40000000)),
                            startY = size.height * 0.5f,
                            endY   = size.height
                        ),
                        topLeft = Offset(0f, size.height * 0.5f),
                        size    = Size(size.width, size.height * 0.5f)
                    )
                    // Top bevel edge
                    drawRect(
                        color = Color(0x30FFFFFF),
                        size  = Size(size.width, 1.5f)
                    )
                }
            }
            .clickable(
                enabled           = isEnabled && !isLoading,
                indication        = null,
                interactionSource = interactionSource,
                onClick           = onClick
            )
            .padding(24.dp, 18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text          = text,
                color         = if (isEnabled) Color.Black else Fog,
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/** Secondary flat outline ghost button */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "ghostScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Depth2)
            .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
            .clickable(enabled = isEnabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(24.dp, 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = text,
            color         = if (isEnabled) Mist else Fog,
            fontWeight    = FontWeight.Bold,
            fontSize      = 14.sp,
            letterSpacing = 0.5.sp
        )
    }
}

