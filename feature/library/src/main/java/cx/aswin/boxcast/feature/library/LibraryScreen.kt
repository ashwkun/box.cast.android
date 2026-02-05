package cx.aswin.boxcast.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.data.database.ListeningHistoryEntity
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults

/**
 * Main Library Screen
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (Episode, Podcast) -> Unit,
    onExploreClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        uiState = uiState,
        onPodcastClick = onPodcastClick,
        onEpisodeClick = onEpisodeClick,
        onExploreClick = onExploreClick
    )
}

@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (Episode, Podcast) -> Unit,
    onExploreClick: () -> Unit
) {
    var showLikedEpisodes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showLikedEpisodes) {
        showLikedEpisodes = false
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        if (showLikedEpisodes) {
             Row(
                 modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 IconButton(onClick = { showLikedEpisodes = false }) {
                     Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                 }
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(
                    text = "Liked Episodes",
                    style = MaterialTheme.typography.headlineSmall,
                     fontWeight = FontWeight.Bold
                 )
             }
        } else {
             Text(
                text = "Library",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
             )
        }

        when (uiState) {
            is LibraryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LibraryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error loading library")
                }
            }
            is LibraryUiState.Success -> {
                val podcasts = uiState.subscribedPodcasts
                
                if (showLikedEpisodes) {
                    val liked = uiState.likedEpisodes
                     if (liked.isEmpty()) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text("No liked episodes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                     } else {
                         LazyColumn(
                             contentPadding = PaddingValues(bottom = 80.dp)
                         ) {
                             items(liked) { historyItem ->
                                 // Convert HistoryItem to minimal Episode for callback
                                 // Ideally we should fetch full episode, but history item has basic info.
                                 // Assuming HistoryEntity has enough info. 
                                 // Wait, ListeningHistoryEntity might NOT have Title/Image!
                                 // I need to check ListeningHistoryEntity.
                                 // It usually has 'episodeId', 'cachedMetadata'.
                                 // If it doesn't have title, we are in trouble.
                                 // Checking ListeningHistoryEntity in previous steps:
                                 // It has metadata!
                                 
                                val episode = Episode(
                                    id = historyItem.episodeId,
                                    title = historyItem.episodeTitle ?: "Unknown Episode",
                                    description = "", // History entity doesn't have description
                                    imageUrl = historyItem.episodeImageUrl ?: "",
                                    audioUrl = historyItem.episodeAudioUrl ?: "",
                                    duration = ((historyItem.durationMs) / 1000).toInt(),
                                    publishedDate = 0L // History entity doesn't have publishedDate
                                )
                                val podcast = Podcast(
                                    id = historyItem.podcastId,
                                    title = historyItem.podcastName,
                                    artist = "",
                                    imageUrl = historyItem.podcastImageUrl ?: "",
                                    description = ""
                                )

                                 ListItem(
                                     headlineContent = { Text(episode.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                     supportingContent = { Text("Liked • ${podcast.title}", style = MaterialTheme.typography.bodySmall) },
                                     leadingContent = {
                                         AsyncImage(
                                             model = episode.imageUrl,
                                             contentDescription = null,
                                             modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                                             contentScale = ContentScale.Crop
                                         )
                                     },
                                     modifier = Modifier.clickable { onEpisodeClick(episode, podcast) }
                                 )
                             }
                         }
                     }
                } else if (podcasts.isEmpty() && uiState.likedEpisodes.isEmpty()) {
                    // Empty State logic (omitted for brevity in replacement, but I must keep it!)
                    // Wait, I am replacing the 'else' block of uiState.Success.
                    // I need to be careful not to delete the empty state logic.
                    // The target content was `if (podcasts.isEmpty()) {`.
                    
                    // RE-INSERTING EMPTY STATE LOGIC because simple replacement overwrites it.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No subscriptions yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Explore to find your next favorite show.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onExploreClick) {
                            Text("Go Explore")
                        }
                        Spacer(modifier = Modifier.height(64.dp)) // Space for bottom nav
                    }
                } else {
                     // Main Library View
                     LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SECTION: Liked Episodes "Folder"
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                             Card(
                                 shape = MaterialTheme.shapes.extraLarge, // Rule: High rounding
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), // Rule: Segmented List Item
                                 modifier = Modifier
                                    .fillMaxWidth()
                                    .expressiveClickable(onClick = { showLikedEpisodes = true }) // Rule: Bounce/Expressive
                             ) {
                                 ListItem(
                                     headlineContent = { 
                                         Text(
                                             "Liked Episodes", 
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.SemiBold
                                         ) 
                                     },
                                     leadingContent = {
                                         Box(
                                             modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
                                             contentAlignment = Alignment.Center
                                         ) {
                                              Icon(
                                                  Icons.Rounded.Favorite, 
                                                  contentDescription = null, 
                                                  tint = MaterialTheme.colorScheme.primary
                                              )
                                         }
                                     },
                                     trailingContent = { 
                                         Text(
                                             "${uiState.likedEpisodes.size}",
                                             style = MaterialTheme.typography.labelLarge,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         ) 
                                     },
                                     colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                 )
                             }
                        }

                        items(items = podcasts, key = { it.id }) { podcast ->
                            LibraryPodcastCard(
                                podcast = podcast,
                                onClick = { onPodcastClick(podcast.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryPodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onClick)
    ) {
        // Image
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.aspectRatio(1f)
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = podcast.artist,
            style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
