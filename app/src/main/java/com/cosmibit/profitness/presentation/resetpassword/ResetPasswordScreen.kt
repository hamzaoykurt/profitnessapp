package com.cosmibit.profitness.presentation.resetpassword

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import com.cosmibit.profitness.presentation.auth.AuthFeedback
import com.cosmibit.profitness.presentation.auth.AuthAutofillHint
import com.cosmibit.profitness.presentation.auth.AuthLiquidField
import com.cosmibit.profitness.presentation.auth.AccentGradientButton
import com.cosmibit.profitness.presentation.auth.PasswordStrengthBar

@Composable
fun ResetPasswordScreen(
    code        : String,
    onDone      : () -> Unit,
    viewModel   : ResetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // PKCE code'u bir kez exchange et
    LaunchedEffect(code) { viewModel.exchangeCode(code) }

    // Şifre güncellendi → Login'e yönlendir
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ResetPasswordEvent.Done -> onDone()
            }
        }
    }

    when {
        state.isExchanging -> ExchangingContent()
        state.isLinkInvalid -> InvalidLinkContent(onDone = onDone)
        else -> NewPasswordContent(state = state, viewModel = viewModel)
    }
}

// ── Kod doğrulanıyor ──────────────────────────────────────────────────────────

@Composable
private fun ExchangingContent() {
    val theme  = LocalAppTheme.current
    Box(
        modifier         = Modifier.fillMaxSize().background(Sinema.Page),
        contentAlignment = Alignment.Center
    ) {
        SinemaScene()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = theme.effectiveAccentColor)
            Spacer(Modifier.height(16.dp))
            Text(theme.t("Bağlantı doğrulanıyor...", "Verifying link..."), color = Color.White.copy(0.75f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Link geçersiz ─────────────────────────────────────────────────────────────

@Composable
private fun InvalidLinkContent(onDone: () -> Unit) {
    val theme  = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    Box(
        modifier         = Modifier.fillMaxSize().background(Sinema.Page),
        contentAlignment = Alignment.Center
    ) {
        SinemaScene()
        Column(
            modifier            = Modifier
                .widthIn(max = responsive.formMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = responsive.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconCapsule(Icons.Rounded.LinkOff)
            Spacer(Modifier.height(28.dp))
            Text(
                theme.t("Bağlantı geçersiz", "Invalid link"),
                style = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold), color = Color.White, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                theme.t(
                    "Bu şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.\nLütfen yeni bir bağlantı isteyin.",
                    "This password reset link is invalid or has expired.\nPlease request a new link."
                ),
                color = Color.White.copy(0.75f), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            AccentGradientButton(
                text     = theme.t("GİRİŞ SAYFASINA DÖN", "BACK TO SIGN IN"),
                icon     = Icons.Rounded.Login,
                onClick  = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IconCapsule(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SinemaCapsule(
        modifier = Modifier.width(88.dp),
        selected = true, height = 88.dp, shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.Center
    ) { fg -> Icon(icon, null, tint = fg, modifier = Modifier.size(44.dp)) }
}

// ── Yeni şifre formu ──────────────────────────────────────────────────────────

@Composable
private fun NewPasswordContent(
    state    : ResetPasswordState,
    viewModel: ResetPasswordViewModel
) {
    val theme    = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    val keyboard = LocalSoftwareKeyboardController.current

    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPass        by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    val confirmFocus    = remember { FocusRequester() }

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(420)) }

    val submit = {
        keyboard?.hide()
        viewModel.submit(password, confirmPassword)
    }

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .graphicsLayer(alpha = alphaAnim.value)
        ) {
            // Orb başlık (SinemaScene her zaman koyu → beyaz metin)
            Box(Modifier.fillMaxWidth().height(280.dp).background(Sinema.Page)) {
                SinemaScene()
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = responsive.horizontalPadding)
                        .padding(top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    IconCapsule(Icons.Rounded.LockReset)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        theme.t("Yeni şifre belirle", "Set a new password"),
                        style = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold), color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        theme.t("Hesabın için güçlü bir şifre seç.", "Choose a strong password for your account."),
                        color = Color.White.copy(0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = responsive.formMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = responsive.horizontalPadding)
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(24.dp))

                AuthLiquidField(
                    value         = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label         = theme.t("YENİ ŞİFRE", "NEW PASSWORD"),
                    icon          = Icons.Rounded.Lock,
                    autofillHint  = AuthAutofillHint.NewPassword,
                    isPassword    = true,
                    showPass      = showPass,
                    onTogglePass  = { showPass = !showPass },
                    imeAction     = ImeAction.Next,
                    onImeAction   = { confirmFocus.requestFocus() }
                )

                if (password.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    PasswordStrengthBar(password = password)
                }

                Spacer(Modifier.height(16.dp))

                AuthLiquidField(
                    value         = confirmPassword,
                    onValueChange = { confirmPassword = it; viewModel.clearError() },
                    label         = theme.t("ŞİFRE TEKRAR", "CONFIRM PASSWORD"),
                    icon          = Icons.Rounded.Lock,
                    autofillHint  = AuthAutofillHint.NewPassword,
                    isPassword    = true,
                    showPass      = showConfirmPass,
                    onTogglePass  = { showConfirmPass = !showConfirmPass },
                    imeAction     = ImeAction.Done,
                    onImeAction   = { if (!state.isUpdating) submit() },
                    modifier      = Modifier.focusRequester(confirmFocus)
                )

                Spacer(Modifier.height(28.dp))

                // Yükleniyor = aynı düğme dim + mevcut metin, tıklama kilitli
                AccentGradientButton(
                    text      = if (state.isUpdating) theme.t("Şifre güncelleniyor...", "Updating password...")
                                else theme.t("ŞİFREYİ GÜNCELLE", "UPDATE PASSWORD"),
                    icon      = Icons.Rounded.LockReset,
                    onClick   = { submit() },
                    isLoading = state.isUpdating,
                    modifier  = Modifier.fillMaxWidth()
                )

                AuthFeedback(error = state.error, hint = null, onHintAction = {})
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
