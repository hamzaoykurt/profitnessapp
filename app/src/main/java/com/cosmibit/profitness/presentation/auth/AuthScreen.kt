@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.cosmibit.profitness.presentation.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.cosmibit.profitness.BuildConfig
import com.cosmibit.profitness.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.core.ui.rememberResponsiveLayoutInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import coil.compose.SubcomposeAsyncImage
import com.cosmibit.profitness.core.ui.categoryImageUrl
import com.cosmibit.profitness.presentation.components.SinemaGlowBar
import com.cosmibit.profitness.presentation.components.SinemaSegmentRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// ── Entry composable — routes between auth screens ────────────────────────────

private enum class AuthMode {
    Login,
    Register
}

enum class AuthAutofillHint {
    LoginEmail,
    NewEmail,
    Email,
    Password,
    NewPassword
}

@Composable
fun AuthScreen(
    onNavigateToDashboard : () -> Unit,
    onNavigateToOnboarding: () -> Unit = onNavigateToDashboard,
    viewModel             : AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToDashboard  -> onNavigateToDashboard()
                AuthEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                AuthEvent.NavigateToAuth       -> { /* Zaten auth ekranındayız; ek eylem yok */ }
            }
        }
    }

    LaunchedEffect(state.isSessionLoading, state.restoredSessionAuthenticated) {
        if (!state.isSessionLoading && state.restoredSessionAuthenticated) {
            onNavigateToDashboard()
        }
    }

    // Session diskten yüklenirken veya restore edilmiş session dashboard'a yönlenirken
    // auth içeriğini hiç çizme; aksi halde login ekranı tek frame bile olsa görünür.
    if (state.isSessionLoading || state.restoredSessionAuthenticated) {
        AuthLoadingSplash()
        return
    }

    BackHandler(enabled = state.screen !is AuthFlowScreen.Login) {
        when (state.screen) {
            is AuthFlowScreen.Register       -> viewModel.navigateTo(AuthFlowScreen.Login)
            is AuthFlowScreen.ForgotPassword -> viewModel.navigateTo(AuthFlowScreen.Login)
            is AuthFlowScreen.OtpVerify      -> viewModel.navigateTo(AuthFlowScreen.Register)
            is AuthFlowScreen.EmailSent      -> viewModel.navigateTo(AuthFlowScreen.Login)
            is AuthFlowScreen.Login          -> Unit
        }
    }

    AnimatedContent(
        targetState  = state.screen,
        transitionSpec = {
            val enter = fadeIn(tween(360)) + scaleIn(
                tween(360), initialScale = 0.96f
            )
            val exit = fadeOut(tween(240)) + scaleOut(
                tween(240), targetScale = 1.03f
            )
            enter togetherWith exit
        },
        label = "auth_flow"
    ) { screen ->
        when (screen) {
            is AuthFlowScreen.Login         -> LoginScreen(state, viewModel)
            is AuthFlowScreen.Register      -> RegisterScreen(state, viewModel)
            is AuthFlowScreen.ForgotPassword -> ForgotPasswordScreen(screen.prefillEmail, state, viewModel)
            is AuthFlowScreen.OtpVerify     -> OtpVerifyScreen(screen.email, state, viewModel)
            is AuthFlowScreen.EmailSent     -> EmailSentScreen(screen.email, screen.type, state, viewModel)
        }
    }
}

// ── Loading Splash ───────────────────────────────────────────────────────────

