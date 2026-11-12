package com.cosmibit.profitness.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Sinema çiziminin (sinema.html) SVG simgeleri, yol verisi birebir. 24x24, çizgi 2 (yuvarlak uç/köşe) ya da dolgu.
 * Renk `Icon(tint = ...)` ile verilir; burada siyah.
 */
object SinemaIcons {
    private fun build(name: String, filled: Boolean = false, width: Float = 2f, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                val nodes = PathParser().parsePathString(d).toNodes()
                if (filled) addPath(nodes, fill = SolidColor(Color.Black))
                else addPath(
                    nodes, stroke = SolidColor(Color.Black), strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    val Dumbbell by lazy { build("sinema_dumb", false, 2f, "M6.5 6.5v11M3.5 9v6M17.5 6.5v11M20.5 9v6M6.5 12h11") }
    val List by lazy { build("sinema_list", false, 2f, "M5 6.5h14M5 12h14M5 17.5h9") }
    val Spark by lazy {
        build("sinema_spark", false, 2f, "M12 3.5l1.9 5.1 5.1 1.9-5.1 1.9-1.9 5.1-1.9-5.1L5 10.5l5.1-1.9z", "M19 3v4M17 5h4")
    }
    val Compass by lazy { build("sinema_compass", false, 2f, "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M15.5 8.5l-2 5-5 2 2-5z") }
    val User by lazy { build("sinema_user", false, 2f, "M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M4.5 20c0-4 3.4-6 7.5-6s7.5 2 7.5 6") }
    val Chevron by lazy { build("sinema_chev", false, 2.4f, "M6 9.5l6 6 6-6") }
    val Check by lazy { build("sinema_check", false, 3f, "M5 12.5l4.6 4.6L19 7.5") }
    val Flame by lazy {
        build("sinema_flame", true, 2f, "M12.5 2c.6 3.6 5 5.6 5 11a5.5 5.5 0 0 1-11 0c0-2 .9-3.3 2-4.4.1 2 1 3 2.2 3.3C10 8.5 10.4 4.8 12.5 2z")
    }
    val Bolt by lazy { build("sinema_bolt", true, 2f, "M13.5 2L5 13.5h6L10 22l9-12h-6.2z") }
    val Play by lazy { build("sinema_play", true, 2f, "M7 4.5v15l12.5-7.5z") }
    val Send by lazy { build("sinema_send", false, 2.4f, "M12 19V5M6 11l6-6 6 6") }
    val Heart by lazy { build("sinema_heart", false, 2f, "M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z") }
    val Bookmark by lazy { build("sinema_mark", false, 2f, "M7 4.5h10v15l-5-3.5-5 3.5z") }
    val Download by lazy { build("sinema_dl", false, 2f, "M12 4v11M7.5 11l4.5 4.5 4.5-4.5M5 19.5h14") }
    val Plus by lazy { build("sinema_plus", false, 2.4f, "M12 5v14M5 12h14") }
    val More by lazy { build("sinema_more", true, 2f, "M12 3.6a1.9 1.9 0 1 0 0 3.8a1.9 1.9 0 1 0 0-3.8z", "M12 10.1a1.9 1.9 0 1 0 0 3.8a1.9 1.9 0 1 0 0-3.8z", "M12 16.6a1.9 1.9 0 1 0 0 3.8a1.9 1.9 0 1 0 0-3.8z") }
}
