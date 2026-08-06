package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayFontFamily = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-black"), weight = FontWeight.Black),
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Bold)
)

val SignatureFontFamily = FontFamily(
    Font(DeviceFontFamilyName("cursive"), weight = FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
)

