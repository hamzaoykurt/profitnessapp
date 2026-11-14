package com.cosmibit.profitness.presentation.store

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.LocalWindowClass
import com.cosmibit.profitness.core.ui.SinemaFitText
import com.cosmibit.profitness.core.ui.SinemaLayout
import com.cosmibit.profitness.data.store.BillingProduct
import com.cosmibit.profitness.data.store.UserPlan
import com.cosmibit.profitness.presentation.components.LocalFloatingTimerReserve
import com.cosmibit.profitness.presentation.profile.photoFadeToBackdrop
import com.cosmibit.profitness.presentation.components.SinemaBigStat
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaGlowBar
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow

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

private data class EnergyPackage(
    val amount     : Int,
    val price      : String,
    val perEnergy  : String,
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
    MasterFeature(Icons.Rounded.FitnessCenter,    "Program antrenman takibi",            UserPlan.FREE),
    MasterFeature(Icons.Rounded.BarChart,          "Temel gelişim metrikleri",            UserPlan.FREE),
    MasterFeature(Icons.Rounded.SelfImprovement,   "Manuel program oluşturma",            UserPlan.FREE),
    MasterFeature(Icons.Rounded.ChatBubbleOutline, "AI Koç sohbet paketi",                UserPlan.PRO),
    MasterFeature(Icons.Rounded.AllInclusive,      "Yüksek limitli AI Koç sohbeti",       UserPlan.PRO),
    MasterFeature(Icons.Rounded.AutoAwesome,       "AI ile program üretimi",              UserPlan.PRO),
    MasterFeature(Icons.AutoMirrored.Rounded.TrendingUp, "Antrenman performans analizi",  UserPlan.PRO),
    MasterFeature(Icons.AutoMirrored.Rounded.ShowChart,  "Egzersiz ilerleme grafikleri",  UserPlan.PRO),
    MasterFeature(Icons.Rounded.MonitorWeight,     "Kilo ve trend AI yorumu",             UserPlan.PRO),
    MasterFeature(Icons.Rounded.Person,            "Kişisel AI antrenör profili",         UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Psychology,        "Elite koç hafızası",                  UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Support,           "Öncelikli destek",                    UserPlan.ELITE),
    MasterFeature(Icons.Rounded.NewReleases,       "Erken erişim özellikleri",            UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Diamond,           "Tüm program özellikleri dahil",       UserPlan.ELITE)
)

// Plan sıralaması: FREE < PRO < ELITE
private fun UserPlan.ordinal() = when (this) {
    UserPlan.FREE  -> 0
    UserPlan.PRO   -> 1
    UserPlan.ELITE -> 2
}

private fun UserPlan.localizedDisplayName(theme: AppThemeState): String = when (this) {
    UserPlan.FREE -> theme.t("Ücretsiz", "Free")
    UserPlan.PRO -> "Pro"
    UserPlan.ELITE -> "Elite"
}

private fun PlanTier.localizedLabel(theme: AppThemeState): String = when (plan) {
    UserPlan.FREE -> theme.t("Ücretsiz", "Free")
    UserPlan.PRO -> label
    UserPlan.ELITE -> label
}

private fun PlanTier.localizedBadge(theme: AppThemeState): String? = when (badge) {
    "POPÜLER" -> theme.t("POPÜLER", "POPULAR")
    "EN İYİ" -> theme.t("EN İYİ", "BEST")
    else -> badge
}

private fun EnergyPackage.localizedBadge(theme: AppThemeState): String? = when (badge) {
    "EN POPÜLER" -> theme.t("EN POPÜLER", "MOST POPULAR")
    else -> badge
}

private fun EnergyPackage.localizedPerEnergy(theme: AppThemeState): String =
    if (perEnergy == "Güvenli checkout") theme.t("Güvenli checkout", "Secure checkout") else perEnergy

private fun energyLabel(amount: Int, theme: AppThemeState): String =
    theme.t("$amount Enerji", "$amount Energy")

