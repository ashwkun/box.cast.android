package cx.aswin.boxcast.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.aswin.boxcast.core.data.PlaybackRepository
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import androidx.compose.ui.layout.ContentScale

@Composable
fun FullPlayerContent(
    playbackRepository: PlaybackRepository,
    downloadRepository: cx.aswin.boxcast.core.data.DownloadRepository,
    colorScheme: ColorScheme,
    onCollapse: () -> Unit,
    onEpisodeInfoClick: (Episode) -> Unit = {},
    onPodcastInfoClick: (Podcast) -> Unit = {}
) {
    val state by playbackRepository.playerState.collectAsState()
    val episode = state.currentEpisode ?: return
    val podcast = state.currentPodcast ?: return // Safety check
    
    // Use passed colorScheme which is already adaptive
    val containerColor = colorScheme.primaryContainer.copy(alpha = 0.6f).compositeOver(colorScheme.surface) // Blend for depth
    
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDarkTheme = isSystemInDarkTheme()
    
    // Manage system bars
    val window = (LocalContext.current as? android.app.Activity)?.window
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isQueueVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isQueueVisible) {
        if (isQueueVisible) {
            // Scroll to Title (Index 2, assuming Spacer is Index 1)
            // Or just scroll to ensure Spacer is visible
            listState.animateScrollToItem(1)
        }
    }
    
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
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            userScrollEnabled = isQueueVisible // Only allow scroll if Queue is visible
        ) {
            // Item 0: The Main Player (Full Height)
            item {
                  val isDownloaded by remember(episode.id) { 
                     downloadRepository.isDownloaded(episode.id)
                  }.collectAsState(initial = false)

                  val isDownloading by remember(episode.id) {
                     downloadRepository.isDownloading(episode.id)
                  }.collectAsState(initial = false)

                 SharedPlayerContent(
                    podcast = podcast,
                    episode = episode,
                    isPlaying = state.isPlaying,
                    isLoading = state.isLoading,
                    positionMs = state.position,
                    durationMs = state.duration,
                    bufferedPositionMs = state.bufferedPosition,
                    playbackSpeed = state.playbackSpeed,
                    sleepTimerEnd = state.sleepTimerEnd,
                    isLiked = state.isLiked,
                    colorScheme = colorScheme,
                    onPlayPause = {
                        if (state.isPlaying) playbackRepository.pause() else playbackRepository.resume()
                    },
                    onSeek = { playbackRepository.seekTo(it) },
                    onPrevious = { playbackRepository.skipBackward() },
                    onNext = { playbackRepository.skipForward() },
                    onSetSpeed = { playbackRepository.setPlaybackSpeed(it) },
                    onSetSleepTimer = { playbackRepository.setSleepTimer(it) },
                    onLikeClick = { scope.launch { playbackRepository.toggleLike() } },
                    onDownloadClick = { 
                        scope.launch {
                            if (isDownloaded || isDownloading) {
                                downloadRepository.removeDownload(episode.id)
                            } else {
                                downloadRepository.addDownload(episode, podcast)
                            }
                        }
                    },
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                     onQueueClick = { 
                        isQueueVisible = !isQueueVisible
                    },
                    onEpisodeInfoClick = { 
                        onCollapse() // Minimize player first
                        onEpisodeInfoClick(episode) 
                    },
                    onPodcastInfoClick = { 
                        onCollapse() // Minimize player first
                        onPodcastInfoClick(podcast) 
                    },
                    modifier = Modifier
                        .fillParentMaxHeight() // Takes full available height
                        .padding(horizontal = 24.dp)
                )
            }
            
            if (isQueueVisible) {
                // Spacer between Player and Queue
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Item 1: "Up Next" Title
                item {
                    Text(
                         text = "Up Next",
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.Bold,
                         modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                     )
                }
                
                // Items: The Queue
                items(state.queue) { ep ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .expressiveClickable { 
                                scope.launch {
                                    playbackRepository.playEpisode(ep, podcast)
                                }
                            },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ep.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ep.title,
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
}

