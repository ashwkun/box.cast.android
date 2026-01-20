package cx.aswin.boxcast.core.designsystem.theme

import android.os.Build
import android.util.Log
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import cx.aswin.boxcast.core.designsystem.R

private const val TAG = "BoxCastTypography"

// Google Fonts Provider for dynamic font loading
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
// Roboto Flex via Google Fonts - variable weight support
private val robotoFlex = GoogleFont("Roboto Flex")

val RobotoFlexFamily = FontFamily(
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.Black)
).also { Log.d(TAG, "Roboto Flex loaded via Google Fonts provider") }

// Logo Font with Variable Axes - Using bundled TTF for full axis control
// Roboto Flex axes: wght (100-1000), wdth (25-151), GRAD (-200 to 150), opsz (8-144)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val LogoFontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            R.font.robotoflex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),  // Bold weight
                FontVariation.width(110f),  // Slightly wider
                FontVariation.Setting("GRAD", 50f),  // Subtle grade for thickness
                FontVariation.Setting("opsz", 48f)   // Optical size for display
            )
        )
    )
} else {
    RobotoFlexFamily // Fallback for older Android versions
}

// Media App "Condensed" Header Font - Wdth 85, Wght 800
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val SectionHeaderFontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            R.font.robotoflex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800),  // Extra Bold
                FontVariation.width(85f),   // Condensed (85% width)
                FontVariation.Setting("GRAD", 150f), // Heavy grade for punch
                FontVariation.Setting("opsz", 24f)  // Optical size for headers
            )
        )
    )
} else {
    RobotoFlexFamily
}

// Material 3 EXPRESSIVE Typography Scale
// Using bolder weights, tighter tracking, and compact line-heights for impact
val BoxCastTypography = Typography(
    // DISPLAY - Bold, tight, impactful
    displayLarge = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp
    ),

    // HEADLINE - Strong, confident
    headlineLarge = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.1).sp
    ),

    // TITLE - Slightly bolder, professional
    titleLarge = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),

    // BODY - Clean, readable
    bodyLarge = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),

    // LABEL - Medium weight, clear
    labelLarge = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)
