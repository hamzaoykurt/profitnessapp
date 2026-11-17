package com.cosmibit.profitness.presentation.program

import com.cosmibit.profitness.core.theme.sinemaSoftRing
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cosmibit.profitness.core.theme.sinemaSolid
import com.cosmibit.profitness.core.theme.accentColor
import com.cosmibit.profitness.core.theme.SinemaRaise
import com.cosmibit.profitness.core.theme.sinemaRaised
import com.cosmibit.profitness.core.theme.sinemaCardAccent
import com.cosmibit.profitness.core.theme.accentHiTextColor
import com.cosmibit.profitness.core.theme.sinemaListEnter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalDensity
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaFitText
import com.cosmibit.profitness.core.ui.SinemaLayout
import com.cosmibit.profitness.core.ui.cappedDisplaySp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.AppThemeState
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.LocalSinemaHaze
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.supportAlert
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.core.theme.accentHi
import com.cosmibit.profitness.core.theme.rewardGold
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.SinemaRadius
import com.cosmibit.profitness.presentation.components.SinemaTypeCover
import com.cosmibit.profitness.presentation.components.coverHueFor
import com.cosmibit.profitness.presentation.components.coverIconFor
import androidx.compose.ui.unit.DpOffset
import com.cosmibit.profitness.domain.model.Program
import com.cosmibit.profitness.domain.model.ProgramType
import com.cosmibit.profitness.presentation.workout.SceneImage
import com.cosmibit.profitness.presentation.workout.WorkoutDayState

/** Antrenman sekmesindeki aktif programın bugünkü özeti (Programlar kapağı için). Veri yoksa null → panel gizlenir. */
data class ProgramTodayInfo(
    val completed: Int,
    val total: Int,
    val todayTitle: String,
    val todayExercises: Int,
    val todayIsRest: Boolean,
    val week: List<WeekCell>,
    /** Bugünün tamamlanan hareket sayısı. */
    val todayCompleted: Int = 0,
    /** Bugünün hedef kas grupları (en çok geçen <=3; veri yoksa boş). */
    val todayMuscles: List<String> = emptyList(),
    /** Tahmini süre (dk): set x (45 sn çalışma + dinlenme); 0 = bilinmiyor. */
    val todayMinutes: Int = 0
)

data class WeekCell(val label: String, val isToday: Boolean, val isRest: Boolean, val isDone: Boolean = false)

fun buildProgramTodayInfo(
    days: List<WorkoutDayState>,
    selectedIdx: Int,
    theme: AppThemeState,
    hasProgramLoaded: Boolean,
    isLoading: Boolean
): ProgramTodayInfo? {
    if (!hasProgramLoaded || isLoading || days.isEmpty()) return null
    val today = days.getOrNull(selectedIdx) ?: return null
    val total = days.sumOf { it.totalCount }
    if (total == 0) return null
    return ProgramTodayInfo(
        completed = days.sumOf { d -> d.day.exercises.count { it.id in d.completedIds } },
        total = total,
        todayTitle = com.cosmibit.profitness.presentation.workout.compactWorkoutTitle(today.day.title, theme),
        todayExercises = today.totalCount,
        todayIsRest = today.day.isRestDay,
        todayCompleted = today.completedCount,
        todayMuscles = topMuscles(today.day.exercises.map { it.target }, theme),
        todayMinutes = (today.day.exercises.sumOf { it.sets * (45 + it.restSeconds) } / 60f).let { if (it < 1f) 0 else (Math.round(it / 5f) * 5).coerceAtLeast(5) },
        week = days.mapIndexed { i, d ->
            WeekCell(com.cosmibit.profitness.presentation.workout.localizedWorkoutDayLabel(d.day.day, theme), i == selectedIdx, d.day.isRestDay,
                isDone = d.totalCount > 0 && d.day.exercises.count { it.id in d.completedIds } >= d.totalCount)
        }
    )
}

private val SheetTopRadius = 28.dp

// ── Kapak (çizim .prh/.prl) ──────────────────────────────────────────────────────

