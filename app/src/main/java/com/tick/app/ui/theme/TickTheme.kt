package com.tick.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tick.app.data.AppSettings
import com.tick.app.data.DarkMode
import com.tick.app.data.PaletteStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun TickTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val controller = remember(settings.palette, settings.darkMode, systemDark) {
        when (settings.palette) {
            PaletteStyle.MONET -> ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                lightColors = lightColorScheme(),
                darkColors = darkColorScheme(),
                isDark = when (settings.darkMode) {
                    DarkMode.SYSTEM -> null
                    DarkMode.LIGHT -> false
                    DarkMode.DARK -> true
                },
            )

            PaletteStyle.CLASSIC -> ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                lightColors = hyperOsLight(),
                darkColors = hyperOsDark(),
                isDark = when (settings.darkMode) {
                    DarkMode.SYSTEM -> null
                    DarkMode.LIGHT -> false
                    DarkMode.DARK -> true
                },
            )
        }
    }

    ApplySystemBars(dark = dark)
    MiuixTheme(controller = controller, content = content)
}

private fun hyperOsLight() = lightColorScheme(
    primary = Color(0xFF3482FF),
    primaryVariant = Color(0xFF3482FF),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF1D5FCC),
    tertiaryContainer = Color(0xFFEAF2FF),
    onTertiaryContainer = Color(0xFF2A6FD6),
)

private fun hyperOsDark() = darkColorScheme(
    primary = Color(0xFF4A95FF),
    primaryVariant = Color(0xFF277AF7),
    primaryContainer = Color(0xFF14345F),
    onPrimaryContainer = Color(0xFFD6E6FF),
    tertiaryContainer = Color(0xFF1B304F),
    onTertiaryContainer = Color(0xFF8FC0FF),
)

@Composable
private fun ApplySystemBars(dark: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    SideEffect {
        val window = activity.window
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
