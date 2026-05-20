package com.avonix.profitness.presentation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.bg3
import com.avonix.profitness.core.theme.readableOnAccentColor
import com.avonix.profitness.core.theme.t
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.core.theme.toSafeAccentArgb
import com.avonix.profitness.core.theme.toSafeAccentColor

private const val MinPickerSaturation = 0.32f
private const val MinPickerValue = 0.48f
private const val MaxPickerValue = 0.94f

@Composable
fun AccentColorSwatch(
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val safeColor = color.toSafeAccentColor()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) safeColor.copy(0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) safeColor else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(safeColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = safeColor.readableOnAccentColor(),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CustomAccentSwatch(
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val safeColor = color.toSafeAccentColor()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) safeColor.copy(0.15f) else Color.Transparent)
            .border(2.dp, safeColor.copy(if (isSelected) 1f else 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(safeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSelected) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = safeColor.readableOnAccentColor(),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun CustomAccentColorDialog(
    initialColor: Color,
    theme: AppThemeState,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val hsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(hsv[1].coerceAtLeast(MinPickerSaturation)) }
    var value by remember(initialColor) { mutableFloatStateOf(hsv[2].coerceIn(MinPickerValue, MaxPickerValue)) }
    val rawColor = Color.hsv(hue, saturation, value)
    val safeColor = rawColor.toSafeAccentColor()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = theme.bg2,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.t("OZEL RENK", "CUSTOM COLOR"),
                            color = theme.text0,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            theme.t("Cok koyu, soluk ve asiri parlak tonlar secim alani disinda.", "Very dark, washed-out, and overly bright tones are outside the picker range."),
                            color = theme.text2,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(safeColor)
                    )
                }

                SaturationValuePanel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { newSaturation, newValue ->
                        saturation = newSaturation
                        value = newValue
                    }
                )

                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .height(38.dp)
                            .width(82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(safeColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "#%06X".format(0xFFFFFF and safeColor.toArgb()),
                        color = theme.text1,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.bg3,
                            contentColor = theme.text1
                        )
                    ) {
                        Text(theme.t("IPTAL", "CANCEL"), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = {
                            onColorSelected(rawColor.toArgb().toSafeAccentArgb())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = safeColor,
                            contentColor = safeColor.readableOnAccentColor()
                        )
                    ) {
                        Text(theme.t("SEC", "SELECT"), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hueColor = Color.hsv(hue, 1f, 1f)

    fun update(offset: Offset) {
        if (size.width <= 0 || size.height <= 0) return
        val normalizedX = (offset.x / size.width).coerceIn(0f, 1f)
        val normalizedY = (offset.y / size.height).coerceIn(0f, 1f)
        onChange(
            MinPickerSaturation + normalizedX * (1f - MinPickerSaturation),
            MaxPickerValue - normalizedY * (MaxPickerValue - MinPickerValue)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val pointerX = ((saturation - MinPickerSaturation) / (1f - MinPickerSaturation)).coerceIn(0f, 1f) * this.size.width
        val pointerY = ((MaxPickerValue - value) / (MaxPickerValue - MinPickerValue)).coerceIn(0f, 1f) * this.size.height
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(pointerX, pointerY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 10.dp.toPx(),
            center = Offset(pointerX, pointerY),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val colors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )

    fun update(offset: Offset) {
        if (size.width <= 0) return
        onHueChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(99.dp))
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        val y = this.size.height / 2f
        drawLine(
            brush = Brush.horizontalGradient(colors),
            start = Offset(0f, y),
            end = Offset(this.size.width, y),
            strokeWidth = this.size.height,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset((hue / 360f) * this.size.width, y),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