/** Tam boy foto kapak: foto + karartma + AKTİF PROGRAM çipi + başlık + ilerleme paneli + BAŞLA. 496dp; sheet altına 28dp biner. */
@Composable
fun ProgramsHero(active: Program?, today: ProgramTodayInfo?, onStart: () -> Unit, onOpen: (() -> Unit)? = null) {
    val theme = LocalAppTheme.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Kompakt hero: yükseklik içerikten gelir (boş alan yok); kapak içeriği saran katmanda.
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        Box(
            Modifier.matchParentSize()
                // Alt ~%40'ta alfa -> 0: kapak ortak SinemaBackdrop'a yumuşakça söner
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0.62f to Sinema.scrim(1f), 1f to Color.Transparent),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
                .then(if (active != null && onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
        ) {
            SinemaTypeCover(
                icon = coverIconFor(active?.name), hue = coverHueFor(active?.name), modifier = Modifier.fillMaxSize(),
                ghostSize = 88.dp, underStatusBar = true
            )
        }
        if (active != null) {
            val heroHue = coverHueFor(active.name)
            val workoutDays = active.days.count { !it.isRestDay }
            val totalExercises = active.days.sumOf { it.exercises.size }
            CompositionLocalProvider(LocalSinemaHaze provides null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = statusTop + 16.dp, bottom = 46.dp)) {
                    // AKTİF PROGRAM çipi
                    Row(
                        Modifier.height(28.dp).sinemaRaised(RoundedCornerShape(SinemaRadius.R12), SinemaRaise.Mini, fill = heroHue).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(SinemaIcons.Bolt, null, tint = Sinema.Ink, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            theme.t("AKTİF PROGRAM", "ACTIVE PROGRAM"), color = Sinema.Ink,
                            fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    // başlık: ikon bölgesine (sağ-üst) taşmaz
                    Text(
                        active.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.01).em, lineHeight = 1.12.em, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 40.dp)
                    )
                    // başlık altı ince program renkli çizgi
                    Box(Modifier.padding(top = 8.dp).width(48.dp).height(3.dp).clip(CircleShape).background(heroHue))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeroInfoChip(androidx.compose.material.icons.Icons.Rounded.CalendarMonth, theme.t("$workoutDays gün", "$workoutDays days"), heroHue)
                        if (totalExercises > 0) HeroInfoChip(androidx.compose.material.icons.Icons.Rounded.FitnessCenter, theme.t("$totalExercises egzersiz", "$totalExercises exercises"), heroHue)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (today != null) TodayPanel(today, heroHue)
                    SinemaGoButton(
                        label = theme.t("Başla", "Start"), icon = SinemaIcons.Play, onClick = onStart
                    )
                }
            }
        }
    }
}

