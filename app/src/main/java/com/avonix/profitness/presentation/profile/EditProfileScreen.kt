package com.avonix.profitness.presentation.profile

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
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.AppBackButton

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

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(0.14f), Color.Transparent)))
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    AppBackButton(onClick = onBack, accent = accent, size = 48.dp)
                    Text(theme.t("PROFİLİ DÜZENLE", "EDIT PROFILE"), color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.size(48.dp))
                }
            }

            item { Spacer(Modifier.height(28.dp)) }

            // Avatar
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(124.dp)) {
                        Box(modifier = Modifier.size(120.dp).align(Alignment.Center).clip(CircleShape).background(accent))
                        Box(
                            modifier = Modifier
                                .size(110.dp).align(Alignment.Center).clip(CircleShape)
                                .background(theme.bg2)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val currentAvatar = state.avatar
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    color = accent,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else if (currentAvatar.startsWith("http")) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(currentAvatar).crossfade(true).build(),
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(avatar, fontSize = 46.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp).align(Alignment.BottomEnd).clip(CircleShape)
                                .background(theme.bg0).padding(3.dp).clip(CircleShape)
                                .background(accent).clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(15.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(theme.t("Emoji Seç", "Choose Emoji"), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showAvatarPicker = true })
                        Text("•", color = theme.text2, fontSize = 13.sp)
                        Text(theme.t("Fotoğraf Yükle", "Upload Photo"), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") })
                    }
                    avatarError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Form fields
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Name
                    ProfileTextField(
                        value = name, onValue = { name = it }, label = theme.t("AD SOYAD", "FULL NAME"),
                        placeholder = theme.t("Adınızı girin", "Enter your name"), icon = Icons.Rounded.Person,
                        accent = accent, theme = theme, imeAction = ImeAction.Next
                    )

                    // Gender
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(theme.t("CİNSİYET", "GENDER"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                                val selected = gender == dbValue
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) accent else Color.Transparent)
                                        .clickable { gender = dbValue }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        theme.genderLabel(dbValue, label),
                                        color      = if (selected) Color.Black else theme.text2,
                                        fontSize   = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Height & Weight — side by side
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(theme.t("BOY (cm)", "HEIGHT (cm)"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            OutlinedTextField(
                                value = heightText, onValueChange = { v -> val d = v.filter { it.isDigit() }; if (d.length <= 3) heightText = d },
                                placeholder = { Text("175", color = theme.text2, fontSize = 14.sp) },
                                leadingIcon = { Text("📏", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent, unfocusedBorderColor = theme.stroke,
                                    focusedContainerColor = theme.bg1, unfocusedContainerColor = theme.bg1,
                                    cursorColor = accent, focusedTextColor = theme.text0, unfocusedTextColor = theme.text0
                                )
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(theme.t("KİLO (kg)", "WEIGHT (kg)"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            OutlinedTextField(
                                value = weightText, onValueChange = { v -> val d = v.filter { it.isDigit() }; if (d.length <= 3) weightText = d },
                                placeholder = { Text("75", color = theme.text2, fontSize = 14.sp) },
                                leadingIcon = { Text("⚖️", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent, unfocusedBorderColor = theme.stroke,
                                    focusedContainerColor = theme.bg1, unfocusedContainerColor = theme.bg1,
                                    cursorColor = accent, focusedTextColor = theme.text0, unfocusedTextColor = theme.text0
                                )
                            )
                        }
                    }

                    // Doğum Tarihi — DD/MM/YYYY otomatik formatlı (VisualTransformation)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(theme.t("DOĞUM TARİHİ", "BIRTH DATE"), color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        OutlinedTextField(
                            value = birthDigits,
                            onValueChange = { raw ->
                                val d = raw.filter { it.isDigit() }
                                if (d.length <= 8) birthDigits = d
                            },
                            placeholder = { Text("15/06/1995", color = theme.text2, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, tint = accent, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = DateVisualTransformation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent, unfocusedBorderColor = theme.stroke,
                                focusedContainerColor = theme.bg1, unfocusedContainerColor = theme.bg1,
                                cursorColor = accent, focusedTextColor = theme.text0, unfocusedTextColor = theme.text0
                            )
                        )
                    }

                    // BMI preview
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
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("BMI", color = bmiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    "%.1f — %s".format(bmi, bmiLabel),
                                    color = bmiColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Fitness Goal
                    ProfileChoiceSection(
                        title = theme.t("FİTNESS HEDEFİ", "FITNESS GOAL"),
                        options = PROFILE_GOAL_OPTIONS,
                        selected = goal,
                        accent = accent,
                        theme = theme,
                        labelFor = { theme.profileGoalLabel(it) },
                        onSelect = { goal = if (goal == it) "" else it }
                    )

                    ProfileTextField(
                        value = goal,
                        onValue = { goal = it },
                        label = theme.t("VEYA KENDİN YAZ", "OR WRITE YOUR OWN"),
                        placeholder = theme.t("Hedefinizi yazın...", "Write your goal..."),
                        icon = Icons.Rounded.Flag,
                        accent = accent,
                        theme = theme,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences
                    )

                    ProfileChoiceSection(
                        title = theme.t("SPOR DALI", "SPORT"),
                        options = PROFILE_SPORT_OPTIONS,
                        selected = sportBranch,
                        accent = accent,
                        theme = theme,
                        labelFor = { theme.profileSportLabel(it) },
                        onSelect = { sportBranch = it }
                    )

                    ProfileChoiceSection(
                        title = theme.t("SEVİYE", "LEVEL"),
                        options = PROFILE_EXPERIENCE_OPTIONS,
                        selected = experienceLevel,
                        accent = accent,
                        theme = theme,
                        itemHeight = 42.dp,
                        labelFor = { theme.profileExperienceLabel(it) },
                        onSelect = { experienceLevel = it }
                    )

                    ProfileChoiceSection(
                        title = theme.t("HAFTALIK ANTRENMAN", "WEEKLY TRAINING"),
                        options = PROFILE_WEEKLY_DAY_OPTIONS,
                        selected = weeklyDays,
                        accent = accent,
                        theme = theme,
                        labelFor = { theme.t("$it gün", "$it days") },
                        onSelect = { weeklyDays = it }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                Button(
                    onClick  = ::saveAndExit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(theme.t("KAYDET", "SAVE"), fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 14.sp)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Avatar Picker Sheet
    if (showAvatarPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarPicker = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp, 8.dp, 24.dp, 60.dp)
            ) {
                Box(
                    Modifier.width(40.dp).height(4.dp)
                        .clip(CircleShape).background(theme.text2.copy(0.4f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))
                Text(theme.t("AVATAR SEÇ", "CHOOSE AVATAR"), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Spacer(Modifier.height(20.dp))

                AVATAR_CATEGORIES.forEach { category ->
                    Text(
                        theme.avatarCategoryLabel(category.label),
                        color = theme.text2, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    val rows = category.emojis.chunked(6)
                    rows.forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEmojis.forEach { emoji ->
                                val isSelected = emoji == avatar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) accent.copy(0.18f) else theme.bg2)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) accent else theme.stroke,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { avatar = emoji; showAvatarPicker = false },
                                    contentAlignment = Alignment.Center
                                ) { Text(emoji, fontSize = 26.sp) }
                            }
                            // Fill remaining slots
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
private fun ProfileTextField(
    value      : String,
    onValue    : (String) -> Unit,
    label      : String,
    placeholder: String,
    icon       : ImageVector,
    accent     : Color,
    theme      : AppThemeState,
    imeAction  : ImeAction = ImeAction.Next,
    maxLines   : Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = value, onValueChange = onValue,
            placeholder = { Text(placeholder, color = theme.text2, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = theme.stroke,
                focusedContainerColor = theme.bg1, unfocusedContainerColor = theme.bg1,
                cursorColor = accent, focusedTextColor = theme.text0, unfocusedTextColor = theme.text0
            )
        )
    }
}

@Composable
private fun ProfileChoiceSection(
    title     : String,
    options   : List<String>,
    selected  : String,
    accent    : Color,
    theme     : AppThemeState,
    itemHeight: androidx.compose.ui.unit.Dp = 46.dp,
    labelFor  : (String) -> String = { it },
    onSelect  : (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        options.chunked(2).forEachIndexed { index, row ->
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
                            .clickable { onSelect(option) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelFor(option),
                            color = if (isSelected) accent else theme.text1,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            if (index != options.chunked(2).lastIndex) {
                Spacer(Modifier.height(2.dp))
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
