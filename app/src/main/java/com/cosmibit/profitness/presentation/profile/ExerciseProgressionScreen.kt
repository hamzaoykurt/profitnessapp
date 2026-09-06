package com.cosmibit.profitness.presentation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.BaseViewModel
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.presentation.components.AiCreditInfoRow
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.data.ai.AiAccessException
import com.cosmibit.profitness.data.ai.AiAnalysisPrompts
import com.cosmibit.profitness.data.ai.AiToolType
import com.cosmibit.profitness.data.ai.GeminiRepository
import com.cosmibit.profitness.data.local.dao.ExerciseProgressSummary
import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── State & ViewModel ─────────────────────────────────────────────────────────

data class ExerciseProgressionState(
    val summaries       : List<ExerciseProgressSummary>  = emptyList(),
    val historyMap      : Map<String, List<SetCompletionEntity>> = emptyMap(),
    val aiInsightMap    : Map<String, String>             = emptyMap(),
    val aiLoadingSet    : Set<String>                     = emptySet(),
    val isLoading       : Boolean                         = true,
    val userPlan        : com.cosmibit.profitness.data.store.UserPlan = com.cosmibit.profitness.data.store.UserPlan.FREE,
    val aiCredits       : Int                             = com.cosmibit.profitness.data.store.UserPlanRepository.INITIAL_CREDITS_PLACEHOLDER
)

sealed class ExerciseProgressionEvent {
    data object ShowPaywall : ExerciseProgressionEvent()
}