/** Hero bilgi çipi: ikon + metin, yarı saydam beyaz hap. */
@Composable
private fun HeroInfoChip(icon: ImageVector, text: String, hue: Color) {
    Row(
        Modifier.height(28.dp).clip(CircleShape).background(hue.copy(0.24f)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun TodayPanel(info: ProgramTodayInfo, hue: Color) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .sinemaCapsuleGlass(RoundedCornerShape(SinemaRadius.R28), fillTop = 0.20f, fillBottom = 0.07f, blur = 22.dp, overlay = hue.copy(alpha = 0.10f),
                edgeColors = listOf(hue.copy(0.75f), hue.copy(0.12f)), glow = hue.copy(0.25f))
            .drawBehind {
                // sol kenarda kısa dikey program şeridi
                drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Transparent, 0.5f to hue.copy(0.85f), 1f to Color.Transparent, startY = size.height * 0.2f, endY = size.height * 0.8f), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height * 0.6f))
            }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Sol: BUGÜNÜN halkası (0/4); program toplamı ikincil metinde
            val dayFrac = if (info.todayExercises > 0) (info.todayCompleted.toFloat() / info.todayExercises).coerceIn(0f, 1f) else 0f
            val fill = com.cosmibit.profitness.core.theme.rememberSinemaFill(dayFrac)
            val ring = hue
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 6.dp.toPx()
                    val d = size.minDimension - sw
                    val tl = Offset(sw / 2, sw / 2)
                    drawArc(Color.White.copy(0.22f), -90f, 360f, false, tl, Size(d, d), style = Stroke(sw))
                    val frac = fill.value
                    if (frac > 0f) drawArc(ring, -90f, 360f * frac, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (info.todayIsRest) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Bedtime, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(28.dp))
                    } else {
                        Text("${info.todayCompleted}/${info.todayExercises}", color = Color.White, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val todayLabel = info.week.firstOrNull { it.isToday }?.label.orEmpty()
                Text(
                    (theme.t("Bugün", "Today") + if (todayLabel.isNotBlank()) " · $todayLabel" else "").sinemaUpper(theme),
                    color = theme.accentHiTextColor, style = Sinema.Label
                )
                Text(
                    if (info.todayIsRest) theme.t("Dinlenme günü", "Rest day") else info.todayTitle,
                    color = Color.White, fontSize = SinemaType.Headline, fontWeight = FontWeight.ExtraBold,
                    lineHeight = 1.15.em, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (!info.todayIsRest) {
                    val minutes = if (info.todayMinutes > 0) " · ~${info.todayMinutes} ${theme.t("dk", "min")}" else ""
                    Text(
                        theme.t("${info.todayExercises} egzersiz", "${info.todayExercises} exercises") + minutes,
                        color = Color.White.copy(0.78f), fontSize = SinemaType.Caption, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        // Gün seçici: 7 eşit sütun, >=48dp; durumlar: seçili (amber tint), tamamlandı (mercan tik), dinlenme (soluk + ay), diğer (nokta)
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            info.week.forEach { c ->
                val shape = RoundedCornerShape(SinemaRadius.R12)
                val stateDesc = when {
                    c.isToday -> theme.t("seçili gün", "selected day")
                    c.isDone -> theme.t("tamamlandı", "completed")
                    c.isRest -> theme.t("dinlenme günü", "rest day")
                    else -> theme.t("antrenman günü", "training day")
                }
                Column(
                    Modifier.weight(1f).heightIn(min = 52.dp)
                        .graphicsLayer { alpha = if (c.isRest && !c.isToday) 0.55f else 1f }
                        .then(if (c.isToday) Modifier.sinemaRaised(shape, SinemaRaise.Mini, tint = accent) else Modifier.sinemaRaised(shape, SinemaRaise.Mini, top = Color.White.copy(0.22f), bottom = Color.White.copy(0.07f)))
                        .semantics(mergeDescendants = true) { contentDescription = "${c.label}, $stateDesc" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    val actualScale = with(LocalDensity.current) { 12.sp.toDp().value } / 12f
                    Text(
                        c.label, color = if (c.isToday) accent else Color.White.copy(0.88f),
                        fontSize = cappedDisplaySp(12f, actualScale, 1.3f).sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Clip
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        c.isDone -> Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, tint = theme.accentHi, modifier = Modifier.size(14.dp))
                        c.isRest -> Icon(androidx.compose.material.icons.Icons.Rounded.Bedtime, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                        else -> Box(Modifier.size(5.dp).clip(CircleShape).background(if (c.isToday) accent else Color.White.copy(0.45f)))
                    }
                }
            }
        }
    }
}

// ── Sheet bileşenleri ────────────────────────────────────────────────────────────

/** Çizim .prs: sayfa renginde, üstü yuvarlak, vurgu gölgesi + hafif hale. Çocuklar 16dp yan boşlukla. */
@Composable
fun ProgramsSheet(content: @Composable () -> Unit) {
    // Yüzeysiz: içerik doğrudan ortak SinemaBackdrop üstünde; kapak fotoğrafı ona söner (bant/kenar/yerel ışık yok)
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) { content() }
}

@Composable
fun ProgramsSection(title: String, count: Int?, topGap: Dp = 26.dp) {
    val theme = LocalAppTheme.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = topGap, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        com.cosmibit.profitness.core.theme.SinemaLabelDot()
        Spacer(Modifier.width(8.dp))
        Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f))
        if (count != null) Text("$count", color = Sinema.textSecondary(theme), style = Sinema.Label)
    }
}

private fun Modifier.insetLook(theme: AppThemeState, shape: androidx.compose.ui.graphics.Shape): Modifier = this.drawWithContent {
    drawContent()
    // çizim --ins: üstten iç gölge + altta ince beyaz çizgi (Compose'ta inset gölge yok → degrade)
    val h = 9.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Sinema.scrim(0.6f), Color.Transparent), 0f, h), size = Size(size.width, h))
    drawRect(Color.White.copy(0.10f), topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
}

@Composable
private fun raiseBrush(theme: AppThemeState): Brush =
    Brush.linearGradient(listOf(Color(0xFF26262B), Color(0xFF141416)), Offset(0f, 0f), Offset(120f, 320f))

