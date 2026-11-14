package com.cosmibit.profitness.presentation.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cosmibit.profitness.presentation.components.SinemaVideo
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaLayout
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaSectionLabel
import com.cosmibit.profitness.presentation.components.drawGlowLine
import com.cosmibit.profitness.presentation.program.rememberExerciseCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// shortcut: c2/c3 token'ları (accentSecondary/accentTertiary) core/theme'e girene kadar geçici ve yalnız burada (Hamza seti: TEAL VIVID).
// Sobi birleştirirken bu iki satır token okumasıyla değişecek.

private fun AppThemeState.uiLocale(): Locale =
    if (language == AppLanguage.ENGLISH) Locale.ENGLISH else Locale.forLanguageTag("tr-TR")

/**
 * "Hareketi gör": TAM EKRAN dikey video (yoksa foto) + üstte cam kapat düğmesi + altta küçük cam bilgi kartı (ad, çipler, plan).
 * Karta dokununca / yukarı çekince cam panel açılır: kas haritası + REKOR, PLAN, SON PERFORMANS, İLERLEME, NOTLAR,
 * (varsa) harekete özel talimat, AI analiz. Videoya dokunmak sesi açar/kapatır.
 * [showHistory] false ise geçmiş bölümleri çizilmez (program önizlemesi: geçmiş verilmiyor).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailSheet(
    exercise: Exercise,
    dayNotes: String = "",
    onDismiss: () -> Unit,
    weightHistory: List<SetCompletionEntity> = emptyList(),
    aiInsight: String = "",
    isAiLoading: Boolean = false,
    isFree: Boolean = true,
    aiCredits: Int = 0,
    onRequestAiInsight: () -> Unit = {},
    showHistory: Boolean = true
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val accent = theme.effectiveAccentColor
    val locale = remember(theme.language) { theme.uiLocale() }
    val activityMetric = remember(exercise.category, exercise.name, exercise.target, exercise.reps) {
        exerciseActivityMetric(exercise)
    }
    val isStrength = activityMetric == ActivityMetric.Strength
    val stats = remember(weightHistory) { buildHistoryStats(weightHistory) }
    val regions = remember(exercise.target) { muscleRegionsFor(exercise.target) }
    // Harekete özel talimat: katalog (ExerciseEntity.description). Boşsa bölüm hiç çizilmez.
    val catalog = rememberExerciseCatalog()
    val instructions = remember(catalog, exercise.name) { catalog.find(exercise.name)?.description.orEmpty().trim() }

    val videoUrl = exercise.videoUrl?.takeIf { it.isNotBlank() }
    val photoUrl = if (exercise.image.isNotBlank()) exercise.image else categoryImageUrl(exercise.category)
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    var expanded by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(true) }
    var soundHintKey by remember { mutableIntStateOf(0) }
    var showSoundHint by remember { mutableStateOf(false) }
    LaunchedEffect(soundHintKey) {
        if (soundHintKey > 0) {
            showSoundHint = true
            delay(900)
            showSoundHint = false
        }
    }
    val planSummary = buildList {
        if (isStrength) {
            if (exercise.sets > 0) add("${exercise.sets} ${theme.t("set", "sets")}")
            exercise.reps.takeIf { it.isNotBlank() }?.let { add("$it ${theme.t("tekrar", "reps")}") }
        } else {
            plannedDurationLabel(exercise.reps, theme).takeIf { it != "—" }?.let { add(it) }
        }
        if (exercise.restSeconds > 0) add("${exercise.restSeconds} ${theme.t("sn dinlenme", "sec rest")}")
    }.joinToString(" · ")
    val panelScrollGuard = remember {
        // Panel en üstteyken aşağı çekmek paneli kapatır (kalan kaydırma = içerik zaten en üstte).
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 24f) expanded = false
                return Offset.Zero
            }
        }
    }

    Dialog(
        onDismissRequest = { if (expanded) expanded = false else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // ── Tam ekran medya: dikey video (yoksa foto). Dokununca ses aç/kapat; panel açıkken dokunma paneli kapatır ──
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (expanded) expanded = false
                        else if (videoUrl != null) { muted = !muted; soundHintKey++ }
                    }
            ) {
                if (videoUrl != null) {
                    SinemaVideo(url = videoUrl, posterUrl = photoUrl, playing = true, muted = muted, modifier = Modifier.fillMaxSize())
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        // Istek boyutu = olculen boyut (ust sinir 2048px)
                        val reqW = if (constraints.hasBoundedWidth && constraints.maxWidth > 0) constraints.maxWidth.coerceAtMost(2048) else 1080
                        val reqH = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) constraints.maxHeight.coerceAtMost(2048) else 1920
                        val imageRequest = remember(context, photoUrl, reqW, reqH) {
                            ImageRequest.Builder(context).data(photoUrl).size(reqW, reqH).crossfade(false).build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = theme.t("Hareket gösterimi", "Movement demonstration"),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.50f), Color.Transparent)))
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.72f))))
            )

            AnimatedVisibility(
                visible = showSoundHint,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Box(Modifier.size(64.dp).sinemaCapsuleGlass(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        theme.t(if (muted) "Ses kapalı" else "Ses açık", if (muted) "Muted" else "Sound on"),
                        tint = Color.White, modifier = Modifier.size(28.dp)
                    )
                }
            }

            SinemaCapsuleIconButton(
                icon = Icons.Rounded.Close,
                description = theme.t("Kapat", "Close"),
                onClick = onDismiss,
                selected = false,
                size = 44.dp,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = gutter, top = 8.dp)
            )

            // ── Küçük cam bilgi kartı: ad + kas/ekipman çipleri + plan; yukarı çekince / dokununca panel açılır ──
            AnimatedVisibility(
                visible = !expanded,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(), exit = fadeOut()
            ) {
                val chips = listOf(exercise.target to true, exercise.category to false)
                    .map { (t, muscle) -> theme.fitnessTermCompactDisplayName(t).capFirst() to muscle }
                    .filter { it.first.isNotBlank() }
                    .distinctBy { it.first.lowercase() }
                val openLabel = theme.t("Ayrıntıları aç", "Open details")
                var drag by remember { mutableFloatStateOf(0f) }
                Column(
                    Modifier
                        .widthIn(max = SinemaLayout.ContentMaxReading)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = gutter, vertical = 16.dp)
                        .sinemaCapsuleGlass(RoundedCornerShape(26.dp), fillTop = 0.16f, fillBottom = 0.10f)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragEnd = { if (drag < -40f) expanded = true },
                                onVerticalDrag = { _, dy -> drag += dy }
                            )
                        }
                        .clickable(role = Role.Button, onClickLabel = openLabel) { expanded = true }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(width = 32.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(0.40f)))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        theme.exerciseDisplayName(exercise.name),
                        style = Sinema.Title.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (chips.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEach { (text, muscle) ->
                                val c = if (muscle) theme.accent2Color else theme.accent3Color
                                Text(
                                    text,
                                    color = if (muscle) theme.accent2TextColor else theme.accent3TextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(c.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    if (planSummary.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(planSummary, color = Color.White.copy(0.82f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ── Cam panel: mevcut içerik (kas haritası, rekor, plan, son performans, ilerleme, notlar, talimat, AI) ──
            AnimatedVisibility(
                visible = expanded,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Column(
                    Modifier
                        .widthIn(max = SinemaLayout.ContentMaxReading)
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .sinemaGlass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), fillAlpha = 0.94f)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth().sinemaClickable(onClick = { expanded = false }, role = Role.Button, onClickLabel = theme.t("Paneli kapat", "Close panel"), scale = 0.98f)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(0.30f)))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(panelScrollGuard)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                    ) {
            Column(Modifier.padding(horizontal = gutter)) {
                Spacer(Modifier.height(4.dp))

                // ── Başlık bloğu: [kas haritası] başlık + çipler + REKOR ──
                Row(verticalAlignment = Alignment.Top) {
                    if (regions.isNotEmpty()) {
                        MuscleMap(
                            regions = regions,
                            description = theme.t(
                                "Hedef kas: ${theme.fitnessTermCompactDisplayName(exercise.target).capFirst()}",
                                "Target muscle: ${theme.fitnessTermCompactDisplayName(exercise.target).capFirst()}"
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.exerciseDisplayName(exercise.name),
                            style = Sinema.Title.copy(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
                            color = Sinema.text(theme),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        val chips = listOf(exercise.target, exercise.category)
                            .map { theme.fitnessTermCompactDisplayName(it).capFirst() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                        if (chips.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) { chips.forEach { InfoChip(it) } }
                        }
                        val record = stats.record
                        if (isStrength && record != null) {
                            Spacer(Modifier.height(10.dp))
                            val recordText = buildString {
                                append(theme.t("REKOR", "RECORD")).append(' ')
                                append(formatWeightNumber(record.weightKg, locale)).append(" kg")
                                record.reps?.takeIf { it > 0 }?.let { append(" × ").append(it) }
                            }
                            Text(
                                recordText,
                                color = theme.accent3TextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.06.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(theme.accent3Color.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Set tipi / grup
                if (!exercise.groupId.isNullOrBlank()) {
                    val groupName = exercise.groupLabel.ifBlank {
                        when (exercise.groupType) {
                            "giant_set" -> theme.t("Dev Set", "Giant Set")
                            "circuit" -> theme.t("Devre", "Circuit")
                            else -> theme.t("Süperset", "Superset")
                        }
                    }
                    val groupDetails = buildList {
                        exercise.groupRounds?.let { add(theme.t("$it tur", "$it rounds")) }
                        exercise.groupRestSeconds?.let { add(theme.t("$it sn tur arası", "$it sec between rounds")) }
                    }.joinToString(" · ")
                    Spacer(Modifier.height(14.dp))
                    InfoChip(listOf(groupName, groupDetails).filter { it.isNotBlank() }.joinToString(" · "))
                }

                // ── PLAN ──
                Spacer(Modifier.height(24.dp))
                val planCells = buildList {
                    if (isStrength) {
                        add(exercise.sets.toString() to theme.t("set", "sets"))
                        add(exercise.reps.ifBlank { "—" } to theme.t("tekrar", "reps"))
                        if (exercise.weightKg > 0f) add(formatWeightNumber(exercise.weightKg, locale) to "kg")
                    } else {
                        add(plannedDurationLabel(exercise.reps, theme) to theme.t("süre", "duration"))
                        exercise.targetDistanceMeters?.takeIf { it > 0f }?.let {
                            add(formatDistance(it, locale) to theme.t("mesafe", "distance"))
                        }
                    }
                    if (exercise.restSeconds > 0) add(exercise.restSeconds.toString() to theme.t("sn dinlenme", "sec rest"))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .sinemaCard(theme, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                ) {
                    planCells.forEach { (value, label) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                value,
                                style = Sinema.Title.copy(fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
                                color = Sinema.text(theme),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                color = Sinema.textSecondary(theme),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── SON PERFORMANS + İLERLEME (veya geçmiş yok kartı) ──
            if (showHistory) {
                val last = stats.last
                if (last == null) {
                    Spacer(Modifier.height(24.dp))
                    EmptyHistoryCard(
                        theme.t(
                            "Bu hareketi henüz kaydetmedin. İlk setini tamamladığında son performansın ve grafiğin burada görünür.",
                            "You haven't logged this movement yet. Once you finish your first set, your last performance and chart show up here."
                        ),
                        theme
                    )
                } else {
                    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
                    SinemaSectionLabel(
                        theme.t("SON PERFORMANS · ${formatDate(last.date, dateFormat)}", "LAST PERFORMANCE · ${formatDate(last.date, dateFormat)}"),
                        Modifier.padding(top = 8.dp)
                    )
                    Column(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .sinemaCard(theme, RoundedCornerShape(20.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        last.sets.forEachIndexed { i, set ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${i + 1}",
                                    color = Sinema.textSecondary(theme),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    setLine(set, isStrength, locale, theme),
                                    color = Sinema.text(theme),
                                    style = Sinema.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
                                )
                            }
                        }
                        val delta = if (isStrength) stats.topWeightDeltaKg else null
                        if (delta != null) {
                            Spacer(Modifier.height(4.dp))
                            val deltaText = when {
                                delta > 0.001f -> theme.t(
                                    "+${formatWeightNumber(delta, locale)} kg önceki seansa göre",
                                    "+${formatWeightNumber(delta, locale)} kg vs previous session"
                                )
                                delta < -0.001f -> theme.t(
                                    "−${formatWeightNumber(-delta, locale)} kg önceki seansa göre",
                                    "−${formatWeightNumber(-delta, locale)} kg vs previous session"
                                )
                                else -> theme.t("Önceki seansla aynı ağırlık", "Same weight as previous session")
                            }
                            Text(
                                deltaText,
                                color = if (delta >= -0.001f) theme.accent2TextColor else Sinema.textSecondary(theme),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
                            )
                        }
                    }

                    SinemaSectionLabel(theme.t("İLERLEME", "PROGRESS"), Modifier.padding(top = 8.dp))
                    val series = remember(stats, isStrength) { chartSeries(stats.sessions, isStrength) }
                    if (stats.sessions.size < 2 || series.size < 2) {
                        EmptyHistoryCard(
                            theme.t("Grafik için en az 2 seans gerekiyor.", "At least 2 sessions are needed for the chart."),
                            theme
                        )
                    } else {
                        ProgressionCard(
                            series = series,
                            recordDate = if (isStrength) stats.record?.date else null,
                            isStrength = isStrength,
                            locale = locale,
                            dateFormat = dateFormat,
                            theme = theme
                        )
                    }
                }
            }

            // ── NOTLAR ──
            if (dayNotes.isNotBlank() || exercise.notes.isNotBlank()) {
                SinemaSectionLabel(theme.t("NOTLAR", "NOTES"), Modifier.padding(top = 8.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .sinemaCard(theme, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    if (dayNotes.isNotBlank()) {
                        DetailNote(theme.t("GÜN NOTU", "DAY NOTE"), dayNotes, theme)
                    }
                    if (dayNotes.isNotBlank() && exercise.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = theme.stroke)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (exercise.notes.isNotBlank()) {
                        DetailNote(theme.t("HAREKET NOTU", "EXERCISE NOTE"), exercise.notes, theme)
                    }
                }
            }

            // ── NASIL YAPILIR: yalnız harekete özel gerçek metin varsa ──
            if (instructions.isNotBlank()) {
                SinemaSectionLabel(theme.t("NASIL YAPILIR", "HOW TO DO IT"), Modifier.padding(top = 8.dp))
                Text(
                    instructions,
                    color = Sinema.text(theme),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .sinemaCard(theme, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                )
            }

            // ── AI analiz (mevcut davranış korunur) ──
            if (weightHistory.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                AiProgressionSection(
                    insight = aiInsight,
                    isLoading = isAiLoading,
                    isFree = isFree,
                    credits = aiCredits,
                    accent = accent,
                    theme = theme,
                    onRefresh = onRequestAiInsight
                )
            }

            Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  PARÇALAR
// ═════════════════════════════════════════════════════════════════════════════

private fun String.capFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("tr-TR")) else it.toString() }

@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        color = Sinema.Text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun EmptyHistoryCard(text: String, theme: AppThemeState) {
    Text(
        text,
        color = Sinema.textSecondary(theme),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .padding(18.dp)
    )
}

@Composable
private fun DetailNote(label: String, text: String, theme: AppThemeState) {
    Text(text = label, color = Sinema.textSecondary(theme), style = Sinema.Label)
    Spacer(Modifier.height(6.dp))
    Text(text = text, color = Sinema.text(theme), fontSize = 15.sp, lineHeight = 22.sp)
}

private fun formatDate(date: String, format: DateTimeFormatter): String =
    runCatching { LocalDate.parse(date).format(format) }.getOrDefault(date)

private fun setLine(set: SetCompletionEntity, strength: Boolean, locale: Locale, theme: AppThemeState): String {
    if (strength) {
        val kg = set.weightKg?.takeIf { it > 0f }
        val reps = set.repsActual
        return when {
            kg != null && reps != null -> "${formatWeightNumber(kg, locale)} kg × $reps"
            kg != null -> "${formatWeightNumber(kg, locale)} kg"
            reps != null -> "$reps ${theme.t("tekrar", "reps")}"
            else -> "—"
        }
    }
    val parts = listOfNotNull(
        set.durationSeconds?.takeIf { it > 0 }?.let { formatDuration(it, theme) },
        set.distanceMeters?.takeIf { it > 0f }?.let { formatDistance(it, locale) }
    )
    return parts.joinToString(" · ").ifBlank { "—" }
}

private fun formatDuration(seconds: Int, theme: AppThemeState): String {
    val dk = theme.t("dk", "min")
    val sn = theme.t("sn", "sec")
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> "$s $sn"
        s == 0 -> "$m $dk"
        else -> "$m $dk $s $sn"
    }
}

// ── Kas haritası: düz ön + arka siluet (96×120), hedef kas c2, diğerleri beyaz .10 ──
private enum class Side { FRONT, BACK, BOTH }
private data class Part(val side: Side, val region: MuscleRegion?, val x: Float, val y: Float, val w: Float, val h: Float, val r: Float)

// Figür koordinatı: 44 × 120. Sol/sağ çiftler tek parça çizilir.
private val MuscleParts = listOf(
    Part(Side.BOTH, MuscleRegion.SHOULDERS, 6f, 18f, 10f, 9f, 4f),
    Part(Side.BOTH, MuscleRegion.SHOULDERS, 28f, 18f, 10f, 9f, 4f),
    Part(Side.FRONT, MuscleRegion.CHEST, 16f, 19f, 12f, 11f, 3f),
    Part(Side.FRONT, MuscleRegion.ABS, 17f, 31f, 10f, 16f, 3f),
    Part(Side.BACK, MuscleRegion.BACK, 15f, 19f, 14f, 28f, 4f),
    Part(Side.FRONT, MuscleRegion.BICEPS, 3f, 28f, 6f, 13f, 3f),
    Part(Side.FRONT, MuscleRegion.BICEPS, 35f, 28f, 6f, 13f, 3f),
    Part(Side.BACK, MuscleRegion.TRICEPS, 3f, 28f, 6f, 13f, 3f),
    Part(Side.BACK, MuscleRegion.TRICEPS, 35f, 28f, 6f, 13f, 3f),
    Part(Side.BOTH, MuscleRegion.FOREARM, 2f, 42f, 6f, 14f, 3f),
    Part(Side.BOTH, MuscleRegion.FOREARM, 36f, 42f, 6f, 14f, 3f),
    Part(Side.BACK, MuscleRegion.GLUTES, 15f, 48f, 14f, 10f, 4f),
    Part(Side.FRONT, MuscleRegion.QUADS, 15f, 50f, 6.5f, 30f, 3f),
    Part(Side.FRONT, MuscleRegion.QUADS, 22.5f, 50f, 6.5f, 30f, 3f),
    Part(Side.BACK, MuscleRegion.HAMSTRINGS, 15f, 59f, 6.5f, 22f, 3f),
    Part(Side.BACK, MuscleRegion.HAMSTRINGS, 22.5f, 59f, 6.5f, 22f, 3f),
    Part(Side.BOTH, MuscleRegion.CALVES, 15.5f, 84f, 5.5f, 26f, 3f),
    Part(Side.BOTH, MuscleRegion.CALVES, 23f, 84f, 5.5f, 26f, 3f)
)

@Composable
private fun MuscleMap(regions: Set<MuscleRegion>, description: String) {
    val theme = LocalAppTheme.current
    val off = Color.White.copy(alpha = 0.10f)
    Canvas(
        Modifier
            .size(width = 96.dp, height = 120.dp)
            .semantics { contentDescription = description }
    ) {
        val u = size.width / 96f
        listOf(0f to Side.FRONT, 52f to Side.BACK).forEach { (ox, side) ->
            drawCircle(off, radius = 7f * u, center = Offset((ox + 22f) * u, 9f * u))
            MuscleParts.forEach { p ->
                if (p.side == Side.BOTH || p.side == side) {
                    drawRoundRect(
                        color = if (p.region in regions) theme.accent2Color else off,
                        topLeft = Offset((ox + p.x) * u, p.y * u),
                        size = Size(p.w * u, p.h * u),
                        cornerRadius = CornerRadius(p.r * u)
                    )
                }
            }
        }
    }
}

// ── İlerleme grafiği: çizgi c2, rekor noktası c3 ──
@Composable
private fun ProgressionCard(
    series: List<ChartPoint>,
    recordDate: String?,
    isStrength: Boolean,
    locale: Locale,
    dateFormat: DateTimeFormatter,
    theme: AppThemeState
) {
    val lastValue = series.last().value
    val unit = if (isStrength) "kg" else theme.t("dk", "min")
    val valueText = formatWeightNumber(lastValue, locale)
    val minV = (series.minOf { it.value } - if (isStrength) 2f else 1f).coerceAtLeast(0f)
    val maxV = series.maxOf { it.value } + if (isStrength) 2f else 1f
    val range = (maxV - minV).coerceAtLeast(1f)
    val gridColor = Sinema.line(theme)
    val recordIndex = recordDate?.let { d -> series.indexOfFirst { it.date == d } } ?: -1
    val chartDesc = theme.t(
        "${if (isStrength) "Ağırlık" else "Süre"} grafiği: ${formatWeightNumber(series.first().value, locale)} $unit değerinden $valueText $unit değerine",
        "${if (isStrength) "Weight" else "Duration"} chart: ${formatWeightNumber(series.first().value, locale)} $unit to $valueText $unit"
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(valueText, style = Sinema.Title.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold), color = theme.accent2TextColor)
            Spacer(Modifier.width(6.dp))
            Text(
                if (isStrength) theme.t("kg · son seansın en yüksek seti", "kg · last session's top set") else theme.t("dk toplam, son seans", "min total, last session"),
                color = Sinema.textSecondary(theme),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = chartDesc }
        ) {
            val w = size.width
            val h = size.height
            val padH = 12.dp.toPx()
            val padV = 12.dp.toPx()
            val chartW = w - padH * 2f
            val chartH = h - padV * 2f
            val n = series.lastIndex.coerceAtLeast(1)

            fun xOf(i: Int) = padH + (i.toFloat() / n) * chartW
            fun yOf(v: Float) = padV + chartH * (1f - (v - minV) / range)

            repeat(4) { i ->
                val gy = padV + chartH * (i.toFloat() / 3f)
                drawLine(gridColor, Offset(padH, gy), Offset(w - padH, gy), 1f)
            }

            val linePath = Path()
            val fillPath = Path()
            series.forEachIndexed { i, pt ->
                val x = xOf(i)
                val y = yOf(pt.value)
                if (i == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    val px = xOf(i - 1)
                    val py = yOf(series[i - 1].value)
                    val cpX = (px + x) / 2f
                    linePath.cubicTo(cpX, py, cpX, y, x, y)
                    fillPath.cubicTo(cpX, py, cpX, y, x, y)
                }
            }
            fillPath.lineTo(xOf(series.lastIndex), h)
            fillPath.close()

            drawPath(fillPath, Brush.verticalGradient(listOf(theme.accent2Color.copy(0.12f), Color.Transparent), padV, h))
            drawGlowLine(linePath, theme.accent2Color, 2.5.dp.toPx())
            if (recordIndex >= 0) {
                drawCircle(theme.accent3Color, radius = 5.dp.toPx(), center = Offset(xOf(recordIndex), yOf(series[recordIndex].value)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(formatDate(series.first().date, dateFormat), color = Sinema.textSecondary(theme), style = Sinema.Label)
            Spacer(Modifier.weight(1f))
            Text(formatDate(series.last().date, dateFormat), color = Sinema.textSecondary(theme), style = Sinema.Label)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  AKTİVİTE TÜRÜ
// ═════════════════════════════════════════════════════════════════════════════

private enum class ActivityMetric { Strength, Duration, DurationDistance }

private fun exerciseActivityMetric(exercise: Exercise): ActivityMetric {
    val haystack = listOf(exercise.category, exercise.name, exercise.target, exercise.reps)
        .joinToString(" ")
        .lowercase()
    val distanceKeywords = listOf(
        "koş", "kos", "run", "jog", "bisiklet", "bike", "cycle", "cycling",
        "yüz", "yuz", "swim", "yürüy", "yuruy", "walk", "kürek", "kurek", "row"
    )
    if (distanceKeywords.any { it in haystack }) return ActivityMetric.DurationDistance
    val durationKeywords = listOf(
        "kardiyo", "cardio", "dayan", "endurance", "elliptical",
        "tempo", "interval", "hiit", "plank", "yoga", "pilates", "mobility"
    )
    return if (
        durationKeywords.any { it in haystack } ||
        exercise.reps.contains("s", ignoreCase = true) ||
        exercise.reps.contains("dk", ignoreCase = true) ||
        exercise.reps.contains("min", ignoreCase = true)
    ) ActivityMetric.Duration else ActivityMetric.Strength
}

private fun plannedDurationLabel(reps: String, theme: AppThemeState): String {
    val raw = reps.trim()
    val number = raw.filter { it.isDigit() }.toIntOrNull()
    return when {
        raw.contains("dk", ignoreCase = true) || raw.contains("min", ignoreCase = true) -> raw
        raw.contains("s", ignoreCase = true) -> raw
        number != null -> "$number ${theme.t("dk", "min")}"
        else -> raw.ifBlank { "—" }
    }
}

private fun formatDistance(meters: Float, locale: Locale): String =
    if (meters >= 1000f) "${"%.1f".format(locale, meters / 1000f)} km" else "${meters.toInt()} m"

// ═════════════════════════════════════════════════════════════════════════════
//  AI INSIGHT (davranış değişmedi)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiProgressionSection(
    insight: String,
    isLoading: Boolean,
    isFree: Boolean,
    credits: Int,
    accent: Color,
    theme: AppThemeState,
    onRefresh: () -> Unit
) {
    val noEnergy = isFree && credits <= 0
    SinemaSectionLabel(
        theme.t("AI KOÇ", "AI COACH"),
        trailing = if (isFree) theme.t("$credits Enerji kaldı", "$credits Energy left") else null
    )
    Column(Modifier.padding(horizontal = 20.dp)) {
        if (insight.isNotBlank() || isLoading) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(theme.t("Analiz yapılıyor...", "Analyzing..."), color = Sinema.textSecondary(theme), fontSize = 14.sp)
                    }
                } else {
                    Text(insight, color = Sinema.text(theme), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        SinemaGoOrIdle(
            label = if (insight.isBlank()) theme.t("AI ANALİZİ AL", "GET AI ANALYSIS") else theme.t("YENİLE", "REFRESH"),
            icon = Icons.Rounded.AutoAwesome,
            enabled = !isLoading && !noEnergy,
            onClick = onRefresh
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (noEnergy) theme.t("Enerjin bitti. AI analizi 2 Enerji harcar.", "You're out of Energy. AI analysis costs 2 Energy.")
            else theme.t("AI analizi 2 Enerji harcar.", "AI analysis costs 2 Energy."),
            color = Sinema.textSecondary(theme),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
