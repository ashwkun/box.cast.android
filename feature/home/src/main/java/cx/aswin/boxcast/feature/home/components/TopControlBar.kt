package cx.aswin.boxcast.feature.home.components

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import cx.aswin.boxcast.core.designsystem.R
import androidx.compose.foundation.combinedClickable

private const val TAG = "StylizedLogo"

/**
 * Collapsing M3-aligned Top Bar with stylized variable logo and profile.
 * 
 * @param scrollFraction 0f = fully expanded (roomier), 1f = fully collapsed
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TopControlBar(
    scrollFraction: Float = 0f,
    modifier: Modifier = Modifier,
    onAvatarLongClick: () -> Unit = {}
) {
    // Expanded state: roomier padding, surface color
    // Collapsed state: compact padding, surfaceContainerLow color
    val expandedPadding = 16.dp
    val collapsedPadding = 8.dp
    
    val expandedColor = MaterialTheme.colorScheme.surface
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainerLow
    
    // Animate based on scroll fraction
    val verticalPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(expandedPadding, collapsedPadding, scrollFraction.coerceIn(0f, 1f)),
        animationSpec = tween(durationMillis = 150),
        label = "paddingAnimation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = lerp(expandedColor, collapsedColor, scrollFraction.coerceIn(0f, 1f)),
        animationSpec = tween(durationMillis = 150),
        label = "colorAnimation"
    )
    
    // Update system status bar color to match
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            
            // Calculate luminance to determine icon color
            // High luminance (light bg) = dark icons, Low luminance (dark bg) = light icons
            val luminance = (0.299f * backgroundColor.red + 
                            0.587f * backgroundColor.green + 
                            0.114f * backgroundColor.blue)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = luminance > 0.5f
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stylized variable logo
        cx.aswin.boxcast.core.designsystem.components.BoxCastLogo()
        


        // Profile
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .size(40.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onAvatarLongClick
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