/** AI (kabarık vurgu) + Manuel (gömülü) blokları. */
@Composable
fun ProgramsMakeRow(onAi: () -> Unit, onManual: () -> Unit) {
    val theme = LocalAppTheme.current
    val shape = RoundedCornerShape(SinemaRadius.R28)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI: opak yüzey + c3 ikon diski (tam c1 degrade kalktı)
        Box(
            Modifier.weight(1f).fillMaxHeight().heightIn(min = 148.dp)
                .sinemaRaised(shape, SinemaRaise.Card, fill = theme.accentHi)
                .clickable(role = Role.Button, onClick = onAi)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.12f)), contentAlignment = Alignment.Center) {
                Icon(SinemaIcons.Spark, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                Text(theme.t("AI ile\nOluştur", "Create\nwith AI"), color = theme.effectiveOnAccentColor, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, lineHeight = 1.15.em, letterSpacing = (-0.01).em)
                Text(theme.t("Sana özel plan", "A plan made for you"), color = theme.effectiveOnAccentColor.copy(0.72f), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
            }
        }
        // Manuel
        Box(
            Modifier.weight(1f).fillMaxHeight().heightIn(min = 148.dp)
                .sinemaRaised(shape, SinemaRaise.Card)
                .clickable(role = Role.Button, onClick = onManual)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(theme.accentHi.copy(0.16f)),
                contentAlignment = Alignment.Center
            ) { Icon(SinemaIcons.Plus, null, tint = theme.accentHi, modifier = Modifier.size(22.dp)) }
            Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                Text(theme.t("Manuel\nOluştur", "Create\nManually"), color = Sinema.text(theme), fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold, lineHeight = 1.15.em, letterSpacing = (-0.01).em)
                Text(theme.t("Kendin kur", "Build your own"), color = Sinema.text(theme).copy(0.7f), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
            }
        }
    }
}

