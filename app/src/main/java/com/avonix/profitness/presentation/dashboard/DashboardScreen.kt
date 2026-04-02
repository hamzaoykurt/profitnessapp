package com.avonix.profitness.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.absoluteValue
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.aicoach.AICoachScreen
import com.avonix.profitness.presentation.news.NewsScreen
import com.avonix.profitness.presentation.profile.AchievementsDetailScreen
import com.avonix.profitness.presentation.profile.EditProfileScreen
import com.avonix.profitness.presentation.profile.PerformanceDetailScreen
import com.avonix.profitness.presentation.profile.ProfileScreen
import com.avonix.profitness.presentation.program.ProgramBuilderScreen
import com.avonix.profitness.presentation.workout.WorkoutScreen

sealed class DashboardTab(val route: String, val icon: ImageVector, val label: String) {
    object Workout : DashboardTab("workout",  Icons.Rounded.FitnessCenter, "FORGE")
    object Program : DashboardTab("program",  Icons.Rounded.CalendarMonth, "PLAN")
    object AICoach : DashboardTab("ai_coach", Icons.Rounded.AutoAwesome,   "ORACLE")
    object News    : DashboardTab("news",     Icons.Rounded.Newspaper,     "MUSE")
    object Profile : DashboardTab("profile",  Icons.Rounded.Person,        "USER")
}

private val ALL_TABS = listOf(
    DashboardTab.Workout, DashboardTab.Program, DashboardTab.AICoach,
    DashboardTab.News,    DashboardTab.Profile
)

@Composable
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit, onLogout: () -> Unit = {}) {
    var selectedTab             by remember { mutableStateOf<DashboardTab>(DashboardTab.Workout) }
    var programInitialMode      by remember { mutableStateOf<com.avonix.profitness.presentation.program.BuilderMode>(com.avonix.profitness.presentation.program.BuilderMode.Choose) }
    var showPerformanceDetail   by remember { mutableStateOf(false) }
    var showAchievementsDetail  by remember { mutableStateOf(false) }
    var showEditProfile         by remember { mutableStateOf(false) }

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp
    val haptic = LocalHapticFeedback.current

    // ── Swipe gesture — Orientation.Horizontal doesn't compete with vertical scrollers
    var swipeAccum by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta -> swipeAccum += delta }
    val swipeModifier = Modifier.draggable(
        state       = draggableState,
        orientation = Orientation.Horizontal,
        onDragStarted = { swipeAccum = 0f },
        onDragStopped = { velocity ->
            val curIdx = ALL_TABS.indexOf(selectedTab)
            // Fast fling (velocity) OR slow-but-wide drag both trigger tab change
            val byVelocity = abs(velocity) > 500f
            val byDistance = abs(swipeAccum) > 120f
            if (byVelocity || byDistance) {
                val goNext = if (byVelocity) velocity < 0f else swipeAccum < 0f
                if (goNext && curIdx < ALL_TABS.lastIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTab = ALL_TABS[curIdx + 1]
                } else if (!goNext && curIdx > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTab = ALL_TABS[curIdx - 1]
                }
            }
            swipeAccum = 0f
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(modifier = Modifier.fillMaxSize())

        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = {
                val fromIdx = ALL_TABS.indexOf(initialState)
                val toIdx   = ALL_TABS.indexOf(targetState)
                // Pure slide+fade — NO scale. Scale forces GPU layer changes every frame
                // on the full composable tree which is the #1 cause of jank during transitions.
                // Short durations (240/160ms) keep double-render window minimal.
                val easeOut    = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                val easeIn     = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)
                val enterSlide = tween<IntOffset>(240, easing = easeOut)
                val enterFade  = tween<Float>(200, easing = easeOut)
                val exitSlide  = tween<IntOffset>(160, easing = easeIn)
                val exitFade   = tween<Float>(120)
                if (toIdx > fromIdx) {
                    (slideInHorizontally(enterSlide) { it } + fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { -it / 4 } + fadeOut(exitFade))
                } else {
                    (slideInHorizontally(enterSlide) { -it } + fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { it / 4 } + fadeOut(exitFade))
                }
            },
            modifier = swipeModifier,
            label = "tab_slide"
        ) { tab ->
            when (tab) {
                DashboardTab.Workout -> WorkoutScreen(
                    bottomPadding = contentPad,
                    onNavigateToAIBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.AI
                        selectedTab = DashboardTab.Program
                    },
                    onNavigateToManualBuilder = {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.Manual
                        selectedTab = DashboardTab.Program
                    }
                )
                DashboardTab.Program -> {
                    val capturedMode = programInitialMode
                    // Modu kullandıktan sonra sıfırla (geri dönüşte Choose'a dönmesi için)
                    LaunchedEffect(capturedMode) {
                        programInitialMode = com.avonix.profitness.presentation.program.BuilderMode.Choose
                    }
                    ProgramBuilderScreen(initialMode = capturedMode)
                }
                DashboardTab.AICoach -> AICoachScreen(bottomPadding = contentPad)
                DashboardTab.News    -> NewsScreen()
                DashboardTab.Profile -> ProfileScreen(
                    onThemeChange            = onThemeChange,
                    onNavigateToPerformance  = { showPerformanceDetail = true },
                    onNavigateToAchievements = { showAchievementsDetail = true },
                    onLogout                 = onLogout,
                    onEditProfile            = { showEditProfile = true }
                )
            }
        }

        AppNavBar(
            tabs     = ALL_TABS,
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f)
        )

        // Shared spring spec for full-screen overlays — feels instant yet smooth
        val overlayEnterSpec = spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness    = Spring.StiffnessMediumLow
        )
        val overlayExitSpec = spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness    = Spring.StiffnessMedium
        )

        // ── Performance Detail Overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = showPerformanceDetail,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            PerformanceDetailScreen(onBack = { showPerformanceDetail = false })
        }

        // ── Achievements Detail Overlay ─────────────────────────────────
        AnimatedVisibility(
            visible = showAchievementsDetail,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            AchievementsDetailScreen(onBack = { showAchievementsDetail = false })
        }

        // ── Edit Profile Overlay ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showEditProfile,
            enter   = slideInHorizontally(overlayEnterSpec) { it } + fadeIn(tween(200)),
            exit    = slideOutHorizontally(overlayExitSpec) { it } + fadeOut(tween(150)),
            modifier = Modifier.zIndex(200f)
        ) {
            EditProfileScreen(onBack = { showEditProfile = false })
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  APP BACKGROUND
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    // Light mode: much subtler bloom — warm earthy bg doesn't need heavy neon glow
    val radialPeak = if (theme.isDark) 0.16f else 0.06f
    val radialMid  = if (theme.isDark) 0.10f else 0.03f
    val radialEdge = if (theme.isDark) 0.04f else 0.01f
    val sweepPeak  = if (theme.isDark) 0.07f else 0.03f
    val sweepMid   = if (theme.isDark) 0.02f else 0.005f

    Box(modifier = modifier.drawWithCache {
        val radial = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f  to accent.copy(alpha = radialPeak),
                0.30f to accent.copy(alpha = radialMid),
                0.60f to accent.copy(alpha = radialEdge),
                1.0f  to Color.Transparent
            ),
            center = Offset(size.width, 0f),
            radius = size.width * 2.2f
        )
        val sweep = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to accent.copy(alpha = sweepPeak),
                0.5f to accent.copy(alpha = sweepMid),
                1.0f to Color.Transparent
            ),
            start = Offset(size.width, 0f),
            end   = Offset(size.width * 0.3f, size.height)
        )
        onDrawBehind {
            drawRect(theme.bg0)
            drawRect(radial)
            drawRect(sweep)
        }
    })
}

