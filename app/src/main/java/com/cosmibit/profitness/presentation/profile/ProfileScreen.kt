package com.cosmibit.profitness.presentation.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.presentation.components.AccentColorSwatch
import com.cosmibit.profitness.presentation.components.CustomAccentColorDialog
import com.cosmibit.profitness.presentation.components.CustomAccentSwatch
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.data.integration.orbit.OrbitConnectionStatus
import com.cosmibit.profitness.data.integration.orbit.OrbitIntegrationState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onThemeChange              : (AppThemeState) -> Unit,
    onNavigateToPerformance    : () -> Unit = {},
    onNavigateToAchievements   : () -> Unit = {},
    onNavigateToLeaderboard          : (com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab) -> Unit = {},
    onLogout                         : () -> Unit = {},
    onEditProfile              : () -> Unit = {},
    onNavigateToStore          : () -> Unit = {},
    timerExtraPad              : androidx.compose.ui.unit.Dp = 0.dp,
    viewModel                  : ProfileViewModel = hiltViewModel()
) {
    val theme   = LocalAppTheme.current
    val accent  = MaterialTheme.colorScheme.primary
    val strings = theme.strings
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var showAppearance       by remember { mutableStateOf(false) }
    var showNotifications    by remember { mutableStateOf(false) }
    var showLanguagePicker   by remember { mutableStateOf(false) }
    var showIntegrations     by remember { mutableStateOf(false) }
    var achievementPopup     by remember { mutableStateOf<Triple<String, String, String>?>(null) } // icon, name, description

    // Achievement unlock bildirimi
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.AchievementUnlocked ->
                    achievementPopup = Triple(event.icon, event.name, event.description)
                is ProfileEvent.ShowSnackbar -> {} // global snackbar host handles other profile events
                is ProfileEvent.OpenOrbitUrl -> uriHandler.openUri(event.url)
            }
        }
    }

    // Tab geçiş animasyonu bittikten sonra ilk profil yükünü başlat.
    LaunchedEffect(Unit) {
        delay(16)
        viewModel.initLoad()
        viewModel.refreshOrbitStatus()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadIfStale()
                viewModel.refreshOrbitStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp + timerExtraPad)
        ) {
            item {
                ProfileHeroBanner(
                    name             = state.displayName.ifBlank { theme.t("Kullanıcı", "User") },
                    avatar           = state.avatar,
                    rank             = state.rank,
                    level            = state.level,
                    xp               = state.xp,
                    xpPerLevel       = state.xpPerLevel,
                    currentStreak     = state.currentStreak,
                    streakRankPosition = state.streakRankPosition,
                    userPlan         = state.userPlan,
                    aiCredits        = state.aiCredits,
                    accent           = accent,
                    theme            = theme,
                    onSettingsClick  = { showAppearance = true },
                    onNavigateToStore = onNavigateToStore,
                    onOpenXpRanking   = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Xp) },
                    onOpenRankRanking = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Achievements) },
                    onOpenStreakRanking = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Streak) }
                )
            }
            item {
                PerformanceMetricsSection(
                    accent             = accent,
                    theme              = theme,
                    strings            = strings,
                    currentStreak      = state.currentStreak,
                    longestStreak      = state.longestStreak,
                    totalExercises     = state.totalExercises,
                    totalDurationSeconds = state.totalDurationSeconds,
                    totalDistanceMeters  = state.totalDistanceMeters,
                    onNavigateToDetail = onNavigateToPerformance
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
                    onIntegrationsClick  = { showIntegrations = true },
                    orbit                = state.orbitIntegration,
                    displayName          = state.displayName.ifBlank { theme.t("Kullanıcı", "User") },
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
                            theme.t("YENİ BAŞARIM!", "NEW ACHIEVEMENT!"),
                            color         = accent,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                        Text(
                            localizedAchievementText(name, theme),
                            color      = Snow,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (desc.isNotBlank()) {
                            Text(
                                localizedAchievementText(desc, theme),
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

    if (showIntegrations) {
        ModalBottomSheet(
            onDismissRequest = { showIntegrations = false },
            containerColor = theme.bg1,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            OrbitIntegrationSheet(
                orbit = state.orbitIntegration,
                theme = theme,
                accent = accent,
                onConnect = viewModel::connectOrbit,
                onDisconnect = viewModel::disconnectOrbit,
                onSyncEnabled = viewModel::setOrbitSyncEnabled,
                onSyncNow = viewModel::syncOrbitNow,
                onManage = { state.orbitIntegration.manageUrl?.let(uriHandler::openUri) }
            )
        }
    }
}

// ── Profile Hero Banner ───────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileHeroBanner(
    name           : String,
    avatar         : String,
    rank           : String,
    level          : Int,
    xp             : Int,
    xpPerLevel     : Int,
    currentStreak  : Int,
    streakRankPosition: Long,
    userPlan        : UserPlan = UserPlan.FREE,
    aiCredits       : Int = 0,
    accent          : Color,
    theme           : AppThemeState,
    onSettingsClick : () -> Unit,
    onNavigateToStore: () -> Unit = {},
    onOpenXpRanking : () -> Unit = {},
    onOpenRankRanking: () -> Unit = {},
    onOpenStreakRanking: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = if (theme.isDark) 0.07f else 0.025f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        theme.t("Profil", "Profile"),
                        color = theme.text0,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier
                            .size(34.dp)
                            .profilePremiumAction(
                                theme = theme,
                                accent = accent,
                                onClick = onSettingsClick,
                                recessed = true,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Tune, null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                }

                // Enerji / Plan chip — sağ üst köşe, tıklanınca store'a gider
                val isPaid     = userPlan != UserPlan.FREE
                val chipTint   = if (isPaid) Color(0xFFFFD700) else accent
                // Plan varsa: "Elite · 5"  |  Sadece free: "5 Enerji"
                val chipText   = if (isPaid) "${userPlan.displayName} · $aiCredits" else theme.t("$aiCredits Enerji", "$aiCredits Energy")

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .profilePremiumAction(
                            theme = theme,
                            accent = chipTint,
                            onClick = onNavigateToStore,
                            recessed = true,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isPaid) Icons.Rounded.WorkspacePremium else Icons.Rounded.Bolt,
                        null, tint = chipTint, modifier = Modifier.size(14.dp)
                    )
                    Text(
                        chipText,
                        color      = chipTint,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val xpProgress = if (xpPerLevel > 0) (xp % xpPerLevel).toFloat() / xpPerLevel else 0f
            val xpInLevel = if (xpPerLevel > 0) xp % xpPerLevel else xp
            val xpLeft = (xpPerLevel - xpInLevel).coerceAtLeast(0)
            val animatedXpProgress by animateFloatAsState(
                targetValue = xpProgress.coerceIn(0f, 1f),
                animationSpec = tween(560, easing = FastOutSlowInEasing),
                label = "profile_xp_progress"
            )
            val rankColor = rankColor(rank)

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .performanceSignatureSurface(theme, accent, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(theme.bg0),
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
                                Text(avatar, fontSize = 34.sp)
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = theme.text0,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BadgeChip("★ ${rank.uppercase()}", rankColor)
                        BadgeChip("LVL $level", accent)
                    }
                    }
                    }

                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .insetControlSurface(accent, theme, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        theme.t("SEVİYE $level → ${level + 1}", "LEVEL $level → ${level + 1}"),
                                        color = theme.text2,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "$xpInLevel / $xpPerLevel XP",
                                        color = theme.text0,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(accent.copy(alpha = 0.13f))
                                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    if (xpLeft == 0) theme.t("HAZIR", "READY") else theme.t("$xpLeft KALDI", "$xpLeft LEFT"),
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.35.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(theme.bg0.copy(alpha = 0.72f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(animatedXpProgress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent.copy(alpha = 0.72f), accent)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HeroMiniStat(
                            label = theme.t("TOPLAM XP", "TOTAL XP"),
                            value = "$xp",
                            icon = Icons.Rounded.Bolt,
                            color = accent,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenXpRanking
                        )
                        HeroMiniStat(
                            label = theme.t("RÜTBE", "RANK"),
                            value = rank.uppercase(),
                            icon = Icons.Rounded.EmojiEvents,
                            color = rankColor,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenRankRanking
                        )
                        HeroMiniStat(
                            label = theme.t("SERİ", "STREAK"),
                            value = theme.t("$currentStreak gün", "$currentStreak days"),
                            icon = Icons.Rounded.LocalFireDepartment,
                            color = accent,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenStreakRanking
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMiniStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, color = theme.text0, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color         = color,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold
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

private fun formatProfileDurationValue(seconds: Int): String =
    (seconds / 60).coerceAtLeast(1).toString()

private fun formatProfileDistanceValue(meters: Float): String =
    if (meters >= 1000f) "%.1f".format(meters / 1000f) else "%.0f".format(meters)

private fun formatProfileDistanceUnit(meters: Float): String =
    if (meters >= 1000f) "km" else "m"

@Composable
private fun PerformanceMetricsSection(
    accent            : Color,
    theme             : AppThemeState,
    strings           : AppStrings,
    currentStreak     : Int,
    longestStreak     : Int,
    totalExercises    : Int,
    totalDurationSeconds: Int,
    totalDistanceMeters : Float,
    onNavigateToDetail: () -> Unit
) {
    val metrics = buildList {
        add(PerformanceMetric(currentStreak.toString(),  strings.unitStreak, strings.dailyStreakLabel,  Icons.Rounded.LocalFireDepartment, CardCoral))
        add(PerformanceMetric(longestStreak.toString(),  strings.unitStreak, theme.t("En uzun seri", "Longest streak"), Icons.Rounded.EmojiEvents, CardGreen))
        add(PerformanceMetric(totalExercises.toString(), theme.t("kez", "times"), theme.t("Toplam egzersiz", "Total exercises"), Icons.Rounded.FitnessCenter, CardCyan))
        if (totalDurationSeconds > 0) {
            add(PerformanceMetric(formatProfileDurationValue(totalDurationSeconds), theme.t("dk", "min"), theme.t("Toplam süre", "Total duration"), Icons.Rounded.Timer, CardGreen))
        }
        if (totalDistanceMeters > 0f) {
            add(PerformanceMetric(formatProfileDistanceValue(totalDistanceMeters), formatProfileDistanceUnit(totalDistanceMeters), theme.t("Toplam mesafe", "Total distance"), Icons.Rounded.Straighten, Color(0xFF64D2FF)))
        }
    }

    Column(modifier = Modifier.padding(top = 18.dp)) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                theme.t("Performans", "Performance"),
                style         = MaterialTheme.typography.titleLarge,
                color         = theme.text0,
                letterSpacing = (-0.2).sp
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
                MetricCard(
                    metric = metric,
                    theme = theme,
                    onClick = onNavigateToDetail
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: PerformanceMetric,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val visualAccent = theme.effectiveAccentColor
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp)
            .profilePremiumAction(
                theme = theme,
                accent = visualAccent,
                onClick = onClick,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(visualAccent.copy(if (theme.isDark) 0.14f else 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(metric.icon, null, tint = visualAccent, modifier = Modifier.size(22.dp))
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
                        color      = visualAccent,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(bottom = 3.dp)
                    )
                }
                Text(
                    metric.label,
                    color         = theme.text2,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(theme.stroke.copy(0.35f))
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
                .profilePremiumSurface(theme, RoundedCornerShape(20.dp), accent)
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
                theme.t("Başarımlar", "Achievements"),
                style         = MaterialTheme.typography.titleLarge,
                color         = theme.text0,
                letterSpacing = (-0.2).sp
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val unlockedCount = achievements.count { it.isUnlocked }
                Text(
                    "$unlockedCount/${achievements.size}",
                    color    = theme.text2,
                    fontSize = 11.sp
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
                    Text(strings.seeAll, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (sorted.isEmpty()) {
            // Loading placeholder
            Text(
                theme.t("Başarımlar yükleniyor...", "Loading achievements..."),
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
            .profilePremiumSurface(
                theme = theme,
                shape = RoundedCornerShape(20.dp),
                accent = if (achievement.isUnlocked) colorFrom else null,
                elevation = if (achievement.isUnlocked && theme.isDark) 12.dp else 6.dp
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
                localizedAchievementText(achievement.name, theme).take(18),
                color         = if (achievement.isUnlocked) colorFrom else theme.text2,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                localizedAchievementText(achievement.description, theme).take(42),
                color      = theme.text2.copy(alpha),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp
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
    onIntegrationsClick : () -> Unit = {},
    orbit               : OrbitIntegrationState = OrbitIntegrationState(),
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
                .profilePremiumAction(
                    theme = theme,
                    accent = accent,
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(16.dp)
                )
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
                    displayName.ifBlank { theme.t("Kullanıcı", "User") }.uppercase(),
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
                .profilePremiumSurface(theme, RoundedCornerShape(16.dp), accent)
        ) {
            val notifStatus = if (theme.notificationsEnabled) strings.notificationsActive
                              else strings.notificationsOff
            SettingsRow(
                icon    = Icons.Rounded.Notifications,
                label   = strings.notificationsLabel,
                sub     = notifStatus,
                theme   = theme,
                accent  = accent,
                onClick = onNotificationsClick
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(
                icon    = Icons.Rounded.Language,
                label   = strings.languageLabel,
                sub     = strings.currentLanguageName,
                theme   = theme,
                accent  = accent,
                onClick = onLanguageClick
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            theme.t("ENTEGRASYONLAR", "INTEGRATIONS"),
            style = MaterialTheme.typography.labelSmall,
            color = theme.text1,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumSurface(theme, RoundedCornerShape(16.dp), accent)
        ) {
            SettingsRow(
                icon = Icons.Rounded.Hub,
                label = "Orbit Personal OS",
                sub = when {
                    orbit.isLoading -> theme.t("Kontrol ediliyor", "Checking")
                    orbit.status == OrbitConnectionStatus.CONNECTED && orbit.fitnessSyncEntitled ->
                        theme.t("Bağlı · Fitness Sync", "Connected · Fitness Sync")
                    orbit.status == OrbitConnectionStatus.CONNECTED ->
                        theme.t("Bağlı · Orbit yetkisi gerekli", "Connected · Orbit entitlement required")
                    orbit.status == OrbitConnectionStatus.RECONNECT_REQUIRED ->
                        theme.t("Yeniden bağlantı gerekli", "Reconnect required")
                    else -> theme.t("Bağlı değil", "Not connected")
                },
                theme = theme,
                accent = accent,
                onClick = onIntegrationsClick
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumAction(
                    theme = theme,
                    accent = CardCoral,
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp)
                )
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
                Icon(Icons.Rounded.Logout, null, tint = CardCoral, modifier = Modifier.size(21.dp))
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
    accent : Color,
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
                .background(accent.copy(0.12f))
                .border(1.dp, accent.copy(0.24f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub,   color = theme.text2, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = accent.copy(0.62f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun OrbitIntegrationSheet(
    orbit: OrbitIntegrationState,
    theme: AppThemeState,
    accent: Color,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onManage: () -> Unit
) {
    val connected = orbit.status == OrbitConnectionStatus.CONNECTED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(.28f), accent.copy(.08f))))
                    .border(1.dp, accent.copy(.42f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Hub, null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Orbit Personal OS", color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    theme.t("Fitness özetini güvenli ve isteğe bağlı paylaş", "Securely share your Fitness summary when you choose"),
                    color = theme.text2,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .profilePremiumSurface(theme, RoundedCornerShape(20.dp), accent)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val statusText = when (orbit.status) {
                OrbitConnectionStatus.CONNECTED -> orbit.accountLabel?.let { theme.t("$it bağlı", "$it connected") }
                    ?: theme.t("Orbit hesabı bağlı", "Orbit account connected")
                OrbitConnectionStatus.RECONNECT_REQUIRED -> theme.t("Yeniden bağlantı gerekli", "Reconnect required")
                OrbitConnectionStatus.TEMPORARILY_UNAVAILABLE -> theme.t("Geçici olarak kullanılamıyor", "Temporarily unavailable")
                OrbitConnectionStatus.NOT_CONNECTED -> theme.t("Bağlı değil", "Not connected")
            }
            Text(statusText, color = theme.text0, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                theme.t(
                    "Haftalık hedef, bu haftaki tamamlamalar, bugünkü antrenman ve son tamamlanma zamanı paylaşılır. Fitness verileri burada kalır.",
                    "Weekly target, this week's completions, today's workout and last completion time are shared. Fitness remains the source of truth."
                ),
                color = theme.text2,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            if (connected) {
                HorizontalDivider(color = theme.stroke)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(theme.t("Fitness Sync", "Fitness Sync"), color = theme.text0, fontWeight = FontWeight.Bold)
                        Text(
                            if (orbit.fitnessSyncEntitled) theme.t("Orbit tarafından yetkilendirildi", "Authorized by Orbit")
                            else theme.t("Orbit üyeliğinde bu özellik açık değil", "Not enabled by your Orbit membership"),
                            color = if (orbit.fitnessSyncEntitled) accent else theme.text2,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = orbit.syncEnabled && orbit.fitnessSyncEntitled,
                        onCheckedChange = onSyncEnabled,
                        enabled = orbit.fitnessSyncEntitled
                    )
                }
            }
            if (orbit.lastErrorCode != null) {
                Text(
                    theme.t("Orbit geçici olarak kullanılamıyor. Fitness normal çalışmaya devam eder.", "Orbit is temporarily unavailable. Fitness continues to work normally."),
                    color = CardCoral,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        if (!connected || orbit.status == OrbitConnectionStatus.RECONNECT_REQUIRED) {
            Button(
                onClick = onConnect,
                enabled = !orbit.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Rounded.Link, null)
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Orbit'e Bağlan", "Connect Orbit"), fontWeight = FontWeight.Black)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = if (orbit.manageUrl != null) onManage else onSyncNow,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text(if (orbit.manageUrl != null) theme.t("Yönet", "Manage") else theme.t("Şimdi Eşitle", "Sync now"))
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CardCoral)
                ) {
                    Text(theme.t("Bağlantıyı Kes", "Disconnect"))
                }
            }
        }
        Text(
            theme.t(
                "Orbit premium durumu bu uygulamada oluşturulmaz; her zaman Orbit sunucusundan doğrulanır.",
                "Orbit premium status is never created in this app; it is always verified by Orbit."
            ),
            color = theme.text2,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(bottom = 28.dp)
        )
    }
}

// ── Appearance / Theme Settings Sheet ────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettingsSheet(
    current: AppThemeState,
    strings: AppStrings,
    onApply: (AppThemeState) -> Unit
) {
    var isDark          by remember { mutableStateOf(current.isDark) }
    var accent          by remember { mutableStateOf(current.accent) }
    var intensity       by remember { mutableStateOf(current.intensity) }
    var customAccentArgb by remember { mutableStateOf(current.customAccentArgb) }
    var showColorPicker by remember { mutableStateOf(false) }
    val theme           = LocalAppTheme.current
    val presetRows = remember {
        listOf(
            AccentPreset.LIME,
            AccentPreset.PURPLE,
            AccentPreset.CYAN,
            AccentPreset.ORANGE,
            AccentPreset.PINK,
            AccentPreset.BLUE,
            AccentPreset.RED,
            AccentPreset.YELLOW,
            AccentPreset.GREEN,
            AccentPreset.TEAL,
            AccentPreset.AMBER
        )
    }

    // Live preview state — her değişimde anında güncellenir
    val preview = current.copy(
        isDark          = isDark,
        accent           = accent,
        surfaceStyle     = SurfaceStyle.OLED,
        intensity        = intensity,
        customAccentArgb = customAccentArgb
    )
    val previewAccent    = preview.effectiveAccentColor
    val previewOnAccent  = preview.effectiveOnAccentColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp, 8.dp, 24.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            strings.appearanceTitle,
            style         = MaterialTheme.typography.titleLarge,
            color         = theme.text0,
            fontWeight    = FontWeight.SemiBold
        )

        SectionLabel(current.t("Görünüm", "Appearance"), theme)
        SegmentedSelector(
            options = listOf(
                true  to current.t("Koyu", "Dark"),
                false to current.t("Açık", "Light")
            ),
            selected = isDark,
            accent   = previewAccent,
            onAccent = previewOnAccent,
            theme    = theme,
            onSelect = { isDark = it }
        )

        // ── Live Preview Card ─────────────────────────────────────────────────
        SectionLabel(strings.previewLabel, theme)
        PreviewCard(preview = preview)

        // ── Accent Color ──────────────────────────────────────────────────────
        SectionLabel(strings.accentColorLabel, theme)
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            presetRows.forEach { preset ->
                AccentColorSwatch(
                    color      = preset.color,
                    isSelected = customAccentArgb == null && accent == preset,
                    onClick    = {
                        accent = preset
                        customAccentArgb = null
                    }
                )
            }
            CustomAccentSwatch(
                color      = customAccentArgb?.let { Color(it) } ?: previewAccent,
                isSelected = customAccentArgb != null,
                onClick    = { showColorPicker = true }
            )
        }

        // ── Background Tone (sadece dark) ────────────────────────────────────
        // ── Accent Intensity ──────────────────────────────────────────────────
        SectionLabel(strings.intensityLabel, theme)
        SegmentedSelector(
            options = listOf(
                AccentIntensity.NEON   to strings.intensityNeonLabel,
                AccentIntensity.PASTEL to strings.intensityPastelLabel,
                AccentIntensity.VIVID  to current.t("CANLI", "VIVID"),
                AccentIntensity.SOFT   to current.t("SOFT", "SOFT")
            ),
            selected  = intensity,
            accent    = previewAccent,
            onAccent  = previewOnAccent,
            theme     = theme,
            onSelect  = { intensity = it }
        )

        Button(
            onClick  = {
                onApply(
                    current.copy(
                        isDark       = isDark,
                        accent       = accent,
                        surfaceStyle = SurfaceStyle.OLED,
                        intensity    = intensity,
                        customAccentArgb = customAccentArgb
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = previewAccent,
                contentColor   = previewOnAccent
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }

    if (showColorPicker) {
        CustomAccentColorDialog(
            initialColor = customAccentArgb?.let { Color(it) } ?: previewAccent,
            theme = preview,
            onDismiss = { showColorPicker = false },
            onColorSelected = { selected ->
                customAccentArgb = selected
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String, theme: AppThemeState) {
    Text(
        text,
        color         = theme.text1,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

/** Ayarların canlı önizlemesi: kart yüzeyi + başlık + mini buton. */
@Composable
private fun PreviewCard(preview: AppThemeState) {
    val acc = preview.effectiveAccentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(preview, RoundedCornerShape(16.dp), acc)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(acc)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                preview.accentDisplayLabel,
                color         = acc,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
        Text(
            "Profitness",
            color      = preview.text0,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            preview.strings.helloAthlete,
            color    = preview.text1,
            fontSize = 12.sp
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(acc)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                preview.strings.applyLabel,
                color         = preview.effectiveOnAccentColor,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

/** Genel amaçlı segmented selector — 2+ seçenek için. */
@Composable
private fun <T> SegmentedSelector(
    options : List<Pair<T, String>>,
    selected: T,
    accent  : Color,
    onAccent: Color,
    theme   : AppThemeState,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(12.dp),
                accent,
                if (theme.isDark) 8.dp else 4.dp
            )
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(if (isSel) Modifier.insetControlSurface(accent, theme, RoundedCornerShape(9.dp)) else Modifier)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color         = if (isSel) theme.text0 else theme.text1,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
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
                .profilePremiumSurface(theme, RoundedCornerShape(16.dp), accent)
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
        }

        Button(
            onClick  = { onApply(workoutReminders || progressUpdates) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = theme.effectiveOnAccentColor
            ),
            border = BorderStroke(1.dp, Color.White.copy(if (theme.isDark) 0.28f else 0.42f)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (theme.isDark) 12.dp else 7.dp,
                pressedElevation = 2.dp
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
                checkedThumbColor   = theme.effectiveOnAccentColor,
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
                .profilePremiumSurface(theme, RoundedCornerShape(16.dp), accent)
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
                contentColor   = theme.effectiveOnAccentColor
            ),
            border = BorderStroke(1.dp, Color.White.copy(if (theme.isDark) 0.28f else 0.42f)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (theme.isDark) 12.dp else 7.dp,
                pressedElevation = 2.dp
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
                    tint     = theme.effectiveOnAccentColor,
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
        bmi < 18.5 -> theme.t("Zayıf", "Underweight")
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> theme.t("Fazla Kilolu", "Overweight")
        else       -> theme.t("Obez", "Obese")
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            theme.t("VÜCUT METRİKLERİ", "BODY METRICS"),
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
                    label    = theme.t("BOY", "HEIGHT"),
                    color    = CardCyan,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (weightKg > 0) {
                BodyMetricTile(
                    value    = "${weightKg.toInt()}",
                    unit     = "kg",
                    label    = theme.t("KİLO", "WEIGHT"),
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
                    label    = theme.t("YAĞ", "FAT"),
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
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(0.12f), theme.bg1.copy(0.92f))
                )
            )
            .border(1.dp, color.copy(0.30f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(value, color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(unit, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = theme.text2, fontSize = 8.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun PerformanceShortcutsSection(
    accent         : Color,
    theme          : AppThemeState,
    weightKg       : Double,
    onWeightClick  : () -> Unit,
    onExerciseClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PerformanceShortcutCard(
            title    = theme.t("Vücut Kilosu", "Body Weight"),
            subtitle = if (weightKg > 0) {
                theme.t("${"%.1f".format(weightKg)} kg · Trend & AI Analiz", "${"%.1f".format(weightKg)} kg · Trend & AI Analysis")
            } else {
                theme.t("Kilo trendi ve AI analiz", "Weight trend and AI analysis")
            },
            icon     = Icons.Rounded.ShowChart,
            accent   = accent,
            theme    = theme,
            onClick  = onWeightClick
        )
        PerformanceShortcutCard(
            title    = theme.t("Hareket Performansı", "Exercise Performance"),
            subtitle = theme.t("Ağırlık, süre, mesafe ve hareket bazlı gelişim", "Weight, duration, distance and movement-based progress"),
            icon     = Icons.Rounded.TrendingUp,
            accent   = accent,
            theme    = theme,
            onClick  = onExerciseClick
        )
    }
}

@Composable
private fun PerformanceShortcutCard(
    title   : String,
    subtitle: String,
    icon    : ImageVector,
    accent  : Color,
    theme   : AppThemeState,
    onClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumAction(theme, accent, onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = accent,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color      = theme.text0,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color    = theme.text2,
                fontSize = 11.sp
            )
        }

        Icon(
            Icons.Rounded.ArrowForwardIos,
            null,
            tint     = accent.copy(0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Weight Tracking Card (Profile → detail navigation) ───────────────────────

@Composable
fun WeightTrackingCard(
    accent   : Color,
    theme    : AppThemeState,
    weightKg : Double,
    onClick  : () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 32.dp)) {
        Text(
            theme.t("PERFORMANS ÖLÇÜTLERİ", "PERFORMANCE METRICS"),
            style         = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumAction(theme, accent, onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ShowChart,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.t("Vücut Kilosu", "Body Weight"),
                    color      = theme.text0,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (weightKg > 0) theme.t("${"%.1f".format(weightKg)} kg · Trend & AI Analiz", "${"%.1f".format(weightKg)} kg · Trend & AI Analysis")
                    else theme.t("Kilo ölçümlerini performans verilerinle birlikte takip et", "Track weight measurements alongside your performance data"),
                    color    = theme.text2,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Rounded.ArrowForwardIos,
                null,
                tint     = accent.copy(0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Exercise Progression Card (Profile → detail navigation) ──────────────────

@Composable
fun ExerciseProgressionCard(
    accent  : Color,
    theme   : AppThemeState,
    onClick : () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        Text(
            theme.t("SPOR PERFORMANSI", "SPORT PERFORMANCE"),
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumAction(theme, accent, onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.t("Hareket Performansı", "Exercise Performance"),
                    color      = theme.text0,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    theme.t("Ağırlık · Süre · Mesafe · AI Analiz", "Weight · Duration · Distance · AI Analysis"),
                    color    = theme.text2,
                    fontSize = 11.sp
                )
            }
            Icon(
                Icons.Rounded.ArrowForwardIos,
                null,
                tint     = accent.copy(0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Leaderboard Preview Card ─────────────────────────────────────────────────

@Composable
private fun LeaderboardPreviewCard(
    accent             : Color,
    theme              : AppThemeState,
    onOpenXp           : () -> Unit,
    onOpenAchievements : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .profilePremiumSurface(theme, accent = accent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.15f))
                    .border(1.dp, accent.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Leaderboard, null,
                    tint     = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("SIRALAMA", "RANKINGS"),
                    color         = theme.text0,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    theme.t("Diğer kullanıcılarla kıyasla", "Compare with other users"),
                    color    = theme.text2,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RankModeTile(
                title    = "XP",
                subtitle = theme.t("Deneyim puanı", "Experience points"),
                icon     = Icons.Rounded.Bolt,
                accent   = accent,
                theme    = theme,
                onClick  = onOpenXp,
                modifier = Modifier.weight(1f)
            )
            RankModeTile(
                title    = theme.t("Başarım", "Achievements"),
                subtitle = theme.t("Kazanılan rozetler", "Earned badges"),
                icon     = Icons.Rounded.EmojiEvents,
                accent   = accent,
                theme    = theme,
                onClick  = onOpenAchievements,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RankModeTile(
    title   : String,
    subtitle: String,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    accent  : Color,
    theme   : AppThemeState,
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .profilePremiumAction(
                theme = theme,
                accent = accent,
                onClick = onClick,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint     = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                color      = theme.text0,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.ArrowForwardIos, null,
                tint     = accent.copy(0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            subtitle,
            color    = theme.text2,
            fontSize = 10.sp
        )
    }
}

