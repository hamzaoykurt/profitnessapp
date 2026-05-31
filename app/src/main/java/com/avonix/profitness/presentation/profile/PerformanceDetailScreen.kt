package com.avonix.profitness.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.AppBackButton
import com.avonix.profitness.presentation.components.glassCard
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDetailScreen(
    onBack   : () -> Unit,
    onNavigateToWeightTracking      : () -> Unit = {},
    onNavigateToExerciseProgression : () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val strings = theme.strings
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    var showCalculators by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppBackButton(onClick = onBack, accent = accent, size = 48.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            theme.t("VERİ ANALİZİ", "DATA ANALYSIS"),
                            color         = theme.text0,
                            fontSize      = 18.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(theme.t("Trendler, metrikler ve hesaplamalar", "Trends, metrics and calculations"), color = theme.text2, fontSize = 11.sp)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CalculatorLauncherButton(
                        accent = accent,
                        theme = theme,
                        onClick = { showCalculators = true }
                    )
                }
            }

            item {
                PerformanceRecordsSection(
                    weightKg = state.weightKg,
                    accent = accent,
                    theme = theme,
                    topPadding = 14.dp,
                    onNavigateToWeightTracking = onNavigateToWeightTracking,
                    onNavigateToExerciseProgression = onNavigateToExerciseProgression
                )
            }

            item {
                WorkoutBarChart(
                    counts  = state.weeklyWorkoutCounts,
                    accent  = accent,
                    theme   = theme,
                    modifier= Modifier.padding(20.dp, 18.dp, 20.dp, 0.dp)
                )
            }

            item {
                WeeklyActivityDataCard(
                    accent = accent,
                    theme = theme,
                    strings = strings,
                    weeklyActivity = state.weeklyActivity,
                    modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 0.dp)
                )
            }

            item {
                MetricSection(
                    title = theme.t("ANTRENMAN VERİLERİ", "WORKOUT DATA"),
                    metrics = trainingMetrics(state, accent, theme),
                    theme = theme,
                    accent = accent,
                    topPadding = 28.dp
                )
            }

            item {
                MetricSection(
                    title = theme.t("VÜCUT VERİLERİ", "BODY DATA"),
                    metrics = bodyMetrics(state, accent, theme),
                    theme = theme,
                    accent = accent,
                    topPadding = 32.dp
                )
            }

            item {
                MetricSection(
                    title = theme.t("SERİ VE SEVİYE", "STREAK & LEVEL"),
                    metrics = consistencyMetrics(state, accent, theme),
                    theme = theme,
                    accent = accent,
                    topPadding = 32.dp
                )
            }

        }
    }

    if (showCalculators) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.94f
        ModalBottomSheet(
            onDismissRequest = { showCalculators = false },
            sheetState = sheetState,
            containerColor = theme.bg1,
            contentColor = theme.text0,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(theme.text2.copy(0.35f))
                )
            }
        ) {
            CalculationsSheet(
                state = state,
                accent = accent,
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun PerformanceShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    theme: AppThemeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(146.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                spotColor = accent.copy(if (theme.isDark) 0.26f else 0.16f),
                ambientColor = Color.Black.copy(if (theme.isDark) 0.42f else 0.10f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(if (theme.isDark) 0.18f else 0.11f),
                        theme.bg1.copy(if (theme.isDark) 0.96f else 0.98f),
                        theme.bg0.copy(if (theme.isDark) 0.95f else 0.96f)
                    )
                )
            )
            .border(1.dp, accent.copy(if (theme.isDark) 0.42f else 0.34f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(0.13f))
                        .border(1.dp, accent.copy(0.18f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Icon(
                    Icons.Rounded.ArrowForwardIos,
                    null,
                    tint = accent.copy(0.70f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = theme.text0,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 17.sp
                )
                Text(
                    subtitle,
                    color = theme.text2,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent, accent.copy(0.16f))
                            )
                        )
                )
            }
        }
    }
}

private enum class CalculatorMode { OneRm, Bmi, BodyFat, Bmr, HeartRate, Protein, Vo2Max }
private enum class OneRmFormula { Epley, Brzycki, Lombardi }
private enum class CalcGender { Male, Female }
private enum class ProteinGoal { General, MuscleGain, FatLoss }

private data class ActivityFactor(
    val label: String,
    val factor: Double
)

private val activityFactors = listOf(
    ActivityFactor("Sedanter", 1.2),
    ActivityFactor("Hafif", 1.375),
    ActivityFactor("Orta", 1.55),
    ActivityFactor("Yoğun", 1.725)
)

