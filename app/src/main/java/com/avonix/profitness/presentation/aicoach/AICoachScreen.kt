package com.avonix.profitness.presentation.aicoach

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.ai.ChatSession
import com.avonix.profitness.data.store.UserPlan
import com.avonix.profitness.presentation.components.AiCreditInfoRow
import com.avonix.profitness.presentation.store.PaywallDialog
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
    viewModel        : AICoachViewModel = hiltViewModel()
) {
    val theme     = LocalAppTheme.current
    val strings   = theme.strings
    val isEnglish = theme.language == AppLanguage.ENGLISH

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
        AICoachOnboardingScreen(
            initialPrefs  = viewModel.loadCurrentPrefs(),
            bottomPadding = bottomPadding,
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

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val quickChips = listOf(
        strings.chipNutrition, strings.chipMotivation, strings.chipProgram,
        strings.chipRecovery,  strings.chipHiitVsLiss
    )

    fun sendMessage(text: String) {
        if (text.isBlank() || state.isLoading) return
        inputText = ""
        viewModel.sendMessage(text)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
            .imePadding()           // keyboard resizes this Box from the bottom
    ) {
        PageAccentBloom()

        // ── Message feed fills full Box, has bottom padding for the input area ─
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val inputAreaHeight = if (imeVisible) 72.dp else 148.dp + bottomPadding
        LazyColumn(
            state           = listState,
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(top = 116.dp, bottom = inputAreaHeight),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.userPlan == UserPlan.FREE) {
                item(key = "credit_info") {
                    AiCreditInfoRow(
                        isFree    = true,
                        credits   = state.aiCredits,
                        costLabel = "1 kredi / mesaj",
                        theme     = theme,
                        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            items(state.messages, key = { it.id }) { msg ->
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
                visible = !imeVisible,
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
                                .background(chipTheme.bg1.copy(0.85f))
                                .border(1.dp, chipTheme.stroke.copy(0.4f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !state.isLoading) { sendMessage(chip) }
                                .padding(14.dp, 7.dp)
                        ) {
                            Text(
                                chip,
                                color         = chipTheme.text1,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Light,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            SanctuaryInput(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = { sendMessage(inputText) },
                isTyping      = state.isLoading,
                isFree        = state.userPlan == UserPlan.FREE,
                credits       = state.aiCredits
            )
        }

        // ── App Logo / Title (en üstte render edilmeli — touch almak için) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
        ) {
            // Sol: Geçmiş
            IconButton(
                onClick  = { viewModel.openHistory() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = "Geçmiş",
                    tint     = theme.text2.copy(0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Orta: Başlık
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.Center)
            ) {
                Text(
                    "ORACLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraLight
                )
                Text(
                    "SANCTUARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.text2,
                    letterSpacing = 2.sp,
                    fontSize = 8.sp
                )
            }

            // Sağ: Kredi rozeti + Yeni Sohbet + Ayarlar
            Row(
                modifier          = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kredi / plan badge
                val hasPlan = state.userPlan != UserPlan.FREE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (hasPlan) CardPurple.copy(0.15f) else Forge500.copy(0.15f)
                        )
                        .border(
                            1.dp,
                            if (hasPlan) CardPurple.copy(0.4f) else Forge500.copy(0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onNavigateToStore() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (hasPlan) Icons.Rounded.AllInclusive else Icons.Rounded.Bolt,
                            null,
                            tint     = if (hasPlan) CardPurple else Forge500,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            if (hasPlan) state.userPlan.displayName.uppercase()
                            else "${state.aiCredits}",
                            color         = if (hasPlan) CardPurple else Forge500,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                IconButton(onClick = {
                    viewModel.startNewSession()
                    viewModel.initWelcome(strings.oracleWelcome)
                }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Yeni Sohbet",
                        tint     = theme.text2.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { viewModel.openPreferences() }) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Ayarlar",
                        tint     = theme.text2.copy(0.6f),
                        modifier = Modifier.size(18.dp)
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
                    Text("Programı Planlarıma Ekle", color = theme.text1, fontWeight = FontWeight.SemiBold)
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
                                    Text("Program oluşturuluyor...", color = theme.text2, fontSize = 13.sp)
                                }
                            }
                            is ProgramStatus.Error -> {
                                Text(ps.msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                                ProgramNameField(value = programNameInput, onValueChange = { programNameInput = it }, theme = theme)
                            }
                            else -> {
                                Text(
                                    "Oracle'ın oluşturduğu program planlarına eklenecek.",
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
                                val name = programNameInput.trim().ifEmpty { "Oracle Programı" }
                                viewModel.applyProgram(pMsg.text, name)
                            }
                        ) {
                            Text("Oluştur", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                dismissButton = {
                    if (state.programStatus !is ProgramStatus.Loading) {
                        TextButton(onClick = {
                            programDialogMsg = null
                            viewModel.resetProgramStatus()
                        }) {
                            Text("İptal", color = theme.text2)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A3A2F))
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
                        "\"${ps.name}\" planlarına eklendi! Plan sekmesinden görebilirsin.",
                        color = Snow,
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
private fun ProgramNameField(value: String, onValueChange: (String) -> Unit, theme: AppThemeState) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text("Program Adı", fontSize = 12.sp) },
        placeholder   = { Text("Oracle Programı", fontSize = 12.sp, color = theme.text2.copy(0.5f)) },
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
    val showProgramButton = !msg.isUser && msg.id != "welcome" && looksLikeProgram(msg.text)

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
                        .drawBehind {
                            drawRect(color = accent)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(0.22f), Color.Transparent),
                                    startY = 0f, endY = size.height * 0.5f
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(
                        text       = msg.text,
                        color      = Color.White,
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
                    .drawBehind {
                        val r  = size.minDimension / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        drawCircle(color = accent.copy(0.18f), radius = r, center = Offset(cx, cy))
                        drawCircle(
                            brush  = Brush.verticalGradient(
                                listOf(Color.White.copy(0.15f), Color.Transparent),
                                startY = 0f, endY = size.height
                            ),
                            radius = r, center = Offset(cx, cy)
                        )
                    }
                    .border(0.8.dp, accent.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(14.dp))
            }

            Column(horizontalAlignment = Alignment.Start) {
                // Sender label
                Text(
                    "ORACLE",
                    color         = accent,
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    modifier      = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Glass bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .drawBehind {
                            // Dark glass base — clip handles asymmetric corners
                            drawRect(color = Color(0xFF1A1A1A).copy(alpha = 0.82f))
                            // Top glass sheen
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(0.07f), Color.Transparent),
                                    startY = 0f, endY = size.height * 0.4f
                                )
                            )
                            // Left accent border line
                            drawRect(
                                color   = accent.copy(0.55f),
                                topLeft = Offset(0f, 0f),
                                size    = Size(2.dp.toPx(), size.height)
                            )
                        }
                        .border(
                            width = 0.8.dp,
                            brush = Brush.linearGradient(
                                listOf(accent.copy(0.30f), Color.White.copy(0.06f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        )
                        .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 11.dp)
                ) {
                    Text(
                        text          = msg.text,
                        color         = Snow.copy(0.92f),
                        fontSize      = 15.sp,
                        lineHeight    = 24.sp,
                        fontWeight    = FontWeight.Light,
                        letterSpacing = 0.2.sp
                    )
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
                    if (showProgramButton) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(accent.copy(0.12f))
                                .border(0.8.dp, accent.copy(0.35f), RoundedCornerShape(16.dp))
                                .clickable { onApplyProgram(msg) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(11.dp))
                                Text("Planlarıma Ekle", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
    isTyping: Boolean,
    isFree: Boolean = true,
    credits: Int = 0
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape  = RoundedCornerShape(28.dp)

    val borderBrush = Brush.horizontalGradient(
        listOf(
            accent.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.10f),
            accent.copy(alpha = 0.35f)
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isFree) {
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("1 kredi / mesaj", color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text("Kalan: $credits", color = theme.text2, fontSize = 10.sp)
            }
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .background(theme.bg0.copy(alpha = 0.92f))
            .border(1.dp, borderBrush, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text("Sanctuary'ye sor...", color = Mist.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Light)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor        = Snow,
                unfocusedTextColor      = Snow,
                selectionColors         = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(0.2f)),
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier       = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            singleLine     = false,
            maxLines       = 4
        )

        val sendActive = value.isNotBlank() && !isTyping
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (sendActive) Modifier
                        .background(Brush.radialGradient(listOf(accent.copy(0.25f), accent.copy(0.08f))))
                        .border(1.dp, accent.copy(0.45f), CircleShape)
                    else Modifier.background(Color.Transparent)
                )
                .clickable(enabled = sendActive) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint     = if (sendActive) accent else TextMuted.copy(0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    } // Column
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
            title = { Text("Sohbeti Sil", color = theme.text1, fontWeight = FontWeight.SemiBold) },
            text  = {
                Text(
                    "\"${target.title}\" sohbeti silinecek. Bu işlem geri alınamaz.",
                    color = theme.text2, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(target.id); deleteTarget = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("İptal", color = theme.text2)
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
                "GEÇMİŞ SOHBETLER",
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
                        "Yeni Sohbet",
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
                        "Henüz kaydedilmiş sohbet yok",
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
                                text     = formatSessionDate(session.updatedAt),
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
                                contentDescription = "Sil",
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

private fun formatSessionDate(millis: Long): String {
    val now        = System.currentTimeMillis()
    val diffMs     = now - millis
    val diffHours  = diffMs / (1000 * 60 * 60)
    val diffDays   = diffMs / (1000 * 60 * 60 * 24)
    return when {
        diffHours < 1   -> "Az önce"
        diffHours < 24  -> "${diffHours}s önce"
        diffDays  == 1L -> "Dün"
        diffDays  < 7   -> "${diffDays} gün önce"
        else            -> SimpleDateFormat("d MMM", Locale("tr")).format(Date(millis))
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
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .drawBehind {
                    drawRect(color = Color(0xFF1A1A1A).copy(alpha = 0.82f))
                    drawRect(color = accent.copy(0.55f), topLeft = Offset(0f, 0f), size = Size(2.dp.toPx(), size.height))
                }
                .border(0.8.dp, accent.copy(0.25f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
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
