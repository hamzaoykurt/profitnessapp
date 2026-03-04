package com.avonix.profitness.presentation.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onThemeChange: (AppThemeState) -> Unit,
    onNavigateToPerformance: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                ProfileHeroBanner(
                    name            = "Hamza Oykurt",
                    avatar          = "🏋️",
                    accent          = accent,
                    theme           = theme,
                    onSettingsClick = { showSettings = true }
                )
            }
            item {
                PerformanceMetricsSection(
                    accent             = accent,
                    theme              = theme,
                    onNavigateToDetail = onNavigateToPerformance
                )
            }
            item { WeeklyActivitySection(accent = accent, theme = theme) }
            item { TrophyGallery(accent = accent, theme = theme) }
            item { SettingsSection(theme = theme, accent = accent, onLogout = onLogout) }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ThemeSettingsSheet(
                current = theme,
                onApply = { newTheme ->
                    onThemeChange(newTheme)
                    showSettings = false
                }
            )
        }
    }
}

// ── Profile Hero Banner ───────────────────────────────────────────────────────

@Composable
private fun ProfileHeroBanner(
    name           : String,
    avatar         : String,
    accent         : Color,
    theme          : AppThemeState,
    onSettingsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Accent gradient wash at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: member ID + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MEMBER ID",
                        color         = theme.text0.copy(0.4f),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "#4492-AVX",
                        color      = theme.text0.copy(0.65f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.bg1.copy(0.75f))
                        .border(1.dp, theme.stroke, CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Tune, null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Avatar with accent ring + edit button
            Box(modifier = Modifier.size(110.dp)) {
                // Accent ring (outer)
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(accent)
                )
                // Inner bg circle (creates ring effect)
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(theme.bg2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatar, fontSize = 40.sp)
                }
                // Edit button (bottom-end)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(theme.bg0)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        null,
                        tint     = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Name
            Text(
                name.uppercase(),
                color         = theme.text0,
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            // Badge chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BadgeChip("★ GOLD", Color(0xFFFFD700))
                BadgeChip("LVL 12 ELİTE", accent)
            }

            Spacer(Modifier.height(20.dp))

            // XP Progress bar
            Column(
                modifier              = Modifier.width(220.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "LEVEL 12 → 13",
                        color         = theme.text2,
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text("8,100 XP", color = theme.text1, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(theme.bg3)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.81f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accent, accent.copy(0.65f))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color         = color,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

// ── Performance Metrics (Horizontal Scroll) ───────────────────────────────────

private data class PerformanceMetric(
    val value : String,
    val unit  : String,
    val label : String,
    val icon  : ImageVector,
    val color : Color
)

@Composable
private fun PerformanceMetricsSection(
    accent            : Color,
    theme             : AppThemeState,
    onNavigateToDetail: () -> Unit
) {
    val metrics = listOf(
        PerformanceMetric("14",    "%",    "YAĞ ORANI",        Icons.Rounded.Speed,         CardCyan),
        PerformanceMetric("72",    "kg",   "KAS KÜTLESİ",      Icons.Rounded.FitnessCenter, accent),
        PerformanceMetric("127",   "gün",  "AKTİF GÜN",        Icons.Rounded.CalendarToday, CardPurple),
        PerformanceMetric("12",    "seri", "GÜNLÜK SERİ",       Icons.Rounded.Whatshot,      CardCoral),
        PerformanceMetric("48",    "ml",   "VO2 MAX",           Icons.Rounded.Air,           CardGreen),
        PerformanceMetric("22.4",  "BMI",  "VÜCUT KİTLE İND.", Icons.Rounded.Star,          Color(0xFFFFD700)),
        PerformanceMetric("8.420", "kcal", "HAFTALIK KALORİ",  Icons.Rounded.Favorite,      CardCoral),
    )

    Column(modifier = Modifier.padding(top = 32.dp)) {
        // Section header with "see all" arrow
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "PERFORMANS ÖLÇÜTLERİ",
                style         = MaterialTheme.typography.labelSmall,
                color         = accent,
                letterSpacing = 2.sp
            )
            // Navigate to detail arrow
            Row(
                modifier          = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(0.1f))
                    .clickable(onClick = onNavigateToDetail)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Tümünü Gör",
                    color         = accent,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold
                )
                Icon(
                    Icons.Rounded.ArrowForwardIos,
                    null,
                    tint     = accent,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Horizontal scrollable cards
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(metrics) { metric ->
                MetricCard(metric = metric, theme = theme)
            }
        }
    }
}

@Composable
private fun MetricCard(metric: PerformanceMetric, theme: AppThemeState) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(160.dp)
            .glassCard(metric.color, theme, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(metric.color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(20.dp))
            }

            // Value + unit
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.value,
                        color      = theme.text0,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        metric.unit,
                        color      = metric.color,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(bottom = 3.dp)
                    )
                }
                Text(
                    metric.label,
                    color         = theme.text2,
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Bottom accent bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(metric.color, metric.color.copy(0.2f))
                        )
                    )
            )
        }
    }
}

// ── Weekly Activity ───────────────────────────────────────────────────────────

