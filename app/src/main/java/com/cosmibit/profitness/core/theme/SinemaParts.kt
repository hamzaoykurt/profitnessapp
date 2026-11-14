package com.cosmibit.profitness.core.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * SAYFA ZEMİNİ — TEK KAYNAK (.ekip/yon-cam-siyah.md "Sayfa zemini sistemi", seçenek C).
 * Her sayfada aynı, SABİT (animasyon yok, içerikle kaymaz): zemin #0C0D10;
 * üst ışık r360 merkez (120,130) vurgu α.30 → %92'de 0; alt ışık r310 merkez (260, alt−220) c2 α.18 (yon-renk-hareket.md) → 0;
 * alt 380dp: saydam → (alt−84dp) sayfa rengi %45. Ölçüler 360dp genişlik çizimine göre ekran genişliğiyle ölçeklenir.
 * Tek çizim: 2 radyal + 1 dikey degrade, Brush'lar drawWithCache'te (kare başı ayırma yok).
 */
@Composable
fun SinemaBackdrop(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val accent2 = theme.accent2Color   // alt ışık = c2 (ölç)
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val k = (size.width / 360.dp.toPx()).coerceIn(com.cosmibit.profitness.core.ui.SinemaLayout.BackdropScaleMin, com.cosmibit.profitness.core.ui.SinemaLayout.BackdropScaleMax)   // çizim çerçevesi 360dp → ekran; geniş ekranda ışık ekranı kaplamaz
                fun d(v: Float) = v.dp.toPx() * k
                val c1 = Offset(d(120f), d(130f))
                val r1 = d(360f)
                val o1 = Brush.radialGradient(
                    0f to accent.copy(SinemaBackdropTopAlpha), 0.92f to Color.Transparent, 1f to Color.Transparent,
                    center = c1, radius = r1
                )
                val c2 = Offset(d(260f), size.height - d(220f))
                val r2 = d(310f)
                val o2 = Brush.radialGradient(
                    0f to accent2.copy(SinemaBackdropBottomAlpha), 1f to Color.Transparent, center = c2, radius = r2
                )
                val sh = d(380f).coerceAtMost(size.height)
                val shTop = size.height - sh
                val solidFrom = if (sh > 0f) ((sh - d(84f)) / sh).coerceIn(0f, 1f) else 1f
                val scrim = Brush.verticalGradient(
                    0f to Color.Transparent, solidFrom to Sinema.scrim(0.45f), 1f to Sinema.scrim(0.45f),
                    startY = shTop, endY = size.height
                )
                onDrawBehind {
                    drawRect(Sinema.Page)
                    drawCircle(o2, radius = r2, center = c2)
                    drawCircle(o1, radius = r1, center = c1)
                    drawRect(scrim, topLeft = Offset(0f, shTop), size = androidx.compose.ui.geometry.Size(size.width, sh))
                }
            }
    )
}

/** Seçenek C ışık güçleri (sayfaya göre DEĞİŞMEZ). */
const val SinemaBackdropTopAlpha = 0.30f
const val SinemaBackdropBottomAlpha = 0.18f   // c2 c1'den açık → .22 değil .18

/** Eski ad: gövdesi artık [SinemaBackdrop] (orb hareketi kalktı). Çağıranlar otomatik yeni zemine geçer. */
@Composable
fun SinemaScene(modifier: Modifier = Modifier) = SinemaBackdrop(modifier)

/**
 * TEK ORTAK CAM KAPSÜL (hap, giriş çubuğu, satır, seçili/dolu hal) — cam C (`sinemaCapsuleGlass`).
 * - `selected`: seçili taş (`.stone`): dolu vurgu + iç ışık + vurgu hale .35; `content` ön plan rengini (onAccent / metin) alır.
 * - `height`: hap 32-36, satır/giriş 52dp. `shape` varsayılan tam yuvarlak.
 * - `onClick` null ise tıklanmaz (giriş çubuğu içinde kendi alanları var).
 * - `light`: geriye uyum için durur, YOK SAYILIR (tek koyu tema).
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
    /** Düz kenar rengi (ör. odakta vurgu); null/beyaz .18 = cam C degrade kenar. */
    edge: Color? = null,
    @Suppress("UNUSED_PARAMETER") light: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    selectable: Boolean = false,
    content: @Composable RowScope.(Color) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val fg = if (selected) theme.effectiveOnAccentColor else Sinema.Text
    val src = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = height)
            .then(if (onClick != null && enabled) Modifier.sinemaPressScale(src) else Modifier)
            .sinemaCapsuleGlass(
                shape,
                fill = if (selected) accent else null,
                glow = if (selected) accent.copy(0.35f) else null,
                edge = edge
            )
            .then(if (selectable) Modifier.semantics { this.selected = selected } else Modifier)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = src, indication = LocalIndication.current,
                    enabled = enabled, role = Role.Button,
                    onClick = {
                        // seçim değişimi (filtre/segment) hafif titreşir; zaten seçili olana dokunmak titreşmez
                        if (selectable && !selected) haptic.performHapticFeedback(SinemaHaptics.Light)
                        onClick()
                    }
                ) else Modifier
            )
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
 * Kapsül içi yuvarlak ikon düğme (cam C): seçili/dolu = taş (vurgu + hale .35), değilse beyaz ışık camı.
 * `description` zorunlu parametre (varsayılanı yok; geriye uyum için `String?`) — ekran okuyucu etiketi, null verme.
 * Dokunma hedefi ≥48dp, görsel `size` aynı. `light` geriye uyum için durur, yok sayılır (tek koyu tema).
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
    @Suppress("UNUSED_PARAMETER") light: Boolean = false
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val fg = if (selected) theme.effectiveOnAccentColor else Sinema.Text
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .then(if (enabled) Modifier.sinemaPressScale(src, pressedScale = 0.92f) else Modifier)
            .sinemaCapsuleGlass(
                CircleShape,
                fill = if (selected) accent else null,
                glow = if (selected) accent.copy(0.35f) else null
            )
            .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
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
        if (pressed) 0.96f else 1f, SinemaMotion.press(), label = "go_scale"
    )
    val haptic = LocalHapticFeedback.current
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
            .clickable(src, null, role = Role.Button, onClick = { haptic.performHapticFeedback(SinemaHaptics.Light); onClick() }),
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

/** Nötr Sinema kartı: #15171C (opak) + 1dp beyaz .09 çizgi, vurgu tonu yok. */
fun Modifier.sinemaCard(theme: AppThemeState, shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .clip(shape)
    .background(Sinema.surface(theme))
    .border(1.dp, Sinema.line(theme), shape)

/**
 * Çizimdeki hap/segment düğmesi: pasif = cam C (beyaz ışık), aktif = seçili taş (dolu vurgu + hale .35). `content` aktif/pasif ön plan rengini alır.
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
