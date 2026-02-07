package cx.aswin.boxcast.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable

@Composable
fun ExpressivePlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    isResume: Boolean,
    accentColor: Color,
    progress: Float = 0f,
    timeText: String? = null,
    modifier: Modifier = Modifier
) {
    // Stadium / Pill Shape (M3 Standard)
    val playPillShape = CircleShape

    Surface(
        color = accentColor,
        contentColor = Color.White,
        shape = playPillShape,
        modifier = modifier
            .height(56.dp)
            .expressiveClickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // INTEGRATED PROGRESS BAR (Bottom Strip)
            if (isResume && progress > 0f) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp), // Thin strip at bottom
                        color = Color.White.copy(alpha = 0.5f), // Lighter tint of content
                        trackColor = Color.Transparent,
                        drawStopIndicator = {}
                    )
                }
            }

            // CONTENT
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp)
                )
                
                // Show text if there is space (implied, or always show for this component)
                Spacer(modifier = Modifier.width(10.dp))
                
                // Formatted Text: "Resume • 12m left" or "Play"
                val displayText = if (isPlaying) {
                    "Pause"
                } else if (isResume && timeText != null) {
                    "Resume • $timeText"
                } else if (isResume) {
                    "Resume"
                } else {
                    "Play"
                }
                
                Text(
                    text = displayText,
                    style = if (isResume) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
