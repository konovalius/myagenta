package com.example.myagent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myagent.R

val SmoochSans = FontFamily(
    Font(R.font.smooch_sans_black, weight = FontWeight.Black)
)

private val headerShadow = Shadow(
    color = Color.Black.copy(alpha = 0.15f),
    offset = Offset(-8f, 8f),
    blurRadius = 6f
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SmoochSans,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        shadow = headerShadow
    ),
    displayMedium = TextStyle(
        fontFamily = SmoochSans,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        shadow = headerShadow
    ),
    headlineLarge = TextStyle(
        fontFamily = SmoochSans,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        shadow = headerShadow
    ),
    headlineMedium = TextStyle(
        fontFamily = SmoochSans,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        shadow = headerShadow
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SmoochSans,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
)