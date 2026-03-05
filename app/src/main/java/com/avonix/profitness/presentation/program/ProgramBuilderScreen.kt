package com.avonix.profitness.presentation.program

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.coroutines.delay

// ── Data ─────────────────────────────────────────────────────────────────────

data class SavedProgram(
    val name: String,
    val days: Int,
    val focus: String,
    val icon: ImageVector
)

enum class ProgramCategory(val trLabel: String, val color: Color, val icon: ImageVector) {
    ALL("TÜMÜ",          Snow,       Icons.Rounded.GridView),
    MUSCLE("KAS",        CardPurple, Icons.Rounded.FitnessCenter),
    FAT_LOSS("YAĞ YAKIMI", CardCoral, Icons.Rounded.LocalFireDepartment),
    STRENGTH("GÜÇ",     Amber,      Icons.Rounded.Bolt),
    ENDURANCE("DAYANIKLILIK", CardCyan, Icons.Rounded.DirectionsRun),
    BEGINNER("BAŞLANGIÇ", CardGreen, Icons.Rounded.StarOutline)
}

/** Returns the localised display label for this category. */
@Composable
fun ProgramCategory.localizedLabel(): String {
    val strings = LocalAppTheme.current.strings
    return when (this) {
        ProgramCategory.ALL       -> strings.progCatAll
        ProgramCategory.MUSCLE    -> strings.progCatMuscle
        ProgramCategory.FAT_LOSS  -> strings.progCatFatLoss
        ProgramCategory.STRENGTH  -> strings.progCatStrength
        ProgramCategory.ENDURANCE -> strings.progCatEndurance
        ProgramCategory.BEGINNER  -> strings.progCatBeginner
    }
}

data class ReadyProgram(
    val title: String,
    val subtitle: String,
    val category: ProgramCategory,
    val days: Int,
    val weeks: Int,
    val level: String,
    val goal: String,
    val description: String,
    val schedule: String,
    val muscleLabels: List<String>,
    val musclePct: List<String>,
    val muscleFractions: List<Float>
)

