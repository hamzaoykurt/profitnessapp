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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Data ─────────────────────────────────────────────────────────────────────

private val MESSAGE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class ChatMessage(
    val id: String = "",
    val text: String,
    val isUser: Boolean,
    val timestamp: String = LocalTime.now().format(MESSAGE_TIME_FORMATTER)
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AICoachScreen(
    bottomPadding: Dp = 0.dp,
    viewModel: AICoachViewModel = hiltViewModel()
) {
    val theme     = LocalAppTheme.current
    val strings   = theme.strings
    val isEnglish = theme.language == AppLanguage.ENGLISH

    val state     by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

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

        // ── App Logo / Title ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(0.dp, 16.dp, 0.dp, 0.dp)
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
                    color = LocalAppTheme.current.text2,
                    letterSpacing = 2.sp,
                    fontSize = 8.sp
                )
            }
        }

        // ── Message Feed ─────────────────────────────────────────────────────
        val inputAreaHeight = 160.dp
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 120.dp, 0.dp, bottomPadding + inputAreaHeight + imeBottom),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                SanctuaryMessage(msg)
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
    }
}

// ── Sub-Components ────────────────────────────────────────────────────────────

@Composable
private fun SanctuaryMessage(msg: ChatMessage) {
    val arrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    val textAlign   = if (msg.isUser) Alignment.End   else Alignment.Start

    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp, 0.dp),
        horizontalArrangement = arrangement
    ) {
        Column(horizontalAlignment = textAlign, modifier = Modifier.widthIn(max = 300.dp)) {
            if (!msg.isUser) {
                Text(
                    "ORACLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(4.dp, 0.dp, 4.dp, 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(if (msg.isUser) 20.dp else 12.dp))
                    .then(
                        if (msg.isUser) {
                            val accentC = MaterialTheme.colorScheme.primary
                            Modifier
                                .background(Brush.linearGradient(listOf(accentC.copy(0.15f), accentC.copy(0.05f))))
                                .border(1.dp, accentC.copy(0.2f), RoundedCornerShape(20.dp))
                        } else Modifier
                    )
                    .padding(
                        horizontal = if (msg.isUser) 16.dp else 0.dp,
                        vertical   = if (msg.isUser) 12.dp else 0.dp
                    )
            ) {
                Text(
                    text       = msg.text,
                    color      = if (msg.isUser) Snow else Snow.copy(0.9f),
                    style      = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    fontWeight = if (msg.isUser) FontWeight.Normal else FontWeight.Light,
                    letterSpacing = if (msg.isUser) 0.sp else 0.5.sp
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text         = msg.timestamp,
                color        = TextMuted.copy(0.5f),
                fontSize     = 9.sp,
                fontWeight   = FontWeight.ExtraLight,
                modifier     = Modifier.padding(start = 4.dp).padding(end = 4.dp)
            )
        }
    }
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
