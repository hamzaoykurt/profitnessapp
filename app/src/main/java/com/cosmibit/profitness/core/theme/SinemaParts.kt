package com.cosmibit.profitness.core.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Çizimde animasyon yoksa (sistem animasyon ölçeği 0) hareket kapalı. */
@Composable
fun rememberSinemaMotionOn(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

/**
 * Çizimdeki `.scn` (AI Koç sahnesi): nötr koyu zemin, iki küçük canlı vurgu halesi
 * ve altta 380dp karartma (zemin nötr siyah, kahve tonu yok). Her zaman koyu; temadan bağımsız.
 */
@Composable
fun SinemaScene(modifier: Modifier = Modifier) {
    val accent = LocalAppTheme.current.effectiveAccentColor
    val motion = rememberSinemaMotionOn()
    val t = rememberInfiniteTransition(label = "scene")
    val d1 by t.animateFloat(0f, if (motion) 1f else 0f, infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d1")
    val d2 by t.animateFloat(0f, if (motion) 1f else 0f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d2")
    Canvas(modifier.fillMaxSize()) {
        // Çizim .scn: vurgunun %10'u ile karışmış siyah zemin + iki büyük yavaş kayan vurgu halesi (.o1 %54, .o2 %28)
        drawRect(lerp(Color.Black, accent, 0.10f))
        fun dp(v: Float) = v.dp.toPx()
        // .o1: 720dp, sol -260 üst -240; translate (-70,30) → (110,-10); transparent @%92
        val c1 = Offset(dp(100f + (-70f + 180f * d1)), dp(120f + (30f - 40f * d1)))
        drawCircle(
            Brush.radialGradient(0f to accent.copy(0.54f), 0.92f to Color.Transparent, 1f to Color.Transparent, center = c1, radius = dp(360f)),
            radius = dp(360f), center = c1
        )
        // .o2: 620dp, sol -40 üst 250; translate (70,0) → (-90,40)
        val c2 = Offset(dp(270f + (70f - 160f * d2)), dp(560f + 40f * d2))
        drawCircle(
            Brush.radialGradient(0f to accent.copy(0.28f), 1f to Color.Transparent, center = c2, radius = dp(310f)),
            radius = dp(310f), center = c2
        )
        val h = dp(380f).coerceAtMost(size.height)
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.78f to Color.Black.copy(0.88f), 1f to Color.Black.copy(0.88f), startY = size.height - h, endY = size.height),
            topLeft = Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h)
        )
    }
}

/**
 * TEK ORTAK KALIN BUZLU CAM KAPSÜL (hap, giriş çubuğu, satır, seçili/dolu hal). Sonraki dilimler bunu kullanır.
 * - `selected`: dolu vurgu + sinemaGlow (vurgu %40, 0 8dp 20dp); `content` ön plan rengini (onAccent / beyaz) alır.
 * - `height`: hap 32-36, satır/giriş 52dp. `shape` varsayılan tam yuvarlak.
 * - `onClick` null ise tıklanmaz (giriş çubuğu içinde kendi alanları var).
 * - `light`: açık tema cam (beyaz tint + koyu yazı) — foto üstü sayfalar hep koyu, düz açık sayfada true.
 * - Tıklanabilirken dokunma hedefi ≥48dp (görsel boyut aynı; minimumInteractiveComponentSize).
 * - `selectable`: seçilebilir kullanım (filtre/segment) — `selected` erişilebilirlik semantiği yayınlanır.
 */
@Composable
fun SinemaCapsule(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(height / 2),
    edge: Color = Color.White.copy(0.18f),
    light: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    selectable: Boolean = false,
    content: @Composable RowScope.(Color) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val fg = if (selected) theme.effectiveOnAccentColor else if (light) Sinema.text(theme) else Color.White
    Row(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = height)
            .sinemaCapsuleGlass(
                shape, edge = if (light) Color.Black.copy(0.12f) else edge, blur = 20.dp,
                fill = if (selected) accent else null,
                glow = if (selected) accent.copy(0.4f) else null,
                tint = if (light) Color.White else Color.Black,
                fillAlpha = if (light) 0.55f else 0.35f
            )
            .then(if (selectable) Modifier.semantics { this.selected = selected } else Modifier)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement
    ) { content(fg) }
}

/** `.pill`: 32dp cam hap (içerik + isteğe bağlı tıklama; dokunma hedefi 48dp) — SinemaCapsule üzerine. */
@Composable
fun SinemaPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    SinemaCapsule(
        modifier.then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        onClick = onClick, height = 32.dp, shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
    ) { content() }
}

/**
 * Kapsül içi yuvarlak ikon düğme (cam kapsül dili): seçili/dolu = vurgu + glow.
 * `description` zorunlu parametre (varsayılanı yok; geriye uyum için `String?`) — ekran okuyucu etiketi, null verme.
 * Dokunma hedefi ≥48dp, görsel `size` aynı.
 * `light`: düz açık sayfada pasif hal (koyu %6 zemin + koyu ikon); foto üstü sayfalarda false.
 */
@Composable
fun SinemaCapsuleIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    size: Dp = 38.dp,
    light: Boolean = false
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val fg = if (selected) theme.effectiveOnAccentColor else if (light) Sinema.text(theme) else Color.White
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .then(if (selected) Modifier.sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(if (selected) accent else if (light) Color.Black.copy(0.06f) else Color.White.copy(0.14f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = fg, modifier = Modifier.size(18.dp)) }
}

/** Düz vurgu dolgulu ana düğme (çizimdeki `.go`): 56dp, r28, üst iç parlama + vurgu %40 gölge. */
@Composable
fun SinemaGoButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dim: Boolean = false
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "go_scale"
    )
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (dim) 0.5f else 1f }
            .sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, shape)
            .clip(shape)
            .background(accent)
            .drawWithContent {
                drawContent()
                val line = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(0f to Color.White.copy(0.40f), (line * 6f / size.height) to Color.Transparent),
                    cornerRadius = CornerRadius(28.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(line * 1.5f)
                )
            }
            .clickable(src, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                label, color = theme.effectiveOnAccentColor, fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em, maxLines = 1
            )
        }
    }
}

/** Nötr Sinema kartı: #141416 (açıkta #F2F3F5) + 1dp çizgi, vurgu tonu yok. */
fun Modifier.sinemaCard(theme: AppThemeState, shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .clip(shape)
    .background(Sinema.surface(theme))
    .border(1.dp, Sinema.line(theme), shape)

/**
 * Çizimdeki hap/segment düğmesi: pasif = siyah %35 cam + beyaz %18 kenar,
 * aktif = dolu vurgu + sinemaGlow (vurgu %40, 0 8dp 20dp). `content` aktif/pasif ön plan rengini alır.
 * Dokunma hedefi ≥48dp. `selectable` = filtre/segment kullanımı: `active` erişilebilirlikte "seçili" okunur.
 */
@Composable
fun SinemaChip(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 32.dp,
    shape: Shape = CircleShape,
    selectable: Boolean = false,
    content: @Composable RowScope.(Color) -> Unit
) {
    SinemaCapsule(
        modifier, selected = active, onClick = onClick, enabled = enabled, height = height, shape = shape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, selectable = selectable, content = content
    )
}
