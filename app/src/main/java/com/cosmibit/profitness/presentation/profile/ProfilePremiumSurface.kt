package com.cosmibit.profitness.presentation.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.bg2
import com.cosmibit.profitness.core.theme.bg3
import com.cosmibit.profitness.core.theme.stroke

/**
 * Profile surfaces deliberately use a solid, layered material. Glass is reserved for
 * transient overlays and photo/header treatments, so data-heavy screens keep their depth
 * without turning into a wall of translucent panels.
 */
internal fun Modifier.profilePremiumSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(22.dp),
    accent: Color? = null,
    elevation: Dp = if (theme.isDark) 14.dp else 8.dp
): Modifier {
    val surface = if (theme.isDark) {
        Brush.verticalGradient(
            0f to theme.bg3,
            0.42f to theme.bg2,
            1f to theme.bg1
        )
    } else {
        Brush.verticalGradient(
            0f to Color.White,
            0.58f to Color.White,
            1f to theme.bg2.copy(alpha = 0.52f)
        )
    }
    val rim = if (theme.isDark) {
        Brush.linearGradient(
            listOf(
                (accent ?: Color.White).copy(alpha = if (accent == null) 0.10f else 0.20f),
                Color.White.copy(alpha = 0.10f),
                theme.stroke.copy(alpha = 0.86f),
                Color.Black.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                theme.stroke.copy(alpha = 0.72f),
                (accent ?: theme.stroke).copy(alpha = if (accent == null) 0.36f else 0.11f)
            )
        )
    }
    val ambient = if (theme.isDark) Color.Black.copy(alpha = 0.68f)
    else Color(0xFF64748B).copy(alpha = 0.13f)
    val spot = if (theme.isDark) (accent ?: Color.Black).copy(alpha = if (accent == null) 0.48f else 0.12f)
    else Color(0xFF64748B).copy(alpha = 0.09f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambient,
            spotColor = spot
        )
        .clip(shape)
        .background(surface)
        .border(BorderStroke(1.dp, rim), shape)
}

/** A raised solid control that visibly settles into its surface while pressed. */
internal fun Modifier.profilePremiumAction(
    theme: AppThemeState,
    accent: Color,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = true
): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    val isPressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "profileActionScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else if (theme.isDark) 13.dp else 7.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 600f),
        label = "profileActionElevation"
    )
    val indication = LocalIndication.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = if (isPressed) 1.8.dp.toPx() else 0f
            alpha = if (enabled) 1f else 0.52f
        }
        .profilePremiumSurface(theme, shape, accent, elevation)
        .clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactions,
            indication = indication,
            onClick = onClick
        )
}
