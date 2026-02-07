package cx.aswin.boxcast.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast

/**
 * Screen for displaying all liked episodes.
 * Moved from LibraryScreen.kt to support Predictive Back Navigation.
 */
@Composable
fun LikedEpisodesScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onEpisodeClick: (Episode, Podcast) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Liked Episodes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (uiState) {
                is LibraryUiState.Success -> {
                    val liked = (uiState as LibraryUiState.Success).likedEpisodes
                    if (liked.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No liked episodes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 120.dp) // Nav bar padding
                        ) {
                            items(liked) { historyItem ->
                                val episode = Episode(
                                    id = historyItem.episodeId,
                                    title = historyItem.episodeTitle ?: "Unknown Episode",
                                    description = "",
                                    imageUrl = historyItem.episodeImageUrl ?: "",
                                    audioUrl = historyItem.episodeAudioUrl ?: "",
                                    duration = ((historyItem.durationMs) / 1000).toInt(),
                                    publishedDate = 0L
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
                                    supportingContent = { Text(podcast.title, style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = {
                                        AsyncImage(
                                            model = episode.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                                            contentScale = ContentScale.Crop
                                        )
                                    },
                                    modifier = Modifier
                                        .clickable { onEpisodeClick(episode, podcast) }
                                )
                            }
                        }
                    }
                }
                else -> { /* Handle Loading/Error */ }
            }
        }
    }
}
