package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
    val surface = lerp(Sinema.Surface, hue, 0.30f)
    val ghost = lerp(hue, Color.White, 0.30f).copy(alpha = 0.38f)
    Box(modifier.background(surface).clipToBounds()) {
        Icon(
            icon, contentDescription = null, tint = ghost,
            modifier = Modifier.align(ghostAlignment).offset(ghostOffset.x, ghostOffset.y).requiredSize(ghostSize)
        )
        content()
    }
}

/** Program / etkinlik türü: kapak rengi + ikonu bu tek tablodan gelir (yalnız kapaklarda; UI geneline sızmaz). */
enum class CoverType(val hue: Color, val icon: () -> ImageVector) {
    Football(Color(0xFF4ADE80), { Icons.Rounded.SportsSoccer }),
    Tennis(Color(0xFFC6F135), { Icons.Rounded.SportsTennis }),
    Swim(Color(0xFF38BDF8), { Icons.Rounded.Pool }),
    Run(Color(0xFFFF7A6E), { Icons.Rounded.DirectionsRun }),
    Bike(Color(0xFF2DD4BF), { Icons.Rounded.DirectionsBike }),
    Combat(Color(0xFFF0524F), { Icons.Rounded.SportsMma }),
    Yoga(Color(0xFFA78BFA), { Icons.Rounded.SelfImprovement }),
    Strength(Color(0xFFFFB938), { SinemaIcons.Dumbbell }),
    Cardio(Color(0xFFFF6FA5), { SinemaIcons.Flame }),
    Basketball(Color(0xFFFB923C), { Icons.Rounded.SportsBasketball }),
    Volleyball(Color(0xFF60A5FA), { Icons.Rounded.SportsVolleyball }),
    Pilates(Color(0xFFE879F9), { Icons.Rounded.SportsGymnastics }),
    Core(Color(0xFF34D399), { SinemaIcons.Bolt })
}

private fun hasAny(k: String, vararg w: String) = w.any { it in k }

/** Anahtar (kategori / etiket / başlık; TR+EN) -> tür. Eşleşmezse başlık hash'i ile tür (aynı başlık hep aynı tür). */
fun coverTypeFor(key: String?): CoverType {
    val k = key.orEmpty().lowercase()
    return when {
        hasAny(k, "futbol", "football", "soccer") -> CoverType.Football
        hasAny(k, "tenis", "tennis", "raket", "badminton", "padel") -> CoverType.Tennis
        hasAny(k, "yüz", "swim", "pool", "havuz") -> CoverType.Swim
        hasAny(k, "bisiklet", "cycl", "bike", "spinning") -> CoverType.Bike
        hasAny(k, "koş", "run", "jog", "maraton", "marathon", "yürü", "walk") -> CoverType.Run
        hasAny(k, "boks", "box", "dövüş", "mma", "muay", "kick", "karate", "güreş", "wrestl", "martial") -> CoverType.Combat
        hasAny(k, "pilates") -> CoverType.Pilates
        hasAny(k, "yoga", "esnek", "mobil", "stretch", "flex") -> CoverType.Yoga
        hasAny(k, "basket") -> CoverType.Basketball
        hasAny(k, "voley", "volley") -> CoverType.Volleyball
        hasAny(k, "kardiyo", "cardio", "hiit", "dayan", "endur", "crossfit", "hibrit", "hybrid", "karma") -> CoverType.Cardio
        hasAny(k, "core", "karın", "abs") -> CoverType.Core
        hasAny(k, "kuvvet", "strength", "halter", "weight", "göğüs", "sırt", "bacak", "omuz", "kol", "chest", "back", "leg", "shoulder", "arm", "kas", "muscle", "bulk") -> CoverType.Strength
        else -> CoverType.entries[(k.hashCode() and 0x7fffffff) % CoverType.entries.size]
    }
}

/** Kapak tek rengi (türe göre; opak düz yüzey için). */
fun coverHueFor(key: String?): Color = coverTypeFor(key).hue

/** Kapak hayalet ikonu (türe göre belirgin ikon). */
fun coverIconFor(key: String?): ImageVector = coverTypeFor(key).icon()
