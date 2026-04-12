package com.avonix.profitness.presentation.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

/**
 * Compact row shown on every AI feature screen for FREE users.
 * Displays cost per action and current balance.
 * No-op for paid plans.
 */
@Composable
fun AiCreditInfoRow(
    isFree    : Boolean,
    credits   : Int,
    costLabel : String = "1 kredi",
    theme     : AppThemeState,
    modifier  : Modifier = Modifier
) {
    if (!isFree) return
    val accent    = MaterialTheme.colorScheme.primary
    val outOfCredits = credits == 0
    val badgeColor = if (outOfCredits) Color(0xFFFF4444) else accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(badgeColor.copy(alpha = 0.08f))
            .border(1.dp, badgeColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Column {
            Text(
                if (outOfCredits) "Kredi bitti!" else costLabel,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (outOfCredits) "Kredi satın al veya plana yükselt"
                else "Kalan bakiye: $credits kredi",
                color = theme.text2,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeColor.copy(alpha = 0.14f))
                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (outOfCredits) "0 kredi" else "$credits kredi",
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
