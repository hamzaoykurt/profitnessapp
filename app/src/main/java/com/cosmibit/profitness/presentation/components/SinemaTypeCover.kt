package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
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
    ghostAlignment: Alignment = Alignment.TopEnd,
    ghostOffset: DpOffset = DpOffset(48.dp, 48.dp),
    hue: Color = LocalAppTheme.current.accentColor,
    /** true: kapak durum çubuğunun altına uzanır (hero); ikon durum çubuğu kadar aşağı alınır. */
    underStatusBar: Boolean = false,
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
        // Küçük + sağ-üst köşe: başlık / metrik / çip bölgesiyle çakışmaz (başlıklar kartın alt-orta bölgesinde)
        val sz = minOf(ghostSize, 64.dp, maxHeight * 0.30f)
        val top = (if (underStatusBar) 0.dp else 14.dp) + if (underStatusBar) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else 0.dp
        Icon(
            icon, contentDescription = null, tint = ghost,
            modifier = Modifier.align(ghostAlignment).padding(top = if (ghostAlignment == Alignment.TopEnd) top else 0.dp, end = if (underStatusBar) 10.dp else 14.dp).requiredSize(sz)
        )
        content()
    }
}

/**
 * Program / etkinlik türü: kapak rengi + ikonu bu tek tablodan gelir (yalnız kapaklarda; UI geneline sızmaz).
 * FİTNESS ailesi (Strength mercan / Cardio pembe / Core indigo) varsayılandır; spor türleri yalnız başlıkta/etiketinde spor adı geçince.
 */
enum class CoverType(val hue: Color, val icon: () -> ImageVector) {
    Strength(Color(0xFFFF7A6E), { SinemaIcons.Dumbbell }),
    Cardio(Color(0xFFFF4D8D), { SinemaIcons.Flame }),
    Core(Color(0xFF818CF8), { SinemaIcons.Bolt }),
    Run(Color(0xFFFFB938), { Icons.Rounded.DirectionsRun }),
    Walk(Color(0xFFD4A373), { Icons.Rounded.Hiking }),
    Bike(Color(0xFF2DD4BF), { Icons.Rounded.DirectionsBike }),
    Swim(Color(0xFF38BDF8), { Icons.Rounded.Pool }),
    Rowing(Color(0xFF22D3EE), { Icons.Rounded.Rowing }),
    Football(Color(0xFF4ADE80), { Icons.Rounded.SportsSoccer }),
    Basketball(Color(0xFFF97316), { Icons.Rounded.SportsBasketball }),
    Volleyball(Color(0xFF60A5FA), { Icons.Rounded.SportsVolleyball }),
    Tennis(Color(0xFFC6F135), { Icons.Rounded.SportsTennis }),
    Combat(Color(0xFF94A3B8), { Icons.Rounded.SportsMma }),
    Yoga(Color(0xFFC084FC), { Icons.Rounded.SelfImprovement }),
    Pilates(Color(0xFFE879F9), { Icons.Rounded.SportsGymnastics }),
    Handball(Color(0xFFE11D48), { Icons.Rounded.SportsHandball }),
    Rugby(Color(0xFFA16207), { Icons.Rounded.SportsRugby }),
    Cricket(Color(0xFF65A30D), { Icons.Rounded.SportsCricket }),
    Golf(Color(0xFF059669), { Icons.Rounded.SportsGolf }),
    Baseball(Color(0xFFDC2626), { Icons.Rounded.SportsBaseball }),
    Ski(Color(0xFFF1F5F9), { Icons.Rounded.DownhillSkiing }),
    Surf(Color(0xFF0EA5E9), { Icons.Rounded.Surfing })
}