@Composable
private fun GlassTag(text: String, hue: Color) {
    Box(
        Modifier.height(24.dp).sinemaCapsuleGlass(RoundedCornerShape(SinemaRadius.R12), blur = 12.dp, overlay = hue.copy(alpha = 0.26f)).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
}

private fun cardBorderShadow(theme: AppThemeState) = Color.Black.copy(0.6f)

/** Kayıtlı program kartı (çizim .sv): 226x288 foto kart, tür + ⋮ menü, ad, çipler, Aktif et. İşlevler: etkinleştir, düzenle, paylaş, sil. */
@Composable
fun SavedProgramCard(
    program: Program,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit = onEdit,
    showTypeBadge: Boolean = true
) {
    val theme = LocalAppTheme.current
    val badge = theme.accentHiTextColor   // c2 açık = rozet/ikon; ekranda ≤3 öğe → yalnız ilk kartta
    val shape = RoundedCornerShape(SinemaRadius.R28)
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val (typeIcon, typeLabel) = when (program.type) {
        ProgramType.TEMPLATE -> SinemaIcons.List to theme.t("Şablon", "Template")
        ProgramType.AI -> SinemaIcons.Spark to "AI"
        ProgramType.MANUAL -> SinemaIcons.Plus to theme.t("Manuel", "Manual")
    }
    val workoutDays = program.days.count { !it.isRestDay }
    val totalExercises = program.days.sumOf { it.exercises.size }
    val coral = theme.supportAlert

    if (confirmDelete) {
        com.cosmibit.profitness.presentation.components.SinemaDialog(
            onDismiss = { confirmDelete = false },
            title = theme.t("Programı Sil", "Delete Program"),
            body = theme.t("\"${program.name}\" silinecek. Bu işlem geri alınamaz.", "\"${program.name}\" will be deleted. This action cannot be undone."),
            confirmLabel = theme.t("Sil", "Delete"),
            confirmIcon = Icons.Rounded.DeleteOutline,
            onConfirm = { confirmDelete = false; onDelete() },
            dismissLabel = theme.t("İptal", "Cancel")
        )
    }

    Box(
        Modifier.width(226.dp).height(288.dp)
            .sinemaRaised(shape, SinemaRaise.Card)
            .then(if (program.isActive) Modifier.sinemaSoftRing(coverHueFor(program.name), shape) else Modifier)
            .clickable(role = Role.Button, onClick = onOpen)
    ) {
        Box(Modifier.fillMaxSize()) {
            SinemaTypeCover(icon = coverIconFor(program.name), hue = coverHueFor(program.name), modifier = Modifier.fillMaxSize(), ghostSize = 150.dp, ghostAlignment = Alignment.CenterEnd, ghostLift = 36.dp)
        }
        CompositionLocalProvider(LocalSinemaHaze provides null) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                val typeTint = badge
                Icon(typeIcon, null, tint = typeTint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(typeLabel.sinemaUpper(theme), color = typeTint, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em, modifier = Modifier.weight(1f), maxLines = 1)
                Box {
                    // dokunma alanı 48dp, görsel cam disk 32dp
                    Box(
                        Modifier.size(48.dp).clickable(role = Role.Button) { menu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(32.dp).sinemaCapsuleGlass(CircleShape, blur = 16.dp), contentAlignment = Alignment.Center) {
                            Icon(SinemaIcons.More, theme.t("Diğer", "More"), tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = menu, onDismissRequest = { menu = false },
                        containerColor = Sinema.surface(theme), shape = RoundedCornerShape(SinemaRadius.R20),
                        tonalElevation = 0.dp, shadowElevation = 12.dp,
                        border = null
                    ) {
                        DropdownMenuItem(
                            text = { Text(theme.t("Düzenle", "Edit"), color = Sinema.text(theme), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Sinema.text(theme)) },
                            onClick = { menu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text(theme.t("Paylaş", "Share"), color = Sinema.text(theme), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null, tint = Sinema.text(theme)) },
                            onClick = { menu = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text(theme.t("Programı sil", "Delete program"), color = coral, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = coral) },
                            onClick = { menu = false; confirmDelete = true }
                        )
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp)) {
                Text(program.name.sinemaUpper(theme), color = Color.White, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.Bold, lineHeight = 1.15.em, letterSpacing = (-0.01).em, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassTag(theme.t("$workoutDays gün", "$workoutDays days"), coverHueFor(program.name))
                    if (totalExercises > 0) GlassTag(theme.t("$totalExercises egzersiz", "$totalExercises exercises"), coverHueFor(program.name))
                }
                if (program.isActive) {
                    Row(
                        Modifier.fillMaxWidth().height(42.dp)
                            .sinemaCapsuleGlass(RoundedCornerShape(SinemaRadius.R20), edge = Color.White.copy(0.3f), fillAlpha = 0.4f, blur = 12.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(SinemaIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(theme.t("Aktif", "Active"), color = Color.White, fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    ActPill(theme.t("Aktif et", "Activate"), SinemaIcons.Play, filled = true, onClick = onSetActive, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** 42dp ikincil hap (nötr cam; c1 yalnız ekranın tek birincil düğmesinde). */
@Composable
fun ActPill(label: String, icon: ImageVector, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val on = Color.White
    val shape = RoundedCornerShape(SinemaRadius.R20)
    // İkincil eylem: nötr cam (ekranda tek c1 düğme = BAŞLA)
    Row(
        modifier.height(42.dp)
            .sinemaRaised(shape, SinemaRaise.Mini)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = theme.accentHiTextColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = on, fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Filtre hapı (çizim .prf button): kabarık gradyan zemin; seçiliyse vurgu. */
@Composable
fun ProgramFilterPill(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(SinemaRadius.R16)
    val fg = if (selected) accent else Sinema.text(theme)
    Box(Modifier.heightIn48().clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(
            Modifier.height(36.dp)
                .then(Modifier.sinemaRaised(shape, SinemaRaise.Mini, tint = if (selected) accent else null))
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
            Text(label, color = fg, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em, maxLines = 1)
        }
    }
}

private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(48.dp))

/** Hazır program kartı (çizim .hz): 176dp foto kart, etiket, ad, çipler, Ekle. */
@Composable
fun ReadyProgramCard(
    title: String,
    tag: String,
    chips: List<String>,
    iconKey: String,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(SinemaRadius.R28)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp).height(176.dp)
            .sinemaRaised(shape, SinemaRaise.Card)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize()) {
            SinemaTypeCover(icon = coverIconFor(iconKey), hue = coverHueFor(iconKey), modifier = Modifier.fillMaxSize(), ghostSize = 150.dp, ghostAlignment = Alignment.CenterEnd, ghostLift = 36.dp)
        }
        CompositionLocalProvider(LocalSinemaHaze provides null) {
            Text(tag.sinemaUpper(theme), color = Color.White.copy(0.85f), fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em, maxLines = 1,
                modifier = Modifier.padding(start = 14.dp, top = 14.dp))
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = SinemaType.Headline, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em, lineHeight = 1.1.em, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips.forEach { GlassTag(it, coverHueFor(iconKey)) } }
                }
                Spacer(Modifier.width(10.dp))
                ActPill(theme.t("Ekle", "Add"), SinemaIcons.Plus, filled = false, onClick = onClick)
            }
        }
    }
}

@Composable
fun SavedProgramsRow(programs: List<Program>, onSetActive: (Program) -> Unit, onDelete: (Program) -> Unit, onEdit: (Program) -> Unit, onShare: (Program) -> Unit, onOpen: (Program) -> Unit = onEdit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(programs, key = { _, p -> p.id }) { i, p ->
            Box(Modifier.animateItem().sinemaListEnter(i)) {
                SavedProgramCard(p, { onSetActive(p) }, { onDelete(p) }, { onEdit(p) }, { onShare(p) }, { onOpen(p) }, showTypeBadge = i == 0)
            }
        }
    }
}

@Composable
fun ProgramsTopScrim() {
    Box(Modifier.fillMaxWidth().height(56.dp).background(Brush.verticalGradient(listOf(Sinema.scrim(0.6f), Color.Transparent))))
}
