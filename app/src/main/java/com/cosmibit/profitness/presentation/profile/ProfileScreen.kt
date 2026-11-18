package com.cosmibit.profitness.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.cosmibit.profitness.core.theme.rewardGold
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaFitText
import com.cosmibit.profitness.core.ui.SinemaLayout
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.workout.SceneImage
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.em
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.data.store.UserPlan
import kotlinx.coroutines.delay
import java.time.LocalDate

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
    var showNotifications    by remember { mutableStateOf(false) }
    var showLanguagePicker   by remember { mutableStateOf(false) }
    var achievementPopup     by remember { mutableStateOf<Triple<String, String, String>?>(null) } // icon, name, description
    val snackbarHostState    = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportData) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importData) }

    // Achievement unlock bildirimi
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.AchievementUnlocked ->
                    achievementPopup = Triple(event.icon, event.name, event.description)
                is ProfileEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Tab geçiş animasyonu bittikten sonra ilk profil yükünü başlat.
    LaunchedEffect(Unit) {
        delay(16)
        viewModel.initLoad()
    }
    // Sekme canlı tutulur: eskiden her girişte (yeniden kompozisyon + ON_RESUME) stale ise tazeleniyordu; şimdi sekme aktifleşince.
    val tabActive by com.cosmibit.profitness.core.ui.LocalTabActive.current
    com.cosmibit.profitness.core.ui.OnTabReentry(tabActive) { viewModel.reloadIfStale() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadIfStale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val coverViewModel: ProfileCoverViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val cover by coverViewModel.cover.collectAsStateWithLifecycle()
    var showCoverPicker by remember { mutableStateOf(false) }
    if (showCoverPicker) CoverPickerSheet(coverViewModel, onDismiss = { showCoverPicker = false })

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        val profileListState = androidx.compose.foundation.lazy.rememberLazyListState()
        // PERF: ilk karede yalnız kahraman banner; kupa/metrik/haftalık/ayarlar kartları sekme yerleşince (+2 kare).
        val heavyReady = com.cosmibit.profitness.presentation.components.rememberHeavyContentReady()
        LazyColumn(
            state = profileListState,
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
                    currentStreak    = state.currentStreak,
                    longestStreak    = state.longestStreak,
                    totalExercises   = state.totalExercises,
                    userPlan         = state.userPlan,
                    aiCredits        = state.aiCredits,
                    accent           = accent,
                    theme            = theme,
                    scrollOffset     = {
                        if (profileListState.firstVisibleItemIndex == 0) profileListState.firstVisibleItemScrollOffset else 0
                    },
                    onNavigateToStore = onNavigateToStore,
                    onOpenXpRanking   = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Xp) },
                    onOpenRankRanking = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Achievements) },
                    onOpenStreakRanking = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Streak) },
                    onEditProfile     = onEditProfile,
                    isPremium         = state.planLoaded && state.userPlan == UserPlan.PREMIUM,
                    cover             = cover,
                    onChangeCover     = { showCoverPicker = true }
                )
            }
            item {
                RankLevelStrip(
                    xp            = state.xp,
                    level         = state.level,
                    rank          = state.rank,
                    theme         = theme,
                    onOpenRoadmap = onNavigateToAchievements
                )
            }
            if (heavyReady) item {
                PerformanceMetricsSection(
                    accent             = accent,
                    theme              = theme,
                    strings            = strings,
                    currentStreak      = state.currentStreak,
                    longestStreak      = state.longestStreak,
                    totalExercises     = state.totalExercises,
                    totalDurationSeconds = state.totalDurationSeconds,
                    totalDistanceMeters  = state.totalDistanceMeters,
                    weekWorkouts       = state.weeklyWorkoutCounts.lastOrNull() ?: 0,
                    totalWorkouts      = state.totalWorkouts,
                    totalVolumeKg      = state.totalVolumeKg,
                    topLiftName        = state.topLiftName,
                    topLiftKg          = state.topLiftKg,
                    onNavigateToDetail = onNavigateToPerformance
                )
            }
            // SIRALAMA kompakt satırı (Performans'ın hemen altında; Başarımlar'dan önce)
            if (heavyReady) item {
                RankingRow(
                    rank       = state.rank,
                    position   = state.xpRankPosition,
                    totalUsers = state.xpRankTotalUsers,
                    theme      = theme,
                    onOpen     = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Xp) }
                )
            }
            if (heavyReady) item {
                TrophyGallery(
                    accent       = accent,
                    theme        = theme,
                    strings      = strings,
                    achievements = state.achievements,
                    onSeeAll     = onNavigateToAchievements
                )
            }
            if (heavyReady) item {
                WeeklyActivitySection(
                    accent           = accent,
                    theme            = theme,
                    strings          = strings,
                    weeklyActivity   = state.weeklyActivity,
                    weekWorkoutCount = state.weeklyWorkoutCounts.lastOrNull() ?: 0
                )
            }
            if (heavyReady) item {
                SettingsSection(
                    theme                = theme,
                    accent               = accent,
                    strings              = strings,
                    onLogout             = onLogout,
                    onEditProfile        = onEditProfile,
                    onNotificationsClick = { showNotifications = true },
                    onLanguageClick      = { showLanguagePicker = true },
                    membershipLabel      = if (state.planLoaded) state.userPlan.displayName else "…",
                    onMembershipClick    = onNavigateToStore,
                    onExportData         = {
                        exportLauncher.launch("profitness-yedek-${LocalDate.now()}.json")
                    },
                    onImportData         = {
                        importLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                    },
                    isTransferring       = state.isTransferring,
                    displayName          = state.displayName.ifBlank { theme.t("Kullanıcı", "User") },
                    avatar               = state.avatar
                )
            }
            // EN ALTTA: Ücretsiz'de Premium kartı (sakin giriş; Premium'da yalnız ad yanı rozet + Ayarlar > Üyelik)
            if (heavyReady && state.planLoaded && state.userPlan == UserPlan.FREE) item {
                ProfileMembershipCard(
                    plan     = state.userPlan,
                    status   = state.planStatus,
                    freeLeft = state.freeAiProgramsLeft,
                    theme    = theme,
                    onOpen   = onNavigateToStore
                )
            }
        }
        if (profileListState.canScrollBackward) Box(
            Modifier.fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Sinema.page(theme).copy(0.94f), Sinema.page(theme).copy(0.94f), androidx.compose.ui.graphics.Color.Transparent)
            ))
    )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp + timerExtraPad)
        )
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
                        .clip(RoundedCornerShape(SinemaRadius.R28))
                        .background(
                            Brush.verticalGradient(listOf(Sinema.Elevated, Sinema.Surface))
                        )
                        .sinemaSoftRing(accent.copy(0.5f), RoundedCornerShape(SinemaRadius.R28))
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
                            fontSize      = SinemaType.Caption,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            localizedAchievementText(name, theme),
                            color      = Snow,
                            fontSize   = SinemaType.Headline,
                            fontWeight = FontWeight.Black,
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (desc.isNotBlank()) {
                            Text(
                                localizedAchievementText(desc, theme),
                                color     = Snow.copy(0.6f),
                                fontSize  = SinemaType.BodySm,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SinemaRadius.R12))
                            .background(accent.copy(0.15f))
                            .sinemaSoftRing(accent.copy(0.3f), RoundedCornerShape(SinemaRadius.R12))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Harika! Devam et 🔥",
                            color      = accent,
                            fontSize   = SinemaType.BodySm,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── Notifications bottom sheet ────────────────────────────────────────────
    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false },
            containerColor   = Sinema.surface(theme),
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
            containerColor   = Sinema.surface(theme),
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

