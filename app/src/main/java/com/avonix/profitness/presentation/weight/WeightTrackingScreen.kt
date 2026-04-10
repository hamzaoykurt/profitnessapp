package com.avonix.profitness.presentation.weight

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.local.entity.WeightLogEntity
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// Tarih etiketleri için kısa format
private val SHORT_DATE_FMT = DateTimeFormatter.ofPattern("d MMM")
private val FULL_DATE_FMT  = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackingScreen(
    onBack    : () -> Unit,
    viewModel : WeightTrackingViewModel = hiltViewModel()
) {
    val theme   = LocalAppTheme.current
    val accent  = MaterialTheme.colorScheme.primary
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val scope   = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WeightTrackingEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor      = Color.Transparent,
        snackbarHost        = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick            = { viewModel.openAddSheet() },
                containerColor     = accent,
                contentColor       = theme.bg0,
                shape              = RoundedCornerShape(18.dp),
                elevation          = FloatingActionButtonDefaults.elevation(8.dp, 4.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Ağırlık Ekle", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.bg0)
                .padding(innerPadding)
        ) {
            PageAccentBloom()

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = accent
                )
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(bottom = 100.dp)
                ) {
                    // ── Header ────────────────────────────────────────────
                    item {
                        WeightHeader(
                            theme   = theme,
                            accent  = accent,
                            onBack  = onBack,
                            latest  = state.latestWeightKg,
                            delta   = state.totalDeltaKg,
                            entries = state.entries.size
                        )
                    }

                    // ── Haftalık Özet Kartları ────────────────────────────
                    item {
                        WeeklySummaryRow(
                            summary = state.weeklySummary,
                            accent  = accent,
                            theme   = theme
                        )
                    }

                    // ── Trend Grafiği ─────────────────────────────────────
                    if (state.chartPoints.size >= 2) {
                        item {
                            WeightLineChart(
                                points = state.chartPoints,
                                accent = accent,
                                theme  = theme
                            )
                        }
                    }

                    // ── AI Insight Kartı ──────────────────────────────────
                    item {
                        AiInsightCard(
                            insight   = state.aiInsight,
                            isLoading = state.isAiLoading,
                            accent    = accent,
                            theme     = theme,
                            onRefresh = { viewModel.generateAiInsight() }
                        )
                    }

                    // ── Geçmiş Başlık ─────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint     = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Geçmiş Kayıtlar",
                                color      = theme.text1,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${state.entries.size} kayıt",
                                color    = theme.text3,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // ── Boş durum ─────────────────────────────────────────
                    if (state.entries.isEmpty()) {
                        item {
                            EmptyWeightState(accent = accent, theme = theme)
                        }
                    }

                    // ── Kayıt listesi ─────────────────────────────────────
                    items(state.entries, key = { it.id }) { entry ->
                        WeightEntryRow(
                            entry    = entry,
                            accent   = accent,
                            theme    = theme,
                            onEdit   = { viewModel.openEditSheet(entry) },
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }
        }
    }

    // ── Add / Edit Bottom Sheet ────────────────────────────────────────────────
    if (state.showSheet) {
        AddEditWeightSheet(
            state     = state,
            accent    = accent,
            theme     = theme,
            onDismiss = { viewModel.closeSheet() },
            onWeightChange = viewModel::onWeightInputChange,
            onNoteChange   = viewModel::onNoteInputChange,
            onSave         = { viewModel.saveEntry() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HEADER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightHeader(
    theme  : AppThemeState,
    accent : Color,
    onBack : () -> Unit,
    latest : Double?,
    delta  : Double?,
    entries: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Geri butonu
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.bg2)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBackIosNew, null, tint = theme.text1, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Ağırlık Takibi",
                color      = theme.text1,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // Özet hero
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Güncel Kilo",
                    color    = theme.text3,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                if (latest != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${"%.1f".format(latest)}",
                            color      = theme.text1,
                            fontSize   = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "kg",
                            color      = theme.text3,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(bottom = 6.dp)
                        )
                    }
                } else {
                    Text(
                        "— kg",
                        color      = theme.text3,
                        fontSize   = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Delta chip
            if (delta != null && entries >= 2) {
                val isPositive = delta >= 0
                val chipColor  = if (!isPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
                val sign       = if (isPositive) "+" else ""
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipColor.copy(0.15f))
                        .border(1.dp, chipColor.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        null,
                        tint     = chipColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$sign${"%.1f".format(delta)} kg",
                        color      = chipColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HAFTALIK ÖZET
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklySummaryRow(
    summary: WeeklySummary,
    accent : Color,
    theme  : AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label    = "Bu Hafta Ort.",
            value    = summary.thisWeekAvg?.let { "${"%.1f".format(it)} kg" } ?: "—",
            icon     = Icons.Rounded.CalendarToday,
            accent   = accent,
            theme    = theme
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label    = "Geçen Hafta Ort.",
            value    = summary.lastWeekAvg?.let { "${"%.1f".format(it)} kg" } ?: "—",
            icon     = Icons.Rounded.DateRange,
            accent   = accent,
            theme    = theme
        )
        val deltaColor = when {
            summary.deltaKg == null  -> theme.text3
            summary.deltaKg < 0     -> Color(0xFF22C55E)
            summary.deltaKg > 0     -> Color(0xFFEF4444)
            else                    -> theme.text3
        }
        val deltaStr = summary.deltaKg?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign${"%.1f".format(it)} kg"
        } ?: "—"
        SummaryCard(
            modifier   = Modifier.weight(1f),
            label      = "Haftalık Fark",
            value      = deltaStr,
            valueColor = deltaColor,
            icon       = Icons.Rounded.SwapVert,
            accent     = accent,
            theme      = theme
        )
    }
}

@Composable
private fun SummaryCard(
    modifier   : Modifier = Modifier,
    label      : String,
    value      : String,
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    accent     : Color,
    theme      : AppThemeState,
    valueColor : Color = theme.text1
) {
    Column(
        modifier = modifier
            .glassCard(accent, theme)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color      = valueColor,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            label,
            color    = theme.text3,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            letterSpacing = 0.3.sp,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LINE CHART — Canvas ile özel çizim
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightLineChart(
    points : List<WeightPoint>,
    accent : Color,
    theme  : AppThemeState
) {
    val minKg   = (points.minOf { it.weightKg } - 1.0).coerceAtLeast(0.0)
    val maxKg   = points.maxOf { it.weightKg } + 1.0
    val range   = (maxKg - minKg).coerceAtLeast(0.5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .glassCard(accent, theme)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ShowChart, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Son 90 Gün Trendi",
                color      = theme.text1,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${points.size} ölçüm",
                color    = theme.text3,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Grafik canvas
        val accentCopy = accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val padH = 20.dp.toPx()
                    val padV = 12.dp.toPx()
                    val chartW = w - padH * 2
                    val chartH = h - padV * 2

                    fun xOf(i: Int) = padH + (i.toFloat() / (points.lastIndex.coerceAtLeast(1))) * chartW
                    fun yOf(kg: Double) = padV + chartH * (1f - ((kg - minKg) / range).toFloat())

                    // Bezier path
                    val linePath = Path()
                    val fillPath = Path()
                    points.forEachIndexed { i, pt ->
                        val x = xOf(i)
                        val y = yOf(pt.weightKg).toFloat()
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = xOf(i - 1)
                            val prevY = yOf(points[i - 1].weightKg).toFloat()
                            val cpX   = (prevX + x) / 2
                            linePath.cubicTo(cpX, prevY, cpX, y, x, y)
                            fillPath.cubicTo(cpX, prevY, cpX, y, x, y)
                        }
                    }
                    // Kapat — dolgu alanı
                    fillPath.lineTo(xOf(points.lastIndex), h)
                    fillPath.close()

                    val fillBrush = Brush.verticalGradient(
                        colors  = listOf(accentCopy.copy(0.25f), Color.Transparent),
                        startY  = padV,
                        endY    = h
                    )

                    onDrawBehind {
                        // Grid lines (yatay)
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val gy = padV + chartH * (i.toFloat() / gridCount)
                            drawLine(
                                color       = Color.White.copy(0.06f),
                                start       = Offset(padH, gy),
                                end         = Offset(w - padH, gy),
                                strokeWidth = 1f
                            )
                        }
                        // Dolgu
                        drawPath(fillPath, fillBrush)
                        // Çizgi
                        drawPath(
                            linePath,
                            color       = accentCopy,
                            style       = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        // Noktalar — fazla kayıt varsa sadece ilk/son ve min/max göster
                        val minIdx = points.indexOfMinBy { it.weightKg }
                        val maxIdx = points.indexOfMaxBy { it.weightKg }
                        val keySet = setOf(0, points.lastIndex, minIdx, maxIdx)
                        points.forEachIndexed { i, pt ->
                            if (i in keySet) {
                                val x = xOf(i)
                                val y = yOf(pt.weightKg).toFloat()
                                drawCircle(color = accentCopy.copy(0.25f), radius = 8.dp.toPx(), center = Offset(x, y))
                                drawCircle(color = accentCopy, radius = 4.dp.toPx(), center = Offset(x, y))
                            }
                        }
                    }
                }
        )

        // X ekseni etiketleri: ilk ve son tarih
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                points.firstOrNull()?.date?.format(SHORT_DATE_FMT) ?: "",
                color    = theme.text3,
                fontSize = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                points.lastOrNull()?.date?.format(SHORT_DATE_FMT) ?: "",
                color    = theme.text3,
                fontSize = 10.sp
            )
        }

        // Y ekseni: min ve max değer
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${"%.1f".format(maxKg - 1)} kg",
                color    = theme.text3,
                fontSize = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${"%.1f".format(minKg + 1)} kg",
                color    = theme.text3,
                fontSize = 10.sp
            )
        }
    }
}