@Composable
private fun AuthLoadingSplash() {
    // Giriş animasyonu: orb sahnesinden spring ile fırlayarak çıkış (metin yok)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val iconScale by animateFloatAsState(
        targetValue   = if (entered) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue   = if (entered) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label         = "icon_alpha"
    )

    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        SinemaScene()
        Image(
            painter            = painterResource(R.drawable.ic_app_logo),
            contentDescription = "Profitness",
            modifier           = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha  = iconAlpha
                }
                .clip(RoundedCornerShape(20.dp))
        )
    }
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(state: AuthState, viewModel: AuthViewModel) {
    val theme = LocalAppTheme.current
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val passFocus = remember { FocusRequester() }

    AuthScaffold(
        heroTitle    = theme.t("Giriş yap", "Sign in"),
        heroSubtitle = ""
    ) {
        AuthModeTabs(
            selected = AuthMode.Login,
            onSelected = { mode ->
                if (mode == AuthMode.Register) viewModel.navigateTo(AuthFlowScreen.Register)
            }
        )
        Spacer(Modifier.height(18.dp))
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
            autofillHint  = AuthAutofillHint.LoginEmail,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            onImeAction   = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = password,
            onValueChange = { password = it },
            placeholder   = theme.t("Şifre", "Password"),
            icon          = Icons.Rounded.Lock,
            autofillHint  = AuthAutofillHint.Password,
            isPassword    = true,
            showPass      = showPass,
            onTogglePass  = { showPass = !showPass },
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onLoginClick(email, password) },
            modifier      = Modifier.focusRequester(passFocus)
        )

        // "Şifremi Unuttum" link
        Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = {
                viewModel.navigateTo(AuthFlowScreen.ForgotPassword(email.trim()))
            }) {
                Text(
                    theme.t("Şifremi Unuttum", "Forgot Password"),
                    color    = Sinema.textSecondary(theme),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        AccentGradientButton(
            text     = if (state.isLoading) theme.t("Giriş yapılıyor...", "Signing in...") else theme.t("Giriş Yap", "Sign In"),
            icon     = Icons.Rounded.Login,
            onClick  = { if (!state.isLoading) viewModel.onLoginClick(email, password) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        AuthFeedback(
            error = state.error,
            hint  = state.hint,
            onHintAction = { hint ->
                when (hint) {
                    AuthHint.ForgotPassword -> viewModel.navigateTo(AuthFlowScreen.ForgotPassword(email.trim()))
                    else -> Unit
                }
            }
        )
    }
}

// ── Register Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RegisterScreen(state: AuthState, viewModel: AuthViewModel) {
    val theme = LocalAppTheme.current
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPass        by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    val passFocus       = remember { FocusRequester() }
    val confirmFocus    = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val confirmBringIntoViewRequester = remember { BringIntoViewRequester() }

    AuthScaffold(
        heroTitle    = theme.t("Kayıt ol", "Sign up"),
        heroSubtitle = ""
    ) {
        AuthModeTabs(
            selected = AuthMode.Register,
            onSelected = { mode ->
                if (mode == AuthMode.Login) viewModel.navigateTo(AuthFlowScreen.Login)
            }
        )
        Spacer(Modifier.height(18.dp))
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
            autofillHint  = AuthAutofillHint.NewEmail,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            onImeAction   = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = password,
            onValueChange = { password = it },
            placeholder   = theme.t("Şifre", "Password"),
            icon          = Icons.Rounded.Lock,
            autofillHint  = AuthAutofillHint.NewPassword,
            isPassword    = true,
            showPass      = showPass,
            onTogglePass  = { showPass = !showPass },
            imeAction     = ImeAction.Next,
            onImeAction   = { confirmFocus.requestFocus() },
            modifier      = Modifier.focusRequester(passFocus)
        )
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            PasswordStrengthBar(password = password)
        }
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder   = theme.t("Şifre tekrar", "Repeat password"),
            icon          = Icons.Rounded.LockOpen,
            autofillHint  = AuthAutofillHint.NewPassword,
            isPassword    = true,
            showPass      = showConfirmPass,
            onTogglePass  = { showConfirmPass = !showConfirmPass },
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onRegisterClick(email, password, confirmPassword) },
            modifier      = Modifier
                .bringIntoViewRequester(confirmBringIntoViewRequester)
                .focusRequester(confirmFocus)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(250)
                            confirmBringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
        Spacer(Modifier.height(22.dp))

        AccentGradientButton(
            text      = if (state.isLoading) theme.t("Hesap oluşturuluyor...", "Creating account...") else theme.t("Kayıt Ol", "Sign Up"),
            icon      = Icons.Rounded.PersonAdd,
            onClick   = { if (!state.isLoading) viewModel.onRegisterClick(email, password, confirmPassword) },
            isLoading = state.isLoading,
            modifier  = Modifier.fillMaxWidth()
        )

        AuthFeedback(
            error = state.error,
            hint  = state.hint,
            onHintAction = { hint ->
                when (hint) {
                    AuthHint.SwitchToLogin -> viewModel.navigateTo(AuthFlowScreen.Login)
                    else -> Unit
                }
            }
        )
    }
}

