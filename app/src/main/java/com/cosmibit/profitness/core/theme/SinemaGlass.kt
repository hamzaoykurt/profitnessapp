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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
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
 * Kapsül camı (foto üstü set satırı / hap / segment): 000000 %35 + arka plan bulanık 20dp + 1dp FFFFFF %18 kenar
 * (çizimdeki `rgba(0,0,0,.35)` + `backdrop-filter:blur`). `fillAlpha`/`blur` değişebilir (hap 16dp, menü .6).
 */
fun Modifier.sinemaCapsuleGlass(
    shape: Shape,
    edge: Color = Color.White.copy(0.18f),
    fillAlpha: Float = 0.35f,
    blur: Dp = 20.dp
): Modifier = composed {
    val haze = LocalSinemaHaze.current
    Modifier
        .clip(shape)
        .then(
            if (haze != null) Modifier.hazeEffect(state = haze) {
                blurRadius = blur
                backgroundColor = Color.Black
                tints = listOf(HazeTint(Color.Black.copy(alpha = fillAlpha)))
                noiseFactor = 0f
            } else Modifier.background(Color.Black.copy(alpha = fillAlpha))
        )
        .border(1.dp, edge, shape)
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

    Row(
        modifier
            .fillMaxWidth()
            .height(SinemaNavHeight + inset)
            .graphicsLayer {
                translationY = hideProgress * 10.dp.toPx()
                alpha = 1f - hideProgress
            }
            .then(
                if (haze != null) Modifier.hazeEffect(state = haze) {
                    // menü arkası: içerik bulanık + zemin rengine tonlu (okunmaz leke)
                    blurRadius = 24.dp
                    backgroundColor = bg.copy(alpha = 1f)
                    tints = listOf(HazeTint(bg.copy(alpha = if (onPhoto) 0.62f else 0.85f)))
                    noiseFactor = 0f
                } else Modifier
            )
            .drawBehind {
                // içerik menüye yumuşak geçer: üstte 40dp degrade (alttan yukarı koyu/açık geçiş)
                val endA = if (haze != null) (if (onPhoto) 0.62f else 0.85f) else bg.alpha
                val fade = (if (haze != null) 40.dp else 18.dp).toPx()
                drawRect(Brush.verticalGradient(listOf(bg.copy(alpha = 0f), bg.copy(alpha = endA)), startY = -fade, endY = 0f), topLeft = Offset(0f, -fade), size = androidx.compose.ui.geometry.Size(size.width, fade))
                if (haze == null) drawRect(bg)
            }
            .padding(bottom = inset)
    ) {
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