private fun MasterFeature.localizedText(theme: AppThemeState): String = when (text) {
    "Program antrenman takibi" -> theme.t(text, "Program workout tracking")
    "Temel gelişim metrikleri" -> theme.t(text, "Basic progress metrics")
    "Manuel program oluşturma" -> theme.t(text, "Manual program creation")
    "AI Koç sohbet paketi" -> theme.t(text, "AI Coach chat pack")
    "Yüksek limitli AI Koç sohbeti" -> theme.t(text, "High-limit AI Coach chat")
    "AI ile program üretimi" -> theme.t(text, "AI program generation")
    "Antrenman performans analizi" -> theme.t(text, "Workout performance analysis")
    "Egzersiz ilerleme grafikleri" -> theme.t(text, "Exercise progress charts")
    "Kilo ve trend AI yorumu" -> theme.t(text, "Weight and trend AI insight")
    "Kişisel AI antrenör profili" -> theme.t(text, "Personal AI coach profile")
    "Elite koç hafızası" -> theme.t(text, "Elite coach memory")
    "Öncelikli destek" -> theme.t(text, "Priority support")
    "Erken erişim özellikleri" -> theme.t(text, "Early access features")
    "Tüm program özellikleri dahil" -> theme.t(text, "All program features included")
    else -> text
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
        accentColor   = TextSecondary,
        badge         = "POPÜLER"
    ),
    PlanTier(
        plan          = UserPlan.ELITE,
        label         = "Elite",
        monthlyPrice  = "₺249",
        yearlyPrice   = "₺1.799",
        yearlyPerMonth= "≈₺150/ay",
        discountBadge = "%40 indirim",
        accentColor   = TextSecondary,
        badge         = "EN İYİ"
    )
)

