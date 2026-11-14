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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
            .imePadding()           // keyboard resizes this Box from the bottom
    ) {
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val chatting = state.messages.size > 1
        val bottomGap = if (imeVisible) 8.dp else bottomPadding   // çizim: msg alt boşluğu = menü 60 + 16

        // Sahne düz degrade: bulanıklaştırmak görünür fark yaratmaz, bu yüzden Haze kaynağı YOK
        // (balon/hap camları aynı dolguyla çizilir; kaydırmada balon başına blur maliyeti kalkar).
        Box(Modifier.fillMaxSize()) { SinemaScene() }

        CompositionLocalProvider(LocalAppTheme provides theme) {
            // ── Sohbet akışı (konuşma başladıysa) ─────────────────────────────
            if (chatting || state.isLoading) {
                LazyColumn(
                    state           = listState,
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(
                        top = statusTop + 72.dp,
                        bottom = bottomGap + 52.dp + (if (imeVisible) 12.dp else 58.dp)
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(if (chatting) state.messages else emptyList(), key = { it.id }) { msg ->
                        Box(Modifier.animateItem()) {
                            SanctuaryMessage(
                                msg            = msg,
                                onApplyProgram = { programDialogMsg = it; programNameInput = "" }
                            )
                        }
                    }
                    if (state.isLoading) {
                        item(key = "typing") { SanctuaryTypingIndicator() }
                    }
                }
            } else {
                // ── Karşılama (çizim `.aict`): Bugünkü plan + dev sayaç + ilerleme + ana düğme ──
                AnimatedVisibility(
                    visible = !imeVisible,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.padding(top = statusTop + 152.dp)
                ) {
                    OracleTodayBlock(trainingSummary, onAction = ::sendMessage)
                }
            }

            // ── Alt yığın: hızlı soru kapsülleri + mesaj çubuğu ───────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomGap)
            ) {
                if (!imeVisible) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        quickChips.forEach { chip ->
                            SinemaCapsule(
                                modifier = Modifier.weight(1f),
                                onClick = { inputText = chip }, enabled = !state.isLoading,
                                height = 40.dp, shape = RoundedCornerShape(20.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center
                            ) { fg ->
                                Text(
                                    chip.substringBefore(" "), color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                SanctuaryInput(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    onSend        = { sendMessage(inputText) },
                    isTyping      = state.isLoading
                )
            }

            // ── Üst satır (çizim `.prow`): Oracle + cam hap(lar) ────────────
            val hasPlan = state.userPlan != UserPlan.FREE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 20.dp)
                    .padding(horizontal = 16.dp)
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(SinemaIcons.Spark, null, tint = theme.accent3Color, modifier = Modifier.size(16.dp))
                    Text("AI Koç", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                // Çizimde yalnız kredi hapı var; mevcut işlevler (geçmiş, yeni sohbet, ayarlar) aynı cam hap diliyle yanında
                SinemaPill(onClick = { viewModel.openHistory() }) {
                    Icon(Icons.Rounded.History, theme.t("Geçmiş", "History"), tint = Color.White, modifier = Modifier.size(16.dp))
                }
                SinemaPill(onClick = {
                    viewModel.startNewSession()
                    viewModel.initWelcome(strings.oracleWelcome)
                }) {
                    Icon(Icons.Rounded.Add, theme.t("Yeni Sohbet", "New Chat"), tint = Color.White, modifier = Modifier.size(16.dp))
                }
                SinemaPill(onClick = { viewModel.openPreferences() }) {
                    Icon(Icons.Rounded.Settings, theme.t("Ayarlar", "Settings"), tint = Color.White, modifier = Modifier.size(16.dp))
                }
                SinemaPill(onClick = onNavigateToStore) {
                    Icon(SinemaIcons.Bolt, null, tint = theme.accent3Color, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (hasPlan) state.userPlan.displayName
                        else if (state.isBillingLoaded) theme.t("${state.aiCredits} kredi", "${state.aiCredits} credits") else "…",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
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

        // ── Program Oluşturma Dialog ──────────────────────────────────────────
        val pMsg = programDialogMsg
        if (pMsg != null) {
            val loadingProgram = state.programStatus is ProgramStatus.Loading
            com.cosmibit.profitness.presentation.components.SinemaDialog(
                onDismiss = {
                    if (!loadingProgram) {
                        programDialogMsg = null
                        viewModel.resetProgramStatus()
                    }
                },
                title = theme.t("Programı Planlarıma Ekle", "Add Program to My Plans"),
                body = when (state.programStatus) {
                    is ProgramStatus.Loading, is ProgramStatus.Error -> ""
                    else -> theme.t(
                        "AI Koç'un oluşturduğu program planlarına eklenecek.",
                        "The program created by AI Coach will be added to your plans."
                    )
                },
                confirmLabel = theme.t("Oluştur", "Create"),
                confirmIcon = Icons.Rounded.Add,
                onConfirm = {
                    val name = programNameInput.trim().ifEmpty { theme.t("AI Koç Programı", "AI Coach Program") }
                    viewModel.applyProgram(pMsg.text, name)
                },
                dismissLabel = theme.t("İptal", "Cancel"),
                showActions = !loadingProgram,
                content = {
                    when (val ps = state.programStatus) {
                        is ProgramStatus.Loading -> {
                            Text(theme.t("Program oluşturuluyor...", "Creating program..."), color = Sinema.textSecondary(theme), fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            com.cosmibit.profitness.presentation.components.SinemaSkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
                            Spacer(Modifier.height(8.dp))
                            com.cosmibit.profitness.presentation.components.SinemaSkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp))
                        }
                        is ProgramStatus.Error -> {
                            Text(theme.ui(ps.msg), color = theme.supportAlert, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            ProgramNameField(value = programNameInput, onValueChange = { programNameInput = it }, theme = theme)
                        }
                        else -> {
                            Spacer(Modifier.height(12.dp))
                            ProgramNameField(value = programNameInput, onValueChange = { programNameInput = it }, theme = theme)
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
                        elevation = 10.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color.Black.copy(0.44f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(0.16f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.bg2)
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
                containerColor    = Sinema.page(theme),
                scrimColor        = Color.Black.copy(0.6f),
                dragHandle        = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Sinema.line(theme))
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

/** Çizim `.aict`: eyebrow + 104sp sayaç + ilerleme çubuğu + özet satırı + ana düğme. Veri yoksa sayaç/çubuk gösterilmez. */
@Composable
private fun OracleTodayBlock(summary: OracleTrainingSummary?, onAction: (String) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            theme.t("Bugünkü plan", "Today's plan").sinemaUpper(theme), color = Color.White.copy(0.75f), style = Sinema.Label
        )
        if (summary != null && summary.exercises > 0) {
            val frac = (summary.completed.toFloat() / summary.exercises).coerceIn(0f, 1f)
            val fill by animateFloatAsState(frac, tween(900, easing = SinemaMotion.EaseStandard), label = "oracle_fill")
            Spacer(Modifier.height(6.dp))
            SinemaCount(summary.completed) { n ->
                Text("$n/${summary.exercises}", style = Sinema.Display, color = Color.White, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(0.28f))) {
                Box(
                    Modifier.fillMaxWidth(fill).fillMaxHeight()
                        .clip(CircleShape).background(theme.accent2Color)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)) {
                Text(
                    theme.t("${summary.trainingDays} gün · ${summary.exercises} hareket", "${summary.trainingDays} days · ${summary.exercises} exercises"),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                )
                Text(
                    theme.t("${summary.exercises - summary.completed} kaldı", "${summary.exercises - summary.completed} left"),
                    color = Color.White.copy(0.75f), fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                theme.t("Programını birlikte netleştirelim.", "Let's refine your program."),
                color = Color.White, style = Sinema.Title
            )
            Spacer(Modifier.height(18.dp))
        }
        SinemaGoButton(
            label = theme.t("Planımı değerlendir", "Review my plan"),
            icon = SinemaIcons.Spark,
            onClick = { onAction(theme.t("Bugünkü antrenmanımı optimize et", "Optimize today's workout")) }
        )
    }
}

@Composable
private fun ProgramNameField(value: String, onValueChange: (String) -> Unit, theme: AppThemeState) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(theme.t("Program Adı", "Program Name"), fontSize = 12.sp) },
        placeholder   = { Text(theme.t("AI Koç Programı", "AI Coach Program"), fontSize = 12.sp, color = theme.text2.copy(0.5f)) },
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
                val userShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
                Box(
                    modifier = Modifier
                        .sinemaGlow(accent.copy(0.25f), 14.dp, 5.dp, userShape)
                        .clip(userShape)
                        .background(accent)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(
                        text       = msg.text,
                        color      = theme.effectiveOnAccentColor,
                        fontSize   = 15.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = msg.timestamp,
                    color      = Color.White.copy(0.5f),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium
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
                    .sinemaCapsuleGlass(CircleShape, blur = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(14.dp))
            }

            Column(horizontalAlignment = Alignment.Start) {
                // Sender label
                Text(
                    "AI Koç",
                    color         = Color.White.copy(0.7f),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    modifier      = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Glass bubble
                Box(
                    modifier = Modifier
                        .sinemaCapsuleGlass(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp), blur = 16.dp)
                        .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 11.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text          = msg.text,
                            color         = Color.White,
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
                        color      = Color.White.copy(0.5f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
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
                            tint = if (copied) accent else Color.White.copy(0.62f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (showProgramButton) {
                        Box(
                            modifier = Modifier
                                .sinemaCapsuleGlass(CircleShape, blur = 16.dp)
                                .clickable(role = Role.Button) { onApplyProgram(msg) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.FitnessCenter, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(11.dp))
                                Text(theme.t("Planlarıma Ekle", "Add to Plans"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

/** Çizim `.msg`: 52dp cam kapsül, 14sp girdi, sağda 38dp vurgu gönder düğmesi. */
@Composable
private fun SanctuaryInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean
) {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val responsive = rememberResponsiveLayoutInfo()
    val sendActive = value.isNotBlank() && !isTyping
    SinemaCapsule(
        modifier = Modifier.fillMaxWidth(),
        height = 52.dp, shape = RoundedCornerShape(26.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 7.dp, top = 7.dp, bottom = 7.dp)
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    theme.t("AI Koç'a bir şey sor…", "Ask AI Coach anything…"),
                    color = Color.White.copy(0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = if (responsive.isShortScreen) 3 else 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(8.dp))
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed && sendActive) 0.92f else 1f, tween(SinemaMotion.Quick), label = "oracle_send_scale")
        Box(
            Modifier
                .size(38.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(accent)
                .clickable(enabled = sendActive, interactionSource = src, indication = null, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(SinemaIcons.Send, theme.t("Gönder", "Send"), tint = theme.effectiveOnAccentColor, modifier = Modifier.size(18.dp))
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
    var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }

    // Silme onay dialogu
    val target = deleteTarget
    if (target != null) {
        com.cosmibit.profitness.presentation.components.SinemaDialog(
            onDismiss = { deleteTarget = null },
            title = theme.t("Sohbeti Sil", "Delete Chat"),
            body = theme.t(
                "\"${target.title}\" sohbeti silinecek. Bu işlem geri alınamaz.",
                "\"${target.title}\" chat will be deleted. This cannot be undone."
            ),
            confirmLabel = theme.t("Sil", "Delete"),
            confirmIcon = Icons.Rounded.DeleteOutline,
            onConfirm = { onDeleteSession(target.id); deleteTarget = null },
            dismissLabel = theme.t("İptal", "Cancel")
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
                theme.t("Geçmiş sohbetler", "Chat history").sinemaUpper(theme),
                style = Sinema.Label,
                color = Sinema.textSecondary(theme)
            )
            SinemaCapsule(
                selected = true, onClick = onNewChat, height = 36.dp,
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) { fg ->
                Icon(Icons.Rounded.Add, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(theme.t("Yeni Sohbet", "New Chat"), color = fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(4.dp))

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                com.cosmibit.profitness.presentation.components.SinemaEmptyState(
                    icon = Icons.Rounded.History,
                    title = theme.t("Henüz kaydedilmiş sohbet yok", "No saved chats yet")
                )
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxWidth(),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isActive = session.id == currentId
                    val rowShape = RoundedCornerShape(20.dp)
                    val accent = theme.effectiveAccentColor
                    val fg = if (isActive) theme.effectiveOnAccentColor else Sinema.text(theme)
                    val fg2 = if (isActive) theme.effectiveOnAccentColor.copy(0.7f) else Sinema.textSecondary(theme)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isActive) Modifier.sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, rowShape) else Modifier)
                            .then(if (isActive) Modifier.clip(rowShape).background(accent) else Modifier.sinemaCard(theme, rowShape))
                            .clickable(role = Role.Button) { onSelectSession(session) }
                            .heightIn(min = 56.dp)
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text     = session.title,
                                color    = fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text     = formatSessionDate(session.updatedAt, theme),
                                color    = fg2,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick  = { deleteTarget = session },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = theme.t("Sil", "Delete"),
                                tint     = fg2,
                                modifier = Modifier.size(20.dp)
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
    // Tek sonsuz geçiş (faz 0..1); değer yalnız çizim aşamasında okunur → her karede yeniden kompozisyon yok.
    // Hareket azaltmada geçiş hiç kurulmaz (noktalar sabit).
    val motionOn = rememberSinemaMotionOn()
    val phase = if (motionOn) {
        rememberInfiniteTransition(label = "dots").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "dots_phase"
        )
    } else null

    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier          = Modifier.padding(start = 16.dp, top = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp, end = 8.dp)
                .size(30.dp)
                .sinemaCapsuleGlass(CircleShape, blur = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(14.dp))
        }

        Box(
            modifier = Modifier
                .sinemaCapsuleGlass(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp), blur = 16.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val lift = with(LocalDensity.current) { 4.dp.toPx() }
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                // nokta başı 0.16 faz kayması; üçgen dalga 0..1..0, yumuşatılmış
                                val t = phase?.value?.let { ((it - i * 0.16f) % 1f + 1f) % 1f }
                                val wave = if (t == null) 0.6f else (1f - kotlin.math.abs(t * 2f - 1f)).let { w -> w * w * (3f - 2f * w) }
                                alpha = 0.25f + 0.75f * wave
                                translationY = if (t == null) 0f else -lift * wave
                            }
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
        }
    }
}
