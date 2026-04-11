package com.avonix.profitness.presentation.store

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.store.UserPlan

// ── Domain nesneleri ──────────────────────────────────────────────────────────

private data class PlanTier(
    val plan         : UserPlan,
    val label        : String,
    val monthlyPrice : String,
    val yearlyPrice  : String,
    val yearlyPerMonth: String,
    val yearlyBadge  : String,
    val accentColor  : Color,
    val badge        : String?,
    val features     : List<Pair<ImageVector, String>>
)

private data class CreditPackage(
    val credits    : Int,
    val price      : String,
    val perCredit  : String,
    val badge      : String?,
    val accentColor: Color
)

private val PLANS = listOf(
    PlanTier(
        plan          = UserPlan.FREE,
        label         = "Ücretsiz",
        monthlyPrice  = "₺0",
        yearlyPrice   = "₺0",
        yearlyPerMonth= "",
        yearlyBadge   = "",
        accentColor   = TextSecondary,
        badge         = null,
        features      = listOf(
            Icons.Rounded.ChatBubbleOutline to "5 AI mesaj (başlangıç kredisi)",
            Icons.Rounded.FitnessCenter      to "Manuel program oluşturma",
            Icons.Rounded.CheckCircle        to "Antrenman takibi",
            Icons.Rounded.BarChart           to "Temel analitik"
        )
    ),
    PlanTier(
        plan          = UserPlan.PRO,
        label         = "Pro",
        monthlyPrice  = "₺149",
        yearlyPrice   = "₺999",
        yearlyPerMonth= "≈₺83/ay",
        yearlyBadge   = "%44 indirim",
        accentColor   = Forge500,
        badge         = "POPÜLER",
        features      = listOf(
            Icons.Rounded.AllInclusive       to "Sınırsız AI Coach sohbeti",
            Icons.Rounded.AutoAwesome        to "AI program oluşturma",
            Icons.Rounded.TrendingUp         to "Gelişmiş performans analizi",
            Icons.Rounded.ShowChart          to "Egzersiz ilerleme grafikleri",
            Icons.Rounded.MonitorWeight      to "Ağırlık takibi + AI trend analizi"
        )
    ),
    PlanTier(
        plan          = UserPlan.ELITE,
        label         = "Elite",
        monthlyPrice  = "₺249",
        yearlyPrice   = "₺1.799",
        yearlyPerMonth= "≈₺150/ay",
        yearlyBadge   = "%40 indirim",
        accentColor   = CardPurple,
        badge         = "EN İYİ",
        features      = listOf(
            Icons.Rounded.AllInclusive       to "Sınırsız AI Coach + program",
            Icons.Rounded.Person             to "Kişisel AI antrenör profili",
            Icons.Rounded.Support            to "Öncelikli destek",
            Icons.Rounded.NewReleases        to "Erken erişim özellikleri",
            Icons.Rounded.WorkspacePremium   to "Tüm Pro özellikleri dahil"
        )
    )
)

