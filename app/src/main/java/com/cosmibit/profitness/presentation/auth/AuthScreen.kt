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
import com.cosmibit.profitness.presentation.components.AppBackButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// ── Entry composable — routes between auth screens ────────────────────────────

private enum class AuthMode {
    Login,
    Register
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
    val theme = LocalAppTheme.current

    // Giriş animasyonu: siyah ekrandan spring ile fırlayarak çıkış
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
        modifier         = Modifier.fillMaxSize().background(theme.bg0),
        contentAlignment = Alignment.Center
    ) {
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
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = theme.text2.copy(0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = iconAlpha }
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
        mode         = AuthMode.Login,
        onModeChange = { mode ->
            if (mode == AuthMode.Register) viewModel.navigateTo(AuthFlowScreen.Register)
        },
        heroTitle    = theme.t("Giriş yap", "Sign in"),
        heroSubtitle = ""
    ) {
        AuthModeTabs(
            selected = AuthMode.Login,
            onSelected = { mode ->
                if (mode == AuthMode.Register) viewModel.navigateTo(AuthFlowScreen.Register)
            }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
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
                    color    = theme.text2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(14.dp))

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
        mode         = AuthMode.Register,
        onModeChange = { mode ->
            if (mode == AuthMode.Login) viewModel.navigateTo(AuthFlowScreen.Login)
        },
        heroTitle    = theme.t("Kayıt ol", "Sign up"),
        heroSubtitle = ""
    ) {
        AuthModeTabs(
            selected = AuthMode.Register,
            onSelected = { mode ->
                if (mode == AuthMode.Login) viewModel.navigateTo(AuthFlowScreen.Login)
            }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
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
            isPassword    = true,
            showPass      = showPass,
            onTogglePass  = { showPass = !showPass },
            imeAction     = ImeAction.Next,
            onImeAction   = { confirmFocus.requestFocus() },
            modifier      = Modifier.focusRequester(passFocus)
        )
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordStrengthBar(password = password)
        }
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder   = theme.t("Şifre tekrar", "Repeat password"),
            icon          = Icons.Rounded.LockOpen,
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
        Spacer(Modifier.height(18.dp))

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
        heroSubtitle = theme.t("Kayıtlı email adresini gir, link gönderelim.", "Enter your registered email and we'll send a link.")
    ) {
        BackRow(onBack = { viewModel.navigateTo(AuthFlowScreen.Login) })
        Spacer(Modifier.height(20.dp))

        GlassInputField(
            value         = email.value,
            onValueChange = { email.value = it },
            placeholder   = theme.t("Email adresi", "Email address"),
            icon          = Icons.Rounded.Email,
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
    val accent = MaterialTheme.colorScheme.primary
    var code   by remember { mutableStateOf("") }

    AuthCenteredScaffold {
        // Back
        Box(modifier = Modifier.fillMaxWidth()) {
            AppBackButton(
                onClick = { viewModel.navigateTo(AuthFlowScreen.Register) },
                accent = accent,
                size = 42.dp
            )
        }
        Spacer(Modifier.height(12.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MarkEmailRead,
                contentDescription = null,
                tint     = accent,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            theme.t("Email'ini kontrol et", "Check your email"),
            color      = theme.text0,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text       = theme.t(
                "$email\nadresine 6 haneli doğrulama kodu gönderdik.",
                "We sent a 6-digit verification code to\n$email."
            ),
            color      = theme.text1,
            fontSize   = 14.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(36.dp))

        OtpInputRow(
            code         = code,
            onCodeChange = { new ->
                if (new.length <= 6 && new.all { it.isDigit() }) {
                    code = new
                    if (new.length == 6) viewModel.onVerifyOtp(email, new)
                }
            },
            accent = accent,
            theme  = theme
        )

        AnimatedVisibility(state.error != null) {
            state.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = CriticalRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(28.dp))

        AccentGradientButton(
            text      = if (state.otpLoading) theme.t("Doğrulanıyor...", "Verifying...") else theme.t("Onayla", "Confirm"),
            onClick   = { if (!state.otpLoading) viewModel.onVerifyOtp(email, code) },
            isLoading = state.otpLoading,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = { viewModel.onResendOtp(email) },
            enabled = !state.resendCooldown
        ) {
            Text(
                text     = if (state.resendCooldown) theme.t("Kod gönderildi (30sn bekle)", "Code sent (wait 30s)") else theme.t("Kodu tekrar gönder", "Resend code"),
                color    = if (state.resendCooldown) theme.text2 else accent,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = theme.text2, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(theme.t("Spam/junk klasörünü de kontrol et.", "Also check your spam/junk folder."), color = theme.text2, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OtpInputRow(
    code        : String,
    onCodeChange: (String) -> Unit,
    accent      : Color,
    theme       : AppThemeState
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

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
                val char      = code.getOrNull(idx)
                val isFocused = code.length == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(46f / 58f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bg2)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = when {
                                isFocused   -> accent
                                char != null -> accent.copy(alpha = 0.4f)
                                else         -> theme.stroke
                            },
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != null) {
                        Text(
                            text       = char.toString(),
                            color      = theme.text0,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isFocused) {
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
    val accent = MaterialTheme.colorScheme.primary

    val scaleAnim = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    AuthCenteredScaffold {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scaleAnim.value, scaleY = scaleAnim.value)
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.LockReset, null, tint = accent, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(28.dp))
        Text(
            theme.t("Link gönderildi", "Link sent"),
            color      = theme.text0,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(12.dp))
        Text(
            theme.t(
                "$email adresine şifre sıfırlama linki gönderdik.\n\nLinke tıklayarak yeni şifreni belirle.",
                "We sent a password reset link to $email.\n\nTap the link to set your new password."
            ),
            color      = theme.text1,
            fontSize   = 15.sp,
            lineHeight = 24.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        AccentGradientButton(
            text     = theme.t("Giriş Sayfasına Dön", "Back to Sign In"),
            onClick  = { viewModel.navigateTo(AuthFlowScreen.Login) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = theme.text2, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(theme.t("Spam/junk klasörünü de kontrol et.", "Also check your spam/junk folder."), color = theme.text2, fontSize = 12.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── LAYOUT SCAFFOLDS ─────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Ana auth scaffold — logo + hero başlık + glassmorphism kart içinde form alanları.
 */
@Composable
private fun AuthScaffold(
    mode         : AuthMode? = null,
    onModeChange : (AuthMode) -> Unit = {},
    heroTitle   : String,
    heroSubtitle: String,
    content     : @Composable ColumnScope.() -> Unit
) {
    val theme  = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()
    val heroStyle = if (responsive.isSmallPhone || responsive.isLargeFont) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.headlineLarge
    }
    val topPad = when {
        responsive.isShortScreen -> 36.dp
        responsive.isSmallPhone -> 52.dp
        else -> 72.dp
    }

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(28f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(500, easing = EaseOutCubic))
        yAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = responsive.formMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = responsive.horizontalPadding)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(topPad))

            AuthBrandHeader()

            Spacer(Modifier.height(if (responsive.isSmallPhone) 18.dp else 22.dp))

            Text(
                text       = heroTitle,
                style      = heroStyle,
                color      = theme.text0,
                lineHeight = if (responsive.isLargeFont) 34.sp else 38.sp,
                fontWeight = FontWeight.Black,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            if (heroSubtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = heroSubtitle,
                    color    = theme.text1,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(if (responsive.isShortScreen) 18.dp else 24.dp))

            GlassCard {
                content()
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun AuthBrandHeader() {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "Profitness",
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, if (theme.isDark) Color.White.copy(0.10f) else theme.stroke, RoundedCornerShape(13.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "PROFITNESS",
                color = theme.text0,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(0.12f))
                .border(1.dp, accent.copy(0.26f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun AuthHeroQuickSwitch(mode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val target = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
    val text = if (mode == AuthMode.Login) {
        theme.t("Yeni misin? Hızlıca hesap oluştur", "New here? Create an account")
    } else {
        theme.t("Zaten hesabın var mı? Giriş yap", "Already have an account? Sign in")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(theme.bg2.copy(0.58f))
            .border(1.dp, theme.stroke.copy(0.55f), RoundedCornerShape(999.dp))
            .clickable { onModeChange(target) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (mode == AuthMode.Login) Icons.Rounded.PersonAdd else Icons.Rounded.Login,
            null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = theme.text1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Merkez hizalı scaffold — OTP ve EmailSent gibi tek odaklı ekranlar için.
 */
@Composable
private fun AuthCenteredScaffold(
    content: @Composable ColumnScope.() -> Unit
) {
    val theme  = LocalAppTheme.current
    val responsive = rememberResponsiveLayoutInfo()

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(500))
        yAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
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
            Spacer(Modifier.height(80.dp))
            GlassCard(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── DESIGN SYSTEM COMPONENTS ─────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Authentication surface. Dark mode keeps a restrained forged sheen while light
 * mode uses a deliberately solid paper-white card with slate depth.
 */
@Composable
private fun GlassCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)
    val cardBrush = if (theme.isDark) {
        Brush.verticalGradient(listOf(theme.bg2, theme.bg1))
    } else {
        Brush.verticalGradient(listOf(theme.bg1, theme.bg2.copy(alpha = 0.58f)))
    }
    val borderColor = if (theme.isDark) Color.White.copy(alpha = 0.10f) else theme.stroke.copy(alpha = 0.80f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (theme.isDark) 18.dp else 12.dp,
                shape = shape,
                clip = false,
                ambientColor = if (theme.isDark) Color.Black.copy(0.54f) else Color(0xFF64748B).copy(0.10f),
                spotColor = if (theme.isDark) accent.copy(0.12f) else Color(0xFF64748B).copy(0.14f)
            )
            .clip(shape)
            .background(cardBrush)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(accent.copy(0.34f), borderColor, borderColor.copy(0.42f))
                ),
                shape
            )
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, accent.copy(0.24f), Color.Transparent)
                        ),
                        topLeft = Offset(24.dp.toPx(), 0f),
                        size = Size((size.width - 48.dp.toPx()).coerceAtLeast(0f), 1.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
private fun AuthModeTabs(selected: AuthMode, onSelected: (AuthMode) -> Unit) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (theme.isDark) theme.bg2 else theme.bg1)
            .border(1.dp, theme.stroke.copy(0.52f), RoundedCornerShape(16.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AuthModeTab(
            text = theme.t("Giriş", "Sign in"),
            icon = Icons.Rounded.Login,
            selected = selected == AuthMode.Login,
            onClick = { onSelected(AuthMode.Login) },
            modifier = Modifier.weight(1f)
        )
        AuthModeTab(
            text = theme.t("Kayıt", "Sign up"),
            icon = Icons.Rounded.PersonAdd,
            selected = selected == AuthMode.Register,
            onClick = { onSelected(AuthMode.Register) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthModeTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val bg by animateColorAsState(
        if (selected) accent.copy(0.18f) else Color.Transparent,
        tween(180),
        label = "auth_mode_bg"
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) accent.copy(0.50f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) accent else theme.text2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            color = if (selected) theme.text0 else theme.text2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun AuthTrustRow(items: List<Pair<ImageVector, String>>) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (icon, label) ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(theme.bg2.copy(0.42f))
                    .border(1.dp, theme.stroke.copy(0.45f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    label,
                    color = theme.text1,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AuthFinePrint(text: String) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Lock, null, tint = theme.text2, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            color = theme.text2,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * Modern input field — yumuşak arka plan, ince border, focus animasyonu.
 */
@Composable
fun GlassInputField(
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    icon         : ImageVector,
    isPassword   : Boolean = false,
    showPass     : Boolean = false,
    onTogglePass : () -> Unit = {},
    keyboardType : KeyboardType = KeyboardType.Text,
    imeAction    : ImeAction = ImeAction.Next,
    onImeAction  : () -> Unit = {},
    modifier     : Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        if (isFocused) accent.copy(alpha = 0.72f)
        else theme.stroke.copy(alpha = if (theme.isDark) 0.82f else 0.95f),
        animationSpec = tween(200),
        label = "field_border"
    )
    val iconColor by animateColorAsState(
        if (isFocused) accent else theme.text2,
        animationSpec = tween(200),
        label = "field_icon"
    )
    val fieldBg by animateColorAsState(
        if (isFocused) accent.copy(0.08f)
        else if (theme.isDark) theme.bg2.copy(alpha = 0.88f)
        else theme.bg1,
        tween(180),
        label = "field_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .shadow(
                elevation = if (isFocused) 8.dp else if (theme.isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = if (theme.isDark) accent.copy(0.12f) else Color(0xFF64748B).copy(0.08f),
                spotColor = if (theme.isDark) accent.copy(0.08f) else Color(0xFF64748B).copy(0.10f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(fieldBg)
            .border(if (isFocused) 1.4.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            TextField(
                value               = value,
                onValueChange       = onValueChange,
                placeholder         = { Text(placeholder, color = theme.text2, fontSize = 14.sp) },
                visualTransformation = if (isPassword && !showPass)
                    PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions     = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction    = imeAction
                ),
                keyboardActions     = KeyboardActions(onAny = { onImeAction() }),
                interactionSource   = interactionSource,
                singleLine          = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor    = Color.Transparent,
                    unfocusedContainerColor  = Color.Transparent,
                    focusedIndicatorColor    = Color.Transparent,
                    unfocusedIndicatorColor  = Color.Transparent,
                    focusedTextColor         = theme.text0,
                    unfocusedTextColor       = theme.text0,
                    cursorColor              = accent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                modifier  = Modifier.weight(1f)
            )
            if (isPassword) {
                IconButton(onClick = onTogglePass, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null, tint = theme.text2, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Gradient accent button — accent renginde gradient dolgu, press animasyonu.
 */
@Composable
fun AccentGradientButton(
    text     : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    isLoading: Boolean = false,
    icon     : ImageVector? = null,
    accent   : Color = Color.Unspecified
) {
    val resolvedAccent   = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
    val resolvedOnAccent = resolvedAccent.readableOnAccentColor()
    val theme = LocalAppTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.965f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else if (theme.isDark) 12.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_elevation"
    )
    val shape = RoundedCornerShape(16.dp)

    val gradientBrush = Brush.horizontalGradient(
        listOf(
            resolvedAccent,
            resolvedAccent.copy(alpha = 0.85f)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { translationY = if (isPressed) 2.dp.toPx() else 0f }
            .heightIn(min = 54.dp)
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = if (theme.isDark) Color.Black.copy(0.48f) else Color(0xFF64748B).copy(0.12f),
                spotColor = resolvedAccent.copy(if (theme.isDark) 0.34f else 0.20f)
            )
            .clip(shape)
            .background(gradientBrush)
            .drawWithCache {
                val bevel = Brush.verticalGradient(
                    listOf(Color.White.copy(if (theme.isDark) 0.26f else 0.34f), Color.Transparent, Color.Black.copy(0.16f))
                )
                onDrawBehind { drawRect(bevel) }
            }
            .border(1.dp, Color.White.copy(if (theme.isDark) 0.24f else 0.38f), shape)
            .clickable(interactionSource, null, enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(22.dp),
                color     = resolvedOnAccent,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = resolvedOnAccent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text          = text,
                    color         = resolvedOnAccent,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

// ── Backward-compatible aliases ──────────────────────────────────────────────

@Composable
fun ObsidianButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    accent  : Color = Color.Unspecified
) {
    AccentGradientButton(text = text, onClick = onClick, modifier = modifier, accent = accent)
}

@Composable
fun AuthLiquidField(
    value        : String,
    onValueChange: (String) -> Unit,
    label        : String,
    icon         : ImageVector,
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
        isPassword    = isPassword,
        showPass      = showPass,
        onTogglePass  = onTogglePass,
        imeAction     = imeAction,
        onImeAction   = onImeAction,
        modifier      = modifier
    )
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun BackRow(onBack: () -> Unit) {
    val theme = LocalAppTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        AppBackButton(onClick = onBack, accent = MaterialTheme.colorScheme.primary, size = 36.dp)
        Spacer(Modifier.width(8.dp))
        Text(theme.t("Giriş sayfasına dön", "Back to sign in"), color = theme.text1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun AuthFeedback(
    error       : String?,
    hint        : AuthHint?,
    onHintAction: (AuthHint) -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = error != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            error?.let {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CriticalRed.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline, null,
                        tint = CriticalRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text  = it,
                        color = CriticalRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = hint != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            hint?.let {
                Spacer(Modifier.height(8.dp))
                val (text, icon) = hintContent(it)
                TextButton(onClick = { onHintAction(it) }) {
                    Icon(icon, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text, fontSize = 13.sp)
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

@Composable
private fun AuthSwitchRow(message: String, actionText: String, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(message, color = theme.text2, fontSize = 14.sp)
        TextButton(onClick = onClick) {
            Text(
                actionText,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = accent
            )
        }
    }
}

@Composable
internal fun PasswordStrengthBar(password: String) {
    val accent = MaterialTheme.colorScheme.primary
    val theme = LocalAppTheme.current
    val strength = when {
        password.length < 6  -> 0
        password.length < 8  -> 1
        password.length < 12 && !password.any { it.isDigit() } -> 2
        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 4
        else -> 3
    }
    val (label, color) = when (strength) {
        0    -> theme.t("Çok kısa", "Too short") to CriticalRed
        1    -> theme.t("Zayıf", "Weak") to CriticalRed.copy(0.8f)
        2    -> theme.t("Orta", "Medium") to Amber
        3    -> theme.t("İyi", "Good") to Lime
        else -> theme.t("Güçlü", "Strong") to accent
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < strength) color else color.copy(0.15f))
            )
            if (i < 3) Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
