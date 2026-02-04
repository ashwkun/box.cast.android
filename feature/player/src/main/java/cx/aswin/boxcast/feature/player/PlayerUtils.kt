package cx.aswin.boxcast.feature.player

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Extracts a seed color from a bitmap using Palette API.
 */
suspend fun extractSeedColor(bitmap: Bitmap): Color = withContext(Dispatchers.Default) {
    val scaledBitmap = if (bitmap.width > 100 || bitmap.height > 100) {
        val scale = 100f / max(bitmap.width, bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    } else {
        bitmap
    }
    
    val palette = Palette.Builder(scaledBitmap)
        .maximumColorCount(16)
        .generate()
    
    val color = palette.vibrantSwatch?.rgb?.let { Color(it) }
        ?: palette.mutedSwatch?.rgb?.let { Color(it) }
        ?: palette.dominantSwatch?.rgb?.let { Color(it) }
        ?: Color(0xFF6750A4) // Default purple
    
    if (scaledBitmap != bitmap) {
        scaledBitmap.recycle()
    }
    
    color
}

/**
 * Generates a color scheme from a seed color.
 */
fun generateColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seedColor.red * 255).toInt(),
        (seedColor.green * 255).toInt(),
        (seedColor.blue * 255).toInt(),
        hsl
    )
    
    fun hslToColor(h: Float, s: Float, l: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
    }
    
    val hue = hsl[0]
    val saturation = hsl[1].coerceIn(0.3f, 0.7f)
    
    return if (isDark) {
        darkColorScheme(
            primary = hslToColor(hue, saturation, 0.7f),
            onPrimary = hslToColor(hue, saturation * 0.3f, 0.15f),
            primaryContainer = hslToColor(hue, saturation * 0.5f, 0.25f),
            onPrimaryContainer = hslToColor(hue, saturation * 0.3f, 0.9f),
            surface = hslToColor(hue, saturation * 0.2f, 0.12f),
            onSurface = hslToColor(hue, saturation * 0.1f, 0.9f),
            surfaceVariant = hslToColor(hue, saturation * 0.3f, 0.2f),
            onSurfaceVariant = hslToColor(hue, saturation * 0.2f, 0.75f),
        )
    } else {
        lightColorScheme(
            primary = hslToColor(hue, saturation, 0.4f),
            onPrimary = hslToColor(hue, saturation * 0.2f, 0.95f),
            primaryContainer = hslToColor(hue, saturation * 0.6f, 0.9f),
            onPrimaryContainer = hslToColor(hue, saturation * 0.5f, 0.15f),
            surface = hslToColor(hue, saturation * 0.3f, 0.96f),
            onSurface = hslToColor(hue, saturation * 0.2f, 0.1f),
            surfaceVariant = hslToColor(hue, saturation * 0.4f, 0.92f),
            onSurfaceVariant = hslToColor(hue, saturation * 0.3f, 0.3f),
        )
    }
}

/**
 * Formats milliseconds to time string.
 * Shows HH:MM:SS if hours > 0, otherwise MM:SS.
 */
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
