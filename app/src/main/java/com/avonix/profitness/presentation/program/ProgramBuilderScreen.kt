package com.avonix.profitness.presentation.program

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.program.ManualExerciseInput
import com.avonix.profitness.data.store.UserPlan
import com.avonix.profitness.data.program.autoTitle
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.domain.model.ProgramType
import com.avonix.profitness.presentation.components.AiCreditInfoRow
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.coroutines.delay

// ── Data ─────────────────────────────────────────────────────────────────────

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
        "Her kas grubuna haftada 2 kez uyarı veren Reddit PPL tabanlı 6 günlük program. Push/Pull/Legs döngüsü ile hacim ve frekans dengesini optimize eder.",
        "📌 Paz: Push — Göğüs, Omuz, Triceps\n📌 Sal: Pull — Sırt, Biceps\n📌 Çar: Legs — Quad, Hamstring, Baldır\n📌 Per: Push (Hacim)\n📌 Cum: Pull (Hacim)\n📌 Cmt: Legs (Hacim)",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("88%", "72%", "42%"),
        listOf(0.88f, 0.72f, 0.42f)
    ),
    ReadyProgram(
        "Arnold Split",
        "Schwarzenegger'ın Hacim Protokolü",
        ProgramCategory.MUSCLE, 6, 12, "İleri",
        "Kas Kütlesi",
        "Arnold Schwarzenegger'ın 'The Education of a Bodybuilder' kitabında anlattığı orijinal 6 günlük split. Göğüs+Sırt süpersetleri maksimum kasılma ve kan akışı sağlar.",
        "📌 Paz: Chest & Back\n📌 Sal: Shoulders & Arms\n📌 Çar: Legs\n📌 Per: Chest & Back (Hacim)\n📌 Cum: Shoulders & Arms (Hacim)\n📌 Cmt: Legs (Hacim)",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("92%", "70%", "38%"),
        listOf(0.92f, 0.70f, 0.38f)
    ),
    ReadyProgram(
        "Upper / Lower Split",
        "PHUL — Güç & Hacim Dengesi",
        ProgramCategory.MUSCLE, 4, 8, "Orta",
        "Kas Kütlesi",
        "Power Hypertrophy Upper Lower (PHUL) protokolü. Haftanın 2 günü düşük tekrar güç, 2 günü yüksek tekrar hacim antrenmanı ile hem kuvvet hem kas kütlesi gelişimi sağlar.",
        "📌 Paz: Upper Power (5 rep)\n📌 Sal: Lower Power (5 rep)\n📌 Per: Upper Hypertrophy (10–12 rep)\n📌 Cum: Lower Hypertrophy (10–15 rep)",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("82%", "82%", "48%"),
        listOf(0.82f, 0.82f, 0.48f)
    ),
    ReadyProgram(
        "Bro Split",
        "Klasik 5 Gün Bölme",
        ProgramCategory.MUSCLE, 5, 8, "Orta",
        "Kas Kütlesi",
        "Her kas grubuna haftada bir gün yüksek hacimli çalışma. Göğüs, sırt, omuz, kol ve bacak için izole günlerle maksimum pompa ve kas hasarı yaratır.",
        "📌 Paz: Chest\n📌 Sal: Back\n📌 Çar: Shoulders\n📌 Per: Arms (Biceps & Triceps)\n📌 Cum: Legs & Core",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("86%", "62%", "52%"),
        listOf(0.86f, 0.62f, 0.52f)
    ),
    // ── YAĞ YAKIMI ────────────────────────────────────────────────────────────
    ReadyProgram(
        "HIIT Metabolik",
        "Yüksek Yoğunluklu Yağ Yakımı",
        ProgramCategory.FAT_LOSS, 4, 6, "Orta",
        "Yağ Yakımı",
        "30 sn çalışma / 30 sn dinlenme interval protokolü. EPOC (afterburn) etkisiyle antrenman sonrası 24–48 saat boyunca kalori yakımı yüksek kalır. Araştırmalar bu formatin düzenli kardiyoya göre %28 daha fazla yağ yaktığını göstermektedir.",
        "📌 Paz: Upper Body HIIT\n📌 Sal: Lower Body Circuit\n📌 Çar: Dinlenme\n📌 Per: Full Body HIIT\n📌 Cum: Cardio & Core",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("72%", "55%", "78%"),
        listOf(0.72f, 0.55f, 0.78f)
    ),
    ReadyProgram(
        "Fat Burn Full Body",
        "Tam Vücut Yağ Yakımı",
        ProgramCategory.FAT_LOSS, 3, 8, "Başlangıç",
        "Yağ Yakımı",
        "Haftada 3 gün, dinlenme günleriyle ayrılmış tam vücut devre antrenmanı. Bileşik hareketler kalp atış hızını yüksek tutarak hem kas korur hem yağ yakar. Başlangıç seviyesi için idealdir.",
        "📌 Paz: Full Body A — Squat, Push, Row\n📌 Çar: Full Body B — Hinge, Press, Pull\n📌 Cum: Full Body C — Kettlebell & Patlayıcı",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("62%", "65%", "68%"),
        listOf(0.62f, 0.65f, 0.68f)
    ),
    ReadyProgram(
        "Metabolik Kondisyon",
        "Yoğun Devre Antrenmanı",
        ProgramCategory.FAT_LOSS, 5, 6, "İleri",
        "Yağ Yakımı",
        "5 günlük yüksek yoğunluklu devre formatı. Kısa dinlenme süreleri metabolik stresin zirvede tutulmasını sağlar. Kas kütlesini korurken agresif yağ yakımı için CrossFit metodolojisinden ilham alınmıştır.",
        "📌 Paz: Upper Push Circuit\n📌 Sal: Leg Metabolic\n📌 Çar: Upper Pull Circuit\n📌 Per: Full Body EMOM\n📌 Cum: Cardio Finisher",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("78%", "62%", "74%"),
        listOf(0.78f, 0.62f, 0.74f)
    ),
    // ── GÜÇ ──────────────────────────────────────────────────────────────────
    ReadyProgram(
        "5×5 Güç",
        "StrongLifts — Lineer Progresyon",
        ProgramCategory.STRENGTH, 3, 12, "Başlangıç",
        "Güç",
        "StrongLifts 5×5 protokolü. Haftada 3 gün A/B antrenman rotasyonu. Her seansta ağırlığı 2,5 kg artırarak lineer güç gelişimi sağlar. Squat her seansta yapılır; kuvvet programlarının en kanıtlanmış temelidir.",
        "📌 Paz: A — Squat + Bench + Barbell Row\n📌 Çar: B — Squat + OHP + Deadlift\n📌 Cum: A — Squat + Bench + Barbell Row",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("74%", "84%", "56%"),
        listOf(0.74f, 0.84f, 0.56f)
    ),
    ReadyProgram(
        "Powerlifting Temel",
        "Rekabet Gücü Protokolü",
        ProgramCategory.STRENGTH, 4, 16, "İleri",
        "Güç",
        "Squat, bench press ve deadlift odaklı 16 haftalık periodizasyon. Her hafta ayrı bir lift için yoğun çalışma yapılır; aksesuar günü zayıf noktaları kapatır. Rekabetçi powerlifting sahnesine uygun yapı.",
        "📌 Paz: Squat Focus (5×5 yoğun)\n📌 Sal: Bench Focus (5×5 yoğun)\n📌 Çar: Dinlenme\n📌 Per: Deadlift Focus (5×3 yoğun)\n📌 Cum: Accessory & Strengthening",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("78%", "92%", "50%"),
        listOf(0.78f, 0.92f, 0.50f)
    ),
    ReadyProgram(
        "GZCLP",
        "Tier Tabanlı Güç Protokolü",
        ProgramCategory.STRENGTH, 3, 10, "Orta",
        "Güç",
        "u/Sayings tarafından geliştirilen GZCLP sistemi: T1 (5×3 ağır), T2 (3×10 orta), T3 (2×15 aksesuar). Her tier kendi progresyon kuralına göre ilerler. Verimli ve bilimsel tabanlı bir güç artış programıdır.",
        "📌 Paz: A — Squat T1 · Bench T2 · Row T3\n📌 Çar: B — Bench T1 · Squat T2 · Lat Pull T3\n📌 Cum: C — Deadlift T1 · OHP T2 · Leg Press T3",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("76%", "84%", "52%"),
        listOf(0.76f, 0.84f, 0.52f)
    ),
    // ── DAYANIKLILIK ──────────────────────────────────────────────────────────
    ReadyProgram(
        "Kardiyovasküler Temel",
        "Aerobik Kapasite Gelişimi",
        ProgramCategory.ENDURANCE, 4, 8, "Başlangıç",
        "Dayanıklılık",
        "Koşu, bisiklet ve kürek çekme kombinasyonuyla aerobik baz oluşturan 4 günlük program. Uzun tempo koşusu aerobik kapasiteyi, interval günleri laktik eşiği geliştirir. 5K/10K koşusuna hazırlık için idealdir.",
        "📌 Paz: Long Run — Düşük Tempolu Uzun Koşu\n📌 Sal: Interval Training — HIIT + Patlayıcı\n📌 Çar: Dinlenme\n📌 Per: Cycling & Rowing\n📌 Cum: Tempo Run — Orta-Yüksek Hızda",
        listOf("KARDİYO", "ALT VÜCUT", "CORE"),
        listOf("92%", "58%", "44%"),
        listOf(0.92f, 0.58f, 0.44f)
    ),
    ReadyProgram(
        "Hybrid Athlete",
        "Güç + Dayanıklılık Hibrit",
        ProgramCategory.ENDURANCE, 5, 10, "İleri",
        "Dayanıklılık",
        "Nick Bare tarzı hibrit antrenman. Haftada 2 gün barbell kuvvet çalışması, 2 gün metabolik kondisyon, 1 gün aerobik kapasite. Hem kuvveti hem VO2max'ı geliştiren az kişinin yaptığı programdır.",
        "📌 Paz: Strength Day — Squat, Bench, Deadlift\n📌 Sal: MetCon — Burpee, KB Swing, Box Jump\n📌 Çar: Aerobic Capacity — Uzun Koşu/Bisiklet\n📌 Per: Strength + Speed — Clean & Press, Jump Squat\n📌 Cum: Long MetCon — Kürek + Devre",
        listOf("KARDİYO", "GÜÇ", "CORE"),
        listOf("76%", "78%", "65%"),
        listOf(0.76f, 0.78f, 0.65f)
    ),
    // ── BAŞLANGIÇ ────────────────────────────────────────────────────────────
    ReadyProgram(
        "Full Body Başlangıç",
        "Starting Strength Tabanlı",
        ProgramCategory.BEGINNER, 3, 8, "Başlangıç",
        "Genel Fitness",
        "Mark Rippetoe'nun Starting Strength felsefesinden ilham alınan 3 günlük tam vücut programı. A/B/C rotasyonuyla squat, bench, row, deadlift ve OHP temel hareketleri öğretilir. Kas ve güç için en sağlam başlangıç noktasıdır.",
        "📌 Paz: Day A — Squat + Bench + Barbell Row\n📌 Çar: Day B — Romanian Deadlift + Lat Pulldown + DB Press\n📌 Cum: Day C — Full Body Circuit (Goblet, Push-Up, Lunge)",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("65%", "65%", "48%"),
        listOf(0.65f, 0.65f, 0.48f)
    ),
    ReadyProgram(
        "Vücut Ağırlığı",
        "Ekipmansız Kalistenik",
        ProgramCategory.BEGINNER, 3, 6, "Başlangıç",
        "Genel Fitness",
        "Sıfır ekipmanla yapılabilen 3 günlük kalistenik program. Push-up, pull-up, dips ve squat varyasyonlarıyla tüm vücut kası ve fonksiyonel kuvvet gelişimi sağlar. Ev, park veya seyahatte uygulanabilir.",
        "📌 Paz: Upper Body — Push-Up, Dips, Pull-Up\n📌 Çar: Lower Body — Goblet Squat, Lunge, Bulgarian\n📌 Cum: Core & Cardio — Plank, Burpee, Mountain Climber",
        listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE"),
        listOf("62%", "65%", "72%"),
        listOf(0.62f, 0.65f, 0.72f)
    )
)