// ── Profile Hero (SİNEMA çizimi: kapak foto + XP + sheet) ───────────────────────

private fun rankColor(rank: String) = when (rank.lowercase()) {
    "silver"   -> Color(0xFFB4B8C0)
    "gold"     -> Sinema.Gold
    "platinum" -> androidx.compose.ui.graphics.lerp(Sinema.Gold, Color.White, 0.5f)
    "diamond"  -> androidx.compose.ui.graphics.lerp(Sinema.Gold, Color.White, 0.8f)
    else       -> androidx.compose.ui.graphics.lerp(Sinema.Gold, Color.Black, 0.35f)  // Bronze
}

/** Çizimdeki `.chip.metal`: silver için birebir 135° C0C4CC→8E949E; diğer rütbelerde rütbe renginden türetilir. */
private fun rankBrush(rank: String): Brush {
    if (rank.equals("silver", ignoreCase = true)) return Brush.linearGradient(listOf(Color(0xFFC0C4CC), Color(0xFF8E949E)))
    val c = rankColor(rank)
    return Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(c, Color.White, 0.18f), androidx.compose.ui.graphics.lerp(c, Color.Black, 0.22f)))
}

/** Çizimdeki `.chip`: 28dp, r14, 12sp bold, harf aralığı .06em. */
@Composable
private fun ProfileChip(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    fill: Color = Color.Transparent,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(SinemaRadius.R12)
    Box(
        modifier
            .height(28.dp)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(fill))
            .then(if (onClick != null) Modifier.sinemaClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = textColor, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold,
            letterSpacing = 0.06.em, maxLines = 1
        )
    }
}

