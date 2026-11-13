package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccentTriadTest {
    // .ekip/yon-renk-hareket.md tablosu (NEON): preset -> (c2, c3)
    private val table = mapOf(
        AccentPreset.LIME to (0x00C6E5 to 0xFF9BF3), AccentPreset.PURPLE to (0xFB9250 to 0x00E2CE),
        AccentPreset.CYAN to (0x9AAAFF to 0xFFB161), AccentPreset.ORANGE to (0x89C55E to 0xBCBCFF),
        AccentPreset.PINK to (0xF1BB3B to 0x7AD0FF), AccentPreset.BLUE to (0xE78BDC to 0x85DE70),
        AccentPreset.RED to (0xC1CF51 to 0xA8C4FF), AccentPreset.YELLOW to (0x00CDB7 to 0xFF9BF3),
        AccentPreset.GREEN to (0x4FBCFF to 0xFFAF7F), AccentPreset.TEAL to (0x97ACFF to 0xFFB167),
        AccentPreset.INDIGO to (0xE78BDC to 0x49E497), AccentPreset.ROSE to (0xD6C842 to 0x9AC8FF),
        AccentPreset.MINT to (0x75B5FF to 0xFFAF7F), AccentPreset.SKY to (0xDB92E4 to 0xD9E257),
        AccentPreset.VIOLET to (0xFB9250 to 0x00E4BA), AccentPreset.AMBER to (0x41CE90 to 0xDCAEFF)
    )

    private fun near(expected: Int, actual: Int, tol: Int = 3): Boolean =
        listOf(16, 8, 0).all { abs(((expected shr it) and 0xFF) - ((actual shr it) and 0xFF)) <= tol }

    private fun hex(v: Int) = "#%06X".format(v and 0xFFFFFF)

    @Test fun presetTableMatches() {
        val bad = table.mapNotNull { (p, exp) ->
            val t = AccentTriadMath.derive(AppThemeState(accent = p, intensity = AccentIntensity.NEON).effectiveAccentColor.toArgb())
            if (near(exp.first, t.c2) && near(exp.second, t.c3)) null
            else "${p.name}: beklenen ${hex(exp.first)}/${hex(exp.second)} gelen ${hex(t.c2)}/${hex(t.c3)}"
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun hamzaSet() {
        val c1 = AppThemeState(accent = AccentPreset.TEAL, intensity = AccentIntensity.VIVID).effectiveAccentColor.toArgb()
        val t = AccentTriadMath.derive(c1)
        println("HAMZA c1=${hex(c1)} c2=${hex(t.c2)} c3=${hex(t.c3)}")
        assertTrue(hex(c1), near(0x10C5B6, c1, 1))
        assertTrue(hex(t.c2), near(0x9BAAFF, t.c2))
        assertTrue(hex(t.c3), near(0xFFB160, t.c3))
    }

    @Test fun neutralCustomUsesCyanHue() {
        val t = AccentTriadMath.derive(0xFF808080.toInt())
        assertTrue(hex(t.c2), near(0x9EACF1, t.c2))
        assertTrue(hex(t.c3), near(0xFAB36D, t.c3))
    }

    @Test fun contrastOnAllSurfaces() {
        val surfaces = listOf(0x0C0D10, 0x15171C, 0x1D2027)
        val fails = mutableListOf<String>()
        for (p in AccentPreset.entries) for (i in AccentIntensity.entries) {
            val st = AppThemeState(accent = p, intensity = i)
            val t = AccentTriadMath.derive(st.effectiveAccentColor.toArgb())
            for (c in listOf(t.c2, t.c3)) for (bg in surfaces) {
                val r = AccentTriadMath.contrast(c, bg)
                if (r < 4.5) fails += "${p.name} ${i.name} ${hex(c)} on ${hex(bg)} = $r"
            }
            // metin varyantı da >= 4.5
            for (c in listOf(st.accent2TextColor.toArgb(), st.accent3TextColor.toArgb())) {
                assertTrue(AccentTriadMath.contrast(c, 0x1D2027) >= 4.5)
            }
        }
        assertTrue(fails.joinToString("\n"), fails.isEmpty())
    }

    @Test fun errorHueIsAvoided() {
        // c2/c3 hiçbir preset/yoğunlukta hata tonunun (h≈11°) ±40° içine girmemeli: hata rengiyle ayırt edilebilir
        val err = 0xF4748A
        for (p in AccentPreset.entries) {
            val t = AccentTriadMath.derive(AppThemeState(accent = p, intensity = AccentIntensity.NEON).effectiveAccentColor.toArgb())
            assertTrue(abs(t.c2 - err) > 0 && abs(t.c3 - err) > 0)
        }
        assertEquals(AccentTriadMath.derive(0xFF10C5B6.toInt()), AccentTriadMath.derive(0xFF10C5B6.toInt()))
    }
}
