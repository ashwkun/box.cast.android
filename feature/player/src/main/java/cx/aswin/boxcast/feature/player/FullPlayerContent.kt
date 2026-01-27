package cx.aswin.boxcast.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cx.aswin.boxcast.core.data.PlaybackRepository

@Composable
fun FullPlayerContent(
    playbackRepository: PlaybackRepository,
    colorScheme: ColorScheme,
    onCollapse: () -> Unit
) {
    val state by playbackRepository.playerState.collectAsState()
    val episode = state.currentEpisode ?: return
    val podcast = state.currentPodcast
    
    // Use passed colorScheme which is already adaptive
    val containerColor = colorScheme.primaryContainer.copy(alpha = 0.6f).compositeOver(colorScheme.surface) // Blend for depth
    val controlTint = colorScheme.primary
    
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDarkTheme = isSystemInDarkTheme()
    
    // Manage system bars
    val window = (LocalContext.current as? android.app.Activity)?.window
    SideEffect {
        window?.let { win ->
             val insetsController = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
             win.statusBarColor = android.graphics.Color.TRANSPARENT
             win.navigationBarColor = android.graphics.Color.TRANSPARENT
             insetsController.isAppearanceLightStatusBars = !isDarkTheme
             insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(top = statusBarPadding, bottom = navBarPadding)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorScheme.onSurface.copy(alpha = 0.1f))
                    .clickable(onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.weight(1f))
        
            Box(modifier = Modifier.size(42.dp))
        }
        
        Spacer(modifier = Modifier.height(12.dp)) // Tight gap
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 1. Album Art
            val imageUrl = episode.imageUrl?.takeIf { it.isNotBlank() } ?: podcast?.imageUrl
            val context = LocalContext.current
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f, fill = false)
                    .aspectRatio(1f)
                    .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = controlTint.copy(alpha = 0.3f), spotColor = controlTint.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .allowHardware(false) // Required for Palette
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp)) // Reduced Buffer
        
        // 2. Metadata
        Column(modifier = Modifier.fillMaxWidth().height(60.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // Reduced from titleLarge
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip, // Must be Clip for marquee
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast?.title ?: "",
                style = MaterialTheme.typography.bodyMedium, // Reduced from bodyLarge
                color = colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp)) // Tight gap
            
            // 3. Linear Buffered Slider
            if (state.duration > 0) {
                val bufferedPercentage = (state.bufferedPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
                
                LinearBufferedSlider(
                    position = state.position,
                    duration = state.duration,
                    bufferedPercentage = bufferedPercentage,
                    onSeek = { playbackRepository.seekTo(it) },
                    color = controlTint
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 4. Player Controls
            PlayerControls(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                colorScheme = colorScheme,
                controlTint = controlTint,
                onPlayPause = {
                    if (state.isPlaying) playbackRepository.pause() else playbackRepository.resume()
                },
                onPrevious = { playbackRepository.skipBackward() },
                onNext = { playbackRepository.skipForward() }
            )
        }
    }
}

@Composable
private fun LinearBufferedSlider(
    position: Long,
    duration: Long,
    bufferedPercentage: Float,
    onSeek: (Long) -> Unit,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp) // Breathing room
    ) {
        // M3 Slider with buffer visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp), // Taller for better touch target
            contentAlignment = Alignment.Center
        ) {
            // Buffer track (behind the slider) - padded to align with Slider track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp) // Align with M3 Slider's internal thumb padding
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                // Buffered portion with proper rounding
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
            
            // M3 Slider
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
        
        // Time labels with better contrast
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
                text = formatTime(duration),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}
