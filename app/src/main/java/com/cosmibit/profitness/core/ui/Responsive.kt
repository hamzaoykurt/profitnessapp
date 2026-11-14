package com.cosmibit.profitness.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded
}

enum class WindowHeightClass { Tiny, Short, Tall }
enum class FontScaleClass { Normal, Large, Huge }

/** Pencere + yazı ölçeği sınıfı (tek kaynak). Hesap saf fonksiyonlarda ([sinemaWindowClass]); birim testli. */
@Immutable
data class SinemaWindowClass(
    val widthDp: Float,
    val heightDp: Float,
    val fontScale: Float,
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val fontClass: FontScaleClass
) {
    /** Yazı ölçeği ekranı "küçültür": etkin genişlik <340 veya etkin yükseklik <620. */
    val isTight: Boolean get() = widthDp / fontScale < 340f || heightDp / fontScale < 620f
    /** Hamza kararı: yalnız ≥840dp'de yan menü (NavigationRail); daha küçükte alt menü. */
    val useNavRail: Boolean get() = useNavRailFor(widthDp)
}

fun useNavRailFor(widthDp: Float): Boolean = widthDp >= 840f

fun sinemaWindowClass(widthDp: Float, heightDp: Float, fontScale: Float): SinemaWindowClass {
    val fs = fontScale.coerceAtLeast(0.5f)
    return SinemaWindowClass(
        widthDp = widthDp, heightDp = heightDp, fontScale = fs,
        widthClass = when { widthDp < 600f -> WindowWidthClass.Compact; widthDp < 840f -> WindowWidthClass.Medium; else -> WindowWidthClass.Expanded },
        heightClass = when { heightDp < 560f -> WindowHeightClass.Tiny; heightDp < 700f -> WindowHeightClass.Short; else -> WindowHeightClass.Tall },
        fontClass = when { fs >= 1.5f -> FontScaleClass.Huge; fs >= 1.15f -> FontScaleClass.Large; else -> FontScaleClass.Normal }
    )
}

/** ≥40sp "dev rakam" yazı ölçeğiyle en fazla bu kadar büyür (gövde metni tam ölçeklenir). */
const val DisplayFontScaleCap = 1.15f

/**
 * Dev rakam için verilecek sp: `actualScale` = sistemin bu boyuta uyguladığı gerçek ölçek (API 34+ doğrusal değil),
 * etkin ölçek `min(actualScale, cap)` olur. Dönen sp, Compose'un ölçeğiyle çarpılınca `base * min(actual, cap)` dp verir.
 */
fun cappedDisplaySp(baseSp: Float, actualScale: Float, cap: Float = DisplayFontScaleCap): Float =
    if (actualScale <= cap || actualScale <= 0f) baseSp else baseSp * cap / actualScale

/** Alt menü/rail etiketi sütuna sığar mı (Sora ≈0.62em/harf varsayımı, ölçülmedi). fontScale ≥1.3'te hep gizli. */
fun navLabelsFit(columnWidthDp: Float, fontScale: Float, maxChars: Int, labelSp: Float = 10f): Boolean =
    fontScale < 1.3f && maxChars * labelSp * 0.62f * fontScale <= columnWidthDp

/** Sinema yerleşim token'ları (renk/ışığa dokunmaz). */
object SinemaLayout {
    val ContentMaxList = 600.dp      // liste sekmeleri (Programlar, Profil)
    val ContentMaxReading = 680.dp   // okuma/detay
    val ContentMaxForm = 520.dp      // form
    val NavMax = 560.dp              // alt menü kapsülü (Medium'da ortalı)
    val RailWidth = 96.dp            // rail + boşluk (içerik bu kadar sağa alınır)
    const val BackdropScaleMin = 0.9f
    const val BackdropScaleMax = 1.4f

    /** Kenar boşluğu: <360 → 12, <600 → 16, <840 → 24, ≥840 → 32. */
    fun gutter(widthDp: Float): Dp = when { widthDp < 360f -> 12.dp; widthDp < 600f -> 16.dp; widthDp < 840f -> 24.dp; else -> 32.dp }

    /** Kapak yüksekliği = ekran yüksekliği × oran, [min, max] ile sınırlı (kapaklar `height()` sabiti yerine bunu kullanır). */
    fun cover(screenHeightDp: Float, fraction: Float, min: Dp, max: Dp): Dp =
        (screenHeightDp * fraction).dp.coerceIn(min, max)
}

val LocalWindowClass = staticCompositionLocalOf { sinemaWindowClass(360f, 800f, 1f) }

