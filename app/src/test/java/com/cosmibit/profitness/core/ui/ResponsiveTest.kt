package com.cosmibit.profitness.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveTest {
    @Test fun widthClassesAndRail() {
        assertEquals(WindowWidthClass.Compact, sinemaWindowClass(599f, 900f, 1f).widthClass)
        assertEquals(WindowWidthClass.Medium, sinemaWindowClass(839f, 900f, 1f).widthClass)
        assertEquals(WindowWidthClass.Expanded, sinemaWindowClass(840f, 800f, 1f).widthClass)
        assertFalse(sinemaWindowClass(839f, 900f, 1f).useNavRail)
        assertTrue(sinemaWindowClass(840f, 800f, 1f).useNavRail)
    }

    @Test fun tightAndFontClasses() {
        assertTrue(sinemaWindowClass(320f, 640f, 1f).isTight)       // 320 < 340
        assertTrue(sinemaWindowClass(411f, 891f, 1.5f).isTight)     // 411/1.5 = 274
        assertFalse(sinemaWindowClass(411f, 891f, 1f).isTight)
        assertEquals(FontScaleClass.Large, sinemaWindowClass(411f, 891f, 1.3f).fontClass)
        assertEquals(FontScaleClass.Huge, sinemaWindowClass(411f, 891f, 2f).fontClass)
        assertEquals(WindowHeightClass.Tiny, sinemaWindowClass(360f, 400f, 1f).heightClass)
    }

    @Test fun displayClampCapsAtOnePointOneFive() {
        assertEquals(104f, cappedDisplaySp(104f, 1.0f), 0.001f)
        assertEquals(104f, cappedDisplaySp(104f, 1.15f), 0.001f)
        // 2.0 ölçekte etkin boyut 104*1.15 olmalı: verilen sp * 2.0
        assertEquals(104f * 1.15f, cappedDisplaySp(104f, 2f) * 2f, 0.01f)
    }

    @Test fun navLabelsHideOnBigFontOrNarrowColumn() {
        assertTrue(navLabelsFit(64f, 1.0f, 10))          // "Programlar" 320dp'de 1.0
        assertFalse(navLabelsFit(64f, 1.15f, 10))
        assertFalse(navLabelsFit(112f, 1.3f, 10))        // 1.3+ hep gizli
    }

    @Test fun gutterTokens() {
        assertEquals(12f, SinemaLayout.gutter(320f).value, 0f)
        assertEquals(16f, SinemaLayout.gutter(411f).value, 0f)
        assertEquals(24f, SinemaLayout.gutter(700f).value, 0f)
        assertEquals(32f, SinemaLayout.gutter(1280f).value, 0f)
        assertEquals(396.8f, SinemaLayout.cover(640f, 0.62f, androidx.compose.ui.unit.Dp(360f), androidx.compose.ui.unit.Dp(560f)).value, 0.01f)
    }
}
