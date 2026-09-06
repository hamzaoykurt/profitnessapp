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
                // Both sources sit well outside the viewport so the user sees
                // ambient falloff, never a literal coloured circle.
                val accentAtmosphere = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f  to accent.copy(alpha = if (theme.isDark) 0.22f else 0.085f),
                        0.28f to accent.copy(alpha = if (theme.isDark) 0.075f else 0.025f),
                        1.0f  to Color.Transparent
                    ),
                    center = Offset(size.width * 1.38f, -size.height * 0.16f),
                    radius = size.width * 2.15f
                )
                val mineralAtmosphere = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to if (theme.isDark) Color(0xFF315D8A).copy(0.16f) else Color(0xFF6887A7).copy(0.075f),
                        0.42f to if (theme.isDark) Color(0xFF315D8A).copy(0.035f) else Color(0xFFB7C7D7).copy(0.018f),
                        1.0f to Color.Transparent,
                    ),
                    center = Offset(-size.width * 0.48f, size.height * 0.72f),
                    radius = size.width * 2.05f
                )
                onDrawBehind {
                    drawRect(mineralAtmosphere)
                    drawRect(accentAtmosphere)
                }
            }
    )
}
