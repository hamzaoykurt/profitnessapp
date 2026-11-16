package com.cosmibit.profitness.presentation.store

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.cosmibit.profitness.presentation.components.SinemaGlassField
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

private val MAX_YEARLY_DISCOUNT_PCT: Int =
    PLANS.mapNotNull { it.discountBadge.filter(Char::isDigit).toIntOrNull() }.maxOrNull() ?: 0

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
    var selectedAmount by remember { mutableIntStateOf(0) }
    val effectiveAmount = energyPackages.firstOrNull { it.amount == selectedAmount }?.amount
        ?: (energyPackages.firstOrNull { it.badge != null } ?: energyPackages.firstOrNull())?.amount ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(theme))
    ) {
        SinemaBackdrop(Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            StoreTopBar(
                theme       = theme,
                activeTab   = activeTab,
                onBack      = onBack,
                onTabChange = { activeTab = it }
            )

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
                        onSelectPlan    = { selectedPlan = it },
                        onYearlyChange  = viewModel::setYearly,
                        plans           = plans,
                        onCancelPlan    = { showCancelDialog = true }
                    )
                    else -> EnergyTab(
                        theme          = theme,
                        haptic         = haptic,
                        state          = state,
                        packages       = energyPackages,
                        selectedAmount = effectiveAmount,
                        onSelectAmount = { selectedAmount = it }
                    )
                }
            }
        }

        // Alt sabit dock: tek birincil düğme
        StoreDock(Modifier.align(Alignment.BottomCenter)) {
            Box(Modifier.widthIn(max = SinemaLayout.ContentMaxList).fillMaxWidth().align(Alignment.Center)) {
                if (activeTab == 0) {
                    val tier = plans.first { it.plan == selectedPlan }
                    val isCurrent = state.plan == selectedPlan
                    val yearly = state.isYearly && tier.discountBadge.isNotEmpty()
                    val price = if (yearly) tier.yearlyPrice else tier.monthlyPrice
                    val per = if (yearly) theme.t("/yıl", "/yr") else theme.t("/ay", "/mo")
                    when {
                        isCurrent -> SinemaGoOrIdle(
                            label = theme.t("Aktif mod", "Current mode"),
                            icon = Icons.Rounded.WorkspacePremium,
                            enabled = false,
                            onClick = {}
                        )
                        selectedPlan == UserPlan.FREE -> SinemaCapsule(
                            modifier = Modifier.fillMaxWidth().alpha(if (state.isLoading) 0.5f else 1f),
                            height = 56.dp,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCancelDialog = true
                            },
                            enabled = !state.isLoading,
                            horizontalArrangement = Arrangement.Center
                        ) { fg ->
                            Text(theme.t("Ücretsiz moda dön", "Return to Free mode"), color = fg, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        else -> StoreGoButton(
                            label = theme.t(
                                "${tier.localizedLabel(theme)} modunu aç · $price$per",
                                "Unlock ${tier.localizedLabel(theme)} mode · $price$per"
                            ),
                            icon = Icons.Rounded.WorkspacePremium,
                            enabled = !state.isLoading,
                            loading = state.isLoading,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.purchasePlan(selectedPlan)
                            }
                        )
                    }
                } else {
                    val pkg = energyPackages.firstOrNull { it.amount == effectiveAmount }
                    if (pkg != null) {
                        StoreGoButton(
                            label = theme.t("${pkg.amount} Enerji al · ${pkg.price}", "Get ${pkg.amount} Energy · ${pkg.price}"),
                            icon = Icons.Rounded.Bolt,
                            enabled = !state.isLoading,
                            loading = state.isLoading,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.purchaseCredits(pkg.amount)
                            }
                        )
                    }
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
                    Text(msg, color = fg, fontWeight = FontWeight.SemiBold, fontSize = SinemaType.BodySm)
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
            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R28), tint = theme.accentHi)
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
                Text(theme.t("Demo ödeme", "Demo payment"), color = textMain, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.sandboxAvailable) theme.t("Test kartı ile satın alma akışını tamamla.", "Complete the purchase flow with a test card.")
                    else theme.t("Demo ödeme modu şu an kapalı.", "Demo payment mode is currently off."),
                    color = textSub,
                    fontSize = SinemaType.Caption,
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
                fontSize = SinemaType.Caption,
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
                fontSize = SinemaType.Caption,
                lineHeight = 16.sp
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
    onBack     : () -> Unit,
    onTabChange: (Int) -> Unit
) {
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = gutter, end = gutter, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            SinemaCapsuleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = theme.t("Geri", "Back"),
                onClick = onBack,
                selected = false,
                size = 48.dp,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(theme.t("Mağaza", "Store"), color = Sinema.text(theme), fontSize = SinemaType.Title, fontWeight = FontWeight.Bold)
        }
        SinemaSegmentRow(
            options = listOf(0, 1),
            selected = activeTab,
            label = { idx -> if (idx == 0) theme.t("Üyelik", "Membership") else theme.t("Enerji", "Energy") },
            onSelect = onTabChange,
            height = 48.dp
        )
    }
}