@Composable
private fun ProfileHeroBanner(
    name           : String,
    avatar         : String,
    rank           : String,
    level          : Int,
    xp             : Int,
    xpPerLevel     : Int,
    currentStreak  : Int,
    longestStreak  : Int,
    totalExercises : Int,
    userPlan        : UserPlan = UserPlan.FREE,
    aiCredits       : Int = 0,
    accent          : Color,
    theme           : AppThemeState,
    scrollOffset    : () -> Int,
    onNavigateToStore: () -> Unit = {},
    onOpenXpRanking : () -> Unit = {},
    onOpenRankRanking: () -> Unit = {},
    onOpenStreakRanking: () -> Unit = {},
    onEditProfile   : () -> Unit = {},
    isPremium       : Boolean = false,
    cover           : com.cosmibit.profitness.data.profile.ProfileCover = com.cosmibit.profitness.data.profile.ProfileCover(),
    onChangeCover   : () -> Unit = {}
) {
    val page = Sinema.page(theme)
    val textMain = Sinema.text(theme)
    val textSub = Sinema.textSecondary(theme)
    val wc = LocalWindowClass.current
    val gutter = SinemaLayout.gutter(wc.widthDp)
    // Kapak biraz küçüldü (≈%34, en çok 300dp): Performans ve Sıralama ilk ekrana sığsın
    val coverH = SinemaLayout.cover(wc.heightDp, 0.34f, 220.dp, 300.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── Kapak 320dp (çizim `.cover`): foto + 3 duraklı karartma (.5 → 0 @%40 → .75) ──
        Box(Modifier.fillMaxWidth().height(coverH).clipToBounds()) {
            // Foto + karartma: alt 200dp maske ile sayfa zeminine (SinemaBackdrop) söner
            Box(Modifier.fillMaxSize().photoFadeToBackdrop()) {
                // Kullanıcı kapağı (yerel foto / hazır kapak / varsayılan salon fotoğrafı)
                ProfileCoverImage(cover, Modifier.fillMaxSize(), parallax = { scrollOffset() * 0.5f })
                // Okunurluk: üst (durum çubuğu) + alt (isim) karartma her kapakta korunur
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Sinema.scrim(0.50f), 0.40f to Color.Transparent, 1f to Sinema.scrim(0.75f)
                        )
                    )
                )
            }
            // Kapağı değiştir (48dp, durum çubuğunun altında sağ üst)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = gutter - 8.dp)
                    .size(48.dp)
                    .sinemaClickable(onClick = onChangeCover, role = androidx.compose.ui.semantics.Role.Button)
                    .semantics { contentDescription = theme.t("Kapağı değiştir", "Change cover") },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Sinema.scrim(0.45f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

        }

        // ── Sheet (çizim `.sheet`: üstten 300dp, r28, üstte 1dp çizgi) ───────────────
        val lineColor = Sinema.line(theme)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = coverH - 20.dp)
        ) {
            // Avatar + ad: dokununca Profil düzenle (≥48dp). Avatarda küçük kalem, adın yanında ince ok; Premium'da altın rozet.
            Box(Modifier.fillMaxWidth().padding(horizontal = gutter).height(62.dp)) {
                Box(
                    Modifier
                        .offset(y = (-48 + (96 - 62) / 2).dp)
                        .requiredSize(96.dp)
                        .sinemaClickable(onClick = onEditProfile, role = androidx.compose.ui.semantics.Role.Button)
                        .semantics { contentDescription = theme.t("Profili düzenle", "Edit profile") }
                ) {
                    Box(
                        Modifier.fillMaxSize().clip(CircleShape).background(page).padding(4.dp).clip(CircleShape).sinemaSolid(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatar.startsWith("http")) {
                            AsyncImage(
                                model = LocalContext.current.let { ctx -> remember(avatar) { ImageRequest.Builder(ctx).data(avatar).crossfade(true).build() } },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(avatar, fontSize = 40.sp, color = theme.effectiveOnAccentColor)
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape).background(page).padding(3.dp)
                            .clip(CircleShape).background(Sinema.surface(theme)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Edit, null, tint = textMain, modifier = Modifier.size(14.dp)) }
                }
                Row(
                    Modifier.padding(start = 112.dp).heightIn(min = 48.dp).padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f, fill = false).heightIn(min = 48.dp)
                            .sinemaClickable(onClick = onEditProfile, role = androidx.compose.ui.semantics.Role.Button),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, color = textMain, style = Sinema.Title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Rounded.ChevronRight, theme.t("Profili düzenle", "Edit profile"), tint = textSub, modifier = Modifier.size(20.dp))
                    }
                    if (isPremium) {
                        Spacer(Modifier.width(6.dp))
                        PremiumBadge(theme = theme, onClick = onNavigateToStore)
                    }
                }
            }

            // 3 metrik (üstten 118dp; 28dp aralık → 78dp yükseklik: 196'daki XP çizgisine kadar)
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetric(currentStreak.toString(), theme.t("Seri · gün", "Streak · days"), theme, Modifier.weight(1f), onOpenStreakRanking)
                HeroMetric(longestStreak.toString(), theme.t("En uzun seri", "Longest streak"), theme, Modifier.weight(1f))
                HeroMetric(totalExercises.toString(), theme.t("Egzersiz", "Exercises"), theme, Modifier.weight(1f))
            }

        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String, theme: AppThemeState, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.sinemaClickable(onClick = onClick) else Modifier)
    ) {
        SinemaFitText(text = value, style = Sinema.Metric, color = Sinema.text(theme))
        Spacer(Modifier.height(6.dp))
        Text(
            label.sinemaUpper(theme), color = Sinema.textSecondary(theme),
            style = Sinema.Label.copy(fontSize = 12.sp, lineHeight = 13.sp), maxLines = 2
        )
    }
}

/** Bölüm başlığı: 32dp ara + 1dp çizgi + Label (ikincil) + isteğe bağlı sağ öğe. */
@Composable
private fun SinemaSection(
    theme: AppThemeState,
    title: String,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = sinemaGutter()).height(1.dp).background(Sinema.line(theme)))
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SinemaLabelDot()
            Spacer(Modifier.width(8.dp))
            Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f))
            trailing?.invoke(this)
        }
        content()
    }
}

