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
import androidx.navigation.compose.rememberNavController
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.core.theme.AppThemeStateSaver
import com.avonix.profitness.core.theme.ProfitnessTheme
import com.avonix.profitness.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var themeState by rememberSaveable(stateSaver = AppThemeStateSaver) {
                mutableStateOf(AppThemeState())
            }
            ProfitnessTheme(themeState = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController, onThemeChange = { themeState = it })
                }
            }
        }
    }
}
