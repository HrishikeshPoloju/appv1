package com.schoolapp.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GenesisColors {
    val MeteorBlack = Color(0xFF1E40AF)      // Royal School Blue Header & Brand Surface
    val MeteorSurface = Color(0xFF1D4ED8)    // Elevated Blue Pill / Action Surface
    val MeteorBorder = Color(0xFF3B82F6)     // Clean Blue Accent Border
    val SkyAccent = Color(0xFFBFDBFE)        // Soft Sky Blue Subtitle & Icon Accent
    val Primary = Color(0xFF2563EB)          // Vibrant Action Blue
    val Navy = Color(0xFF1E3A8A)             // Deep Campus Blue Banner
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val AiGreen = Color(0xFF10A37F)
    val ClassroomPurple = Color(0xFF7C3AED)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Emergency = Color(0xFFDC2626)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val Border = Color(0xFFE2E8F0)
}

object GenesisSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object GenesisRadii {
    val small = 8.dp
    val card = 16.dp
    val modal = 20.dp
    val pill = 999.dp
}

val GenesisTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
        color = GenesisColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = GenesisColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        color = GenesisColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        color = GenesisColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
        color = GenesisColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = GenesisColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = GenesisColors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        color = GenesisColors.TextSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        color = GenesisColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val LightColors = lightColorScheme(
    primary = GenesisColors.Primary,
    onPrimary = Color.White,
    secondary = GenesisColors.ClassroomPurple,
    onSecondary = Color.White,
    tertiary = GenesisColors.AiGreen,
    background = GenesisColors.Background,
    surface = GenesisColors.Surface,
    onBackground = GenesisColors.TextPrimary,
    onSurface = GenesisColors.TextPrimary,
    outline = GenesisColors.Border,
    error = GenesisColors.Emergency
)

@Composable
fun SchoolAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = GenesisTypography,
        content = content
    )
}
