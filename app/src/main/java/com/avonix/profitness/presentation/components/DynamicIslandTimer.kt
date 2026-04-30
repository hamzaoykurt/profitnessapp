package com.avonix.profitness.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.workout.RestTimerState
import kotlinx.coroutines.delay

// ╔══════════════════════════════════════════════════════════════════╗
// ║   DYNAMIC ISLAND TIMER — Neon Forge Design                      ║
// ║   Compact pill → Expanded arc timer, glassmorphism overlay      ║
// ╚══════════════════════════════════════════════════════════════════╝

/**
 * Uygulamanın üst kısmında yüzen, Dynamic Island stilinde rest timer.
 *
 * Davranış:
 *  - Timer başlayınca kompakt pill olarak belirir
 *  - Tıklayınca dairesel ilerleme çubuğu + egzersiz adıyla genişler
 *  - Timer bitince "Hazırsın!" animasyonu oynar, 4 sn sonra kaybolur
 *  - Tüm geçişler spring + tween animasyonlarıyla akıcı
 */
@Composable
fun DynamicIslandTimer(
    timer    : RestTimerState,
    topOffset: Dp = 48.dp,
    onStop   : () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val accent  = MaterialTheme.colorScheme.primary
    val haptic  = LocalHapticFeedback.current
    val theme   = LocalAppTheme.current

    // Expanded / collapsed toggle
    var isExpanded by remember { mutableStateOf(false) }

    // Timer bitince auto-dismiss
    LaunchedEffect(timer.isDone) {
        if (timer.isDone) {
            isExpanded = true          // tam zamanı göster
            delay(3_500)
            onDismiss()
        }
    }

    // Hiç aktif değilse gösterme
    if (!timer.isRunning && !timer.isDone) return

    val glowAlpha = when {
        timer.isDone -> 0.38f
        timer.isRunning -> 0.22f
        else -> 0f
    }

    // ── Arc progress ─────────────────────────────────────────────────────────
    val arcProgress by animateFloatAsState(
        targetValue   = if (timer.totalSeconds > 0) 1f - timer.progress else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "arc_progress"
    )

    // ── Island shape animation ────────────────────────────────────────────────
    val cornerRadius by animateDpAsState(
        targetValue   = if (isExpanded) 28.dp else 22.dp,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label         = "corner"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topOffset)
            .zIndex(10f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState   = isExpanded,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(280, easing = FastOutSlowInEasing), 0.88f)) togetherWith
                (fadeOut(tween(160)) + scaleOut(tween(200), 0.92f))
            },
            label = "island_content"
        ) { expanded ->
            if (expanded) {
                ExpandedIsland(
                    timer       = timer,
                    accent      = accent,
                    theme       = theme,
                    arcProgress = arcProgress,
                    glowAlpha   = glowAlpha,
                    cornerRadius = cornerRadius,
                    onCollapse  = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isExpanded = false
                    },
                    onStop      = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                        onDismiss()
                    }
                )
            } else {
                CompactPill(
                    timer       = timer,
                    accent      = accent,
                    theme       = theme,
                    glowAlpha   = glowAlpha,
                    onClick     = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isExpanded = true
                    }
                )
            }
        }
    }
}

// ── Compact Pill ──────────────────────────────────────────────────────────────
@Composable
private fun CompactPill(
    timer    : RestTimerState,
    accent   : Color,
    theme    : AppThemeState,
    glowAlpha: Float,
    onClick  : () -> Unit
) {
    val min = timer.secondsLeft / 60
    val sec = timer.secondsLeft % 60
    val timeStr = if (min > 0) "${min}:${sec.toString().padStart(2, '0')}"
                  else "${timer.secondsLeft}s"

    val pillColor = when {
        timer.isDone    -> Amber
        timer.isRunning -> accent
        else            -> accent
    }

    Box(modifier = Modifier.wrapContentSize()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            theme.bg2.copy(0.96f),
                            theme.bg1.copy(0.96f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            pillColor.copy(0.60f),
                            pillColor.copy(0.20f),
                            theme.stroke.copy(0.40f)
                        )
                    ),
                    RoundedCornerShape(22.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timer.isDone) {
                Text(
                    "💪  Hazırsın!",
                    color      = Amber,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            } else {
                Text(
                    timeStr,
                    color      = pillColor,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "·  ${timer.exerciseName}",
                    color      = TextSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1
                )
            }
        }
    }
}

