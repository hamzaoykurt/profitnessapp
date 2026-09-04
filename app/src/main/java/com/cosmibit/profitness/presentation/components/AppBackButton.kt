package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.*

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
    contentDescription: String = "Geri"
) {
    val theme = LocalAppTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(),
        label = "app_back_button_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 11.dp,
        animationSpec = spring(),
        label = "app_back_button_depth"
    )
    val fillTop = if (theme.isDark) theme.bg3 else Color.White
    val fillBottom = if (theme.isDark) theme.bg1 else theme.bg3.copy(alpha = 0.82f)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isPressed) 2.dp.toPx() else 0f
            }
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                spotColor = if (theme.isDark) accent.copy(0.22f) else Color(0xFF526176).copy(0.14f),
                ambientColor = if (theme.isDark) Color.Black.copy(0.48f) else Color(0xFF526176).copy(0.07f)
            )
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
            .drawWithCache {
                val canvasSize = this.size
                onDrawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(if (theme.isDark) 0.13f else 0.62f), Color.Transparent)
                        ),
                        size = Size(canvasSize.width, canvasSize.height * 0.48f)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(if (theme.isDark) 0.25f else 0.06f)),
                            startY = canvasSize.height * 0.55f,
                            endY = canvasSize.height
                        ),
                        topLeft = Offset(0f, canvasSize.height * 0.55f),
                        size = Size(canvasSize.width, canvasSize.height * 0.45f)
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.34f),
                        theme.stroke.copy(alpha = 0.88f),
                        Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.34f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = contentDescription,
            tint = theme.text0,
            modifier = Modifier
                .size(size * 0.58f)
                .padding(end = size * 0.04f)
        )
    }
}
