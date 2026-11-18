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
import com.cosmibit.profitness.data.store.UserPlanRepository
import com.cosmibit.profitness.presentation.components.LocalFloatingTimerReserve
import com.cosmibit.profitness.presentation.components.SinemaGlassField
import com.cosmibit.profitness.presentation.components.SinemaGoOrIdle
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow

// ── Domain ────────────────────────────────────────────────────────────────────

/** Tek abonelik (Premium) fiyatı. Şimdilik Supabase/DEMO değerleri; gerçek fiyat Google Play'den gelecek. */
private data class PremiumPrice(
    val monthlyPrice  : String,
    val yearlyPrice   : String,
    val yearlyPerMonth: String,
    val discountBadge : String
)

private fun UserPlan.localizedDisplayName(theme: AppThemeState): String = when (this) {
    UserPlan.FREE -> theme.t("Ücretsiz", "Free")
    UserPlan.PREMIUM -> "Premium"
}

private val DEMO_PREMIUM_PRICE = PremiumPrice(
    monthlyPrice   = "₺149",
    yearlyPrice    = "₺999",
    yearlyPerMonth = "≈₺83/ay",
    discountBadge  = "%44"
)

private fun cleanPrice(label: String): String = label.substringBefore("/").trim()

/** Sunucu ürünlerinden Premium fiyatı (geçişte Pro SKU'su Premium sayılır; yoksa demo değer). */
private fun premiumPriceFrom(products: List<BillingProduct>): PremiumPrice {
    fun product(period: String) = products
        .filter { it.kind == "subscription" && it.plan == UserPlan.PREMIUM && it.billingPeriod == period }
        .let { list -> list.firstOrNull { "premium" in it.sku } ?: list.firstOrNull { "pro" in it.sku } ?: list.firstOrNull() }
    return DEMO_PREMIUM_PRICE.copy(
        monthlyPrice = product("month")?.priceLabel?.let(::cleanPrice) ?: DEMO_PREMIUM_PRICE.monthlyPrice,
        yearlyPrice = product("year")?.priceLabel?.let(::cleanPrice) ?: DEMO_PREMIUM_PRICE.yearlyPrice
    )
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
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StoreEvent.ShowToast -> toastMsg = event.message
            }
        }
    }

    val price = remember(state.products) { premiumPriceFrom(state.products) }
    val isPremium = state.plan == UserPlan.PREMIUM

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sinema.page(theme))
    ) {
        SinemaBackdrop(Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            StoreTopBar(theme = theme, onBack = onBack)

            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = SinemaLayout.ContentMaxList)
                    .fillMaxSize()
            ) {
                PremiumTab(
                    theme          = theme,
                    state          = state,
                    price          = price,
                    onYearlyChange = viewModel::setYearly,
                    onCancelPlan   = { showCancelDialog = true }
                )
            }
        }

        // Alt sabit dock: tek birincil düğme
        StoreDock(Modifier.align(Alignment.BottomCenter)) {
            Box(Modifier.widthIn(max = SinemaLayout.ContentMaxList).fillMaxWidth().align(Alignment.Center)) {
                val yearly = state.isYearly
                val shownPrice = if (yearly) price.yearlyPrice else price.monthlyPrice
                val per = if (yearly) theme.t("/yıl", "/yr") else theme.t("/ay", "/mo")
                if (isPremium) SinemaGoOrIdle(
                    label = theme.t("Premium aktif", "Premium active"),
                    icon = Icons.Rounded.WorkspacePremium,
                    enabled = false,
                    onClick = {}
                ) else StoreGoButton(
                    label = theme.t("Premium'a geç · $shownPrice$per", "Go Premium · $shownPrice$per"),
                    icon = Icons.Rounded.WorkspacePremium,
                    enabled = !state.isLoading,
                    loading = state.isLoading,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.purchasePlan(UserPlan.PREMIUM)
                    }
                )
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
    theme : AppThemeState,
    onBack: () -> Unit
) {
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = gutter, end = gutter, top = 10.dp)
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        SinemaCapsuleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            description = theme.t("Geri", "Back"),
            onClick = onBack,
            selected = false,
            size = 48.dp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text("Premium", color = Sinema.text(theme), fontSize = SinemaType.Title, fontWeight = FontWeight.Bold)
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

// ── Premium ───────────────────────────────────────────────────────────────────