@HiltViewModel
class ExerciseProgressionViewModel @Inject constructor(
    private val workoutRepository : WorkoutRepository,
    private val geminiRepository  : GeminiRepository,
    private val planRepository    : com.cosmibit.profitness.data.store.UserPlanRepository,
    private val supabase          : SupabaseClient
) : BaseViewModel<ExerciseProgressionState, ExerciseProgressionEvent>(ExerciseProgressionState()) {

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(planRepository.planFlow, planRepository.creditsFlow) { plan, credits -> plan to credits }
                .collect { (plan, credits) -> updateState { it.copy(userPlan = plan, aiCredits = credits) } }
        }
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }
            runCatching { workoutRepository.syncFromRemote(userId) }
            val summaries = workoutRepository.getTrackedExerciseSummaries(userId).getOrElse { emptyList() }
            updateState { it.copy(summaries = summaries, isLoading = false) }

            delay(800L)
            val settledSummaries = workoutRepository.getTrackedExerciseSummaries(userId).getOrElse { summaries }
            if (settledSummaries != uiState.value.summaries) {
                updateState { it.copy(summaries = settledSummaries) }
            }
        }
    }

    fun loadHistory(exerciseId: String) {
        if (uiState.value.historyMap.containsKey(exerciseId)) return
        viewModelScope.launch {
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return@launch
            val history = workoutRepository.getExerciseWeightHistory(userId, exerciseId, weeks = 12)
                .getOrElse { emptyList() }
            updateState { it.copy(historyMap = it.historyMap + (exerciseId to history)) }
        }
    }

    fun analyzeProgression(exerciseId: String, exerciseName: String, targetMuscle: String) {
        if (uiState.value.aiLoadingSet.contains(exerciseId)) return
        updateState { it.copy(aiLoadingSet = it.aiLoadingSet + exerciseId) }

        viewModelScope.launch {
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            val history = uiState.value.historyMap[exerciseId]
                ?: userId?.let {
                    workoutRepository.getExerciseWeightHistory(it, exerciseId, weeks = 12)
                        .getOrElse { emptyList() }
                }
                ?: emptyList()

            val prompt = AiAnalysisPrompts.exerciseProgression(
                exerciseName = exerciseName,
                targetMuscle = targetMuscle,
                history = history
            )
            if (prompt == null) {
                updateState {
                    it.copy(
                        aiInsightMap = it.aiInsightMap + (exerciseId to "Bu egzersiz için henüz analiz yapacak kadar ağırlık, süre veya mesafe verisi yok. En az 1-2 seansı kaydettikten sonra daha net yorum yapabilirim."),
                        aiLoadingSet = it.aiLoadingSet - exerciseId
                    )
                }
                return@launch
            }

            if (!planRepository.consumeCredit()) {
                updateState { it.copy(aiLoadingSet = it.aiLoadingSet - exerciseId) }
                sendEvent(ExerciseProgressionEvent.ShowPaywall)
                return@launch
            }

            val result = geminiRepository.chat(
                emptyList(),
                prompt.userMessage,
                prompt.systemPrompt,
                AiToolType.EXERCISE_PROGRESS_ANALYSIS
            )
            if (result.exceptionOrNull() is AiAccessException) {
                updateState { it.copy(aiLoadingSet = it.aiLoadingSet - exerciseId) }
                sendEvent(ExerciseProgressionEvent.ShowPaywall)
                return@launch
            }
            planRepository.refresh()
            val insight = result.getOrElse { planRepository.refundCredit(); "Analiz yapılamadı." }
            updateState {
                it.copy(
                    aiInsightMap = it.aiInsightMap + (exerciseId to insight),
                    aiLoadingSet = it.aiLoadingSet - exerciseId
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val SHORT_DATE_FMT = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun ExerciseProgressionScreen(
    onBack            : () -> Unit,
    onNavigateToStore : () -> Unit = {},
    viewModel         : ExerciseProgressionViewModel = hiltViewModel()
) {
    val theme   = LocalAppTheme.current
    val accent  = MaterialTheme.colorScheme.primary
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseProgressionEvent.ShowPaywall -> showPaywall = true
            }
        }
    }

    if (showPaywall) {
        com.cosmibit.profitness.presentation.store.PaywallDialog(
            onDismiss   = { showPaywall = false },
            onGoToStore = { showPaywall = false; onNavigateToStore() }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBackButton(onClick = onBack, accent = accent, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        theme.t("PERFORMANS ÖLÇÜTLERİ", "PERFORMANCE METRICS"),
                        color = theme.text0,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        theme.t("Ağırlık, süre ve mesafe takibi", "Weight, duration and distance tracking"),
                        color = theme.text2,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = theme.stroke.copy(0.72f), thickness = 0.5.dp)

            // ── Content ───────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                state.summaries.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        EmptyProgressionState(accent = accent, theme = theme)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "summary_hero") {
                            ProgressionSummaryHero(
                                summaries = state.summaries,
                                accent = accent,
                                theme = theme
                            )
                        }
                        item(key = "credit_info") {
                            AiCreditInfoRow(
                                isFree    = state.userPlan == com.cosmibit.profitness.data.store.UserPlan.FREE,
                                credits   = state.aiCredits,
                                costLabel = theme.t("2 Enerji / egzersiz analizi", "2 Energy / exercise analysis"),
                                theme     = theme
                            )
                        }
                        items(state.summaries, key = { it.exerciseId }) { summary ->
                            ExerciseProgressionCard(
                                summary      = summary,
                                history      = state.historyMap[summary.exerciseId] ?: emptyList(),
                                aiInsight    = state.aiInsightMap[summary.exerciseId] ?: "",
                                isAiLoading  = summary.exerciseId in state.aiLoadingSet,
                                accent       = accent,
                                theme        = theme,
                                isFree       = state.userPlan == com.cosmibit.profitness.data.store.UserPlan.FREE,
                                aiCredits    = state.aiCredits,
                                onExpand     = { viewModel.loadHistory(summary.exerciseId) },
                                onRequestAi  = { viewModel.analyzeProgression(summary.exerciseId, summary.name, summary.targetMuscle) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionSummaryHero(
    summaries: List<ExerciseProgressSummary>,
    accent: Color,
    theme: AppThemeState
) {
    val trackedCount = summaries.size
    val weightCount = summaries.count { it.maxWeight > 0f || it.totalVolume > 0f }
    val timedCount = summaries.count { it.totalDurationSeconds > 0 }
    val distanceCount = summaries.count { it.totalDistanceMeters > 0f }
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(theme, shape, accent, if (theme.isDark) 22.dp else 10.dp)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(accent.copy(0.16f))
                        .border(1.dp, accent.copy(0.28f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = accent, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        theme.t("GELİŞİM PANELİ", "PROGRESSION PANEL"),
                        color = theme.text0,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        theme.t("Kayıtlı hareket performanslarını karşılaştır", "Compare tracked exercise performance"),
                        color = theme.text2,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Text(
                    "$trackedCount",
                    color = accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill(theme.t("Ağırlık", "Weight"), weightCount.toString(), accent, theme, Modifier.weight(1f))
                SummaryPill(theme.t("Süre", "Time"), timedCount.toString(), CardGreen, theme, Modifier.weight(1f))
                SummaryPill(theme.t("Mesafe", "Distance"), distanceCount.toString(), CardCyan, theme, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    color: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(14.dp),
                color,
                if (theme.isDark) 7.dp else 3.dp
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(5.dp))
        Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EmptyProgressionState(
    accent: Color,
    theme: AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(theme, RoundedCornerShape(26.dp), accent)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(accent.copy(0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(30.dp))
        }
        Text(
            theme.t("Henüz performans kaydı yok", "No performance records yet"),
            color = theme.text0,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            theme.t(
                "Antrenman sırasında ağırlık, süre veya mesafe girerek gelişimini takip et",
                "Track progress by entering weight, duration or distance during workouts"
            ),
            color = theme.text2,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ── Exercise Progression Card ─────────────────────────────────────────────────

@Composable
private fun ExerciseProgressionCard(
    summary     : ExerciseProgressSummary,
    history     : List<SetCompletionEntity>,
    aiInsight   : String,
    isAiLoading : Boolean,
    accent      : Color,
    theme       : AppThemeState,
    isFree      : Boolean = true,
    aiCredits   : Int = 0,
    onExpand    : () -> Unit,
    onRequestAi : () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chartData  = remember(history) { buildChartData(history) }
    val hasWeight = summary.maxWeight > 0f || summary.totalVolume > 0f
    val hasDuration = summary.totalDurationSeconds > 0
    val hasDistance = summary.totalDistanceMeters > 0f
    val cardShape = RoundedCornerShape(24.dp)
    val cardAccent = when {
        hasWeight -> accent
        hasDuration -> CardGreen
        hasDistance -> CardCyan
        else -> accent
    }
    val displayName = theme.exerciseDisplayName(summary.name).localizedPrimary(theme)
    val targetName = theme.fitnessTermDisplayName(summary.targetMuscle).localizedPrimary(theme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(
                theme = theme,
                shape = cardShape,
                accent = cardAccent,
                elevation = if (isExpanded) 24.dp else 15.dp
            )
    ) {
        // ── Header row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                    if (isExpanded) onExpand()
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(cardAccent.copy(0.20f), theme.bg2),
                            start = Offset(0f, 0f),
                            end = Offset(120f, 120f)
                        )
                    )
                    .border(1.dp, cardAccent.copy(0.28f), RoundedCornerShape(16.dp))
            ) {
                if (summary.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = summary.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = cardAccent, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    color = theme.text0,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Text(
                    targetName,
                    color = theme.text2,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasWeight) {
                        StatChip(label = theme.t("MAKS", "MAX") + " ${"%.1f".format(summary.maxWeight)} kg", color = cardAccent)
                    }
                    if (hasDistance) {
                        StatChip(label = formatDistance(summary.totalDistanceMeters), color = CardCyan)
                    }
                    if (hasDuration) {
                        StatChip(label = formatDuration(summary.totalDurationSeconds, theme), color = CardGreen)
                    }
                    if (!hasWeight && !hasDistance && !hasDuration) {
                        StatChip(label = "${summary.sessionCount} ${theme.t("seans", "sessions")}", color = theme.text2)
                    }
                }
            }

            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = if (isExpanded) cardAccent else theme.text2,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Expanded: chart + AI ──────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
            exit    = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp)) {
                HorizontalDivider(color = cardAccent.copy(0.22f), thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))

                // ── İstatistik Izgara ─────────────────────────────────────────
                StatsGrid(summary = summary, accent = accent, theme = theme)
                Spacer(Modifier.height(12.dp))

                // ── İlerleme Grafiği ──────────────────────────────────────────
                if (hasWeight && chartData.size >= 2) {
                    ProgressionChartSection(chartData = chartData, accent = accent, theme = theme)
                    Spacer(Modifier.height(12.dp))
                } else if (hasWeight && history.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                    }
                } else if (hasWeight) {
                    Text(
                        theme.t(
                            "Ağırlık grafiği için en az 2 farklı günden veri gerekli",
                            "Weight chart needs data from at least 2 different days"
                        ),
                        color = theme.text2, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ── Son Seans Set Dökümü ──────────────────────────────────────
                if (history.isNotEmpty()) {
                    LastSessionBreakdown(history = history, accent = accent, theme = theme)
                    Spacer(Modifier.height(12.dp))
                }

                // AI Card
                AiInsightCard(
                    insight     = aiInsight,
                    isLoading   = isAiLoading,
                    accent      = accent,
                    theme       = theme,
                    isFree      = isFree,
                    credits     = aiCredits,
                    onRefresh   = onRequestAi
                )
            }
        }
    }
}

// ── Stats Grid ────────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    summary : ExerciseProgressSummary,
    accent  : Color,
    theme   : AppThemeState
) {
    val stats = buildList {
        if (summary.totalVolume > 0f) add(Triple(theme.t("TOPLAM HACİM", "TOTAL VOLUME"), "${"%.0f".format(summary.totalVolume)} kg", accent))
        if (summary.totalDistanceMeters > 0f) add(Triple(theme.t("TOPLAM MESAFE", "TOTAL DISTANCE"), formatDistance(summary.totalDistanceMeters), CardCyan))
        if (summary.totalDurationSeconds > 0) add(Triple(theme.t("TOPLAM SÜRE", "TOTAL TIME"), formatDuration(summary.totalDurationSeconds, theme), CardGreen))
        if (summary.totalReps > 0) add(Triple(theme.t("TOPLAM TEKRAR", "TOTAL REPS"), "${summary.totalReps}", theme.text1))
        if (summary.totalSets > 0) add(Triple(theme.t("KAYIT", "SETS"), "${summary.totalSets}", theme.text1))
        add(Triple(theme.t("SEANS", "SESSIONS"), "${summary.sessionCount}", theme.text1))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { stat ->
                    StatsTile(stat, Modifier.weight(1f), theme)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (!summary.lastDate.isNullOrBlank()) {
            Text(
                text = theme.t("Son antrenman", "Last workout") + ": ${runCatching { LocalDate.parse(summary.lastDate).format(SHORT_DATE_FMT) }.getOrElse { summary.lastDate!! }}",
                color = theme.text2, fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StatsTile(
    data: Triple<String, String, Color>,
    modifier: Modifier,
    theme: AppThemeState
) {
    val (label, value, valueColor) = data
    Column(
        modifier = modifier
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(10.dp),
                valueColor,
                if (theme.isDark) 7.dp else 3.dp
            )
            .padding(10.dp)
    ) {
        Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ── Last Session Breakdown — set-by-set ağırlık & tekrar ─────────────────────

@Composable
private fun LastSessionBreakdown(
    history : List<SetCompletionEntity>,
    accent  : Color,
    theme   : AppThemeState
) {
    // Son antrenmanın setlerini al
    val lastDate = history.mapNotNull { it.date }.maxOrNull() ?: return
    val lastSets = history
        .filter { it.date == lastDate }
        .sortedBy { it.setIndex }
    if (lastSets.isEmpty()) return

    val weightSum = lastSets.mapNotNull { it.weightKg }.sum()
    val weightCnt = lastSets.count { it.weightKg != null }
    val avgWeight = if (weightCnt > 0) weightSum / weightCnt else 0f
    val durationSeconds = lastSets.sumOf { it.durationSeconds ?: 0 }
    val distanceMeters = lastSets.mapNotNull { it.distanceMeters }.sum()
    val sessionSummary = when {
        weightCnt > 0 -> theme.t("ORT", "AVG") + " ${"%.1f".format(avgWeight)} kg"
        distanceMeters > 0f -> formatDistance(distanceMeters)
        durationSeconds > 0 -> formatDuration(durationSeconds, theme)
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(12.dp),
                accent,
                if (theme.isDark) 8.dp else 4.dp
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (weightCnt > 0) Icons.Rounded.FitnessCenter else Icons.Rounded.Timer,
                null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(theme.t("SON ANTRENMAN", "LAST WORKOUT"), color = theme.text1, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            if (sessionSummary.isNotBlank()) {
                Text(
                    text = sessionSummary,
                    color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        lastSets.forEach { set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent.copy(0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${set.setIndex + 1}",
                        color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = set.weightKg?.let { "${"%.1f".format(it)} kg" }
                        ?: set.distanceMeters?.takeIf { it > 0f }?.let { formatDistance(it) }
                        ?: set.durationSeconds?.takeIf { it > 0 }?.let { formatDuration(it, theme) }
                        ?: "-",
                    color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when {
                        set.weightKg != null -> set.repsActual?.let { "× $it ${theme.t("tekrar", "reps")}" } ?: theme.t("taslak", "draft")
                        set.distanceMeters != null && set.durationSeconds != null -> set.durationSeconds?.let { formatDuration(it, theme) } ?: theme.t("süre", "time")
                        set.durationSeconds != null -> theme.t("süre", "time")
                        set.distanceMeters != null -> theme.t("mesafe", "distance")
                        else -> theme.t("taslak", "draft")
                    },
                    color = theme.text2, fontSize = 11.sp, fontWeight = FontWeight.Medium
                )
                if (set.weightKg != null && set.repsActual != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "= ${"%.0f".format(set.weightKg!! * set.repsActual!!)}",
                        color = accent.copy(0.75f), fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Chart Section ─────────────────────────────────────────────────────────────

private data class ProgressPoint(val date: LocalDate, val maxKg: Float)

private fun buildChartData(history: List<SetCompletionEntity>): List<ProgressPoint> =
    history
        .filter { it.weightKg != null }
        .groupBy { it.date }
        .map { (date, sets) ->
            ProgressPoint(
                date  = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() },
                maxKg = sets.maxOf { it.weightKg!! }
            )
        }
        .sortedBy { it.date }

@Composable
private fun ProgressionChartSection(
    chartData : List<ProgressPoint>,
    accent    : Color,
    theme     : AppThemeState
) {
    val firstKg = chartData.first().maxKg
    val lastKg  = chartData.last().maxKg
    val delta   = lastKg - firstKg
    val isGain  = delta >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(12.dp),
                accent,
                if (theme.isDark) 8.dp else 4.dp
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ShowChart, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(theme.t("İLERLEME", "PROGRESS"), color = theme.text1, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            val chipColor = if (isGain) Color(0xFF22C55E) else Color(0xFFEF4444)
            val sign      = if (isGain) "+" else ""
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(chipColor.copy(0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isGain) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    null, tint = chipColor, modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text("$sign${"%.1f".format(delta)} kg", color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        val minKg     = (chartData.minOf { it.maxKg } - 2f).coerceAtLeast(0f)
        val maxKg     = chartData.maxOf { it.maxKg } + 2f
        val range     = (maxKg - minKg).coerceAtLeast(1f)
        val accentColor = accent

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            val w = size.width; val h = size.height
            val padH = 12.dp.toPx(); val padV = 6.dp.toPx()
            val chartW = w - padH * 2f; val chartH = h - padV * 2f
            val n = chartData.lastIndex.coerceAtLeast(1)
            fun xOf(i: Int) = padH + (i.toFloat() / n) * chartW
            fun yOf(kg: Float) = padV + chartH * (1f - (kg - minKg) / range)
            val grid = accentColor.copy(0.06f)
            repeat(3) { i ->
                val gy = padV + chartH * (i.toFloat() / 2f)
                drawLine(grid, Offset(padH, gy), Offset(w - padH, gy), 1f)
            }
            val linePath = Path(); val fillPath = Path()
            chartData.forEachIndexed { i, pt ->
                val x = xOf(i); val y = yOf(pt.maxKg)
                if (i == 0) { linePath.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                else {
                    val px = xOf(i - 1); val py = yOf(chartData[i - 1].maxKg); val cpX = (px + x) / 2f
                    linePath.cubicTo(cpX, py, cpX, y, x, y)
                    fillPath.cubicTo(cpX, py, cpX, y, x, y)
                }
            }
            fillPath.lineTo(xOf(chartData.lastIndex), h); fillPath.close()
            drawPath(fillPath, Brush.verticalGradient(listOf(accentColor.copy(0.18f), Color.Transparent), padV, h))
            drawPath(linePath, accentColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            listOf(0, chartData.lastIndex).forEach { i ->
                val x = xOf(i); val y = yOf(chartData[i].maxKg)
                drawCircle(accentColor.copy(0.25f), 5.dp.toPx(), Offset(x, y))
                drawCircle(accentColor, 2.5.dp.toPx(), Offset(x, y))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(chartData.first().date.format(SHORT_DATE_FMT), color = theme.text2, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            Text(chartData.last().date.format(SHORT_DATE_FMT), color = theme.text2, fontSize = 8.sp)
        }
    }
}

// ── AI Insight Card ───────────────────────────────────────────────────────────

@Composable
private fun AiInsightCard(
    insight   : String,
    isLoading : Boolean,
    accent    : Color,
    theme     : AppThemeState,
    isFree    : Boolean = true,
    credits   : Int = 0,
    onRefresh : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .profilePremiumSurface(
                theme,
                RoundedCornerShape(12.dp),
                accent,
                if (theme.isDark) 9.dp else 4.dp
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(0.15f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(theme.t("AI KOÇ", "AI COACH"), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            if (isFree && !isLoading) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(theme.t("2 Enerji", "2 Energy"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(theme.t("$credits kaldı", "$credits left"), color = theme.text2, fontSize = 9.sp)
                }
                Spacer(Modifier.width(6.dp))
            }
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .profilePremiumAction(
                            theme = theme,
                            accent = accent,
                            onClick = onRefresh,
                            shape = CircleShape,
                            enabled = !isLoading
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = theme.text2, modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            isLoading  -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Analiz yapılıyor...", "Analyzing..."), color = theme.text2, fontSize = 11.sp)
            }
            insight.isBlank() -> Text(
                theme.t("AI koçtan gelişim analizi al ->", "Get progression analysis from AI coach ->"),
                color = theme.text2, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = !isLoading, onClick = onRefresh).padding(8.dp)
            )
            else -> Text(localizedProgressionMessage(insight, theme), color = theme.text1, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

// ── Stat Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, color: Color) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun String.localizedPrimary(theme: AppThemeState): String {
    if (theme.language == AppLanguage.ENGLISH) return this
    val parenthetical = Regex("""^(.+)\((.+)\)$""").matchEntire(this.trim())
    return parenthetical?.groupValues?.getOrNull(2)?.trim() ?: this
}

private fun localizedProgressionMessage(value: String, theme: AppThemeState): String =
    if (theme.language != AppLanguage.ENGLISH) value else when (value) {
        "Analiz yapılamadı." -> "Analysis could not be completed."
        "Bu egzersiz için henüz analiz yapacak kadar ağırlık, süre veya mesafe verisi yok. En az 1-2 seansı kaydettikten sonra daha net yorum yapabilirim." ->
            "There is not enough weight, duration or distance data to analyze this exercise yet. After 1-2 logged sessions, I can give a clearer read."
        else -> value
    }

private fun formatDuration(totalSeconds: Int, theme: AppThemeState): String {
    val minutes = (totalSeconds / 60).coerceAtLeast(0)
    val hours = minutes / 60
    val mins = minutes % 60
    val hourUnit = theme.t("sa", "h")
    val minuteUnit = theme.t("dk", "m")
    return when {
        totalSeconds <= 0 -> "0 $minuteUnit"
        hours > 0 && mins > 0 -> "$hours$hourUnit $mins$minuteUnit"
        hours > 0 -> "$hours$hourUnit"
        minutes > 0 -> "$minutes $minuteUnit"
        else -> "${totalSeconds}s"
    }
}

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) "${"%.1f".format(meters / 1000f)} km" else "${meters.toInt()} m"
