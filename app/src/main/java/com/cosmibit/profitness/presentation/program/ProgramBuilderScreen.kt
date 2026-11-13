@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmibit.profitness.presentation.program

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.cosmibit.profitness.presentation.components.SinemaDialog
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.cosmibit.profitness.presentation.workout.SceneImage
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.data.program.ManualExerciseInput
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.data.program.autoTitle
import com.cosmibit.profitness.domain.model.Program
import com.cosmibit.profitness.presentation.components.AiCreditInfoRow
import com.cosmibit.profitness.presentation.components.AppToast
import com.cosmibit.profitness.presentation.components.AppToastData
import com.cosmibit.profitness.presentation.components.AppToastType
import com.cosmibit.profitness.presentation.workout.ExerciseMetric
import com.cosmibit.profitness.presentation.workout.activityTrackingSpec
import com.cosmibit.profitness.presentation.workout.defaultDurationSecondsForExercise
import kotlinx.coroutines.delay
import java.util.UUID

// ── Data ─────────────────────────────────────────────────────────────────────

private const val AI_MAX_UPLOAD_BYTES = 1_200_000
private const val AI_EDIT_ENERGY_COST = 4
private val AI_ALLOWED_UPLOAD_MIME = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "application/pdf"
)

private fun aiUploadLimitLabel(): String {
    val mb = AI_MAX_UPLOAD_BYTES / 1_000_000.0
    return "%.1f MB".format(mb)
}

enum class ProgramCategory(val trLabel: String, val color: Color, val icon: ImageVector) {
    ALL("TÜMÜ",          Color(0xFF94A3B8), Icons.Rounded.GridView),
    MUSCLE("KAS",        CardPurple, Icons.Rounded.FitnessCenter),
    FAT_LOSS("YAĞ YAKIMI", CardCoral, Icons.Rounded.LocalFireDepartment),
    STRENGTH("GÜÇ",     Amber,      Icons.Rounded.Bolt),
    ENDURANCE("DAYANIKLILIK", CardCyan, Icons.Rounded.DirectionsRun),
    BEGINNER("BAŞLANGIÇ", CardGreen, Icons.Rounded.StarOutline)
}

enum class ProgramSportFilter(val trLabel: String, val enLabel: String, val color: Color, val icon: ImageVector) {
    ALL("TÜM SPORLAR", "ALL SPORTS", Color(0xFF94A3B8), Icons.Rounded.GridView),
    FITNESS("FİTNESS", "FITNESS", CardPurple, Icons.Rounded.FitnessCenter),
    RUNNING("KOŞU", "RUNNING", CardCyan, Icons.Rounded.DirectionsRun),
    CYCLING("BİSİKLET", "CYCLING", CardGreen, Icons.Rounded.DirectionsBike),
    SWIMMING("YÜZME", "SWIMMING", CardCyan, Icons.Rounded.Pool),
    ROWING("KÜREK", "ROWING", Amber, Icons.Rounded.Rowing),
    WALKING("YÜRÜYÜŞ", "WALKING", CardGreen, Icons.Rounded.DirectionsWalk),
    BOXING("BOKS", "BOXING", CardCoral, Icons.Rounded.SportsMma),
    YOGA("YOGA", "YOGA", CardPurple, Icons.Rounded.SelfImprovement),
    FOOTBALL("FUTBOL", "FOOTBALL", CardGreen, Icons.Rounded.SportsSoccer),
    BASKETBALL("BASKETBOL", "BASKETBALL", Amber, Icons.Rounded.SportsBasketball),
    TENNIS("TENİS", "TENNIS", CardCyan, Icons.Rounded.SportsTennis)
}

private fun ProgramSportFilter.matches(program: ReadyProgram): Boolean {
    val token = "${program.title} ${program.goal} ${program.muscleLabels.joinToString(" ")}".uppercase()
    return when (this) {
        ProgramSportFilter.ALL -> true
        ProgramSportFilter.FITNESS -> listOf(
            "KAS KÜTLESİ",
            "YAĞ YAKIMI",
            "GÜÇ",
            "GENEL FITNESS",
            "DAYANIKLILIK"
        ).any { program.goal.uppercase() == it }
        ProgramSportFilter.RUNNING -> token.contains("KOŞU")
        ProgramSportFilter.CYCLING -> token.contains("BİSİKLET")
        ProgramSportFilter.SWIMMING -> token.contains("YÜZME")
        ProgramSportFilter.ROWING -> token.contains("KÜREK")
        ProgramSportFilter.WALKING -> token.contains("YÜRÜYÜŞ")
        ProgramSportFilter.BOXING -> token.contains("BOKS")
        ProgramSportFilter.YOGA -> token.contains("YOGA") || token.contains("MOBİLİTE")
        ProgramSportFilter.FOOTBALL -> token.contains("FUTBOL")
        ProgramSportFilter.BASKETBALL -> token.contains("BASKETBOL")
        ProgramSportFilter.TENNIS -> token.contains("TENİS")
    }
}

