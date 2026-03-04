package com.avonix.profitness.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

data class ProfileData(
    val name  : String = "Hamza Oykurt",
    val avatar: String = "🏋️",
    val bio   : String = "",
    val goal  : String = ""
)

private val AVATAR_OPTIONS = listOf(
    "🏋️", "🤸", "🏃", "🚴", "🧘", "⚽", "🏊", "🥊",
    "🏆", "💪", "🦁", "🐺", "🔥", "⚡", "🌟", "💎",
    "🦅", "🐯", "🚀", "🎯"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: ProfileData,
    onSave : (ProfileData) -> Unit,
    onBack : () -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var name             by remember { mutableStateOf(profile.name) }
    var avatar           by remember { mutableStateOf(profile.avatar) }
    var bio              by remember { mutableStateOf(profile.bio) }
    var goal             by remember { mutableStateOf(profile.goal) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    fun saveAndExit() {
        onSave(ProfileData(name = name.trim().ifEmpty { profile.name }, avatar = avatar, bio = bio.trim(), goal = goal.trim()))
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        // Top accent wash
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(theme.bg1.copy(0.8f))
                        .border(1.dp, theme.stroke, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBackIosNew,
                        null,
                        tint     = theme.text0,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Text(
                    "PROFİLİ DÜZENLE",
                    color         = theme.text0,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                // Save check button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = ::saveAndExit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        tint     = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Avatar ────────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(124.dp)) {
                    // Accent ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(theme.bg2)
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatar, fontSize = 46.sp)
                    }
                    // Camera badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(theme.bg0)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .clickable { showAvatarPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            null,
                            tint     = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "Avatar Değiştir",
                    color      = accent,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable { showAvatarPicker = true }
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Form Fields ───────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProfileTextField(
                    value       = name,
                    onValue     = { name = it },
                    label       = "AD SOYAD",
                    placeholder = "Adınızı girin",
                    icon        = Icons.Rounded.Person,
                    accent      = accent,
                    theme       = theme,
                    imeAction   = ImeAction.Next
                )

                ProfileTextField(
                    value       = bio,
                    onValue     = { bio = it },
                    label       = "BİYOGRAFİ",
                    placeholder = "Kendinizi kısaca tanıtın...",
                    icon        = Icons.Rounded.Info,
                    accent      = accent,
                    theme       = theme,
                    imeAction   = ImeAction.Next,
                    maxLines    = 3
                )

                ProfileTextField(
                    value       = goal,
                    onValue     = { goal = it },
                    label       = "FİTNESS HEDEFİ",
                    placeholder = "Hedefiniz nedir? (ör. 10 kg vermek)",
                    icon        = Icons.Rounded.Flag,
                    accent      = accent,
                    theme       = theme,
                    imeAction   = ImeAction.Done
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                onClick  = ::saveAndExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor   = Color.Black
                )
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "KAYDET",
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 3.sp,
                    fontSize      = 14.sp
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Avatar Picker Sheet ───────────────────────────────────────────────────
    if (showAvatarPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarPicker = false },
            containerColor   = theme.bg1,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, 8.dp, 24.dp, 48.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(theme.text2.copy(0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "AVATAR SEÇ",
                    color         = accent,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns               = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.height(220.dp)
                ) {
                    items(AVATAR_OPTIONS) { emoji ->
                        val isSelected = emoji == avatar
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) accent.copy(0.18f) else theme.bg2)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else theme.stroke,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    avatar = emoji
                                    showAvatarPicker = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Text Field Component ──────────────────────────────────────────────────────

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
        Text(
            label,
            color         = theme.text2,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValue,
            placeholder   = { Text(placeholder, color = theme.text2, fontSize = 14.sp) },
            leadingIcon   = {
                Icon(icon, null, tint = accent.copy(0.7f), modifier = Modifier.size(20.dp))
            },
            modifier      = Modifier.fillMaxWidth(),
            maxLines      = maxLines,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = imeAction
            ),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accent,
                unfocusedBorderColor    = theme.stroke,
                focusedContainerColor   = theme.bg1,
                unfocusedContainerColor = theme.bg1,
                cursorColor             = accent,
                focusedTextColor        = theme.text0,
                unfocusedTextColor      = theme.text0
            )
        )
    }
}
