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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Data ─────────────────────────────────────────────────────────────────────

private val MESSAGE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class ChatMessage(
    val id: String = "",
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val timestamp: String = LocalTime.now().format(MESSAGE_TIME_FORMATTER)
)

// ── Responses Logic (Preserved but UI simplified) ───────────────────────────

private val CANNED_RESPONSES: Map<String, List<String>> = mapOf(
    "beslenme" to listOf(
        "Protein alımın hedef vücut ağırlığının kg başına 1.8–2.2g olmalı. Günlük kalori açığını %15–20 ile sınırlı tut — daha fazlası kas kaybına yol açar.",
        "Antrenman öncesi (~90 dk) kompleks karbonhidrat + protein: yulaf + yumurta ideal. Antrenman sonrası 30 dakika içinde yüksek GI karbonhidrat + whey dene.",
        "Kreatin monohidrat 5g/gün kanıtlanmış en güvenli takviyedir. Yükleme fazı şart değil — 3–4 haftada doygunluğa ulaşır. Bol su iç."
    ),
    "motivasyon" to listOf(
        "Motivasyon bir his değil, bir disiplindir. Hissetmesen de yap — vücut sinyali beyin sinyalinden önce gelir. Salona gir, gerisi otomatik.",
        "En zor set ilk rep'tir. Sonraki her rep öncekini mümkün kılıyor. Ağırlık seni yenemiyor, ancak sen kendin kendini yenebilirsin.",
        "Progresif yükleme prensibine dön: her hafta ya bir kg ekle ya bir rep yap. Küçük kazanımlar 12 haftada dönüşüme kapı açar."
    ),
    "program" to listOf(
        "Hedefin hipertrofi ise Push-Pull-Legs 6 günlük split optimal. Her grup haftada 2x çalışıyor, toplam 12–20 set/grup yeterli. Önce compound hareketler, sonra izolasyon.",
        "Başlangıç seviyesi için Full Body 3 günlük program daha etkili — frekans teknikten önemli. Squat, Press, Hinge, Pull, Carry — bu 5 hareket kategorisini kap.",
        "Upper/Lower split dengeli güç ve hacim için ideal. Alt gün squat dominant, üst gün press dominant. 4 gün üst düzey adaptasyon sağlar."
    )
)

private val DEFAULT_RESPONSES = listOf(
    "Oracle bu soruyu analiz ediyor... Liquid Forge protokolüne göre: spesifik, ölçülebilir ve zaman çerçeveli bir hedef belirlemek ilk adımdır.",
    "Bu iyi bir soru. Cevap birçok değişkene bağlı: başlangıç seviyeniz, genetik yanıt hızınız ve tutarlılığınız."
)

private val QUICK_CHIPS = listOf(
    "Beslenme Tavsiyesi", "Motivasyon", "Program Önerisi", "Toparlanma", "HIIT vs LISS"
)

private fun getResponse(input: String, counters: MutableMap<String, Int>): String {
    val lower = input.lowercase()
    val category = when {
        "beslenme" in lower || "protein" in lower || "yemek" in lower -> "beslenme"
        "motivasyon" in lower || "motive" in lower -> "motivasyon"
        "program" in lower || "split" in lower || "plan" in lower -> "program"
        else -> "default"
    }
    return if (category == "default") DEFAULT_RESPONSES.random() else {
        val list = CANNED_RESPONSES[category] ?: DEFAULT_RESPONSES
        val idx = (counters[category] ?: 0) % list.size
        counters[category] = idx + 1
        list[idx]
    }
}

// ── Composable: AICoachScreen (Oracle Sanctuary) ──────────────────────────

