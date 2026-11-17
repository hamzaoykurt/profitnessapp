package com.cosmibit.profitness.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Ana sekmeler bellekte canlı tutulur (DashboardScreen); görünmeyen sekme kompoze kalır ama çizilmez.
 * Sekme içeriği "sekmeye her girişte" işi (stale ise tazele vb.) bu değere bağlar: `LaunchedEffect(active)`.
 * Sekme dışı kullanımda (overlay, önizleme) her zaman true.
 */
val LocalTabActive = staticCompositionLocalOf<State<Boolean>> { AlwaysActive }

private val AlwaysActive: State<Boolean> = mutableStateOf(true)

/** Önbellek tazeliği (SWR): veri varsa ve son yüklemeden bu yana [ttlMs] geçmediyse yeniden yükleme gerekmez. */
fun isCacheFresh(hasData: Boolean, lastLoadMs: Long, nowMs: Long, ttlMs: Long = 5 * 60_000L): Boolean =
    hasData && lastLoadMs > 0L && nowMs - lastLoadMs in 0 until ttlMs

/**
 * Sekmeye YENİDEN giriş algılayıcı: ilk aktiflik (ilk yükleme ayrı yapılır) tetiklemez; gizlenip tekrar aktif olunca true döner.
 */
class TabReentryTracker {
    private var seenActive = false
    private var wasHidden = false
    fun update(active: Boolean): Boolean {
        if (!active) { if (seenActive) wasHidden = true; return false }
        val fire = seenActive && wasHidden
        seenActive = true; wasHidden = false
        return fire
    }
}

/** Sekme gizlenip tekrar aktif olduğunda [onReentry] çalışır (ör. stale ise tazele). */
@androidx.compose.runtime.Composable
fun OnTabReentry(active: Boolean, onReentry: () -> Unit) {
    val tracker = androidx.compose.runtime.remember { TabReentryTracker() }
    val latest = androidx.compose.runtime.rememberUpdatedState(onReentry)
    androidx.compose.runtime.LaunchedEffect(active) { if (tracker.update(active)) latest.value() }
}
