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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.domain.challenges.MovementInput
import com.avonix.profitness.domain.model.ExerciseItem

private val multiCategoryColors = mapOf(
    "Göğüs"   to CardCoral,
    "Sırt"    to CardCyan,
    "Omuz"    to CardPurple,
    "Biceps"  to Amber,
    "Triceps" to CardGreen,
    "Bacak"   to CardCoral,
    "Core"    to CardCyan,
    "Kardiyo" to Amber
)

private fun multiCategoryColor(cat: String): Color =
    multiCategoryColors.entries.firstOrNull { cat.contains(it.key, ignoreCase = true) }?.value ?: Mist

/**
 * Multi-select + per-item config (sets/reps/duration) variant of [ExercisePickerSheet].
 * Used by event challenge creation (FAZ 7J) for movement_list mode.
 *
 * initialSelected preserves ordering & per-item suggestions on re-open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseMultiPickerSheet(
    exercises: List<ExerciseItem>,
    initialSelected: List<MovementInput> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<MovementInput>) -> Unit,
) {
    val theme = LocalAppTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expandedId       by remember { mutableStateOf<String?>(null) }

    // exerciseId -> MovementInput (sort order = insertion order in initialSelected, then new picks).
    val selected = remember {
        mutableStateMapOf<String, MovementInput>().apply {
            initialSelected.sortedBy { it.sortIndex }.forEach { put(it.exerciseId, it) }
        }
    }
    // Preserve selection order separately so checkbox re-toggle preserves it.
    val orderedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(initialSelected.sortedBy { it.sortIndex }.map { it.exerciseId })
        }
    }

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

    fun toggle(ex: ExerciseItem) {
        if (selected.containsKey(ex.id)) {
            selected.remove(ex.id)
            orderedIds.remove(ex.id)
        } else {
            orderedIds.add(ex.id)
            selected[ex.id] = MovementInput(
                exerciseId      = ex.id,
                sortIndex       = orderedIds.size - 1,
                suggestedSets   = ex.setsDefault.coerceAtLeast(1),
                suggestedReps   = ex.repsDefault.coerceAtLeast(1),
                suggestedDurSec = null
            )
            expandedId = ex.id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.bg1,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = theme.stroke) },
        tonalElevation   = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "HAREKETLERİ SEÇ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${selected.size} hareket seçildi · ${exercises.size} mevcut",
                        color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.Light
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null, tint = theme.text2, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search bar ────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg2)
                    .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = theme.text2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = theme.text0, fontWeight = FontWeight.Normal
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
                            Icons.Rounded.Clear, null, tint = theme.text2,
                            modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
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
                    MultiPickerCategoryChip(
                        label = "TÜMÜ",
                        color = Snow,
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(categories) { cat ->
                    MultiPickerCategoryChip(
                        label    = cat.uppercase(),
                        color    = multiCategoryColor(cat),
                        selected = selectedCategory == cat,
                        onClick  = { selectedCategory = if (selectedCategory == cat) null else cat }
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
                    .heightIn(max = 420.dp)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val isSelected = selected.containsKey(exercise.id)
                    val isExpanded = expandedId == exercise.id && isSelected
                    val accent = multiCategoryColor(exercise.category)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.08f) else theme.bg2)
                            .border(
                                1.dp,
                                if (isSelected) accent.copy(alpha = 0.4f) else theme.stroke,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggle(exercise) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox-like indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) accent
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) accent else theme.stroke,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check, null,
                                        tint = Surface0,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
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
                            if (isSelected) {
                                IconButton(
                                    onClick = {
                                        expandedId = if (isExpanded) null else exercise.id
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.Tune,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // ── Per-item config panel ────────────────────────────
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            val mi = selected[exercise.id]
                            if (mi != null) {
                                MovementConfigPanel(
                                    accent = accent,
                                    sets = mi.suggestedSets ?: 0,
                                    reps = mi.suggestedReps ?: 0,
                                    durSec = mi.suggestedDurSec ?: 0,
                                    onChange = { s, r, d ->
                                        selected[exercise.id] = mi.copy(
                                            suggestedSets   = s.takeIf { it > 0 },
                                            suggestedReps   = r.takeIf { it > 0 },
                                            suggestedDurSec = d.takeIf { it > 0 }
                                        )
                                    },
                                    onRemove = {
                                        selected.remove(exercise.id)
                                        orderedIds.remove(exercise.id)
                                        expandedId = null
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Bottom confirm bar ───────────────────────────────────────────
            HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.stroke)
                ) {
                    Text("İptal", color = theme.text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        // Rebuild list in orderedIds order; re-index sortIndex 0..N.
                        val out = orderedIds.mapIndexedNotNull { idx, exId ->
                            selected[exId]?.copy(sortIndex = idx)
                        }
                        onConfirm(out)
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1.4f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "EKLE (${selected.size})",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementConfigPanel(
    accent: Color,
    sets: Int,
    reps: Int,
    durSec: Int,
    onChange: (sets: Int, reps: Int, durSec: Int) -> Unit,
    onRemove: () -> Unit
) {
    val theme = LocalAppTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
    ) {
        HorizontalDivider(color = accent.copy(0.15f), thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.bg2)
                .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp)
        ) {
            MultiCounterField(
                label = "SET",
                value = sets,
                onDecrement = { onChange((sets - 1).coerceAtLeast(0), reps, durSec) },
                onIncrement = { onChange((sets + 1).coerceAtMost(10), reps, durSec) },
                accent = accent
            )
            HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)
            MultiCounterField(
                label = "TEKRAR",
                value = reps,
                onDecrement = { onChange(sets, (reps - 1).coerceAtLeast(0), durSec) },
                onIncrement = { onChange(sets, (reps + 1).coerceAtMost(100), durSec) },
                accent = accent
            )
            HorizontalDivider(color = theme.stroke, thickness = 0.5.dp)
            MultiCounterField(
                label = "SÜRE",
                value = durSec,
                step = 15,
                onDecrement = { onChange(sets, reps, (durSec - 15).coerceAtLeast(0)) },
                onIncrement = { onChange(sets, reps, (durSec + 15).coerceAtMost(3600)) },
                displayOverride = if (durSec == 0) "—" else "${durSec}s",
                accent = accent
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "0 = öneri yok (isteğe bağlı)",
            color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.DeleteOutline, null, tint = CardCoral, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Listeden Çıkar",
                color = CardCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun MultiCounterField(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accent: Color,
    step: Int = 1,
    displayOverride: String? = null
) {
    val theme = LocalAppTheme.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f))
                .border(1.dp, accent.copy(0.35f), CircleShape)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Remove, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(
            displayOverride ?: value.toString(),
            color = theme.text0,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 60.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(0.15f))
                .border(1.dp, accent.copy(0.35f), CircleShape)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MultiPickerCategoryChip(
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
