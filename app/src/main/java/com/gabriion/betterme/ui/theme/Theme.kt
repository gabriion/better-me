package com.gabriion.betterme.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = TealDeep,
    onPrimary = Cream,
    primaryContainer = SkySoft,
    onPrimaryContainer = Ink,
    secondary = Sage,
    onSecondary = Ink,
    secondaryContainer = SageDeep,
    onSecondaryContainer = Cream,
    tertiary = TealMid,
    background = Mist,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = SkySoft,
    onPrimary = Ink,
    primaryContainer = TealDeep,
    onPrimaryContainer = Cream,
    secondary = Sage,
    onSecondary = Ink,
    secondaryContainer = SageDeep,
    onSecondaryContainer = Cream,
    tertiary = TealMid,
    background = Ink,
    onBackground = Cream,
    surface = TealDeep,
    onSurface = Cream
)

@Composable
fun BetterMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BetterMeTypography,
        content = content
    )
}
