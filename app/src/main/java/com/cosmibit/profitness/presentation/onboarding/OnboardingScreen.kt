package com.cosmibit.profitness.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.unit.em
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.presentation.components.CustomAccentColorDialog
import com.cosmibit.profitness.presentation.components.CustomAccentSwatch
import com.cosmibit.profitness.presentation.profile.readSafeProfilePhotoBytes
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
    val responsive = rememberResponsiveLayoutInfo()
    val motionOn = rememberSinemaMotionOn()

    BackHandler(enabled = state.step > 0 && !state.isSaving) {
        viewModel.prevStep()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(theme))
    ) {
        // Sinema: üst 240dp orb (accent wash yok). Orb ve ilerleme sabit, yalnız içerik kayar.
        OnboardingOrb(theme, Modifier.align(Alignment.TopCenter).fillMaxWidth().height(240.dp))

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = responsive.formMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            OnboardingProgressBar(
                currentStep = state.step,
                totalSteps  = 7,
                theme       = theme,
                modifier    = Modifier.statusBarsPadding().padding(top = 14.dp, bottom = 4.dp)
            )

            AnimatedContent(
                targetState  = state.step,
                transitionSpec = {
                    if (!motionOn) {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(120))
                    } else {
                        val forward = targetState > initialState
                        val enter = slideInHorizontally(
                            tween(320, easing = FastOutSlowInEasing)
                        ) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(280))
                        val exit  = slideOutHorizontally(
                            tween(280, easing = FastOutSlowInEasing)
                        ) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200))
                        enter togetherWith exit
                    }
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> StepName(state, viewModel, theme)
                    1 -> StepTheme(current = theme, onThemeChange = onThemeChange, vm = viewModel)
                    2 -> StepSportExperience(state, viewModel, theme)
                    3 -> StepGenderBirth(state, viewModel, theme)
                    4 -> StepBodyMetrics(state, viewModel, theme)
                    5 -> StepGoal(state, viewModel, theme)
                    6 -> StepPersonalProgram(state, viewModel, theme)
                }
            }
        }
    }
}

// ── Orb (üst sahne) ──────────────────────────────────────────────────────────

/** SinemaScene orb zemini.*/
@Composable
private fun OnboardingOrb(theme: AppThemeState, modifier: Modifier) {
    SinemaScene(modifier)
}

// ── İlerleme çubuğu ──────────────────────────────────────────────────────────

@Composable
private fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps : Int,
    theme      : AppThemeState,
    modifier   : Modifier = Modifier
) {
    val accent = theme.effectiveAccentColor
    Row(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { i ->
                val active = i == currentStep
                val fraction by animateFloatAsState(
                    targetValue   = if (i <= currentStep) 1f else 0f,
                    animationSpec = tween(400),
                    label         = "progress_$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .then(if (active) Modifier.sinemaGlow(accent.copy(0.4f), 8.dp, 0.dp, CircleShape) else Modifier)
                        .clip(CircleShape)
                        .background(Sinema.line(theme))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(accent)
                    )
                }
            }
        }
        Text(
            "%02d/%02d".format(currentStep + 1, totalSteps),
            style = Sinema.Label,
            color = Sinema.text(theme).copy(0.8f),
            maxLines = 1
        )
    }
}

// ── Adım iskeleti: kayan içerik + altta sabit gezinme ────────────────────────