private val ENERGY_PACKAGES = listOf(
    EnergyPackage(10,  "₺29",  "Kısa AI hamleleri", null,         TextSecondary),
    EnergyPackage(25,  "₺59",  "Hafif kullanım",     null,         TextSecondary),
    EnergyPackage(50,  "₺99",  "Dengeli kullanım", "EN POPÜLER", TextSecondary),
    EnergyPackage(100, "₺169", "Avantajlı rezerv",  null,         TextSecondary),
    EnergyPackage(200, "₺299", "Yoğun AI rezervi",  null,         TextSecondary),
    EnergyPackage(500, "₺649", "Maksimum rezerv",   null,         TextSecondary)
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

private fun energyPackagesFrom(products: List<BillingProduct>): List<EnergyPackage> {
    val creditProducts = products.filter { it.kind == "credit_pack" && it.creditAmount > 0 }
    if (creditProducts.isEmpty()) return ENERGY_PACKAGES
    val packagesByAmount = ENERGY_PACKAGES.associateBy { it.amount }.toMutableMap()
    creditProducts.map { product ->
        val fallback = packagesByAmount[product.creditAmount]
        EnergyPackage(
            amount = product.creditAmount,
            price = product.priceLabel,
            perEnergy = fallback?.perEnergy.orEmpty(),
            badge = product.badge ?: fallback?.badge,
            accentColor = TextSecondary
        )
    }.forEach { pkg ->
        packagesByAmount[pkg.amount] = pkg
    }
    return packagesByAmount.values.sortedBy { it.amount }
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
    val energyPackages = remember(state.products) { energyPackagesFrom(state.products) }
    val hasPaidPlan  = state.plan != UserPlan.FREE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(theme))
    ) {
        SinemaBackdrop(Modifier.fillMaxSize())
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

            // Tab content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(SinemaMotion.Standard)) { dir * it / 3 } + fadeIn(tween(SinemaMotion.Standard))) togetherWith
                    (slideOutHorizontally(tween(SinemaMotion.Standard)) { -dir * it / 4 } + fadeOut(tween(SinemaMotion.Quick)))
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = SinemaLayout.ContentMaxList)
                    .fillMaxSize(),
                label    = "store_tab"
            ) { tab ->
                when (tab) {
                    0 -> SubscriptionTab(
                        theme           = theme,
                        haptic          = haptic,
                        state           = state,
                        selectedPlan    = selectedPlan,
                        hasPaidPlan     = hasPaidPlan,
                        onSelectPlan    = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedPlan = it
                        },
                        onYearlyChange  = viewModel::setYearly,
                        plans           = plans,
                        onPurchase      = { viewModel.purchasePlan(it) },
                        onCancelPlan    = { showCancelDialog = true }
                    )
                    else -> EnergyTab(
                        theme      = theme,
                        haptic     = haptic,
                        state      = state,
                        packages   = energyPackages,
                        onPurchase = { viewModel.purchaseCredits(it) }
                    )
                }
            }
        }

        if (state.pendingOrderId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.48f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.dismissPendingOrder() }
                    .zIndex(14f)
            )
            PendingOrderPanel(
                state = state,
                theme = theme,
                onSandboxComplete = viewModel::completeSandboxCheckout,
                onDismiss = viewModel::dismissPendingOrder,
                modifier = Modifier
                    .align(Alignment.Center)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .zIndex(15f)
            )
        }

        // Toast
        AnimatedVisibility(
            visible  = toastMsg != null,
            enter    = slideInVertically { -it } + fadeIn(tween(SinemaMotion.Standard)),
            exit     = slideOutVertically { -it } + fadeOut(tween(SinemaMotion.Quick)),
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
                SinemaCapsule(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    height = 44.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) { fg ->
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = theme.effectiveAccentColor, modifier = Modifier.size(16.dp))
                    Text(msg, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Cancel plan dialog
        if (showCancelDialog) {
            CancelPlanDialog(
                planName  = state.plan.localizedDisplayName(theme),
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
    val accent = theme.effectiveAccentColor
    val textMain = Sinema.text(theme)
    val textSub = Sinema.textSecondary(theme)
    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var expiry by remember { mutableStateOf("12/30") }
    var cvc by remember { mutableStateOf("123") }
    val digits = cardNumber.filter { it.isDigit() }
    val expiryDigits = expiry.filter { it.isDigit() }
    val canSubmit = state.sandboxAvailable && digits.length == 16 && expiryDigits.length == 4 && cvc.length >= 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.t("Demo ödeme", "Demo payment"), color = textMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.sandboxAvailable) theme.t("Test kartı ile satın alma akışını tamamla.", "Complete the purchase flow with a test card.")
                    else theme.t("Demo ödeme modu şu an kapalı.", "Demo payment mode is currently off."),
                    color = textSub,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, theme.t("Kapat", "Close"), tint = textSub, modifier = Modifier.size(20.dp))
            }
        }

        if (state.sandboxAvailable) {
            Spacer(Modifier.height(12.dp))
            DemoField(
                value = cardNumber,
                onValueChange = {
                    cardNumber = it
                        .filter { ch -> ch.isDigit() }
                        .take(16)
                        .chunked(4)
                        .joinToString(" ")
                },
                label = theme.t("Kart numarası", "Card number"),
                placeholder = "4242 4242 4242 4242",
                theme = theme
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DemoField(
                    value = expiry,
                    onValueChange = {
                        val raw = it.filter { ch -> ch.isDigit() }.take(4)
                        expiry = if (raw.length > 2) "${raw.take(2)}/${raw.drop(2)}" else raw
                    },
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
            Spacer(Modifier.height(10.dp))
            Text(
                theme.t("Test kartı hazır dolduruldu. Gerçek ödeme alınmaz.", "The test card is prefilled. No real payment is charged."),
                color = textSub,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(14.dp))
            StoreGoButton(
                label = theme.t("Demo ödemeyi tamamla", "Complete demo payment"),
                icon = Icons.Rounded.Science,
                enabled = canSubmit,
                loading = state.isLoading,
                onClick = onSandboxComplete
            )
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                theme.t(
                    "Ödeme tamamlandığında üyelik ve Enerji hakların otomatik olarak hesabına yansır.",
                    "Membership and Energy benefits are applied automatically after payment is completed."
                ),
                color = textSub,
                fontSize = 12.sp,
                lineHeight = 16.sp
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
    val accent = theme.effectiveAccentColor
    SinemaCapsule(
        modifier = modifier.fillMaxWidth(),
        height = 56.dp,
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) { fg ->
        Icon(
            if (state.billingSandboxAvailable) Icons.Rounded.Science else Icons.Rounded.Lock,
            null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.billingSandboxAvailable) theme.t("Demo ödeme modu açık", "Demo payment mode on") else theme.t("Demo ödeme modu kapalı", "Demo payment mode off"),
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (state.billingSandboxAvailable)
                    theme.t("Gerçek çekim yapılmadan akışı deneyebilirsin.", "Test the flow without a real charge.")
                else
                    theme.t("Ödeme tamamlanınca haklar hesabına işlenir.", "Benefits are applied when payment is completed."),
                color = fg.copy(0.7f),
                fontSize = 11.sp,
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
    SinemaGlassField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        modifier = modifier.fillMaxWidth()
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
    val accent = theme.effectiveAccentColor
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val wc = LocalWindowClass.current
    val gutter = SinemaLayout.gutter(wc.widthDp)
    val coverH = SinemaLayout.cover(wc.heightDp, 0.275f, 200.dp, 280.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(coverH + topInset)
    ) {
        // Salon fotoğrafı; yüklenmezse SinemaScene
        Box(Modifier.fillMaxSize().photoFadeToBackdrop(90.dp)) {
        coil.compose.SubcomposeAsyncImage(
            model = com.cosmibit.profitness.core.ui.categoryImageUrl(""),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { SinemaScene() },
            error = { SinemaScene() }
        )
        // Karartma: beyaz yazı için alt bölgede >= %85 siyah
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(0.40f),
                        0.5f to Color.Black.copy(0.46f),
                        1f to Color.Black.copy(0.85f)
                    )
                )
        )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = gutter, end = gutter, top = 10.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SinemaCapsuleIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = theme.t("Geri", "Back"),
                    onClick = onBack,
                    selected = false,
                    size = 48.dp
                )
                // Foto üstü: her zaman koyu cam segment
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(theme.t("Üyelik", "Membership"), theme.t("Enerji", "Energy")).forEachIndexed { idx, label ->
                        SinemaCapsule(
                            modifier = Modifier.weight(1f),
                            selected = activeTab == idx,
                            onClick = { onTabChange(idx) },
                            height = 48.dp,
                            horizontalArrangement = Arrangement.Center,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { fg ->
                            Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                SinemaCapsule(
                    height = 48.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) { fg ->
                    Icon(Icons.Rounded.Bolt, null, tint = theme.accent3Color, modifier = Modifier.size(16.dp))
                    Text("$credits", color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            Spacer(Modifier.weight(1f))
            StoreAccountStrip(plan = plan, theme = theme)
        }
    }
}

// ── Subscription Tab ──────────────────────────────────────────────────────────

@Composable
private fun SubscriptionTab(
    theme         : AppThemeState,
    haptic        : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state         : StoreState,
    selectedPlan  : UserPlan,
    hasPaidPlan   : Boolean,
    onSelectPlan  : (UserPlan) -> Unit,
    onYearlyChange: (Boolean) -> Unit,
    plans         : List<PlanTier>,
    onPurchase    : (UserPlan) -> Unit,
    onCancelPlan  : () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SinemaLayout.gutter(LocalWindowClass.current.widthDp), end = SinemaLayout.gutter(LocalWindowClass.current.widthDp), top = 10.dp, bottom = 28.dp + LocalFloatingTimerReserve.current),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.billingSandboxAvailable) {
            item {
                PaymentModeBand(state = state, theme = theme)
            }
        }

        item {
            BillingCycleSelector(
                isYearly = state.isYearly,
                onToggle = onYearlyChange,
                theme = theme
            )
        }

        items(plans.size) { index ->
            val tier = plans[index]
            PlanOfferCard(
                tier = tier,
                currentPlan = state.plan,
                selected = selectedPlan == tier.plan,
                isYearly = state.isYearly,
                isLoading = state.isLoading,
                hasPaidPlan = hasPaidPlan,
                theme = theme,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectPlan(tier.plan)
                },
                onPurchase = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (tier.plan == UserPlan.FREE) onCancelPlan() else onPurchase(tier.plan)
                }
            )
        }

        if (hasPaidPlan) {
            item {
                SinemaCapsule(
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                    onClick = onCancelPlan,
                    horizontalArrangement = Arrangement.Center
                ) { fg ->
                    Icon(Icons.Rounded.Cancel, null, tint = fg.copy(0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(theme.t("Aboneliği iptal et", "Cancel subscription"), color = fg.copy(0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

    }
}

@Composable
private fun StoreAccountStrip(
    plan: UserPlan,
    theme: AppThemeState
) {
    // Foto üstünde: her zaman beyaz (karartma altı)
    Column(Modifier.fillMaxWidth()) {
        Text(
            "${theme.t("Program Merkezi", "Program Hub")} · ${theme.t("AKTİF PLAN", "ACTIVE PLAN")}".sinemaUpper(theme),
            color = Color.White.copy(0.78f), style = Sinema.Label.copy(fontSize = 11.sp), maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            plan.localizedDisplayName(theme),
            color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.01).em, maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            theme.t("Planın, Enerjin ve AI hamlelerin", "Your plan, Energy, and AI moves"),
            color = Color.White.copy(0.78f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1
        )
    }
}

@Composable
private fun BillingCycleSelector(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme: AppThemeState
) {
    Column(Modifier.fillMaxWidth()) {
        SinemaSegmentRow(
            options = listOf(false, true),
            selected = isYearly,
            label = { yearly -> if (yearly) theme.t("Yıllık", "Yearly") else theme.t("Aylık", "Monthly") },
            onSelect = onToggle,
            height = 48.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isYearly) theme.t("%40'a varan avantaj", "Up to 40% value") else theme.t("Esnek", "Flexible"),
            color = Sinema.textSecondary(theme), fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PlanOfferCard(
    tier: PlanTier,
    currentPlan: UserPlan,
    selected: Boolean,
    isYearly: Boolean,
    isLoading: Boolean,
    hasPaidPlan: Boolean,
    theme: AppThemeState,
    onSelect: () -> Unit,
    onPurchase: () -> Unit
) {
    val accent = theme.effectiveAccentColor
    val textMain = Sinema.text(theme)
    val textSub = Sinema.textSecondary(theme)
    val isCurrent = currentPlan == tier.plan
    val price = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period = when {
        tier.plan == UserPlan.FREE -> ""
        isYearly -> theme.t("/yıl", "/yr")
        else -> theme.t("/ay", "/mo")
    }
    val ctaLabel = when {
        isCurrent -> theme.t("Aktif mod", "Current mode")
        tier.plan == UserPlan.FREE && hasPaidPlan -> theme.t("Ücretsiz moda dön", "Return to Free mode")
        tier.plan == UserPlan.FREE -> theme.t("Ücretsiz mod", "Free mode")
        else -> theme.t("${tier.localizedLabel(theme)} modunu aç", "Unlock ${tier.localizedLabel(theme)} mode")
    }
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(if (isPressed) 0.985f else 1f, label = "plan_offer_press")
    val priceSize = if (selected) 56.sp else 28.sp

    Column(
        modifier = Modifier
            .scale(cardScale)
            .fillMaxWidth()
            .storeCard(theme, accent, selected, if (selected) 28.dp else 24.dp)
            .clickable(
                indication = null,
                interactionSource = interaction,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onSelect
            )
            .padding(if (selected) 20.dp else 16.dp)
    ) {
        Text(
            when (tier.plan) {
                UserPlan.FREE -> theme.t("GÜNLÜK MOD", "DAILY MODE")
                UserPlan.PRO -> theme.t("PRO MODU", "PRO MODE")
                UserPlan.ELITE -> theme.t("ELITE MOD", "ELITE MODE")
            },
            color = textSub, style = Sinema.Label.copy(fontSize = 11.sp)
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tier.localizedLabel(theme), color = textMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            tier.localizedBadge(theme)?.let { badge -> StoreChip(badge, filled = true, theme = theme) }
            if (isCurrent) StoreChip(theme.t("AKTİF", "ACTIVE"), filled = true, theme = theme)
        }
        Spacer(Modifier.height(4.dp))
        Text(planOneLiner(tier.plan, theme), color = textSub, fontSize = 13.sp, lineHeight = 18.sp)

        Spacer(Modifier.height(if (selected) 14.dp else 10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            SinemaFitText(
                text = price,
                style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04).em),
                color = textMain,
                baseSp = priceSize.value,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (period.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    period, color = textSub, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = if (selected) 10.dp else 4.dp)
                )
            }
        }

        if (isYearly && tier.discountBadge.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tier.yearlyPerMonth, color = textSub, fontSize = 12.sp)
                StoreChip(tier.discountBadge, filled = false, theme = theme)
            }
        }

        Spacer(Modifier.height(12.dp))
        planHighlights(tier.plan, theme).take(2).forEach { (icon, text) ->
            CompactPlanFeature(icon = icon, text = text, accent = accent, theme = theme)
        }

        Spacer(Modifier.height(14.dp))
        when {
            isCurrent -> SinemaGoOrIdle(
                label = ctaLabel,
                icon = Icons.Rounded.WorkspacePremium,
                enabled = false,
                onClick = {}
            )
            selected -> StoreGoButton(
                label = ctaLabel,
                icon = Icons.Rounded.WorkspacePremium,
                enabled = !isLoading,
                loading = isLoading,
                onClick = onPurchase
            )
            else -> SinemaCapsule(
                modifier = Modifier.fillMaxWidth().alpha(if (isLoading) 0.5f else 1f),
                height = 52.dp,
                onClick = onPurchase,
                enabled = !isLoading,
                horizontalArrangement = Arrangement.Center
            ) { fg ->
                Text(ctaLabel, color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

/** Mağaza kartı: öne çıkan = accent 1.5dp kenar + glow; diğerleri nötr sinemaCard. */
private fun Modifier.storeCard(theme: AppThemeState, accent: Color, featured: Boolean, radius: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return if (featured) {
        this
            .sinemaGlow(accent.copy(0.4f), 20.dp, 8.dp, shape)
            .clip(shape)
            .background(Sinema.surface(theme))
            .border(1.5.dp, accent, shape)
    } else {
        this.sinemaCard(theme, shape)
    }
}

/** Küçük cam rozet (SinemaCapsule ailesi): dolu = accent + glow. */
@Composable
private fun StoreChip(text: String, filled: Boolean, theme: AppThemeState) {
    if (filled) {
        // c3 = işaretle: öne çıkan rozet (dolgu düğme/taş değil, %16 örtü)
        Box(
            Modifier.height(24.dp).clip(CircleShape).background(theme.accent3Color.copy(0.16f)).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = theme.accent3TextColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        return
    }
    SinemaCapsule(
        height = 24.dp,
        shape = CircleShape,
        selected = filled,
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) { fg ->
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Ana eylem: yükleniyor = aynı düğme dim + kilitli; pasif = nötr gri. */
@Composable
private fun StoreGoButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    when {
        loading -> SinemaGoButton(label = label, icon = icon, onClick = {}, dim = true)
        !enabled -> SinemaGoOrIdle(label = label, icon = icon, enabled = false, onClick = onClick)
        else -> SinemaGoButton(label = label, icon = icon, onClick = onClick)
    }
}

@Composable
private fun CompactPlanFeature(
    icon: ImageVector,
    text: String,
    accent: Color,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Text(text, color = Sinema.text(theme), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

private fun planOneLiner(plan: UserPlan, theme: AppThemeState): String = when (plan) {
    UserPlan.FREE -> theme.t("Temel takip; AI hakları dahil değildir.", "Basic tracking; AI allowance is not included.")
    UserPlan.PRO -> theme.t("AI üretim, analiz ve AI Koç akışı açılır.", "Unlock AI creation, analysis, and AI Coach flow.")
    UserPlan.ELITE -> theme.t("Pro'nun üstüne kişisel koç, öncelik ve üst limitler.", "Pro plus personal coaching, priority, and higher limits.")
}

private fun periodLabel(period: String, theme: AppThemeState): String = when (period) {
    theme.t("aylık", "monthly") -> theme.t("/ay", "/mo")
    theme.t("yıllık", "yearly") -> theme.t("/yıl", "/yr")
    else -> ""
}

@Composable
private fun StoreSectionHeader(
    title: String,
    subtitle: String,
    theme: AppThemeState,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Sinema.textSecondary(theme), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        trailing?.invoke()
    }
}

// ── Energy Tab ────────────────────────────────────────────────────────────────

@Composable
private fun EnergyTab(
    theme     : AppThemeState,
    haptic    : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state     : StoreState,
    packages  : List<EnergyPackage>,
    onPurchase: (Int) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(start = SinemaLayout.gutter(LocalWindowClass.current.widthDp), end = SinemaLayout.gutter(LocalWindowClass.current.widthDp), top = 10.dp, bottom = 28.dp + LocalFloatingTimerReserve.current),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SimpleEnergyBalanceCard(state = state, theme = theme)
        }

        if (state.billingSandboxAvailable) {
            item {
                PaymentModeBand(state = state, theme = theme)
            }
        }

        item {
            StoreSectionHeader(
                title = theme.t("Enerji paketleri", "Energy packs"),
                subtitle = "",
                theme = theme
            )
        }

        items(packages.size) { idx ->
            val pkg = packages[idx]
            SimpleEnergyPackageRow(
                pkg = pkg,
                theme = theme,
                isLoading = state.isLoading,
                onPurchase = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPurchase(pkg.amount)
                }
            )
        }
    }
}

@Composable
private fun SimpleEnergyBalanceCard(
    state: StoreState,
    theme: AppThemeState
) {
    val capacity = energyCapacityFor(state.credits)
    val fillTarget = (state.credits.toFloat() / capacity.toFloat()).coerceIn(0.04f, 1f)
    val fill by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(durationMillis = 900, easing = SinemaMotion.EaseStandard),
        label = "energy_reserve_fill"
    )
    val creditsText = state.credits.toString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sinemaCard(theme, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        SinemaBigStat(
            value = creditsText,
            label = theme.t("ENERJİ", "ENERGY"),
            unit = theme.t("Enerji", "Energy"),
            size = if (creditsText.length >= 5) 64 else 88
        )
        Spacer(Modifier.height(18.dp))
        SinemaGlowBar(progress = fill, height = 10.dp, color = theme.accent2Color)
    }
}

private fun energyCapacityFor(credits: Int): Int = when {
    credits <= 32 -> 32
    credits <= 100 -> 100
    credits <= 250 -> 250
    else -> (((credits + 99) / 100) * 100).coerceAtLeast(300)
}

@Composable
private fun SimpleEnergyPackageRow(
    pkg: EnergyPackage,
    theme: AppThemeState,
    isLoading: Boolean,
    onPurchase: () -> Unit
) {
    val accent = theme.effectiveAccentColor
    val featured = pkg.localizedBadge(theme) != null
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(if (isPressed) 0.985f else 1f, label = "energy_pack_press")

    Row(
        modifier = Modifier
            .scale(rowScale)
            .fillMaxWidth()
            .alpha(if (isLoading) 0.5f else 1f)
            .storeCard(theme, accent, featured, 24.dp)
            .clickable(
                enabled = !isLoading,
                indication = null,
                interactionSource = interaction,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onPurchase
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    pkg.amount.toString(),
                    color = Sinema.text(theme), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.04).em, maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    theme.t("Enerji", "Energy"),
                    color = Sinema.textSecondary(theme), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            pkg.localizedBadge(theme)?.let { badge ->
                Spacer(Modifier.height(8.dp))
                StoreChip(badge, filled = true, theme = theme)
            }
        }
        Text(
            pkg.price,
            color = Sinema.text(theme), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.02).em, maxLines = 1
        )
    }
}

private fun energyPackageIntensity(amount: Int): Int = when {
    amount >= 200 -> 3
    amount >= 50 -> 2
    else -> 1
}

private fun planHighlights(plan: UserPlan, theme: AppThemeState): List<Pair<ImageVector, String>> = when (plan) {
    UserPlan.FREE -> listOf(
        Icons.Rounded.FitnessCenter to theme.t("Manuel antrenman takibi.", "Manual workout tracking."),
        Icons.Rounded.SelfImprovement to theme.t("Programını kendin oluşturma.", "Build your own program."),
        Icons.Rounded.BarChart to theme.t("Temel gelişim ve profil özeti.", "Basic progress and profile summary."),
    )
    UserPlan.PRO -> listOf(
        Icons.Rounded.ChatBubbleOutline to theme.t("AI Koç sohbet paketleri dahil.", "AI Coach chat bundles included."),
        Icons.Rounded.AutoAwesome to theme.t("AI program üretimi ve düzenleme.", "AI program generation and editing."),
        Icons.AutoMirrored.Rounded.TrendingUp to theme.t("Antrenman, kilo ve egzersiz AI analizleri.", "Workout, weight, and exercise AI analysis."),
        Icons.AutoMirrored.Rounded.ShowChart to theme.t("Gelişim grafiklerinde Pro içgörüler.", "Pro insights in progression charts."),
    )
    UserPlan.ELITE -> listOf(
        Icons.Rounded.Person to theme.t("Kişisel AI antrenör profili.", "Personal AI coach profile."),
        Icons.Rounded.Psychology to theme.t("Koç hafızası ve daha uzun bağlam.", "Coach memory and longer context."),
        Icons.Rounded.Diamond to theme.t("Pro limitlerinin üstünde Elite akış.", "Elite flow above Pro limits."),
        Icons.Rounded.Support to theme.t("Öncelikli destek ve erken erişim.", "Priority support and early access."),
    )
}

private fun energyPackageSubtitle(amount: Int, theme: AppThemeState): String = when {
    amount >= 200 -> theme.t("Yoğun AI kullanımına hazır büyük depo.", "A large reserve for heavy AI usage.")
    amount >= 50 -> theme.t("Haftalık ritim için dengeli ve avantajlı seçim.", "A balanced pick for your weekly rhythm.")
    else -> theme.t("Hızlı denemeler ve kısa analizler için ideal.", "Ideal for quick tries and short analyses.")
}

// ── Cancel Plan Dialog ────────────────────────────────────────────────────────

@Composable
private fun CancelPlanDialog(
    planName : String,
    theme    : AppThemeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                theme.t("Aboneliği İptal Et", "Cancel Subscription"),
                style = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold), color = Sinema.text(theme)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "$planName aboneliğini iptal etmek istediğine emin misin?\nMevcut dönem sonunda ücretsiz plana geçilecek.",
                    "Are you sure you want to cancel your $planName subscription?\nYou will move to the free plan at the end of the current period."
                ),
                color = Sinema.textSecondary(theme), fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            SinemaGoButton(
                label = theme.t("Vazgeç", "Keep Plan"),
                icon = Icons.Rounded.WorkspacePremium,
                onClick = onDismiss
            )
            Spacer(Modifier.height(12.dp))
            // İptal eylemi nötr kapsül (ödeme akışı: onConfirm aynı)
            SinemaCapsule(
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp,
                onClick = onConfirm,
                horizontalArrangement = Arrangement.Center
            ) { fg ->
                Text(theme.t("İptal Et", "Cancel"), color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
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
    com.cosmibit.profitness.presentation.components.SinemaDialog(
        onDismiss = onDismiss,
        title = theme.t("AI Enerji Gerekli", "AI Energy Required"),
        body = theme.t(
            "Bu özelliği kullanmak için Enerjin bitmiş.\nPremium'a geç veya Enerji yükle.",
            "You are out of Energy for this feature.\nUpgrade to Premium or top up Energy."
        ),
        confirmLabel = theme.t("Planları gör", "View plans"),
        confirmIcon = Icons.Rounded.WorkspacePremium,
        onConfirm = onGoToStore,
        dismissLabel = theme.t("Vazgeç", "Cancel")
    )
}
