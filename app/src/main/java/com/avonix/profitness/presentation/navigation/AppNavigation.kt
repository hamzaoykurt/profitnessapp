package com.avonix.profitness.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    clearRecoveryCode: () -> Unit,
    onThemeChange: (AppThemeState) -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val code by recoveryCode.collectAsState()
    var activeRecoveryCode by remember { mutableStateOf<String?>(null) }

    // Deep link ile recovery code geldiğinde reset_password route'una yönlendir.
    // StateFlow olduğu için compose başlamadan önce set edilse bile kaçmaz.
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        activeRecoveryCode = c
        clearRecoveryCode()
        navController.navigate(Routes.RESET_PASSWORD) {
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

        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                code   = activeRecoveryCode.orEmpty(),
                onDone = {
                    activeRecoveryCode = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
    }
}
