package com.example.zim.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightGray,
    secondary = DarkGreenishGray,
    tertiary = Purple,

    background = DarkGreenishGray,
    surface = LightGray,
    onPrimary = DarkGreenishGray,
    onSecondary = LightGray,
    onTertiary = LightGray,
    onBackground = LightGray,
    onSurface = DarkGreenishGray,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreenishGray,
    secondary = LightGray,
    tertiary = Purple,

    /* Other default colors to override */
    background = LightGray,
    surface = DarkGreenishGray,
    onPrimary = LightGray,
    onSecondary = DarkGreenishGray,
    onTertiary = LightGray,
    onBackground = DarkGreenishGray,
    onSurface = LightGray,
)

@Composable
fun ZIMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}