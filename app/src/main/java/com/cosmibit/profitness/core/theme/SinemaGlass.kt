package com.cosmibit.profitness.core.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * SİNEMA camı: tek yer. `sinemaGlass` bağımlılıksız (Haze YOK) — yarı saydam koyu dolgu + kenar + üst parlama + gölge.
 * Haze gelirse yalnız bu iki fonksiyonun içi değişir.
 * Cam YALNIZ foto/gradyan üstünde kullanılır.
 */
fun Modifier.sinemaGlass(shape: Shape, fillAlpha: Float = 0.70f): Modifier = this
    .shadow(16.dp, shape, ambientColor = Color.Black.copy(0.40f), spotColor = Color.Black.copy(0.40f))
    .clip(shape)
    .background(Sinema.Ink.copy(alpha = fillAlpha))
    .border(1.dp, Color.White.copy(0.18f), shape)
    .drawWithContent {
        drawContent()
        val line = 1.dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent, 0.15f to Color.White.copy(0.35f),
                0.85f to Color.White.copy(0.35f), 1f to Color.Transparent
            ),
            topLeft = Offset(0f, line * 0.5f),
            size = androidx.compose.ui.geometry.Size(size.width, line)
        )
    }

/** Haze kaynağı: foto sayfa katmanı sağlar; kapsül/hap camı bununla arka planı bulanıklaştırır (yoksa düz yarı saydam dolgu). */
val LocalSinemaHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * KALIN BUZLU CAM KAPSÜL (tek yer): arka plan bulanık + düşük tint, üst parlama (sheen) + üstten alta sönen degrade kenar,
 * alt iç gölge ve dışa düşen yumuşak alt gölge (kabarık görünüm). Dolu/seçili hal: `fill` = vurgu rengi, `glow` = sinemaGlow rengi.
 * Haze yoksa/API<31 ise bulanıklık yerine biraz daha koyu düz tint. Tint rengi `tint` (açık temada Color.White).
 */
fun Modifier.sinemaCapsuleGlass(
    shape: Shape,
    edge: Color = Color.White.copy(0.18f),
    fillAlpha: Float = 0.35f,
    blur: Dp = 20.dp,
    fill: Color? = null,
    glow: Color? = null,
    tint: Color = Color.Black
): Modifier = composed {
    val haze = LocalSinemaHaze.current
    val blurOk = haze != null && Build.VERSION.SDK_INT >= 31
    val light = tint.luminance() > 0.5f
    Modifier
        // dışa kabarık: şeklin DIŞINA düşen alt gölge (içine sızmaz)
        .drawBehind {
            val path = shapePath(shape, size, layoutDirection, this)
            clipPath(path, ClipOp.Difference) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        this.color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(9.dp.toPx(), 0f, 4.dp.toPx(), Color.Black.copy(if (light) 0.22f else 0.50f).toArgb())
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                }
            }
        }
        .then(if (glow != null) Modifier.sinemaGlow(glow, 20.dp, 8.dp, shape) else Modifier)
        .clip(shape)
        .then(
            when {
                fill != null -> Modifier.background(fill)
                blurOk -> Modifier.hazeEffect(state = haze!!) {
                    blurRadius = blur
                    backgroundColor = tint
                    tints = listOf(HazeTint(tint.copy(alpha = fillAlpha)))
                    noiseFactor = 0f
                }
                else -> Modifier.background(tint.copy(alpha = (fillAlpha + 0.22f).coerceAtMost(0.7f)))
            }
        )
        .drawBehind {
            val h = size.height
            val sheenA = if (fill != null) 0.28f else if (light) 0.55f else 0.16f
            // üst parlama: üstten ~%55'e sönen beyaz
            drawRect(Brush.verticalGradient(0f to Color.White.copy(sheenA), 0.55f to Color.Transparent, startY = 0f, endY = h))
            // alt iç gölge: kalınlık hissi
            drawRect(Brush.verticalGradient(0.50f to Color.Transparent, 1f to Color.Black.copy(if (fill != null) 0.16f else if (light) 0.07f else 0.22f), startY = 0f, endY = h))
            // degrade kenar: üstte parlak, altta sönük (çizgi düz değil)
            val topA = (edge.alpha * 2.2f).coerceAtMost(1f)
            drawPath(
                shapePath(shape, size, layoutDirection, this),
                Brush.verticalGradient(0f to edge.copy(topA), 0.5f to edge.copy(edge.alpha * 0.7f), 1f to edge.copy(edge.alpha * 0.45f), startY = 0f, endY = h),
                style = Stroke(2.dp.toPx())
            )
        }
}

private fun shapePath(shape: Shape, size: Size, dir: androidx.compose.ui.unit.LayoutDirection, d: androidx.compose.ui.unit.Density): Path {
    val outline = shape.createOutline(size, dir, d)
    return Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
}

