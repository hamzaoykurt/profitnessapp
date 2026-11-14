package com.cosmibit.profitness.presentation.challenges

import androidx.compose.runtime.getValue
import com.cosmibit.profitness.core.theme.sinemaClickable
import androidx.compose.runtime.setValue
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaBackdrop
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.workout.SceneImage
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.cosmibit.profitness.BuildConfig
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.exerciseDisplayName
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.theme.stroke
import com.cosmibit.profitness.core.theme.effectiveOnAccentColor
import com.cosmibit.profitness.domain.challenges.ChallengeTargetType
import com.cosmibit.profitness.domain.challenges.ChallengeVisibility
import com.cosmibit.profitness.domain.challenges.CreateEventChallengeRequest
import com.cosmibit.profitness.domain.challenges.EventMode
import com.cosmibit.profitness.domain.challenges.MovementInput
import com.cosmibit.profitness.domain.model.ExerciseItem
import com.cosmibit.profitness.presentation.program.ExerciseMultiPickerSheet
import com.cosmibit.profitness.presentation.workout.SportType
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
        password: String?,
        maxParticipants: Int?
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
    var participantLimitEnabled by rememberSaveable { mutableStateOf(false) }
    var participantLimit by rememberSaveable { mutableStateOf("10") }

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

    // Kaydedilmemiş değişiklik onayı (kaydet burada doğrulamaya bağlı → yalnız Vazgeç/Devam).
    val isDirty = title.isNotBlank() || description.isNotBlank() || eventLocation.isNotBlank() ||
        eventEndLocation.isNotBlank() || eventOnlineUrl.isNotBlank() || password.isNotBlank() ||
        selectedMovements.isNotEmpty()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestClose: () -> Unit = { if (isDirty && !inFlight) showDiscardDialog = true else onDismiss() }
    if (showDiscardDialog) {
        com.cosmibit.profitness.presentation.components.SinemaDialog(
            onDismiss = { showDiscardDialog = false },
            title = theme.t("Taslak silinsin mi?", "Discard draft?"),
            body = theme.t("Çıkarsan girdiğin bilgiler silinir.", "If you leave, what you entered will be discarded."),
            confirmLabel = theme.t("VAZGEÇ, SİL", "DISCARD"),
            confirmIcon = Icons.Rounded.Close,
            onConfirm = { showDiscardDialog = false; onDismiss() },
            dismissLabel = theme.t("Düzenlemeye devam", "Keep editing")
        )
    }

    LaunchedEffect(
        kind,
        title,
        description,
        visibility,
        password,
        participantLimitEnabled,
        participantLimit,
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
        onDismissRequest = requestClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
    CreateSceneBars()
    Box(Modifier.fillMaxSize()) {
        SinemaBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
// ── Sahne: seçilen türün fotoğrafı + karartma + başlık ──
            val isMetric = kind == CreateFormKind.Metric
            val headerTitle = if (isMetric) strings.newChallengeTitle else strings.newEventTitle
            val headerSubtitle = if (isMetric)
                theme.t("Hedefini koy, takip et, başar", "Set a goal, track it, finish strong")
            else
                theme.t("Etkinlik planla, dostlarını davet et", "Plan an event and invite friends")
            val sceneCategory = createPhotoCategory(isMetric, metricTargetType, eventMode, eventSportType)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Sinema.scrim(1f))
            ) {
                SceneImage(
                    url = categoryImageUrl(sceneCategory),
                    modifier = Modifier.matchParentSize(),
                    requestWidthPx = 1080, requestHeightPx = 720, fallbackGradient = true
                )
                Box(Modifier.matchParentSize().background(Sinema.BottomScrim))
                Box(
                    Modifier.fillMaxWidth().height(130.dp).background(
                        Brush.verticalGradient(0f to Sinema.scrim(0.55f), 1f to Color.Transparent)
                    )
                )
                Column(
                    Modifier.matchParentSize().statusBarsPadding().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SinemaCapsuleIconButton(
                        icon = Icons.Rounded.Close, description = theme.t("Kapat", "Close"),
                        onClick = requestClose, selected = false, size = 44.dp
                    )
                    Column {
                        Text(
                            headerTitle,
                            style = Sinema.ScreenTitle, color = Color.White, maxLines = 2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(headerSubtitle, color = Color.White.copy(0.78f), fontSize = 14.sp, maxLines = 1)
                    }
                }
            }

            // ── Kind segment ──
Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KindChip(
                    label = strings.challengeKindMetric,
                    isActive = kind == CreateFormKind.Metric,
                    accent = accent,
                    onClick = { kind = CreateFormKind.Metric },
                    modifier = Modifier.weight(1f)
                )
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
                placeholder = theme.t("Kısa açıklama (opsiyonel)", "Short description (optional)"),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibilityChip(
                    label    = strings.challengeVisPublic,
                    icon     = Icons.Rounded.Public,
                    isActive = visibility == ChallengeVisibility.Public,
                    accent   = accent,
                    onClick  = { visibility = ChallengeVisibility.Public },
                    modifier = Modifier.weight(1f)
                )
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
                FieldLabel(theme.t("ŞİFRE (arkadaşlarına paylaş)", "PASSWORD (share with friends)"))
                TextInputBox(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = theme.t(
                        "Opsiyonel - boş bırakılırsa herkes girebilir",
                        "Optional - leave empty to let anyone join"
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password
                )
            }

            FieldLabel(theme.t("KATILIM SINIRI", "PARTICIPANT LIMIT"))
            ParticipantLimitSection(
                enabled = participantLimitEnabled,
                onEnabled = { participantLimitEnabled = it },
                value = participantLimit,
                onValue = { participantLimit = it },
                accent = accent
            )

            // ── Error ──
error?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .sinemaCard(theme, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text(it, color = Sinema.text(theme), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Submit ──
            SinemaGoOrIdle(
                label = if (inFlight) theme.t("OLUŞTURULUYOR…", "CREATING…") else strings.challengeCreateBtn.sinemaUpper(theme),
                icon = Icons.Rounded.Check,
                enabled = !inFlight,
                modifier = Modifier.padding(16.dp),
                onClick = {
                        if (kind == CreateFormKind.Metric) {
                            onSubmit(
                                title,
                                description,
                                metricTargetType,
                                metricTargetValue.toLongOrNull() ?: 0L,
                                today.format(fmt),
                                metricEndDate.format(fmt),
                                visibility,
                                password.ifBlank { null },
                                if (participantLimitEnabled) participantLimit.toIntOrNull() ?: 0 else null
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
                                password    = password.ifBlank { null },
                                maxParticipants = if (participantLimitEnabled) participantLimit.toIntOrNull() ?: 0 else null
                            )
                            onSubmitEvent(req)
                        }
                }
            )

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(40.dp)
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
                }) { Text(theme.t("Tamam", "OK")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(theme.t("İptal", "Cancel")) }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = Sinema.surface(theme))
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
                    .sinemaCard(theme, RoundedCornerShape(28.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Spacer(Modifier.height(12.dp))
                Row {
                    TextButton(onClick = { showTimePicker = false }) { Text(theme.t("İptal", "Cancel")) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        eventTimeIso = "%02d:%02d:00".format(state.hour, state.minute)
                        showTimePicker = false
                    }) { Text(theme.t("Tamam", "OK")) }
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
    FieldLabel(theme.t("HEDEF TİPİ", "TARGET TYPE"))
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
            FieldLabel(theme.t("HEDEF", "TARGET") + " (${targetType.displayUnit(theme)})", padded = false)
            NumberInputInline(value = targetValue, onValueChange = onTargetValue)
        }
        Column(Modifier.weight(1f)) {
            FieldLabel(theme.t("SÜRE (gün)", "DURATION (days)"), padded = false)
            NumberInputInline(value = days, onValueChange = onDays)
        }
    }
    Text(
        theme.t("Bitiş: ${endDate.format(fmt)}", "Ends: ${endDate.format(fmt)}"),
        color = Sinema.textSecondary(theme),
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
    val exerciseById = remember(exercises) { exercises.associateBy { it.id } }

    // ── Mode selector ────────────────────────────────────────────────────
    FieldLabel(strings.challengeKindEvent)
Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EventModeChip(
            label = strings.eventModePhysical,
            icon = Icons.Rounded.LocationOn,
            isActive = mode == EventMode.Physical,
            accent = accent,
            onClick = { onMode(EventMode.Physical) },
            modifier = Modifier.weight(1f)
        )
        EventModeChip(
            label = strings.eventModeOnline,
            icon = Icons.Rounded.Link,
            isActive = mode == EventMode.Online,
            accent = accent,
            onClick = { onMode(EventMode.Online) },
            modifier = Modifier.weight(1f)
        )
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
        FieldLabel(theme.t("SPOR TÜRÜ", "SPORT TYPE"))
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
            FieldLabel(theme.t("TARİH", "DATE"), padded = false)
            PickerField(
                text = dateIso,
                icon = Icons.Rounded.CalendarMonth,
                onClick = onPickDate,
                accent = accent
            )
        }
        Column(Modifier.weight(1f)) {
            FieldLabel(theme.t("SAAT (ops)", "TIME (opt)"), padded = false)
            PickerField(
                text = timeIso ?: "—",
                icon = Icons.Rounded.Schedule,
                onClick = onPickTime,
                trailing = if (timeIso != null) {
                    { Icon(
                        Icons.Rounded.Close, null,
                        tint = Sinema.textSecondary(theme),
                        modifier = Modifier.size(14.dp).sinemaClickable(onClick = { onClearTime() })
                    ) }
                } else null,
                accent = accent
            )
        }
    }

    // ── Mode-specific fields ─────────────────────────────────────────────
    when (mode) {
        EventMode.Physical -> {
            FieldLabel(theme.t("BAŞLANGIÇ KONUMU", "START LOCATION"))
            PlaceSearchField(
                value = location,
                onValueChange = onLocation,
                placeholder = theme.t("ör. Maçka Parkı, İstanbul", "e.g. Central Park, New York"),
                isResolved = locationResolved,
                required = true,
                onPlaceSelected = onLocationSelected
            )
            FieldLabel(theme.t("BİTİŞ KONUMU (opsiyonel)", "END LOCATION (optional)"))
            PlaceSearchField(
                value = endLocation,
                onValueChange = onEndLocation,
                placeholder = theme.t("ör. Caddebostan Sahil, İstanbul", "e.g. Brooklyn Bridge, New York"),
                isResolved = endLocationResolved,
                required = false,
                onPlaceSelected = onEndLocationSelected
            )
            Text(
                theme.t(
                    "Konumu listeden seç; aynı isimli yerlerde il/ilçe bilgisiyle doğru adayı kaydederiz.",
                    "Pick a location from the list so similarly named places are saved correctly."
                ),
                color = Sinema.textSecondary(LocalAppTheme.current),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OptionalMetricTargetSection(
                enabled = targetEnabled,
                onEnabled = onTargetEnabled,
                targetOptions = targetTypesForSport(sportType),
                targetType = targetType,
                onTargetType = onTargetType,
                targetValue = targetValue,
                onTargetValue = onTargetValue,
                accent = accent
            )
        }
        EventMode.Online -> {
            FieldLabel(theme.t("ONLINE LİNK", "ONLINE LINK"))
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
                targetOptions = targetTypesForSport(sportType),
                targetType = targetType,
                onTargetType = onTargetType,
                targetValue = targetValue,
                onTargetValue = onTargetValue,
                accent = accent
            )
        }
        EventMode.MovementList -> {
            FieldLabel(theme.t("HAREKETLER", "MOVEMENTS") + " (${movements.size})")
Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .sinemaCard(theme, RoundedCornerShape(24.dp))
            ) {
                if (movements.isEmpty()) {
                    Text(
                        theme.t("Henüz hareket seçilmedi", "No movements selected yet"),
                        color = Sinema.textSecondary(theme),
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
                                exerciseById[mv.exerciseId]?.let { theme.exerciseDisplayName(it.name, it.nameEn) }
                                    ?: mv.exerciseId.take(8) + "...",
                                color = Sinema.text(theme),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val suffix = movementSpecLabel(mv, theme)
                            if (suffix.isNotEmpty()) {
                                Text(suffix, color = Sinema.textSecondary(theme), fontSize = 11.sp)
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
SinemaCapsule(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                onClick = onOpenMultiPicker, height = 52.dp,
                horizontalArrangement = Arrangement.Center
            ) { fg ->
                Icon(Icons.Rounded.FitnessCenter, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (movements.isEmpty()) theme.t("HAREKET SEÇ", "SELECT MOVEMENTS")
                    else theme.t("HAREKETLERİ DÜZENLE", "EDIT MOVEMENTS"),
                    color = fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                )
            }
            Text(
                theme.t(
                    "Hedef otomatik: ${movements.size} hareket",
                    "Automatic target: ${movements.size} movements"
                ),
                color = Sinema.textSecondary(theme),
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
            SinemaCapsule(
                selected = sport == selected, onClick = { onSelect(sport) }, height = 40.dp,
contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) { fg ->
                Text(
                    sport.displayLabel(theme).sinemaUpper(theme),
                    color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun OptionalMetricTargetSection(
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    targetOptions: List<ChallengeTargetType>,
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
            .sinemaClickable(onClick = { onEnabled(!enabled) }, scale = 0.98f)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (enabled) accent else Color.Transparent)
                .border(1.5.dp, if (enabled) accent else Sinema.textSecondary(theme).copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (enabled) Icon(Icons.Rounded.Check, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                theme.t("MESAFE / SÜRE HEDEFİ", "DISTANCE / DURATION TARGET"),
                color = Sinema.text(theme),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                theme.t(
                    "Katılımcılar hedefi tamamladığında challenge biter",
                    "The challenge ends when participants complete the target"
                ),
                color = Sinema.textSecondary(theme),
                fontSize = 10.sp
            )
        }
    }

    if (enabled) {
        FieldLabel(theme.t("HEDEF TİPİ", "TARGET TYPE"))
        Column(Modifier.padding(horizontal = 16.dp)) {
            targetOptions.forEach { t ->
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
            FieldLabel(theme.t("HEDEF", "TARGET") + " (${targetType.displayUnit(theme)})", padded = false)
            NumberInputInline(value = targetValue, onValueChange = onTargetValue)
        }
    }
}

@Composable
private fun ParticipantLimitSection(
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    value: String,
    onValue: (String) -> Unit,
    accent: Color
) {
    val theme = LocalAppTheme.current
Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .sinemaCard(theme, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp).sinemaClickable(onClick = { onEnabled(!enabled) }, scale = 0.98f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (enabled) accent else Color.Transparent)
                    .border(1.5.dp, if (enabled) accent else Sinema.textSecondary(theme).copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (enabled) Icon(Icons.Rounded.Check, null, tint = theme.effectiveOnAccentColor, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("KİŞİ SAYISINI SINIRLA", "LIMIT PARTICIPANTS"),
                    color = Sinema.text(theme),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    theme.t("Limit dolunca yeni katılım kapanır", "Joining closes when the limit is reached"),
                    color = Sinema.textSecondary(theme),
                    fontSize = 12.sp
                )
            }
        }
        if (enabled) {
            Spacer(Modifier.height(12.dp))
            FieldLabel(theme.t("MAKSİMUM KATILIMCI", "MAX PARTICIPANTS"), padded = false)
            NumberInputInline(value = value, onValueChange = onValue)
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
        text.sinemaUpper(theme),
        color = Sinema.textSecondary(theme),
        style = Sinema.Label,
        modifier = Modifier.padding(
            start = if (padded) 20.dp else 4.dp,
            top = 18.dp,
            bottom = 8.dp
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
            message = theme.t(
                "Google Places API key eksik; local.properties içine MAPS_API_KEY ekle.",
                "Google Places API key is missing; add MAPS_API_KEY to local.properties."
            )
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
                    result.candidates.isEmpty() -> theme.t(
                        "Sonuç bulunamadı; il/ilçe ekleyerek tekrar dene.",
                        "No results found; try adding city or district."
                    )
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
SinemaCapsule(
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = Sinema.text(theme), fontSize = 15.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(placeholder, color = Sinema.textSecondary(theme).copy(0.72f), fontSize = 15.sp, lineHeight = 20.sp)
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
                        tint = Sinema.textSecondary(theme),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (hasResolvedValue) {
            Text(
                theme.t("Seçili konum kaydedilecek.", "Selected location will be saved."),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        } else if (value.isNotBlank() && required) {
            Text(
                theme.t(
                    "Devam etmek için aşağıdaki sonuçlardan bir konum seç.",
                    "Select a location from the results below to continue."
                ),
                color = Sinema.textSecondary(theme),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        }

        if (candidates.isNotEmpty()) {
Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .sinemaCard(theme, RoundedCornerShape(20.dp))
            ) {
                candidates.forEachIndexed { index, candidate ->
                    val isResolving = resolvingPlaceId == candidate.placeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sinemaClickable(enabled = resolvingPlaceId == null, scale = 0.98f, onClick = onClick@{
                                val client = placesClient ?: return@onClick
                                resolvingPlaceId = candidate.placeId
                                message = null
                                scope.launch {
                                    val resolved = resolvePlaceCandidate(client, sessionToken, candidate)
                                    resolvingPlaceId = null
                                    if (resolved?.lat != null && resolved.lng != null) {
                                        onPlaceSelected(resolved)
                                    } else {
                                        message = theme.t(
                                            "Konum koordinatı alınamadı; başka bir sonuç seç.",
                                            "Location coordinates could not be loaded; choose another result."
                                        )
                                    }
                                }
                            })
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
Icon(
                            Icons.Rounded.LocationOn,
                            null,
                            tint = Sinema.textSecondary(theme),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                candidate.title,
                                color = Sinema.text(theme),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (candidate.subtitle.isNotBlank()) {
                                Text(
                                    candidate.subtitle,
                                    color = Sinema.textSecondary(theme),
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
                    color = Sinema.textSecondary(theme).copy(0.82f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        } else if (message != null) {
            Text(
                message.orEmpty(),
                color = Sinema.textSecondary(theme),
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

/** Cam giriş (SinemaGlassField ile aynı kapsül dili; etiket FieldLabel'da). */
@Composable
private fun GlassInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    minLines: Int = 1,
    bold: Boolean = false
) {
    val theme = LocalAppTheme.current
    val multi = minLines > 1
    SinemaCapsule(
        modifier.fillMaxWidth(),
        height = if (multi) 96.dp else 52.dp,
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (multi) 14.dp else 0.dp),
        verticalAlignment = if (multi) Alignment.Top else Alignment.CenterVertically
    ) { fg ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = fg, fontSize = 16.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, lineHeight = 22.sp),
            cursorBrush = SolidColor(theme.effectiveAccentColor),
            singleLine = !multi,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, color = fg.copy(0.4f), fontSize = 16.sp, lineHeight = 22.sp, maxLines = 1)
                    }
                    inner()
                }
            }
        )
    }
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
    GlassInput(
        value, onValueChange, placeholder, Modifier.padding(horizontal = 16.dp),
        imeAction, keyboardType, visualTransformation, minLines
    )
}

/** Inline text input — Row hücrelerinde padding'siz; opsiyonel keyboardType + filter. */
@Composable
private fun TextInputInline(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    GlassInput(
        value = value,
        onValueChange = { v ->
            val filtered = if (keyboardType == KeyboardType.Number)
                v.filter { it.isDigit() || it == '.' || it == '-' }.take(12)
            else v
            onValueChange(filtered)
        },
        placeholder = placeholder, keyboardType = keyboardType
    )
}

@Composable
private fun NumberInputInline(value: String, onValueChange: (String) -> Unit) {
    GlassInput(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(6)) },
        placeholder = "0", keyboardType = KeyboardType.Number, bold = true
    )
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
    SinemaCapsule(Modifier.fillMaxWidth(), onClick = onClick, height = 52.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)) { fg ->
        Icon(icon, null, tint = fg.copy(0.7f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
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
    SportType.IndoorFootball,
    SportType.Volleyball,
    SportType.BasketballTennis -> ChallengeTargetType.TotalDurationMinutes
    else -> ChallengeTargetType.TotalDistanceM
}

private fun targetTypesForSport(sport: SportType): List<ChallengeTargetType> = when (sport) {
    SportType.YogaPilates,
    SportType.Boxing,
    SportType.JumpRopeHiit,
    SportType.Football,
    SportType.IndoorFootball,
    SportType.Volleyball,
    SportType.BasketballTennis -> listOf(ChallengeTargetType.TotalDurationMinutes)
    else -> listOf(
        ChallengeTargetType.TotalDistanceM,
        ChallengeTargetType.TotalDistanceKm,
        ChallengeTargetType.TotalDurationMinutes
    )
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
    SportType.IndoorFootball -> "60"
    SportType.Volleyball -> "45"
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
        SportType.IndoorFootball -> listOf("hali saha", "halisaha", "futsal", "football", "soccer", "futbol")
        SportType.Volleyball -> listOf("volleyball", "voleybol")
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
    SinemaCapsule(
        Modifier.fillMaxWidth().padding(vertical = 4.dp), selected = isActive, onClick = onClick,
        height = 52.dp
    ) { fg ->
        Icon(iconForTargetType(type), null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            type.displayLabel(theme), color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 1
        )
        Text(type.displayUnit(theme), color = fg.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun movementSpecLabel(movement: MovementInput, theme: com.cosmibit.profitness.core.theme.AppThemeState): String = buildString {
    val hasStrength = movement.suggestedSets != null || movement.suggestedReps != null
    if (hasStrength) {
        movement.suggestedSets?.let { append("${it} set") }
        movement.suggestedReps?.let {
            if (isNotEmpty()) append(" · ")
            append("${it} ${theme.t("tekrar", "reps")}")
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
    SinemaCapsule(
        modifier, selected = isActive, onClick = onClick, height = 48.dp,
        horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 10.dp)
    ) { fg ->
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1)
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
    SinemaCapsule(
        modifier, selected = isActive, onClick = onClick, height = 48.dp,
        horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 6.dp)
    ) { fg ->
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, maxLines = 1)
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
    SinemaCapsule(
        modifier, selected = isActive, onClick = onClick, height = 48.dp,
        horizontalArrangement = Arrangement.Center, contentPadding = PaddingValues(horizontal = 10.dp)
    ) { fg ->
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, maxLines = 1)
    }
}


/** Seçilen türe göre sahne fotoğrafı kategorisi (liste/detay ile aynı kategori anahtarları). */
private fun createPhotoCategory(
    isMetric: Boolean,
    metricType: ChallengeTargetType,
    mode: EventMode,
    sport: SportType
): String = when {
    isMetric -> when (metricType) {
        ChallengeTargetType.TotalDistanceM,
        ChallengeTargetType.TotalDistanceKm,
        ChallengeTargetType.TotalDurationMinutes -> "Kardiyo"
        ChallengeTargetType.TotalWorkouts,
        ChallengeTargetType.MovementsCompleted -> "Göğüs"
        else -> ""
    }
    mode == EventMode.MovementList -> "Göğüs"
    sport == SportType.Strength -> "Göğüs"
    sport == SportType.YogaPilates -> "Core"
    else -> "Kardiyo"
}

@Composable
private fun CreateSceneBars() {
    val theme = LocalAppTheme.current
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}
