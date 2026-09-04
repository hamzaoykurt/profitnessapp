package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo

/**
 * ForgeButton — Apple/Substack-inspired 3D tactile button.
 *
 * Physics:
 * - Press → scale 0.96 + shadow collapses (feels like pressing INTO screen)
 * - Release → spring bounce back with DampingRatioMediumBouncy
 * - Active: one theme accent, restrained bevel and controlled glow
 * - Disabled: quiet theme surface, no glow
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
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val fill = if (theme.isDark) accent.mixWith(Color(0xFF16171C), 0.66f) else accent
    val onAccent = if (theme.isDark) accent else MaterialTheme.colorScheme.onPrimary
    val responsive = rememberResponsiveLayoutInfo()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Physical press: scale down + shadow collapse
    val scale by animateFloatAsState(
        targetValue    = when {
            !isEnabled  -> 1f
            isPressed   -> 0.975f
            else        -> 1f
        },
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )
    val shape = RoundedCornerShape(18.dp)
    val accentTop = fill.mixWith(Color.White, if (theme.isDark) 0.10f else 0.055f)
    val accentCore = fill.mixWith(Color.Black, if (theme.isDark) 0.05f else 0.035f)
    val accentBottom = fill.mixWith(Color.Black, if (theme.isDark) 0.20f else 0.15f)
    val ledge = fill.mixWith(Color.Black, if (theme.isDark) 0.48f else 0.34f)
    val faceOffset by animateDpAsState(
        targetValue = if (isPressed && isEnabled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "buttonFaceOffset"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = responsive.controlMinHeight)
            .shadow(
                elevation    = if (isPressed) 4.dp else 16.dp,
                shape        = shape,
                spotColor    = if (isEnabled) accent.copy(if (theme.isDark) 0.22f else 0.10f) else Color.Transparent,
                ambientColor = if (isEnabled) Color.Black.copy(if (theme.isDark) 0.64f else 0.14f) else Color.Transparent
            )
            .padding(bottom = 5.dp)
    ) {
        Box(
            Modifier.matchParentSize().offset(y = 5.dp).clip(shape).background(ledge)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = faceOffset)
                .clip(shape)
                .background(
                    if (isEnabled) Brush.verticalGradient(listOf(accentTop, accentCore, accentBottom))
                    else Brush.verticalGradient(listOf(theme.bg3, theme.bg2))
                )
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            Brush.verticalGradient(listOf(Color.White.copy(if (theme.isDark) 0.16f else 0.13f), Color.Transparent)),
                            size = Size(size.width, size.height * 0.48f)
                        )
                        drawRect(Color.White.copy(if (theme.isDark) 0.31f else 0.40f), size = Size(size.width, 1.dp.toPx()))
                    }
                }
                .border(1.dp, if (theme.isDark) accent.copy(0.40f) else Color.White.copy(0.48f), shape)
                .clickable(
                    enabled = isEnabled && !isLoading,
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(horizontal = 18.dp, vertical = if (responsive.isLargeFont) 10.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = onAccent, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    leadingIcon?.let {
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(if (theme.isDark) 0.20f else 0.10f))
                                .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(it, null, tint = if (isEnabled) onAccent else theme.text2, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(text, color = if (isEnabled) onAccent else theme.text2, fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp, letterSpacing = 0.35.sp, lineHeight = 18.sp, maxLines = 2, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Secondary tactile control: neutral porcelain/graphite face on a visible base. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val theme  = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "ghostScale"
    )
    val shape = RoundedCornerShape(18.dp)
    val faceOffset by animateDpAsState(
        targetValue = if (isPressed && isEnabled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "ghostFaceOffset"
    )
    val faceTop = if (theme.isDark) Color(0xFF2B2C34) else Color.White
    val faceBottom = if (theme.isDark) Color(0xFF17181E) else Color(0xFFF2EFF6)
    val ledge = if (theme.isDark) Color(0xFF090A0D) else Color(0xFFD8D2DE)
    Box(
        modifier = modifier.scale(scale).heightIn(min = responsive.controlMinHeight)
            .shadow(
                elevation = if (isPressed) 3.dp else if (theme.isDark) 15.dp else 12.dp,
                shape = shape,
                spotColor = Color.Black.copy(if (theme.isDark) 0.68f else 0.14f),
                ambientColor = Color.Black.copy(if (theme.isDark) 0.34f else 0.07f)
            )
            .padding(bottom = 5.dp)
    ) {
        Box(Modifier.matchParentSize().offset(y = 5.dp).clip(shape).background(ledge))
        Box(
            modifier = Modifier.matchParentSize().offset(y = faceOffset).clip(shape)
                .background(Brush.verticalGradient(listOf(faceTop, faceBottom)))
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            Brush.verticalGradient(listOf(Color.White.copy(if (theme.isDark) 0.12f else 0.72f), Color.Transparent)),
                            size = Size(size.width, size.height * 0.44f)
                        )
                        drawRect(Color.White.copy(if (theme.isDark) 0.20f else 0.94f), size = Size(size.width, 1.dp.toPx()))
                    }
                }
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(if (theme.isDark) 0.18f else 0.98f), theme.stroke)), shape)
                .clickable(enabled = isEnabled, interactionSource = interactionSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(horizontal = 18.dp, vertical = if (responsive.isLargeFont) 10.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                leadingIcon?.let {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (theme.isDark) Color.Black.copy(0.28f) else Color(0xFFECE8F0))
                            .border(1.dp, Color.White.copy(if (theme.isDark) 0.10f else 0.72f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(it, null,
                            tint = if (isEnabled) theme.effectiveAccentColor else theme.text2,
                            modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                }
                Text(text, color = if (isEnabled) theme.text0 else theme.text2, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, letterSpacing = 0.2.sp, lineHeight = 18.sp, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun Color.mixWith(other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha
    )
}

/** Compact tactile control for FABs and prominent icon-only actions. */
@Composable
fun PremiumIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(54.dp),
    isEnabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    val theme = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val fill = if (theme.isDark) accent.mixWith(theme.bg2, 0.72f) else accent
    val onAccent = if (theme.isDark) accent else MaterialTheme.colorScheme.onPrimary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isEnabled) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "premiumIconScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed && isEnabled) 3.dp else 11.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "premiumIconDepth"
    )
    val top = fill.mixWith(Color.White, if (theme.isDark) 0.05f else 0.03f)
    val bottom = fill.mixWith(Color.Black, if (theme.isDark) 0.16f else 0.12f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isPressed && isEnabled) 3.dp.toPx() else 0f
            }
            .shadow(
                elevation = if (isEnabled) elevation else 0.dp,
                shape = shape,
                spotColor = if (isEnabled) accent.copy(if (theme.isDark) 0.20f else 0.10f) else Color.Transparent,
                ambientColor = Color.Black.copy(if (theme.isDark) 0.38f else 0.08f)
            )
            .clip(shape)
            .background(
                if (isEnabled) Brush.verticalGradient(listOf(top, fill, bottom))
                else Brush.verticalGradient(listOf(theme.bg3, theme.bg2))
            )
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.White.copy(if (theme.isDark) 0.12f else 0.08f), Color.Transparent)),
                        size = Size(size.width, size.height * 0.48f)
                    )
                    if (isEnabled) drawRect(Color.White.copy(if (theme.isDark) 0.26f else 0.34f), size = Size(size.width, 1.dp.toPx()))
                }
            }
            .border(
                1.dp,
                if (isEnabled) {
                    if (theme.isDark) accent.copy(0.34f) else Color.White.copy(0.32f)
                } else theme.stroke,
                shape
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isEnabled) onAccent else theme.text2,
            modifier = Modifier.size(23.dp)
        )
    }
}

