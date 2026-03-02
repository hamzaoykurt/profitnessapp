package com.avonix.profitness.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ProfitnessTheme(
    themeState: AppThemeState = AppThemeState(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeState.isDark) {
        darkColorScheme(
            primary        = themeState.accent.color,
            onPrimary      = themeState.accent.onColor,
            secondary      = Lava500,
            tertiary       = Forge300,
            background     = themeState.bg0,
            surface        = themeState.bg1,
            onBackground   = themeState.text0,
            onSurface      = themeState.text0,
            surfaceVariant = themeState.bg2
        )
    } else {
        lightColorScheme(
            primary        = themeState.accent.color,
            onPrimary      = themeState.accent.onColor,
            secondary      = Lava500,
            tertiary       = Forge300,
            background     = themeState.bg0,
            surface        = themeState.bg1,
            onBackground   = themeState.text0,
            onSurface      = themeState.text0,
            surfaceVariant = themeState.bg2
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !themeState.isDark
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
