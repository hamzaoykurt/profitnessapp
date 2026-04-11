package com.avonix.profitness.presentation.store

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.store.UserPlan

// ── Domain ────────────────────────────────────────────────────────────────────

private data class PlanTier(
    val plan          : UserPlan,
    val label         : String,
    val monthlyPrice  : String,
    val yearlyPrice   : String,
    val yearlyPerMonth: String,
    val yearlyBadge   : String,
    val accentColor   : Color,
    val badge         : String?,
    val features      : List<Pair<ImageVector, String>>
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
        plan           = UserPlan.FREE,
        label          = "Ücretsiz",
        monthlyPrice   = "₺0",
        yearlyPrice    = "₺0",
        yearlyPerMonth = "",
        yearlyBadge    = "",
        accentColor    = TextSecondary,
        badge          = null,
        features       = listOf(
            Icons.Rounded.ChatBubbleOutline to "5 AI mesaj (başlangıç kredisi)",
            Icons.Rounded.FitnessCenter      to "Manuel program oluşturma",
            Icons.Rounded.CheckCircle        to "Antrenman takibi",
            Icons.Rounded.BarChart           to "Temel analitik"
        )
    ),
    PlanTier(
        plan           = UserPlan.PRO,
        label          = "Pro",
        monthlyPrice   = "₺149",
        yearlyPrice    = "₺999",
        yearlyPerMonth = "≈₺83/ay",
        yearlyBadge    = "%44 indirim",
        accentColor    = Forge500,
        badge          = "POPÜLER",
        features       = listOf(
            Icons.Rounded.AllInclusive  to "Sınırsız AI Coach sohbeti",
            Icons.Rounded.AutoAwesome   to "AI program oluşturma",
            Icons.Rounded.TrendingUp    to "Gelişmiş performans analizi",
            Icons.Rounded.ShowChart     to "Egzersiz ilerleme grafikleri",
            Icons.Rounded.MonitorWeight to "Ağırlık takibi + AI trend analizi"
        )
    ),
    PlanTier(
        plan           = UserPlan.ELITE,
        label          = "Elite",
        monthlyPrice   = "₺249",
        yearlyPrice    = "₺1.799",
        yearlyPerMonth = "≈₺150/ay",
        yearlyBadge    = "%40 indirim",
        accentColor    = CardPurple,
        badge          = "EN İYİ",
        features       = listOf(
            Icons.Rounded.AllInclusive      to "Sınırsız AI Coach + program",
            Icons.Rounded.Person            to "Kişisel AI antrenör profili",
            Icons.Rounded.Support           to "Öncelikli destek",
            Icons.Rounded.NewReleases       to "Erken erişim özellikleri",
            Icons.Rounded.WorkspacePremium  to "Tüm Pro özellikleri dahil"
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

    var toastMsg    by remember { mutableStateOf<String?>(null) }
    var activeTab   by remember { mutableIntStateOf(0) } // 0=Abonelik, 1=Kredi
    var selectedPlan by remember { mutableStateOf(UserPlan.PRO) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StoreEvent.ShowToast -> toastMsg = event.message
            }
        }
    }

    // Otomatik olarak mevcut plana scroll et
    LaunchedEffect(state.plan) {
        if (state.plan != UserPlan.FREE) selectedPlan = state.plan
    }

    val selectedTier = PLANS.first { it.plan == selectedPlan }
    val isCurrent    = state.plan == selectedPlan
    val isFree       = selectedPlan == UserPlan.FREE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            StoreTopBar(
                theme      = theme,
                activeTab  = activeTab,
                plan       = state.plan,
                credits    = state.credits,
                onBack     = onBack,
                onTabChange = { activeTab = it }
            )

            // ── Tab Content ───────────────────────────────────────────────────
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(280)) { dir * it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(200)) { -dir * it / 4 } + fadeOut(tween(160)))
                },
                modifier = Modifier.fillMaxSize(),
                label    = "store_tab"
            ) { tab ->
                when (tab) {
                    0 -> SubscriptionTab(
                        theme        = theme,
                        haptic       = haptic,
                        state        = state,
                        selectedPlan = selectedPlan,
                        selectedTier = selectedTier,
                        isCurrent    = isCurrent,
                        isFree       = isFree,
                        onSelectPlan = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedPlan = it
                        },
                        onYearlyChange = viewModel::setYearly,
                        onPurchase     = { viewModel.purchasePlan(selectedPlan) }
                    )
                    else -> CreditsTab(
                        theme     = theme,
                        haptic    = haptic,
                        state     = state,
                        onPurchase = { viewModel.purchaseCredits(it) }
                    )
                }
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

