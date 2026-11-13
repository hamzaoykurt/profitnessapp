package com.cosmibit.profitness.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Shared 8pt-based product tokens for the Performance Luxury UI. */
@Immutable
object PerformanceSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val huge = 48.dp
    val display = 64.dp
}

@Immutable
object PerformanceRadius {
    val control = 14.dp
    val button = 16.dp
    val card = 20.dp
    val major = 24.dp
}

object PerformanceMotion {
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val fast = tween<Float>(SinemaMotion.Quick, easing = standardEasing)
    val standard = tween<Float>(SinemaMotion.Standard, easing = standardEasing)
}

/** Opaque content surface. Borders are intentionally omitted; hierarchy is tonal. */
fun Modifier.performanceSurface(
    theme: AppThemeState,
    level: Int = 1,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    @Suppress("UNUSED_PARAMETER") elevated: Boolean = false
): Modifier {
    val color = when (level.coerceIn(1, 3)) {
        1 -> theme.bg1
        2 -> theme.bg2
        else -> theme.bg3
    }
    return this
        .clip(shape)
        .background(color)
}

private val WarmLight = Color(0xFFFFFFFF)
private typealias Stops = Array<Pair<Float, Color>>

/**
 * Single cached paint pass for every layered surface: fill, vertical light, optional accent
 * reflection, 1dp top line, optional hero bar, then a 1dp vertical-gradient edge over the content.
 * No saveLayer, no Modifier.border(brush), no blur.
 */
private fun Modifier.layeredMaterial(
    shape: Shape,
    fill: Color,
    light: Stops,
    topLine: Color,
    edge: Stops?,
    reflection: Color? = null,
    reflectionScale: Float = 1.1f,
    barAccent: Color? = null
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val w = size.width
        val h = size.height
        val lightBrush = Brush.verticalGradient(colorStops = light, startY = 0f, endY = h)
        val reflectionBrush = reflection?.let {
            Brush.radialGradient(
                colors = listOf(it, it.copy(alpha = 0f)),
                center = Offset(w * 1.05f, -h * 0.25f),
                radius = maxOf(w, h) * reflectionScale
            )
        }
        val edgeBrush = edge?.let { Brush.verticalGradient(colorStops = it, startY = 0f, endY = h) }
        val outline = if (edgeBrush != null) shape.createOutline(size, layoutDirection, this) else null
        val lineH = 1.dp.toPx()
        val barW = 3.dp.toPx()
        val barBrush = barAccent?.let {
            Brush.verticalGradient(
                listOf(it, it.copy(alpha = 0.55f)),
                startY = h * 0.18f,
                endY = h * 0.82f
            )
        }
        onDrawWithContent {
            drawRect(fill)
            drawRect(lightBrush)
            if (reflectionBrush != null) drawRect(reflectionBrush)
            drawRect(topLine, size = Size(w, lineH))
            if (barBrush != null) {
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(0f, h * 0.18f),
                    size = Size(barW, h * 0.64f),
                    cornerRadius = CornerRadius(barW)
                )
            }
            drawContent()
            if (edgeBrush != null && outline != null) {
                // clipped to the shape, so a 2dp centred stroke leaves exactly 1dp inside
                drawOutline(outline, edgeBrush, style = Stroke(lineH * 2f))
            }
        }
    }

/** Translucent card base so the page atmosphere tints the glass. */
private fun glassFill(): Color = Sinema.Surface

private fun cardLight(): Stops = arrayOf(
    0f to WarmLight.copy(0.08f), 0.45f to WarmLight.copy(0f),
    0.70f to Color.Black.copy(0f), 1f to Color.Black.copy(0.26f)
)

private fun cardEdge(): Stops = arrayOf(
    0f to Color.White.copy(0.20f), 0.5f to Color.White.copy(0.10f), 1f to Color.White.copy(0.06f)
)

/**
 * Layered opaque card: top-to-bottom tone, 1dp top highlight, 1dp gradient edge, faint accent
 * reflection (alpha capped at .06 for contrast), one soft shadow.
 */
