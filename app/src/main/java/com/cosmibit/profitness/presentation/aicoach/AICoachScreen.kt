package com.cosmibit.profitness.presentation.aicoach

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.data.ai.ChatSession
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.presentation.components.AiCreditInfoRow
import com.cosmibit.profitness.presentation.components.PremiumButton
import com.cosmibit.profitness.presentation.components.floatingGlassSurface
import com.cosmibit.profitness.presentation.components.insetControlSurface
import com.cosmibit.profitness.presentation.store.PaywallDialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ── Data ─────────────────────────────────────────────────────────────────────

private val MESSAGE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class ChatMessage(
    val id: String = "",
    val text: String,
    val isUser: Boolean,
    val timestamp: String = LocalTime.now().format(MESSAGE_TIME_FORMATTER)
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    bottomPadding    : Dp = 0.dp,
    onNavigateToStore: () -> Unit = {},
    viewModel        : AICoachViewModel = hiltViewModel(),
    trainingSummary: OracleTrainingSummary? = null
) {
    val theme     = LocalAppTheme.current
    val strings   = theme.strings
    val isEnglish = theme.language == AppLanguage.ENGLISH
    val responsive = rememberResponsiveLayoutInfo()

    var showPaywall by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Tek-seferlik event'leri dinle (paywall tetikleyici)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AICoachEvent.ShowPaywall -> showPaywall = true
            }
        }
    }

    // İlk açılış veya ayarlar → onboarding/prefs ekranı
    if (state.showOnboarding) {
        val currentPrefs = viewModel.loadCurrentPrefs()
        AICoachOnboardingScreen(
            initialPrefs  = currentPrefs,
            bottomPadding = bottomPadding,
            onCancel      = if (currentPrefs.onboardingCompleted) {
                viewModel::closePreferencesIfCompleted
            } else {
                null
            },
            onComplete    = viewModel::completeOnboarding
        )
        return
    }

    var inputText         by remember { mutableStateOf("") }
    var programDialogMsg  by remember { mutableStateOf<ChatMessage?>(null) }
    var programNameInput  by remember { mutableStateOf("") }
    val listState         = rememberLazyListState()
    val scope             = rememberCoroutineScope()

    LaunchedEffect(state.programStatus) {
        when (state.programStatus) {
            is ProgramStatus.Success -> {
                programDialogMsg = null
                programNameInput = ""
                delay(3000)
                viewModel.resetProgramStatus()
            }
            else -> {}
        }
    }

    LaunchedEffect(strings.oracleWelcome) {
        viewModel.initWelcome(strings.oracleWelcome)
    }

    LaunchedEffect(state.messages.size, state.isLoading, state.userPlan, state.isBillingLoaded) {
        if (state.messages.size > 1 || state.isLoading) {
            val typingOffset = if (state.isLoading) 1 else 0
            listState.animateScrollToItem(
                state.messages.lastIndex + typingOffset + if (state.messages.size <= 1) 1 else 0
            )
        }
    }

    val quickChips = listOf(strings.chipRecovery, strings.chipProgram, strings.chipNutrition)

    fun sendMessage(text: String) {
        if (text.isBlank() || state.isLoading) return
        inputText = ""
        viewModel.sendMessage(text)
    }

    BackHandler(
        enabled = showPaywall ||
            state.showHistory ||
            programDialogMsg != null ||
            state.programStatus is ProgramStatus.Success
    ) {
        when {
            showPaywall -> showPaywall = false
            programDialogMsg != null && state.programStatus !is ProgramStatus.Loading -> {
                programDialogMsg = null
                viewModel.resetProgramStatus()
            }
            state.programStatus is ProgramStatus.Success -> viewModel.resetProgramStatus()
            state.showHistory -> viewModel.closeHistory()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
            .drawWithCache {
                val atmosphere = Brush.radialGradient(
                    listOf(
                        theme.effectiveAccentColor.copy(if (theme.isDark) 0.045f else 0.022f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.18f),
                    radius = size.width * 0.92f
                )
                onDrawBehind { drawRect(atmosphere) }
            }
            .imePadding()           // keyboard resizes this Box from the bottom
    ) {
        // ── Message feed fills full Box, has bottom padding for the input area ─
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val inputAreaHeight = if (imeVisible) {
            if (responsive.isLargeFont) 112.dp else 88.dp
        } else if (state.messages.size <= 1) {
            (if (responsive.isLargeFont) 124.dp else 104.dp) + bottomPadding
        } else {
            (if (responsive.isLargeFont) 172.dp else 148.dp) + bottomPadding
        }
        LazyColumn(
            state           = listState,
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(top = 92.dp, bottom = inputAreaHeight),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.messages.size <= 1) {
                item(key = "performance_intelligence") {
                    PerformanceIntelligenceOverview(trainingSummary, onAction = ::sendMessage)
                }
            }
            items(if (state.messages.size > 1) state.messages else emptyList(), key = { it.id }) { msg ->
                SanctuaryMessage(
                    msg            = msg,
                    onApplyProgram = { programDialogMsg = it; programNameInput = "" }
                )
            }
            if (state.isLoading) {
                item(key = "typing") { SanctuaryTypingIndicator() }
            }
        }

        // ── Input + chips pinned to bottom of the (keyboard-adjusted) Box ────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (!imeVisible) Modifier.navigationBarsPadding() else Modifier)
                .padding(bottom = if (imeVisible) 8.dp else bottomPadding + 8.dp)
        ) {
            AnimatedVisibility(
                visible = !imeVisible && state.messages.size > 1,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(bottom = 10.dp)
                ) {
                    items(quickChips) { chip ->
                        val chipTheme = LocalAppTheme.current
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipTheme.bg1)
                                .clickable(enabled = !state.isLoading) { sendMessage(chip) }
                                .padding(14.dp, 7.dp)
                        ) {
                            Text(
                                chip,
                                color         = chipTheme.text1,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Light,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }

            SanctuaryInput(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = { sendMessage(inputText) },
                isTyping      = state.isLoading
            )
        }

        // ── App Logo / Title (en üstte render edilmeli — touch almak için) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Transparent)
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
        ) {
            val hasPlan = state.userPlan != UserPlan.FREE

            // Sol: Geçmiş + Enerji / plan rozeti
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.openHistory() },
                    modifier = Modifier
                        .size(42.dp)
                        .performanceControlSurface(theme, theme.bg2, false, RoundedCornerShape(13.dp))
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = theme.t("Geçmiş", "History"),
                        tint     = theme.text0,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .performanceControlSurface(theme, theme.bg2, false, RoundedCornerShape(13.dp))
                        .clickable { onNavigateToStore() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (hasPlan) Icons.Rounded.AllInclusive else Icons.Rounded.Bolt,
                            null,
                            tint     = if (hasPlan) CardPurple else Forge500,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (hasPlan) state.userPlan.displayName.uppercase()
                            else if (state.isBillingLoaded) "${state.aiCredits}" else "...",
                            color         = if (hasPlan) CardPurple else Forge500,
                            fontSize      = 12.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Orta: Başlık
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.Center)
            ) {
                Text(
                    "Oracle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.45).sp,
                    color = theme.text0
                )
            }

            // Sağ: Yeni Sohbet + Ayarlar
            Row(
                modifier          = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.startNewSession()
                        viewModel.initWelcome(strings.oracleWelcome)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .performanceControlSurface(theme, theme.bg2, false, RoundedCornerShape(13.dp))
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = theme.t("Yeni Sohbet", "New Chat"),
                        tint     = theme.text0,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.openPreferences() },
                    modifier = Modifier
                        .size(42.dp)
                        .performanceControlSurface(theme, theme.bg2, false, RoundedCornerShape(13.dp))
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = theme.t("Ayarlar", "Settings"),
                        tint     = theme.text0,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Paywall Dialog
            if (showPaywall) {
                PaywallDialog(
                    onDismiss   = { showPaywall = false },
                    onGoToStore = {
                        showPaywall = false
                        onNavigateToStore()
                    }
                )
            }
        }

        // ── Program Oluşturma Dialog ──────────────────────────────────────────
        val pMsg = programDialogMsg
        if (pMsg != null) {
            AlertDialog(
                onDismissRequest = {
                    if (state.programStatus !is ProgramStatus.Loading) {
                        programDialogMsg = null
                        viewModel.resetProgramStatus()
                    }
                },
                containerColor = theme.bg1,
                title = {
                    Text(theme.t("Programı Planlarıma Ekle", "Add Program to My Plans"), color = theme.text1, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column {
                        when (val ps = state.programStatus) {
                            is ProgramStatus.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(theme.t("Program oluşturuluyor...", "Creating program..."), color = theme.text2, fontSize = 13.sp)
                                }
                            }
                            is ProgramStatus.Error -> {
                                Text(theme.ui(ps.msg), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                                ProgramNameField(value = programNameInput, onValueChange = { programNameInput = it }, theme = theme)
                            }
                            else -> {
                                Text(
                                    theme.t(
                                        "Oracle'ın oluşturduğu program planlarına eklenecek.",
                                        "The program created by Oracle will be added to your plans."
                                    ),
                                    color = theme.text2,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                ProgramNameField(value = programNameInput, onValueChange = { programNameInput = it }, theme = theme)
                            }
                        }
                    }
                },
                confirmButton = {
                    if (state.programStatus !is ProgramStatus.Loading) {
                        TextButton(
                            onClick = {
                                val name = programNameInput.trim().ifEmpty { theme.t("Oracle Programı", "Oracle Program") }
                                viewModel.applyProgram(pMsg.text, name)
                            }
                        ) {
                            Text(theme.t("Oluştur", "Create"), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                dismissButton = {
                    if (state.programStatus !is ProgramStatus.Loading) {
                        TextButton(onClick = {
                            programDialogMsg = null
                            viewModel.resetProgramStatus()
                        }) {
                            Text(theme.t("İptal", "Cancel"), color = theme.text2)
                        }
                    }
                }
            )
        }

        // ── Başarı Banner ─────────────────────────────────────────────────────
        val ps = state.programStatus
        if (ps is ProgramStatus.Success) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 90.dp)
                    .shadow(
                        elevation = if (theme.isDark) 10.dp else 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = if (theme.isDark) Color.Black.copy(0.44f) else Color(0xFF64748B).copy(0.10f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(if (theme.isDark) 0.16f else 0.08f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (theme.isDark) theme.bg2 else theme.bg1)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.6f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.resetProgramStatus() }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        theme.t(
                            "\"${ps.name}\" planlarına eklendi! Plan sekmesinden görebilirsin.",
                            "\"${ps.name}\" was added to your plans. You can find it in the Plan tab."
                        ),
                        color = theme.text0,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Geçmiş Sohbetler Bottom Sheet ────────────────────────────────────
        if (state.showHistory) {
            ModalBottomSheet(
                onDismissRequest  = { viewModel.closeHistory() },
                containerColor    = theme.bg1,
                scrimColor        = Color.Black.copy(0.6f),
                dragHandle        = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.stroke)
                    )
                }
            ) {
                SessionHistorySheet(
                    sessions        = state.sessions,
                    currentId       = state.currentSessionId,
                    onSelectSession = { viewModel.loadSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onNewChat       = {
                        viewModel.startNewSession()
                        viewModel.initWelcome(strings.oracleWelcome)
                    },
                    bottomPadding   = bottomPadding
                )
            }
        }

    }
}

// ── Sub-Components ────────────────────────────────────────────────────────────

@Composable
private fun PerformanceIntelligenceOverview(summary: OracleTrainingSummary?, onAction: (String) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val heroShape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .performanceSignatureSurface(theme, accent, heroShape)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            theme.t("Bugünkü plan", "Today's plan"),
                            color = theme.text0,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        summary?.let { data ->
                            Text(
                                theme.t(
                                    "${data.trainingDays} gün · ${data.exercises} hareket · ${data.completed} tamamlandı",
                                    "${data.trainingDays} days · ${data.exercises} exercises · ${data.completed} completed"
                                ),
                                color = theme.text1,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } ?: Text(
                            theme.t("Programını birlikte netleştirelim.", "Let's refine your program."),
                            color = theme.text1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PremiumButton(
            text = theme.t("Planımı değerlendir", "Review my plan"),
            onClick = { onAction(theme.t("Bugünkü antrenmanımı optimize et", "Optimize today's workout")) },
            leadingIcon = Icons.Rounded.AutoAwesome,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProgramNameField(value: String, onValueChange: (String) -> Unit, theme: AppThemeState) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(theme.t("Program Adı", "Program Name"), fontSize = 12.sp) },
        placeholder   = { Text(theme.t("Oracle Programı", "Oracle Program"), fontSize = 12.sp, color = theme.text2.copy(0.5f)) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = theme.stroke,
            focusedTextColor     = theme.text1,
            unfocusedTextColor   = theme.text1,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor  = theme.text2
        )
    )
}

@Composable
private fun SanctuaryMessage(
    msg            : ChatMessage,
    onApplyProgram : (ChatMessage) -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(msg.id) { mutableStateOf(false) }
    val showProgramButton = !msg.isUser && msg.id != "welcome" && looksLikeProgram(msg.text)

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }

    if (msg.isUser) {
        // ── User bubble — right aligned, solid accent fill ──────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                        .background(theme.bg2)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(
                        text       = msg.text,
                        color      = theme.text0,
                        fontSize   = 15.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = msg.timestamp,
                    color      = theme.text2.copy(0.45f),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    } else {
        // ── Oracle bubble — left aligned, glass dark panel + accent left border ─
        Row(
            modifier          = Modifier.fillMaxWidth().padding(start = 16.dp, end = 56.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Oracle avatar
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 8.dp)
                    .size(30.dp)
                    .background(theme.bg2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(14.dp))
            }

            Column(horizontalAlignment = Alignment.Start) {
                // Sender label
                Text(
                    "Oracle",
                    color         = theme.text1,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                    modifier      = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Glass bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(
                            if (theme.isDark) Color(0xFF10141B)
                            else Color(0xFFF7F9FC)
                        )
                        .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 11.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text          = msg.text,
                            color         = theme.text0.copy(0.92f),
                            fontSize      = 14.sp,
                            lineHeight    = 22.sp,
                            fontWeight    = FontWeight.Normal,
                            letterSpacing = 0.sp
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    modifier              = Modifier.padding(start = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = msg.timestamp,
                        color      = theme.text2.copy(0.40f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Light
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            copied = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = theme.t("Mesajı kopyala", "Copy message"),
                            tint = if (copied) accent else theme.text2.copy(0.62f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (showProgramButton) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(theme.bg2)
                                .clickable { onApplyProgram(msg) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(11.dp))
                                Text(theme.t("Planlarıma Ekle", "Add to Plans"), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun looksLikeProgram(text: String): Boolean {
    val lower = text.lowercase()

    // 1. En az 2 farklı gün işaretçisi varsa kesin program (set/tekrar olmasa bile)
    val dayNumbers = listOf("1", "2", "3", "4", "5", "6", "7")
    val gunCount = dayNumbers.count { n -> lower.contains("gün $n") || lower.contains("day $n") }
    if (gunCount >= 2) return true

    // 2. Set + tekrar içeren yapılandırılmış liste
    val hasSetInfo = (lower.contains("set") || lower.contains("sets")) &&
                     (lower.contains("tekrar") || lower.contains("rep"))
    val hasStructure =
        lower.contains("gün")       ||
        lower.contains("day")       ||
        lower.contains("push")      ||
        lower.contains("pull")      ||
        lower.contains("legs")      ||
        lower.contains("program")   ||
        lower.contains("antrenman") ||
        lower.contains("workout")   ||
        lower.contains("egzersiz")  ||
        lower.contains("•")
    if (hasSetInfo && hasStructure) return true

    // 3. "PROGRAM NOTLARI" gibi başlık içeriyorsa program
    if (lower.contains("program notları") || lower.contains("program notes")) return true

    return false
}

@Composable
private fun SanctuaryInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape  = RoundedCornerShape(22.dp)
    val responsive = rememberResponsiveLayoutInfo()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .insetControlSurface(accent = accent, theme = theme, shape = shape)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        theme.stroke.copy(if (theme.isDark) 0.88f else 0.62f),
                        Color.White.copy(if (theme.isDark) 0.035f else 0.68f)
                    )
                ),
                shape
            )
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .performanceControlSurface(
                    theme,
                    accent.copy(if (theme.isDark) 0.16f else 0.10f),
                    false,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(5.dp))
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(
                    theme.t("Oracle'a sor...", "Ask Oracle..."),
                    color = theme.text2.copy(0.72f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor        = theme.text0,
                unfocusedTextColor      = theme.text0,
                selectionColors         = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(0.2f)),
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier       = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            singleLine     = false,
            maxLines       = if (responsive.isShortScreen) 3 else 4
        )

        val sendActive = value.isNotBlank() && !isTyping
        val sendInteraction = remember { MutableInteractionSource() }
        val sendPressed by sendInteraction.collectIsPressedAsState()
        val sendScale by animateFloatAsState(
            if (sendPressed && sendActive) 0.98f else 1f,
            tween(120),
            label = "oracle_send_scale"
        )
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(44.dp)
                .graphicsLayer {
                    scaleX = sendScale
                    scaleY = sendScale
                }
                .performanceControlSurface(
                    theme,
                    if (sendActive) accent else theme.bg3,
                    sendPressed && sendActive,
                    RoundedCornerShape(14.dp)
                )
                .clickable(
                    enabled = sendActive,
                    interactionSource = sendInteraction,
                    indication = null,
                    onClick = onSend
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint     = if (sendActive) theme.effectiveOnAccentColor else theme.text2.copy(0.72f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Geçmiş Sohbetler Sheet ────────────────────────────────────────────────────

@Composable
private fun SessionHistorySheet(
    sessions        : List<ChatSession>,
    currentId       : String,
    onSelectSession : (ChatSession) -> Unit,
    onDeleteSession : (String) -> Unit,
    onNewChat       : () -> Unit,
    bottomPadding   : Dp = 0.dp
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }

    // Silme onay dialogu
    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = theme.bg1,
            title = { Text(theme.t("Sohbeti Sil", "Delete Chat"), color = theme.text1, fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    theme.t(
                        "\"${target.title}\" sohbeti silinecek. Bu işlem geri alınamaz.",
                        "\"${target.title}\" chat will be deleted. This cannot be undone."
                    ),
                    color = theme.text2, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(target.id); deleteTarget = null }) {
                    Text(theme.t("Sil", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(theme.t("İptal", "Cancel"), color = theme.text2)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding + 8.dp)
    ) {
        // Başlık + Yeni Sohbet butonu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                theme.t("GEÇMİŞ SOHBETLER", "CHAT HISTORY"),
                style        = MaterialTheme.typography.labelSmall,
                color        = accent,
                letterSpacing = 3.sp,
                fontSize     = 10.sp,
                fontWeight   = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(0.12f))
                    .border(1.dp, accent.copy(0.3f), RoundedCornerShape(20.dp))
                    .clickable { onNewChat() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint     = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        theme.t("Yeni Sohbet", "New Chat"),
                        color    = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = theme.stroke.copy(0.4f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(4.dp))

        if (sessions.isEmpty()) {
            // Boş durum
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint     = theme.text2.copy(0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        theme.t("Henüz kaydedilmiş sohbet yok", "No saved chats yet"),
                        color    = theme.text2.copy(0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxWidth(),
                contentPadding  = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isActive = session.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isActive)
                                    Modifier.background(accent.copy(0.1f)).border(1.dp, accent.copy(0.25f), RoundedCornerShape(12.dp))
                                else
                                    Modifier.background(theme.bg0.copy(0.5f))
                            )
                            .clickable { onSelectSession(session) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text     = session.title,
                                color    = if (isActive) accent else theme.text1,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text     = formatSessionDate(session.updatedAt, theme),
                                color    = theme.text2.copy(0.6f),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick  = { deleteTarget = session },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = theme.t("Sil", "Delete"),
                                tint     = theme.text2.copy(0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
}
}

private fun formatSessionDate(millis: Long, theme: AppThemeState): String {
    val now        = System.currentTimeMillis()
    val diffMs     = now - millis
    val diffHours  = diffMs / (1000 * 60 * 60)
    val diffDays   = diffMs / (1000 * 60 * 60 * 24)
    return when {
        diffHours < 1   -> theme.t("Az önce", "Just now")
        diffHours < 24  -> theme.t("${diffHours}s önce", "${diffHours}h ago")
        diffDays  == 1L -> theme.t("Dün", "Yesterday")
        diffDays  < 7   -> theme.t("${diffDays} gün önce", "${diffDays} days ago")
        else            -> SimpleDateFormat("d MMM", if (theme.language == AppLanguage.ENGLISH) Locale.ENGLISH else Locale("tr")).format(Date(millis))
    }
}

// ── Typing Indicator ──────────────────────────────────────────────────────────

@Composable
private fun SanctuaryTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val dot0 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 0),   RepeatMode.Reverse), label = "d0")
    val dot1 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 160), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 320), RepeatMode.Reverse), label = "d2")

    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current

    Row(
        modifier          = Modifier.padding(start = 16.dp, top = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp, end = 8.dp)
                .size(30.dp)
                .drawBehind {
                    drawCircle(color = accent.copy(0.18f))
                    drawCircle(brush = Brush.verticalGradient(listOf(Color.White.copy(0.15f), Color.Transparent), 0f, size.height))
                }
                .border(0.8.dp, accent.copy(0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(14.dp))
        }

        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (theme.isDark) 8.dp else 6.dp,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    clip = false,
                    ambientColor = if (theme.isDark) Color.Black.copy(0.36f) else Color(0xFF64748B).copy(0.09f),
                    spotColor = if (theme.isDark) accent.copy(0.10f) else Color(0xFF64748B).copy(0.10f)
                )
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .drawBehind {
                    drawRect(color = if (theme.isDark) theme.bg2 else theme.bg1)
                    drawRect(color = accent.copy(0.55f), topLeft = Offset(0f, 0f), size = Size(2.dp.toPx(), size.height))
                }
                .border(
                    0.8.dp,
                    if (theme.isDark) accent.copy(0.25f) else theme.stroke,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                listOf(dot0, dot1, dot2).forEach { alpha ->
                    val dotY by rememberInfiniteTransition(label = "y").animateFloat(
                        initialValue    = 0f,
                        targetValue     = -4f,
                        animationSpec   = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label           = "dot_y"
                    )
                    Box(
                        Modifier
                            .offset(y = (dotY * alpha).dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha))
                    )
                }
            }
        }
    }
}
