package com.avonix.profitness.presentation.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onThemeChange: (AppThemeState) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var displayName  by remember { mutableStateOf("Hamza Oykurt") }
    var avatar       by remember { mutableStateOf("🏋️") }
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 140.dp)
        ) {
            // ── Legend Profile Hero ──────────────────────────────────────────────
            item {
                AthleteLegendHero(
                    name    = displayName,
                    avatar  = avatar,
                    accent  = accent,
                    theme   = theme,
                    onSettingsClick = { showSettings = true }
                )
            }

            // ── Stat Grid ────────────────────────────────────────────────────────
            item {
                Column(Modifier.padding(24.dp, 40.dp, 24.dp, 0.dp)) {
                    Text("PERFORMANS ÖLÇÜTLERİ", style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 2.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PerformanceStatCard("14%", "YAĞ ORANI", CardCyan, Modifier.weight(1f))
                        PerformanceStatCard("12.4s", "AKTIF SÜRE", accent, Modifier.weight(1f))
                    }
                }
            }

            // ── Trophy Gallery ───────────────────────────────────────────────────
            item {
                TrophyGallery(accent = accent, theme = theme)
            }

            // ── Settings ─────────────────────────────────────────────────────────
            item {
                Column(Modifier.padding(24.dp, 48.dp, 24.dp, 0.dp)) {
                    Text("HESAP VE AYARLAR", style = MaterialTheme.typography.labelSmall, color = theme.text1, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    SettingsLuxuryRow(Icons.Rounded.Notifications, "Bildirimler", "Aktif", theme = theme)
                    SettingsLuxuryRow(Icons.Rounded.Language, "Dil", "Türkçe", theme = theme)
                    SettingsLuxuryRow(Icons.Rounded.Security, "Güvenlik", "Yüksek", theme = theme)
                    Spacer(Modifier.height(16.dp))
                    SettingsLuxuryRow(Icons.Rounded.Logout, "Çıkış Yap", "", tint = CardCoral, theme = theme)
                }
            }
        }
    }

    // ── Theme Settings Bottom Sheet ──────────────────────────────────────────
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ThemeSettingsSheet(
                current  = theme,
                onApply  = { newTheme ->
                    onThemeChange(newTheme)
                    showSettings = false
                }
            )
        }
    }
}

@Composable
private fun ThemeSettingsSheet(
    current: AppThemeState,
    onApply: (AppThemeState) -> Unit
) {
    var isDark  by remember { mutableStateOf(current.isDark) }
    var accent  by remember { mutableStateOf(current.accent) }
    val theme   = LocalAppTheme.current
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp, 24.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Handle
        Box(
            Modifier
                .width(40.dp).height(4.dp)
                .clip(CircleShape)
                .background(theme.text2.copy(0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Text(
            "GÖRÜNÜM AYARLARI",
            style       = MaterialTheme.typography.labelSmall,
            color       = primary,
            letterSpacing = 3.sp,
            fontWeight  = FontWeight.Black
        )

        // ── Dark / Light Segmented Control ───────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MOD", color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg3),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ModeOption(
                    label      = "KARANLIK",
                    icon       = Icons.Rounded.DarkMode,
                    isSelected = isDark,
                    accent     = primary,
                    theme      = theme,
                    modifier   = Modifier.weight(1f),
                    onClick    = { isDark = true }
                )
                ModeOption(
                    label      = "AYDINLIK",
                    icon       = Icons.Rounded.LightMode,
                    isSelected = !isDark,
                    accent     = primary,
                    theme      = theme,
                    modifier   = Modifier.weight(1f),
                    onClick    = { isDark = false }
                )
            }
        }

        // ── Accent Color Presets ──────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("VURGU RENGİ", color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccentPreset.entries.forEach { preset ->
                    ColorSwatch(
                        preset     = preset,
                        isSelected = accent == preset,
                        onClick    = { accent = preset }
                    )
                }
            }
        }

        // ── Apply Button ─────────────────────────────────────────────────────
        Button(
            onClick  = { onApply(AppThemeState(isDark = isDark, accent = accent)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor   = if (accent == current.accent && isDark == current.isDark)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("UYGULA", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ModeOption(
    label     : String,
    icon      : androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    modifier  : Modifier = Modifier,
    onClick   : () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accent else theme.text2,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                color      = if (isSelected) accent else theme.text2,
                fontSize   = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    preset    : AccentPreset,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) preset.color.copy(0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) preset.color else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(preset.color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint     = preset.onColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AthleteLegendHero(
    name           : String,
    avatar         : String,
    accent         : Color,
    theme          : AppThemeState,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(24.dp, 80.dp, 24.dp, 0.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.bg2, theme.bg3),
                    start  = Offset(0f, 0f),
                    end    = Offset(400f, 800f)
                )
            )
            .border(1.dp, WhiteGlow, RoundedCornerShape(32.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(WhiteGlow.copy(0.1f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = size.width
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("MEMBER ID", color = theme.text0.copy(0.3f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("#4492-AVX", color = theme.text0.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Settings button
                    IconButton(
                        onClick  = onSettingsClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(theme.bg0.copy(0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Tune, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatar, fontSize = 24.sp)
                    }
                }
            }

            Column {
                Text("ATLET", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                Text(name.uppercase(), style = MaterialTheme.typography.displaySmall, color = theme.text0, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(8.dp))
                    Text("GOLD MEMBERSHIP", color = theme.text0.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("PROGRESSION", color = theme.text0.copy(0.3f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Lvl 12 Elite", color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.height(4.dp).width(120.dp).clip(CircleShape).background(theme.bg3)) {
                    Box(Modifier.fillMaxWidth(0.81f).fillMaxHeight().background(accent))
                }
            }
        }
    }
}

@Composable
private fun PerformanceStatCard(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.bg1.copy(0.4f))
            .border(1.dp, theme.stroke, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(0.1f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            }
            Column {
                Text(value, color = theme.text0, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(label, color = theme.text1, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun TrophyGallery(accent: Color, theme: AppThemeState) {
    Column(modifier = Modifier.padding(0.dp, 40.dp, 0.dp, 0.dp)) {
        Text(
            "LEGENDARY ACHIEVEMENTS",
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(24.dp, 0.dp)
        )

        Spacer(Modifier.height(16.dp))

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(5) { i ->
                Box(
                    modifier = Modifier
                        .size(120.dp, 160.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.bg2)
                        .border(1.dp, WhiteGlow.copy(0.1f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("🏆", "🎖️", "🔥", "💎", "🌟")[i], fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            listOf("CHAMP", "STREAK", "ELITE", "LEGEND", "MASTER")[i],
                            color      = theme.text0,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLuxuryRow(
    icon    : ImageVector,
    label   : String,
    sub     : String,
    tint    : Color = Color.Unspecified,
    theme   : AppThemeState
) {
    val rowTint = if (tint == Color.Unspecified) theme.text0 else tint
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(theme.bg2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = rowTint.copy(0.7f), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = rowTint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) {
                Text(sub, color = theme.text1, fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = theme.text2, modifier = Modifier.size(18.dp))
    }
}
