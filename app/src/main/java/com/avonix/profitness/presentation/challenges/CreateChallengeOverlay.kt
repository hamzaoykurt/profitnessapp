package com.avonix.profitness.presentation.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.avonix.profitness.core.theme.LocalAppTheme
import com.avonix.profitness.core.theme.PageAccentBloom
import com.avonix.profitness.core.theme.strings
import com.avonix.profitness.core.theme.bg0
import com.avonix.profitness.core.theme.bg1
import com.avonix.profitness.core.theme.bg2
import com.avonix.profitness.core.theme.stroke
import com.avonix.profitness.core.theme.text0
import com.avonix.profitness.core.theme.text1
import com.avonix.profitness.core.theme.text2
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.CreateEventChallengeRequest
import com.avonix.profitness.domain.challenges.EventMode
import com.avonix.profitness.domain.challenges.MovementInput
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.presentation.program.ExerciseMultiPickerSheet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class CreateFormKind { Metric, Event }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeOverlay(
    inFlight: Boolean,
    error: String?,
    exercises: List<ExerciseItem>,
    onDismiss: () -> Unit,
    /** Metric challenge submit — legacy form. */
    onSubmit: (
        title: String,
        description: String,
        targetType: ChallengeTargetType,
        targetValue: Long,
        startDateIso: String,
        endDateIso: String,
        visibility: ChallengeVisibility,
        password: String?
    ) -> Unit,
    /** Event challenge submit — new. */
    onSubmitEvent: (CreateEventChallengeRequest) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary

    // ── Top-level mode ────────────────────────────────────────────────────
    var kind by rememberSaveable { mutableStateOf(CreateFormKind.Metric) }

    // ── Shared ────────────────────────────────────────────────────────────
    var title       by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var visibility  by rememberSaveable { mutableStateOf(ChallengeVisibility.Public) }
    var password    by rememberSaveable { mutableStateOf("") }

    // ── Metric-only ───────────────────────────────────────────────────────
    var metricTargetType by rememberSaveable { mutableStateOf(ChallengeTargetType.TotalWorkouts) }
    var metricTargetValue by rememberSaveable { mutableStateOf("10") }
    var days by rememberSaveable { mutableStateOf("14") }

    // ── Event-only ────────────────────────────────────────────────────────
    var eventMode by rememberSaveable { mutableStateOf(EventMode.Physical) }
    var eventDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var eventTimeIso by rememberSaveable { mutableStateOf<String?>(null) }
    var eventLocation by rememberSaveable { mutableStateOf("") }
    var eventOnlineUrl by rememberSaveable { mutableStateOf("") }
    var eventTargetEnabled by rememberSaveable { mutableStateOf(false) }
    var eventTargetType by rememberSaveable { mutableStateOf(ChallengeTargetType.TotalDistanceM) }
    var eventTargetValue by rememberSaveable { mutableStateOf("100") }
    val selectedMovements = remember { mutableStateListOf<MovementInput>() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMultiPicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val metricEndDate = remember(days) {
        val d = days.toIntOrNull()?.coerceIn(1, 365) ?: 14
        today.plusDays(d.toLong())
    }
    val timezone = remember { ZoneId.systemDefault().id }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
    Box(Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.bg1)
                        .border(1.dp, theme.stroke, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, null, tint = theme.text0, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (kind == CreateFormKind.Metric) strings.newChallengeTitle else strings.newEventTitle,
                    color         = theme.text0,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // ── Kind segment ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg1.copy(0.6f))
                    .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                KindChip(
                    label = strings.challengeKindMetric,
                    isActive = kind == CreateFormKind.Metric,
                    accent = accent,
                    onClick = { kind = CreateFormKind.Metric },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(3.dp))
                KindChip(
                    label = strings.challengeKindEvent,
                    isActive = kind == CreateFormKind.Event,
                    accent = accent,
                    onClick = { kind = CreateFormKind.Event },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Title ──
            FieldLabel(strings.challengeFieldTitle)
            TextInputBox(
                value = title,
                onValueChange = { title = it },
                placeholder = if (kind == CreateFormKind.Metric)
                    "30 gün kardiyo serisi"
                else
                    "Pazar günü halı saha",
                imeAction = ImeAction.Next
            )

            // ── Description ──
            FieldLabel(strings.challengeFieldDesc)
            TextInputBox(
                value = description,
                onValueChange = { description = it },
                placeholder = "Kısa açıklama (opsiyonel)",
                imeAction = ImeAction.Default,
                minLines = 2
            )

            if (kind == CreateFormKind.Metric) {
                MetricForm(
                    accent = accent,
                    targetType = metricTargetType,
                    onTargetType = { metricTargetType = it },
                    targetValue = metricTargetValue,
                    onTargetValue = { metricTargetValue = it },
                    days = days,
                    onDays = { days = it },
                    endDate = metricEndDate,
                    fmt = fmt
                )
            } else {
                EventForm(
                    accent = accent,
                    mode = eventMode,
                    onMode = { eventMode = it },
                    dateIso = eventDateIso,
                    onPickDate = { showDatePicker = true },
                    timeIso = eventTimeIso,
                    onPickTime = { showTimePicker = true },
                    onClearTime = { eventTimeIso = null },
                    location = eventLocation,
                    onLocation = { eventLocation = it },
                    onlineUrl = eventOnlineUrl,
                    onOnlineUrl = { eventOnlineUrl = it },
                    movements = selectedMovements,
                    onOpenMultiPicker = { showMultiPicker = true },
                    targetEnabled = eventTargetEnabled,
                    onTargetEnabled = { eventTargetEnabled = it },
                    targetType = eventTargetType,
                    onTargetType = { eventTargetType = it },
                    targetValue = eventTargetValue,
                    onTargetValue = { eventTargetValue = it }
                )
            }

            // ── Visibility ──
            FieldLabel(strings.challengeVisibility)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg1.copy(0.6f))
                    .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                VisibilityChip(
                    label    = strings.challengeVisPublic,
                    isActive = visibility == ChallengeVisibility.Public,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Public },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(3.dp))
                VisibilityChip(
                    label    = strings.challengeVisPrivate,
                    isActive = visibility == ChallengeVisibility.Private,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Private },
                    modifier = Modifier.weight(1f)
                )
            }

            if (visibility == ChallengeVisibility.Private) {
                FieldLabel("ŞİFRE (arkadaşlarına paylaş)")
                TextInputBox(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Opsiyonel — boş bırakılırsa herkes girebilir",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password
                )
            }

            // ── Error ──
            error?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5252).copy(0.14f))
                        .border(1.dp, Color(0xFFFF5252).copy(0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }

            // ── Submit ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent)
                    .clickable(enabled = !inFlight) {
                        if (kind == CreateFormKind.Metric) {
                            onSubmit(
                                title,
                                description,
                                metricTargetType,
                                metricTargetValue.toLongOrNull() ?: 0L,
                                today.format(fmt),
                                metricEndDate.format(fmt),
                                visibility,
                                password.ifBlank { null }
                            )
                        } else {
                            val req = CreateEventChallengeRequest(
                                title       = title,
                                description = description.ifBlank { null },
                                mode        = eventMode,
                                dateIso     = eventDateIso,
                                timeIso     = eventTimeIso,
                                timezone    = timezone,
                                location    = eventLocation.ifBlank { null },
                                geoLat      = null,  // TODO: map picker ileride eklenecek
                                geoLng      = null,
                                onlineUrl   = eventOnlineUrl.ifBlank { null },
                                movements   = if (eventMode == EventMode.MovementList)
                                    selectedMovements.toList() else emptyList(),
                                targetType  = if (eventMode != EventMode.MovementList && eventTargetEnabled)
                                    eventTargetType else null,
                                targetValue = if (eventMode != EventMode.MovementList && eventTargetEnabled)
                                    eventTargetValue.toLongOrNull() else null,
                                visibility  = visibility,
                                password    = password.ifBlank { null }
                            )
                            onSubmitEvent(req)
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inFlight) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        strings.challengeCreateBtn,
                        color         = Color.Black,
                        fontSize      = 14.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Floating navbar (capsule) için boşluk + system nav bar insets
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(120.dp)
            )
        }
    }
    }

    // ── Date picker dialog ───────────────────────────────────────────────
    if (showDatePicker) {
        val initial = runCatching { LocalDate.parse(eventDateIso) }.getOrDefault(today)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                        eventDateIso = d.toString()
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // ── Time picker dialog ───────────────────────────────────────────────
    if (showTimePicker) {
        val initial = runCatching {
            eventTimeIso?.let { LocalTime.parse(it) }
        }.getOrNull() ?: LocalTime.of(20, 0)
        val state = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.bg1)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Spacer(Modifier.height(12.dp))
                Row {
                    TextButton(onClick = { showTimePicker = false }) { Text("İptal") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        eventTimeIso = "%02d:%02d:00".format(state.hour, state.minute)
                        showTimePicker = false
                    }) { Text("Tamam") }
                }
            }
        }
    }

    // ── Multi-picker sheet ───────────────────────────────────────────────
    if (showMultiPicker) {
        ExerciseMultiPickerSheet(
            exercises       = exercises,
            initialSelected = selectedMovements.toList(),
            onDismiss       = { showMultiPicker = false },
            onConfirm       = { out ->
                selectedMovements.clear()
                selectedMovements.addAll(out)
                showMultiPicker = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  METRİK FORMU
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MetricForm(
    accent: Color,
    targetType: ChallengeTargetType,
    onTargetType: (ChallengeTargetType) -> Unit,
    targetValue: String,
    onTargetValue: (String) -> Unit,
    days: String,
    onDays: (String) -> Unit,
    endDate: LocalDate,
    fmt: DateTimeFormatter
) {
    val theme = LocalAppTheme.current
    FieldLabel("HEDEF TİPİ")
    Column(Modifier.padding(horizontal = 16.dp)) {
        ChallengeTargetType.selectableForMetric.forEach { t ->
            TargetTypeOption(
                type     = t,
                isActive = t == targetType,
                accent   = accent,
                onClick  = { onTargetType(t) }
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            FieldLabel("HEDEF (${targetType.unit})", padded = false)
            NumberInputInline(value = targetValue, onValueChange = onTargetValue)
        }
        Column(Modifier.weight(1f)) {
            FieldLabel("SÜRE (gün)", padded = false)
            NumberInputInline(value = days, onValueChange = onDays)
        }
    }
    Text(
        "Bitiş: ${endDate.format(fmt)}",
        color = theme.text2,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  ETKİNLİK FORMU
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EventForm(
    accent: Color,
    mode: EventMode,
    onMode: (EventMode) -> Unit,
    dateIso: String,
    onPickDate: () -> Unit,
    timeIso: String?,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    location: String,
    onLocation: (String) -> Unit,
    onlineUrl: String,
    onOnlineUrl: (String) -> Unit,
    movements: List<MovementInput>,
    onOpenMultiPicker: () -> Unit,
    targetEnabled: Boolean,
    onTargetEnabled: (Boolean) -> Unit,
    targetType: ChallengeTargetType,
    onTargetType: (ChallengeTargetType) -> Unit,
    targetValue: String,
    onTargetValue: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings

    // ── Mode selector ────────────────────────────────────────────────────
    FieldLabel(strings.challengeKindEvent)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        EventModeChip(
            label = strings.eventModePhysical,
            icon = Icons.Rounded.LocationOn,
            isActive = mode == EventMode.Physical,
            accent = accent,
            onClick = { onMode(EventMode.Physical) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(3.dp))
        EventModeChip(
            label = strings.eventModeOnline,
            icon = Icons.Rounded.Link,
            isActive = mode == EventMode.Online,
            accent = accent,
            onClick = { onMode(EventMode.Online) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(3.dp))
        EventModeChip(
            label = strings.eventModeMovementList,
            icon = Icons.Rounded.PlaylistAddCheck,
            isActive = mode == EventMode.MovementList,
            accent = accent,
            onClick = { onMode(EventMode.MovementList) },
            modifier = Modifier.weight(1f)
        )
    }

    // ── Date & time ──────────────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            FieldLabel("TARİH", padded = false)
            PickerField(
                text = dateIso,
                icon = Icons.Rounded.CalendarMonth,
                onClick = onPickDate,
                accent = accent
            )
        }
        Column(Modifier.weight(1f)) {
            FieldLabel("SAAT (ops)", padded = false)
            PickerField(
                text = timeIso ?: "—",
                icon = Icons.Rounded.Schedule,
                onClick = onPickTime,
                trailing = if (timeIso != null) {
                    { Icon(
                        Icons.Rounded.Close, null,
                        tint = theme.text2,
                        modifier = Modifier.size(14.dp).clickable { onClearTime() }
                    ) }
                } else null,
                accent = accent
            )
        }
    }

    // ── Mode-specific fields ─────────────────────────────────────────────
    when (mode) {
        EventMode.Physical -> {
            FieldLabel("KONUM")
            TextInputBox(
                value = location,
                onValueChange = onLocation,
                placeholder = "ör. Maçka Parkı, İstanbul",
                imeAction = ImeAction.Default
            )
            OptionalMetricTargetSection(
                enabled = targetEnabled,
                onEnabled = onTargetEnabled,
                targetType = targetType,
                onTargetType = onTargetType,
                targetValue = targetValue,
                onTargetValue = onTargetValue,
                accent = accent
            )
        }
        EventMode.Online -> {
            FieldLabel("ONLINE LİNK")
            TextInputBox(
                value = onlineUrl,
                onValueChange = onOnlineUrl,
                placeholder = "https://meet.example.com/…",
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Uri
            )
            OptionalMetricTargetSection(
                enabled = targetEnabled,
                onEnabled = onTargetEnabled,
                targetType = targetType,
                onTargetType = onTargetType,
                targetValue = targetValue,
                onTargetValue = onTargetValue,
                accent = accent
            )
        }
        EventMode.MovementList -> {
            FieldLabel("HAREKETLER (${movements.size})")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg1.copy(0.6f))
                    .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            ) {
                if (movements.isEmpty()) {
                    Text(
                        "Henüz hareket seçilmedi",
                        color = theme.text2,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    movements.forEachIndexed { idx, mv ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${idx + 1}.",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(
                                mv.exerciseId.take(8) + "…",
                                color = theme.text0,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val suffix = buildString {
                                mv.suggestedSets?.let { append("${it}×") }
                                mv.suggestedReps?.let { append("$it") }
                                mv.suggestedDurSec?.let {
                                    if (isNotEmpty()) append(" · ")
                                    append("${it}s")
                                }
                            }
                            if (suffix.isNotEmpty()) {
                                Text(suffix, color = theme.text2, fontSize = 11.sp)
                            }
                        }
                        if (idx < movements.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = theme.stroke.copy(0.4f), thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.18f))
                    .border(1.dp, accent.copy(0.45f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenMultiPicker)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (movements.isEmpty()) "HAREKET SEÇ" else "HAREKETLERİ DÜZENLE",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            Text(
                "Hedef otomatik: ${movements.size} hareket",
                color = theme.text2,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OptionalMetricTargetSection(
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    targetType: ChallengeTargetType,
    onTargetType: (ChallengeTargetType) -> Unit,
    targetValue: String,
    onTargetValue: (String) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
    // Toggle row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .clickable { onEnabled(!enabled) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (enabled) accent else Color.Transparent)
                .border(1.dp, if (enabled) accent else theme.stroke, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "MESAFE / SÜRE HEDEFİ",
                color = theme.text0,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "Katılımcılar hedefi tamamladığında challenge biter",
                color = theme.text2,
                fontSize = 10.sp
            )
        }
    }

    if (enabled) {
        FieldLabel("HEDEF TİPİ")
        Column(Modifier.padding(horizontal = 16.dp)) {
            listOf(
                ChallengeTargetType.TotalDistanceM,
                ChallengeTargetType.TotalDistanceKm,
                ChallengeTargetType.TotalDurationMinutes
            ).forEach { t ->
                TargetTypeOption(
                    type     = t,
                    isActive = t == targetType,
                    accent   = accent,
                    onClick  = { onTargetType(t) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            FieldLabel("HEDEF (${targetType.unit})", padded = false)
            NumberInputInline(value = targetValue, onValueChange = onTargetValue)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Küçük yardımcılar
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FieldLabel(text: String, padded: Boolean = true) {
    val theme = LocalAppTheme.current
    Text(
        text,
        color      = theme.text2,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(
            start = if (padded) 20.dp else 0.dp,
            top = 10.dp,
            bottom = 4.dp
        )
    )
}

@Composable
private fun TextInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    minLines: Int = 1
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = theme.text0, fontSize = 14.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = theme.text2.copy(0.55f), fontSize = 14.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun NumberInputInline(value: String, onValueChange: (String) -> Unit) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(6)) },
            textStyle = TextStyle(color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Black),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PickerField(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Color,
    trailing: (@Composable () -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg1.copy(0.6f))
            .border(1.dp, theme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = theme.text0,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun TargetTypeOption(
    type: ChallengeTargetType,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accent.copy(0.14f) else theme.bg1.copy(0.5f))
            .border(
                1.dp,
                if (isActive) accent.copy(0.5f) else theme.stroke.copy(0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isActive) accent else Color.Transparent)
                .border(1.dp, if (isActive) accent else theme.text2, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            type.label,
            color = if (isActive) theme.text0 else theme.text1,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(type.unit, color = theme.text2, fontSize = 11.sp)
    }
}

@Composable
private fun KindChip(
    label: String,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(accent.copy(0.22f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun EventModeChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(accent.copy(0.22f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isActive) accent else theme.text2, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun VisibilityChip(
    label: String,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(accent.copy(0.22f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
    }
}