sealed class BuilderMode {
    object Choose : BuilderMode()
    object AI     : BuilderMode()
    object Manual : BuilderMode()
    data class Edit(val program: com.avonix.profitness.domain.model.Program) : BuilderMode()
}

@Composable
fun ProgramBuilderScreen(
    initialMode      : BuilderMode = BuilderMode.Choose,
    timerExtraPad    : androidx.compose.ui.unit.Dp = 0.dp,
    onNavigateToStore: () -> Unit = {},
    viewModel        : ProgramViewModel = hiltViewModel(),
    shareViewModel   : ProgramShareViewModel = hiltViewModel()
) {
    var showPaywall by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<Program?>(null) }
    val shareState by shareViewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(shareState.result) {
        shareState.result?.let { r ->
            val msg = when (r) {
                ProgramShareResult.Success -> "Program topluluk akışına eklendi ✓"
                is ProgramShareResult.Error -> "Paylaşım başarısız: ${r.message}"
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            shareViewModel.consumeResult()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf<BuilderMode>(initialMode) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    // Tab geçişinde stale ise yenile (3 dk cache)
    LaunchedEffect(Unit) { viewModel.reloadIfStale() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProgramEvent.ShowSnackbar -> snackbarMsg = event.message
                ProgramEvent.NavigateBack    -> mode = BuilderMode.Choose
                ProgramEvent.ShowPaywall     -> showPaywall = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        ArchitectGrid()
        PageAccentBloom()

        when (val m = mode) {
                is BuilderMode.Choose -> BuilderChooseScreen(
                    userPrograms     = uiState.userPrograms.filterNot { it.id in uiState.deletingProgramIds },
                    isLoading        = uiState.isLoading,
                    onMode           = { newMode ->
                        // AI Builder → ViewModel'in checkAiAccess'ini çağır;
                        // yetersizse ProgramEvent.ShowPaywall fırlatır.
                        if (newMode == BuilderMode.AI && !viewModel.checkAiAccess()) return@BuilderChooseScreen
                        mode = newMode
                    },
                    onSelectTemplate = { viewModel.selectTemplate(it) },
                    onSetActive      = { viewModel.setActive(it) },
                    onDeleteProgram  = { programId ->
                        viewModel.deleteProgram(
                            programId = programId,
                            onSuccess = {},
                            onFailure = {}
                        )
                    },
                    onEditProgram    = { prog -> mode = BuilderMode.Edit(prog) },
                    onShareProgram   = { prog -> shareTarget = prog },
                    timerExtraPad    = timerExtraPad
                )
                is BuilderMode.AI -> AIBuilderScreen(
                    viewModel     = viewModel,
                    onBack        = { mode = BuilderMode.Choose },
                    timerExtraPad = timerExtraPad
                )
                is BuilderMode.Manual -> ManualBuilderScreen(
                    viewModel     = viewModel,
                    onBack        = { mode = BuilderMode.Choose },
                    timerExtraPad = timerExtraPad
                )
                is BuilderMode.Edit -> EditProgramScreen(
                    program       = m.program,
                    viewModel     = viewModel,
                    onBack        = { mode = BuilderMode.Choose },
                    timerExtraPad = timerExtraPad
                )
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

        // Paywall dialog
        if (showPaywall) {
            com.avonix.profitness.presentation.store.PaywallDialog(
                onDismiss   = { showPaywall = false },
                onGoToStore = {
                    showPaywall = false
                    onNavigateToStore()
                }
            )
        }

        // Program paylaşım sheet'i (kart üzerindeki "PAYLAŞ" ile açılır)
        shareTarget?.let { target ->
            com.avonix.profitness.presentation.discover.ShareProgramSheet(
                programs             = uiState.userPrograms,
                preselectedProgramId = target.id,
                onDismiss            = { shareTarget = null },
                onConfirm            = { programId, title, desc, tags, difficulty, weeks, days ->
                    shareViewModel.shareProgram(programId, title, desc, tags, difficulty, weeks, days)
                    shareTarget = null
                }
            )
        }
    }
}

// ── Architect Grid ────────────────────────────────────────────────────────────

@Composable
private fun ArchitectGrid() {
    val theme = LocalAppTheme.current
    val gridColor = theme.stroke.copy(0.3f)
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
    userPrograms   : List<Program>,
    isLoading      : Boolean,
    onMode         : (BuilderMode) -> Unit,
    onSelectTemplate: (String) -> Unit,
    onSetActive    : (String) -> Unit,
    onDeleteProgram: (String) -> Unit,
    onEditProgram  : (Program) -> Unit,
    onShareProgram : (Program) -> Unit,
    timerExtraPad  : androidx.compose.ui.unit.Dp = 0.dp
) {
    var selectedProgram by remember { mutableStateOf<ReadyProgram?>(null) }
    var activeCategory by remember { mutableStateOf(ProgramCategory.ALL) }

    selectedProgram?.let { prog ->
        ProgramDetailDialog(
            program   = prog,
            onDismiss = { selectedProgram = null },
            onApply   = { onSelectTemplate(prog.title); selectedProgram = null }
        )
    }

    val filtered = remember(activeCategory) {
        if (activeCategory == ProgramCategory.ALL) READY_PROGRAMS
        else READY_PROGRAMS.filter { it.category == activeCategory }
    }

    val sectionStrings = LocalAppTheme.current.strings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp + timerExtraPad)
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
                    color = LocalAppTheme.current.text0,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    LocalAppTheme.current.strings.programStudioSub,
                    color = LocalAppTheme.current.text1,
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

        // ── My Programs ───────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        } else if (userPrograms.isNotEmpty()) {
            item {
                SectionLabel(sectionStrings.activeProtocols, MaterialTheme.colorScheme.primary)
            }
            items(userPrograms, key = { it.id }) { prog ->
                SavedProgramTile(
                    program     = prog,
                    onSetActive = { onSetActive(prog.id) },
                    onDelete    = { onDeleteProgram(prog.id) },
                    onEdit      = { onEditProgram(prog) },
                    onShare     = { onShareProgram(prog) }
                )
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
    val theme  = LocalAppTheme.current
    val iSource = remember { MutableInteractionSource() }
    val isPressed by iSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "pscale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 7.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg1.copy(alpha = 0.9f))
            .border(1.dp, theme.stroke, RoundedCornerShape(20.dp))
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
                    color = theme.text0,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    program.subtitle,
                    color = theme.text1,
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
    val theme = LocalAppTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = theme.text2, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Saved Program Tile ────────────────────────────────────────────────────────

@Composable
private fun SavedProgramTile(
    program    : Program,
    onSetActive: () -> Unit,
    onDelete   : () -> Unit,
    onEdit     : () -> Unit,
    onShare    : () -> Unit = {}
) {
    val theme   = LocalAppTheme.current
    val primary = MaterialTheme.colorScheme.primary
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val typeIcon = when (program.type) {
        ProgramType.TEMPLATE -> Icons.Rounded.ViewList
        ProgramType.AI       -> Icons.Rounded.AutoAwesome
        ProgramType.MANUAL   -> Icons.Rounded.Construction
    }
    val typeLabel = when (program.type) {
        ProgramType.TEMPLATE -> "ŞABLON"
        ProgramType.AI       -> "AI"
        ProgramType.MANUAL   -> "MANUEL"
    }

    // ── Silme Onayı ───────────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = theme.bg1,
            title = {
                Text(
                    "Programı Sil",
                    color      = theme.text0,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            },
            text = {
                Text(
                    "\"${program.name}\" silinecek. Bu işlem geri alınamaz.",
                    color    = theme.text1,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Sil", color = CardCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("İptal", color = theme.text2)
                }
            }
        )
    }

    // ── Type accent color ─────────────────────────────────────────────────────
    val typeColor = when (program.type) {
        ProgramType.AI       -> CardPurple
        ProgramType.MANUAL   -> Amber
        ProgramType.TEMPLATE -> CardCyan
    }
    val accentColor = if (program.isActive) primary else typeColor

    // ── Program stats ─────────────────────────────────────────────────────────
    val workoutDays  = program.days.count { !it.isRestDay }
    val totalExercises = program.days.sumOf { it.exercises.size }
    val dayLabels    = listOf("M", "S", "Ç", "P", "C", "C", "P")

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
    ) {
        // ── Glow shadow for active program ────────────────────────────────
        if (program.isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(0.18f), Color.Transparent),
                            radius = 600f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (program.isActive)
                        Brush.linearGradient(
                            listOf(primary.copy(0.10f), theme.bg1.copy(0.95f))
                        )
                    else Brush.linearGradient(
                        listOf(theme.bg1.copy(0.95f), theme.bg1.copy(0.95f))
                    )
                )
                .border(
                    width = if (program.isActive) 1.5.dp else 1.dp,
                    brush = if (program.isActive)
                        Brush.linearGradient(listOf(primary.copy(0.6f), primary.copy(0.2f)))
                    else Brush.linearGradient(listOf(theme.stroke, theme.stroke)),
                    shape = shape
                )
        ) {
            // ── Main content (tappable → edit) ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .padding(start = 0.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor, accentColor.copy(0.25f))
                            )
                        )
                )

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 14.dp, bottom = 14.dp)
                ) {
                    // ── Top: badge + active indicator ─────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Type badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    typeIcon, null,
                                    tint     = accentColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    typeLabel,
                                    color         = accentColor,
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        if (program.isActive) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primary.copy(0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    theme.strings.activeLabel,
                                    color         = primary,
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Program name ──────────────────────────────────────
                    Text(
                        program.name,
                        color      = theme.text0,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(5.dp))

                    // ── Stats row ─────────────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth, null,
                            tint     = theme.text2.copy(0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$workoutDays antrenman günü",
                            color    = theme.text2.copy(0.7f),
                            fontSize = 12.sp
                        )
                        if (totalExercises > 0) {
                            Text(
                                "  ·  $totalExercises egzersiz",
                                color    = theme.text2.copy(0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Day dots strip ────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        dayLabels.forEachIndexed { idx, label ->
                            val day     = program.days.firstOrNull { it.dayIndex == idx }
                            val isWork  = day != null && !day.isRestDay
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isWork) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isWork) accentColor
                                            else theme.text2.copy(0.18f)
                                        )
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    label,
                                    color    = if (isWork) accentColor.copy(0.8f) else theme.text2.copy(0.3f),
                                    fontSize = 8.sp,
                                    fontWeight = if (isWork) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ── Divider ───────────────────────────────────────────────────
            HorizontalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)

            // ── Action strip ──────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                if (!program.isActive) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClick = onSetActive)
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "AKTİF ET",
                            color      = primary,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    VerticalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onEdit)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = theme.text1.copy(0.8f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "DÜZENLE",
                        color      = theme.text1.copy(0.8f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                VerticalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onShare)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Share, null, tint = primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "PAYLAŞ",
                        color      = primary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                VerticalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .weight(if (program.isActive) 0.6f else 0.8f)
                        .fillMaxHeight()
                        .clickable(onClick = { showDeleteConfirm = true })
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = CardCoral.copy(0.85f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "SİL",
                        color      = CardCoral.copy(0.85f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ── Program Detail Dialog ─────────────────────────────────────────────────────

@Composable
private fun ProgramDetailDialog(program: ReadyProgram, onDismiss: () -> Unit, onApply: () -> Unit = onDismiss) {
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
                Text(program.title, style = MaterialTheme.typography.headlineMedium, color = theme.text0, fontWeight = FontWeight.Black)
                Text(program.subtitle, color = theme.text1, fontSize = 13.sp, fontWeight = FontWeight.Light)

                Spacer(Modifier.height(20.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bg2.copy(0.8f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DialogStat(Icons.Rounded.CalendarMonth, "${program.days}", strings.dayLabel, accent)
                    Box(Modifier.width(1.dp).height(36.dp).background(theme.stroke))
                    DialogStat(Icons.Rounded.Schedule, "${program.weeks}", strings.weekLabel, accent)
                    Box(Modifier.width(1.dp).height(36.dp).background(theme.stroke))
                    DialogStat(Icons.Rounded.TrackChanges, program.level.take(3).uppercase(), strings.levelLabel, accent)
                }

                Spacer(Modifier.height(20.dp))

                // Description
                Text(program.description, color = theme.text1, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Light)

                Spacer(Modifier.height(20.dp))

                // Muscle distribution
                Text(strings.muscleDistLabel, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                program.muscleLabels.forEachIndexed { i, label ->
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = theme.text1, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(program.musclePct[i], color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(theme.bg3)
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
                Text(strings.scheduleLabel, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bg2.copy(0.6f))
                        .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(program.schedule, color = theme.text1, fontSize = 13.sp, lineHeight = 22.sp, fontWeight = FontWeight.Light)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onApply,
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
    val theme = LocalAppTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = theme.text0, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── AI Builder Screen ─────────────────────────────────────────────────────────

@Composable
private fun AIBuilderScreen(viewModel: ProgramViewModel, onBack: () -> Unit, timerExtraPad: androidx.compose.ui.unit.Dp = 0.dp) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    var prompt    by remember { mutableStateOf("") }
    val aiTheme    = LocalAppTheme.current
    val aiStrings  = aiTheme.strings
    val context    = LocalContext.current

    // Seçilen dosya bilgisi
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedBase64   by remember { mutableStateOf<String?>(null) }
    var selectedMimeType by remember { mutableStateOf<String?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) {
            selectedBase64   = Base64.encodeToString(bytes, Base64.NO_WRAP)
            selectedMimeType = mime
            selectedFileName = uri.lastPathSegment ?: "dosya"
        }
    }

    val canAnalyze = (prompt.isNotBlank() || selectedBase64 != null) && !uiState.aiLoading
    val scrollState = rememberScrollState()

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Scrollable content ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = contentPad + 80.dp + timerExtraPad)
                .verticalScroll(scrollState)
        ) {
            DetailHeader(title = "Oracle AI", sub = aiStrings.aiProtocolSub, onBack = onBack)
            Spacer(Modifier.height(16.dp))

            // Kredi bilgisi
            AiCreditInfoRow(
                isFree  = uiState.userPlan == UserPlan.FREE,
                credits = uiState.aiCredits,
                costLabel = "1 kredi / program",
                theme   = aiTheme,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Metin giriş kutusu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(aiTheme.bg1.copy(0.9f))
                    .border(1.dp, aiTheme.stroke, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("TASARIM PARAMETRELERİ", color = aiTheme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    val textFieldScroll = rememberScrollState()
                    BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = aiTheme.text0, fontWeight = FontWeight.Light),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp)
                            .verticalScroll(textFieldScroll),
                        enabled = !uiState.aiLoading,
                        decorationBox = { inner ->
                            if (prompt.isEmpty()) Text(
                                if (selectedBase64 != null) "Ek talimat ekleyebilirsin (opsiyonel)..."
                                else "Antrenman sıklığı, hedefin ve seviyeni belirt...",
                                color = aiTheme.text2, fontSize = 15.sp
                            )
                            inner()
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Dosya yükleme butonu + seçilen dosya gösterimi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { fileLauncher.launch("*/*") },
                    enabled = !uiState.aiLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selectedBase64 != null) MaterialTheme.colorScheme.primary else aiTheme.stroke),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (selectedBase64 != null) Icons.Rounded.CheckCircle else Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = if (selectedBase64 != null) MaterialTheme.colorScheme.primary else aiTheme.text2,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedBase64 != null) "Değiştir" else "Görsel / PDF / HTML Yükle",
                        color = if (selectedBase64 != null) MaterialTheme.colorScheme.primary else aiTheme.text2,
                        fontSize = 13.sp
                    )
                }

                if (selectedBase64 != null) {
                    IconButton(
                        onClick = {
                            selectedBase64   = null
                            selectedMimeType = null
                            selectedFileName = null
                        }
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kaldır", tint = CardCoral)
                    }
                }
            }

            // Seçilen dosya adı
            selectedFileName?.let { name ->
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = aiTheme.text2, modifier = Modifier.size(14.dp))
                    Text(name, color = aiTheme.text2, fontSize = 12.sp, maxLines = 1)
                }
            }

            // Error banner
            uiState.aiError?.let { errMsg ->
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardCoral.copy(alpha = 0.15f))
                        .border(1.dp, CardCoral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(errMsg, color = CardCoral, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Fixed bottom button ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, aiTheme.bg0),
                        startY = 0f,
                        endY = 60f
                    )
                )
                .padding(start = 24.dp, end = 24.dp, bottom = contentPad + timerExtraPad, top = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            if (uiState.userPlan == UserPlan.FREE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("1 kredi", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Text("Kalan: ${uiState.aiCredits}", color = LocalAppTheme.current.text2, fontSize = 10.sp)
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.clearAiError()
                    viewModel.createFromAI(prompt, selectedBase64, selectedMimeType)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAnalyze) MaterialTheme.colorScheme.primary else Surface2
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = canAnalyze
            ) {
                if (uiState.aiLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        if (selectedBase64 != null) "DOSYADAN PROGRAM OLUŞTUR" else "PROTOKOLÜ ANALİZ ET",
                        color = if (canAnalyze) MaterialTheme.colorScheme.onPrimary else TextMuted,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            } // Column
        }
    }
}

