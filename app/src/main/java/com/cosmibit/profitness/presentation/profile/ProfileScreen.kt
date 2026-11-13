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
import com.cosmibit.profitness.core.theme.*
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
import com.cosmibit.profitness.presentation.components.AccentColorSwatch
import com.cosmibit.profitness.presentation.components.CustomAccentColorDialog
import com.cosmibit.profitness.presentation.components.CustomAccentSwatch
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
    var showAppearance       by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        val profileListState = androidx.compose.foundation.lazy.rememberLazyListState()
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
                    onOpenStreakRanking = { onNavigateToLeaderboard(com.cosmibit.profitness.presentation.leaderboard.LeaderboardTab.Streak) }
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
                SettingsSection(
                    theme                = theme,
                    accent               = accent,
                    strings              = strings,
                    onLogout             = onLogout,
                    onAppearanceClick    = { showAppearance = true },
                    onEditProfile        = onEditProfile,
                    onNotificationsClick = { showNotifications = true },
                    onLanguageClick      = { showLanguagePicker = true },
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
                            letterSpacing = 1.2.sp
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
            containerColor   = Sinema.surface(theme),
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
    "silver"   -> Color(0xFFB0BEC5)
    "gold"     -> Color(0xFFF2C23F)
    "platinum" -> Color(0xFF3DB5E8)
    "diamond"  -> Color(0xFF64B5F6)
    else       -> Color(0xFFCD7F32)  // Bronze
}

/** Çizimdeki `.chip.metal`: silver için birebir 135° C0C4CC→8E949E; diğer rütbelerde rütbe renginden türetilir. */
private fun rankBrush(rank: String): Brush {
    if (rank.equals("silver", ignoreCase = true)) return Brush.linearGradient(listOf(Color(0xFFC0C4CC), Color(0xFF8E949E)))
    val c = rankColor(rank)
    return Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(c, Color.White, 0.18f), androidx.compose.ui.graphics.lerp(c, Color.Black, 0.22f)))
}

