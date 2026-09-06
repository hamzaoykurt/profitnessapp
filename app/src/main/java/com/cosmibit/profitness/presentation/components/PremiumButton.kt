package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo

/**
 * Compact premium CTA. The face stays in the layout plane and becomes recessed
 * on press; there is no raised lower ledge or expensive stacked shadow chain.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val haptic = LocalHapticFeedback.current
    val responsive = rememberResponsiveLayoutInfo()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && isEnabled) 1f else 0f,
        animationSpec = tween(120),
        label = "premiumButtonPress"
    )
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .heightIn(min = responsive.controlMinHeight)
            .graphicsLayer {
                scaleX = 1f - pressProgress * 0.012f
                scaleY = 1f - pressProgress * 0.012f
            }
            .performanceControlSurface(theme, if (isEnabled) accent else theme.bg3, pressed && isEnabled, shape)
            .clickable(
                enabled = isEnabled && !isLoading,
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = if (responsive.isLargeFont) 11.dp else 13.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = if (isEnabled) theme.effectiveOnAccentColor else theme.text2, strokeWidth = 2.dp)
        } else {
            ButtonContent(text, leadingIcon, if (isEnabled) theme.effectiveOnAccentColor else theme.text2, false)
        }
    }
}

/** Neutral secondary action using the same inset press language. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    val responsive = rememberResponsiveLayoutInfo()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && isEnabled) 1f else 0f,
        animationSpec = tween(120),
        label = "ghostButtonPress"
    )
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .heightIn(min = responsive.controlMinHeight)
            .graphicsLayer {
                scaleX = 1f - pressProgress * 0.01f
                scaleY = 1f - pressProgress * 0.01f
            }
            .performanceControlSurface(theme, theme.bg2, pressed && isEnabled, shape)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = if (responsive.isLargeFont) 11.dp else 13.dp),
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(text, leadingIcon, if (isEnabled) theme.text0 else theme.text2, true)
    }
}

@Composable
private fun ButtonContent(text: String, leadingIcon: ImageVector?, color: Color, neutral: Boolean) {
    val theme = LocalAppTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        leadingIcon?.let {
            Icon(it, null, tint = if (neutral) theme.text1 else color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

/** Icon-only variant with the same quiet, recessed press response. */
@Composable
fun PremiumIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(54.dp),
    isEnabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && isEnabled) 1f else 0f,
        animationSpec = tween(120),
        label = "premiumIconPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = 1f - pressProgress * 0.018f
                scaleY = 1f - pressProgress * 0.018f
            }
            .performanceControlSurface(theme, theme.bg2, pressed && isEnabled, shape)
            .clickable(enabled = isEnabled, interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isEnabled) theme.effectiveAccentColor else theme.text2,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun Color.mix(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha
    )
}
