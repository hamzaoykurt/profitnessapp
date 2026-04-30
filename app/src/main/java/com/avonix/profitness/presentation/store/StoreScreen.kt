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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.data.store.BillingProduct
import com.avonix.profitness.data.store.UserPlan

// ── Domain ────────────────────────────────────────────────────────────────────

private data class PlanTier(
    val plan          : UserPlan,
    val label         : String,
    val monthlyPrice  : String,
    val yearlyPrice   : String,
    val yearlyPerMonth: String,
    val discountBadge : String,
    val accentColor   : Color,
    val badge         : String?
)

private data class CreditPackage(
    val credits    : Int,
    val price      : String,
    val perCredit  : String,
    val badge      : String?,
    val accentColor: Color
)

/** Hangi plandan itibaren bu özellik açık? */
private data class MasterFeature(
    val icon   : ImageVector,
    val text   : String,
    val minPlan: UserPlan   // bu plan ve üstünde açık
)

private val ALL_FEATURES = listOf(
    MasterFeature(Icons.Rounded.FitnessCenter,    "Antrenman takibi",                   UserPlan.FREE),
    MasterFeature(Icons.Rounded.BarChart,          "Temel analitik",                     UserPlan.FREE),
    MasterFeature(Icons.Rounded.SelfImprovement,   "Manuel program oluşturma",           UserPlan.FREE),
    MasterFeature(Icons.Rounded.ChatBubbleOutline, "AI Coach (kredi tabanlı)",           UserPlan.FREE),
    MasterFeature(Icons.Rounded.AllInclusive,      "Yüksek limitli AI Coach sohbeti",    UserPlan.PRO),
    MasterFeature(Icons.Rounded.AutoAwesome,       "AI ile program oluşturma",           UserPlan.PRO),
    MasterFeature(Icons.Rounded.TrendingUp,        "Gelişmiş performans analizi",        UserPlan.PRO),
    MasterFeature(Icons.Rounded.ShowChart,         "Egzersiz ilerleme grafikleri (AI)",  UserPlan.PRO),
    MasterFeature(Icons.Rounded.MonitorWeight,     "Ağırlık takibi + AI trend analizi",  UserPlan.PRO),
    MasterFeature(Icons.Rounded.Person,            "Kişisel AI antrenör profili",        UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Support,           "Öncelikli destek",                   UserPlan.ELITE),
    MasterFeature(Icons.Rounded.NewReleases,       "Erken erişim özellikleri",           UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Diamond,           "Tüm özellikler dahil",               UserPlan.ELITE)
)

// Plan sıralaması: FREE < PRO < ELITE
private fun UserPlan.ordinal() = when (this) {
    UserPlan.FREE  -> 0
    UserPlan.PRO   -> 1
    UserPlan.ELITE -> 2
}

private val PLANS = listOf(
    PlanTier(
        plan          = UserPlan.FREE,
        label         = "Ücretsiz",
        monthlyPrice  = "₺0",
        yearlyPrice   = "₺0",
        yearlyPerMonth= "",
        discountBadge = "",
        accentColor   = TextSecondary,
        badge         = null
    ),
    PlanTier(
        plan          = UserPlan.PRO,
        label         = "Pro",
        monthlyPrice  = "₺149",
        yearlyPrice   = "₺999",
        yearlyPerMonth= "≈₺83/ay",
        discountBadge = "%44 indirim",
        accentColor   = Forge500,
        badge         = "POPÜLER"
    ),
    PlanTier(
        plan          = UserPlan.ELITE,
        label         = "Elite",
        monthlyPrice  = "₺249",
        yearlyPrice   = "₺1.799",
        yearlyPerMonth= "≈₺150/ay",
        discountBadge = "%40 indirim",
        accentColor   = CardPurple,
        badge         = "EN İYİ"
    )
)

private val CREDIT_PACKAGES = listOf(
    CreditPackage(10,  "₺29",  "₺2,9/kredi", null,         Lime),
    CreditPackage(50,  "₺99",  "₺2,0/kredi", "EN POPÜLER", Forge500),
    CreditPackage(200, "₺299", "₺1,5/kredi", null,         CardCyan)
)

private fun cleanPrice(label: String): String = label.substringBefore("/").trim()

