package com.avonix.profitness.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.avonix.profitness.core.theme.*


@Composable
fun AuthScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var email     by remember { mutableStateOf("hamzaoykurt@gmail.com") }
    var password  by remember { mutableStateOf("123456") }
    var isLogin   by remember { mutableStateOf(true) }
    var showPass  by remember { mutableStateOf(false) }

    // Navigate on successful auth — event-based (one-time, no double-fire risk)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    // Cinematic entrance animation
    val alphaAnim = remember { Animatable(0f) }
    val yOffset   = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(1000, easing = EaseOutExpo))
        yOffset.animateTo(0f, spring(Spring.DampingRatioLowBouncy))
    }

    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg0)
    ) {
        PageAccentBloom()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp, 0.dp)
                .graphicsLayer(alpha = alphaAnim.value, translationY = yOffset.value),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(100.dp))
            
            // Monogram Branding — solid amber, squared
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
                text = if (isLogin) "Tekrar\nhoş geldin." else "Gücü\nserbest bırak.",
                style = MaterialTheme.typography.displayLarge,
                color = ObsidianText,
                lineHeight = 52.sp
            )
            
            Spacer(Modifier.height(48.dp))
            
            // FORM
            AuthLiquidField(
                value = email,
                onValueChange = { email = it },
                label = "EMAIL",
                icon = Icons.Rounded.Email
            )
            
            Spacer(Modifier.height(20.dp))
            
            AuthLiquidField(
                value = password,
                onValueChange = { password = it },
                label = "ŞİFRE",
                icon = Icons.Rounded.Lock,
                isPassword = true,
                showPass = showPass,
                onTogglePass = { showPass = !showPass }
            )
            
            Spacer(Modifier.height(48.dp))
            
            // Primary Action — Solid Obsidian Button
            ObsidianButton(
                text = if (state.isLoading) "..." else if (isLogin) "GİRİŞ YAP" else "KAYIT OL",
                onClick = {
                    if (!state.isLoading) {
                        if (isLogin) viewModel.onLoginClick(email, password)
                        else viewModel.onRegisterClick(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    color = CriticalRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(4.dp, 0.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            
            // Switch Mode
            TextButton(
                onClick = { isLogin = !isLogin },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isLogin) "Hesabın yok mu? Kaydol" else "Zaten üye misin? Giriş yap",
                    color = ObsidianSub,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AuthLiquidField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPass: Boolean = false,
    onTogglePass: () -> Unit = {}
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
                    // Top rim light — cached per size change, not per draw
                    val rimBrush = Brush.horizontalGradient(
                        listOf(Color.Transparent, accent.copy(0.3f), Color.Transparent)
                    )
                    val rimHeight = 1.dp.toPx()
                    onDrawBehind {
                        drawRect(brush = rimBrush, size = Size(size.width, rimHeight))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(16.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ObsidianMuted,
                    modifier = Modifier.size(20.dp)
                )
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    visualTransformation = if (isPassword && !showPass) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = ObsidianText,
                        unfocusedTextColor = ObsidianText
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

/**
 * ObsidianButton — Bespoke, solid, high-contrast action button.
 * Amber fill, black text — no Material primitives, no gradient gradients.
 */
@Composable
fun ObsidianButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified
) {
    val resolvedAccent  = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
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
            text = text,
            color = resolvedOnAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}


