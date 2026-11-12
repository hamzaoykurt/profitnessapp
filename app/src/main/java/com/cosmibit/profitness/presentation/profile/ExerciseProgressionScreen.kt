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
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaDetailHeader
import com.cosmibit.profitness.presentation.components.drawGlowDot
import com.cosmibit.profitness.presentation.components.drawGlowLine
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
    val accent  = theme.effectiveAccentColor
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        Column(modifier = Modifier.fillMaxSize()) {
            SinemaDetailHeader(
                title    = theme.t("Hareket performansı", "Exercise performance"),
                subtitle = theme.t("Ağırlık, süre ve mesafe takibi", "Weight, duration and distance tracking"),
                onBack   = onBack
            )

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                state.summaries.isEmpty() -> {
                    EmptyProgressionState(theme = theme, onBack = onBack)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = SinemaNavHeight + 24.dp + navBarBottom),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "summary_hero") {
                            ProgressionSummaryHero(summaries = state.summaries, theme = theme)
                        }
                        items(state.summaries, key = { it.exerciseId }) { summary ->
                            ExerciseProgressionCard(
                                summary      = summary,
                                history      = state.historyMap[summary.exerciseId] ?: emptyList(),
                                aiInsight    = state.aiInsightMap[summary.exerciseId] ?: "",
                                isAiLoading  = summary.exerciseId in state.aiLoadingSet,
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
    theme: AppThemeState
) {
    val trackedCount = summaries.size
    val weightCount = summaries.count { it.maxWeight > 0f || it.totalVolume > 0f }
    val timedCount = summaries.count { it.totalDurationSeconds > 0 }
    val distanceCount = summaries.count { it.totalDistanceMeters > 0f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SinemaBigStat(
            value = "$trackedCount",
            label = theme.t("Takip edilen hareket", "Tracked exercises"),
            size = 72,
            color = theme.effectiveAccentColor
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SinemaBigStat("$weightCount", theme.t("Ağırlık", "Weight"), Modifier.weight(1f), size = 28)
            SinemaBigStat("$timedCount", theme.t("Süre", "Time"), Modifier.weight(1f), size = 28)
            SinemaBigStat("$distanceCount", theme.t("Mesafe", "Distance"), Modifier.weight(1f), size = 28)
        }
    }
}

@Composable
private fun EmptyProgressionState(
    theme: AppThemeState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(72.dp).sinemaCard(theme, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = Sinema.text(theme), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            theme.t("Henüz performans kaydı yok", "No performance records yet"),
            color = Sinema.text(theme), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            theme.t(
                "Antrenman sırasında ağırlık, süre veya mesafe girerek gelişimini takip et.",
                "Track progress by entering weight, duration or distance during workouts."
            ),
            color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        SinemaGoButton(
            label = theme.t("Profile dön", "Back to profile"), icon = Icons.Rounded.FitnessCenter,
            onClick = onBack, modifier = Modifier.widthIn(max = 280.dp)
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
    theme       : AppThemeState,
    isFree      : Boolean = true,
    aiCredits   : Int = 0,
    onExpand    : () -> Unit,
    onRequestAi : () -> Unit
) {
    val accent = theme.effectiveAccentColor
    var isExpanded by remember { mutableStateOf(false) }
    val chartData  = remember(history) { buildChartData(history) }
    val hasWeight = summary.maxWeight > 0f || summary.totalVolume > 0f
    val hasDuration = summary.totalDurationSeconds > 0
    val hasDistance = summary.totalDistanceMeters > 0f
    val displayName = theme.exerciseDisplayName(summary.name).localizedPrimary(theme)
    val targetName = theme.fitnessTermDisplayName(summary.targetMuscle).localizedPrimary(theme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(24.dp))
    ) {
        // ── Header row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                    if (isExpanded) onExpand()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Sinema.line(theme))
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
                        Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    color = Sinema.text(theme),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Text(
                    targetName,
                    color = Sinema.textSecondary(theme),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasWeight) {
                        StatChip(label = theme.t("MAKS", "MAX") + " ${"%.1f".format(summary.maxWeight)} kg")
                    }
                    if (hasDistance) {
                        StatChip(label = formatDistance(summary.totalDistanceMeters))
                    }
                    if (hasDuration) {
                        StatChip(label = formatDuration(summary.totalDurationSeconds, theme))
                    }
                    if (!hasWeight && !hasDistance && !hasDuration) {
                        StatChip(label = "${summary.sessionCount} ${theme.t("seans", "sessions")}")
                    }
                }
            }

            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = if (isExpanded) accent else Sinema.textSecondary(theme),
                modifier = Modifier.size(24.dp)
            )
        }

        // ── Expanded: istatistik + grafik + AI ────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
            exit    = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                StatsGrid(summary = summary, theme = theme)
                Spacer(Modifier.height(20.dp))

                if (hasWeight && chartData.size >= 2) {
                    ProgressionChartSection(chartData = chartData, theme = theme)
                    Spacer(Modifier.height(20.dp))
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
                        color = Sinema.textSecondary(theme), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (history.isNotEmpty()) {
                    LastSessionBreakdown(history = history, theme = theme)
                    Spacer(Modifier.height(20.dp))
                }

                AiInsightCard(
                    insight     = aiInsight,
                    isLoading   = isAiLoading,
                    theme       = theme,
                    isFree      = isFree,
                    credits     = aiCredits,
                    onRefresh   = onRequestAi
                )
            }
        }
    }
}

