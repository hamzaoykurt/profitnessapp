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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onThemeChange              : (AppThemeState) -> Unit,
    onNavigateToPerformance    : () -> Unit = {},
    onNavigateToAchievements   : () -> Unit = {},
    onLogout                   : () -> Unit = {},
    onEditProfile              : () -> Unit = {},
    viewModel                  : ProfileViewModel = hiltViewModel()
) {
    val theme   = LocalAppTheme.current
    val accent  = MaterialTheme.colorScheme.primary
    val strings = theme.strings
    val state   by viewModel.uiState.collectAsStateWithLifecycle()

    var showAppearance       by remember { mutableStateOf(false) }
    var showNotifications    by remember { mutableStateOf(false) }
    var showLanguagePicker   by remember { mutableStateOf(false) }
    var achievementPopup     by remember { mutableStateOf<Triple<String, String, String>?>(null) } // icon, name, description

    // Achievement unlock bildirimi
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.AchievementUnlocked ->
                    achievementPopup = Triple(event.icon, event.name, event.description)
                is ProfileEvent.ShowSnackbar -> {} // snackbar handled elsewhere
            }
        }
    }

    // Tab geçişinde stale ise yenile (5 dk cache)
    LaunchedEffect(Unit) { viewModel.reloadIfStale() }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                ProfileHeroBanner(
                    name            = state.displayName.ifBlank { "Kullanıcı" },
                    avatar          = state.avatar,
                    rank            = state.rank,
                    level           = state.level,
                    xp              = state.xp,
                    xpPerLevel      = state.xpPerLevel,
                    fitnessGoal     = state.fitnessGoal,
                    accent          = accent,
                    theme           = theme,
                    onSettingsClick = { showAppearance = true },
                    onEditAvatar    = onEditProfile
                )
            }
            item {
                PerformanceMetricsSection(
                    accent             = accent,
                    theme              = theme,
                    strings            = strings,
                    currentStreak      = state.currentStreak,
                    longestStreak      = state.longestStreak,
                    totalWorkouts      = state.totalWorkouts,
                    totalExercises     = state.totalExercises,
                    onNavigateToDetail = onNavigateToPerformance
                )
            }
            item {
                BodyMetricsCard(
                    heightCm   = state.heightCm,
                    weightKg   = state.weightKg,
                    bmi        = state.bmi,
                    bodyFatPct = state.bodyFatPct,
                    accent     = accent,
                    theme      = theme
                )
            }
            item {
                WeeklyActivitySection(
                    accent         = accent,
                    theme          = theme,
                    strings        = strings,
                    weeklyActivity = state.weeklyActivity
                )
            }
            item {
                TrophyGallery(
                    accent       = accent,
                    theme        = theme,
                    strings      = strings,
                    achievements = state.achievements,
                    onSeeAll     = onNavigateToAchievements
                )
            }
            item {
                SettingsSection(
                    theme                = theme,
                    accent               = accent,
                    strings              = strings,
                    onLogout             = onLogout,
                    onEditProfile        = onEditProfile,
                    onNotificationsClick = { showNotifications = true },
                    onLanguageClick      = { showLanguagePicker = true },
                    displayName          = state.displayName.ifBlank { "Kullanıcı" },
                    avatar               = state.avatar
                )
            }
        }
    }

    // ── Achievement Popup ─────────────────────────────────────────────────────
    achievementPopup?.let { (icon, name, desc) ->
        LaunchedEffect(achievementPopup) {
            kotlinx.coroutines.delay(4500)
            achievementPopup = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.75f))
                .clickable { achievementPopup = null },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.scaleIn(
                    initialScale = 0.6f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    )
                ) + androidx.compose.animation.fadeIn()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0D2B1F), Color(0xFF051A12)))
                        )
                        .border(1.dp, accent.copy(0.5f), RoundedCornerShape(28.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Glow ring around icon
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(accent.copy(0.18f))
                                .border(2.dp, accent.copy(0.6f), CircleShape)
                        )
                        Text(icon, fontSize = 52.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "YENİ BAŞARIM!",
                            color         = accent,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                        Text(
                            name,
                            color      = Snow,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (desc.isNotBlank()) {
                            Text(
                                desc,
                                color     = Snow.copy(0.6f),
                                fontSize  = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.15f))
                            .border(1.dp, accent.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Harika! Devam et 🔥",
                            color      = accent,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── Appearance bottom sheet ───────────────────────────────────────────────
    if (showAppearance) {
        ModalBottomSheet(
            onDismissRequest = { showAppearance = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ThemeSettingsSheet(
                current = theme,
                strings = strings,
                onApply = { newTheme ->
                    onThemeChange(newTheme)
                    showAppearance = false
                }
            )
        }
    }

    // ── Notifications bottom sheet ────────────────────────────────────────────
    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            NotificationsSettingsSheet(
                currentEnabled = theme.notificationsEnabled,
                strings        = strings,
                accent         = accent,
                theme          = theme,
                onApply        = { enabled ->
                    onThemeChange(theme.copy(notificationsEnabled = enabled))
                    showNotifications = false
                }
            )
        }
    }

    // ── Language picker bottom sheet ──────────────────────────────────────────
    if (showLanguagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showLanguagePicker = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LanguageSettingsSheet(
                current = theme.language,
                strings = strings,
                accent  = accent,
                theme   = theme,
                onApply = { lang ->
                    onThemeChange(theme.copy(language = lang))
                    showLanguagePicker = false
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
    rank           : String,
    level          : Int,
    xp             : Int,
    xpPerLevel     : Int,
    fitnessGoal    : String = "",
    accent         : Color,
    theme          : AppThemeState,
    onSettingsClick: () -> Unit,
    onEditAvatar   : () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PROFITNESS",
                        color         = theme.text0.copy(0.5f),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Fitness Koçun",
                        color      = accent.copy(0.8f),
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

            Box(modifier = Modifier.size(110.dp)) {
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(accent)
                )
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(theme.bg2),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.startsWith("http")) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(avatar).crossfade(true).build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(avatar, fontSize = 40.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(theme.bg0)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onEditAvatar),
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

            Text(
                name.uppercase(),
                color         = theme.text0,
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )

            if (fitnessGoal.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    fitnessGoal,
                    color    = theme.text1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val rankColor = rankColor(rank)
                BadgeChip("★ ${rank.uppercase()}", rankColor)
                BadgeChip("LVL $level", accent)
            }

            Spacer(Modifier.height(20.dp))

            val xpProgress = if (xpPerLevel > 0) (xp % xpPerLevel).toFloat() / xpPerLevel else 0f
            Column(
                modifier              = Modifier.width(220.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "LEVEL $level → ${level + 1}",
                        color         = theme.text2,
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text("${xp % xpPerLevel} XP", color = theme.text1, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                            .fillMaxWidth(xpProgress.coerceIn(0f, 1f))
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

private fun rankColor(rank: String) = when (rank.lowercase()) {
    "silver"   -> Color(0xFFB0BEC5)
    "gold"     -> Color(0xFFFFD700)
    "platinum" -> Color(0xFF00E5FF)
    "diamond"  -> Color(0xFF64B5F6)
    else       -> Color(0xFFCD7F32)  // Bronze
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
    strings           : AppStrings,
    currentStreak     : Int,
    longestStreak     : Int,
    totalWorkouts     : Int,
    totalExercises    : Int,
    onNavigateToDetail: () -> Unit
) {
    val metrics = listOf(
        PerformanceMetric(totalWorkouts.toString(),  strings.unitDays,   strings.activeDaysLabel,  Icons.Rounded.CalendarToday, CardPurple),
        PerformanceMetric(currentStreak.toString(),  strings.unitStreak, strings.dailyStreakLabel,  Icons.Rounded.Whatshot,      CardCoral),
        PerformanceMetric(longestStreak.toString(),  strings.unitStreak, "EN UZUN SERİ",            Icons.Rounded.EmojiEvents,   CardGreen),
        PerformanceMetric(totalExercises.toString(), "kez",              "TOPLAM EGZERSİZ",         Icons.Rounded.FitnessCenter, CardCyan),
    )

    Column(modifier = Modifier.padding(top = 32.dp)) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                strings.performanceMetrics,
                style         = MaterialTheme.typography.labelSmall,
                color         = accent,
                letterSpacing = 2.sp
            )
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
                    strings.seeAll,
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
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(metric.color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(20.dp))
            }

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
private fun WeeklyActivitySection(
    accent         : Color,
    theme          : AppThemeState,
    strings        : AppStrings,
    weeklyActivity : List<Float>
) {
    val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1   // 0=Pzt … 6=Paz

    Column(modifier = Modifier.padding(20.dp, 36.dp, 20.dp, 0.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                strings.weeklyActivity,
                style         = MaterialTheme.typography.labelSmall,
                color         = accent,
                letterSpacing = 2.sp
            )
            Text(strings.thisWeekSummary, color = theme.text2, fontSize = 10.sp)
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
                strings.dayAbbreviations.forEachIndexed { i, day ->
                    val level   = weeklyActivity.getOrElse(i) { 0f }.coerceIn(0f, 1f)
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
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.bg3)
                            )
                            if (level > 0f) {
                                // En az %15 yükseklik göster (çok küçük olmasın)
                                val displayLevel = level.coerceAtLeast(0.15f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(displayLevel)
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

// ── Trophy Gallery (Gerçek Başarımlar) ────────────────────────────────────────

private fun achievementColor(category: String): Pair<Color, Color> = when (category) {
    "streak"    -> Pair(Color(0xFFF97316), Color(0xFFEF4444))
    "volume"    -> Pair(Color(0xFF9B59FF), Color(0xFF6C35DE))
    "xp"        -> Pair(Color(0xFFFFD700), Color(0xFFFF8C00))
    "milestone" -> Pair(Color(0xFF00E5D3), Color(0xFF3B82F6))
    else        -> Pair(Color(0xFFEC4899), Color(0xFFA855F7))
}

@Composable
private fun TrophyGallery(
    accent       : Color,
    theme        : AppThemeState,
    strings      : AppStrings,
    achievements : List<AchievementUiModel>,
    onSeeAll     : () -> Unit = {}
) {
    // Önce açılanlar, sonra kilitliler; max 12 göster
    val sorted = achievements.sortedByDescending { it.isUnlocked }.take(12)

    Column(modifier = Modifier.padding(top = 36.dp)) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                strings.achievements,
                style         = MaterialTheme.typography.labelSmall,
                color         = accent,
                letterSpacing = 2.sp
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val unlockedCount = achievements.count { it.isUnlocked }
                Text(
                    "$unlockedCount/${achievements.size}",
                    color    = theme.text2,
                    fontSize = 10.sp
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(0.1f))
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(strings.seeAll, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (sorted.isEmpty()) {
            // Loading placeholder
            Text(
                "Başarımlar yükleniyor...",
                color    = theme.text2,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sorted) { ach ->
                    AchievementCard(achievement = ach, theme = theme)
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementUiModel, theme: AppThemeState) {
    val (colorFrom, colorTo) = achievementColor(achievement.category)
    val alpha = if (achievement.isUnlocked) 1f else 0.35f

    Box(
        modifier = Modifier
            .size(118.dp, 158.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colorFrom.copy(if (achievement.isUnlocked) 0.15f else 0.05f), colorTo.copy(0.03f)),
                    start  = Offset(0f, 0f),
                    end    = Offset(300f, 450f)
                )
            )
            .border(
                1.dp,
                if (achievement.isUnlocked) colorFrom.copy(0.4f) else theme.stroke,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colorFrom.copy(if (achievement.isUnlocked) 0.15f else 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (achievement.isUnlocked) achievement.icon else "🔒",
                    fontSize = 26.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                achievement.name.uppercase().take(10),
                color         = if (achievement.isUnlocked) colorFrom else theme.text2,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                achievement.description.take(28),
                color      = theme.text2.copy(alpha),
                fontSize   = 7.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 10.sp
            )
        }
    }
}

// ── Settings Section ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    theme               : AppThemeState,
    accent              : Color,
    strings             : AppStrings,
    onLogout            : () -> Unit = {},
    onEditProfile       : () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLanguageClick     : () -> Unit = {},
    displayName         : String     = "",
    avatar              : String     = "🏋️"
) {
    Column(modifier = Modifier.padding(20.dp, 40.dp, 20.dp, 0.dp)) {
        Text(
            strings.accountSettings,
            style         = MaterialTheme.typography.labelSmall,
            color         = theme.text1,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(0.08f))
                .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp))
                .clickable(onClick = onEditProfile)
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.18f))
                    .border(1.5.dp, accent.copy(0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatar.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(avatar).crossfade(true).build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(avatar, fontSize = 22.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    displayName.ifBlank { "Kullanıcı" }.uppercase(),
                    color      = accent,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    strings.editProfileHint,
                    color    = theme.text2,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    null,
                    tint     = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
        ) {
            val notifStatus = if (theme.notificationsEnabled) strings.notificationsActive
                              else strings.notificationsOff
            SettingsRow(
                icon    = Icons.Rounded.Notifications,
                label   = strings.notificationsLabel,
                sub     = notifStatus,
                theme   = theme,
                onClick = onNotificationsClick
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(
                icon    = Icons.Rounded.Language,
                label   = strings.languageLabel,
                sub     = strings.currentLanguageName,
                theme   = theme,
                onClick = onLanguageClick
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(
                icon    = Icons.Rounded.Security,
                label   = strings.securityLabel,
                sub     = strings.securityValue,
                theme   = theme,
                onClick = {}
            )
        }

        Spacer(Modifier.height(12.dp))

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
                strings.logoutLabel,
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

// ── Appearance / Theme Settings Sheet ────────────────────────────────────────

@Composable
private fun ThemeSettingsSheet(
    current: AppThemeState,
    strings: AppStrings,
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
        SheetHandle(theme)

        Text(
            strings.appearanceTitle,
            style         = MaterialTheme.typography.labelSmall,
            color         = primary,
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.Black
        )


        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.accentColorLabel, color = theme.text1, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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
            onClick  = { onApply(current.copy(isDark = isDark, accent = accent)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
        }
    }
}

// ── Notifications Settings Sheet ──────────────────────────────────────────────

@Composable
private fun NotificationsSettingsSheet(
    currentEnabled: Boolean,
    strings       : AppStrings,
    accent        : Color,
    theme         : AppThemeState,
    onApply       : (Boolean) -> Unit
) {
    var workoutReminders by remember { mutableStateOf(currentEnabled) }
    var progressUpdates  by remember { mutableStateOf(currentEnabled) }
    var newsAlerts       by remember { mutableStateOf(currentEnabled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp, 24.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SheetHandle(theme)

        Text(
            strings.notifSheetTitle,
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.Black
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.bg2)
                .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
        ) {
            NotifToggleRow(
                label           = strings.workoutReminders,
                icon            = Icons.Rounded.FitnessCenter,
                checked         = workoutReminders,
                accent          = accent,
                theme           = theme,
                onCheckedChange = { workoutReminders = it }
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            NotifToggleRow(
                label           = strings.progressUpdates,
                icon            = Icons.Rounded.ShowChart,
                checked         = progressUpdates,
                accent          = accent,
                theme           = theme,
                onCheckedChange = { progressUpdates = it }
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            NotifToggleRow(
                label           = strings.newsAlerts,
                icon            = Icons.Rounded.Newspaper,
                checked         = newsAlerts,
                accent          = accent,
                theme           = theme,
                onCheckedChange = { newsAlerts = it }
            )
        }

        Button(
            onClick  = { onApply(workoutReminders || progressUpdates || newsAlerts) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NotifToggleRow(
    label          : String,
    icon           : ImageVector,
    checked        : Boolean,
    accent         : Color,
    theme          : AppThemeState,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.bg3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint     = if (checked) accent else theme.text2,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            label,
            color      = theme.text0,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.weight(1f)
        )
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedTrackColor   = accent,
                checkedThumbColor   = Color.White,
                uncheckedTrackColor = theme.bg3,
                uncheckedThumbColor = theme.text2
            )
        )
    }
}

// ── Language Settings Sheet ───────────────────────────────────────────────────

@Composable
private fun LanguageSettingsSheet(
    current: AppLanguage,
    strings: AppStrings,
    accent : Color,
    theme  : AppThemeState,
    onApply: (AppLanguage) -> Unit
) {
    var selected by remember { mutableStateOf(current) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp, 24.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SheetHandle(theme)

        Text(
            strings.langSheetTitle,
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.Black
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.bg2)
                .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
        ) {
            LanguageOptionRow(
                flag       = "🇹🇷",
                name       = strings.turkishLabel,
                isSelected = selected == AppLanguage.TURKISH,
                accent     = accent,
                theme      = theme,
                onClick    = { selected = AppLanguage.TURKISH }
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            LanguageOptionRow(
                flag       = "🇬🇧",
                name       = strings.englishLabel,
                isSelected = selected == AppLanguage.ENGLISH,
                accent     = accent,
                theme      = theme,
                onClick    = { selected = AppLanguage.ENGLISH }
            )
        }

        Button(
            onClick  = { onApply(selected) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LanguageOptionRow(
    flag      : String,
    name      : String,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    onClick   : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) accent.copy(0.07f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(flag, fontSize = 24.sp)
        Text(
            name,
            color      = if (isSelected) accent else theme.text0,
            fontSize   = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SheetHandle(theme: AppThemeState) {
    Box(
        modifier         = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(theme.text2.copy(0.4f))
        )
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

// ── Vücut Metrikleri Kartı ────────────────────────────────────────────────────

@Composable
private fun BodyMetricsCard(
    heightCm   : Double,
    weightKg   : Double,
    bmi        : Double,
    bodyFatPct : Double,
    accent     : Color,
    theme      : AppThemeState
) {
    if (heightCm <= 0 && weightKg <= 0) return

    val bmiColor = when {
        bmi <= 0   -> accent
        bmi < 18.5 -> Color(0xFF64B5F6)
        bmi < 25.0 -> Color(0xFF4CAF50)
        bmi < 30.0 -> Color(0xFFFFB74D)
        else       -> Color(0xFFEF5350)
    }
    val bmiLabel = when {
        bmi <= 0   -> ""
        bmi < 18.5 -> "Zayıf"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Fazla Kilolu"
        else       -> "Obez"
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "VÜCUT METRİKLERİ",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (heightCm > 0) {
                BodyMetricTile(
                    value    = "${heightCm.toInt()}",
                    unit     = "cm",
                    label    = "BOY",
                    color    = CardCyan,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (weightKg > 0) {
                BodyMetricTile(
                    value    = "${weightKg.toInt()}",
                    unit     = "kg",
                    label    = "KİLO",
                    color    = CardPurple,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (bmi > 0) {
                BodyMetricTile(
                    value    = "%.1f".format(bmi),
                    unit     = bmiLabel,
                    label    = "BMI",
                    color    = bmiColor,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (bodyFatPct > 0) {
                BodyMetricTile(
                    value    = "%.1f".format(bodyFatPct),
                    unit     = "%",
                    label    = "YAĞ",
                    color    = CardCoral,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BodyMetricTile(
    value   : String,
    unit    : String,
    label   : String,
    color   : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(unit, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = theme.text2, fontSize = 8.sp, letterSpacing = 0.5.sp)
        }
    }
}
