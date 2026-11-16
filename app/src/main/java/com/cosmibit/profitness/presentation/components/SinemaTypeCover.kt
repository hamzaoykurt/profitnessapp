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
import com.cosmibit.profitness.core.theme.accentColor
import com.cosmibit.profitness.core.theme.accentHi

/** Kapak köşe ışığı: Kuvvet = c1, Kardiyo = c2, Esneklik = nötr, Hibrit/diğer = c1 + c2 (düşük alfa). */
enum class CoverGlow { Strength, Cardio, Mobility, Hybrid }

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
    glow: CoverGlow = CoverGlow.Hybrid,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val c1 = theme.accentColor
    val c2 = theme.accentHi
    // (köşe ışığı rengi, alfa) — ikinci ışık yalnız Hibrit'te sağ-alt köşede düşük alfa
    val (primary, a1) = when (glow) {
        CoverGlow.Strength -> c1 to 0.16f
        CoverGlow.Cardio -> c2 to 0.16f
        CoverGlow.Mobility -> Color.White to 0.06f
        CoverGlow.Hybrid -> c1 to 0.11f
    }
    val ghostTint = when (glow) {
        CoverGlow.Strength -> c1.copy(alpha = 0.07f)
        CoverGlow.Cardio -> c2.copy(alpha = 0.08f)
        CoverGlow.Mobility -> Color.White.copy(alpha = 0.05f)
        CoverGlow.Hybrid -> Color.White.copy(alpha = 0.05f)
    }
    Box(
        modifier.background(Sinema.Surface).clipToBounds()
            .drawBehind {
                // tek yön (sol-üst), lekesiz: radyal yerine geniş doğrusal sönüm
                drawRect(
                    Brush.linearGradient(
                        0f to primary.copy(alpha = a1), 0.55f to Color.Transparent, 1f to Color.Transparent,
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    )
                )
                if (glow == CoverGlow.Hybrid) {
                    drawRect(
                        Brush.linearGradient(
                            0f to c2.copy(alpha = 0.09f), 0.5f to Color.Transparent, 1f to Color.Transparent,
                            start = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            end = androidx.compose.ui.geometry.Offset(0f, 0f)
                        )
                    )
                }
            }
    ) {
        Icon(
            icon, contentDescription = null, tint = ghostTint,
            modifier = Modifier.align(ghostAlignment).offset(ghostOffset.x, ghostOffset.y).requiredSize(ghostSize)
        )
        content()
    }
}

/** Kategori / etiket anahtarından köşe ışığı (yoksa/diğer = Hibrit). coverIconFor ile aynı anahtar. */
fun coverGlowFor(key: String?): CoverGlow {
    val k = key.orEmpty().lowercase()
    return when {
        "kardiyo" in k || "cardio" in k || "koş" in k || "run" in k || "bisiklet" in k || "yüzme" in k || "dayan" in k || "endur" in k -> CoverGlow.Cardio
        "esnek" in k || "mobil" in k || "stretch" in k || "yoga" in k || "pilates" in k || "flex" in k -> CoverGlow.Mobility
        "hibrit" in k || "hybrid" in k || "karma" in k || "hiit" in k || "crossfit" in k -> CoverGlow.Hybrid
        "kuvvet" in k || "strength" in k || "göğüs" in k || "sırt" in k || "bacak" in k || "omuz" in k || "kol" in k ||
            "chest" in k || "back" in k || "leg" in k || "shoulder" in k || "arm" in k || "core" in k || "karın" in k || "abs" in k -> CoverGlow.Strength
        else -> CoverGlow.Hybrid
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
