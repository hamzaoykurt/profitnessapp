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
 * TEK ışık (yon-renk-v2 `.bd`): r520 merkez (180, −40), c1 α tema değeri → %55'te α×.35 → 0; alt ışık YOK (leke yok);
 * alt 380dp: saydam → (alt−84dp) sayfa rengi %45. Ölçüler 360dp genişlik çizimine göre ekran genişliğiyle ölçeklenir.
 * Tek çizim: 1 radyal + 1 dikey degrade, Brush'lar drawWithCache'te (kare başı ayırma yok).
 */
@Composable
fun SinemaBackdrop(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = Color(0xFFFFD9A0)   // sıcak-nötr ışık (saf amber kahve/zeytin lekesi yapıyordu)
    val glowA = theme.palette.glowAlpha
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val k = (size.width / 360.dp.toPx()).coerceIn(com.cosmibit.profitness.core.ui.SinemaLayout.BackdropScaleMin, com.cosmibit.profitness.core.ui.SinemaLayout.BackdropScaleMax)   // çizim çerçevesi 360dp → ekran; geniş ekranda ışık ekranı kaplamaz
                fun d(v: Float) = v.dp.toPx() * k
                val c1 = Offset(d(180f), d(-40f))
                val r1 = d(360f)
                val o1 = Brush.radialGradient(
                    0f to accent.copy(glowA), 0.45f to accent.copy(glowA * 0.35f), 1f to Color.Transparent,
                    center = c1, radius = r1
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
                    drawCircle(o1, radius = r1, center = c1)
                    drawRect(scrim, topLeft = Offset(0f, shTop), size = androidx.compose.ui.geometry.Size(size.width, sh))
                }
            }
    )
}

/** Tepe ışığı gücü (sayfaya göre DEĞİŞMEZ): etkin temanın değeri (.15). Alt ışık kaldırıldı. */
val SinemaBackdropTopAlpha: Float get() = SinemaPalette.Current.glowAlpha

/** Eski ad: gövdesi artık [SinemaBackdrop] (orb hareketi kalktı). Çağıranlar otomatik yeni zemine geçer. */
@Composable
fun SinemaScene(modifier: Modifier = Modifier) = SinemaBackdrop(modifier)

/**
 * TEK ORTAK CAM KAPSÜL (hap, giriş çubuğu, satır, seçili/dolu hal) — cam C (`sinemaCapsuleGlass`).
 * - `selected`: seçili taş (`.stone`): dolu vurgu + iç ışık (glow YOK, yon-renk-v2); `content` ön plan rengini (onAccent / metin) alır.
 * - `height`: hap 32-36, satır/giriş 52dp. `shape` varsayılan tam yuvarlak.
 * - `onClick` null ise tıklanmaz (giriş çubuğu içinde kendi alanları var).
 * - `light`: geriye uyum için durur, YOK SAYILIR (tek koyu tema).
 * - Tıklanabilirken dokunma hedefi ≥48dp (görsel boyut aynı; minimumInteractiveComponentSize).
 * - `selectable`: seçilebilir kullanım (filtre/segment) — `selected` erişilebilirlik semantiği yayınlanır.
 * - `glass`: true = cam C (YALNIZ foto/video/renkli kapak üstü, SinemaBackdrop üst bölgesi, yüzen menü/zamanlayıcı/toast);
 *   false (varsayılan) = yükseltilmiş mini blok ([sinemaRaised] Mini; düz siyah zemin üstü). Cam içinde cam yok: cam yüzey içindeki kapsül glass=false.
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
    glass: Boolean = false,
    /** Yumuşak mercan dolgu (c2 α.16 + açık mercan ön plan): bilgi çipi / ikincil düğme. Yalnız cam değil + seçili değil iken. */
    soft: Boolean = false,
    /** Renkli blok (düz siyah üstü; [sinemaRaised] tint). Seçili/cam değilken geçerli. */
    tint: Color? = null,
    /** Seçili değilken DOLU renkli blok (ör. mercan c2; ikincil öne çıkan öğe). Ön plan koyu. */
    fillColor: Color? = null,
    /** Seçili hali amber dolgu yerine amber tint + amber metin (ekranda dolu amber tek kalsın). */
    neutralSelected: Boolean = false,
    content: @Composable RowScope.(Color) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val solid = selected && !neutralSelected
    val softOn = soft && !glass && !solid && fillColor == null && !(selected && neutralSelected)
    val fg = if (solid || (fillColor != null && !glass)) theme.effectiveOnAccentColor else if (selected && neutralSelected) accent else if (softOn) theme.accentHiTextColor else Sinema.Text
    val src = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = height)
            .then(if (onClick != null && enabled) Modifier.sinemaPressScale(src) else Modifier)
            .then(
                if (glass) Modifier.sinemaCapsuleGlass(shape, fill = if (solid) accent else null, edge = edge)
                else Modifier.sinemaRaised(shape, SinemaRaise.Mini, fill = if (solid) accent else fillColor ?: if (softOn) softHiFill(theme) else null, tint = if (selected && neutralSelected) accent else if (selected) null else tint)
                    .then(if (edge != null && !selected) Modifier.border(1.dp, edge, shape) else Modifier)
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
    glass: Boolean = false,
    /** Tıklanmayan bilgi çipi varsayılanı yumuşak mercan; tıklanabilir hap nötr. */
    soft: Boolean = onClick == null,
    content: @Composable RowScope.() -> Unit
) {
    SinemaCapsule(
        modifier.then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        onClick = onClick, height = 32.dp, shape = CircleShape, glass = glass, soft = soft,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
    ) { content() }
}

