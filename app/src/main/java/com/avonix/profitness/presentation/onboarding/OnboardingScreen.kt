package com.avonix.profitness.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.core.ui.rememberResponsiveLayoutInfo
import com.avonix.profitness.presentation.components.AppBackButton

private val AVATAR_ROWS = listOf(
    listOf("🏋️‍♂️","🤸‍♂️","🏃‍♂️","🚴‍♂️","⛹️‍♂️","🏊‍♂️"),
    listOf("🏋️‍♀️","🤸‍♀️","🏃‍♀️","🚴‍♀️","🧘‍♀️","🏄‍♀️"),
    listOf("🦁","🐺","🦅","🐯","🦊","🐲"),
    listOf("💪","🔥","⚡","🏆","💎","🎯")
)

private val GENDER_OPTIONS = listOf(
    "Erkek" to "male",
    "Kadın" to "female",
    "Belirtme" to "other"
)

private val GOAL_SUGGESTIONS = listOf(
    "Kilo vermek",
    "Kas kazanmak",
    "Formda kalmak",
    "Yağ yakarken kas yapmak",
    "Kondisyon geliştirmek",
    "Sağlıklı yaşam"
)

private val SPORT_BRANCH_OPTIONS = listOf(
    "Fitness",
    "Koşu",
    "Bisiklet",
    "Yüzme",
    "Futbol",
    "Basketbol",
    "Tenis",
    "Boks",
    "Yoga",
    "Yürüyüş"
)

private val EXPERIENCE_OPTIONS = listOf(
    "Yeni başlıyorum",
    "Başlangıç",
    "Orta",
    "İleri"
)

private val WEEKLY_DAY_OPTIONS = listOf("3", "4", "5", "6")

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    onThemeChange        : (AppThemeState) -> Unit,
    viewModel            : OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val responsive = rememberResponsiveLayoutInfo()

    BackHandler(enabled = state.step > 0 && !state.isSaving) {
        viewModel.prevStep()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        // Arka plan degrade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = responsive.formMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            // Üst ilerleme çubuğu
            Spacer(Modifier.height(52.dp))
            OnboardingProgressBar(currentStep = state.step, totalSteps = 7, accent = accent, theme = theme)
            Spacer(Modifier.height(8.dp))

            // Adım içeriği (animasyonlu)
            AnimatedContent(
                targetState  = state.step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(
                        tween(320, easing = FastOutSlowInEasing)
                    ) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(280))
                    val exit  = slideOutHorizontally(
                        tween(280, easing = FastOutSlowInEasing)
                    ) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200))
                    enter togetherWith exit
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> StepName(state, viewModel, accent, theme)
                    1 -> StepTheme(current = theme, onThemeChange = onThemeChange, vm = viewModel)
                    2 -> StepSportExperience(state, viewModel, accent, theme)
                    3 -> StepGenderBirth(state, viewModel, accent, theme)
                    4 -> StepBodyMetrics(state, viewModel, accent, theme)
                    5 -> StepGoal(state, viewModel, accent, theme)
                    6 -> StepPersonalProgram(state, viewModel, accent, theme)
                }
            }
        }
    }
}

// ── İlerleme çubuğu ──────────────────────────────────────────────────────────

