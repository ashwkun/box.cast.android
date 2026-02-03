package cx.aswin.boxcast.core.designsystem.components

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cx.aswin.boxcast.core.designsystem.R
import java.io.File

private const val TAG = "BoxCastLogo"

/**
 * Reusable BOXCAST logo with each letter having distinct variable font axes.
 * Uses native Android Typeface.Builder + scaleX transform for width control.
 */
@Composable
fun BoxCastLogo(
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface 
) {
    val context = LocalContext.current
    
    // Log API level
    LaunchedEffect(Unit) {
        Log.d(TAG, "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}, O = ${Build.VERSION_CODES.O}")
    }
    
    // Define letter styles: (weight, slant, scaleX)
    // Same height, only WIDE scaling (no squeezing), varied weights/slants
    data class LetterStyle(val weight: Float, val slant: Float, val scaleX: Float)
    val letterStyles = listOf(
        LetterStyle(900f, 0f, 1.5f),    // B - Black, WIDE
        LetterStyle(200f, -10f, 1.0f),  // O - Thin, normal, italic
        LetterStyle(1000f, 0f, 1.0f),   // X - Ultra black, normal
        LetterStyle(300f, -12f, 1.8f),  // C - Light, VERY WIDE, italic
        LetterStyle(700f, 0f, 1.0f),    // A - Bold, normal
        LetterStyle(400f, -8f, 1.3f),   // S - Regular, wide, slight italic
        LetterStyle(1000f, 0f, 1.2f)    // T - Ultra black, slightly wide
    )
    
    // Pre-create typefaces using native API (weight + slant only)
    val typefaces = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            letterStyles.mapIndexed { index, style ->
                try {
                    val fontRes = context.resources.openRawResource(R.font.robotoflex_variable)
                    val tempFile = File.createTempFile("font_$index", ".ttf", context.cacheDir)
                    tempFile.outputStream().use { fontRes.copyTo(it) }
                    
                    val typeface = android.graphics.Typeface.Builder(tempFile)
                        .setFontVariationSettings(
                            "'wght' ${style.weight}, 'slnt' ${style.slant}"
                        )
                        .build()
                    
                    tempFile.delete()
                    typeface
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create typeface: ${e.message}")
                    android.graphics.Typeface.DEFAULT
                }
            }
        } else {
            List(7) { android.graphics.Typeface.DEFAULT }
        }
    }
    
    val letters = "BOXCAST"
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)  // Space between letters
    ) {
        letters.forEachIndexed { index, char ->
            val typeface = typefaces[index]
            val style = letterStyles[index]
            
            // Use AndroidView with scaleX for width control (same height for all)
            AndroidView(
                factory = { ctx ->
                    android.widget.TextView(ctx).apply {
                        text = char.toString()
                        textSize = 24f  // Same height for all
                        setTextColor(android.graphics.Color.WHITE)
                        this.typeface = typeface
                        includeFontPadding = false
                        scaleX = style.scaleX
                    }
                },
                update = { tv ->
                    tv.typeface = typeface
                    tv.scaleX = style.scaleX
                    tv.setTextColor(textColor.hashCode()) // Ensure integer color is used
                }
            )
        }
    }
}
