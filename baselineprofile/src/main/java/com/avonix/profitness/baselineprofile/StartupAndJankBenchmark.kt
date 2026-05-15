package com.avonix.profitness.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupAndJankBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }

    @Test
    fun workoutScrollAndTabTransitions() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }
    ) {
        clickTab("FORGE", 0)
        repeat(3) {
            device.swipe(centerX(), lowerY(), centerX(), upperY(), 12)
            device.swipe(centerX(), upperY(), centerX(), lowerY(), 12)
        }
        clickTab("PLAN", 1)
        clickTab("ORACLE", 2)
        clickTab("KEŞFET", 3)
        clickTab("USER", 4)
        clickTab("FORGE", 0)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.clickTab(label: String, index: Int) {
        val node = device.findObject(By.desc(label))
        if (node != null) {
            node.click()
        } else {
            device.click(bottomTabXPositions()[index], bottomNavY())
        }
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.centerX(): Int = device.displayWidth / 2

    private fun MacrobenchmarkScope.upperY(): Int = (device.displayHeight * 0.30f).toInt()

    private fun MacrobenchmarkScope.lowerY(): Int = (device.displayHeight * 0.78f).toInt()

    private fun MacrobenchmarkScope.bottomNavY(): Int = (device.displayHeight * 0.93f).toInt()

    private fun MacrobenchmarkScope.bottomTabXPositions(): List<Int> =
        listOf(0.28f, 0.39f, 0.50f, 0.61f, 0.72f).map { (device.displayWidth * it).toInt() }

    private companion object {
        const val PACKAGE_NAME = "com.avonix.profitness"
    }
}
