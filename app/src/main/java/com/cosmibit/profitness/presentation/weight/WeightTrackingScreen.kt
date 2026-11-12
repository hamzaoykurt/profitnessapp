@file:OptIn(ExperimentalMaterial3Api::class)

package com.cosmibit.profitness.presentation.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaDetailHeader
import com.cosmibit.profitness.presentation.components.SinemaDialog
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import com.cosmibit.profitness.presentation.components.drawGlowDot
import com.cosmibit.profitness.presentation.components.drawGlowLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHORT_DATE_FMT = DateTimeFormatter.ofPattern("d MMM")
private val FULL_DATE_FMT  = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")

@Composable
fun WeightTrackingScreen(
    onBack            : () -> Unit,
    onNavigateToStore : () -> Unit = {},
    viewModel         : WeightTrackingViewModel = hiltViewModel()
) {
    val theme             = LocalAppTheme.current
    val accent            = theme.effectiveAccentColor
    val state             by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPaywall       by remember { mutableStateOf(false) }
    val navBarBottom      = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isFree            = state.userPlan == com.cosmibit.profitness.data.store.UserPlan.FREE

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WeightTrackingEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(theme.ui(event.message))
                is WeightTrackingEvent.ShowPaywall ->
                    showPaywall = true
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
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accent)
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = SinemaNavHeight + 24.dp + navBarBottom)
            ) {
                item {
                    SinemaDetailHeader(
                        title    = theme.t("Vücut Kilosu", "Body Weight"),
                        subtitle = null,
                        onBack   = onBack,
                        trailing = {
                            SinemaCapsule(
                                selected = true, onClick = { viewModel.openAddSheet() }, height = 40.dp,
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) { fg ->
                                Icon(Icons.Rounded.Add, null, tint = fg, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(theme.t("Kilo ekle", "Add weight"), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            }
                        }
                    )
                }
                item { WeightHero(theme = theme, latest = state.latestWeightKg, delta = state.totalDeltaKg, entries = state.entries.size) }
                item { WeeklySummaryRow(summary = state.weeklySummary, theme = theme) }
                if (state.chartPoints.size >= 2) {
                    item { WeightLineChart(points = state.chartPoints, theme = theme) }
                }
                item {
                    AiInsightCard(
                        insight   = state.aiInsight,
                        isLoading = state.isAiLoading,
                        theme     = theme,
                        isFree    = isFree,
                        credits   = state.aiCredits,
                        onRefresh = { viewModel.generateAiInsight() }
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    SinemaSectionLabel(
                        theme.t("Geçmiş kayıtlar", "History"),
                        trailing = theme.t("${state.entries.size} kayıt", "${state.entries.size} entries")
                    )
                }
                if (state.entries.isEmpty()) {
                    item { EmptyWeightState(theme = theme, onAdd = { viewModel.openAddSheet() }) }
                }
                items(state.entries, key = { it.id }) { entry ->
                    WeightEntryRow(
                        entry    = entry,
                        theme    = theme,
                        onEdit   = { viewModel.openEditSheet(entry) },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SinemaNavHeight + 16.dp + navBarBottom)
        )
    }

    if (state.showSheet) {
        AddEditWeightSheet(
            state          = state,
            theme          = theme,
            onDismiss      = { viewModel.closeSheet() },
            onWeightChange = viewModel::onWeightInputChange,
            onNoteChange   = viewModel::onNoteInputChange,
            onSave         = { viewModel.saveEntry() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HERO — dev güncel kilo
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightHero(
    theme  : AppThemeState,
    latest : Double?,
    delta  : Double?,
    entries: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        SinemaBigStat(
            value = latest?.let { "%.1f".format(it) } ?: "—",
            unit  = "kg",
            label = theme.t("Güncel kilo", "Current weight"),
            size  = 88,
            modifier = Modifier.weight(1f)
        )
        if (delta != null && entries >= 2) {
            val isGain = delta >= 0
            val sign   = if (isGain) "+" else ""
            SinemaCapsule(
                height = 36.dp, light = !theme.isDark, contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) { fg ->
                Icon(if (isGain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("$sign${"%.1f".format(delta)} kg", color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HAFTALIK ÖZET
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklySummaryRow(summary: WeeklySummary, theme: AppThemeState) {
    val sign = if ((summary.deltaKg ?: 0.0) >= 0) "+" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SinemaBigStat(
            value = summary.thisWeekAvg?.let { "%.1f".format(it) } ?: "—",
            label = theme.t("Bu hafta", "This week"), size = 26, modifier = Modifier.weight(1f)
        )
        SinemaBigStat(
            value = summary.lastWeekAvg?.let { "%.1f".format(it) } ?: "—",
            label = theme.t("Geçen hafta", "Last week"), size = 26, modifier = Modifier.weight(1f)
        )
        SinemaBigStat(
            value = summary.deltaKg?.let { "$sign${"%.1f".format(it)}" } ?: "—",
            label = theme.t("Fark", "Change"), size = 26, modifier = Modifier.weight(1f),
            color = if (summary.deltaKg != null) theme.effectiveAccentColor else Sinema.text(theme)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LINE CHART — vurgu çizgi + glow
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightLineChart(points: List<WeightPoint>, theme: AppThemeState) {
    val accent = theme.effectiveAccentColor
    val gridColor = Sinema.line(theme)
    val minKg = (points.minOf { it.weightKg } - 1.0).coerceAtLeast(0.0)
    val maxKg = points.maxOf { it.weightKg } + 1.0
    val range = (maxKg - minKg).coerceAtLeast(0.5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                theme.t("Son 90 gün", "Last 90 days").sinemaUpper(theme),
                color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f)
            )
            Text(theme.t("${points.size} ölçüm", "${points.size} entries"), color = Sinema.textSecondary(theme), fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            val w      = size.width
            val h      = size.height
            val padH   = 14.dp.toPx()
            val padV   = 14.dp.toPx()
            val chartW = w - padH * 2f
            val chartH = h - padV * 2f

            val n = points.lastIndex.coerceAtLeast(1)

            fun xOf(i: Int): Float = padH + (i.toFloat() / n.toFloat()) * chartW
            fun yOf(kg: Double): Float = padV + chartH * (1f - ((kg - minKg) / range).toFloat())

            repeat(3) { i ->
                val gy = padV + chartH * (i.toFloat() / 2f)
                drawLine(color = gridColor, start = Offset(padH, gy), end = Offset(w - padH, gy), strokeWidth = 1f)
            }

            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { i, pt ->
                val x = xOf(i)
                val y = yOf(pt.weightKg)
                if (i == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    val px   = xOf(i - 1)
                    val py   = yOf(points[i - 1].weightKg)
                    val cpX  = (px + x) / 2f
                    linePath.cubicTo(cpX, py, cpX, y, x, y)
                    fillPath.cubicTo(cpX, py, cpX, y, x, y)
                }
            }
            fillPath.lineTo(xOf(points.lastIndex), h)
            fillPath.close()

            drawPath(
                path  = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(accent.copy(0.16f), androidx.compose.ui.graphics.Color.Transparent),
                    startY = padV,
                    endY   = h
                )
            )

            drawGlowLine(linePath, accent, 3.dp.toPx())

            val minIdx = points.indices.minByOrNull { points[it].weightKg } ?: 0
            val maxIdx = points.indices.maxByOrNull { points[it].weightKg } ?: points.lastIndex
            setOf(0, points.lastIndex, minIdx, maxIdx).forEach { i ->
                drawGlowDot(Offset(xOf(i), yOf(points[i].weightKg)), accent, 3.5.dp.toPx())
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(points.firstOrNull()?.date?.format(SHORT_DATE_FMT) ?: "", color = Sinema.textSecondary(theme), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(points.lastOrNull()?.date?.format(SHORT_DATE_FMT) ?: "", color = Sinema.textSecondary(theme), fontSize = 11.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  AI INSIGHT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AiInsightCard(
    insight: String,
    isLoading: Boolean,
    theme: AppThemeState,
    isFree: Boolean,
    credits: Int,
    onRefresh: () -> Unit
) {
    val accent = theme.effectiveAccentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.t("AI ANALİZ", "AI ANALYSIS"), color = Sinema.text(theme), style = Sinema.Label)
                if (isFree && !isLoading) {
                    Text(
                        if (credits <= 0) theme.t("Enerjin bitti · Enerji yükle veya plana yükselt", "Out of energy · Get more energy or upgrade")
                        else theme.t("2 Enerji · $credits kaldı", "2 Energy · $credits left"),
                        color = if (credits <= 0) Sinema.text(theme) else Sinema.textSecondary(theme),
                        fontSize = 12.sp, fontWeight = if (credits <= 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            if (!isLoading) {
                if (isFree && credits <= 0) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Sinema.line(theme)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Refresh, theme.t("Enerji yok", "No energy"), tint = Sinema.textSecondary(theme).copy(0.6f), modifier = Modifier.size(18.dp))
                    }
                } else {
                    SinemaCapsuleIconButton(
                        icon = Icons.Rounded.Refresh, description = theme.t("Analiz et", "Analyze"),
                        onClick = onRefresh, selected = true, size = 40.dp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            isLoading -> Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(theme.t("Trend analizi yapılıyor...", "Analyzing trend..."), color = Sinema.textSecondary(theme), fontSize = 14.sp)
            }
            insight.isBlank() -> Text(
                theme.t(
                    "En az 2 ölçüm eklediğinde AI koçun trendi analiz edecek.",
                    "Your AI coach will analyze the trend after you add at least 2 entries."
                ),
                color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp
            )
            else -> Text(insight, color = Sinema.text(theme), fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  KAYIT SATIRI
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightEntryRow(
    entry   : WeightLogEntity,
    theme   : AppThemeState,
    onEdit  : () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    val dateStr = runCatching {
        java.time.LocalDateTime.parse(entry.recordedAt).format(FULL_DATE_FMT)
    }.getOrElse { entry.recordedAt }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .clickable(onClick = onEdit)
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${"%.1f".format(entry.weightKg)}", color = Sinema.text(theme),
            style = Sinema.Title.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        )
        Text(" kg", color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dateStr, color = Sinema.textSecondary(theme), fontSize = 12.sp)
            if (entry.note.isNotBlank()) Text(entry.note, color = Sinema.textSecondary(theme), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!entry.synced) {
            Icon(Icons.Rounded.CloudOff, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable { showDelete = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DeleteOutline, theme.t("Sil", "Delete"), tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
        }
    }

    if (showDelete) {
        SinemaDialog(
            onDismiss    = { showDelete = false },
            title        = theme.t("Kaydı sil?", "Delete entry?"),
            body         = "${"%.1f".format(entry.weightKg)} kg — $dateStr",
            confirmLabel = theme.t("Sil", "Delete"),
            confirmIcon  = Icons.Rounded.DeleteOutline,
            onConfirm    = { showDelete = false; onDelete() },
            dismissLabel = theme.t("İptal", "Cancel")
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOŞ DURUM
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyWeightState(theme: AppThemeState, onAdd: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(72.dp).sinemaCard(theme, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MonitorWeight, null, tint = Sinema.text(theme), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            theme.t("Henüz ölçüm yok", "No entries yet"), color = Sinema.text(theme),
            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            theme.t(
                "İlk kilonu kaydet; AI koçun trendini analiz edebilmesi için en az 2 ölçüm gerekir.",
                "Save your first weight; your AI coach needs at least 2 entries to analyze your trend."
            ),
            color = Sinema.textSecondary(theme), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        SinemaGoButton(
            label = theme.t("İlk kilonu ekle", "Add first weight"), icon = Icons.Rounded.Add,
            onClick = onAdd, modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD / EDIT BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AddEditWeightSheet(
    state          : WeightTrackingState,
    theme          : AppThemeState,
    onDismiss      : () -> Unit,
    onWeightChange : (String) -> Unit,
    onNoteChange   : (String) -> Unit,
    onSave         : () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Sinema.surface(theme),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Sinema.line(theme))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                if (state.editingEntry == null) theme.t("Kilo ekle", "Add weight") else theme.t("Kaydı düzenle", "Edit entry"),
                color = Sinema.text(theme), style = Sinema.Title
            )

            SinemaGlassField(
                value         = state.sheetWeightInput,
                onValueChange = onWeightChange,
                label         = theme.t("Ağırlık (kg)", "Weight (kg)"),
                placeholder   = theme.t("örn. 75.5", "e.g. 75.5"),
                suffix        = "kg",
                keyboardType  = KeyboardType.Decimal,
                imeAction     = ImeAction.Next
            )

            SinemaGlassField(
                value         = state.sheetNoteInput,
                onValueChange = onNoteChange,
                label         = theme.t("Not (isteğe bağlı)", "Note (optional)"),
                placeholder   = theme.t("sabah, spor sonrası, vb.", "morning, after workout, etc."),
                keyboardType  = KeyboardType.Text,
                imeAction     = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            SinemaGoButton(
                label = if (state.editingEntry == null) theme.t("Kaydet", "Save") else theme.t("Güncelle", "Update"),
                icon = Icons.Rounded.CheckCircle,
                onClick = { if (!state.isSaving) { focusManager.clearFocus(); onSave() } },
                dim = state.isSaving
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