// ── Stats Grid — dev rakamlar ─────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    summary : ExerciseProgressSummary,
    theme   : AppThemeState
) {
    val stats = buildList {
        if (summary.totalVolume > 0f) add(theme.t("TOPLAM HACİM", "TOTAL VOLUME") to "${"%.0f".format(summary.totalVolume)} kg")
        if (summary.totalDistanceMeters > 0f) add(theme.t("TOPLAM MESAFE", "TOTAL DISTANCE") to formatDistance(summary.totalDistanceMeters))
        if (summary.totalDurationSeconds > 0) add(theme.t("TOPLAM SÜRE", "TOTAL TIME") to formatDuration(summary.totalDurationSeconds, theme))
        if (summary.totalReps > 0) add(theme.t("TOPLAM TEKRAR", "TOTAL REPS") to "${summary.totalReps}")
        if (summary.totalSets > 0) add(theme.t("KAYIT", "SETS") to "${summary.totalSets}")
        add(theme.t("SEANS", "SESSIONS") to "${summary.sessionCount}")
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    SinemaBigStat(value, label, Modifier.weight(1f), size = 30)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (!summary.lastDate.isNullOrBlank()) {
            Text(
                text = theme.t("Son antrenman", "Last workout") + ": ${runCatching { LocalDate.parse(summary.lastDate).format(SHORT_DATE_FMT) }.getOrElse { summary.lastDate!! }}",
                color = Sinema.textSecondary(theme), fontSize = 12.sp
            )
        }
    }
}

// ── Last Session Breakdown — set-by-set ağırlık & tekrar ─────────────────────

