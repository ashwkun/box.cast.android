package cx.aswin.boxcast.feature.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdvancedPlayerControls(
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    colorScheme: ColorScheme,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Squircle Shape (26% smoothing for small buttons)
    val squircleShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 14.dp, smoothnessAsPercentTL = 60,
        cornerRadiusTR = 14.dp, smoothnessAsPercentTR = 60,
        cornerRadiusBL = 14.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBR = 14.dp, smoothnessAsPercentBR = 60
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, androidx.compose.ui.Alignment.CenterHorizontally), // Centered + Wider Spacing
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // 1. LIKE (Squircle)
        Surface(
            color = colorScheme.primary.copy(alpha = 0.15f), // Fixed container color
            shape = squircleShape,
            modifier = Modifier
                .size(48.dp)
                .expressiveClickable(onClick = onLikeClick)
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = colorScheme.primary, // Always Primary
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. DOWNLOAD (Squircle)
        Surface(
            color = if (isDownloaded) colorScheme.tertiaryContainer else colorScheme.primary.copy(alpha = 0.15f),
            shape = squircleShape,
            modifier = Modifier
                .size(48.dp)
                .expressiveClickable(onClick = onDownloadClick)
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (isDownloading) {
                   CircularProgressIndicator(
                       modifier = Modifier.size(22.dp),
                       strokeWidth = 2.dp,
                       color = colorScheme.primary
                   )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                        contentDescription = "Download",
                        tint = if (isDownloaded) colorScheme.onTertiaryContainer else colorScheme.primary, // Adaptive tint
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // 3. QUEUE (Squircle)
        Surface(
            color = colorScheme.primary.copy(alpha = 0.15f),
            shape = squircleShape,
            modifier = Modifier
                .size(48.dp)
                .expressiveClickable(onClick = onQueueClick)
        ) {
             Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                 Icon(
                     Icons.Rounded.QueueMusic,
                     contentDescription = "Queue",
                     tint = colorScheme.primary, // Adaptive tint
                     modifier = Modifier.size(24.dp)
                 )
             }
        }
    }
}
