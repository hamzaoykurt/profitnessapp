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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    bottomPadding: Dp = 0.dp,
    viewModel: AICoachViewModel = hiltViewModel()
) {
    val theme     = LocalAppTheme.current
    val strings   = theme.strings
    val isEnglish = theme.language == AppLanguage.ENGLISH

    val state     by viewModel.uiState.collectAsStateWithLifecycle()

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

    // Program oluşturma sonucu: dialog kapat veya hata göster; başarıda 3sn sonra banner kapat
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

    // Hoş geldin mesajını dil değişimine duyarlı biçimde başlat
    LaunchedEffect(strings.oracleWelcome) {
        viewModel.initWelcome(strings.oracleWelcome)
    }

    // Yeni mesaj gelince en alta kaydır
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

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        // ── Accent Background Bloom ───────────────────────────────────────────
        PageAccentBloom()

        // ── Message Feed ─────────────────────────────────────────────────────
        val inputAreaHeight = 160.dp
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 120.dp, 0.dp, bottomPadding + inputAreaHeight + imeBottom),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                SanctuaryMessage(
                    msg           = msg,
                    onApplyProgram = { programDialogMsg = it; programNameInput = "" }
                )
            }
            if (state.isLoading) {
                item(key = "typing") { SanctuaryTypingIndicator() }
            }
        }

        // ── Bottom: Quick Chips + Input ───────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 16.dp + imeBottom)
                .fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(quickChips) { chip ->
                    val chipTheme = LocalAppTheme.current
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipTheme.bg1.copy(0.85f))
                            .border(1.dp, chipTheme.stroke.copy(0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !state.isLoading) { sendMessage(chip) }
                            .padding(16.dp, 8.dp)
                    ) {
                        Text(
                            chip,
                            color = chipTheme.text1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            SanctuaryInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { sendMessage(inputText) },
                isTyping = state.isLoading
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

            // Sağ: Yeni Sohbet + Ayarlar
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
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
    val arrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    val textAlign   = if (msg.isUser) Alignment.End   else Alignment.Start
    val accent      = MaterialTheme.colorScheme.primary
    val theme       = LocalAppTheme.current

    // Oracle mesajı program içeriyor mu? (Gün + set/tekrar/rep kelimesi içeriyorsa)
    val showProgramButton = !msg.isUser && msg.id != "welcome" &&
        looksLikeProgram(msg.text)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = arrangement
    ) {
        Column(
            horizontalAlignment = textAlign,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (!msg.isUser) {
                Text(
                    "ORACLE",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = accent,
                    letterSpacing = 2.sp,
                    fontSize  = 9.sp,
                    modifier  = Modifier.padding(4.dp, 0.dp, 4.dp, 6.dp)
                )
            }

            if (msg.isUser) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(accent.copy(0.15f), accent.copy(0.05f))))
                        .border(1.dp, accent.copy(0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text       = msg.text,
                        color      = Snow,
                        style      = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                Text(
                    text          = msg.text,
                    color         = Snow.copy(0.9f),
                    style         = MaterialTheme.typography.bodyLarge,
                    lineHeight    = 26.sp,
                    fontWeight    = FontWeight.Light,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Saat
            Text(
                text       = msg.timestamp,
                color      = TextMuted.copy(0.75f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Light,
                modifier   = Modifier.padding(horizontal = 4.dp)
            )

            // Program uygula butonu
            if (showProgramButton) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(0.1f))
                        .border(1.dp, accent.copy(0.3f), RoundedCornerShape(20.dp))
                        .clickable { onApplyProgram(msg) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = accent, modifier = Modifier.size(13.dp))
                        Text("Planlarıma Ekle", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
    val shape  = RoundedCornerShape(28.dp)

    val borderBrush = Brush.horizontalGradient(
        listOf(
            accent.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.10f),
            accent.copy(alpha = 0.35f)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(
                elevation    = 24.dp,
                shape        = shape,
                clip         = false,
                spotColor    = accent.copy(alpha = 0.30f),
                ambientColor = Color.Black.copy(alpha = 0.60f)
            )
            .clip(shape)
            .drawWithCache {
                val bgBase      = theme.bg0.copy(alpha = 0.82f)
                val topMirror   = Brush.verticalGradient(colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.09f),
                    0.30f to Color.White.copy(alpha = 0.02f),
                    0.55f to Color.Transparent
                ))
                val accentBleed = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to accent.copy(alpha = 0.18f),
                        0.28f to accent.copy(alpha = 0.09f),
                        0.58f to accent.copy(alpha = 0.03f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(0f, size.height * 0.5f),
                    end   = Offset(size.width, size.height * 0.5f)
                )
                val depthShadow = Brush.verticalGradient(colorStops = arrayOf(
                    0.42f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.38f)
                ))
                onDrawBehind {
                    drawRect(bgBase)
                    drawRect(accentBleed)
                    drawRect(depthShadow)
                    drawRect(topMirror)
                }
            }
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
    val dot0 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 0),   RepeatMode.Reverse), label = "d0")
    val dot1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d2")

    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.padding(24.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "ORACLE DÜŞÜNÜYOR",
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(0.6f),
            letterSpacing = 2.sp,
            fontSize = 8.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(dot0, dot1, dot2).forEach { alpha ->
                Box(Modifier.size(4.dp).clip(CircleShape).background(accent.copy(alpha)))
            }
        }
    }
}
