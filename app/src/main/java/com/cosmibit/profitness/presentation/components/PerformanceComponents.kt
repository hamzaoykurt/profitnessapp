package com.cosmibit.profitness.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*

@Composable
fun PerformanceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            eyebrow?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = theme.text2)
                Spacer(Modifier.height(4.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = theme.text0)
        }
        action?.invoke()
    }
}

@Composable
fun PremiumChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        if (selected) accent.copy(if (theme.isDark) 0.15f else 0.10f) else theme.bg2,
        tween(220), label = "premiumChip"
    )
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.insetControlSurface(accent, theme, RoundedCornerShape(14.dp)) else Modifier.background(bg))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Icon(it, null, tint = if (selected) accent else theme.text1, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = if (selected) accent else theme.text1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun <T> PremiumSegmentedControl(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(theme.bg1).padding(4.dp)) {
        items.forEach { item ->
            val active = item == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .then(if (active) Modifier.insetControlSurface(MaterialTheme.colorScheme.primary, theme, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { onSelected(item) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label(item), color = if (active) theme.text0 else theme.text1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MetricDisplay(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier) {
        icon?.let { Icon(it, null, tint = theme.text1, modifier = Modifier.size(18.dp)); Spacer(Modifier.height(12.dp)) }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = theme.text0, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            if (unit.isNotBlank()) Text(unit, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = theme.text2, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PremiumCreationControl(
    title: String,
    supportingText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier.graphicsLayer { val s = if (pressed) 0.985f else 1f; scaleX = s; scaleY = s }
            .performanceSignatureSurface(theme, accent, RoundedCornerShape(16.dp))
            .clickable(source, null, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = theme.text0, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(3.dp))
            Text(supportingText, color = theme.text1, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = accent, modifier = Modifier.size(20.dp))
    }
}