// ── Forgot Password Screen ────────────────────────────────────────────────────

@Composable
private fun ForgotPasswordScreen(
    prefillEmail: String,
    state: AuthState,
    viewModel: AuthViewModel
) {
    val theme = LocalAppTheme.current
    var email = remember { mutableStateOf(prefillEmail) }

    AuthScaffold(
        heroTitle    = theme.t("Şifreni\nsıfırla.", "Reset your\npassword."),
        heroSubtitle = theme.t("Kayıtlı email adresini gir, link gönderelim.", "Enter your registered email and we'll send a link."),
        onBack       = { viewModel.navigateTo(AuthFlowScreen.Login) },
        backLabel    = theme.t("Giriş sayfasına dön", "Back to sign in")
    ) {
        GlassInputField(
            value         = email.value,
            onValueChange = { email.value = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
            autofillHint  = AuthAutofillHint.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onForgotPasswordClick(email.value) }
        )
        Spacer(Modifier.height(28.dp))

        AccentGradientButton(
            text      = if (state.isLoading) theme.t("Gönderiliyor...", "Sending...") else theme.t("Link Gönder", "Send Link"),
            onClick   = { if (!state.isLoading) viewModel.onForgotPasswordClick(email.value) },
            isLoading = state.isLoading,
            modifier  = Modifier.fillMaxWidth()
        )

        AuthFeedback(error = state.error, hint = null, onHintAction = {})
    }
}

// ── OTP Verify Screen ─────────────────────────────────────────────────────────