/** MainActivity'de bir kez: LocalConfiguration (çoklu pencerede pencere boyutu) + yazı ölçeği. */
@Composable
fun ProvideWindowClass(content: @Composable () -> Unit) {
    val c = LocalConfiguration.current
    val fs = LocalDensity.current.fontScale
    val wc = remember(c.screenWidthDp, c.screenHeightDp, fs) {
        sinemaWindowClass(c.screenWidthDp.toFloat(), c.screenHeightDp.toFloat(), fs)
    }
    CompositionLocalProvider(LocalWindowClass provides wc, content = content)
}

/**
 * Dev rakam metni: (a) başlangıç boyu yazı ölçeğiyle en fazla ×[DisplayFontScaleCap] büyür, `isTight`'ta %12 küçük başlar,
 * (b) genişliğe sığmazsa %8 adımlarla `floorSp`'ye kadar küçülür, (c) son çare tek satır Ellipsis. `baseSp` Compose sp'si (ölçek öncesi).
 */
@Composable
fun SinemaFitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    baseSp: Float = style.fontSize.value,
    floorSp: Float = baseSp * 0.45f,
    textAlign: TextAlign? = null
) {
    val density = LocalDensity.current
    val tight = LocalWindowClass.current.isTight
    val actual = with(density) { baseSp.sp.toDp().value } / baseSp
    val start = cappedDisplaySp(baseSp, actual) * (if (tight) 0.88f else 1f)
    val floor = cappedDisplaySp(floorSp, with(density) { floorSp.sp.toDp().value } / floorSp)
    var size by remember(text, start) { mutableFloatStateOf(start) }
    Text(
        text, style = style.copy(fontSize = size.sp), color = color, modifier = modifier, textAlign = textAlign,
        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.didOverflowWidth && size > floor) size = (size * 0.92f).coerceAtLeast(floor) }
    )
}

@Immutable
data class ResponsiveLayoutInfo(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val fontScale: Float,
    val widthClass: WindowWidthClass,
    val isSmallPhone: Boolean,
    val isShortScreen: Boolean,
    val isTablet: Boolean,
    val isLargeFont: Boolean,
    val isVeryLargeFont: Boolean,
    val useNavigationRail: Boolean,
    val contentMaxWidth: Dp,
    val formMaxWidth: Dp,
    val horizontalPadding: Dp,
    val navRailWidth: Dp,
    val bottomNavHeight: Dp,
    val compactChipMinHeight: Dp,
    val controlMinHeight: Dp,
    val cardHorizontalPadding: Dp,
    val denseVerticalSpacing: Dp
)

@Composable
fun rememberResponsiveLayoutInfo(): ResponsiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val fontScale = LocalDensity.current.fontScale
    val widthClass = when {
        screenWidth < 600.dp -> WindowWidthClass.Compact
        screenWidth < 840.dp -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
    val isSmallPhone = screenWidth < 380.dp
    val isShortScreen = screenHeight < 700.dp
    val isLargeFont = fontScale >= 1.18f
    val isVeryLargeFont = fontScale >= 1.38f

    return ResponsiveLayoutInfo(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        fontScale = fontScale,
        widthClass = widthClass,
        isSmallPhone = isSmallPhone,
        isShortScreen = isShortScreen,
        isTablet = screenWidth >= 600.dp,
        isLargeFont = isLargeFont,
        isVeryLargeFont = isVeryLargeFont,
        useNavigationRail = useNavRailFor(screenWidth.value),
        contentMaxWidth = SinemaLayout.ContentMaxList,
        formMaxWidth = when (widthClass) {
            WindowWidthClass.Compact -> 520.dp
            WindowWidthClass.Medium -> 560.dp
            WindowWidthClass.Expanded -> 620.dp
        },
        horizontalPadding = when {
            screenWidth < 360.dp -> 14.dp
            widthClass == WindowWidthClass.Compact -> 20.dp
            widthClass == WindowWidthClass.Medium -> 32.dp
            else -> 40.dp
        },
        navRailWidth = if (useNavRailFor(screenWidth.value)) SinemaLayout.RailWidth else 0.dp,
        bottomNavHeight = when {
            isVeryLargeFont -> 108.dp
            isLargeFont -> 100.dp
            else -> 92.dp
        },
        compactChipMinHeight = if (isLargeFont) 48.dp else 40.dp,
        controlMinHeight = if (isLargeFont) 58.dp else 52.dp,
        cardHorizontalPadding = if (isSmallPhone) 16.dp else 20.dp,
        denseVerticalSpacing = if (isShortScreen) 8.dp else 12.dp
    )
}
