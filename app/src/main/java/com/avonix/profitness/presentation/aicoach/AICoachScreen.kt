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

// ── Responses Logic ──────────────────────────────────────────────────────────

private val TR_CANNED: Map<String, List<String>> = mapOf(
    "nutrition" to listOf(
        "Protein alımın hedef vücut ağırlığının kg başına 1.8–2.2g olmalı. Günlük kalori açığını %15–20 ile sınırlı tut — daha fazlası kas kaybına yol açar.",
        "Antrenman öncesi (~90 dk) kompleks karbonhidrat + protein: yulaf + yumurta ideal. Antrenman sonrası 30 dakika içinde yüksek GI karbonhidrat + whey dene.",
        "Kreatin monohidrat 5g/gün kanıtlanmış en güvenli takviyedir. Yükleme fazı şart değil — 3–4 haftada doygunluğa ulaşır. Bol su iç."
    ),
    "motivation" to listOf(
        "Motivasyon bir his değil, bir disiplindir. Hissetmesen de yap — vücut sinyali beyin sinyalinden önce gelir. Salona gir, gerisi otomatik.",
        "En zor set ilk rep'tir. Sonraki her rep öncekini mümkün kılıyor. Ağırlık seni yenemiyor, ancak sen kendin kendini yenebilirsin.",
        "Progresif yükleme prensibine dön: her hafta ya bir kg ekle ya bir rep yap. Küçük kazanımlar 12 haftada dönüşüme kapı açar."
    ),
    "program" to listOf(
        "Hedefin hipertrofi ise Push-Pull-Legs 6 günlük split optimal. Her grup haftada 2x çalışıyor, toplam 12–20 set/grup yeterli. Önce compound hareketler, sonra izolasyon.",
        "Başlangıç seviyesi için Full Body 3 günlük program daha etkili — frekans teknikten önemli. Squat, Press, Hinge, Pull, Carry — bu 5 hareket kategorisini kap.",
        "Upper/Lower split dengeli güç ve hacim için ideal. Alt gün squat dominant, üst gün press dominant. 4 gün üst düzey adaptasyon sağlar."
    ),
    "recovery" to listOf(
        "Toparlanma antrenmanın yarısıdır. Uyku sırasında büyüme hormonu zirvesine ulaşır — 7–9 saat şart. Kalite düşükse tüm program çöker.",
        "Aktif toparlanma pasif dinlenmeden daha etkilidir. Hafif yürüyüş, mobilite çalışması ve yüzme, laktik asit temizlemeyi hızlandırır.",
        "Soğuk duş (2–5°C, 3–5 dk) iltihabı azaltır ve kas ağrısını kısaltır. Antrenman sonrası 1 saat içinde uygula."
    )
)

private val TR_DEFAULT = listOf(
    "Oracle bu soruyu analiz ediyor... Liquid Forge protokolüne göre: spesifik, ölçülebilir ve zaman çerçeveli bir hedef belirlemek ilk adımdır.",
    "Bu iyi bir soru. Cevap birçok değişkene bağlı: başlangıç seviyeniz, genetik yanıt hızınız ve tutarlılığınız."
)

private val EN_CANNED: Map<String, List<String>> = mapOf(
    "nutrition" to listOf(
        "Protein intake should be 1.8–2.2g per kg of target bodyweight. Keep your daily calorie deficit to 15–20% — more than that leads to muscle loss.",
        "Pre-workout (~90 min before): complex carbs + protein — oats and eggs are ideal. Post-workout within 30 minutes: high-GI carbs + whey protein.",
        "Creatine monohydrate 5g/day is the most proven safe supplement. Loading phase isn't necessary — saturation is reached in 3–4 weeks. Drink plenty of water."
    ),
    "motivation" to listOf(
        "Motivation is a discipline, not a feeling. Do it even when you don't feel like it — the body signal comes before the brain signal. Get in the gym; the rest is automatic.",
        "The hardest set is the first rep. Every subsequent rep makes the previous one possible. The weight can't beat you — only you can beat yourself.",
        "Return to progressive overload: add one kg or one rep each week. Small wins open the door to transformation in 12 weeks."
    ),
    "program" to listOf(
        "For hypertrophy, a 6-day Push-Pull-Legs split is optimal. Each group trains 2x per week; 12–20 sets/group is enough. Compound movements first, then isolation.",
        "For beginners, a 3-day Full Body programme is more effective — frequency matters more than technique. Cover Squat, Press, Hinge, Pull, Carry — these 5 movement patterns.",
        "Upper/Lower split is ideal for balanced strength and volume. Lower days are squat-dominant; upper days are press-dominant. 4 days provides top-level adaptation."
    ),
    "recovery" to listOf(
        "Recovery is half the training. Growth hormone peaks during sleep — 7–9 hours is non-negotiable. Poor sleep quality collapses the entire programme.",
        "Active recovery outperforms passive rest. Light walking, mobility work and swimming accelerate lactic acid clearance.",
        "Cold exposure (2–5°C, 3–5 min) reduces inflammation and shortens DOMS. Apply within 1 hour post-workout."
    )
)

