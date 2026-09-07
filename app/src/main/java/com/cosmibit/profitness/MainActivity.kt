package com.cosmibit.profitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.AppThemeStateSaver
import com.cosmibit.profitness.core.theme.ProfitnessTheme
import com.cosmibit.profitness.core.theme.ThemeRepository
import com.cosmibit.profitness.core.theme.ThemeMode
import com.cosmibit.profitness.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    /**
     * Şifre sıfırlama deep link'inden çıkarılan PKCE code.
     * StateFlow olduğu için setContent öncesi set edilse bile Compose onu kaçırmaz.
     */
    private val recoveryCode = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractRecoveryCode(intent)

        setContent {
            var themeState by rememberSaveable(stateSaver = AppThemeStateSaver) {
                mutableStateOf(AppThemeState())
            }

            val persisted by themeRepository.themeFlow.collectAsStateWithLifecycle(initialValue = null)
            LaunchedEffect(persisted) {
                persisted?.let { saved ->
                    themeState = saved
                }
            }

            val systemIsDark = isSystemInDarkTheme()
            val effectiveThemeState = themeState.copy(
                isDark = when (themeState.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> systemIsDark
                }
            )

            ProfitnessTheme(themeState = effectiveThemeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = Color.Transparent
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController  = navController,
                        recoveryCode   = recoveryCode,
                        onThemeChange  = { newState ->
                            themeState = newState.copy(
                                isDark = when (newState.themeMode) {
                                    ThemeMode.DARK -> true
                                    ThemeMode.LIGHT -> false
                                    ThemeMode.SYSTEM -> systemIsDark
                                }
                            )
                            lifecycleScope.launch { themeRepository.saveTheme(newState) }
                        }
                    )
                }
            }
        }
    }

    /** Uygulama açıkken deep link ile gelindiyse (launchMode=singleTop) */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractRecoveryCode(intent)
    }

    private fun extractRecoveryCode(intent: Intent) {
        val data = intent.data ?: return
        val isVerifiedAppLink =
            data.scheme == "https" &&
            data.host == BuildConfig.RESET_PASSWORD_LINK_HOST &&
            data.path == "/reset-password"
        val isLegacyRecoveryLink = data.scheme == "profitness" && data.host == "reset-password"
        if (isVerifiedAppLink || isLegacyRecoveryLink) {
            val code = data.getQueryParameter("code") ?: return
            recoveryCode.value = code
        }
    }
}