@Composable
private fun StepScaffold(
    nav: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Kısa adımlarda kalan yüksekliği orb sahnesi doldurur (ölü alan yok); uzunsa kayar.
                Column(Modifier.fillMaxWidth().heightIn(min = viewport)) {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(12.dp))
                        content()
                        Spacer(Modifier.height(16.dp))
                    }
                    OnboardingOrb(LocalAppTheme.current, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            content = nav
        )
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
        surfaceStyle     = SurfaceStyle.GRAPHITE,
        intensity        = intensity,
        customAccentArgb = customAccentArgb
    )
    val previewAccent = preview.effectiveAccentColor
    // Sayfa zemini gerçek temada kalır; Sinema parçaları seçilen vurguyu (önizleme) kullanır.
    val accentTheme = preview

    CompositionLocalProvider(LocalAppTheme provides accentTheme) {
        StepScaffold(
            nav = {
                StepNavButtons(
                    onBack    = { vm.prevStep() },
                    onNext    = {
                        onThemeChange(preview)
                        vm.nextStep()
                    },
                    nextLabel = current.t("Temayı Uygula", "Apply Theme"),
                    nextIcon  = Icons.Rounded.Check,
                    theme     = accentTheme
                )
            }
        ) {
            StepHeader(
                step     = "2 / 7",
                title    = current.t("Temanı seç", "Choose your theme"),
                subtitle = current.t("Uygulamanın sana nasıl görüneceğini ayarla", "Adjust how the app looks for you"),
                theme    = accentTheme
            )

            Spacer(Modifier.height(24.dp))

            ThemePreviewCard(preview)
            Spacer(Modifier.height(22.dp))

            SectionLabel(current.t("VURGU RENGİ", "ACCENT COLOR"), accentTheme)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                presetRows.forEach { preset ->
                    ThemeColorSwatch(
                        preset     = preset,
                        isSelected = customAccentArgb == null && accent == preset,
                        ringColor  = Sinema.text(current),
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

            SectionLabel(current.t("YOĞUNLUK", "INTENSITY"), accentTheme)
            Spacer(Modifier.height(8.dp))
            SinemaSegmentRow(
                options  = listOf(AccentIntensity.NEON, AccentIntensity.PASTEL, AccentIntensity.VIVID, AccentIntensity.SOFT),
                selected = intensity,
                label    = {
                    when (it) {
                        AccentIntensity.NEON -> "NEON"
                        AccentIntensity.PASTEL -> "PASTEL"
                        AccentIntensity.VIVID -> current.t("CANLI", "VIVID")
                        AccentIntensity.SOFT -> current.t("SOFT", "SOFT")
                    }
                },
                onSelect = { intensity = it }
            )
        }
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
    theme    : AppThemeState
) {
    val context = LocalContext.current
    val accent = theme.effectiveAccentColor
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

    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.nextStep() },
                nextLabel = theme.t("Devam Et", "Continue"),
                nextIcon  = Icons.AutoMirrored.Rounded.ArrowForward,
                theme     = theme,
                showBack  = false,
                error     = state.nameError
            )
        }
    ) {
        StepHeader(
            step     = "1 / 7",
            eyebrow  = theme.t("Merhaba!", "Hello!"),
            title    = theme.t("Seni tanıyalım", "Let's get to know you"),
            subtitle = theme.t("Adını ve avatarını seç", "Choose your name and avatar"),
            theme    = theme
        )

        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Seçili avatar önizleme
            Box(
                modifier         = Modifier
                    .size(100.dp)
                    .sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Sinema.surface(theme))
                    .border(2.dp, accent, CircleShape),
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

            Spacer(Modifier.height(14.dp))

            SinemaCapsule(
                onClick = { photoPickerLauncher.launch("image/*") },
                enabled = !state.isUploadingAvatar,
                height = 44.dp,
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) { fg ->
                Icon(Icons.Rounded.PhotoCamera, null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(theme.t("Profil fotoğrafı yükle", "Upload profile photo"), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        state.avatarError?.let {
            Spacer(Modifier.height(10.dp))
            OnboardingErrorCapsule(it, theme)
        }

        Spacer(Modifier.height(16.dp))

        // Avatar grid
        val avatarShape = RoundedCornerShape(16.dp)
        val onAccent = theme.effectiveOnAccentColor
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
                            .then(
                                if (selected) Modifier.sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, avatarShape).clip(avatarShape).background(accent)
                                else Modifier.sinemaCard(theme, avatarShape)
                            )
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.setAvatar(emoji) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 24.sp, color = onAccent)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // İsim alanı
        SinemaGlassField(
            value         = state.name,
            onValueChange = { vm.setName(it) },
            label         = theme.t("ADIN SOYADIN", "FULL NAME"),
            placeholder   = theme.t("Adını gir", "Enter your name"),
            keyboardType  = KeyboardType.Text,
            imeAction     = ImeAction.Done,
            capitalization = KeyboardCapitalization.Words,
            modifier      = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(nameBringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.hasFocus) {
                        coroutineScope.launch {
                            delay(250)
                            nameBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
    }
}

// ── Adım 3: Spor profili ─────────────────────────────────────────────────────

private fun sportIcon(option: String): ImageVector = when (option) {
    "Fitness" -> Icons.Rounded.FitnessCenter
    "Koşu" -> Icons.Rounded.DirectionsRun
    "Bisiklet" -> Icons.Rounded.DirectionsBike
    "Yüzme" -> Icons.Rounded.Pool
    "Futbol" -> Icons.Rounded.SportsSoccer
    "Basketbol" -> Icons.Rounded.SportsBasketball
    "Tenis" -> Icons.Rounded.SportsTennis
    "Boks" -> Icons.Rounded.SportsMma
    "Yoga" -> Icons.Rounded.SelfImprovement
    else -> Icons.Rounded.DirectionsWalk
}

private fun experienceIcon(option: String): ImageVector = when (option) {
    "Yeni başlıyorum" -> Icons.Rounded.Eco
    "Başlangıç" -> Icons.Rounded.StarBorder
    "Orta" -> Icons.Rounded.StarHalf
    else -> Icons.Rounded.Star
}

private fun goalIcon(option: String): ImageVector = when (option) {
    "Kilo vermek" -> Icons.Rounded.TrendingDown
    "Kas kazanmak" -> Icons.Rounded.FitnessCenter
    "Formda kalmak" -> Icons.Rounded.Favorite
    "Yağ yakarken kas yapmak" -> Icons.Rounded.Whatshot
    "Kondisyon geliştirmek" -> Icons.Rounded.Speed
    else -> Icons.Rounded.Spa
}

@Composable
private fun StepSportExperience(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    theme  : AppThemeState
) {
    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.nextStep() },
                nextLabel = theme.t("Devam Et", "Continue"),
                nextIcon  = Icons.AutoMirrored.Rounded.ArrowForward,
                theme     = theme,
                canSkip   = true,
                onSkip    = { vm.nextStep() }
            )
        }
    ) {
        StepHeader(
            step     = "3 / 7",
            title    = theme.t("Spor profilin", "Your sport profile"),
            subtitle = theme.t("Programın spor dalına ve seviyene göre şekillensin", "Shape the plan around your sport and level"),
            theme    = theme
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel(theme.t("SPOR DALI", "SPORT"), theme)
        Spacer(Modifier.height(8.dp))
        OnboardingChoiceGrid(
            options = SPORT_BRANCH_OPTIONS,
            selected = state.sportBranch,
            theme = theme,
            iconFor = ::sportIcon,
            labelFor = { theme.sportBranchLabel(it) },
            onSelect = { vm.setSportBranch(it) }
        )

        Spacer(Modifier.height(18.dp))

        SectionLabel(theme.t("SEVİYE", "LEVEL"), theme)
        Spacer(Modifier.height(8.dp))
        OnboardingChoiceGrid(
            options = EXPERIENCE_OPTIONS,
            selected = state.experienceLevel,
            theme = theme,
            iconFor = ::experienceIcon,
            labelFor = { theme.experienceLabel(it) },
            onSelect = { vm.setExperienceLevel(it) }
        )

        Spacer(Modifier.height(18.dp))

        SectionLabel(theme.t("HAFTALIK ANTRENMAN", "WEEKLY TRAINING"), theme)
        Spacer(Modifier.height(8.dp))
        OnboardingChoiceGrid(
            options = WEEKLY_DAY_OPTIONS,
            selected = state.weeklyDays,
            theme = theme,
            iconFor = { Icons.Rounded.CalendarMonth },
            labelFor = { theme.t("$it gün", "$it days") },
            onSelect = { vm.setWeeklyDays(it) }
        )
    }
}

