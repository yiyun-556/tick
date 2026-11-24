package com.tick.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * One motion language for the whole app, modeled on iOS:
 * ease-out for entering, slightly faster for leaving, no bouncy page transitions.
 */
object TickMotion {
    const val FastMs = 160
    const val StandardMs = 220
    const val ExpandMs = 240

    val EnterEase: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val StandardEase: Easing = FastOutSlowInEasing

    fun <T> enter() = tween<T>(durationMillis = StandardMs, easing = EnterEase)
    fun <T> exit() = tween<T>(durationMillis = FastMs, easing = EnterEase)
    fun <T> expand() = tween<T>(durationMillis = ExpandMs, easing = StandardEase)
}