@Composable
private fun CalculatorLauncherButton(
    accent: Color,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(min = 136.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(0.13f))
            .border(1.dp, accent.copy(0.28f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Calculate, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(theme.t("Hesapla", "Calculate"), color = theme.text0, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalculationsSheet(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CalculatorMode.OneRm) }

    Column(
        modifier = modifier
            .background(theme.bg1)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 54.dp, end = 20.dp, bottom = 0.dp)
    ) {
        Text(
            theme.t("HESAPLAMALAR", "CALCULATIONS"),
            color = theme.text0,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(theme.t("1RM, BMI, yağ oranı, kalori, nabız, protein ve VO2 max", "1RM, BMI, body fat, calories, heart rate, protein and VO2 max"), color = theme.text2, fontSize = 11.sp)

        Spacer(Modifier.height(16.dp))

        CalculatorModeGrid(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(16.dp))

        when (selectedMode) {
            CalculatorMode.OneRm -> OneRepMaxCalculator(accent = accent, theme = theme)
            CalculatorMode.Bmi -> BmiCalculator(state = state, accent = accent, theme = theme)
            CalculatorMode.BodyFat -> BodyFatCalculator(state = state, accent = accent, theme = theme)
            CalculatorMode.Bmr -> BmrCalculator(state = state, accent = accent, theme = theme)
            CalculatorMode.HeartRate -> HeartRateCalculator(state = state, accent = accent, theme = theme)
            CalculatorMode.Protein -> ProteinCalculator(state = state, accent = accent, theme = theme)
            CalculatorMode.Vo2Max -> Vo2MaxCalculator(accent = accent, theme = theme)
        }

    }
}

@Composable
private fun CalculatorModeGrid(
    selectedMode: CalculatorMode,
    onModeSelected: (CalculatorMode) -> Unit,
    accent: Color,
    theme: AppThemeState
) {
    val modes = listOf(
        CalculatorMode.OneRm,
        CalculatorMode.Bmi,
        CalculatorMode.BodyFat,
        CalculatorMode.Bmr,
        CalculatorMode.HeartRate,
        CalculatorMode.Protein,
        CalculatorMode.Vo2Max
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { mode ->
                    CalculatorModeButton(
                        mode = mode,
                        selected = mode == selectedMode,
                        accent = accent,
                        theme = theme,
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorModeButton(
    mode: CalculatorMode,
    selected: Boolean,
    accent: Color,
    theme: AppThemeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeAccent = calculatorModeColor(mode, accent)
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) modeAccent.copy(0.15f) else theme.bg0.copy(0.58f))
            .border(
                1.dp,
                if (selected) modeAccent.copy(0.55f) else theme.stroke,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(calculatorModeIcon(mode), null, tint = modeAccent, modifier = Modifier.size(18.dp))
        Text(
            calculatorModeTitle(mode, theme),
            color = if (selected) theme.text0 else theme.text1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OneRepMaxCalculator(
    accent: Color,
    theme: AppThemeState
) {
    var liftedWeight by remember { mutableStateOf("100") }
    var reps by remember { mutableStateOf("5") }
    var formula by remember { mutableStateOf(OneRmFormula.Epley) }
    val weight = liftedWeight.toPositiveDoubleOrNull()
    val repetitionCount = reps.trim().toIntOrNull()?.takeIf { it in 1..30 }
    val oneRm = if (weight != null && repetitionCount != null) {
        estimateOneRepMax(weight, repetitionCount, formula)
    } else null

    CalculatorPanel(
        title = theme.t("Tek Tekrar Maksimumu", "One-Rep Max"),
        subtitle = theme.t("Calculator.net 1RM formülleri", "Calculator.net 1RM formulas"),
        icon = Icons.Rounded.FitnessCenter,
        accent = CardCoral,
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Ağırlık kg", "Weight kg"),
                value = liftedWeight,
                onValueChange = { liftedWeight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Tekrar", "Reps"),
                value = reps,
                onValueChange = { reps = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Spacer(Modifier.height(12.dp))

        FormulaSelector(
            selectedFormula = formula,
            onFormulaSelected = { formula = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (oneRm != null) {
            CalculationResultCard(
                title = theme.t("Tahmini 1RM", "Estimated 1RM"),
                value = "${formatOneDecimal(oneRm)} kg",
                subtitle = theme.t("${oneRmFormulaLabel(formula)} formülü", "${oneRmFormulaLabel(formula)} formula"),
                accent = CardCoral,
                theme = theme
            )

            Spacer(Modifier.height(12.dp))

            listOf(95, 90, 85, 80, 75, 70).forEach { pct ->
                CalculationRow(
                    label = "%$pct",
                    value = "${formatOneDecimal(oneRm * pct / 100.0)} kg",
                    theme = theme
                )
            }
        } else {
            EmptyCalculationHint(theme.t("Ağırlık ve 1-30 arası tekrar gir.", "Enter weight and 1-30 reps."))
        }
    }
}

@Composable
private fun BmiCalculator(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState
) {
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    val weightKg = weight.toPositiveDoubleOrNull()
    val heightCm = height.toPositiveDoubleOrNull()
    val bmi = calculateBmi(weightKg, heightCm)
    val category = bmi?.let { bmiCategoryLabel(it, theme) }
    val resultColor = bmi?.let(::bmiCategoryColor) ?: accent

    CalculatorPanel(
        title = theme.t("Vücut Kitle İndeksi", "Body Mass Index"),
        subtitle = theme.t("CDC metrik BMI hesabı", "CDC metric BMI calculation"),
        icon = Icons.Rounded.Analytics,
        accent = CardGreen,
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Boy cm", "Height cm"),
                value = height,
                onValueChange = { height = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (bmi != null) {
            CalculationResultCard(
                title = "BMI",
                value = formatOneDecimal(bmi),
                subtitle = category.orEmpty(),
                accent = resultColor,
                theme = theme
            )
        } else {
            EmptyCalculationHint(theme.t("Kilo ve boy değerlerini gir.", "Enter weight and height."))
        }
    }
}

@Composable
private fun BodyFatCalculator(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState
) {
    var gender by remember(state.gender) { mutableStateOf(profileGender(state.gender)) }
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    var neck by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }

    val ageYears = age.trim().toIntOrNull()?.takeIf { it in 8..100 }
    val weightKg = weight.toPositiveDoubleOrNull()
    val heightCm = height.toPositiveDoubleOrNull()
    val navy = navyBodyFatPct(
        gender = gender,
        heightCm = heightCm,
        neckCm = neck.toPositiveDoubleOrNull(),
        waistCm = waist.toPositiveDoubleOrNull(),
        hipCm = hip.toPositiveDoubleOrNull()
    )
    val bmiBased = bmiBodyFatPct(
        gender = gender,
        age = ageYears,
        weightKg = weightKg,
        heightCm = heightCm
    )

    CalculatorPanel(
        title = theme.t("Vücut Yağ Oranı", "Body Fat Percentage"),
        subtitle = theme.t("U.S. Navy ve BMI yöntemi", "U.S. Navy and BMI method"),
        icon = Icons.Rounded.Person,
        accent = CardPurple,
        theme = theme
    ) {
        GenderSelector(
            selectedGender = gender,
            onGenderSelected = { gender = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Boy cm", "Height cm"),
                value = height,
                onValueChange = { height = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
            CalculationInput(
                label = theme.t("Boyun cm", "Neck cm"),
                value = neck,
                onValueChange = { neck = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Bel cm", "Waist cm"),
                value = waist,
                onValueChange = { waist = it },
                modifier = Modifier.weight(1f)
            )
            if (gender == CalcGender.Female) {
                CalculationInput(
                    label = theme.t("Kalça cm", "Hip cm"),
                    value = hip,
                    onValueChange = { hip = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (navy != null) {
            CalculationResultCard(
                title = "U.S. Navy",
                value = "%${formatOneDecimal(navy)}",
                subtitle = bodyFatCategory(navy, gender, theme),
                accent = CardPurple,
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
        }

        if (bmiBased != null) {
            CalculationResultCard(
                title = theme.t("BMI yöntemi", "BMI method"),
                value = "%${formatOneDecimal(bmiBased)}",
                subtitle = theme.t("Hızlı tahmin", "Quick estimate"),
                accent = CardGreen,
                theme = theme
            )
        }

        if (navy == null && bmiBased == null) {
            EmptyCalculationHint(theme.t("Profil verileri yoksa yaş, kilo, boy ve ölçüleri gir.", "If profile data is missing, enter age, weight, height and measurements."))
        }
    }
}

@Composable
private fun BmrCalculator(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState
) {
    var gender by remember(state.gender) { mutableStateOf(profileGender(state.gender)) }
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var height by remember(state.heightCm) { mutableStateOf(profileNumberInput(state.heightCm, decimals = 0)) }
    var activity by remember { mutableStateOf(activityFactors[1]) }

    val bmr = calculateBmr(
        gender = gender,
        age = age.trim().toIntOrNull()?.takeIf { it in 8..100 },
        weightKg = weight.toPositiveDoubleOrNull(),
        heightCm = height.toPositiveDoubleOrNull()
    )
    val tdee = bmr?.times(activity.factor)

    CalculatorPanel(
        title = theme.t("Kalori İhtiyacı", "Calorie Needs"),
        subtitle = theme.t("Mifflin-St Jeor BMR + aktivite", "Mifflin-St Jeor BMR + activity"),
        icon = Icons.Rounded.LocalFireDepartment,
        accent = CardCyan,
        theme = theme
    ) {
        GenderSelector(
            selectedGender = gender,
            onGenderSelected = { gender = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Kilo kg", "Weight kg"),
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        CalculationInput(
            label = theme.t("Boy cm", "Height cm"),
            value = height,
            onValueChange = { height = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ActivitySelector(
            selectedActivity = activity,
            onActivitySelected = { activity = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (bmr != null && tdee != null) {
            CalculationResultCard(
                title = "BMR",
                value = "${formatWhole(bmr)} kcal",
                subtitle = theme.t("Dinlenme metabolizması", "Resting metabolism"),
                accent = CardCyan,
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
            CalculationResultCard(
                title = theme.t("Günlük ihtiyaç", "Daily needs"),
                value = "${formatWhole(tdee)} kcal",
                subtitle = theme.t("${activity.label} aktivite", "${activity.localizedLabel(theme)} activity"),
                accent = accent,
                theme = theme
            )
        } else {
            EmptyCalculationHint(theme.t("Yaş, kilo ve boy değerlerini gir.", "Enter age, weight and height."))
        }
    }
}

@Composable
private fun HeartRateCalculator(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState
) {
    var age by remember(state.birthDate) { mutableStateOf(profileAgeInput(state)) }
    var restingHr by remember { mutableStateOf("60") }

    val ageYears = age.trim().toIntOrNull()?.takeIf { it in 8..100 }
    val resting = restingHr.trim().toIntOrNull()?.takeIf { it in 30..120 }
    val maxHr = ageYears?.let(::calculateMaxHeartRate)
    val zones = if (maxHr != null && resting != null && resting < maxHr) {
        heartRateZones(maxHr, resting)
    } else emptyList()

    CalculatorPanel(
        title = theme.t("Nabız Bölgeleri", "Heart Rate Zones"),
        subtitle = theme.t("Tanaka max nabız + Karvonen HRR", "Tanaka max heart rate + Karvonen HRR"),
        icon = Icons.Rounded.Favorite,
        accent = Color(0xFF64D2FF),
        theme = theme
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalculationInput(
                label = theme.t("Yaş", "Age"),
                value = age,
                onValueChange = { age = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
            CalculationInput(
                label = theme.t("Dinlenik nabız", "Resting heart rate"),
                value = restingHr,
                onValueChange = { restingHr = it },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Spacer(Modifier.height(14.dp))

        if (maxHr != null && zones.isNotEmpty()) {
            CalculationResultCard(
                title = theme.t("Tahmini max nabız", "Estimated max heart rate"),
                value = "${maxHr.roundToInt()} bpm",
                subtitle = theme.t("208 - 0.7 x yaş", "208 - 0.7 x age"),
                accent = Color(0xFF64D2FF),
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            zones.forEach { zone ->
                CalculationRow(
                    label = zone.localizedLabel(theme),
                    value = "${zone.low}-${zone.high} bpm",
                    theme = theme
                )
            }
        } else {
            EmptyCalculationHint(theme.t("Yaş ve 30-120 arası dinlenik nabız gir.", "Enter age and resting heart rate between 30-120."))
        }
    }
}

@Composable
private fun ProteinCalculator(
    state: ProfileState,
    accent: Color,
    theme: AppThemeState
) {
    var weight by remember(state.weightKg) { mutableStateOf(profileNumberInput(state.weightKg)) }
    var goal by remember { mutableStateOf(ProteinGoal.MuscleGain) }
    val weightKg = weight.toPositiveDoubleOrNull()
    val range = weightKg?.let { proteinRange(it, goal) }

    CalculatorPanel(
        title = theme.t("Protein Hedefi", "Protein Target"),
        subtitle = theme.t("Sporcu g/kg aralıkları", "Athlete g/kg ranges"),
        icon = Icons.Rounded.Restaurant,
        accent = CardGreen,
        theme = theme
    ) {
        CalculationInput(
            label = theme.t("Kilo kg", "Weight kg"),
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ProteinGoalSelector(
            selectedGoal = goal,
            onGoalSelected = { goal = it },
            accent = accent,
            theme = theme
        )

        Spacer(Modifier.height(14.dp))

        if (range != null) {
            CalculationResultCard(
                title = theme.t("Günlük protein", "Daily protein"),
                value = "${formatWhole(range.first)}-${formatWhole(range.second)} g",
                subtitle = proteinGoalLabel(goal, theme),
                accent = CardGreen,
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            CalculationRow(theme.t("Alt sınır", "Lower limit"), "${formatOneDecimal(proteinGramPerKg(goal).first)} g/kg", theme)
            CalculationRow(theme.t("Üst sınır", "Upper limit"), "${formatOneDecimal(proteinGramPerKg(goal).second)} g/kg", theme)
        } else {
            EmptyCalculationHint(theme.t("Kilo değerini gir.", "Enter weight."))
        }
    }
}

@Composable
private fun Vo2MaxCalculator(
    accent: Color,
    theme: AppThemeState
) {
    var distance by remember { mutableStateOf("2400") }
    val distanceMeters = distance.toPositiveDoubleOrNull()
    val vo2Max = distanceMeters?.let(::cooperVo2Max)

    CalculatorPanel(
        title = "VO2 Max",
        subtitle = theme.t("Cooper 12 dakika koşu tahmini", "Cooper 12-minute run estimate"),
        icon = Icons.Rounded.DirectionsRun,
        accent = CardPurple,
        theme = theme
    ) {
        CalculationInput(
            label = theme.t("12 dk mesafe m", "12 min distance m"),
            value = distance,
            onValueChange = { distance = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Number
        )

        Spacer(Modifier.height(14.dp))

        if (vo2Max != null) {
            CalculationResultCard(
                title = theme.t("Tahmini VO2 max", "Estimated VO2 max"),
                value = formatOneDecimal(vo2Max),
                subtitle = theme.t("ml/kg/dk", "ml/kg/min"),
                accent = CardPurple,
                theme = theme
            )
            Spacer(Modifier.height(12.dp))
            CalculationRow(theme.t("Formül", "Formula"), theme.t("(mesafe - 504.9) / 44.73", "(distance - 504.9) / 44.73"), theme)
        } else {
            EmptyCalculationHint(theme.t("12 dakikada koşulan mesafeyi metre olarak gir.", "Enter the distance run in 12 minutes in meters."))
        }
    }
}

@Composable
private fun FormulaSelector(
    selectedFormula: OneRmFormula,
    onFormulaSelected: (OneRmFormula) -> Unit,
    accent: Color,
    theme: AppThemeState
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(OneRmFormula.Epley, OneRmFormula.Brzycki, OneRmFormula.Lombardi).forEach { formula ->
            val selected = selectedFormula == formula
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(0.14f) else theme.bg0.copy(0.55f))
                    .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke, RoundedCornerShape(12.dp))
                    .clickable { onFormulaSelected(formula) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    oneRmFormulaLabel(formula),
                    color = if (selected) theme.text0 else theme.text2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GenderSelector(
    selectedGender: CalcGender,
    onGenderSelected: (CalcGender) -> Unit,
    accent: Color,
    theme: AppThemeState
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(CalcGender.Male, CalcGender.Female).forEach { gender ->
            val selected = selectedGender == gender
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(0.14f) else theme.bg0.copy(0.55f))
                    .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke, RoundedCornerShape(12.dp))
                    .clickable { onGenderSelected(gender) },
                contentAlignment = Alignment.Center
            ) {
                Text(gender.localizedLabel(theme), color = if (selected) theme.text0 else theme.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActivitySelector(
    selectedActivity: ActivityFactor,
    onActivitySelected: (ActivityFactor) -> Unit,
    accent: Color,
    theme: AppThemeState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        activityFactors.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { activity ->
                    val selected = selectedActivity == activity
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent.copy(0.14f) else theme.bg0.copy(0.55f))
                            .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke, RoundedCornerShape(12.dp))
                            .clickable { onActivitySelected(activity) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activity.localizedLabel(theme), color = if (selected) theme.text0 else theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProteinGoalSelector(
    selectedGoal: ProteinGoal,
    onGoalSelected: (ProteinGoal) -> Unit,
    accent: Color,
    theme: AppThemeState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ProteinGoal.General, ProteinGoal.MuscleGain, ProteinGoal.FatLoss).forEach { goal ->
            val selected = selectedGoal == goal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(0.14f) else theme.bg0.copy(0.55f))
                    .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke, RoundedCornerShape(12.dp))
                    .clickable { onGoalSelected(goal) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    proteinGoalLabel(goal, theme),
                    color = if (selected) theme.text0 else theme.text2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CalculatorPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    theme: AppThemeState,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(0.10f), theme.bg0.copy(0.82f))))
            .border(1.dp, accent.copy(0.30f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column {
                Text(title, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = theme.text2, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CalculationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    val theme = LocalAppTheme.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        delay(250)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        label = { Text(label) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.text0,
            unfocusedTextColor = theme.text0,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = theme.text2,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = theme.stroke,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = theme.bg0.copy(0.35f),
            unfocusedContainerColor = theme.bg0.copy(0.35f)
        )
    )
}

@Composable
private fun CalculationResultCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(0.12f))
            .border(1.dp, accent.copy(0.28f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(value, color = theme.text0, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = theme.text2, fontSize = 11.sp)
        Text(value, color = theme.text0, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCalculationHint(message: String) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg0.copy(0.45f))
            .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(message, color = theme.text2, fontSize = 11.sp)
    }
}

private fun calculatorModeTitle(mode: CalculatorMode, theme: AppThemeState): String = when (mode) {
    CalculatorMode.OneRm -> "1RM"
    CalculatorMode.Bmi -> "BMI"
    CalculatorMode.BodyFat -> theme.t("Yağ Oranı", "Body Fat")
    CalculatorMode.Bmr -> theme.t("Kalori", "Calories")
    CalculatorMode.HeartRate -> theme.t("Nabız", "Heart Rate")
    CalculatorMode.Protein -> "Protein"
    CalculatorMode.Vo2Max -> "VO2 Max"
}

private fun calculatorModeIcon(mode: CalculatorMode): ImageVector = when (mode) {
    CalculatorMode.OneRm -> Icons.Rounded.FitnessCenter
    CalculatorMode.Bmi -> Icons.Rounded.Analytics
    CalculatorMode.BodyFat -> Icons.Rounded.Person
    CalculatorMode.Bmr -> Icons.Rounded.LocalFireDepartment
    CalculatorMode.HeartRate -> Icons.Rounded.Favorite
    CalculatorMode.Protein -> Icons.Rounded.Restaurant
    CalculatorMode.Vo2Max -> Icons.Rounded.DirectionsRun
}

private fun calculatorModeColor(mode: CalculatorMode, accent: Color): Color = when (mode) {
    CalculatorMode.OneRm -> CardCoral
    CalculatorMode.Bmi -> CardGreen
    CalculatorMode.BodyFat -> CardPurple
    CalculatorMode.Bmr -> accent
    CalculatorMode.HeartRate -> Color(0xFF64D2FF)
    CalculatorMode.Protein -> CardGreen
    CalculatorMode.Vo2Max -> CardPurple
}

private fun CalcGender.localizedLabel(theme: AppThemeState): String =
    if (this == CalcGender.Male) theme.t("Erkek", "Male") else theme.t("Kadın", "Female")

private fun ActivityFactor.localizedLabel(theme: AppThemeState): String = when (label) {
    "Sedanter" -> theme.t("Sedanter", "Sedentary")
    "Hafif" -> theme.t("Hafif", "Light")
    "Orta" -> theme.t("Orta", "Moderate")
    "Yoğun" -> theme.t("Yoğun", "Intense")
    else -> label
}

private fun oneRmFormulaLabel(formula: OneRmFormula): String = when (formula) {
    OneRmFormula.Epley -> "Epley"
    OneRmFormula.Brzycki -> "Brzycki"
    OneRmFormula.Lombardi -> "Lombardi"
}

private fun estimateOneRepMax(weightKg: Double, reps: Int, formula: OneRmFormula): Double? {
    if (reps == 1) return weightKg
    return when (formula) {
        OneRmFormula.Epley -> weightKg * (1.0 + reps / 30.0)
        OneRmFormula.Brzycki -> if (reps < 37) weightKg * 36.0 / (37.0 - reps) else null
        OneRmFormula.Lombardi -> weightKg * reps.toDouble().pow(0.10)
    }
}

private fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null || heightCm <= 0.0) return null
    val heightM = heightCm / 100.0
    return (weightKg / (heightM * heightM)).takeIf { it.isFinite() && it > 0.0 }
}

private fun bmiCategoryLabel(bmi: Double, theme: AppThemeState): String = when {
    bmi < 18.5 -> theme.t("Zayıf", "Underweight")
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> theme.t("Fazla kilolu", "Overweight")
    else       -> theme.t("Obez", "Obese")
}

private fun bmiCategoryColor(bmi: Double): Color = when {
    bmi < 18.5 -> Color(0xFF64B5F6)
    bmi < 25.0 -> CardGreen
    bmi < 30.0 -> Color(0xFFFFB74D)
    else       -> CardCoral
}

private fun navyBodyFatPct(
    gender: CalcGender,
    heightCm: Double?,
    neckCm: Double?,
    waistCm: Double?,
    hipCm: Double?
): Double? {
    if (heightCm == null || neckCm == null || waistCm == null) return null
    return when (gender) {
        CalcGender.Male -> {
            val waistMinusNeck = waistCm - neckCm
            if (waistMinusNeck <= 0.0) null
            else 495.0 / (1.0324 - 0.19077 * log10(waistMinusNeck) + 0.15456 * log10(heightCm)) - 450.0
        }
        CalcGender.Female -> {
            val hipValue = hipCm ?: return null
            val waistPlusHipMinusNeck = waistCm + hipValue - neckCm
            if (waistPlusHipMinusNeck <= 0.0) null
            else 495.0 / (1.29579 - 0.35004 * log10(waistPlusHipMinusNeck) + 0.22100 * log10(heightCm)) - 450.0
        }
    }?.takeIf { it.isFinite() }?.coerceIn(2.0, 75.0)
}

private fun bmiBodyFatPct(
    gender: CalcGender,
    age: Int?,
    weightKg: Double?,
    heightCm: Double?
): Double? {
    val bmi = calculateBmi(weightKg, heightCm) ?: return null
    val ageYears = age ?: return null
    val raw = if (gender == CalcGender.Male) {
        1.20 * bmi + 0.23 * ageYears - 16.2
    } else {
        1.20 * bmi + 0.23 * ageYears - 5.4
    }
    return raw.takeIf { it.isFinite() }?.coerceIn(3.0, 60.0)
}

private fun bodyFatCategory(bodyFatPct: Double, gender: CalcGender, theme: AppThemeState): String =
    if (gender == CalcGender.Male) {
        when {
            bodyFatPct < 6.0  -> theme.t("Esansiyel", "Essential")
            bodyFatPct < 14.0 -> theme.t("Atletik", "Athletic")
            bodyFatPct < 18.0 -> "Fitness"
            bodyFatPct < 25.0 -> theme.t("Ortalama", "Average")
            else              -> theme.t("Yüksek", "High")
        }
    } else {
        when {
            bodyFatPct < 14.0 -> theme.t("Esansiyel", "Essential")
            bodyFatPct < 21.0 -> theme.t("Atletik", "Athletic")
            bodyFatPct < 25.0 -> "Fitness"
            bodyFatPct < 32.0 -> theme.t("Ortalama", "Average")
            else              -> theme.t("Yüksek", "High")
        }
    }

private fun calculateBmr(
    gender: CalcGender,
    age: Int?,
    weightKg: Double?,
    heightCm: Double?
): Double? {
    if (age == null || weightKg == null || heightCm == null) return null
    val offset = if (gender == CalcGender.Male) 5.0 else -161.0
    return (10.0 * weightKg + 6.25 * heightCm - 5.0 * age + offset).takeIf { it.isFinite() && it > 0.0 }
}

private data class HeartRateZone(
    val label: String,
    val low: Int,
    val high: Int
)

private fun calculateMaxHeartRate(age: Int): Double = 208.0 - 0.7 * age

private fun heartRateZones(maxHr: Double, restingHr: Int): List<HeartRateZone> {
    val reserve = maxHr - restingHr
    return listOf(
        heartRateZone("Z1 Toparlanma", reserve, restingHr, 0.50, 0.60),
        heartRateZone("Z2 Aerobik", reserve, restingHr, 0.60, 0.70),
        heartRateZone("Z3 Tempo", reserve, restingHr, 0.70, 0.80),
        heartRateZone("Z4 Eşik", reserve, restingHr, 0.80, 0.90),
        heartRateZone("Z5 Maksimum", reserve, restingHr, 0.90, 1.00)
    )
}

private fun heartRateZone(
    label: String,
    reserve: Double,
    restingHr: Int,
    lowPct: Double,
    highPct: Double
): HeartRateZone =
    HeartRateZone(
        label = label,
        low = (restingHr + reserve * lowPct).roundToInt(),
        high = (restingHr + reserve * highPct).roundToInt()
    )

private fun HeartRateZone.localizedLabel(theme: AppThemeState): String = when (label) {
    "Z1 Toparlanma" -> theme.t("Z1 Toparlanma", "Z1 Recovery")
    "Z2 Aerobik" -> theme.t("Z2 Aerobik", "Z2 Aerobic")
    "Z3 Tempo" -> "Z3 Tempo"
    "Z4 Eşik" -> theme.t("Z4 Eşik", "Z4 Threshold")
    "Z5 Maksimum" -> theme.t("Z5 Maksimum", "Z5 Maximum")
    else -> label
}

private fun proteinGoalLabel(goal: ProteinGoal, theme: AppThemeState): String = when (goal) {
    ProteinGoal.General -> theme.t("Genel fitness", "General fitness")
    ProteinGoal.MuscleGain -> theme.t("Kas kazanımı", "Muscle gain")
    ProteinGoal.FatLoss -> theme.t("Yağ kaybı / definasyon", "Fat loss / cutting")
}

private fun proteinGramPerKg(goal: ProteinGoal): Pair<Double, Double> = when (goal) {
    ProteinGoal.General -> 1.2 to 1.6
    ProteinGoal.MuscleGain -> 1.6 to 2.2
    ProteinGoal.FatLoss -> 1.8 to 2.7
}

private fun proteinRange(weightKg: Double, goal: ProteinGoal): Pair<Double, Double> {
    val grams = proteinGramPerKg(goal)
    return weightKg * grams.first to weightKg * grams.second
}

private fun cooperVo2Max(distanceMeters: Double): Double? =
    ((distanceMeters - 504.9) / 44.73).takeIf { distanceMeters >= 800.0 && it.isFinite() && it > 0.0 }

private fun profileGender(rawGender: String): CalcGender =
    if (rawGender.lowercase() in listOf("female", "kadın", "kadin", "woman")) CalcGender.Female else CalcGender.Male

private fun profileAgeInput(state: ProfileState): String =
    runCatching {
        val date = LocalDate.parse(state.birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
        Period.between(date, LocalDate.now()).years.takeIf { it in 8..100 }?.toString()
    }.getOrNull().orEmpty()

private fun profileNumberInput(value: Double, decimals: Int = 1): String =
    if (value > 0.0) {
        if (decimals == 0) "%.0f".format(value) else "%.1f".format(value)
    } else ""

private fun String.toPositiveDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

private fun formatOneDecimal(value: Double): String = "%.1f".format(value)

private fun formatWhole(value: Double): String = "%.0f".format(value)

// ── Haftalık Antrenman Bar Chart (Gerçek Veri) ────────────────────────────────

@Composable
private fun WorkoutBarChart(
    counts  : List<Int>,
    accent  : Color,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val maxVal   = counts.max().toFloat().coerceAtLeast(1f)
    val totalSum = counts.sum()
    val avgStr   = if (counts.isNotEmpty()) String.format("%.1f", totalSum.toFloat() / counts.size) else "0"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(theme.t("HAFTALIK ANTRENMANLAR", "WEEKLY WORKOUTS"), color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(theme.t("Son 13 hafta", "Last 13 weeks"), color = theme.text2, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        theme.t("Ort: $avgStr/hf", "Avg: $avgStr/wk"),
                        color      = accent,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val barW = size.width / (counts.size * 2 - 1)
                val maxH = size.height - 8.dp.toPx()

                counts.forEachIndexed { i, v ->
                    val barH = (v / maxVal) * maxH
                    val left = i * (barW + barW)
                    val top  = size.height - barH
                    drawRoundRect(
                        color        = accent.copy(if (i == counts.lastIndex) 1f else 0.45f),
                        topLeft      = Offset(left, top),
                        size         = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(theme.t("13 hf önce", "13 wk ago"), color = theme.text2, fontSize = 9.sp)
                Text(theme.t("Bu hafta", "This week"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Gerçek Metrik Grid ────────────────────────────────────────────────────────

private data class RealMetric(
    val value : String,
    val unit  : String,
    val label : String,
    val icon  : ImageVector,
    val color : Color
)

@Composable
private fun MetricSection(
    title     : String,
    metrics   : List<RealMetric>,
    theme     : AppThemeState,
    accent    : Color,
    topPadding: Dp
) {
    if (metrics.isEmpty()) return

    Column(modifier = Modifier.padding(20.dp, topPadding, 20.dp, 0.dp)) {
        Text(
            title,
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(14.dp))
        MetricsGrid(metrics = metrics, theme = theme)
    }
}

@Composable
private fun MetricsGrid(
    metrics: List<RealMetric>,
    theme  : AppThemeState
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { m ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 128.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(m.color.copy(0.10f), theme.bg1.copy(0.92f))
                                )
                            )
                            .border(1.dp, m.color.copy(0.28f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(m.color.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(m.icon, null, tint = m.color, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(m.value, color = theme.text0, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Text(m.unit, color = m.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                }
                                Text(m.label, color = theme.text2, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeeklyActivityDataCard(
    accent         : Color,
    theme          : AppThemeState,
    strings        : AppStrings,
    weeklyActivity : List<Float>,
    modifier       : Modifier = Modifier
) {
    val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(22.dp))
            .padding(20.dp, 20.dp, 20.dp, 14.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.weeklyActivity, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(strings.thisWeekSummary, color = theme.text2, fontSize = 10.sp)
                }
                Text("7 ${strings.unitDays}", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                strings.dayAbbreviations.forEachIndexed { i, day ->
                    val level   = weeklyActivity.getOrElse(i) { 0f }.coerceIn(0f, 1f)
                    val isToday = i == todayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .width(30.dp)
                                .height(76.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(theme.bg3)
                            )
                            if (level > 0f) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(level.coerceAtLeast(0.15f))
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isToday) accent else accent.copy(0.42f))
                                )
                            }
                        }
                        Text(
                            day,
                            color      = if (isToday) accent else theme.text2,
                            fontSize   = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceRecordsSection(
    weightKg: Double,
    accent: Color,
    theme: AppThemeState,
    topPadding: Dp = 32.dp,
    onNavigateToWeightTracking: () -> Unit,
    onNavigateToExerciseProgression: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp, topPadding, 20.dp, 0.dp)) {
        Text(
            theme.t("PERFORMANS KAYITLARI", "PERFORMANCE RECORDS"),
            style         = MaterialTheme.typography.labelSmall,
            color         = accent,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PerformanceShortcutCard(
                title = theme.t("Vücut Kilosu", "Body Weight"),
                subtitle = if (weightKg > 0) {
                    theme.t("${"%.1f".format(weightKg)} kg · Kilo trendi ve AI analiz", "${"%.1f".format(weightKg)} kg · Weight trend and AI analysis")
                } else {
                    theme.t("Kilo trendi ve AI analiz", "Weight trend and AI analysis")
                },
                icon = Icons.Rounded.ShowChart,
                accent = CardPurple,
                theme = theme,
                onClick = onNavigateToWeightTracking,
                modifier = Modifier.weight(1f)
            )
            PerformanceShortcutCard(
                title = theme.t("Hareket Performansı", "Exercise Performance"),
                subtitle = theme.t("Ağırlık, süre, mesafe ve hareket bazlı gelişim", "Weight, duration, distance and movement-based progress"),
                icon = Icons.Rounded.TrendingUp,
                accent = CardCyan,
                theme = theme,
                onClick = onNavigateToExerciseProgression,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun trainingMetrics(state: ProfileState, accent: Color, theme: AppThemeState): List<RealMetric> {
    val weeklyAverage = if (state.weeklyWorkoutCounts.isNotEmpty())
        String.format("%.1f", state.weeklyWorkoutCounts.sum().toFloat() / state.weeklyWorkoutCounts.size)
    else "0"

    return listOf(
        RealMetric(state.totalWorkouts.toString(), theme.t("antrenman", "workouts"), theme.t("TOPLAM ANTRENMAN", "TOTAL WORKOUTS"), Icons.Rounded.FitnessCenter, accent),
        RealMetric(state.totalExercises.toString(), theme.t("kez", "times"), theme.t("TOPLAM EGZERSİZ", "TOTAL EXERCISES"), Icons.Rounded.Timer, CardCyan),
        RealMetric(formatDetailDurationValue(state.totalDurationSeconds), theme.t("dk", "min"), theme.t("TOPLAM SÜRE", "TOTAL DURATION"), Icons.Rounded.Timer, CardGreen),
        RealMetric(formatDetailDistanceValue(state.totalDistanceMeters), formatDetailDistanceUnit(state.totalDistanceMeters), theme.t("TOPLAM MESAFE", "TOTAL DISTANCE"), Icons.Rounded.Straighten, Color(0xFF64D2FF)),
        RealMetric((state.weeklyWorkoutCounts.lastOrNull() ?: 0).toString(), theme.t("ant", "wkts"), theme.t("BU HAFTA", "THIS WEEK"), Icons.Rounded.CalendarMonth, CardPurple),
        RealMetric(weeklyAverage, theme.t("ant/hf", "wkts/wk"), theme.t("13 HAFTA ORT.", "13 WEEK AVG."), Icons.Rounded.BarChart, Color(0xFFFFD700))
    )
}

private fun bodyMetrics(state: ProfileState, accent: Color, theme: AppThemeState): List<RealMetric> {
    val bmiColor = when {
        state.bmi <= 0   -> accent
        state.bmi < 18.5 -> Color(0xFF64B5F6)
        state.bmi < 25.0 -> Color(0xFF4CAF50)
        state.bmi < 30.0 -> Color(0xFFFFB74D)
        else             -> Color(0xFFEF5350)
    }
    val bmiLabel = when {
        state.bmi <= 0   -> null
        state.bmi < 18.5 -> theme.t("ZAYIF", "UNDERWEIGHT")
        state.bmi < 25.0 -> "NORMAL"
        state.bmi < 30.0 -> theme.t("FAZLA KİLOLU", "OVERWEIGHT")
        else             -> theme.t("OBEZ", "OBESE")
    }

    return listOfNotNull(
        state.heightCm.takeIf { it > 0 }?.let {
            RealMetric(it.toInt().toString(), "cm", theme.t("BOY", "HEIGHT"), Icons.Rounded.Straighten, CardCyan)
        },
        state.weightKg.takeIf { it > 0 }?.let {
            RealMetric("%.1f".format(it), "kg", theme.t("KİLO", "WEIGHT"), Icons.Rounded.ShowChart, CardPurple)
        },
        state.bmi.takeIf { it > 0 }?.let {
            RealMetric("%.1f".format(it), "BMI", bmiLabel ?: theme.t("VÜCUT KİTLE İND.", "BODY MASS INDEX"), Icons.Rounded.Analytics, bmiColor)
        },
        state.bodyFatPct.takeIf { it > 0 }?.let {
            RealMetric("%.1f".format(it), "%", theme.t("VÜCUT YAĞ ORANI", "BODY FAT"), Icons.Rounded.Person, CardCoral)
        }
    )
}

private fun consistencyMetrics(state: ProfileState, accent: Color, theme: AppThemeState): List<RealMetric> =
    listOfNotNull(
        RealMetric(state.currentStreak.toString(), theme.t("gün", "days"), theme.t("AKTİF SERİ", "CURRENT STREAK"), Icons.Rounded.Whatshot, CardCoral),
        RealMetric(state.longestStreak.toString(), theme.t("gün", "days"), theme.t("EN UZUN SERİ", "LONGEST STREAK"), Icons.Rounded.EmojiEvents, CardGreen),
        RealMetric(state.level.toString(), theme.t("seviye", "level"), theme.t("MEVCUT SEVİYE", "CURRENT LEVEL"), Icons.Rounded.Star, Color(0xFFFFD700)),
        RealMetric(state.xp.toString(), "XP", theme.t("TOPLAM XP", "TOTAL XP"), Icons.Rounded.Bolt, CardPurple),
        state.streakRankPosition.takeIf { it > 0L }?.let {
            RealMetric(it.toString(), theme.t("sıra", "rank"), theme.t("SERİ SIRALAMASI", "STREAK RANKING"), Icons.Rounded.Leaderboard, accent)
        }
    )

private fun formatDetailDistanceValue(meters: Float): String =
    if (meters >= 1000f) "%.1f".format(meters / 1000f) else "%.0f".format(meters)

private fun formatDetailDistanceUnit(meters: Float): String =
    if (meters >= 1000f) "km" else "m"

private fun formatDetailDurationValue(seconds: Int): String =
    if (seconds <= 0) "0" else (seconds / 60).coerceAtLeast(1).toString()