/** 2 sütun sinemaCard r24, 88dp; seçili = dolu vurgu + glow. */
@Composable
private fun OnboardingChoiceGrid(
    options : List<String>,
    selected: String,
    theme   : AppThemeState,
    iconFor : (String) -> ImageVector,
    labelFor: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    val responsive = rememberResponsiveLayoutInfo()
    val accent = theme.effectiveAccentColor
    val onAccent = theme.effectiveOnAccentColor
    val shape = RoundedCornerShape(24.dp)
    val renderedOptions = options.map { it to labelFor(it) }
    val useSingleColumn = responsive.isVeryLargeFont ||
        responsive.screenWidth < 350.dp ||
        (responsive.isLargeFont && renderedOptions.any { (_, label) -> label.length > 14 })
    val rows = if (useSingleColumn) renderedOptions.map { listOf(it) } else renderedOptions.chunked(2)
    rows.forEachIndexed { index, row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { (option, label) ->
                val isSelected = selected == option
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 88.dp)
                        .then(
                            if (isSelected) Modifier.sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, shape).clip(shape).background(accent)
                            else Modifier.sinemaCard(theme, shape)
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        iconFor(option), null,
                        tint = if (isSelected) onAccent else Sinema.textSecondary(theme),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label,
                        color = if (isSelected) onAccent else Sinema.text(theme),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (row.size == 1 && !useSingleColumn) Spacer(Modifier.weight(1f))
        }
        if (index != rows.lastIndex) {
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── Adım 4: Cinsiyet + Doğum Tarihi ──────────────────────────────────────────

@Composable
private fun StepGenderBirth(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    theme  : AppThemeState
) {
    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.nextStep() },
                nextLabel = theme.t("Devam Et", "Continue"),
                nextIcon  = Icons.AutoMirrored.Rounded.ArrowForward,
                theme     = theme,
                canSkip   = true,
                onSkip    = { vm.nextStep() }
            )
        }
    ) {
        StepHeader(
            step    = "4 / 7",
            title   = theme.t("Biraz daha bilgi", "A little more info"),
            subtitle = theme.t("Cinsiyet ve doğum tarihin", "Gender and birth date"),
            theme   = theme
        )

        Spacer(Modifier.height(28.dp))

        // Cinsiyet
        SectionLabel(theme.t("CİNSİYET", "GENDER"), theme)
        Spacer(Modifier.height(8.dp))
        SinemaSegmentRow(
            options  = GENDER_OPTIONS.map { it.second },
            selected = state.gender,
            label    = { dbValue -> theme.genderLabel(dbValue, GENDER_OPTIONS.firstOrNull { it.second == dbValue }?.first ?: dbValue) },
            onSelect = { vm.setGender(it) },
            height   = 48.dp
        )

        Spacer(Modifier.height(24.dp))

        // Doğum tarihi
        SinemaGlassField(
            value         = state.birthDigits,
            onValueChange = { vm.setBirthDigits(it) },
            label         = theme.t("DOĞUM TARİHİ", "BIRTH DATE"),
            placeholder   = "15/06/1995",
            keyboardType  = KeyboardType.Number,
            imeAction     = ImeAction.Done,
            visualTransformation = DateVisualTransformation
        )
    }
}

