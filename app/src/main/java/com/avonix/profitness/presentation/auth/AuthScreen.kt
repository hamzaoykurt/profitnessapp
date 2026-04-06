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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.avonix.profitness.core.theme.*

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
                AuthEvent.NavigateToDashboard     -> onNavigateToDashboard()
                AuthEvent.NavigateToOnboarding    -> onNavigateToOnboarding()
                AuthEvent.NavigateToAuthForRecovery -> { /* AppNavigation halleder; burada ek eylem yok */ }
                AuthEvent.NavigateToAuth            -> { /* Zaten auth ekranındayız; ek eylem yok */ }
            }
        }
    }

    // Session diskten yüklenirken boş ekran göster; login sayfası yanıp sönmez.
    if (state.isSessionLoading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    AnimatedContent(
        targetState  = state.screen,
        transitionSpec = {
            val enter  = slideInHorizontally(
                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
            ) { it / 2 } + fadeIn(tween(200))
            val exit   = slideOutHorizontally(
                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
            ) { -it / 3 } + fadeOut(tween(160))
            enter togetherWith exit
        },
        label = "auth_flow"
    ) { screen ->
        when (screen) {
            is AuthFlowScreen.Login          -> LoginScreen(state, viewModel)
            is AuthFlowScreen.Register       -> RegisterScreen(state, viewModel)
            is AuthFlowScreen.ForgotPassword -> ForgotPasswordScreen(screen.prefillEmail, state, viewModel)
            is AuthFlowScreen.OtpVerify      -> OtpVerifyScreen(screen.email, state, viewModel)
            is AuthFlowScreen.EmailSent      -> EmailSentScreen(screen.email, screen.type, state, viewModel)
            is AuthFlowScreen.NewPassword    -> NewPasswordScreen(state, viewModel)
        }
    }
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(state: AuthState, viewModel: AuthViewModel) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val theme    = LocalAppTheme.current
    val passFocus = remember { FocusRequester() }

    AuthScaffold(title = "Tekrar\nhoş geldin.") {
        AuthLiquidField(
            value        = email,
            onValueChange = { email = it },
            label        = "EMAIL",
            icon         = Icons.Rounded.Email,
            imeAction    = ImeAction.Next,
            onImeAction  = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(20.dp))
        AuthLiquidField(
            value         = password,
            onValueChange = { password = it },
            label         = "ŞİFRE",
            icon          = Icons.Rounded.Lock,
            isPassword    = true,
            showPass      = showPass,
            onTogglePass  = { showPass = !showPass },
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onLoginClick(email, password) },
            modifier      = Modifier.focusRequester(passFocus)
        )

        // "Şifremi Unuttum" link — right-aligned, subtle
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = {
                viewModel.navigateTo(AuthFlowScreen.ForgotPassword(email.trim()))
            }) {
                Text(
                    "Şifremi Unuttum",
                    color = ObsidianMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        ObsidianButton(
            text     = if (state.isLoading) "Giriş yapılıyor..." else "GİRİŞ YAP",
            onClick  = { if (!state.isLoading) viewModel.onLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth()
        )

        AuthFeedback(
            error = state.error,
            hint  = state.hint,
            onHintAction = { hint ->
                when (hint) {
                    AuthHint.ForgotPassword  -> viewModel.navigateTo(AuthFlowScreen.ForgotPassword(email.trim()))
                    else                     -> Unit
                }
            }
        )

        Spacer(Modifier.height(24.dp))
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
    val theme           = LocalAppTheme.current
    val passFocus       = remember { FocusRequester() }
    val confirmFocus    = remember { FocusRequester() }

    AuthScaffold(title = "Gücü\nserbest bırak.") {
        AuthLiquidField(
            value         = email,
            onValueChange = { email = it },
            label         = "EMAIL",
            icon          = Icons.Rounded.Email,
            imeAction     = ImeAction.Next,
            onImeAction   = { passFocus.requestFocus() }
        )
        Spacer(Modifier.height(20.dp))
        AuthLiquidField(
            value         = password,
            onValueChange = { password = it },
            label         = "ŞİFRE",
            icon          = Icons.Rounded.Lock,
            isPassword    = true,
            showPass      = showPass,
            onTogglePass  = { showPass = !showPass },
            imeAction     = ImeAction.Next,
            onImeAction   = { confirmFocus.requestFocus() },
            modifier      = Modifier.focusRequester(passFocus)
        )
        // Password strength bar
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            PasswordStrengthBar(password = password)
        }
        Spacer(Modifier.height(20.dp))
        AuthLiquidField(
            value         = confirmPassword,
            onValueChange = { confirmPassword = it },
            label         = "ŞİFRE TEKRAR",
            icon          = Icons.Rounded.LockOpen,
            isPassword    = true,
            showPass      = showConfirmPass,
            onTogglePass  = { showConfirmPass = !showConfirmPass },
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onRegisterClick(email, password, confirmPassword) },
            modifier      = Modifier.focusRequester(confirmFocus)
        )
        Spacer(Modifier.height(36.dp))

        ObsidianButton(
            text     = if (state.isLoading) "Hesap oluşturuluyor..." else "KAYIT OL",
            onClick  = { if (!state.isLoading) viewModel.onRegisterClick(email, password, confirmPassword) },
            modifier = Modifier.fillMaxWidth()
        )

        AuthFeedback(
            error = state.error,
            hint  = state.hint,
            onHintAction = { hint ->
                when (hint) {
                    AuthHint.SwitchToLogin -> viewModel.navigateTo(AuthFlowScreen.Login)
                    else                   -> Unit
                }
            }
        )

        Spacer(Modifier.height(24.dp))
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
    val theme = LocalAppTheme.current

    AuthScaffold(title = "Şifreni\nsıfırla.") {
        // Back row
        BackRow(onBack = { viewModel.navigateTo(AuthFlowScreen.Login) })
        Spacer(Modifier.height(16.dp))

        Text(
            "Kayıtlı email adresini gir, şifre sıfırlama linkini gönderelim.",
            color      = ObsidianSub,
            fontSize   = 14.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))

        AuthLiquidField(
            value         = email.value,
            onValueChange = { email.value = it },
            label         = "EMAIL",
            icon          = Icons.Rounded.Email,
            imeAction     = ImeAction.Done,
            onImeAction   = { viewModel.onForgotPasswordClick(email.value) }
        )
        Spacer(Modifier.height(36.dp))

        ObsidianButton(
            text     = if (state.isLoading) "Gönderiliyor..." else "LINK GÖNDER",
            onClick  = { if (!state.isLoading) viewModel.onForgotPasswordClick(email.value) },
            modifier = Modifier.fillMaxWidth()
        )

        AuthFeedback(error = state.error, hint = null, onHintAction = {})
    }
}