private val CREDIT_PACKAGES = listOf(
    CreditPackage(10,  "₺29",  "₺2,9/kredi", null,         Lime),
    CreditPackage(50,  "₺99",  "₺2,0/kredi", "EN POPÜLER", Forge500),
    CreditPackage(200, "₺299", "₺1,5/kredi", null,         CardCyan)
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun StoreScreen(
    onBack   : () -> Unit,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val theme   = LocalAppTheme.current
    val haptic  = LocalHapticFeedback.current
    val density = LocalDensity.current

    var toastMsg      by remember { mutableStateOf<String?>(null) }
    var selectedPlan  by remember { mutableStateOf(UserPlan.PRO) }
    var ctaHeightPx   by remember { mutableIntStateOf(0) }
    val ctaHeight      = with(density) { ctaHeightPx.toDp() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StoreEvent.ShowToast -> toastMsg = event.message
            }
        }
    }

    val selectedTier = PLANS.first { it.plan == selectedPlan }
    val isCurrent    = state.plan == selectedPlan
    val isFree       = selectedPlan == UserPlan.FREE
    val displayPrice = if (state.isYearly && selectedTier.yearlyBadge.isNotEmpty())
        selectedTier.yearlyPrice else selectedTier.monthlyPrice
    val displayPeriod = when {
        isFree              -> ""
        state.isYearly      -> "/yıl"
        else                -> "/ay"
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero ─────────────────────────────────────────────────────────
            HeroSection(
                theme    = theme,
                plan     = state.plan,
                credits  = state.credits,
                onBack   = onBack
            )

            // ── Billing toggle ────────────────────────────────────────────────
            BillingToggle(
                isYearly = state.isYearly,
                onToggle = viewModel::setYearly,
                theme    = theme
            )

            Spacer(Modifier.height(20.dp))

            // ── Plan tab selector ─────────────────────────────────────────────
            PlanTabRow(
                plans        = PLANS,
                selectedPlan = selectedPlan,
                currentPlan  = state.plan,
                isYearly     = state.isYearly,
                onSelect     = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedPlan = it
                }
            )

            Spacer(Modifier.height(16.dp))

            // ── Plan detail card (AnimatedContent) ───────────────────────────
            AnimatedContent(
                targetState  = selectedTier,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 8 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 8 })
                },
                label = "plan_detail"
            ) { tier ->
                PlanDetailCard(
                    tier      = tier,
                    isYearly  = state.isYearly,
                    isCurrent = state.plan == tier.plan
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── AI Credits section ────────────────────────────────────────────
            CreditsSection(
                theme     = theme,
                isLoading = state.isLoading,
                onPurchase = { viewModel.purchaseCredits(it) }
            )

            Spacer(Modifier.height(20.dp))

            // Legal note
            Text(
                "Abonelikler otomatik yenilenir. İstediğin zaman iptal edebilirsin.\nFiyatlar KDV dahildir.",
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                color      = theme.text2.copy(0.45f),
                fontSize   = 10.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 15.sp
            )

            // Padding so sticky CTA doesn't cover content
            Spacer(Modifier.height(ctaHeight + 24.dp))
        }

        // ── Sticky bottom CTA ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, theme.bg0.copy(0.97f), theme.bg0)
                    )
                )
                .navigationBarsPadding()
                .onGloballyPositioned { ctaHeightPx = it.size.height }
        ) {
            if (!isFree) {
                CtaButton(
                    isCurrent  = isCurrent,
                    price      = displayPrice,
                    period     = displayPeriod,
                    accentColor = selectedTier.accentColor,
                    isLoading  = state.isLoading,
                    yearlyPerMonth = if (state.isYearly) selectedTier.yearlyPerMonth else "",
                    onClick    = {
                        if (!isCurrent) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.purchasePlan(selectedPlan)
                        }
                    }
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Toast ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = toastMsg != null,
            enter    = slideInVertically { -it } + fadeIn(tween(200)),
            exit     = slideOutVertically { -it } + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
                .zIndex(20f)
        ) {
            toastMsg?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2500)
                    toastMsg = null
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Lime)
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = LimeText, modifier = Modifier.size(16.dp))
                    Text(msg, color = LimeText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Hero Section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    theme  : AppThemeState,
    plan   : UserPlan,
    credits: Int,
    onBack : () -> Unit
) {
    val planAccent = when (plan) {
        UserPlan.FREE  -> Forge500
        UserPlan.PRO   -> Forge500
        UserPlan.ELITE -> CardPurple
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .drawBehind {
                // Üst kısımda amber/forge radial glow
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f   to planAccent.copy(alpha = 0.22f),
                            0.5f to planAccent.copy(alpha = 0.08f),
                            1f   to Color.Transparent
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.8f
                    )
                )
                // Alt kısımda fade
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color(0x55000000)),
                        startY = size.height * 0.5f,
                        endY   = size.height
                    )
                )
            }
    ) {
        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Rounded.ArrowBackIosNew, null,
                tint = theme.text1, modifier = Modifier.size(20.dp))
        }

        // Center content
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated crown icon with ring
            Box(contentAlignment = Alignment.Center) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(planAccent.copy(0.08f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(planAccent.copy(0.5f), planAccent.copy(0.1f))
                            ),
                            shape = CircleShape
                        )
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(20.dp, CircleShape, spotColor = planAccent.copy(0.7f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(planAccent.copy(0.4f), planAccent.copy(0.15f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null,
                        tint = planAccent, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "PROFİTNESS",
                color         = planAccent.copy(0.9f),
                fontWeight    = FontWeight.ExtraLight,
                fontSize      = 10.sp,
                letterSpacing = 7.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "PREMIUM",
                color      = theme.text0,
                fontWeight = FontWeight.Black,
                fontSize   = 28.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            // Current plan chip
            val planColor = when (plan) {
                UserPlan.FREE  -> theme.text2
                UserPlan.PRO   -> Forge500
                UserPlan.ELITE -> CardPurple
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(planColor.copy(0.12f))
                    .border(1.dp, planColor.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (plan == UserPlan.FREE) Icons.Rounded.Lock else Icons.Rounded.CheckCircle,
                    null, tint = planColor, modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (plan == UserPlan.FREE)
                        "${plan.displayName} · $credits AI kredi"
                    else
                        "${plan.displayName} · Aktif",
                    color      = planColor,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Billing Toggle ────────────────────────────────────────────────────────────

@Composable
private fun BillingToggle(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme   : AppThemeState
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // AYLIK
        Text(
            "AYLIK",
            color         = if (!isYearly) theme.text0 else theme.text2,
            fontSize      = 12.sp,
            fontWeight    = if (!isYearly) FontWeight.Bold else FontWeight.Normal,
            modifier      = Modifier.clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(false) }
        )

        Spacer(Modifier.width(14.dp))

        // Toggle switch
        val thumbOffset by animateDpAsState(
            targetValue   = if (isYearly) 20.dp else 0.dp,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
            label         = "toggleThumb"
        )
        Box(
            modifier = Modifier
                .size(44.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isYearly) Lime.copy(0.85f) else theme.stroke
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(!isYearly) }
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset + 2.dp, top = 2.dp, bottom = 2.dp)
                    .size(20.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(Modifier.width(14.dp))

        // YILLIK
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "YILLIK",
                color         = if (isYearly) theme.text0 else theme.text2,
                fontSize      = 12.sp,
                fontWeight    = if (isYearly) FontWeight.Bold else FontWeight.Normal,
                modifier      = Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(true) }
            )
            Spacer(Modifier.width(6.dp))
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn() + expandHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Lime.copy(0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("-%40", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ── Plan Tab Row ──────────────────────────────────────────────────────────────

@Composable
private fun PlanTabRow(
    plans       : List<PlanTier>,
    selectedPlan: UserPlan,
    currentPlan : UserPlan,
    isYearly    : Boolean,
    onSelect    : (UserPlan) -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        plans.forEach { tier ->
            PlanTab(
                tier        = tier,
                isSelected  = selectedPlan == tier.plan,
                isCurrent   = currentPlan == tier.plan,
                isYearly    = isYearly,
                theme       = theme,
                onClick     = { onSelect(tier.plan) },
                modifier    = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlanTab(
    tier      : PlanTier,
    isSelected: Boolean,
    isCurrent : Boolean,
    isYearly  : Boolean,
    theme     : AppThemeState,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier
) {
    val accent      = tier.accentColor
    val price       = if (isYearly && tier.yearlyBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val borderWidth by animateDpAsState(
        if (isSelected) 1.5.dp else 0.5.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "bw"
    )
    val bgAlpha by animateFloatAsState(
        if (isSelected) 0.12f else 0.0f,
        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium), label = "bg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(bgAlpha))
            .border(borderWidth, if (isSelected) accent.copy(0.7f) else theme.stroke, RoundedCornerShape(16.dp))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge
        if (tier.badge != null) {
            Text(
                tier.badge,
                color         = accent,
                fontSize      = 8.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(3.dp))
        } else {
            Spacer(Modifier.height(11.dp))
        }

        Text(
            tier.label,
            color      = if (isSelected) accent else theme.text1,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            price,
            color      = if (isSelected) theme.text0 else theme.text2,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Black
        )

        if (isCurrent) {
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Lime.copy(0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("AKTİF", color = Lime, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ── Plan Detail Card ──────────────────────────────────────────────────────────

@Composable
private fun PlanDetailCard(
    tier     : PlanTier,
    isYearly : Boolean,
    isCurrent: Boolean
) {
    val theme  = LocalAppTheme.current
    val accent = tier.accentColor
    val isFree = tier.plan == UserPlan.FREE
    val price  = if (isYearly && tier.yearlyBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period = when { isFree -> ""; isYearly -> "/yıl"; else -> "/ay" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(if (isCurrent) 24.dp else 12.dp, RoundedCornerShape(24.dp),
                spotColor = accent.copy(if (isCurrent) 0.4f else 0.15f))
            .clip(RoundedCornerShape(24.dp))
            .background(theme.bg2)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) accent.copy(0.55f) else theme.stroke,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Plan adı büyük watermark
        Text(
            tier.plan.name,
            modifier  = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp),
            color     = accent.copy(0.06f),
            fontSize  = 72.sp,
            fontWeight = FontWeight.Black
        )

        Column(modifier = Modifier.padding(24.dp)) {
            // Fiyat satırı
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState  = price,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    label = "price_anim"
                ) { p ->
                    Text(
                        p,
                        color      = theme.text0,
                        fontSize   = 40.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp
                    )
                }
                if (period.isNotEmpty()) {
                    Text(
                        period,
                        color      = theme.text2,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!isFree) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accent.copy(0.15f))
                            .border(1.dp, accent.copy(0.3f), CircleShape)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (tier.plan == UserPlan.ELITE) Icons.Rounded.Diamond else Icons.Rounded.WorkspacePremium,
                            null, tint = accent, modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Yıllık aylık eşdeğer + tasarruf
            if (isYearly && tier.yearlyBadge.isNotEmpty()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(tier.yearlyPerMonth,
                        color = theme.text2, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Lime.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(tier.yearlyBadge, color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)
            Spacer(Modifier.height(18.dp))

            // Feature list
            tier.features.forEach { (icon, text) ->
                Row(
                    modifier              = Modifier.padding(vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(if (isFree) 0.06f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null,
                            tint     = if (isFree) theme.text2 else accent,
                            modifier = Modifier.size(16.dp))
                    }
                    Text(text,
                        color    = if (isFree) theme.text2 else theme.text1,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Credits Section ───────────────────────────────────────────────────────────

@Composable
private fun CreditsSection(
    theme    : AppThemeState,
    isLoading: Boolean,
    onPurchase: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI KREDİSİ",
                    color         = theme.text2,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    "Ekstra AI işlemi için kredi satın al",
                    color    = theme.text2.copy(0.6f),
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Rounded.Bolt, null, tint = Lime, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(CREDIT_PACKAGES.size) { idx ->
                val pkg = CREDIT_PACKAGES[idx]
                CreditCard(
                    pkg       = pkg,
                    theme     = theme,
                    isLoading = isLoading,
                    onPurchase = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPurchase(pkg.credits)
                    }
                )
            }
        }
    }
}

@Composable
private fun CreditCard(
    pkg       : CreditPackage,
    theme     : AppThemeState,
    isLoading : Boolean,
    onPurchase: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "cs"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .width(148.dp)
            .shadow(if (pkg.badge != null) 16.dp else 6.dp, RoundedCornerShape(20.dp),
                spotColor = pkg.accentColor.copy(if (pkg.badge != null) 0.4f else 0.1f))
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg2)
            .border(
                1.dp,
                if (pkg.badge != null) pkg.accentColor.copy(0.4f) else theme.stroke,
                RoundedCornerShape(20.dp)
            )
            .clickable(
                enabled           = !isLoading,
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPurchase
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top row: icon + badge
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(pkg.accentColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, null,
                    tint = pkg.accentColor, modifier = Modifier.size(18.dp))
            }
            if (pkg.badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pkg.accentColor.copy(0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        "HOT",
                        color         = pkg.accentColor,
                        fontSize      = 7.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "${pkg.credits}",
            color      = theme.text0,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 30.sp
        )
        Text(
            "AI KREDİ",
            color         = pkg.accentColor,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = theme.stroke.copy(0.4f), thickness = 0.5.dp)
        Spacer(Modifier.height(10.dp))

        // Price
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp).align(Alignment.CenterHorizontally),
                color       = pkg.accentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                pkg.price,
                color      = theme.text0,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                pkg.perCredit,
                color    = theme.text2,
                fontSize = 10.sp
            )
        }
    }
}

// ── Sticky CTA Button ─────────────────────────────────────────────────────────

@Composable
private fun CtaButton(
    isCurrent     : Boolean,
    price         : String,
    period        : String,
    accentColor   : Color,
    isLoading     : Boolean,
    yearlyPerMonth: String,
    onClick       : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && !isCurrent && !isLoading) 0.97f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "cta"
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(
                    elevation    = if (!isCurrent) 16.dp else 0.dp,
                    shape        = RoundedCornerShape(18.dp),
                    spotColor    = accentColor.copy(0.5f),
                    ambientColor = accentColor.copy(0.2f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isCurrent)
                        Brush.linearGradient(listOf(Surface3, Surface3))
                    else
                        Brush.linearGradient(
                            colors = listOf(accentColor, accentColor.copy(0.8f)),
                            start  = Offset(0f, 0f),
                            end    = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                )
                .clickable(
                    enabled           = !isCurrent && !isLoading,
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onClick
                )
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = if (isCurrent) TextMuted else Color.White,
                    strokeWidth = 2.dp
                )
            } else if (isCurrent) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = Lime, modifier = Modifier.size(18.dp))
                    Text(
                        "Mevcut planınız",
                        color      = Lime,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            } else {
                val textColor = if (accentColor == Lime || accentColor == CardCyan) LimeText else Color.White
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$price$period ile Başla",
                        color         = textColor,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                    if (yearlyPerMonth.isNotEmpty()) {
                        Text(
                            yearlyPerMonth,
                            color    = textColor.copy(0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "İstediğin zaman iptal et · Taahhüt yok",
            color    = TextMuted.copy(0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Paywall Dialog ────────────────────────────────────────────────────────────

@Composable
fun PaywallDialog(
    onDismiss  : () -> Unit,
    onGoToStore: () -> Unit
) {
    val theme  = LocalAppTheme.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.80f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom sheet style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(theme.bg1)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(Forge500.copy(0.5f), Color.Transparent)),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.stroke)
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(16.dp, CircleShape, spotColor = Forge500.copy(0.6f))
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Forge500.copy(0.3f), Surface2)))
                    .border(1.dp, Forge500.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.WorkspacePremium, null,
                    tint = Forge500, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "AI Kredin Tükendi",
                color      = theme.text0,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Oracle AI ve program oluşturma için kredi satın al veya Premium'a geç.",
                color      = theme.text2,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            // Mağazaya git
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Forge500.copy(0.4f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Forge500, Lava500),
                            start  = Offset(0f, 0f),
                            end    = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onGoToStore()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Mağazaya Git",
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Şimdi Değil",
                    color = theme.text2, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}
