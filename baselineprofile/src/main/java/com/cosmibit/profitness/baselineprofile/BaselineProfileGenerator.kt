package com.cosmibit.profitness.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline profile yolculukları. Yalnız gezinme ve kaydırma yapar; hiçbir buton
 * (Başla, Aktif et, Ekle, Hesapla, Çıkış Yap, Yeni challenge, mesaj gönder) tıklanmaz,
 * böylece kullanıcı verisi değişmez. Uygulama giriş yapılmış hâlde olmalı; değilse
 * yolculuk Auth ekranında kalır ve yalnız açılış profili anlamlı olur.
 *
 * Çalıştırma: `set ANDROID_SERIAL=<serial>` + `gradlew :app:generateBaselineProfile`
 * (`-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` ile uygulama ve oturum silinmez).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = true
    ) {
        startActivityAndWait()
        waitForMainScreen()
    }

    @Test
    fun primaryInteractions() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = false
    ) {
        startActivityAndWait()
        if (!waitForMainScreen()) return@collect // Auth ekranı: gezinme yok.

        // Alt menü ile 5 sekme ve geri.
        TAB_DESCS.forEach { openTab(it) }
        TAB_DESCS.asReversed().forEach { openTab(it) }

        // Yatay swipe ile sekmeler arası (Antrenman -> Profil -> Antrenman).
        openTab(TAB_WORKOUT)
        repeat(4) { swipeHorizontal(toLeft = true) }
        repeat(4) { swipeHorizontal(toLeft = false) }

        // Antrenman: kaydır.
        openTab(TAB_WORKOUT)
        scrollVertical(times = 3)

        // Programlar: kaydır.
        openTab(TAB_PROGRAMS)
        scrollVertical(times = 4)

        // Keşfet: üst sekmeler (yalnız sekme başlıkları; FAB'a dokunulmaz).
        openTab(TAB_DISCOVER)
        listOf("Programlar", "Topluluk", "Challenge", "Programlar").forEach { label ->
            device.findObject(By.text(label))?.click()
            device.waitForIdle()
            scrollVertical(times = 1)
        }

        // Profil: en üste dön, kaydır, alt ekranları aç-kapat.
        openTab(TAB_PROFILE)
        scrollVertical(times = 4, down = false)
        scrollVertical(times = 2)
        scrollVertical(times = 2, down = false)

        openAndBack(By.text("BAŞARIMLAR"), scrollInside = true)

        if (clickText("Tümünü Gör")) {
            device.wait(Until.hasObject(By.text("Veri analizi")), WAIT_MS)
            scrollVertical(times = 3)
            if (clickText("Hareket gelişimi")) {
                device.wait(Until.hasObject(By.text("Hareket performansı")), WAIT_MS)
                scrollVertical(times = 2)
                device.pressBack()
                device.waitForIdle()
            }
            device.pressBack()
            device.waitForIdle()
        }

        openTab(TAB_WORKOUT)
    }

    private companion object {
        const val PACKAGE_NAME = "com.cosmibit.profitness"
        const val WAIT_MS = 5_000L
        const val TAB_WORKOUT = "Antrenman"
        const val TAB_PROGRAMS = "Programlar"
        const val TAB_DISCOVER = "Keşfet"
        const val TAB_PROFILE = "Profil"
        val TAB_DESCS = listOf(TAB_WORKOUT, TAB_PROGRAMS, "AI Koç", TAB_DISCOVER, TAB_PROFILE)
    }

    /** Alt menü görünene kadar bekler; görünmezse (Auth ekranı) false. */
    private fun MacrobenchmarkScope.waitForMainScreen(): Boolean {
        val found = device.wait(Until.hasObject(By.desc(TAB_PROFILE)), 15_000L) ?: false
        device.waitForIdle()
        return found
    }

    private fun MacrobenchmarkScope.openTab(desc: String) {
        device.findObject(By.desc(desc))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.clickText(text: String): Boolean {
        val node = device.findObject(By.text(text)) ?: return false
        node.click()
        device.waitForIdle()
        return true
    }

    private fun MacrobenchmarkScope.openAndBack(
        selector: androidx.test.uiautomator.BySelector,
        scrollInside: Boolean
    ) {
        val node = device.findObject(selector) ?: return
        node.click()
        device.waitForIdle()
        if (scrollInside) scrollVertical(times = 2)
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipeHorizontal(toLeft: Boolean) {
        val w = device.displayWidth
        val y = (device.displayHeight * 0.5f).toInt()
        val (from, to) = if (toLeft) (w * 0.83f).toInt() to (w * 0.14f).toInt()
        else (w * 0.14f).toInt() to (w * 0.83f).toInt()
        device.swipe(from, y, to, y, 12)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollVertical(times: Int, down: Boolean = true) {
        val x = device.displayWidth / 2
        val top = (device.displayHeight * 0.30f).toInt()
        val bottom = (device.displayHeight * 0.75f).toInt()
        repeat(times) {
            if (down) device.swipe(x, bottom, x, top, 15) else device.swipe(x, top, x, bottom, 15)
            device.waitForIdle()
        }
    }
}
