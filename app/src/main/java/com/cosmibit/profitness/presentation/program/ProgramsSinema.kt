package com.cosmibit.profitness.presentation.program

import com.cosmibit.profitness.core.theme.sinemaGlow
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
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
import com.cosmibit.profitness.core.theme.SinemaGoButton
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.domain.model.Program
import com.cosmibit.profitness.domain.model.ProgramType
import com.cosmibit.profitness.presentation.workout.SceneImage
import com.cosmibit.profitness.presentation.workout.WorkoutDayState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Antrenman sekmesindeki aktif programın bugünkü özeti (Programlar kapağı için). Veri yoksa null → panel gizlenir. */
data class ProgramTodayInfo(
    val completed: Int,
    val total: Int,
    val todayTitle: String,
    val todayExercises: Int,
    val todayIsRest: Boolean,
    val week: List<WeekCell>
)

data class WeekCell(val label: String, val isToday: Boolean, val isRest: Boolean)

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
        week = days.mapIndexed { i, d ->
            WeekCell(com.cosmibit.profitness.presentation.workout.localizedWorkoutDayLabel(d.day.day, theme), i == selectedIdx, d.day.isRestDay)
        }
    )
}

private val SheetTopRadius = 28.dp

// ── Kapak (çizim .prh/.prl) ──────────────────────────────────────────────────────

