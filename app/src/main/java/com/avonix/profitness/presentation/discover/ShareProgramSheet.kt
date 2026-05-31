package com.avonix.profitness.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.t
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.discover.Difficulty
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.presentation.components.glassCard

/**
 * Paylaş sheet'i — iki adımlı:
 * 1) Kullanıcı kendi programlarından birini seçer (preselectedProgramId varsa direkt 2. adıma geçer)
 * 2) Başlık / açıklama / etiket / zorluk / süre meta'sını doldurur ve paylaşır
 *
 * Title boş olamaz; diğerleri opsiyonel. programs boşsa boş-durum gösterilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgramSheet(
    programs: List<Program>,
    alreadySharedProgramIds: Set<String> = emptySet(),
    alreadySharedProgramHashes: Set<String> = emptySet(),
    preselectedProgramId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        programId: String,
        title: String,
        description: String?,
        tags: List<String>,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?
    ) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Preselected varsa direkt meta adımına geç; yoksa seçici göster.
    var selectedProgramId by rememberSaveable { mutableStateOf(preselectedProgramId) }
    val selectedProgram = programs.firstOrNull { it.id == selectedProgramId }

    // Meta form state'leri — seçilen program değişince title varsayılanı yenilenir.
    var title        by rememberSaveable(selectedProgramId) {
        mutableStateOf(selectedProgram?.name.orEmpty())
    }
    var description  by rememberSaveable(selectedProgramId) { mutableStateOf("") }
    var tagsText     by rememberSaveable(selectedProgramId) { mutableStateOf("") }
    var weeksText    by rememberSaveable(selectedProgramId) { mutableStateOf("") }
    var daysText     by rememberSaveable(selectedProgramId) {
        mutableStateOf(selectedProgram?.days?.count { !it.isRestDay }?.toString().orEmpty())
    }
    var difficulty: Difficulty? by remember(selectedProgramId) { mutableStateOf(null) }

    val selectedAlreadyShared = selectedProgram
        ?.isAlreadyShared(alreadySharedProgramIds, alreadySharedProgramHashes) == true
    val canSubmit = selectedProgram != null && !selectedAlreadyShared && title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.bg1
    ) {
        if (selectedProgram == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                // ── 1. Adım: Program seç ─────────────────────────────────────
                Text(theme.t("PAYLAŞILACAK PROGRAMI SEÇ", "SELECT PROGRAM TO SHARE"),
                    color = theme.text0, fontSize = 18.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = theme.t(
                        "Kendi programlarından birini seç, ardından başlık/açıklama ekleyerek topluluğa paylaş. Paylaşım snapshot olarak sabit kalır; programı sonra düzenlesen bile bu paylaşım değişmez.",
                        "Choose one of your programs, then add a title/description and share it with the community. The share stays fixed as a snapshot, even if you edit the program later."
                    ),
                    color    = theme.text2.copy(0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(16.dp))

                if (programs.isEmpty()) {
                    EmptyProgramsNotice()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(programs, key = { it.id }) { p ->
                        ProgramPickerRow(
                            program = p,
                            alreadyShared = p.isAlreadyShared(
                                alreadySharedProgramIds,
                                alreadySharedProgramHashes
                            ),
                            onClick = { selectedProgramId = p.id }
                        )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.bg2.copy(0.7f))
                            .border(1.dp, theme.stroke.copy(0.4f), RoundedCornerShape(14.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(theme.t("VAZGEÇ", "CANCEL"), color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                // ── 2. Adım: Meta form ───────────────────────────────────────
                Text(theme.t("PROGRAMI PAYLAŞ", "SHARE PROGRAM"),
                    color = theme.text0, fontSize = 18.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = selectedProgram.name,
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // Preselected değilse kullanıcı geri dönüp başka program seçebilir
                    if (preselectedProgramId == null) {
                        Text(
                            text = theme.t("Değiştir", "Change"),
                            color = theme.text2.copy(0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedProgramId = null }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (selectedAlreadyShared) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = theme.t("Bu program zaten toplulukta yayında.", "This program is already live in the community."),
                        color = accent.copy(0.82f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))

                LabeledField(theme.t("Başlık *", "Title *"), title, onChange = { title = it }, placeholder = theme.t("Örn. 3 Günlük Güç Protokolü", "e.g. 3-Day Strength Protocol"))
                Spacer(Modifier.height(12.dp))
                LabeledField(theme.t("Açıklama", "Description"), description, onChange = { description = it },
                    placeholder = theme.t("Kime, ne kadar sürede, hangi amaca?", "For whom, over how long, and for what goal?"), multiline = true)
                Spacer(Modifier.height(12.dp))
                LabeledField(theme.t("Etiketler (virgülle ayır)", "Tags (comma-separated)"), tagsText, onChange = { tagsText = it },
                    placeholder = theme.t("güç, hipertrofi, başlangıç", "strength, hypertrophy, beginner"))
                Spacer(Modifier.height(12.dp))

                Text(theme.t("SEVİYE", "LEVEL"), color = theme.text2.copy(0.65f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyChip(theme.t("Başlangıç", "Beginner"), difficulty == Difficulty.BEGINNER)
                        { difficulty = if (difficulty == Difficulty.BEGINNER) null else Difficulty.BEGINNER }
                    DifficultyChip(theme.t("Orta", "Intermediate"),      difficulty == Difficulty.INTERMEDIATE)
                        { difficulty = if (difficulty == Difficulty.INTERMEDIATE) null else Difficulty.INTERMEDIATE }
                    DifficultyChip(theme.t("İleri", "Advanced"),     difficulty == Difficulty.ADVANCED)
                        { difficulty = if (difficulty == Difficulty.ADVANCED) null else Difficulty.ADVANCED }
                }
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        LabeledField(theme.t("Süre (hafta)", "Duration (weeks)"), weeksText, onChange = { weeksText = it.filter { c -> c.isDigit() } },
                            placeholder = "8", numeric = true)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LabeledField(theme.t("Gün/Hafta", "Days/Week"), daysText, onChange = { daysText = it.filter { c -> c.isDigit() } },
                            placeholder = "3", numeric = true)
                    }
                }
                Spacer(Modifier.height(20.dp))

                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .glassCard(theme.text2, theme, RoundedCornerShape(14.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(theme.t("VAZGEÇ", "CANCEL"), color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (canSubmit) Brush.linearGradient(listOf(accent, accent.copy(0.75f)))
                                else           Brush.linearGradient(listOf(theme.bg2.copy(0.4f), theme.bg2.copy(0.4f)))
                            )
                            .clickable(enabled = canSubmit) {
                                val tagList = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(8)
                                onConfirm(
                                    selectedProgram.id,
                                    title.trim(),
                                    description.trim().takeIf { it.isNotBlank() },
                                    tagList,
                                    difficulty?.raw,
                                    weeksText.toIntOrNull(),
                                    daysText.toIntOrNull()
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(theme.t("PAYLAŞ", "SHARE"),
                            color = if (canSubmit) Color.Black else theme.text2.copy(0.4f),
                            fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    }
                }
            }
        }
    }
}

private fun Program.isAlreadyShared(
    sharedProgramIds: Set<String>,
    sharedProgramHashes: Set<String>
): Boolean = id in sharedProgramIds || contentHash?.let { it in sharedProgramHashes } == true

@Composable
private fun ProgramPickerRow(
    program: Program,
    alreadyShared: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    val workoutDays = program.days.count { !it.isRestDay }
    val totalEx = program.days.sumOf { it.exercises.size }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(shape)
            .background(theme.bg2.copy(if (alreadyShared) 0.25f else 0.45f))
            .border(
                1.dp,
                when {
                    alreadyShared -> accent.copy(0.28f)
                    program.isActive -> accent.copy(0.45f)
                    else -> theme.stroke.copy(0.4f)
                },
                shape
            )
            .clickable(enabled = !alreadyShared) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = program.name,
                    color = if (alreadyShared) theme.text2.copy(0.58f) else theme.text0,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (program.isActive) {
                    Spacer(Modifier.width(6.dp))
                    ProgramStatusBadge(theme.t("AKTİF", "ACTIVE"), accent)
                }
                if (alreadyShared) {
                    Spacer(Modifier.width(6.dp))
                    ProgramStatusBadge(theme.t("YAYINDA", "LIVE"), accent)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = theme.t(
                    "$workoutDays antrenman günü · $totalEx egzersiz",
                    "$workoutDays workout days · $totalEx exercises"
                ),
                color = theme.text2.copy(0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.CheckCircle,
            null,
            tint = if (alreadyShared) theme.text2.copy(0.28f) else accent.copy(0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProgramStatusBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun EmptyProgramsNotice() {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg2.copy(0.4f))
            .border(1.dp, theme.stroke.copy(0.35f), RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(theme.t("Paylaşılabilir programın yok", "You have no programs to share"),
                color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = theme.t("Önce Plan sekmesinde bir program oluştur.", "Create a program in the Plan tab first."),
                color = theme.text2.copy(0.7f), fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    multiline: Boolean = false,
    numeric: Boolean = false
) {
    val theme = LocalAppTheme.current
    Column {
        Text(label.uppercase(), color = theme.text2.copy(0.65f), fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = theme.text2.copy(0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = !multiline,
            minLines = if (multiline) 2 else 1,
            maxLines = if (multiline) 4 else 1,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(0.70f),
                unfocusedBorderColor = theme.stroke.copy(0.70f),
                focusedContainerColor = theme.bg2.copy(0.35f),
                unfocusedContainerColor = theme.bg2.copy(0.28f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = theme.text0,
                unfocusedTextColor = theme.text0
            ),
            keyboardOptions = if (numeric)
                KeyboardOptions(keyboardType = KeyboardType.Number)
            else KeyboardOptions.Default
        )
    }
}

@Composable
private fun DifficultyChip(
    label: String, selected: Boolean, onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accent.copy(0.22f) else theme.bg2.copy(0.5f))
            .border(1.dp, if (selected) accent.copy(0.45f) else theme.stroke.copy(0.35f), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) accent else theme.text2.copy(0.75f),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
