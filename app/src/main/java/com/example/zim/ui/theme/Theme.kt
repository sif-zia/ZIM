package com.example.zim.ui.theme

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
    surface = DarkGreenishGray,
    onPrimary = DarkGreenishGray,
    onSecondary = LightGray,
    onTertiary = LightGray,
    onBackground = LightGray,
    onSurface = DarkGreenishGray,
)

private val LightColorScheme = lightColorScheme(
    primary = LightGray,
    secondary = DarkGreenishGray,
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
    content: @Composable () -> Unit
) {
//    val colors = if(darkTheme) DarkColorScheme else LightColorScheme;
    val colors = DarkColorScheme;

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}