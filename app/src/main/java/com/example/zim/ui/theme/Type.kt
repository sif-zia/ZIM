package com.example.zim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.zim.R

val RobotoCondensed = FontFamily(
    // Regular
    Font(R.font.roboto_condensed_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.roboto_condensed_italic, FontWeight.Normal, FontStyle.Italic),

    // Thin
    Font(R.font.roboto_condensed_thin, FontWeight.Thin, FontStyle.Normal),
    Font(R.font.roboto_condensed_thinitalic, FontWeight.Thin, FontStyle.Italic),

    // Extra Light
    Font(R.font.roboto_condensed_extralight, FontWeight.ExtraLight, FontStyle.Normal),
    Font(R.font.roboto_condensed_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),

    // Light
    Font(R.font.roboto_condensed_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.roboto_condensed_lightitalic, FontWeight.Light, FontStyle.Italic),

    // Medium
    Font(R.font.roboto_condensed_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.roboto_condensed_mediumitalic, FontWeight.Medium, FontStyle.Italic),

    // SemiBold
    Font(R.font.roboto_condensed_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.roboto_condensed_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),

    // Bold
    Font(R.font.roboto_condensed_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.roboto_condensed_bolditalic, FontWeight.Bold, FontStyle.Italic),

    // Extra Bold
    Font(R.font.roboto_condensed_extrabold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.roboto_condensed_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),

    // Black
    Font(R.font.roboto_condensed_black, FontWeight.Black, FontStyle.Normal),
    Font(R.font.roboto_condensed_blackitalic, FontWeight.Black, FontStyle.Italic)
)


val Typography = Typography(
    // 1) TitleFont
    displayLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 80.sp
    ),

    // 2) LargeFont
    displayMedium = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    ),

    // 3) Somewhat Large Font
    headlineMedium = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 35.sp
    ),

    // 3) NormalFont
    bodyLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),

    // 4) SmallFont
    bodyMedium = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    // 5) SmallItalicFont
    bodySmall = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontStyle = FontStyle.Italic
    ),

    // 6) ExtraSmallFont
    labelSmall = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)