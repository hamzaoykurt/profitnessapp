package com.cosmibit.profitness.presentation.components

import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Tam ekran Dialog penceresi: kenardan kenara (durum/gezinme çubuğunun ve çentiğin arkasına uzanır),
 * şeffaf sistem çubukları, açık ikonlar. Dialog { } içinde ilk satırda çağrılır.
 * Üst/alt düğmeler içerikte safeDrawing/statusBars+displayCutout inset'iyle yerleştirilmelidir.
 */
@Composable
fun FullScreenDialogWindow() {
    val view = LocalView.current
    // Bir kez uygula: SideEffect her recomposition'da pencereyi yeniden yerleştirip videoyu/fotoyu titretiyordu (çift yenilenme).
    var applied by remember { mutableStateOf(false) }
    SideEffect {
        if (applied) return@SideEffect
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        applied = true
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

/**
 * Dialog penceresi (FLAG_LAYOUT_NO_LIMITS + decorFitsSystemWindows=false) sistem inset'lerini
 * Compose'a 0 verebiliyor; bu yüzden güvenli alan Dialog'un DIŞINDA (Activity kökü) ölçülüp
 * CompositionLocal ile Dialog içeriğine geçirilir.
 */
val LocalSafeTopInset = compositionLocalOf { 0.dp }
val LocalSafeBottomInset = compositionLocalOf { 0.dp }

@Composable
fun ProvideSafeInsets(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val top = with(density) {
        maxOf(WindowInsets.statusBars.getTop(density), WindowInsets.displayCutout.getTop(density)).toDp()
    }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    CompositionLocalProvider(LocalSafeTopInset provides top, LocalSafeBottomInset provides bottom, content = content)
}

/** Üst güvenli alan: max(dialog içi inset, dışarıda ölçülen) — düğmeler durum çubuğu/çentiğin altında kalır. */
@Composable
fun Modifier.safeTopPadding(): Modifier {
    val outer = LocalSafeTopInset.current
    val inner = WindowInsets.safeDrawing.getTop(LocalDensity.current)
    val innerDp = with(LocalDensity.current) { inner.toDp() }
    return this
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(top = maxOf(outer, innerDp))
}

/** Alt güvenli alan (gezinme çubuğu): max(dialog içi, dışarıda ölçülen). */
@Composable
fun Modifier.safeBottomPadding(): Modifier {
    val outer = LocalSafeBottomInset.current
    val innerDp = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp() }
    return this.padding(bottom = maxOf(outer, innerDp))
}
