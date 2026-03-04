package com.avonix.profitness.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.presentation.auth.AuthScreen
import com.avonix.profitness.presentation.dashboard.DashboardScreen

private val DURATION = 420

/** Cinematic shared-axis forward slide */
private fun slideEnter() = slideInHorizontally(
    initialOffsetX = { it / 4 },
    animationSpec  = tween(DURATION, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))

/** Cinematic shared-axis backward fade */
private fun slideExit() = slideOutHorizontally(
    targetOffsetX = { -it / 6 },
    animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))

private fun slidePopEnter() = slideInHorizontally(
    initialOffsetX = { -it / 4 },
    animationSpec  = tween(DURATION, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(DURATION))

private fun slidePopExit() = slideOutHorizontally(
    targetOffsetX = { it / 4 },
    animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(DURATION))

@Composable
fun AppNavigation(
    navController: NavHostController,
    onThemeChange: (AppThemeState) -> Unit
) {
    NavHost(
        navController    = navController,
        startDestination = Routes.AUTH,
        enterTransition  = { slideEnter() },
        exitTransition   = { slideExit() },
        popEnterTransition  = { slidePopEnter() },
        popExitTransition   = { slidePopExit() }
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onThemeChange = onThemeChange,
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