@Composable
private fun OtpVerifyScreen(
    email    : String,
    state    : AuthState,
    viewModel: AuthViewModel
) {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    var code   by remember { mutableStateOf("") }

    AuthCenteredScaffold(
        onBack    = { viewModel.navigateTo(AuthFlowScreen.Register) },
        backLabel = theme.t("Geri", "Back")
    ) {
        AuthIconCapsule(Icons.Rounded.MarkEmailRead)

        Spacer(Modifier.height(24.dp))
        Text(
            theme.t("Email'ini kontrol et", "Check your email"),
            style      = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold),
            color      = Color.White,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text       = theme.t(
                "$email\nadresine 6 haneli doğrulama kodu gönderdik.",
                "We sent a 6-digit verification code to\n$email."
            ),
            color      = Color.White.copy(0.75f),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        OtpInputRow(
            code         = code,
            onCodeChange = { new ->
                if (new.length <= 6 && new.all { it.isDigit() }) {
                    code = new
                    if (new.length == 6) viewModel.onVerifyOtp(email, new)
                }
            },
            accent = accent
        )

        AuthFeedback(error = state.error, hint = null, onHintAction = {}, onDarkScene = true)

        Spacer(Modifier.height(24.dp))

        AccentGradientButton(
            text      = if (state.otpLoading) theme.t("Doğrulanıyor...", "Verifying...") else theme.t("Onayla", "Confirm"),
            icon      = Icons.Rounded.Check,
            onClick   = { if (!state.otpLoading) viewModel.onVerifyOtp(email, code) },
            isLoading = state.otpLoading,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { viewModel.onResendOtp(email) },
            enabled = !state.resendCooldown
        ) {
            Text(
                text     = if (state.resendCooldown) theme.t("Kod gönderildi (30sn bekle)", "Code sent (wait 30s)") else theme.t("Kodu tekrar gönder", "Resend code"),
                color    = if (state.resendCooldown) Color.White.copy(0.5f) else accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(theme.t("Spam/junk klasörünü de kontrol et.", "Also check your spam/junk folder."), color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

/** 6 adet SinemaCapsule hane: dolu hane kenarı accent, aktif hane glow. */
@Composable
private fun OtpInputRow(
    code        : String,
    onCodeChange: (String) -> Unit,
    accent      : Color
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current
    val digitShape     = RoundedCornerShape(18.dp)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(contentAlignment = Alignment.Center) {
        BasicTextField(
            value           = code,
            onValueChange   = onCodeChange,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction    = ImeAction.Done
            ),
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(6) { idx ->
                val char     = code.getOrNull(idx)
                val isActive = code.length == idx
                SinemaCapsule(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isActive) Modifier.sinemaGlow(accent.copy(0.4f), 14.dp, 0.dp, digitShape) else Modifier),
                    height = 56.dp,
                    shape = digitShape,
                    edge = if (char != null || isActive) accent else Color.White.copy(0.18f),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.Center
                ) { fg ->
                    if (char != null) {
                        Text(
                            text       = char.toString(),
                            color      = fg,
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else if (isActive) {
                        val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
                            initialValue = 0f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "cursor_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(accent.copy(alpha = cursorAlpha))
                        )
                    }
                }
            }
        }
    }
}

// ── Email Sent Screen ─────────────────────────────────────────────────────────

@Composable
private fun EmailSentScreen(
    email    : String,
    type     : EmailSentType,
    state    : AuthState,
    viewModel: AuthViewModel
) {
    val theme  = LocalAppTheme.current

    val scaleAnim = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    AuthCenteredScaffold {
        AuthIconCapsule(Icons.Rounded.LockReset, Modifier.graphicsLayer(scaleX = scaleAnim.value, scaleY = scaleAnim.value))

        Spacer(Modifier.height(28.dp))
        Text(
            theme.t("Link gönderildi", "Link sent"),
            style      = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold),
            color      = Color.White,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            theme.t(
                "$email adresine şifre sıfırlama linki gönderdik.\n\nLinke tıklayarak yeni şifreni belirle.",
                "We sent a password reset link to $email.\n\nTap the link to set your new password."
            ),
            color      = Color.White.copy(0.75f),
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        AccentGradientButton(
            text     = theme.t("Giriş Sayfasına Dön", "Back to Sign In"),
            icon     = Icons.Rounded.Login,
            onClick  = { viewModel.navigateTo(AuthFlowScreen.Login) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(theme.t("Spam/junk klasörünü de kontrol et.", "Also check your spam/junk folder."), color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

/** Dev ikon kapsülü 88dp: dolu vurgu + glow (SinemaCapsule selected). */
@Composable
private fun AuthIconCapsule(icon: ImageVector, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        modifier = modifier.width(88.dp),
        selected = true,
        height = 88.dp,
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.Center
    ) { fg ->
        Icon(icon, null, tint = fg, modifier = Modifier.size(44.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── LAYOUT SCAFFOLDS ─────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Ana auth scaffold — üstte salon fotoğrafı + karartma (marka + başlık), altında form doğrudan zeminde.
 * İçerik kısaysa foto alanı büyür (alt ölü alan yok).
 */
@Composable
private fun AuthScaffold(
    heroTitle   : String,
    heroSubtitle: String,
    onBack      : (() -> Unit)? = null,
    backLabel   : String? = null,
    content     : @Composable ColumnScope.() -> Unit
) {
    val theme  = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    val motionOn = rememberSinemaMotionOn()
    val density = LocalDensity.current

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(if (motionOn) 28f else 0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(if (motionOn) 500 else 200, easing = EaseOutCubic))
        yAnim.animateTo(0f, tween(260, easing = EaseOutCubic))
    }

    // Foto yüksekliği: en az %42; form kısaysa ekranı dolduracak kadar büyür (en çok %62).
    var formHeightPx by remember { mutableIntStateOf(0) }
    val screenH = responsive.screenHeight
    val formH = with(density) { formHeightPx.toDp() }
    val heroH = (screenH - formH).coerceIn(screenH * 0.42f, screenH * 0.62f)

    Box(modifier = Modifier.fillMaxSize().background(Sinema.page(theme))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value)
        ) {
            AuthHero(
                height    = heroH,
                title     = heroTitle,
                subtitle  = heroSubtitle,
                onBack    = onBack,
                backLabel = backLabel,
                horizontalPadding = responsive.horizontalPadding
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = responsive.formMaxWidth)
                    .fillMaxWidth()
                    .onSizeChanged { formHeightPx = it.height }
                    .padding(horizontal = responsive.horizontalPadding)
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(20.dp))
                content()
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Üst foto bölgesi: foto + alt/üst karartma, marka başlığı, sol üstte geri, altta başlık. Yüklenmezse SinemaScene. */
@Composable
private fun AuthHero(
    height   : Dp,
    title    : String,
    subtitle : String,
    onBack   : (() -> Unit)?,
    backLabel: String?,
    horizontalPadding: Dp
) {
    Box(Modifier.fillMaxWidth().height(height)) {
        SubcomposeAsyncImage(
            model = categoryImageUrl(""),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { SinemaScene() },
            error = { SinemaScene() }
        )
        Box(Modifier.fillMaxSize().background(Sinema.BottomScrim))
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(0f to Color.Black.copy(Sinema.TopScrimMidAlpha), 1f to Color.Transparent))
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    SinemaCapsuleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        description = backLabel,
                        onClick = onBack,
                        selected = false,
                        size = 44.dp
                    )
                    Spacer(Modifier.width(12.dp))
                }
                AuthBrandHeader(Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text     = title,
                style    = Sinema.Title.copy(fontWeight = FontWeight.ExtraBold),
                color    = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = subtitle,
                    color    = Color.White.copy(0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AuthBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "Profitness",
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "PROFITNESS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.14.em
            )
        }
        SinemaPill {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em
            )
        }
    }
}

/**
 * Merkez hizalı scaffold — OTP ve EmailSent gibi tek odaklı ekranlar için: SinemaScene orb + ortalı içerik.
 * Sahne her zaman koyu; metinler beyaz.
 */
@Composable
private fun AuthCenteredScaffold(
    onBack   : (() -> Unit)? = null,
    backLabel: String? = null,
    content  : @Composable ColumnScope.() -> Unit
) {
    val responsive = rememberResponsiveLayoutInfo()
    val motionOn = rememberSinemaMotionOn()

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(if (motionOn) 30f else 0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(if (motionOn) 500 else 200))
        yAnim.animateTo(0f, tween(260, easing = EaseOutCubic))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        SinemaScene()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = responsive.formMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = responsive.horizontalPadding)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(72.dp))
            content()
            Spacer(Modifier.height(40.dp))
        }
        if (onBack != null) {
            SinemaCapsuleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = backLabel,
                onClick = onBack,
                selected = false,
                size = 44.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = responsive.horizontalPadding, top = 12.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── DESIGN SYSTEM COMPONENTS ─────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════

/** Tek SinemaSegmentRow (Giriş · Kayıt): seçili dolu vurgu + glow. */
@Composable
private fun AuthModeTabs(selected: AuthMode, onSelected: (AuthMode) -> Unit) {
    val theme = LocalAppTheme.current
    SinemaSegmentRow(
        options  = listOf(AuthMode.Login, AuthMode.Register),
        selected = selected,
        label    = { if (it == AuthMode.Login) theme.t("Giriş", "Sign in") else theme.t("Kayıt", "Sign up") },
        onSelect = onSelected,
        height   = 44.dp
    )
}

/**
 * Sinema giriş alanı: SinemaGlassField görünümü (etiket üstte, 52dp cam kapsül) + sol ikon + şifre göster/gizle.
 * Şifre yöneticisi autofill (AutofillNode + focus'ta requestAutofill) birebir korunur.
 */
@Composable
fun GlassInputField(
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    icon         : ImageVector,
    autofillHint : AuthAutofillHint,
    isPassword   : Boolean = false,
    showPass     : Boolean = false,
    onTogglePass : () -> Unit = {},
    keyboardType : KeyboardType = KeyboardType.Text,
    imeAction    : ImeAction = ImeAction.Next,
    onImeAction  : () -> Unit = {},
    modifier     : Modifier = Modifier
) {
    val theme  = LocalAppTheme.current
    val accent = theme.effectiveAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val autofillTypes = remember(autofillHint) {
        when (autofillHint) {
            AuthAutofillHint.LoginEmail -> listOf(AutofillType.Username, AutofillType.EmailAddress)
            AuthAutofillHint.NewEmail -> listOf(AutofillType.NewUsername, AutofillType.EmailAddress)
            AuthAutofillHint.Email -> listOf(AutofillType.EmailAddress)
            AuthAutofillHint.Password -> listOf(AutofillType.Password)
            AuthAutofillHint.NewPassword -> listOf(AutofillType.NewPassword)
        }
    }
    val autofillNode = remember(autofillHint) {
        AutofillNode(
            autofillTypes = autofillTypes,
            onFill = { currentOnValueChange(it) }
        )
    }
    var isPositionedForAutofill by remember(autofillNode) { mutableStateOf(false) }

    DisposableEffect(autofillTree, autofillNode) {
        autofillTree += autofillNode
        onDispose { autofillTree.children.remove(autofillNode.id) }
    }

    DisposableEffect(isFocused, isPositionedForAutofill, autofill, autofillNode) {
        if (isFocused && isPositionedForAutofill) {
            autofill?.requestAutofillForNode(autofillNode)
        }
        onDispose {
            if (isFocused && isPositionedForAutofill) {
                autofill?.cancelAutofillForNode(autofillNode)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            placeholder.sinemaUpper(theme),
            color = Sinema.textSecondary(theme), style = Sinema.Label,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        SinemaCapsule(
            modifier = Modifier.fillMaxWidth().then(modifier),
            height = 52.dp,
            light = !theme.isDark,
            edge = if (isFocused) accent.copy(0.72f) else Color.White.copy(0.18f),
            contentPadding = PaddingValues(start = 16.dp, end = if (isPassword) 6.dp else 16.dp)
        ) { fg ->
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (isFocused) accent else fg.copy(0.6f),
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value               = value,
                onValueChange       = onValueChange,
                visualTransformation = if (isPassword && !showPass)
                    PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions     = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction    = imeAction
                ),
                keyboardActions     = KeyboardActions(onAny = { onImeAction() }),
                interactionSource   = interactionSource,
                singleLine          = true,
                textStyle           = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                cursorBrush         = SolidColor(accent),
                modifier  = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        autofillNode.boundingBox = coordinates.boundsInWindow()
                        isPositionedForAutofill = true
                    }
            )
            if (isPassword) {
                IconButton(onClick = onTogglePass, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null, tint = fg.copy(0.6f), modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Ana eylem: SinemaGoButton (gradyan yok). `isLoading` = aynı düğme dim + mevcut "...yapılıyor" metni, tıklama kilitli.
 * `accent` yalnız imza uyumu için durur (kullanılmaz).
 */
@Composable
fun AccentGradientButton(
    text     : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    isLoading: Boolean = false,
    icon     : ImageVector? = null,
    @Suppress("UNUSED_PARAMETER") accent: Color = Color.Unspecified
) {
    SinemaGoButton(
        label    = text,
        icon     = icon ?: Icons.AutoMirrored.Rounded.ArrowForward,
        onClick  = { if (!isLoading) onClick() },
        modifier = modifier,
        dim      = isLoading
    )
}

// ── Backward-compatible aliases ──────────────────────────────────────────────

/** İkincil/nötr eylem: SinemaCapsule 52dp. */
@Composable
fun ObsidianButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") accent: Color = Color.Unspecified
) {
    val theme = LocalAppTheme.current
    SinemaCapsule(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        height = 52.dp,
        light = !theme.isDark,
        horizontalArrangement = Arrangement.Center
    ) { fg ->
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
fun AuthLiquidField(
    value        : String,
    onValueChange: (String) -> Unit,
    label        : String,
    icon         : ImageVector,
    autofillHint : AuthAutofillHint,
    isPassword   : Boolean = false,
    showPass     : Boolean = false,
    onTogglePass : () -> Unit = {},
    imeAction    : ImeAction = ImeAction.Next,
    onImeAction  : () -> Unit = {},
    modifier     : Modifier = Modifier
) {
    GlassInputField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = label.lowercase().replaceFirstChar { it.uppercase() },
        icon          = icon,
        autofillHint  = autofillHint,
        isPassword    = isPassword,
        showPass      = showPass,
        onTogglePass  = onTogglePass,
        imeAction     = imeAction,
        onImeAction   = onImeAction,
        modifier      = modifier
    )
}

// ── Shared sub-components ─────────────────────────────────────────────────────

/**
 * Hata = SinemaCapsule içinde 16dp uyarı ikonu (yalnız ikon hata rengi) + metin; kırmızı kutu yok.
 * `onDarkScene`: SinemaScene üstünde (OTP) koyu cam; aksi halde temaya göre.
 */
@Composable
internal fun AuthFeedback(
    error       : String?,
    hint        : AuthHint?,
    onHintAction: (AuthHint) -> Unit,
    onDarkScene : Boolean = false
) {
    val theme = LocalAppTheme.current
    Column {
        AnimatedVisibility(
            visible = error != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            error?.let {
                Column {
                    Spacer(Modifier.height(14.dp))
                    SinemaCapsule(
                        modifier = Modifier.fillMaxWidth(),
                        height = 40.dp,
                        light = !theme.isDark && !onDarkScene,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { fg ->
                        Icon(Icons.Rounded.ErrorOutline, null, tint = CriticalRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = it, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = hint != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            hint?.let {
                Column {
                    Spacer(Modifier.height(8.dp))
                    val (text, icon) = hintContent(it)
                    TextButton(
                        onClick = { onHintAction(it) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (onDarkScene) theme.effectiveAccentColor else theme.accentTextColor
                        )
                    ) {
                        Icon(icon, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun hintContent(hint: AuthHint): Pair<String, ImageVector> {
    val theme = LocalAppTheme.current
    return when (hint) {
        AuthHint.SwitchToLogin -> theme.t("Bu email zaten kayıtlı — Giriş Yap", "This email is already registered — Sign In") to Icons.Rounded.Login
        AuthHint.SwitchToRegister -> theme.t("Hesabın yok — Kayıt Ol", "No account yet — Sign Up") to Icons.Rounded.PersonAdd
        AuthHint.ForgotPassword -> theme.t("Şifreni mi unuttun? Sıfırla", "Forgot your password? Reset it") to Icons.Rounded.LockReset
    }
}

/** SinemaGlowBar 6dp + seviye etiketi (11sp/600). */
@Composable
internal fun PasswordStrengthBar(password: String) {
    val theme = LocalAppTheme.current
    val strength = when {
        password.length < 6  -> 0
        password.length < 8  -> 1
        password.length < 12 && !password.any { it.isDigit() } -> 2
        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 4
        else -> 3
    }
    val label = when (strength) {
        0    -> theme.t("Çok kısa", "Too short")
        1    -> theme.t("Zayıf", "Weak")
        2    -> theme.t("Orta", "Medium")
        3    -> theme.t("İyi", "Good")
        else -> theme.t("Güçlü", "Strong")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SinemaGlowBar(progress = strength / 4f, modifier = Modifier.weight(1f), height = 6.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (strength == 4) theme.accentTextColor else Sinema.textSecondary(theme),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