fun Modifier.performanceElevatedSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    elevated: Boolean = true,
    accent: Color = Color.Unspecified,
    glowStrength: Float = 1f
): Modifier {
    val acc = if (accent == Color.Unspecified) theme.effectiveAccentColor else accent
    val shadowColor = Color.Black.copy(0.30f)
    return this
        .then(
            if (elevated) Modifier.shadow(
                4.dp, shape,
                ambientColor = shadowColor, spotColor = shadowColor
            ) else Modifier
        )
        .layeredMaterial(
            shape = shape,
            fill = glassFill(),
            light = cardLight(),
            topLine = WarmLight.copy(0.16f),
            edge = cardEdge(),
            reflection = acc.copy(0.02f * glowStrength.coerceIn(0f, 1f))
        )
}

/** Flat control: secondary (sheen + top line) or primary (filled accent, accent spot shadow). */
fun Modifier.performanceControlSurface(
    theme: AppThemeState,
    color: Color,
    pressed: Boolean,
    shape: Shape = RoundedCornerShape(PerformanceRadius.button),
    primary: Boolean = false
): Modifier {
    val shadowDp = if (pressed) 0.dp else if (primary) 5.dp else 2.dp
    val spot = when {
        primary -> color.copy(0.22f * color.alpha)
        else -> Color.Black.copy(0.30f)
    }
    val ambient = if (primary) Color.Transparent else spot
    val light: Stops = when {
        pressed -> arrayOf(0f to Color.Black.copy(0.14f), 1f to Color.Black.copy(0.14f))
        primary -> arrayOf(
            0f to Color.White.copy(0.14f), 0.55f to Color.White.copy(0f),
            1f to Color.Black.copy(0.10f)
        )
        else -> arrayOf(
            0f to Color.White.copy(0.10f), 0.5f to Color.White.copy(0f),
            1f to Color.White.copy(0f)
        )
    }
    val line = when {
        pressed -> Color.Transparent
        primary -> Color.White.copy(0.22f)
        else -> Color.White.copy(0.14f)
    }
    return this
        .then(if (primary && !pressed) Modifier.sinemaGlow(color.copy(0.40f * color.alpha), 18.dp, 8.dp, shape) else Modifier.shadow(shadowDp, shape, ambientColor = ambient, spotColor = spot))
        .layeredMaterial(shape, color, light, line, edge = null)
}

/** Recessed well (set boxes, day strip, XP well, ring track): inner top shadow + bottom hairline. */
fun Modifier.performanceInsetSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier {
    return this
        .clip(shape)
        .drawWithCache {
            val h = size.height
            val shade = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(0.28f), 0.35f to Color.Black.copy(0f), 1f to Color.Black.copy(0f)
                ),
                startY = 0f, endY = h
            )
            val fill = Color(0xFF0B0B0D)
            val bottom = WarmLight.copy(0.05f)
            val lineH = 1.dp.toPx()
            onDrawBehind {
                drawRect(fill)
                drawRect(shade)
                drawRect(bottom, topLeft = Offset(0f, h - lineH), size = Size(size.width, lineH))
            }
        }
}

/**
 * Floating nav capsule: kalın cam levha. Taban + dikey ışık (üst beyaz α.12 → .45 şeffaf → alt siyah α.22),
 * ortada yoğun/uçlarda kaybolan 1dp üst parlama, 6dp iç üst gölge, 1dp alt rim ışığı (vurgu, ortada yoğun),
 * gradyan dış kenar. Tek drawWithCache; blur/saveLayer/border(brush) yok.
 */
fun Modifier.performanceNavSurface(
    theme: AppThemeState,
    shape: Shape,
    accent: Color = Color.Unspecified
): Modifier {
    val acc = if (accent == Color.Unspecified) theme.effectiveAccentColor else accent
    return this
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val lineH = 1.dp.toPx()
            val fill = Color(0xFF221D19).copy(alpha = 0.72f)
            val light = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(0.12f), 0.45f to Color.White.copy(0f),
                    0.7f to Color.Black.copy(0f), 1f to Color.Black.copy(0.22f)
                ),
                startY = 0f, endY = h
            )
            val inner = Brush.verticalGradient(
                listOf(Color.Black.copy(0.25f), Color.Transparent),
                startY = 0f, endY = 6.dp.toPx()
            )
            val topLine = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(0.45f), Color.Transparent),
                startX = 0f, endX = w
            )
            val rim = Brush.horizontalGradient(
                listOf(
                    Color.Transparent, acc.copy(0.22f), Color.Transparent
                ),
                startX = w * 0.08f, endX = w * 0.92f
            )
            val edge = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(0.28f), 1f to Color.White.copy(0.05f)
                ),
                startY = 0f, endY = h
            )
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawRect(fill)
                drawRect(light)
                drawRect(inner, size = Size(w, 6.dp.toPx()))
                drawRect(topLine, topLeft = Offset(0f, 0f), size = Size(w, lineH))
                drawRect(rim, topLeft = Offset(0f, h - lineH), size = Size(w, lineH))
                drawContent()
                drawOutline(outline, edge, style = Stroke(lineH * 2f))
            }
        }
}

