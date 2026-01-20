package cx.aswin.boxcast.feature.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import cx.aswin.boxcast.core.designsystem.component.ExpressiveExtendedFab
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EpisodeInfoScreen(
    episodeId: String,
    episodeTitle: String,
    episodeDescription: String,
    episodeImageUrl: String,
    episodeAudioUrl: String,
    episodeDuration: Int,
    podcastId: String,
    podcastTitle: String,
    viewModel: EpisodeInfoViewModel,
    onBack: () -> Unit,
    onPodcastClick: (String) -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(episodeId) {
        viewModel.loadEpisode(
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            episodeDescription = episodeDescription,
            episodeImageUrl = episodeImageUrl,
            episodeAudioUrl = episodeAudioUrl,
            episodeDuration = episodeDuration,
            podcastId = podcastId,
            podcastTitle = podcastTitle
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // M3 Expressive: Medium Flexible Top App Bar
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = episodeTitle,
                        maxLines = 2, // Flexible: allows wrapping
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            when (val state = uiState) {
                is EpisodeInfoUiState.Success -> {
                    val hasResume = state.resumePositionMs > 0
                    ExpressiveExtendedFab(
                        text = if (hasResume) "Resume" else "Play Episode",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onPlay
                    )
                }
                else -> { /* Hide FAB while loading */ }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is EpisodeInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    // M3 Expressive: Wavy Loader
                    BoxCastLoader.CircularWavy()
                }
            }
            is EpisodeInfoUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load episode", color = MaterialTheme.colorScheme.error)
                }
            }
            is EpisodeInfoUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Episode Image with simple fallback
                    // Episode Image with simple fallback
                    SubcomposeAsyncImage(
                        model = state.episode.imageUrl?.ifEmpty { null },
                        contentDescription = state.episode.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop
                    ) {
                        val imgState = painter.state
                        if (imgState is coil.compose.AsyncImagePainter.State.Loading || 
                            imgState is coil.compose.AsyncImagePainter.State.Error || 
                            state.episode.imageUrl.isNullOrEmpty()) {
                            // Simple fallback - gradient box placeholder
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }

                    // Podcast Name (Clickable)
                    TextButton(onClick = { onPodcastClick(state.podcastId) }) {
                        Text(
                            text = "From: ${state.podcastTitle}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Resume Progress (M3 Wavy)
                    if (state.resumePositionMs > 0 && state.durationMs > 0) {
                        Column {
                            val progress = (state.resumePositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                            val remaining = ((state.durationMs - state.resumePositionMs) / 60000).coerceAtLeast(1)
                            
                            Text(
                                text = "${remaining}m remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // M3 Expressive: Wavy Progress Bar
                            LinearWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp), // Thick variant (8dp)
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Description
                    if (state.episode.description.isNotEmpty()) {
                        Text(
                            text = state.episode.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Extra padding for FAB + NavBar
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
