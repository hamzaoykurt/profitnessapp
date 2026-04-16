package com.avonix.profitness.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.avonix.profitness.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.avonix.profitness.core.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Entry composable — routes between auth screens ────────────────────────────

@Composable
fun AuthScreen(
    onNavigateToDashboard : () -> Unit,
    onNavigateToOnboarding: () -> Unit = onNavigateToDashboard,
    viewModel             : AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToDashboard  -> onNavigateToDashboard()
                AuthEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                AuthEvent.NavigateToAuth       -> { /* Zaten auth ekranındayız; ek eylem yok */ }
            }
        }
    }

    // Session diskten yüklenirken splash göster
    if (state.isSessionLoading) {
        AuthLoadingSplash()
        return
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
    }
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(state: AuthState, viewModel: AuthViewModel) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val passFocus = remember { FocusRequester() }

    AuthScaffold(
        heroTitle    = "Tekrar\nhoş geldin.",
        heroSubtitle = "Hedeflerine kaldığın yerden devam et."
    ) {
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = "Email adresi",
            icon          = Icons.Rounded.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            onImeAction   = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = password,
            onValueChange = { password = it },
            placeholder   = "Şifre",
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
                    "Şifremi Unuttum",
                    color    = ObsidianMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        AccentGradientButton(
            text     = if (state.isLoading) "Giriş yapılıyor..." else "Giriş Yap",
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

        Spacer(Modifier.height(28.dp))
        AuthSwitchRow(
            message    = "Hesabın yok mu?",
            actionText = "Kayıt Ol",
            onClick    = { viewModel.navigateTo(AuthFlowScreen.Register) }
        )
    }
}

// ── Register Screen ───────────────────────────────────────────────────────────

