package cx.aswin.boxcast.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// VIOLET (Baseline/Default)
private val VioletLight = generateBrandColorScheme(Color(0xFF6750A4), isDark = false)
private val VioletDark = generateBrandColorScheme(Color(0xFFD0BCFF), isDark = true)

// EMERALD (Green)
private val EmeraldLight = generateBrandColorScheme(Color(0xFF006C4C), isDark = false)
private val EmeraldDark = generateBrandColorScheme(Color(0xFF6CDBAC), isDark = true)

// OCEAN (Blue)
private val OceanLight = generateBrandColorScheme(Color(0xFF0061A4), isDark = false)
private val OceanDark = generateBrandColorScheme(Color(0xFF9ECAFF), isDark = true)

// SAKURA (Pink)
private val SakuraLight = generateBrandColorScheme(Color(0xFFBC004B), isDark = false)
private val SakuraDark = generateBrandColorScheme(Color(0xFFFFB2BE), isDark = true)

// TANGERINE (Orange)
private val TangerineLight = generateBrandColorScheme(Color(0xFF964900), isDark = false)
private val TangerineDark = generateBrandColorScheme(Color(0xFFFFB784), isDark = true)

// CRIMSON (Red)
private val CrimsonLight = generateBrandColorScheme(Color(0xFFB91823), isDark = false)
private val CrimsonDark = generateBrandColorScheme(Color(0xFFFFB3AD), isDark = true)

// CANARY (Yellow/Gold)
private val CanaryLight = generateBrandColorScheme(Color(0xFF725C00), isDark = false)
private val CanaryDark = generateBrandColorScheme(Color(0xFFEBC248), isDark = true)

@Composable
fun BoxCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    themeBrand: String = "violet",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (themeBrand) {
                "emerald" -> if (darkTheme) EmeraldDark else EmeraldLight
                "ocean" -> if (darkTheme) OceanDark else OceanLight
                "sakura" -> if (darkTheme) SakuraDark else SakuraLight
                "tangerine" -> if (darkTheme) TangerineDark else TangerineLight
                "crimson" -> if (darkTheme) CrimsonDark else CrimsonLight
                "canary" -> if (darkTheme) CanaryDark else CanaryLight
                else -> if (darkTheme) VioletDark else VioletLight // Default "violet"
            }
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BoxCastTypography,
        shapes = BoxCastShapes,
        content = content
    )
}

/**
 * Generates a complete Material 3 color scheme algorithmically from a single semantic seed color.
 */
fun generateBrandColorScheme(seedColor: Color, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.RGBToHSL(
        (seedColor.red * 255).toInt(),
        (seedColor.green * 255).toInt(),
        (seedColor.blue * 255).toInt(),
        hsl
    )
    
    fun hslToColor(h: Float, s: Float, l: Float): Color {
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
    }
    
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.3f, 0.8f)

    return if (isDark) {
        darkColorScheme(
            primary = hslToColor(hue, sat, 0.7f),
            onPrimary = hslToColor(hue, sat * 0.3f, 0.15f),
            primaryContainer = hslToColor(hue, sat * 0.5f, 0.25f),
            onPrimaryContainer = hslToColor(hue, sat * 0.3f, 0.9f),
            secondary = hslToColor(hue, sat * 0.4f, 0.7f),
            onSecondary = hslToColor(hue, sat * 0.3f, 0.15f),
            secondaryContainer = hslToColor(hue, sat * 0.4f, 0.25f),
            onSecondaryContainer = hslToColor(hue, sat * 0.3f, 0.9f),
            tertiary = hslToColor(hue + 30f, sat * 0.5f, 0.7f),
            onTertiary = hslToColor(hue + 30f, sat * 0.3f, 0.15f),
            tertiaryContainer = hslToColor(hue + 30f, sat * 0.5f, 0.25f),
            onTertiaryContainer = hslToColor(hue + 30f, sat * 0.3f, 0.9f),
            background = hslToColor(hue, sat * 0.15f, 0.10f),
            onBackground = hslToColor(hue, sat * 0.1f, 0.90f),
            surface = hslToColor(hue, sat * 0.15f, 0.10f),
            onSurface = hslToColor(hue, sat * 0.1f, 0.90f),
            surfaceVariant = hslToColor(hue, sat * 0.3f, 0.20f),
            onSurfaceVariant = hslToColor(hue, sat * 0.2f, 0.75f),
            outline = hslToColor(hue, sat * 0.25f, 0.60f),
            outlineVariant = hslToColor(hue, sat * 0.2f, 0.30f),
            surfaceContainerLowest = hslToColor(hue, sat * 0.15f, 0.05f),
            surfaceContainerLow = hslToColor(hue, sat * 0.15f, 0.08f),
            surfaceContainer = hslToColor(hue, sat * 0.15f, 0.12f),
            surfaceContainerHigh = hslToColor(hue, sat * 0.15f, 0.15f),
            surfaceContainerHighest = hslToColor(hue, sat * 0.15f, 0.20f)
        )
    } else {
        lightColorScheme(
            primary = hslToColor(hue, sat, 0.4f),
            onPrimary = hslToColor(hue, sat * 0.2f, 0.95f),
            primaryContainer = hslToColor(hue, sat * 0.6f, 0.9f),
            onPrimaryContainer = hslToColor(hue, sat * 0.5f, 0.15f),
            secondary = hslToColor(hue, sat * 0.4f, 0.4f),
            onSecondary = hslToColor(hue, sat * 0.2f, 0.95f),
            secondaryContainer = hslToColor(hue, sat * 0.4f, 0.9f),
            onSecondaryContainer = hslToColor(hue, sat * 0.4f, 0.15f),
            tertiary = hslToColor(hue + 30f, sat * 0.5f, 0.4f),
            onTertiary = hslToColor(hue + 30f, sat * 0.2f, 0.95f),
            tertiaryContainer = hslToColor(hue + 30f, sat * 0.5f, 0.9f),
            onTertiaryContainer = hslToColor(hue + 30f, sat * 0.4f, 0.15f),
            background = hslToColor(hue, sat * 0.1f, 0.98f),
            onBackground = hslToColor(hue, sat * 0.1f, 0.10f),
            surface = hslToColor(hue, sat * 0.1f, 0.98f),
            onSurface = hslToColor(hue, sat * 0.1f, 0.10f),
            surfaceVariant = hslToColor(hue, sat * 0.3f, 0.90f),
            onSurfaceVariant = hslToColor(hue, sat * 0.2f, 0.30f),
            outline = hslToColor(hue, sat * 0.25f, 0.45f),
            outlineVariant = hslToColor(hue, sat * 0.25f, 0.80f),
            surfaceContainerLowest = hslToColor(hue, sat * 0.1f, 1.00f),
            surfaceContainerLow = hslToColor(hue, sat * 0.1f, 0.96f),
            surfaceContainer = hslToColor(hue, sat * 0.15f, 0.93f),
            surfaceContainerHigh = hslToColor(hue, sat * 0.15f, 0.90f),
            surfaceContainerHighest = hslToColor(hue, sat * 0.15f, 0.86f)
        )
    }
}