// ═══════════════════════════════════════════════════════════════════════════
//  APP NAV BAR — Glass pill, pure-orb items, drag-to-select
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AppNavBar(
    tabs    : List<DashboardTab>,
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val shape  = RoundedCornerShape(50)

    val borderBrush = remember(accent) {
        Brush.horizontalGradient(
            listOf(
                accent.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.10f),
                accent.copy(alpha = 0.32f)
            )
        )
    }

    // Sliding accent indicator — animated float tracks selected index
    val selectedIdx       = tabs.indexOf(selected)
    val indicatorProgress by animateFloatAsState(
        targetValue   = selectedIdx.toFloat(),
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label         = "nav_indicator"
    )

    // Drag-to-select state
    var dragLastIdx by remember { mutableStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .shadow(
                    elevation    = 42.dp,
                    shape        = shape,
                    clip         = false,
                    spotColor    = accent.copy(alpha = if (theme.isDark) 0.50f else 0.22f),
                    ambientColor = if (theme.isDark) Color.Black.copy(alpha = 0.72f)
                                   else Color.Black.copy(alpha = 0.14f)
                )
                .clip(shape)
                .drawWithCache {
                    val bgBase = theme.bg0.copy(alpha = if (theme.isDark) 0.48f else 0.88f)
                    val topMirror = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (theme.isDark) 0.18f else 0.52f),
                            0.28f to Color.White.copy(alpha = if (theme.isDark) 0.04f else 0.12f),
                            0.50f to Color.Transparent
                        )
                    )
                    val accentBleed = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to accent.copy(alpha = if (theme.isDark) 0.26f else 0.10f),
                            0.28f to accent.copy(alpha = if (theme.isDark) 0.12f else 0.05f),
                            0.58f to accent.copy(alpha = if (theme.isDark) 0.04f else 0.01f),
                            1.00f to Color.Transparent
                        ),
                        start = Offset(0f, size.height * 0.5f),
                        end   = Offset(size.width, size.height * 0.5f)
                    )
                    val depthShadow = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.38f to Color.Transparent,
                            1.00f to if (theme.isDark) Color.Black.copy(0.45f) else Color(0xFF6B4E2A).copy(0.10f)
                        )
                    )
                    val rimLight = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (theme.isDark) 0.06f else 0.10f),
                            0.05f to Color.Transparent,
                            0.92f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = if (theme.isDark) 0.18f else 0.06f)
                        )
                    )
                    onDrawBehind {
                        drawRect(bgBase)
                        drawRect(accentBleed)
                        drawRect(depthShadow)
                        drawRect(topMirror)
                        drawRect(rimLight)

                        // ── Sliding accent bar under active icon ──────────────────
                        // Draw a thin pill that glides between icon positions
                        val tabW = size.width / tabs.size
                        val barW = tabW * 0.36f
                        val barH = 3.dp.toPx()
                        val barCx = (indicatorProgress + 0.5f) * tabW
                        val barY  = size.height - barH - 6.dp.toPx()
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = 0.20f),
                                    accent.copy(alpha = 0.90f),
                                    accent.copy(alpha = 0.20f)
                                ),
                                startX = barCx - barW / 2f,
                                endX   = barCx + barW / 2f
                            ),
                            topLeft     = Offset(barCx - barW / 2f, barY),
                            size        = Size(barW, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2f)
                        )
                    }
                }
                .border(1.dp, borderBrush, shape)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                // Drag-to-select: slide finger across icons without lifting
                .pointerInput(tabs) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ -> dragLastIdx = -1 },
                        onHorizontalDrag = { change, _ ->
                            val x   = change.position.x.coerceIn(0f, size.width.toFloat())
                            val idx = ((x / size.width) * tabs.size).toInt().coerceIn(0, tabs.lastIndex)
                            if (idx != dragLastIdx) {
                                dragLastIdx = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tabs[idx])
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                NavOrbItem(
                    tab        = tab,
                    isSelected = tab == selected,
                    accent     = accent,
                    theme      = theme,
                    onClick    = { onSelect(tab) }
                )
            }
        }
    }
}