private val READY_PROGRAMS = listOf(
    // ── KAS GELİŞİMİ ─────────────────────────────────────────────────────────
    ReadyProgram(
        "Push / Pull / Legs",
        "Hipertrofi Klasiği",
        ProgramCategory.MUSCLE, 6, 8, "Orta",
        "Kas Kütlesi",
        "Göğüs-omuz-triceps / sırt-biceps / bacak şeklinde gruplanmış 6 günlük yüksek frekanslı hipertrofi programı.",
        "📌 Paz: Göğüs · Omuz · Triceps\n📌 Sal: Sırt · Biceps · Ön Kol\n📌 Çar: Bacak + Core\n📌 Per: Göğüs · Omuz · Triceps\n📌 Cum: Sırt · Biceps · Ön Kol\n📌 Cmt: Bacak + Core",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("85%", "70%", "45%"),
        listOf(0.85f, 0.70f, 0.45f)
    ),
    ReadyProgram(
        "Arnold Split",
        "Efsane Hacim Protokolü",
        ProgramCategory.MUSCLE, 6, 12, "İleri",
        "Kas Kütlesi",
        "Arnold Schwarzenegger'ın ikonik 6 günlük split programı. Göğüs+sırt, omuz+kol, bacak yapısıyla maksimum hacim.",
        "📌 Paz: Göğüs + Sırt\n📌 Sal: Omuz + Kol\n📌 Çar: Bacak\n📌 Per: Göğüs + Sırt\n📌 Cum: Omuz + Kol\n📌 Cmt: Bacak",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("90%", "75%", "40%"),
        listOf(0.90f, 0.75f, 0.40f)
    ),
    ReadyProgram(
        "Upper / Lower Split",
        "Güç & Hacim Dengesi",
        ProgramCategory.MUSCLE, 4, 8, "Orta",
        "Kas Kütlesi",
        "Üst ve alt vücudu ayrı günlerde çalıştıran dengeli 4 günlük program. Güç ve hacim antrenmanlarını harmanlıyor.",
        "📌 Paz: Üst Vücut (Güç)\n📌 Sal: Alt Vücut (Güç)\n📌 Per: Üst Vücut (Hacim)\n📌 Cum: Alt Vücut (Hacim)",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("80%", "80%", "50%"),
        listOf(0.80f, 0.80f, 0.50f)
    ),
    ReadyProgram(
        "Bro Split",
        "Klasik 5 Gün Bölme",
        ProgramCategory.MUSCLE, 5, 8, "Orta",
        "Kas Kütlesi",
        "Her kas grubuna haftada bir tam gün ayrılan klasik 5 günlük bölme programı. Maksimum pompa ve hacim.",
        "📌 Paz: Göğüs\n📌 Sal: Sırt\n📌 Çar: Omuz\n📌 Per: Kol (Biceps + Triceps)\n📌 Cum: Bacak + Core",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("85%", "65%", "55%"),
        listOf(0.85f, 0.65f, 0.55f)
    ),
    // ── YAĞ YAKIMI ────────────────────────────────────────────────────────────
    ReadyProgram(
        "HIIT Metabolik",
        "Yüksek Yoğunluklu Yağ Yakımı",
        ProgramCategory.FAT_LOSS, 4, 6, "Orta",
        "Yağ Yakımı",
        "Yüksek yoğunluklu interval ve ağırlık kombinasyonu. Afterburn etkisiyle kalori yakımını 48 saat yüksek tutar.",
        "📌 Paz: Üst Vücut HIIT\n📌 Sal: Alt Vücut Devre\n📌 Per: Full Body HIIT\n📌 Cum: Cardio + Core",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("70%", "55%", "80%"),
        listOf(0.70f, 0.55f, 0.80f)
    ),
    ReadyProgram(
        "Fat Burn Full Body",
        "Tam Vücut Yağ Yakımı",
        ProgramCategory.FAT_LOSS, 3, 8, "Başlangıç",
        "Yağ Yakımı",
        "Başlangıç seviyesi için tasarlanmış 3 günlük tam vücut yağ yakımı programı. Süperset yapısı ve bileşik hareketler.",
        "📌 Paz: Full Body A\n📌 Çar: Full Body B\n📌 Cum: Full Body C",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("60%", "65%", "70%"),
        listOf(0.60f, 0.65f, 0.70f)
    ),
    ReadyProgram(
        "Metabolik Kondisyon",
        "Yoğun Devre Antrenmanı",
        ProgramCategory.FAT_LOSS, 5, 6, "İleri",
        "Yağ Yakımı",
        "5 günlük devre antrenman formatında yüksek metabolik talep. Hem kas korur hem de hızlı yağ yakımı sağlar.",
        "📌 Paz: Üst Push Devre\n📌 Sal: Bacak Metabolik\n📌 Çar: Üst Pull Devre\n📌 Per: Full Body EMOM\n📌 Cum: Cardio Finisher",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("75%", "65%", "75%"),
        listOf(0.75f, 0.65f, 0.75f)
    ),
    // ── GÜÇ ──────────────────────────────────────────────────────────────────
    ReadyProgram(
        "5×5 Güç",
        "Lineer Güç Gelişimi",
        ProgramCategory.STRENGTH, 3, 12, "Başlangıç",
        "Güç",
        "Squat, bench press ve deadlift odaklı klasik 5×5 metodolojisi. Lineer progresyon ile hafta hafta güç artışı.",
        "📌 Paz: Squat + Bench + Row\n📌 Çar: Squat + Overhead Press + Deadlift\n📌 Cum: Squat + Bench + Row",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("75%", "80%", "60%"),
        listOf(0.75f, 0.80f, 0.60f)
    ),
    ReadyProgram(
        "Powerlifting Temel",
        "Rekabet Gücü Protokolü",
        ProgramCategory.STRENGTH, 4, 16, "İleri",
        "Güç",
        "Rekabetçi powerlifting için squat, bench, deadlift odaklı 16 haftalık periodizasyon programı.",
        "📌 Paz: Squat Odak\n📌 Sal: Bench Press Odak\n📌 Per: Deadlift Odak\n📌 Cum: Aksesuar & Güçlendirme",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("80%", "90%", "55%"),
        listOf(0.80f, 0.90f, 0.55f)
    ),
    ReadyProgram(
        "GZCLP",
        "Hızlı Güç Birikim Protokolü",
        ProgramCategory.STRENGTH, 3, 10, "Orta",
        "Güç",
        "Tier sistemine dayalı verimli 3 günlük güç programı. Az zamanda maksimum güç artışı için optimize edilmiş.",
        "📌 Paz: Squat T1 + Bench T2 + Row T3\n📌 Sal: Bench T1 + Squat T2 + Row T3\n📌 Per: Deadlift T1 + OHP T2 + Leg T3",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("78%", "82%", "58%"),
        listOf(0.78f, 0.82f, 0.58f)
    ),
    // ── DAYANIKLILIK ──────────────────────────────────────────────────────────
    ReadyProgram(
        "Kardiyovasküler Temel",
        "Aerobik Kapasite Gelişimi",
        ProgramCategory.ENDURANCE, 4, 8, "Başlangıç",
        "Dayanıklılık",
        "Koşu, bisiklet ve kürek çekme kombinasyonu ile aerobik kapasiteyi geliştiren 4 günlük başlangıç programı.",
        "📌 Paz: Uzun Mesafe Koşu\n📌 Sal: Interval Antrenmanı\n📌 Per: Bisiklet/Kürek\n📌 Cum: Tempo Koşu",
        listOf("KARDİYO", "ALT VÜCUT", "CORE"),
        listOf("90%", "60%", "50%"),
        listOf(0.90f, 0.60f, 0.50f)
    ),
    ReadyProgram(
        "Hybrid Athlete",
        "Güç + Dayanıklılık Hibrit",
        ProgramCategory.ENDURANCE, 5, 10, "İleri",
        "Dayanıklılık",
        "Hem güç hem dayanıklılık gelişimini hedefleyen hibrit 5 günlük program. Crossfit tarzı metabolik kondisyon.",
        "📌 Paz: Güç Antrenmanı\n📌 Sal: MetCon\n📌 Çar: Aerobik Kapasite\n📌 Per: Güç + Hız\n📌 Cum: Uzun MetCon",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("75%", "75%", "70%"),
        listOf(0.75f, 0.75f, 0.70f)
    ),
    // ── BAŞLANGIÇ ────────────────────────────────────────────────────────────
    ReadyProgram(
        "Full Body Başlangıç",
        "Temel Hareket Kalıpları",
        ProgramCategory.BEGINNER, 3, 8, "Başlangıç",
        "Genel Fitness",
        "Sıfırdan başlayanlara özel 3 günlük tam vücut programı. Temel hareket örüntülerini öğrenmek için ideal.",
        "📌 Paz: Squat + Bench + Row\n📌 Çar: Deadlift + OHP + Pulldown\n📌 Cum: Full Body Devre",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("65%", "65%", "50%"),
        listOf(0.65f, 0.65f, 0.50f)
    ),
    ReadyProgram(
        "Vücut Ağırlığı",
        "Ekipmansız Fitness",
        ProgramCategory.BEGINNER, 3, 6, "Başlangıç",
        "Genel Fitness",
        "Hiç ekipman gerektirmeyen 3 günlük vücut ağırlığı programı. Her yerde, her zaman yapılabilir.",
        "📌 Paz: Üst Vücut (Şınav, Dips)\n📌 Çar: Alt Vücut (Squat, Lunge)\n📌 Cum: Core + Cardio",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("60%", "60%", "70%"),
        listOf(0.60f, 0.60f, 0.70f)
    )
)

