package com.avonix.profitness.presentation.onboarding

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*

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
    "Kondisyon geliştirmek",
    "Sağlıklı yaşam"
)

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
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

        Column(modifier = Modifier.fillMaxSize()) {
            // Üst ilerleme çubuğu
            Spacer(Modifier.height(52.dp))
            OnboardingProgressBar(currentStep = state.step, totalSteps = 4, accent = accent, theme = theme)
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
                    1 -> StepGenderBirth(state, viewModel, accent, theme)
                    2 -> StepBodyMetrics(state, viewModel, accent, theme)
                    3 -> StepGoal(state, viewModel, accent, theme)
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
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            Text("Devam Et", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ── Adım 1: Cinsiyet + Doğum Tarihi ──────────────────────────────────────────

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
            step    = "2 / 4",
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
            step     = "3 / 4",
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

        // BMI önizleme
        val h = state.heightText.toDoubleOrNull() ?: 0.0
        val w = state.weightText.toDoubleOrNull() ?: 0.0
        if (h > 0 && w > 0) {
            Spacer(Modifier.height(16.dp))
            val bmi      = w / ((h / 100) * (h / 100))
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
                    Text("BMI", color = bmiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("%.1f — %s".format(bmi, bmiLabel), color = bmiColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            step     = "4 / 4",
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

        // Tamamla butonu
        Button(
            onClick  = { vm.saveAndFinish() },
            enabled  = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Rounded.RocketLaunch, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hadi Başlayalım!", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Geri
        TextButton(
            onClick  = { vm.prevStep() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.ArrowBackIos, null, tint = theme.text2, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("Geri", color = theme.text2, fontSize = 13.sp)
        }

        Spacer(Modifier.height(48.dp))
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
            colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            Text(nextLabel, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIos, null, tint = theme.text2, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Geri", color = theme.text2, fontSize = 13.sp)
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
