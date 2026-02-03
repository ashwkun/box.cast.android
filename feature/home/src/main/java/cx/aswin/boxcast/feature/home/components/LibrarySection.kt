package cx.aswin.boxcast.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import cx.aswin.boxcast.core.designsystem.theme.SectionHeaderFontFamily
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.designsystem.components.AnimatedShapesFallback

/**
 * Merged "Your Shows" Section (formerly LibrarySection + LatestSection)
 *
 * Layout:
 * 1. Header: Icon + "Your Shows" + Arrow (styled like OnTheRise)
 * 2. Part A: Small podcast cover grid (max 2 rows, 4 per row) + "View Library" button
 * 3. Part B: Scrollable row of new episode cards (linked to EpisodeInfo)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YourShowsSection(
    subscribedPodcasts: List<Podcast>,
    latestEpisodes: List<Podcast>, // Podcasts with latestEpisode populated
    onPodcastClick: (Podcast) -> Unit,
    onEpisodeClick: (Episode, Podcast) -> Unit, // Navigate to EpisodeInfo
    onViewLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (subscribedPodcasts.isEmpty() && latestEpisodes.isEmpty()) return

    Column(modifier = modifier) {
        // --- Header (Styled like OnTheRise) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Shows",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = SectionHeaderFontFamily
                    ),
                    letterSpacing = 0.sp
                )
            }

            FilledTonalIconButton(
                onClick = onViewLibrary,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "View Library",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- Part A: Subscribed Shows Grid (5 items + 2 items + Button) ---
        if (subscribedPodcasts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp) // Standard margin
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalColumns = 5
                val row1Items = subscribedPodcasts.take(totalColumns)
                val row2Items = subscribedPodcasts.drop(totalColumns).take(2)
                val remainingCount = (subscribedPodcasts.size - 7).coerceAtLeast(0)
                // Show button if we have more items OR if we have valid items in row 2 that leave space
                // User asked for 5 items, then 2 items + button.
                // We'll show Row 2 if we have > 5 items.
                
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Fill with items
                    row1Items.forEach { podcast ->
                        Box(modifier = Modifier.weight(1f)) {
                           ResponsivePodcastCover(podcast = podcast, onClick = { onPodcastClick(podcast) })
                        }
                    }
                    // Fill remaining empty slots in Row 1 if < 5
                    repeat(totalColumns - row1Items.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Row 2
                if (subscribedPodcasts.size > 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // First 2 slots: Items
                        for (i in 0 until 2) {
                            if (i < row2Items.size) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ResponsivePodcastCover(podcast = row2Items[i], onClick = { onPodcastClick(row2Items[i]) })
                                }
                            } else {
                                // Empty slot if we somehow have < 2 items here but entered this block (unlikely given logic)
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        // Remaining 3 slots: Button
                        // Button spans 3 columns
                        Box(modifier = Modifier.weight(3f)) {
                             FilledTonalButton(
                                onClick = onViewLibrary,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).aspectRatio(3f/1f, matchHeightConstraintsFirst = false) // Attempt to match height aspect
                            ) {
                                // We need the button to match the height of the covers.
                                // Covers are roughly aspect ratio 1:1.
                                // So this button is spanning 3 slots.
                                // It should probably just fill height.
                                // Let's use a simpler approach for the button content.
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (remainingCount > 0) "+$remainingCount" else "More",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Part B: New Episodes Rail ---
        if (latestEpisodes.isNotEmpty()) {
            Text(
                text = "New Episodes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(latestEpisodes, key = { "${it.id}_${it.latestEpisode?.id}" }) { podcast ->
                    val episode = podcast.latestEpisode
                    if (episode != null) {
                        NewEpisodeCard(
                            episode = episode,
                            podcast = podcast,
                            onClick = { onEpisodeClick(episode, podcast) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Responsive podcast cover for weighted grids.
 */
@Composable
private fun ResponsivePodcastCover(
    podcast: Podcast,
    onClick: () -> Unit
) {
    SubcomposeAsyncImage(
        model = podcast.imageUrl,
        contentDescription = podcast.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square
            .clip(MaterialTheme.shapes.small)
            .expressiveClickable(onClick = onClick)
    ) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Loading || 
            state is AsyncImagePainter.State.Error || 
            podcast.imageUrl.isEmpty()) {
            AnimatedShapesFallback()
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

/**
 * Small podcast cover (Fixed 56dp) - Deprecated for new grid
 */
@Composable
private fun SmallPodcastCover(
    podcast: Podcast,
    onClick: () -> Unit
) {
    SubcomposeAsyncImage(
        model = podcast.imageUrl,
        contentDescription = podcast.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.small)
            .expressiveClickable(onClick = onClick)
    ) {
        // ... (Same content)
        val state = painter.state
        if (state is AsyncImagePainter.State.Loading || 
            state is AsyncImagePainter.State.Error || 
            podcast.imageUrl.isEmpty()) {
            AnimatedShapesFallback()
        } else {
            SubcomposeAsyncImageContent()
        }
    }
}

/**
 * New Episode Card for the scrollable rail.
 * Shows episode cover with gradient overlay and title.
 */
@Composable
private fun NewEpisodeCard(
    episode: Episode,
    podcast: Podcast,
    onClick: () -> Unit
) {
    // Track image URL chain: Episode → Podcast
    var currentUrl by remember(episode.imageUrl, podcast.imageUrl) {
        mutableStateOf(
            episode.imageUrl?.takeIf { it.isNotEmpty() } 
                ?: podcast.imageUrl.takeIf { it.isNotEmpty() }
        )
    }

    // Match PodcastCard style but smaller (140.dp width)
    androidx.compose.material3.OutlinedCard(
        shape = MaterialTheme.shapes.medium, // Match PodcastCard's large shape, slightly smaller for mini card
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(140.dp)
            .expressiveClickable(onClick = onClick)
    ) {
        Column {
            Box {
                SubcomposeAsyncImage(
                    model = currentUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Square image
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            // Try fallback to podcast cover
                            val episodeUrl = episode.imageUrl
                            if (currentUrl == episodeUrl && podcast.imageUrl.isNotEmpty()) {
                                currentUrl = podcast.imageUrl
                            }
                        }
                    }
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading || 
                        state is AsyncImagePainter.State.Error || 
                        currentUrl.isNullOrEmpty()) {
                        AnimatedShapesFallback()
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
            
            Column(modifier = Modifier.padding(10.dp)) {
                // Episode title
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Podcast name
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Keep backward compatibility - deprecated, use YourShowsSection
@Deprecated("Use YourShowsSection instead", ReplaceWith("YourShowsSection"))
@Composable
fun LibrarySection(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    modifier: Modifier = Modifier
) {
    YourShowsSection(
        subscribedPodcasts = podcasts,
        latestEpisodes = emptyList(),
        onPodcastClick = onPodcastClick,
        onEpisodeClick = { _, _ -> },
        onViewLibrary = {},
        modifier = modifier
    )
}
