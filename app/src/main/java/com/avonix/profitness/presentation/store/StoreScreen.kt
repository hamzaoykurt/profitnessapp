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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
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
import com.avonix.profitness.presentation.components.AppBackButton
import com.avonix.profitness.presentation.components.glassCard

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
    MasterFeature(Icons.Rounded.FitnessCenter,    "Forge antrenman takibi",              UserPlan.FREE),
    MasterFeature(Icons.Rounded.BarChart,          "Temel gelişim metrikleri",            UserPlan.FREE),
    MasterFeature(Icons.Rounded.SelfImprovement,   "Manuel program oluşturma",            UserPlan.FREE),
    MasterFeature(Icons.Rounded.ChatBubbleOutline, "Oracle sohbet paketi",                UserPlan.PRO),
    MasterFeature(Icons.Rounded.AllInclusive,      "Yüksek limitli Oracle sohbeti",       UserPlan.PRO),
    MasterFeature(Icons.Rounded.AutoAwesome,       "AI ile program üretimi",              UserPlan.PRO),
    MasterFeature(Icons.AutoMirrored.Rounded.TrendingUp, "Antrenman performans analizi",  UserPlan.PRO),
    MasterFeature(Icons.AutoMirrored.Rounded.ShowChart,  "Egzersiz ilerleme grafikleri",  UserPlan.PRO),
    MasterFeature(Icons.Rounded.MonitorWeight,     "Kilo ve trend AI yorumu",             UserPlan.PRO),
    MasterFeature(Icons.Rounded.Person,            "Kişisel AI antrenör profili",         UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Psychology,        "Elite koç hafızası",                  UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Support,           "Öncelikli destek",                    UserPlan.ELITE),
    MasterFeature(Icons.Rounded.NewReleases,       "Erken erişim özellikleri",            UserPlan.ELITE),
    MasterFeature(Icons.Rounded.Diamond,           "Tüm Forge özellikleri dahil",         UserPlan.ELITE)
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
    "Forge antrenman takibi" -> theme.t(text, "Forge workout tracking")
    "Temel gelişim metrikleri" -> theme.t(text, "Basic progress metrics")
    "Manuel program oluşturma" -> theme.t(text, "Manual program creation")
    "Oracle sohbet paketi" -> theme.t(text, "Oracle chat pack")
    "Yüksek limitli Oracle sohbeti" -> theme.t(text, "High-limit Oracle chat")
    "AI ile program üretimi" -> theme.t(text, "AI program generation")
    "Antrenman performans analizi" -> theme.t(text, "Workout performance analysis")
    "Egzersiz ilerleme grafikleri" -> theme.t(text, "Exercise progress charts")
    "Kilo ve trend AI yorumu" -> theme.t(text, "Weight and trend AI insight")
    "Kişisel AI antrenör profili" -> theme.t(text, "Personal AI coach profile")
    "Elite koç hafızası" -> theme.t(text, "Elite coach memory")
    "Öncelikli destek" -> theme.t(text, "Priority support")
    "Erken erişim özellikleri" -> theme.t(text, "Early access features")
    "Tüm Forge özellikleri dahil" -> theme.t(text, "All Forge features included")
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
            .background(theme.bg0)
    ) {
        PageAccentBloom()

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
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(msg, color = theme.text0, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
    val accent = MaterialTheme.colorScheme.primary
    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var expiry by remember { mutableStateOf("12/30") }
    var cvc by remember { mutableStateOf("123") }
    val digits = cardNumber.filter { it.isDigit() }
    val expiryDigits = expiry.filter { it.isDigit() }
    val canSubmit = state.sandboxAvailable && digits.length == 16 && expiryDigits.length == 4 && cvc.length >= 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .glassCard(accent, theme, RoundedCornerShape(18.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.t("Demo ödeme", "Demo payment"), color = theme.text0, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.sandboxAvailable) theme.t("Test kartı ile satın alma akışını tamamla.", "Complete the purchase flow with a test card.")
                    else theme.t("Demo ödeme modu şu an kapalı.", "Demo payment mode is currently off."),
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
            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t("Test kartı hazır dolduruldu. Gerçek ödeme alınmaz.", "The test card is prefilled. No real payment is charged."),
                color = theme.text2.copy(0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit) accent.copy(0.14f) else theme.bg2)
                    .border(1.dp, if (canSubmit) accent.copy(0.35f) else theme.stroke, RoundedCornerShape(14.dp))
                    .clickable(enabled = !state.isLoading && canSubmit) { onSandboxComplete() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Science, null, tint = accent, modifier = Modifier.size(17.dp))
                        Text(theme.t("Demo ödemeyi tamamla", "Complete demo payment"), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                theme.t(
                    "Ödeme tamamlandığında üyelik ve Enerji hakların otomatik olarak hesabına yansır.",
                    "Membership and Energy benefits are applied automatically after payment is completed."
                ),
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
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (state.billingSandboxAvailable) Icons.Rounded.Science else Icons.Rounded.Lock,
                null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (state.billingSandboxAvailable) theme.t("Demo ödeme modu açık", "Demo payment mode on") else theme.t("Demo ödeme modu kapalı", "Demo payment mode off"),
                color = theme.text0,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (state.billingSandboxAvailable)
                    theme.t("Gerçek çekim yapılmadan akışı deneyebilirsin.", "Test the flow without a real charge.")
                else
                    theme.t("Ödeme tamamlanınca haklar hesabına işlenir.", "Benefits are applied when payment is completed."),
                color = theme.text2,
                fontSize = 10.sp,
                lineHeight = 13.sp
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
    val accent = MaterialTheme.colorScheme.primary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = theme.text2.copy(0.55f)) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.text0,
            unfocusedTextColor = theme.text0,
            focusedBorderColor = accent.copy(0.55f),
            unfocusedBorderColor = theme.stroke,
            focusedLabelColor = accent,
            unfocusedLabelColor = theme.text2,
            cursorColor = accent,
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
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack, accent = accent, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.t("Forge Merkezi", "Forge Hub"),
                    color      = theme.text0,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    theme.t("Planın, Enerjin ve AI hamlelerin", "Your plan, Energy, and AI moves"),
                    color      = theme.text2,
                    fontSize   = 11.sp,
                    lineHeight = 13.sp
                )
            }

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
                    if (isPaid) "${plan.localizedDisplayName(theme)} · $credits" else "$credits",
                    color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                if (!isPaid) {
                    Text(theme.t("Enerji", "Energy"), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glassCard(accent, theme, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(theme.t("Üyelik", "Membership") to Icons.Rounded.WorkspacePremium,
                   theme.t("Enerji", "Energy")  to Icons.Rounded.Bolt
            ).forEachIndexed { idx, (label, icon) ->
                val isActive = activeTab == idx
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) accent.copy(0.16f) else Color.Transparent)
                        .border(1.dp, if (isActive) accent.copy(0.32f) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabChange(idx) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(icon, null,
                        tint     = if (isActive) accent else theme.text2,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        color      = if (isActive) accent else theme.text2,
                        fontSize   = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StoreAccountStrip(
                plan = state.plan,
                theme = theme
            )
        }

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
                TextButton(
                    onClick = onCancelPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Cancel, null, tint = theme.text2.copy(0.55f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(theme.t("Aboneliği iptal et", "Cancel subscription"), color = theme.text2.copy(0.62f), fontSize = 12.sp)
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
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                theme.t("AKTİF PLAN", "ACTIVE PLAN"),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(plan.localizedDisplayName(theme), color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Icon(Icons.Rounded.WorkspacePremium, null, tint = accent, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BillingCycleSelector(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme: AppThemeState
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BillingCycleOption(
            label = theme.t("Aylık", "Monthly"),
            sub = theme.t("Esnek", "Flexible"),
            selected = !isYearly,
            theme = theme,
            modifier = Modifier.weight(1f),
            onClick = { onToggle(false) }
        )
        BillingCycleOption(
            label = theme.t("Yıllık", "Yearly"),
            sub = theme.t("%40'a varan avantaj", "Up to 40% value"),
            selected = isYearly,
            theme = theme,
            modifier = Modifier.weight(1f),
            onClick = { onToggle(true) }
        )
    }
}

@Composable
private fun BillingCycleOption(
    label: String,
    sub: String,
    selected: Boolean,
    theme: AppThemeState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(0.14f) else Color.Transparent)
            .border(1.dp, if (selected) accent.copy(0.30f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = if (selected) accent else theme.text1, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = if (selected) accent.copy(0.78f) else theme.text2, fontSize = 10.sp, maxLines = 1)
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
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val isCurrent = currentPlan == tier.plan
    val price = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period = when {
        tier.plan == UserPlan.FREE -> ""
        isYearly -> theme.t("/yıl", "/yr")
        else -> theme.t("/ay", "/mo")
    }
    val borderColor = when {
        isCurrent -> accent.copy(0.48f)
        selected -> accent.copy(0.48f)
        else -> theme.stroke.copy(0.40f)
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

    Column(
        modifier = Modifier
            .scale(cardScale)
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(22.dp))
            .border(if (selected || isCurrent) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                indication = null,
                interactionSource = interaction,
                onClick = onSelect
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (tier.plan) {
                        UserPlan.FREE -> theme.t("GÜNLÜK MOD", "DAILY MODE")
                        UserPlan.PRO -> theme.t("FORGE MODU", "FORGE MODE")
                        UserPlan.ELITE -> theme.t("ELITE MOD", "ELITE MODE")
                    },
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tier.localizedLabel(theme), color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    tier.localizedBadge(theme)?.let { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(badge, color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(theme.t("AKTİF", "ACTIVE"), color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(planOneLiner(tier.plan, theme), color = theme.text2, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, color = theme.text0, fontSize = 24.sp, fontWeight = FontWeight.Black)
                if (period.isNotEmpty()) Text(period, color = theme.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (isYearly && tier.discountBadge.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tier.yearlyPerMonth, color = theme.text2, fontSize = 11.sp)
                Text(tier.discountBadge, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(12.dp))
        planHighlights(tier.plan, theme).take(2).forEach { (icon, text) ->
            CompactPlanFeature(icon = icon, text = text, accent = accent, theme = theme)
        }

        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (isCurrent) Brush.linearGradient(listOf(theme.bg2, theme.bg2))
                    else Brush.linearGradient(
                        listOf(accent, accent.copy(0.78f)),
                        start = Offset(0f, 0f),
                        end = Offset(720f, 0f)
                    )
                )
                .border(1.dp, if (isCurrent) theme.stroke.copy(0.45f) else Color.White.copy(0.10f), RoundedCornerShape(13.dp))
                .clickable(enabled = !isCurrent && !isLoading, onClick = onPurchase),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && selected) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = if (tier.plan == UserPlan.FREE) accent else onAccent,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    ctaLabel,
                    color = if (isCurrent) accent else onAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        Text(text, color = theme.text1, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

private fun planOneLiner(plan: UserPlan, theme: AppThemeState): String = when (plan) {
    UserPlan.FREE -> theme.t("Temel takip; AI hakları dahil değildir.", "Basic tracking; AI allowance is not included.")
    UserPlan.PRO -> theme.t("AI üretim, analiz ve Oracle akışı açılır.", "Unlock AI creation, analysis, and Oracle flow.")
    UserPlan.ELITE -> theme.t("Pro'nun üstüne kişisel koç, öncelik ve üst limitler.", "Pro plus personal coaching, priority, and higher limits.")
}

@Composable
private fun PlanHeroCard(
    state: StoreState,
    selectedTier: PlanTier,
    isYearly: Boolean,
    theme: AppThemeState
) {
    val accent = selectedTier.accentColor.takeIf { selectedTier.plan != UserPlan.FREE } ?: Lime
    val price = if (isYearly && selectedTier.discountBadge.isNotEmpty()) selectedTier.yearlyPrice else selectedTier.monthlyPrice
    val period = when {
        selectedTier.plan == UserPlan.FREE -> theme.t("her zaman", "always")
        isYearly -> theme.t("yıllık", "yearly")
        else -> theme.t("aylık", "monthly")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(0.20f),
                        theme.bg1.copy(0.98f),
                        theme.bg2.copy(0.92f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 640f)
                )
            )
            .border(1.dp, accent.copy(0.26f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(0.13f))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(theme.t("SEÇİLİ PLAN", "SELECTED PLAN"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (state.plan == selectedTier.plan) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Lime.copy(0.12f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(theme.t("AKTİF", "ACTIVE"), color = Lime, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    selectedTier.localizedLabel(theme),
                    color = theme.text0,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    theme.t("AI koç, program üretimi ve analiz akışını tek merkezden yönet.", "Manage AI coach, program generation, and analysis from one place."),
                    color = theme.text2,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(0.13f))
                    .border(1.dp, accent.copy(0.28f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (selectedTier.plan == UserPlan.ELITE) Icons.Rounded.Diamond else Icons.Rounded.WorkspacePremium,
                    null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroMetric(
                label = theme.t("Fiyat", "Price"),
                value = if (selectedTier.plan == UserPlan.FREE) price else "$price ${periodLabel(period, theme)}",
                accent = accent,
                icon = Icons.Rounded.Payments,
                theme = theme,
                modifier = Modifier.weight(1.25f)
            )
            HeroMetric(
                label = theme.t("Enerji", "Energy"),
                value = "${state.credits}",
                accent = Lime,
                icon = Icons.Rounded.Bolt,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun periodLabel(period: String, theme: AppThemeState): String = when (period) {
    theme.t("aylık", "monthly") -> theme.t("/ay", "/mo")
    theme.t("yıllık", "yearly") -> theme.t("/yıl", "/yr")
    else -> ""
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(theme.bg0.copy(0.42f))
            .border(1.dp, accent.copy(0.20f), RoundedCornerShape(15.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
        Column {
            Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StoreSectionHeader(
    title: String,
    subtitle: String,
    theme: AppThemeState,
    trailing: @Composable (() -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(accent, accent.copy(0.32f))))
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = theme.text0, fontSize = 14.sp, fontWeight = FontWeight.Black)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = theme.text2, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        trailing?.invoke()
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
    val planAccent = when (state.plan) {
        UserPlan.FREE -> Lime
        UserPlan.PRO -> Forge500
        UserPlan.ELITE -> CardPurple
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        planAccent.copy(0.18f),
                        theme.bg1.copy(0.92f),
                        theme.bg2.copy(0.82f)
                    )
                )
            )
            .border(1.dp, planAccent.copy(0.24f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("Forge hesabın", "Your Forge Account"),
                    color = theme.text0,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    theme.t(
                        "Antrenman takibi, Oracle sohbeti ve AI analizleri aynı merkezden yönetilir.",
                        "Manage workout tracking, Oracle chat, and AI analyses from one place."
                    ),
                    color = theme.text2,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(planAccent.copy(0.14f))
                    .border(1.dp, planAccent.copy(0.28f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = planAccent, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric(
                label = theme.t("Aktif plan", "Active plan"),
                value = state.plan.localizedDisplayName(theme),
                icon = Icons.Rounded.WorkspacePremium,
                accent = planAccent,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = theme.t("AI Enerji", "AI Energy"),
                value = "${state.credits}",
                icon = Icons.Rounded.Bolt,
                accent = Lime,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.bg0.copy(0.36f))
                .border(1.dp, theme.stroke.copy(0.26f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Lime, modifier = Modifier.size(16.dp))
            Text(
                if (state.plan == UserPlan.FREE)
                    theme.t("Oracle ve AI özellikleri Enerji kullandıkça çalışır.", "Oracle and AI features use Energy as you go.")
                else
                    theme.t(
                        "${state.plan.localizedDisplayName(theme)} hakların aktif; ekstra AI hamleleri Enerjiyle tamamlanır.",
                        "${state.plan.localizedDisplayName(theme)} benefits are active; extra AI actions use Energy."
                    ),
                color = theme.text1,
                fontSize = 11.sp,
                lineHeight = 15.sp,
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
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bg1.copy(0.86f))
            .border(1.dp, accent.copy(0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(theme.t("Enerji rehberi", "Energy Guide"), color = theme.text0, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CostPill(theme.t("Sohbet", "Chat"), theme.t("10 mesaj / 1 Enerji", "10 messages / 1 Energy"), accent, theme, Modifier.weight(1f))
            CostPill(theme.t("Program", "Program"), theme.t("6-10 Enerji", "6-10 Energy"), accent, theme, Modifier.weight(1f))
            CostPill(theme.t("Analiz", "Analysis"), energyLabel(2, theme), accent, theme, Modifier.weight(1f))
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
        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
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
    val accent = MaterialTheme.colorScheme.primary
    val capacity = energyCapacityFor(state.credits)
    val fillTarget = (state.credits.toFloat() / capacity.toFloat()).coerceIn(0.04f, 1f)
    val fill by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "energy_reserve_fill"
    )
    val creditsText = state.credits.toString()
    val countSize = when {
        creditsText.length >= 5 -> 38.sp
        creditsText.length == 4 -> 44.sp
        creditsText.length == 3 -> 52.sp
        else -> 56.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(22.dp))
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.06f),
                    radius = size.width * 0.56f
                )
                onDrawBehind { drawRect(glow) }
            }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("ENERJİ", "ENERGY"),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        creditsText,
                        color = theme.text0,
                        fontSize = countSize,
                        lineHeight = countSize,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        theme.t("Enerji", "Energy"),
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }
            }
            EnergyReserveBattery(
                fill = fill,
                accent = accent,
                theme = theme
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(theme.bg0.copy(0.52f))
                .border(1.dp, accent.copy(0.18f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(0.58f), accent, Color.White.copy(0.22f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun EnergyReserveBattery(
    fill: Float,
    accent: Color,
    theme: AppThemeState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(accent.copy(0.28f))
        )
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(86.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(theme.bg0.copy(0.46f))
                .border(1.dp, accent.copy(0.30f), RoundedCornerShape(22.dp))
                .padding(7.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fill)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(0.24f), accent.copy(0.90f), accent.copy(0.58f))
                        )
                    )
            )
            Icon(Icons.Rounded.Bolt, null, tint = Color.White.copy(0.82f), modifier = Modifier.align(Alignment.Center).size(22.dp))
        }
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
    val accent = MaterialTheme.colorScheme.primary
    val intensity = energyPackageIntensity(pkg.amount)
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(if (isPressed) 0.985f else 1f, label = "energy_pack_press")

    Row(
        modifier = Modifier
            .scale(rowScale)
            .fillMaxWidth()
            .glassCard(accent, theme, RoundedCornerShape(18.dp))
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.07f + intensity * 0.035f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.50f),
                    radius = size.width * (0.18f + intensity * 0.08f)
                )
                onDrawBehind { drawRect(glow) }
            }
            .clickable(
                enabled = !isLoading,
                indication = null,
                interactionSource = interaction,
                onClick = onPurchase
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(0.12f))
                .border(1.dp, accent.copy(0.20f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(energyLabel(pkg.amount, theme), color = theme.text0, fontSize = 16.sp, fontWeight = FontWeight.Black)
                pkg.localizedBadge(theme)?.let { badge ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(badge, color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            EnergyPackSignal(level = intensity, accent = accent, theme = theme)
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(0.10f))
                    .border(1.dp, accent.copy(0.20f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(pkg.price, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = accent, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun EnergyPackSignal(
    level: Int,
    accent: Color,
    theme: AppThemeState
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height((4 + index * 2).dp)
                    .clip(CircleShape)
                    .background(if (index < level) accent.copy(0.72f) else theme.stroke.copy(0.40f))
            )
        }
    }
}

private fun energyPackageIntensity(amount: Int): Int = when {
    amount >= 200 -> 3
    amount >= 50 -> 2
    else -> 1
}

@Composable
private fun EnergyBalanceHero(
    state: StoreState,
    theme: AppThemeState,
    pulse: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Lime.copy(0.20f),
                        theme.bg1.copy(0.96f),
                        CardCyan.copy(0.10f),
                        theme.bg2.copy(0.92f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 720f)
                )
            )
            .border(1.dp, Lime.copy(0.28f), RoundedCornerShape(26.dp))
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(Lime.copy(alpha = 0.24f * pulse), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.12f),
                    radius = size.width * 0.72f
                )
                onDrawBehind { drawRect(glow) }
            }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("Enerji Merkezi", "Energy Hub"),
                    color = theme.text0,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    theme.t(
                        "AI hamlelerin burada şarj olur: sohbet, program ve analiz tek bakiyeden akar.",
                        "Charge your AI moves here: chat, programs, and analysis all run from one balance."
                    ),
                    color = theme.text2,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Lime.copy(0.12f))
                    .border(1.dp, Lime.copy(0.34f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((36 + (pulse * 6)).dp)
                        .clip(CircleShape)
                        .background(Lime.copy(0.12f))
                )
                Icon(Icons.Rounded.Bolt, null, tint = Lime, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.bg0.copy(0.40f))
                .border(1.dp, Lime.copy(0.20f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(theme.t("Kalan Enerji", "Remaining Energy"), color = theme.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("${state.credits}", color = Lime, fontSize = 44.sp, fontWeight = FontWeight.Black)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Lime.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Rounded.FlashOn, null, tint = Lime, modifier = Modifier.size(13.dp))
                Text(theme.t("Kullanıma hazır", "Ready to use"), color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val accent = MaterialTheme.colorScheme.primary
            EnergyUseTile(theme.t("Sohbet", "Chat"), theme.t("10 mesaj / 1 Enerji", "10 messages / 1 Energy"), accent, theme, Modifier.weight(1f))
            EnergyUseTile(theme.t("Program", "Program"), theme.t("6-10 Enerji", "6-10 Energy"), accent, theme, Modifier.weight(1f))
            EnergyUseTile(theme.t("Analiz", "Analysis"), energyLabel(2, theme), accent, theme, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EnergyUseTile(
    label: String,
    value: String,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg0.copy(0.32f))
            .border(1.dp, accent.copy(0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

// ── Billing Toggle ────────────────────────────────────────────────────────────

@Composable
private fun BillingToggle(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme   : AppThemeState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            theme.t("AYLIK", "MONTHLY"),
            color      = if (!isYearly) theme.text0 else theme.text2,
            fontSize   = 12.sp,
            fontWeight = if (!isYearly) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(false) }
        )

        Spacer(Modifier.width(10.dp))

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

        Spacer(Modifier.width(10.dp))

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                theme.t("YILLIK", "YEARLY"),
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
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    val price  = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice

    val selColor = tier.accentColor.takeIf { tier.plan != UserPlan.FREE } ?: Lime
    val bgColor by animateColorAsState(
        if (isSelected) selColor.copy(0.13f) else theme.bg1.copy(0.90f),
        tween(180), label = "bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) selColor.copy(0.56f) else theme.stroke.copy(0.36f),
        tween(180), label = "border"
    )

    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.height(13.dp), contentAlignment = Alignment.Center) {
            if (tier.badge != null) {
                Text(
                    tier.localizedBadge(theme).orEmpty(),
                    color         = if (isSelected) selColor else theme.text2,
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Text(
            tier.localizedLabel(theme),
            color      = if (isSelected) selColor else theme.text1,
            fontSize   = 14.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
        )

        Text(
            price,
            color      = if (isSelected) theme.text0 else theme.text2,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Black
        )

        Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Lime.copy(0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(theme.t("AKTİF", "ACTIVE"), color = Lime, fontSize = 7.sp,
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
    credits  : Int,
    theme    : AppThemeState
) {
    val accent    = tier.accentColor
    val isFree    = tier.plan == UserPlan.FREE
    val price     = if (isYearly && tier.discountBadge.isNotEmpty()) tier.yearlyPrice else tier.monthlyPrice
    val period    = when { isFree -> ""; isYearly -> theme.t("/yıl", "/year"); else -> theme.t("/ay", "/month") }
    val highlights = planHighlights(tier.plan, theme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(theme.bg1, theme.bg1.copy(0.96f), theme.bg0.copy(0.78f))
                )
            )
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) Lime.copy(0.45f) else accent.copy(0.26f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    theme.t("Plan özeti", "Plan summary"),
                    color = theme.text2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tier.localizedLabel(theme), color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Lime.copy(0.13f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(theme.t("AKTİF", "ACTIVE"), color = Lime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                AnimatedContent(
                    targetState = price,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "price_anim"
                ) { p ->
                    Text(p, color = theme.text0, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                if (period.isNotEmpty()) {
                    Text(period, color = theme.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isYearly && tier.discountBadge.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tier.yearlyPerMonth, color = theme.text2, fontSize = 11.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Lime.copy(0.13f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(tier.discountBadge, color = Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanMiniStat(
                icon = Icons.Rounded.Bolt,
                label = theme.t("Enerji", "Energy"),
                value = "$credits",
                accent = Lime,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
            PlanMiniStat(
                icon = Icons.Rounded.AutoAwesome,
                label = theme.t("AI akışı", "AI flow"),
                value = if (tier.plan == UserPlan.FREE) theme.t("Enerji", "Energy") else theme.t("Dahil", "Included"),
                accent = accent,
                theme = theme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = theme.stroke.copy(0.28f), thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))

        highlights.forEach { (icon, text) ->
            PlanFeatureRow(icon = icon, text = text, accent = accent, theme = theme)
        }
    }
}

@Composable
private fun PlanMiniStat(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(theme.bg0.copy(0.38f))
            .border(1.dp, accent.copy(0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        Column {
            Text(label, color = theme.text2, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = theme.text0, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PlanFeatureRow(
    icon: ImageVector,
    text: String,
    accent: Color,
    theme: AppThemeState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Text(text, color = theme.text1, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

private fun planHighlights(plan: UserPlan, theme: AppThemeState): List<Pair<ImageVector, String>> = when (plan) {
    UserPlan.FREE -> listOf(
        Icons.Rounded.FitnessCenter to theme.t("Manuel antrenman takibi.", "Manual workout tracking."),
        Icons.Rounded.SelfImprovement to theme.t("Programını kendin oluşturma.", "Build your own program."),
        Icons.Rounded.BarChart to theme.t("Temel gelişim ve profil özeti.", "Basic progress and profile summary."),
    )
    UserPlan.PRO -> listOf(
        Icons.Rounded.ChatBubbleOutline to theme.t("Oracle sohbet paketleri dahil.", "Oracle chat bundles included."),
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

// ── Energy Package Card ──────────────────────────────────────────────────────

@Composable
private fun EnergyPackageCard(
    pkg       : EnergyPackage,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        pkg.accentColor.copy(0.18f),
                        theme.bg1.copy(0.96f),
                        theme.bg2.copy(0.92f)
                    ),
                    start = Offset.Zero,
                    end = Offset(820f, 520f)
                )
            )
            .border(
                width = if (pkg.badge != null) 1.5.dp else 1.dp,
                color = if (pkg.badge != null) Lime.copy(0.46f) else pkg.accentColor.copy(0.26f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                enabled           = !isLoading,
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPurchase
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(pkg.accentColor.copy(0.12f))
                    .border(1.dp, pkg.accentColor.copy(0.26f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = pkg.accentColor, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(energyLabel(pkg.amount, theme), color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    if (pkg.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Lime.copy(0.14f))
                                .border(1.dp, Lime.copy(0.24f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(pkg.localizedBadge(theme).orEmpty(), color = Lime, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    energyPackageSubtitle(pkg.amount, theme),
                    color = theme.text2,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.bg0.copy(0.44f))
                    .border(1.dp, pkg.accentColor.copy(0.24f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(pkg.price, color = pkg.accentColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyPackageMeta(Icons.Rounded.Shield, theme.t("Güvenli ödeme", "Secure payment"), pkg.accentColor, theme, Modifier.weight(1f))
            EnergyPackageMeta(Icons.Rounded.Speed, pkg.localizedPerEnergy(theme), pkg.accentColor, theme, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(pkg.accentColor.copy(0.14f))
                .border(1.dp, pkg.accentColor.copy(0.30f), RoundedCornerShape(15.dp))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    color       = pkg.accentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Rounded.FlashOn, null, tint = pkg.accentColor, modifier = Modifier.size(16.dp))
                    Text(theme.t("Enerji yükle", "Top up Energy"), color = pkg.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun EnergyPackageMeta(
    icon: ImageVector,
    label: String,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg0.copy(0.34f))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = theme.text2, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun energyPackageSubtitle(amount: Int, theme: AppThemeState): String = when {
    amount >= 200 -> theme.t("Yoğun AI kullanımına hazır büyük depo.", "A large reserve for heavy AI usage.")
    amount >= 50 -> theme.t("Haftalık ritim için dengeli ve avantajlı seçim.", "A balanced pick for your weekly rhythm.")
    else -> theme.t("Hızlı denemeler ve kısa analizler için ideal.", "Ideal for quick tries and short analyses.")
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
    theme         : AppThemeState,
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
                            end   = Offset(720f, 0f)
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
                    Text(theme.t("Mevcut planınız", "Your current plan"),
                        color = Lime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Text(
                    theme.t("$price$period ile Başla", "Start with $price$period"),
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
                    append(theme.t("İstediğin zaman iptal et  ·  Taahhüt yok", "Cancel anytime  ·  No commitment"))
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
            Text(theme.t("Aboneliği İptal Et", "Cancel Subscription"),
                color = theme.text0, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "$planName aboneliğini iptal etmek istediğine emin misin?\nMevcut dönem sonunda ücretsiz plana geçilecek.",
                    "Are you sure you want to cancel your $planName subscription?\nYou will move to the free plan at the end of the current period."
                ),
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
                    Text(theme.t("Vazgeç", "Keep Plan"), color = theme.text2, fontSize = 14.sp)
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
                    Text(theme.t("İptal Et", "Cancel"),
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
    val accent = MaterialTheme.colorScheme.primary
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
                    .background(accent.copy(0.1f))
                    .border(1.dp, accent.copy(0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null,
                    tint = accent, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(theme.t("AI Enerji Gerekli", "AI Energy Required"),
                color = theme.text0, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "Bu özelliği kullanmak için Enerjin bitmiş.\nPremium'a geç veya Enerji yükle.",
                    "You are out of Energy for this feature.\nUpgrade to Premium or top up Energy."
                ),
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
                    .background(accent.copy(0.12f))
                    .border(1.dp, accent.copy(0.35f), RoundedCornerShape(14.dp))
                    .clickable { onGoToStore() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(theme.t("Planları Gör", "View Plans"), color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(theme.t("Vazgeç", "Cancel"), color = theme.text2, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