sealed class BuilderMode {
    object Choose : BuilderMode()
    object AI : BuilderMode()
    object Manual : BuilderMode()
}

@Composable
fun ProgramBuilderScreen() {
    val savedPrograms = remember { mutableStateListOf<SavedProgram>() }
    var mode by remember { mutableStateOf<BuilderMode>(BuilderMode.Choose) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        ArchitectGrid()
        PageAccentBloom()

        Crossfade(targetState = mode, label = "builder_fade") { m ->
            when (m) {
                is BuilderMode.Choose -> BuilderChooseScreen(
                    savedPrograms = savedPrograms,
                    onMode = { mode = it }
                )
                is BuilderMode.AI -> AIBuilderScreen(
                    onBack = { mode = BuilderMode.Choose },
                    onSave = { name ->
                        savedPrograms.add(SavedProgram(name, 6, "Oracle Optimized", Icons.Rounded.AutoAwesome))
                        snackbarMsg = "\"$name\" ${strings.programSavedMsg}"
                        mode = BuilderMode.Choose
                    }
                )
                is BuilderMode.Manual -> ManualBuilderScreen(
                    onBack = { mode = BuilderMode.Choose },
                    onSave = { name ->
                        savedPrograms.add(SavedProgram(name, 4, "Custom Protocol", Icons.Rounded.Construction))
                        snackbarMsg = strings.programSavedDefault
                        mode = BuilderMode.Choose
                    }
                )
            }
        }

        snackbarMsg?.let { msg ->
            LaunchedEffect(msg) {
                delay(2500)
                snackbarMsg = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(msg, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── Architect Grid ────────────────────────────────────────────────────────────

@Composable
private fun ArchitectGrid() {
    val gridColor = SurfaceStroke.copy(0.3f)
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 40.dp.toPx()
                val strokeWidth = 0.5.dp.toPx()
                val gridPath = Path().apply {
                    var x = 0f
                    while (x <= size.width) { moveTo(x, 0f); lineTo(x, size.height); x += step }
                    var y = 0f
                    while (y <= size.height) { moveTo(0f, y); lineTo(size.width, y); y += step }
                }
                onDrawBehind { drawPath(gridPath, gridColor, style = Stroke(strokeWidth)) }
            }
    )
}

// ── Choose Screen ─────────────────────────────────────────────────────────────

@Composable
private fun BuilderChooseScreen(
    savedPrograms: List<SavedProgram>,
    onMode: (BuilderMode) -> Unit
) {
    var selectedProgram by remember { mutableStateOf<ReadyProgram?>(null) }
    var activeCategory by remember { mutableStateOf(ProgramCategory.ALL) }

    selectedProgram?.let { prog ->
        ProgramDetailDialog(program = prog, onDismiss = { selectedProgram = null })
    }

    val filtered = remember(activeCategory) {
        if (activeCategory == ProgramCategory.ALL) READY_PROGRAMS
        else READY_PROGRAMS.filter { it.category == activeCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 72.dp, end = 24.dp, bottom = 28.dp)
            ) {
                Text(
                    "PROGRAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraLight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "STUDIO",
                    style = MaterialTheme.typography.displayLarge,
                    color = Snow,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    LocalAppTheme.current.strings.programStudioSub,
                    color = Mist,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // ── Quick Create Buttons ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chooseStrings = LocalAppTheme.current.strings
                QuickCreateButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AutoAwesome,
                    label = chooseStrings.createWithAI,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onMode(BuilderMode.AI) }
                )
                QuickCreateButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Draw,
                    label = chooseStrings.createManually,
                    accent = CardCyan,
                    onClick = { onMode(BuilderMode.Manual) }
                )
            }
        }

        // ── Active Programs ───────────────────────────────────────────────────
        val sectionStrings = LocalAppTheme.current.strings
        if (savedPrograms.isNotEmpty()) {
            item {
                SectionLabel(sectionStrings.activeProtocols, MaterialTheme.colorScheme.primary)
            }
            items(savedPrograms) { prog ->
                SavedProgramTile(prog)
            }
        }

        // ── Ready Programs Header ─────────────────────────────────────────────
        item {
            SectionLabel(sectionStrings.readyPrograms, TextMuted)
        }

        // ── Category Tabs ─────────────────────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ProgramCategory.values()) { cat ->
                    CategoryChip(
                        category = cat,
                        selected = cat == activeCategory,
                        onClick = { activeCategory = cat }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Program Cards ─────────────────────────────────────────────────────
        items(filtered) { prog ->
            ProgramCard(
                program = prog,
                onClick = { selectedProgram = prog }
            )
        }
    }
}