@Composable
private fun profileMotionOn(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
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
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(28.dp)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(fill))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
    onOpenStreakRanking: () -> Unit = {}
) {
    val page = Sinema.page(theme)
    val textMain = Sinema.text(theme)
    val textSub = Sinema.textSecondary(theme)
    val xpInLevel = if (xpPerLevel > 0) xp % xpPerLevel else xp
    val xpProgress = if (xpPerLevel > 0) xpInLevel.toFloat() / xpPerLevel else 0f
    val xpLeft = (xpPerLevel - xpInLevel).coerceAtLeast(0)
    val fill = remember { Animatable(0f) }
    LaunchedEffect(xpProgress) { fill.animateTo(xpProgress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    val motionOn = profileMotionOn()
    val kenBurns by rememberInfiniteTransition(label = "profile_kb").animateFloat(
        initialValue = 1f, targetValue = if (motionOn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(Sinema.KenBurnsMs, easing = LinearEasing), RepeatMode.Reverse),
        label = "kb"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── Kapak 320dp (çizim `.cover`): foto + 3 duraklı karartma (.5 → 0 @%40 → .75) ──
        Box(Modifier.fillMaxWidth().height(320.dp).clipToBounds()) {
            // Foto + karartma: alt 200dp maske ile sayfa zeminine (SinemaBackdrop) söner
            Box(Modifier.fillMaxSize().photoFadeToBackdrop()) {
                SceneImage(
                    url = categoryImageUrl(""),
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationY = scrollOffset() * 0.5f
                        scaleX = kenBurns; scaleY = kenBurns
                    },
                    requestWidthPx = 1080, requestHeightPx = 960,
                    fallbackGradient = true
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Sinema.scrim(0.50f), 0.40f to Color.Transparent, 1f to Sinema.scrim(0.75f)
                        )
                    )
                )
            }

            // XP satırı (çizim `.xpb`: üstten 206dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 206.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$xpInLevel / $xpPerLevel XP",
                        color = Sinema.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (xpLeft == 0) theme.t("hazır", "ready") else theme.t("$xpLeft kaldı", "$xpLeft left"),
                        color = Sinema.White.copy(0.75f), fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Sinema.White.copy(0.28f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fill.value)
                            .fillMaxHeight()
                            .sinemaGlow(accent, 12.dp, 0.dp, CircleShape)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
        }

        // ── Sheet (çizim `.sheet`: üstten 300dp, r28, üstte 1dp çizgi) ───────────────
        val lineColor = Sinema.line(theme)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 300.dp)
        ) {
            // Avatar (96dp, 4dp sayfa renginde halka, vurgu dolgusu) + ad (sol 128dp, üst 14dp)
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(62.dp)) {
                Box(
                    Modifier
                        .offset(y = (-48 + (96 - 62) / 2).dp) // requiredSize 62dp satırda ortalanır; avatar üstü sheet üstünden 48dp yukarıda
                        .requiredSize(96.dp)
                        .clip(CircleShape)
                        .background(page)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(accent),
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
                        Text(avatar, fontSize = 40.sp, color = theme.effectiveOnAccentColor)
                    }
                }
                Text(
                    name,
                    color = textMain, style = Sinema.Title,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 112.dp, top = 14.dp)
                )
            }

            // Çipler (sol 16dp, üstten 62dp): rütbe (metal) · seviye · plan/enerji
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileChip(
                    rank.uppercase(), Color(0xFF0A0A0B), brush = rankBrush(rank), onClick = onOpenRankRanking
                )
                ProfileChip(theme.t("Seviye $level", "Level $level"), textMain, fill = Sinema.surface(theme))
                val isPaid = userPlan != UserPlan.FREE
                ProfileChip(
                    if (isPaid) "${userPlan.displayName} · $aiCredits" else theme.t("$aiCredits Enerji", "$aiCredits Energy"),
                    textMain, fill = Sinema.surface(theme), onClick = onNavigateToStore
                )
            }

            // 3 metrik (üstten 118dp; 28dp aralık → 78dp yükseklik: 196'daki XP çizgisine kadar)
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetric(currentStreak.toString(), theme.t("Seri · gün", "Streak · days"), theme, Modifier.weight(1f), onOpenStreakRanking)
                HeroMetric(longestStreak.toString(), theme.t("En uzun seri", "Longest streak"), theme, Modifier.weight(1f))
                HeroMetric(totalExercises.toString(), theme.t("Egzersiz", "Exercises"), theme, Modifier.weight(1f))
            }

            // Toplam XP (çizim `.tot`: üst çizgi, 14dp iç boşluk, 30sp rakam); sonraki bölüm 284dp çevresinden başlar
            Box(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(lineColor))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp)
                        .clickable(onClick = onOpenXpRanking)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        theme.t("TOPLAM XP", "TOTAL XP"), color = textSub, style = Sinema.Label.copy(fontSize = 11.sp),
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        "$xp", color = textMain,
                        style = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em),
                        maxLines = 1, modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String, theme: AppThemeState, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            value,
            color = Sinema.text(theme),
            style = if (value.length >= 4) Sinema.Metric.copy(fontSize = 34.sp) else Sinema.Metric,
            maxLines = 1, softWrap = false
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label.sinemaUpper(theme), color = Sinema.textSecondary(theme),
            style = Sinema.Label.copy(fontSize = 10.sp, lineHeight = 13.sp), maxLines = 2
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
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(Sinema.line(theme)))
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text, color = Sinema.text(theme), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    onNavigateToDetail: () -> Unit
) {
    val metrics = buildList {
        add(PerformanceMetric(currentStreak.toString(),  strings.unitStreak, strings.dailyStreakLabel,  Icons.Rounded.LocalFireDepartment, theme.effectiveAccentColor))
        add(PerformanceMetric(longestStreak.toString(),  strings.unitStreak, theme.t("En uzun seri", "Longest streak"), Icons.Rounded.EmojiEvents, theme.supportReward))
        add(PerformanceMetric(totalExercises.toString(), theme.t("kez", "times"), theme.t("Toplam egzersiz", "Total exercises"), Icons.Rounded.FitnessCenter, theme.supportStrength))
        if (totalDurationSeconds > 0) {
            add(PerformanceMetric(formatProfileDurationValue(totalDurationSeconds), theme.t("dk", "min"), theme.t("Toplam süre", "Total duration"), Icons.Rounded.Timer, theme.supportSuccess))
        }
        if (totalDistanceMeters > 0f) {
            add(PerformanceMetric(formatProfileDistanceValue(totalDistanceMeters), formatProfileDistanceUnit(totalDistanceMeters), theme.t("Toplam mesafe", "Total distance"), Icons.Rounded.Straighten, theme.supportRest))
        }
    }

    SinemaSection(
        theme = theme,
        title = theme.t("Performans", "Performance"),
        trailing = { SeeAllLink(theme, strings.seeAll, onNavigateToDetail) }
    ) {
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            items(metrics) { metric -> MetricCell(metric, theme, onNavigateToDetail) }
        }
    }
}

@Composable
private fun MetricCell(metric: PerformanceMetric, theme: AppThemeState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
                color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(metric.color))
        Spacer(Modifier.height(10.dp))
        Text(metric.label, color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
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

    Column(modifier = Modifier.padding(20.dp, 36.dp, 20.dp, 0.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                strings.weeklyActivity,
                style         = MaterialTheme.typography.labelSmall,
                color         = theme.accentTextColor,
                letterSpacing = 1.2.sp
            )
            Text(
                if (weekWorkoutCount > 0) "${strings.thisWeekSummary} · " + theme.t("$weekWorkoutCount antrenman", "$weekWorkoutCount workouts")
                else strings.thisWeekSummary,
                color = theme.text2, fontSize = 10.sp
            )
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
                            color      = if (isToday) theme.accentTextColor else theme.text2,
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
                .heightIn(min = 44.dp)
                .clickable(onClick = onSeeAll)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
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
        Text(achievement.icon, fontSize = 24.sp, modifier = Modifier.alpha(if (unlocked) 1f else 0.3f))
    }
}