// ── OTP Verify Screen ─────────────────────────────────────────────────────────

@Composable
private fun OtpVerifyScreen(
    email     : String,
    state     : AuthState,
    viewModel : AuthViewModel
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    var code   by remember { mutableStateOf("") }

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
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Back
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { viewModel.navigateTo(AuthFlowScreen.Register) }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri", tint = theme.text1)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MarkEmailRead,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Email'ini kontrol et",
                color      = ObsidianText,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text      = "$email\nadresine 6 haneli doğrulama kodu gönderdik.",
                color     = ObsidianSub,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(40.dp))

            // OTP kutular
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

            // Hata
            AnimatedVisibility(state.error != null) {
                state.error?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, color = CriticalRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(32.dp))

            ObsidianButton(
                text     = if (state.otpLoading) "Doğrulanıyor..." else "ONAYLA",
                onClick  = { if (!state.otpLoading) viewModel.onVerifyOtp(email, code) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            TextButton(
                onClick = { viewModel.onResendOtp(email) },
                enabled = !state.resendCooldown
            ) {
                Text(
                    text  = if (state.resendCooldown) "Kod gönderildi (30sn bekle)" else "Kodu tekrar gönder",
                    color = if (state.resendCooldown) ObsidianMuted else accent,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))
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
}

@Composable
private fun OtpInputRow(
    code        : String,
    onCodeChange: (String) -> Unit,
    accent      : Color,
    theme       : com.avonix.profitness.core.theme.AppThemeState
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(contentAlignment = Alignment.Center) {
        // Gerçek input — görünmez, klavyeyi tetikler
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.bg2)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = when {
                                isFocused  -> accent
                                char != null -> accent.copy(alpha = 0.5f)
                                else         -> theme.stroke
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != null) {
                        Text(
                            text       = char.toString(),
                            color      = ObsidianText,
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

// ── Email Sent Screen (sadece şifre sıfırlama için) ───────────────────────────

@Composable
private fun EmailSentScreen(
    email     : String,
    type      : EmailSentType,
    state     : AuthState,
    viewModel : AuthViewModel
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.88f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(500))
        scaleAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = alphaAnim.value, scaleX = scaleAnim.value, scaleY = scaleAnim.value)
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LockReset, null, tint = accent, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Link gönderildi",
                color      = ObsidianText,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Black,
                modifier   = Modifier.graphicsLayer(alpha = alphaAnim.value)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "$email adresine şifre sıfırlama linki gönderdik.\n\nLinke tıklayarak yeni şifreni belirle.",
                color      = ObsidianSub,
                fontSize   = 15.sp,
                lineHeight = 24.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.graphicsLayer(alpha = alphaAnim.value)
            )

            Spacer(Modifier.height(48.dp))

            ObsidianButton(
                text     = "Giriş Sayfasına Dön",
                onClick  = { viewModel.navigateTo(AuthFlowScreen.Login) },
                modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = alphaAnim.value)
            )

            Spacer(Modifier.height(20.dp))
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
}

// ── New Password Screen ───────────────────────────────────────────────────────