@Composable
private fun RegisterScreen(state: AuthState, viewModel: AuthViewModel) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPass        by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    val passFocus       = remember { FocusRequester() }
    val confirmFocus    = remember { FocusRequester() }

    AuthScaffold(
        heroTitle    = "Gücü\nserbest bırak.",
        heroSubtitle = "Hesabını oluştur, antrenmanlarına başla."
    ) {
        GlassInputField(
            value         = email,
            onValueChange = { email = it },
            placeholder   = "Email adresi",
            icon          = Icons.Rounded.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Next,
            onImeAction   = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(14.dp))
        GlassInputField(
            value         = password,
            onValueChange = { password = it },
            placeholder   = "Şifre",
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
            placeholder   = "Şifre tekrar",
            icon          = Icons.Rounded.LockOpen,
            isPassword    = true,
            showPass      = showConfirmPass,
            onTogglePass  = { showConfirmPass = !showConfirmPass },
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onRegisterClick(email, password, confirmPassword) },
            modifier      = Modifier.focusRequester(confirmFocus)
        )
        Spacer(Modifier.height(28.dp))

        AccentGradientButton(
            text      = if (state.isLoading) "Hesap oluşturuluyor..." else "Kayıt Ol",
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

        Spacer(Modifier.height(28.dp))
        AuthSwitchRow(
            message    = "Zaten üye misin?",
            actionText = "Giriş Yap",
            onClick    = { viewModel.navigateTo(AuthFlowScreen.Login) }
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
    var email = remember { mutableStateOf(prefillEmail) }

    AuthScaffold(
        heroTitle    = "Şifreni\nsıfırla.",
        heroSubtitle = "Kayıtlı email adresini gir, link gönderelim."
    ) {
        BackRow(onBack = { viewModel.navigateTo(AuthFlowScreen.Login) })
        Spacer(Modifier.height(20.dp))

        GlassInputField(
            value         = email.value,
            onValueChange = { email.value = it },
            placeholder   = "Email adresi",
            icon          = Icons.Rounded.Email,
            keyboardType  = KeyboardType.Email,
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onForgotPasswordClick(email.value) }
        )
        Spacer(Modifier.height(28.dp))

        AccentGradientButton(
            text      = if (state.isLoading) "Gönderiliyor..." else "Link Gönder",
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
            IconButton(onClick = { viewModel.navigateTo(AuthFlowScreen.Register) }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri", tint = theme.text1)
            }
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
            "Email'ini kontrol et",
            color      = theme.text0,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text       = "$email\nadresine 6 haneli doğrulama kodu gönderdik.",
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
            text      = if (state.otpLoading) "Doğrulanıyor..." else "Onayla",
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
                text     = if (state.resendCooldown) "Kod gönderildi (30sn bekle)" else "Kodu tekrar gönder",
                color    = if (state.resendCooldown) ObsidianMuted else accent,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = ObsidianMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Spam/junk klasörünü de kontrol et.", color = ObsidianMuted, fontSize = 12.sp)
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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) { idx ->
                val char      = code.getOrNull(idx)
                val isFocused = code.length == idx
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 58.dp)
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
            "Link gönderildi",
            color      = theme.text0,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "$email adresine şifre sıfırlama linki gönderdik.\n\nLinke tıklayarak yeni şifreni belirle.",
            color      = theme.text1,
            fontSize   = 15.sp,
            lineHeight = 24.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        AccentGradientButton(
            text     = "Giriş Sayfasına Dön",
            onClick  = { viewModel.navigateTo(AuthFlowScreen.Login) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Info, null, tint = ObsidianMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Spam/junk klasörünü de kontrol et.", color = ObsidianMuted, fontSize = 12.sp)
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
    heroTitle   : String,
    heroSubtitle: String,
    content     : @Composable ColumnScope.() -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(500, easing = EaseOutCubic))
        yAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        // Ambient glow orbs
        AmbientGlowBackground(accent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Image(
                painter            = painterResource(R.drawable.ic_app_logo),
                contentDescription = "Profitness",
                modifier           = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(Modifier.height(24.dp))

            // Hero title
            Text(
                text       = heroTitle,
                style      = MaterialTheme.typography.displayLarge,
                color      = theme.text0,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = heroSubtitle,
                color    = theme.text1,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // Glass card with form
            GlassCard {
                content()
            }

            Spacer(Modifier.height(40.dp))
        }
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
    val accent = MaterialTheme.colorScheme.primary

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(500))
        yAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        AmbientGlowBackground(accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
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
 * Glassmorphism kart — yarı saydam arka plan, ince border, blur efekti hissi.
 */
@Composable
private fun GlassCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val cardBg = if (theme.isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
    val borderColor = if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/**
 * Ambient glow arka plan — accent renginde yumuşak ışıma orb'ları.
 */
@Composable
private fun AmbientGlowBackground(accent: Color) {
    val theme = LocalAppTheme.current
    val orbAlpha = if (theme.isDark) 0.12f else 0.06f

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val orb1 = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to accent.copy(alpha = orbAlpha),
                        0.5f to accent.copy(alpha = orbAlpha * 0.3f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 1.2f
                )
                val orb2 = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to accent.copy(alpha = orbAlpha * 0.5f),
                        0.6f to accent.copy(alpha = orbAlpha * 0.1f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.7f),
                    radius = size.width * 0.9f
                )
                onDrawBehind {
                    drawRect(orb1)
                    drawRect(orb2)
                }
            }
    )
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
        if (isFocused) accent.copy(alpha = 0.6f)
        else if (theme.isDark) Color.White.copy(alpha = 0.08f)
        else Color.Black.copy(alpha = 0.06f),
        animationSpec = tween(200),
        label = "field_border"
    )
    val iconColor by animateColorAsState(
        if (isFocused) accent else ObsidianMuted,
        animationSpec = tween(200),
        label = "field_icon"
    )
    val fieldBg = if (theme.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fieldBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
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
                modifier           = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            TextField(
                value               = value,
                onValueChange       = onValueChange,
                placeholder         = { Text(placeholder, color = ObsidianMuted, fontSize = 15.sp) },
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
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                modifier  = Modifier.weight(1f)
            )
            if (isPassword) {
                IconButton(onClick = onTogglePass, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null, tint = ObsidianMuted, modifier = Modifier.size(20.dp)
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
    accent   : Color = Color.Unspecified
) {
    val resolvedAccent   = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
    val resolvedOnAccent = MaterialTheme.colorScheme.onPrimary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )

    val gradientBrush = Brush.horizontalGradient(
        listOf(
            resolvedAccent,
            resolvedAccent.copy(alpha = 0.85f)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable(interactionSource, null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(22.dp),
                color     = resolvedOnAccent,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text          = text,
                color         = resolvedOnAccent,
                fontSize      = 15.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onBack)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri", tint = ObsidianSub, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Giriş sayfasına dön", color = ObsidianSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

private fun hintContent(hint: AuthHint): Pair<String, ImageVector> = when (hint) {
    AuthHint.SwitchToLogin    -> "Bu email zaten kayıtlı — Giriş Yap" to Icons.Rounded.Login
    AuthHint.SwitchToRegister -> "Hesabın yok — Kayıt Ol" to Icons.Rounded.PersonAdd
    AuthHint.ForgotPassword   -> "Şifreni mi unuttun? Sıfırla" to Icons.Rounded.LockReset
}

@Composable
private fun AuthSwitchRow(message: String, actionText: String, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(message, color = ObsidianMuted, fontSize = 14.sp)
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
    val strength = when {
        password.length < 6  -> 0
        password.length < 8  -> 1
        password.length < 12 && !password.any { it.isDigit() } -> 2
        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 4
        else -> 3
    }
    val (label, color) = when (strength) {
        0    -> "Çok kısa" to CriticalRed
        1    -> "Zayıf"   to CriticalRed.copy(0.8f)
        2    -> "Orta"    to Amber
        3    -> "İyi"     to Lime
        else -> "Güçlü"  to accent
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
