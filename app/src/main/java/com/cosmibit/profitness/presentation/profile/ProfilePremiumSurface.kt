package com.cosmibit.profitness.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
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
import com.cosmibit.profitness.core.theme.performanceSurface
import com.cosmibit.profitness.core.theme.performanceElevatedSurface
import com.cosmibit.profitness.presentation.components.insetControlSurface

/**
 * Profile surfaces deliberately use a solid, layered material. Glass is reserved for
 * transient overlays and photo/header treatments, so data-heavy screens keep their depth
 * without turning into a wall of translucent panels.
 */
internal fun Modifier.profilePremiumSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(22.dp),
    accent: Color? = null,
    elevation: Dp = if (theme.isDark) 8.dp else 5.dp
): Modifier {
    return this.performanceElevatedSurface(theme, shape = shape, elevated = !theme.isDark && elevation > 0.dp)
}

/** A raised solid control that visibly settles into its surface while pressed. */
internal fun Modifier.profilePremiumAction(
    theme: AppThemeState,
    accent: Color,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = true,
    recessed: Boolean = false
): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    val isPressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "profileActionScale"
    )
    val indication = LocalIndication.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = 0f
            alpha = if (enabled) 1f else 0.52f
        }
        .then(
            if (recessed) Modifier.insetControlSurface(accent, theme, shape)
            else Modifier.performanceElevatedSurface(theme, shape, elevated = !isPressed)
        )
        .clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactions,
            indication = indication,
            onClick = onClick
        )
}