// ─── Pure-orb nav item — no expanding text, just icon bloom + glow ───────────
@Composable
private fun NavOrbItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    onClick   : () -> Unit
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    // Orb and icon sizes animate on selection — the "bloom" effect IS the active indicator
    val orbSize  by animateFloatAsState(
        targetValue   = if (isSelected) 52f else 40f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "orb_size"
    )
    val iconSize by animateFloatAsState(
        targetValue   = if (isSelected) 26f else 20f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "icon_size"
    )
    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.82f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "orb_press"
    )

    Box(
        modifier = Modifier
            .scale(pressScale)
            .size(orbSize.dp)
            .drawBehind {
                val r  = size.minDimension / 2f
                val cx = size.width  / 2f
                val cy = size.height / 2f

                // Ambient glow halo (selected only)
                if (isSelected) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to accent.copy(alpha = 0.55f),
                                0.5f to accent.copy(alpha = 0.20f),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r * 1.9f
                        ),
                        radius = r * 1.9f,
                        center = Offset(cx, cy)
                    )
                }
                // Base fill
                drawCircle(
                    color  = if (isSelected) accent else theme.bg2.copy(alpha = 0.85f),
                    radius = r,
                    center = Offset(cx, cy)
                )
                // Convex lighting — top highlight, bottom shadow
                drawCircle(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = if (isSelected) 0.35f else 0.18f),
                            0.45f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = if (isSelected) 0.15f else 0.30f)
                        ),
                        startY = 0f, endY = size.height
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
                // Specular lens flare — upper-left highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = if (isSelected) 0.75f else 0.45f),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(size.width * 0.33f, size.height * 0.21f),
                        radius = r * 0.50f
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
            .border(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                brush = Brush.verticalGradient(
                    if (isSelected) listOf(Color.White.copy(0.55f), accent.copy(0.38f))
                    else             listOf(Color.White.copy(0.20f), theme.stroke.copy(0.15f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = tab.icon,
            contentDescription = tab.label,
            tint               = if (isSelected) theme.accent.onColor
                                  else            theme.text2.copy(alpha = 0.55f),
            modifier           = Modifier.size(iconSize.dp)
        )
    }
}
