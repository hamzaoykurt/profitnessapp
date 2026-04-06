package com.avonix.profitness.presentation.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.domain.model.ExerciseItem

private val categoryColors = mapOf(
    "Göğüs"     to CardCoral,
    "Sırt"      to CardCyan,
    "Omuz"      to CardPurple,
    "Biceps"    to Amber,
    "Triceps"   to CardGreen,
    "Bacak"     to CardCoral,
    "Core"      to CardCyan,
    "Kardiyo"   to Amber
)

private fun categoryColor(cat: String): Color =
    categoryColors.entries.firstOrNull { cat.contains(it.key, ignoreCase = true) }?.value ?: Mist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<ExerciseItem>,
    onDismiss: () -> Unit,
    onConfirm: (exerciseId: String, exerciseName: String, targetMuscle: String, sets: Int, reps: Int, restSeconds: Int) -> Unit,
    onRequestExercise: ((name: String, targetMuscle: String, notes: String) -> Unit)? = null,
    requestLoading: Boolean = false
) {
    val theme = LocalAppTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery       by remember { mutableStateOf("") }
    var selectedCategory  by remember { mutableStateOf<String?>(null) }
    var configExercise    by remember { mutableStateOf<ExerciseItem?>(null) }
    var sets              by remember { mutableStateOf(3) }
    var reps              by remember { mutableStateOf(10) }
    var restSeconds       by remember { mutableStateOf(90) }
    var showRequestDialog by remember { mutableStateOf(false) }

    val categories = remember(exercises) {
        exercises.map { it.category }.distinct().sorted()
    }

    val filtered = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { ex ->
            val matchSearch = searchQuery.isEmpty() ||
                ex.name.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == null || ex.category == selectedCategory
            matchSearch && matchCat
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.bg1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.stroke) },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "HAREKET SEÇ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${exercises.size} hareket mevcut",
                        color = theme.text2,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = theme.text2, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search bar ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        tint = theme.text2,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = theme.text0,
                            fontWeight = FontWeight.Normal
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Hareket ara...",
                                    color = theme.text2,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Clear,
                            null,
                            tint = theme.text2,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Category chips ────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PickerCategoryChip(
                        label = "TÜMÜ",
                        color = Snow,
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(categories) { cat ->
                    PickerCategoryChip(
                        label = cat.uppercase(),
                        color = categoryColor(cat),
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Exercise list ─────────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 480.dp)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val isSelected = configExercise?.id == exercise.id
                    val accent = categoryColor(exercise.category)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.08f) else theme.bg2
                            )
                            .border(
                                1.dp,
                                if (isSelected) accent.copy(alpha = 0.4f) else theme.stroke,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        // Exercise row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        configExercise = null
                                    } else {
                                        configExercise = exercise
                                        sets = exercise.setsDefault.coerceAtLeast(1)
                                        reps = exercise.repsDefault.coerceAtLeast(1)
                                        restSeconds = 90
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.FitnessCenter,
                                    null,
                                    tint = accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    exercise.name,
                                    color = theme.text0,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    exercise.targetMuscle,
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                "${exercise.setsDefault}×${exercise.repsDefault}",
                                color = theme.text2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isSelected) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null,
                                tint = if (isSelected) accent else theme.text2,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Config panel (animated)
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            ) {
                                HorizontalDivider(color = accent.copy(0.15f), thickness = 1.dp)
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CounterField(
                                        label = "SET",
                                        value = sets,
                                        onDecrement = { if (sets > 1) sets-- },
                                        onIncrement = { if (sets < 10) sets++ },
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CounterField(
                                        label = "TEKrar",
                                        value = reps,
                                        onDecrement = { if (reps > 1) reps-- },
                                        onIncrement = { if (reps < 100) reps++ },
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CounterField(
                                        label = "DİNLENME",
                                        value = restSeconds,
                                        step = 15,
                                        onDecrement = { if (restSeconds > 15) restSeconds -= 15 },
                                        onIncrement = { if (restSeconds < 300) restSeconds += 15 },
                                        displayOverride = "${restSeconds}s",
                                        accent = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        onConfirm(
                                            exercise.id,
                                            exercise.name,
                                            exercise.targetMuscle,
                                            sets,
                                            reps,
                                            restSeconds
                                        )
                                        configExercise = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "PROGRAMA EKLE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Talep Et footer ───────────────────────────────────────────
                if (onRequestExercise != null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.bg2)
                                .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                                .clickable { showRequestDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AddCircleOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Aradığın hareketi bulamadın mı?",
                                        color = theme.text0,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Eklenmesi için talep gönder",
                                        color = theme.text2,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    null,
                                    tint = theme.text2,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    // ── Request Dialog ────────────────────────────────────────────────────────
    if (showRequestDialog && onRequestExercise != null) {
        ExerciseRequestDialog(
            requestLoading = requestLoading,
            onDismiss = { showRequestDialog = false },
            onSubmit = { name, muscle, notes ->
                onRequestExercise(name, muscle, notes)
                showRequestDialog = false
            }
        )
    }
}

@Composable
private fun ExerciseRequestDialog(
    requestLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, targetMuscle: String, notes: String) -> Unit
) {
    val theme = LocalAppTheme.current
    var name   by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var notes  by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg1)
                .padding(20.dp)
        ) {
            Text(
                "YENİ HAREKET TALEBİ",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "İstediğin hareket eklensin",
                color = theme.text2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(16.dp))

            RequestInputField(
                label = "Hareket adı *",
                value = name,
                onValueChange = { name = it },
                placeholder = "ör. Cable Fly, Hack Squat",
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
            RequestInputField(
                label = "Kas grubu",
                value = muscle,
                onValueChange = { muscle = it },
                placeholder = "ör. Göğüs, Bacak",
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
            RequestInputField(
                label = "Notlar",
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Eklemek istediğin bilgiler...",
                theme = theme
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.stroke)
                ) {
                    Text("İptal", color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) onSubmit(name.trim(), muscle.trim(), notes.trim())
                    },
                    enabled = name.isNotBlank() && !requestLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (requestLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Snow
                        )
                    } else {
                        Text("Gönder", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

// ── Request Input Field ───────────────────────────────────────────────────────

@Composable
private fun RequestInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    theme: AppThemeState
) {
    Column {
        Text(
            label,
            color = theme.text2,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.bg2)
                .border(1.dp, theme.stroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = theme.text0,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = theme.text2, fontSize = 13.sp, fontWeight = FontWeight.Light)
                    }
                    inner()
                }
            )
        }
    }
}

// ── Counter Field ─────────────────────────────────────────────────────────────

@Composable
private fun CounterField(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    step: Int = 1,
    displayOverride: String? = null
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.bg3)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = theme.text2,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.15f))
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Remove, null, tint = accent, modifier = Modifier.size(12.dp))
            }
            Text(
                displayOverride ?: value.toString(),
                color = theme.text0,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.15f))
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Category Chip (picker variant) ────────────────────────────────────────────

@Composable
private fun PickerCategoryChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.06f))
            .border(1.dp, if (selected) color else color.copy(0.2f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Surface0 else color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
