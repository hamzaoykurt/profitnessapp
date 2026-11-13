package com.cosmibit.profitness.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow

data class ProfileData(
    val name  : String = "Kullanıcı",
    val avatar: String = "🏋️",
    val bio   : String = "",
    val goal  : String = ""
)

private data class AvatarCategory(val label: String, val emojis: List<String>)

private val AVATAR_CATEGORIES = listOf(
    AvatarCategory("Erkek Sporcular", listOf(
        "🏋️‍♂️","🤸‍♂️","🏃‍♂️","🚴‍♂️","⛹️‍♂️","🏊‍♂️","🤼‍♂️","🧗‍♂️","⛷️","🏄‍♂️","🥊","🤺"
    )),
    AvatarCategory("Kadın Sporcular", listOf(
        "🏋️‍♀️","🤸‍♀️","🏃‍♀️","🚴‍♀️","⛹️‍♀️","🏊‍♀️","🧘‍♀️","🤼‍♀️","🧗‍♀️","🏄‍♀️","🥋","🤺"
    )),
    AvatarCategory("Hayvanlar", listOf(
        "🦁","🐺","🦅","🐯","🦊","🐲","🦈","🦂","🐻","🦏","🐆","🦬"
    )),
    AvatarCategory("Semboller", listOf(
        "💪","🔥","⚡","🏆","💎","🎯","🌟","⚔️","🛡️","👑","🦾","☄️"
    ))
)

// UI label → DB value
private val GENDER_OPTIONS = listOf(
    "Erkek"    to "male",
    "Kadın"    to "female",
    "Belirtme" to "other"
)

private val PROFILE_GOAL_OPTIONS = listOf(
    "Kilo vermek",
    "Kas kazanmak",
    "Formda kalmak",
    "Yağ yakarken kas yapmak",
    "Kondisyon geliştirmek",
    "Sağlıklı yaşam"
)

