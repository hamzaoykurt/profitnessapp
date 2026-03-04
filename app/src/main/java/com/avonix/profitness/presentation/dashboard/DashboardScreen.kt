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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.aicoach.AICoachScreen
import com.avonix.profitness.presentation.news.NewsScreen
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
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit) {
    var selectedTab           by remember { mutableStateOf<DashboardTab>(DashboardTab.Workout) }
    var showPerformanceDetail by remember { mutableStateOf(false) }

    val navBarHeight = 78.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(modifier = Modifier.fillMaxSize())

        Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
            when (tab) {
                DashboardTab.Workout -> WorkoutScreen(bottomPadding = contentPad)
                DashboardTab.Program -> ProgramBuilderScreen()
                DashboardTab.AICoach -> AICoachScreen(bottomPadding = contentPad)
                DashboardTab.News    -> NewsScreen()
                DashboardTab.Profile -> ProfileScreen(
                    onThemeChange           = onThemeChange,
                    onNavigateToPerformance = { showPerformanceDetail = true }
                )
            }
        }

        AppNavBar(
            tabs     = ALL_TABS,
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f)
        )

        // ── Performance Detail Overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = showPerformanceDetail,
            enter   = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec  = tween(380, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(380)),
            exit    = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(320)),
            modifier = Modifier.zIndex(200f)
        ) {
            PerformanceDetailScreen(onBack = { showPerformanceDetail = false })
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

    Box(modifier = modifier.drawWithCache {
        val radial = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f  to accent.copy(alpha = 0.16f),
                0.30f to accent.copy(alpha = 0.10f),
                0.60f to accent.copy(alpha = 0.04f),
                1.0f  to Color.Transparent
            ),
            center = Offset(size.width, 0f),
            radius = size.width * 2.2f
        )
        val sweep = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to accent.copy(alpha = 0.07f),
                0.5f to accent.copy(alpha = 0.02f),
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

    val borderBrush = Brush.horizontalGradient(
        listOf(
            accent.copy(alpha = 0.50f),
            Color.White.copy(alpha = 0.10f),
            accent.copy(alpha = 0.32f)
        )
    )

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
                    elevation    = 30.dp,
                    shape        = shape,
                    clip         = false,
                    spotColor    = accent.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.65f)
                )
                .clip(shape)
                .drawWithCache {
                    // ① Dense semi-transparent base — more opaque = "blurrier" feel
                    val bgBase = theme.bg0.copy(alpha = 0.76f)

                    // ② Top glass reflection — restrained highlight, preserves dark feel
                    val topMirror = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.09f),
                            0.30f to Color.White.copy(alpha = 0.02f),
                            0.55f to Color.Transparent
                        )
                    )

                    // ③ Accent bleed: linear left→right, spreads across full pill width
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

                    // ④ Bottom inner shadow — adds weight/3D grounding
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "nav_item_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .then(
                if (isSelected) Modifier.background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to theme.bg3.copy(alpha = 0.72f),
                            1.0f to theme.bg2.copy(alpha = 0.82f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // ── Icon circle ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = if (isSelected) accent else theme.bg2.copy(alpha = 0.80f),
                    shape = CircleShape
                )
                .then(
                    if (!isSelected) Modifier.border(0.5.dp, theme.stroke.copy(alpha = 0.55f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = tab.label,
                tint               = if (isSelected) theme.accent.onColor else theme.text2,
                modifier           = Modifier.size(22.dp)
            )
        }

        // ── Animated label ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isSelected,
            enter   = fadeIn(tween(200, delayMillis = 80)) +
                      expandHorizontally(
                          animationSpec = tween(280, easing = FastOutSlowInEasing),
                          expandFrom    = Alignment.Start
                      ),
            exit    = fadeOut(tween(80)) +
                      shrinkHorizontally(
                          animationSpec = tween(220, easing = FastOutSlowInEasing),
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
