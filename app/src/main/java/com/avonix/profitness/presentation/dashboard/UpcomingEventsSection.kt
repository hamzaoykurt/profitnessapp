package com.avonix.profitness.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.strings
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.EventMode
import java.time.LocalDate

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
                fontSize = 11.sp,
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
    val ev = summary.event
    val icon = when (ev?.mode) {
        EventMode.Physical     -> Icons.Rounded.LocationOn
        EventMode.Online       -> Icons.Rounded.Link
        EventMode.MovementList -> Icons.Rounded.PlaylistAddCheck
        else                   -> Icons.Rounded.Event
    }

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.text0.copy(0.04f))
            .border(1.dp, theme.stroke.copy(0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                ev?.dateIso ?: "—",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (ev?.timeIso != null) {
                Text(
                    " · ${ev.timeIso.take(5)}",
                    color = theme.text2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            summary.title,
            color = theme.text0,
            fontSize = 13.sp,
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
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
