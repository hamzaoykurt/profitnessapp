package com.cosmibit.profitness.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
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
 * Ücretsiz planda AI program hakkı satırı ("2 ücretsiz AI program hakkın var" / "1 hakkın kaldı").
 * Enerji/kredi modeli kalktı; Premium'da çağıran göstermez. [freeLeft] sunucudan gelir (null = bilinmiyor).
 */
@Composable
fun AiCreditInfoRow(
    freeLeft : Int?,
    theme    : AppThemeState,
    modifier : Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val usedUp = freeLeft != null && freeLeft <= 0
    val english = theme.language == AppLanguage.ENGLISH
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SinemaRadius.R12))
            .background(theme.bg2)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (usedUp) Icons.Rounded.Lock else Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                com.cosmibit.profitness.domain.ai.AiAccessPolicy.freeGrantLine(freeLeft, english),
                color = theme.text1, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold,
                lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                if (usedUp) theme.t("Sınırsız AI program için Premium'a geç.", "Go Premium for unlimited AI programs.")
                else theme.t("Premium'da sınırsız.", "Unlimited in Premium."),
                color = theme.text2, fontSize = SinemaType.Caption, lineHeight = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
