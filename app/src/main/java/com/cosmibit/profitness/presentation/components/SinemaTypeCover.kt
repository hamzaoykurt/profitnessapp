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
import androidx.compose.ui.graphics.lerp
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

/**
 * Tipografik kapak: program / etkinlik / challenge yüzeyleri için ortak zemin (egzersiz fotoğrafı YOK;
 * görsel/video yalnız egzersizlerde). TEK renk, OPAK düz yüzey: #15171C üstüne [hue] %27 karışım; köşe gradyanı / çift ışık YOK.
 * Sağ-altta hayalet ikon (hue açık, α.16). Metinler tam okunur olduğundan foto perdesi gerekmez.
 */
@Composable
fun SinemaTypeCover(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    ghostSize: Dp = 160.dp,
    ghostAlignment: Alignment = Alignment.BottomEnd,
    ghostOffset: DpOffset = DpOffset(48.dp, 48.dp),
    hue: Color = LocalAppTheme.current.accentColor,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val surface = lerp(Sinema.Surface, hue, 0.27f)
    val ghost = lerp(hue, Color.White, 0.30f).copy(alpha = 0.16f)
    Box(modifier.background(surface).clipToBounds()) {
        Icon(
            icon, contentDescription = null, tint = ghost,
            modifier = Modifier.align(ghostAlignment).offset(ghostOffset.x, ghostOffset.y).requiredSize(ghostSize)
        )
        content()
    }
}

/**
 * Kapak tek rengi (c1 amber | c2 mercan): Kuvvet = c1, Kardiyo / Esneklik = c2,
 * Hibrit / bilinmeyen / boş = anahtarın (başlık) hash'i ile c1|c2 (aynı başlık hep aynı renk). coverIconFor ile aynı anahtar.
 */
@Composable
fun coverHueFor(key: String?): Color {
    val theme = LocalAppTheme.current
    val k = key.orEmpty().lowercase()
    val useC2 = when {
        "kardiyo" in k || "cardio" in k || "koş" in k || "run" in k || "bisiklet" in k || "yüzme" in k || "dayan" in k || "endur" in k ||
            "esnek" in k || "mobil" in k || "stretch" in k || "yoga" in k || "pilates" in k || "flex" in k -> true
        "kuvvet" in k || "strength" in k || "göğüs" in k || "sırt" in k || "bacak" in k || "omuz" in k || "kol" in k ||
            "chest" in k || "back" in k || "leg" in k || "shoulder" in k || "arm" in k || "core" in k || "karın" in k || "abs" in k -> false
        else -> (k.hashCode() and 1) == 1   // Hibrit / boş: başlık hash'i
    }
    return if (useC2) theme.accentHi else theme.accentColor
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
