package com.cosmibit.profitness.core.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * HAREKET TOKEN'LARI — TEK KAYNAK (.ekip/plan-animasyon.md §2).
 * Süre (ms): Instant 90 (çıkış solması) · Quick 140 (basılma, renk) · Standard 220 (giriş, kapsül) ·
 * Emphasis 360 (sayfa/overlay, vurgu) · Slow 600 (sayaç, halka/çubuk dolumu).
 * Kural: giriş = Standard + EaseEnter, çıkış = Quick + EaseExit, ekranda kalan hareket = EaseStandard.
 * Karakter: sakin, sıçramasız; taşma yalnız [pop].
 */
object SinemaMotion {
    const val Instant = 90
    const val Quick = 140
    const val Standard = 220
    const val Emphasis = 360
    const val Slow = 600

    /** Ekranda kalan hareket (konum/boyut/renk değişimi). */
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Giriş ve vurgu: hızlı başlar, yumuşak oturur. */
    val EaseEnter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Çıkış: yumuşak başlar, hızlı gider. */
    val EaseExit = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** Basılma ölçeği (0.96 ↔ 1). */
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    /** Kayan taş, overlay yerleşmesi: taşmasız. */
    fun <T> settle() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    /** Tik / rozet: tek küçük taşma. */
    fun <T> pop() = spring<T>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

    /** Liste girişi kademesi: öğe başı gecikme ve en çok kademelenen öğe (toplam ≤ 240 ms). */
    const val StaggerMs = 40
    const val StaggerMax = 6
}

/** Hafif dokunuş (seçim, segment, BAŞLA) ve güçlü onay (set tamamlandı) titreşimleri — tek yerden. */
object SinemaHaptics {
    val Light = HapticFeedbackType.TextHandleMove
    val Confirm = HapticFeedbackType.LongPress
}

/**
 * Basılma ölçeği (yaylı, varsayılan 0.96). Zincirde yüzeyden ÖNCE konur; aynı [source] `clickable`'a verilir.
 * Ölçek yalnız graphicsLayer'da okunur (yeniden kompozisyon/yerleşim yok).
 */
@Composable
fun Modifier.sinemaPressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed) pressedScale else 1f, SinemaMotion.press(), label = "sinema_press")
    return this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/**
 * Sayan rakam: hedef değişince eski değerden yenisine [SinemaMotion.Slow] sürede sayar.
 * `fromZero` = ilk görünüşte 0'dan say (ekran başına bir kez; kaydırıp dönünce tekrar oynamaz).
 * Hareket azaltmada doğrudan hedefi verir. Küçük bir composable içinde çağır (sayarken o kapsam yeniden çizilir).
 */
@Composable
fun rememberSinemaCount(target: Int, fromZero: Boolean = false, durationMs: Int = SinemaMotion.Slow): Int {
    if (!rememberSinemaMotionOn()) return target
    var played by rememberSaveable { mutableStateOf(!fromZero) }
    val anim = remember { Animatable(if (played) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), tween(durationMs, easing = SinemaMotion.EaseStandard))
        played = true
    }
    return anim.value.roundToInt()
}

/**
 * Halka/çubuk dolumu 0..1: ilk görünüşte 0'dan (bir kez), sonra değer değiştikçe eski değerden.
 * Dönen State yalnız çizim aşamasında okunmalı (`drawBehind`/`Canvas`/`graphicsLayer` içinde `.value`).
 */
@Composable
fun rememberSinemaFill(target: Float, durationMs: Int = SinemaMotion.Slow): State<Float> {
    val t = target.coerceIn(0f, 1f)
    val motion = rememberSinemaMotionOn()
    var played by rememberSaveable { mutableStateOf(false) }
    val anim = remember { Animatable(if (played || !motion) t else 0f) }
    LaunchedEffect(t, motion) {
        if (motion) anim.animateTo(t, tween(durationMs, easing = SinemaMotion.EaseStandard)) else anim.snapTo(t)
        played = true
    }
    return anim.asState()
}

/**
 * Liste girişi: ilk [SinemaMotion.StaggerMax] öğe 12dp aşağıdan + solarak, öğe başı [SinemaMotion.StaggerMs] gecikmeyle gelir.
 * Yalnız ilk görünüşte oynar (kaydırıp geri dönünce tekrar etmez); hareket azaltmada hiç oynamaz.
 */
@Composable
fun Modifier.sinemaListEnter(index: Int): Modifier {
    if (index >= SinemaMotion.StaggerMax || !rememberSinemaMotionOn()) return this
    var played by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.toLong() * SinemaMotion.StaggerMs)
            p.animateTo(1f, tween(SinemaMotion.Standard, easing = SinemaMotion.EaseEnter))
            played = true
        }
    }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 12.dp.toPx()
    }
}

/** Sayan rakamı küçük bir kapsamda tutar: sayarken yalnız [content] yeniden çizilir. Kullanım: `SinemaCount(n) { Text("$it") }`. */
@Composable
fun SinemaCount(value: Int, fromZero: Boolean = true, content: @Composable (Int) -> Unit) {
    content(rememberSinemaCount(value, fromZero))
}