// ── Menü durumu ──────────────────────────────────────────────────────────────

/**
 * Antrenman pager'ı `pagerOwned` olur ve `pagerBusy` ile kaydırma durumunu bildirir; menü kaydırma sırasında
 * kısa süre saklanır (çizimdeki `.nv3.hide`). Diğer sayfalarda menü hep görünür.
 */
@Stable
class SinemaNavState {
    var scrollHidden by mutableStateOf(false)
    var pagerOwned by mutableStateOf(false)
    var pagerBusy by mutableStateOf(false)
    /** AI Koç kurulum gibi açık düz zeminli alt sayfa: menü foto değil sayfa rengi alır. */
    var lightPage by mutableStateOf(false)
    val hidden: Boolean get() = if (pagerOwned) pagerBusy else scrollHidden
}

val LocalSinemaNav = staticCompositionLocalOf { SinemaNavState() }

@Immutable
data class SinemaNavItem(val icon: ImageVector, val label: String)

/** Çizimdeki menü yüksekliği (sistem çubuğu hariç). */
val SinemaNavHeight = 60.dp

/** Menü camının yukarı doğru yumuşakça söndüğü yükseklik. */
val NavFadeHeight = 56.dp

/**
 * Çizimdeki alt menü (.nv3): 60dp, 5 eşit sütun, üstte 20dp simge + 3dp + 10sp etiket (hepsinde etiket var, nokta/kutu yok).
 * `onPhoto` = foto sayfası (Antrenman/AI Koç/Keşfet): zemin şeffaf, beyaz %70, seçili beyaz.
 * Değilse düz zemin sayfası (Programlar/Profil): zemin sayfa rengi, ikincil metin, seçili ana metin.
 * Seçili simge her iki durumda vurgu renginde.
 */
@Composable
fun SinemaNavBar(
    items: List<SinemaNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    visible: Boolean,
    onPhoto: Boolean,
    modifier: Modifier = Modifier,
    haze: HazeState? = null
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val hideProgress by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(Sinema.NavMs, easing = FastOutSlowInEasing),
        label = "sinema_nav_hide"
    )
    if (hideProgress >= 1f) return

    val bg by animateColorAsState(
        if (onPhoto) Color.Black.copy(0.55f) else Sinema.page(theme), tween(250), label = "nav_bg"
    )
    val passive by animateColorAsState(
        if (onPhoto) Color.White.copy(0.7f) else Sinema.textSecondary(theme), tween(250), label = "nav_passive"
    )
    val active by animateColorAsState(
        if (onPhoto) Color.White else Sinema.text(theme), tween(250), label = "nav_active"
    )

    val blurOk = haze != null && Build.VERSION.SDK_INT >= 31
    val navH = SinemaNavHeight + inset
    val fade = NavFadeHeight
    Box(
        modifier
            .fillMaxWidth()
            .height(navH)
            .graphicsLayer {
                translationY = hideProgress * 10.dp.toPx()
                alpha = 1f - hideProgress
            }
    ) {
        // Gerçek buzlu cam: menüden ~56dp yukarı taşan katman; blur + düşük tint, üstte yumuşak maske ile söner (keskin kenar yok).
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .requiredHeight(navH + fade)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val f = fade.toPx()
                    val fr = f / size.height
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(0f), fr * 0.30f to Color.Black.copy(0.10f),
                            fr * 0.62f to Color.Black.copy(0.50f), fr * 0.86f to Color.Black.copy(0.88f), fr to Color.Black, 1f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .then(
                    if (blurOk) Modifier.hazeEffect(state = haze!!) {
                        blurRadius = 32.dp
                        backgroundColor = bg.copy(alpha = 1f)
                        tints = listOf(HazeTint(bg.copy(alpha = 0.30f)))
                        noiseFactor = 0f
                    } else Modifier.background(bg.copy(alpha = if (onPhoto) 0.62f else 0.72f))
                )
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = inset)) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = selected, indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Tab, onClick = { onSelect(i) })
                    .padding(top = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    item.icon, contentDescription = null,
                    tint = if (selected) accent else passive,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label, style = Sinema.Nav.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = if (selected) active else passive,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
        }
    }
}

/**
 * Gerçek yumuşak renkli parlama (çizimdeki `box-shadow: 0 dy blur color`): şeklin arkasına bulanık gölge katmanı çizer.
 * `Modifier.shadow` renk/spot'u cihazda zayıf kaldığı için glow bununla yapılır. Eleman içeriği üstüne çizer, şeklin içi görünmez.
 */
fun Modifier.sinemaGlow(color: Color, blur: Dp, dy: Dp = 0.dp, shape: Shape): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = androidx.compose.ui.graphics.Path().apply {
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
            is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
            is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
        }
    }
    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(blur.toPx(), 0f, dy.toPx(), color.toArgb())
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}
