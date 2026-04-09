package com.avonix.profitness.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.presentation.auth.AuthEvent
import com.avonix.profitness.presentation.auth.AuthScreen
import com.avonix.profitness.presentation.auth.AuthViewModel
import com.avonix.profitness.presentation.dashboard.DashboardScreen
import com.avonix.profitness.presentation.onboarding.OnboardingScreen
import com.avonix.profitness.presentation.resetpassword.ResetPasswordScreen
import kotlinx.coroutines.flow.StateFlow

private val DURATION = 420

private fun slideEnter() = slideInHorizontally(
    initialOffsetX = { it / 4 },
    animationSpec  = tween(DURATION, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(DURATION, easing = FastOutSlowInEasing))

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
    recoveryCode : StateFlow<String?>,
    onThemeChange: (AppThemeState) -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val code by recoveryCode.collectAsState()

    // Deep link ile recovery code geldiğinde reset_password route'una yönlendir.
    // StateFlow olduğu için compose başlamadan önce set edilse bile kaçmaz.
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        navController.navigate(Routes.resetPassword(c)) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    // Çıkış event'i: signOut tamamlandıktan sonra gelir.
    LaunchedEffect(authViewModel) {
        authViewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToAuth -> {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = false }
                    }
                }
                else -> Unit
            }
        }
    }

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
                onNavigateToDashboard  = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                viewModel = authViewModel
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
                onLogout      = { authViewModel.logout() }
            )
        }

        composable(
            route     = Routes.RESET_PASSWORD,
            arguments = listOf(navArgument("code") { type = NavType.StringType })
        ) { backStackEntry ->
            val recoveryCodeArg = backStackEntry.arguments?.getString("code") ?: ""
            ResetPasswordScreen(
                code   = recoveryCodeArg,
                onDone = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
    }
}