@Composable
private fun SeeAllLink(theme: AppThemeState, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .sinemaClickable(onClick = onClick)
            .clip(RoundedCornerShape(SinemaRadius.R24))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text, color = Sinema.text(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold)
        Icon(Icons.Rounded.ArrowForwardIos, null, tint = Sinema.text(theme), modifier = Modifier.size(11.dp))
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
    weekWorkouts      : Int = 0,
    totalWorkouts     : Int = 0,
    totalVolumeKg     : Double = 0.0,
    topLiftName       : String = "",
    topLiftKg         : Float = 0f,
    onNavigateToDetail: () -> Unit
) {
    // Seri bilgisi üstteki istatistik satırında; burada TEKRAR YOK. Mevcut verilerden (yeni sorgu yok) en değerli üç değer.
    val metrics = performanceMetricsFor(
        theme, weekWorkouts, totalWorkouts, totalVolumeKg, topLiftName, topLiftKg, totalDurationSeconds, totalDistanceMeters
    )

    SinemaSection(
        theme = theme,
        title = theme.t("Performans", "Performance"),
        trailing = { SeeAllLink(theme, strings.seeAll, onNavigateToDetail) }
    ) {
        if (metrics.isEmpty()) {
            Text(
                theme.t("İlk antrenmanını tamamla; hacim, rekor ve süre burada görünür.", "Finish your first workout; volume, records and time show up here."),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Body,
                modifier = Modifier.padding(horizontal = sinemaGutter(), vertical = 8.dp)
            )
        } else LazyRow(
            contentPadding        = PaddingValues(horizontal = sinemaGutter()),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(metrics) { metric -> MetricCell(metric, theme, onNavigateToDetail) }
        }
    }
}