// ── Manual Builder — helper state classes ─────────────────────────────────────

private class MutableManualDay {
    var title    by mutableStateOf("")
    var isRestDay by mutableStateOf(false)
    val exercises = mutableStateListOf<DraftExercise>()
}

private data class DraftExercise(
    val exerciseId  : String,
    val name        : String,
    val targetMuscle: String,
    val sets        : Int,
    val reps        : Int,
    val restSeconds : Int
)

// ── Edit Program Screen ───────────────────────────────────────────────────────

@Composable
private fun EditProgramScreen(
    program      : com.avonix.profitness.domain.model.Program,
    viewModel    : ProgramViewModel,
    onBack       : () -> Unit,
    timerExtraPad: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val exercises  = uiState.exercises
    val editTheme  = LocalAppTheme.current
    val editStrings = editTheme.strings

    var programName      by remember { mutableStateOf(program.name) }
    var showPickerForDay by remember { mutableStateOf<Int?>(null) }
    var showAiEditDialog by remember { mutableStateOf(false) }
    var aiPrompt         by remember { mutableStateOf("") }
    // Pair<dayIndex, exerciseIndex>
    var editingExercise  by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Mevcut programın günlerini pre-populate et
    val days = remember {
        mutableStateListOf<MutableManualDay>().also { list ->
            program.days.forEach { day ->
                val m = MutableManualDay()
                m.title    = day.title
                m.isRestDay = day.isRestDay
                day.exercises.sortedBy { it.orderIndex }.forEach { ex ->
                    m.exercises.add(
                        DraftExercise(
                            exerciseId   = ex.exerciseId,
                            name         = ex.exerciseName,
                            targetMuscle = ex.targetMuscle,
                            sets         = ex.sets,
                            reps         = ex.reps,
                            restSeconds  = ex.restSeconds
                        )
                    )
                }
                list.add(m)
            }
            if (list.isEmpty()) list.add(MutableManualDay())
        }
    }

    editingExercise?.let { (dayIdx, exIdx) ->
        val ex = days.getOrNull(dayIdx)?.exercises?.getOrNull(exIdx)
        if (ex != null) {
            ExerciseEditDialog(
                exercise  = ex,
                onDismiss = { editingExercise = null },
                onConfirm = { newSets, newReps, newRest ->
                    days[dayIdx].exercises[exIdx] = ex.copy(sets = newSets, reps = newReps, restSeconds = newRest)
                    editingExercise = null
                }
            )
        }
    }

    showPickerForDay?.let { dayIndex ->
        ExercisePickerSheet(
            exercises = exercises,
            onDismiss = { showPickerForDay = null },
            onConfirm = { exerciseId, name, muscle, sets, reps, rest ->
                val day = days[dayIndex]
                day.exercises.add(DraftExercise(exerciseId, name, muscle, sets, reps, rest))
                if (day.title.isBlank()) {
                    day.title = autoTitle(day.exercises.map { it.targetMuscle })
                }
                showPickerForDay = null
            },
            onRequestExercise = { name, muscle, notes ->
                viewModel.requestExercise(name, muscle, notes)
            },
            requestLoading = uiState.requestLoading
        )
    }

    // AI edit sonucu geldiğinde günleri güncelle
    val aiEditResult = uiState.aiEditResult
    LaunchedEffect(aiEditResult) {
        if (aiEditResult != null) {
            programName = aiEditResult.first
            days.clear()
            aiEditResult.second.forEach { dayResult ->
                val m = MutableManualDay()
                m.title    = dayResult.title
                m.isRestDay = dayResult.isRestDay
                dayResult.exercises.forEach { ex ->
                    m.exercises.add(DraftExercise(ex.exerciseId, ex.name, ex.targetMuscle, ex.sets, ex.reps, ex.restSeconds))
                }
                days.add(m)
            }
            viewModel.clearAiEditResult()
            showAiEditDialog = false
            aiPrompt = ""
        }
    }

    // AI Düzenle Dialog
    if (showAiEditDialog) {
        Dialog(onDismissRequest = {
            if (!uiState.aiEditLoading) {
                showAiEditDialog = false
                viewModel.clearAiEditError()
            }
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(editTheme.bg1)
                    .border(1.dp, editTheme.stroke, RoundedCornerShape(24.dp))
            ) {
                // Gradient header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(CardPurple.copy(0.35f), CardCyan.copy(0.2f)))
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CardPurple, CardCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint     = Snow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "AI ile Düzenle",
                                color      = Snow,
                                fontWeight = FontWeight.Black,
                                fontSize   = 18.sp
                            )
                            Text(
                                "Programı nasıl değiştireyim?",
                                color    = Snow.copy(0.65f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // İçerik
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Prompt alanı
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(editTheme.bg0)
                            .border(1.dp, editTheme.stroke, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value         = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            enabled       = !uiState.aiEditLoading,
                            textStyle     = MaterialTheme.typography.bodyMedium.copy(
                                color    = editTheme.text0,
                                fontSize = 15.sp
                            ),
                            modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp).verticalScroll(rememberScrollState()),
                            decorationBox = { inner ->
                                if (aiPrompt.isEmpty()) {
                                    Text(
                                        "Örn: Bacak günü ekle, karın egzersizlerini çıkar, dinlenme süresini azalt...",
                                        color      = editTheme.text2,
                                        fontSize   = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }

                    // Hata
                    if (uiState.aiEditError != null) {
                        Text(
                            uiState.aiEditError!!,
                            color    = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    // Butonlar
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                showAiEditDialog = false
                                viewModel.clearAiEditError()
                            },
                            enabled  = !uiState.aiEditLoading,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, editTheme.stroke)
                        ) {
                            Text("İptal", color = editTheme.text1, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick  = {
                                val currentDrafts = days.mapIndexed { i, d ->
                                    ManualDayDraft(
                                        title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                                        isRestDay = d.isRestDay,
                                        selectedExercises = d.exercises.mapIndexed { ei, ex ->
                                            com.avonix.profitness.data.program.ManualExerciseInput(
                                                exerciseId  = ex.exerciseId,
                                                sets        = ex.sets,
                                                reps        = ex.reps,
                                                restSeconds = ex.restSeconds,
                                                orderIndex  = ei
                                            )
                                        }
                                    )
                                }
                                viewModel.editWithAI(
                                    programId       = program.id,
                                    currentName     = programName,
                                    currentDays     = currentDrafts,
                                    userInstruction = aiPrompt
                                )
                            },
                            enabled  = !uiState.aiEditLoading && aiPrompt.isNotBlank(),
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = CardPurple)
                        ) {
                            if (uiState.aiEditLoading) {
                                CircularProgressIndicator(
                                    color       = Snow,
                                    modifier    = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Uygula", color = Snow, fontWeight = FontWeight.Bold)
                                if (uiState.userPlan == UserPlan.FREE) {
                                    Spacer(Modifier.width(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Snow.copy(alpha = 0.20f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Bolt, null, tint = Snow, modifier = Modifier.size(9.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("1 kredi", color = Snow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = contentPad + 80.dp + timerExtraPad)) {
            DetailHeader(title = "Düzenle", sub = "Programı güncelle", onBack = onBack)

            // Program adı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(editTheme.bg1)
                    .border(1.dp, editTheme.stroke, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value       = programName,
                    onValueChange = { programName = it },
                    textStyle   = MaterialTheme.typography.titleMedium.copy(
                        color      = editTheme.text0,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (programName.isEmpty()) {
                            Text(
                                "Program adı...",
                                color      = editTheme.text2,
                                fontWeight = FontWeight.Light,
                                fontSize   = 16.sp
                            )
                        }
                        inner()
                    }
                )
            }

            // Günler listesi
            LazyColumn(
                modifier      = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(days) { index, day ->
                    ManualDayCard(
                        day              = day,
                        dayNumber        = index + 1,
                        onAddExercise    = { showPickerForDay = index },
                        onRemoveExercise = { exIdx -> day.exercises.removeAt(exIdx) },
                        onRemoveDay      = { if (days.size > 1) days.removeAt(index) },
                        onEditExercise   = { exIdx -> editingExercise = Pair(index, exIdx) }
                    )
                }
                item {
                    if (days.size < 7) {
                        TextButton(
                            onClick  = { days.add(MutableManualDay()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = CardCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "+ YENİ GÜN  (${days.size}/7)",
                                color      = CardCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Alt aksiyon butonları
        val isLoading = uiState.isLoading
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        // Gradient sadece buton alanını kapsar, altı transparan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, editTheme.bg0),
                        startY = 0f, endY = 60f
                    )
                )
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 16.dp)
        ) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment   = Alignment.CenterVertically
            ) {
                // AI ile Düzenle butonu
                OutlinedButton(
                    onClick         = { showAiEditDialog = true },
                    enabled         = !isLoading && !uiState.aiEditLoading,
                    modifier        = Modifier.height(64.dp),
                    shape           = RoundedCornerShape(16.dp),
                    contentPadding  = PaddingValues(horizontal = 16.dp),
                    colors          = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardPurple.copy(0.18f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(CardPurple, CardCyan))
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint     = CardPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "AI",
                            color         = Snow,
                            fontWeight    = FontWeight.Black,
                            fontSize      = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Kaydet butonu
                Button(
                    onClick = {
                        val dayDrafts = days.mapIndexed { i, d ->
                            ManualDayDraft(
                                title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                                isRestDay = d.isRestDay,
                                selectedExercises = d.exercises.mapIndexed { ei, ex ->
                                    com.avonix.profitness.data.program.ManualExerciseInput(
                                        exerciseId  = ex.exerciseId,
                                        sets        = ex.sets,
                                        reps        = ex.reps,
                                        restSeconds = ex.restSeconds,
                                        orderIndex  = ei
                                    )
                                }
                            )
                        }
                        viewModel.updateManualProgram(
                            programId = program.id,
                            name      = programName,
                            days      = dayDrafts
                        )
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape    = RoundedCornerShape(16.dp),
                    enabled  = !isLoading && !uiState.aiEditLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("KAYDET", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
        // Navbar + timer alanı — transparan boşluk, arka plan yok
        Spacer(Modifier.height(contentPad + timerExtraPad))
        } // Column
    }
}

// ── Manual Builder Screen ─────────────────────────────────────────────────────

@Composable
private fun ManualBuilderScreen(
    viewModel    : ProgramViewModel,
    onBack       : () -> Unit,
    timerExtraPad: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val exercises   = uiState.exercises
    val manTheme    = LocalAppTheme.current
    val manStrings  = manTheme.strings

    var programName       by remember { mutableStateOf("") }
    val days              = remember { mutableStateListOf<MutableManualDay>().also { it.add(MutableManualDay()) } }
    var showPickerForDay  by remember { mutableStateOf<Int?>(null) }
    var editingExercise   by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    editingExercise?.let { (dayIdx, exIdx) ->
        val ex = days.getOrNull(dayIdx)?.exercises?.getOrNull(exIdx)
        if (ex != null) {
            ExerciseEditDialog(
                exercise  = ex,
                onDismiss = { editingExercise = null },
                onConfirm = { newSets, newReps, newRest ->
                    days[dayIdx].exercises[exIdx] = ex.copy(sets = newSets, reps = newReps, restSeconds = newRest)
                    editingExercise = null
                }
            )
        }
    }

    // Exercise picker bottom sheet
    showPickerForDay?.let { dayIndex ->
        ExercisePickerSheet(
            exercises = exercises,
            onDismiss = { showPickerForDay = null },
            onConfirm = { exerciseId, name, muscle, sets, reps, rest ->
                val day = days[dayIndex]
                day.exercises.add(DraftExercise(exerciseId, name, muscle, sets, reps, rest))
                if (day.title.isBlank()) {
                    day.title = autoTitle(day.exercises.map { it.targetMuscle })
                }
                showPickerForDay = null
            },
            onRequestExercise = { name, muscle, notes ->
                viewModel.requestExercise(name, muscle, notes)
            },
            requestLoading = uiState.requestLoading
        )
    }

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp
    Column(modifier = Modifier.fillMaxSize().padding(bottom = contentPad + timerExtraPad)) {
        DetailHeader(title = "Manuel", sub = manStrings.manualBuilderSub, onBack = onBack)

        // ── Program name input ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(manTheme.bg1)
                .border(1.dp, manTheme.stroke, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = programName,
                onValueChange = { programName = it },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = manTheme.text0,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (programName.isEmpty()) {
                        Text(
                            "Program adı...",
                            color = manTheme.text2,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
        }

        // ── Days list ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(days) { index, day ->
                ManualDayCard(
                    day              = day,
                    dayNumber        = index + 1,
                    onAddExercise    = { showPickerForDay = index },
                    onRemoveExercise = { exIdx -> day.exercises.removeAt(exIdx) },
                    onRemoveDay      = { if (days.size > 1) days.removeAt(index) },
                    onEditExercise   = { exIdx -> editingExercise = Pair(index, exIdx) }
                )
            }

            item {
                if (days.size < 7) {
                    TextButton(
                        onClick = { days.add(MutableManualDay()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = CardCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "+ YENİ GÜN  (${days.size}/7)",
                            color = CardCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ── Save button ───────────────────────────────────────────────────────
        val isLoading = uiState.isLoading
        Button(
            onClick = {
                val dayDrafts = days.mapIndexed { i, d ->
                    ManualDayDraft(
                        title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                        isRestDay = d.isRestDay,
                        selectedExercises = d.exercises.mapIndexed { ei, ex ->
                            ManualExerciseInput(
                                exerciseId  = ex.exerciseId,
                                sets        = ex.sets,
                                reps        = ex.reps,
                                restSeconds = ex.restSeconds,
                                orderIndex  = ei
                            )
                        }
                    )
                }
                viewModel.createManualProgram(
                    name = programName,
                    days = dayDrafts
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            } else {
                Text(manStrings.saveProtocol, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ── Manual Day Card ───────────────────────────────────────────────────────────

@Composable
private fun ManualDayCard(
    day             : MutableManualDay,
    dayNumber       : Int,
    onAddExercise   : () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onRemoveDay     : () -> Unit,
    onEditExercise  : (Int) -> Unit = {}
) {
    val theme  = LocalAppTheme.current
    val accent = if (day.isRestDay) Mist else CardCyan

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1.copy(0.9f))
            .border(1.dp, if (day.isRestDay) theme.stroke else accent.copy(0.3f), RoundedCornerShape(16.dp))
    ) {
        // Day header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayNumber.toString(),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(10.dp))

            // Editable title
            BasicTextField(
                value = day.title,
                onValueChange = { day.title = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = theme.text0,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (day.title.isEmpty()) {
                        Text(
                            if (day.isRestDay) "DİNLENME GÜNÜ"
                            else "GÜN $dayNumber",
                            color = theme.text2,
                            fontWeight = FontWeight.Light,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )

            // Rest day toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { day.isRestDay = !day.isRestDay }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Dinlenme",
                    color = if (day.isRestDay) accent else theme.text2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = day.isRestDay,
                    onCheckedChange = { day.isRestDay = it },
                    modifier = Modifier.size(width = 36.dp, height = 20.dp).scale(0.7f),
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Mist,
                        uncheckedTrackColor = theme.bg3
                    )
                )
            }

            IconButton(onClick = onRemoveDay, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, null, tint = theme.text2.copy(0.4f), modifier = Modifier.size(16.dp))
            }
        }

        // Exercises section (hidden when rest day)
        if (!day.isRestDay) {
            if (day.exercises.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = theme.stroke,
                    thickness = 0.5.dp
                )
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    day.exercises.forEachIndexed { i, ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.bg2)
                                .clickable { onEditExercise(i) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, color = theme.text0, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Set badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accent.copy(0.15f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("${ex.sets} SET", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Rep badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accent.copy(0.10f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("${ex.reps} TEK", color = accent.copy(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Rest badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(theme.stroke)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("${ex.restSeconds}s", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(0.2f))
                                    .clickable { onEditExercise(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(theme.bg3)
                                    .clickable { onRemoveExercise(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = theme.text1, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Hareket Ekle",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Exercise Edit Dialog ──────────────────────────────────────────────────────

@Composable
private fun ExerciseEditDialog(
    exercise : DraftExercise,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: Int, restSeconds: Int) -> Unit
) {
    var sets by remember { mutableIntStateOf(exercise.sets) }
    var reps by remember { mutableIntStateOf(exercise.reps) }
    var rest by remember { mutableIntStateOf(exercise.restSeconds) }
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "HAREKETI DÜZENLE",
                        color         = accent,
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        exercise.name,
                        color      = theme.text0,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }

            // Counter fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp)
            ) {
                EditCounterField(
                    label       = "SET",
                    value       = sets,
                    onDecrement = { if (sets > 1) sets-- },
                    onIncrement = { if (sets < 10) sets++ },
                    accent      = accent
                )
                HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)
                EditCounterField(
                    label       = "TEKRAR",
                    value       = reps,
                    onDecrement = { if (reps > 1) reps-- },
                    onIncrement = { if (reps < 100) reps++ },
                    accent      = accent
                )
                HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)
                EditCounterField(
                    label           = "DİNLENME",
                    value           = rest,
                    onDecrement     = { if (rest > 15) rest -= 15 },
                    onIncrement     = { if (rest < 300) rest += 15 },
                    accent          = accent,
                    displayOverride = "${rest}s"
                )
            }

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, theme.stroke)
                ) {
                    Text("İptal", color = theme.text1, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = { onConfirm(sets, reps, rest) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kaydet", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditCounterField(
    label          : String,
    value          : Int,
    onDecrement    : () -> Unit,
    onIncrement    : () -> Unit,
    accent         : Color,
    modifier       : Modifier = Modifier,
    displayOverride: String?  = null
) {
    val theme = LocalAppTheme.current
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color         = accent,
            fontSize      = 13.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            modifier      = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f))
                .border(1.dp, accent.copy(0.35f), CircleShape)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Remove, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(
            displayOverride ?: value.toString(),
            color      = theme.text0,
            fontWeight = FontWeight.Black,
            fontSize   = 22.sp,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            modifier   = Modifier.widthIn(min = 60.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f))
                .border(1.dp, accent.copy(0.35f), CircleShape)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Detail Header ─────────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(title: String, sub: String, onBack: () -> Unit) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 48.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, null, tint = theme.text1)
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text(title, style = MaterialTheme.typography.displayMedium, color = theme.text0, fontWeight = FontWeight.Black)
        }
    }
}