// ── Top Bar with Tabs ─────────────────────────────────────────────────────────

@Composable
private fun StoreTopBar(
    theme      : AppThemeState,
    activeTab  : Int,
    plan       : UserPlan,
    credits    : Int,
    onBack     : () -> Unit,
    onTabChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bg0)
            .statusBarsPadding()
    ) {
        // Back + title row
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, null,
                    tint = theme.text1, modifier = Modifier.size(20.dp))
            }
            Text(
                "MAĞAZA",
                color         = theme.text0,
                fontSize      = 14.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier      = Modifier.weight(1f),
                textAlign     = TextAlign.Center
            )
            // Sağ üst: kredi/plan chip
            val isPaid    = plan != UserPlan.FREE
            val chipColor = if (isPaid) Color(0xFFFFD700).copy(0.15f) else Lime.copy(0.12f)
            val chipBorder= if (isPaid) Color(0xFFFFD700).copy(0.5f)  else Lime.copy(0.4f)
            val chipTint  = if (isPaid) Color(0xFFFFD700) else Lime
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipColor)
                    .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isPaid) Icons.Rounded.WorkspacePremium else Icons.Rounded.Bolt,
                    null, tint = chipTint, modifier = Modifier.size(13.dp)
                )
                Text(
                    if (isPaid) plan.displayName else "$credits",
                    color      = chipTint,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Abonelik", "AI Kredi").forEachIndexed { idx, label ->
                val isActive = activeTab == idx
                val color    = if (idx == 0) Forge500 else Lime
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) color.copy(0.14f) else theme.bg1)
                        .border(
                            1.dp,
                            if (isActive) color.copy(0.5f) else theme.stroke,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabChange(idx) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color      = if (isActive) color else theme.text2,
                        fontSize   = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        HorizontalDivider(color = theme.stroke.copy(0.3f), thickness = 0.5.dp)
    }
}

// ── Subscription Tab ──────────────────────────────────────────────────────────