/** Tam boy foto kapak: foto + karartma + AKTİF PROGRAM çipi + başlık + ilerleme paneli + BAŞLA. 496dp; sheet altına 28dp biner. */
@Composable
fun ProgramsHero(active: Program?, today: ProgramTodayInfo?, onStart: () -> Unit, onOpen: (() -> Unit)? = null) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val imageUrl = remember(active) {
        active?.days?.asSequence()?.flatMap { it.exercises.asSequence() }
            ?.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    }.ifBlank { categoryImageUrl("") }
    val haze = remember { HazeState() }
    val motion = rememberSinemaMotionOn()
    val kb by rememberInfiniteTransition(label = "prog_kb").animateFloat(
        1f, if (motion) 1.08f else 1f, infiniteRepeatable(tween(Sinema.KenBurnsMs, easing = LinearEasing), RepeatMode.Reverse), label = "kb"
    )
    Box(Modifier.fillMaxWidth().height(496.dp).clipToBounds()) {
        Box(
            Modifier.fillMaxSize().hazeSource(haze)
                // Foto alt ~%40'ta alfa → 0: kapak ortak SinemaBackdrop'a yumuşakça söner (katı sayfa rengi bandı yok)
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0.58f to Color.Black, 1f to Color.Transparent),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
                .then(if (active != null && onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
        ) {
            // Foto 640dp yüksekliğinde, üstten hizalı, -110dp kaydırılmış (çizim)
            Box(Modifier.fillMaxWidth().requiredHeight(640.dp).offset(y = (-110).dp).graphicsLayer { scaleX = kb; scaleY = kb }) {
                SceneImage(url = imageUrl, modifier = Modifier.fillMaxSize(), requestWidthPx = 1080, requestHeightPx = 1600,
                    alignment = Alignment.TopCenter, fallbackGradient = true)
            }
            // karartma: .5 → 0 @%24 → .12 @%42 → .88 @%86
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(0.5f), 0.24f to Color.Transparent,
                        0.42f to Color.Black.copy(0.12f), 0.80f to Color.Black.copy(0.88f), 1f to Color.Black.copy(0.88f)
                    )
                )
            )
            // çizimdeki foto karanlık (dk filtre); gerçek fotoğraf daha parlak olduğundan genel hafif karartma
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.22f)))
        }
        if (active != null) {
            // AKTİF PROGRAM çipi
            Row(
                Modifier.padding(start = 16.dp, top = statusTop + 16.dp).height(28.dp)
                    .sinemaGlow(accent.copy(0.4f), 18.dp, 8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).background(accent).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(SinemaIcons.Bolt, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    theme.t("AKTİF PROGRAM", "ACTIVE PROGRAM"), color = theme.effectiveOnAccentColor,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, maxLines = 1
                )
            }
            val workoutDays = active.days.count { !it.isRestDay }
            val totalExercises = active.days.sumOf { it.exercises.size }
            val info = theme.t(
                "$workoutDays antrenman günü" + if (totalExercises > 0) " · $totalExercises egzersiz" else "",
                "$workoutDays training days" + if (totalExercises > 0) " · $totalExercises exercises" else ""
            )
            CompositionLocalProvider(LocalSinemaHaze provides haze) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 46.dp)) {
                    Text(
                        active.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.01).em, lineHeight = 1.12.em, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                    Text(info, color = Color.White.copy(0.75f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                    if (today != null) TodayPanel(today)
                    SinemaGoButton(
                        label = theme.t("BAŞLA", "START"), icon = SinemaIcons.Play, onClick = onStart
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayPanel(info: ProgramTodayInfo) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .sinemaCapsuleGlass(RoundedCornerShape(26.dp), edge = Color.White.copy(0.2f), fillAlpha = 0.38f, blur = 22.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val frac = (info.completed.toFloat() / info.total).coerceIn(0f, 1f)
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 6.dp.toPx()
                    val d = size.minDimension - sw
                    val tl = Offset(sw / 2, sw / 2)
                    drawArc(Color.White.copy(0.22f), -90f, 360f, false, tl, Size(d, d), style = Stroke(sw))
                    if (frac > 0f) {
                        drawArc(accent.copy(0.35f), -90f, 360f * frac, false, tl, Size(d, d), style = Stroke(sw + 6.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(accent, -90f, 360f * frac, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                }
                Text("${info.completed}/${info.total}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.t("Bugün", "Today").sinemaUpper(theme), color = accent, style = Sinema.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                Text(
                    if (info.todayIsRest) theme.t("Dinlenme", "Rest") else info.todayTitle,
                    color = Color.White, fontSize = if (info.todayTitle.length > 9) 26.sp else 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04).em,
                    lineHeight = 1.05.em, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!info.todayIsRest) {
                    Text(theme.t("${info.todayExercises} egzersiz", "${info.todayExercises} exercises"), color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            info.week.forEach { c ->
                val shape = RoundedCornerShape(14.dp)
                val fg = if (c.isToday) onAccent else Color.White.copy(0.85f)
                Column(
                    Modifier.weight(1f).height(42.dp)
                        .graphicsLayer { alpha = if (c.isRest && !c.isToday) 0.45f else 1f }
                        .then(if (c.isToday) Modifier.sinemaGlow(accent.copy(0.4f), 16.dp, 8.dp, shape) else Modifier)
                        .clip(shape)
                        .background(if (c.isToday) accent else Color.White.copy(0.09f)),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Text(c.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (c.isToday) onAccent else Color.White.copy(0.4f)))
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = topGap, bottom = 10.dp)) {
        Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, modifier = Modifier.weight(1f))
        if (count != null) Text("$count", color = Sinema.textSecondary(theme), style = Sinema.Label)
    }
}

private fun Modifier.insetLook(theme: AppThemeState, shape: androidx.compose.ui.graphics.Shape): Modifier = this.drawWithContent {
    drawContent()
    // çizim --ins: üstten iç gölge + altta ince beyaz çizgi (Compose'ta inset gölge yok → degrade)
    val h = 9.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent), 0f, h), size = Size(size.width, h))
    drawRect(Color.White.copy(0.10f), topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
}

@Composable
private fun raiseBrush(theme: AppThemeState): Brush =
    Brush.linearGradient(listOf(Color(0xFF26262B), Color(0xFF141416)), Offset(0f, 0f), Offset(120f, 320f))

/** AI (kabarık vurgu) + Manuel (gömülü) blokları. */
@Composable
fun ProgramsMakeRow(onAi: () -> Unit, onManual: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val on = theme.effectiveOnAccentColor
    val shape = RoundedCornerShape(28.dp)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI
        Box(
            Modifier.weight(1f).height(148.dp)
                .sinemaGlow(accent.copy(0.4f), 14.dp, 8.dp, shape)
                .clip(shape)
                .background(Brush.linearGradient(listOf(lerp(Color.White, accent, 0.78f), accent, lerp(accent, Color.Black, 0.4f)), Offset(0f, 0f), Offset(260f, 420f)))
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.55f), Color.Transparent), 0f, 3.dp.toPx()), size = Size(size.width, 3.dp.toPx()))
                }
                .clickable(role = Role.Button, onClick = onAi)
        ) {
            Icon(SinemaIcons.Spark, null, tint = on.copy(0.2f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-14).dp).size(110.dp).graphicsLayer { rotationZ = 12f })
            Box(Modifier.padding(14.dp).size(42.dp).clip(CircleShape).background(Color.White.copy(0.35f)), contentAlignment = Alignment.Center) {
                Icon(SinemaIcons.Spark, null, tint = on, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(theme.t("AI ile\nOluştur", "Create\nwith AI"), color = on, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.15.em, letterSpacing = (-0.01).em)
                Text(theme.t("Sana özel plan", "A plan made for you"), color = on.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        // Manuel
        Box(
            Modifier.weight(1f).height(148.dp)
                .clip(shape)
                .background(raiseBrush(theme))
                .insetLook(theme, shape)
                .border(1.dp, Sinema.line(theme), shape)
                .clickable(role = Role.Button, onClick = onManual)
        ) {
            Icon(SinemaIcons.Plus, null, tint = accent.copy(0.3f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-14).dp).size(110.dp).graphicsLayer { rotationZ = 12f })
            Box(
                Modifier.padding(14.dp).size(42.dp)
                    .sinemaGlow(accent.copy(0.4f), 16.dp, 8.dp, CircleShape)
                    .clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) { Icon(SinemaIcons.Plus, null, tint = on, modifier = Modifier.size(22.dp)) }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(theme.t("Manuel\nOluştur", "Create\nManually"), color = Sinema.text(theme), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.15.em, letterSpacing = (-0.01).em)
                Text(theme.t("Kendin kur", "Build your own"), color = Sinema.text(theme).copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun GlassTag(text: String) {
    Box(
        Modifier.height(24.dp).sinemaCapsuleGlass(RoundedCornerShape(12.dp), blur = 12.dp).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
}

private fun cardBorderShadow(theme: AppThemeState) = Color.Black.copy(0.6f)

/** Kayıtlı program kartı (çizim .sv): 226x288 foto kart, tür + ⋮ menü, ad, çipler, Aktif et. İşlevler: etkinleştir, düzenle, paylaş, sil. */
@Composable
fun SavedProgramCard(
    program: Program,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val on = theme.effectiveOnAccentColor
    val shape = RoundedCornerShape(28.dp)
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val photo = remember(program) {
        program.days.asSequence().flatMap { it.exercises.asSequence() }.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    }.ifBlank { categoryImageUrl("") }
    val (typeIcon, typeLabel) = when (program.type) {
        ProgramType.TEMPLATE -> SinemaIcons.List to theme.t("Şablon", "Template")
        ProgramType.AI -> SinemaIcons.Spark to "AI"
        ProgramType.MANUAL -> SinemaIcons.Plus to theme.t("Manuel", "Manual")
    }
    val workoutDays = program.days.count { !it.isRestDay }
    val totalExercises = program.days.sumOf { it.exercises.size }
    val coral = com.cosmibit.profitness.core.theme.CardCoral

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Sinema.surface(theme),
            title = { Text(theme.t("Programı Sil", "Delete Program"), color = Sinema.text(theme), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    theme.t("\"${program.name}\" silinecek. Bu işlem geri alınamaz.", "\"${program.name}\" will be deleted. This action cannot be undone."),
                    color = Sinema.textSecondary(theme), fontSize = 14.sp
                )
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(theme.t("Sil", "Delete"), color = coral, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(theme.t("İptal", "Cancel"), color = Sinema.textSecondary(theme)) } }
        )
    }

    val haze = remember { HazeState() }
    Box(
        Modifier.width(226.dp).height(288.dp)
            .shadow(16.dp, shape, ambientColor = cardBorderShadow(theme), spotColor = cardBorderShadow(theme))
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(0.2f), shape)
            .clickable(role = Role.Button, onClick = onEdit)
    ) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            SceneImage(url = photo, modifier = Modifier.fillMaxSize(), requestWidthPx = 452, requestHeightPx = 576, fallbackGradient = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(0.3f), 0.35f to Color.Transparent, 0.82f to Color.Black.copy(0.9f), 1f to Color.Black.copy(0.9f))))
        }
        CompositionLocalProvider(LocalSinemaHaze provides haze) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(typeIcon, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(typeLabel.sinemaUpper(theme), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em, modifier = Modifier.weight(1f), maxLines = 1)
                Box {
                    Box(
                        Modifier.size(32.dp).sinemaCapsuleGlass(CircleShape, blur = 16.dp)
                            .clickable(role = Role.Button) { menu = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(SinemaIcons.More, theme.t("Diğer", "More"), tint = Color.White, modifier = Modifier.size(18.dp)) }
                    DropdownMenu(
                        expanded = menu, onDismissRequest = { menu = false },
                        containerColor = Sinema.surface(theme), shape = RoundedCornerShape(20.dp),
                        tonalElevation = 0.dp, shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Sinema.line(theme))
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
                Text(program.name.sinemaUpper(theme), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 1.15.em, letterSpacing = (-0.01).em, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassTag(theme.t("$workoutDays gün", "$workoutDays days"))
                    if (totalExercises > 0) GlassTag(theme.t("$totalExercises egzersiz", "$totalExercises exercises"))
                }
                if (program.isActive) {
                    Row(
                        Modifier.fillMaxWidth().height(42.dp)
                            .sinemaGlow(accent.copy(0.4f), 14.dp, 8.dp, RoundedCornerShape(21.dp))
                            .sinemaCapsuleGlass(RoundedCornerShape(21.dp), edge = accent, fillAlpha = 0.4f, blur = 12.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(SinemaIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(theme.t("Aktif", "Active"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    ActPill(theme.t("Aktif et", "Activate"), SinemaIcons.Play, filled = true, onClick = onSetActive, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Çizim .act: 42dp vurgu hapı (üst iç parlama + vurgu gölgesi). */
@Composable
fun ActPill(label: String, icon: ImageVector, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val on = theme.effectiveOnAccentColor
    val shape = RoundedCornerShape(21.dp)
    Row(
        modifier.height(42.dp)
            .sinemaGlow(accent.copy(0.4f), 18.dp, 8.dp, shape)
            .clip(shape).background(accent)
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.4f), Color.Transparent), 0f, 3.dp.toPx()), size = Size(size.width, 3.dp.toPx()))
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = on, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = on, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Filtre hapı (çizim .prf button): kabarık gradyan zemin; seçiliyse vurgu. */
@Composable
fun ProgramFilterPill(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(18.dp)
    val fg = if (selected) theme.effectiveOnAccentColor else Sinema.text(theme)
    Box(Modifier.heightIn48().clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(
            Modifier.height(36.dp)
                .then(if (selected) Modifier.sinemaGlow(accent.copy(0.4f), 16.dp, 8.dp, shape) else Modifier)
                .clip(shape)
                .then(if (selected) Modifier.background(accent) else Modifier.background(raiseBrush(theme)).insetLook(theme, shape).border(1.dp, Sinema.line(theme), shape))
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
            Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em, maxLines = 1)
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
    photoUrl: String,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val shape = RoundedCornerShape(28.dp)
    val haze = remember { HazeState() }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp).height(176.dp)
            .shadow(16.dp, shape, ambientColor = cardBorderShadow(theme), spotColor = cardBorderShadow(theme))
            .clip(shape).background(Color.Black).border(1.dp, Color.White.copy(0.2f), shape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            SceneImage(url = photoUrl, modifier = Modifier.fillMaxSize(), requestWidthPx = 1000, requestHeightPx = 480, fallbackGradient = true)
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.Black.copy(0.7f), Color.Black.copy(0.1f)), Offset(0f, 0f), Offset(900f, 300f))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color.Black.copy(0.85f))))
        }
        CompositionLocalProvider(LocalSinemaHaze provides haze) {
            Text(tag.sinemaUpper(theme), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em, maxLines = 1,
                modifier = Modifier.padding(start = 14.dp, top = 14.dp))
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em, lineHeight = 1.1.em, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips.forEach { GlassTag(it) } }
                }
                Spacer(Modifier.width(10.dp))
                ActPill(theme.t("Ekle", "Add"), SinemaIcons.Plus, filled = false, onClick = onClick)
            }
        }
    }
}

@Composable
fun SavedProgramsRow(programs: List<Program>, onSetActive: (Program) -> Unit, onDelete: (Program) -> Unit, onEdit: (Program) -> Unit, onShare: (Program) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(programs, key = { it.id }) { p ->
            SavedProgramCard(p, { onSetActive(p) }, { onDelete(p) }, { onEdit(p) }, { onShare(p) })
        }
    }
}

@Composable
fun ProgramsTopScrim() {
    Box(Modifier.fillMaxWidth().height(56.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent))))
}