/**
 * Kapsül içi yuvarlak ikon düğme (cam C): seçili/dolu = taş (vurgu dolgu, glow yok), değilse beyaz ışık camı.
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
    @Suppress("UNUSED_PARAMETER") light: Boolean = false,
    /** true = cam (yalnız foto/video/kapak üstü); false = yükseltilmiş mini disk. */
    glass: Boolean = false,
    /** Seçili olmayan, cam olmayan ikon düğme yumuşak mercan dolgu alır (ikincil düğme). */
    soft: Boolean = true
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val softOn = soft && !glass && !selected
    val fg = if (selected) theme.effectiveOnAccentColor else if (softOn) theme.accentHiTextColor else Sinema.Text
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .then(if (enabled) Modifier.sinemaPressScale(src, pressedScale = 0.92f) else Modifier)
            .then(
                if (glass) Modifier.sinemaCapsuleGlass(CircleShape, fill = if (selected) accent else null)
                else Modifier.sinemaRaised(CircleShape, SinemaRaise.Mini, fill = if (selected) accent else if (softOn) softHiFill(theme) else null)
            )
            .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = fg, modifier = Modifier.size(18.dp)) }
}

/** Ekranın TEK birincil düğmesi (çizimdeki `.cta`): 56dp, r28, üst iç parlama + tek izinli glow (c1 α.22, 24dp, dy 10dp). */
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
            .sinemaPrimaryGlow(accent, shape)
            .clip(shape)
            .sinemaSolid(accent)
            .drawWithContent {
                drawContent()
                val line = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(0f to Color.White.copy(0.18f), (line * 6f / size.height) to Color.Transparent),
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
                fontWeight = FontWeight.Bold, letterSpacing = 0.sp, maxLines = 1
            )
        }
    }
}

/** Yumuşak mercan dolgu (c2 α.16, #1A1D23 kart ortası üstü; opak): çip / ikon diski / ikincil düğme. */
internal fun softHiFill(theme: AppThemeState): Color = androidx.compose.ui.graphics.lerp(androidx.compose.ui.graphics.lerp(Sinema.Page, Color.White, 0.06f), theme.accentHi, 0.16f)

/** Nötr Sinema kartı = YÜKSELTİLMİŞ BLOK ([sinemaRaised]); tüm kartlar tek yerden bu stile bağlı. [accent] verilirse üst parlama c2 tonlu. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.sinemaCard(theme: AppThemeState, shape: Shape = RoundedCornerShape(24.dp), accent: Color? = null, tint: Color? = null): Modifier =
    sinemaRaised(shape, SinemaRaise.Card, accent, tint = tint)

/**
 * Çizimdeki hap/segment düğmesi: pasif = cam C (beyaz ışık), aktif = seçili taş (dolu vurgu, glow yok). `content` aktif/pasif ön plan rengini alır.
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
    glass: Boolean = false,
    content: @Composable RowScope.(Color) -> Unit
) {
    SinemaCapsule(
        modifier, selected = active, onClick = onClick, enabled = enabled, height = height, shape = shape, glass = glass,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, selectable = selectable, content = content
    )
}

/**
 * c2 BÜTÇESİ yardımcıları (ekranın ~%90'ı nötr; c2 yalnız küçük işaretlerde).
 * Bölüm etiketi öncesi 6dp mercan nokta.
 */
@Composable
fun SinemaLabelDot(modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Box(modifier.size(6.dp).clip(CircleShape).background(theme.accentHi))
}

/**
 * Öne çıkan kart (aktif program, bugünkü antrenman, ana istatistik): üst kenarda 2dp c2 -> saydam gradyan çizgi.
 * Kartın kendi clip/shape'inden SONRA uygulanmalı (içeriğin üstüne çizer, yerleşimi değiştirmez).
 */
fun Modifier.sinemaCardAccent(color: Color, inset: Dp = 18.dp): Modifier = this.drawWithContent {
    drawContent()
    val h = 2.dp.toPx()
    val x = inset.toPx()
    drawRect(
        brush = Brush.horizontalGradient(
            0f to color.copy(alpha = 0.9f), 1f to Color.Transparent,
            startX = x, endX = size.width - x
        ),
        topLeft = Offset(x, 0f),
        size = androidx.compose.ui.geometry.Size((size.width - 2 * x).coerceAtLeast(0f), h)
    )
}
