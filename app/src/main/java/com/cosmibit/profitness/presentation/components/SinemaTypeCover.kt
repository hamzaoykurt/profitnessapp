package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.core.theme.accentSoft

/**
 * Tipografik kapak: program / etkinlik / challenge yüzeyleri için ortak zemin (egzersiz fotoğrafı YOK;
 * görsel/video yalnız egzersizlerde). Yüzey + tek yönlü hafif accentSoft köşe ışığı + sağ-altta hayalet ikon.
 * Metinler tam okunur olduğundan foto perdesi gerekmez. Çizim: .ekip/mockup/uc-sayfa.html `.cover`/`.ghost`.
 */
@Composable
fun SinemaTypeCover(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    ghostSize: Dp = 160.dp,
    ghostAlignment: Alignment = Alignment.BottomEnd,
    ghostOffset: DpOffset = DpOffset(48.dp, 48.dp),
    content: @Composable BoxScope.() -> Unit = {}
) {
    val soft = LocalAppTheme.current.accentSoft
    Box(
        modifier.background(Sinema.Surface).clipToBounds()
            .drawBehind {
                // tek yön (sol-üst), lekesiz: radyal yerine geniş doğrusal sönüm
                drawRect(
                    Brush.linearGradient(
                        0f to soft.copy(alpha = 0.12f), 0.55f to Color.Transparent, 1f to Color.Transparent,
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    )
                )
            }
    ) {
        Icon(
            icon, contentDescription = null, tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.align(ghostAlignment).offset(ghostOffset.x, ghostOffset.y).requiredSize(ghostSize)
        )
        content()
    }
}

/** Kategori / etiket anahtarından hayalet ikon (mevcut Sinema ikon seti; yoksa dambıl). */
fun coverIconFor(key: String?): ImageVector {
    val k = key.orEmpty().lowercase()
    return when {
        "kardiyo" in k || "cardio" in k || "koş" in k || "run" in k -> SinemaIcons.Flame
        "core" in k || "karın" in k || "abs" in k -> SinemaIcons.Bolt
        else -> SinemaIcons.Dumbbell
    }
}
