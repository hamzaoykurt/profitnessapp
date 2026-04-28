package com.avonix.profitness.presentation.resetpassword

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
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.auth.AuthFeedback
import com.avonix.profitness.presentation.auth.AuthLiquidField
import com.avonix.profitness.presentation.auth.ObsidianButton
import com.avonix.profitness.presentation.auth.PasswordStrengthBar

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
    val accent = MaterialTheme.colorScheme.primary
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
}

// ── Link geçersiz ─────────────────────────────────────────────────────────────

@Composable
private fun InvalidLinkContent(onDone: () -> Unit) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier         = Modifier.fillMaxSize().background(theme.bg0),
        contentAlignment = Alignment.Center
    ) {
        PageAccentBloom()
        Column(
            modifier            = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LinkOff, null, tint = accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Bağlantı geçersiz",
                color = ObsidianText, fontSize = 22.sp, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Bu şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.\nLütfen yeni bir bağlantı isteyin.",
                color = ObsidianSub, fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            ObsidianButton(
                text     = "GİRİŞ SAYFASINA DÖN",
                onClick  = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Yeni şifre formu ──────────────────────────────────────────────────────────

@Composable
private fun NewPasswordContent(
    state    : ResetPasswordState,
    viewModel: ResetPasswordViewModel
) {
    val theme    = LocalAppTheme.current
    val accent   = MaterialTheme.colorScheme.primary
    val keyboard = LocalSoftwareKeyboardController.current

    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPass        by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    val confirmFocus    = remember { FocusRequester() }

    val alphaAnim = remember { Animatable(0f) }
    val yAnim     = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(420))
        yAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    if (state.isUpdating) {
        Box(
            modifier         = Modifier.fillMaxSize().background(theme.bg0),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = accent)
                Spacer(Modifier.height(16.dp))
                Text("Şifre güncelleniyor…", color = theme.text2, fontSize = 14.sp)
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
                color = ObsidianText, fontSize = 24.sp, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hesabın için güçlü bir şifre seç.",
                color = ObsidianSub, fontSize = 14.sp
            )

            Spacer(Modifier.height(36.dp))

            AuthLiquidField(
                value         = password,
                onValueChange = { password = it; viewModel.clearError() },
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
                onValueChange = { confirmPassword = it; viewModel.clearError() },
                label         = "ŞİFRE TEKRAR",
                icon          = Icons.Rounded.Lock,
                isPassword    = true,
                showPass      = showConfirmPass,
                onTogglePass  = { showConfirmPass = !showConfirmPass },
                imeAction     = ImeAction.Done,
                onImeAction   = {
                    keyboard?.hide()
                    viewModel.submit(password, confirmPassword)
                },
                modifier      = Modifier.focusRequester(confirmFocus)
            )

            Spacer(Modifier.height(32.dp))

            ObsidianButton(
                text     = "ŞİFREYİ GÜNCELLE",
                onClick  = {
                    keyboard?.hide()
                    viewModel.submit(password, confirmPassword)
                },
                modifier = Modifier.fillMaxWidth()
            )

            AuthFeedback(error = state.error, hint = null, onHintAction = {})
        }
    }
}