@Composable
private fun MetricCell(metric: PerformanceMetric, theme: AppThemeState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(124.dp)
            .sinemaClickable(onClick = onClick)
            .clip(RoundedCornerShape(SinemaRadius.R12))
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                metric.value,
                color = Sinema.text(theme), style = Sinema.Metric,
                maxLines = 1, softWrap = false,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                metric.unit,
                color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(metric.color))
        Spacer(Modifier.height(10.dp))
        Text(metric.label, color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

// ── Weekly Activity ───────────────────────────────────────────────────────────

@Composable
private fun WeeklyActivitySection(
    accent         : Color,
    theme          : AppThemeState,
    strings        : AppStrings,
    weeklyActivity : List<Float>,
    weekWorkoutCount: Int = 0
) {
    val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1   // 0=Pzt … 6=Paz
    val barShape = RoundedCornerShape(8.dp)

    SinemaSection(theme = theme, title = theme.t("Bu hafta", "This week")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sinemaGutter())
                .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24))
                .padding(20.dp)
        ) {
            if (weekWorkoutCount > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        weekWorkoutCount.toString(),
                        color = Sinema.text(theme), style = Sinema.Metric,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        theme.t("antrenman", "workouts"),
                        color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            } else {
                Text(
                    theme.t("Bu hafta henüz antrenman yok", "No workouts yet this week"),
                    color = Sinema.textSecondary(theme), style = Sinema.Body
                )
            }

            Spacer(Modifier.height(20.dp))

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier         = Modifier.width(28.dp).height(72.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(barShape)
                                    .background(Sinema.elevated(theme))
                            )
                            if (level > 0f) {
                                // En az %15 yükseklik göster (çok küçük olmasın)
                                val fill = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(level.coerceAtLeast(0.15f))
                                Box(
                                    fill
                                        .clip(barShape)
                                        .background(if (isToday) accent else accent.copy(0.4f))
                                )
                            }
                        }
                        Text(
                            day,
                            color      = if (isToday) Sinema.text(theme) else Sinema.textSecondary(theme),
                            fontSize   = SinemaType.Caption,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Trophy Gallery (Gerçek Başarımlar) ────────────────────────────────────────

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
    val unlockedCount = achievements.count { it.isUnlocked }
    val sub = Sinema.textSecondary(theme)

    // Çizim `.bst`: başlık (12sp, harf aralığı .1em) + sayaç, altında 60dp daire şeridi (10dp aralık).
    // Başlık satırı "tümünü gör"e gider (çizimde ayrı bağlantı yok).
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp).sinemaClickable(onClick = onSeeAll, scale = 0.98f)
                .padding(horizontal = sinemaGutter()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SinemaLabelDot()
            Spacer(Modifier.width(8.dp))
            Text(
                theme.t("Başarımlar", "Achievements").sinemaUpper(theme), color = sub, style = Sinema.Label,
                modifier = Modifier.weight(1f)
            )
            Text("$unlockedCount / ${achievements.size}", color = sub, style = Sinema.Label)
        }
        if (sorted.isEmpty()) {
            Text(
                theme.t("Başarımlar yükleniyor...", "Loading achievements..."),
                color    = sub,
                fontSize = SinemaType.BodySm,
                modifier = Modifier.padding(horizontal = sinemaGutter())
            )
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = sinemaGutter()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sorted) { ach -> AchievementCard(achievement = ach, theme = theme) }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementUiModel, theme: AppThemeState) {
    val unlocked = achievement.isUnlocked
    val label = localizedAchievementText(achievement.name, theme)
    Box(
        Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Sinema.surface(theme))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(achievement.icon, fontSize = SinemaType.Headline, modifier = Modifier.alpha(if (unlocked) 1f else 0.3f))
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
    membershipLabel     : String     = "",
    onMembershipClick   : () -> Unit = {},
    onExportData        : () -> Unit = {},
    onImportData        : () -> Unit = {},
    isTransferring      : Boolean    = false,
    displayName         : String     = "",
    avatar              : String     = "🏋️"
) {
    val groupShape = RoundedCornerShape(SinemaRadius.R20)
    val surface = Sinema.surface(theme)
    val divider: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().padding(start = 72.dp, end = 20.dp).height(1.dp).background(Sinema.line(theme)))
    }

    SinemaSection(theme = theme, title = strings.accountSettings) {
        Column(
            modifier = Modifier.padding(horizontal = sinemaGutter()).fillMaxWidth().clip(groupShape).background(surface)
        ) {
            SettingsRow(Icons.Rounded.WorkspacePremium, theme.t("Üyelik", "Membership"), membershipLabel, theme, accent, onClick = onMembershipClick)
            divider()
            val notifStatus = if (theme.notificationsEnabled) strings.notificationsActive else strings.notificationsOff
            SettingsRow(Icons.Rounded.Notifications, strings.notificationsLabel, notifStatus, theme, accent, onClick = onNotificationsClick)
            divider()
            SettingsRow(Icons.Rounded.Language, strings.languageLabel, strings.currentLanguageName, theme, accent, onClick = onLanguageClick)
        }

        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = sinemaGutter()).height(1.dp).background(Sinema.line(theme)))
        Box(Modifier.fillMaxWidth().padding(horizontal = sinemaGutter()).heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SinemaLabelDot()
                Spacer(Modifier.width(8.dp))
                Text(theme.t("VERİLER", "DATA"), color = Sinema.textSecondary(theme), style = Sinema.Label)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = sinemaGutter()).fillMaxWidth().clip(groupShape).background(surface)
        ) {
            SettingsRow(
                icon    = Icons.Rounded.UploadFile,
                label   = theme.t("Verileri Dışa Aktar", "Export Data"),
                sub     = if (isTransferring) theme.t("İşlem sürüyor…", "Working…") else
                    theme.t("Profil, program ve antrenman yedeği oluştur", "Create a profile, program and workout backup"),
                theme = theme, accent = accent, enabled = !isTransferring, onClick = onExportData
            )
            divider()
            SettingsRow(
                icon    = Icons.Rounded.Download,
                label   = theme.t("Verileri İçe Aktar", "Import Data"),
                sub     = theme.t("Mevcut verileri silmeden bir yedeği birleştir", "Merge a backup without deleting current data"),
                theme = theme, accent = accent, enabled = !isTransferring, onClick = onImportData
            )
        }

        Text(
            theme.t(
                "XP, kredi, sıralama ve sosyal veriler yedeğe dahil edilmez.",
                "XP, credits, rankings and social data are not included."
            ),
            color = Sinema.textSecondary(theme),
            fontSize = SinemaType.Caption,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        val coral = CardCoral
        Row(
            modifier = Modifier
                .padding(horizontal = sinemaGutter())
                .fillMaxWidth()
                .sinemaClickable(onClick = onLogout, scale = 0.98f)
                .clip(groupShape)
                .background(surface)
                .heightIn(min = 64.dp)
                .padding(horizontal = sinemaGutter()),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Rounded.Logout, null, tint = coral, modifier = Modifier.size(22.dp))
            Text(
                strings.logoutLabel,
                color      = coral,
                fontSize   = SinemaType.BodyLg,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.ChevronRight, null, tint = coral.copy(0.6f), modifier = Modifier.size(20.dp))
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
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().sinemaClickable(enabled = enabled, onClick = onClick, scale = 0.98f)
            .then(if (enabled) Modifier else Modifier.alpha(0.55f))
            .heightIn(min = 64.dp)
            .padding(horizontal = sinemaGutter(), vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // anlamlı renkli ikon diski (ikon adından sabit ton: amber / mercan / teal / mor)
        val tone = remember(icon) { listOf(theme.accentColor, theme.accentHi, Color(0xFF2DD4BF), Color(0xFFC084FC))[Math.floorMod(icon.name.hashCode(), 4)] }
        Box(Modifier.size(40.dp).clip(CircleShape).background(androidx.compose.ui.graphics.lerp(Sinema.Surface, tone, 0.45f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = androidx.compose.ui.graphics.lerp(tone, Color.White, 0.35f), modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = Sinema.text(theme), fontSize = SinemaType.BodyLg, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, lineHeight = 16.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
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
        // tutamaç ModalBottomSheet varsayılanından gelir (ikinci özel tutamaç kaldırıldı)
        Text(
            strings.notifSheetTitle,
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 1.2.sp,
            fontWeight    = FontWeight.Black
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumSurface(theme, RoundedCornerShape(SinemaRadius.R16), accent)
        ) {
            NotifToggleRow(
                label           = strings.workoutReminders,
                icon            = Icons.Rounded.FitnessCenter,
                checked         = workoutReminders,
                accent          = accent,
                theme           = theme,
                onCheckedChange = { workoutReminders = it }
            )
            HorizontalDivider(color = theme.stroke, modifier = Modifier.padding(horizontal = sinemaGutter()))
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
            shape    = RoundedCornerShape(SinemaRadius.R12),
            colors   = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = theme.effectiveOnAccentColor
            ),
            border = null,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = SinemaType.BodySm)
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
                .clip(RoundedCornerShape(SinemaRadius.R12))
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
            fontSize   = SinemaType.Body,
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
        Text(
            strings.langSheetTitle,
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 1.2.sp,
            fontWeight    = FontWeight.Black
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24))
        ) {
            LanguageOptionRow(
                flag       = "🇹🇷",
                name       = strings.turkishLabel,
                isSelected = selected == AppLanguage.TURKISH,
                accent     = accent,
                theme      = theme,
                onClick    = { selected = AppLanguage.TURKISH }
            )
            HorizontalDivider(color = Sinema.line(theme), modifier = Modifier.padding(horizontal = sinemaGutter()))
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
            shape    = RoundedCornerShape(SinemaRadius.R12),
            colors   = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = theme.effectiveOnAccentColor
            ),
            border = null,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = SinemaType.BodySm)
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
            .fillMaxWidth().sinemaClickable(onClick = onClick, scale = 0.98f)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(flag, fontSize = SinemaType.Headline)
        Text(
            name,
            color      = theme.text0,
            fontSize   = SinemaType.BodyLg,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            modifier   = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint     = accent,
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
            .sinemaClickable(onClick = onClick)
            .clip(RoundedCornerShape(SinemaRadius.R12))
            .background(if (isSelected) accent.copy(0.15f) else Color.Transparent)
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
                fontSize      = SinemaType.Caption,
                fontWeight    = if (isSelected) FontWeight.Black else FontWeight.Normal,
                letterSpacing = 1.sp
            )
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
        bmi < 18.5 -> Sinema.TextSecondary
        bmi < 25.0 -> Sinema.Gold
        bmi < 30.0 -> Sinema.TextSecondary
        else       -> Sinema.Alert
    }
    val bmiLabel = when {
        bmi <= 0   -> ""
        bmi < 18.5 -> theme.t("Zayıf", "Underweight")
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> theme.t("Fazla Kilolu", "Overweight")
        else       -> theme.t("Obez", "Obese")
    }

    Column(modifier = Modifier.padding(horizontal = sinemaGutter(), vertical = 16.dp)) {
        Text(
            theme.t("VÜCUT METRİKLERİ", "BODY METRICS"),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            letterSpacing = 1.2.sp
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
                    color    = theme.accentHi,
                    theme    = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (weightKg > 0) {
                BodyMetricTile(
                    value    = "${weightKg.toInt()}",
                    unit     = "kg",
                    label    = theme.t("KİLO", "WEIGHT"),
                    color    = theme.accentHi,
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
                    color    = theme.accentHi,
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
            .clip(RoundedCornerShape(SinemaRadius.R16))
            .background(theme.bg3)
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(value, color = theme.text0, fontSize = SinemaType.Title, fontWeight = FontWeight.Black)
            Text(unit, color = color, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = theme.text2, fontSize = SinemaType.Caption, letterSpacing = 0.5.sp)
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
            .padding(horizontal = sinemaGutter(), vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(SinemaRadius.R12))
                .background(theme.accentHi.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = theme.accentHi,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color      = theme.text0,
                fontSize   = SinemaType.BodyLg,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color    = theme.text2,
                fontSize = SinemaType.Caption
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
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumAction(theme, accent, onClick)
                .padding(horizontal = sinemaGutter(), vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(SinemaRadius.R12))
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
                    fontSize   = SinemaType.BodyLg,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (weightKg > 0) theme.t("${"%.1f".format(weightKg)} kg · Trend & AI Analiz", "${"%.1f".format(weightKg)} kg · Trend & AI Analysis")
                    else theme.t("Kilo ölçümlerini performans verilerinle birlikte takip et", "Track weight measurements alongside your performance data"),
                    color    = theme.text2,
                    fontSize = SinemaType.Caption
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
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .profilePremiumAction(theme, accent, onClick)
                .padding(horizontal = sinemaGutter(), vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(SinemaRadius.R12))
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
                    fontSize   = SinemaType.BodyLg,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    theme.t("Ağırlık · Süre · Mesafe · AI Analiz", "Weight · Duration · Distance · AI Analysis"),
                    color    = theme.text2,
                    fontSize = SinemaType.Caption
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
            .padding(horizontal = sinemaGutter(), vertical = 8.dp)
            .profilePremiumSurface(theme, accent = accent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.15f)),
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
                    fontSize      = SinemaType.BodySm,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    theme.t("Diğer kullanıcılarla kıyasla", "Compare with other users"),
                    color    = theme.text2,
                    fontSize = SinemaType.Caption
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
                shape = RoundedCornerShape(SinemaRadius.R12)
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
                fontSize   = SinemaType.Body,
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
            fontSize = SinemaType.Caption
        )
    }
}


@Composable
private fun sinemaGutter() = SinemaLayout.gutter(LocalWindowClass.current.widthDp)


/**
 * Profil üyelik kartı (çerçevesiz raised): Ücretsiz -> "Premium'a geç" amber düğme + 1 satır avantaj (+ kalan AI hakkı);
 * Premium -> "Premium üyesin" + durum + "Üyeliği yönet". İkisi de Mağaza'yı açar.
 * Yenileme tarihi: billing-status henüz dönmüyor; gelince [status] yanına eklenecek.
 */
@Composable
private fun ProfileMembershipCard(
    plan: com.cosmibit.profitness.data.store.UserPlan,
    status: String,
    freeLeft: Int?,
    theme: AppThemeState,
    onOpen: () -> Unit
) {
    val premium = plan == com.cosmibit.profitness.data.store.UserPlan.PREMIUM
    val english = theme.language == com.cosmibit.profitness.core.theme.AppLanguage.ENGLISH
    Column(
        Modifier
            .padding(horizontal = sinemaGutter(), vertical = 8.dp)
            .fillMaxWidth()
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R24), com.cosmibit.profitness.core.theme.SinemaRaise.Card, tint = theme.accentHi)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = theme.accentHi, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (premium) theme.t("Premium üyesin", "You're Premium") else theme.t("Ücretsiz plan", "Free plan"),
                    color = Sinema.text(theme), fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, maxLines = 1
                )
                Text(
                    if (premium) membershipStatusText(status, theme)
                    else theme.t("Premium: AI Koç, sınırsız AI program ve AI analizler.", "Premium: AI Coach, unlimited AI programs and AI analysis."),
                    color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 2
                )
                if (!premium && freeLeft != null) Text(
                    com.cosmibit.profitness.domain.ai.AiAccessPolicy.freeGrantLine(freeLeft, english),
                    color = theme.accentHi, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (premium) SinemaCapsule(
            modifier = Modifier.fillMaxWidth(),
            height = 48.dp,
            onClick = onOpen,
            horizontalArrangement = Arrangement.Center
        ) { fg ->
            Text(theme.t("Üyeliği yönet", "Manage membership"), color = fg, fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 1)
        } else SinemaGoButton(
            label = theme.t("PREMIUM'A GEÇ", "GO PREMIUM"),
            icon = Icons.Rounded.WorkspacePremium,
            onClick = onOpen
        )
    }
}

