package com.cosmibit.profitness.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import com.cosmibit.profitness.core.theme.text2
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.EventMode

/**
 * Shown above the daily workout program when the user has joined events today.
 * Tapping a row navigates to the Challenges tab + opens the detail overlay.
 */
@Composable
fun ChallengeTodayBanner(
    events: List<ChallengeSummary>,
    onOpen: (ChallengeSummary) -> Unit
) {
    if (events.isEmpty()) return
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    val surface = if (theme.isDark) {
        Brush.verticalGradient(listOf(theme.text0.copy(0.075f), theme.text0.copy(0.025f)))
    } else {
        Brush.verticalGradient(listOf(Color.White, theme.text0.copy(0.018f)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = if (theme.isDark) 14.dp else 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(if (theme.isDark) 0.34f else 0.10f),
                spotColor = accent.copy(if (theme.isDark) 0.18f else 0.05f)
            )
            .clip(shape)
            .background(surface)
            .border(
                1.dp,
                if (theme.isDark) accent.copy(0.34f) else theme.stroke.copy(0.78f),
                shape
            )
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                strings.todayEventsTitle,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
        Spacer(Modifier.height(10.dp))

        events.forEachIndexed { idx, ev ->
            EventMiniRow(summary = ev, onClick = { onOpen(ev) })
            if (idx < events.size - 1) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EventMiniRow(summary: ChallengeSummary, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ev = summary.event
    val (icon, _) = when (ev?.mode) {
        EventMode.Physical     -> Icons.Rounded.LocationOn to "FİZİKSEL"
        EventMode.Online       -> Icons.Rounded.Link to "ONLINE"
        EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck to "HAREKET"
        else                   -> Icons.Rounded.Event to ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (pressed) 0.985f else 1f
                scaleY = if (pressed) 0.985f else 1f
                translationY = if (pressed) 2.dp.toPx() else 0f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (theme.isDark) theme.text0.copy(0.055f) else theme.bg1)
            .border(
                1.dp,
                if (theme.isDark) theme.text0.copy(0.10f) else theme.stroke.copy(0.62f),
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                summary.title,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            val subtitle = buildString {
                ev?.timeIso?.let { append(it.take(5)) }
                if (ev?.mode == EventMode.MovementList) {
                    if (isNotEmpty()) append(" · ")
                    append("${ev.myCompletedCount}/${ev.movementsCount} hareket")
                } else if (!ev?.location.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(ev?.location ?: "")
                } else if (!ev?.onlineUrl.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · online")
                    else append("online")
                }
            }
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = theme.text2, fontSize = 11.sp, maxLines = 1)
            }
        }
        Icon(
            Icons.Rounded.ChevronRight, null,
            tint = theme.text2,
            modifier = Modifier.size(16.dp)
        )
    }
}
