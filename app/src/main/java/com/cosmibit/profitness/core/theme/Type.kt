package com.cosmibit.profitness.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.R

/** Sora (SIL OFL 1.1, docs/legal/OFL-Sora.txt): uygulamaya gömülü değişken font; ağırlıklar Light–ExtraBold. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun sora(w: FontWeight) = Font(
    R.font.sora_var, weight = w,
    variationSettings = FontVariation.Settings(FontVariation.weight(w.weight))
)

val AppFontFamily = FontFamily(
    sora(FontWeight.Light), sora(FontWeight.Normal), sora(FontWeight.Medium),
    sora(FontWeight.SemiBold), sora(FontWeight.Bold), sora(FontWeight.ExtraBold),
    sora(FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 23.sp, letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 19.sp, letterSpacing = 0.05.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.25.sp
    )
)
