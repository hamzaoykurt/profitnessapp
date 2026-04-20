package com.avonix.profitness.presentation.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CreateChallengeOverlay(
    inFlight: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        targetType: ChallengeTargetType,
        targetValue: Long,
        startDateIso: String,
        endDateIso: String,
        visibility: ChallengeVisibility,
        password: String?
    ) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var title       by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var targetType  by rememberSaveable { mutableStateOf(ChallengeTargetType.TotalWorkouts) }
    var targetValue by rememberSaveable { mutableStateOf("10") }
    var days        by rememberSaveable { mutableStateOf("14") }
    var visibility  by rememberSaveable { mutableStateOf(ChallengeVisibility.Public) }
    var password    by rememberSaveable { mutableStateOf("") }

    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val endDate = remember(days) {
        val d = days.toIntOrNull()?.coerceIn(1, 365) ?: 14
        today.plusDays(d.toLong())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.bg1)
                        .border(1.dp, theme.stroke, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, null, tint = theme.text0, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "YENİ CHALLENGE",
                    color         = theme.text0,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // ── Title ──
            FieldLabel("BAŞLIK")
            TextInputBox(
                value = title,
                onValueChange = { title = it },
                placeholder = "30 gün kardiyo serisi",
                imeAction = ImeAction.Next
            )

            // ── Description ──
            FieldLabel("AÇIKLAMA")
            TextInputBox(
                value = description,
                onValueChange = { description = it },
                placeholder = "Kısa açıklama (opsiyonel)",
                imeAction = ImeAction.Default,
                minLines = 2
            )

            // ── Target type ──
            FieldLabel("HEDEF TİPİ")
            Column(Modifier.padding(horizontal = 16.dp)) {
                ChallengeTargetType.values().forEach { t ->
                    TargetTypeOption(
                        type     = t,
                        isActive = t == targetType,
                        accent   = accent,
                        onClick  = { targetType = t }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Target value + süre ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("HEDEF (${targetType.unit})", padded = false)
                    NumberInputInline(
                        value = targetValue,
                        onValueChange = { targetValue = it }
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("SÜRE (gün)", padded = false)
                    NumberInputInline(
                        value = days,
                        onValueChange = { days = it }
                    )
                }
            }

            Text(
                "Bitiş: ${endDate.format(fmt)}",
                color = theme.text2,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── Visibility ──
            FieldLabel("GÖRÜNÜRLÜK")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg1.copy(0.6f))
                    .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                VisibilityChip(
                    label    = "PUBLIC",
                    isActive = visibility == ChallengeVisibility.Public,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Public },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(3.dp))
                VisibilityChip(
                    label    = "ÖZEL",
                    isActive = visibility == ChallengeVisibility.Private,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Private },
                    modifier = Modifier.weight(1f)
                )
            }

            if (visibility == ChallengeVisibility.Private) {
                FieldLabel("ŞİFRE (arkadaşlarına paylaş)")
                TextInputBox(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Opsiyonel — boş bırakılırsa herkes girebilir",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password
                )
            }

            // ── Error ──
            error?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5252).copy(0.14f))
                        .border(1.dp, Color(0xFFFF5252).copy(0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }

            // ── Submit ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent)
                    .clickable(enabled = !inFlight) {
                        onSubmit(
                            title,
                            description,
                            targetType,
                            targetValue.toLongOrNull() ?: 0L,
                            today.format(fmt),
                            endDate.format(fmt),
                            visibility,
                            password.ifBlank { null }
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inFlight) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        "OLUŞTUR",
                        color         = Color.Black,
                        fontSize      = 14.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Küçük yardımcı composable'lar ────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String, padded: Boolean = true) {
    val theme = LocalAppTheme.current
    Text(
        text,
        color      = theme.text2,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(
            start = if (padded) 20.dp else 0.dp,
            top = 10.dp,
            bottom = 4.dp
        )
    )
}

@Composable
private fun TextInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    minLines: Int = 1
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = theme.text0, fontSize = 14.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = theme.text2.copy(0.55f), fontSize = 14.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun NumberInputInline(value: String, onValueChange: (String) -> Unit) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(6)) },
            textStyle = TextStyle(color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Black),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TargetTypeOption(
    type: ChallengeTargetType,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accent.copy(0.14f) else theme.bg1.copy(0.5f))
            .border(
                1.dp,
                if (isActive) accent.copy(0.5f) else theme.stroke.copy(0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isActive) accent else Color.Transparent)
                .border(1.dp, if (isActive) accent else theme.text2, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            type.label,
            color = if (isActive) theme.text0 else theme.text1,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(type.unit, color = theme.text2, fontSize = 11.sp)
    }
}

@Composable
private fun VisibilityChip(
    label: String,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(accent.copy(0.22f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
    }
}