@Composable
internal fun ProgramSportFilter.localizedLabel(): String {
    val theme = LocalAppTheme.current
    return theme.t(trLabel, enLabel)
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

private val ReadyProgramEnglishText = mapOf(
    "Hipertrofi Klasiği" to "Hypertrophy Classic",
    "Schwarzenegger'ın Hacim Protokolü" to "Schwarzenegger Volume Protocol",
    "PHUL — Güç & Hacim Dengesi" to "PHUL - Strength & Hypertrophy Balance",
    "Klasik 5 Gün Bölme" to "Classic 5-Day Split",
    "Metabolik Kondisyon" to "Metabolic Conditioning",
    "HIIT Metabolik" to "Metabolic HIIT",
    "Yüksek Yoğunluklu Yağ Yakımı" to "High Intensity Fat Loss",
    "Tam Vücut Yağ Yakımı" to "Full Body Fat Loss",
    "Yoğun Devre Antrenmanı" to "Intense Circuit Training",
    "5×5 Güç" to "5×5 Strength",
    "StrongLifts — Lineer Progresyon" to "StrongLifts - Linear Progression",
    "Powerlifting Temel" to "Powerlifting Basics",
    "Rekabet Gücü Protokolü" to "Competition Strength Protocol",
    "Tier Tabanlı Güç Protokolü" to "Tier-Based Strength Protocol",
    "Kardiyovasküler Temel" to "Cardiovascular Base",
    "Aerobik Kapasite Gelişimi" to "Aerobic Capacity Development",
    "Güç + Dayanıklılık Hibrit" to "Strength + Endurance Hybrid",
    "Bisiklet 4 Hafta" to "Cycling 4 Weeks",
    "Sürüş + Bacak Kuvveti" to "Riding + Leg Strength",
    "5K Koşu Temeli" to "5K Running Base",
    "Koşu + Runner Strength" to "Running + Runner Strength",
    "Yüzme Temel" to "Swimming Basics",
    "Yüzme + Omuz/Sırt Dayanıklılığı" to "Swimming + Shoulder/Back Endurance",
    "Kürek Erg Temel" to "Rowing Erg Basics",
    "Erg + Total Body Kuvvet" to "Erg + Total Body Strength",
    "Yürüyüş & Hiking" to "Walking & Hiking",
    "Düşük Etki Dayanıklılık" to "Low Impact Endurance",
    "Boks Kondisyon" to "Boxing Conditioning",
    "Round + Kuvvet + Kondisyon" to "Rounds + Strength + Conditioning",
    "Yoga Mobilite" to "Yoga Mobility",
    "Mobilite + Destek Kuvveti" to "Mobility + Support Strength",
    "Futbol Atletik" to "Football Athletic",
    "Saha Kondisyonu + Alt Vücut" to "Field Conditioning + Lower Body",
    "Basketbol Atletik" to "Basketball Athletic",
    "Sıçrama + Court Conditioning" to "Jumping + Court Conditioning",
    "Tenis Atletik" to "Tennis Athletic",
    "Ayak Çalışması + Rotasyon" to "Footwork + Rotation",
    "Full Body Başlangıç" to "Full Body Beginner",
    "Starting Strength Tabanlı" to "Starting Strength Based",
    "Vücut Ağırlığı" to "Bodyweight",
    "Ekipmansız Kalistenik" to "No-Equipment Calisthenics",
    "Başlangıç" to "Beginner",
    "Orta" to "Intermediate",
    "İleri" to "Advanced",
    "Kas Kütlesi" to "Muscle Mass",
    "Yağ Yakımı" to "Fat Loss",
    "Güç" to "Strength",
    "Dayanıklılık" to "Endurance",
    "Bisiklet" to "Cycling",
    "Koşu" to "Running",
    "Yüzme" to "Swimming",
    "Kürek" to "Rowing",
    "Yürüyüş" to "Walking",
    "Boks" to "Boxing",
    "Mobilite" to "Mobility",
    "Futbol" to "Football",
    "Basketbol" to "Basketball",
    "Tenis" to "Tennis",
    "Genel Fitness" to "General Fitness",
    "ÜST VÜCUT" to "UPPER BODY",
    "ALT VÜCUT" to "LOWER BODY",
    "GÜÇ" to "STRENGTH",
    "KARDİYO" to "CARDIO",
    "BİSİKLET" to "CYCLING",
    "KOŞU" to "RUNNING",
    "YÜZME" to "SWIMMING",
    "KÜREK" to "ROWING",
    "YÜRÜYÜŞ" to "WALKING",
    "BOKS" to "BOXING",
    "FUTBOL" to "FOOTBALL",
    "BASKETBOL" to "BASKETBALL",
    "TENİS" to "TENNIS",
    "SIRT/OMUZ" to "BACK/SHOULDERS",
    "SIRT" to "BACK",
    "MOBİLİTE" to "MOBILITY",
    "KONDİSYON" to "CONDITIONING",
    "PATLAYICI GÜÇ" to "EXPLOSIVE POWER",
    "ROTASYON" to "ROTATION",
    "Her kas grubuna haftada 2 kez uyarı veren Reddit PPL tabanlı 6 günlük program. Push/Pull/Legs döngüsü ile hacim ve frekans dengesini optimize eder." to "A Reddit PPL-based 6-day program that stimulates each muscle group twice per week. The Push/Pull/Legs cycle optimizes the balance between training volume and frequency.",
    "Arnold Schwarzenegger'ın 'The Education of a Bodybuilder' kitabında anlattığı orijinal 6 günlük split. Göğüs+Sırt süpersetleri maksimum kasılma ve kan akışı sağlar." to "The original 6-day split Arnold Schwarzenegger described in 'The Education of a Bodybuilder'. Chest and back supersets maximize contraction, pump and blood flow.",
    "Power Hypertrophy Upper Lower (PHUL) protokolü. Haftanın 2 günü düşük tekrar güç, 2 günü yüksek tekrar hacim antrenmanı ile hem kuvvet hem kas kütlesi gelişimi sağlar." to "A Power Hypertrophy Upper Lower (PHUL) protocol. Two low-rep strength days and two higher-rep hypertrophy days build both strength and muscle mass.",
    "Her kas grubuna haftada bir gün yüksek hacimli çalışma. Göğüs, sırt, omuz, kol ve bacak için izole günlerle maksimum pompa ve kas hasarı yaratır." to "A high-volume split that gives each muscle group one dedicated day per week. Isolated chest, back, shoulder, arm and leg days are built for maximum pump and muscle damage.",
    "30 sn çalışma / 30 sn dinlenme interval protokolü. EPOC (afterburn) etkisiyle antrenman sonrası 24–48 saat boyunca kalori yakımı yüksek kalır. Araştırmalar bu formatin düzenli kardiyoya göre %28 daha fazla yağ yaktığını göstermektedir." to "A 30 sec work / 30 sec rest interval protocol. It keeps post-workout calorie burn high through the EPOC effect and is built for efficient conditioning.",
    "Haftada 3 gün, dinlenme günleriyle ayrılmış tam vücut devre antrenmanı. Bileşik hareketler kalp atış hızını yüksek tutarak hem kas korur hem yağ yakar. Başlangıç seviyesi için idealdir." to "A 3-day full body circuit plan separated by recovery days. Compound movements keep heart rate high, helping preserve muscle while supporting fat loss. Ideal for beginners.",
    "5 günlük yüksek yoğunluklu devre formatı. Kısa dinlenme süreleri metabolik stresin zirvede tutulmasını sağlar. Kas kütlesini korurken agresif yağ yakımı için CrossFit metodolojisinden ilham alınmıştır." to "A 5-day high-intensity circuit format. Short rest periods keep metabolic stress high, with CrossFit-inspired structure for aggressive fat loss while preserving muscle mass.",
    "StrongLifts 5×5 protokolü. Haftada 3 gün A/B antrenman rotasyonu. Her seansta ağırlığı 2,5 kg artırarak lineer güç gelişimi sağlar. Squat her seansta yapılır; kuvvet programlarının en kanıtlanmış temelidir." to "A StrongLifts 5x5 protocol using a 3-day A/B workout rotation. Weight increases by 2.5 kg each session for linear strength progress, with squats performed every session.",
    "Squat, bench press ve deadlift odaklı 16 haftalık periodizasyon. Her hafta ayrı bir lift için yoğun çalışma yapılır; aksesuar günü zayıf noktaları kapatır. Rekabetçi powerlifting sahnesine uygun yapı." to "A 16-week periodized plan focused on the squat, bench press and deadlift. Each week emphasizes a different lift, while accessory work targets weak points for competitive powerlifting.",
    "u/Sayings tarafından geliştirilen GZCLP sistemi: T1 (5×3 ağır), T2 (3×10 orta), T3 (2×15 aksesuar). Her tier kendi progresyon kuralına göre ilerler. Verimli ve bilimsel tabanlı bir güç artış programıdır." to "The GZCLP system developed by u/Sayings: T1 heavy work, T2 moderate volume and T3 accessories. Each tier follows its own progression rule for efficient strength development.",
    "Koşu, bisiklet ve kürek çekme kombinasyonuyla aerobik baz oluşturan 4 günlük program. Uzun tempo koşusu aerobik kapasiteyi, interval günleri laktik eşiği geliştirir. 5K/10K koşusuna hazırlık için idealdir." to "A 4-day plan that builds an aerobic base through running, cycling and rowing. Long runs develop aerobic capacity, while interval days improve lactate threshold. Ideal preparation for 5K/10K running.",
    "Nick Bare tarzı hibrit antrenman. Haftada 2 gün barbell kuvvet çalışması, 2 gün metabolik kondisyon, 1 gün aerobik kapasite. Hem kuvveti hem VO2max'ı geliştiren az kişinin yaptığı programdır." to "A Nick Bare-style hybrid plan with 2 barbell strength days, 2 metabolic conditioning days and 1 aerobic capacity day. Built to improve both strength and VO2 max.",
    "Bisikletçi mantığında kurulmuş haftalık yapı: zone 2 sürüş, tırmanış/tempo, uzun sürüş, bacak kuvveti ve core stabilizasyonu. Sadece pedal çevirmek değil, sele üstündeki gücü taşıyacak alt vücut ve posterior chain de çalışır." to "A cyclist-focused weekly structure with zone 2 riding, climbing/tempo work, a long ride, leg strength and core stabilization. It trains the lower body and posterior chain that support power on the bike.",
    "5K için yalnızca koşu değil; kolay koşu, interval, uzun koşu, tek bacak kuvveti, kalça/hamstring ve calf dayanıklılığı içerir. Koşucuda sakatlık riskini azaltan stabilite işleri de eklenmiştir." to "More than just running for a 5K: easy runs, intervals, long runs, single-leg strength, hip/hamstring work and calf endurance. Stability work is included to help reduce injury risk for runners.",
    "Havuz mesafesiyle birlikte lat, scapula, omuz stabilitesi ve core çalışır. Teknik yüzme, threshold yüzme, mesafe günü ve kuru kara kuvveti gerçekçi şekilde dengelenir." to "Pairs pool distance with lat, scapular, shoulder stability and core work. Technique swims, threshold swims, distance work and dry-land strength are balanced realistically.",
    "Kürekçinin ihtiyacı olan bacak itişi, kalça menteşesi, sırt çekişi ve core transferi için erg günlerini deadlift/squat/row kuvvet günüyle birleştirir." to "Combines erg sessions with deadlift, squat and row strength work for the leg drive, hip hinge, back pull and core transfer a rower needs.",
    "Uzun yürüyüş ve eğim günlerinin yanına hiking için bacak kuvveti, calf dayanıklılığı, kalça/hamstring ve mobilite ekler. Doğa yürüyüşüne daha gerçekçi hazırlanır." to "Adds leg strength, calf endurance, hip/hamstring work and mobility alongside long walks and incline days. A more realistic preparation path for hiking.",
    "Boks için round çalışması, ip atlama, omuz/sırt dayanıklılığı, core rotasyonu ve fight conditioning bloklarını birleştirir. Sadece shadow boxing değil, ring kondisyonuna yakın bir yapı." to "Combines boxing rounds, jump rope, shoulder/back endurance, core rotation and fight conditioning blocks. Built closer to ring conditioning than simple shadow boxing.",
    "Yoga akışlarını core, posterior chain ve kontrollü kuvvet destekleriyle tamamlar. Esneklik/toparlanma hedefini korurken vücudu daha dayanıklı hale getirir." to "Combines yoga flows with core, posterior chain and controlled strength support while keeping the focus on mobility and recovery.",
    "Futbol için aerobik taban, alt vücut kuvveti, sprint/interval kondisyonu, core ve patlayıcı güç bloklarını birleştirir. Maç temposuna yakın ama spor salonunda uygulanabilir yapı." to "Combines an aerobic base, lower-body strength, sprint/interval conditioning, core and explosive power blocks for football. Match-tempo inspired, but practical for the gym.",
    "Basketbolcu için sıçrama mekaniği, tek bacak kuvveti, calf dayanıklılığı, üst vücut denge çalışması ve kısa aralıklı kondisyon içerir." to "Includes jump mechanics, single-leg strength, calf endurance, upper-body balance work and short-interval conditioning for basketball players.",
    "Tenis için ayak çalışması kondisyonu, rotasyonel core, omuz/sırt dayanıklılığı ve alt vücut destek kuvvetini birleştirir." to "Combines footwork conditioning, rotational core, shoulder/back endurance and lower-body support strength for tennis.",
    "Mark Rippetoe'nun Starting Strength felsefesinden ilham alınan 3 günlük tam vücut programı. A/B/C rotasyonuyla squat, bench, row, deadlift ve OHP temel hareketleri öğretilir. Kas ve güç için en sağlam başlangıç noktasıdır." to "A 3-day full body plan inspired by Mark Rippetoe's Starting Strength philosophy. The A/B/C rotation teaches foundational squat, bench, row, deadlift and overhead press patterns.",
    "Sıfır ekipmanla yapılabilen 3 günlük kalistenik program. Push-up, pull-up, dips ve squat varyasyonlarıyla tüm vücut kası ve fonksiyonel kuvvet gelişimi sağlar. Ev, park veya seyahatte uygulanabilir." to "A 3-day calisthenics plan that can be done with no equipment. Push-up, pull-up, dip and squat variations develop full body muscle and functional strength at home, in a park or while traveling."
)

private fun ReadyProgram.localizedText(value: String, theme: AppThemeState): String =
    if (theme.language == AppLanguage.ENGLISH) ReadyProgramEnglishText[value] ?: value else value

internal fun ReadyProgram.localizedTitle(theme: AppThemeState): String = localizedText(title, theme)
private fun ReadyProgram.localizedSubtitle(theme: AppThemeState): String = localizedText(subtitle, theme)
internal fun ReadyProgram.localizedLevel(theme: AppThemeState): String = localizedText(level, theme)
private fun ReadyProgram.localizedDescription(theme: AppThemeState): String =
    if (theme.language == AppLanguage.ENGLISH) {
        when (title) {
            "HIIT Metabolik" -> "A 30 sec work / 30 sec rest interval protocol. It keeps post-workout calorie burn high through the EPOC effect and is built for efficient conditioning."
            "Yoga Mobilite" -> "Combines yoga flows with core, posterior chain and controlled strength support while keeping the focus on mobility and recovery."
            "Kürek Erg Temel" -> "Combines erg sessions with deadlift, squat and row strength work for leg drive, hip hinge, back pull and core transfer."
            "Push / Pull / Legs" -> "A 6-day Push/Pull/Legs program that trains each muscle group twice per week and balances volume with frequency."
            else -> localizedText(description, theme)
        }
    } else description

private fun ReadyProgram.localizedSchedule(theme: AppThemeState): String =
    if (theme.language == AppLanguage.ENGLISH) {
        schedule
            .replace("Pzt", "Mon")
            .replace("Paz", "Sun")
            .replace("Sal", "Tue")
            .replace("Çar", "Wed")
            .replace("Per", "Thu")
            .replace("Cum", "Fri")
            .replace("Cmt", "Sat")
            .replace("Dinlenme", "Rest")
            .replace("Düşük Tempolu Uzun Koşu", "Easy Long Run")
            .replace("Orta-Yüksek Hızda", "Medium-High Pace")
            .replace("Uzun Koşu", "Long Run")
            .replace("Bisiklet", "Cycling")
            .replace("Kürek", "Rowing")
            .replace("Devre", "Circuit")
            .replace("Hacim", "Volume")
            .replace("Göğüs", "Chest")
            .replace("Omuz", "Shoulders")
            .replace("Sırt", "Back")
            .replace("Baldır", "Calves")
            .replace("Patlayıcı", "Explosive")
            .replace("yoğun", "heavy")
    } else schedule

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
    ReadyProgram(
        "Bisiklet 4 Hafta",
        "Sürüş + Bacak Kuvveti",
        ProgramCategory.ENDURANCE, 5, 4, "Başlangıç",
        "Bisiklet",
        "Bisikletçi mantığında kurulmuş haftalık yapı: zone 2 sürüş, tırmanış/tempo, uzun sürüş, bacak kuvveti ve core stabilizasyonu. Sadece pedal çevirmek değil, sele üstündeki gücü taşıyacak alt vücut ve posterior chain de çalışır.",
        "📌 Pzt: Zone 2 Ride + Core\n📌 Sal: Cyclist Strength\n📌 Per: Tempo & Climb Ride\n📌 Cum: Bike Conditioning\n📌 Paz: Long Ride",
        listOf("BİSİKLET", "ALT VÜCUT", "CORE"),
        listOf("88%", "78%", "62%"),
        listOf(0.88f, 0.78f, 0.62f)
    ),
    ReadyProgram(
        "5K Koşu Temeli",
        "Koşu + Runner Strength",
        ProgramCategory.ENDURANCE, 4, 6, "Başlangıç",
        "Koşu",
        "5K için yalnızca koşu değil; kolay koşu, interval, uzun koşu, tek bacak kuvveti, kalça/hamstring ve calf dayanıklılığı içerir. Koşucuda sakatlık riskini azaltan stabilite işleri de eklenmiştir.",
        "📌 Pzt: Easy Run + Stability\n📌 Sal: Runner Strength\n📌 Per: Interval Run\n📌 Cmt: Long Run + Mobility",
        listOf("KOŞU", "ALT VÜCUT", "CORE"),
        listOf("86%", "64%", "52%"),
        listOf(0.86f, 0.64f, 0.52f)
    ),
    ReadyProgram(
        "Yüzme Temel",
        "Yüzme + Omuz/Sırt Dayanıklılığı",
        ProgramCategory.ENDURANCE, 4, 4, "Başlangıç",
        "Yüzme",
        "Havuz mesafesiyle birlikte lat, scapula, omuz stabilitesi ve core çalışır. Teknik yüzme, threshold yüzme, mesafe günü ve kuru kara kuvveti gerçekçi şekilde dengelenir.",
        "📌 Pzt: Technique Swim + Scapula\n📌 Çar: Swimmer Strength\n📌 Cum: Threshold Swim\n📌 Paz: Distance Swim",
        listOf("YÜZME", "SIRT/OMUZ", "CORE"),
        listOf("78%", "64%", "58%"),
        listOf(0.78f, 0.64f, 0.58f)
    ),
    ReadyProgram(
        "Kürek Erg Temel",
        "Erg + Total Body Kuvvet",
        ProgramCategory.ENDURANCE, 4, 4, "Orta",
        "Kürek",
        "Kürekçinin ihtiyacı olan bacak itişi, kalça menteşesi, sırt çekişi ve core transferi için erg günlerini deadlift/squat/row kuvvet günüyle birleştirir.",
        "📌 Pzt: Technique Erg + Core\n📌 Sal: Rowing Strength\n📌 Per: Erg Intervals\n📌 Cmt: Long Erg",
        listOf("KÜREK", "SIRT", "ALT VÜCUT"),
        listOf("80%", "70%", "72%"),
        listOf(0.80f, 0.70f, 0.72f)
    ),
    ReadyProgram(
        "Yürüyüş & Hiking",
        "Düşük Etki Dayanıklılık",
        ProgramCategory.ENDURANCE, 4, 4, "Başlangıç",
        "Yürüyüş",
        "Uzun yürüyüş ve eğim günlerinin yanına hiking için bacak kuvveti, calf dayanıklılığı, kalça/hamstring ve mobilite ekler. Doğa yürüyüşüne daha gerçekçi hazırlanır.",
        "📌 Pzt: Easy Walk + Mobility\n📌 Sal: Hiking Strength\n📌 Per: Incline Walk\n📌 Paz: Long Walk",
        listOf("YÜRÜYÜŞ", "ALT VÜCUT", "MOBİLİTE"),
        listOf("76%", "58%", "48%"),
        listOf(0.76f, 0.58f, 0.48f)
    ),
    ReadyProgram(
        "Boks Kondisyon",
        "Round + Kuvvet + Kondisyon",
        ProgramCategory.FAT_LOSS, 4, 4, "Orta",
        "Boks",
        "Boks için round çalışması, ip atlama, omuz/sırt dayanıklılığı, core rotasyonu ve fight conditioning bloklarını birleştirir. Sadece shadow boxing değil, ring kondisyonuna yakın bir yapı.",
        "📌 Pzt: Skill Rounds + Rope\n📌 Sal: Boxing Strength\n📌 Per: Fight Conditioning\n📌 Cmt: Mixed Intervals",
        listOf("BOKS", "HIIT", "CORE"),
        listOf("78%", "86%", "62%"),
        listOf(0.78f, 0.86f, 0.62f)
    ),
    ReadyProgram(
        "Yoga Mobilite",
        "Mobilite + Destek Kuvveti",
        ProgramCategory.BEGINNER, 4, 4, "Başlangıç",
        "Mobilite",
        "Yoga akışlarını core, posterior chain ve kontrollü kuvvet destekleriyle tamamlar. Esneklik/toparlanma hedefini korurken vücudu daha dayanıklı hale getirir.",
        "📌 Pzt: Flow + Core\n📌 Sal: Posterior Chain Support\n📌 Per: Mobility + Breath\n📌 Paz: Deep Stretch",
        listOf("YOGA", "MOBİLİTE", "CORE"),
        listOf("46%", "58%", "68%"),
        listOf(0.46f, 0.58f, 0.68f)
    ),
    ReadyProgram(
        "Futbol Atletik",
        "Saha Kondisyonu + Alt Vücut",
        ProgramCategory.ENDURANCE, 4, 4, "Orta",
        "Futbol",
        "Futbol için aerobik taban, alt vücut kuvveti, sprint/interval kondisyonu, core ve patlayıcı güç bloklarını birleştirir. Maç temposuna yakın ama spor salonunda uygulanabilir yapı.",
        "📌 Pzt: Aerobic Base + Core\n📌 Sal: Lower Strength\n📌 Per: Speed Conditioning\n📌 Cmt: Power + Mobility",
        listOf("FUTBOL", "ALT VÜCUT", "KONDİSYON"),
        listOf("82%", "76%", "72%"),
        listOf(0.82f, 0.76f, 0.72f)
    ),
    ReadyProgram(
        "Basketbol Atletik",
        "Sıçrama + Court Conditioning",
        ProgramCategory.ENDURANCE, 4, 4, "Orta",
        "Basketbol",
        "Basketbolcu için sıçrama mekaniği, tek bacak kuvveti, calf dayanıklılığı, üst vücut denge çalışması ve kısa aralıklı kondisyon içerir.",
        "📌 Pzt: Jump Mechanics\n📌 Sal: Strength Base\n📌 Per: Court Conditioning\n📌 Cmt: Unilateral + Core",
        listOf("BASKETBOL", "PATLAYICI GÜÇ", "CORE"),
        listOf("74%", "82%", "58%"),
        listOf(0.74f, 0.82f, 0.58f)
    ),
    ReadyProgram(
        "Tenis Atletik",
        "Ayak Çalışması + Rotasyon",
        ProgramCategory.ENDURANCE, 4, 4, "Orta",
        "Tenis",
        "Tenis için ayak çalışması kondisyonu, rotasyonel core, omuz/sırt dayanıklılığı ve alt vücut destek kuvvetini birleştirir.",
        "📌 Pzt: Footwork Conditioning\n📌 Sal: Rotational Strength\n📌 Per: Lower Body Support\n📌 Cmt: Match Conditioning",
        listOf("TENİS", "ROTASYON", "ALT VÜCUT"),
        listOf("78%", "64%", "62%"),
        listOf(0.78f, 0.64f, 0.62f)
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
    data class Edit(val program: com.cosmibit.profitness.domain.model.Program) : BuilderMode()
}

@Composable
fun ProgramBuilderScreen(
    initialMode      : BuilderMode = BuilderMode.Choose,
    timerExtraPad    : androidx.compose.ui.unit.Dp = 0.dp,
    onNavigateToStore: () -> Unit = {},
    onProgramShared  : () -> Unit = {},
    todayInfo        : ProgramTodayInfo? = null,
    onStartWorkout   : () -> Unit = {},
    viewModel        : ProgramViewModel = hiltViewModel()
) {
    var showPaywall by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<Program?>(null) }
    var shareFeatureInitialized by remember { mutableStateOf(false) }
    val shareViewModel: ProgramShareViewModel? =
        if (shareFeatureInitialized) hiltViewModel() else null
    val shareState = shareViewModel
        ?.state
        ?.collectAsStateWithLifecycle()
        ?.value
        ?: ProgramShareUiState()
    var currentToast by remember { mutableStateOf<AppToastData?>(null) }

    LaunchedEffect(shareState.result, shareViewModel) {
        shareState.result?.let { r ->
            if (r is ProgramShareResult.Success) {
                onProgramShared()
            }
            currentToast = AppToastData(
                message = when (r) {
                    ProgramShareResult.Success -> "Program topluluk akışına eklendi"
                    is ProgramShareResult.Error -> "Paylaşım başarısız: ${r.message}"
                },
                type = if (r is ProgramShareResult.Success) AppToastType.Success else AppToastType.Error
            )
            shareViewModel?.consumeResult()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf<BuilderMode>(initialMode) }
    val theme   = LocalAppTheme.current
    val strings = theme.strings

    BackHandler(
        enabled = uiState.applyingTemplateKey != null || showPaywall || shareTarget != null || mode !is BuilderMode.Choose
    ) {
        when {
            uiState.applyingTemplateKey != null -> Unit
            showPaywall       -> showPaywall = false
            shareTarget != null -> shareTarget = null
            mode !is BuilderMode.Choose -> mode = BuilderMode.Choose
        }
    }

    // Tab geçişinde stale ise yenile (3 dk cache)
    LaunchedEffect(Unit) {
        delay(120)
        viewModel.reloadIfStale()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProgramEvent.ShowSnackbar -> currentToast = AppToastData(
                    message = event.message,
                    type = AppToastType.Info
                )
                ProgramEvent.NavigateBack    -> mode = BuilderMode.Choose
                ProgramEvent.ShowPaywall     -> showPaywall = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Crossfade(
            targetState = mode,
            animationSpec = tween(140, easing = FastOutSlowInEasing),
            label = "builder_fade"
        ) { m ->
            Box(
                Modifier.fillMaxSize().then(
                    if (m is BuilderMode.Choose) Modifier else Modifier.background(Sinema.page(theme))
                )
            ) {
            when (m) {
                is BuilderMode.Choose -> BuilderChooseScreen(
                    userPrograms     = uiState.userPrograms.filterNot { it.id in uiState.deletingProgramIds },
                    isLoading        = uiState.isLoading,
                    applyingTemplateKey = uiState.applyingTemplateKey,
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
                    onShareProgram   = { prog ->
                        shareFeatureInitialized = true
                        shareTarget = prog
                    },
                    todayInfo        = todayInfo,
                    onStartWorkout   = onStartWorkout,
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
            }
        }

        AppToast(
            toast = currentToast,
            onDismiss = { currentToast = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Paywall dialog
        if (showPaywall) {
            com.cosmibit.profitness.presentation.store.PaywallDialog(
                onDismiss   = { showPaywall = false },
                onGoToStore = {
                    showPaywall = false
                    onNavigateToStore()
                }
            )
        }

        // Program paylaşım sheet'i (kart üzerindeki "PAYLAŞ" ile açılır)
        shareTarget?.let { target ->
            val shareVm = shareViewModel ?: return@let
            com.cosmibit.profitness.presentation.discover.ShareProgramSheet(
                programs             = uiState.userPrograms,
                preselectedProgramId = target.id,
                onDismiss            = { shareTarget = null },
                onConfirm            = { programId, title, desc, tags, difficulty, weeks, days ->
                    shareVm.shareProgram(programId, title, desc, tags, difficulty, weeks, days)
                    shareTarget = null
                }
            )
        }
    }
}

// ── Choose Screen ─────────────────────────────────────────────────────────────

@Composable
private fun BuilderChooseScreen(
    userPrograms   : List<Program>,
    isLoading      : Boolean,
    applyingTemplateKey: String?,
    onMode         : (BuilderMode) -> Unit,
    onSelectTemplate: (String) -> Unit,
    onSetActive    : (String) -> Unit,
    onDeleteProgram: (String) -> Unit,
    onEditProgram  : (Program) -> Unit,
    onShareProgram : (Program) -> Unit,
    todayInfo      : ProgramTodayInfo? = null,
    onStartWorkout : () -> Unit = {},
    timerExtraPad  : androidx.compose.ui.unit.Dp = 0.dp
) {
    var selectedProgram by remember { mutableStateOf<ReadyProgram?>(null) }
    var activeSport by remember { mutableStateOf(ProgramSportFilter.ALL) }
    var activeCategory by remember { mutableStateOf(ProgramCategory.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var closeDialogAfterApply by remember { mutableStateOf(false) }
    val isApplyingTemplate = applyingTemplateKey != null

    LaunchedEffect(applyingTemplateKey) {
        if (closeDialogAfterApply && applyingTemplateKey == null) {
            selectedProgram = null
            closeDialogAfterApply = false
        }
    }

    selectedProgram?.let { prog ->
        ProgramDetailDialog(
            program   = prog,
            isApplying = isApplyingTemplate,
            onDismiss = { if (!isApplyingTemplate) selectedProgram = null },
            onApply   = {
                if (!isApplyingTemplate) {
                    closeDialogAfterApply = true
                    onSelectTemplate(prog.title)
                }
            }
        )
    }

    val randomizedPrograms = remember { READY_PROGRAMS.shuffled() }
    val filtered = remember(randomizedPrograms, activeSport, activeCategory) {
        randomizedPrograms.filter { program ->
            activeSport.matches(program) &&
                (activeCategory == ProgramCategory.ALL || program.category == activeCategory)
        }
    }

    val theme = LocalAppTheme.current
    val sectionStrings = theme.strings
    val responsive = rememberResponsiveLayoutInfo()

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Sinema.surface(theme),
            dragHandle = { BottomSheetDefaults.DragHandle(color = theme.text2.copy(0.45f)) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(theme.t("Program odağı", "Program focus"), style = MaterialTheme.typography.headlineMedium, color = theme.text0)
                Spacer(Modifier.height(16.dp))
                ProgramCategory.values().forEach { category ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                            activeCategory = category; showFilterSheet = false
                        }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(category.icon, null, tint = if (category == activeCategory) MaterialTheme.colorScheme.primary else theme.text1)
                        Spacer(Modifier.width(12.dp))
                        Text(category.localizedLabel(), color = theme.text0, modifier = Modifier.weight(1f))
                        if (category == activeCategory) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Sinema.page(theme))) {
    val activeProgram = userPrograms.firstOrNull { it.isActive }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = com.cosmibit.profitness.core.theme.SinemaNavHeight + 24.dp + timerExtraPad)
    ) {
        item { ProgramsHero(active = activeProgram, today = todayInfo, onStart = onStartWorkout, onOpen = activeProgram?.let { p -> { onEditProgram(p) } }) }
        item {
            Box(Modifier.offset(y = (-28).dp)) {
                ProgramsSheet {
                    ProgramsMakeRow(onAi = { onMode(BuilderMode.AI) }, onManual = { onMode(BuilderMode.Manual) })
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    } else if (userPrograms.isNotEmpty()) {
                        ProgramsSection(theme.t("Kayıtlı programlar", "Saved programs"), userPrograms.size)
                        SavedProgramsRow(
                            programs = userPrograms,
                            onSetActive = { onSetActive(it.id) }, onDelete = { onDeleteProgram(it.id) },
                            onEdit = { onEditProgram(it) }, onShare = { onShareProgram(it) }
                        )
                    }
                    ProgramsSection(sectionStrings.readyPrograms, filtered.size, topGap = if (userPrograms.isNotEmpty() && !isLoading) 4.dp else 26.dp)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        item {
                            ProgramFilterPill(
                                label = theme.t("Filtreler", "Filters"), icon = Icons.Rounded.Tune,
                                selected = activeCategory != ProgramCategory.ALL, onClick = { showFilterSheet = true }
                            )
                        }
                        items(ProgramSportFilter.values()) { sport ->
                            ProgramFilterPill(
                                label = sport.localizedLabel(), icon = null,
                                selected = sport == activeSport, onClick = { activeSport = sport }
                            )
                        }
                    }
                    filtered.forEach { prog ->
                        ReadyProgramCard(
                            title = prog.localizedTitle(theme),
                            tag = prog.category.localizedLabel(),
                            chips = listOf(
                                "${prog.days} ${sectionStrings.dayLabel}",
                                "${prog.weeks} ${sectionStrings.weekLabel}",
                                prog.localizedLevel(theme)
                            ),
                            photoUrl = com.cosmibit.profitness.core.ui.categoryImageUrl(prog.muscleLabels.firstOrNull().orEmpty()),
                            onClick = { selectedProgram = prog }
                        )
                    }
                }
            }
        }
    }
    Box(Modifier.align(Alignment.TopStart)) { ProgramsTopScrim() }
    }
}

// ── Program Detail Dialog ─────────────────────────────────────────────────────

@Composable
private fun ProgramDetailDialog(
    program: ReadyProgram,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit = onDismiss
) {
    val theme   = LocalAppTheme.current
    val accent  = theme.effectiveAccentColor
    val strings = theme.strings
    val catalog = rememberExerciseCatalog()
    var viewerExercise by remember { mutableStateOf<com.cosmibit.profitness.presentation.workout.Exercise?>(null) }
    val templateDays = remember(program.title) {
        com.cosmibit.profitness.data.program.PROGRAM_TEMPLATES.firstOrNull { it.key == program.title }?.days.orEmpty()
    }
    ProgramExerciseSheet(viewerExercise) { viewerExercise = null }

    Dialog(
        onDismissRequest = { if (!isApplying) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = !isApplying,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Sinema.page(theme))
        ) {
            val accentSolid = theme.effectiveAccentColor
            val haze = remember { HazeState() }
            Column(
                modifier = Modifier.hazeSource(haze).verticalScroll(rememberScrollState())
            ) {
                // Kapak: ekran yüksekliğinin ~%45'i, durum çubuğunun altına uzanır
                val coverHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp.coerceAtLeast(300.dp)
                Box(Modifier.fillMaxWidth().height(coverHeight).clipToBounds()) {
                    SinemaScene(Modifier.fillMaxSize())
                    SceneImage(
                        url = com.cosmibit.profitness.core.ui.categoryImageUrl(program.muscleLabels.firstOrNull().orEmpty()),
                        modifier = Modifier.fillMaxSize(),
                        requestWidthPx = 1080, requestHeightPx = 840
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0f to Color.Black.copy(0.45f), 0.40f to Color.Black.copy(0.10f), 1f to Color.Black.copy(0.88f))
                        )
                    )
                    SinemaCapsuleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        description = theme.t("Kapat", "Close"),
                        onClick = { if (!isApplying) onDismiss() },
                        selected = false,
                        size = 48.dp,
                        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 8.dp)
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(
                            "${program.category.localizedLabel()} · ${program.localizedLevel(theme)}".sinemaUpper(theme),
                            color = Sinema.White.copy(0.80f), style = Sinema.Label, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(program.localizedTitle(theme), color = Sinema.White, style = Sinema.ScreenTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 128.dp).navigationBarsPadding()) {
                    Text(program.localizedSubtitle(theme), color = Sinema.textSecondary(theme), fontSize = 14.sp)

                    Spacer(Modifier.height(20.dp))

                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sinemaCard(theme, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DialogStat(Icons.Rounded.CalendarMonth, "${program.days}", strings.dayLabel, accentSolid, Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(56.dp).background(Sinema.line(theme)))
                        DialogStat(Icons.Rounded.Schedule, "${program.weeks}", strings.weekLabel, accentSolid, Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(56.dp).background(Sinema.line(theme)))
                        // Seviye rakam değil kelime: daha küçük stil, tam metin (kesilmez)
                        DialogStat(Icons.Rounded.TrackChanges, program.localizedLevel(theme).uppercase(), strings.levelLabel, accentSolid, Modifier.weight(1f), valueSize = 15.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Description
                    Text(program.localizedDescription(theme), color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 21.sp)

                    Spacer(Modifier.height(24.dp))

                    // Muscle distribution
                    Text(strings.muscleDistLabel.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
                    Spacer(Modifier.height(12.dp))
                    program.muscleLabels.forEachIndexed { i, label ->
                        Column(Modifier.padding(bottom = 12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(program.localizedText(label, theme), color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(program.musclePct[i], color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Sinema.line(theme))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(program.muscleFractions[i])
                                        .fillMaxHeight()
                                        .sinemaGlow(accentSolid.copy(0.4f), 8.dp, 2.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(accentSolid)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Schedule
                    Text(strings.scheduleLabel.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sinemaCard(theme, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text(program.localizedSchedule(theme), color = Sinema.text(theme), fontSize = 13.sp, lineHeight = 22.sp)
                    }

                    templateDays.filter { !it.isRestDay && it.exercises.isNotEmpty() }.forEachIndexed { dayIdx, day ->
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "${dayIdx + 1} · ${day.title}".sinemaUpper(theme),
                            color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                            day.exercises.forEachIndexed { i, ex ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
                                val open = {
                                    viewerExercise = catalog.toViewerExercise(
                                        ex.exerciseName, ex.sets, ex.reps, ex.restSeconds, ex.targetDurationSeconds
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = open).padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        theme.exerciseDisplayName(ex.exerciseName), color = Sinema.text(theme), fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (ex.targetDurationSeconds != null) "${ex.targetDurationSeconds} ${theme.t("SN", "SEC")}" else "${ex.sets}×${ex.reps}",
                                        color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                    ExercisePlayButton(onClick = open, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Altta SABİT "Programı uygula": arkası yumuşak geçiş, içerik üstüne biner
            FrostedBottomBar(
                haze = haze,
                base = Sinema.page(theme),
                modifier = Modifier.align(Alignment.BottomCenter),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Box(Modifier.navigationBarsPadding()) {
                    SinemaGoButton(
                        label = if (isApplying) theme.t("Program uygulanıyor…", "Applying program…") else strings.applyProtocol,
                        icon = Icons.Rounded.Check,
                        onClick = { if (!isApplying) onApply() },
                        dim = isApplying
                    )
                }
            }

            if (isApplying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Sinema.page(theme).copy(alpha = 0.70f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(42.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            theme.t("PROGRAM UYGULANIYOR", "APPLYING PROGRAM"),
                            color = theme.text0,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            theme.t("Tamamlanana kadar bekleyin", "Please wait until it finishes"),
                            color = theme.text1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** Öneri hapları: virgülle ayrılmış jetonlarda TAM eşleşme ("Güç" hapı "Güçlü" metnine dokunmaz). */
internal fun aiSuggestionTokens(prompt: String): List<String> =
    prompt.split(',').map { it.trim() }.filter { it.isNotEmpty() }

internal fun aiSuggestionSelected(prompt: String, label: String): Boolean = label in aiSuggestionTokens(prompt)

internal fun toggleAiSuggestion(prompt: String, label: String): String {
    val tokens = aiSuggestionTokens(prompt)
    return if (label in tokens) tokens.filterNot { it == label }.joinToString(", ")
    else (tokens + label).joinToString(", ")
}

/**
 * Gerçek buzlu alt çubuk (SinemaNavBar yöntemi): haze blur + düşük tint, üstte ~56dp yumuşak maske.
 * API<31 / haze yoksa tint artar ama maske aynı (yumuşak). İçerik `contentPadding` ile yerleşir; maske bölgesi üstte boş durur.
 */
@Composable
private fun FrostedBottomBar(
    haze: HazeState,
    base: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable ColumnScope.() -> Unit
) {
    val blurOk = android.os.Build.VERSION.SDK_INT >= 31
    val fade = 56.dp
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fr = (fade.toPx() / size.height).coerceIn(0.01f, 1f)
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(0f), fr * 0.30f to Color.Black.copy(0.10f),
                            fr * 0.62f to Color.Black.copy(0.50f), fr * 0.86f to Color.Black.copy(0.88f),
                            fr to Color.Black, 1f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .then(
                    if (blurOk) Modifier.hazeEffect(state = haze) {
                        blurRadius = 32.dp
                        backgroundColor = base
                        tints = listOf(HazeTint(base.copy(alpha = 0.30f)))
                        noiseFactor = 0f
                    } else Modifier.background(base.copy(alpha = 0.72f))
                )
        )
        Column(
            Modifier.fillMaxWidth().padding(top = fade).padding(contentPadding),
            content = content
        )
    }
}

@Composable
private fun DialogStat(
    icon: ImageVector, value: String, label: String, accent: Color,
    modifier: Modifier = Modifier, valueSize: androidx.compose.ui.unit.TextUnit = 34.sp
) {
    val theme = LocalAppTheme.current
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
            Text(value, color = theme.text0, fontWeight = FontWeight.Black, fontSize = valueSize, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Text(label, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    val responsive = rememberResponsiveLayoutInfo()

    // Seçilen dosya bilgisi
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedBase64   by remember { mutableStateOf<String?>(null) }
    var selectedMimeType by remember { mutableStateOf<String?>(null) }
    var selectedFileError by remember { mutableStateOf<String?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mime = (context.contentResolver.getType(uri) ?: "application/octet-stream").lowercase()
        if (mime !in AI_ALLOWED_UPLOAD_MIME) {
            selectedBase64 = null
            selectedMimeType = null
            selectedFileName = null
            selectedFileError = aiTheme.t(
                "Sadece PDF, JPG, PNG veya WebP dosyası yükleyebilirsin.",
                "You can only upload PDF, JPG, PNG or WebP files."
            )
            return@rememberLauncherForActivityResult
        }

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            selectedFileError = aiTheme.t(
                "Dosya okunamadı. Başka bir dosya seçmeyi dene.",
                "The file could not be read. Try another file."
            )
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > AI_MAX_UPLOAD_BYTES) {
            selectedBase64 = null
            selectedMimeType = null
            selectedFileName = null
            selectedFileError = aiTheme.t(
                "Dosya en fazla ${aiUploadLimitLabel()} olabilir.",
                "File size can be at most ${aiUploadLimitLabel()}."
            )
            return@rememberLauncherForActivityResult
        }

        selectedBase64   = Base64.encodeToString(bytes, Base64.NO_WRAP)
        selectedMimeType = mime
        selectedFileName = uri.lastPathSegment ?: "dosya"
        selectedFileError = null
    }

    val canAnalyze = (prompt.isNotBlank() || selectedBase64 != null) && !uiState.aiLoading
    val scrollState = rememberScrollState()

    val navBarHeight = responsive.bottomNavHeight
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    // AI Koç sahnesi: her zaman koyu (foto/sahne sayfası), cam kapsüller
    val glassText = Color.White
    val glassSub = Color.White.copy(0.62f)
    val accentAi = aiTheme.effectiveAccentColor
    val inputStyle = androidx.compose.ui.text.TextStyle(
        color = glassText, fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp
    )

    val haze = remember { HazeState() }
    val promptFocus = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize().background(Sinema.Page)) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) { SinemaScene(Modifier.fillMaxSize()) }

        // ── Scrollable content ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(haze)
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(top = 14.dp, bottom = contentPad + 150.dp + timerExtraPad)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SinemaCapsuleIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = aiTheme.t("Geri", "Back"),
                    onClick = onBack,
                    selected = false,
                    size = 48.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        aiStrings.aiProtocolSub.sinemaUpper(aiTheme),
                        color = Color.White.copy(0.72f), style = Sinema.Label, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(aiTheme.t("AI Koç", "AI Coach"), color = Color.White, style = Sinema.ScreenTitle, maxLines = 1)
                }
            }
            Spacer(Modifier.height(28.dp))

            // Enerji bilgisi
            AiCreditInfoRow(
                isFree  = uiState.userPlan == UserPlan.FREE,
                credits = uiState.aiCredits,
                costLabel = aiTheme.t(
                    "6 Enerji / metin · 10 Enerji / dosya",
                    "6 Energy / text · 10 Energy / file"
                ),
                theme   = aiTheme,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Büyük cam giriş kartı (çizgisiz)
            SinemaCapsule(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                onClick = { if (!uiState.aiLoading) promptFocus.requestFocus() },
                height = 220.dp,
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(aiTheme.t("TASARIM PARAMETRELERİ", "DESIGN PARAMETERS"), color = glassSub, style = Sinema.Label)
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        textStyle = inputStyle,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(accentAi),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 220.dp).focusRequester(promptFocus).verticalScroll(rememberScrollState()),
                        enabled = !uiState.aiLoading,
                        decorationBox = { inner ->
                            if (prompt.isEmpty()) Text(
                                if (selectedBase64 != null) {
                                    aiTheme.t("Ek talimat ekleyebilirsin (opsiyonel)...", "You can add extra instructions (optional)...")
                                } else {
                                    aiTheme.t("Antrenman sıklığı, hedefin ve seviyeni belirt...", "Describe your training frequency, goal, and level...")
                                },
                                color = Color.White.copy(0.45f), style = inputStyle
                            )
                            inner()
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Hızlı seçim hapları: metne ekleme yapan öneriler
            val suggestionGroups = listOf(
                aiTheme.t("SIKLIK", "FREQUENCY") to listOf(
                    aiTheme.t("Haftada 3 gün", "3 days a week"),
                    aiTheme.t("Haftada 4 gün", "4 days a week"),
                    aiTheme.t("Haftada 5 gün", "5 days a week")
                ),
                aiTheme.t("HEDEF", "GOAL") to listOf(
                    aiTheme.t("Kas kazanımı", "Muscle gain"),
                    aiTheme.t("Yağ yakımı", "Fat loss"),
                    aiTheme.t("Güç", "Strength")
                ),
                aiTheme.t("SEVİYE", "LEVEL") to listOf(
                    aiTheme.t("Başlangıç", "Beginner"),
                    aiTheme.t("Orta seviye", "Intermediate"),
                    aiTheme.t("İleri seviye", "Advanced")
                )
            )
            suggestionGroups.forEach { (groupLabel, options) ->
                Text(groupLabel, color = glassSub, style = Sinema.Label, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        SinemaCapsule(
                            selected = aiSuggestionSelected(prompt, option),
                            onClick = { prompt = toggleAiSuggestion(prompt, option) },
                            enabled = !uiState.aiLoading,
                            height = 40.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) { fg ->
                            Text(option, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(4.dp))

            // Dosya yükleme (cam kapsül) + kaldır
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SinemaCapsule(
                    modifier = Modifier.weight(1f),
                    selected = selectedBase64 != null,
                    onClick = { fileLauncher.launch("*/*") },
                    enabled = !uiState.aiLoading,
                    height = 56.dp,
                    horizontalArrangement = Arrangement.Center
                ) { fg ->
                    Icon(
                        imageVector = if (selectedBase64 != null) Icons.Rounded.CheckCircle else Icons.Rounded.UploadFile,
                        contentDescription = null, tint = fg, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedBase64 != null) aiTheme.t("Değiştir", "Change")
                        else aiTheme.t("Görsel / PDF Yükle", "Upload Image / PDF"),
                        color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
                if (selectedBase64 != null) {
                    SinemaCapsuleIconButton(
                        icon = Icons.Rounded.Close,
                        description = aiTheme.t("Kaldır", "Remove"),
                        onClick = {
                            selectedBase64   = null
                            selectedMimeType = null
                            selectedFileName = null
                            selectedFileError = null
                        },
                        selected = false,
                        size = 48.dp
                    )
                }
            }

            selectedFileName?.let { name ->
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = glassSub, modifier = Modifier.size(14.dp))
                    Text(name, color = glassSub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            selectedFileError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = CardCoral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 28.dp))
            }

            // Hata: kutusuz, uyarı ikonlu metin
            uiState.aiError?.let { errMsg ->
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = CardCoral, modifier = Modifier.size(16.dp))
                    Text(errMsg, color = CardCoral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Fixed bottom button ──────────────────────────────────────────────
        FrostedBottomBar(
            haze = haze,
            base = Color.Black,
            modifier = Modifier.align(Alignment.BottomCenter).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = contentPad + timerExtraPad, top = 8.dp)
        ) {
            if (uiState.userPlan == UserPlan.FREE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    SinemaPill {
                        Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            aiTheme.t("6-10 Enerji · Kalan ${uiState.aiCredits}", "6-10 Energy · ${uiState.aiCredits} left"),
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                        )
                    }
                }
            }
            SinemaGoOrIdle(
                label = if (uiState.aiLoading) aiTheme.t("Analiz ediliyor…", "Analyzing…")
                else if (selectedBase64 != null) aiTheme.t("DOSYADAN PROGRAM OLUŞTUR", "CREATE PROGRAM FROM FILE")
                else aiTheme.t("PROGRAMI ANALİZ ET", "ANALYZE PROGRAM"),
                icon = Icons.Rounded.AutoAwesome,
                enabled = canAnalyze,
                loading = uiState.aiLoading,
                onClick = {
                    viewModel.clearAiError()
                    viewModel.createFromAI(prompt, selectedBase64, selectedMimeType)
                }
            )
        }
    }
}

/** Aktif: SinemaGoButton (dolu vurgu + glow). Pasif: nötr gri (beyaz %10 zemin, %40 yazı). Yükleniyor: soluk Go. */
@Composable
private fun SinemaGoOrIdle(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (enabled || loading) {
        SinemaGoButton(label = label, icon = icon, onClick = { if (!loading) onClick() }, modifier = modifier, dim = loading)
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(0.10f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White.copy(0.40f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White.copy(0.40f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

// ── Manual Builder — helper state classes ─────────────────────────────────────

private class MutableManualDay(
    val id: String = UUID.randomUUID().toString()
) {
    var title    by mutableStateOf("")
    var isRestDay by mutableStateOf(false)
    var notes by mutableStateOf("")
    val exercises = mutableStateListOf<DraftExercise>()
}

private data class DraftExercise(
    val exerciseId  : String,
    val name        : String,
    val targetMuscle: String,
    val sets        : Int,
    val reps        : Int,
    val restSeconds : Int,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Float? = null,
    val weightKg: Float = 0f,
    val section: String = "",
    val notes: String = "",
    val groupId: String? = null,
    val groupType: String = "straight",
    val groupLabel: String = "",
    val groupRounds: Int? = null,
    val groupRestSeconds: Int? = null
)

// ── Edit Program Screen ───────────────────────────────────────────────────────

@Composable
private fun EditProgramScreen(
    program      : com.cosmibit.profitness.domain.model.Program,
    viewModel    : ProgramViewModel,
    onBack       : () -> Unit,
    timerExtraPad: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val exercises  = uiState.exercises
    val editTheme  = LocalAppTheme.current
    val editStrings = editTheme.strings
    val editAccent = editTheme.effectiveAccentColor
    val editOnAccent = editTheme.effectiveOnAccentColor

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
                m.notes = day.notes
                day.exercises.sortedBy { it.orderIndex }.forEach { ex ->
                    m.exercises.add(
                        DraftExercise(
                            exerciseId   = ex.exerciseId,
                            name         = ex.exerciseName,
                            targetMuscle = ex.targetMuscle,
                            sets         = ex.sets,
                            reps         = ex.reps,
                            restSeconds  = ex.restSeconds,
                            targetDurationSeconds = ex.targetDurationSeconds,
                            targetDistanceMeters = ex.targetDistanceMeters,
                            weightKg = ex.weightKg,
                            section = ex.section,
                            notes = ex.notes,
                            groupId = ex.groupId,
                            groupType = ex.groupType,
                            groupLabel = ex.groupLabel,
                            groupRounds = ex.groupRounds,
                            groupRestSeconds = ex.groupRestSeconds
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
                onConfirm = { newSets, newReps, newRest, newWeight, targetDuration, targetDistance, newNotes ->
                    days[dayIdx].exercises[exIdx] = ex.copy(
                        sets = newSets,
                        reps = newReps,
                        restSeconds = newRest,
                        weightKg = newWeight,
                        targetDurationSeconds = targetDuration,
                        targetDistanceMeters = targetDistance,
                        notes = newNotes
                    )
                    editingExercise = null
                }
            )
        }
    }

    showPickerForDay?.let { dayIndex ->
        ExercisePickerSheet(
            exercises = exercises,
            onDismiss = { showPickerForDay = null },
            onConfirm = { exerciseId, name, muscle, sets, reps, rest, targetDuration, targetDistance ->
                val day = days[dayIndex]
                day.exercises.add(DraftExercise(exerciseId, name, muscle, sets, reps, rest, targetDuration, targetDistance))
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
                m.notes = dayResult.notes
                dayResult.exercises.forEach { ex ->
                    m.exercises.add(
                        DraftExercise(
                            exerciseId = ex.exerciseId,
                            name = ex.name,
                            targetMuscle = ex.targetMuscle,
                            sets = ex.sets,
                            reps = ex.reps,
                            restSeconds = ex.restSeconds,
                            targetDurationSeconds = ex.targetDurationSeconds,
                            targetDistanceMeters = ex.targetDistanceMeters,
                            weightKg = ex.weightKg,
                            section = ex.section,
                            notes = ex.notes,
                            groupId = ex.groupId,
                            groupType = ex.groupType,
                            groupLabel = ex.groupLabel,
                            groupRounds = ex.groupRounds,
                            groupRestSeconds = ex.groupRestSeconds
                        )
                    )
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
            val modalShape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .sinemaCard(editTheme, modalShape)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .sinemaGlow(editAccent.copy(0.4f), 14.dp, 5.dp, CircleShape)
                            .clip(CircleShape)
                            .background(editAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = editOnAccent, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            editTheme.t("AI İLE DÜZENLE", "EDIT WITH AI"),
                            color = Sinema.textSecondary(editTheme), style = Sinema.Label
                        )
                        Text(
                            editTheme.t("Programı nasıl değiştireyim?", "How should I change the program?"),
                            color = Sinema.text(editTheme),
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    if (uiState.userPlan == UserPlan.FREE) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Sinema.line(editTheme))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                editTheme.t("${uiState.aiCredits} Enerji", "${uiState.aiCredits} Energy"),
                                color = Sinema.text(editTheme), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Sinema.page(editTheme))
                        .border(1.dp, Sinema.line(editTheme), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value         = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        enabled       = !uiState.aiEditLoading,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(
                            color    = Sinema.text(editTheme),
                            fontSize = 15.sp
                        ),
                        cursorBrush   = androidx.compose.ui.graphics.SolidColor(editAccent),
                        modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp).verticalScroll(rememberScrollState()),
                        decorationBox = { inner ->
                            if (aiPrompt.isEmpty()) {
                                Text(
                                    editTheme.t("Örn: Bacak günü ekle, karın egzersizlerini çıkar, tekrar sayısını düzenle...", "E.g. Add a leg day, remove ab exercises, adjust rep counts..."),
                                    color      = Sinema.textSecondary(editTheme),
                                    fontSize   = 14.sp,
                                    lineHeight = 21.sp
                                )
                            }
                            inner()
                        }
                    )
                }

                if (uiState.aiEditError != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = CardCoral, modifier = Modifier.size(16.dp))
                        Text(uiState.aiEditError!!, color = CardCoral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SinemaCapsule(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = if (uiState.aiEditLoading) 0.5f else 1f },
                        onClick = {
                            showAiEditDialog = false
                            viewModel.clearAiEditError()
                        },
                        enabled = !uiState.aiEditLoading,
                        height = 56.dp,
                        shape = RoundedCornerShape(28.dp),
                        light = !editTheme.isDark,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { fg ->
                        Text(editTheme.t("İptal", "Cancel"), color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    }
                    SinemaGoOrIdle(
                        label = if (uiState.aiEditLoading) editTheme.t("Uygulanıyor…", "Applying…")
                        else if (uiState.userPlan == UserPlan.FREE) editTheme.t("Uygula · $AI_EDIT_ENERGY_COST Enerji", "Apply · $AI_EDIT_ENERGY_COST Energy")
                        else editTheme.t("Uygula", "Apply"),
                        icon = Icons.Rounded.AutoAwesome,
                        enabled = aiPrompt.isNotBlank() && !uiState.aiEditLoading,
                        loading = uiState.aiEditLoading,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val currentDrafts = days.mapIndexed { i, d ->
                                ManualDayDraft(
                                    title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                                    isRestDay = d.isRestDay,
                                    notes     = d.notes,
                                    selectedExercises = d.exercises.mapIndexed { ei, ex ->
                                        com.cosmibit.profitness.data.program.ManualExerciseInput(
                                            exerciseId  = ex.exerciseId,
                                            sets        = ex.sets,
                                            reps        = ex.reps,
                                            restSeconds = ex.restSeconds,
                                            weightKg    = ex.weightKg,
                                            orderIndex  = ei,
                                            targetDurationSeconds = ex.targetDurationSeconds,
                                            targetDistanceMeters = ex.targetDistanceMeters,
                                            section = ex.section,
                                            notes = ex.notes,
                                            groupId = ex.groupId,
                                            groupType = ex.groupType,
                                            groupLabel = ex.groupLabel,
                                            groupRounds = ex.groupRounds,
                                            groupRestSeconds = ex.groupRestSeconds
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
                        }
                    )
                }
            }
        }
    }

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(editTheme))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = contentPad + 88.dp + timerExtraPad)) {
            BuilderSceneHeader(
                title = editTheme.t("Düzenle", "Edit"),
                sub = editTheme.t("Programı güncelle", "Update program"),
                name = programName,
                onNameChange = { programName = it },
                namePlaceholder = editTheme.t("Program adı...", "Program name..."),
                onBack = onBack
            )

            // Günler listesi
            LazyColumn(
                modifier      = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(days, key = { _, day -> day.id }) { index, day ->
                    ManualDayCard(
                        day              = day,
                        dayNumber        = index + 1,
                        onAddExercise    = { showPickerForDay = index },
                        onRemoveExercise = { exIdx -> day.exercises.removeAt(exIdx) },
                        onRemoveDay      = { if (days.size > 1) days.removeAt(index) },
                        canRemove        = days.size > 1,
                        onEditExercise   = { exIdx -> editingExercise = Pair(index, exIdx) }
                    )
                }
                item {
                    if (days.size < 7) {
                        NewDayBlock(count = days.size, onClick = { days.add(MutableManualDay()) })
                    }
                }
            }
        }

        // Alt aksiyon butonları
        val isLoading = uiState.isLoading
        val actionsEnabled = !isLoading && !uiState.aiEditLoading
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Sinema.page(editTheme)),
                        startY = 0f, endY = 60f
                    )
                )
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 16.dp)
        ) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment   = Alignment.CenterVertically
            ) {
                // AI ile Düzenle butonu
                SinemaCapsule(
                    modifier = Modifier
                        .width(148.dp)
                        .graphicsLayer { alpha = if (actionsEnabled) 1f else 0.5f },
                    onClick = { showAiEditDialog = true },
                    enabled = actionsEnabled,
                    height = 56.dp,
                    shape = RoundedCornerShape(28.dp),
                    light = !editTheme.isDark,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) { fg ->
                    Icon(Icons.Rounded.AutoAwesome, null, tint = fg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        editTheme.t("AI ile düzenle", "Edit with AI"),
                        color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1
                    )
                }

                // Kaydet butonu
                SinemaGoButton(
                    label = editTheme.t("Kaydet", "Save"),
                    icon = Icons.Rounded.Check,
                    onClick = {
                        if (!actionsEnabled) return@SinemaGoButton
                        val dayDrafts = days.mapIndexed { i, d ->
                            ManualDayDraft(
                                title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                                isRestDay = d.isRestDay,
                                notes     = d.notes,
                                selectedExercises = d.exercises.mapIndexed { ei, ex ->
                                    com.cosmibit.profitness.data.program.ManualExerciseInput(
                                        exerciseId  = ex.exerciseId,
                                        sets        = ex.sets,
                                        reps        = ex.reps,
                                        restSeconds = ex.restSeconds,
                                        weightKg    = ex.weightKg,
                                        orderIndex  = ei,
                                        targetDurationSeconds = ex.targetDurationSeconds,
                                        targetDistanceMeters = ex.targetDistanceMeters,
                                        section = ex.section,
                                        notes = ex.notes,
                                        groupId = ex.groupId,
                                        groupType = ex.groupType,
                                        groupLabel = ex.groupLabel,
                                        groupRounds = ex.groupRounds,
                                        groupRestSeconds = ex.groupRestSeconds
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
                    modifier = Modifier.weight(1f),
                    dim = !actionsEnabled
                )
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
    val manAccent   = manTheme.effectiveAccentColor
    val manOnAccent = manTheme.effectiveOnAccentColor

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
                onConfirm = { newSets, newReps, newRest, newWeight, targetDuration, targetDistance, newNotes ->
                    days[dayIdx].exercises[exIdx] = ex.copy(
                        sets = newSets,
                        reps = newReps,
                        restSeconds = newRest,
                        weightKg = newWeight,
                        targetDurationSeconds = targetDuration,
                        targetDistanceMeters = targetDistance,
                        notes = newNotes
                    )
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
            onConfirm = { exerciseId, name, muscle, sets, reps, rest, targetDuration, targetDistance ->
                val day = days[dayIndex]
                day.exercises.add(DraftExercise(exerciseId, name, muscle, sets, reps, rest, targetDuration, targetDistance))
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
    val isLoading = uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(manTheme))
            .padding(bottom = contentPad + timerExtraPad)
    ) {
        BuilderSceneHeader(
            title = manTheme.t("Manuel", "Manual"),
            sub = manStrings.manualBuilderSub,
            name = programName,
            onNameChange = { programName = it },
            namePlaceholder = manTheme.t("Program adı...", "Program name..."),
            onBack = onBack
        )

        // ── Days list ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(days, key = { _, day -> day.id }) { index, day ->
                ManualDayCard(
                    day              = day,
                    dayNumber        = index + 1,
                    onAddExercise    = { showPickerForDay = index },
                    onRemoveExercise = { exIdx -> day.exercises.removeAt(exIdx) },
                    onRemoveDay      = { if (days.size > 1) days.removeAt(index) },
                    canRemove        = days.size > 1,
                    onEditExercise   = { exIdx -> editingExercise = Pair(index, exIdx) }
                )
            }

            item {
                if (days.size < 7) {
                    NewDayBlock(count = days.size, onClick = { days.add(MutableManualDay()) })
                }
            }
        }

        // ── Save button ───────────────────────────────────────────────────────
        SinemaGoButton(
            label = manStrings.saveProtocol,
            icon = Icons.Rounded.Check,
            onClick = {
                if (isLoading) return@SinemaGoButton
                val dayDrafts = days.mapIndexed { i, d ->
                    ManualDayDraft(
                        title     = d.title.ifBlank { autoTitle(d.exercises.map { it.targetMuscle }) },
                        isRestDay = d.isRestDay,
                        notes     = d.notes,
                        selectedExercises = d.exercises.mapIndexed { ei, ex ->
                            ManualExerciseInput(
                                exerciseId  = ex.exerciseId,
                                sets        = ex.sets,
                                reps        = ex.reps,
                                restSeconds = ex.restSeconds,
                                weightKg    = ex.weightKg,
                                orderIndex  = ei,
                                targetDurationSeconds = ex.targetDurationSeconds,
                                targetDistanceMeters = ex.targetDistanceMeters,
                                section = ex.section,
                                notes = ex.notes,
                                groupId = ex.groupId,
                                groupType = ex.groupType,
                                groupLabel = ex.groupLabel,
                                groupRounds = ex.groupRounds,
                                groupRestSeconds = ex.groupRestSeconds
                            )
                        }
                    )
                }
                viewModel.createManualProgram(
                    name = programName,
                    days = dayDrafts
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            dim = isLoading
        )
    }
}

// ── Manual Day Card ───────────────────────────────────────────────────────────

@Composable
private fun BuilderSceneHeader(
    title: String,
    sub: String,
    name: String,
    onNameChange: (String) -> Unit,
    namePlaceholder: String,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val nameFocus = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        SinemaScene(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SinemaCapsuleIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = theme.t("Geri", "Back"),
                    onClick = onBack,
                    selected = false,
                    size = 48.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sub.sinemaUpper(theme),
                        color = Color.White.copy(0.72f),
                        style = Sinema.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        title,
                        color = Color.White,
                        style = Sinema.ScreenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SinemaCapsule(
                modifier = Modifier.fillMaxWidth(),
                onClick = { nameFocus.requestFocus() },
                height = 68.dp,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(nameFocus),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text(
                                namePlaceholder,
                                color = Color.White.copy(0.45f),
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                )
            }
        }
    }
}

/** "Yeni gün" kabarık blok: tek artı ikonu, sayaç. */
@Composable
private fun NewDayBlock(count: Int, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .sinemaGlow(accent.copy(0.4f), 16.dp, 6.dp, CircleShape)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            theme.t("Yeni gün", "New day"),
            color = Sinema.text(theme),
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count/7",
            color = Sinema.textSecondary(theme),
            style = Sinema.Label
        )
    }
}

@Composable
private fun ManualDayCard(
    day             : MutableManualDay,
    dayNumber       : Int,
    onAddExercise   : () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onRemoveDay     : () -> Unit,
    canRemove       : Boolean = true,
    onEditExercise  : (Int) -> Unit = {}
) {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    val textPrimary = Sinema.text(theme)
    val textSec = Sinema.textSecondary(theme)
    val line = Sinema.line(theme)
    val rest = day.isRestDay
    var confirmRemove by remember { mutableStateOf(false) }
    if (confirmRemove) {
        SinemaDialog(
            onDismiss = { confirmRemove = false },
            title = theme.t("Günü sil?", "Delete day?"),
            body = theme.t(
                "\"${day.title.ifBlank { "Gün $dayNumber" }}\" günü ve içindeki ${day.exercises.size} hareket silinecek.",
                "\"${day.title.ifBlank { "Day $dayNumber" }}\" and its ${day.exercises.size} exercises will be deleted."
            ),
            confirmLabel = theme.t("Sil", "Delete"),
            confirmIcon = Icons.Rounded.Delete,
            onConfirm = { confirmRemove = false; onRemoveDay() },
            dismissLabel = theme.t("İptal", "Cancel")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        // Day header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(if (!rest) Modifier.sinemaGlow(accent.copy(0.4f), 16.dp, 6.dp, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(if (rest) line else accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayNumber.toString(),
                    color = if (rest) textSec else onAccent,
                    style = Sinema.Metric.copy(fontSize = 20.sp)
                )
            }
            Spacer(Modifier.width(12.dp))

            // Editable title
            BasicTextField(
                value = day.title,
                onValueChange = { day.title = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textPrimary,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (day.title.isEmpty()) {
                        Text(
                            if (rest) theme.t("DİNLENME GÜNÜ", "REST DAY")
                            else theme.t("GÜN $dayNumber", "DAY $dayNumber"),
                            color = textSec,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    }
                    inner()
                }
            )
            Spacer(Modifier.width(8.dp))

            // Rest day toggle
            SinemaCapsule(
                selected = rest,
                onClick = { day.isRestDay = !day.isRestDay },
                height = 36.dp,
                light = !theme.isDark,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
            ) { fg ->
                Text(theme.t("Dinlenme", "Rest"), color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            if (canRemove) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                            if (day.exercises.isEmpty()) onRemoveDay() else confirmRemove = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, theme.t("Günü sil", "Remove day"), tint = textSec, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Sinema.page(theme))
                .border(1.dp, line, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = day.notes,
                onValueChange = { day.notes = it.take(600) },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = textPrimary, fontSize = 13.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (day.notes.isBlank()) {
                        Text(
                            theme.t("Gün notu (isteğe bağlı)", "Day note (optional)"),
                            color = textSec,
                            fontSize = 13.sp
                        )
                    }
                    inner()
                }
            )
        }

        // Exercises section (hidden when rest day)
        if (!rest) {
            if (day.exercises.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    day.exercises.forEachIndexed { i, ex ->
                        val previous = day.exercises.getOrNull(i - 1)
                        if (ex.section.isNotBlank() && !ex.section.equals(previous?.section, ignoreCase = true)) {
                            Text(
                                ex.section.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                                color = accent,
                                style = Sinema.Label,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }
                        if (!ex.groupId.isNullOrBlank() && ex.groupId != previous?.groupId) {
                            val groupName = ex.groupLabel.ifBlank {
                                when (ex.groupType) {
                                    "giant_set" -> theme.t("Dev Set", "Giant Set")
                                    "circuit" -> theme.t("Devre", "Circuit")
                                    else -> theme.t("Süperset", "Superset")
                                }
                            }
                            val groupDetails = buildList {
                                ex.groupRounds?.let { add("$it ${theme.t("tur", "rounds")}") }
                                ex.groupRestSeconds?.let { add("$it sn ${theme.t("dinlenme", "rest")}") }
                            }.joinToString(" · ")
                            Text(
                                listOf(groupName, groupDetails).filter { it.isNotBlank() }.joinToString(" · "),
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Sinema.page(theme))
                                .border(1.dp, line, RoundedCornerShape(20.dp))
                                .clickable { onEditExercise(i) }
                                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    theme.exerciseDisplayName(ex.name),
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    manualDraftBadges(ex, theme).forEach { badge ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(line)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                badge,
                                                color = textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (ex.notes.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        ex.notes,
                                        color = textSec,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onEditExercise(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Edit, theme.t("Düzenle", "Edit"), tint = textPrimary, modifier = Modifier.size(18.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onRemoveExercise(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Close, theme.t("Kaldır", "Remove"), tint = textSec, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            SinemaCapsule(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                onClick = onAddExercise,
                height = 48.dp,
                light = !theme.isDark,
                horizontalArrangement = Arrangement.Center
            ) { fg ->
                Icon(Icons.Rounded.Add, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    theme.t("Hareket ekle", "Add exercise"),
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ── Exercise Edit Dialog ──────────────────────────────────────────────────────

private fun manualDraftBadges(exercise: DraftExercise, theme: AppThemeState): List<String> {
    val spec = draftTrackingSpec(exercise)
    return when (spec.metric) {
        ExerciseMetric.Strength -> buildList {
            add("${exercise.sets} SET")
            add("${exercise.reps} ${theme.t("TEK", "REP")}")
            if (exercise.weightKg > 0f) {
                val weight = if (exercise.weightKg % 1f == 0f) exercise.weightKg.toInt().toString() else exercise.weightKg.toString()
                add("$weight KG")
            }
        }
        ExerciseMetric.Duration -> listOf(formatDraftDuration(defaultDraftDurationSeconds(exercise), theme))
        ExerciseMetric.DurationDistance -> listOf(
            formatDraftDuration(defaultDraftDurationSeconds(exercise), theme),
            formatDraftDistance(defaultDraftDistanceMeters(exercise).toFloat())
        )
    }
}

private fun draftTrackingSpec(exercise: DraftExercise) =
    activityTrackingSpec(
        category = exercise.targetMuscle,
        name = exercise.name,
        target = exercise.targetMuscle,
        reps = if (exercise.targetDurationSeconds != null) "${exercise.targetDurationSeconds / 60} dk" else exercise.reps.toString(),
        trackingModeRaw = when {
            exercise.targetDistanceMeters != null -> "duration_distance"
            exercise.targetDurationSeconds != null -> "duration"
            else -> null
        }
    )

private fun defaultDraftDurationSeconds(exercise: DraftExercise): Int =
    exercise.targetDurationSeconds
        ?: defaultDurationSecondsForExercise(
            category = exercise.targetMuscle,
            name = exercise.name,
            target = exercise.targetMuscle,
            reps = exercise.reps
        )

private fun defaultDraftDistanceMeters(exercise: DraftExercise): Int =
    exercise.targetDistanceMeters?.toInt()
        ?: when {
            listOf("farmer", "carry", "sled", "shuttle").any { it in exercise.name.lowercase() } -> 30
            draftTrackingSpec(exercise).sportType.raw == "swimming" -> 500
            draftTrackingSpec(exercise).sportType.raw == "walking_hiking" -> 3000
            draftTrackingSpec(exercise).sportType.raw == "cycling" -> 10000
            else -> 1000
        }

private fun formatDraftDuration(seconds: Int, theme: AppThemeState): String =
    "${(seconds / 60).coerceAtLeast(1)} ${theme.t("DK", "MIN")}"

private fun formatDraftDistance(meters: Float): String =
    if (meters >= 1000f) {
        val km = meters / 1000f
        if (meters % 1000f == 0f) "${km.toInt()} KM" else "%.1f KM".format(km)
    } else {
        "${meters.toInt()} M"
    }

@Composable
private fun ExerciseEditDialog(
    exercise : DraftExercise,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: Int, restSeconds: Int, weightKg: Float, targetDurationSeconds: Int?, targetDistanceMeters: Float?, notes: String) -> Unit
) {
    var sets by remember { mutableIntStateOf(exercise.sets) }
    var reps by remember { mutableIntStateOf(exercise.reps) }
    var targetDurationSec by remember(exercise) { mutableIntStateOf(defaultDraftDurationSeconds(exercise)) }
    var targetDistanceM by remember(exercise) { mutableIntStateOf(defaultDraftDistanceMeters(exercise)) }
    var weightTenths by remember(exercise) { mutableIntStateOf((exercise.weightKg * 10).toInt()) }
    var notes by remember(exercise) { mutableStateOf(exercise.notes) }
    val spec = remember(exercise) { draftTrackingSpec(exercise) }
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    val line = Sinema.line(theme)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sinemaCard(theme, RoundedCornerShape(28.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = onAccent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        theme.t("HAREKETİ DÜZENLE", "EDIT EXERCISE"),
                        color = Sinema.textSecondary(theme),
                        style = Sinema.Label
                    )
                    Text(
                        theme.exerciseDisplayName(exercise.name),
                        color      = Sinema.text(theme),
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp
                    )
                }
            }

            // Counter fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Sinema.page(theme))
                    .border(1.dp, line, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp)
            ) {
                if (spec.metric == ExerciseMetric.Strength) {
                    EditCounterField(
                        label       = "SET",
                        value       = sets,
                        onDecrement = { if (sets > 1) sets-- },
                        onIncrement = { if (sets < 10) sets++ },
                        accent      = accent
                    )
                    HorizontalDivider(color = line, thickness = 1.dp)
                    EditCounterField(
                        label       = theme.t("TEKRAR", "REPS"),
                        value       = reps,
                        onDecrement = { if (reps > 1) reps-- },
                        onIncrement = { if (reps < 100) reps++ },
                        accent      = accent
                    )
                    HorizontalDivider(color = line, thickness = 1.dp)
                    EditCounterField(
                        label       = theme.t("BAŞLANGIÇ KİLOSU", "STARTING WEIGHT"),
                        value       = weightTenths,
                        onDecrement = { weightTenths = (weightTenths - 5).coerceAtLeast(0) },
                        onIncrement = { weightTenths = (weightTenths + 5).coerceAtMost(5000) },
                        accent      = accent,
                        displayOverride = if (weightTenths == 0) "—" else "%.1f KG".format(weightTenths / 10f)
                    )
                } else {
                    EditCounterField(
                        label       = theme.t("SÜRE", "DURATION"),
                        value       = targetDurationSec,
                        onDecrement = { targetDurationSec = (targetDurationSec - 300).coerceAtLeast(300) },
                        onIncrement = { targetDurationSec = (targetDurationSec + 300).coerceAtMost(10800) },
                        accent      = accent,
                        displayOverride = formatDraftDuration(targetDurationSec, theme)
                    )
                    if (spec.metric == ExerciseMetric.DurationDistance) {
                        HorizontalDivider(color = line, thickness = 1.dp)
                        EditCounterField(
                            label       = theme.t("MESAFE", "DISTANCE"),
                            value       = targetDistanceM,
                            onDecrement = { targetDistanceM = (targetDistanceM - 500).coerceAtLeast(500) },
                            onIncrement = { targetDistanceM = (targetDistanceM + 500).coerceAtMost(50000) },
                            accent      = accent,
                            displayOverride = formatDraftDistance(targetDistanceM.toFloat())
                        )
                    }
                }
            }

            BasicTextField(
                value = notes,
                onValueChange = { notes = it.take(600) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Sinema.text(theme)),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Sinema.page(theme))
                    .border(1.dp, line, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                decorationBox = { inner ->
                    if (notes.isBlank()) Text(
                        theme.t("Hareket notu / uygulama talimatı", "Exercise note / instruction"),
                        color = Sinema.textSecondary(theme),
                        fontSize = 13.sp
                    )
                    inner()
                }
            )

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SinemaCapsule(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                    height = 56.dp,
                    shape = RoundedCornerShape(28.dp),
                    light = !theme.isDark,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) { fg ->
                    Text(theme.t("İptal", "Cancel"), color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                }
                SinemaGoButton(
                    label = theme.t("Kaydet", "Save"),
                    icon = Icons.Rounded.Check,
                    onClick = {
                        onConfirm(
                            sets,
                            reps,
                            exercise.restSeconds,
                            weightTenths / 10f,
                            targetDurationSec.takeIf { spec.metric != ExerciseMetric.Strength },
                            targetDistanceM.toFloat().takeIf { spec.metric == ExerciseMetric.DurationDistance },
                            notes.trim()
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
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
    val onAccent = theme.effectiveOnAccentColor
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color         = Sinema.textSecondary(theme),
            style         = Sinema.Label,
            modifier      = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Sinema.line(theme))
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Remove, null, tint = Sinema.text(theme), modifier = Modifier.size(18.dp))
        }
        Text(
            displayOverride ?: value.toString(),
            color      = Sinema.text(theme),
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 22.sp,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            modifier   = Modifier.widthIn(min = 64.dp)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, CircleShape)
                .clip(CircleShape)
                .background(accent)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = onAccent, modifier = Modifier.size(18.dp))
        }
    }
}
