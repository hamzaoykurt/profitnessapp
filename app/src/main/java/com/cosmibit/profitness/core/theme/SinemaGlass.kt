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
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * SİNEMA camı: tek yer. `sinemaGlass` bağımlılıksız (Haze YOK) — yarı saydam koyu dolgu + kenar + üst parlama + gölge.
 * Haze gelirse yalnız bu iki fonksiyonun içi değişir.
 * Cam YALNIZ foto/gradyan üstünde kullanılır.
 */
fun Modifier.sinemaGlass(shape: Shape, fillAlpha: Float = 0.70f): Modifier = this
    .shadow(16.dp, shape, ambientColor = Sinema.Page.copy(0.40f), spotColor = Sinema.Page.copy(0.40f))
    .clip(shape)
    .background(Sinema.Surface.copy(alpha = fillAlpha))   // siyah/Ink tint yok (yon-cam-siyah)
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
 * Cam blur'ları yarı çözünürlükte hesaplanır (piksel sayısı 1/4): 16-32dp bulanıklıkta fark görünmez, GPU yükü düşer.
 * Görsel sorun çıkarsa tek satır: HazeInputScale.None.
 */
@OptIn(ExperimentalHazeApi::class)
private val SinemaHazeScale: HazeInputScale = HazeInputScale.Fixed(0.5f)

/**
 * CAM C (tek yer, .ekip/yon-cam-siyah.md "Genel cam tonu" + mockup `.glass`/`.stone`): siyah tint YOK, cam = beyaz ışık.
 * - Dolgu: beyaz dikey degrade [fillTop → fillBottom] (varsayılan .14 → .08); `overlay` varsa üstüne düz örtü (ör. vurgu %18).
 * - Kenar 1.5dp: varsayılan 165° beyaz .50/.14 (%38)/.06 (%72)/.22; `edgeColors` verilirse dikey degrade (tek renk = düz).
 * - İç üst ışık (1dp beyaz .34 + üstten 14dp sönen .22), iç alt gölge (sayfa rengi .22), dış gölge 0 6 14 sayfa .45.
 * - `fill` (seçili taş `.stone`): düz dolgu + iç ışık .35 + iç alt gölge .18; dış gölge yerine `glow` (ör. vurgu .35).
 * - Haze kaynağı varsa (foto üstü) + API≥31 arkayı bulanıklaştırır; yoksa aynı dolgu, yalnız blur yok.
 * Tüm çizim drawWithCache içinde (boyut/şekil değişene kadar Path/Brush önbellekte).
 */
fun Modifier.sinemaCapsuleGlass(
    shape: Shape,
    blur: Dp = 24.dp,
    fill: Color? = null,
    glow: Color? = null,
    fillTop: Float = 0.14f,
    fillBottom: Float = 0.08f,
    overlay: Color? = null,
    edgeColors: List<Color>? = null,
    shadow: Boolean = fill == null,
    /** Geriye uyum: düz kenar rengi (ör. odakta vurgu). Eski varsayılan beyaz .18 → yeni 165° degrade kenar. */
    edge: Color? = null,
    /** Geriye uyum: eski siyah tint alfası; YOK SAYILIR (cam = beyaz ışık). */
    @Suppress("UNUSED_PARAMETER") fillAlpha: Float? = null
): Modifier = composed {
    val edgeColors = edgeColors ?: edge?.takeIf { it != LegacyGlassEdge }?.let { listOf(it) }
    val haze = LocalSinemaHaze.current
    val blurOk = fill == null && haze != null && Build.VERSION.SDK_INT >= 31
    Modifier
        // dış gölge: şeklin DIŞINA düşer (içine sızmaz) — sayfa rengi, siyah değil
        .then(if (shadow) Modifier.drawWithCache {
            val path = shapePath(shape, size, layoutDirection, this)
            val androidPath = path.asAndroidPath()
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(14.dp.toPx(), 0f, 6.dp.toPx(), Sinema.Page.copy(0.45f).toArgb())
            }
            onDrawBehind {
                clipPath(path, ClipOp.Difference) {
                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPath(androidPath, paint) }
                }
            }
        } else Modifier)
        .then(if (glow != null) Modifier.sinemaGlow(glow, 18.dp, 6.dp, shape) else Modifier)
        .clip(shape)
        .then(
            if (blurOk) Modifier.hazeEffect(state = haze!!) {
                blurRadius = blur
                @OptIn(ExperimentalHazeApi::class)
                run { inputScale = SinemaHazeScale }
                backgroundColor = Sinema.Page
                // dolgu aşağıda çiziliyor; neredeyse saydam tint Haze'in varsayılan koyu tintini devre dışı bırakır
                tints = listOf(HazeTint(Color.White.copy(alpha = 0.01f)))
                noiseFactor = 0f
            } else Modifier
        )
        .drawWithCache {
            val w = size.width
            val h = size.height
            val path = shapePath(shape, size, layoutDirection, this)
            val shifted = Path().apply { addPath(path, Offset(0f, 1.dp.toPx())) }
            val fillBrush: Brush = if (fill != null) androidx.compose.ui.graphics.SolidColor(fill)
                else Brush.verticalGradient(0f to Color.White.copy(fillTop), 1f to Color.White.copy(fillBottom), startY = 0f, endY = h)
            val topLineColor = Color.White.copy(if (fill != null) 0.35f else 0.34f)
            val sheen = if (fill == null) Brush.verticalGradient(
                0f to Color.White.copy(0.22f), 1f to Color.Transparent, startY = 0f, endY = 14.dp.toPx().coerceAtMost(h)
            ) else null
            val innerH = (if (fill != null) 6.dp else 8.dp).toPx().coerceAtMost(h)
            val inner = Brush.verticalGradient(
                0f to Color.Transparent, 1f to Sinema.Page.copy(if (fill != null) 0.18f else 0.22f),
                startY = h - innerH, endY = h
            )
            val edgeBrush: Brush? = when {
                fill != null -> null
                edgeColors != null && edgeColors.size == 1 -> androidx.compose.ui.graphics.SolidColor(edgeColors[0])
                edgeColors != null -> Brush.verticalGradient(edgeColors, startY = 0f, endY = h)
                else -> {
                    // CSS 165°: yön (sin165, -cos165) ekran koordinatında; çizgi uzunluğu |w·sin|+|h·cos|
                    val rad = Math.toRadians(165.0)
                    val dx = kotlin.math.sin(rad).toFloat()
                    val dy = (-kotlin.math.cos(rad)).toFloat()
                    val half = (kotlin.math.abs(w * dx) + kotlin.math.abs(h * dy)) / 2f
                    val c = Offset(w / 2f, h / 2f)
                    Brush.linearGradient(
                        0f to Color.White.copy(0.50f), 0.38f to Color.White.copy(0.14f),
                        0.72f to Color.White.copy(0.06f), 1f to Color.White.copy(0.22f),
                        start = Offset(c.x - dx * half, c.y - dy * half), end = Offset(c.x + dx * half, c.y + dy * half)
                    )
                }
            }
            // kenar şeklin içine 1.5dp düşsün: 3dp çizgi, dış yarısı clip ile kesilir
            val edgeStroke = Stroke(3.dp.toPx())
            onDrawBehind {
                drawRect(fillBrush)
                overlay?.let { drawRect(it) }
                sheen?.let { drawRect(it) }
                drawRect(inner, topLeft = Offset(0f, h - innerH), size = Size(w, innerH))
                // iç üst ışık: şeklin üst kenarını izleyen 1dp hilal
                clipPath(shifted, ClipOp.Difference) { drawPath(path, topLineColor) }
                edgeBrush?.let { drawPath(path, it, style = edgeStroke) }
            }
        }
}