private fun membershipStatusText(status: String, theme: AppThemeState): String = when (status.lowercase()) {
    "active" -> theme.t("Abonelik aktif", "Subscription active")
    "trialing" -> theme.t("Deneme süresi", "Trial period")
    "canceled", "cancelled" -> theme.t("İptal edildi · dönem sonuna kadar aktif", "Canceled · active until period end")
    "past_due" -> theme.t("Ödeme bekleniyor", "Payment pending")
    else -> theme.t("Abonelik aktif", "Subscription active")
}


/** Premium altın rozeti (ad yanında, küçük ve canlı): taç ikonu + "Premium", dokununca Mağaza/üyelik. */
@Composable
private fun PremiumBadge(theme: AppThemeState, onClick: () -> Unit) {
    val gold = Brush.linearGradient(listOf(Color(0xFFF7D774), Color(0xFFD9A441), Color(0xFFB07A22)))
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .sinemaClickable(onClick = onClick, role = androidx.compose.ui.semantics.Role.Button)
            .semantics { contentDescription = theme.t("Premium üyelik, yönet", "Premium membership, manage") },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.height(28.dp).clip(CircleShape).background(gold).padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF3A2606), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Premium", color = Color(0xFF3A2606), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

/**
 * RANK & SEVİYE şeridi (kompakt, ~64dp, kart değil): mini metalik rank rozeti + "Seviye N", ince ana çubuk
 * (sonraki rank'a ilerleme) + "Gold'a 2.900 XP", ok. Dokununca Başarımlar/rank yol haritası; (i) kısa açıklama.
 */
