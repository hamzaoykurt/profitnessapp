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
