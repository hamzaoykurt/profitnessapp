package com.avonix.profitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.core.theme.AppThemeStateSaver
import com.avonix.profitness.core.theme.ProfitnessTheme
import com.avonix.profitness.core.theme.ThemeRepository
import com.avonix.profitness.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // rememberSaveable handles rotation/process-death quickly (no I/O wait).
            // DataStore syncs on first collection and on every explicit save.
            var themeState by rememberSaveable(stateSaver = AppThemeStateSaver) {
                mutableStateOf(AppThemeState())
            }

            // One-shot: load persisted theme on first composition
            val persisted by themeRepository.themeFlow.collectAsState(initial = null)
            LaunchedEffect(persisted) {
                persisted?.let { saved ->
                    // Only apply persisted values — keep language/notifications from in-memory state
                    themeState = themeState.copy(isDark = saved.isDark, accent = saved.accent)
                }
            }

            ProfitnessTheme(themeState = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController  = navController,
                        onThemeChange  = { newState ->
                            themeState = newState
                            // Persist asynchronously — does not block UI
                            lifecycleScope.launch { themeRepository.saveTheme(newState) }
                        }
                    )
                }
            }
        }
    }
}
