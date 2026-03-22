package com.avonix.profitness.presentation.aicoach

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.ai.AICoachPrefs
import com.avonix.profitness.data.ai.CommunicationStyle
import com.avonix.profitness.data.ai.ResponseLength

private const val TOTAL_STEPS = 3

@Composable
fun AICoachOnboardingScreen(
    initialPrefs : AICoachPrefs = AICoachPrefs(),
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onComplete   : (AICoachPrefs) -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var step            by remember { mutableIntStateOf(0) }
    var selectedLength  by remember { mutableStateOf(initialPrefs.responseLength) }
    var selectedStyle   by remember { mutableStateOf(initialPrefs.communicationStyle) }
    var allowProfile    by remember { mutableStateOf(initialPrefs.allowProfileAccess) }
    var allowThirdParty by remember { mutableStateOf(initialPrefs.allowThirdPartyProcessing) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top bar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (step > 0) {
                    IconButton(onClick = { step-- }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = theme.text2)
                    }
                }
                Text(
                    "ORACLE",
                    modifier     = Modifier.align(Alignment.Center),
                    style        = MaterialTheme.typography.labelSmall,
                    color        = accent,
                    letterSpacing = 6.sp,
                    fontWeight   = FontWeight.ExtraLight
                )
            }

            // ── Progress dots ──────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                repeat(TOTAL_STEPS) { idx ->
                    val isActive = idx == step
                    val isDone   = idx < step
                    val width    = animateDpAsState(if (isActive) 20.dp else 6.dp, label = "dot_$idx")
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(width.value)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> accent
                                    isDone   -> accent.copy(0.5f)
                                    else     -> theme.stroke.copy(0.4f)
                                }
                            )
                    )
                }
            }

            // ── Step content ───────────────────────────────────────────────────
            AnimatedContent(
                targetState  = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(280)) { if (forward) it else -it } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(250)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(180)))
                },
                label        = "step_anim",
                modifier     = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentStep ->
                when (currentStep) {
                    0 -> LengthStep(selected = selectedLength, accent = accent, theme = theme) { selectedLength = it }
                    1 -> StyleStep(selected  = selectedStyle,  accent = accent, theme = theme) { selectedStyle  = it }
                    2 -> PermissionsStep(
                        allowProfile    = allowProfile,
                        allowThirdParty = allowThirdParty,
                        accent          = accent,
                        theme           = theme,
                        onProfileChange = { allowProfile    = it },
                        onThirdParty    = { allowThirdParty = it }
                    )
                }
            }

            // ── Next / Finish button ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = bottomPadding + 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(0.75f))))
                    .clickable {
                        if (step < TOTAL_STEPS - 1) {
                            step++
                        } else {
                            onComplete(
                                AICoachPrefs(
                                    responseLength          = selectedLength,
                                    communicationStyle      = selectedStyle,
                                    allowProfileAccess      = allowProfile,
                                    allowThirdPartyProcessing = allowThirdParty,
                                    onboardingCompleted     = true
                                )
                            )
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = if (step < TOTAL_STEPS - 1) "Devam Et" else "Oracle'ı Başlat",
                    color     = Color.White,
                    style     = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Step 1: Yanıt Uzunluğu ────────────────────────────────────────────────────

@Composable
private fun LengthStep(
    selected : ResponseLength,
    accent   : Color,
    theme    : AppThemeState,
    onSelect : (ResponseLength) -> Unit
) {
    StepScaffold(
        title    = "YANIT UZUNLUĞU",
        subtitle = "Oracle sana nasıl cevap versin?"
    ) {
        ResponseLength.entries.forEach { length ->
            CenteredOptionCard(
                title       = length.label,
                description = length.description,
                selected    = selected == length,
                accent      = accent,
                theme       = theme,
                onClick     = { onSelect(length) }
            )
        }
    }
}

// ── Step 2: Konuşma Tarzı ─────────────────────────────────────────────────────

@Composable
private fun StyleStep(
    selected : CommunicationStyle,
    accent   : Color,
    theme    : AppThemeState,
    onSelect : (CommunicationStyle) -> Unit
) {
    StepScaffold(
        title    = "KONUŞMA TARZI",
        subtitle = "Oracle seninle nasıl konuşsun?"
    ) {
        CommunicationStyle.entries.forEach { style ->
            CenteredOptionCard(
                title       = style.label,
                description = style.description,
                selected    = selected == style,
                accent      = accent,
                theme       = theme,
                onClick     = { onSelect(style) }
            )
        }
    }
}

// ── Step 3: İzinler ───────────────────────────────────────────────────────────

@Composable
private fun PermissionsStep(
    allowProfile    : Boolean,
    allowThirdParty : Boolean,
    accent          : Color,
    theme           : AppThemeState,
    onProfileChange : (Boolean) -> Unit,
    onThirdParty    : (Boolean) -> Unit
) {
    StepScaffold(
        title    = "GİZLİLİK & İZİNLER",
        subtitle = "Oracle verilerin hakkında ne bilsin?"
    ) {
        PermissionCard(
            title           = "Profil & Program Erişimi",
            description     = "Oracle ismini, hedefini ve aktif programını bilerek sana özel tavsiyeler verir.",
            checked         = allowProfile,
            recommended     = true,
            accent          = accent,
            theme           = theme,
            onCheckedChange = onProfileChange
        )
        PermissionCard(
            title           = "3. Parti Veri İşleme",
            description     = "Konuşma geçmişinin model iyileştirme amacıyla işlenmesine izin ver.",
            checked         = allowThirdParty,
            recommended     = false,
            accent          = accent,
            theme           = theme,
            onCheckedChange = onThirdParty
        )
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

@Composable
private fun StepScaffold(
    title   : String,
    subtitle: String,
    content : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val theme  = LocalAppTheme.current
        val accent = MaterialTheme.colorScheme.primary

        Text(
            title,
            color        = accent,
            fontSize     = 10.sp,
            letterSpacing = 3.sp,
            fontWeight   = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            color      = theme.text1,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Column(
            verticalArrangement     = Arrangement.spacedBy(10.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
            modifier                = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun CenteredOptionCard(
    title      : String,
    description: String,
    selected   : Boolean,
    accent     : Color,
    theme      : AppThemeState,
    onClick    : () -> Unit
) {
    val borderColor = if (selected) accent.copy(0.65f) else theme.stroke.copy(0.2f)
    val bgBrush = if (selected)
        Brush.horizontalGradient(listOf(accent.copy(0.14f), accent.copy(0.04f)))
    else
        Brush.horizontalGradient(listOf(theme.bg1.copy(0.9f), theme.bg1.copy(0.9f)))

    Row(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = accent,
                unselectedColor = theme.text2.copy(0.4f)
            )
        )
        Column {
            Text(title,       color = theme.text1, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(description, color = theme.text2, fontWeight = FontWeight.Light,  fontSize = 12.sp)
        }
    }
}

@Composable
private fun PermissionCard(
    title          : String,
    description    : String,
    checked        : Boolean,
    recommended    : Boolean,
    accent         : Color,
    theme          : AppThemeState,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.9f))
            .border(1.dp, theme.stroke.copy(0.2f), RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = accent,
                uncheckedThumbColor = theme.text2,
                uncheckedTrackColor = theme.bg0
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(title, color = theme.text1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (recommended) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.copy(0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ÖNERİLEN", color = accent, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text(description, color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.Light, lineHeight = 18.sp)
        }
    }
}
