package com.avonix.profitness.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.presentation.auth.AuthEvent
import com.avonix.profitness.presentation.auth.AuthScreen
import com.avonix.profitness.presentation.auth.AuthViewModel
import com.avonix.profitness.presentation.dashboard.DashboardScreen
import com.avonix.profitness.presentation.onboarding.OnboardingScreen

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
    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = Routes.AUTH

    // Recovery deep link geldiğinde kullanıcı dashboard'da olabilir;
    // her yerden AUTH rotasına yönlendiriyoruz (AuthScreen zaten NewPassword state'inde olacak).
    LaunchedEffect(authViewModel) {
        authViewModel.events.collect { event ->
            if (event is AuthEvent.NavigateToAuthForRecovery) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { slideEnter() },
        exitTransition   = { slideExit() },
        popEnterTransition  = { slidePopEnter() },
        popExitTransition   = { slidePopExit() }
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToDashboard  = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onThemeChange = onThemeChange,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
