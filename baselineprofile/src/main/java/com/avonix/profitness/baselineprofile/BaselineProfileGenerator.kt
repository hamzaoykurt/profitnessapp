package com.avonix.profitness.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun primaryInteractions() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = false
    ) {
        startActivityAndWait()
        device.waitForIdle()

        clickIfVisible("FORGE")
        device.waitForIdle()
        clickAt(0.50f, 0.52f)
        device.waitForIdle()

        val tabY = bottomNavY(device.displayHeight)
        TAB_LABELS.forEachIndexed { index, label ->
            if (!clickIfVisible(label)) {
                device.click(bottomTabXPositions(device.displayWidth)[index], tabY)
            }
            device.waitForIdle()
            clickAt(0.50f, 0.48f)
            device.waitForIdle()
        }

        TAB_LABELS.asReversed().forEachIndexed { reversedIndex, label ->
            val index = TAB_LABELS.lastIndex - reversedIndex
            if (!clickIfVisible(label)) {
                device.click(bottomTabXPositions(device.displayWidth)[index], tabY)
            }
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.avonix.profitness"
        val TAB_LABELS = listOf("FORGE", "PLAN", "ORACLE", "KEŞFET", "USER")
    }

    private fun bottomNavY(displayHeight: Int): Int =
        (displayHeight * 0.93f).toInt()

    private fun bottomTabXPositions(displayWidth: Int): List<Int> =
        listOf(0.28f, 0.39f, 0.50f, 0.61f, 0.72f).map { (displayWidth * it).toInt() }

    private fun MacrobenchmarkScope.clickIfVisible(contentDescription: String): Boolean {
        val node = device.findObject(By.desc(contentDescription)) ?: return false
        node.click()
        return true
    }

    private fun MacrobenchmarkScope.clickAt(widthFraction: Float, heightFraction: Float) {
        device.click(
            (device.displayWidth * widthFraction).toInt(),
            (device.displayHeight * heightFraction).toInt()
        )
    }
}