// List extension helpers
private fun <T> List<T>.indexOfMinBy(selector: (T) -> Double): Int =
    indices.minByOrNull { selector(this[it]) } ?: 0

private fun <T> List<T>.indexOfMaxBy(selector: (T) -> Double): Int =
    indices.maxByOrNull { selector(this[it]) } ?: lastIndex

// ═══════════════════════════════════════════════════════════════════════════
//  AI INSIGHT KARTI
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AiInsightCard(
    insight  : String,
    isLoading: Boolean,
    accent   : Color,
    theme    : AppThemeState,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .glassCard(accent, theme)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // AI badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "AI ANALİZ",
                    color      = accent,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(theme.bg3)
                        .clickable(onClick = onRefresh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = theme.text3, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color    = accent,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Trend analizi yapılıyor...", color = theme.text3, fontSize = 13.sp)
            }
        } else if (insight.isBlank()) {
            Text(
                "En az 2 ölçüm eklediğinde AI koçun trendi analiz edecek.",
                color    = theme.text3,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                insight,
                color      = theme.text2,
                fontSize   = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  KAYIT SATIRI
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightEntryRow(
    entry   : WeightLogEntity,
    accent  : Color,
    theme   : AppThemeState,
    onEdit  : () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateStr = runCatching {
        val dt = java.time.LocalDateTime.parse(entry.recordedAt)
        dt.format(FULL_DATE_FMT)
    }.getOrElse { entry.recordedAt }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg2)
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ağırlık değeri
        Text(
            "${"%.1f".format(entry.weightKg)} kg",
            color      = theme.text1,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                dateStr,
                color    = theme.text3,
                fontSize = 11.sp
            )
            if (entry.note.isNotBlank()) {
                Text(
                    entry.note,
                    color    = theme.text3,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Senkron göstergesi
        if (!entry.synced) {
            Icon(
                Icons.Rounded.CloudOff,
                null,
                tint     = theme.text3.copy(0.5f),
                modifier = Modifier.size(13.dp).padding(end = 4.dp)
            )
        }

        // Sil butonu
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable { showDeleteConfirm = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DeleteOutline, null, tint = theme.text3.copy(0.5f), modifier = Modifier.size(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest   = { showDeleteConfirm = false },
            containerColor     = theme.bg2,
            title              = { Text("Kaydı sil?", color = theme.text1, fontWeight = FontWeight.Bold) },
            text               = { Text("${"%.1f".format(entry.weightKg)} kg — $dateStr", color = theme.text3, fontSize = 13.sp) },
            confirmButton      = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Sil", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton      = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("İptal", color = theme.text3)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOŞKEN DURUM
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyWeightState(accent: Color, theme: AppThemeState) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚖️", fontSize = 48.sp)
        Text(
            "Henüz ölçüm yok",
            color      = theme.text1,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "İlk kilonu kaydetmek için + butonuna bas.\nAI koçun trend analizi yapabilsin diye\nen az 2 ölçüme ihtiyaç var.",
            color     = theme.text3,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD / EDIT BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditWeightSheet(
    state          : WeightTrackingState,
    accent         : Color,
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
        containerColor   = theme.bg1,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.stroke)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık
            Text(
                if (state.editingEntry == null) "Ağırlık Ekle" else "Kaydı Düzenle",
                color      = theme.text1,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black
            )

            // Ağırlık input
            OutlinedTextField(
                value         = state.sheetWeightInput,
                onValueChange = onWeightChange,
                label         = { Text("Ağırlık (kg)", color = theme.text3) },
                placeholder   = { Text("örn. 75.5", color = theme.text3.copy(0.5f)) },
                trailingIcon  = {
                    Text("kg", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction    = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accent,
                    unfocusedBorderColor = theme.stroke,
                    focusedTextColor     = theme.text1,
                    unfocusedTextColor   = theme.text1,
                    cursorColor          = accent,
                    focusedContainerColor   = theme.bg2,
                    unfocusedContainerColor = theme.bg2
                )
            )

            // Not input
            OutlinedTextField(
                value         = state.sheetNoteInput,
                onValueChange = onNoteChange,
                label         = { Text("Not (isteğe bağlı)", color = theme.text3) },
                placeholder   = { Text("sabah, spor sonrası, vb.", color = theme.text3.copy(0.5f)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accent,
                    unfocusedBorderColor = theme.stroke,
                    focusedTextColor     = theme.text1,
                    unfocusedTextColor   = theme.text1,
                    cursorColor          = accent,
                    focusedContainerColor   = theme.bg2,
                    unfocusedContainerColor = theme.bg2
                )
            )

            // Kaydet butonu
            Button(
                onClick  = { focusManager.clearFocus(); onSave() },
                enabled  = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor   = theme.bg0
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = theme.bg0,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (state.editingEntry == null) "Kaydet" else "Güncelle",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