@Composable
private fun LastSessionBreakdown(
    history : List<SetCompletionEntity>,
    theme   : AppThemeState
) {
    val accent = theme.effectiveAccentColor
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                theme.t("SON ANTRENMAN", "LAST WORKOUT"),
                color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f)
            )
            if (sessionSummary.isNotBlank()) {
                Text(sessionSummary, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        lastSets.forEach { set ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Sinema.line(theme)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${set.setIndex + 1}", color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = set.weightKg?.let { "${"%.1f".format(it)} kg" }
                        ?: set.distanceMeters?.takeIf { it > 0f }?.let { formatDistance(it) }
                        ?: set.durationSeconds?.takeIf { it > 0 }?.let { formatDuration(it, theme) }
                        ?: "-",
                    color = Sinema.text(theme), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
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
                    color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                if (set.weightKg != null && set.repsActual != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "= ${"%.0f".format(set.weightKg!! * set.repsActual!!)}",
                        color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
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
    theme     : AppThemeState
) {
    val accent = theme.effectiveAccentColor
    val gridColor = Sinema.line(theme)
    val firstKg = chartData.first().maxKg
    val lastKg  = chartData.last().maxKg
    val delta   = lastKg - firstKg
    val isGain  = delta >= 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                theme.t("İLERLEME", "PROGRESS"),
                color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f)
            )
            val sign = if (isGain) "+" else ""
            SinemaCapsule(height = 30.dp, light = !theme.isDark, contentPadding = PaddingValues(horizontal = 12.dp)) { fg ->
                Icon(
                    if (isGain) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    null, tint = fg, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("$sign${"%.1f".format(delta)} kg", color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        val minKg     = (chartData.minOf { it.maxKg } - 2f).coerceAtLeast(0f)
        val maxKg     = chartData.maxOf { it.maxKg } + 2f
        val range     = (maxKg - minKg).coerceAtLeast(1f)

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(130.dp)
        ) {
            val w = size.width; val h = size.height
            val padH = 12.dp.toPx(); val padV = 10.dp.toPx()
            val chartW = w - padH * 2f; val chartH = h - padV * 2f
            val n = chartData.lastIndex.coerceAtLeast(1)
            fun xOf(i: Int) = padH + (i.toFloat() / n) * chartW
            fun yOf(kg: Float) = padV + chartH * (1f - (kg - minKg) / range)
            repeat(3) { i ->
                val gy = padV + chartH * (i.toFloat() / 2f)
                drawLine(gridColor, Offset(padH, gy), Offset(w - padH, gy), 1f)
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
            drawPath(fillPath, Brush.verticalGradient(listOf(accent.copy(0.16f), Color.Transparent), padV, h))
            drawGlowLine(linePath, accent, 2.5.dp.toPx())
            listOf(0, chartData.lastIndex).forEach { i ->
                drawGlowDot(Offset(xOf(i), yOf(chartData[i].maxKg)), accent, 3.dp.toPx())
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(chartData.first().date.format(SHORT_DATE_FMT), color = Sinema.textSecondary(theme), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(chartData.last().date.format(SHORT_DATE_FMT), color = Sinema.textSecondary(theme), fontSize = 11.sp)
        }
    }
}

// ── AI Insight Card ───────────────────────────────────────────────────────────

@Composable
private fun AiInsightCard(
    insight   : String,
    isLoading : Boolean,
    theme     : AppThemeState,
    isFree    : Boolean = true,
    credits   : Int = 0,
    onRefresh : () -> Unit
) {
    val accent = theme.effectiveAccentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Sinema.line(theme))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.t("AI KOÇ", "AI COACH"), color = Sinema.text(theme), style = Sinema.Label)
                if (isFree && !isLoading) {
                    Text(
                        theme.t("2 Enerji · $credits kaldı", "2 Energy · $credits left"),
                        color = Sinema.textSecondary(theme), fontSize = 12.sp
                    )
                }
            }
            if (!isLoading) {
                SinemaCapsuleIconButton(
                    icon = Icons.Rounded.Refresh, description = theme.t("Analiz et", "Analyze"),
                    onClick = onRefresh, selected = true, size = 40.dp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            isLoading  -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Analiz yapılıyor...", "Analyzing..."), color = Sinema.textSecondary(theme), fontSize = 13.sp)
            }
            insight.isBlank() -> Text(
                theme.t("AI koçtan gelişim analizi al", "Get progression analysis from AI coach"),
                color = Sinema.textSecondary(theme), fontSize = 13.sp, lineHeight = 18.sp
            )
            else -> Text(localizedProgressionMessage(insight, theme), color = Sinema.text(theme), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

// ── Stat Chip ─────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Sinema.line(theme))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = Sinema.text(theme), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
