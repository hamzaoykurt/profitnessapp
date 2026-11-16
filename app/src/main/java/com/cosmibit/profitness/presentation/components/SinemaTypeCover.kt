package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
 * görsel/video yalnız egzersizlerde). TEK renk hue, OPAK, 3D yumuşak blok (üst açık / alt koyu dikey gradyan + üst yumuşak parlama + alt iç gölge).
 * Sağ-üstte hayalet ikon (hue açık, α.40; alt satır düğmeleriyle çakışmaz). Metinler tam okunur olduğundan foto perdesi gerekmez.
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
    val base = lerp(Sinema.Surface, hue, 0.30f)
    val ghost = lerp(hue, Color.White, 0.30f).copy(alpha = 0.40f)
    // 3D yumuşak blok: tek renk hue; üst açık, alt koyu, üstte yayılan yumuşak parlama, altta iç gölge (keskin hat yok)
    BoxWithConstraints(
        modifier
            .background(Brush.verticalGradient(0f to lerp(base, Color.White, 0.12f), 1f to lerp(base, Color.Black, 0.38f)))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(0f to Color.White.copy(0.14f), 1f to Color.Transparent, startY = 0f, endY = 10.dp.toPx().coerceAtMost(size.height)),
                    size = androidx.compose.ui.geometry.Size(size.width, 10.dp.toPx().coerceAtMost(size.height))
                )
                val h = 22.dp.toPx().coerceAtMost(size.height)
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(0.28f), startY = size.height - h, endY = size.height),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h)
                )
            }
            .clipToBounds()
    ) {
        // hayalet ikon: sağ-üst/orta bölge, düğme/çiplerle (alt satır) çakışmasın diye yüksekliğin ~%52'siyle sınırlı
        val sz = minOf(ghostSize, maxHeight * 0.52f, maxWidth * 0.5f)
        Icon(
            icon, contentDescription = null, tint = ghost,
            modifier = Modifier.align(Alignment.TopEnd).offset(-(maxWidth * 0.06f), maxHeight * 0.10f).requiredSize(sz)
        )
        content()
    }
}

/** Program / etkinlik türü: kapak rengi + ikonu bu tek tablodan gelir (yalnız kapaklarda; UI geneline sızmaz). */
enum class CoverType(val hue: Color, val icon: () -> ImageVector) {
    Football(Color(0xFF4ADE80), { Icons.Rounded.SportsSoccer }),
    Tennis(Color(0xFFC6F135), { Icons.Rounded.SportsTennis }),
    Swim(Color(0xFF38BDF8), { Icons.Rounded.Pool }),
    Run(Color(0xFFFFB938), { Icons.Rounded.DirectionsRun }),
    Bike(Color(0xFF2DD4BF), { Icons.Rounded.DirectionsBike }),
    Combat(Color(0xFF94A3B8), { Icons.Rounded.SportsMma }),
    Yoga(Color(0xFFC084FC), { Icons.Rounded.SelfImprovement }),
    Strength(Color(0xFFFF7A6E), { SinemaIcons.Dumbbell }),
    Cardio(Color(0xFFFF4D8D), { SinemaIcons.Flame }),
    Basketball(Color(0xFFF97316), { Icons.Rounded.SportsBasketball }),
    Volleyball(Color(0xFF60A5FA), { Icons.Rounded.SportsVolleyball }),
    Pilates(Color(0xFFE879F9), { Icons.Rounded.SportsGymnastics }),
    Core(Color(0xFF818CF8), { SinemaIcons.Bolt })
}

private val GeneralTypes = listOf(CoverType.Strength, CoverType.Cardio, CoverType.Run, CoverType.Yoga, CoverType.Bike, CoverType.Strength, CoverType.Swim, CoverType.Core)

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
        k.isBlank() -> CoverType.Strength   // bilinmeyen/boş = fitness mercan
        else -> GeneralTypes[(k.hashCode() and 0x7fffffff) % GeneralTypes.size]   // eşleşmeyen başlık: genel türler arası (kartlar tek renk olmasın)
    }
}

/** Kapak tek rengi (türe göre; opak düz yüzey için). */
fun coverHueFor(key: String?): Color = coverTypeFor(key).hue

/** Kapak hayalet ikonu (türe göre belirgin ikon). */
fun coverIconFor(key: String?): ImageVector = coverTypeFor(key).icon()