@Composable
private fun WeeklyActivitySection(accent: Color, theme: AppThemeState) {
    val days       = listOf("Pzt", "Sal", "Çrş", "Per", "Cum", "Cmt", "Paz")
    val levels     = listOf(0.85f, 0.6f, 0.9f, 0.4f, 1.0f, 0.7f, 0f)
    val todayIndex = 4

    Column(modifier = Modifier.padding(20.dp, 36.dp, 20.dp, 0.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "HAFTALIK AKTİVİTE",
                style         = MaterialTheme.typography.labelSmall,
                color         = accent,
                letterSpacing = 2.sp
            )
            Text("Bu hafta 5 antrenman", color = theme.text2, fontSize = 10.sp)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(accent, theme, RoundedCornerShape(20.dp))
                .padding(20.dp, 20.dp, 20.dp, 14.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                days.forEachIndexed { i, day ->
                    val level   = levels[i]
                    val isToday = i == todayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .width(28.dp)
                                .height(72.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Background bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.bg3)
                            )
                            // Fill bar
                            if (level > 0f) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(level)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isToday) accent else accent.copy(0.4f)
                                        )
                                )
                            }
                        }
                        Text(
                            day,
                            color      = if (isToday) accent else theme.text2,
                            fontSize   = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ── Trophy Gallery ────────────────────────────────────────────────────────────

private data class TrophyData(
    val emoji    : String,
    val label    : String,
    val subtitle : String,
    val colorFrom: Color,
    val colorTo  : Color
)

@Composable
private fun TrophyGallery(accent: Color, theme: AppThemeState) {
    val trophies = listOf(
        TrophyData("🏆", "CHAMP",   "İlk Zafer",   Color(0xFFFFD700), Color(0xFFFF8C00)),
        TrophyData("🎖️", "STREAK",  "7 Günlük",    Color(0xFF9B59FF), Color(0xFF6C35DE)),
        TrophyData("🔥", "ELİTE",   "50 Antrenman", Color(0xFFF97316), Color(0xFFEF4444)),
        TrophyData("💎", "LEGEND",  "Süper Üye",    Color(0xFF00E5D3), Color(0xFF3B82F6)),
        TrophyData("🌟", "MASTER",  "Mükemmellik",  Color(0xFFEC4899), Color(0xFFA855F7)),
    )

    Column(modifier = Modifier.padding(top = 36.dp)) {
        Text(
            "BAŞARILAR",
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trophies) { trophy ->
                TrophyCard(trophy = trophy, theme = theme)
            }
        }
    }
}

@Composable
private fun TrophyCard(trophy: TrophyData, theme: AppThemeState) {
    Box(
        modifier = Modifier
            .size(118.dp, 158.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        trophy.colorFrom.copy(0.15f),
                        trophy.colorTo.copy(0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(300f, 450f)
                )
            )
            .border(1.dp, trophy.colorFrom.copy(0.3f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(trophy.colorFrom.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(trophy.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                trophy.label,
                color         = trophy.colorFrom,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                trophy.subtitle,
                color      = theme.text2,
                fontSize   = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Settings Section ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(theme: AppThemeState, accent: Color, onLogout: () -> Unit = {}) {
    Column(modifier = Modifier.padding(20.dp, 40.dp, 20.dp, 0.dp)) {
        Text(
            "HESAP VE AYARLAR",
            style         = MaterialTheme.typography.labelSmall,
            color         = theme.text1,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(16.dp))

        // Edit profile card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(0.08f))
                .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp))
                .clickable {}
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("PROFİLİ DÜZENLE", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Ad, avatar ve hedeflerini güncelle", color = theme.text2, fontSize = 11.sp)
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint     = accent.copy(0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Grouped settings rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
        ) {
            SettingsRow(Icons.Rounded.Notifications, "Bildirimler", "Aktif",   theme, onClick = {})
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(Icons.Rounded.Language,      "Dil",         "Türkçe",  theme, onClick = {})
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(Icons.Rounded.Security,      "Güvenlik",    "Yüksek",  theme, onClick = {})
        }

        Spacer(Modifier.height(12.dp))

        // Logout row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardCoral.copy(0.07f))
                .border(1.dp, CardCoral.copy(0.2f), RoundedCornerShape(16.dp))
                .clickable(onClick = onLogout)
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardCoral.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Logout, null, tint = CardCoral, modifier = Modifier.size(20.dp))
            }
            Text(
                "Çıkış Yap",
                color      = CardCoral,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint     = CardCoral.copy(0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon   : ImageVector,
    label  : String,
    sub    : String,
    theme  : AppThemeState,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.bg2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = theme.text1, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub,   color = theme.text2, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = theme.text2, modifier = Modifier.size(16.dp))
    }
}

// ── Theme Settings Sheet ──────────────────────────────────────────────────────

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
        Box(
            Modifier
                .width(40.dp).height(4.dp)
                .clip(CircleShape)
                .background(theme.text2.copy(0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Text(
            "GÖRÜNÜM AYARLARI",
            style         = MaterialTheme.typography.labelSmall,
            color         = primary,
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.Black
        )

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

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("VURGU RENGİ", color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(
                modifier              = Modifier.fillMaxWidth(),
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

        Button(
            onClick  = { onApply(AppThemeState(isDark = isDark, accent = accent)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("UYGULA", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ModeOption(
    label     : String,
    icon      : ImageVector,
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
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (isSelected) accent else theme.text2,
                modifier           = Modifier.size(16.dp)
            )
            Text(
                label,
                color         = if (isSelected) accent else theme.text2,
                fontSize      = 11.sp,
                fontWeight    = if (isSelected) FontWeight.Black else FontWeight.Normal,
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