/** Anahtar kelime tablosu (ASCII-normalize; "=" ile başlayan tam jeton, diğerleri jeton öneki). Sıra = öncelik. */
private val SportKeywords: List<Pair<CoverType, List<String>>> = listOf(
    CoverType.Football to listOf("futbol", "football", "soccer"),
    CoverType.Basketball to listOf("basket"),
    CoverType.Volleyball to listOf("voleybol", "volley"),
    CoverType.Tennis to listOf("tenis", "tennis", "badminton", "padel", "squash", "raket"),
    CoverType.Rowing to listOf("kurek", "rowing", "=row", "=erg", "kano", "kayaking", "canoe"),
    CoverType.Swim to listOf("yuzme", "yuzucu", "swim", "havuz", "=pool"),
    CoverType.Surf to listOf("surf"),
    CoverType.Ski to listOf("kayak", "ski", "snowboard"),
    CoverType.Bike to listOf("bisiklet", "cycl", "bike", "spinning", "pedal"),
    CoverType.Run to listOf("kosu", "kosma", "running", "runner", "=run", "jog", "maraton", "marathon", "sprint"),
    CoverType.Walk to listOf("yuruyus", "yurume", "walking", "=walk", "hiking", "trekking", "=trek", "tirmanis", "climbing"),
    CoverType.Combat to listOf("boks", "=box", "boxing", "kickbox", "dovus", "=mma", "muay", "judo", "karate", "taekwondo", "gures", "wrestl", "jiu", "martial", "krav"),
    CoverType.Pilates to listOf("pilates", "jimnastik", "gymnast", "dans", "dance", "balet", "ballet"),
    CoverType.Yoga to listOf("yoga", "esnek", "stretch", "mobilit", "flexib", "meditasyon"),
    CoverType.Handball to listOf("hentbol", "handball"),
    CoverType.Rugby to listOf("ragbi", "rugby"),
    CoverType.Cricket to listOf("kriket", "cricket"),
    CoverType.Golf to listOf("golf"),
    CoverType.Baseball to listOf("beyzbol", "baseball")
)

/** İkinci öncelik: kategori / fitness alt türleri (spor adı yoksa). Eşleşmeyen HER şey Strength (fitness mercan + dambıl). */
private val CategoryKeywords: List<Pair<CoverType, List<String>>> = listOf(
    CoverType.Cardio to listOf("hiit", "kardiyo", "cardio", "crossfit", "kondisyon", "conditioning", "dayaniklilik", "endurance", "aerobik", "aerobic", "tabata"),
    CoverType.Core to listOf("=core", "karin", "=abs")
)

private fun normalizeCoverKey(s: String): String = buildString {
    for (ch in s.lowercase(java.util.Locale.ROOT)) {
        when (ch) {
            'ı' -> append('i'); 'ş' -> append('s'); 'ğ' -> append('g'); 'ü' -> append('u'); 'ö' -> append('o'); 'ç' -> append('c')
            '\u0307' -> {}   // İ -> i + birleşik nokta
            else -> append(ch)
        }
    }
}

private fun matches(token: String, kw: String) = if (kw.startsWith("=")) token == kw.substring(1) else token.startsWith(kw)

/**
 * Anahtar (başlık + etiketler + kategori; TR+EN) -> tür. Öncelik: 1) spor adı (başlıktaki ilk eşleşen jeton), 2) kategori (kardiyo/core),
 * 3) hepsi değilse FİTNESS = Strength. Hash / rastgele atama YOK. "Atletik / Temel / Hibrit / Güncellenmiş Antrenman Programı" spor değildir.
 */
fun coverTypeFor(key: String?): CoverType {
    val tokens = normalizeCoverKey(key.orEmpty()).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    for (t in tokens) SportKeywords.firstOrNull { (_, kws) -> kws.any { matches(t, it) } }?.let { return it.first }
    for (t in tokens) CategoryKeywords.firstOrNull { (_, kws) -> kws.any { matches(t, it) } }?.let { return it.first }
    return CoverType.Strength
}

/** Kapak tek rengi (türe göre; opak düz yüzey için). */
fun coverHueFor(key: String?): Color = coverTypeFor(key).hue

/** Kapak hayalet ikonu (türe göre belirgin ikon). */
fun coverIconFor(key: String?): ImageVector = coverTypeFor(key).icon()