@Composable
private fun NewPasswordScreen(
    state    : AuthState,
    viewModel: AuthViewModel
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPass        by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    val confirmFocus    = remember { FocusRequester() }
    val keyboard        = LocalSoftwareKeyboardController.current

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(420))
        yAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    // Loading sırasında (session geri yüklenirken) sade bir gösterge
    if (state.isLoading) {
        Box(
            modifier         = Modifier.fillMaxSize().background(theme.bg0),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = accent)
                Spacer(Modifier.height(16.dp))
                Text("Bağlantı doğrulanıyor…", color = theme.text2, fontSize = 14.sp)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LockReset, null, tint = accent, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Yeni şifre belirle",
                color      = ObsidianText,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hesabın için güçlü bir şifre seç.",
                color    = ObsidianSub,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(36.dp))

            AuthLiquidField(
                value         = password,
                onValueChange = { password = it },
                label         = "YENİ ŞİFRE",
                icon          = Icons.Rounded.Lock,
                isPassword    = true,
                showPass      = showPass,
                onTogglePass  = { showPass = !showPass },
                imeAction     = ImeAction.Next,
                onImeAction   = { confirmFocus.requestFocus() }
            )

            if (password.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                PasswordStrengthBar(password = password)
            }

            Spacer(Modifier.height(16.dp))

            AuthLiquidField(
                value         = confirmPassword,
                onValueChange = { confirmPassword = it },
                label         = "ŞİFRE TEKRAR",
                icon          = Icons.Rounded.Lock,
                isPassword    = true,
                showPass      = showConfirmPass,
                onTogglePass  = { showConfirmPass = !showConfirmPass },
                imeAction     = ImeAction.Done,
                onImeAction   = {
                    keyboard?.hide()
                    viewModel.onNewPasswordSubmit(password, confirmPassword)
                },
                modifier      = Modifier.focusRequester(confirmFocus)
            )

            Spacer(Modifier.height(32.dp))

            ObsidianButton(
                text     = "ŞİFREYİ GÜNCELLE",
                onClick  = {
                    keyboard?.hide()
                    viewModel.onNewPasswordSubmit(password, confirmPassword)
                },
                modifier = Modifier.fillMaxWidth()
            )

            AuthFeedback(error = state.error, hint = null, onHintAction = {})
        }
    }
}

// ── Shared layout scaffold ────────────────────────────────────────────────────

@Composable
private fun AuthScaffold(
    title   : String,
    content : @Composable ColumnScope.() -> Unit
) {
    val theme     = LocalAppTheme.current
    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(32f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(600, easing = EaseOutExpo))
        yAnim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp, 0.dp)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yAnim.value),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(100.dp))

            // Monogram
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberCore),
                contentAlignment = Alignment.Center
            ) {
                Text("P", color = Color.Black, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text      = title,
                style     = MaterialTheme.typography.displayLarge,
                color     = ObsidianText,
                lineHeight = 52.sp
            )

            Spacer(Modifier.height(40.dp))

            content()
        }
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri", tint = ObsidianSub)
        }
        Text("Giriş sayfasına dön", color = ObsidianSub, fontSize = 13.sp)
    }
}

@Composable
private fun AuthFeedback(
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
                Text(
                    text  = it,
                    color = CriticalRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(4.dp, 0.dp)
                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(message, color = ObsidianMuted, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = onClick) {
            Text(actionText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
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
                    .background(if (i < strength) color else color.copy(0.2f))
            )
            if (i < 3) Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── AuthLiquidField ───────────────────────────────────────────────────────────

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
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.bg2)
                .drawWithCache {
                    val rimBrush = Brush.horizontalGradient(
                        listOf(Color.Transparent, accent.copy(0.3f), Color.Transparent)
                    )
                    val rimHeight = 1.dp.toPx()
                    onDrawBehind {
                        drawRect(brush = rimBrush, size = Size(size.width, rimHeight))
                    }
                }
                .then(modifier),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(16.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = ObsidianMuted,
                    modifier           = Modifier.size(20.dp)
                )
                TextField(
                    value               = value,
                    onValueChange       = onValueChange,
                    visualTransformation = if (isPassword && !showPass)
                        PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions     = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                        imeAction    = imeAction
                    ),
                    keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor    = Color.Transparent,
                        unfocusedContainerColor  = Color.Transparent,
                        focusedIndicatorColor    = Color.Transparent,
                        unfocusedIndicatorColor  = Color.Transparent,
                        focusedTextColor         = ObsidianText,
                        unfocusedTextColor       = ObsidianText
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (isPassword) {
                    IconButton(onClick = onTogglePass) {
                        Icon(
                            if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            null, tint = ObsidianMuted
                        )
                    }
                }
            }
        }
    }
}

// ── ObsidianButton ────────────────────────────────────────────────────────────

@Composable
fun ObsidianButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    accent  : Color = Color.Unspecified
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

    Box(
        modifier = modifier
            .scale(scale)
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(resolvedAccent)
            .clickable(interactionSource, null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = text,
            color         = resolvedOnAccent,
            fontSize      = 15.sp,
            fontWeight    = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}