// ── Section Label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 36.dp, end = 24.dp, bottom = 16.dp)
    )
}

// ── Quick Create Button ───────────────────────────────────────────────────────

@Composable
private fun QuickCreateButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val iSource = remember { MutableInteractionSource() }
    val isPressed by iSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    Box(
        modifier = modifier
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(iSource, null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── Category Chip ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryChip(
    category: ProgramCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) category.color else category.color.copy(alpha = 0.06f)
    val textColor = if (selected) {
        if (category == ProgramCategory.ALL) Surface0 else Surface0
    } else {
        category.color
    }
    val border = if (selected) category.color else category.color.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                category.icon,
                null,
                tint = if (selected) Surface0 else category.color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                category.localizedLabel(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Program Card ──────────────────────────────────────────────────────────────

@Composable
private fun ProgramCard(program: ReadyProgram, onClick: () -> Unit) {
    val accent = program.category.color
    val iSource = remember { MutableInteractionSource() }
    val isPressed by iSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "pscale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 7.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1.copy(alpha = 0.7f))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .clickable(iSource, null, onClick = onClick)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(0.3f)))
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    program.category.icon,
                    null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Category + Level badges row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProgramBadge(program.category.localizedLabel(), accent)
                    LevelBadge(program.level)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    program.title,
                    color = Snow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    program.subtitle,
                    color = Mist,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(10.dp))
                val cardS = LocalAppTheme.current.strings
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatChip(Icons.Rounded.CalendarMonth, "${program.days} ${cardS.dayLabel}")
                    StatChip(Icons.Rounded.Schedule, "${program.weeks} ${cardS.weekLabel}")
                    StatChip(Icons.Rounded.TrackChanges, program.goal)
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = accent.copy(0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ProgramBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun LevelBadge(level: String) {
    val color = when (level) {
        "Başlangıç" -> CardGreen
        "Orta" -> Amber
        "İleri" -> CardCoral
        else -> Mist
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(level, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Saved Program Tile ────────────────────────────────────────────────────────

@Composable
private fun SavedProgramTile(prog: SavedProgram) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1.copy(0.4f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(prog.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val tileStrings = LocalAppTheme.current.strings
                Text(prog.name, color = Snow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${prog.days} ${tileStrings.dayLabel} • ${prog.focus.uppercase()}", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(LocalAppTheme.current.strings.activeLabel, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}

// ── Program Detail Dialog ─────────────────────────────────────────────────────

@Composable
private fun ProgramDetailDialog(program: ReadyProgram, onDismiss: () -> Unit) {
    val accent  = program.category.color
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(accent, theme, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(program.category.icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(program.category.localizedLabel(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text(program.level, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(program.title, style = MaterialTheme.typography.headlineMedium, color = Snow, fontWeight = FontWeight.Black)
                Text(program.subtitle, color = Mist, fontSize = 13.sp, fontWeight = FontWeight.Light)

                Spacer(Modifier.height(20.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2.copy(0.6f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DialogStat(Icons.Rounded.CalendarMonth, "${program.days}", strings.dayLabel, accent)
                    Box(Modifier.width(1.dp).height(36.dp).background(SurfaceStroke))
                    DialogStat(Icons.Rounded.Schedule, "${program.weeks}", strings.weekLabel, accent)
                    Box(Modifier.width(1.dp).height(36.dp).background(SurfaceStroke))
                    DialogStat(Icons.Rounded.TrackChanges, program.level.take(3).uppercase(), strings.levelLabel, accent)
                }

                Spacer(Modifier.height(20.dp))

                // Description
                Text(program.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Light)

                Spacer(Modifier.height(20.dp))

                // Muscle distribution
                Text(strings.muscleDistLabel, color = Mist, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                program.muscleLabels.forEachIndexed { i, label ->
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = Snow, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(program.musclePct[i], color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Surface3)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(program.muscleFractions[i])
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(listOf(accent, accent.copy(0.5f)))
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Schedule
                Text(strings.scheduleLabel, color = Mist, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2.copy(0.4f))
                        .border(1.dp, SurfaceStroke, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(program.schedule, color = TextSecondary, fontSize = 13.sp, lineHeight = 22.sp, fontWeight = FontWeight.Light)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        strings.applyProtocol,
                        color = Surface0,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogStat(icon: ImageVector, value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Snow, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── AI Builder Screen ─────────────────────────────────────────────────────────

@Composable
private fun AIBuilderScreen(onBack: () -> Unit, onSave: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2500)
            isLoading = false
            showResult = true
        }
    }

    val aiStrings = LocalAppTheme.current.strings
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)) {
        DetailHeader(title = "Oracle AI", sub = aiStrings.aiProtocolSub, onBack = onBack)
        Spacer(Modifier.height(40.dp))

        if (showResult) {
            LazyColumn(modifier = Modifier.padding(horizontal = 24.dp)) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Surface1.copy(0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(aiStrings.analysisResult, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("PUSH / PULL / LEGS • 8 ${aiStrings.weekLabel.uppercase()}", color = Snow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Oracle seviyeniz için 6 günlük yüksek frekanslı bir hipertrofi planı oluşturdu. Progresif yükleme için temel setler hazırlandı.", color = Mist, fontSize = 14.sp, fontWeight = FontWeight.Light)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { onSave("Oracle Optimized Plan") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(aiStrings.saveProtocol, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface1.copy(0.4f))
                    .border(1.dp, SurfaceStroke, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("TASARIM PARAMETRELERİ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Snow, fontWeight = FontWeight.Light),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        decorationBox = { inner ->
                            if (prompt.isEmpty()) Text("Antrenman sıklığı, hedefin ve seviyeni belirt...", color = TextMuted, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                Button(
                    onClick = { if (prompt.isNotBlank()) isLoading = true },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (prompt.isNotBlank()) MaterialTheme.colorScheme.primary else Surface2
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    else Text(
                        "PROTOKOLÜ ANALİZ ET",
                        color = if (prompt.isNotBlank()) MaterialTheme.colorScheme.onPrimary else TextMuted,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ── Manual Builder Screen ─────────────────────────────────────────────────────

@Composable
private fun ManualBuilderScreen(onBack: () -> Unit, onSave: (String) -> Unit) {
    data class ManualDay(val name: String)
    val days       = remember { mutableStateListOf(ManualDay("DAY 01"), ManualDay("DAY 02")) }
    val manStrings = LocalAppTheme.current.strings

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)) {
        DetailHeader(title = "Manuel", sub = manStrings.manualBuilderSub, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1.copy(0.4f))
                        .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ViewQuilt, null, tint = CardCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(day.name, color = Snow, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Add, null, tint = CardCyan)
                    }
                }
            }

            item {
                TextButton(
                    onClick = { days.add(ManualDay("DAY 0${days.size + 1}")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ YENİ GÜN", color = CardCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { onSave("Custom Strategic Plan") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(manStrings.saveProtocol, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
        }
    }
}

// ── Detail Header ─────────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(title: String, sub: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 48.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, null, tint = Mist)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text(title, style = MaterialTheme.typography.displayMedium, color = Snow, fontWeight = FontWeight.Black)
        }
    }
}
