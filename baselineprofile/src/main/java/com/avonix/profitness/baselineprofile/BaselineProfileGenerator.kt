package com.avonix.profitness.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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

        val tabY = bottomNavY(device.displayHeight)
        bottomTabXPositions(device.displayWidth).forEach { x ->
            device.click(x, tabY)
            device.waitForIdle()
        }

        bottomTabXPositions(device.displayWidth).asReversed().forEach { x ->
            device.click(x, tabY)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.avonix.profitness"
    }

    private fun bottomNavY(displayHeight: Int): Int =
        (displayHeight * 0.93f).toInt()

    private fun bottomTabXPositions(displayWidth: Int): List<Int> =
        listOf(0.28f, 0.39f, 0.50f, 0.61f, 0.72f).map { (displayWidth * it).toInt() }
}
