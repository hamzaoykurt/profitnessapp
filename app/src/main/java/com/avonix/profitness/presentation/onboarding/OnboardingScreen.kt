package com.avonix.profitness.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.AccentColorSwatch
import com.avonix.profitness.core.ui.rememberResponsiveLayoutInfo
import com.avonix.profitness.presentation.components.AppBackButton
import com.avonix.profitness.presentation.components.CustomAccentColorDialog
import com.avonix.profitness.presentation.components.CustomAccentSwatch
import com.avonix.profitness.presentation.profile.readSafeProfilePhotoBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SPORT_AVATAR_ROWS = listOf(
    listOf("🏋️", "🏃", "🚴", "🏊", "⚽", "🏀"),
    listOf("🎾", "🥊", "🧘", "🚶", "🤸", "⛹️"),
    listOf("🏋️‍♀️", "🏃‍♀️", "🚴‍♀️", "🏊‍♀️", "🏐", "🏓"),
    listOf("💪", "🔥", "⚡", "🏆", "🎯", "🥇")
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

private fun AppThemeState.genderLabel(dbValue: String, fallback: String): String = when (dbValue) {
    "male" -> t("Erkek", "Male")
    "female" -> t("Kadın", "Female")
    "other" -> t("Belirtme", "Prefer not to say")
    else -> fallback
}

private fun AppThemeState.goalLabel(value: String): String = when (value) {
    "Kilo vermek" -> t("Kilo vermek", "Lose weight")
    "Kas kazanmak" -> t("Kas kazanmak", "Build muscle")
    "Formda kalmak" -> t("Formda kalmak", "Stay fit")
    "Yağ yakarken kas yapmak" -> t("Yağ yakarken kas yapmak", "Recompose")
    "Kondisyon geliştirmek" -> t("Kondisyon geliştirmek", "Improve conditioning")
    "Sağlıklı yaşam" -> t("Sağlıklı yaşam", "Healthy lifestyle")
    else -> value
}

private fun AppThemeState.sportBranchLabel(value: String): String = when (value) {
    "Koşu" -> t("Koşu", "Running")
    "Bisiklet" -> t("Bisiklet", "Cycling")
    "Yüzme" -> t("Yüzme", "Swimming")
    "Futbol" -> t("Futbol", "Football")
    "Basketbol" -> t("Basketbol", "Basketball")
    "Tenis" -> t("Tenis", "Tennis")
    "Boks" -> t("Boks", "Boxing")
    "Yürüyüş" -> t("Yürüyüş", "Walking")
    else -> value
}

private fun AppThemeState.experienceLabel(value: String): String = when (value) {
    "Yeni başlıyorum" -> t("Yeni başlıyorum", "Brand new")
    "Başlangıç" -> t("Başlangıç", "Beginner")
    "Orta" -> t("Orta", "Intermediate")
    "İleri" -> t("İleri", "Advanced")
    else -> value
}

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
    var intensity    by remember { mutableStateOf(current.intensity) }
    var customAccentArgb by remember { mutableStateOf(current.customAccentArgb) }
    var showColorPicker by remember { mutableStateOf(false) }
    val presetRows = remember {
        listOf(
            AccentPreset.LIME,
            AccentPreset.PURPLE,
            AccentPreset.CYAN,
            AccentPreset.ORANGE,
            AccentPreset.PINK,
            AccentPreset.BLUE,
            AccentPreset.RED,
            AccentPreset.YELLOW,
            AccentPreset.GREEN,
            AccentPreset.TEAL,
            AccentPreset.AMBER
        )
    }
    val preview = current.copy(
        accent           = accent,
        surfaceStyle     = SurfaceStyle.OLED,
        intensity        = intensity,
        customAccentArgb = customAccentArgb
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
            title    = current.t("Temanı seç", "Choose your theme"),
            subtitle = current.t("Uygulamanın sana nasıl görüneceğini ayarla", "Adjust how the app looks for you"),
            accent   = previewAccent,
            theme    = preview
        )

        Spacer(Modifier.height(24.dp))
        ThemePreviewCard(preview)
        Spacer(Modifier.height(22.dp))

        Text(current.t("VURGU RENGİ", "ACCENT COLOR"), color = current.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            presetRows.forEach { preset ->
                AccentColorSwatch(
                    color      = preset.color,
                    isSelected = customAccentArgb == null && accent == preset,
                    onClick    = {
                        accent = preset
                        customAccentArgb = null
                    }
                )
            }
            CustomAccentSwatch(
                color      = customAccentArgb?.let { Color(it) } ?: previewAccent,
                isSelected = customAccentArgb != null,
                onClick    = { showColorPicker = true }
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(current.t("YOĞUNLUK", "INTENSITY"), color = current.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        ThemeSegmentedSelector(
            options = listOf(
                AccentIntensity.NEON to "NEON",
                AccentIntensity.PASTEL to "PASTEL",
                AccentIntensity.VIVID to current.t("CANLI", "VIVID"),
                AccentIntensity.SOFT to current.t("SOFT", "SOFT")
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
            nextLabel = current.t("Temayı Uygula", "Apply Theme"),
            accent    = previewAccent,
            theme     = preview
        )

        Spacer(Modifier.height(48.dp))
    }

    if (showColorPicker) {
        CustomAccentColorDialog(
            initialColor = customAccentArgb?.let { Color(it) } ?: previewAccent,
            theme = preview,
            onDismiss = { showColorPicker = false },
            onColorSelected = { selected ->
                customAccentArgb = selected
            }
        )
    }
}

// ── Adım 0: İsim + Avatar ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepName(
    state    : OnboardingState,
    vm       : OnboardingViewModel,
    accent   : Color,
    theme    : AppThemeState
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val nameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val result = readSafeProfilePhotoBytes(context, uri)
        val bytes = result.bytes
        if (bytes == null) {
            vm.setAvatarError(result.errorMessage ?: "Profil fotoğrafı okunamadı.")
            return@rememberLauncherForActivityResult
        }
        vm.uploadPhoto(bytes)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(theme.t("Merhaba!", "Hello!"), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            theme.t("Seni tanıyalım", "Let's get to know you"),
            color      = theme.text0,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            theme.t("Adını ve avatarını seç", "Choose your name and avatar"),
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
            when {
                state.isUploadingAvatar -> {
                    CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                }
                state.avatar.startsWith("http") -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(state.avatar).crossfade(true).build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                else -> {
                    Text(state.avatar, fontSize = 44.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { photoPickerLauncher.launch("image/*") },
            enabled = !state.isUploadingAvatar,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = accent.copy(0.08f)),
            modifier = Modifier.height(42.dp)
        ) {
            Icon(Icons.Rounded.PhotoCamera, null, tint = accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(theme.t("Profil fotoğrafı yükle", "Upload profile photo"), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        state.avatarError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }

        Spacer(Modifier.height(14.dp))

        // Avatar grid
        SPORT_AVATAR_ROWS.forEach { row ->
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(nameBringIntoViewRequester),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(theme.t("ADIN SOYADIN", "FULL NAME"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            OutlinedTextField(
                value          = state.name,
                onValueChange  = { vm.setName(it) },
                placeholder    = { Text(theme.t("Adını gir", "Enter your name"), color = theme.text2, fontSize = 14.sp) },
                leadingIcon    = { Icon(Icons.Rounded.Person, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp)) },
                isError        = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } },
                modifier       = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(250)
                                nameBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
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
            Text(theme.t("Devam Et", "Continue"), fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
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
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        StepHeader(
            step     = "3 / 7",
            title    = theme.t("Spor profilin", "Your sport profile"),
            subtitle = theme.t("Programın spor dalına ve seviyene göre şekillensin", "Shape the plan around your sport and level"),
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(18.dp))

        Text(theme.t("SPOR DALI", "SPORT"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OnboardingChoiceGrid(
            options = SPORT_BRANCH_OPTIONS,
            selected = state.sportBranch,
            accent = accent,
            theme = theme,
            itemHeight = 38.dp,
            rowSpacing = 7.dp,
            fontSize = 12.sp,
            labelFor = { theme.sportBranchLabel(it) },
            onSelect = { vm.setSportBranch(it) }
        )

        Spacer(Modifier.height(12.dp))

        Text(theme.t("SEVİYE", "LEVEL"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OnboardingChoiceGrid(
            options = EXPERIENCE_OPTIONS,
            selected = state.experienceLevel,
            accent = accent,
            theme = theme,
            itemHeight = 38.dp,
            rowSpacing = 7.dp,
            fontSize = 12.sp,
            labelFor = { theme.experienceLabel(it) },
            onSelect = { vm.setExperienceLevel(it) }
        )

        Spacer(Modifier.height(12.dp))

        Text(theme.t("HAFTALIK ANTRENMAN", "WEEKLY TRAINING"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OnboardingChoiceGrid(
            options = WEEKLY_DAY_OPTIONS,
            selected = state.weeklyDays,
            accent = accent,
            theme = theme,
            labelFor = { theme.t("$it gün", "$it days") },
            onSelect = { vm.setWeeklyDays(it) }
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(14.dp))

        StepNavButtons(
            onBack    = { vm.prevStep() },
            onNext    = { vm.nextStep() },
            nextLabel = theme.t("Devam Et", "Continue"),
            accent    = accent,
            theme     = theme,
            canSkip   = true,
            onSkip    = { vm.nextStep() }
        )

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun OnboardingChoiceGrid(
    options : List<String>,
    selected: String,
    accent  : Color,
    theme   : AppThemeState,
    itemHeight: androidx.compose.ui.unit.Dp = 38.dp,
    rowSpacing: androidx.compose.ui.unit.Dp = 7.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    labelFor: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    val rows = options.chunked(2)
    rows.forEachIndexed { index, row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(itemHeight)
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
                        fontSize = fontSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        if (index != rows.lastIndex) {
            Spacer(Modifier.height(rowSpacing))
        }
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
            title   = theme.t("Biraz daha bilgi", "A little more info"),
            subtitle = theme.t("Cinsiyet ve doğum tarihin", "Gender and birth date"),
            accent  = accent,
            theme   = theme
        )

        Spacer(Modifier.height(36.dp))

        // Cinsiyet
        Text(theme.t("CİNSİYET", "GENDER"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                        theme.genderLabel(dbValue, label),
                        color      = if (selected) Color.Black else theme.text2,
                        fontSize   = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Doğum tarihi
        Text(theme.t("DOĞUM TARİHİ", "BIRTH DATE"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
            nextLabel = theme.t("Devam Et", "Continue"),
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
            title    = theme.t("Vücut ölçülerin", "Body metrics"),
            subtitle = theme.t("Boy ve kilonu gir", "Enter your height and weight"),
            accent   = accent,
            theme    = theme
        )

        Spacer(Modifier.height(36.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(theme.t("BOY (cm)", "HEIGHT (cm)"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                Text(theme.t("KİLO (kg)", "WEIGHT (kg)"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                bmi < 18.5 -> theme.t("Zayıf", "Underweight")
                bmi < 25.0 -> theme.t("Normal", "Normal")
                bmi < 30.0 -> theme.t("Fazla Kilolu", "Overweight")
                else       -> theme.t("Obez", "Obese")
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
                    Text(theme.t("BMI / VKİ", "BMI"), color = bmiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("%.1f — %s".format(bmi, bmiLabel), color = bmiColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (bodyFat > 0) {
                            Text(
                                theme.t("Tahmini yağ: %.1f%%", "Estimated fat: %.1f%%").format(bodyFat),
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
            nextLabel = theme.t("Devam Et", "Continue"),
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
            title    = theme.t("Hedefin ne?", "What's your goal?"),
            subtitle = theme.t("Seni motive edecek hedefi seç", "Choose the goal that motivates you"),
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
                            theme.goalLabel(suggestion),
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
        Text(theme.t("VEYA KENDİN YAZ", "OR WRITE YOUR OWN"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value         = state.fitnessGoal,
            onValueChange = { vm.setFitnessGoal(it) },
            placeholder   = { Text(theme.t("Hedefinizi yazın…", "Write your goal..."), color = theme.text2, fontSize = 14.sp) },
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
                Text(theme.t("Program Önerisine Geç", "Go to Program Suggestion"), fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
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
    val bmiText = if (bmi > 0) "%.1f".format(bmi) else theme.t("Eksik", "Missing")
    val fatText = if (bodyFat > 0) "%.1f%%".format(bodyFat) else theme.t("Eksik", "Missing")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepHeader(
            step     = "7 / 7",
            title    = theme.t("Programın hazır olsun mu?", "Ready for your program?"),
            subtitle = theme.t("Bilgilerine göre AI ile kişiye özel plan çıkaralım", "Let AI build a personal plan from your info"),
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
                        Text(theme.sportBranchLabel(state.sportBranch), color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("${theme.experienceLabel(state.experienceLevel)} • ${theme.t("Haftada ${state.weeklyDays} gün", "${state.weeklyDays} days/week")}", color = theme.text2, fontSize = 13.sp)
                    }
                }

                ProfileSummaryRow(theme.t("Hedef", "Goal"), state.fitnessGoal.ifBlank { theme.t("Fit olmak", "Get fit") }, theme)
                ProfileSummaryRow(theme.t("Boy / Kilo", "Height / Weight"), "${state.heightText.ifBlank { "-" }} cm • ${state.weightText.ifBlank { "-" }} kg", theme)
                ProfileSummaryRow(theme.t("BMI / VKİ", "BMI"), bmiText, theme)
                ProfileSummaryRow(theme.t("Yağ oranı", "Body fat"), fatText, theme)
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
                Text(theme.t("Program hazırlanıyor...", "Preparing program..."), fontWeight = FontWeight.Black, fontSize = 14.sp)
            } else {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Size Özel Program Oluştur", "Create Your Program"), fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        TextButton(
            onClick = { vm.saveAndFinish() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(theme.t("Şimdilik Atla", "Skip for now"), color = theme.text2, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = { vm.prevStep() }, accent = accent, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                theme.t("Geri", "Back"),
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
    val useStackedLayout = label == theme.t("Hedef", "Goal") || value.length > 26

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
                    theme.t("Geri", "Back"),
                    color = theme.text2,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onBack)
                )
            }
            if (canSkip) {
                TextButton(onClick = onSkip) {
                    Text(theme.t("Atla", "Skip"), color = theme.text2, fontSize = 13.sp)
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
                preview.accentDisplayLabel,
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