@Composable
private fun RankLevelStrip(
    xp: Int,
    level: Int,
    rank: String,
    theme: AppThemeState,
    onOpenRoadmap: () -> Unit
) {
    val tiers = com.cosmibit.profitness.domain.progress.RankTiers
    val next = tiers.nextTier(xp)
    val progress = tiers.progressToNext(xp)
    val fmt = profileNumberFormat(theme)
    var showInfo by remember { mutableStateOf(false) }
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress, tween(900, easing = SinemaMotion.EaseStandard)) }
    val nextLine = if (next != null) theme.t("${next.name}'a ${fmt.format(next.minXp - xp)} XP", "${fmt.format(next.minXp - xp)} XP to ${next.name}")
                   else theme.t("En üst rank", "Top rank")

    Row(
        Modifier
            .padding(horizontal = sinemaGutter(), vertical = 4.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .sinemaClickable(onClick = onOpenRoadmap, scale = 0.98f, role = androidx.compose.ui.semantics.Role.Button)
            .semantics {
                contentDescription = theme.t(
                    "$rank rank, Seviye $level, $nextLine. Rank yol haritasını aç",
                    "$rank rank, Level $level, $nextLine. Open rank roadmap"
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini metalik rank rozeti (3D: gradyan + üst parlama)
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(rankBrush(rank))
                .drawWithCache {
                    val glow = Brush.verticalGradient(0f to Color.White.copy(0.35f), 0.5f to Color.Transparent)
                    onDrawWithContent { drawContent(); drawRect(glow) }
                },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.MilitaryTech, null, tint = Color(0xFF1A1B1F), modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(" · " + theme.t("Seviye $level", "Level $level"), color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text(nextLine, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1)
            }
            Text(
                theme.t("Toplam ${fmt.format(xp)} XP", "Total ${fmt.format(xp)} XP"),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Sinema.line(theme))) {
                Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().clip(CircleShape).background(rankBrush(next?.name ?: rank)))
            }
        }
        Box(
            Modifier.size(48.dp).sinemaClickable(onClick = { showInfo = true }, role = androidx.compose.ui.semantics.Role.Button),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Info, theme.t("Seviye ve rank nedir", "What are level and rank"), tint = Sinema.textSecondary(theme), modifier = Modifier.size(18.dp)) }
        Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
    }

    if (showInfo) {
        com.cosmibit.profitness.presentation.components.SinemaDialog(
            onDismiss = { showInfo = false },
            title = theme.t("Seviye ve Rank", "Level and Rank"),
            body = rankLevelExplainer(theme),
            confirmLabel = theme.t("Tamam", "OK"),
            confirmIcon = Icons.Rounded.Check,
            onConfirm = { showInfo = false },
            dismissLabel = theme.t("Kapat", "Close")
        )
    }
}

