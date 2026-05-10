package com.avonix.profitness.presentation.challenges

import android.util.Log
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TravelExplore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.avonix.profitness.BuildConfig
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
import com.avonix.profitness.presentation.workout.SportType
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class CreateFormKind { Metric, Event }

private const val PLACES_LOG_TAG = "CreateChallengePlaces"

private data class PlaceCandidate(
    val placeId: String,
    val title: String,
    val subtitle: String,
    val lat: Double?,
    val lng: Double?
) {
    val displayName: String
        get() = listOf(title, subtitle).filter { it.isNotBlank() }.joinToString(", ")
}

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
    onFormChanged: () -> Unit,
    onSubmitEvent: (CreateEventChallengeRequest) -> Unit
) {
    val theme = LocalAppTheme.current
    val strings = theme.strings
    val accent = MaterialTheme.colorScheme.primary

    // ── Top-level mode ────────────────────────────────────────────────────
    var kind by rememberSaveable { mutableStateOf(CreateFormKind.Event) }

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
    var eventEndLocation by rememberSaveable { mutableStateOf("") }
    var eventGeoLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var eventGeoLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var eventEndGeoLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var eventEndGeoLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var eventOnlineUrl by rememberSaveable { mutableStateOf("") }
    var eventSportType by rememberSaveable { mutableStateOf(SportType.Running) }
    var eventTargetEnabled by rememberSaveable { mutableStateOf(true) }
    var eventTargetType by rememberSaveable { mutableStateOf(ChallengeTargetType.TotalDistanceM) }
    var eventTargetValue by rememberSaveable { mutableStateOf("5000") }
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

    LaunchedEffect(
        kind,
        title,
        description,
        visibility,
        password,
        metricTargetType,
        metricTargetValue,
        days,
        eventMode,
        eventDateIso,
        eventTimeIso,
        eventLocation,
        eventGeoLat,
        eventGeoLng,
        eventEndLocation,
        eventEndGeoLat,
        eventEndGeoLng,
        eventOnlineUrl,
        eventSportType,
        eventTargetEnabled,
        eventTargetType,
        eventTargetValue,
        selectedMovements.size
    ) {
        if (error != null) onFormChanged()
    }

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
            // ── Top bar (premium) ──
            val isMetric = kind == CreateFormKind.Metric
            val headerIcon = if (isMetric) Icons.Rounded.EmojiEvents else Icons.Rounded.CalendarMonth
            val headerTitle = if (isMetric) strings.newChallengeTitle else strings.newEventTitle
            val headerSubtitle = if (isMetric)
                "Hedefini koy, takip et, başar"
            else
                "Etkinlik planla, dostlarını davet et"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(0.10f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Close button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.bg1.copy(0.85f))
                            .border(1.dp, theme.stroke.copy(0.6f), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = theme.text0,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Icon badge (kind indicator)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(accent.copy(0.32f), accent.copy(0.14f))
                                )
                            )
                            .border(1.dp, accent.copy(0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            headerIcon,
                            null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            headerTitle,
                            color         = theme.text0,
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            maxLines      = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            headerSubtitle,
                            color    = theme.text2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── Kind segment ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg1.copy(0.72f))
                    .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
                    .padding(4.dp)
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
                    exercises = exercises,
                    dateIso = eventDateIso,
                    onPickDate = { showDatePicker = true },
                    timeIso = eventTimeIso,
                    onPickTime = { showTimePicker = true },
                    onClearTime = { eventTimeIso = null },
                    location = eventLocation,
                    onLocation = {
                        eventLocation = it
                        eventGeoLat = null
                        eventGeoLng = null
                    },
                    locationResolved = eventGeoLat != null && eventGeoLng != null,
                    onLocationSelected = {
                        eventLocation = it.displayName
                        eventGeoLat = it.lat
                        eventGeoLng = it.lng
                    },
                    endLocation = eventEndLocation,
                    onEndLocation = {
                        eventEndLocation = it
                        eventEndGeoLat = null
                        eventEndGeoLng = null
                    },
                    endLocationResolved = eventEndGeoLat != null && eventEndGeoLng != null,
                    onEndLocationSelected = {
                        eventEndLocation = it.displayName
                        eventEndGeoLat = it.lat
                        eventEndGeoLng = it.lng
                    },
                    onlineUrl = eventOnlineUrl,
                    onOnlineUrl = { eventOnlineUrl = it },
                    sportType = eventSportType,
                    onSportType = {
                        eventSportType = it
                        if (eventMode != EventMode.MovementList) {
                            eventTargetEnabled = true
                            eventTargetType = defaultTargetTypeForSport(it)
                            eventTargetValue = defaultTargetValueForSport(it)
                        }
                    },
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg1.copy(0.72f))
                    .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                VisibilityChip(
                    label    = strings.challengeVisPublic,
                    icon     = Icons.Rounded.Public,
                    isActive = visibility == ChallengeVisibility.Public,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Public },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(3.dp))
                VisibilityChip(
                    label    = strings.challengeVisPrivate,
                    icon     = Icons.Rounded.Lock,
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
                                description = description.trim().ifBlank { null },
                                mode        = eventMode,
                                sportType   = eventSportType,
                                exerciseId  = canonicalExerciseIdForSport(eventSportType, exercises),
                                dateIso     = eventDateIso,
                                timeIso     = eventTimeIso,
                                timezone    = timezone,
                                location    = eventLocation.trim().ifBlank { null },
                                geoLat      = eventGeoLat,
                                geoLng      = eventGeoLng,
                                endLocation = eventEndLocation.trim().ifBlank { null },
                                endGeoLat   = eventEndGeoLat,
                                endGeoLng   = eventEndGeoLng,
                                onlineUrl   = eventOnlineUrl.trim().ifBlank { null },
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

private sealed interface PlaceSearchResult {
    data class Success(val candidates: List<PlaceCandidate>) : PlaceSearchResult
    data class Failure(val message: String) : PlaceSearchResult
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
    exercises: List<ExerciseItem>,
    dateIso: String,
    onPickDate: () -> Unit,
    timeIso: String?,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    location: String,
    onLocation: (String) -> Unit,
    locationResolved: Boolean,
    onLocationSelected: (PlaceCandidate) -> Unit,
    endLocation: String,
    onEndLocation: (String) -> Unit,
    endLocationResolved: Boolean,
    onEndLocationSelected: (PlaceCandidate) -> Unit,
    onlineUrl: String,
    onOnlineUrl: (String) -> Unit,
    sportType: SportType,
    onSportType: (SportType) -> Unit,
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
    val exerciseNameById = remember(exercises) { exercises.associate { it.id to it.name } }

    // ── Mode selector ────────────────────────────────────────────────────
    FieldLabel(strings.challengeKindEvent)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
            .padding(4.dp)
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

    if (mode != EventMode.MovementList) {
        FieldLabel("SPOR TÜRÜ")
        LazySportTypeRow(
            selected = sportType,
            accent = accent,
            onSelect = onSportType
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
            FieldLabel("BAŞLANGIÇ KONUMU")
            PlaceSearchField(
                value = location,
                onValueChange = onLocation,
                placeholder = "ör. Maçka Parkı, İstanbul",
                isResolved = locationResolved,
                required = true,
                onPlaceSelected = onLocationSelected
            )
            FieldLabel("BİTİŞ KONUMU (opsiyonel)")
            PlaceSearchField(
                value = endLocation,
                onValueChange = onEndLocation,
                placeholder = "ör. Caddebostan Sahil, İstanbul",
                isResolved = endLocationResolved,
                required = false,
                onPlaceSelected = onEndLocationSelected
            )
            Text(
                "Konumu listeden seç; aynı isimli yerlerde il/ilçe bilgisiyle doğru adayı kaydederiz.",
                color = LocalAppTheme.current.text2,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg1.copy(0.72f))
                    .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
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
                                exerciseNameById[mv.exerciseId] ?: mv.exerciseId.take(8) + "…",
                                color = theme.text0,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val suffix = movementSpecLabel(mv)
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
private fun LazySportTypeRow(
    selected: SportType,
    accent: Color,
    onSelect: (SportType) -> Unit
) {
    val theme = LocalAppTheme.current
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(SportType.challengeChoices.size) { idx ->
            val sport = SportType.challengeChoices[idx]
            val active = sport == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) accent else theme.bg1.copy(0.72f))
                    .border(1.dp, if (active) accent else theme.stroke.copy(0.68f), RoundedCornerShape(999.dp))
                    .clickable { onSelect(sport) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    sport.label.uppercase(),
                    color = if (active) Color.Black else theme.text1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
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
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
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
        color      = theme.text1,
        fontSize   = 10.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.25.sp,
        modifier = Modifier.padding(
            start = if (padded) 20.dp else 0.dp,
            top = 14.dp,
            bottom = 6.dp
        )
    )
}

@Composable
private fun PlaceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isResolved: Boolean,
    required: Boolean,
    onPlaceSelected: (PlaceCandidate) -> Unit
) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val placesClient = remember(context) { createPlacesClientOrNull(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    val scope = rememberCoroutineScope()
    val hasResolvedValue = isResolved && value.isNotBlank()
    var candidates by remember { mutableStateOf<List<PlaceCandidate>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var resolvingPlaceId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(value, isResolved) {
        val query = value.trim()
        candidates = emptyList()
        message = null
        loading = false
        if (hasResolvedValue || query.length < 3) return@LaunchedEffect
        if (placesClient == null) {
            message = "Google Places API key eksik; local.properties içine MAPS_API_KEY ekle."
            return@LaunchedEffect
        }

        delay(350)
        loading = true
        val result = searchPlaceCandidates(placesClient, sessionToken, query)
        loading = false
        when (result) {
            is PlaceSearchResult.Success -> {
                candidates = result.candidates
                message = when {
                    result.candidates.isEmpty() -> "Sonuç bulunamadı; il/ilçe ekleyerek tekrar dene."
                    else -> null
                }
            }
            is PlaceSearchResult.Failure -> {
                candidates = emptyList()
                message = result.message
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.bg1.copy(0.72f))
                .border(
                    1.dp,
                    if (hasResolvedValue) MaterialTheme.colorScheme.primary.copy(0.70f) else theme.stroke.copy(0.70f),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = theme.text0, fontSize = 15.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(placeholder, color = theme.text2.copy(0.72f), fontSize = 15.sp, lineHeight = 20.sp)
                        }
                        inner()
                    }
                )
                Spacer(Modifier.width(10.dp))
                when {
                    loading || resolvingPlaceId != null -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    hasResolvedValue -> Icon(
                        Icons.Rounded.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        Icons.Rounded.TravelExplore,
                        null,
                        tint = theme.text2,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (hasResolvedValue) {
            Text(
                "Seçili konum kaydedilecek.",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        } else if (value.isNotBlank() && required) {
            Text(
                "Devam etmek için aşağıdaki sonuçlardan bir konum seç.",
                color = theme.text2,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        }

        if (candidates.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg1.copy(0.84f))
                    .border(1.dp, theme.stroke.copy(0.68f), RoundedCornerShape(14.dp))
            ) {
                candidates.forEachIndexed { index, candidate ->
                    val isResolving = resolvingPlaceId == candidate.placeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = resolvingPlaceId == null) {
                                val client = placesClient ?: return@clickable
                                resolvingPlaceId = candidate.placeId
                                message = null
                                scope.launch {
                                    val resolved = resolvePlaceCandidate(client, sessionToken, candidate)
                                    resolvingPlaceId = null
                                    if (resolved?.lat != null && resolved.lng != null) {
                                        onPlaceSelected(resolved)
                                    } else {
                                        message = "Konum koordinatı alınamadı; başka bir sonuç seç."
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(if (index == 0) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                candidate.title,
                                color = theme.text0,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (candidate.subtitle.isNotBlank()) {
                                Text(
                                    candidate.subtitle,
                                    color = theme.text2,
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        if (isResolving) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    "Powered by Google",
                    color = theme.text2.copy(0.82f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        } else if (message != null) {
            Text(
                message.orEmpty(),
                color = theme.text2,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }
    }
}

private fun createPlacesClientOrNull(context: android.content.Context): PlacesClient? {
    val apiKey = BuildConfig.MAPS_API_KEY.trim()
    if (apiKey.isEmpty()) return null
    if (!Places.isInitialized()) {
        Places.initializeWithNewPlacesApiEnabled(context.applicationContext, apiKey)
    }
    return Places.createClient(context.applicationContext)
}

private suspend fun searchPlaceCandidates(
    placesClient: PlacesClient,
    sessionToken: AutocompleteSessionToken,
    query: String
): PlaceSearchResult = withContext(Dispatchers.IO) {
    runCatching {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("TR")
            .setSessionToken(sessionToken)
            .build()

        Tasks.await(placesClient.findAutocompletePredictions(request))
            .autocompletePredictions
            .take(5)
            .map { prediction ->
                PlaceCandidate(
                    placeId = prediction.placeId,
                    title = prediction.getPrimaryText(null).toString(),
                    subtitle = prediction.getSecondaryText(null).toString(),
                    lat = null,
                    lng = null
                )
            }
    }.fold(
        onSuccess = { PlaceSearchResult.Success(it) },
        onFailure = { error ->
            Log.w(PLACES_LOG_TAG, "Places autocomplete failed", error)
            PlaceSearchResult.Failure(error.toPlacesMessage())
        }
    )
}

private fun Throwable.toPlacesMessage(): String {
    val apiException = this as? ApiException
    val status = apiException?.status
    val detail = listOfNotNull(
        status?.statusMessage?.takeIf { it.isNotBlank() },
        message?.takeIf { it.isNotBlank() }
    )
        .distinct()
        .joinToString(" - ")

    return when {
        apiException != null && detail.isNotBlank() ->
            "Google Places hatası (${apiException.statusCode}): $detail"
        apiException != null ->
            "Google Places hatası (${apiException.statusCode}). API key kısıtlarını ve Places API (New) ayarını kontrol et."
        detail.isNotBlank() ->
            "Google Places hatası: $detail"
        else ->
            "Google Places isteği tamamlanamadı. API key, billing ve Places API (New) ayarlarını kontrol et."
    }
}

private suspend fun resolvePlaceCandidate(
    placesClient: PlacesClient,
    sessionToken: AutocompleteSessionToken,
    candidate: PlaceCandidate
): PlaceCandidate? = withContext(Dispatchers.IO) {
    runCatching {
        val request = FetchPlaceRequest.builder(
            candidate.placeId,
            listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LAT_LNG)
        )
            .setSessionToken(sessionToken)
            .build()

        val place = Tasks.await(placesClient.fetchPlace(request)).place
        val latLng = place.latLng ?: return@runCatching null
        candidate.copy(
            title = place.displayName ?: candidate.title,
            subtitle = place.formattedAddress ?: candidate.subtitle,
            lat = latLng.latitude,
            lng = latLng.longitude
        )
    }.getOrNull()
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
            .heightIn(min = if (minLines == 1) 58.dp else 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.70f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = theme.text0, fontSize = 15.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = theme.text2.copy(0.72f), fontSize = 15.sp, lineHeight = 20.sp)
                }
                inner()
            }
        )
    }
}

/** Inline text input — Row hücrelerinde padding'siz; opsiyonel keyboardType + filter. */
@Composable
private fun TextInputInline(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.70f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v ->
                val filtered = if (keyboardType == KeyboardType.Number)
                    v.filter { it.isDigit() || it == '.' || it == '-' }.take(12)
                else v
                onValueChange(filtered)
            },
            textStyle = TextStyle(color = theme.text0, fontSize = 15.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = theme.text2.copy(0.72f), fontSize = 15.sp)
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
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.70f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg1.copy(0.72f))
            .border(1.dp, theme.stroke.copy(0.70f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
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

private fun iconForTargetType(type: ChallengeTargetType): ImageVector = when (type) {
    ChallengeTargetType.TotalWorkouts        -> Icons.Rounded.FitnessCenter
    ChallengeTargetType.TotalXp              -> Icons.Rounded.Bolt
    ChallengeTargetType.CurrentStreak        -> Icons.Rounded.LocalFireDepartment
    ChallengeTargetType.TotalDurationMinutes -> Icons.Rounded.Timer
    ChallengeTargetType.TotalDistanceM       -> Icons.Rounded.Straighten
    ChallengeTargetType.TotalDistanceKm      -> Icons.Rounded.Speed
    ChallengeTargetType.MovementsCompleted   -> Icons.Rounded.PlaylistAddCheck
}

private fun defaultTargetTypeForSport(sport: SportType): ChallengeTargetType = when (sport) {
    SportType.YogaPilates,
    SportType.Boxing,
    SportType.JumpRopeHiit,
    SportType.Football,
    SportType.BasketballTennis -> ChallengeTargetType.TotalDurationMinutes
    else -> ChallengeTargetType.TotalDistanceM
}

private fun defaultTargetValueForSport(sport: SportType): String = when (sport) {
    SportType.Cycling -> "10000"
    SportType.Running -> "5000"
    SportType.Swimming -> "1000"
    SportType.Rowing -> "2000"
    SportType.WalkingHiking -> "5000"
    SportType.YogaPilates -> "45"
    SportType.Boxing -> "30"
    SportType.JumpRopeHiit -> "20"
    SportType.Football -> "60"
    SportType.BasketballTennis -> "45"
    SportType.Strength -> "1"
}

private fun canonicalExerciseIdForSport(sport: SportType, exercises: List<ExerciseItem>): String? {
    val needles = when (sport) {
        SportType.Cycling -> listOf("cycling", "bisiklet", "bike")
        SportType.Running -> listOf("treadmill run", "running", "kos", "run")
        SportType.Swimming -> listOf("swimming", "yuzme", "yuz")
        SportType.Rowing -> listOf("rowing machine", "outdoor rowing", "kurek", "row")
        SportType.WalkingHiking -> listOf("walk", "walking", "yuruyus", "hike")
        SportType.JumpRopeHiit -> listOf("jump rope", "hiit", "burpee")
        SportType.YogaPilates -> listOf("yoga", "pilates", "mobility")
        SportType.Boxing -> listOf("shadow boxing", "boxing", "boks")
        SportType.Football -> listOf("football", "soccer", "futbol")
        SportType.BasketballTennis -> listOf("basketball", "tennis", "basket", "tenis")
        SportType.Strength -> emptyList()
    }
    return exercises.firstOrNull { ex ->
        val haystack = listOf(ex.name, ex.nameEn, ex.category, ex.targetMuscle)
            .joinToString(" ")
            .lowercase()
        needles.any { it in haystack }
    }?.id
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive) Brush.horizontalGradient(
                    listOf(accent.copy(0.18f), accent.copy(0.08f))
                ) else Brush.horizontalGradient(
                    listOf(theme.bg1.copy(0.72f), theme.bg1.copy(0.54f))
                )
            )
            .border(
                1.dp,
                if (isActive) accent.copy(0.58f) else theme.stroke.copy(0.55f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) accent.copy(0.22f) else theme.bg2.copy(0.64f)
                )
                .border(
                    1.dp,
                    if (isActive) accent.copy(0.5f) else theme.stroke.copy(0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconForTargetType(type),
                null,
                tint = if (isActive) accent else theme.text2,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            type.label,
            color = if (isActive) theme.text0 else theme.text1,
            fontSize = 13.5.sp,
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        // Unit pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isActive) accent.copy(0.20f) else theme.bg2.copy(0.5f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                type.unit,
                color = if (isActive) accent else theme.text2,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun movementSpecLabel(movement: MovementInput): String = buildString {
    val hasStrength = movement.suggestedSets != null || movement.suggestedReps != null
    if (hasStrength) {
        movement.suggestedSets?.let { append("${it} set") }
        movement.suggestedReps?.let {
            if (isNotEmpty()) append(" · ")
            append("${it} tekrar")
        }
        return@buildString
    }
    movement.suggestedDurSec?.let { append(formatChallengeDuration(it)) }
}

private fun formatChallengeDuration(seconds: Int): String =
    if (seconds >= 60) "${(seconds / 60).coerceAtLeast(1)} dk" else "${seconds}s"

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
                if (isActive) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(0.24f), accent.copy(0.12f))
                    )
                ).border(1.dp, accent.copy(0.40f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
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
                if (isActive) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(0.24f), accent.copy(0.12f))
                    )
                ).border(1.dp, accent.copy(0.40f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isActive) accent else theme.text2, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun VisibilityChip(
    label: String,
    isActive: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(0.24f), accent.copy(0.12f))
                    )
                ).border(1.dp, accent.copy(0.40f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon, null,
                tint = if (isActive) accent else theme.text2,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            color = if (isActive) accent else theme.text2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}