// ── Adım 2: Boy + Kilo ───────────────────────────────────────────────────────

@Composable
private fun StepBodyMetrics(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    theme  : AppThemeState
) {
    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.nextStep() },
                nextLabel = theme.t("Devam Et", "Continue"),
                nextIcon  = Icons.AutoMirrored.Rounded.ArrowForward,
                theme     = theme,
                canSkip   = true,
                onSkip    = { vm.nextStep() }
            )
        }
    ) {
        StepHeader(
            step     = "5 / 7",
            title    = theme.t("Vücut ölçülerin", "Body metrics"),
            subtitle = theme.t("Boy ve kilonu gir", "Enter your height and weight"),
            theme    = theme
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                SinemaBigStat(
                    value = state.heightText.ifBlank { "–" },
                    label = theme.t("BOY (cm)", "HEIGHT (cm)"),
                    size  = 56
                )
                Spacer(Modifier.height(12.dp))
                SinemaGlassField(
                    value         = state.heightText,
                    onValueChange = { vm.setHeight(it) },
                    label         = "",
                    placeholder   = "175",
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Next
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                SinemaBigStat(
                    value = state.weightText.ifBlank { "–" },
                    label = theme.t("KİLO (kg)", "WEIGHT (kg)"),
                    size  = 56
                )
                Spacer(Modifier.height(12.dp))
                SinemaGlassField(
                    value         = state.weightText,
                    onValueChange = { vm.setWeight(it) },
                    label         = "",
                    placeholder   = "75",
                    keyboardType  = KeyboardType.Number,
                    imeAction     = ImeAction.Done
                )
            }
        }

        // BMI ve yağ oranı önizleme
        val h = state.heightText.toDoubleOrNull() ?: 0.0
        val w = state.weightText.toDoubleOrNull() ?: 0.0
        if (h > 0 && w > 0) {
            Spacer(Modifier.height(20.dp))
            val bmi      = vm.bmi
            val bodyFat  = vm.bodyFatPct
            val bmiLabel = when {
                bmi < 18.5 -> theme.t("Zayıf", "Underweight")
                bmi < 25.0 -> theme.t("Normal", "Normal")
                bmi < 30.0 -> theme.t("Fazla Kilolu", "Overweight")
                else       -> theme.t("Obez", "Obese")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sinemaCard(theme, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SinemaBigStat(value = "%.1f".format(bmi), label = theme.t("BMI / VKİ", "BMI"), size = 44)
                Column(horizontalAlignment = Alignment.End) {
                    Text(bmiLabel, color = Sinema.text(theme), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    if (bodyFat > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            theme.t("Tahmini yağ: %.1f%%", "Estimated fat: %.1f%%").format(bodyFat),
                            color = Sinema.textSecondary(theme),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Adım 3: Fitness Hedefi ────────────────────────────────────────────────────

@Composable
private fun StepGoal(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    theme  : AppThemeState
) {
    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.nextStep() },
                nextLabel = theme.t("Program Önerisine Geç", "Go to Program Suggestion"),
                nextIcon  = Icons.Rounded.AutoAwesome,
                theme     = theme,
                locked    = state.isSaving
            )
        }
    ) {
        StepHeader(
            step     = "6 / 7",
            title    = theme.t("Hedefin ne?", "What's your goal?"),
            subtitle = theme.t("Seni motive edecek hedefi seç", "Choose the goal that motivates you"),
            theme    = theme
        )

        Spacer(Modifier.height(24.dp))

        OnboardingChoiceGrid(
            options = GOAL_SUGGESTIONS,
            selected = state.fitnessGoal,
            theme = theme,
            iconFor = ::goalIcon,
            labelFor = { theme.goalLabel(it) },
            onSelect = { suggestion ->
                vm.setFitnessGoal(if (state.fitnessGoal == suggestion) "" else suggestion)
            }
        )

        Spacer(Modifier.height(20.dp))

        // Serbest metin
        SinemaGlassField(
            value         = state.fitnessGoal,
            onValueChange = { vm.setFitnessGoal(it) },
            label         = theme.t("VEYA KENDİN YAZ", "OR WRITE YOUR OWN"),
            placeholder   = theme.t("Hedefinizi yazın…", "Write your goal..."),
            keyboardType  = KeyboardType.Text,
            imeAction     = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences
        )
    }
}

// ── Adım 7: Kişiye özel program ──────────────────────────────────────────────

@Composable
private fun StepPersonalProgram(
    state  : OnboardingState,
    vm     : OnboardingViewModel,
    theme  : AppThemeState
) {
    val accent = theme.effectiveAccentColor
    val loading = state.isSaving || state.isGeneratingProgram
    val bmi = vm.bmi
    val bodyFat = vm.bodyFatPct
    val bmiText = if (bmi > 0) "%.1f".format(bmi) else theme.t("Eksik", "Missing")
    val fatText = if (bodyFat > 0) "%.1f%%".format(bodyFat) else theme.t("Eksik", "Missing")

    StepScaffold(
        nav = {
            StepNavButtons(
                onBack    = { vm.prevStep() },
                onNext    = { vm.saveAndGeneratePersonalProgram() },
                nextLabel = if (loading) theme.t("Program hazırlanıyor...", "Preparing program...")
                            else theme.t("Size Özel Program Oluştur", "Create Your Program"),
                nextIcon  = Icons.Rounded.AutoAwesome,
                theme     = theme,
                canSkip   = true,
                skipLabel = theme.t("Şimdilik Atla", "Skip for now"),
                onSkip    = { vm.saveAndFinish() },
                locked    = loading,
                error     = state.programError
            )
        }
    ) {
        StepHeader(
            step     = "7 / 7",
            title    = theme.t("Programın hazır olsun mu?", "Ready for your program?"),
            subtitle = theme.t("Bilgilerine göre AI ile kişiye özel plan çıkaralım", "Let AI build a personal plan from your info"),
            theme    = theme
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sinemaCard(theme, RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(theme.sportBranchLabel(state.sportBranch), color = Sinema.text(theme), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${theme.experienceLabel(state.experienceLevel)} • ${theme.t("Haftada ${state.weeklyDays} gün", "${state.weeklyDays} days/week")}",
                        color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
            }

            ProfileSummaryRow(theme.t("Hedef", "Goal"), state.fitnessGoal.ifBlank { theme.t("Fit olmak", "Get fit") }, theme)
            ProfileSummaryRow(theme.t("Boy / Kilo", "Height / Weight"), "${state.heightText.ifBlank { "-" }} cm • ${state.weightText.ifBlank { "-" }} kg", theme)
            ProfileSummaryRow(theme.t("BMI / VKİ", "BMI"), bmiText, theme)
            ProfileSummaryRow(theme.t("Yağ oranı", "Body fat"), fatText, theme)
        }
    }
}

@Composable
private fun ProfileSummaryRow(label: String, value: String, theme: AppThemeState) {
    val useStackedLayout = label == theme.t("Hedef", "Goal") || value.length > 26
    val labelText: @Composable (Modifier) -> Unit = { m ->
        Text(
            label.sinemaUpper(theme),
            color = Sinema.textSecondary(theme),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.em,
            modifier = m
        )
    }

    if (useStackedLayout) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            labelText(Modifier)
            Text(
                value,
                color = Sinema.text(theme),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labelText(Modifier.weight(1f))
            Text(
                value,
                color = Sinema.text(theme),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Yardımcı composable'lar ───────────────────────────────────────────────────

/** Adım etiketi 12sp + dev adım rakamı 72sp + başlık 30sp/800 + alt metin 14sp. */
@Composable
private fun StepHeader(
    step    : String,
    title   : String,
    subtitle: String,
    theme   : AppThemeState,
    eyebrow : String? = null
) {
    val number = step.substringBefore("/").trim().padStart(2, '0')
    Text((eyebrow ?: step).sinemaUpper(theme), style = Sinema.Label, color = Sinema.text(theme).copy(0.8f), maxLines = 1)
    Text(
        number,
        style = Sinema.Metric.copy(fontSize = 72.sp),
        color = Sinema.text(theme),
        maxLines = 1
    )
    Spacer(Modifier.height(4.dp))
    Text(
        title,
        style = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold),
        color = Sinema.text(theme),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(6.dp))
    Text(
        subtitle,
        color = Sinema.textSecondary(theme),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SectionLabel(text: String, theme: AppThemeState) {
    Text(text.sinemaUpper(theme), style = Sinema.Label, color = Sinema.textSecondary(theme), maxLines = 1)
}

/** Ortak hata kapsülü: 16dp uyarı ikonu (yalnız ikon hata rengi) + text0. */
@Composable
private fun OnboardingErrorCapsule(text: String, theme: AppThemeState, modifier: Modifier = Modifier) {
    SinemaCapsule(
        modifier = modifier.fillMaxWidth(),
        height = 40.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) { fg ->
        Icon(Icons.Rounded.ErrorOutline, null, tint = CriticalRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
    }
}

/**
 * Altta sabit gezinme: üstte geri (cam daire) + atla (SinemaPill), altında ana eylem SinemaGoButton.
 * `locked` (kaydediliyor): Go dim + tıklama kilitli, geri/atla kilitli. Hata Go'nun üstünde kapsül.
 */
@Composable
private fun StepNavButtons(
    onBack   : () -> Unit,
    onNext   : () -> Unit,
    nextLabel: String,
    nextIcon : ImageVector,
    theme    : AppThemeState,
    canSkip  : Boolean = false,
    skipLabel: String = theme.t("Atla", "Skip"),
    onSkip   : () -> Unit = {},
    showBack : Boolean = true,
    locked   : Boolean = false,
    error    : String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showBack || canSkip) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack) {
                    SinemaCapsule(
                        modifier = Modifier.width(48.dp).alpha(if (locked) 0.4f else 1f),
                        onClick = onBack, enabled = !locked, height = 48.dp, shape = CircleShape,
horizontalArrangement = Arrangement.Center,
                        contentPadding = PaddingValues(0.dp)
                    ) { fg ->
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, theme.t("Geri", "Back"), tint = fg, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (canSkip) {
                    SinemaCapsule(
                        modifier = Modifier.alpha(if (locked) 0.4f else 1f),
                        onClick = onSkip, enabled = !locked, height = 40.dp, shape = CircleShape,
contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { fg ->
                        Text(skipLabel, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = fg, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        if (error != null) OnboardingErrorCapsule(error, theme)
        SinemaGoButton(
            label   = nextLabel,
            icon    = nextIcon,
            onClick = { if (!locked) onNext() },
            dim     = locked
        )
    }
}

/** Mini önizleme: gerçek tema renkleriyle sinemaCard r28. */
@Composable
private fun ThemePreviewCard(preview: AppThemeState) {
    val accent = preview.effectiveAccentColor
    val btnShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(preview, RoundedCornerShape(28.dp))
            .padding(20.dp),
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
                color         = preview.accentTextColor,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.1.em
            )
        }
        Text("Profitness", color = Sinema.text(preview), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Merhaba, Atlet 👋", color = Sinema.textSecondary(preview), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(
            Modifier
                .padding(top = 4.dp)
                .sinemaGlow(accent.copy(0.4f), 14.dp, 5.dp, btnShape)
                .clip(btnShape)
                .background(accent)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                "UYGULA",
                color         = preview.effectiveOnAccentColor,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.1.em
            )
        }
    }
}

/** 44dp daire; seçili = 3dp halka + glow. */
@Composable
private fun ThemeColorSwatch(
    preset    : AccentPreset,
    isSelected: Boolean,
    ringColor : Color,
    onClick   : () -> Unit
) {
    val color = preset.color.toSafeAccentColor()
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(if (isSelected) Modifier.sinemaGlow(color.copy(0.4f), 14.dp, 4.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(color)
            .then(if (isSelected) Modifier.border(3.dp, ringColor, CircleShape) else Modifier)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Rounded.Check, null, tint = color.readableOnAccentColor(), modifier = Modifier.size(16.dp))
        }
    }
}

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