/** Eski kapsül varsayılan kenarı; verilirse yeni degrade kenar kullanılır. */
private val LegacyGlassEdge = Color.White.copy(0.18f)

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
        animationSpec = tween(Sinema.NavMs, easing = SinemaMotion.EaseStandard),
        label = "sinema_nav_hide"
    )
    if (hideProgress >= 1f) return

    val bg by animateColorAsState(
        if (onPhoto) Sinema.scrim(0.55f) else Sinema.page(theme), tween(SinemaMotion.Standard), label = "nav_bg"
    )
    val passive by animateColorAsState(
        if (onPhoto) Color.White.copy(0.7f) else Sinema.textSecondary(theme), tween(SinemaMotion.Standard), label = "nav_passive"
    )
    val active by animateColorAsState(
        if (onPhoto) Color.White else Sinema.text(theme), tween(SinemaMotion.Standard), label = "nav_active"
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
                        @OptIn(ExperimentalHazeApi::class)
                        run { inputScale = SinemaHazeScale }
                        backgroundColor = bg.copy(alpha = 1f)
                        tints = listOf(HazeTint(bg.copy(alpha = 0.30f)))
                        noiseFactor = 0f
                    } else Modifier.background(bg.copy(alpha = if (onPhoto) 0.62f else 0.72f))
                )
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = inset)) {
        val navHaptic = LocalHapticFeedback.current
        val navMotion = rememberSinemaMotionOn()
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            // Seçilen ikon bir kez hafifçe "oturur" (0.86 → 1, tek küçük taşma); renk kısa solmayla geçer.
            val pop = remember { Animatable(1f) }
            var wasSelected by remember { mutableStateOf(selected) }
            LaunchedEffect(selected) {
                if (selected && !wasSelected && navMotion) {
                    pop.snapTo(0.86f)
                    pop.animateTo(1f, SinemaMotion.pop())
                }
                wasSelected = selected
            }
            val iconTint by animateColorAsState(if (selected) accent else passive, tween(SinemaMotion.Quick), label = "nav_icon")
            val labelTint by animateColorAsState(if (selected) active else passive, tween(SinemaMotion.Quick), label = "nav_label")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = selected, indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Tab, onClick = {
                            if (!selected) navHaptic.performHapticFeedback(SinemaHaptics.Light)
                            onSelect(i)
                        })
                    .padding(top = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    item.icon, contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label, style = Sinema.Nav.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = labelTint,
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
fun Modifier.sinemaGlow(color: Color, blur: Dp, dy: Dp = 0.dp, shape: Shape): Modifier = this.drawWithCache {
    val androidPath = shapePath(shape, size, layoutDirection, this).asAndroidPath()
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.TRANSPARENT
        setShadowLayer(blur.toPx(), 0f, dy.toPx(), color.toArgb())
    }
    onDrawBehind { drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPath(androidPath, paint) } }
}