private val PROFILE_SPORT_OPTIONS = listOf(
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

private val PROFILE_EXPERIENCE_OPTIONS = listOf(
    "Yeni başlıyorum",
    "Başlangıç",
    "Orta",
    "İleri"
)

private val PROFILE_WEEKLY_DAY_OPTIONS = listOf("3", "4", "5", "6")

private data class ProfileGoalParts(
    val goal           : String = "",
    val sportBranch    : String = "Fitness",
    val experienceLevel: String = "Başlangıç",
    val weeklyDays     : String = "4"
)

private fun AppThemeState.genderLabel(dbValue: String, fallback: String): String = when (dbValue) {
    "male" -> t("Erkek", "Male")
    "female" -> t("Kadın", "Female")
    "other" -> t("Belirtme", "Prefer not to say")
    else -> fallback
}

private fun AppThemeState.avatarCategoryLabel(label: String): String = when (label) {
    "Erkek Sporcular" -> t("Erkek Sporcular", "Male athletes")
    "Kadın Sporcular" -> t("Kadın Sporcular", "Female athletes")
    "Hayvanlar" -> t("Hayvanlar", "Animals")
    "Semboller" -> t("Semboller", "Symbols")
    else -> label
}

private fun AppThemeState.profileGoalLabel(value: String): String = when (value) {
    "Kilo vermek" -> t("Kilo vermek", "Lose weight")
    "Kas kazanmak" -> t("Kas kazanmak", "Build muscle")
    "Formda kalmak" -> t("Formda kalmak", "Stay fit")
    "Yağ yakarken kas yapmak" -> t("Yağ yakarken kas yapmak", "Recompose")
    "Kondisyon geliştirmek" -> t("Kondisyon geliştirmek", "Improve conditioning")
    "Sağlıklı yaşam" -> t("Sağlıklı yaşam", "Healthy lifestyle")
    "Fit olmak" -> t("Fit olmak", "Get fit")
    else -> value
}

private fun AppThemeState.profileSportLabel(value: String): String = when (value) {
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

private fun AppThemeState.profileExperienceLabel(value: String): String = when (value) {
    "Yeni başlıyorum" -> t("Yeni başlıyorum", "Brand new")
    "Başlangıç" -> t("Başlangıç", "Beginner")
    "Orta" -> t("Orta", "Intermediate")
    "İleri" -> t("İleri", "Advanced")
    else -> value
}

private fun parseProfileGoalParts(rawGoal: String): ProfileGoalParts {
    if (rawGoal.isBlank()) return ProfileGoalParts()

    var goal = ""
    var sportBranch = "Fitness"
    var experienceLevel = "Başlangıç"
    var weeklyDays = "4"

    rawGoal.split("•")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEachIndexed { index, segment ->
            when {
                segmentValue(segment, "Spor:", "Sport:") != null -> {
                    sportBranch = normalizeSportBranch(segmentValue(segment, "Spor:", "Sport:").orEmpty())
                }
                segmentValue(segment, "Seviye:", "Level:") != null -> {
                    experienceLevel = normalizeExperienceLevel(segmentValue(segment, "Seviye:", "Level:").orEmpty())
                }
                segment.startsWith("Haftada", ignoreCase = true) || segment.contains("days", ignoreCase = true) -> {
                    weeklyDays = segment.filter { it.isDigit() }
                        .takeIf { it in PROFILE_WEEKLY_DAY_OPTIONS }
                        ?: weeklyDays
                }
                index == 0 -> goal = segment
            }
        }

    return ProfileGoalParts(
        goal = goal,
        sportBranch = sportBranch,
        experienceLevel = experienceLevel,
        weeklyDays = weeklyDays
    )
}

private fun segmentValue(segment: String, vararg prefixes: String): String? {
    val trimmed = segment.trim()
    prefixes.forEach { prefix ->
        if (trimmed.startsWith(prefix, ignoreCase = true)) {
            return trimmed.drop(prefix.length).trim()
        }
    }
    return null
}

private fun normalizeSportBranch(value: String): String = when (value.trim().lowercase()) {
    "running" -> "Koşu"
    "cycling" -> "Bisiklet"
    "swimming" -> "Yüzme"
    "football", "soccer" -> "Futbol"
    "basketball" -> "Basketbol"
    "tennis" -> "Tenis"
    "boxing" -> "Boks"
    "walking" -> "Yürüyüş"
    else -> PROFILE_SPORT_OPTIONS.firstOrNull { it.equals(value.trim(), ignoreCase = true) } ?: "Fitness"
}

private fun normalizeExperienceLevel(value: String): String = when (value.trim().lowercase()) {
    "brand new" -> "Yeni başlıyorum"
    "beginner" -> "Başlangıç"
    "intermediate" -> "Orta"
    "advanced" -> "İleri"
    else -> PROFILE_EXPERIENCE_OPTIONS.firstOrNull { it.equals(value.trim(), ignoreCase = true) } ?: "Başlangıç"
}

private fun buildProfileGoal(goal: String, sportBranch: String, experienceLevel: String, weeklyDays: String): String =
    buildString {
        append(goal.trim().ifEmpty { "Fit olmak" })
        append(" • Spor: $sportBranch")
        append(" • Seviye: $experienceLevel")
        append(" • Haftada $weeklyDays gün")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack    : () -> Unit,
    viewModel : ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val goalParts = remember(state.fitnessGoal) { parseProfileGoalParts(state.fitnessGoal) }

    var name             by remember(state.displayName) { mutableStateOf(state.displayName) }
    var avatar           by remember(state.avatar)      { mutableStateOf(state.avatar) }
    var goal             by remember(goalParts.goal) { mutableStateOf(goalParts.goal) }
    var sportBranch      by remember(goalParts.sportBranch) { mutableStateOf(goalParts.sportBranch) }
    var experienceLevel  by remember(goalParts.experienceLevel) { mutableStateOf(goalParts.experienceLevel) }
    var weeklyDays       by remember(goalParts.weeklyDays) { mutableStateOf(goalParts.weeklyDays) }
    var heightText       by remember(state.heightCm)    { mutableStateOf(if (state.heightCm > 0) state.heightCm.toInt().toString() else "") }
    var weightText       by remember(state.weightKg)    { mutableStateOf(if (state.weightKg > 0) state.weightKg.toInt().toString() else "") }
    var gender           by remember(state.gender)      { mutableStateOf(state.gender.ifBlank { "male" }) }
    var avatarError      by remember { mutableStateOf<String?>(null) }
    // Sadece rakam tutulur, gösterimde VisualTransformation ile DD/MM/YYYY olur
    var birthDigits      by remember(state.birthDate)   {
        val digits = if (state.birthDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val p = state.birthDate.split("-")
            "${p[2]}${p[1]}${p[0]}"  // YYYYMMDD → DDMMYYYY
        } else state.birthDate.filter { it.isDigit() }
        mutableStateOf(digits)
    }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val result = readSafeProfilePhotoBytes(context, uri)
        val bytes = result.bytes
        if (bytes == null) {
            avatarError = result.errorMessage ?: theme.t("Profil fotoğrafı okunamadı.", "Profile photo could not be read.")
            return@rememberLauncherForActivityResult
        }
        avatarError = null
        viewModel.uploadPhoto(bytes)
    }

    fun saveAndExit() {
        // DDMMYYYY → YYYY-MM-DD (DB formatı)
        val dbBirthDate = if (birthDigits.length == 8) {
            "${birthDigits.substring(4)}-${birthDigits.substring(2, 4)}-${birthDigits.substring(0, 2)}"
        } else ""
        viewModel.updateProfile(
            displayName = name.trim().ifEmpty { state.displayName },
            avatar      = avatar,
            fitnessGoal = buildProfileGoal(goal, sportBranch, experienceLevel, weeklyDays),
            heightCm    = heightText.toDoubleOrNull() ?: state.heightCm,
            weightKg    = weightText.toDoubleOrNull() ?: state.weightKg,
            gender      = gender,
            birthDate   = dbBirthDate,
            onComplete  = onBack
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        SinemaBackdrop()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Kapak: foto (yoksa Sinema sahnesi) + avatar halkası ─────────
            item {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    if (state.avatar.startsWith("http")) {
                        Box(Modifier.fillMaxSize().photoFadeToBackdrop()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(state.avatar).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(Modifier.fillMaxSize().background(Sinema.BottomScrim))
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        AppBackButton(onClick = onBack, accent = accent, size = 48.dp)
                    }
                    // Avatar: vurgu halkası + glow
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).size(132.dp)) {
                        Box(
                            Modifier
                                .size(124.dp)
                                .align(Alignment.Center)
                                .sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(accent)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Sinema.surface(theme))
                                .clickable(role = androidx.compose.ui.semantics.Role.Button) { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val currentAvatar = state.avatar
                            if (state.isSaving) {
                                CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            } else if (currentAvatar.startsWith("http")) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(currentAvatar).crossfade(true).build(),
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(avatar, fontSize = 50.sp)
                            }
                        }
                        SinemaCapsuleIconButton(
                            icon = Icons.Rounded.CameraAlt,
                            description = theme.t("Fotoğraf Yükle", "Upload Photo"),
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            size = 44.dp
                        )
                    }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(theme.t("Profili düzenle", "Edit profile"), style = Sinema.ScreenTitle, color = Sinema.text(theme))
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SinemaCapsule(onClick = { showAvatarPicker = true }, height = 40.dp, contentPadding = PaddingValues(horizontal = 16.dp)) { fg ->
                            Text(theme.t("Emoji Seç", "Choose Emoji"), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        SinemaCapsule(onClick = { photoPickerLauncher.launch("image/*") }, height = 40.dp, contentPadding = PaddingValues(horizontal = 16.dp)) { fg ->
                            Text(theme.t("Fotoğraf Yükle", "Upload Photo"), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    avatarError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Sinema.text(theme), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }

            item { Spacer(Modifier.height(28.dp)) }

            // Form
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    SinemaGlassField(
                        value = name, onValueChange = { name = it },
                        label = theme.t("Ad soyad", "Full name"),
                        placeholder = theme.t("Adınızı girin", "Enter your name"),
                        keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words
                    )

                    Column {
                        Text(theme.t("Cinsiyet", "Gender").sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
                        Spacer(Modifier.height(8.dp))
                        SinemaSegmentRow(
                            options = GENDER_OPTIONS.map { it.second },
                            selected = gender,
                            label = { db -> theme.genderLabel(db, GENDER_OPTIONS.first { it.second == db }.first) },
                            onSelect = { gender = it },
                            height = 44.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SinemaGlassField(
                            value = heightText,
                            onValueChange = { v -> val d = v.filter { it.isDigit() }; if (d.length <= 3) heightText = d },
                            label = theme.t("Boy (cm)", "Height (cm)"), placeholder = "175", suffix = "cm",
                            keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)
                        )
                        SinemaGlassField(
                            value = weightText,
                            onValueChange = { v -> val d = v.filter { it.isDigit() }; if (d.length <= 3) weightText = d },
                            label = theme.t("Kilo (kg)", "Weight (kg)"), placeholder = "75", suffix = "kg",
                            keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)
                        )
                    }

                    // Doğum Tarihi — DD/MM/YYYY otomatik formatlı (VisualTransformation)
                    SinemaGlassField(
                        value = birthDigits,
                        onValueChange = { raw ->
                            val d = raw.filter { it.isDigit() }
                            if (d.length <= 8) birthDigits = d
                        },
                        label = theme.t("Doğum tarihi", "Birth date"), placeholder = "15/06/1995",
                        keyboardType = KeyboardType.Number, visualTransformation = DateVisualTransformation
                    )

                    // BMI önizleme — dev rakam
                    val h = heightText.toDoubleOrNull() ?: 0.0
                    val w = weightText.toDoubleOrNull() ?: 0.0
                    if (h > 0 && w > 0) {
                        val bmi = w / ((h / 100) * (h / 100))
                        val bmiLabel = when {
                            bmi < 18.5 -> theme.t("Zayıf", "Underweight")
                            bmi < 25.0 -> theme.t("Normal", "Normal")
                            bmi < 30.0 -> theme.t("Fazla Kilolu", "Overweight")
                            else       -> theme.t("Obez", "Obese")
                        }
                        Row(
                            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(24.dp)).padding(20.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            SinemaBigStat("%.1f".format(bmi), "BMI", Modifier.weight(1f), size = 44, color = accent)
                            Text(bmiLabel, color = Sinema.text(theme), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }

                    ProfileChoiceSection(
                        title = theme.t("Fitness hedefi", "Fitness goal"),
                        options = PROFILE_GOAL_OPTIONS,
                        selected = goal,
                        theme = theme,
                        labelFor = { theme.profileGoalLabel(it) },
                        onSelect = { goal = if (goal == it) "" else it }
                    )

                    SinemaGlassField(
                        value = goal, onValueChange = { goal = it },
                        label = theme.t("Veya kendin yaz", "Or write your own"),
                        placeholder = theme.t("Hedefinizi yazın...", "Write your goal..."),
                        keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences
                    )

                    ProfileChoiceSection(
                        title = theme.t("Spor dalı", "Sport"),
                        options = PROFILE_SPORT_OPTIONS,
                        selected = sportBranch,
                        theme = theme,
                        labelFor = { theme.profileSportLabel(it) },
                        onSelect = { sportBranch = it }
                    )

                    ProfileChoiceSection(
                        title = theme.t("Seviye", "Level"),
                        options = PROFILE_EXPERIENCE_OPTIONS,
                        selected = experienceLevel,
                        theme = theme,
                        labelFor = { theme.profileExperienceLabel(it) },
                        onSelect = { experienceLevel = it }
                    )

                    ProfileChoiceSection(
                        title = theme.t("Haftalık antrenman", "Weekly training"),
                        options = PROFILE_WEEKLY_DAY_OPTIONS,
                        selected = weeklyDays,
                        theme = theme,
                        labelFor = { theme.t("$it gün", "$it days") },
                        onSelect = { weeklyDays = it }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                SinemaGoOrIdle(
                    label = theme.t("Kaydet", "Save"),
                    icon = Icons.Rounded.Check,
                    enabled = !state.isSaving,
                    onClick = ::saveAndExit,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Avatar Picker Sheet
    if (showAvatarPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarPicker = false },
            containerColor   = Sinema.surface(theme),
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp, 8.dp, 24.dp, 60.dp)
            ) {
                Text(theme.t("Avatar seç", "Choose avatar"), style = Sinema.Title, color = Sinema.text(theme))
                Spacer(Modifier.height(20.dp))

                AVATAR_CATEGORIES.forEach { category ->
                    Text(
                        theme.avatarCategoryLabel(category.label).sinemaUpper(theme),
                        color = Sinema.textSecondary(theme), style = Sinema.Label,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    val rows = category.emojis.chunked(6)
                    rows.forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEmojis.forEach { emoji ->
                                SinemaCapsule(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    selected = emoji == avatar,
                                    onClick = { avatar = emoji; showAvatarPicker = false },
                                    height = 44.dp, shape = RoundedCornerShape(18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(emoji, fontSize = 24.sp) }
                            }
                            repeat(6 - rowEmojis.size) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileChoiceSection(
    title     : String,
    options   : List<String>,
    selected  : String,
    theme     : AppThemeState,
    labelFor  : (String) -> String = { it },
    onSelect  : (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { option ->
                    SinemaCapsule(
                        modifier = Modifier.weight(1f),
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        height = 46.dp,
                        horizontalArrangement = Arrangement.Center,
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { fg ->
                        Text(
                            text = labelFor(option), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// DD/MM/YYYY görsel dönüşümü — state sadece rakam tutar (ör. "12092003")
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