private val EN_DEFAULT = listOf(
    "Oracle is analysing your question… According to the Liquid Forge protocol: setting a specific, measurable and time-bound goal is the first step.",
    "That's a good question. The answer depends on many variables: your starting level, genetic response rate and consistency."
)

private fun getResponse(
    input: String,
    isEnglish: Boolean,
    counters: MutableMap<String, Int>
): String {
    val lower   = input.lowercase()
    val canned  = if (isEnglish) EN_CANNED  else TR_CANNED
    val default = if (isEnglish) EN_DEFAULT else TR_DEFAULT

    val category = when {
        "nutrition" in lower || "protein" in lower || "diet" in lower ||
        "beslenme" in lower || "yemek"   in lower                       -> "nutrition"
        "motivation" in lower || "motivasyon" in lower                  -> "motivation"
        "program" in lower || "split" in lower || "plan" in lower       -> "program"
        "recovery" in lower || "toparlanma" in lower || "sleep" in lower ||
        "uyku"     in lower                                             -> "recovery"
        else -> "default"
    }

    return if (category == "default") default.random() else {
        val list = canned[category] ?: default
        val idx  = (counters[category] ?: 0) % list.size
        counters[category] = idx + 1
        list[idx]
    }
}

// ── Composable: AICoachScreen (Oracle Sanctuary) ──────────────────────────

@Composable
fun AICoachScreen(bottomPadding: Dp = 0.dp) {
    val theme     = LocalAppTheme.current
    val strings   = theme.strings
    val isEnglish = theme.language == com.avonix.profitness.core.theme.AppLanguage.ENGLISH

    val messages = remember { mutableStateListOf(ChatMessage(id = "w", text = strings.oracleWelcome, isUser = false)) }
    var inputText by remember { mutableStateOf("") }
    var isTyping  by remember { mutableStateOf(false) }
    val listState         = rememberLazyListState()
    val scope             = rememberCoroutineScope()
    val responseCounters  = remember { mutableStateMapOf<String, Int>() }

    val quickChips = listOf(
        strings.chipNutrition, strings.chipMotivation, strings.chipProgram,
        strings.chipRecovery,  strings.chipHiitVsLiss
    )

    // Update welcome message when language changes
    LaunchedEffect(isEnglish) {
        if (messages.isNotEmpty() && messages[0].id == "w") {
            messages[0] = messages[0].copy(text = strings.oracleWelcome)
        }
    }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(id = System.currentTimeMillis().toString(), text = text, isUser = true))
        inputText = ""
        isTyping = true
        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            delay(1500)
            isTyping = false
            messages.add(ChatMessage(
                id     = (System.currentTimeMillis() + 1).toString(),
                text   = getResponse(text, isEnglish, responseCounters),
                isUser = false
            ))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }
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
        // Bottom content padding expands when keyboard is open so messages stay visible
        val inputAreaHeight = 160.dp
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 120.dp, 0.dp, bottomPadding + inputAreaHeight + imeBottom),
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
        // imePadding + bottomPadding pushes input above keyboard and navbar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 16.dp + imeBottom)
                .fillMaxWidth()
        ) {
            // Quick Suggestion Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(quickChips) { chip ->
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

            // Input Field — styled like bottom navbar
            SanctuaryInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { sendMessage(inputText) },
                isTyping = isTyping
            )
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
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(28.dp)

    // Gradient border matching the navbar style
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
                // Same layered background as the navbar
                val bgBase = theme.bg0.copy(alpha = 0.82f)

                val topMirror = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.09f),
                        0.30f to Color.White.copy(alpha = 0.02f),
                        0.55f to Color.Transparent
                    )
                )

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

                val depthShadow = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.42f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.38f)
                    )
                )

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
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Sanctuary'ye sor...", color = Mist.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Light) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Snow,
                unfocusedTextColor = Snow,
                selectionColors = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(0.2f)),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            singleLine = false,
            maxLines = 4
        )

        // Send button with accent circle when active
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
                tint = if (sendActive) accent else TextMuted.copy(0.5f),
                modifier = Modifier.size(20.dp)
            )
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
