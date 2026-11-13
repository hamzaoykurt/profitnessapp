@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.cosmibit.profitness.presentation.workout

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.cosmibit.profitness.data.workout.SetCompletionKey
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.accent2Color
import com.cosmibit.profitness.core.theme.accent2TextColor
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.presentation.components.glassCard
import com.cosmibit.profitness.presentation.components.PremiumButton
import com.cosmibit.profitness.presentation.components.GhostButton
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.presentation.challenges.ChallengeDetailOverlay
import com.cosmibit.profitness.presentation.dashboard.ChallengeTodayBanner
import com.cosmibit.profitness.presentation.dashboard.DashboardViewModel
import com.cosmibit.profitness.presentation.dashboard.UpcomingEventsSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay

// ── Data models ─────────────────────────────────────────────────────────────

private const val FIRST_REMOTE_SYNC_DELAY_MS = 1_500L
private const val FIRST_DASHBOARD_EVENTS_DELAY_MS = 1_700L

internal fun localizedWorkoutDayLabel(value: String, theme: AppThemeState): String = when (value.lowercase()) {
    "pzt" -> theme.t("Pzt", "Mon")
    "sal" -> theme.t("Sal", "Tue")
    "çar", "car" -> theme.t("Çar", "Wed")
    "per" -> theme.t("Per", "Thu")
    "cum" -> theme.t("Cum", "Fri")
    "cmt" -> theme.t("Cmt", "Sat")
    "paz" -> theme.t("Paz", "Sun")
    else -> value
}

private fun localizedWorkoutTitle(value: String, theme: AppThemeState): String =
    if (theme.language == AppLanguage.ENGLISH) {
        when (value) {
            "GÖĞÜS & TRİSEPS" -> "CHEST & TRICEPS"
            "SIRT & BiSEPS" -> "BACK & BICEPS"
            "DİNLENME" -> "REST"
            "OMUZ & BACAK" -> "SHOULDERS & LEGS"
            "CORE & KARDİYO" -> "CORE & CARDIO"
            "MOBİLİTE & GERİNME" -> "MOBILITY & STRETCHING"
            "Mobilite" -> "Mobility"
            else -> theme.fitnessTermDisplayName(value)
        }
    } else value

private val WorkoutTitlePrefix = Regex(
    pattern = """^\s*(?:(?:pazartesi|salı|sali|çarşamba|carsamba|perşembe|persembe|cuma|cumartesi|pazar|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\s*[:—–-]\s*)?(?:\d+\s*\.\s*(?:gün|gun|day)\s*[:—–-]\s*)?""",
    option = RegexOption.IGNORE_CASE
)

internal fun compactWorkoutTitle(value: String, theme: AppThemeState): String {
    val localized = localizedWorkoutTitle(value, theme).trim()
    val compact = localized.replaceFirst(WorkoutTitlePrefix, "").trim().ifBlank { localized }
    if (compact.any { it.isLowerCase() }) return compact
    val locale = if (theme.language == AppLanguage.TURKISH) java.util.Locale.forLanguageTag("tr-TR") else java.util.Locale.ENGLISH
    return compact.lowercase(locale).split(" ").joinToString(" ") { word ->
        if (word == "&") word else word.replaceFirstChar { it.titlecase(locale) }
    }
}

@Stable
data class Exercise(
    val id: String,
    val name: String,
    val target: String,
    val sets: Int,
    val reps: String,
    val image: String,
    val isCompleted: Boolean = false,
    val category: String = "Strength",
    val restSeconds: Int = 60,           // set arası dinlenme
    val exerciseRestSeconds: Int = 180,  // son set / egzersiz sonu — sonraki harekete geçmeden önce
    val exerciseTableId: String = "", // FK to exercises table — used for DB logging
    val weightKg: Float = 0f,         // programdan gelen planlı ağırlık
    val sportType: String = "",
    val trackingMode: String = "",
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Float? = null,
    val targetElevationMeters: Float? = null,
    val targetInclinePercent: Float? = null,
    val section: String = "",
    val notes: String = "",
    val groupId: String? = null,
    val groupType: String = "straight",
    val groupLabel: String = "",
    val groupRounds: Int? = null,
    val groupRestSeconds: Int? = null,
    val challengeId: String? = null,
    /** Dikey (9:16) hareket videosu; null → bugünkü gibi foto. Kaynak: uzak `exercises.video_url` (bkz. ExerciseNameDto). */
    val videoUrl: String? = null
)

