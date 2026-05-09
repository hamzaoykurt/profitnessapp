package com.avonix.profitness.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded
}

@Immutable
data class ResponsiveLayoutInfo(
    val screenWidth: Dp,
    val widthClass: WindowWidthClass,
    val isSmallPhone: Boolean,
    val useNavigationRail: Boolean,
    val contentMaxWidth: Dp,
    val formMaxWidth: Dp,
    val horizontalPadding: Dp,
    val navRailWidth: Dp
)

@Composable
fun rememberResponsiveLayoutInfo(): ResponsiveLayoutInfo {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val widthClass = when {
        screenWidth < 600.dp -> WindowWidthClass.Compact
        screenWidth < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }

    return ResponsiveLayoutInfo(
        screenWidth = screenWidth,
        widthClass = widthClass,
        isSmallPhone = screenWidth < 380.dp,
        useNavigationRail = screenWidth >= 720.dp,
        contentMaxWidth = when (widthClass) {
            WindowWidthClass.Compact -> 560.dp
            WindowWidthClass.Medium -> 760.dp
            WindowWidthClass.Expanded -> 960.dp
        },
        formMaxWidth = when (widthClass) {
            WindowWidthClass.Compact -> 520.dp
            WindowWidthClass.Medium -> 560.dp
            WindowWidthClass.Expanded -> 620.dp
        },
        horizontalPadding = when {
            screenWidth < 360.dp -> 16.dp
            widthClass == WindowWidthClass.Compact -> 24.dp
            widthClass == WindowWidthClass.Medium -> 32.dp
            else -> 40.dp
        },
        navRailWidth = 92.dp
    )
}