@Composable
private fun SubscriptionTab(
    theme        : AppThemeState,
    haptic       : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state        : StoreState,
    selectedPlan : UserPlan,
    selectedTier : PlanTier,
    isCurrent    : Boolean,
    isFree       : Boolean,
    onSelectPlan : (UserPlan) -> Unit,
    onYearlyChange: (Boolean) -> Unit,
    onPurchase   : () -> Unit
) {
    val displayPrice = if (state.isYearly && selectedTier.yearlyBadge.isNotEmpty())
        selectedTier.yearlyPrice else selectedTier.monthlyPrice
    val displayPeriod = when {
        isFree         -> ""
        state.isYearly -> "/yıl"
        else           -> "/ay"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Billing toggle
            item {
                Spacer(Modifier.height(20.dp))
                BillingToggle(
                    isYearly = state.isYearly,
                    onToggle = onYearlyChange,
                    theme    = theme
                )
                Spacer(Modifier.height(20.dp))
            }

            // Plan tab selector
            item {
                PlanTabRow(
                    plans        = PLANS,
                    selectedPlan = selectedPlan,
                    currentPlan  = state.plan,
                    isYearly     = state.isYearly,
                    onSelect     = onSelectPlan
                )
                Spacer(Modifier.height(16.dp))
            }

            // Plan detail card
            item {
                AnimatedContent(
                    targetState = selectedTier,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 8 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 8 })
                    },
                    label = "plan_detail"
                ) { tier ->
                    PlanDetailCard(
                        tier      = tier,
                        isYearly  = state.isYearly,
                        isCurrent = state.plan == tier.plan,
                        theme     = theme
                    )
                }
            }

            // Legal note
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Abonelikler otomatik yenilenir. İstediğin zaman iptal edebilirsin.\nFiyatlar KDV dahildir.",
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    color     = theme.text2.copy(0.45f),
                    fontSize  = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }

        // Sticky CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, theme.bg0.copy(0.96f), theme.bg0)
                    )
                )
                .navigationBarsPadding()
        ) {
            if (!isFree) {
                CtaButton(
                    isCurrent      = isCurrent,
                    price          = displayPrice,
                    period         = displayPeriod,
                    accentColor    = selectedTier.accentColor,
                    isLoading      = state.isLoading,
                    yearlyPerMonth = if (state.isYearly) selectedTier.yearlyPerMonth else "",
                    onClick        = {
                        if (!isCurrent) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPurchase()
                        }
                    }
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Credits Tab ───────────────────────────────────────────────────────────────

@Composable
private fun CreditsTab(
    theme     : AppThemeState,
    haptic    : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state     : StoreState,
    onPurchase: (Int) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Lime.copy(0.12f))
                        .border(1.dp, Lime.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Bolt, null,
                        tint = Lime, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "AI KREDİSİ",
                    color         = theme.text0,
                    fontSize      = 22.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Her AI işlemi 1 kredi harcar.\nÜcretsiz planda geçerlidir.",
                    color      = theme.text2,
                    fontSize   = 12.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                // Mevcut kredi göstergesi
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Lime.copy(0.08f))
                        .border(1.dp, Lime.copy(0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, null,
                        tint = Lime, modifier = Modifier.size(16.dp))
                    Text(
                        "Mevcut: ",
                        color    = theme.text2,
                        fontSize = 13.sp
                    )
                    Text(
                        "${state.credits} kredi",
                        color      = Lime,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Paket kartları
        items(CREDIT_PACKAGES.size) { idx ->
            val pkg = CREDIT_PACKAGES[idx]
            CreditCard(
                pkg       = pkg,
                theme     = theme,
                isLoading = state.isLoading,
                onPurchase = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPurchase(pkg.credits)
                }
            )
        }

        item {
            Text(
                "Krediler abonelik gerektirmez. FREE planında kullanılır.\nÖdeme bilgileri şifreli olarak saklanır.",
                modifier  = Modifier.fillMaxWidth(),
                color     = theme.text2.copy(0.45f),
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
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
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            "AYLIK",
            color      = if (!isYearly) theme.text0 else theme.text2,
            fontSize   = 12.sp,
            fontWeight = if (!isYearly) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(false) }
        )

        Spacer(Modifier.width(14.dp))

        // Toggle switch — basit Switch yerine özel görünüm
        val trackColor by animateColorAsState(
            if (isYearly) Lime.copy(0.85f) else theme.stroke,
            tween(200), label = "track"
        )
        Box(
            modifier = Modifier
                .size(44.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(trackColor)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(!isYearly) }
                .padding(2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val thumbX by animateFloatAsState(
                targetValue   = if (isYearly) 20f else 0f,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label         = "thumb"
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbX.toInt(), 0) }
                    .size(20.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(Modifier.width(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "YILLIK",
                color      = if (isYearly) theme.text0 else theme.text2,
                fontSize   = 12.sp,
                fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Normal,
                modifier   = Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(true) }
            )
            Spacer(Modifier.width(6.dp))
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
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        plans.forEach { tier ->
            PlanTab(
                tier       = tier,
                isSelected = selectedPlan == tier.plan,
                isCurrent  = currentPlan == tier.plan,
                isYearly   = isYearly,
                theme      = theme,
                onClick    = { onSelect(tier.plan) },
                modifier   = Modifier.weight(1f)
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
    val bgAlpha by animateFloatAsState(
        if (isSelected) 0.12f else 0.0f,
        tween(200), label = "bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) accent.copy(0.7f) else theme.stroke,
        tween(200), label = "border"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(bgAlpha))
            .border(if (isSelected) 1.5.dp else 0.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (tier.badge != null) {
            Text(tier.badge,
                color         = accent,
                fontSize      = 8.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp)
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
                Text("AKTİF", color = Lime, fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ── Plan Detail Card ──────────────────────────────────────────────────────────

@Composable
private fun PlanDetailCard(
    tier     : PlanTier,
    isYearly : Boolean,
    isCurrent: Boolean,
    theme    : AppThemeState
) {
    val accent = tier.accentColor
    val isFree = tier.plan == UserPlan.FREE
    val price  = if (isYearly && tier.yearlyBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period = when { isFree -> ""; isYearly -> "/yıl"; else -> "/ay" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation    = if (isCurrent) 24.dp else 8.dp,
                shape        = RoundedCornerShape(24.dp),
                spotColor    = accent.copy(if (isCurrent) 0.4f else 0.1f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(theme.bg2)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) accent.copy(0.55f) else theme.stroke,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // Watermark
        Text(
            tier.plan.name,
            modifier   = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp),
            color      = accent.copy(0.06f),
            fontSize   = 72.sp,
            fontWeight = FontWeight.Black
        )

        Column(modifier = Modifier.padding(24.dp)) {
            // Fiyat satırı
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = price,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    label = "price_anim"
                ) { p ->
                    Text(p,
                        color      = theme.text0,
                        fontSize   = 40.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp)
                }
                if (period.isNotEmpty()) {
                    Text(period,
                        color      = theme.text2,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(start = 4.dp, bottom = 6.dp))
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
                            if (tier.plan == UserPlan.ELITE) Icons.Rounded.Diamond
                            else Icons.Rounded.WorkspacePremium,
                            null, tint = accent, modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Yıllık tasarruf göstergesi
            if (isYearly && tier.yearlyBadge.isNotEmpty()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(top = 4.dp)
                ) {
                    Text(tier.yearlyPerMonth, color = theme.text2, fontSize = 12.sp)
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
                        color      = if (isFree) theme.text2 else theme.text1,
                        fontSize   = 13.sp,
                        lineHeight = 18.sp,
                        modifier   = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Credit Card ───────────────────────────────────────────────────────────────

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
        if (isPressed) 0.97f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "cs"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (pkg.badge != null) 16.dp else 4.dp,
                shape     = RoundedCornerShape(20.dp),
                spotColor = pkg.accentColor.copy(if (pkg.badge != null) 0.35f else 0.1f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg2)
            .border(
                width = if (pkg.badge != null) 1.5.dp else 1.dp,
                color = if (pkg.badge != null) pkg.accentColor.copy(0.4f) else theme.stroke,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                enabled           = !isLoading,
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPurchase
            )
            .padding(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pkg.accentColor.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, null,
                tint = pkg.accentColor, modifier = Modifier.size(28.dp))
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${pkg.credits} Kredi",
                    color      = theme.text0,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black
                )
                if (pkg.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pkg.accentColor.copy(0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            pkg.badge,
                            color         = pkg.accentColor,
                            fontSize      = 8.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(pkg.perCredit, color = theme.text2, fontSize = 11.sp)
        }

        // Price CTA
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                color       = pkg.accentColor,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(pkg.accentColor, pkg.accentColor.copy(0.75f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    pkg.price,
                    color      = if (pkg.accentColor == Lime) LimeText else Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
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
                    Text("Mevcut planınız",
                        color = Lime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Text(
                    "$price$period ile Başla",
                    color      = if (accentColor == Lime) LimeText else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize   = 16.sp
                )
            }
        }

        if (yearlyPerMonth.isNotEmpty() && !isCurrent) {
            Spacer(Modifier.height(6.dp))
            Text(
                "$yearlyPerMonth · İstediğin zaman iptal et · Taahhüt yok",
                color     = theme_text2_placeholder,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center
            )
        } else if (!isCurrent) {
            Spacer(Modifier.height(6.dp))
            Text(
                "İstediğin zaman iptal et · Taahhüt yok",
                color     = theme_text2_placeholder,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val theme_text2_placeholder = TextMuted

// ── Paywall Dialog (AI için) ──────────────────────────────────────────────────

@Composable
fun PaywallDialog(
    onDismiss     : () -> Unit,
    onGoToStore   : () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.65f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(theme.bg1)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(horizontal = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.stroke)
            )
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .drawBehind {
                        drawCircle(
                            brush  = Brush.radialGradient(
                                listOf(Forge500.copy(0.4f), Color.Transparent)
                            ),
                            radius = size.minDimension * 0.9f
                        )
                    }
                    .clip(CircleShape)
                    .background(Forge500.copy(0.15f))
                    .border(1.dp, Forge500.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null,
                    tint = Forge500, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text("AI Kredi Gerekli",
                color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Bu özelliği kullanmak için AI krediniz bitmiş.\nPremium'a geçin veya kredi satın alın.",
                color     = theme.text2,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Forge500, Forge500.copy(0.8f))))
                    .clickable { onGoToStore() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Planları Gör",
                    color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Vazgeç", color = theme.text2, fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