private fun planTiersFrom(products: List<BillingProduct>): List<PlanTier> {
    if (products.isEmpty()) return PLANS
    fun product(plan: UserPlan, period: String) =
        products.firstOrNull { it.kind == "subscription" && it.plan == plan && it.billingPeriod == period }

    return PLANS.map { tier ->
        when (tier.plan) {
            UserPlan.FREE -> tier
            else -> {
                val monthly = product(tier.plan, "month")
                val yearly = product(tier.plan, "year")
                tier.copy(
                    monthlyPrice = monthly?.priceLabel?.let(::cleanPrice) ?: tier.monthlyPrice,
                    yearlyPrice = yearly?.priceLabel?.let(::cleanPrice) ?: tier.yearlyPrice
                )
            }
        }
    }
}

private fun creditPackagesFrom(products: List<BillingProduct>): List<CreditPackage> {
    val creditProducts = products.filter { it.kind == "credit_pack" && it.creditAmount > 0 }
    if (creditProducts.isEmpty()) return CREDIT_PACKAGES
    val accents = listOf(Lime, Forge500, CardCyan, CardPurple)
    return creditProducts.mapIndexed { index, product ->
        CreditPackage(
            credits = product.creditAmount,
            price = product.priceLabel,
            perCredit = "Güvenli checkout",
            badge = product.badge,
            accentColor = accents[index % accents.size]
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun StoreScreen(
    onBack   : () -> Unit,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val theme    = LocalAppTheme.current
    val haptic   = LocalHapticFeedback.current

    var toastMsg     by remember { mutableStateOf<String?>(null) }
    var activeTab    by remember { mutableIntStateOf(0) }
    var selectedPlan by remember { mutableStateOf(UserPlan.PRO) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StoreEvent.ShowToast -> toastMsg = event.message
            }
        }
    }

    LaunchedEffect(state.plan) {
        if (state.plan != UserPlan.FREE) selectedPlan = state.plan
    }

    val plans = remember(state.products) { planTiersFrom(state.products) }
    val creditPackages = remember(state.products) { creditPackagesFrom(state.products) }
    val selectedTier = plans.first { it.plan == selectedPlan }
    val isCurrent    = state.plan == selectedPlan
    val isFree       = selectedPlan == UserPlan.FREE
    val hasPaidPlan  = state.plan != UserPlan.FREE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            StoreTopBar(
                theme       = theme,
                activeTab   = activeTab,
                plan        = state.plan,
                credits     = state.credits,
                onBack      = onBack,
                onTabChange = { activeTab = it }
            )

            PaymentModeBand(
                state = state,
                theme = theme,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { dir * it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(200)) { -dir * it / 4 } + fadeOut(tween(150)))
                },
                modifier = Modifier.fillMaxSize(),
                label    = "store_tab"
            ) { tab ->
                when (tab) {
                    0 -> SubscriptionTab(
                        theme           = theme,
                        haptic          = haptic,
                        state           = state,
                        selectedPlan    = selectedPlan,
                        selectedTier    = selectedTier,
                        isCurrent       = isCurrent,
                        isFree          = isFree,
                        hasPaidPlan     = hasPaidPlan,
                        onSelectPlan    = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedPlan = it
                        },
                        onYearlyChange  = viewModel::setYearly,
                        plans           = plans,
                        onPurchase      = { viewModel.purchasePlan(selectedPlan) },
                        onCancelPlan    = { showCancelDialog = true }
                    )
                    else -> CreditsTab(
                        theme      = theme,
                        haptic     = haptic,
                        state      = state,
                        packages   = creditPackages,
                        onPurchase = { viewModel.purchaseCredits(it) }
                    )
                }
            }
        }

        if (state.pendingOrderId != null) {
            PendingOrderPanel(
                state = state,
                theme = theme,
                onSandboxComplete = viewModel::completeSandboxCheckout,
                onDismiss = viewModel::dismissPendingOrder,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .zIndex(15f)
            )
        }

        // Toast
        AnimatedVisibility(
            visible  = toastMsg != null,
            enter    = slideInVertically { -it } + fadeIn(tween(180)),
            exit     = slideOutVertically { -it } + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
                .zIndex(20f)
        ) {
            toastMsg?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2500)
                    toastMsg = null
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.bg2)
                        .border(1.dp, Lime.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = Lime, modifier = Modifier.size(16.dp))
                    Text(msg, color = theme.text0, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Cancel plan dialog
        if (showCancelDialog) {
            CancelPlanDialog(
                planName  = state.plan.displayName,
                theme     = theme,
                onDismiss = { showCancelDialog = false },
                onConfirm = {
                    showCancelDialog = false
                    viewModel.cancelPlan()
                }
            )
        }
    }
}

