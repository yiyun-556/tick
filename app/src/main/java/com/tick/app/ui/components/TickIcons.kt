package com.tick.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object TickIcons {
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "TickHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3.2f)
                lineTo(21f, 10.6f)
                lineTo(19.3f, 12.6f)
                lineTo(12f, 6.6f)
                lineTo(4.7f, 12.6f)
                lineTo(3f, 10.6f)
                close()
                moveTo(5.4f, 12.1f)
                lineTo(12f, 6.7f)
                lineTo(18.6f, 12.1f)
                lineTo(18.6f, 20.2f)
                lineTo(13.8f, 20.2f)
                lineTo(13.8f, 14.6f)
                lineTo(10.2f, 14.6f)
                lineTo(10.2f, 20.2f)
                lineTo(5.4f, 20.2f)
                close()
            }
        }.build()
    }
}
