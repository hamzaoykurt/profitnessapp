package com.cosmibit.profitness.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first

private val AlwaysSettled: State<Boolean> = mutableStateOf(true)

/**
 * Sekme kabı (KeptTab) giriş animasyonu bitince true olur. Sekme dışındaki ekranlarda hep true.
 * Ağır içerik geçiş animasyonu sırasında çizilmesin diye [rememberHeavyContentReady] bunu bekler.
 */
val LocalTabSettled = staticCompositionLocalOf { AlwaysSettled }

/**
 * PERF (ilk açılış / ilk sekme girişi): ilk karede yalnız üst bölüm + düz zemin çizilsin; gölgeli/gradyanlı kartlar,
 * listeler ve görseller sekme yerleşince + [frames] kare sonra kompoze edilir. Bir kez true olunca öyle kalır.
 */
@Composable
fun rememberHeavyContentReady(frames: Int = 2): Boolean {
    val settled = LocalTabSettled.current
    var ready by remember { mutableStateOf(false) }
    if (!ready) LaunchedEffect(Unit) {
        snapshotFlow { settled.value }.first { it }
        repeat(frames) { withFrameNanos { } }
        ready = true
    }
    return ready
}