/** Hero/selected material (streak band, active program): card recipe, wider reflection, accent bar. */
fun Modifier.performanceSignatureSurface(
    theme: AppThemeState,
    accent: Color,
    shape: Shape = RoundedCornerShape(PerformanceRadius.major)
): Modifier {
    val shadowColor = Color.Black.copy(0.30f)
    return this
        .shadow(
            4.dp, shape,
            ambientColor = shadowColor, spotColor = shadowColor
        )
        .layeredMaterial(
            shape = shape,
            fill = theme.bg2,
            light = cardLight(),
            topLine = WarmLight.copy(0.16f),
            edge = cardEdge(),
            reflection = accent.copy(0.02f),
            reflectionScale = 1.4f,
            barAccent = accent
        )
}

// ── Ortak malzeme API'si (cam / kabarık / gömme) ─────────────────────────────
// Blur yok, saveLayer yok, Modifier.border(brush) yok; yüzey başına tek drawWithCache.

private val GlassInk = Color(0xFF0E0F11)

/**
 * Fotoğraf üstü cam çip/disk: koyu taban + beyaz α.16→.10 dikey geçiş, 1dp üst çizgi (α.30),
 * gradyan kenar (üst α.28 → alt α.06). Fotoğraf iki temada da karartılı olduğundan tema bağımsız.
 * Kullanım: `Row(Modifier.height(32.dp).glassChipSurface()) { ... }`
 */
fun Modifier.glassChipSurface(shape: Shape = CircleShape): Modifier = layeredMaterial(
    shape = shape,
    fill = GlassInk.copy(alpha = 0.58f),
    light = arrayOf(0f to Color.White.copy(0.16f), 1f to Color.White.copy(0.10f)),
    topLine = Color.White.copy(0.30f),
    edge = arrayOf(0f to Color.White.copy(0.28f), 1f to Color.White.copy(0.06f))
)

/**
 * Kabarık birincil düğme/hap: vurgu dolgusu, üstten beyaz α.22→şeffaf parlama, alt kenarda koyu ton
 * (siyah α.18), 1dp üst çizgi, vurgu renkli gölge. `pressed` = gömülür (gölge 0, parlama ters).
 * Kullanım: `Box(Modifier.size(68.dp).raisedButtonSurface(theme, pressed = isPressed)) { ... }`
 */
fun Modifier.raisedButtonSurface(
    theme: AppThemeState,
    pressed: Boolean = false,
    color: Color = Color.Unspecified,
    shape: Shape = CircleShape,
    lift: Dp = 6.dp
): Modifier {
    val c = if (color == Color.Unspecified) theme.effectiveAccentColor else color
    val spot = c.copy(0.30f)
    val light: Stops = if (pressed) arrayOf(
        0f to Color.Black.copy(0.16f), 0.5f to Color.Black.copy(0f), 1f to Color.White.copy(0.14f)
    ) else arrayOf(
        0f to Color.White.copy(0.22f), 0.5f to Color.White.copy(0f),
        0.72f to Color.Black.copy(0f), 1f to Color.Black.copy(0.18f)
    )
    return this
        .then(if (pressed) Modifier else Modifier.sinemaGlow(c.copy(0.40f), (lift * 3).coerceAtLeast(10.dp), lift, shape))
        .layeredMaterial(shape, c, light, if (pressed) Color.Transparent else Color.White.copy(0.30f), edge = null)
}

/**
 * Seçili gün / segment kapsülü: raisedButtonSurface'in hafif gölgeli hali.
 * Kullanım: `Box(Modifier.height(40.dp).raisedPillSurface(theme))`
 */
fun Modifier.raisedPillSurface(
    theme: AppThemeState,
    pressed: Boolean = false,
    color: Color = Color.Unspecified,
    shape: Shape = CircleShape
): Modifier = raisedButtonSurface(theme, pressed, color, shape, lift = 2.dp)