// ── Settings Section ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    theme               : AppThemeState,
    accent              : Color,
    strings             : AppStrings,
    onLogout            : () -> Unit = {},
    onAppearanceClick   : () -> Unit = {},
    onEditProfile       : () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLanguageClick     : () -> Unit = {},
    onExportData        : () -> Unit = {},
    onImportData        : () -> Unit = {},
    isTransferring      : Boolean    = false,
    displayName         : String     = "",
    avatar              : String     = "🏋️"
) {
    val groupShape = RoundedCornerShape(20.dp)
    val surface = Sinema.surface(theme)
    val divider: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().padding(start = 72.dp, end = 20.dp).height(1.dp).background(Sinema.line(theme)))
    }

    SinemaSection(theme = theme, title = strings.accountSettings) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(groupShape).background(surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditProfile)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Sinema.page(theme)).border(2.dp, accent, CircleShape),
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
                        Text(avatar, fontSize = 20.sp)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        displayName.ifBlank { theme.t("Kullanıcı", "User") }.uppercase(),
                        color = Sinema.text(theme), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(strings.editProfileHint, color = Sinema.textSecondary(theme), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.Edit, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(groupShape).background(surface)
        ) {
            val notifStatus = if (theme.notificationsEnabled) strings.notificationsActive else strings.notificationsOff
            SettingsRow(
                Icons.Rounded.Tune, theme.t("Görünüm", "Appearance"),
                theme.t("Tema ve vurgu rengi", "Theme and accent color"), theme, accent, onClick = onAppearanceClick
            )
            divider()
            SettingsRow(Icons.Rounded.Notifications, strings.notificationsLabel, notifStatus, theme, accent, onClick = onNotificationsClick)
            divider()
            SettingsRow(Icons.Rounded.Language, strings.languageLabel, strings.currentLanguageName, theme, accent, onClick = onLanguageClick)
        }

        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(Sinema.line(theme)))
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
            Text(theme.t("VERİLER", "DATA"), color = Sinema.textSecondary(theme), style = Sinema.Label)
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(groupShape).background(surface)
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
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        val coral = CardCoral
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(groupShape)
                .background(surface)
                .clickable(onClick = onLogout)
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Rounded.Logout, null, tint = coral, modifier = Modifier.size(22.dp))
            Text(
                strings.logoutLabel,
                color      = coral,
                fontSize   = 15.sp,
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
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.55f))
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Sinema.text(theme), modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = Sinema.text(theme), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Sinema.textSecondary(theme), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
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
        isDark          = true,   // tek koyu tema (açık tema kaldırıldı)
        accent           = accent,
        surfaceStyle     = SurfaceStyle.GRAPHITE,
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
                        isDark       = true,
                        accent       = accent,
                        surfaceStyle = SurfaceStyle.GRAPHITE,
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
        letterSpacing = 1.2.sp
    )
}

/** Ayarların canlı önizlemesi: kart yüzeyi + başlık + mini buton. */
@Composable
private fun PreviewCard(preview: AppThemeState) {
    val acc = preview.effectiveAccentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(preview, RoundedCornerShape(24.dp))
            .padding(20.dp),
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
                color         = preview.accentTextColor,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 1.2.sp
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
                letterSpacing = 1.2.sp
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
            .sinemaCard(theme, RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            val segShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSel) Modifier.sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, segShape) else Modifier)
                    .clip(segShape)
                    .then(if (isSel) Modifier.background(accent) else Modifier)
                    .clickable { onSelect(value) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color         = if (isSel) onAccent else theme.text1,
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
            letterSpacing = 1.2.sp,
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
            border = BorderStroke(1.dp, Color.White.copy(0.28f)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 13.sp)
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
                .sinemaCard(theme, RoundedCornerShape(24.dp))
        ) {
            LanguageOptionRow(
                flag       = "🇹🇷",
                name       = strings.turkishLabel,
                isSelected = selected == AppLanguage.TURKISH,
                accent     = accent,
                theme      = theme,
                onClick    = { selected = AppLanguage.TURKISH }
            )
            HorizontalDivider(color = Sinema.line(theme), modifier = Modifier.padding(horizontal = 16.dp))
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
            border = BorderStroke(1.dp, Color.White.copy(0.28f)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(strings.applyLabel, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 13.sp)
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
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(flag, fontSize = 24.sp)
        Text(
            name,
            color      = theme.text0,
            fontSize   = 15.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
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
            .background(theme.bg3)
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
            letterSpacing = 1.2.sp
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
            letterSpacing = 1.2.sp
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
                    letterSpacing = 1.2.sp
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

