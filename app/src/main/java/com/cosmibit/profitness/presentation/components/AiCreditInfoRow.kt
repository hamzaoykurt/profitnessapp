package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*

/**
 * Compact row shown on every AI feature screen for FREE users.
 * Displays cost per action and current balance.
 * No-op for paid plans.
 */
@Composable
fun AiCreditInfoRow(
    isFree    : Boolean,
    credits   : Int,
    costLabel : String = "1 Enerji",
    theme     : AppThemeState,
    modifier  : Modifier = Modifier
) {
    if (!isFree) return
    val accent    = MaterialTheme.colorScheme.primary
    val outOfCredits = credits == 0
    val badgeColor = if (outOfCredits) Color(0xFFFF4444) else accent
    val badgeText = if (outOfCredits) {
        theme.ui("0 Enerji")
    } else {
        theme.ui("%d Enerji", "%d Energy").format(credits)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(badgeColor.copy(alpha = 0.08f))
            .border(1.dp, badgeColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = badgeColor, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (outOfCredits) theme.ui("Enerjin bitti!") else theme.ui(costLabel),
                    color = badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (outOfCredits) theme.ui("Enerji yükle veya plana yükselt")
                    else theme.ui("Kalan Enerji: %d", "Remaining Energy: %d").format(credits),
                    color = theme.text2,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            EnergyBadge(text = badgeText, color = badgeColor)
        }
    }
}

@Composable
private fun EnergyBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 13.sp,
            maxLines = 1
        )
    }
}
