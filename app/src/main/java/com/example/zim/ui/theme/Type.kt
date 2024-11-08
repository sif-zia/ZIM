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
    Font(R.font.roboto_condensed_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.roboto_condensed_regular, FontWeight.Bold, FontStyle.Normal), // Use regular for bold
    Font(R.font.roboto_condensed_italic, FontWeight.Normal, FontStyle.Italic)
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