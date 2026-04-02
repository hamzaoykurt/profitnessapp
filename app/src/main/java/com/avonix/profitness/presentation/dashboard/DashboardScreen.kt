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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
                // easeOutExpo: explosive start → long natural settle (closest to iOS spring physics)
                // Parallax: new screen enters at full width, old screen exits at ¼ speed
                // Depth: new screen grows from 95% (arrives from "behind"), old shrinks to 97%
                val easeOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)   // easeOutExpo
                val easeIn  = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)    // easeInQuart — snappy exit
                val enterSlide = tween<IntOffset>(400, easing = easeOut)
                val enterScale = tween<Float>(420, easing = easeOut)
                val enterFade  = tween<Float>(280, easing = easeOut)
                val exitSlide  = tween<IntOffset>(320, easing = easeIn)
                val exitScale  = tween<Float>(280, easing = easeIn)
                val exitFade   = tween<Float>(200)
                if (toIdx > fromIdx) {
                    (slideInHorizontally(enterSlide) { it } +
                     scaleIn(initialScale = 0.95f, animationSpec = enterScale) +
                     fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { -it / 4 } +
                     scaleOut(targetScale = 0.97f, animationSpec = exitScale) +
                     fadeOut(exitFade))
                } else {
                    (slideInHorizontally(enterSlide) { -it } +
                     scaleIn(initialScale = 0.95f, animationSpec = enterScale) +
                     fadeIn(enterFade)) togetherWith
                    (slideOutHorizontally(exitSlide) { it / 4 } +
                     scaleOut(targetScale = 0.97f, animationSpec = exitScale) +
                     fadeOut(exitFade))
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
//  APP NAV BAR — Floating pill container with expanding selected item
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
                    // ① Deeply transparent frosted base — true glass feel
                    val bgBase = theme.bg0.copy(alpha = if (theme.isDark) 0.48f else 0.88f)

                    // ② Strong top-edge glass reflection — the primary glass cue
                    val topMirrorAlpha0 = if (theme.isDark) 0.18f else 0.52f
                    val topMirrorAlpha1 = if (theme.isDark) 0.04f else 0.12f
                    val topMirror = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = topMirrorAlpha0),
                            0.28f to Color.White.copy(alpha = topMirrorAlpha1),
                            0.50f to Color.Transparent
                        )
                    )

                    // ③ Accent bleed: left-side color signature
                    val accentBleedA0 = if (theme.isDark) 0.26f else 0.10f
                    val accentBleedA1 = if (theme.isDark) 0.12f else 0.05f
                    val accentBleedA2 = if (theme.isDark) 0.04f else 0.01f
                    val accentBleed = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to accent.copy(alpha = accentBleedA0),
                            0.28f to accent.copy(alpha = accentBleedA1),
                            0.58f to accent.copy(alpha = accentBleedA2),
                            1.00f to Color.Transparent
                        ),
                        start = Offset(0f, size.height * 0.5f),
                        end   = Offset(size.width, size.height * 0.5f)
                    )

                    // ④ Bottom inner shadow — depth under glass
                    val depthColor = if (theme.isDark) Color.Black.copy(alpha = 0.45f)
                                     else Color(0xFF6B4E2A).copy(alpha = 0.10f)
                    val depthShadow = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.38f to Color.Transparent,
                            1.00f to depthColor
                        )
                    )

                    // ⑤ Subtle inner rim light — separates glass panel from content
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
                    }
                }
                .border(1.dp, borderBrush, shape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                ExpandingNavItem(
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

// ─── Expanding pill nav item ─────────────────────────────────────────────────
@Composable
private fun ExpandingNavItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    onClick   : () -> Unit
) {
    val haptic             = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "nav_item_scale"
    )

    val selectedBrush = remember(theme.bg3, theme.bg2) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to theme.bg3.copy(alpha = 0.72f),
                1.0f to theme.bg2.copy(alpha = 0.82f)
            )
        )
    }

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .then(
                if (isSelected) Modifier.background(brush = selectedBrush, shape = RoundedCornerShape(50))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // ── Icon circle — 3D glass orb ───────────────────────────────────────
        // Layers (back→front): ambient glow · base fill · convex depth gradient ·
        //                       off-center specular highlight · gradient rim border
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    val r  = size.minDimension / 2f
                    val cx = size.width  / 2f
                    val cy = size.height / 2f

                    // ① Ambient glow halo behind the orb (selected only)
                    if (isSelected) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to accent.copy(alpha = 0.50f),
                                    0.5f to accent.copy(alpha = 0.18f),
                                    1.0f to Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = r * 1.85f
                            ),
                            radius = r * 1.85f,
                            center = Offset(cx, cy)
                        )
                    }

                    // ② Solid base color
                    drawCircle(
                        color  = if (isSelected) accent else theme.bg2.copy(alpha = 0.88f),
                        radius = r,
                        center = Offset(cx, cy)
                    )

                    // ③ Convex depth gradient — lighter top, darker bottom = 3D sphere illusion
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White.copy(alpha = if (isSelected) 0.30f else 0.20f),
                                0.42f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = if (isSelected) 0.18f else 0.28f)
                            ),
                            startY = 0f,
                            endY   = size.height
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )

                    // ④ Specular lens flare — off-center upper-left radial highlight
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.White.copy(alpha = if (isSelected) 0.70f else 0.48f),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(size.width * 0.34f, size.height * 0.22f),
                            radius = r * 0.52f
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
                .border(
                    width = if (isSelected) 1.5.dp else 0.8.dp,
                    brush = if (isSelected) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.40f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                theme.stroke.copy(alpha = 0.18f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = tab.label,
                tint               = if (isSelected) theme.accent.onColor else theme.text2,
                modifier           = Modifier.size(26.dp)
            )
        }

        // ── Animated label ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isSelected,
            enter   = fadeIn(tween(180, delayMillis = 40)) +
                      expandHorizontally(
                          animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                          expandFrom    = Alignment.Start
                      ),
            exit    = fadeOut(tween(100)) +
                      shrinkHorizontally(
                          animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
                          shrinkTowards = Alignment.Start
                      )
        ) {
            Text(
                text     = tab.label,
                color    = theme.text0,
                modifier = Modifier.padding(start = 8.dp, end = 10.dp),
                style    = MaterialTheme.typography.labelMedium.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            )
        }
    }
}
