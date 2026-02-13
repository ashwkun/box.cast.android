package cx.aswin.boxcast.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Person
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.designsystem.components.AdvancedPlayerControls
import cx.aswin.boxcast.feature.player.components.SimplePlayerControls

@Composable
fun SharedPlayerContent(
    podcast: Podcast,
    episode: Episode?, // Nullable
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    sleepTimerEnd: Long?,
    isLiked: Boolean,
    colorScheme: ColorScheme,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onQueueClick: () -> Unit,
    onEpisodeInfoClick: () -> Unit = {},
    onPodcastInfoClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
    footerContent: @Composable ColumnScope.() -> Unit = {}
) {
    val controlTint = colorScheme.primary
    val context = LocalContext.current
    
    MaterialTheme(colorScheme = colorScheme) {

        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
        ) {
            // Responsive breakpoints based on available height
            val isCompact = maxHeight < 600.dp
            val isMedium = maxHeight in 600.dp..700.dp
            
            // Artwork sizing: larger on big screens, smaller on small screens
            val artworkWidth = when {
                isCompact -> 0.55f // 55% width on small screens
                isMedium -> 0.65f // 65% on medium
                else -> 0.75f // 75% on large screens  
            }
            val artworkMaxSize = when {
                isCompact -> 200.dp
                isMedium -> 280.dp
                else -> 360.dp
            }
            
            // Control sizing for PlayerControls
            val controlRowHeight = if (isCompact) 64.dp else 80.dp
            val actionButtonSize = if (isCompact) 40.dp else 48.dp
            val spacingSmall = if (isCompact) 8.dp else 16.dp
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.weight(0.01f))
                // 1. Album Art - responsive sizing
                val imageUrl = episode?.imageUrl?.takeIf { it.isNotBlank() } ?: podcast.imageUrl
                
                Surface(
                    modifier = Modifier
                        .widthIn(max = artworkMaxSize)
                        .fillMaxWidth(artworkWidth)
                        .aspectRatio(1f)
                    .shadow(
                        12.dp, 
                        RoundedCornerShape(28.dp), 
                        ambientColor = controlTint.copy(alpha = 0.3f), 
                        spotColor = controlTint.copy(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .allowHardware(false) // Required for Palette if needed, safe to keep
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(spacingSmall))
        
            // 2. Metadata - removed fixed height for responsiveness
            Column(
                modifier = Modifier.fillMaxWidth(), 
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = episode?.title ?: podcast.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (episode != null) onEpisodeInfoClick() }
                        .basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (episode != null) podcast.title else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPodcastInfoClick() }
                        .basicMarquee()
                )
            }
            
            Spacer(modifier = Modifier.weight(0.01f))
            
            // 3. Linear Buffered Slider
            if (durationMs > 0) {
                val bufferedPercentage = (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                
                LinearBufferedSlider(
                    position = positionMs,
                    duration = durationMs,
                    bufferedPercentage = bufferedPercentage,
                    onSeek = onSeek,
                    color = controlTint
                )
            }
            
            Spacer(modifier = Modifier.weight(0.02f))
            
            // Slot for Visualizer or other content
            extraContent()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 4. Player Controls (Play/Pause/Skip)
            PlayerControls(
                isPlaying = isPlaying,
                isLoading = isLoading,
                colorScheme = colorScheme,
                controlTint = controlTint,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                height = controlRowHeight
            )
            
            Spacer(modifier = Modifier.weight(0.02f))
            
            // 5. Controls Section
            // Row 1: Playback Modifiers (Speed, Timer)
            SimplePlayerControls(
                 playbackSpeed = playbackSpeed,
                 sleepTimerEnd = sleepTimerEnd,
                 duration = durationMs, // Pass duration
                 position = positionMs, // Pass position
                 colorScheme = colorScheme,
                 onSpeedChange = onSetSpeed,
                 onSleepClick = onSetSleepTimer,
                 modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp)) // Reduced gap for small screens
            
            // Row 2: Actions (Like, Download, Queue)
            AdvancedPlayerControls(
                 isLiked = isLiked, 
                 isDownloaded = isDownloaded,
                 isDownloading = isDownloading,
                 colorScheme = colorScheme,
                 onLikeClick = onLikeClick,
                 onDownloadClick = onDownloadClick,
                 onQueueClick = onQueueClick,
                 style = cx.aswin.boxcast.core.designsystem.components.ControlStyle.Squircle,
                 controlSize = actionButtonSize,
                 modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.weight(0.02f))

            
            // Footer (e.g. Up Next List)
            footerContent()
            }
        }
    }
}

@Composable
fun LinearBufferedSlider(
    position: Long,
    duration: Long,
    bufferedPercentage: Float,
    onSeek: (Long) -> Unit,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // M3 Slider with buffer visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp), 
            contentAlignment = Alignment.Center
        ) {
            // Buffer track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                // Buffered portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedPercentage)
                        .fillMaxHeight()
                        .background(
                            color.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
            
            // Slider
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color.copy(alpha = 0.85f)
            )
            Text(
                text = "-" + formatTime((duration - position).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}
