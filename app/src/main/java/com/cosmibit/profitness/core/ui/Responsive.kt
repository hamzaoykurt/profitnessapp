package com.cosmibit.profitness.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    val screenHeight: Dp,
    val fontScale: Float,
    val widthClass: WindowWidthClass,
    val isSmallPhone: Boolean,
    val isShortScreen: Boolean,
    val isTablet: Boolean,
    val isLargeFont: Boolean,
    val isVeryLargeFont: Boolean,
    val useNavigationRail: Boolean,
    val contentMaxWidth: Dp,
    val formMaxWidth: Dp,
    val horizontalPadding: Dp,
    val navRailWidth: Dp,
    val bottomNavHeight: Dp,
    val compactChipMinHeight: Dp,
    val controlMinHeight: Dp,
    val cardHorizontalPadding: Dp,
    val denseVerticalSpacing: Dp
)

@Composable
fun rememberResponsiveLayoutInfo(): ResponsiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val fontScale = LocalDensity.current.fontScale
    val widthClass = when {
        screenWidth < 600.dp -> WindowWidthClass.Compact
        screenWidth < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
    val isSmallPhone = screenWidth < 380.dp
    val isShortScreen = screenHeight < 700.dp
    val isLargeFont = fontScale >= 1.18f
    val isVeryLargeFont = fontScale >= 1.38f

    return ResponsiveLayoutInfo(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        fontScale = fontScale,
        widthClass = widthClass,
        isSmallPhone = isSmallPhone,
        isShortScreen = isShortScreen,
        isTablet = screenWidth >= 600.dp,
        isLargeFont = isLargeFont,
        isVeryLargeFont = isVeryLargeFont,
        useNavigationRail = false,
        contentMaxWidth = when (widthClass) {
            WindowWidthClass.Compact -> 560.dp
            WindowWidthClass.Medium -> 720.dp
            WindowWidthClass.Expanded -> 840.dp
        },
        formMaxWidth = when (widthClass) {
            WindowWidthClass.Compact -> 520.dp
            WindowWidthClass.Medium -> 560.dp
            WindowWidthClass.Expanded -> 620.dp
        },
        horizontalPadding = when {
            screenWidth < 360.dp -> 14.dp
            widthClass == WindowWidthClass.Compact -> 20.dp
            widthClass == WindowWidthClass.Medium -> 32.dp
            else -> 40.dp
        },
        navRailWidth = 0.dp,
        bottomNavHeight = when {
            isVeryLargeFont -> 108.dp
            isLargeFont -> 100.dp
            else -> 92.dp
        },
        compactChipMinHeight = if (isLargeFont) 48.dp else 40.dp,
        controlMinHeight = if (isLargeFont) 58.dp else 52.dp,
        cardHorizontalPadding = if (isSmallPhone) 16.dp else 20.dp,
        denseVerticalSpacing = if (isShortScreen) 8.dp else 12.dp
    )
}
