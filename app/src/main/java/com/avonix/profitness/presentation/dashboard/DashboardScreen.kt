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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    val navBarHeight = 80.dp
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
//  APP NAV BAR — Compact side tabs + prominent center AI FAB
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
    val shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    val leftTabs   = tabs.filter { it != DashboardTab.AICoach }.take(2)
    val rightTabs  = tabs.filter { it != DashboardTab.AICoach }.drop(2)
    val aiSelected = selected == DashboardTab.AICoach

    // Pulsing glow animation for the center AI button when active
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val aiGlowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.28f,
        targetValue   = 0.60f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_glow"
    )
    val aiGlowAlpha = if (aiSelected) aiGlowPulse else 0.22f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 28.dp,
                shape        = shape,
                spotColor    = accent.copy(alpha = 0.30f),
                ambientColor = Color.Black.copy(alpha = 0.65f)
            )
            .background(theme.bg2, shape)
            .navigationBarsPadding()
            .height(80.dp)
            .drawWithCache {
                val rimBrush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(0.15f), Color.Transparent)
                )
                onDrawBehind {
                    // Subtle top rim shimmer
                    drawRect(rimBrush, size = Size(size.width, 1.5.dp.toPx()))
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftTabs.forEach { tab ->
                NavBarItem(
                    tab        = tab,
                    isSelected = tab == selected,
                    accent     = accent,
                    theme      = theme,
                    modifier   = Modifier.weight(1f),
                    onClick    = { onSelect(tab) }
                )
            }

            // Center AI FAB slot — gets extra width weight for visual prominence
            Box(
                modifier         = Modifier.weight(1.4f),
                contentAlignment = Alignment.Center
            ) {
                AiCenterButton(
                    isSelected = aiSelected,
                    accent     = accent,
                    glowAlpha  = aiGlowAlpha,
                    onClick    = { onSelect(DashboardTab.AICoach) }
                )
            }

            rightTabs.forEach { tab ->
                NavBarItem(
                    tab        = tab,
                    isSelected = tab == selected,
                    accent     = accent,
                    theme      = theme,
                    modifier   = Modifier.weight(1f),
                    onClick    = { onSelect(tab) }
                )
            }
        }
    }
}

// ─── Center AI Floating Action Button ───────────────────────────────────────
@Composable
private fun AiCenterButton(
    isSelected: Boolean,
    accent    : Color,
    glowAlpha : Float,
    onClick   : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = when {
            isPressed  -> 0.87f
            isSelected -> 1.08f
            else       -> 1f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "ai_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .shadow(
                elevation    = if (isSelected) 20.dp else 8.dp,
                shape        = CircleShape,
                spotColor    = accent.copy(alpha = glowAlpha),
                ambientColor = accent.copy(alpha = 0.12f)
            )
            .background(
                brush = Brush.verticalGradient(listOf(LimeBright, Lime, LimeDim)),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint               = Color(0xFF0A0A0F),
            modifier           = Modifier.size(26.dp)
        )
    }
}

// ─── Side Nav Item — icon + label with animated glow ────────────────────────
@Composable
private fun NavBarItem(
    tab       : DashboardTab,
    isSelected: Boolean,
    accent    : Color,
    theme     : AppThemeState,
    modifier  : Modifier = Modifier,
    onClick   : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.83f else if (isSelected) 1.12f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "nav_scale"
    )

    val itemTint by animateColorAsState(
        targetValue   = if (isSelected) accent else theme.text2,
        animationSpec = tween(200),
        label         = "nav_tint"
    )

    val glowAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.15f else 0f,
        animationSpec = tween(260),
        label         = "nav_glow"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon inside a glow-circle container
        Box(
            modifier = Modifier
                .size(38.dp)
                .drawBehind {
                    drawCircle(
                        color  = accent.copy(alpha = glowAlpha),
                        radius = 19.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = null,
                tint               = itemTint,
                modifier           = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text  = tab.label,
            color = itemTint,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize      = 9.sp,
                fontWeight    = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.6.sp
            )
        )
    }
}