@Composable
private fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps : Int,
    accent     : Color,
    theme      : AppThemeState
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { i ->
            val active   = i == currentStep
            val finished = i < currentStep
            val fraction by animateFloatAsState(
                targetValue    = if (finished) 1f else if (active) 0.5f else 0f,
                animationSpec  = tween(400),
                label          = "progress_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(theme.stroke)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

// ── Adım 1: Tema ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepTheme(
    current      : AppThemeState,
    onThemeChange: (AppThemeState) -> Unit,
    vm           : OnboardingViewModel
) {
    var accent       by remember { mutableStateOf(current.accent) }
    var surfaceStyle by remember { mutableStateOf(current.surfaceStyle) }
    var intensity    by remember { mutableStateOf(current.intensity) }
    val preview = current.copy(
        accent       = accent,
        surfaceStyle = surfaceStyle,
        intensity    = intensity
    )
    val previewAccent   = preview.effectiveAccentColor
    val previewOnAccent = preview.effectiveOnAccentColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "2 / 7",
            title    = "Temanı seç",
            subtitle = "Uygulamanın sana nasıl görüneceğini ayarla",
            accent   = previewAccent,
            theme    = preview
        )

        Spacer(Modifier.height(24.dp))
        ThemePreviewCard(preview)
        Spacer(Modifier.height(22.dp))

        Text("VURGU RENGİ", color = current.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            AccentPreset.entries.forEach { preset ->
                ThemeColorSwatch(
                    preset     = preset,
                    isSelected = accent == preset,
                    onClick    = { accent = preset }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Text("ARKA PLAN TONU", color = current.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        ThemeSegmentedSelector(
            options = listOf(
                SurfaceStyle.CLASSIC to "KLASİK",
                SurfaceStyle.OLED to "OLED",
                SurfaceStyle.GRAPHITE to "GRAFİT"
            ),
            selected = surfaceStyle,
            accent = previewAccent,
            onAccent = previewOnAccent,
            theme = current,
            onSelect = { surfaceStyle = it }
        )

        Spacer(Modifier.height(18.dp))

        Text("YOĞUNLUK", color = current.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        ThemeSegmentedSelector(
            options = listOf(
                AccentIntensity.NEON to "NEON",
                AccentIntensity.PASTEL to "PASTEL"
            ),
            selected = intensity,
            accent = previewAccent,
            onAccent = previewOnAccent,
            theme = current,
            onSelect = { intensity = it }
        )

        Spacer(Modifier.height(28.dp))

        StepNavButtons(
            onBack    = { vm.prevStep() },
            onNext    = {
                onThemeChange(preview)
                vm.nextStep()
            },
            nextLabel = "Temayı Uygula",
            accent    = previewAccent,
            theme     = preview
        )

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 0: İsim + Avatar ─────────────────────────────────────────────────────

@Composable
private fun StepName(
    state    : OnboardingState,
    vm       : OnboardingViewModel,
    accent   : Color,
    theme    : AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text("Merhaba!", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Seni tanıyalım",
            color      = theme.text0,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Adını ve avatarını seç",
            color    = theme.text2,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(36.dp))

        // Seçili avatar önizleme
        Box(
            modifier         = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f))
                .border(2.dp, accent.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(state.avatar, fontSize = 44.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Avatar grid
        AVATAR_ROWS.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    val selected = emoji == state.avatar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent.copy(0.18f) else theme.bg1)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent else theme.stroke,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { vm.setAvatar(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 24.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        // İsim alanı
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ADIN SOYADIN", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            OutlinedTextField(
                value          = state.name,
                onValueChange  = { vm.setName(it) },
                placeholder    = { Text("Adını gir", color = theme.text2, fontSize = 14.sp) },
                leadingIcon    = { Icon(Icons.Rounded.Person, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp)) },
                isError        = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } },
                modifier       = Modifier.fillMaxWidth(),
                singleLine     = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done
                ),
                shape  = RoundedCornerShape(14.dp),
                colors = onboardingTextFieldColors(accent, theme)
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = { vm.nextStep() },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = theme.effectiveOnAccentColor)
        ) {
            Text("Devam Et", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 3: Spor profili ─────────────────────────────────────────────────────

@Composable
private fun StepSportExperience(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    accent : Color,
    theme  : AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "3 / 7",
            title    = "Spor profilin",
            subtitle = "Programın spor dalına ve seviyene göre şekillensin",
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(28.dp))

        Text("SPOR DALI", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        OnboardingChoiceGrid(
            options = SPORT_BRANCH_OPTIONS,
            selected = state.sportBranch,
            accent = accent,
            theme = theme,
            onSelect = { vm.setSportBranch(it) }
        )

        Spacer(Modifier.height(22.dp))

        Text("SEVİYE", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        OnboardingChoiceGrid(
            options = EXPERIENCE_OPTIONS,
            selected = state.experienceLevel,
            accent = accent,
            theme = theme,
            onSelect = { vm.setExperienceLevel(it) }
        )

        Spacer(Modifier.height(22.dp))

        Text("HAFTALIK ANTRENMAN", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        OnboardingChoiceGrid(
            options = WEEKLY_DAY_OPTIONS,
            selected = state.weeklyDays,
            accent = accent,
            theme = theme,
            labelFor = { "$it gün" },
            onSelect = { vm.setWeeklyDays(it) }
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(48.dp))

        StepNavButtons(
            onBack    = { vm.prevStep() },
            onNext    = { vm.nextStep() },
            nextLabel = "Devam Et",
            accent    = accent,
            theme     = theme,
            canSkip   = true,
            onSkip    = { vm.nextStep() }
        )

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun OnboardingChoiceGrid(
    options : List<String>,
    selected: String,
    accent  : Color,
    theme   : AppThemeState,
    labelFor: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    options.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accent.copy(0.18f) else theme.bg1)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else theme.stroke,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFor(option),
                        color = if (isSelected) accent else theme.text1,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ── Adım 4: Cinsiyet + Doğum Tarihi ──────────────────────────────────────────

@Composable
private fun StepGenderBirth(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    accent : Color,
    theme  : AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step    = "4 / 7",
            title   = "Biraz daha bilgi",
            subtitle = "Cinsiyet ve doğum tarihin",
            accent  = accent,
            theme   = theme
        )

        Spacer(Modifier.height(36.dp))

        // Cinsiyet
        Text("CİNSİYET", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.bg1)
                .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GENDER_OPTIONS.forEach { (label, dbValue) ->
                val selected = state.gender == dbValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent else Color.Transparent)
                        .clickable { vm.setGender(dbValue) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color      = if (selected) Color.Black else theme.text2,
                        fontSize   = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Doğum tarihi
        Text("DOĞUM TARİHİ", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value         = state.birthDigits,
            onValueChange = { vm.setBirthDigits(it) },
            placeholder   = { Text("15/06/1995", color = theme.text2, fontSize = 14.sp) },
            leadingIcon   = { Icon(Icons.Rounded.CalendarMonth, null, tint = accent, modifier = Modifier.size(20.dp)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            visualTransformation = DateVisualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape  = RoundedCornerShape(14.dp),
            colors = onboardingTextFieldColors(accent, theme)
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(48.dp))

        StepNavButtons(
            onBack   = { vm.prevStep() },
            onNext   = { vm.nextStep() },
            nextLabel = "Devam Et",
            accent   = accent,
            theme    = theme,
            canSkip  = true,
            onSkip   = { vm.nextStep() }
        )

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 2: Boy + Kilo ───────────────────────────────────────────────────────

@Composable
private fun StepBodyMetrics(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    accent : Color,
    theme  : AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "5 / 7",
            title    = "Vücut ölçülerin",
            subtitle = "Boy ve kilonu gir",
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(36.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BOY (cm)", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                OutlinedTextField(
                    value         = state.heightText,
                    onValueChange = { vm.setHeight(it) },
                    placeholder   = { Text("175", color = theme.text2, fontSize = 14.sp) },
                    leadingIcon   = { Text("📏", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape  = RoundedCornerShape(14.dp),
                    colors = onboardingTextFieldColors(accent, theme)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KİLO (kg)", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                OutlinedTextField(
                    value         = state.weightText,
                    onValueChange = { vm.setWeight(it) },
                    placeholder   = { Text("75", color = theme.text2, fontSize = 14.sp) },
                    leadingIcon   = { Text("⚖️", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape  = RoundedCornerShape(14.dp),
                    colors = onboardingTextFieldColors(accent, theme)
                )
            }
        }

        // BMI ve yağ oranı önizleme
        val h = state.heightText.toDoubleOrNull() ?: 0.0
        val w = state.weightText.toDoubleOrNull() ?: 0.0
        if (h > 0 && w > 0) {
            Spacer(Modifier.height(16.dp))
            val bmi      = vm.bmi
            val bodyFat  = vm.bodyFatPct
            val bmiLabel = when {
                bmi < 18.5 -> "Zayıf"
                bmi < 25.0 -> "Normal"
                bmi < 30.0 -> "Fazla Kilolu"
                else       -> "Obez"
            }
            val bmiColor = when {
                bmi < 18.5 -> Color(0xFF64B5F6)
                bmi < 25.0 -> Color(0xFF4CAF50)
                bmi < 30.0 -> Color(0xFFFFB74D)
                else       -> Color(0xFFEF5350)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bmiColor.copy(0.1f))
                    .border(1.dp, bmiColor.copy(0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.SpaceBetween,
                    modifier               = Modifier.fillMaxWidth()
                ) {
                    Text("BMI / VKİ", color = bmiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("%.1f — %s".format(bmi, bmiLabel), color = bmiColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (bodyFat > 0) {
                            Text(
                                "Tahmini yağ: %.1f%%".format(bodyFat),
                                color = bmiColor.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(48.dp))

        StepNavButtons(
            onBack    = { vm.prevStep() },
            onNext    = { vm.nextStep() },
            nextLabel = "Devam Et",
            accent    = accent,
            theme     = theme,
            canSkip   = true,
            onSkip    = { vm.nextStep() }
        )

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 3: Fitness Hedefi ────────────────────────────────────────────────────

@Composable
private fun StepGoal(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    accent : Color,
    theme  : AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "6 / 7",
            title    = "Hedefin ne?",
            subtitle = "Seni motive edecek hedefi seç",
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(28.dp))

        // Hızlı seçim çipleri
        val chunked = GOAL_SUGGESTIONS.chunked(2)
        chunked.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { suggestion ->
                    val selected = state.fitnessGoal == suggestion
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent.copy(0.18f) else theme.bg1)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent else theme.stroke,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { vm.setFitnessGoal(if (selected) "" else suggestion) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            suggestion,
                            color      = if (selected) accent else theme.text1,
                            fontSize   = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                // tek kalan chip varsa boşluk doldur
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Serbest metin
        Text("VEYA KENDİN YAZ", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value         = state.fitnessGoal,
            onValueChange = { vm.setFitnessGoal(it) },
            placeholder   = { Text("Hedefinizi yazın…", color = theme.text2, fontSize = 14.sp) },
            leadingIcon   = { Icon(Icons.Rounded.Flag, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp)) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Done
            ),
            shape  = RoundedCornerShape(14.dp),
            colors = onboardingTextFieldColors(accent, theme)
        )

        Spacer(Modifier.height(32.dp))

        // Program önerisine geç
        Button(
            onClick  = { vm.nextStep() },
            enabled  = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = theme.effectiveOnAccentColor)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(color = theme.effectiveOnAccentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Program Önerisine Geç", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Geri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = { vm.prevStep() }, accent = accent, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Geri",
                color = theme.text2,
                fontSize = 13.sp,
                modifier = Modifier.clickable { vm.prevStep() }
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 7: Kişiye özel program ──────────────────────────────────────────────

@Composable
private fun StepPersonalProgram(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    accent : Color,
    theme  : AppThemeState
) {
    val loading = state.isSaving || state.isGeneratingProgram
    val bmi = vm.bmi
    val bodyFat = vm.bodyFatPct
    val bmiText = if (bmi > 0) "%.1f".format(bmi) else "Eksik"
    val fatText = if (bodyFat > 0) "%.1f%%".format(bodyFat) else "Eksik"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "7 / 7",
            title    = "Programın hazır olsun mu?",
            subtitle = "Bilgilerine göre AI ile kişiye özel plan çıkaralım",
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(26.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(state.sportBranch, color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("${state.experienceLevel} • Haftada ${state.weeklyDays} gün", color = theme.text2, fontSize = 13.sp)
                    }
                }

                ProfileSummaryRow("Hedef", state.fitnessGoal.ifBlank { "Fit olmak" }, theme)
                ProfileSummaryRow("Boy / Kilo", "${state.heightText.ifBlank { "-" }} cm • ${state.weightText.ifBlank { "-" }} kg", theme)
                ProfileSummaryRow("BMI / VKİ", bmiText, theme)
                ProfileSummaryRow("Yağ oranı", fatText, theme)
            }
        }

        state.programError?.let { error ->
            Spacer(Modifier.height(14.dp))
            Text(
                error,
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(36.dp))

        Button(
            onClick  = { vm.saveAndGeneratePersonalProgram() },
            enabled  = !loading,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = theme.effectiveOnAccentColor)
        ) {
            if (loading) {
                CircularProgressIndicator(color = theme.effectiveOnAccentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Program hazırlanıyor...", fontWeight = FontWeight.Black, fontSize = 14.sp)
            } else {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Size Özel Program Oluştur", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        TextButton(
            onClick = { vm.saveAndFinish() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Şimdilik Atla", color = theme.text2, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = { vm.prevStep() }, accent = accent, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Geri",
                color = theme.text2,
                fontSize = 13.sp,
                modifier = Modifier.clickable(enabled = !loading) { vm.prevStep() }
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun ProfileSummaryRow(label: String, value: String, theme: AppThemeState) {
    val useStackedLayout = label == "Hedef" || value.length > 26

    if (useStackedLayout) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                color = theme.text2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                value,
                color = theme.text0,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = theme.text2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Yardımcı composable'lar ───────────────────────────────────────────────────

@Composable
private fun StepHeader(
    step    : String,
    title   : String,
    subtitle: String,
    accent  : Color,
    theme   : AppThemeState
) {
    Text(step, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(6.dp))
    Text(title, color = theme.text0, fontSize = 26.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, color = theme.text2, fontSize = 14.sp)
}

@Composable
private fun StepNavButtons(
    onBack   : () -> Unit,
    onNext   : () -> Unit,
    nextLabel: String,
    accent   : Color,
    theme    : AppThemeState,
    canSkip  : Boolean = false,
    onSkip   : () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = theme.effectiveOnAccentColor)
        ) {
            Text(nextLabel, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack, accent = accent, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Geri",
                    color = theme.text2,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onBack)
                )
            }
            if (canSkip) {
                TextButton(onClick = onSkip) {
                    Text("Atla", color = theme.text2, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.ArrowForwardIos, null, tint = theme.text2, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(preview: AppThemeState) {
    val accent = preview.effectiveAccentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(preview.bg1)
            .border(1.dp, preview.stroke, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                preview.accent.label,
                color         = accent,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
        Text("Profitness", color = preview.text0, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Merhaba, Atlet 👋", color = preview.text1, fontSize = 12.sp)
        Box(
            Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "UYGULA",
                color         = preview.effectiveOnAccentColor,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun ThemeColorSwatch(
    preset    : AccentPreset,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) preset.color.copy(0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) preset.color else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(preset.color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.Check, null, tint = preset.onColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun <T> ThemeSegmentedSelector(
    options : List<Pair<T, String>>,
    selected: T,
    accent  : Color,
    onAccent: Color,
    theme   : AppThemeState,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) accent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color         = if (isSelected) onAccent else theme.text1,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

@Composable
private fun onboardingTextFieldColors(accent: Color, theme: AppThemeState) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor    = accent,
        unfocusedBorderColor  = theme.stroke,
        focusedContainerColor = theme.bg1,
        unfocusedContainerColor = theme.bg1,
        cursorColor           = accent,
        focusedTextColor      = theme.text0,
        unfocusedTextColor    = theme.text0
    )

// DD/MM/YYYY görsel dönüşümü (EditProfileScreen'deki ile aynı)
private object DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if ((i == 1 || i == 3) && i < digits.lastIndex) append('/')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 4 -> offset + 1
                else        -> offset + 2
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset - 1
                else        -> offset - 2
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
