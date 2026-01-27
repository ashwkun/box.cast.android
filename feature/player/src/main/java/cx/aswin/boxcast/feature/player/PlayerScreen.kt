package cx.aswin.boxcast.feature.player

import cx.aswin.boxcast.core.designsystem.theme.simpleSharedElement
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown

import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun PlayerRoute(
    podcastId: String,
    apiBaseUrl: String,
    apiKey: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: PlayerViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(application, apiBaseUrl, apiKey) as T
            }
        }
    )
    
    LaunchedEffect(podcastId) {
        viewModel.loadPodcast(podcastId)
    }

    val uiState by viewModel.uiState.collectAsState()
    
    PlayerScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPlayPause = viewModel::togglePlayPause,
        onEpisodeClick = viewModel::playEpisode,
        onSeek = viewModel::seekTo,
        onSkipForward = viewModel::skipForward,
        onSkipBackward = viewModel::skipBackward,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets(0), // Full bleed
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is PlayerUiState.Loading -> {
                    // M3 Expressive: Morphing Loader
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BoxCastLoader.Expressive()
                    }
                }
                is PlayerUiState.Success -> {
                    PlayerContent(
                        podcast = uiState.podcast,
                        episodes = uiState.episodes,
                        currentEpisode = uiState.currentEpisode,
                        isPlaying = uiState.isPlaying,
                        positionMs = uiState.positionMs,
                        durationMs = uiState.durationMs,
                        onPlayPause = onPlayPause,
                        onEpisodeClick = onEpisodeClick,
                        onSeek = onSeek,
                        onSkipForward = onSkipForward,
                        onSkipBackward = onSkipBackward
                    )
                }
                is PlayerUiState.Error -> {
                    Text("Error loading player", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun PlayerContent(
    podcast: Podcast,
    episodes: List<Episode>,
    currentEpisode: Episode?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Artwork with extraLarge shape
        AsyncImage(
            model = currentEpisode?.imageUrl ?: podcast.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Metadata
        Text(
            text = currentEpisode?.title ?: podcast.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = podcast.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Seekbar
        if (durationMs > 0) {
            Column {
                androidx.compose.material3.Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // M3 Expressive: Bouncy Visualizer
        AudioVisualizer(isPlaying = isPlaying)

        Spacer(modifier = Modifier.height(16.dp))
        
        // Controls with Expressive Motion
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
             // Skip Back
             IconButton(onClick = onSkipBackward) {
                 Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s")
             }
             
             Spacer(modifier = Modifier.width(16.dp))
             
             // M3 Expressive: Large Play Button with bouncy click
             FilledIconButton(
                 onClick = onPlayPause,
                 modifier = Modifier
                     .size(72.dp)
                     .expressiveClickable(onClick = onPlayPause),
                 colors = IconButtonDefaults.filledIconButtonColors(
                     containerColor = MaterialTheme.colorScheme.primary
                 )
             ) {
                 Icon(
                     imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                     contentDescription = "Play/Pause",
                     modifier = Modifier.size(32.dp),
                     tint = MaterialTheme.colorScheme.onPrimary
                 )
             }
             
             Spacer(modifier = Modifier.width(16.dp))
             
             // Skip Forward
             IconButton(onClick = onSkipForward) {
                 Icon(Icons.Rounded.Forward30, contentDescription = "Forward 30s")
             }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Up Next with segmented styling
        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // M3 Expressive: Segmented Episode List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             items(episodes) { episode ->
                 // Segmented List Item
                 ElevatedCard(
                     modifier = Modifier
                         .fillMaxWidth()
                         .expressiveClickable { onEpisodeClick(episode) },
                     shape = MaterialTheme.shapes.medium
                 ) {
                     Row(
                         modifier = Modifier.padding(12.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         AsyncImage(
                             model = episode.imageUrl,
                             contentDescription = null,
                             modifier = Modifier
                                 .size(48.dp)
                                 .clip(MaterialTheme.shapes.small),
                             contentScale = ContentScale.Crop
                         )
                         Spacer(modifier = Modifier.width(12.dp))
                         Text(
                             text = episode.title,
                             style = MaterialTheme.typography.bodyMedium,
                             maxLines = 2,
                             overflow = TextOverflow.Ellipsis
                         )
                     }
                 }
             }
        }
    }
}



/**
 * M3 Expressive: Bouncy Audio Visualizer using Spring physics.
 */
@Composable
fun AudioVisualizer(isPlaying: Boolean) {
    val barCount = 20
    val animatables = remember { List(barCount) { Animatable(0.3f) } }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatables.forEach { animatable ->
                launch {
                     while(true) {
                         // M3 Expressive: Spring physics for bouncy bars
                         animatable.animateTo(
                             targetValue = Random.nextFloat().coerceIn(0.2f, 1f),
                             animationSpec = spring(
                                 dampingRatio = Spring.DampingRatioMediumBouncy,
                                 stiffness = Spring.StiffnessLow
                             )
                         )
                     }
                }
            }
        } else {
             animatables.forEach { 
                 launch {
                    // Settle down with bounce
                    it.animateTo(
                        0.1f, 
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                 }
             }
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        animatables.forEach { animatable ->
             Box(
                 modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height((60 * animatable.value).dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
             )
        }
    }
}