// ── Expanded Island ───────────────────────────────────────────────────────────
@Composable
private fun ExpandedIsland(
    timer       : RestTimerState,
    accent      : Color,
    theme       : AppThemeState,
    arcProgress : Float,
    glowAlpha   : Float,
    cornerRadius: Dp,
    onCollapse  : () -> Unit,
    onStop      : () -> Unit
) {
    val pillColor = if (timer.isDone) Amber else accent
    val min = timer.secondsLeft / 60
    val sec = timer.secondsLeft % 60

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            // Outer glow halo
            .drawBehind {
                drawRoundRect(
                    color        = pillColor.copy(alpha = glowAlpha * 0.30f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                    blendMode    = BlendMode.Screen
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            theme.bg2.copy(0.97f),
                            theme.bg1.copy(0.97f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            pillColor.copy(0.55f),
                            theme.stroke.copy(0.35f),
                            pillColor.copy(0.25f)
                        )
                    ),
                    RoundedCornerShape(cornerRadius)
                )
                // Top rim light (Neon Forge signature)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    pillColor.copy(0.45f),
                                    Color.Transparent
                                )
                            ),
                            size = Size(size.width, 1.dp.toPx())
                        )
                    }
                }
                .clickable(onClick = onCollapse)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Timer,
                            null,
                            tint     = pillColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (timer.isDone) "DİNLENDİN" else "SET ARASI",
                            color      = pillColor,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.bg3)
                            .clickable(onClick = onStop),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint     = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Circular Arc Timer ─────────────────────────────────────────
                Box(
                    modifier        = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Arka plan halkası
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val radius = (size.minDimension - stroke) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Track
                        drawArc(
                            color      = theme.bg3,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            style      = Stroke(stroke, cap = StrokeCap.Round),
                            size       = Size(radius * 2, radius * 2),
                            topLeft    = Offset(center.x - radius, center.y - radius)
                        )

                        // Progress arc
                        if (arcProgress > 0f) {
                            drawArc(
                                brush      = Brush.sweepGradient(
                                    listOf(
                                        pillColor.copy(0.7f),
                                        pillColor,
                                        pillColor.copy(0.8f)
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = arcProgress * 360f,
                                useCenter  = false,
                                style      = Stroke(stroke, cap = StrokeCap.Round),
                                size       = Size(radius * 2, radius * 2),
                                topLeft    = Offset(center.x - radius, center.y - radius)
                            )

                            // Glow dot at arc tip
                            val angleRad = Math.toRadians((-90f + arcProgress * 360f).toDouble())
                            val dotX = center.x + radius * Math.cos(angleRad).toFloat()
                            val dotY = center.y + radius * Math.sin(angleRad).toFloat()
                            drawCircle(
                                color  = pillColor,
                                radius = stroke / 2f,
                                center = Offset(dotX, dotY)
                            )
                            drawCircle(
                                color  = pillColor.copy(0.3f),
                                radius = stroke * 1.2f,
                                center = Offset(dotX, dotY)
                            )
                        }
                    }

                    // Center content
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (timer.isDone) {
                            Text("✅", fontSize = 32.sp)
                            Text(
                                "HAZIRSIN",
                                color      = Amber,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        } else {
                            Text(
                                text = if (min > 0) "${min}:${sec.toString().padStart(2, '0')}"
                                       else "${timer.secondsLeft}",
                                color      = TextPrimary,
                                fontSize   = 38.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (min == 0) {
                                Text(
                                    "saniye",
                                    color      = TextMuted,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Exercise name ──────────────────────────────────────────────
                Text(
                    text       = timer.exerciseName,
                    color      = TextSecondary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(4.dp))

                // ── Hint ──────────────────────────────────────────────────────
                Text(
                    text = if (timer.isDone) "Sonraki seti başlatmak için hazırsın"
                           else "Kapat için dokun",
                    color      = TextMuted,
                    fontSize   = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Mini Arc Indicator (pill içi küçük daire) ─────────────────────────────────
@Composable
private fun MiniArcIndicator(
    progress: Float,
    color   : Color,
    size    : Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 2.5.dp.toPx()
        val radius = (this.size.minDimension - stroke) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        drawArc(
            color      = color.copy(0.25f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter  = false,
            style      = Stroke(stroke, cap = StrokeCap.Round),
            size       = Size(radius * 2, radius * 2),
            topLeft    = Offset(center.x - radius, center.y - radius)
        )
        if (progress > 0f) {
            drawArc(
                color      = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter  = false,
                style      = Stroke(stroke, cap = StrokeCap.Round),
                size       = Size(radius * 2, radius * 2),
                topLeft    = Offset(center.x - radius, center.y - radius)
            )
        }
    }
}
