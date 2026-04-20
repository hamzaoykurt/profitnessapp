package com.avonix.profitness.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.discover.Difficulty

/**
 * Paylaş sheet'i — kullanıcının aktif programını feed'e ekleme formu.
 * Title boş olamaz; diğerleri opsiyonel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgramSheet(
    onDismiss: () -> Unit,
    onConfirm: (
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

    var title        by rememberSaveable { mutableStateOf("") }
    var description  by rememberSaveable { mutableStateOf("") }
    var tagsText     by rememberSaveable { mutableStateOf("") }
    var weeksText    by rememberSaveable { mutableStateOf("") }
    var daysText     by rememberSaveable { mutableStateOf("") }
    var difficulty: Difficulty? by remember { mutableStateOf(null) }

    val canSubmit = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.bg1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("AKTİF PROGRAMI PAYLAŞ",
                color = theme.text0, fontSize = 18.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "Şu an aktif olan programın toplulukla paylaşılır. Snapshot alınır — paylaştıktan sonra programını değiştirsen de feed'deki etkilenmez.",
                color    = theme.text2.copy(0.7f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(16.dp))

            LabeledField("Başlık *", title, onChange = { title = it }, placeholder = "Örn. 3 Günlük Güç Protokolü")
            Spacer(Modifier.height(12.dp))
            LabeledField("Açıklama", description, onChange = { description = it },
                placeholder = "Kime, ne kadar sürede, hangi amaca?", multiline = true)
            Spacer(Modifier.height(12.dp))
            LabeledField("Etiketler (virgülle ayır)", tagsText, onChange = { tagsText = it },
                placeholder = "güç, hipertrofi, başlangıç")
            Spacer(Modifier.height(12.dp))

            // Difficulty chips
            Text("SEVİYE", color = theme.text2.copy(0.65f), fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DifficultyChip("Başlangıç", difficulty == Difficulty.BEGINNER)
                    { difficulty = if (difficulty == Difficulty.BEGINNER) null else Difficulty.BEGINNER }
                DifficultyChip("Orta",      difficulty == Difficulty.INTERMEDIATE)
                    { difficulty = if (difficulty == Difficulty.INTERMEDIATE) null else Difficulty.INTERMEDIATE }
                DifficultyChip("İleri",     difficulty == Difficulty.ADVANCED)
                    { difficulty = if (difficulty == Difficulty.ADVANCED) null else Difficulty.ADVANCED }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LabeledField("Süre (hafta)", weeksText, onChange = { weeksText = it.filter { c -> c.isDigit() } },
                        placeholder = "8", numeric = true)
                }
                Box(modifier = Modifier.weight(1f)) {
                    LabeledField("Gün/Hafta", daysText, onChange = { daysText = it.filter { c -> c.isDigit() } },
                        placeholder = "3", numeric = true)
                }
            }
            Spacer(Modifier.height(20.dp))

            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bg2.copy(0.7f))
                        .border(1.dp, theme.stroke.copy(0.4f), RoundedCornerShape(14.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("VAZGEÇ", color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
                    Text("PAYLAŞ",
                        color = if (canSubmit) Color.Black else theme.text2.copy(0.4f),
                        fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                }
            }
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