/** SIRALAMA kompakt satırı: rank rozeti + "Sıralaman #N · Genel"; veri yoksa boş durum. Dokununca Sıralama. */
@Composable
private fun RankingRow(rank: String, position: Long, totalUsers: Long, theme: AppThemeState, onOpen: () -> Unit) {
    val fmt = profileNumberFormat(theme)
    Row(
        Modifier
            .padding(horizontal = sinemaGutter(), vertical = 6.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .sinemaClickable(onClick = onOpen, scale = 0.98f, role = androidx.compose.ui.semantics.Role.Button)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), com.cosmibit.profitness.core.theme.SinemaRaise.Mini)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(rankBrush(rank)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Leaderboard, null, tint = Color(0xFF1A1B1F), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (position > 0) theme.t("Sıralaman #${fmt.format(position)} · Genel", "Your ranking #${fmt.format(position)} · Global")
                else theme.t("Sıralamaya gir", "Join the ranking"),
                color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 1
            )
            Text(
                if (position > 0 && totalUsers > 0) theme.t("${fmt.format(totalUsers)} sporcu arasında · arkadaş ve seri sıralaması da var", "Among ${fmt.format(totalUsers)} athletes · friends and streak boards too")
                else theme.t("Arkadaş ekle, antrenman yap, sıralamada yerini gör.", "Add friends, train, and see your place."),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(theme.t("Gör", "View"), color = theme.accentHi, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold)
        Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
    }
}

private fun profileNumberFormat(theme: AppThemeState): java.text.NumberFormat =
    java.text.NumberFormat.getIntegerInstance(
        if (theme.language == com.cosmibit.profitness.core.theme.AppLanguage.ENGLISH) java.util.Locale.ENGLISH
        else java.util.Locale.forLanguageTag("tr-TR")
    )

/** Seviye/Rank açıklaması (Profil ve Başarımlar aynı metni kullanır). */
internal fun rankLevelExplainer(theme: AppThemeState): String = theme.t(
    "Seviye her ${com.cosmibit.profitness.domain.progress.RankTiers.LEVEL_XP} XP'de yükselir.\nRank ise uzun vadeli ligin: Bronze → Silver → Gold → Platinum → Diamond.\nİkisi de aynı toplam XP'den gelir.",
    "Level goes up every ${com.cosmibit.profitness.domain.progress.RankTiers.LEVEL_XP} XP.\nRank is your long-term league: Bronze → Silver → Gold → Platinum → Diamond.\nBoth come from the same total XP."
)


/**
 * Performans değerleri (saf, testli mantık [performanceValues] ile): Bu hafta antrenman, Toplam hacim, Rekor
 * (en ağır kaldırış + hareket adı); hacim/rekor yoksa Toplam antrenman / Toplam süre / Mesafe ile tamamlanır. En çok 3.
 */
private fun performanceMetricsFor(
    theme: AppThemeState, weekWorkouts: Int, totalWorkouts: Int, totalVolumeKg: Double,
    topLiftName: String, topLiftKg: Float, totalDurationSeconds: Int, totalDistanceMeters: Float
): List<PerformanceMetric> = performanceValues(weekWorkouts, totalWorkouts, totalVolumeKg, topLiftName, topLiftKg, totalDurationSeconds, totalDistanceMeters)
    .map { v ->
        val en = theme.language == com.cosmibit.profitness.core.theme.AppLanguage.ENGLISH
        when (v) {
            is PerfValue.Week -> PerformanceMetric(v.count.toString(), theme.t("antr.", "wkt"), theme.t("Bu hafta", "This week"), Icons.Rounded.CalendarMonth, theme.accentColor)
            is PerfValue.Volume -> PerformanceMetric(formatVolume(v.kg, en).first, formatVolume(v.kg, en).second, theme.t("Toplam hacim", "Total volume"), Icons.Rounded.FitnessCenter, theme.accentHi)
            is PerfValue.TopLift -> PerformanceMetric(
                com.cosmibit.profitness.presentation.workout.formatWeightNumber(v.kg, if (en) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("tr-TR")),
                "kg", theme.t("Rekor · ${v.name}", "Record · ${v.name}"), Icons.Rounded.EmojiEvents, theme.rewardGold
            )
            is PerfValue.Workouts -> PerformanceMetric(v.count.toString(), theme.t("antr.", "wkt"), theme.t("Toplam antrenman", "Total workouts"), Icons.Rounded.Timer, theme.supportReward)
            is PerfValue.Duration -> {
                val (value, unit) = splitHoursMinutes(v.seconds, en)
                PerformanceMetric(value, unit, theme.t("Toplam süre", "Total time"), Icons.Rounded.Timer, theme.accentHi)
            }
            is PerfValue.Distance -> PerformanceMetric(formatProfileDistanceValue(v.meters), formatProfileDistanceUnit(v.meters), theme.t("Toplam mesafe", "Total distance"), Icons.Rounded.Straighten, theme.accentHi)
        }
    }

/** "12,4 t" / "860 kg" (TR virgül). */
private fun formatVolume(kg: Double, english: Boolean): Pair<String, String> {
    val loc = if (english) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("tr-TR")
    return if (kg >= 1000) String.format(loc, "%.1f", kg / 1000) to "t" else String.format(loc, "%.0f", kg) to "kg"
}

/** Büyük rakam + birim: 6300 sn -> ("1", "sa 45 dk"); 2700 -> ("45", "dk"); 7200 -> ("2", "sa"). */
internal fun splitHoursMinutes(seconds: Int, english: Boolean): Pair<String, String> {
    val m = seconds / 60
    val h = m / 60
    val mm = m % 60
    return when {
        h == 0 -> mm.toString() to (if (english) "min" else "dk")
        mm == 0 -> h.toString() to (if (english) "h" else "sa")
        else -> h.toString() to (if (english) "h $mm min" else "sa $mm dk")
    }
}
