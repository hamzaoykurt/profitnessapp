package com.cosmibit.profitness.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.bg1
import com.cosmibit.profitness.core.theme.bg2
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.text0
import com.cosmibit.profitness.core.theme.text1
import com.cosmibit.profitness.core.theme.text2
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.EventMode
import java.time.LocalDate
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.SinemaRadius

/**
 * Horizontal scrollable strip of next-7-days events (user's joined events).
 * Today is filtered out (the today banner already shows those).
 */
@Composable
fun UpcomingEventsSection(
    events: List<ChallengeSummary>,
    onOpen: (ChallengeSummary) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val today = remember_today_string()
    val list = events.filter { it.event?.dateIso != today }
    if (list.isEmpty()) return

    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.upcomingEventsTitle,
                color = theme.text2,
                fontSize = SinemaType.Caption,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { "up_${it.id}" }) { ev ->
                UpcomingCard(summary = ev, onClick = { onOpen(ev) })
            }
        }
    }
}

@Composable
private fun remember_today_string(): String = LocalDate.now().toString()

@Composable
private fun UpcomingCard(summary: ChallengeSummary, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(SinemaRadius.R16)
    val ev = summary.event
    val icon = when (ev?.mode) {
        EventMode.Physical     -> Icons.Rounded.LocationOn
        EventMode.Online       -> Icons.Rounded.Link
        EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck
        else                   -> Icons.Rounded.Event
    }

    Column(
        modifier = Modifier
            .width(190.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.975f else 1f
                scaleY = if (pressed) 0.975f else 1f
                translationY = if (pressed) 2.dp.toPx() else 0f
            }
            .shadow(
                elevation = if (pressed) 2.dp else 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(0.30f),
                spotColor = Color.Black.copy(0.36f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(theme.bg2, theme.bg1))
            )
            .border(
                1.dp,
                theme.text0.copy(0.10f),
                shape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                ev?.dateIso ?: "—",
                color = accent,
                fontSize = SinemaType.Caption,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (ev?.timeIso != null) {
                Text(
                    " · ${ev.timeIso.take(5)}",
                    color = theme.text2,
                    fontSize = SinemaType.Caption,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            summary.title,
            color = theme.text0,
            fontSize = SinemaType.BodySm,
            fontWeight = FontWeight.Black,
            maxLines = 2
        )
        Spacer(Modifier.height(4.dp))
        val sub = when (ev?.mode) {
            EventMode.Physical     -> ev.location ?: strings.eventModePhysical
            EventMode.Online       -> strings.eventModeOnline
            EventMode.MovementList -> "${ev.movementsCount} ${strings.eventModeMovementList.lowercase()}"
            else                   -> ""
        }
        if (sub.isNotBlank()) {
            Text(
                sub,
                color = theme.text1,
                fontSize = SinemaType.Caption,
                maxLines = 1
            )
        }
    }
}
