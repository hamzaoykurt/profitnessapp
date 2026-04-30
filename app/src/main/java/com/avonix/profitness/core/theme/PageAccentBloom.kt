package com.avonix.profitness.core.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sağ üst köşeden tema vurgu rengiyle ışıma efekti.
 * Radial + diyagonal sweep kombinasyonu sayfa sonuna kadar uzanır.
 * Tüm ekranlarda arka plan üzerine katman olarak kullanılır.
 * Light modda daha subtle — warm-earthy arka plan rengini bozmaz.
 */
@Composable
fun PageAccentBloom(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    // Light modda bloom çok daha subtle — neon glow warm bg üzerinde garip durur
    val radialPeak  = if (theme.isDark) 0.16f else 0.07f
    val radialMid   = if (theme.isDark) 0.10f else 0.04f
    val radialEdge  = if (theme.isDark) 0.04f else 0.01f
    val sweepPeak   = if (theme.isDark) 0.07f else 0.03f
    val sweepMid    = if (theme.isDark) 0.02f else 0.01f

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .drawWithCache {
                // Sağ üst köşeden yayılan radyal bloom — büyük yarıçap
                val radial = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f  to accent.copy(alpha = radialPeak),
                        0.30f to accent.copy(alpha = radialMid),
                        0.60f to accent.copy(alpha = radialEdge),
                        1.0f  to Color.Transparent
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.width * 1.15f
                )
                // Diyagonal sweep — sağ üstten sol alta rengi uzatır
                val sweep = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to accent.copy(alpha = sweepPeak),
                        0.5f to accent.copy(alpha = sweepMid),
                        1.0f to Color.Transparent
                    ),
                    start = Offset(size.width, 0f),
                    end   = Offset(size.width * 0.3f, size.height)
                )
                onDrawBehind {
                    drawRect(radial)
                    drawRect(sweep)
                }
            }
    )
}
