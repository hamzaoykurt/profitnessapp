package com.avonix.profitness.presentation.profile

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.local.dao.ExerciseProgressSummary
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
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
    val isLoading       : Boolean                         = true
)

sealed class ExerciseProgressionEvent {
    data object ShowPaywall : ExerciseProgressionEvent()
}

@HiltViewModel
class ExerciseProgressionViewModel @Inject constructor(
    private val workoutRepository : WorkoutRepository,
    private val geminiRepository  : GeminiRepository,
    private val planRepository    : com.avonix.profitness.data.store.UserPlanRepository,
    private val supabase          : SupabaseClient
) : BaseViewModel<ExerciseProgressionState, ExerciseProgressionEvent>(ExerciseProgressionState()) {

    init { load() }

    fun load() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }
            val summaries = workoutRepository.getTrackedExerciseSummaries(userId).getOrElse { emptyList() }
            updateState { it.copy(summaries = summaries, isLoading = false) }
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

    fun analyzeProgression(exerciseId: String, exerciseName: String) {
        if (uiState.value.aiLoadingSet.contains(exerciseId)) return
        viewModelScope.launch {
            if (!planRepository.consumeCredit()) {
                sendEvent(ExerciseProgressionEvent.ShowPaywall)
                return@launch
            }
            updateState { it.copy(aiLoadingSet = it.aiLoadingSet + exerciseId) }
            val history = uiState.value.historyMap[exerciseId] ?: emptyList()
            val summary = buildString {
                val grouped = history.groupBy { it.date }
                    .entries.sortedBy { it.key }.takeLast(8)
                grouped.forEach { (date, sets) ->
                    val maxKg = sets.mapNotNull { it.weightKg }.maxOrNull() ?: return@forEach
                    appendLine("$date: max ${maxKg}kg")
                }
            }
            if (summary.isBlank()) {
                updateState { it.copy(aiLoadingSet = it.aiLoadingSet - exerciseId) }
                return@launch
            }
            val systemPrompt = "Sen bir fitness koçusun. Kısa, motive edici ve net Türkçe tavsiye ver."
            val userMessage = "$exerciseName egzersizi için ağırlık geçmişim:\n$summary\nGelişimimi değerlendir, öneri ver. 3-4 cümle yeterli."
            val result = geminiRepository.chat(emptyList(), userMessage, systemPrompt)
            val insight = result.getOrElse { "Analiz yapılamadı." }
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
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseProgressionEvent.ShowPaywall -> showPaywall = true
            }
        }
    }

    if (showPaywall) {
        com.avonix.profitness.presentation.store.PaywallDialog(
            onDismiss   = { showPaywall = false },
            onGoToStore = { showPaywall = false; onNavigateToStore() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        // ── Top Bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "ANTRENMAN GELİŞİMİ",
                    color = accent, fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                )
                Text(
                    "Egzersiz bazlı ağırlık takibi",
                    color = theme.text2, fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)

        // ── Content ───────────────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            state.summaries.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("💪", fontSize = 48.sp)
                        Text(
                            "Henüz ağırlık kaydı yok",
                            color = theme.text1, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Antrenman sırasında set başına\nağırlık girerek gelişimini takip et",
                            color = theme.text2, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.summaries, key = { it.exerciseId }) { summary ->
                        ExerciseProgressionCard(
                            summary      = summary,
                            history      = state.historyMap[summary.exerciseId] ?: emptyList(),
                            aiInsight    = state.aiInsightMap[summary.exerciseId] ?: "",
                            isAiLoading  = summary.exerciseId in state.aiLoadingSet,
                            accent       = accent,
                            theme        = theme,
                            onExpand     = { viewModel.loadHistory(summary.exerciseId) },
                            onRequestAi  = { viewModel.analyzeProgression(summary.exerciseId, summary.name) }
                        )
                    }
                }
            }
        }
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
    onExpand    : () -> Unit,
    onRequestAi : () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chartData  = remember(history) { buildChartData(history) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.bg2)
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

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.name.uppercase(),
                    color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    summary.targetMuscle,
                    color = theme.text2, fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(label = "${"%.1f".format(summary.maxWeight)} kg", color = accent)
                    StatChip(label = "${summary.sessionCount} seans", color = theme.text2)
                }
            }

            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = theme.text2,
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
                HorizontalDivider(color = theme.stroke, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))

                if (chartData.size >= 2) {
                    ProgressionChartSection(chartData = chartData, accent = accent, theme = theme)
                    Spacer(Modifier.height(12.dp))
                } else if (history.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                    }
                } else {
                    Text(
                        "Grafik için en az 2 farklı günden veri gerekli",
                        color = theme.text2, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // AI Card
                AiInsightCard(
                    insight     = aiInsight,
                    isLoading   = isAiLoading,
                    accent      = accent,
                    theme       = theme,
                    onRefresh   = onRequestAi
                )
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
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ShowChart, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("İLERLEME", color = theme.text1, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
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
                    if (isGain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
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
    onRefresh : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
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
                Text("AI KOÇ", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(theme.bg3)
                        .clickable(onClick = onRefresh),
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
                Text("Analiz yapılıyor...", color = theme.text2, fontSize = 11.sp)
            }
            insight.isBlank() -> Text(
                "AI koçtan gelişim analizi al →",
                color = theme.text2, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRefresh).padding(8.dp)
            )
            else -> Text(insight, color = theme.text1, fontSize = 11.sp, lineHeight = 17.sp)
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
