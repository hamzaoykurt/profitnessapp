package com.cosmibit.profitness.core.theme

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
 * Light modda daha subtle — nötr arka planın temizliğini bozmaz.
 */
@Composable
fun PageAccentBloom(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val accentAtmosphere = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f  to accent.copy(alpha = if (theme.isDark) 0.085f else 0.028f),
                        0.34f to accent.copy(alpha = if (theme.isDark) 0.030f else 0.010f),
                        1.0f  to Color.Transparent
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.width * 1.45f
                )
                val mineralAtmosphere = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to if (theme.isDark) Color(0xFF746688).copy(0.045f) else Color(0xFFB8A8CA).copy(0.16f),
                        0.48f to if (theme.isDark) Color(0xFF746688).copy(0.012f) else Color(0xFFE8DDF0).copy(0.055f),
                        1.0f to Color.Transparent,
                    ),
                    center = Offset(0f, size.height * 0.16f),
                    radius = size.width * 1.15f
                )
                onDrawBehind {
                    drawRect(mineralAtmosphere)
                    drawRect(accentAtmosphere)
                }
            }
    )
}