data class WorkoutDay(
    val day: String,
    val title: String,
    val exercises: ImmutableList<Exercise>,
    val isRestDay: Boolean = false,
    val notes: String = "",
    val totalKcal: Int = 0,
    val durationMin: Int = 0,
    val programDayId: String = ""   // program_days tablosundaki ID — workout log için
)

// ── Demo Data — 7 günün hepsi dolu ─────────────────────────────────────────

val DEMO_WORKOUTS = persistentListOf(
    WorkoutDay("Pzt", "GÖĞÜS & TRİSEPS", persistentListOf(
        Exercise("1", "Bench Press", "Göğüs", 4, "8-12",
            "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800", category = "Strength"),
        Exercise("2", "Incline DB Press", "Üst Göğüs", 3, "10",
            "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800", category = "Strength"),
        Exercise("3", "Dips", "Triceps", 3, "12",
            "https://images.unsplash.com/photo-1598971639058-fab3c32f850c?w=800", category = "Bodyweight"),
        Exercise("4", "Cable Flyes", "Göğüs", 3, "15",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Cable")
    ), totalKcal = 420, durationMin = 55),
    WorkoutDay("Sal", "SIRT & BiSEPS", persistentListOf(
        Exercise("5", "Lat Pulldown", "Sırt", 4, "10-15",
            "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=800", category = "Cable"),
        Exercise("6", "Barbell Row", "Orta Sırt", 4, "8-10",
            "https://images.unsplash.com/photo-1532384661128-d446b2d55db7?w=800", category = "Strength"),
        Exercise("7", "Pull-ups", "Sırt/Biceps", 3, "Max",
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800", category = "Bodyweight"),
        Exercise("8", "Hammer Curl", "Biceps", 3, "12",
            "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800", category = "Strength")
    ), totalKcal = 380, durationMin = 50),
    WorkoutDay("Çar", "DİNLENME", persistentListOf(), isRestDay = true),
    WorkoutDay("Per", "OMUZ & BACAK", persistentListOf(
        Exercise("9", "Squat", "Quadriceps", 4, "8-12",
            "https://images.unsplash.com/photo-1567013127542-490d757e51fc?w=800", category = "Strength"),
        Exercise("10", "Shoulder Press", "Omuz", 4, "10",
            "https://images.unsplash.com/photo-1584735935169-11c341bfed13?w=800", category = "Strength"),
        Exercise("11", "Lateral Raise", "Yan Omuz", 3, "15",
            "https://images.unsplash.com/photo-1530822847156-5df684ec5933?w=800", category = "Cable"),
        Exercise("12", "Romanian DL", "Hamstring", 3, "10",
            "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800", category = "Strength")
    ), totalKcal = 510, durationMin = 65),
    WorkoutDay("Cum", "CORE & KARDİYO", persistentListOf(
        Exercise("13", "Plank", "Core", 3, "60s",
            "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=800", category = "Bodyweight"),
        Exercise("14", "Mountain Climber", "Core/Cardio", 3, "30s",
            "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800", category = "Bodyweight"),
        Exercise("15", "Cable Crunch", "Core", 3, "20",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Cable"),
        Exercise("16", "Burpee", "Full Body", 3, "15",
            "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800", category = "Bodyweight")
    ), totalKcal = 480, durationMin = 45),
    WorkoutDay("Cmt", "DİNLENME", persistentListOf(), isRestDay = true),
    WorkoutDay("Paz", "MOBİLİTE & GERİNME", persistentListOf(
        Exercise("17", "Hip Flexor Stretch", "Kalça", 2, "60s",
            "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800", category = "Bodyweight"),
        Exercise("18", "Thoracic Rotation", "Sırt Mobilite", 2, "10 rep",
            "https://images.unsplash.com/photo-1511275539165-cc46b1ee8960?w=800", category = "Bodyweight"),
        Exercise("19", "Band Pull Apart", "Omuz Mobilite", 3, "20",
            "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800", category = "Cable"),
        Exercise("20", "World's Greatest", "Full Body", 2, "5/yan",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800", category = "Bodyweight")
    ), totalKcal = 220, durationMin = 35)
)

// ── Mutable state wrapper ────────────────────────────────────────────────────

data class WorkoutDayState(
    val day: WorkoutDay,
    val completedIds: ImmutableSet<String> = persistentSetOf()
) {
    val completedCount get() = completedIds.size
    val totalCount get() = day.exercises.size
    val progress get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

@Composable
fun WorkoutScreen(
    bottomPadding           : Dp = 0.dp,
    floatingTimerPad        : Dp = 0.dp,
    onNavigateToAIBuilder   : () -> Unit = {},
    onNavigateToManualBuilder: () -> Unit = {},
    onNavigateToStore       : () -> Unit = {},
    viewModel               : WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val theme   = LocalAppTheme.current
    val strings = theme.strings
    val context = LocalContext.current

    // ── Bildirim izni (Android 13+) ───────────────────────────────────────────
    var notifPermissionDenied by remember { mutableStateOf(false) }
    var pendingTimerAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingTimerAction
        pendingTimerAction = null
        if (granted) {
            action?.invoke()
        } else {
            notifPermissionDenied = true
        }
    }
    val runWithNotificationPermission: (() -> Unit) -> Unit = remember(context) {
        { action ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                action()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    action()
                } else {
                    pendingTimerAction = action
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { pendingTimerAction = null }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionDenied = false
        }
    }
    LaunchedEffect(Unit) {
        delay(FIRST_REMOTE_SYNC_DELAY_MS)
        viewModel.triggerInitialSync()
    }

    var showPaywall by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutEvent.ShowPaywall -> showPaywall = true
            }
        }
    }
    BackHandler(enabled = showPaywall || notifPermissionDenied) {
        when {
            showPaywall -> showPaywall = false
            notifPermissionDenied -> notifPermissionDenied = false
        }
    }
    if (showPaywall) {
        com.cosmibit.profitness.presentation.store.PaywallDialog(
            onDismiss   = { showPaywall = false },
            onGoToStore = { showPaywall = false; onNavigateToStore() }
        )
    }

    // ON_STOP bildirimi hemen tazeler; ON_RESUME arka planda stale-aware Supabase sync tetikler.
    WorkoutRefreshOnResumeEffect(
        onStop = { viewModel.syncVisibleTimerNotification(force = true) },
        onResume = { viewModel.refresh() }
    )

    // Kronometre artık üstte değil: DashboardScreen'deki yüzen kapsül tüm sayfalarda gösterir.
    val focusExerciseId by viewModel.focusExerciseRequest.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Zemin: DashboardScreen kökündeki tek SinemaBackdrop (yükleniyor / boş / dinlenme dahil)

        // ── İçerik ────────────────────────────────────────────────────────────
        when {
            state.isLoading -> {
                // Yükleniyor: yalnız ortak zemin (yukarıdaki SinemaBackdrop), metin yok.
                Box(Modifier.fillMaxSize())
            }
            state.dayStates.isEmpty() -> {
                NoProgramView(
                    bottomPadding = bottomPadding + floatingTimerPad,
                    onAI = onNavigateToAIBuilder,
                    onManual = onNavigateToManualBuilder
                )
            }
            else -> {
                WorkoutContent(
                    state = state,
                    restTimer = restTimer,
                    viewModel = viewModel,
                    bottomPadding = bottomPadding,
                    floatingTimerPad = floatingTimerPad,
                    focusExerciseId = focusExerciseId,
                    runWithNotificationPermission = runWithNotificationPermission
                )
            }
        }

        // ── Bildirim izni banner — Neon Forge glass stili ─────────────────────
        AnimatedVisibility(
            visible = notifPermissionDenied,
            enter   = slideInVertically { -it } + fadeIn(tween(SinemaMotion.Emphasis)),
            exit    = slideOutVertically { -it } + fadeOut(tween(SinemaMotion.Standard)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NotificationPermissionBanner(
                onRequest = {
                    notifPermissionDenied = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { notifPermissionDenied = false }
            )
        }
    }
}

// ── Bildirim İzni Banner — Neon Forge glass kart ─────────────────────────────
@Composable
private fun NotificationPermissionBanner(
    onRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp, 20.dp, 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.bg2)
                .padding(20.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.t("Bildirim izni gerekli", "Notification permission needed"),
                    color      = theme.text0,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    theme.t("Timer sesi + arka plan bildirimleri", "Timer sound + background notifications"),
                    color    = theme.text1,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            // İzin ver butonu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRequest()
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    theme.t("İZİN VER", "ALLOW"),
                    color      = theme.effectiveOnAccentColor,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Kapat
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.bg3)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    null,
                    tint     = theme.text2,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun NoProgramView(
    bottomPadding: Dp,
    onAI: () -> Unit,
    onManual: () -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 80.dp, bottom = bottomPadding + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon
        Text("🏋️", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))

        Text(
            theme.t("HENÜZ PROGRAMIN YOK", "NO PROGRAM YET"),
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            theme.t(
                "Antrenman programını şimdi oluştur.\nAI sana özel bir plan hazırlasın ya da kendin düzenle.",
                "Create your training program now.\nLet AI prepare a personal plan or build it yourself."
            ),
            color = theme.text2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(36.dp))

        // AI ile Oluştur
        PremiumButton(
            text = theme.t("AI İLE OLUŞTUR", "CREATE WITH AI"),
            onClick = onAI,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Manuel Oluştur
        GhostButton(
            text = theme.t("MANUEL OLUŞTUR", "CREATE MANUALLY"),
            onClick = onManual,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Stable
private data class ExerciseDraftInputs(
    val setWeights: Map<Int, String> = emptyMap(),
    val setDurations: Map<Int, String> = emptyMap()
)

private fun Map<String, DraftInput>.draftsForExercise(exerciseId: String, totalSets: Int): ExerciseDraftInputs {
    if (totalSets <= 0) return ExerciseDraftInputs()

    val weights = mutableMapOf<Int, String>()
    val durations = mutableMapOf<Int, String>()
    for (setIndex in 0 until totalSets) {
        val draft = this[draftInputKey(exerciseId, setIndex)] ?: continue
        if (draft.hasWeight) {
            weights[setIndex] = draft.weight
        }
        if (draft.hasDuration) {
            durations[setIndex] = draft.duration
        }
    }
    return ExerciseDraftInputs(
        setWeights = if (weights.isEmpty()) emptyMap() else weights.toMap(),
        setDurations = if (durations.isEmpty()) emptyMap() else durations.toMap()
    )
}

@Composable
private fun WorkoutContent(
    state: WorkoutScreenState,
    restTimer: RestTimerState,
    viewModel: WorkoutViewModel,
    bottomPadding: Dp,
    floatingTimerPad: Dp = 0.dp,
    focusExerciseId: String? = null,
    runWithNotificationPermission: (() -> Unit) -> Unit = { action -> action() }
) {
    val dayStates = state.dayStates
    val selectedDayIdx = state.selectedDayIdx
    val currentState = dayStates[selectedDayIdx]
    val currentDay   = currentState.day
    val draftInputs by viewModel.draftInputs.collectAsStateWithLifecycle()
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    // ── FAZ 7J-9: Dashboard event challenges ──────────────────────────────
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val dashState by dashboardVm.state.collectAsStateWithLifecycle()
    var detailChallengeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(FIRST_DASHBOARD_EVENTS_DELAY_MS)
        dashboardVm.reloadIfStale()
    }
    WorkoutRefreshOnResumeEffect { dashboardVm.reloadIfStale() }
    LaunchedEffect(state.challengeSyncVersion) {
        if (state.challengeSyncVersion > 0) dashboardVm.refresh(force = true)
    }

    // Event banner verisi stale ise resume sonrası yenilenir; UI tab geçişi bloklanmaz.
    val todayEvents = dashState.today
    val upcomingEvents = dashState.upcoming
    val hasTodayBanner = todayEvents.isNotEmpty()
    val skipProgramToday = dashState.skipProgramToday && hasTodayBanner
    val hasUpcomingSection = upcomingEvents.any {
        it.event?.dateIso != java.time.LocalDate.now().toString()
    }
    val visibleExercises = remember(currentDay.exercises, todayEvents) {
        currentDay.exercises + todayEvents.mapNotNull { event ->
            challengeExerciseForHome(event, currentDay.exercises)
        }
    }

    BackHandler(enabled = detailChallengeId != null) { detailChallengeId = null }

    val allDone = currentState.totalCount > 0 && currentState.completedCount >= currentState.totalCount
    val isRestHero = currentDay.isRestDay && visibleExercises.isEmpty()
    var detailExercise by remember { mutableStateOf<Exercise?>(null) }

    // Durum çubuğu ikonları foto sayfaların (koyu karartma) üstünde açık renk olmalı.
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        if (controller != null) controller.isAppearanceLightStatusBars = false
        onDispose { if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    val actions = remember(viewModel, selectedDayIdx, runWithNotificationPermission) {
        SinemaWorkoutActions(
            onToggleSet = { ex, si -> viewModel.toggleSet(ex.id, si) },
            onComplete = { ex ->
                if (ex.challengeId != null) viewModel.toggleChallengeActivity(selectedDayIdx, ex)
                else viewModel.toggleExercise(selectedDayIdx, ex.id)
            },
            onSetWeightChanged = { ex, si, v -> viewModel.updateSetWeight(ex.id, si, v) },
            onSetDurationChanged = { ex, si, v -> viewModel.updateSetDuration(ex.id, si, v) },
            onActivityDurationChanged = { ex, v -> viewModel.updateActivityDuration(ex.id, v) },
            onActivityDistanceChanged = { ex, v -> viewModel.updateActivityDistance(ex.id, v) },
            onActivityElevationChanged = { ex, v -> viewModel.updateActivityElevation(ex.id, v) },
            onActivityInclineChanged = { ex, v -> viewModel.updateActivityIncline(ex.id, v) },
            onActivityRepsChanged = { ex, v -> viewModel.updateActivityReps(ex.id, v) },
            onStartTimer = { ex, secs ->
                runWithNotificationPermission {
                    viewModel.startActivityCountdownTimer(ex.id, ex.name, secs.takeIf { it > 0 } ?: 60)
                }
            },
            onStartSetTimer = { ex, si, secs ->
                runWithNotificationPermission {
                    viewModel.startTimedSetCountdownTimer(ex.id, ex.name, si, secs.takeIf { it > 0 } ?: 60)
                }
            },
            onStartSetStopwatch = { ex, si ->
                runWithNotificationPermission { viewModel.startTimedSetStopwatchTimer(ex.id, ex.name, si) }
            },
            onStartStopwatch = { ex ->
                runWithNotificationPermission { viewModel.startActivityStopwatchTimer(ex.id, ex.name) }
            },
            onLoadLastSession = { ex -> viewModel.loadLastSession(ex.id) },
            onShowDetail = { ex -> detailExercise = ex }
        )
    }

    val dataFor: (Exercise) -> SinemaExerciseData = { exercise ->
        val isCompleted = exercise.id in currentState.completedIds
        val completionKey = SetCompletionKey(
            programDayId = currentState.day.programDayId,
            exerciseId = exercise.exerciseTableId.ifBlank { exercise.id }
        )
        val persistedSetCompletions = state.setCompletions[completionKey]
        val doneSetIndices = if (isCompleted) (0 until exercise.sets).toSet() else persistedSetCompletions ?: emptySet()
        val exerciseDrafts = draftInputs.draftsForExercise(exercise.id, exercise.sets)
        val savedWeights = state.setWeights[exercise.id].orEmpty()
        val savedDurations = state.setDurations[exercise.id].orEmpty()
        SinemaExerciseData(
            doneSetIndices = doneSetIndices,
            setWeights = if (exerciseDrafts.setWeights.isEmpty()) savedWeights else savedWeights + exerciseDrafts.setWeights,
            setDurations = if (exerciseDrafts.setDurations.isEmpty()) savedDurations else savedDurations + exerciseDrafts.setDurations,
            lastSessionData = state.lastSessionData[exercise.id] ?: emptyMap(),
            profileWeightKg = state.profileWeightKg,
            activityDuration = state.activityDurations[exercise.id]
                ?: exercise.targetDurationSeconds?.let { (it / 60f).trimmedUi() }
                ?: exercise.inferredHoldDurationSeconds()?.let { (it / 60f).trimmedUi() }
                ?: "",
            activityDistance = state.activityDistances[exercise.id]
                ?: exercise.targetDistanceMeters?.trimmedUi()
                ?: "",
            activityElevation = state.activityElevations[exercise.id]
                ?: exercise.targetElevationMeters?.trimmedUi()
                ?: "",
            activityIncline = state.activityInclines[exercise.id]
                ?: exercise.targetInclinePercent?.trimmedUi()
                ?: "",
            activityReps = state.activityReps[exercise.id]
                ?: exercise.reps.takeIf {
                    exercise.trackingMode == "duration_reps" || exercise.sportType == "jump_rope_hiit"
                }
                ?: ""
        )
    }

    SinemaWorkoutPager(
        programTitle = compactWorkoutTitle(currentDay.title, theme),
        exercises = visibleExercises,
        completedIds = currentState.completedIds,
        days = dayStates,
        selectedDayIdx = selectedDayIdx,
        onSelectDay = { viewModel.selectDay(it) },
        streak = state.currentStreak,
        progress = currentState.progress,
        isRestDay = isRestHero,
        isSkipped = skipProgramToday,
        allDone = allDone,
        dataFor = dataFor,
        actions = actions,
        topExtra = 0.dp,
        bottomExtra = floatingTimerPad,
        focusExerciseId = focusExerciseId,
        onFocusConsumed = { viewModel.consumeFocusExercise() },
        listExtras = { dismiss ->
            if (hasUpcomingSection) {
                UpcomingEventsSection(events = upcomingEvents, onOpen = { dismiss(); detailChallengeId = it.id })
            }
            if (hasTodayBanner) {
                ChallengeTodayBanner(events = todayEvents, onOpen = { dismiss(); detailChallengeId = it.id })
            }
        }
    )

    // "Hareketi gör": hareketin detay sheet'i.
    detailExercise?.let { ex ->
        LaunchedEffect(ex.id) {
            viewModel.loadExerciseHistory(ex.id)
        }
        ExerciseDetailSheet(
            exercise          = ex,
            dayNotes          = currentDay.notes,
            onDismiss         = { detailExercise = null },
            weightHistory     = state.exerciseHistory[ex.id] ?: emptyList(),
            aiInsight         = state.exerciseAiInsight[ex.id] ?: "",
            isAiLoading       = ex.id in state.exerciseAiLoading,
            isFree            = state.userPlan == com.cosmibit.profitness.data.store.UserPlan.FREE,
            aiCredits         = state.aiCredits,
            onRequestAiInsight = { viewModel.analyzeProgression(ex.id, ex.name) }
        )
    }

    // ── FAZ 7J-9: Challenge detail overlay (banner/upcoming tap target) ──
    detailChallengeId?.let { id ->
        ChallengeDetailOverlay(
            challengeId = id,
            onBack = { detailChallengeId = null },
            onChanged = {
                // Event üzerinde bir şey değişmiş olabilir (movement completion, skip toggle)
                dashboardVm.refresh(force = true)
            }
        )
    }
}

private fun challengeExerciseForHome(
    event: com.cosmibit.profitness.domain.challenges.ChallengeSummary,
    programExercises: List<Exercise>
): Exercise? {
    val ev = event.event ?: return null
    if (ev.mode == com.cosmibit.profitness.domain.challenges.EventMode.MovementList) return null

    val sameExercise = ev.exerciseId?.let { id ->
        programExercises.any { it.exerciseTableId == id || it.id == id }
    } ?: false
    val sameSport = programExercises.any { exercise ->
        val sport = SportType.fromRaw(exercise.sportType).takeUnless { exercise.sportType.isBlank() }
            ?: classifySportType(exercise.category, exercise.name, exercise.target, exercise.reps)
        sport == ev.sportType
    }
    if (sameExercise || sameSport) return null

    val canonicalExerciseId = ev.exerciseId ?: return null
    val trackingMode = when (event.targetType) {
        com.cosmibit.profitness.domain.challenges.ChallengeTargetType.TotalDurationMinutes -> "duration"
        else -> "duration_distance"
    }
    return Exercise(
        id = "challenge-${event.id}",
        name = event.title,
        target = ev.sportType.label,
        sets = 1,
        reps = if (event.targetValue > 0) event.targetValue.toString() else "1",
        image = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800",
        category = "Kardiyo",
        restSeconds = 60,
        exerciseTableId = canonicalExerciseId,
        sportType = ev.sportType.raw,
        trackingMode = trackingMode,
        targetDurationSeconds = if (trackingMode == "duration") (event.targetValue * 60).toInt() else null,
        targetDistanceMeters = if (trackingMode != "duration") event.targetValue.toFloat() else null,
        challengeId = event.id
    )
}

private fun Float.trimmedUi(): String =
    if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

private fun Exercise.inferredHoldDurationSeconds(): Int? {
    val metric = classifyExerciseMetric(category, name, target, reps, sportType, trackingMode)
    if (metric != ExerciseMetric.Duration) return null
    val haystack = listOf(category, name, target)
        .joinToString(" ")
        .lowercase()
        .normalizeWorkoutText()
    val isTimedHold = listOf("plank", "wall sit", "hollow hold", "dead hang")
        .any { it in haystack }
    if (!isTimedHold) return null
    val cleaned = reps.lowercase().normalizeWorkoutText().trim()
    return when {
        cleaned.contains("dk") || cleaned.contains("min") || cleaned.contains("dakika") ->
            cleaned.filter { it.isDigit() || it == ',' || it == '.' }
                .replace(',', '.')
                .toFloatOrNull()
                ?.let { (it * 60f).toInt().coerceAtLeast(1) }
        else ->
            cleaned.filter { it.isDigit() }
                .toIntOrNull()
                ?.coerceAtLeast(1)
    }
}

private fun String.normalizeWorkoutText(): String =
    replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')

@Composable
private fun SkippedProgramNotice() {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.30f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Event,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            strings.skippedProgramTitle,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            strings.skippedProgramBody,
            color = theme.text1,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ── Circular Progress Ring ─────────────────────────────────────────────────
@Composable
fun CircularProgressRing(
    progress: Float,
    size: Dp,
    label: String,
    trackColor: Color = Color.Unspecified,
    ringColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val complete = progress >= 1f
    val resolvedRingColor = when {
        ringColor != Color.Unspecified -> ringColor
        complete -> theme.supportSuccess
        else -> theme.accent2Color   // c2 = ölç
    }
    val resolvedTrackColor = if (trackColor == Color.Unspecified) theme.supportRest.copy(0.10f) else trackColor

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "progress"
    )
    // Memoize sweep brush — recreating it on every animation frame (60fps × 1.2s) is expensive
    val arcColor = resolvedRingColor
    val sweepBrush = remember(arcColor) {
        Brush.sweepGradient(listOf(arcColor.copy(0.65f), arcColor))
    }
    val bigRing = size >= 56.dp

    Box(
        modifier = modifier
            .size(size)
            .performanceInsetSurface(theme, CircleShape)
            .then(if (complete) Modifier.background(resolvedRingColor.copy(0.12f)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val strokeWidth = (if (bigRing) 8.dp else 6.dp).toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val startAngle = -90f

            drawCircle(
                color = resolvedTrackColor,
                radius = radius,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                rotate(startAngle, center) {
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 0f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                }
            }
        }

        val responsive = rememberResponsiveLayoutInfo()
        Text(
            text = label,
            color = if (progress > 0) (if (ringColor == Color.Unspecified) (if (complete) theme.supportSuccess else theme.accent2TextColor) else resolvedRingColor) else theme.text1,
            fontSize = if (!bigRing) 13.sp else if (responsive.isLargeFont) 13.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}