@Composable
private fun PremiumTab(
    theme         : AppThemeState,
    state         : StoreState,
    price         : PremiumPrice,
    onYearlyChange: (Boolean) -> Unit,
    onCancelPlan  : () -> Unit
) {
    val textSub = Sinema.textSecondary(theme)
    val gutter = SinemaLayout.gutter(LocalWindowClass.current.widthDp)
    val isPremium = state.plan == UserPlan.PREMIUM
    LazyColumn(
        modifier = Modifier.fillMaxSize().sinemaTopFade(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = 20.dp, bottom = storeListBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(theme.t("Şu anki plan:", "Current plan:"), color = textSub, fontSize = SinemaType.BodySm)
                Text(state.plan.localizedDisplayName(theme), color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!isPremium) item { BillingCycleSelector(isYearly = state.isYearly, onToggle = onYearlyChange, discount = price.discountBadge, theme = theme) }
        item { PremiumOfferCard(price = price, isYearly = state.isYearly, isCurrent = isPremium, theme = theme) }

        item { StoreSectionLabel(theme.t("PREMIUM İLE", "WITH PREMIUM"), theme) }
        val premium = premiumHighlights(theme)
        items(premium.size) { i -> StoreFeatureRow(premium[i].first, premium[i].second, theme, included = true) }

        item { StoreSectionLabel(theme.t("ÜCRETSİZ PLANDA DA VAR", "ALSO IN FREE"), theme) }
        val free = freeHighlights(theme)
        items(free.size) { i -> StoreFeatureRow(free[i].first, free[i].second, theme, included = false) }

        if (state.billingSandboxAvailable) {
            item { DemoNote(theme) }
        }

        if (isPremium) {
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
private fun StoreSectionLabel(text: String, theme: AppThemeState) {
    Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        com.cosmibit.profitness.core.theme.SinemaLabelDot()
        Spacer(Modifier.width(8.dp))
        Text(text.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label.copy(fontSize = 12.sp))
    }
}

/** Avantaj satırı: Premium = vurgulu ikon; Ücretsiz = nötr ikon. */
@Composable
private fun StoreFeatureRow(icon: ImageVector, text: String, theme: AppThemeState, included: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Mini)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = if (included) theme.accentHi else Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
        Text(text, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Medium, lineHeight = 20.sp, modifier = Modifier.weight(1f))
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
    discount: String,
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
                theme.t("$discount indirim", "${discount.removePrefix("%")}% off"),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, maxLines = 2,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/** Tek Premium teklif kartı (dolu vurgu zemin): fiyat + dönem. */
@Composable
private fun PremiumOfferCard(price: PremiumPrice, isYearly: Boolean, isCurrent: Boolean, theme: AppThemeState) {
    val fg = theme.effectiveOnAccentColor
    val shown = if (isYearly) price.yearlyPrice else price.monthlyPrice
    val period = if (isYearly) theme.t("/yıl", "/yr") else theme.t("/ay", "/mo")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 104.dp)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R24), SinemaRaise.Card, fill = theme.accentHi)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Rounded.WorkspacePremium, null, tint = fg, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Premium", color = fg, fontSize = SinemaType.Title, fontWeight = FontWeight.Bold, maxLines = 1)
                if (isCurrent) StoreChip(theme.t("Aktif", "Active"), theme = theme, onFill = true)
            }
            Text(
                theme.t("AI Koç, sınırsız AI program ve AI analizler.", "AI Coach, unlimited AI programs and AI analysis."),
                color = fg.copy(0.72f), fontSize = SinemaType.BodySm, maxLines = 2
            )
        }
        if (!isCurrent) Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(shown, color = fg, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, maxLines = 1)
                Text(period, color = fg.copy(0.72f), fontSize = SinemaType.BodySm, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
            }
            if (isYearly) Text(price.yearlyPerMonth.replace("≈", "≈ "), color = fg.copy(0.72f), fontSize = SinemaType.Caption, maxLines = 1)
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
        Text(text, color = if (onFill) theme.effectiveOnAccentColor else if (hi) theme.accentHiTextColor else Sinema.textSecondary(theme), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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

private fun premiumHighlights(theme: AppThemeState): List<Pair<ImageVector, String>> = listOf(
    Icons.Rounded.ChatBubbleOutline to theme.t("AI Koç: kişisel antrenman soruları, cevaptan programa.", "AI Coach: personal training questions, answers into programs."),
    Icons.Rounded.AutoAwesome to theme.t("Sınırsız AI program oluşturma, içe aktarma ve düzenleme.", "Unlimited AI program creation, import and editing."),
    Icons.AutoMirrored.Rounded.TrendingUp to theme.t("AI analizler: kilo trendi, hareket gelişimi, plan değerlendirme.", "AI analysis: weight trend, exercise progress, plan review."),
    Icons.Rounded.Psychology to theme.t("Gelişmiş içgörüler, koç hafızası ve öncelikli işlem.", "Advanced insights, coach memory and priority processing."),
)

private fun freeHighlights(theme: AppThemeState): List<Pair<ImageVector, String>> = listOf(
    Icons.Rounded.FitnessCenter to theme.t("Antrenman takibi, manuel program oluşturma ve düzenleme.", "Workout tracking, manual program building and editing."),
    Icons.Rounded.AutoAwesome to theme.t(
        "${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} AI program oluşturma / içe aktarma hakkı.",
        "${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} AI program creations / imports."
    ),
    Icons.Rounded.SelfImprovement to theme.t("Keşfet, paylaşma, challenge ve etkinlikler.", "Discover, sharing, challenges and events."),
    Icons.Rounded.BarChart to theme.t("Kilo kaydı, başarımlar, istatistik grafikleri, hareket videoları.", "Weight log, achievements, stat charts, exercise videos."),
)

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
                    "Demo $planName aboneliği hemen iptal edilir ve Ücretsiz plana geçersin.\nPremium'a istediğin zaman tekrar geçebilirsin.",
                    "Your demo $planName subscription is canceled right away and you move to Free.\nYou can go Premium again any time."
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
        title = theme.t("Premium özelliği", "Premium feature"),
        body = theme.t(
            "Bu AI özelliği Premium'da. Ücretsiz planda AI ile ${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} program oluşturabilirsin; diğer her şey ücretsiz.",
            "This AI feature is in Premium. On Free you can create ${UserPlanRepository.FREE_AI_PROGRAM_GRANTS} programs with AI; everything else is free."
        ),
        confirmLabel = theme.t("Premium'u gör", "See Premium"),
        confirmIcon = Icons.Rounded.WorkspacePremium,
        onConfirm = onGoToStore,
        dismissLabel = theme.t("Vazgeç", "Cancel")
    )
}