@Composable
fun AICoachScreen(bottomPadding: Dp = 0.dp) {
    val messages = remember { mutableStateListOf(ChatMessage(id="w", text="Oracle Sanctuary'ye Hoş Geldiniz. Performans hedeflerini analiz etmeye hazırım.", isUser=false)) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val responseCounters = remember { mutableStateMapOf<String, Int>() }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(id = System.currentTimeMillis().toString(), text = text, isUser = true))
        inputText = ""
        isTyping = true
        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            delay(1500)
            isTyping = false
            messages.add(ChatMessage(id = (System.currentTimeMillis() + 1).toString(), text = getResponse(text, responseCounters), isUser = false))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val theme = LocalAppTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        // ── Accent Background Bloom ───────────────────────────────────────────
        PageAccentBloom()

        // ── Navigation / App Logo ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp).align(Alignment.Center)) {
                Text("ORACLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 6.sp, fontWeight = FontWeight.ExtraLight)
                Text("SANCTUARY", style = MaterialTheme.typography.labelSmall, color = Snow.copy(0.4f), letterSpacing = 2.sp, fontSize = 8.sp)
            }
        }

        // ── Message Feed ─────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 120.dp, 0.dp, bottomPadding + 200.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                SanctuaryMessage(msg)
            }
            if (isTyping) {
                item { SanctuaryTypingIndicator() }
            }
        }

        // ── Bottom Section: Floating Input ───────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(0.dp, 0.dp, 0.dp, bottomPadding + 24.dp)
                .fillMaxWidth()
        ) {
            // Quick Suggestion Chips (Minimalist)
            LazyRow(
                contentPadding = PaddingValues(24.dp, 0.dp, 24.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp)
            ) {
                items(QUICK_CHIPS) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface1.copy(0.5f))
                            .border(1.dp, WhiteGlow, RoundedCornerShape(12.dp))
                            .clickable { sendMessage(chip) }
                            .padding(16.dp, 8.dp)
                    ) {
                        Text(chip, color = Mist, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
                    }
                }
            }

            // Input Field (Floating Glass)
            Box(modifier = Modifier.padding(24.dp, 0.dp)) {
                SanctuaryInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = { sendMessage(inputText) },
                    isTyping = isTyping
                )
            }
        }
    }
}

// ── Sub-Components: Sanctuary Aesthetic ──────────────────────────────────────


@Composable
private fun SanctuaryMessage(msg: ChatMessage) {
    val arrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    val textAlign = if (msg.isUser) Alignment.End else Alignment.Start

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
                            Modifier.background(Brush.linearGradient(listOf(accentC.copy(0.15f), accentC.copy(0.05f))))
                                .border(1.dp, accentC.copy(0.2f), RoundedCornerShape(20.dp))
                        } else Modifier
                    )
                    .padding(if (msg.isUser) 16.dp else 0.dp, if (msg.isUser) 12.dp else 0.dp, if (msg.isUser) 16.dp else 0.dp, if (msg.isUser) 12.dp else 0.dp)
            ) {
                Text(
                    text = msg.text,
                    color = if (msg.isUser) Snow else Snow.copy(0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    fontWeight = if (msg.isUser) FontWeight.Normal else FontWeight.Light,
                    letterSpacing = if (msg.isUser) 0.sp else 0.5.sp
                )
            }
            
            Spacer(Modifier.height(6.dp))
            Text(
                text = msg.timestamp,
                color = TextMuted.copy(0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraLight,
                modifier = Modifier.padding(start = 4.dp).padding(end = 4.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Surface1.copy(0.4f))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(20.dp, 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Sanctuary'ye sor...", color = Mist, fontSize = 14.sp, fontWeight = FontWeight.Light) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Snow,
                    unfocusedTextColor = Snow,
                    selectionColors = TextSelectionColors(handleColor = MaterialTheme.colorScheme.primary, backgroundColor = MaterialTheme.colorScheme.primary.copy(0.2f)),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent)
                    .clickable(enabled = value.isNotBlank() && !isTyping) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    null,
                    tint = if (value.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SanctuaryTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val dot0 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse), label = "d0")
    val dot1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d2")

    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.padding(24.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ORACLE DÜŞÜNÜYOR", style = MaterialTheme.typography.labelSmall, color = accent.copy(0.6f), letterSpacing = 2.sp, fontSize = 8.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(dot0, dot1, dot2).forEach { alpha ->
                Box(Modifier.size(4.dp).clip(CircleShape).background(accent.copy(alpha)))
            }
        }
    }
}