/**
 * Gömme ray/kuyu: üstten siyah α.28→şeffaf (.35), altta 1dp beyaz α.05 (açıkta 3A2A1A α.07 / beyaz).
 * `fill` verilmezse tema kuyu rengi. Kullanım: `Row(Modifier.height(48.dp).insetTrackSurface(theme)) { ... }`
 */
fun Modifier.insetTrackSurface(
    theme: AppThemeState,
    shape: Shape = CircleShape,
    fill: Color = Color.Unspecified
): Modifier {
    val base = if (fill == Color.Unspecified) Color(0xFF0B0B0D) else fill
    return clip(shape).drawWithCache {
        val h = size.height
        val shade = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Black.copy(0.28f), 0.35f to Color.Black.copy(0f), 1f to Color.Black.copy(0f)
            ),
            startY = 0f, endY = h
        )
        val bottom = Color.White.copy(0.05f)
        val lineH = 1.dp.toPx()
        onDrawBehind {
            drawRect(base)
            drawRect(shade)
            drawRect(bottom, topLeft = Offset(0f, h - lineH), size = Size(size.width, lineH))
        }
    }
}

/**
 * Cam kart (satır/kart): performanceElevatedSurface malzemesi; `highlighted` = vurgu tonlu kenar
 * (α.40) ve hafif iç yansıma, çubuk yok. Kullanım: `Row(Modifier.glassCardSurface(theme, RoundedCornerShape(18.dp)))`
 */
fun Modifier.glassCardSurface(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(PerformanceRadius.card),
    highlighted: Boolean = false,
    accent: Color = Color.Unspecified
): Modifier {
    if (!highlighted) return performanceElevatedSurface(theme, shape, accent = accent)
    val acc = if (accent == Color.Unspecified) theme.effectiveAccentColor else accent
    val shadowColor = Color.Black.copy(0.30f)
    return this
        .shadow(4.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
        .layeredMaterial(
            shape = shape,
            fill = glassFill(),
            light = cardLight(),
            topLine = WarmLight.copy(0.16f),
            edge = arrayOf(0f to acc.copy(0.40f), 0.5f to acc.copy(0.28f), 1f to acc.copy(0.12f)),
            reflection = acc.copy(0.10f)
        )
}

/**
 * Gömme küçük görsel çerçevesi: içerik üstüne 1.5dp iç gölge + ince kenar. Köşeyi kırpar.
 * Kullanım: `AsyncImage(..., modifier = Modifier.size(56.dp).insetFrame(theme, RoundedCornerShape(14.dp)))`
 */
fun Modifier.insetFrame(
    theme: AppThemeState,
    shape: Shape = RoundedCornerShape(PerformanceRadius.control)
): Modifier = clip(shape).drawWithCache {
    val inner = Color.Black.copy(0.50f)
    val rim = Color.White.copy(0.10f)
    val top = Brush.verticalGradient(
        listOf(inner, inner.copy(alpha = 0f)), startY = 0f, endY = 6.dp.toPx()
    )
    val outline = shape.createOutline(size, layoutDirection, this)
    val sw = 1.5.dp.toPx()
    onDrawWithContent {
        drawContent()
        drawRect(top, size = Size(size.width, 6.dp.toPx()))
        drawOutline(outline, rim, style = Stroke(sw * 2f))
    }
}

/**
 * Gradyan kahraman/poster zemini (görsel yokken): vurgu %55 → %18 → şeffaf, 135° (sol-üst → sağ-alt).
 * Açık temada [lightStart] → [lightEnd] → şeffaf. Kullanım: `Modifier.background(heroGradientBrush(theme))`
 */
fun heroGradientBrush(
    theme: AppThemeState,
    @Suppress("UNUSED_PARAMETER") lightStart: Float = 0.30f,
    @Suppress("UNUSED_PARAMETER") lightEnd: Float = 0.08f
): Brush {
    val acc = theme.effectiveAccentColor
    return Brush.linearGradient(listOf(acc.copy(0.55f), acc.copy(0.18f), Color.Transparent))
}

/** Dev rakam stili (64sp): Sora ExtraBold, tabular rakam, harf aralığı -2%. Renk/boyut çağıranda. */
val BigNumberStyle = TextStyle(
    fontSize = 64.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum",
    letterSpacing = (-0.02).em,
    lineHeight = 64.sp
)

/** Koyu cam çip/disk üstündeki metin ve ikon rengi (cam iki temada da koyu). */
val OnGlassColor = Color(0xFFF8F4EE)