@Composable
private fun PendingOrderPanel(
    state: StoreState,
    theme: AppThemeState,
    onSandboxComplete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    val digits = cardNumber.filter { it.isDigit() }
    val canSubmit = state.sandboxAvailable && digits.length >= 12 && expiry.length >= 4 && cvc.length >= 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(theme.bg1)
            .border(1.dp, Lime.copy(0.28f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lime.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ReceiptLong, null, tint = Lime, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Demo ödeme", color = theme.text0, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.sandboxAvailable) "Test kartı ile satın alma akışını tamamla."
                    else "Sandbox kapalı; Supabase secret açılmalı.",
                    color = theme.text2,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Close, null, tint = theme.text2, modifier = Modifier.size(18.dp))
            }
        }

        if (state.sandboxAvailable) {
            Spacer(Modifier.height(12.dp))
            DemoField(
                value = cardNumber,
                onValueChange = { cardNumber = it.filter { ch -> ch.isDigit() || ch == ' ' }.take(23) },
                label = "Kart numarası",
                placeholder = "4242 4242 4242 4242",
                theme = theme
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoField(
                    value = expiry,
                    onValueChange = { expiry = it.filter { ch -> ch.isDigit() || ch == '/' }.take(5) },
                    label = "SKT",
                    placeholder = "12/30",
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                DemoField(
                    value = cvc,
                    onValueChange = { cvc = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "CVC",
                    placeholder = "123",
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Test için 4242 4242 4242 4242, 12/30, 123 kullanabilirsin. Gerçek ödeme alınmaz.",
                color = theme.text2.copy(0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit) Lime.copy(0.14f) else theme.bg2)
                    .border(1.dp, if (canSubmit) Lime.copy(0.35f) else theme.stroke, RoundedCornerShape(14.dp))
                    .clickable(enabled = !state.isLoading && canSubmit) { onSandboxComplete() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Lime, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Science, null, tint = Lime, modifier = Modifier.size(17.dp))
                        Text("Demo kartla ödemeyi tamamla", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Sandbox kapalı. Supabase ortamında BILLING_SANDBOX_ENABLED=true olduğunda test tamamlama açılır.",
                color = theme.text2.copy(0.65f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PaymentModeBand(
    state: StoreState,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (state.billingSandboxAvailable) Lime.copy(0.08f) else Amber.copy(0.07f))
            .border(
                1.dp,
                if (state.billingSandboxAvailable) Lime.copy(0.24f) else Amber.copy(0.22f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (state.billingSandboxAvailable) Icons.Rounded.Science else Icons.Rounded.Lock,
            null,
            tint = if (state.billingSandboxAvailable) Lime else Amber,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                if (state.billingSandboxAvailable) "Test kartı açık" else "Test ödeme kapalı",
                color = theme.text0,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (state.billingSandboxAvailable)
                    "Satın alırken demo kart formu açılır; gerçek çekim yapılmaz."
                else
                    "Supabase Edge Function secret: BILLING_SANDBOX_ENABLED=true",
                color = theme.text2,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun DemoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = theme.text2.copy(0.55f)) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.text0,
            unfocusedTextColor = theme.text0,
            focusedBorderColor = Lime.copy(0.55f),
            unfocusedBorderColor = theme.stroke,
            focusedLabelColor = Lime,
            unfocusedLabelColor = theme.text2,
            cursorColor = Lime,
            focusedContainerColor = theme.bg0,
            unfocusedContainerColor = theme.bg0
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun StoreTopBar(
    theme      : AppThemeState,
    activeTab  : Int,
    plan       : UserPlan,
    credits    : Int,
    onBack     : () -> Unit,
    onTabChange: (Int) -> Unit
) {
    val accent = Lime

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bg0)
            .statusBarsPadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
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
            // Plan/kredi chip
            val isPaid    = plan != UserPlan.FREE
            val chipColor = if (isPaid) accent.copy(0.12f) else accent.copy(0.08f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipColor)
                    .border(1.dp, accent.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isPaid) Icons.Rounded.WorkspacePremium else Icons.Rounded.Bolt,
                    null, tint = accent, modifier = Modifier.size(13.dp)
                )
                Text(
                    if (isPaid) "${plan.displayName} · $credits" else "$credits",
                    color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Tab row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Abonelik" to Icons.Rounded.WorkspacePremium,
                   "AI Kredi"  to Icons.Rounded.Bolt
            ).forEachIndexed { idx, (label, icon) ->
                val isActive = activeTab == idx
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) Lime.copy(0.12f) else theme.bg1)
                        .border(
                            1.dp,
                            if (isActive) Lime.copy(0.45f) else theme.stroke,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabChange(idx) }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(icon, null,
                        tint     = if (isActive) Lime else theme.text2,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        color      = if (isActive) Lime else theme.text2,
                        fontSize   = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = theme.stroke.copy(0.25f), thickness = 0.5.dp)
    }
}

// ── Subscription Tab ──────────────────────────────────────────────────────────

@Composable
private fun SubscriptionTab(
    theme         : AppThemeState,
    haptic        : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state         : StoreState,
    selectedPlan  : UserPlan,
    selectedTier  : PlanTier,
    isCurrent     : Boolean,
    isFree        : Boolean,
    hasPaidPlan   : Boolean,
    onSelectPlan  : (UserPlan) -> Unit,
    onYearlyChange: (Boolean) -> Unit,
    plans         : List<PlanTier>,
    onPurchase    : () -> Unit,
    onCancelPlan  : () -> Unit
) {
    val displayPrice = if (state.isYearly && selectedTier.discountBadge.isNotEmpty())
        selectedTier.yearlyPrice else selectedTier.monthlyPrice
    val displayPeriod = when {
        isFree         -> ""
        state.isYearly -> "/yıl"
        else           -> "/ay"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                AccountSummaryHero(state = state, theme = theme)
                Spacer(Modifier.height(12.dp))
                AiCostMatrix(theme = theme)
                Spacer(Modifier.height(16.dp))
                BillingToggle(
                    isYearly = state.isYearly,
                    onToggle = onYearlyChange,
                    theme    = theme
                )
                Spacer(Modifier.height(14.dp))
            }

            item {
                SectionLabel(
                    title = "Plan seçimi",
                    subtitle = if (state.isYearly) "Yıllık fiyatlandırma aktif" else "Aylık fiyatlandırma aktif",
                    theme = theme
                )
                Spacer(Modifier.height(10.dp))
                PlanSelector(
                    plans        = plans,
                    selectedPlan = selectedPlan,
                    currentPlan  = state.plan,
                    isYearly     = state.isYearly,
                    theme        = theme,
                    onSelect     = onSelectPlan
                )
                Spacer(Modifier.height(14.dp))
            }

            item {
                AnimatedContent(
                    targetState = selectedTier,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 10 })
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

            // İptal butonu — mevcut ücretli plan seçiliyken göster
            if (isCurrent && hasPaidPlan) {
                item {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick  = onCancelPlan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Cancel, null,
                            tint = theme.text2.copy(0.5f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Aboneliği İptal Et",
                            color    = theme.text2.copy(0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Text(
                    "Test modunda demo kartla tamamlanan sipariş hesaba işlenir; gerçek ödeme alınmaz.\nCanlı ödeme sağlayıcısı bağlanınca aynı akış gerçek checkout'a dönecek.",
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    color      = theme.text2.copy(0.4f),
                    fontSize   = 10.sp,
                    textAlign  = TextAlign.Center,
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
                        0f to Color.Transparent,
                        0.3f to theme.bg0.copy(0.92f),
                        1f to theme.bg0
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

@Composable
private fun SectionLabel(
    title: String,
    subtitle: String,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = theme.text2,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun AccountSummaryHero(
    state: StoreState,
    theme: AppThemeState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke.copy(0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Hesap durumu",
                    color = theme.text0,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Abonelik hakları ve kredi bakiyesi ayrı tutulur.",
                    color = theme.text2,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lime.copy(0.1f))
                    .border(1.dp, Lime.copy(0.24f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = Lime, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric(
                label = "Aktif plan",
                value = state.plan.displayName,
                icon = Icons.Rounded.WorkspacePremium,
                accent = if (state.plan == UserPlan.FREE) TextSecondary else Forge500,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "AI kredi",
                value = "${state.credits}",
                icon = Icons.Rounded.Bolt,
                accent = Lime,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg0.copy(0.52f))
            .border(1.dp, accent.copy(0.18f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Column {
            Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AiCostMatrix(theme: AppThemeState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke.copy(0.42f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Lime, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("AI kullanım maliyetleri", color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CostPill("Oracle", "1 kredi", Lime, theme, Modifier.weight(1f))
            CostPill("Program", "8-12", Forge500, theme, Modifier.weight(1f))
            CostPill("Analiz", "3 kredi", CardCyan, theme, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CostPill(
    title: String,
    value: String,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

// ── Credits Tab ───────────────────────────────────────────────────────────────

@Composable
private fun CreditsTab(
    theme     : AppThemeState,
    haptic    : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state     : StoreState,
    packages  : List<CreditPackage>,
    onPurchase: (Int) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Lime.copy(0.1f))
                        .border(1.dp, Lime.copy(0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Bolt, null,
                        tint = Lime, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("AI Kredisi",
                    color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Oracle, program üretimi ve analizler ayrı limitlerle takip edilir. Satın alınan krediler abonelikten bağımsız saklanır.",
                    color     = theme.text2,
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))

                // Mevcut kredi
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Lime.copy(0.08f))
                        .border(1.dp, Lime.copy(0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, null,
                        tint = Lime, modifier = Modifier.size(15.dp))
                    Text("Mevcut kredi  ", color = theme.text2, fontSize = 13.sp)
                    Text("${state.credits}",
                        color = Lime, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        items(packages.size) { idx ->
            val pkg = packages[idx]
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
            BillingSafetyCard(theme = theme)
        }

        if (state.recentUsage.isNotEmpty()) {
            item {
                RecentUsageCard(state = state, theme = theme)
            }
        }

        item {
            Text(
                "Ödeme sağlayıcısı bağlanana kadar bu ekran sadece güvenli checkout kaydı oluşturur; hesaba otomatik kredi veya abonelik eklemez.",
                modifier  = Modifier.fillMaxWidth(),
                color     = theme.text2.copy(0.4f),
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun BillingSafetyCard(theme: AppThemeState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke.copy(0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardCyan.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.VerifiedUser, null, tint = CardCyan, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Güvenli satın alma modu", color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(
                "Kredi bakiyesi ve abonelikler sadece doğrulanmış ödeme webhook'u ile değişir.",
                color = theme.text2,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecentUsageCard(state: StoreState, theme: AppThemeState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(1.dp, theme.stroke.copy(0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text("Son AI kullanımları", color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        state.recentUsage.take(5).forEach { usage ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(toolLabel(usage.tool), color = theme.text1, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(
                    if (usage.creditCost > 0) "-${usage.creditCost} kredi" else usageLabel(usage.source),
                    color = if (usage.creditCost > 0) Lime else theme.text2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun toolLabel(tool: String): String = when (tool) {
    "ORACLE_CHAT" -> "Oracle sohbet"
    "PROGRAM_GENERATE_TEXT" -> "AI program"
    "PROGRAM_GENERATE_MEDIA" -> "Dosyadan program"
    "PROGRAM_EDIT" -> "Program düzenleme"
    "WEIGHT_TREND_ANALYSIS" -> "Kilo analizi"
    "EXERCISE_PROGRESS_ANALYSIS" -> "Egzersiz analizi"
    "WORKOUT_PROGRESS_ANALYSIS" -> "Antrenman analizi"
    "ORACLE_TO_PROGRAM" -> "Oracle program ekleme"
    else -> tool.lowercase().replace('_', ' ')
}

private fun usageLabel(source: String): String = when (source) {
    "free_limit" -> "ücretsiz hak"
    "plan_limit" -> "plan hakkı"
    else -> source
}

// ── Billing Toggle ────────────────────────────────────────────────────────────

@Composable
private fun BillingToggle(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme   : AppThemeState
) {
    val density = LocalDensity.current

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
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(false) }
        )

        Spacer(Modifier.width(14.dp))

        // Toggle — animasyon dp→px dönüşümü ile
        val trackColor by animateColorAsState(
            if (isYearly) Lime else theme.stroke,
            tween(200), label = "track"
        )
        // Thumb ne kadar kayacak (px cinsinden)
        val thumbTravelPx = with(density) { 20.dp.toPx() }
        val thumbOffsetPx by animateFloatAsState(
            targetValue   = if (isYearly) thumbTravelPx else 0f,
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label         = "thumb"
        )

        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(trackColor)
                .clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(!isYearly) }
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .offset { IntOffset(thumbOffsetPx.toInt(), 0) }
                    .size(20.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isYearly) LimeText else Color.White)
            )
        }

        Spacer(Modifier.width(14.dp))

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "YILLIK",
                color      = if (isYearly) theme.text0 else theme.text2,
                fontSize   = 12.sp,
                fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Normal,
                modifier   = Modifier.clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(true) }
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Lime.copy(0.14f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("-%40", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Plan Selector ─────────────────────────────────────────────────────────────

@Composable
private fun PlanSelector(
    plans       : List<PlanTier>,
    selectedPlan: UserPlan,
    currentPlan : UserPlan,
    isYearly    : Boolean,
    theme       : AppThemeState,
    onSelect    : (UserPlan) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
    val accent = if (isSelected) Lime else theme.stroke
    val price  = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice

    val selColor = tier.accentColor.takeIf { tier.plan != UserPlan.FREE } ?: Lime
    val bgColor by animateColorAsState(
        if (isSelected) selColor.copy(0.1f) else theme.bg1,
        tween(180), label = "bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) selColor.copy(0.55f) else theme.stroke.copy(0.4f),
        tween(180), label = "border"
    )

    // Sabit yükseklik — tüm kartlar eşit
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Badge satırı
        Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.Center) {
            if (tier.badge != null) {
                Text(
                    tier.badge,
                    color         = if (isSelected) selColor else theme.text2,
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Plan adı
        Text(
            tier.label,
            color      = if (isSelected) selColor else theme.text1,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
        )

        // Fiyat
        Text(
            price,
            color      = if (isSelected) theme.text0 else theme.text2,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black
        )

        // Aktif badge — sabit yükseklik
        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Lime.copy(0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("AKTİF", color = Lime, fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
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
    val accent    = tier.accentColor
    val isFree    = tier.plan == UserPlan.FREE
    val price     = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period    = when { isFree -> ""; isYearly -> "/yıl"; else -> "/ay" }
    val tierLevel = tier.plan.ordinal()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg1)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) accent.copy(0.5f) else theme.stroke.copy(0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        // Fiyat + ikon
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = price,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "price_anim"
                    ) { p ->
                        Text(p, color = theme.text0, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                    if (period.isNotEmpty()) {
                        Text(period,
                            color    = theme.text2,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    }
                }
                if (isYearly && tier.discountBadge.isNotEmpty()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.padding(top = 2.dp)
                    ) {
                        Text(tier.yearlyPerMonth, color = theme.text2, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Lime.copy(0.13f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(tier.discountBadge, color = Lime, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (!isFree) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(0.12f))
                        .border(1.dp, accent.copy(0.25f), RoundedCornerShape(12.dp)),
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

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = theme.stroke.copy(0.3f), thickness = 0.5.dp)
        Spacer(Modifier.height(14.dp))

        // Master feature list — açık ✓ / kilitli 🔒
        ALL_FEATURES.forEach { feature ->
            val unlocked = tierLevel >= feature.minPlan.ordinal()
            val rowAccent = when {
                !unlocked             -> theme.stroke
                feature.minPlan == UserPlan.ELITE -> CardPurple
                feature.minPlan == UserPlan.PRO   -> Forge500
                else                              -> Lime.copy(0.7f)
            }

            Row(
                modifier              = Modifier.padding(vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon kutusu
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (unlocked) rowAccent.copy(0.12f) else theme.bg2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (unlocked) feature.icon else Icons.Rounded.Lock,
                        null,
                        tint     = rowAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Metin
                Text(
                    feature.text,
                    color      = if (unlocked) theme.text1 else theme.text2.copy(0.45f),
                    fontSize   = 13.sp,
                    lineHeight = 18.sp,
                    modifier   = Modifier.weight(1f),
                    textDecoration = if (!unlocked) androidx.compose.ui.text.style.TextDecoration.None else null
                )

                // Sağ: hangi planla açılır chip (kilitliyse)
                if (!unlocked) {
                    val unlockAccent = when (feature.minPlan) {
                        UserPlan.ELITE -> CardPurple
                        UserPlan.PRO   -> Forge500
                        else           -> Lime
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(unlockAccent.copy(0.1f))
                            .border(1.dp, unlockAccent.copy(0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            feature.minPlan.displayName,
                            color      = unlockAccent,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (unlocked && feature.minPlan == tier.plan && feature.minPlan != UserPlan.FREE) {
                    // Bu planla yeni açılan özellik
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rowAccent.copy(0.1f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("YENİ", color = rowAccent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
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
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1)
            .border(
                width = if (pkg.badge != null) 1.dp else 1.dp,
                color = if (pkg.badge != null) Lime.copy(0.35f) else theme.stroke.copy(0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled           = !isLoading,
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPurchase
            )
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pkg.accentColor.copy(0.1f))
                .border(1.dp, pkg.accentColor.copy(0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, null,
                tint = pkg.accentColor, modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${pkg.credits} Kredi",
                    color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Black)
                if (pkg.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Lime.copy(0.13f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(pkg.badge, color = Lime, fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(pkg.perCredit, color = theme.text2, fontSize = 11.sp)
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                color       = Lime,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lime.copy(0.12f))
                    .border(1.dp, Lime.copy(0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(pkg.price, color = Lime, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ── Sticky CTA ────────────────────────────────────────────────────────────────

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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isCurrent)
                        Brush.linearGradient(listOf(Surface3, Surface3))
                    else
                        Brush.linearGradient(
                            listOf(accentColor, accentColor.copy(0.8f)),
                            start = Offset(0f, 0f),
                            end   = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                )
                .clickable(
                    enabled           = !isCurrent && !isLoading,
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onClick
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = if (isCurrent) TextMuted else LimeText,
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
                    fontSize   = 15.sp
                )
            }
        }

        if (!isCurrent) {
            Spacer(Modifier.height(5.dp))
            Text(
                buildString {
                    if (yearlyPerMonth.isNotEmpty()) append("$yearlyPerMonth  ·  ")
                    append("İstediğin zaman iptal et  ·  Taahhüt yok")
                },
                color     = TextMuted,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Cancel Plan Dialog ────────────────────────────────────────────────────────

@Composable
private fun CancelPlanDialog(
    planName : String,
    theme    : AppThemeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.6f))
            .clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(theme.bg1)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(theme.stroke.copy(0.5f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(horizontal = 24.dp)
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
            Spacer(Modifier.height(24.dp))

            Icon(Icons.Rounded.Cancel, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp))

            Spacer(Modifier.height(12.dp))
            Text("Aboneliği İptal Et",
                color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "$planName aboneliğini iptal etmek istediğine emin misin?\nMevcut dönem sonunda ücretsiz plana geçilecek.",
                color     = theme.text2,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Vazgeç", color = theme.text2, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(0.4f), RoundedCornerShape(12.dp))
                        .clickable { onConfirm() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("İptal Et",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Paywall Dialog ────────────────────────────────────────────────────────────

@Composable
fun PaywallDialog(
    onDismiss  : () -> Unit,
    onGoToStore: () -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.65f))
            .clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(theme.bg1)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(theme.stroke.copy(0.5f), Color.Transparent)),
                    RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                )
                .clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(horizontal = 26.dp)
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
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Lime.copy(0.1f))
                    .border(1.dp, Lime.copy(0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null,
                    tint = Lime, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(16.dp))
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

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Lime.copy(0.12f))
                    .border(1.dp, Lime.copy(0.35f), RoundedCornerShape(14.dp))
                    .clickable { onGoToStore() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Planları Gör", color = Lime, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Vazgeç", color = theme.text2, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
