package com.avonix.profitness.core.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Sağ üst köşeden tema vurgu rengiyle ışıma efekti.
 * Radial + diyagonal sweep kombinasyonu sayfa sonuna kadar uzanır.
 * Tüm ekranlarda arka plan üzerine katman olarak kullanılır.
 */
@Composable
fun PageAccentBloom(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // Sağ üst köşeden yayılan radyal bloom — büyük yarıçap
                val radial = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f  to accent.copy(alpha = 0.16f),
                        0.30f to accent.copy(alpha = 0.10f),
                        0.60f to accent.copy(alpha = 0.04f),
                        1.0f  to Color.Transparent
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.width * 2.2f
                )
                // Diyagonal sweep — sağ üstten sol alta rengi uzatır
                val sweep = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to accent.copy(alpha = 0.07f),
                        0.5f to accent.copy(alpha = 0.02f),
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
