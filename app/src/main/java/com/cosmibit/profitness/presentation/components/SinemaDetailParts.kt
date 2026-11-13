package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaGlow
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t

/**
 * Sinema detay sayfası parçaları (Profil detayları + Kilo): düz zemin sayfa başlığı, dev rakam,
 * glow'lu ilerleme çubuğu, cam giriş, glow'lu grafik çizgisi ve Sinema diyaloğu.
 */

/** Sayfa zemini: koyu #000 / açık #FFF. */
fun sinemaPageBg(theme: AppThemeState): Color = Sinema.page(theme)

/** Büyük başlık (ScreenTitle 800) + alt satır + cam/nötr geri. */
@Composable
fun SinemaDetailHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = onBack, accent = theme.effectiveAccentColor, size = 48.dp)
            Spacer(Modifier.weight(1f))
            trailing?.invoke(this)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = Sinema.ScreenTitle, color = Sinema.text(theme), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

/** Küçük büyük harf bölüm etiketi (çizgi yığını yok). */
@Composable
fun SinemaSectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val theme = LocalAppTheme.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Sinema.textSecondary(theme), style = Sinema.Label)
    }
}

/** Dev rakam + birim + etiket. `size` rakam punto. */
@Composable
fun SinemaBigStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: Int = 44,
    color: Color = Sinema.text(LocalAppTheme.current)
) {
    val theme = LocalAppTheme.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = Sinema.Metric.copy(fontSize = size.sp),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit, color = Sinema.textSecondary(theme), fontSize = (size * 0.36f).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = (size * 0.12f).dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Vurgu dolgulu, glow'lu ilerleme çubuğu (iz: nötr). */
@Composable
fun SinemaGlowBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, color: Color? = null) {
    val theme = LocalAppTheme.current
    val c = color ?: theme.effectiveAccentColor
    val p = progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Sinema.line(theme))) {
        if (p > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(p)
                    .fillMaxHeight()
                    .sinemaGlow(c.copy(0.4f), 10.dp, 0.dp, CircleShape)
                    .clip(CircleShape)
                    .background(c)
            )
        }
    }
}

/** Cam giriş: etiket üstte, SinemaCapsule içinde BasicTextField. */
@Composable
fun SinemaGlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    capitalization: androidx.compose.ui.text.input.KeyboardCapitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
    singleLine: Boolean = true
) {
    val theme = LocalAppTheme.current
    Column(modifier) {
        Text(label.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        SinemaCapsule(
            modifier = Modifier.fillMaxWidth(), height = 52.dp,
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) { fg ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, color = fg.copy(0.4f), fontSize = 15.sp, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    textStyle = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(theme.effectiveAccentColor),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = capitalization),
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (suffix != null) {
                Spacer(Modifier.width(8.dp))
                Text(suffix, color = fg.copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Grafik çizgisi: vurgu + glow (geniş düşük alfa katmanları + ana çizgi). */
fun DrawScope.drawGlowLine(path: Path, color: Color, widthPx: Float) {
    drawPath(path, color.copy(0.10f), style = Stroke(widthPx * 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color.copy(0.22f), style = Stroke(widthPx * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Grafik noktası: halka glow + dolu çekirdek. */
fun DrawScope.drawGlowDot(center: Offset, color: Color, radiusPx: Float) {
    drawCircle(color.copy(0.18f), radiusPx * 3f, center)
    drawCircle(color.copy(0.35f), radiusPx * 1.8f, center)
    drawCircle(color, radiusPx, center)
}

/** Sinema diyaloğu: sinemaCard + başlık + metin + SinemaGoButton + yazı iptal. */
@Composable
fun SinemaDialog(
    onDismiss: () -> Unit,
    title: String,
    body: String,
    confirmLabel: String,
    confirmIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onConfirm: () -> Unit,
    dismissLabel: String
) {
    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = Sinema.Title, color = Sinema.text(theme))
            Spacer(Modifier.height(8.dp))
            Text(body, color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(24.dp))
            SinemaGoButton(label = confirmLabel, icon = confirmIcon, onClick = onConfirm)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(dismissLabel, color = Sinema.textSecondary(theme), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Seçici satırı (sekme/aralık/formül): SinemaCapsule dolu vurgu + glow. */
@Composable
fun <T> SinemaSegmentRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    val theme = LocalAppTheme.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            SinemaCapsule(
                modifier = Modifier.weight(1f), selected = opt == selected, onClick = { onSelect(opt) },
                height = height,horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { fg ->
                Text(label(opt), color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Aktif: SinemaGoButton. Pasif: nötr gri (beyaz %10 zemin, %40 yazı; açıkta siyah %8). */
@Composable
fun SinemaGoOrIdle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    if (enabled) {
        SinemaGoButton(label = label, icon = icon, onClick = onClick, modifier = modifier)
    } else {
        val fg = Sinema.textSecondary(theme).copy(0.7f)
        Row(
            modifier = modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Sinema.line(theme)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = fg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}