/** Liste alt boşluğu: dock (96dp) + yüzen sayaç rezervi + gezinme çubuğu. */
@Composable
private fun storeListBottomPadding(): Dp =
    96.dp + LocalFloatingTimerReserve.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Alt sabit dock: tek birincil düğme. */
@Composable
private fun StoreDock(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val page = Sinema.page(LocalAppTheme.current)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.25f to page, 1f to page))
            .padding(bottom = LocalFloatingTimerReserve.current)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp)
    ) { content() }
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
    onCancelPlan  : () -> Unit
) {
    val textSub = Sinema.textSecondary(theme)
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = storeListBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(theme.t("Şu anki plan:", "Current plan:"), color = textSub, fontSize = SinemaType.BodySm)
                Text(state.plan.localizedDisplayName(theme), color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            BillingCycleSelector(isYearly = state.isYearly, onToggle = onYearlyChange, theme = theme)
        }

        items(plans.size) { index ->
            val tier = plans[index]
            PlanOfferCard(
                tier = tier,
                isCurrent = state.plan == tier.plan,
                selected = selectedPlan == tier.plan,
                isYearly = state.isYearly,
                theme = theme,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectPlan(tier.plan)
                }
            )
        }

        item {
            val name = selectedPlan.localizedDisplayName(theme)
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.cosmibit.profitness.core.theme.SinemaLabelDot()
                Spacer(Modifier.width(8.dp))
                Text(
                    theme.t("$name İLE AÇILANLAR", "UNLOCKED WITH $name").sinemaUpper(theme),
                    color = textSub, style = Sinema.Label.copy(fontSize = 11.sp)
                )
            }
        }
        val features = planHighlights(selectedPlan, theme)
        items(features.size) { i ->
            val (icon, text) = features[i]
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Mini)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, null, tint = theme.accentHi, modifier = Modifier.size(20.dp))
                Text(text, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Medium, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
        }

        if (state.billingSandboxAvailable) {
            item { DemoNote(theme) }
        }

        if (hasPaidPlan) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .sinemaClickable(onClick = onCancelPlan, role = androidx.compose.ui.semantics.Role.Button)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            theme.t("Aboneliği iptal et", "Cancel subscription"),
                            color = textSub, fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoNote(theme: AppThemeState) {
    SinemaCapsule(
        modifier = Modifier.fillMaxWidth(),
        height = 48.dp,
        shape = RoundedCornerShape(SinemaRadius.R20),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { fg ->
        Icon(Icons.Rounded.Info, null, tint = fg.copy(0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            theme.t("Demo ödeme modu açık · gerçek çekim yok", "Demo payment mode on · no real charge"),
            color = fg.copy(0.8f), fontSize = SinemaType.BodySm, maxLines = 2
        )
    }
}

@Composable
private fun BillingCycleSelector(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit,
    theme: AppThemeState
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SinemaSegmentRow(
            options = listOf(false, true),
            selected = isYearly,
            label = { yearly -> if (yearly) theme.t("Yıllık", "Yearly") else theme.t("Aylık", "Monthly") },
            onSelect = onToggle,
            height = 44.dp,
            neutral = true,
            modifier = Modifier.weight(1f)
        )
        if (isYearly) {
            Text(
                theme.t("%${MAX_YEARLY_DISCOUNT_PCT}'e varan indirim", "Up to $MAX_YEARLY_DISCOUNT_PCT% off"),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, maxLines = 2,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/** Seçilebilir radyo kart (96dp, eşit boy): seçili = soft zemin + ince c1 kenar; glow/boyut sıçraması yok. */
@Composable
private fun SelectableStoreCard(
    selected: Boolean,
    theme: AppThemeState,
    enabled: Boolean = true,
    onSelect: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(SinemaRadius.R20)
    val accent = theme.effectiveAccentColor
    val base = Modifier
        .fillMaxWidth()
        .height(96.dp)
        .alpha(if (enabled) 1f else 0.5f)
        .sinemaClickable(onClick = onSelect, enabled = enabled, scale = 0.98f, role = androidx.compose.ui.semantics.Role.RadioButton)
    Row(
        modifier = (if (selected) base.sinemaRaised(shape, SinemaRaise.Card, fill = theme.accentHi)
        else base.sinemaCard(theme, shape)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(20.dp).border(1.5.dp, if (selected) theme.effectiveOnAccentColor else Sinema.textSecondary(theme), CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(theme.effectiveOnAccentColor)) }
        content()
    }
}

@Composable
private fun PlanOfferCard(
    tier: PlanTier,
    isCurrent: Boolean,
    selected: Boolean,
    isYearly: Boolean,
    theme: AppThemeState,
    onSelect: () -> Unit
) {
    val textMain = if (selected) theme.effectiveOnAccentColor else Sinema.text(theme)   // seçili = dolu mercan zemin: koyu metin
    val textSub = if (selected) theme.effectiveOnAccentColor.copy(0.72f) else Sinema.textSecondary(theme)
    val yearly = isYearly && tier.discountBadge.isNotEmpty()
    val price = if (yearly) tier.yearlyPrice else tier.monthlyPrice
    val period = when {
        tier.plan == UserPlan.FREE -> ""
        isYearly -> theme.t("/yıl", "/yr")
        else -> theme.t("/ay", "/mo")
    }
    SelectableStoreCard(selected = selected, theme = theme, onSelect = onSelect) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tier.localizedLabel(theme), color = textMain, fontSize = SinemaType.Title, fontWeight = FontWeight.Bold, maxLines = 1)
                if (isCurrent) StoreChip(theme.t("Aktif", "Active"), theme = theme, onFill = selected)
                else tier.localizedBadge(theme)?.let { StoreChip(sentenceCase(it), theme = theme, onFill = selected) }
                if (yearly) StoreChip(tier.discountBadge.substringBefore(" ").replace("%", "-%"), theme = theme, hi = false, onFill = selected)
            }
            Text(planOneLiner(tier.plan, theme), color = textSub, fontSize = SinemaType.BodySm, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, color = textMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, maxLines = 1)
                if (period.isNotEmpty()) Text(period, color = textSub, fontSize = SinemaType.BodySm, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
            }
            if (yearly) Text(tier.yearlyPerMonth.replace("≈", "≈ "), color = textSub, fontSize = SinemaType.Caption, maxLines = 1)
        }
    }
}

private fun sentenceCase(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }

/** Nötr cam rozet. */
@Composable
private fun StoreChip(text: String, theme: AppThemeState, hi: Boolean = true, onFill: Boolean = false) {
    Box(
        Modifier.height(22.dp).clip(CircleShape).background(if (onFill) Color.Black.copy(0.14f) else Color.White.copy(0.10f)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (onFill) theme.effectiveOnAccentColor else if (hi) theme.accentHiTextColor else Sinema.textSecondary(theme), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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

private fun planOneLiner(plan: UserPlan, theme: AppThemeState): String = when (plan) {
    UserPlan.FREE -> theme.t("Temel takip; AI hakları dahil değildir.", "Basic tracking; AI allowance is not included.")
    UserPlan.PRO -> theme.t("AI üretim, analiz ve AI Koç akışı açılır.", "Unlock AI creation, analysis, and AI Coach flow.")
    UserPlan.ELITE -> theme.t("Pro'nun üstüne kişisel koç, öncelik ve üst limitler.", "Pro plus personal coaching, priority, and higher limits.")
}

// ── Energy Tab ────────────────────────────────────────────────────────────────

@Composable
private fun EnergyTab(
    theme         : AppThemeState,
    haptic        : androidx.compose.ui.hapticfeedback.HapticFeedback,
    state         : StoreState,
    packages      : List<EnergyPackage>,
    selectedAmount: Int,
    onSelectAmount: (Int) -> Unit
) {
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = storeListBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Bolt, null, tint = theme.rewardGold, modifier = Modifier.size(24.dp))
                    Text(theme.t("Enerji", "Energy").sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label.copy(fontSize = 11.sp))
                }
                SinemaFitText(
                    text = state.credits.toString(),
                    style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
                    color = Sinema.text(theme),
                    baseSp = 64f
                )
            }
        }

        items(packages.size) { idx ->
            val pkg = packages[idx]
            SelectableStoreCard(
                selected = pkg.amount == selectedAmount,
                theme = theme,
                enabled = !state.isLoading,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectAmount(pkg.amount)
                }
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(energyLabel(pkg.amount, theme), color = Sinema.text(theme), fontSize = SinemaType.Title, fontWeight = FontWeight.Bold, maxLines = 1)
                        pkg.localizedBadge(theme)?.let { StoreChip(sentenceCase(it), theme = theme) }
                    }
                    if (pkg.perEnergy.isNotBlank()) {
                        Text(pkg.localizedPerEnergy(theme), color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, maxLines = 1)
                    }
                }
                Text(pkg.price, color = Sinema.text(theme), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, maxLines = 1)
            }
        }

        if (state.billingSandboxAvailable) {
            item { DemoNote(theme) }
        }
    }
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
            Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(SinemaRadius.R28)).sinemaCardAccent(theme.accentHi).padding(24.dp),
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
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 20.sp
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
                Text(theme.t("İptal Et", "Cancel"), color = fg, fontSize = SinemaType.Body, fontWeight = FontWeight.ExtraBold)
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
