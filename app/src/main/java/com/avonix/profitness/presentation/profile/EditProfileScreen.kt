package com.avonix.profitness.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*

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

private val GENDER_OPTIONS = listOf("Erkek", "Kadın", "Belirtme")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack    : () -> Unit,
    viewModel : ProfileViewModel = hiltViewModel()
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val state  by viewModel.uiState.collectAsStateWithLifecycle()

    var name             by remember(state.displayName) { mutableStateOf(state.displayName) }
    var avatar           by remember(state.avatar)      { mutableStateOf(state.avatar) }
    var goal             by remember(state.fitnessGoal) { mutableStateOf(state.fitnessGoal) }
    var heightText       by remember(state.heightCm)    { mutableStateOf(if (state.heightCm > 0) state.heightCm.toInt().toString() else "") }
    var weightText       by remember(state.weightKg)    { mutableStateOf(if (state.weightKg > 0) state.weightKg.toInt().toString() else "") }
    var gender           by remember(state.gender)      { mutableStateOf(state.gender.ifBlank { "Erkek" }) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    fun saveAndExit() {
        viewModel.updateProfile(
            displayName = name.trim().ifEmpty { state.displayName },
            avatar      = avatar,
            fitnessGoal = goal.trim(),
            heightCm    = heightText.toDoubleOrNull() ?: state.heightCm,
            weightKg    = weightText.toDoubleOrNull() ?: state.weightKg,
            gender      = gender
        )
        onBack()
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
                    Box(
                        modifier = Modifier
                            .size(42.dp).clip(CircleShape)
                            .background(theme.bg1.copy(0.8f))
                            .border(1.dp, theme.stroke, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBackIosNew, null, tint = theme.text0, modifier = Modifier.size(17.dp))
                    }
                    Text("PROFİLİ DÜZENLE", color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Box(
                        modifier = Modifier
                            .size(42.dp).clip(CircleShape)
                            .background(accent)
                            .clickable(onClick = ::saveAndExit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
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
                        ) { Text(avatar, fontSize = 46.sp) }
                        Box(
                            modifier = Modifier
                                .size(36.dp).align(Alignment.BottomEnd).clip(CircleShape)
                                .background(theme.bg0).padding(3.dp).clip(CircleShape)
                                .background(accent).clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(15.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Avatar Değiştir", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showAvatarPicker = true })
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
                        value = name, onValue = { name = it }, label = "AD SOYAD",
                        placeholder = "Adınızı girin", icon = Icons.Rounded.Person,
                        accent = accent, theme = theme, imeAction = ImeAction.Next
                    )

                    // Gender
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CİNSİYET", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.bg1)
                                .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GENDER_OPTIONS.forEach { option ->
                                val selected = gender == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) accent else Color.Transparent)
                                        .clickable { gender = option }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        option,
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
                            Text("BOY (cm)", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            OutlinedTextField(
                                value = heightText, onValueChange = { if (it.length <= 3) heightText = it },
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
                            Text("KİLO (kg)", color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            OutlinedTextField(
                                value = weightText, onValueChange = { if (it.length <= 3) weightText = it },
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

                    // BMI preview
                    val h = heightText.toDoubleOrNull() ?: 0.0
                    val w = weightText.toDoubleOrNull() ?: 0.0
                    if (h > 0 && w > 0) {
                        val bmi = w / ((h / 100) * (h / 100))
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
                    ProfileTextField(
                        value = goal, onValue = { goal = it }, label = "FİTNESS HEDEFİ",
                        placeholder = "Hedefiniz nedir? (ör. 10 kg vermek)",
                        icon = Icons.Rounded.Flag, accent = accent, theme = theme,
                        imeAction = ImeAction.Done
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
                    Text("KAYDET", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 14.sp)
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
                Text("AVATAR SEÇ", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Spacer(Modifier.height(20.dp))

                AVATAR_CATEGORIES.forEach { category ->
                    Text(
                        category.label,
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
    maxLines   : Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = value, onValueChange = onValue,
            placeholder = { Text(placeholder, color = theme.text2, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = theme.stroke,
                focusedContainerColor = theme.bg1, unfocusedContainerColor = theme.bg1,
                cursorColor = accent, focusedTextColor = theme.text0, unfocusedTextColor = theme.text0
            )
        )
    }
}
