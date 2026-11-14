package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.core.theme.SinemaMotion
import com.cosmibit.profitness.core.theme.accent3Color
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.supportAlert
import com.cosmibit.profitness.core.theme.t

/**
 * ORTAK DURUM BİLEŞENLERİ (boş / hata / iskelet) — Sinema dili, tek koyu tema, cam C.
 * Üçü de bulunduğu alanı doldurur (`fillMaxSize`); ekran gerekirse Box içine koyar.
 */

/** Boş durum: cam disk + c3 ikon + başlık + kısa metin + (varsa) tek eylem. */
@Composable
fun SinemaEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector = SinemaIcons.Plus,
    onAction: (() -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(72.dp).sinemaCapsuleGlass(CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = theme.accent3Color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            title, color = Sinema.text(theme), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            lineHeight = 26.sp, textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                body, color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp)
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            SinemaGoButton(label = actionLabel, icon = actionIcon, onClick = onAction, modifier = Modifier.widthIn(max = 280.dp))
        }
    }
}

/** Hata durumu: uyarı ikonu + metin + cam "Tekrar dene" kapsülü (kırmızı kutu yok). */
@Composable
fun SinemaErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = theme.supportAlert, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            message, color = Sinema.text(theme), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(20.dp))
        SinemaCapsule(onClick = onRetry, height = 48.dp, contentPadding = PaddingValues(horizontal = 22.dp)) { fg ->
            Icon(Icons.Rounded.Refresh, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                theme.t("TEKRAR DENE", "TRY AGAIN"), color = fg, fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, maxLines = 1
            )
        }
    }
}

/**
 * Yumuşak parıltılı iskelet yüzeyi. Parıltı çizim aşamasında okunur (yeniden kompozisyon yok);
 * hareket azaltmada (animasyon ölçeği 0) sabit düz blok. Süre: [SinemaMotion.Slow] × 2 — tek token'dan.
 */
@Composable
fun Modifier.sinemaShimmer(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val base = Color.White.copy(0.06f)
    if (!rememberSinemaMotionOn()) return this.clip(shape).background(base)
    val phase by rememberInfiniteTransition(label = "sinema_skeleton").animateFloat(
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(SinemaMotion.Slow * 3, easing = LinearEasing), RepeatMode.Restart),
        label = "sinema_skeleton_phase"
    )
    return this.clip(shape).background(base).drawWithContent {
        drawContent()
        val w = size.width
        val x = phase * w
        drawRect(
            brush = Brush.linearGradient(
                0f to Color.Transparent, 0.5f to Color.White.copy(0.07f), 1f to Color.Transparent,
                start = Offset(x - w * 0.4f, 0f), end = Offset(x + w * 0.4f, size.height)
            )
        )
    }
}

/** Tek iskelet bloğu (metin çizgisi, görsel, rozet). */
@Composable
fun SinemaSkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    Box(modifier.sinemaShimmer(shape))
}

/** Kart iskeleti (fotoğraflı liste kartı / ana kart): tam genişlik, [height] yüksek, 24dp köşe. */
@Composable
fun SinemaSkeletonCard(modifier: Modifier = Modifier, height: Dp = 200.dp) {
    Box(modifier.fillMaxWidth().height(height).sinemaShimmer(RoundedCornerShape(24.dp)))
}

/** Satır iskeleti: yuvarlak avatar + iki metin çizgisi. */
@Composable
fun SinemaSkeletonRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        SinemaSkeletonBlock(Modifier.size(44.dp), CircleShape)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            SinemaSkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp))
            Spacer(Modifier.height(8.dp))
            SinemaSkeletonBlock(Modifier.fillMaxWidth(0.35f).height(11.dp))
        }
    }
}

/** Rakam iskeleti: büyük sayı + altında etiket çizgisi. */
@Composable
fun SinemaSkeletonNumber(modifier: Modifier = Modifier, width: Dp = 96.dp) {
    Column(modifier) {
        SinemaSkeletonBlock(Modifier.width(width).height(40.dp), RoundedCornerShape(12.dp))
        Spacer(Modifier.height(8.dp))
        SinemaSkeletonBlock(Modifier.width(width * 0.6f).height(11.dp))
    }
}

/** Liste yüklenirken: [count] kart iskeleti (ekran okuyucuya tek "yükleniyor" etiketi). */
@Composable
fun SinemaSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 3,
    cardHeight: Dp = 200.dp,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
) {
    val theme = LocalAppTheme.current
    val label = theme.t("Yükleniyor", "Loading")
    Column(
        modifier.fillMaxSize().padding(contentPadding).semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) { Box(Modifier.clearAndSetSemantics { }) { SinemaSkeletonCard(height = cardHeight) } }
    }
}
