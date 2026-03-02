package com.avonix.profitness.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    object AICoach : DashboardTab("ai_coach", Icons.Rounded.AutoAwesome, "ORACLE")
    object News    : DashboardTab("news",     Icons.Rounded.Newspaper, "MUSE")
    object Profile : DashboardTab("profile",  Icons.Rounded.Person, "USER")
}

private val ALL_TABS = listOf(
    DashboardTab.Workout, DashboardTab.Program, DashboardTab.AICoach,
    DashboardTab.News,    DashboardTab.Profile
)

@Composable
fun DashboardScreen(onThemeChange: (AppThemeState) -> Unit) {
    var selectedTab           by remember { mutableStateOf<DashboardTab>(DashboardTab.Workout) }
    var showPerformanceDetail by remember { mutableStateOf(false) }

    val navBarHeight = 72.dp
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad   = navBarHeight + navBarBottom + 8.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Vibrant dark background ───────────────────────────────────────
        AppBackground(modifier = Modifier.fillMaxSize())

        // ── Screen content ────────────────────────────────────────────────
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

        // ── Bottom Navigation ─────────────────────────────────────────────
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
//  APP NAV BAR — Full-width, top-rounded, sliding pill indicator
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AppNavBar(
    tabs    : List<DashboardTab>,
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme       = LocalAppTheme.current
    val accent      = MaterialTheme.colorScheme.primary
    val selectedIdx = tabs.indexOf(selected)

    val indicatorPos by animateFloatAsState(
        targetValue = selectedIdx.toFloat(),
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "nav_slide"
    )

    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 24.dp,
                shape        = shape,
                spotColor    = accent.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.6f)
            )
            .background(theme.bg2, shape)
            .navigationBarsPadding()
            .height(72.dp)
            .drawWithCache {
                val itemW  = size.width / tabs.size
                val pillW  = itemW * 0.62f
                val pillH  = size.height * 0.60f
                val rimBrush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(0.12f), Color.Transparent)
                )
                val gradBrush = Brush.verticalGradient(
                    listOf(Color.White.copy(0.03f), Color.Transparent)
                )
                onDrawBehind {
                    // Top rim line
                    drawRect(rimBrush, size = Size(size.width, 1.5.dp.toPx()))
                    // Subtle inner gradient
                    drawRect(gradBrush)
                    // Sliding active pill
                    val pillX = indicatorPos * itemW + (itemW - pillW) / 2f
                    val pillY = (size.height - pillH) / 2f
                    drawRoundRect(
                        color      = accent.copy(alpha = 0.18f),
                        topLeft    = Offset(pillX, pillY),
                        size       = Size(pillW, pillH),
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
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

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.82f else if (isSelected) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "nav_scale"
    )

    val tint by animateColorAsState(
        targetValue   = if (isSelected) accent else theme.text2,
        animationSpec = tween(200),
        label         = "nav_tint"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = tab.icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(24.dp).scale(scale)
        )
    }
}
